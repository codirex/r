package com.musicplayer.app.data.repository;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.musicplayer.app.data.dao.PlaylistDao;
import com.musicplayer.app.data.dao.SongDao;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Fetches music metadata from MediaStore/ContentResolver on first launch and stores
 * it in Room. After that, all app reads go through Room. The library is kept in sync
 * by {@link com.musicplayer.app.service.MediaStoreObserver} and
 * {@link com.musicplayer.app.data.repository.LibrarySyncWorker}.
 */
@Singleton
public class MusicRepository {

    private final SongDao songDao;
    private final PlaylistDao playlistDao;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final String[] DEFAULT_PLAYLISTS = {"All Songs", "Favourites", "Most Played"};

    @Inject
    public MusicRepository(Context context, SongDao songDao, PlaylistDao playlistDao) {
        this.context = context.getApplicationContext();
        this.songDao = songDao;
        this.playlistDao = playlistDao;
    }

    public SongDao songs() {
        return songDao;
    }

    public PlaylistDao playlists() {
        return playlistDao;
    }

    /** Runs an async import from MediaStore into Room (first launch + background sync). */
    public void syncFromMediaStore() {
        executor.execute(() -> {
            List<Song> scanned = queryMediaStore();
            songDao.insertAll(scanned);
            removeDeleted(scanned);
            seedDefaultPlaylists();
        });
    }

    private List<Song> queryMediaStore() {
        List<Song> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.DATE_MODIFIED,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.DATA}
                : projectionLegacy();

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = resolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor == null) return songs;
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            int yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            int genreCol = cursor.getColumnIndex("genre");
            int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = artistCol >= 0 ? cursor.getString(artistCol) : null;
                String album = albumCol >= 0 ? cursor.getString(albumCol) : null;
                String albumId = albumIdCol >= 0 ? String.valueOf(cursor.getLong(albumIdCol)) : null;
                long duration = durationCol >= 0 ? cursor.getLong(durationCol) : 0;
                long size = sizeCol >= 0 ? cursor.getLong(sizeCol) : 0;
                long dateAdded = dateAddedCol >= 0 ? cursor.getLong(dateAddedCol) * 1000 : 0;
                String year = yearCol >= 0 ? String.valueOf(cursor.getInt(yearCol)) : null;
                String genre = genreCol >= 0 ? cursor.getString(genreCol) : null;
                String path = dataCol >= 0 ? cursor.getString(dataCol) : null;

                String artwork = "content://media/external/audio/media/" + id + "/albumart";

                songs.add(new Song(id, title, artist, album, albumId, genre, path,
                        duration, size, String.valueOf(dateAdded), year,
                        false, 0, 0, System.currentTimeMillis(), artwork));
            }
        } catch (Exception ignored) {
            // Permission missing or MediaStore unavailable; sync will be retried by WorkManager.
        }
        return songs;
    }

    private String[] projectionLegacy() {
        return new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA};
    }

    /** Deletes rows from Room for tracks that no longer exist in MediaStore. */
    private void removeDeleted(List<Song> scanned) {
        List<Song> known = songDao.getAllSongsSync();
        for (Song old : known) {
            boolean stillExists = false;
            for (Song fresh : scanned) {
                if (fresh.getId() == old.getId()) {
                    stillExists = true;
                    break;
                }
            }
            if (!stillExists) {
                songDao.delete(old);
            }
        }
    }

    /** Creates the three non-deletable system playlists once, on first launch. */
    private void seedDefaultPlaylists() {
        if (playlistDao.getAllSongsPlaylist() == null) {
            playlistDao.insertPlaylist(new Playlist("All Songs", null, null, true));
        }
        if (playlistDao.getFavouritesPlaylist() == null) {
            playlistDao.insertPlaylist(new Playlist("Favourites", null, null, true));
        }
        if (playlistDao.getMostPlayedPlaylist() == null) {
            playlistDao.insertPlaylist(new Playlist("Most Played", null, null, true));
        }
    }

    /** Returns the MediaStore album art URI for a song id (empty string if unavailable). */
    public static Uri albumArtUri(long songId) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/media"), songId);
    }
}
