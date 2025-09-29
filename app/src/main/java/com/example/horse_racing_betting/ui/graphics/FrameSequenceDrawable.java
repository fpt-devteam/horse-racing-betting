package com.example.horse_racing_betting.ui.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * FrameSequenceDrawable cycles through a list of resource frames (PNG/WebP), useful when you have
 * individual images instead of a single spritesheet strip.
 */
public class FrameSequenceDrawable extends android.graphics.drawable.Drawable implements Runnable, android.graphics.drawable.Animatable {

    private final Resources res;
    private final int[] frameResIds;
    private final long frameDurationMs;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<Bitmap> frames = new ArrayList<>();

    private int currentFrame = 0;
    private boolean running = false;
    private int desiredWidthPx = 0;
    private int desiredHeightPx = 0;

    public FrameSequenceDrawable(@NonNull Resources res, @NonNull int[] frameResIds, long frameDurationMs) {
        this.res = res;
        this.frameResIds = frameResIds;
        this.frameDurationMs = Math.max(16L, frameDurationMs);
    }

    /** Decode all frames now; you can call this once after constructing. */
    public FrameSequenceDrawable prepare() {
        frames.clear();
        for (int id : frameResIds) {
            Bitmap b = BitmapFactory.decodeResource(res, id);
            if (b != null) frames.add(b);
        }
        return this;
    }

    public FrameSequenceDrawable setDesiredSizePx(int widthPx, int heightPx) {
        this.desiredWidthPx = widthPx;
        this.desiredHeightPx = heightPx;
        return this;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (frames.isEmpty()) return;
        Bitmap frame = frames.get(currentFrame % frames.size());
        if (frame == null || frame.isRecycled()) return;
        Rect dst = getBounds();
        if (desiredWidthPx > 0 && desiredHeightPx > 0) {
            int cx = dst.centerX();
            int cy = dst.centerY();
            int halfW = desiredWidthPx / 2;
            int halfH = desiredHeightPx / 2;
            dst = new Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        }
        canvas.drawBitmap(frame, null, dst, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        if (!frames.isEmpty()) return frames.get(0).getWidth();
        return desiredWidthPx > 0 ? desiredWidthPx : 0;
    }

    @Override
    public int getIntrinsicHeight() {
        if (!frames.isEmpty()) return frames.get(0).getHeight();
        return desiredHeightPx > 0 ? desiredHeightPx : 0;
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        scheduleSelf(this, SystemClock.uptimeMillis() + frameDurationMs);
    }

    @Override
    public void stop() {
        running = false;
        unscheduleSelf(this);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        if (!running || frames.isEmpty()) return;
        currentFrame = (currentFrame + 1) % frames.size();
        invalidateSelf();
        scheduleSelf(this, SystemClock.uptimeMillis() + frameDurationMs);
    }
}
