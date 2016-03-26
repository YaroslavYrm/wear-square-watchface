package com.yyermolenko.squarewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;

public class SquareWatchFace extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        boolean registeredTimeZoneReceiver = false;
        Paint backgroundPaint;
        Paint minutePaint;
        Paint hourPaint;
        Paint hourBorderPaint;
        Paint minuteTextPaint;
        Paint hourTextPaint;

        boolean isAmbient;
        Time time;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.clear(intent.getStringExtra("time-zone"));
            }
        };
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean isLowBitAmbient;
        private float dp;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SquareWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SquareWatchFace.this.getResources();
            dp = resources.getDisplayMetrics().density;

            backgroundPaint = new Paint();
            int backgroundColor = resources.getColor(R.color.background);
            backgroundPaint.setColor(backgroundColor);

            float hourStroke = resources.getDimension(R.dimen.hand_hour_stroke);
            float minuteStroke = resources.getDimension(R.dimen.hand_minute_stroke);

            hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hourPaint.setColor(resources.getColor(R.color.analog_hands));
            hourPaint.setAntiAlias(true);

            minutePaint = new Paint(hourPaint);

            hourPaint.setStrokeWidth(hourStroke);
            minutePaint.setStrokeWidth(minuteStroke);

            hourBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hourBorderPaint.setColor(backgroundColor);
            hourBorderPaint.setStrokeWidth(hourStroke + dp);

            minuteTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            minuteTextPaint.setColor(backgroundColor);

            Typeface font = Typeface.createFromAsset(getAssets(), "Minercraftory.ttf");
            minuteTextPaint.setTypeface(font);
            minuteTextPaint.setTextAlign(Paint.Align.CENTER);

            hourTextPaint = new Paint(minuteTextPaint);

            minuteTextPaint.setTextSize(minuteStroke * 0.9f);
            hourTextPaint.setTextSize(hourStroke * 0.9f);

            time = new Time();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            isLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (isAmbient != inAmbientMode) {
                isAmbient = inAmbientMode;
                if (isLowBitAmbient) {
                    hourPaint.setAntiAlias(!inAmbientMode);
                    hourBorderPaint.setAntiAlias(!inAmbientMode);
                    hourTextPaint.setAntiAlias(!inAmbientMode);
                    minutePaint.setAntiAlias(!inAmbientMode);
                    minuteTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            time.setToNow();

            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
            }

            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            int minutes = time.minute;
            int hours = time.hour;
            float minRot = minutes / 60f * 360f;
            float hrRot = ((hours + (minutes / 60f)) / 12f) * 360f;

            float minLength = centerX - 10;
            float hrLength = centerX - 80;

            canvas.save();

            canvas.rotate(minRot - 90, canvas.getWidth() / 2, canvas.getHeight() / 2);
            canvas.drawLine(centerX, centerY, centerX + minLength, centerY, minutePaint);
            canvas.drawText(Integer.toString(time.minute),
                    centerX + hrLength + (minLength - hrLength) / 2,
                    centerY - ((minuteTextPaint.descent() + minuteTextPaint.ascent()) / 2),
                    minuteTextPaint);

            canvas.restore();
            canvas.rotate(hrRot - 90, canvas.getWidth() / 2, canvas.getHeight() / 2);
            canvas.drawLine(centerX - dp, centerY, centerX + dp + hrLength, centerY, hourBorderPaint);
            canvas.drawLine(centerX, centerY, centerX + hrLength, centerY, hourPaint);

            String hourText = Integer.toString(hours);
            canvas.drawText(hourText,
                    centerX + hrLength / 2,
                    centerY - ((hourTextPaint.descent() + hourTextPaint.ascent()) / 2),
                    hourTextPaint);

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                time.clear(TimeZone.getDefault().getID());
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SquareWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            SquareWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

    }
}
