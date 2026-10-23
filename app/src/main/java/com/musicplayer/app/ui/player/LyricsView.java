package com.musicplayer.app.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.musicplayer.app.R;
import com.musicplayer.app.util.LrcParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lyrics viewer with live highlighting synced to playback position.
 * Parses LRC-style timestamps ([mm:ss.xx]) or falls back to plain lines shown
 * without timestamps. The current line scales up, uses the accent color and
 * the view auto-scrolls so the active line stays near the vertical center.
 */
public class LyricsView extends View {

    private final List<LrcParser.LyricLine> lines = new ArrayList<>();
    private final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int currentIndex = -1;
    private float scrollOffsetY = 0;
    private float accentColor;
    private float lineSpacing;

    public LyricsView(Context context) {
        this(context, null);
    }

    public LyricsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LyricsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        lineSpacing = dp(28);
        accentColor = ContextCompat.getColor(context, R.color.accent);

        normalPaint.setTextSize(dp(17));
        normalPaint.setTextAlign(Paint.Align.CENTER);
        normalPaint.setColor(ContextCompat.getColor(context, R.color.lyrics_normal));

        highlightPaint.setTextSize(dp(23));
        highlightPaint.setTextAlign(Paint.Align.CENTER);
        highlightPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        highlightPaint.setColor((int) accentColor);

        dimPaint.set(normalPaint);
        dimPaint.setAlpha(120);
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        highlightPaint.setColor(color);
        invalidate();
    }

    public void setLyrics(String raw) {
        lines.clear();
        lines.addAll(LrcParser.parse(raw));
        currentIndex = -1;
        scrollOffsetY = 0;
        invalidate();
    }

    /** Feed the current playback position (ms) for live highlighting. */
    public void updatePosition(long positionMs) {
        if (lines.isEmpty()) return;
        int newIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).timeMs >= 0 && lines.get(i).timeMs <= positionMs) {
                newIndex = i;
            } else if (lines.get(i).timeMs < 0) {
                // Untimed lyrics: never highlight
                newIndex = -1;
                break;
            }
        }
        if (newIndex != currentIndex && newIndex >= 0) {
            currentIndex = newIndex;
            animateScrollTo(currentIndex);
            invalidate();
        }
    }

    private void animateScrollTo(int index) {
        float target = (getHeight() / 2f) - index * lineSpacing;
        // Clamp so first and last lines don't scroll past the edges
        float maxOffset = 0;
        float minOffset = getHeight() / 2f - (lines.size() - 1) * lineSpacing;
        target = Math.max(minOffset, Math.min(maxOffset, target));
        this.animate().cancel();
        scrollOffsetY = target;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lines.isEmpty()) {
            canvas.drawText(getContext().getString(R.string.no_lyrics),
                    getWidth() / 2f, getHeight() / 2f, dimPaint);
            return;
        }
        float y = scrollOffsetY;

        for (int i = 0; i < lines.size(); i++) {
            LrcParser.LyricLine line = lines.get(i);
            Paint paint;
            if (i == currentIndex) {
                paint = highlightPaint;
            } else if (currentIndex >= 0 && Math.abs(i - currentIndex) > 4) {
                paint = dimPaint;
            } else {
                paint = normalPaint;
            }

            if (y > -lineSpacing && y < getHeight() + lineSpacing) {
                canvas.drawText(line.text, getWidth() / 2f, y, paint);
            }
            y += lineSpacing;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
