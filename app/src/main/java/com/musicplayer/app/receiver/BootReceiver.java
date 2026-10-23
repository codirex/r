package com.musicplayer.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.musicplayer.app.data.repository.SyncScheduler;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Boot receiver: re-registers library sync and resumes playback configuration
 * after the device restarts so the app keeps working in the background.
 */
@AndroidEntryPoint
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MEDIA_MOUNTED.equals(action)
                || Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
            new SyncScheduler(context).schedulePeriodicSync();
            new SyncScheduler(context).syncNow();
        }
    }
}
