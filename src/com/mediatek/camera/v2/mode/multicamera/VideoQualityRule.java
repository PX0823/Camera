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
 * MediaTek Inc. (C) 2016. All rights reserved.
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
package com.mediatek.camera.v2.mode.multicamera;

import java.util.ArrayList;
import java.util.List;

import android.media.CamcorderProfile;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;
import com.mediatek.camera.v2.setting.ISettingRule;
import com.mediatek.camera.v2.setting.ISettingServant;
import com.mediatek.camera.v2.setting.SettingCtrl;
import com.mediatek.camera.v2.setting.SettingItem;
import com.mediatek.camera.v2.setting.SettingItem.Record;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;

/**
 * Because cross mount don't support 4k video quality,
 * so remove the 4k video quality in setting item.
 * the setting item will show high item.
 */
class VideoQualityRule implements ISettingRule {
    private static final Tag TAG = new Tag(VideoQualityRule.class.getSimpleName());
    private static final String CROSS_MOUNT_ON = "on";
    private static final String CROSS_MOUNT_OFF = "off";
    private static final int SUPPORTED_MAX_VIDEO_QULAITY_WIDTH = 1920;
    private final SettingCtrl mSettingCtrl;
    private ISettingServant mSettingServant;
    private String mCurrentCameraId = null;

    /**
     * Get the setting controller when create the rule.
     * @param settingCtrl the setting controller.
     */
    public VideoQualityRule(SettingCtrl settingCtrl) {
        mSettingCtrl = settingCtrl;
        mSettingServant = mSettingCtrl.getSettingServant(null);
    }

    @Override
    public void execute() {
        mCurrentCameraId = mSettingCtrl.getCurrentCameraId();
        String crossMountKeyValue = mSettingServant
                .getSettingValue(SettingKeys.KEY_MULTI_CAMERA_SINK);
        SettingItem qualityItem = mSettingServant.getSettingItem(SettingKeys.KEY_VIDEO_QUALITY);

        if (CROSS_MOUNT_ON.equalsIgnoreCase(crossMountKeyValue)) {
            List<String> supportedValues = getSmallerSupportedMaxVideoQualities();
            String currentQuality = mSettingServant.getSettingValue(SettingKeys.KEY_VIDEO_QUALITY);
            String quality = getQuality(currentQuality, supportedValues);
            qualityItem.setValue(quality);
            LogHelper.d(TAG, "enter cross mount set quality:" + quality);
            // update video quality setting UI.
            String overrideValue = null;
            if (supportedValues != null) {
                String[] values = new String[supportedValues.size()];
                overrideValue = Utils.buildEnableList(supportedValues.toArray(values));
                qualityItem.setOverrideValue(overrideValue);
            }
            Record record = qualityItem.new Record(quality, overrideValue);
            qualityItem.addOverrideRecord(SettingKeys.KEY_MULTI_CAMERA_SINK, record);
        } else if (CROSS_MOUNT_OFF.equalsIgnoreCase(crossMountKeyValue)) {
            int overrideCount = qualityItem.getOverrideCount();
            Record record = qualityItem.getOverrideRecord(SettingKeys.KEY_MULTI_CAMERA_SINK);
            if (record == null) {
                return;
            }
            qualityItem.removeOverrideRecord(SettingKeys.KEY_MULTI_CAMERA_SINK);
            overrideCount--;
            String quality = null;
            if (overrideCount > 0) {
                Record topRecord = qualityItem.getTopOverrideRecord();
                if (topRecord != null) {
                    quality = topRecord.getValue();
                    qualityItem.setValue(quality);
                    String overrideValue = topRecord.getOverrideValue();
                    qualityItem.setValue(quality);
                    qualityItem.setOverrideValue(overrideValue);
                }
            } else {
                quality = mSettingServant.getSharedPreferencesValue(SettingKeys.KEY_VIDEO_QUALITY);
                qualityItem.setOverrideValue(null);
                qualityItem.setValue(quality);
            }
            LogHelper.d(TAG, "exit cross mount set quality:" + quality + " overrideCount "
                    + overrideCount);
        }
    }

    @Override
    public void addLimitation(String condition, List<String> result) {
    }

    private List<String> getSmallerSupportedMaxVideoQualities() {
        LogHelper.d(TAG, "getSmallerSupportedMaxVideoQualities");
        ArrayList<String> supported = new ArrayList<String>();
        if (isSmallerSupportedMaxValue(Utils.VIDEO_QUALITY_FINE)) {
            supported.add(Integer.toString(Utils.VIDEO_QUALITY_FINE));
        }
        if (isSmallerSupportedMaxValue(Utils.VIDEO_QUALITY_HIGH)) {
            supported.add(Integer.toString(Utils.VIDEO_QUALITY_HIGH));
        }
        if (isSmallerSupportedMaxValue(Utils.VIDEO_QUALITY_MEDIUM)) {
            supported.add(Integer.toString(Utils.VIDEO_QUALITY_MEDIUM));
        }
        //Don't support the low video quality resolution.
//        if (isSmallerSupportedMaxValue(Utils.VIDEO_QUALITY_LOW)) {
//            supported.add(Integer.toString(Utils.VIDEO_QUALITY_LOW));
//        }
        int size = supported.size();
        if (size > 0) {
            return supported;
        }
        return null;
    }

    private boolean isSmallerSupportedMaxValue(int quality) {
        int cameraId = Integer.valueOf(mCurrentCameraId);
        if (CamcorderProfile.hasProfile(cameraId, quality)) {
            CamcorderProfile profile = Utils.getVideoProfile(cameraId, quality);
            return profile.videoFrameWidth <= SUPPORTED_MAX_VIDEO_QULAITY_WIDTH;
        }
        return false;
    }

    private String getQuality(String current, List<String> supportedList) {
        String supported = current;
        if (supportedList != null && !supportedList.contains(current)) {
            if (Integer.toString(Utils.VIDEO_QUALITY_FINE).equals(current)) {
                // match normal fine quality to high in cross mount mode
                supported = Integer.toString(Utils.VIDEO_QUALITY_HIGH);
            }
        }
        if (!supportedList.contains(supported)) {
            supported = supportedList.get(0);
        }
        return supported;
    }
}