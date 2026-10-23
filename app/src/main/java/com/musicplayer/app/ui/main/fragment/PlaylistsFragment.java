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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.MusicRepository;
import com.musicplayer.app.ui.main.adapter.GridItemAdapter;
import com.musicplayer.app.ui.player.PlayerViewModel;
import com.musicplayer.app.ui.playlist.CreatePlaylistDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Playlists tab. On first launch Room contains the three non-deletable system
 * playlists (All Songs, Favourites, Most Played); the user can create more with
 * a cover image and an app-assigned mood. MediaStore playlists are deprecated,
 * so every playlist lives in Room.
 */
@AndroidEntryPoint
public class PlaylistsFragment extends Fragment
        implements CreatePlaylistDialogFragment.OnPlaylistCreated {

    @Inject
    MusicRepository repository;

    private RecyclerView recyclerView;
    private GridItemAdapter adapter;
    private PlayerViewModel viewModel;
    private List<Playlist> playlists = new ArrayList<>();
    private List<Playlist> visiblePlaylists = new ArrayList<>();
    private String sortKey = "name";
    private boolean descending = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        adapter = new GridItemAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnGridItemClickListener(new GridItemAdapter.OnGridItemClickListener() {
            @Override
            public void onClick(GridItemAdapter.GridItem item, int position) {
                if (position >= 0 && position < visiblePlaylists.size()) {
                    playPlaylist(visiblePlaylists.get(position));
                }
            }

            @Override
            public void onLongClick(GridItemAdapter.GridItem item, int position) {
                if (position >= 0 && position < visiblePlaylists.size()) {
                    Playlist playlist = visiblePlaylists.get(position);
                    if (!playlist.isSystemPlaylist()) {
                        CreatePlaylistDialogFragment.forEdit(playlist)
                                .show(getChildFragmentManager(), "edit_playlist");
                    } else {
                        Toast.makeText(getContext(), playlist.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fabCreate);
        if (fab != null) {
            fab.setOnClickListener(v -> CreatePlaylistDialogFragment.newInstance()
                    .show(getChildFragmentManager(), "create_playlist"));
        }

        repository.playlists().getAllPlaylists().observe(getViewLifecycleOwner(), all -> {
            playlists = all == null ? new ArrayList<>() : all;
            rebuild();
        });

        // Rebuild when the shared search query changes, and re-sort when the
        // shared sort choice changes.
        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), q -> rebuild());
        viewModel.getSortSpec().observe(getViewLifecycleOwner(), spec -> {
            sortKey = spec.key;
            descending = spec.descending;
            rebuild();
        });

        return view;
    }

    private void rebuild() {
        String query = viewModel.getSearchQuery().getValue();
        List<Playlist> visible = new ArrayList<>();
        for (Playlist playlist : playlists) {
            if (PlayerViewModel.matches(query, playlist.getName())
                    || PlayerViewModel.matches(query, playlist.getMood())) {
                visible.add(playlist);
            }
        }

        if ("tracks".equals(sortKey)) {
            // Track counts need a Room query: count off the main thread, then publish.
            final boolean orderDescending = descending;
            final List<Playlist> toSort = visible;
            final androidx.fragment.app.FragmentActivity activity = getActivity();
            new Thread(() -> {
                Map<Long, Integer> counts = new HashMap<>();
                for (Playlist playlist : toSort) {
                    counts.put(playlist.getId(),
                            repository.playlists().getSongCount(playlist.getId()));
                }
                if (activity == null) return;
                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    publish(sortedPlaylists(toSort, counts, orderDescending));
                });
            }).start();
        } else {
            publish(sortedPlaylists(visible, Collections.emptyMap(), descending));
        }
    }

    private void publish(List<Playlist> ordered) {
        visiblePlaylists = ordered;
        adapter.setItems(buildItems(ordered));
    }

    /** Applies the shared sort spec to the playlist list. */
    private List<Playlist> sortedPlaylists(List<Playlist> list,
                                           Map<Long, Integer> counts,
                                           boolean orderDescending) {
        List<Playlist> sorted = new ArrayList<>(list);
        Comparator<Playlist> comparator;
        switch (sortKey) {
            case "tracks":
                comparator = Comparator.comparingInt(p -> counts.getOrDefault(p.getId(), 0));
                break;
            case "created":
                comparator = Comparator.comparingLong(Playlist::getCreatedAt);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER);
                break;
        }
        if (orderDescending) comparator = comparator.reversed();
        sorted.sort(comparator);
        return sorted;
    }

    /** Tapping a playlist starts it: system playlists map to their backing query. */
    private void playPlaylist(Playlist playlist) {
        final String name = playlist.getName();
        final long playlistId = playlist.getId();
        final androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) return;

        new Thread(() -> {
            final List<Song> songs;
            if ("All Songs".equals(name)) {
                songs = repository.songs().getAllSongsSync();
            } else if ("Favourites".equals(name)) {
                songs = repository.songs().getFavoriteSongsSync();
            } else if ("Most Played".equals(name)) {
                songs = repository.songs().getMostPlayedSongsSync();
            } else {
                songs = repository.playlists().getSongsInPlaylistSync(playlistId);
            }
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                if (songs == null || songs.isEmpty()) {
                    Toast.makeText(getContext(), R.string.playlist_empty,
                            Toast.LENGTH_SHORT).show();
                } else {
                    viewModel.playSongs(songs, 0);
                }
            });
        }).start();
    }

    private List<GridItemAdapter.GridItem> buildItems(List<Playlist> list) {
        List<GridItemAdapter.GridItem> items = new ArrayList<>();
        for (Playlist playlist : list) {
            String subtitle = playlist.isSystemPlaylist()
                    ? "System" : (playlist.getMood() != null ? playlist.getMood() : "Custom");
            items.add(new GridItemAdapter.GridItem(playlist.getId(), playlist.getName(),
                    subtitle, playlist.getCoverImagePath(), R.drawable.bg_album_placeholder));
        }
        return items;
    }

    @Override
    public void onPlaylistCreated(Playlist playlist) {
        Toast.makeText(getContext(), playlist.getName(), Toast.LENGTH_SHORT).show();
    }
}
