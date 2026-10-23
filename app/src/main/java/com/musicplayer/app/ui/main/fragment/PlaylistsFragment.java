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
import com.musicplayer.app.data.repository.MusicRepository;
import com.musicplayer.app.ui.main.adapter.GridItemAdapter;
import com.musicplayer.app.ui.playlist.CreatePlaylistDialogFragment;

import java.util.ArrayList;
import java.util.List;

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
    private List<Playlist> playlists = new ArrayList<>();
    private List<Playlist> visiblePlaylists = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);

        adapter = new GridItemAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnGridItemClickListener(new GridItemAdapter.OnGridItemClickListener() {
            @Override
            public void onClick(GridItemAdapter.GridItem item, int position) {
                if (position >= 0 && position < visiblePlaylists.size()) {
                    Playlist playlist = visiblePlaylists.get(position);
                    Toast.makeText(getContext(), playlist.getName(), Toast.LENGTH_SHORT).show();
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

        // Rebuild when the shared search query changes.
        new ViewModelProvider(requireActivity())
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .getSearchQuery()
                .observe(getViewLifecycleOwner(), q -> rebuild());

        return view;
    }

    private void rebuild() {
        String query = new ViewModelProvider(requireActivity())
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .getSearchQuery().getValue();
        List<Playlist> visible = new ArrayList<>();
        for (Playlist playlist : playlists) {
            if (com.musicplayer.app.ui.player.PlayerViewModel.matches(query, playlist.getName())
                    || com.musicplayer.app.ui.player.PlayerViewModel.matches(query, playlist.getMood())) {
                visible.add(playlist);
            }
        }
        visiblePlaylists = visible;
        adapter.setItems(buildItems(visible));
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
