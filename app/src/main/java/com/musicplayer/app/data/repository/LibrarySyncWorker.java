package com.musicplayer.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import javax.inject.Inject;

import dagger.hilt.android.EntryPointAccessors;

import com.musicplayer.app.di.SyncEntryPoint;

/**
 * WorkManager worker that re-imports MediaStore into Room when external changes
 * are detected (new downloads, edits, deletions, metadata changes).
 */
public class LibrarySyncWorker extends Worker {

    @Inject
    public LibrarySyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SyncEntryPoint entryPoint = EntryPointAccessors.fromApplication(
                    getApplicationContext(), SyncEntryPoint.class);
            entryPoint.musicRepository().syncFromMediaStore();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
