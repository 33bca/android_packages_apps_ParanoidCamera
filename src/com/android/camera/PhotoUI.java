/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.os.AsyncTask;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.FocusOverlayManager.FocusUI;
import com.android.camera.cameradevice.CameraManager;
import com.android.camera.debug.Log;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.camera.widget.AspectRatioDialogLayout;
import com.android.camera.widget.AspectRatioSelector;
import com.android.camera.widget.LocationDialogLayout;
import com.android.camera2.R;

import java.util.List;

public class PhotoUI implements PreviewStatusListener,
    CameraManager.CameraFaceDetectionCallback {

    private static final Log.Tag TAG = new Log.Tag("PhotoUI");
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private static final float UNSET = 0f;

    private final PreviewOverlay mPreviewOverlay;
    private final FocusUI mFocusUI;
    private final CameraActivity mActivity;
    private final PhotoController mController;

    private final View mRootView;
    private Dialog mDialog = null;

    // TODO: Remove face view logic if UX does not bring it back within a month.
    private FaceView mFaceView = null;
    private DecodeImageForReview mDecodeTaskForReview = null;

    private int mZoomMax;
    private List<Integer> mZoomRatios;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mAspectRatio = UNSET;

    private final GestureDetector.OnGestureListener mPreviewGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
    };
    private final DialogInterface.OnDismissListener mOnDismissListener
            = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            mDialog = null;
        }
    };
    private Runnable mRunnableForNextFrame = null;

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right,
            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int width = right - left;
        int height = bottom - top;
        if (mPreviewWidth != width || mPreviewHeight != height) {
            mPreviewWidth = width;
            mPreviewHeight = height;
        }
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    @Override
    public boolean shouldAutoAdjustBottomBar() {
        return true;
    }

    @Override
    public void onPreviewFlipped() {
        mController.updateCameraOrientation();
    }

    /**
     * Sets the runnable to run when the next frame comes in.
     */
    public void setRunnableForNextFrame(Runnable runnable) {
        mRunnableForNextFrame = runnable;
    }

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte [] mData;
        private final int mOrientation;
        private final boolean mMirror;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            mData = data;
            mOrientation = orientation;
            mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // Decode image in background.
            Bitmap bitmap = CameraUtil.downSample(mData, DOWN_SAMPLE_FACTOR);
            if (mOrientation != 0 || mMirror) {
                Matrix m = new Matrix();
                if (mMirror) {
                    // Flip horizontally
                    m.setScale(-1f, 1f);
                }
                m.preRotate(mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                        false);
            }
            return bitmap;
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                return;
            }
            mDecodeTaskForReview = null;
        }
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        mActivity = activity;
        mController = controller;
        mRootView = parent;

        ViewGroup moduleRoot = (ViewGroup) mRootView.findViewById(R.id.module_layout);
        mActivity.getLayoutInflater().inflate(R.layout.photo_module,
                 moduleRoot, true);
        initIndicators();
        mFocusUI = (FocusUI) mRootView.findViewById(R.id.focus_overlay);
        mPreviewOverlay = (PreviewOverlay) mRootView.findViewById(R.id.preview_overlay);
    }

    public FocusUI getFocusUI() {
        return mFocusUI;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1f) {
            aspectRatio = 1f / aspectRatio;
        }

        if (mAspectRatio != aspectRatio) {
            mAspectRatio = aspectRatio;
            // Update transform matrix with the new aspect ratio.
            mController.updatePreviewAspectRatio(mAspectRatio);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mController.onPreviewUIDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mRunnableForNextFrame != null) {
            mRootView.post(mRunnableForNextFrame);
            mRunnableForNextFrame = null;
        }
    }

    public View getRootView() {
        return mRootView;
    }

    private void initIndicators() {
        // TODO init toggle buttons on bottom bar here
    }

    public void onCameraOpened(Camera.Parameters params) {
        initializeZoom(params);
    }

    public void animateCapture(final byte[] jpegData, int orientation, boolean mirror) {
        // Decode jpeg byte array and then animate the jpeg
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.execute();
    }

    // called from onResume but only the first time
    public void initializeFirstTime() {

    }

    // called from onResume every other time
    public void initializeSecondTime(Camera.Parameters params) {
        initializeZoom(params);
        if (mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        }
        // Removes pie menu.
    }

    public void showLocationAndAspectRatioDialog(
            final PhotoModule.LocationDialogCallback locationCallback,
            final PhotoModule.AspectRatioDialogCallback aspectRatioDialogCallback) {
        setDialog(new Dialog(mActivity,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen));
        final LocationDialogLayout locationDialogLayout = (LocationDialogLayout) mActivity
                .getLayoutInflater().inflate(R.layout.location_dialog_layout, null);
        locationDialogLayout.setLocationTaggingSelectionListener(
                new LocationDialogLayout.LocationTaggingSelectionListener() {
            @Override
            public void onLocationTaggingSelected(boolean selected) {
                // Update setting.
                locationCallback.onLocationTaggingSelected(selected);
                // Go to next page.
                showAspectRatioDialog(aspectRatioDialogCallback, mDialog);
            }
        });
        mDialog.setContentView(locationDialogLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mDialog.show();
    }

    /**
     * Dismisses previous dialog if any, sets current dialog to the given dialog,
     * and set the on dismiss listener for the given dialog.
     * @param dialog dialog to show
     */
    private void setDialog(Dialog dialog) {
        if (mDialog != null) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
        }
        mDialog = dialog;
        if (mDialog != null) {
            mDialog.setOnDismissListener(mOnDismissListener);
        }
    }

    public void showAspectRatioDialog(final PhotoModule.AspectRatioDialogCallback callback) {
        setDialog(new Dialog(mActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen));
        showAspectRatioDialog(callback, mDialog);
    }

    private void showAspectRatioDialog(final PhotoModule.AspectRatioDialogCallback callback,
            final Dialog aspectRatioDialog) {
        if (aspectRatioDialog == null) {
            Log.e(TAG, "Dialog for aspect ratio is null.");
            return;
        }
        final AspectRatioDialogLayout aspectRatioDialogLayout =
                (AspectRatioDialogLayout) mActivity
                .getLayoutInflater().inflate(R.layout.aspect_ratio_dialog_layout, null);
        aspectRatioDialogLayout.initialize(
                new AspectRatioDialogLayout.AspectRatioChangedListener() {
                    @Override
                    public void onAspectRatioChanged(AspectRatioSelector.AspectRatio aspectRatio) {
                        // callback to set picture size.
                        callback.onAspectRatioSelected(aspectRatio, new Runnable() {
                            @Override
                            public void run() {
                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                            }
                        });
                    }
                }, callback.get4x3AspectRatioText(), callback.get16x9AspectRatioText(),
                callback.getCurrentAspectRatio());
        aspectRatioDialog.setContentView(aspectRatioDialogLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        aspectRatioDialog.show();
    }

    public void initializeZoom(Camera.Parameters params) {
        if ((params == null) || !params.isZoomSupported()) return;
        mZoomMax = params.getMaxZoom();
        mZoomRatios = params.getZoomRatios();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        // TODO: Need to setup a path to AppUI to do this
        mPreviewOverlay.setupZoom(mZoomMax, params.getZoom(), mZoomRatios, new ZoomChangeListener());
    }

    public void animateFlash() {
        mController.startPreCaptureAnimation();
    }

    public boolean onBackPressed() {
        // In image capture mode, back button should:
        // 1) if there is any popup, dismiss them, 2) otherwise, get out of
        // image capture
        if (mController.isImageCaptureIntent()) {
            mController.onCaptureCancelled();
            return true;
        } else if (!mController.isCameraIdle()) {
            // ignore backs while we're taking a picture
            return true;
        } else {
            return false;
        }
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        mDecodeTaskForReview.execute();

        mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        pauseFaceDetection();
    }

    protected void hidePostCaptureAlert() {
        if (mDecodeTaskForReview != null) {
            mDecodeTaskForReview.cancel(true);
        }
        resumeFaceDetection();
    }

    public void setDisplayOrientation(int orientation) {
        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(orientation);
        }
    }

    private class ZoomChangeListener implements PreviewOverlay.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            mController.onZoomChanged(index);
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    }

    public void setSwipingEnabled(boolean enable) {
        mActivity.setSwipingEnabled(enable);
    }

    public void onPause() {
        if (mFaceView != null) mFaceView.clear();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    public void clearFaces() {
        if (mFaceView != null) {
            mFaceView.clear();
        }
    }

    public void pauseFaceDetection() {
        if (mFaceView != null) mFaceView.pause();
    }

    public void resumeFaceDetection() {
        if (mFaceView != null) mFaceView.resume();
    }

    public void onStartFaceDetection(int orientation, boolean mirror) {
        if (mFaceView != null) {
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(orientation);
            mFaceView.setMirror(mirror);
            mFaceView.resume();
        }
    }

    @Override
    public void onFaceDetection(Face[] faces, CameraManager.CameraProxy camera) {
        if (mFaceView != null) {
            mFaceView.setFaces(faces);
        }
    }

    /**
     * Returns a {@link com.android.camera.ui.PreviewStatusListener.PreviewAreaChangedListener}
     * that should be registered to listen to preview area change.
     */
    public PreviewAreaChangedListener getPreviewAreaSizeChangedListener() {
        return mFaceView;
    }

}
