package com.musicplayer.app.service;

import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.musicplayer.app.equalizer.EqualizerManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Foreground Media3 service. Keeps playback alive when the app is closed or
 * swiped away from recents, hosts the MediaSession that drives both the custom
 * designed notification player and the optional system-styled player, and routes
 * audio through the equalizer.
 *
 * The custom vs system notification style is selected from settings
 * ("Player type: custom | system").
 */
@AndroidEntryPoint
public class MusicService extends MediaSessionService {

    @Inject
    ExoPlayer player;

    @Inject
    EqualizerManager equalizerManager;

    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        player.setAudioAttributes(audioAttributes, true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);

        // Attach the equalizer to this player's audio session.
        equalizerManager.attach(player.getAudioSessionId());

        // Custom designed notification player (or system-styled, from settings).
        boolean systemPlayerStyle = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("player_type_system", false);
        if (!systemPlayerStyle) {
            setMediaNotificationProvider(new CustomNotificationProvider(this));
        }

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntentToFullPlayer())
                .build();
    }

    private android.app.PendingIntent pendingIntentToFullPlayer() {
        Intent intent = new Intent(this, com.musicplayer.app.ui.player.FullPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
        return android.app.PendingIntent.getActivity(this, 0, intent, flags);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int startId, int flags) {
        return super.onStartCommand(intent, startId, flags);
    }

    @Override
    public void onDestroy() {
        equalizerManager.release();
        mediaSession.release();
        // The ExoPlayer instance is a process-level singleton shared with the UI,
        // so stop playback instead of releasing it: the service may be recreated
        // within the same process.
        player.stop();
        player.clearMediaItems();
        super.onDestroy();
    }
}
