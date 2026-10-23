package com.musicplayer.app.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.musicplayer.app.data.dao.PlaylistDao;
import com.musicplayer.app.data.dao.SongDao;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.PlaylistSong;
import com.musicplayer.app.data.model.Song;

@Database(entities = {Song.class, Playlist.class, PlaylistSong.class}, version = 1, exportSchema = false)
public abstract class MusicDatabase extends RoomDatabase {
    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
}
