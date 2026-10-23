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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.musicplayer.app.R;
import com.musicplayer.app.ui.player.PlayerViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Sleep timer dialog: stops playback after a chosen number of minutes.
 * Complements the shutdown dialog opened via long-press on play/pause.
 */
@AndroidEntryPoint
public class SleepTimerDialogFragment extends DialogFragment {

    private PlayerViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_sleep_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        SeekBar seek = view.findViewById(R.id.seekMinutes);
        TextView tvMinutes = view.findViewById(R.id.tvMinutes);
        MaterialButton btnStart = view.findViewById(R.id.btnStart);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        seek.setMax(179); // 1..180 minutes
        seek.setProgress(29);
        updateLabel(seek.getProgress(), tvMinutes);

        seek.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateLabel(progress, tvMinutes);
            }
        });

        btnStart.setOnClickListener(v -> {
            long ms = (seek.getProgress() + 1) * 60_000L;
            viewModel.getController().setStopAfterTime(ms);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            viewModel.getController().clearStopAfter();
            dismiss();
        });
    }

    private void updateLabel(int progress, TextView tv) {
        tv.setText(String.format(Locale.US, "%d minutes", progress + 1));
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

    private abstract static class SimpleSeekListener
            implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
