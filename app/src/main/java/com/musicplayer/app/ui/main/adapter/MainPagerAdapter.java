package com.musicplayer.app.ui.main.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.musicplayer.app.ui.main.fragment.AlbumsFragment;
import com.musicplayer.app.ui.main.fragment.ArtistsFragment;
import com.musicplayer.app.ui.main.fragment.GenresFragment;
import com.musicplayer.app.ui.main.fragment.PlaylistsFragment;
import com.musicplayer.app.ui.main.fragment.TracksFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TracksFragment();
            case 1:
                return new AlbumsFragment();
            case 2:
                return new ArtistsFragment();
            case 3:
                return new GenresFragment();
            case 4:
                return new PlaylistsFragment();
            default:
                return new TracksFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
