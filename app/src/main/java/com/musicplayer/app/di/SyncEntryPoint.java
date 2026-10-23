package com.musicplayer.app.di;

import com.musicplayer.app.data.repository.MusicRepository;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface SyncEntryPoint {
    MusicRepository musicRepository();
}
