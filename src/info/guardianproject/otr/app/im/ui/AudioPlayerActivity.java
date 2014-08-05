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
    
    public static final String FILENAME = "filename";
    public static final String MIMETYPE = "mimeType";
    
    String filename;
    String mimeType;
    
    private TextView filenameTextView;
    private Button playButton;
    private boolean playState = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // vfs
        IocVfs.init();
        // ui
        setContentView(R.layout.audio_player_activity);
        filename = getIntent().getStringExtra(FILENAME);
        mimeType = getIntent().getStringExtra(MIMETYPE);
        filenameTextView = (TextView) findViewById(R.id.audio_player_text);
        filenameTextView.setText(filename) ;
        
        playButton = (Button) findViewById(R.id.audio_player_play);
        playButton.setOnClickListener(onClickPlay);
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        try {
            Uri uri = httpStream(filename, mimeType);
            initPlayer( uri );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private OnClickListener onClickPlay = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            if (playState)
                pause() ;
            else
                play();
        }
    };
    
    private void play() {
        playState = true;
        mMediaPlayer.start();
        refreshUi();
    }
    
    private void pause() {
        playState = false;
        mMediaPlayer.stop();
        refreshUi();
    }

    private void refreshUi() {
        if (playState) {
            playButton.setText("Pause");
        } else {
            playButton.setText("Play");
        }
    }
    
    MediaPlayer mMediaPlayer ;
    private void initPlayer( Uri uri ) throws Exception {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {
            
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                return;
            }
        });
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.stop();
            }
        });
        
        mMediaPlayer.setDataSource(this, uri);
        mMediaPlayer.prepare();
    }
    
    
    private ServerSocket ss = null;
    
    private Uri httpStream(final String filename, final String mimeType) throws IOException
    {
        final File f = new File( filename );
        if (!f.exists()) {
            throw new IOException("File not found " + filename);
        }
        
        final int port = 8080;
        boolean keepServerRunning = false;
        
        final String shareMimeType = "application/mpegts";
        
        try
        {
            if (ss != null)
                ss.close();
        }
        catch (Exception e){}
        
        new Thread ()
        {
            public void run ()
            {
                try {
                    
                    ss = new ServerSocket(port);
                    Socket socket = ss.accept();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append( "HTTP/1.1 200\r\n");
                    sb.append( "Content-Type: " + mimeType + "\r\n");
                    sb.append( "Content-Length: " + f.length() + "\r\n\r\n" );
                    
                    BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                    
                    bos.write(sb.toString().getBytes());
                    
                    int len = -1;
                    FileInputStream fis = new FileInputStream(f);
                    
                    int idx = 0;
                    
                    byte[] b = new byte[8096];
                    while ((len = fis.read(b)) != -1)
                    {
                        bos.write(b,0,len);
                        idx+=len;
                        Log.d("TAG","sharing via stream: " + idx);
                    }

                    fis.close();
                    bos.flush();
                    bos.close();
                    
                    socket.close();
                    ss.close();
                    ss = null;
                    
                } catch (IOException e) {
                    Log.d("ServerShare","web share error",e);
                }
            }
        }.start();
        
        Uri uri = Uri.parse("http://localhost:" + port + f.getAbsolutePath());
        return uri;
        
    }       
    
        
    
}
