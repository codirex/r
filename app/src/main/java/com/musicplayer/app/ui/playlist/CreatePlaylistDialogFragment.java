package com.musicplayer.app.ui.playlist;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Playlist;
import com.musicplayer.app.data.repository.MusicRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Creates a user playlist stored in Room (MediaStore playlists are deprecated).
 * The user gives the playlist a name, optionally picks a cover image, and the app
 * assigns a mood — the mood picker is an in-app feature shown as a chip group.
 */
@AndroidEntryPoint
public class CreatePlaylistDialogFragment extends DialogFragment {

    public static final String ARG_EDIT_PLAYLIST = "edit_playlist";
    public static final String ARG_PICK_EXISTING = "pick_existing";

    @Inject
    MusicRepository repository;

    private String coverUri;
    private Playlist editing;
    private Playlist selectedExistingPlaylist;

    public interface OnPlaylistCreated {
        void onPlaylistCreated(Playlist playlist);
    }

    public static CreatePlaylistDialogFragment newInstance() {
        return new CreatePlaylistDialogFragment();
    }

    /** Opened from "Add to playlist": also lists the existing playlists to pick from. */
    public static CreatePlaylistDialogFragment newInstanceForPick() {
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PICK_EXISTING, true);
        fragment.setArguments(args);
        return fragment;
    }

    public static CreatePlaylistDialogFragment forEdit(Playlist playlist) {
        CreatePlaylistDialogFragment fragment = new CreatePlaylistDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EDIT_PLAYLIST, playlist);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            editing = getArguments().getParcelable(ARG_EDIT_PLAYLIST);
        }

        EditText etName = view.findViewById(R.id.etName);
        ImageView ivCover = view.findViewById(R.id.ivCover);
        ChipGroup moodGroup = view.findViewById(R.id.moodGroup);
        MaterialButton btnPickImage = view.findViewById(R.id.btnPickImage);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        if (editing != null) {
            etName.setText(editing.getName());
            coverUri = editing.getCoverImagePath();
            if (coverUri != null) {
                Glide.with(this).load(coverUri).centerCrop().into(ivCover);
            }
        }

        btnPickImage.setOnClickListener(v -> pickImage.launch("image/*"));

        // "Add to playlist" mode: offer the existing playlists alongside creating one.
        if (editing == null && getArguments() != null
                && getArguments().getBoolean(ARG_PICK_EXISTING, false)) {
            setupExistingPlaylists(view);
        }

        btnSave.setOnClickListener(v -> {
            // Picking an existing playlist adds the track straight to it.
            if (editing == null && selectedExistingPlaylist != null) {
                notifyResult(selectedExistingPlaylist);
                dismiss();
                return;
            }

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError(getString(R.string.required));
                return;
            }

            int checkedId = moodGroup.getCheckedChipId();
            String mood = moodNameFor(checkedId);

            if (editing != null) {
                editing.setName(name);
                editing.setCoverImagePath(coverUri);
                editing.setMood(mood);
                new Thread(() -> repository.playlists().updatePlaylist(editing)).start();
                notifyResult(editing);
            } else {
                Playlist playlist = new Playlist(name, coverUri, mood, false);
                new Thread(() -> {
                    long id = repository.playlists().insertPlaylist(playlist);
                    playlist.setId(id);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> notifyResult(playlist));
                    }
                }).start();
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    /** Fills the "existing playlists" radio list when opened for "Add to playlist". */
    private void setupExistingPlaylists(View root) {
        final TextView label = root.findViewById(R.id.labelExisting);
        final RadioGroup group = root.findViewById(R.id.radioExistingPlaylists);

        repository.playlists().getUserPlaylists().observe(getViewLifecycleOwner(), list -> {
            if (!isAdded()) return;
            group.removeAllViews();
            selectedExistingPlaylist = null;

            boolean any = list != null && !list.isEmpty();
            label.setVisibility(any ? View.VISIBLE : View.GONE);
            group.setVisibility(any ? View.VISIBLE : View.GONE);
            if (!any) return;

            for (Playlist playlist : list) {
                MaterialRadioButton radio = new MaterialRadioButton(requireContext());
                radio.setText(playlist.getName());
                radio.setTag(playlist);
                group.addView(radio);
            }
            group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                View checked = radioGroup.findViewById(checkedId);
                selectedExistingPlaylist = checked != null && checked.getTag() instanceof Playlist
                        ? (Playlist) checked.getTag() : null;
            });
        });
    }

    private void notifyResult(Playlist playlist) {
        if (getParentFragment() instanceof OnPlaylistCreated) {
            ((OnPlaylistCreated) getParentFragment()).onPlaylistCreated(playlist);
        }
        if (getActivity() instanceof OnPlaylistCreated) {
            ((OnPlaylistCreated) getActivity()).onPlaylistCreated(playlist);
        }
    }

    /** The app assigns the playlist's mood from the in-app mood chips. */
    private String moodNameFor(int chipId) {
        if (chipId == R.id.moodHappy) return "Happy";
        if (chipId == R.id.moodChill) return "Chill";
        if (chipId == R.id.moodSad) return "Sad";
        if (chipId == R.id.moodEnergetic) return "Energetic";
        if (chipId == R.id.moodFocus) return "Focus";
        if (chipId == R.id.moodRomantic) return "Romantic";
        return null;
    }

    private final androidx.activity.result.ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            coverUri = uri.toString();
                            View v = getView();
                            if (v != null) {
                                ImageView ivCover = v.findViewById(R.id.ivCover);
                                Glide.with(ivCover).load(coverUri).centerCrop().into(ivCover);
                            }
                        }
                    });

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
