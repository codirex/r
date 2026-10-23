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
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Song song : all) {
            String genre = song.getGenre() == null || song.getGenre().isEmpty()
                    ? "Unknown" : song.getGenre();
            counts.merge(genre, 1, Integer::sum);
        }
        List<GridItemAdapter.GridItem> result = new ArrayList<>();
        long id = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            result.add(new GridItemAdapter.GridItem(id++, entry.getKey(),
                    entry.getValue() + " tracks", null, R.drawable.bg_album_placeholder));
        }
        return result;
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
