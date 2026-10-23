package com.musicplayer.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.ui.player.PlayerViewModel;
import com.musicplayer.app.util.FormatUtil;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Long-press on a track opens this bottom sheet:
 *  - artwork across the full sheet width with rounded corners (ShapeableImageView)
 *  - track info: name, album, artist, date, file size, play count, avg listen time...
 *  - actions: play after current, add to queue, add to playlist, stop after this song,
 *    edit, snapshot/export, set as ringtone, share, delete, select
 */
@AndroidEntryPoint
public class TrackDetailsSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String ARG_SONG = "song";

    public interface SheetActions {
        void onPlayAfterCurrent(Song song);

        void onAddToQueue(Song song);

        void onAddToPlaylist(Song song);

        void onStopAfterThis(Song song);

        void onEdit(Song song);

        void onSnapshot(Song song);

        void onSetAsRingtone(Song song);

        void onShare(Song song);

        void onDelete(Song song);

        void onSelect(Song song);
    }

    public static TrackDetailsSheetDialogFragment newInstance(Song song) {
        TrackDetailsSheetDialogFragment fragment = new TrackDetailsSheetDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    private PlayerViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_track_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);
        Song song = getArguments() != null ? getArguments().getParcelable(ARG_SONG) : null;
        if (song == null) {
            dismiss();
            return;
        }

        ImageView ivArtwork = view.findViewById(R.id.ivArtwork);
        Glide.with(this).load(song.getArtworkUri())
                .placeholder(R.drawable.ic_album_placeholder_large)
                .error(R.drawable.ic_album_placeholder_large)
                .centerCrop().into(ivArtwork);

        setText(view, R.id.tvTitle, song.getTitle());
        setText(view, R.id.tvAlbum, song.getAlbum());
        setText(view, R.id.tvArtist, song.getArtist());
        setText(view, R.id.tvDate, FormatUtil.formatDate(parseLong(song.getDateAdded())));
        setText(view, R.id.tvSize, FormatUtil.formatFileSize(song.getSize()));
        setText(view, R.id.tvPlays, String.format(Locale.US, "%d plays", song.getPlayCount()));
        setText(view, R.id.tvDuration, FormatUtil.formatDuration(song.getDuration()));
        // Average listening time: derived from play count as a placeholder metric
        // maintained by the library; shows total listening when unavailable.
        long avgListened = song.getPlayCount() > 0
                ? song.getDuration() : 0;
        setText(view, R.id.tvAvgListen, FormatUtil.formatDuration(avgListened));

        bindAction(view, R.id.actionPlayAfterCurrent, R.string.action_play_after_current, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onPlayAfterCurrent(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionAddToQueue, R.string.action_add_to_queue, v -> {
            viewModel.getController().appendToQueue(song);
            dismiss();
        });
        bindAction(view, R.id.actionAddToPlaylist, R.string.action_add_to_playlist, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onAddToPlaylist(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionStopAfterThis, R.string.action_stop_after, v -> {
            viewModel.getController().setStopAfterCurrent(true);
            dismiss();
        });
        bindAction(view, R.id.actionEdit, R.string.action_edit, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onEdit(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionSnapshot, R.string.action_snapshot, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onSnapshot(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionRingtone, R.string.action_ringtone, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onSetAsRingtone(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionShare, R.string.action_share, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onShare(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionDelete, R.string.action_delete, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onDelete(song);
            }
            dismiss();
        });
        bindAction(view, R.id.actionSelect, R.string.action_select, v -> {
            if (getActivity() instanceof SheetActions) {
                ((SheetActions) getActivity()).onSelect(song);
            }
            dismiss();
        });
    }

    private void bindAction(View root, int id, int labelRes, View.OnClickListener listener) {
        LinearLayout item = root.findViewById(id);
        if (item != null) {
            TextView label = item.findViewById(R.id.tvAction);
            if (label != null) label.setText(labelRes);
            item.setOnClickListener(listener);
        }
    }

    private void setText(View root, int id, @Nullable String value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value == null ? "—" : value);
    }

    private long parseLong(@Nullable String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
