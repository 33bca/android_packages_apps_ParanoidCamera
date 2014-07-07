/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.android.camera.CaptureLayoutHelper;
import com.android.camera.ShutterButton;
import com.android.camera.debug.Log;
import com.android.camera.util.Gusterpolator;
import com.android.camera2.R;

/**
 * BottomBar swaps its width and height on rotation. In addition, it also changes
 * gravity and layout orientation based on the new orientation. Specifically, in
 * landscape it aligns to the right side of its parent and lays out its children
 * vertically, whereas in portrait, it stays at the bottom of the parent and has
 * a horizontal layout orientation.
*/
public class BottomBar extends FrameLayout {

    private static final Log.Tag TAG = new Log.Tag("BottomBar");

    private static final int CIRCLE_ANIM_DURATION_MS = 300;

    private static final int MODE_CAPTURE = 0;
    private static final int MODE_INTENT = 1;
    private static final int MODE_INTENT_REVIEW = 2;
    private static final int MODE_CANCEL = 3;

    private int mMode;

    private float mPreviewShortEdge;
    private float mPreviewLongEdge;

    private final int mMinimumHeight;
    private final int mMaximumHeight;
    private final int mOptimalHeight;
    private final int mBackgroundAlphaOverlay;
    private final int mBackgroundAlphaDefault;
    private boolean mOverLayBottomBar;
    // To avoid multiple object allocations in onLayout().
    private final RectF mAlignArea = new RectF();

    private FrameLayout mCaptureLayout;
    private FrameLayout mCancelLayout;
    private TopRightWeightedLayout mIntentReviewLayout;

    private ShutterButton mShutterButton;
    private ImageButton mCancelButton;

    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private int mBackgroundAlpha = 0xff;

