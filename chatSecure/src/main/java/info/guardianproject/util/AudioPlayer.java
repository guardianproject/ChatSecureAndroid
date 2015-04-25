package info.guardianproject.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.widget.Toast;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private Context mContext;
    private String mFileName;
    private String mMimeType;

    private MediaPlayer mediaPlayer;
    private HttpMediaStreamer streamer;

    public AudioPlayer(Context context, String fileName, String mimeType) {
        mContext = context.getApplicationContext();
        mFileName = fileName;
        mMimeType = mimeType;
    }

    public void play() {
        try {
            initPlayer();
        } catch (Exception e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void pause() {
        killPlayer();
    }

    private void killPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (streamer != null) {
            streamer.destroy();
            streamer = null;
        }
    }

    private void initPlayer() throws Exception {
        streamer = new HttpMediaStreamer(mFileName, mMimeType);
        Uri uri = streamer.getUri();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                killPlayer();
            }
        });

        mediaPlayer.setDataSource(mContext, uri);
        mediaPlayer.prepareAsync();
    }
}
