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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.ui.main.adapter.GridItemAdapter;
import com.musicplayer.app.ui.player.PlayerViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/** Genres tab: grid of genres, tapping a genre plays its tracks. */
@AndroidEntryPoint
public class GenresFragment extends Fragment {

    private RecyclerView recyclerView;
    private GridItemAdapter adapter;
    private PlayerViewModel viewModel;
    private List<Song> songs = new ArrayList<>();
    private String sortKey = "name";
    private boolean descending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_genres, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        adapter = new GridItemAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnGridItemClickListener(new GridItemAdapter.OnGridItemClickListener() {
            @Override
            public void onClick(GridItemAdapter.GridItem item, int position) {
                List<Song> genreSongs = songsFor(item.title);
                if (!genreSongs.isEmpty()) {
                    viewModel.playSongs(genreSongs, 0);
                }
            }

            @Override
            public void onLongClick(GridItemAdapter.GridItem item, int position) {
                Toast.makeText(getContext(), item.title, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getAllSongs().observe(getViewLifecycleOwner(), allSongs -> {
            songs = allSongs == null ? new ArrayList<>() : allSongs;
            rebuild();
        });

        // Rebuild when the shared search query changes.
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> rebuild());

        // Re-sort whenever the shared sort choice changes.
        viewModel.getSortSpec().observe(getViewLifecycleOwner(), spec -> {
            sortKey = spec.key;
            descending = spec.descending;
            rebuild();
        });

        return view;
    }

    private void rebuild() {
        String query = viewModel.getSearchQuery().getValue();
        List<Song> visible = new ArrayList<>();
        for (Song song : songs) {
            if (PlayerViewModel.matches(query, song.getGenre())
                    || PlayerViewModel.matches(query, song.getTitle())) {
                visible.add(song);
            }
        }
        adapter.setItems(buildGenres(visible));
    }

    private List<GridItemAdapter.GridItem> buildGenres(List<Song> all) {
        Map<String, List<Song>> byGenre = new LinkedHashMap<>();
        for (Song song : all) {
            String genre = song.getGenre() == null || song.getGenre().isEmpty()
                    ? "Unknown" : song.getGenre();
            List<Song> bucket = byGenre.get(genre);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byGenre.put(genre, bucket);
            }
            bucket.add(song);
        }

        List<Map.Entry<String, List<Song>>> entries = new ArrayList<>(byGenre.entrySet());
        entries.sort(genreComparator());

        List<GridItemAdapter.GridItem> result = new ArrayList<>();
        long id = 0;
        for (Map.Entry<String, List<Song>> entry : entries) {
            result.add(new GridItemAdapter.GridItem(id++, entry.getKey(),
                    entry.getValue().size() + " tracks", null, R.drawable.bg_album_placeholder));
        }
        return result;
    }

    /** Sorts genre groups by the current shared sort spec. */
    private Comparator<Map.Entry<String, List<Song>>> genreComparator() {
        Comparator<Map.Entry<String, List<Song>>> comparator;
        switch (sortKey) {
            case "tracks":
                comparator = Comparator.comparingInt(e -> e.getValue().size());
                break;
            case "recent":
                comparator = Comparator.comparingLong(this::newestDateAdded);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(e -> e.getKey(), String.CASE_INSENSITIVE_ORDER);
                break;
        }
        return descending ? comparator.reversed() : comparator;
    }

    private long newestDateAdded(Map.Entry<String, List<Song>> entry) {
        long newest = 0;
        for (Song song : entry.getValue()) {
            try {
                newest = Math.max(newest, Long.parseLong(song.getDateAdded()));
            } catch (NumberFormatException ignored) {
            }
        }
        return newest;
    }

    private List<Song> songsFor(String genre) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs) {
            String g = song.getGenre() == null || song.getGenre().isEmpty()
                    ? "Unknown" : song.getGenre();
            if (genre.equals(g)) result.add(song);
        }
        return result;
    }
}
