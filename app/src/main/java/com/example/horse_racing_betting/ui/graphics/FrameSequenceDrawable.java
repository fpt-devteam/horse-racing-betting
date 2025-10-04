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
 * Drawable that animates a sequence of bitmap frames (PNG/WebP) loaded from resources.
 * Usage:
 *   FrameSequenceDrawable d = new FrameSequenceDrawable(res, ids, /*frameMs* / 60)
 *       .setDesiredSizePx(96, 96)   // optional, set before prepare() to downsample
 *       .prepare();
 *   view.setImageDrawable(d);
 *   d.start();
 */
public class FrameSequenceDrawable extends android.graphics.drawable.Drawable
        implements Runnable, android.graphics.drawable.Animatable {

    // ----- Decode / state -----
    private final Resources res;
    private final int[] frameResIds;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<Bitmap> frames = new ArrayList<>();

    // ----- Timing -----
    private long frameDurationMs;   // per-frame duration
    private boolean running = false;
    private boolean loop = true;    // loop by default
    private int currentFrame = 0;

    // ----- Sizing / drawing -----
    private int desiredWidthPx = 0;
    private int desiredHeightPx = 0;
    private final Rect dstRect = new Rect();

    public FrameSequenceDrawable(@NonNull Resources res, @NonNull int[] frameResIds, long frameDurationMs) {
        this.res = res;
        this.frameResIds = frameResIds;
        this.frameDurationMs = Math.max(16L, frameDurationMs); // cap to ~60fps
    }

    /** Optional: set frames-per-second instead of milliseconds. */
    public FrameSequenceDrawable setFps(float fps) {
        if (fps > 0f) {
            this.frameDurationMs = Math.max(16L, (long) Math.floor(1000f / fps));
        }
        return this;
    }

    /** Optional: enable or disable looping (default true). */
    public FrameSequenceDrawable setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    /** Optional desired size. If set BEFORE prepare(), images decode downsampled to save memory. */
    public FrameSequenceDrawable setDesiredSizePx(int widthPx, int heightPx) {
        this.desiredWidthPx = Math.max(0, widthPx);
        this.desiredHeightPx = Math.max(0, heightPx);
        onBoundsChange(getBounds()); // recompute dst for new size
        return this;
    }

    /** Decode all frames now. Call once after constructing (and after size if you want downsampling). */
    public FrameSequenceDrawable prepare() {
        release(); // in case of reuse
        if (frameResIds.length == 0) return this;

        final boolean scaleToDesired = desiredWidthPx > 0 && desiredHeightPx > 0;

        for (int id : frameResIds) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

            if (scaleToDesired) {
                // probe to compute inSampleSize
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(res, id, opts);
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = computeInSampleSize(
                        Math.max(1, opts.outWidth),
                        Math.max(1, opts.outHeight),
                        desiredWidthPx, desiredHeightPx
                );
            }

            Bitmap b = BitmapFactory.decodeResource(res, id, opts);
            if (b != null) frames.add(b);
        }
        return this;
    }

    // ---- Drawable overrides ----

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (frames.isEmpty()) return;
        Bitmap frame = frames.get(currentFrame % frames.size());
        if (frame == null || frame.isRecycled()) return;
        canvas.drawBitmap(frame, null, dstRect, paint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateDstRect(bounds);
    }

    private void updateDstRect(@NonNull Rect bounds) {
        dstRect.set(bounds);
        if (frames.isEmpty()) return;

        Bitmap ref = frames.get(0);
        if (ref == null) return;

        // Fit-center while preserving aspect ratio.
        int targetW = desiredWidthPx > 0 ? desiredWidthPx : bounds.width();
        int targetH = desiredHeightPx > 0 ? desiredHeightPx : bounds.height();

        targetW = Math.max(1, targetW);
        targetH = Math.max(1, targetH);

        float srcW = Math.max(1, ref.getWidth());
        float srcH = Math.max(1, ref.getHeight());
        float scale = Math.min(targetW / srcW, targetH / srcH);

        int outW = Math.max(1, Math.round(srcW * scale));
        int outH = Math.max(1, Math.round(srcH * scale));

        int cx = bounds.centerX();
        int cy = bounds.centerY();
        int halfW = outW / 2;
        int halfH = outH / 2;
        dstRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
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
        if (!frames.isEmpty() && frames.get(0) != null) return frames.get(0).getWidth();
        return desiredWidthPx > 0 ? desiredWidthPx : super.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        if (!frames.isEmpty() && frames.get(0) != null) return frames.get(0).getHeight();
        return desiredHeightPx > 0 ? desiredHeightPx : super.getIntrinsicHeight();
    }

    // ---- Animatable ----

    @Override
    public void start() {
        if (running || frames.isEmpty()) return;
        running = true;
        scheduleSelf(this, SystemClock.uptimeMillis() + frameDurationMs);
        invalidateSelf();
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        unscheduleSelf(this);
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public void run() {
        if (!running || frames.isEmpty()) return;

        int next = currentFrame + 1;
        if (next >= frames.size()) {
            if (!loop) {
                stop();
                return;
            }
            next = 0;
        }
        currentFrame = next;

        invalidateSelf();
        if (running) {
            scheduleSelf(this, SystemClock.uptimeMillis() + frameDurationMs);
        }
    }

    /** Pause/resume automatically when drawable visibility changes. */
    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (changed) {
            if (!visible) {
                stop();
            } else if (restart) {
                currentFrame = 0;
                start();
            } else if (!isRunning()) {
                start();
            }
        }
        return changed;
    }

    /** Free all decoded bitmaps. Call when you no longer need the drawable. */
    public void release() {
        stop();
        for (Bitmap b : frames) {
            if (b != null && !b.isRecycled()) b.recycle();
        }
        frames.clear();
    }

    // ---- Helpers ----

    private static int computeInSampleSize(int srcW, int srcH, int reqW, int reqH) {
        int inSample = 1;
        if (srcH > reqH || srcW > reqW) {
            final int halfH = srcH / 2;
            final int halfW = srcW / 2;
            while ((halfH / inSample) >= reqH && (halfW / inSample) >= reqW) {
                inSample *= 2;
            }
        }
        return Math.max(1, inSample);
    }
}
