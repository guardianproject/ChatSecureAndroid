package info.guardianproject.otr.app.im.ui;

import java.io.IOException;

import info.guardianproject.iocipher.File;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.util.HttpMediaStreamer;
import android.app.Activity;
import android.content.Context;
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

        // TODO; we will make this an invisible activity for now

        /**
        setContentView(R.layout.audio_player_activity);
        filenameTextView = (TextView) findViewById(R.id.audio_player_text);
        filenameTextView.setText(new File(filename).getName());

        playButton = (Button) findViewById(R.id.audio_player_play);
        playButton.setOnClickListener(onClickPlay);
        */

        play();
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

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (streamer != null) {
            streamer.destroy();
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
        /**
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
        }*/
    }

    private MediaPlayer mediaPlayer;
    private HttpMediaStreamer streamer;

    private void killPlayer() {
        if (streamer != null) {
            streamer.destroy();
        }

        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;

        refreshUi();
    }

    private void initPlayer(String filename, String mimeType) throws Exception {
        streamer = new HttpMediaStreamer(filename, mimeType);
        Uri uri = streamer.getUri();

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
                finish();
            }
        });

        mediaPlayer.setDataSource(this, uri);
        mediaPlayer.prepare();
    }


    public static void playOnce(Context context, String filename, String mimeType) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {

        final HttpMediaStreamer streamer = new HttpMediaStreamer(filename, mimeType);
        Uri uri = streamer.getUri();

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.stop();
                mp.release();
                mp = null;
                streamer.destroy();
            }
        });

        mediaPlayer.setDataSource(context, uri);
        mediaPlayer.prepare();
    }
}
