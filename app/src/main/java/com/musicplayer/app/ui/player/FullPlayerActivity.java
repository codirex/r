package com.musicplayer.app.ui.player;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.ui.dialog.ShutdownDialogFragment;
import com.musicplayer.app.ui.dialog.SleepTimerDialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Full player per the design spec:
 * - artwork 75% of width, from the very top, bottom corners rounded 50%
 * - title/album/artist floating over the artwork's lower area, colored by Palette
 * - inverted-arc slider under the artwork with counters at both ends
 * - control row: shuffle | previous | -10s | play/pause | +10s | next | repeat
 * - long-press play/pause opens the shutdown dialog fragment
 * - lyrics panel dragged up from inside the player with live highlighting
 */
@AndroidEntryPoint
public class FullPlayerActivity extends AppCompatActivity {

    private static final long LONG_TRACK_THRESHOLD_MS = 5 * 60_000;

    private PlayerViewModel viewModel;

    private MotionLayout motionRoot;
    private com.google.android.material.imageview.ShapeableImageView ivArtwork;
    private TextView tvTitle, tvAlbum, tvArtist;
    private ArcSliderView arcSlider;
    private MaterialButton btnShuffle, btnPrevious, btnBackward, btnPlayPause,
            btnForward, btnNext, btnRepeat;
    private ImageButton btnFavorite, btnSleepTimer, btnLyrics, btnEqualizer, btnQueue;
    private LyricsView lyricsView;

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            viewModel.tick();
            Song song = viewModel.getCurrentSong().getValue();
            if (song != null) {
                arcSlider.setProgress(viewModel.getController().getPosition(),
                        viewModel.getController().getDuration());
                lyricsView.updatePosition(viewModel.getController().getPosition());
            }
            ticker.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        viewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        initViews();
        setupControls();
        observePlayer();
        setupLyricsGesture();
    }

    private void initViews() {
        motionRoot = findViewById(R.id.motionRoot);
        ivArtwork = findViewById(R.id.ivArtwork);
        tvTitle = findViewById(R.id.tvTitle);
        tvAlbum = findViewById(R.id.tvAlbum);
        tvArtist = findViewById(R.id.tvArtist);
        arcSlider = findViewById(R.id.arcSlider);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnBackward = findViewById(R.id.btnBackward);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnForward = findViewById(R.id.btnForward);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnSleepTimer = findViewById(R.id.btnSleepTimer);
        btnLyrics = findViewById(R.id.btnLyrics);
        btnEqualizer = findViewById(R.id.btnEqualizer);
        btnQueue = findViewById(R.id.btnQueue);
        lyricsView = findViewById(R.id.lyricsView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCloseLyrics).setOnClickListener(v ->
                motionRoot.transitionToStart());
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());

        // Long press play/pause -> shutdown dialog fragment (n tracks / n time).
        btnPlayPause.setOnLongClickListener(v -> {
            new ShutdownDialogFragment().show(getSupportFragmentManager(), "shutdown");
            return true;
        });

        btnShuffle.setOnClickListener(v -> {
            viewModel.toggleShuffle();
            updateControlStates();
        });

        btnRepeat.setOnClickListener(v -> {
            viewModel.cycleRepeat();
            updateControlStates();
        });

        btnPrevious.setOnClickListener(v -> viewModel.previous());
        btnNext.setOnClickListener(v -> viewModel.next());
        btnBackward.setOnClickListener(v -> viewModel.seekBy(-10_000));
        btnForward.setOnClickListener(v -> viewModel.seekBy(10_000));

        btnFavorite.setOnClickListener(v -> {
            Song song = viewModel.getCurrentSong().getValue();
            if (song != null) {
                viewModel.toggleFavorite(song);
                updateFavoriteIcon(song.isFavorite());
            }
        });

        btnSleepTimer.setOnClickListener(v -> {
            new SleepTimerDialogFragment().show(getSupportFragmentManager(), "sleep");
        });

        btnLyrics.setOnClickListener(v -> {
            if (motionRoot.getProgress() < 0.5f) motionRoot.transitionToEnd();
            else motionRoot.transitionToStart();
        });

        btnEqualizer.setOnClickListener(v ->
                Toast.makeText(this, R.string.equalizer, Toast.LENGTH_SHORT).show());

        arcSlider.setOnSeekListener(new ArcSliderView.OnSeekListener() {
            @Override
            public void onSeek(long positionMs, long durationMs) {
                viewModel.seekTo(positionMs);
            }

            @Override
            public void onSeekStart() {
            }

            @Override
            public void onSeekEnd() {
            }
        });
    }

    private void observePlayer() {
        viewModel.getCurrentSong().observe(this, song -> {
            if (song == null) return;
            tvTitle.setText(song.getTitle());
            tvAlbum.setText(song.getAlbum() == null ? "" : song.getAlbum());
            tvArtist.setText(song.getArtist() == null ? "" : song.getArtist());
            updateFavoriteIcon(song.isFavorite());

            // Show +10/-10 only for long tracks.
            boolean longTrack = song.getDuration() >= LONG_TRACK_THRESHOLD_MS;
            btnBackward.setVisibility(longTrack ? View.VISIBLE : View.GONE);
            btnForward.setVisibility(longTrack ? View.VISIBLE : View.GONE);

            loadArtwork(song);
            loadLyrics(song);
        });

        viewModel.getIsPlaying().observe(this, playing -> {
            btnPlayPause.setIcon(getDrawable(playing ? R.drawable.ic_pause : R.drawable.ic_play));
        });

        viewModel.getDurationMs().observe(this, duration -> {
            arcSlider.setProgress(viewModel.getController().getPosition(),
                    duration == null ? 0 : duration);
        });
    }

    private void loadArtwork(Song song) {
        Glide.with(this)
                .asBitmap()
                .load(song.getArtworkUri())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_album_placeholder_large)
                .error(R.drawable.ic_album_placeholder_large)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        ivArtwork.setImageBitmap(resource);
                        applyPalette(resource);
                    }

                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {
                        ivArtwork.setImageDrawable(placeholder);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        ivArtwork.setImageDrawable(errorDrawable);
                        applyDefaultTheme();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        ivArtwork.setImageDrawable(placeholder);
                    }
                });
    }

    /** Extract colors from artwork and theme the player; fall back if unavailable. */
    private void applyPalette(Bitmap bitmap) {
        if (bitmap == null) {
            applyDefaultTheme();
            return;
        }
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                applyDefaultTheme();
                return;
            }
            int accent = palette.getVibrantColor(
                    palette.getDominantColor(getResources().getColor(R.color.accent, null)));
            int titleColor = palette.getLightVibrantColor(
                    getResources().getColor(R.color.on_background_default, null));
            int muted = palette.getMutedColor(
                    getResources().getColor(R.color.on_background_muted, null));
            int bg = palette.getDarkMutedColor(
                    getResources().getColor(R.color.full_player_bg, null));

            tvTitle.setTextColor(titleColor);
            tvAlbum.setTextColor(muted);
            tvArtist.setTextColor(muted);
            arcSlider.setAccentColor(accent);
            lyricsView.setAccentColor(accent);

            // Dark background derived from artwork for the whole player surface.
            int darkened = ColorUtils.blendARGB(bg, 0xFF000000, 0.35f);
            motionRoot.setBackgroundColor(darkened);
        });
    }

    private void applyDefaultTheme() {
        tvTitle.setTextColor(getResources().getColor(R.color.on_background_default, null));
        tvAlbum.setTextColor(getResources().getColor(R.color.on_background_muted, null));
        tvArtist.setTextColor(getResources().getColor(R.color.on_background_muted, null));
        int accent = getResources().getColor(R.color.accent, null);
        arcSlider.setAccentColor(accent);
        lyricsView.setAccentColor(accent);
        motionRoot.setBackgroundColor(getResources().getColor(R.color.full_player_bg, null));
    }

    private void updateFavoriteIcon(boolean favorite) {
        btnFavorite.setImageResource(favorite
                ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void updateControlStates() {
        btnShuffle.setIcon(getDrawable(viewModel.getController().isShuffleEnabled()
                ? R.drawable.ic_shuffle_active : R.drawable.ic_shuffle));
        int repeatMode = viewModel.getController().getRepeatMode();
        btnRepeat.setIcon(getDrawable(
                repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE
                        ? R.drawable.ic_repeat_one
                        : repeatMode == androidx.media3.common.Player.REPEAT_MODE_ALL
                        ? R.drawable.ic_repeat_active : R.drawable.ic_repeat));
    }

    private void setupLyricsGesture() {
        motionRoot.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.setDismissed) {
                    finish();
                    overridePendingTransition(0, android.R.anim.fade_out);
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
            }
        });
    }

    /**
     * Loads lyrics: tries an .lrc sidecar file next to the track first, then the
     * embedded tag lyrics are handled by the caller through setLyricsText.
     */
    private void loadLyrics(Song song) {
        if (song.getPath() == null) {
            lyricsView.setLyrics("");
            return;
        }
        new Thread(() -> {
            String raw = null;
            try {
                File lrc = new File(song.getPath().replaceFirst("\\.[^.]+$", ".lrc"));
                if (lrc.exists()) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(lrc))) {
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                    }
                    raw = sb.toString();
                }
            } catch (Exception ignored) {
            }
            String result = raw == null ? "" : raw;
            runOnUiThread(() -> lyricsView.setLyrics(result));
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ticker.post(tickRunnable);
        updateControlStates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tickRunnable);
    }
}
