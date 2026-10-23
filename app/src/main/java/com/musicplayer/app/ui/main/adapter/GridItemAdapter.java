package com.musicplayer.app.ui.main.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.musicplayer.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid adapter used for Albums, Artists, Genres and Playlists.
 * Shows a shaped image (album artwork, prepared layer-list placeholder for artists,
 * or user-provided image) with name and subtitle underneath.
 */
public class GridItemAdapter extends RecyclerView.Adapter<GridItemAdapter.GridViewHolder> {

    public interface OnGridItemClickListener {
        void onClick(GridItem item, int position);
        void onLongClick(GridItem item, int position);
    }

    public static class GridItem {
        public final long id;
        public final String title;
        public final String subtitle;
        public final String imagePath; // null -> use placeholder
        public final int placeholderRes; // fallback drawable (layer-list)

        public GridItem(long id, String title, String subtitle, String imagePath, int placeholderRes) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.imagePath = imagePath;
            this.placeholderRes = placeholderRes;
        }
    }

    private final List<GridItem> items = new ArrayList<>();
    private OnGridItemClickListener listener;

    public void setOnGridItemClickListener(OnGridItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<GridItem> items) {
        this.items.clear();
        if (items != null) this.items.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivImage;
        private final TextView tvTitle;
        private final TextView tvSubtitle;

        GridViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }

        void bind(GridItem item, OnGridItemClickListener listener) {
            tvTitle.setText(item.title);
            tvSubtitle.setText(item.subtitle);

            if (item.imagePath != null) {
                com.bumptech.glide.Glide.with(ivImage.getContext())
                        .load(item.imagePath)
                        .placeholder(item.placeholderRes)
                        .error(item.placeholderRes)
                        .centerCrop()
                        .into(ivImage);
            } else {
                ivImage.setImageResource(item.placeholderRes);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item, getAdapterPosition());
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(item, getAdapterPosition());
                return listener != null;
            });
        }
    }
}
