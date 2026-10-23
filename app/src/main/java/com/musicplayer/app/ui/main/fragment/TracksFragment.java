package com.musicplayer.app.ui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.SyncScheduler;
import com.musicplayer.app.ui.dialog.SortDialogFragment;
import com.musicplayer.app.ui.dialog.TrackDetailsSheetDialogFragment;
import com.musicplayer.app.ui.main.adapter.TrackAdapter;
import com.musicplayer.app.ui.player.PlayerViewModel;

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
        implements SortDialogFragment.OnSortChosen {

    @Inject
    SyncScheduler syncScheduler;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TrackAdapter adapter;
    private PlayerViewModel viewModel;

    private List<Song> allSongs = new ArrayList<>();
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

        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song ->
                adapter.setCurrentPlayingId(song == null ? -1 : song.getId()));

        viewModel.getAllSongs().observe(getViewLifecycleOwner(), songs -> {
            allSongs = songs == null ? new ArrayList<>() : songs;
            applySort();
        });

        // Re-filter whenever the search text changes.
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> applySort());

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
        adapter.setSongs(sorted);
    }

    @Override
    public void onSortChosen(String sortKey, boolean descending) {
        this.sortKey = sortKey;
        this.descending = descending;
        applySort();
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
