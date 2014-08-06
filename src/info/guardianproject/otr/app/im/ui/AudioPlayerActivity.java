package info.guardianproject.otr.app.im.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.IocVfs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AudioPlayerActivity extends Activity {

    private static final String TAG = AudioPlayerActivity.class.getSimpleName();

    public static final String FILENAME = "filename";
    public static final String MIMETYPE = "mimeType";

    String filename;
    String mimeType;

    private TextView filenameTextView;
    private Button playButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filename = getIntent().getStringExtra(FILENAME);
        mimeType = getIntent().getStringExtra(MIMETYPE);
        // vfs
        IocVfs.init();
        // ui
        setContentView(R.layout.audio_player_activity);
        filenameTextView = (TextView) findViewById(R.id.audio_player_text);
        filenameTextView.setText(filename);

        playButton = (Button) findViewById(R.id.audio_player_play);
        playButton.setOnClickListener(onClickPlay);
        findViewById(R.id.audio_player_rewind).setOnClickListener(onClickRewind);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            Uri uri = httpStream(filename, mimeType);
            initPlayer(uri);
        } catch (Exception e) {
            // TODO error
            e.printStackTrace();
        }
    }

    private OnClickListener onClickPlay = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mediaPlayer.isPlaying())
                pause();
            else
                play();
        }
    };
    
    private OnClickListener onClickRewind = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaPlayer.stop();
            refreshUi();
        }
    };

    private void play() {
        mediaPlayer.start();
        refreshUi();
    }

    private void pause() {
        mediaPlayer.stop();
        refreshUi();
    }

    private void refreshUi() {
        if (mediaPlayer.isPlaying()) {
            playButton.setText("Pause");
        } else {
            playButton.setText("Play");
        }
    }

    private MediaPlayer mediaPlayer;

    private void initPlayer(Uri uri) throws Exception {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                return;
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.stop();
            }
        });

        mediaPlayer.setDataSource(this, uri);
        mediaPlayer.prepare();
    }

    private ServerSocket serverSocket = null;

    private Uri httpStream(final String filename, final String mimeType) throws IOException {
        final File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File not found " + filename);
        }

        final int port = 8080;

        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
        }

        new Thread() {
            public void run() {
                try {

                    serverSocket = new ServerSocket(port);
                    Socket socket = serverSocket.accept();

                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP/1.1 200\r\n");
                    sb.append("Content-Type: " + mimeType + "\r\n");
                    sb.append("Content-Length: " + file.length() + "\r\n\r\n");

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                    bos.write(sb.toString().getBytes());

                    int len = -1;
                    FileInputStream fis = new FileInputStream(file);

                    int idx = 0;

                    byte[] b = new byte[8096];
                    while ((len = fis.read(b)) != -1) {
                        bos.write(b, 0, len);
                        idx += len;
                        Log.d(TAG, "sharing via stream: " + idx);
                    }

                    fis.close();
                    bos.flush();
                    bos.close();

                    socket.close();
                    serverSocket.close();
                    serverSocket = null;

                } catch (IOException e) {
                    Log.d(TAG, "web share error", e);
                }
            }
        }.start();

        Uri uri = Uri.parse("http://localhost:" + port + file.getAbsolutePath());
        return uri;

    }

}
