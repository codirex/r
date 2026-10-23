package com.musicplayer.app.service;

import static androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM;
import static androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;
import com.musicplayer.app.R;
import com.musicplayer.app.ui.player.FullPlayerActivity;

/**
 * Custom designed notification player for the MediaSessionService.
 *
 * Uses RemoteViews layouts styled after the app's identity (rounded artwork, palette
 * color accents, play/pause, previous/next). Note: on Android 13+ (API 33) the system
 * media player card is populated from the session metadata itself, so good
 * {@link MediaMetadata} (title, artist, artworkUri) keeps the system card looking right,
 * while this custom provider styles the classic notification surface.
 */
@OptIn(markerClass = UnstableApi.class)
public class CustomNotificationProvider implements MediaNotification.Provider {

    private static final int NOTIFICATION_ID = 0x4D53; // 'MS'
    private static final String CHANNEL_ID = "music_playback";

    private final Context context;

    public CustomNotificationProvider(Context context) {
        this.context = context.getApplicationContext();
        ensureChannel();
    }

    @Override
    public MediaNotification createNotification(
            MediaSession mediaSession,
            ImmutableList<CommandButton> mediaButtonPreferences,
            MediaNotification.ActionFactory actionFactory,
            MediaNotification.Provider.Callback onNotificationChangedCallback) {

        Player player = mediaSession.getPlayer();
        MediaMetadata metadata = player.getMediaMetadata();
        boolean playing = player.isPlaying() || player.getPlayWhenReady();

        RemoteViews small = buildViews(R.layout.notification_custom, metadata, playing,
                mediaSession, actionFactory, player);
        RemoteViews big = buildViews(R.layout.notification_custom_expanded, metadata, playing,
                mediaSession, actionFactory, player);

        PendingIntent contentIntent = openAppIntent(mediaSession);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(small)
                .setCustomBigContentView(big)
                .setContentTitle(metadata.title)
                .setContentText(metadata.artist)
                .setContentIntent(contentIntent)
                .setOngoing(playing)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOnlyAlertOnce(true);

        // Load artwork asynchronously if metadata only carries a URI.
        if (metadata.artworkUri != null) {
            mediaSession.getBitmapLoader().loadBitmapFromMetadata(metadata)
                    .addListener(() -> {
                        try {
                            Bitmap bitmap = mediaSession.getBitmapLoader()
                                    .loadBitmapFromMetadata(metadata).get();
                            if (bitmap != null) {
                                small.setImageViewBitmap(R.id.artwork, bitmap);
                                big.setImageViewBitmap(R.id.artwork, bitmap);
                                builder.setCustomContentView(small);
                                builder.setCustomBigContentView(big);
                                onNotificationChangedCallback.onNotificationChanged(
                                        new MediaNotification(NOTIFICATION_ID, builder.build()));
                            }
                        } catch (Exception ignored) {
                        }
                    }, Runnable::run);
        }

        return new MediaNotification(NOTIFICATION_ID, builder.build());
    }

    private RemoteViews buildViews(int layoutId, MediaMetadata metadata, boolean playing,
                                   MediaSession session,
                                   MediaNotification.ActionFactory actionFactory,
                                   Player player) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setTextViewText(R.id.tvTitle,
                metadata.title == null ? "" : metadata.title);
        views.setTextViewText(R.id.tvArtist,
                metadata.artist == null ? "" : metadata.artist);

        if (metadata.artworkData != null) {
            Bitmap artwork = BitmapFactory.decodeByteArray(
                    metadata.artworkData, 0, metadata.artworkData.length);
            if (artwork != null) views.setImageViewBitmap(R.id.artwork, artwork);
        }

        NotificationCompat.Action playPause = actionFactory.createMediaAction(
                session,
                IconCompat.createWithResource(context,
                        playing ? R.drawable.ic_pause : R.drawable.ic_play),
                playing ? "Pause" : "Play",
                Player.COMMAND_PLAY_PAUSE);
        NotificationCompat.Action previous = actionFactory.createMediaAction(
                session,
                IconCompat.createWithResource(context, R.drawable.ic_previous),
                "Previous",
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM);
        NotificationCompat.Action next = actionFactory.createMediaAction(
                session,
                IconCompat.createWithResource(context, R.drawable.ic_next),
                "Next",
                COMMAND_SEEK_TO_NEXT_MEDIA_ITEM);

        views.setOnClickPendingIntent(R.id.btnPlayPause, playPause.actionIntent);
        views.setOnClickPendingIntent(R.id.btnPrevious, previous.actionIntent);
        views.setOnClickPendingIntent(R.id.btnNext, next.actionIntent);
        views.setOnClickPendingIntent(R.id.ivArtwork, contentIntentFor(session));

        return views;
    }

    private PendingIntent contentIntentFor(MediaSession session) {
        PendingIntent sessionActivity = session.getSessionActivity();
        if (sessionActivity != null) return sessionActivity;
        return openAppIntent(session);
    }

    private PendingIntent openAppIntent(MediaSession session) {
        Intent intent = new Intent(context, FullPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
        return false;
    }

    @Override
    public NotificationChannelInfo getNotificationChannelInfo() {
        return new NotificationChannelInfo(CHANNEL_ID,
                context.getString(R.string.notif_channel_name));
    }
}
