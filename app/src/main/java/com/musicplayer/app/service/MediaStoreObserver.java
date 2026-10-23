package com.musicplayer.app.service;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import com.musicplayer.app.data.repository.SyncScheduler;

/**
 * Watches MediaStore for external changes (song downloaded, edited, deleted,
 * metadata/tags changed) and schedules a Room resync through WorkManager.
 * Debounced so bursts of changes trigger a single sync.
 */
public class MediaStoreObserver extends ContentObserver {

    private static final long DEBOUNCE_MS = 5000;
    private final SyncScheduler scheduler;
    private final Runnable pendingSync;

    public MediaStoreObserver(Handler handler, SyncScheduler scheduler) {
        super(handler);
        this.scheduler = scheduler;
        this.pendingSync = scheduler::syncNow;
    }

    @Override
    public void onChange(boolean selfChange) {
        // Remove any pending sync and re-post: collapses change bursts.
        handler.removeCallbacks(pendingSync);
        handler.postDelayed(pendingSync, DEBOUNCE_MS);
    }

    /** Convenience helper: register the observer on the audio collection. */
    public static MediaStoreObserver register(Context context, SyncScheduler scheduler) {
        Handler handler = new Handler(Looper.getMainLooper());
        MediaStoreObserver observer = new MediaStoreObserver(handler, scheduler);
        context.getContentResolver().registerContentObserver(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, observer);
        return observer;
    }

    public void unregister(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }
}
