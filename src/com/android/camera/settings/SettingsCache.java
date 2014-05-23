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

package com.android.camera.settings;

import android.content.Context;
import android.util.SparseArray;

import com.android.camera.settings.SettingsManager.Setting;
import com.android.camera.settings.SettingsManager.SettingsCapabilities;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsCache class caches Settings descriptors and also maintains
 * an efficient mapping between SharedPreferences String keys and Setting id.
 */
public class SettingsCache {
    /** A mapping of setting id to {@link #SettingsManager.Setting}. */
    private final SparseArray<Setting> mCache = new SparseArray<Setting>();

    /** The max index of a non-null element in the cache. */
    private int mCacheMaxIndex = -1;

    /** A mapping of SharedPreference key to setting id. */
    private static Map<String, Integer> mKeyMap = new HashMap<String, Integer>();

    /** An interface for adding new settings ids and setting
     *  initialization to the cache.  This is only appropriate
     *  for settings not known to this package.
     */
    private final ExtraSettings mExtraSettings;

    /** The activity context, used by setting initializers to
     *  to lookup a setting's default values[
     */
    private final Context mContext;

    /** The interface through which settings can know
     *  limitations set by the camera device.
     */
    private SettingsCapabilities mCapabilities;

    /**
     * A cache which manages on demand initialization and efficient
     * lookup of {@link #SettingsManager.Settings}s.
     */
    public SettingsCache(Context context, ExtraSettings extraSettings) {
        mContext = context;
        mExtraSettings = extraSettings;

        initKeyMap();
        mExtraSettings.upgradeKeyMap(mKeyMap);
    }

    /**
     * Sets the SettingsCapabilities to enable creation of
     * camera dependent settings.
     */
    public void setCapabilities(SettingsCapabilities capabilities) {
        mCapabilities = capabilities;
    }

    /**
     * ExtraSettings provides an interface for extra settings to be
     * added to the cache.
     */
    public interface ExtraSettings {
        /**
         * Add additional key to id mappings to the key map.
         */
        public void upgradeKeyMap(Map<String, Integer> map);

        /**
         * Generate additional Settings from ids.  Must return null
         * by default.
         */
        public Setting settingFromId(int id, Context context);
    }

