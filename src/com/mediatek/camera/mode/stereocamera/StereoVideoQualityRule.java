/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.camera.mode.stereocamera;

import android.media.CamcorderProfile;

import com.mediatek.camcorder.CamcorderProfileEx;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.ISettingRule.MappingFinder;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.ParametersHelper;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingItem.Record;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;

import java.util.ArrayList;
import java.util.List;
/**
 * This class used for Stereo video quality rule.
 */
public class StereoVideoQualityRule implements ISettingRule {
    private static final String TAG = "StereoVideoQualityRule";
    private static final int QUALITY_WIDTH = 1920;
    private static final String VIDEO_QUALITY_LOW =
            Integer.toString(CamcorderProfileEx.QUALITY_LOW);
    private static final String VIDEO_QUALITY_MEDIUM =
            Integer.toString(CamcorderProfileEx.QUALITY_MEDIUM);
    private static final String VIDEO_QUALITY_HIGH =
            Integer.toString(CamcorderProfileEx.QUALITY_HIGH);
    private static final String VIDEO_QUALITY_FINE =
            Integer.toString(CamcorderProfileEx.QUALITY_FINE);
    private String mConditionKey = null;

    private ICameraDeviceManager mICameraDeviceManager;
    private List<String> mConditions = new ArrayList<String>();
    private List<List<String>> mResults = new ArrayList<List<String>>();
    private List<MappingFinder> mMappingFinder = new ArrayList<MappingFinder>();
    private ISettingCtrl mISettingCtrl;
    private ICameraContext mCameraContext;

    /**
     * create Stereo video quality rule.
     * @param cameraContext camera context instance.
     * @param conditionKey check mode condition.
     */
    public StereoVideoQualityRule(ICameraContext cameraContext, String conditionKey) {
        Log.i(TAG, "[StereoVideoQualityRule]constructor...");
        mCameraContext = cameraContext;
        mConditionKey = conditionKey;
        mICameraDeviceManager = cameraContext.getCameraDeviceManager();
    }

    @Override
    public void execute() {
        Log.i(TAG, "[execute]...");
        mISettingCtrl = mCameraContext.getSettingController();

        String conditionValue = mISettingCtrl.getSettingValue(mConditionKey);
        int index = conditionSatisfied(conditionValue);
        Log.i(TAG, "[execute], mConditionKey:" + mConditionKey + ", index = " + index);
        SettingItem setting = mISettingCtrl.getSetting(SettingConstants.KEY_VIDEO_QUALITY);
        ListPreference pref = mISettingCtrl.getListPreference(SettingConstants.KEY_VIDEO_QUALITY);
        int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
        ICameraDevice cameraDevice = mICameraDeviceManager
                .getCameraDevice(currentCameraId);
        Parameters parameters = cameraDevice.getParameters();
        if (!ParametersHelper.isVsDofSupported(parameters)) {
            Log.i(TAG, "VsDof only support 1080p video");
            return;
        }
        if (index == -1) {
            int overrideCount = setting.getOverrideCount();
            Record record = setting.getOverrideRecord(mConditionKey);
            if (record == null) {
                return;
            }
            setting.removeOverrideRecord(mConditionKey);
            overrideCount--;
            String quality = null;
            if (overrideCount > 0) {
                Record topRecord = setting.getTopOverrideRecord();
                if (topRecord != null) {
                    quality = topRecord.getValue();
                    setting.setValue(quality);
                    String overrideValue = topRecord.getOverrideValue();
                    setting.setValue(quality);
                    if (pref != null) {
                        pref.setOverrideValue(overrideValue);
                    }
                }
            } else {
                if (pref != null) {
                    quality = pref.getValue();
                    pref.setOverrideValue(null);
                }
                setting.setValue(quality);
            }
            Log.i(TAG, "set quality:" + quality);
        } else {
            // override video quality and write value to setting.
            List<String> supportedValues = getSupportedVideoQualities();
            String currentQuality = setting.getValue();
            String quality = getQuality(currentQuality, supportedValues);
            setting.setValue(quality);
            Log.i(TAG, "set quality:" + quality);

            // update video quality setting ui.
            if (pref != null) {
                pref.setOverrideValue(quality);
            }

            Record record = setting.new Record(quality, quality);
            setting.addOverrideRecord(mConditionKey, record);
        }
    }

    @Override
    public void addLimitation(String condition, List<String> result,
            MappingFinder mappingFinder) {
        Log.i(TAG, "[addLimitation]condition = " + condition);
        mConditions.add(condition);
        mResults.add(result);
        mMappingFinder.add(mappingFinder);
    }

    private int conditionSatisfied(String conditionValue) {
        int index = mConditions.indexOf(conditionValue);
        return index;
    }

    private List<String> getSupportedVideoQualities() {
        Log.i(TAG, "getSupportedVideoQualities");
        ArrayList<String> supported = new ArrayList<String>();
        if (checkSatisfyVideoQuality(CamcorderProfileEx.QUALITY_FINE)) {
            supported.add(VIDEO_QUALITY_FINE);
        }
        if (checkSatisfyVideoQuality(CamcorderProfileEx.QUALITY_HIGH)) {
            supported.add(VIDEO_QUALITY_HIGH);
        }
        if (checkSatisfyVideoQuality(CamcorderProfileEx.QUALITY_MEDIUM)) {
            supported.add(VIDEO_QUALITY_MEDIUM);
        }
        if (checkSatisfyVideoQuality(CamcorderProfileEx.QUALITY_LOW)) {
            supported.add(VIDEO_QUALITY_LOW);
        }
        int size = supported.size();
        if (size > 0) {
            return supported;
        }
        return null;
    }

    private boolean checkSatisfyVideoQuality(int quality) {
        int cameraId = mICameraDeviceManager.getCurrentCameraId();
        if (CamcorderProfile.hasProfile(cameraId, quality)) {
            CamcorderProfile profile = CamcorderProfileEx.getProfile(cameraId, quality);
            return profile.videoFrameWidth == QUALITY_WIDTH;
        }
        return false;
    }

    private String getQuality(String current, List<String> supportedList) {
        String supported = current;
        if (supportedList != null && !supportedList.isEmpty()) {
           supported = supportedList.get(0);
        }
        return supported;
    }
}