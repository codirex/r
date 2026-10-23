package com.musicplayer.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.musicplayer.app.R;
import com.musicplayer.app.data.model.Song;
import com.musicplayer.app.data.repository.MusicRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Edit dialog opened from the track bottom sheet: lets the user change the
 * track artwork (pick an image) and metadata fields (title, artist, album, genre, year).
 * Changes are written back into Room so the library reflects them immediately;
 * MediaStore itself is not mutated (it may be read-only for the app).
 */
@AndroidEntryPoint
public class EditTrackDialogFragment extends DialogFragment {

    public static final String ARG_SONG = "song";

    @Inject
    MusicRepository repository;

    private Song song;

    public interface OnTrackEdited {
        void onTrackEdited(Song song);
    }

    public static EditTrackDialogFragment newInstance(Song song) {
        EditTrackDialogFragment fragment = new EditTrackDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_track, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            song = getArguments().getParcelable(ARG_SONG);
        }
        if (song == null) {
            dismiss();
            return;
        }

        ImageView ivArtwork = view.findViewById(R.id.ivArtwork);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etArtist = view.findViewById(R.id.etArtist);
        EditText etAlbum = view.findViewById(R.id.etAlbum);
        EditText etGenre = view.findViewById(R.id.etGenre);
        EditText etYear = view.findViewById(R.id.etYear);
        MaterialButton btnPickImage = view.findViewById(R.id.btnPickImage);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        Glide.with(this).load(song.getArtworkUri())
                .placeholder(R.drawable.ic_album_placeholder_large)
                .centerCrop().into(ivArtwork);

        etTitle.setText(song.getTitle());
        etArtist.setText(song.getArtist());
        etAlbum.setText(song.getAlbum());
        etGenre.setText(song.getGenre());
        etYear.setText(song.getYear());

        btnPickImage.setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            song.setTitle(etTitle.getText().toString().trim());
            song.setArtist(etArtist.getText().toString().trim());
            song.setAlbum(etAlbum.getText().toString().trim());
            song.setGenre(etGenre.getText().toString().trim());
            song.setYear(etYear.getText().toString().trim());
            new Thread(() -> repository.songs().update(song)).start();
            if (getParentFragment() instanceof OnTrackEdited) {
                ((OnTrackEdited) getParentFragment()).onTrackEdited(song);
            }
            if (getActivity() instanceof OnTrackEdited) {
                ((OnTrackEdited) getActivity()).onTrackEdited(song);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && song != null) {
                            // Persist a read permission so artwork survives restarts.
                            try {
                                requireContext().getContentResolver()
                                        .takePersistableUriPermission(uri,
                                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {
                                // Not all providers support persistable grants; keep the
                                // transient read for this session.
                            }
                            song.setArtworkUri(uri.toString());
                            ImageView iv = requireView().findViewById(R.id.ivArtwork);
                            if (iv != null) {
                                Glide.with(iv).load(uri).centerCrop().into(iv);
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