    private void initKeyMap() {
        mKeyMap.put(SettingsManager.KEY_RECORD_LOCATION, SettingsManager.SETTING_RECORD_LOCATION);
        mKeyMap.put(SettingsManager.KEY_VIDEO_QUALITY_BACK, SettingsManager.SETTING_VIDEO_QUALITY_BACK);
        mKeyMap.put(SettingsManager.KEY_VIDEO_QUALITY_FRONT, SettingsManager.SETTING_VIDEO_QUALITY_FRONT);
        mKeyMap.put(SettingsManager.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL,
                SettingsManager.SETTING_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        mKeyMap.put(SettingsManager.KEY_PICTURE_SIZE_BACK, SettingsManager.SETTING_PICTURE_SIZE_BACK);
        mKeyMap.put(SettingsManager.KEY_PICTURE_SIZE_FRONT, SettingsManager.SETTING_PICTURE_SIZE_FRONT);
        mKeyMap.put(SettingsManager.KEY_JPEG_QUALITY, SettingsManager.SETTING_JPEG_QUALITY);
        mKeyMap.put(SettingsManager.KEY_FOCUS_MODE, SettingsManager.SETTING_FOCUS_MODE);
        mKeyMap.put(SettingsManager.KEY_FLASH_MODE, SettingsManager.SETTING_FLASH_MODE);
        mKeyMap.put(SettingsManager.KEY_VIDEOCAMERA_FLASH_MODE,
                SettingsManager.SETTING_VIDEOCAMERA_FLASH_MODE);
        mKeyMap.put(SettingsManager.KEY_SCENE_MODE, SettingsManager.SETTING_SCENE_MODE);
        mKeyMap.put(SettingsManager.KEY_EXPOSURE, SettingsManager.SETTING_EXPOSURE_COMPENSATION_VALUE);
        mKeyMap.put(SettingsManager.KEY_VIDEO_EFFECT, SettingsManager.SETTING_VIDEO_EFFECT);
        mKeyMap.put(SettingsManager.KEY_CAMERA_ID, SettingsManager.SETTING_CAMERA_ID);
        mKeyMap.put(SettingsManager.KEY_CAMERA_HDR, SettingsManager.SETTING_CAMERA_HDR);
        mKeyMap.put(SettingsManager.KEY_CAMERA_HDR_PLUS, SettingsManager.SETTING_CAMERA_HDR_PLUS);
        mKeyMap.put(SettingsManager.KEY_CAMERA_FIRST_USE_HINT_SHOWN,
                SettingsManager.SETTING_CAMERA_FIRST_USE_HINT_SHOWN);
        mKeyMap.put(SettingsManager.KEY_VIDEO_FIRST_USE_HINT_SHOWN,
                SettingsManager.SETTING_VIDEO_FIRST_USE_HINT_SHOWN);
        mKeyMap.put(SettingsManager.KEY_STARTUP_MODULE_INDEX,
                SettingsManager.SETTING_STARTUP_MODULE_INDEX);
        mKeyMap.put(SettingsManager.KEY_CAMERA_MODULE_LAST_USED,
                SettingsManager.SETTING_KEY_CAMERA_MODULE_LAST_USED_INDEX);
        mKeyMap.put(SettingsManager.KEY_CAMERA_PANO_ORIENTATION,
                SettingsManager.SETTING_CAMERA_PANO_ORIENTATION);
        mKeyMap.put(SettingsManager.KEY_CAMERA_GRID_LINES,
                SettingsManager.SETTING_CAMERA_GRID_LINES);
        mKeyMap.put(SettingsManager.KEY_RELEASE_DIALOG_LAST_SHOWN_VERSION,
                SettingsManager.SETTING_RELEASE_DIALOG_LAST_SHOWN_VERSION);
        mKeyMap.put(SettingsManager.KEY_FLASH_SUPPORTED_BACK_CAMERA,
                SettingsManager.SETTING_FLASH_SUPPORTED_BACK_CAMERA);
        mKeyMap.put(SettingsManager.KEY_STRICT_UPGRADE_VERSION,
                SettingsManager.SETTING_STRICT_UPGRADE_VERSION);
        mKeyMap.put(SettingsManager.KEY_REQUEST_RETURN_HDR_PLUS,
                SettingsManager.SETTING_REQUEST_RETURN_HDR_PLUS);
        mKeyMap.put(SettingsManager.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING,
                SettingsManager.SETTING_SHOULD_SHOW_REFOCUS_VIEWER_CLING);
        mKeyMap.put(SettingsManager.KEY_EXPOSURE_COMPENSATION_ENABLED,
                SettingsManager.SETTING_EXPOSURE_COMPENSATION_ENABLED);
        mKeyMap.put(SettingsManager.KEY_USER_SELECTED_ASPECT_RATIO,
                SettingsManager.SETTING_USER_SELECTED_ASPECT_RATIO);
        mKeyMap.put(SettingsManager.KEY_COUNTDOWN_DURATION,
                SettingsManager.SETTING_COUNTDOWN_DURATION);
        mKeyMap.put(SettingsManager.KEY_HDR_PLUS_FLASH_MODE,
                SettingsManager.SETTING_HDR_PLUS_FLASH_MODE);
        mKeyMap.put(SettingsManager.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING,
                SettingsManager.SETTING_SHOULD_SHOW_SETTINGS_BUTTON_CLING);
    }

    /**
     * Gets a pre-initialized Settings descriptor from the SettingsCache
     * if the same descriptor has already been used in this session.
     * Otherwise allocates a new Settings descriptor and caches it.
     */
    public Setting get(int id) {
        Setting setting = mCache.get(id);
        if (setting == null) {
            setting = settingFromId(id);
            mCache.put(id, setting);

            if (id > mCacheMaxIndex) {
                mCacheMaxIndex = id;
            }
        }
        return setting;
    }

    /**
     * Efficiently convert a SharedPreference String key to a Settings
     * descriptor id.
     */
    public Integer getId(String key) {
        return mKeyMap.get(key);
    }

    private Setting settingFromId(int id) {
        switch (id) {
            case SettingsManager.SETTING_RECORD_LOCATION:
                return SettingsManager.getLocationSetting(mContext);
            case SettingsManager.SETTING_VIDEO_QUALITY_BACK:
                return SettingsManager.getVideoQualityBackSetting(mContext);
            case SettingsManager.SETTING_VIDEO_QUALITY_FRONT:
                return SettingsManager.getVideoQualityFrontSetting(mContext);
            case SettingsManager.SETTING_VIDEO_TIME_LAPSE_FRAME_INTERVAL:
                return SettingsManager.getTimeLapseFrameIntervalSetting(mContext);
            case SettingsManager.SETTING_PICTURE_SIZE_BACK:
                return SettingsManager.getPictureSizeBackSetting(mContext);
            case SettingsManager.SETTING_PICTURE_SIZE_FRONT:
                return SettingsManager.getPictureSizeFrontSetting(mContext);
            case SettingsManager.SETTING_JPEG_QUALITY:
                return SettingsManager.getJpegQualitySetting(mContext);
            case SettingsManager.SETTING_FOCUS_MODE:
                return SettingsManager.getFocusModeSetting(mContext);
            case SettingsManager.SETTING_FLASH_MODE:
                return SettingsManager.getFlashSetting(mContext);
            case SettingsManager.SETTING_VIDEOCAMERA_FLASH_MODE:
                return SettingsManager.getVideoFlashSetting(mContext);
            case SettingsManager.SETTING_SCENE_MODE:
                return SettingsManager.getSceneModeSetting(mContext);
            case SettingsManager.SETTING_EXPOSURE_COMPENSATION_VALUE:
                return SettingsManager.getExposureSetting(mContext, mCapabilities);
            case SettingsManager.SETTING_VIDEO_EFFECT:
                return SettingsManager.getVideoEffectSetting(mContext);
            case SettingsManager.SETTING_CAMERA_ID:
                return SettingsManager.getDefaultCameraIdSetting(mContext);
            case SettingsManager.SETTING_CAMERA_HDR:
                return SettingsManager.getHdrSetting(mContext);
            case SettingsManager.SETTING_CAMERA_HDR_PLUS:
                return SettingsManager.getHdrPlusSetting(mContext);
            case SettingsManager.SETTING_CAMERA_FIRST_USE_HINT_SHOWN:
                return SettingsManager.getHintSetting(mContext);
            case SettingsManager.SETTING_VIDEO_FIRST_USE_HINT_SHOWN:
                return SettingsManager.getHintVideoSetting(mContext);
            case SettingsManager.SETTING_STARTUP_MODULE_INDEX:
                return SettingsManager.getStartupModuleSetting(mContext);
            case SettingsManager.SETTING_KEY_CAMERA_MODULE_LAST_USED_INDEX:
                return SettingsManager.getLastUsedCameraModule(mContext);
            case SettingsManager.SETTING_CAMERA_PANO_ORIENTATION:
                return SettingsManager.getPanoOrientationSetting(mContext);
            case SettingsManager.SETTING_CAMERA_GRID_LINES:
                return SettingsManager.getGridLinesSetting(mContext);
            case SettingsManager.SETTING_RELEASE_DIALOG_LAST_SHOWN_VERSION:
                return SettingsManager.getReleaseDialogLastShownVersionSetting(mContext);
            case SettingsManager.SETTING_FLASH_SUPPORTED_BACK_CAMERA:
                return SettingsManager.getFlashSupportedBackCameraSetting(mContext);
            case SettingsManager.SETTING_STRICT_UPGRADE_VERSION:
                return SettingsManager.getStrictUpgradeVersionSetting(mContext);
            case SettingsManager.SETTING_REQUEST_RETURN_HDR_PLUS:
                return SettingsManager.getRequestReturnHdrPlusSetting(mContext);
            case SettingsManager.SETTING_SHOULD_SHOW_REFOCUS_VIEWER_CLING:
                return SettingsManager.getShouldShowRefocusViewerCling(mContext);
            case SettingsManager.SETTING_EXPOSURE_COMPENSATION_ENABLED:
                return SettingsManager.getManualExposureCompensationSetting(mContext);
            case SettingsManager.SETTING_USER_SELECTED_ASPECT_RATIO:
                return SettingsManager.getUserSelectedAspectRatioSetting(mContext);
            case SettingsManager.SETTING_COUNTDOWN_DURATION:
                return SettingsManager.getCountDownDurationSetting(mContext);
            case SettingsManager.SETTING_HDR_PLUS_FLASH_MODE:
                return SettingsManager.getHdrPlusFlashSetting(mContext);
            case SettingsManager.SETTING_SHOULD_SHOW_SETTINGS_BUTTON_CLING:
                return SettingsManager.getShouldShowSettingsButtonCling(mContext);
            default:
                return mExtraSettings.settingFromId(id, mContext);
        }
    }

    /**
     * Flush Settings with flush value {@link #SettingsManager.FLUSH_ON}.
     * This should be called every time the camera device changes.
     */
    public void flush() {
        for (int i = 0; i <= mCacheMaxIndex; i++) {
            Setting setting = mCache.get(i);
            if (setting != null && setting.isFlushedOnCameraChanged()) {
                mCache.delete(i);
            }
        }
        resetCacheMaxIndex();
    }

    private void resetCacheMaxIndex() {
        for (int i = mCacheMaxIndex; i >= 0; i--) {
            Setting setting = mCache.get(i);
            if (setting == null) {
                mCacheMaxIndex--;
            } else {
                return;
            }
        }
    }
}
