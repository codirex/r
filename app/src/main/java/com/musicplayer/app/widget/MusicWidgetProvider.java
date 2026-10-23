package com.musicplayer.app.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.musicplayer.app.R;
import com.musicplayer.app.ui.main.MainActivity;
import com.musicplayer.app.ui.player.FullPlayerActivity;

/**
 * Launcher widget keeping the app's identity: rounded artwork, accent play button,
 * material styling matching the theme. Controls forward taps to the app and sends
 * media button events for play/pause/next/previous.
 */
public class MusicWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context));
        }
    }

    private RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_music_player);

        views.setOnClickPendingIntent(R.id.widgetRoot, pendingActivity(context, 0,
                new Intent(context, FullPlayerActivity.class)));
        views.setOnClickPendingIntent(R.id.widgetArtwork, pendingActivity(context, 1,
                new Intent(context, FullPlayerActivity.class)));
        views.setOnClickPendingIntent(R.id.widgetPlayPause, pendingBroadcast(context, 2,
                mediaIntent(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)));
        views.setOnClickPendingIntent(R.id.widgetNext, pendingBroadcast(context, 3,
                mediaIntent(context, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)));
        views.setOnClickPendingIntent(R.id.widgetPrevious, pendingBroadcast(context, 4,
                mediaIntent(context, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)));
        views.setOnClickPendingIntent(R.id.widgetOpen, pendingActivity(context, 5,
                new Intent(context, MainActivity.class)));

        return views;
    }

    private Intent mediaIntent(Context context, int keyCode) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode));
        return intent;
    }

    private android.app.PendingIntent pendingActivity(Context context, int requestCode, Intent intent) {
        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
        return android.app.PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    private android.app.PendingIntent pendingBroadcast(Context context, int requestCode, Intent intent) {
        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
        return android.app.PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager am = AppWidgetManager.getInstance(context);
            int[] ids = am.getAppWidgetIds(new ComponentName(context, MusicWidgetProvider.class));
            onUpdate(context, am, ids);
        }
    }
}
