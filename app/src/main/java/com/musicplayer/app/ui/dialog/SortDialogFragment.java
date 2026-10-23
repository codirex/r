package com.musicplayer.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.musicplayer.app.R;

/**
 * Dynamic sort dialog: the option set is chosen by content type passed in
 * {@link #ARG_CONTENT_TYPE} (tracks / albums / artists / genres / playlists),
 * because the sorts available for tracks differ from albums and so on.
 *
 * Result is delivered to the target fragment as a resultCode via
 * {@link #ARG_SORT_KEY} in the arguments bundle consumed with
 * OnFragmentResultListener-style callbacks.
 */
public class SortDialogFragment extends DialogFragment {

    public static final String ARG_CONTENT_TYPE = "content_type";
    public static final String TYPE_TRACKS = "tracks";
    public static final String TYPE_ALBUMS = "albums";
    public static final String TYPE_ARTISTS = "artists";
    public static final String TYPE_GENRES = "genres";
    public static final String TYPE_PLAYLISTS = "playlists";

    public static final String RESULT_SORT_KEY = "sort_key";
    public static final String RESULT_DESCENDING = "descending";

    public interface OnSortChosen {
        void onSortChosen(String sortKey, boolean descending);
    }

    private String contentType = TYPE_TRACKS;

    public static SortDialogFragment newInstance(String contentType) {
        SortDialogFragment fragment = new SortDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT_TYPE, contentType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            contentType = getArguments().getString(ARG_CONTENT_TYPE, TYPE_TRACKS);
        }

        RadioGroup group = view.findViewById(R.id.radioGroupSorts);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);

        populateSorts(group, contentType);

        btnCancel.setOnClickListener(v -> dismiss());

        btnApply.setOnClickListener(v -> {
            int checkedId = group.getCheckedRadioButtonId();
            String sortKey = keyFor(checkedId);
            boolean descending = view.findViewById(R.id.switchDescending) != null
                    && ((com.google.android.material.materialswitch.MaterialSwitch)
                    view.findViewById(R.id.switchDescending)).isChecked();
            if (getParentFragment() instanceof OnSortChosen) {
                ((OnSortChosen) getParentFragment()).onSortChosen(sortKey, descending);
            }
            if (getActivity() instanceof OnSortChosen) {
                ((OnSortChosen) getActivity()).onSortChosen(sortKey, descending);
            }
            dismiss();
        });
    }

    /** Builds the option list dynamically: tracks differ from albums etc. */
    private void populateSorts(RadioGroup group, String type) {
        int[] options;
        switch (type) {
            case TYPE_ALBUMS:
                options = new int[]{
                        R.id.sortName, R.id.sortArtist, R.id.sortYear,
                        R.id.sortTracks, R.id.sortRecentlyAdded};
                break;
            case TYPE_ARTISTS:
                options = new int[]{
                        R.id.sortName, R.id.sortTracks, R.id.sortRecentlyAdded};
                break;
            case TYPE_GENRES:
                options = new int[]{R.id.sortName, R.id.sortTracks};
                break;
            case TYPE_PLAYLISTS:
                options = new int[]{
                        R.id.sortName, R.id.sortRecentlyCreated, R.id.sortTracks};
                break;
            case TYPE_TRACKS:
            default:
                options = new int[]{
                        R.id.sortName, R.id.sortArtist, R.id.sortAlbum,
                        R.id.sortDuration, R.id.sortRecentlyAdded,
                        R.id.sortMostPlayed, R.id.sortYear};
                break;
        }

        // Hide options that do not apply to this content type.
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            boolean visible = false;
            for (int option : options) {
                if (child.getId() == option) {
                    visible = true;
                    break;
                }
            }
            child.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        if (group.getCheckedRadioButtonId() == -1 && group.getChildCount() > 0) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    group.check(child.getId());
                    break;
                }
            }
        }
    }

    private String keyFor(int checkedId) {
        if (checkedId == R.id.sortName) return "name";
        if (checkedId == R.id.sortArtist) return "artist";
        if (checkedId == R.id.sortAlbum) return "album";
        if (checkedId == R.id.sortDuration) return "duration";
        if (checkedId == R.id.sortRecentlyAdded) return "recent";
        if (checkedId == R.id.sortMostPlayed) return "played";
        if (checkedId == R.id.sortYear) return "year";
        if (checkedId == R.id.sortTracks) return "tracks";
        if (checkedId == R.id.sortRecentlyCreated) return "created";
        return "name";
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
