package com.musicplayer.app.di;

import android.content.Context;

import androidx.room.Room;

import com.musicplayer.app.data.dao.PlaylistDao;
import com.musicplayer.app.data.dao.SongDao;
import com.musicplayer.app.data.database.MusicDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    @Provides
    @Singleton
    public MusicDatabase provideMusicDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, MusicDatabase.class, "music_database")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public SongDao provideSongDao(MusicDatabase database) {
        return database.songDao();
    }

    @Provides
    public PlaylistDao providePlaylistDao(MusicDatabase database) {
        return database.playlistDao();
    }
}
