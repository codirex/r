package com.musicplayer.app.ui.player;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.MusicRepository;
import com.musicplayer.app.equalizer.EqualizerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of truth for playback state shared by the UI (miniplayer,
 * full player, lists) and the service. Wraps the singleton ExoPlayer: queue
 * management, shuffle, repeat modes, "stop after N tracks/time" scheduling
 * and play-count tracking back into Room.
 */
@Singleton
public class PlaybackController {

    private final ExoPlayer player;
    private final MusicRepository repository;
    private final EqualizerManager equalizerManager;
    private final Context appContext;

    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Long> positionMs = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> durationMs = new MutableLiveData<>(0L);

    private final List<Song> queue = new ArrayList<>();
    private int queueIndex = -1;

    // "Shutdown after ..." state
    private int stopAfterTracks = -1;      // -1 = disabled
    private long stopAfterTimeMs = -1;     // -1 = disabled
    private int tracksPlayedSinceTimer = 0;
    private boolean stopAfterCurrent = false;

    /** Observer pumping position updates to UI while playing. */
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            syncCurrentFromQueue();
            if (stopAfterTracks > 0) {
                tracksPlayedSinceTimer++;
                if (tracksPlayedSinceTimer >= stopAfterTracks) {
                    stopAfterTracks = -1;
                    tracksPlayedSinceTimer = 0;
                    pause();
                    return;
                }
            }
            if (stopAfterCurrent) {
                stopAfterCurrent = false;
                pause();
            }
        }

        @Override
        public void onIsPlayingChanged(boolean playing) {
            isPlaying.setValue(playing);
            if (playing && queueIndex >= 0 && queueIndex < queue.size()) {
                Song song = queue.get(queueIndex);
                new Thread(() -> repository.songs().incrementPlayCount(
                        song.getId(), System.currentTimeMillis())).start();
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY) {
                durationMs.setValue(player.getDuration());
            }
        }
    };

    @Inject
    public PlaybackController(Context context, ExoPlayer player, MusicRepository repository,
                              EqualizerManager equalizerManager) {
        this.appContext = context.getApplicationContext();
        this.player = player;
        this.repository = repository;
        this.equalizerManager = equalizerManager;
        player.addListener(playerListener);
        equalizerManager.attach(player.getAudioSessionId());
    }

    // ---- Observation ----
    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Long> getPositionMs() {
        return positionMs;
    }

    public LiveData<Long> getDurationMs() {
        return durationMs;
    }

    public long getDuration() {
        return player.getDuration();
    }

    public long getPosition() {
        return player.getCurrentPosition();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    // ---- Queue ----
    public void setQueue(List<Song> songs, int startIndex) {
        queue.clear();
        queue.addAll(songs);
        queueIndex = startIndex;
        rebuildMediaItems();
        if (startIndex >= 0 && startIndex < queue.size()) {
            player.seekTo(startIndex, 0);
            play();
        }
    }

    public void addToQueue(Song song, boolean playNext) {
        int insertAt = playNext ? queueIndex + 1 : queue.size();
        queue.add(Math.max(0, Math.min(insertAt, queue.size())), song);
        rebuildMediaItems();
    }

    public void addToNext(Song song) {
        addToQueue(song, true);
    }

    public void appendToQueue(Song song) {
        addToQueue(song, false);
    }

    public List<Song> getQueue() {
        return Collections.unmodifiableList(queue);
    }

    private void rebuildMediaItems() {
        List<MediaItem> items = new ArrayList<>();
        for (Song song : queue) {
            items.add(toMediaItem(song));
        }
        long pos = player.getCurrentPosition();
        boolean wasPlaying = player.isPlaying();
        player.setMediaItems(items, Math.max(0, queueIndex), pos);
        if (wasPlaying) player.play();
    }

    private MediaItem toMediaItem(Song song) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtist(song.getArtist())
                .setAlbumTitle(song.getAlbum())
                .setArtworkUri(song.getArtworkUri() != null
                        ? Uri.parse(song.getArtworkUri()) : null)
                .build();
        return new MediaItem.Builder()
                .setUri(song.getPath())
                .setMediaId(String.valueOf(song.getId()))
                .setMediaMetadata(metadata)
                .build();
    }

    private void syncCurrentFromQueue() {
        int index = player.getCurrentMediaItemIndex();
        if (index >= 0 && index < queue.size()) {
            queueIndex = index;
            currentSong.setValue(queue.get(index));
            durationMs.setValue(player.getDuration());
        }
    }

    // ---- Transport ----
    public void play() {
        player.prepare();
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void togglePlayPause() {
        if (player.isPlaying()) pause(); else play();
    }

    public void next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem();
        syncCurrentFromQueue();
    }

    public void previous() {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem();
        else player.seekTo(0);
        syncCurrentFromQueue();
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
        this.positionMs.setValue(positionMs);
    }

    public void seekBy(long deltaMs) {
        long target = Math.max(0, Math.min(
                player.getDuration() > 0 ? player.getDuration() : Long.MAX_VALUE,
                player.getCurrentPosition() + deltaMs));
        seekTo(target);
    }

    public void toggleShuffle() {
        player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
    }

    public boolean isShuffleEnabled() {
        return player.getShuffleModeEnabled();
    }

    public void cycleRepeatMode() {
        int mode = player.getRepeatMode();
        if (mode == Player.REPEAT_MODE_OFF) player.setRepeatMode(Player.REPEAT_MODE_ALL);
        else if (mode == Player.REPEAT_MODE_ALL) player.setRepeatMode(Player.REPEAT_MODE_ONE);
        else player.setRepeatMode(Player.REPEAT_MODE_OFF);
    }

    public int getRepeatMode() {
        return player.getRepeatMode();
    }

    /** Tick called by UI/foreground loop to publish position and enforce stop-after-time. */
    public void tick() {
        if (player.isPlaying()) {
            positionMs.setValue(player.getCurrentPosition());
            if (stopAfterTimeMs > 0) {
                stopAfterTimeMs -= 1000;
                if (stopAfterTimeMs <= 0) {
                    stopAfterTimeMs = -1;
                    pause();
                }
            }
        }
    }

    // ---- Shutdown timer dialog results ----
    public void setStopAfterTracks(int n) {
        this.stopAfterTracks = n;
        this.tracksPlayedSinceTimer = 0;
    }

    public void setStopAfterTime(long totalMs) {
        this.stopAfterTimeMs = totalMs;
    }

    public void clearStopAfter() {
        stopAfterTracks = -1;
        stopAfterTimeMs = -1;
        stopAfterCurrent = false;
    }

    public void setStopAfterCurrent(boolean value) {
        this.stopAfterCurrent = value;
    }

    public boolean isStopAfterCurrent() {
        return stopAfterCurrent;
    }

    public long getRemainingStopAfterTimeMs() {
        return stopAfterTimeMs;
    }

    public int getRemainingStopAfterTracks() {
        return stopAfterTracks;
    }
}
