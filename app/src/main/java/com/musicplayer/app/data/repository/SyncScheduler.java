package com.musicplayer.app.data.repository;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Entry points for scheduling library synchronization.
 *
 * - {@link #syncNow()} enqueues a one-off sync used by MediaStoreObserver whenever
 *   external changes happen (download / edit / delete / tag change).
 * - {@link #schedulePeriodicSync()} keeps a slow periodic safety net.
 */
@Singleton
public class SyncScheduler {

    public static final String WORK_NOW = "library_sync_now";
    public static final String WORK_PERIODIC = "library_sync_periodic";

    private final Context context;

    @Inject
    public SyncScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void syncNow() {
        WorkManager wm = WorkManager.getInstance(context);
        wm.enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(LibrarySyncWorker.class).build());
    }

    public void schedulePeriodicSync() {
        WorkManager wm = WorkManager.getInstance(context);
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                LibrarySyncWorker.class, 15, TimeUnit.MINUTES).build();
        wm.enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
