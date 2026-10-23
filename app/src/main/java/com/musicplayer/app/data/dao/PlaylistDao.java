package com.musicplayer.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.PlaylistSong;
import com.musicplayer.app.data.model.Song;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(Playlist playlist);

    @Update
    void updatePlaylist(Playlist playlist);

    @Delete
    void deletePlaylist(Playlist playlist);

    @Query("SELECT * FROM playlists WHERE isSystemPlaylist = 0 ORDER BY createdAt DESC")
    LiveData<List<Playlist>> getUserPlaylists();

    @Query("SELECT * FROM playlists ORDER BY isSystemPlaylist DESC, createdAt ASC")
    LiveData<List<Playlist>> getAllPlaylists();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addSongToPlaylist(PlaylistSong playlistSong);

    @Delete
    void removeSongFromPlaylist(PlaylistSong playlistSong);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCount(long playlistId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_songs ON songs.id = playlist_songs.songId WHERE playlist_songs.playlistId = :playlistId ORDER BY playlist_songs.position ASC")
    LiveData<List<Song>> getSongsInPlaylist(long playlistId);

    /** Synchronous twin used to start playback from a background thread. */
    @Query("SELECT songs.* FROM songs INNER JOIN playlist_songs ON songs.id = playlist_songs.songId WHERE playlist_songs.playlistId = :playlistId ORDER BY playlist_songs.position ASC")
    List<Song> getSongsInPlaylistSync(long playlistId);

    @Query("SELECT * FROM playlists WHERE name = 'All Songs'")
    Playlist getAllSongsPlaylist();

    @Query("SELECT * FROM playlists WHERE name = 'Favourites'")
    Playlist getFavouritesPlaylist();

    @Query("SELECT * FROM playlists WHERE name = 'Most Played'")
    Playlist getMostPlayedPlaylist();
}
