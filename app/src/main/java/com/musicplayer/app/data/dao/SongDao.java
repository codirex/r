package com.musicplayer.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.musicplayer.app.data.model.Song;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Song> songs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);

    @Update
    void update(Song song);

    @Delete
    void delete(Song song);

    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<Song> getAllSongsSync();

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    LiveData<List<Song>> getFavoriteSongs();

    /** Synchronous twin used when starting playback off the main thread. */
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    List<Song> getFavoriteSongsSync();

    @Query("SELECT * FROM songs ORDER BY playCount DESC, lastPlayed DESC LIMIT 100")
    LiveData<List<Song>> getMostPlayedSongs();

    /** Synchronous twin used when starting playback off the main thread. */
    @Query("SELECT * FROM songs ORDER BY playCount DESC, lastPlayed DESC LIMIT 100")
    List<Song> getMostPlayedSongsSync();

    @Query("SELECT DISTINCT artist FROM songs WHERE artist IS NOT NULL ORDER BY artist ASC")
    LiveData<List<String>> getAllArtists();

    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL ORDER BY album ASC")
    LiveData<List<String>> getAllAlbums();

    @Query("SELECT DISTINCT genre FROM songs WHERE genre IS NOT NULL ORDER BY genre ASC")
    LiveData<List<String>> getAllGenres();

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, title ASC")
    LiveData<List<Song>> getSongsByArtist(String artist);

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY title ASC")
    LiveData<List<Song>> getSongsByAlbum(String album);

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    LiveData<List<Song>> getSongsByGenre(String genre);

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    LiveData<List<Song>> searchSongs(String query);

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :time WHERE id = :songId")
    void incrementPlayCount(long songId, long time);

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    void updateFavorite(long songId, boolean isFavorite);
}
