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

/**
 * Albums tab: grid layout. Album covers come from the track artwork when present,
 * otherwise a prepared layer-list placeholder is used (theme-adaptive).
 */
@AndroidEntryPoint
public class AlbumsFragment extends Fragment {

    private RecyclerView recyclerView;
    private GridItemAdapter adapter;
    private PlayerViewModel viewModel;
    private List<Song> songs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        adapter = new GridItemAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnGridItemClickListener(new GridItemAdapter.OnGridItemClickListener() {
            @Override
            public void onClick(GridItemAdapter.GridItem item, int position) {
                List<Song> albumSongs = songsFor(item.title);
                if (!albumSongs.isEmpty()) {
                    viewModel.playSongs(albumSongs, 0);
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
            if (PlayerViewModel.matches(query, song.getTitle())
                    || PlayerViewModel.matches(query, song.getAlbum())
                    || PlayerViewModel.matches(query, song.getArtist())) {
                visible.add(song);
            }
        }
        adapter.setItems(buildAlbums(visible));
    }

    private List<GridItemAdapter.GridItem> buildAlbums(List<Song> all) {
        Map<String, GridItemAdapter.GridItem> albums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Song song : all) {
            String album = song.getAlbum() == null ? "Unknown Album" : song.getAlbum();
            counts.merge(album, 1, Integer::sum);
            if (!albums.containsKey(album)) {
                String artwork = firstArtworkFor(all, album);
                albums.put(album, new GridItemAdapter.GridItem(
                        albums.size(), album, null, artwork, R.drawable.bg_album_placeholder));
            }
        }
        List<GridItemAdapter.GridItem> result = new ArrayList<>();
        for (Map.Entry<String, GridItemAdapter.GridItem> entry : albums.entrySet()) {
            GridItemAdapter.GridItem old = entry.getValue();
            result.add(new GridItemAdapter.GridItem(old.id, old.title,
                    counts.get(entry.getKey()) + " tracks", old.imagePath, old.placeholderRes));
        }
        return result;
    }

    private String firstArtworkFor(List<Song> all, String album) {
        for (Song song : all) {
            if (album.equals(song.getAlbum())) return song.getArtworkUri();
        }
        return null;
    }

    private List<Song> songsFor(String album) {
        List<Song> result = new ArrayList<>();
        for (Song song : songs) {
            if (album.equals(song.getAlbum())) result.add(song);
        }
        return result;
    }
}