    private boolean mDrawCircle;
    private final float mCircleRadius;
    private float mCurrentCircleRadius;
    private int mCircleColor;
    private final Paint mCirclePaint= new Paint();
    private float mCenterX;
    private float mCenterY;
    private final RectF mRect = new RectF();
    private CaptureLayoutHelper mCaptureLayoutHelper = null;

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMinimumHeight = getResources().getDimensionPixelSize(R.dimen.bottom_bar_height_min);
        mMaximumHeight = getResources().getDimensionPixelSize(R.dimen.bottom_bar_height_max);
        mOptimalHeight = getResources().getDimensionPixelSize(R.dimen.bottom_bar_height_optimal);
        mCircleRadius = getResources()
            .getDimensionPixelSize(R.dimen.video_capture_circle_diameter) / 2;
        mBackgroundAlphaOverlay = getResources().getInteger(R.integer.bottom_bar_background_alpha_overlay);
        mBackgroundAlphaDefault = getResources().getInteger(R.integer
                .bottom_bar_background_alpha);
    }

    private void setPaintColor(int alpha, int color, boolean isCaptureChange) {
        mCircleColor = (alpha << 24) | (color & 0x00ffffff);
        mCirclePaint.setColor(mCircleColor);
        invalidate();
    }

    private void setPaintColor(int alpha, int color) {
        setPaintColor(alpha, color, false);
    }

    private void setCaptureButtonUp() {
        setPaintColor(mBackgroundAlpha, mBackgroundColor, true);
        invalidate();
    }

    private void setCaptureButtonDown() {
        setPaintColor(mBackgroundAlpha, mBackgroundPressedColor, true);
        invalidate();
    }

    @Override
    public void onFinishInflate() {
        mCaptureLayout
            = (FrameLayout) findViewById(R.id.bottombar_capture);
        mCancelLayout
            = (FrameLayout) findViewById(R.id.bottombar_cancel);

        mCancelLayout.setVisibility(View.INVISIBLE);

        mIntentReviewLayout
            = (TopRightWeightedLayout) findViewById(R.id.bottombar_intent_review);

        mShutterButton
            = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
                    setCaptureButtonDown();
                } else if (MotionEvent.ACTION_UP == event.getActionMasked() ||
                        MotionEvent.ACTION_CANCEL == event.getActionMasked()) {
                    setCaptureButtonUp();
                } else if (MotionEvent.ACTION_MOVE == event.getActionMasked()) {
                    if (!mRect.contains(event.getX(), event.getY())) {
                        setCaptureButtonUp();
                    }
                }
                return false;
            }
        });

        mCancelButton
                = (ImageButton) findViewById(R.id.shutter_cancel_button);
        mCancelButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getActionMasked()) {
                    mCancelLayout.setBackgroundColor(mBackgroundPressedColor);
                } else if (MotionEvent.ACTION_UP == event.getActionMasked() ||
                        MotionEvent.ACTION_CANCEL == event.getActionMasked()) {
                    mCancelLayout.setBackgroundColor(mCircleColor);
                } else if (MotionEvent.ACTION_MOVE == event.getActionMasked()) {
                    if (!mRect.contains(event.getX(), event.getY())) {
                        mCancelLayout.setBackgroundColor(mCircleColor);
                    }
                }
                return false;
            }
        });

    }

    /**
     * Hide the intent layout.  This is necessary for switching between
     * the intent capture layout and the bottom bar options.
     */
    private void hideIntentReviewLayout() {
        mIntentReviewLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * Perform a transition from the bottom bar options layout to the
     * bottom bar capture layout.
     */
    public void transitionToCapture() {
        mCaptureLayout.setVisibility(View.VISIBLE);
        mCancelLayout.setVisibility(View.INVISIBLE);
        if (mMode == MODE_INTENT || mMode == MODE_INTENT_REVIEW) {
            mIntentReviewLayout.setVisibility(View.INVISIBLE);
        }

        mMode = MODE_CAPTURE;
    }


    /**
     * Perform a transition from the bottom bar options layout to the
     * bottom bar capture layout.
     */
    public void transitionToCancel() {
        mCaptureLayout.setVisibility(View.INVISIBLE);
        mIntentReviewLayout.setVisibility(View.INVISIBLE);
        mCancelLayout.setBackgroundColor(mCircleColor);
        mCancelLayout.setVisibility(View.VISIBLE);
        mMode = MODE_CANCEL;
    }

    /**
     * Perform a transition to the global intent layout.
     * The current layout state of the bottom bar is irrelevant.
     */
    public void transitionToIntentCaptureLayout() {
        mIntentReviewLayout.setVisibility(View.INVISIBLE);
        mCaptureLayout.setVisibility(View.VISIBLE);
        mCancelLayout.setVisibility(View.INVISIBLE);

        mMode = MODE_INTENT;
    }

    /**
     * Perform a transition to the global intent review layout.
     * The current layout state of the bottom bar is irrelevant.
     */
    public void transitionToIntentReviewLayout() {
        mCaptureLayout.setVisibility(View.INVISIBLE);
        mIntentReviewLayout.setVisibility(View.VISIBLE);
        mCancelLayout.setVisibility(View.INVISIBLE);

        mMode = MODE_INTENT_REVIEW;
    }

    /**
     * @return whether UI is in intent review mode
     */
    public boolean isInIntentReview() {
        return mMode == MODE_INTENT_REVIEW;
    }

    private void setButtonImageLevels(int level) {
        ((ImageButton) findViewById(R.id.cancel_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.done_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.retake_button)).setImageLevel(level);
    }

    private void setOverlayBottomBar(boolean overlay) {
        mOverLayBottomBar = overlay;
        if (overlay) {
            setBackgroundAlpha(mBackgroundAlphaOverlay);
            setButtonImageLevels(1);
        } else {
            setBackgroundAlpha(mBackgroundAlphaDefault);
            setButtonImageLevels(0);
        }
    }

    /**
     * Sets a capture layout helper to query layout rect from.
     */
    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        mCaptureLayoutHelper = helper;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (measureWidth == 0 || measureHeight == 0) {
            return;
        }

        if (mCaptureLayoutHelper == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "Capture layout helper needs to be set first.");
        } else {
            RectF bottomBarRect = mCaptureLayoutHelper.getBottomBarRect();
            super.onMeasure(MeasureSpec.makeMeasureSpec(
                            (int) bottomBarRect.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) bottomBarRect.height(), MeasureSpec.EXACTLY)
            );
            boolean shouldOverlayBottomBar = mCaptureLayoutHelper.shouldOverlayBottomBar();
            setOverlayBottomBar(shouldOverlayBottomBar);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mCenterX = (right - left) / 2;
        mCenterY = (bottom - top) / 2;
    }

    // prevent touches on bottom bar (not its children)
    // from triggering a touch event on preview area
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        switch (mMode) {
            case MODE_CAPTURE:
                if (mDrawCircle) {
                    canvas.drawCircle(mCenterX, mCenterY, mCurrentCircleRadius, mCirclePaint);
                } else {
                    canvas.drawColor(mCircleColor);
                }
                break;
            case MODE_INTENT:
                canvas.drawPaint(mCirclePaint); // TODO make this case handle capture button
                                                // highlighting correctly
                break;
            case MODE_INTENT_REVIEW:
                canvas.drawPaint(mCirclePaint);
        }

        super.onDraw(canvas);
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    public void setBackgroundPressedColor(int color) {
        mBackgroundPressedColor = color;
    }

    public void setBackgroundAlpha(int alpha) {
        mBackgroundAlpha = alpha;
        setPaintColor(mBackgroundAlpha, mBackgroundColor);
    }

    /**
     * Sets the shutter button enabled if true, disabled if false.
     * <p>
     * Disabled means that the shutter button is not clickable and is
     * greyed out.
     */
    public void setShutterButtonEnabled(boolean enabled) {
        mShutterButton.setEnabled(enabled);
        setShutterButtonImportantToA11y(enabled);
    }

    /**
     * Sets whether shutter button should be included in a11y announcement and navigation
     */
    public void setShutterButtonImportantToA11y(boolean important) {
        if (important) {
            mShutterButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        } else {
            mShutterButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    /**
     * Returns whether the capture button is enabled.
     */
    public boolean isShutterButtonEnabled() {
        return mShutterButton.isEnabled();
    }

    private double diagonalLength(double w, double h) {
        return Math.sqrt((w*w) + (h*h));
    }
    private double diagonalLength() {
        return diagonalLength(getWidth(), getHeight());
    }

    private TransitionDrawable crossfadeDrawable(Drawable from, Drawable to) {
        Drawable [] arrayDrawable = new Drawable[2];
        arrayDrawable[0] = from;
        arrayDrawable[1] = to;
        TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
        transitionDrawable.setCrossFadeEnabled(true);
        return transitionDrawable;
    }

    /**
     * Sets the shutter button's icon resource. By default, all drawables instances
     * loaded from the same resource share a common state; if you modify the state
     * of one instance, all the other instances will receive the same modification.
     * In order to modify properties of this icon drawable without affecting other
     * drawables, here we use a mutable drawable which is guaranteed to not share
     * states with other drawables.
     */
    public void setShutterButtonIcon(int resId) {
        Drawable iconDrawable = getResources().getDrawable(resId);
        if (iconDrawable != null) {
            iconDrawable = iconDrawable.mutate();
        }
        mShutterButton.setImageDrawable(iconDrawable);
    }

    /**
     * Animates bar to a single stop button
     */
    public void animateToVideoStop(int resId) {
        if (mOverLayBottomBar) {
            final ValueAnimator radiusAnimator =
                ValueAnimator.ofFloat((float) diagonalLength()/2, mCircleRadius);
            radiusAnimator.setDuration(CIRCLE_ANIM_DURATION_MS);
            radiusAnimator.setInterpolator(Gusterpolator.INSTANCE);

            radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentCircleRadius = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            mDrawCircle = true;
            radiusAnimator.start();
        }

        TransitionDrawable transitionDrawable = crossfadeDrawable(
                mShutterButton.getDrawable(),
                getResources().getDrawable(resId));
        mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(CIRCLE_ANIM_DURATION_MS);
    }

    /**
     * Animates bar to full width / length with video capture icon
     */
    public void animateToFullSize(int resId) {
        if (mDrawCircle) {
            final float endRadius = (float) diagonalLength()/2;
            final ValueAnimator radiusAnimator =
                ValueAnimator.ofFloat(mCircleRadius, endRadius);
            radiusAnimator.setDuration(CIRCLE_ANIM_DURATION_MS);
            radiusAnimator.setInterpolator(Gusterpolator.INSTANCE);
            radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentCircleRadius = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            radiusAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDrawCircle = false;
                    mCurrentCircleRadius = endRadius;
               }
            });
            radiusAnimator.start();
        }

        TransitionDrawable transitionDrawable = crossfadeDrawable(
                mShutterButton.getDrawable(),
                getResources().getDrawable(resId));
        mShutterButton.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(CIRCLE_ANIM_DURATION_MS);
    }
}
