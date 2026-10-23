package com.musicplayer.app.equalizer;

import android.content.Context;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Wraps the Android {@link Equalizer} audio effect. Attaches to the ExoPlayer audio
 * session id, exposes band levels/gains and provides factory presets
 * (Flat, Live, Pop, Rock, Jazz, Classical, Dance, Heavy Bass, Treble Boost, Vocal...).
 */
@Singleton
public class EqualizerManager {

    public static class Preset {
        public final String name;
        public final short[] bandLevels; // millibels per band

        public Preset(String name, short[] bandLevels) {
            this.name = name;
            this.bandLevels = bandLevels;
        }
    }

    private Equalizer equalizer;
    private final Context context;
    private boolean enabled = true;
    private int currentPresetIndex = 0;

    @Inject
    public EqualizerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Attach to the player's audio session. Re-attaches if session changes. */
    public synchronized void attach(int audioSessionId) {
        release();
        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(enabled);
        } catch (Exception e) {
            equalizer = null;
        }
    }

    public synchronized void release() {
        if (equalizer != null) {
            try {
                equalizer.release();
            } catch (Exception ignored) {
            }
            equalizer = null;
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (equalizer != null) {
            try {
                equalizer.setEnabled(enabled);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getNumberOfBands() {
        if (equalizer == null) return 5;
        try {
            return equalizer.getNumberOfBands();
        } catch (Exception e) {
            return 5;
        }
    }

    public short[] getBandLevelRange() {
        if (equalizer == null) return new short[]{-1500, 1500};
        try {
            return equalizer.getBandLevelRange();
        } catch (Exception e) {
            return new short[]{-1500, 1500};
        }
    }

    public short getBandLevel(int band) {
        if (equalizer == null) return 0;
        try {
            return equalizer.getBandLevel((short) band);
        } catch (Exception e) {
            return 0;
        }
    }

    public void setBandLevel(int band, short level) {
        if (equalizer == null) return;
        try {
            equalizer.setBandLevel((short) band, level);
        } catch (Exception ignored) {
        }
    }

    public synchronized int getCurrentPresetIndex() {
        return currentPresetIndex;
    }

    public synchronized void applyPreset(int index) {
        currentPresetIndex = index;
        Preset preset = getPresets()[index];
        for (int i = 0; i < preset.bandLevels.length; i++) {
            setBandLevel(i, preset.bandLevels[i]);
        }
    }

    /** Built-in preset bank applied across all supported bands. */
    public static Preset[] getPresets() {
        return new Preset[]{
                new Preset("Flat", new short[]{0, 0, 0, 0, 0}),
                new Preset("Live", new short[]{-300, 0, 300, 300, 100}),
                new Preset("Pop", new short[]{-100, 300, 400, 300, -100}),
                new Preset("Rock", new short[]{400, 200, -100, 200, 400}),
                new Preset("Jazz", new short[]{300, 100, -100, 100, 300}),
                new Preset("Classical", new short[]{400, 200, 0, 200, 400}),
                new Preset("Dance", new short[]{500, 300, 0, -200, -300}),
                new Preset("Heavy Bass", new short[]{600, 500, 200, 0, 0}),
                new Preset("Treble Boost", new short[]{0, 0, 200, 400, 600}),
                new Preset("Vocal", new short[]{-200, -100, 300, 400, 300}),
        };
    }
}
