/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.camera.one.v1;

import com.google.common.base.Optional;

import android.hardware.Camera;
import android.os.Handler;

import com.android.camera.CameraActivity;
import com.android.camera.debug.Log;
import com.android.camera.one.OneCamera.Facing;
import com.android.camera.one.OneCamera.OpenCallback;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraCharacteristics;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.v2.imagesaver.ImageSaver;
import com.android.camera.util.Size;

/**
 * The {@link OneCameraManager} implementation on top of the Camera API 1.
 */
public class OneCameraManagerImpl extends OneCameraManager {
    private static final Log.Tag TAG = new Log.Tag("OneCameraMgrImpl1");

    private static final int NO_DEVICE = -1;

    private final int mFirstBackCameraId;
    private final int mFirstFrontCameraId;
    private final Camera.CameraInfo[] mCameraInfos;

    OneCameraCharacteristics mBackCameraCharacteristics;
    OneCameraCharacteristics mFrontCameraCharacteristics;

    public static Optional<OneCameraManager> create(CameraActivity activity) {
        int numberOfCameras;
        Camera.CameraInfo[] cameraInfos;
        try {
            numberOfCameras = Camera.getNumberOfCameras();
            cameraInfos = new Camera.CameraInfo[numberOfCameras];
            for (int i = 0; i < numberOfCameras; i++) {
                cameraInfos[i] = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfos[i]);
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "Exception while creating CameraDeviceInfo", ex);
            return Optional.absent();
        }

        int firstFront = NO_DEVICE;
        int firstBack = NO_DEVICE;
        // Get the first (smallest) back and first front camera id.
        for (int i = numberOfCameras - 1; i >= 0; i--) {
            if (cameraInfos[i].facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                firstBack = i;
            } else {
                if (cameraInfos[i].facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    firstFront = i;
                }
            }
        }

        OneCameraManager cameraManager =
                new OneCameraManagerImpl(firstBack, firstFront, cameraInfos);
        return Optional.of(cameraManager);
    }

    /**
     * Instantiates a new {@link OneCameraManager} for Camera1 API.
     */
    public OneCameraManagerImpl(
            int firstBackCameraId,
            int firstFrontCameraId,
            Camera.CameraInfo[] info) {
        mFirstBackCameraId = firstBackCameraId;
        mFirstFrontCameraId = firstFrontCameraId;
        mCameraInfos = info;
    }

    @Override
    public void open(Facing facing, boolean enableHdr, Size pictureSize,
            ImageSaver.Builder imageSaverBuilder, OpenCallback callback, Handler handler) {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public boolean hasCameraFacing(Facing facing) {
        if (facing == Facing.BACK) {
            return mFirstBackCameraId != NO_DEVICE;
        } else if (facing == Facing.FRONT) {
            return mFirstFrontCameraId != NO_DEVICE;
        }
        return false;
    }

    @Override
    public OneCameraCharacteristics getCameraCharacteristics(Facing facing)
            throws OneCameraAccessException {
        // Returns the cached object if there exists one.
        if (facing == Facing.BACK && mBackCameraCharacteristics != null) {
            return mBackCameraCharacteristics;
        } else if (facing == Facing.FRONT && mFrontCameraCharacteristics != null) {
            return mFrontCameraCharacteristics;
        }

        int cameraId = NO_DEVICE;
        if (facing == Facing.BACK) {
            cameraId = mFirstBackCameraId;
        } else if (facing == Facing.FRONT) {
            cameraId = mFirstFrontCameraId;
        }
        if (cameraId == NO_DEVICE) {
            throw new OneCameraAccessException(
                    "Unable to get camera characteristics (no camera id.)");
        }

        OneCameraCharacteristics characteristics;
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
            Camera.Parameters cameraParameters = camera.getParameters();
            if (cameraParameters == null) {
                Log.e(TAG, "Camera object returned null parameters!");
                throw new OneCameraAccessException("API1 Camera.getParameters() returned null");
            }
            characteristics = new OneCameraCharacteristicsImpl(
                    mCameraInfos[cameraId], cameraParameters);
        } finally {
            camera.release();
        }
        return characteristics;
    }
}