package org.tensorflow.lite.examples.soundclassifier;

import android.content.Context;
import android.media.MediaPlayer;

public class PlayNotification {
    public static void playSound(Context context){
        MediaPlayer mediaPlayer= MediaPlayer.create(context,R.raw.notification);
        mediaPlayer.setVolume(1, 1);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.reset();
            mp.release();
        });
    }
}
