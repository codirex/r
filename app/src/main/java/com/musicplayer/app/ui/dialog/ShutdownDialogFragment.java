package com.musicplayer.app.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.musicplayer.app.R;
import com.musicplayer.app.ui.player.PlayerViewModel;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import androidx.lifecycle.ViewModelProvider;

/**
 * Long-press on play/pause opens this custom designed DialogFragment.
 * It offers two shutdown modes for playback:
 *  - stop after N tracks
 *  - stop after N minutes
 * Styled as a bottom-anchored card with a mode toggle and a dynamic value slider.
 */
@AndroidEntryPoint
public class ShutdownDialogFragment extends DialogFragment {

    private PlayerViewModel viewModel;

    private MaterialButtonToggleGroup toggleMode;
    private SeekBar seekValue;
    private TextView tvValue;
    private TextView tvHeader;
    private MaterialButton btnConfirm;
    private MaterialButton btnCancel;
    private MaterialCardView cardRoot;

    private boolean tracksMode = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_shutdown, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        toggleMode = view.findViewById(R.id.toggleMode);
        seekValue = view.findViewById(R.id.seekValue);
        tvValue = view.findViewById(R.id.tvValue);
        tvHeader = view.findViewById(R.id.tvHeader);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnCancel = view.findViewById(R.id.btnCancel);
        cardRoot = view.findViewById(R.id.cardRoot);

        toggleMode.check(R.id.modeTracks);

        toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            tracksMode = checkedId == R.id.modeTracks;
            // 1..50 tracks, 1..180 minutes
            seekValue.setMax(tracksMode ? 49 : 179);
            seekValue.setProgress(tracksMode ? 5 : 30);
            updateValueLabel();
        });

        seekValue.setMax(tracksMode ? 49 : 179); // 1..50 tracks / 1..180 minutes
        seekValue.setProgress(5);
        seekValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateValueLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        updateValueLabel();

        btnConfirm.setOnClickListener(v -> {
            int value = seekValue.getProgress() + 1;
            if (tracksMode) {
                viewModel.getController().setStopAfterTracks(value);
            } else {
                viewModel.getController().setStopAfterTime(value * 60_000L);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            viewModel.getController().clearStopAfter();
            dismiss();
        });

        cardRoot.setTranslationY(dp(240));
        cardRoot.animate().translationY(0).setDuration(260).start();
    }

    private void updateValueLabel() {
        int value = seekValue.getProgress() + 1;
        if (tracksMode) {
            tvValue.setText(String.format(Locale.US, "%d tracks", value));
            tvHeader.setText(R.string.shutdown_after_tracks);
        } else {
            tvValue.setText(String.format(Locale.US, "%d min", value));
            tvHeader.setText(R.string.shutdown_after_time);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
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
