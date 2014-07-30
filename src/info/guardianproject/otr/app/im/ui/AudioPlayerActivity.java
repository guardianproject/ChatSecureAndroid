package info.guardianproject.otr.app.im.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.app.IocVfs;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AudioPlayerActivity extends Activity {
    
    public static final String FILENAME = "filename";
    
    private String sourceFilename;
    private String cacheFilename;
    
    private AudioManager audioManager;
    private SoundPool soundPool;
    private int soundID;
    private TextView filenameTextView;
    private Button playButton;
    private boolean playState = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // vfs
        IocVfs.init();
        // audio
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);        
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); // hardware controls audio
        // ui
        setContentView(R.layout.audio_player_activity);
        sourceFilename = getIntent().getStringExtra(FILENAME);
        filenameTextView = (TextView) findViewById(R.id.audio_player_text);
        filenameTextView.setText(sourceFilename) ;
        
        playButton = (Button) findViewById(R.id.audio_player_play);
        playButton.setOnClickListener(onClickPlay);
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                try {
                    String cacheFilename = copyToCache(sourceFilename);
                    load(cacheFilename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    protected String copyToCache(String filename) throws IOException {
        java.io.File tmp = new java.io.File( filename );
        java.io.File outFile = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), tmp.getName());
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
        FileInputStream fis = new FileInputStream(new File(filename));
        
        byte[] buffer = new byte[2048];
        int length;
        while ((length = fis.read(buffer)) > 0){
            fos.write(buffer, 0, length);
        }

        // close
        fos.flush();
        fos.close();
        fis.close();
        return outFile.getAbsolutePath();
    }

    private void load( String filename ) {
        refreshUi();
        soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                play();
            }
        }); 
        java.io.File file = new java.io.File(filename);
        soundID = soundPool.load(file.getAbsolutePath(),0);
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
        soundPool.play(soundID, 1, 1, 1, 0, 1);
        refreshUi();
    }
    
    private void pause() {
        playState = false;
        soundPool.stop(soundID);
        refreshUi();
    }

    private void refreshUi() {
        if (playState) {
            playButton.setText("Pause");
        } else {
            playButton.setText("Play");
        }
    }
    
}
