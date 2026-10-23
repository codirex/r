package com.musicplayer.app.ui.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.musicplayer.app.R;
import com.musicplayer.app.equalizer.EqualizerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Equalizer screen: an on/off switch, the ten factory presets (Flat, Live, Pop, Rock,
 * Jazz, Classical, Dance, Heavy Bass, Treble Boost, Vocal) and one level slider per
 * hardware band. Moving a slider switches the tuning to "custom" (no preset selected).
 */
@AndroidEntryPoint
public class EqualizerDialogFragment extends DialogFragment {

    @Inject
    EqualizerManager equalizerManager;

    private ChipGroup chipGroupPresets;
    private SwitchMaterial switchEnabled;
    private LinearLayout bandsContainer;
    private final List<SeekBar> bandSeekBars = new ArrayList<>();
    private final List<TextView> bandValueViews = new ArrayList<>();
    private boolean updatingUi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_equalizer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        chipGroupPresets = view.findViewById(R.id.chipGroupPresets);
        switchEnabled = view.findViewById(R.id.switchEnabled);
        bandsContainer = view.findViewById(R.id.bandsContainer);

        switchEnabled.setChecked(equalizerManager.isEnabled());
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingUi) return;
            equalizerManager.setEnabled(isChecked);
            updateBandRowsEnabled(isChecked);
        });

        buildPresetChips();
        buildBandRows();
        updateBandRowsEnabled(equalizerManager.isEnabled());

        View btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
    }

    private void buildPresetChips() {
        EqualizerManager.Preset[] presets = EqualizerManager.getPresets();
        for (int i = 0; i < presets.length; i++) {
            final int index = i;
            Chip chip = new Chip(requireContext());
            chip.setCheckable(true);
            chip.setText(presets[i].name);
            chip.setTag(index);
            chipGroupPresets.addView(chip);
        }

        int current = equalizerManager.getCurrentPresetIndex();
        if (current >= 0 && current < presets.length) {
            View saved = chipGroupPresets.getChildAt(current);
            if (saved instanceof Chip) ((Chip) saved).setChecked(true);
        }

        chipGroupPresets.setOnCheckedChangeListener((group, checkedId) -> {
            if (updatingUi) return;
            View checked = group.findViewById(checkedId);
            if (checked == null || !(checked.getTag() instanceof Integer)) return;
            equalizerManager.applyPreset((Integer) checked.getTag());
            refreshBandRows();
        });
    }

    private void buildBandRows() {
        bandSeekBars.clear();
        bandValueViews.clear();
        bandsContainer.removeAllViews();

        int bands = equalizerManager.getNumberOfBands();
        short[] range = equalizerManager.getBandLevelRange();
        final int min = range[0];

        for (int i = 0; i < bands; i++) {
            final int band = i;
            short currentLevel = equalizerManager.getBandLevel(band);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = new TextView(requireContext());
            label.setText(freqLabel(equalizerManager.getBandFreqRange(band)));
            label.setTextSize(12f);

            SeekBar seek = new SeekBar(requireContext());
            seek.setMax(range[1] - min);
            seek.setProgress(currentLevel - min);

            TextView value = new TextView(requireContext());
            value.setTextSize(12f);
            value.setGravity(Gravity.END);
            value.setMinWidth(dp(56));
            value.setText(levelText(currentLevel));

            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f);
            LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
            seekLp.setMargins(dp(8), 0, dp(8), 0);

            row.addView(label, labelLp);
            row.addView(seek, seekLp);
            row.addView(value, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    short level = (short) (min + progress);
                    equalizerManager.setBandLevel(band, level);
                    value.setText(levelText(level));
                    // Manual tuning no longer matches any preset.
                    if (!updatingUi) {
                        updatingUi = true;
                        chipGroupPresets.clearCheck();
                        updatingUi = false;
                        equalizerManager.markCustom();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            bandSeekBars.add(seek);
            bandValueViews.add(value);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(6);
            bandsContainer.addView(row, rowLp);
        }
    }

    /** Pushes the manager's current band levels into the sliders (after a preset change). */
    private void refreshBandRows() {
        updatingUi = true;
        int min = equalizerManager.getBandLevelRange()[0];
        for (int i = 0; i < bandSeekBars.size(); i++) {
            short level = equalizerManager.getBandLevel(i);
            bandSeekBars.get(i).setProgress(level - min);
            bandValueViews.get(i).setText(levelText(level));
        }
        updatingUi = false;
    }

    private void updateBandRowsEnabled(boolean enabled) {
        bandsContainer.setAlpha(enabled ? 1f : 0.45f);
        for (SeekBar seek : bandSeekBars) {
            seek.setEnabled(enabled);
        }
    }

    private String freqLabel(int[] rangeHz) {
        if (rangeHz == null || rangeHz.length < 2 || rangeHz[1] == 0) return "";
        return formatFreq(rangeHz[0]) + " - " + formatFreq(rangeHz[1]);
    }

    private String formatFreq(int hz) {
        if (hz >= 1000) return String.format(Locale.US, "%d kHz", Math.round(hz / 1000f));
        return hz + " Hz";
    }

    private String levelText(short millibel) {
        return String.format(Locale.US, "%+.1f dB", millibel / 100f);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
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
