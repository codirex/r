package com.musicplayer.app.ui.main.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private long currentPlayingId = -1;
    private OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Song song, int position);
        void onTrackLongClick(Song song, int position);
    }

    public void setOnTrackClickListener(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public void setCurrentPlayingId(long id) {
        long oldId = this.currentPlayingId;
        this.currentPlayingId = id;
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() == oldId || songs.get(i).getId() == id) {
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView ivAlbumArt;
        private final ImageView ivPlayingIndicator;
        private final TextView tvTitle;
        private final TextView tvArtist;
        private final TextView tvDuration;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            ivPlayingIndicator = itemView.findViewById(R.id.ivPlayingIndicator);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        void bind(Song song, OnTrackClickListener listener) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            tvDuration.setText(FormatUtil.formatDuration(song.getDuration()));

            boolean isPlaying = song.getId() == currentPlayingId;
            if (isPlaying) {
                ivPlayingIndicator.setVisibility(View.VISIBLE);
                AnimationDrawable waves = (AnimationDrawable) ivPlayingIndicator.getBackground();
                if (waves != null && !waves.isRunning()) waves.start();
                cardView.setCardBackgroundColor(
                        itemView.getResources().getColor(R.color.row_selected));
            } else {
                ivPlayingIndicator.setVisibility(View.GONE);
                if (ivPlayingIndicator.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable waves = (AnimationDrawable) ivPlayingIndicator.getBackground();
                    if (waves.isRunning()) waves.stop();
                }
                cardView.setCardBackgroundColor(
                        itemView.getResources().getColor(R.color.row_normal));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTrackClick(song, getAdapterPosition());
                animateClick();
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onTrackLongClick(song, getAdapterPosition());
                return true;
            });
        }

        private void animateClick() {
            itemView.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                    .withEndAction(() -> itemView.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        }
    }
}
