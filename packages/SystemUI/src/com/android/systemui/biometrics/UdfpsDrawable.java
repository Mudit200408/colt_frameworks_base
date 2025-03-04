/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.biometrics;

import android.content.Context;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.provider.Settings;
import android.util.PathParser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.util.colt.ColtUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;

/**
 * Abstract base class for drawable displayed when the finger is not touching the
 * sensor area.
 */
public abstract class UdfpsDrawable extends Drawable {
    static final float DEFAULT_STROKE_WIDTH = 3f;
    static final String UDFPS_ICON = "system:" + Settings.System.UDFPS_ICON;

    String udfpsResourcesPackage = "com.colt.udfps.resources";

    @NonNull final Context mContext;
    @NonNull final ShapeDrawable mFingerprintDrawable;
    private final Paint mPaint;
    private boolean mDisplayConfigured;

    int mSelectedIcon = 0;

    Drawable mUdfpsDrawable;
    Resources udfpsRes;
    String[] mUdfpsIcons;

    int mAlpha = 255; // 0 - 255
    public UdfpsDrawable(@NonNull Context context) {
        mContext = context;
        final String fpPath = context.getResources().getString(R.string.config_udfpsIcon);
        mFingerprintDrawable = new ShapeDrawable(
                new PathShape(PathParser.createPathFromPathData(fpPath), 72, 72));
        mFingerprintDrawable.mutate();

        mPaint = mFingerprintDrawable.getPaint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        setStrokeWidth(DEFAULT_STROKE_WIDTH);

        init();
    }

    void init() {
        if (ColtUtils.isPackageInstalled(mContext, udfpsResourcesPackage)) {
            try {
                PackageManager pm = mContext.getPackageManager();
                udfpsRes = pm.getResourcesForApplication(udfpsResourcesPackage);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            int res = udfpsRes.getIdentifier("udfps_icons",
                    "array", udfpsResourcesPackage);
            mUdfpsIcons = udfpsRes.getStringArray(res);

            TunerService.Tunable tunable = (key, newValue) -> {
                if (UDFPS_ICON.equals(key)) {
                    mSelectedIcon = newValue == null ? 0 : Integer.parseInt(newValue);
                    mUdfpsDrawable = mSelectedIcon == 0 ? null :
                            loadDrawable(udfpsRes,
                                    mUdfpsIcons[mSelectedIcon]);
                }
            };
            Dependency.get(TunerService.class).addTunable(tunable, UDFPS_ICON);
        }
    }

    void setStrokeWidth(float strokeWidth) {
        mPaint.setStrokeWidth(strokeWidth);
        invalidateSelf();
    }

    /**
     * @param sensorRect the rect coordinates for the sensor area
     */
    public void onSensorRectUpdated(@NonNull RectF sensorRect) {
        final int margin =  (int) sensorRect.height() / 16;

        final Rect bounds = new Rect((int) sensorRect.left + margin,
                (int) sensorRect.top + margin,
                (int) sensorRect.right - margin,
                (int) sensorRect.bottom - margin);
        updateFingerprintIconBounds(bounds);
    }

    /**
     * Bounds for the fingerprint icon
     */
    protected void updateFingerprintIconBounds(@NonNull Rect bounds) {
        mFingerprintDrawable.setBounds(bounds);
        if (mUdfpsDrawable != null) {
            mUdfpsDrawable.setBounds(bounds);
        }
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mFingerprintDrawable.setAlpha(mAlpha);
        if (mUdfpsDrawable != null) {
            mUdfpsDrawable.setAlpha(mAlpha);
        }
        invalidateSelf();
    }

    boolean isDisplayConfigured() {
        return mDisplayConfigured;
    }

    Drawable getUdfpsDrawable() {
        return mUdfpsDrawable;
    }

    Drawable loadDrawable(Resources res, String resName) {
        int resId = res.getIdentifier(resName,
                "drawable", udfpsResourcesPackage);
        return res.getDrawable(resId);
    }

    void setDisplayConfigured(boolean showing) {
        if (mDisplayConfigured == showing) {
            return;
        }
        mDisplayConfigured = showing;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
