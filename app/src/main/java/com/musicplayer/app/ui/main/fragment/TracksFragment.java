package com.musicplayer.app.ui.main.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.PlaylistSong;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.MusicRepository;
import com.musicplayer.app.data.repository.SyncScheduler;
import com.musicplayer.app.ui.dialog.TrackDetailsSheetDialogFragment;
import com.musicplayer.app.ui.main.adapter.TrackAdapter;
import com.musicplayer.app.ui.player.PlayerViewModel;
import com.musicplayer.app.ui.playlist.CreatePlaylistDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Tracks tab: vertical list of songs. The playing row shows animated waves instead
 * of the normal layout; tapping a track plays it (tapping the playing track pauses
 * it, tapping again resumes); long press opens the details bottom sheet.
 */
@AndroidEntryPoint
public class TracksFragment extends Fragment
        implements CreatePlaylistDialogFragment.OnPlaylistCreated {

    @Inject
    SyncScheduler syncScheduler;

    @Inject
    MusicRepository repository;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TrackAdapter adapter;
    private PlayerViewModel viewModel;

    private LinearLayout selectionBar;
    private TextView tvSelectionCount;
    private OnBackPressedCallback selectionBackPressed;
    private List<Song> pendingPlaylistSongs = new ArrayList<>();

    private List<Song> allSongs = new ArrayList<>();
    private List<Song> visibleSongs = new ArrayList<>();
    private String sortKey = "name";
    private boolean descending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracks, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        adapter = new TrackAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnTrackClickListener(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(Song song, int position) {
                Song current = viewModel.getCurrentSong().getValue();
                if (current != null && current.getId() == song.getId()) {
                    // Pressing the playing track toggles pause/resume.
                    viewModel.togglePlayPause();
                } else {
                    List<Song> playable = new ArrayList<>(allSongs);
                    int index = indexOf(playable, song);
                    viewModel.playSongs(playable, Math.max(0, index));
                }
            }

            @Override
            public void onTrackLongClick(Song song, int position) {
                TrackDetailsSheetDialogFragment.newInstance(song)
                        .show(getChildFragmentManager(), "details");
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            syncScheduler.syncNow();
            swipeRefresh.setRefreshing(false);
        });

        // ---- Selection mode (entered from the details sheet's "Select") ----
        selectionBar = view.findViewById(R.id.selectionBar);
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount);
        view.findViewById(R.id.btnSelectionClose).setOnClickListener(v -> exitSelection());
        view.findViewById(R.id.btnSelectionAll).setOnClickListener(v -> adapter.selectAll());
        view.findViewById(R.id.btnSelectionAdd).setOnClickListener(v -> addToPlaylistSelected());
        view.findViewById(R.id.btnSelectionShare).setOnClickListener(v -> shareSelected());
        view.findViewById(R.id.btnSelectionDelete).setOnClickListener(v -> deleteSelected());

        adapter.setOnSelectionChangedListener(count -> {
            if (tvSelectionCount != null) {
                tvSelectionCount.setText(getString(R.string.selected_count, count));
            }
        });

        // Back exits selection mode instead of closing the app.
        selectionBackPressed = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                exitSelection();
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), selectionBackPressed);

        // One-shot request from the track details sheet.
        viewModel.getSelectRequest().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                enterSelection(song);
                viewModel.clearSelectRequest();
            }
        });

        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song ->
                adapter.setCurrentPlayingId(song == null ? -1 : song.getId()));

        viewModel.getAllSongs().observe(getViewLifecycleOwner(), songs -> {
            allSongs = songs == null ? new ArrayList<>() : songs;
            applySort();
        });

        // Re-filter whenever the search text changes.
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> applySort());

        // Re-sort whenever the shared sort choice changes.
        viewModel.getSortSpec().observe(getViewLifecycleOwner(), spec -> {
            sortKey = spec.key;
            descending = spec.descending;
            applySort();
        });

        return view;
    }

    private void applySort() {
        // Filter by the shared search query (title / artist / album / genre).
        String query = viewModel.getSearchQuery().getValue();
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (PlayerViewModel.matches(query, song.getTitle())
                    || PlayerViewModel.matches(query, song.getArtist())
                    || PlayerViewModel.matches(query, song.getAlbum())
                    || PlayerViewModel.matches(query, song.getGenre())) {
                filtered.add(song);
            }
        }
        List<Song> sorted = new ArrayList<>(filtered);
        Comparator<Song> comparator;
        switch (sortKey) {
            case "artist":
                comparator = Comparator.comparing(s -> nullToEmpty(s.getArtist()), String.CASE_INSENSITIVE_ORDER);
                break;
            case "album":
                comparator = Comparator.comparing(s -> nullToEmpty(s.getAlbum()), String.CASE_INSENSITIVE_ORDER);
                break;
            case "duration":
                comparator = Comparator.comparingLong(Song::getDuration);
                break;
            case "recent":
                comparator = Comparator.comparingLong(s -> parseLong(s.getDateAdded()));
                break;
            case "played":
                comparator = Comparator.comparingInt(Song::getPlayCount);
                break;
            case "year":
                comparator = Comparator.comparing(s -> nullToEmpty(s.getYear()), String.CASE_INSENSITIVE_ORDER);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(s -> nullToEmpty(s.getTitle()), String.CASE_INSENSITIVE_ORDER);
                break;
        }
        if (descending) {
            comparator = comparator.reversed();
        }
        Collections.sort(sorted, comparator);
        visibleSongs = sorted;
        adapter.setSongs(sorted);
    }

    // ---- Selection mode ----

    private void enterSelection(Song song) {
        if (!adapter.isSelectionMode()) {
            adapter.setSelectionMode(true);
            selectionBar.setVisibility(View.VISIBLE);
            selectionBackPressed.setEnabled(true);
        }
        adapter.setSelected(song.getId());
        if (recyclerView != null) {
            int index = indexOf(visibleSongs, song);
            if (index >= 0) recyclerView.scrollToPosition(index);
        }
    }

    private void exitSelection() {
        if (!adapter.isSelectionMode()) return;
        adapter.setSelectionMode(false);
        selectionBar.setVisibility(View.GONE);
        selectionBackPressed.setEnabled(false);
    }

    private void addToPlaylistSelected() {
        pendingPlaylistSongs = adapter.getSelectedSongs();
        if (pendingPlaylistSongs.isEmpty()) {
            Toast.makeText(getContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        CreatePlaylistDialogFragment.newInstance()
                .show(getChildFragmentManager(), "add_selection");
    }

    private void shareSelected() {
        List<Song> selected = adapter.getSelectedSongs();
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            for (Song song : selected) {
                File file = new File(song.getPath());
                if (!file.exists()) continue;
                uris.add(FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", file));
            }
            if (uris.isEmpty()) {
                Toast.makeText(getContext(), R.string.action_share, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
            share.setType("audio/*");
            share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.action_share)));
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.action_share, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelected() {
        final List<Song> selected = adapter.getSelectedSongs();
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        final ContentResolver resolver = requireContext().getContentResolver();
        new Thread(() -> {
            for (Song song : selected) {
                try {
                    resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Audio.Media._ID + "=?",
                            new String[]{String.valueOf(song.getId())});
                } catch (Exception ignored) {
                    // Removal keeps going; the sync observer reconciles Room.
                }
            }
        }).start();
        exitSelection();
        Toast.makeText(getContext(), R.string.action_delete, Toast.LENGTH_SHORT).show();
    }

    /** Adds every selected track to the playlist that was just created. */
    @Override
    public void onPlaylistCreated(Playlist playlist) {
        final List<Song> toAdd = pendingPlaylistSongs;
        pendingPlaylistSongs = new ArrayList<>();
        if (toAdd.isEmpty()) return;

        new Thread(() -> {
            int position = repository.playlists().getSongCount(playlist.getId());
            for (Song song : toAdd) {
                repository.playlists().addSongToPlaylist(
                        new PlaylistSong(playlist.getId(), song.getId(), position++));
            }
        }).start();

        exitSelection();
        Toast.makeText(getContext(), R.string.action_add_to_playlist, Toast.LENGTH_SHORT).show();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int indexOf(List<Song> songs, Song song) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == song.getId()) return i;
        }
        return -1;
    }
}
