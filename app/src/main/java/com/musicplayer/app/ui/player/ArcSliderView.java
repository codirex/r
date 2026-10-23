package com.musicplayer.app.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.musicplayer.app.R;
import com.musicplayer.app.util.FormatUtil;

/**
 * Custom progress slider shaped like an inverted arc (قوس مقلوب), designed to hug the
 * bottom curve of the album artwork.
 *
 * Layout of the view:
 *   [running time]                    [total duration]
 *        \                               /
 *         \______ inverted arc _________/
 *
 * The two time counters sit at the upper ends of the arc and the arc drawing starts
 * after them. Progress fills the arc with the accent color; dragging anywhere on the
 * arc seeks.
 */
public class ArcSliderView extends View {

    public interface OnSeekListener {
        void onSeek(long positionMs, long durationMs);

        void onSeekStart();

        void onSeekEnd();
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arcPath = new Path();
    private final Path progressPath = new Path();
    private final RectF oval = new RectF();

    private long positionMs = 0;
    private long durationMs = 0;
    private float arcDepthRatio = 0.18f;   // how deep the arc bows downward
    private float arcStartFraction = 0.16f; // gap used by the time counters at each end
    private float thumbRadius;
    private boolean dragging = false;
    private OnSeekListener seekListener;

    private int colorTrack;
    private int colorProgress;
    private int colorText;

    public ArcSliderView(Context context) {
        this(context, null);
    }

    public ArcSliderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcSliderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        colorTrack = ContextCompat.getColor(getContext(), R.color.slider_track);
        colorProgress = ContextCompat.getColor(getContext(), R.color.accent);
        colorText = ContextCompat.getColor(getContext(), R.color.slider_text);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(colorTrack);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(colorProgress);

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(colorProgress);

        thumbRingPaint.setStyle(Paint.Style.FILL);
        thumbRingPaint.setColor(ContextCompat.getColor(getContext(), R.color.slider_thumb_ring));

        textPaint.setColor(colorText);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    /** Accent color follows the palette extracted from the artwork. */
    public void setAccentColor(int color) {
        this.colorProgress = color;
        progressPaint.setColor(color);
        thumbPaint.setColor(color);
        invalidate();
    }

    public void setOnSeekListener(OnSeekListener listener) {
        this.seekListener = listener;
    }

    public void setProgress(long positionMs, long durationMs) {
        if (dragging) return;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        invalidate();
    }

    private void layoutArc() {
        float w = getWidth();
        float h = getHeight();
        float counterGap = Math.max(w * arcStartFraction, dp(64));
        float pad = dp(10);

        float left = pad + counterGap;
        float right = w - pad - counterGap;
        float depth = h * arcDepthRatio * 2f;

        // The arc's bounding oval: center above the view so the visible bottom part
        // bows downward — an inverted (smile-shaped) arc.
        oval.set(left, pad - depth, right, pad + depth * 2.2f);
        arcPath.rewind();
        arcPath.moveTo(oval.left + oval.width() / 2f, oval.top);
        arcPath.addArc(oval, 270f, 180f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        textPaint.setTextSize(dp(12));
        trackPaint.setStrokeWidth(dp(5));
        progressPaint.setStrokeWidth(dp(5));
        thumbRadius = dp(8);
        thumbRingPaint.setAlpha(70);
        layoutArc();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        layoutArc();

        float cy = oval.top + oval.height() / 2f; // vertical center of the oval
        // Visible arc runs along the BOTTOM half of the oval? No: addArc(270,180)
        // draws the TOP half. For an inverted (bowing down) arc we keep the top half
        // of an oval whose center sits BELOW the counters:
        // points: left end (oval.left, cy) -> bottom (cx, oval.bottom) -> right end.
        float startX = oval.left;
        float startY = cy;
        float endX = oval.right;
        float endY = cy;

        // Track
        arcPath.rewind();
        arcPath.moveTo(startX, startY);
        arcPath.quadTo((startX + endX) / 2f, oval.bottom + (oval.bottom - cy),
                endX, endY);
        canvas.drawPath(arcPath, trackPaint);

        // Time counters at the upper ends
        float counterY = startY - dp(6);
        canvas.drawText(FormatUtil.formatDuration(positionMs),
                dp(10) + dp(52), counterY, textPaint);
        canvas.drawText(FormatUtil.formatDuration(durationMs),
                getWidth() - dp(10) - dp(52), counterY, textPaint);

        // Progress portion via path measure
        float fraction = durationMs > 0
                ? Math.min(1f, positionMs / (float) durationMs) : 0f;

        progressPath.rewind();
        // Sample the quadratic curve and keep the portion up to the progress fraction.
        int steps = 60;
        float px = startX, py = startY;
        float controlX = (startX + endX) / 2f;
        float controlY = oval.bottom + (oval.bottom - cy);
        float targetLen = fraction; // approximate by t-parameter
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float x = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * controlX + t * t * endX;
            float y = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * controlY + t * t * endY;
            if (t <= targetLen || i == 1) {
                if (progressPath.isEmpty()) progressPath.moveTo(startX, startY);
                progressPath.lineTo(x, y);
                px = x;
                py = y;
            }
        }
        canvas.drawPath(progressPath, progressPaint);

        // Thumb at progress end
        if (durationMs > 0) {
            float t = fraction;
            float tx = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * controlX + t * t * endX;
            float ty = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * controlY + t * t * endY;
            canvas.drawCircle(tx, ty, thumbRadius + dp(4), thumbRingPaint);
            canvas.drawCircle(tx, ty, thumbRadius, thumbPaint);
        }
    }

    private float xToFraction(float x) {
        float startX = oval.left;
        float endX = oval.right;
        return Math.max(0f, Math.min(1f, (x - startX) / (endX - startX)));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (durationMs <= 0) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                dragging = true;
                float frac = xToFraction(event.getX());
                positionMs = (long) (frac * durationMs);
                invalidate();
                if (seekListener != null) seekListener.onSeek(positionMs, durationMs);
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                        && seekListener != null) {
                    seekListener.onSeekStart();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                invalidate();
                if (seekListener != null) seekListener.onSeekEnd();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
