package com.musicplayer.app.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.search.SearchBar;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.ui.dialog.EditTrackDialogFragment;
import com.musicplayer.app.ui.dialog.SortDialogFragment;
import com.musicplayer.app.ui.dialog.TrackDetailsSheetDialogFragment;
import com.musicplayer.app.ui.main.adapter.MainPagerAdapter;
import com.musicplayer.app.ui.player.FullPlayerActivity;
import com.musicplayer.app.ui.playlist.CreatePlaylistDialogFragment;
import com.musicplayer.app.ui.settings.SettingsActivity;

import java.io.File;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Hosts the search bar, sort button, ViewPager2 tabs and the miniplayer.
 * Also handles the actions offered by the track details bottom sheet.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity
        implements SortDialogFragment.OnSortChosen,
        TrackDetailsSheetDialogFragment.SheetActions,
        CreatePlaylistDialogFragment.OnPlaylistCreated {

    private SearchBar searchBar;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private MainPagerAdapter pagerAdapter;
    private Song pendingShareSong;
    private Song pendingPlaylistSong;

    private final ActivityResultLauncher<String> ringtonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingShareSong != null) {
                    applyRingtone(pendingShareSong);
                } else {
                    Toast.makeText(this, R.string.required, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupViewPager();
        setupBottomNavigation();
        setupMiniPlayer();
    }

    private void initViews() {
        searchBar = findViewById(R.id.searchBar);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupSearch();
        setupSortButton();
    }

    /** Binds the material SearchView to the SearchBar and feeds the shared query. */
    private void setupSearch() {
        com.google.android.material.search.SearchView searchView = findViewById(R.id.searchView);
        searchView.setupWithSearchBar(searchBar);
        searchView.addOnQueryTextListener(
                new com.google.android.material.search.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        viewModel().setSearchQuery(query);
                        searchView.hide();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        viewModel().setSearchQuery(query);
                        return true;
                    }
                });
    }

    private void setupSortButton() {
        findViewById(R.id.btnSort).setOnClickListener(v -> {
            String type = viewPager.getCurrentItem() == 1 ? SortDialogFragment.TYPE_ALBUMS
                    : viewPager.getCurrentItem() == 2 ? SortDialogFragment.TYPE_ARTISTS
                    : viewPager.getCurrentItem() == 3 ? SortDialogFragment.TYPE_GENRES
                    : viewPager.getCurrentItem() == 4 ? SortDialogFragment.TYPE_PLAYLISTS
                    : SortDialogFragment.TYPE_TRACKS;
            SortDialogFragment.newInstance(type)
                    .show(getSupportFragmentManager(), "sort");
        });
    }

    private com.musicplayer.app.ui.player.PlayerViewModel viewModel() {
        return new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class);
    }

    @javax.inject.Inject
    com.musicplayer.app.data.repository.MusicRepository repository;

    private void setupViewPager() {
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.nav_tracks);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.nav_albums);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.nav_artists);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.nav_genres);
                        break;
                    case 4:
                        bottomNavigationView.setSelectedItemId(R.id.nav_playlists);
                        break;
                }
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tracks) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_albums) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_artists) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_genres) {
                viewPager.setCurrentItem(3);
                return true;
            } else if (itemId == R.id.nav_playlists) {
                viewPager.setCurrentItem(4);
                return true;
            }
            return false;
        });
    }

    private void setupMiniPlayer() {
        findViewById(R.id.miniPlayer).setOnClickListener(v -> {
            startActivity(new Intent(this, FullPlayerActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            String type = viewPager.getCurrentItem() == 1 ? SortDialogFragment.TYPE_ALBUMS
                    : viewPager.getCurrentItem() == 2 ? SortDialogFragment.TYPE_ARTISTS
                    : viewPager.getCurrentItem() == 3 ? SortDialogFragment.TYPE_GENRES
                    : viewPager.getCurrentItem() == 4 ? SortDialogFragment.TYPE_PLAYLISTS
                    : SortDialogFragment.TYPE_TRACKS;
            SortDialogFragment.newInstance(type)
                    .show(getSupportFragmentManager(), "sort");
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSortChosen(String sortKey, boolean descending) {
        // One shared sort spec: every tab observes it and re-orders its own content.
        new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .setSortSpec(sortKey, descending);
    }

    // ---- TrackDetailsSheetDialogFragment.SheetActions ----

    @Override
    public void onPlayAfterCurrent(Song song) {
        new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .getController().addToNext(song);
        Toast.makeText(this, R.string.action_play_after_current, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddToQueue(Song song) {
        new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .getController().appendToQueue(song);
    }

    @Override
    public void onAddToPlaylist(Song song) {
        pendingPlaylistSong = song;
        // Pick an existing playlist or create a new one in the same dialog.
        CreatePlaylistDialogFragment.newInstanceForPick()
                .show(getSupportFragmentManager(), "add_to_playlist");
    }

    @Override
    public void onStopAfterThis(Song song) {
        new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .getController().setStopAfterCurrent(true);
    }

    @Override
    public void onEdit(Song song) {
        EditTrackDialogFragment.newInstance(song)
                .show(getSupportFragmentManager(), "edit");
    }

    @Override
    public void onSnapshot(Song song) {
        // Export a shareable copy of the track through the FileProvider.
        try {
            File src = new File(song.getPath());
            File out = new File(getCacheDir(), "snapshots");
            out.mkdirs();
            File copy = new File(out, song.getTitle() + "_" + src.getName());
            java.nio.file.Files.copy(src.toPath(), copy.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", copy);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.action_snapshot)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.action_snapshot, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSetAsRingtone(Song song) {
        pendingShareSong = song;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (android.provider.Settings.System.canWrite(this)) {
                applyRingtone(song);
            } else {
                ringtonePermissionLauncher.launch(android.Manifest.permission.WRITE_SETTINGS);
            }
        } else {
            applyRingtone(song);
        }
    }

    private void applyRingtone(Song song) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.TITLE, song.getTitle());
            values.put(android.provider.MediaStore.MediaColumns.IS_RINGTONE, 1);
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            getContentResolver().update(
                    android.Uri.withAppendedPath(uri, String.valueOf(song.getId())),
                    values, null, null);
            android.provider.Settings.System.putString(getContentResolver(),
                    android.provider.Settings.System.RINGTONE,
                    android.Uri.withAppendedPath(uri, String.valueOf(song.getId())).toString());
            Toast.makeText(this, R.string.action_ringtone, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.action_ringtone, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShare(Song song) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", new File(song.getPath()));
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.action_share)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.action_share, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(Song song) {
        try {
            getContentResolver().delete(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    android.provider.MediaStore.Audio.Media._ID + "=?",
                    new String[]{String.valueOf(song.getId())});
        } catch (Exception ignored) {
        }
        Toast.makeText(this, R.string.action_delete, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSelect(Song song) {
        // Jump to the Tracks tab and enter multi-select with this track pre-checked.
        bottomNavigationView.setSelectedItemId(R.id.nav_tracks);
        new androidx.lifecycle.ViewModelProvider(this)
                .get(com.musicplayer.app.ui.player.PlayerViewModel.class)
                .requestSelect(song);
    }

    @Override
    public void onPlaylistCreated(Playlist playlist) {
        Song toAdd = pendingPlaylistSong;
        pendingPlaylistSong = null;
        if (toAdd != null) {
            // Add the song that triggered "add to playlist" into the new/existing playlist.
            final Song song = toAdd;
            new Thread(() -> {
                int count = playlistRepository().playlists().getSongCount(playlist.getId());
                playlistRepository().playlists().addSongToPlaylist(
                        new com.musicplayer.app.data.model.PlaylistSong(
                                playlist.getId(), song.getId(), count));
            }).start();
        }
        Toast.makeText(this, playlist.getName(), Toast.LENGTH_SHORT).show();
    }

    private com.musicplayer.app.data.repository.MusicRepository playlistRepository() {
        return repository;
    }
}
