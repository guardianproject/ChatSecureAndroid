package info.guardianproject.otr.app.im.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.IocVfs;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        // ui
        setContentView(R.layout.audio_player_activity);
        filenameTextView = (TextView) findViewById(R.id.audio_player_text);
        filenameTextView.setText(filename);

        playButton = (Button) findViewById(R.id.audio_player_play);
        playButton.setOnClickListener(onClickPlay);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            initPlayer(filename, mimeType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OnClickListener onClickPlay = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mediaPlayer == null) {
                play();
                return;
            }
            if (mediaPlayer.isPlaying())
                pause();
            else
                play();
        }
    };
    
    private void play() {
        try {
            initPlayer(filename, mimeType);
            refreshUi();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void pause() {
        killPlayer();
    }

    private void refreshUi() {
        if (mediaPlayer == null) {
            Log.e(TAG, "refreshUi: No player");
            playButton.setText("Play");
            return;
        }
        // TODO use string resources
        if (mediaPlayer.isPlaying()) {
            Log.e(TAG, "refreshUi: Stop");
            playButton.setText("Stop");
        } else {
            Log.e(TAG, "refreshUi: Play");
            playButton.setText("Play");
        }
    }

    private MediaPlayer mediaPlayer;
    
    private void killPlayer() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        refreshUi();
    }
    
    private void initPlayer(String filename, String mimeType) throws Exception {
        Uri uri = httpStream(filename, mimeType);
        
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                refreshUi();
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
                killPlayer();
            }
        });

        mediaPlayer.setDataSource(this, uri);
        mediaPlayer.prepare();
    }

    private ServerSocket serverSocket = null;

    private Uri httpStream(final String filename, final String mimeType) throws IOException {
        
        // FIXME generate a random token for security
        final File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File not found " + filename);
        }

        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
        }

        serverSocket = new ServerSocket(0); // use random free port
        new Thread() {
            public void run() {
                try {
                    while (true) {
                    Socket socket = serverSocket.accept();

                    byte[] b = new byte[8192];
                    int len;
                    
                    InputStream is = socket.getInputStream();
                    StringBuilder isb = new StringBuilder();
                    len = is.read(b);
                    isb.append(new String(b));
                    
                    Log.i(TAG, "request: " + isb.toString());
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP/1.1 200\r\n");
                    sb.append("Content-Type: " + mimeType + "\r\n");
                    sb.append("Content-Length: " + file.length() + "\r\n\r\n");

                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                    bos.write(sb.toString().getBytes());

                    FileInputStream fis = new FileInputStream(file);

                    int idx = 0;

                    while ((len = fis.read(b)) != -1) {
                        bos.write(b, 0, len);
                        idx += len;
                        Log.d(TAG, "sharing via stream: " + idx);
                    }

                    fis.close();
                    bos.flush();
                    bos.close();

                    socket.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "web share error", e);
                }
            }
        }.start();

        Uri uri = Uri.parse("http://localhost:" + serverSocket.getLocalPort() + file.getAbsolutePath());
        return uri;
    }
}
