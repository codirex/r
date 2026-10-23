package com.musicplayer.app.ui.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.MusicRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Shared ViewModel driving both the miniplayer and the full player:
 * current song, play state, position ticks, queue control and library data.
 */
@HiltViewModel
public class PlayerViewModel extends ViewModel {

    private final PlaybackController controller;
    private final MusicRepository repository;

    /**
     * Sort choice shared by every tab: the sort dialog writes it once and each
     * fragment applies it to its own content type.
     */
    public static final class SortSpec {
        public final String key;
        public final boolean descending;

        public SortSpec(String key, boolean descending) {
            this.key = key == null ? "name" : key;
            this.descending = descending;
        }
    }

    private final MutableLiveData<Integer> stopAfterDialogState = new MutableLiveData<>();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<SortSpec> sortSpec =
            new MutableLiveData<>(new SortSpec("name", false));
    private final MutableLiveData<Song> selectRequest = new MutableLiveData<>();

    @Inject
    public PlayerViewModel(PlaybackController controller, MusicRepository repository) {
        this.controller = controller;
        this.repository = repository;
    }

    public LiveData<Song> getCurrentSong() {
        return controller.getCurrentSong();
    }

    public LiveData<Boolean> getIsPlaying() {
        return controller.getIsPlaying();
    }

    public LiveData<Long> getPositionMs() {
        return controller.getPositionMs();
    }

    public LiveData<Long> getDurationMs() {
        return controller.getDurationMs();
    }

    public PlaybackController getController() {
        return controller;
    }

    public void playSongs(List<Song> songs, int startIndex) {
        controller.setQueue(songs, startIndex);
    }

    public void togglePlayPause() {
        controller.togglePlayPause();
    }

    public void next() {
        controller.next();
    }

    public void previous() {
        controller.previous();
    }

    public void seekTo(long ms) {
        controller.seekTo(ms);
    }

    public void seekBy(long deltaMs) {
        controller.seekBy(deltaMs);
    }

    public void toggleShuffle() {
        controller.toggleShuffle();
    }

    public void cycleRepeat() {
        controller.cycleRepeatMode();
    }

    public LiveData<List<Song>> getAllSongs() {
        return repository.songs().getAllSongs();
    }

    public LiveData<List<Song>> getFavorites() {
        return repository.songs().getFavoriteSongs();
    }

    public LiveData<List<Song>> getMostPlayed() {
        return repository.songs().getMostPlayedSongs();
    }

    public void toggleFavorite(Song song) {
        new Thread(() -> repository.songs().updateFavorite(
                song.getId(), !song.isFavorite())).start();
    }

    public void tick() {
        controller.tick();
    }

    /** Shared search text: every tab observes and filters by it. */
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query == null ? "" : query);
    }

    /**
     * One-shot request from the track details sheet: the Tracks tab reacts by
     * entering selection mode with the requested song pre-checked.
     */
    public LiveData<Song> getSelectRequest() {
        return selectRequest;
    }

    public void requestSelect(Song song) {
        selectRequest.setValue(song);
    }

    public void clearSelectRequest() {
        selectRequest.setValue(null);
    }

    /** Shared sort choice: every tab observes and re-orders by it. */
    public LiveData<SortSpec> getSortSpec() {
        return sortSpec;
    }

    public void setSortSpec(String key, boolean descending) {
        SortSpec current = sortSpec.getValue();
        if (current != null && current.key.equals(key == null ? "name" : key)
                && current.descending == descending) {
            return;
        }
        sortSpec.setValue(new SortSpec(key, descending));
    }

    /** True when [value] matches [query] (case-insensitive, empty query matches all). */
    public static boolean matches(String query, String value) {
        if (query == null || query.isEmpty()) return true;
        if (value == null) return false;
        return value.toLowerCase(java.util.Locale.ROOT)
                .contains(query.toLowerCase(java.util.Locale.ROOT));
    }
}
