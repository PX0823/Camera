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
 * MediaTek Inc. (C) 2014. All rights reserved.
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
package com.android.camera.bridge;

import android.hardware.Camera.Area;
import android.hardware.Camera.Size;

import com.android.camera.CameraManager;
import com.android.camera.Log;

import com.mediatek.camera.platform.Parameters;

import java.util.ArrayList;
import java.util.List;

public class ParametersExt implements Parameters {

    private final static String TAG = ParametersExt.class.getSimpleName();

    private final CameraManager.CameraProxy mCameraDevice;
    private android.hardware.Camera.Parameters mParameters;

    public ParametersExt(CameraManager.CameraProxy cameraDevice,
            android.hardware.Camera.Parameters parameters) {
        mCameraDevice = cameraDevice;
        mParameters = parameters;
    }

    public void setparameters(android.hardware.Camera.Parameters parameters) {
        mParameters = parameters;
    }

    @Override
    public void set(final String key, final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.set(key, value);
            }
        });
    }

    @Override
    public void set(final String key, final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.set(key, value);
            }
        });
    }

    @Override
    public String get(String key) {
        String value = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.get(key);
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public int getInt(String key) {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getInt(key);
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setPreviewSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewSize(width, height);
            }
        });
    }

    @Override
    public Size getPreviewSize() {
        Size size = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                size = mParameters.getPreviewSize();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        Log.d(TAG, "getPreviewSize, size:" + size);
        return size;
    }

    @Override
    public List<Size> getSupportedPreviewSizes() {
        return mParameters.getSupportedPreviewSizes();
    }

    @Override
    public List<Size> getSupportedVideoSizes() {
        return mParameters.getSupportedVideoSizes();
    }

    @Override
    public Size getPreferredPreviewSizeForVideo() {
        return mParameters.getPreferredPreviewSizeForVideo();
    }

    @Override
    public Size getPreferredPreviewSizeForSlowMotionVideo() {
        return mParameters.getPreferredPreviewSizeForSlowMotionVideo();
    }

    @Override
    public List<Size> getSupportedSlowMotionVideoSizes() {
        return mParameters.getSupportedSlowMotionVideoSizes();
    }

    @Override
    public void setJpegThumbnailSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegThumbnailSize(width, height);
            }
        });
    }

    @Override
    public Size getJpegThumbnailSize() {
        Size size = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                size = mParameters.getJpegThumbnailSize();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        Log.d(TAG, "getJpegThumbnailSize, size:" + size);
        return size;
    }

    @Override
    public List<Size> getSupportedJpegThumbnailSizes() {
        return mParameters.getSupportedJpegThumbnailSizes();
    }

    @Override
    public void setJpegThumbnailQuality(final int quality) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegThumbnailQuality(quality);
            }
        });
    }

    @Override
    public int getJpegThumbnailQuality() {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getJpegThumbnailQuality();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setJpegQuality(final int quality) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setJpegQuality(quality);
            }
        });
    }

    @Override
    public int getJpegQuality() {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getJpegQuality();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setPreviewFrameRate(final int fps) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFrameRate(fps);
            }
        });
    }

    @Override
    public int getPreviewFrameRate() {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getPreviewFrameRate();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public List<Integer> getSupportedPreviewFrameRates() {
        return mParameters.getSupportedPreviewFrameRates();
    }

    @Override
    public void setPreviewFpsRange(final int min, final int max) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFpsRange(min, max);
            }
        });
    }

    @Override
    public void getPreviewFpsRange(int[] range) {
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mParameters.getPreviewFpsRange(range);
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
    }

    @Override
    public List<int[]> getSupportedPreviewFpsRange() {
        return mParameters.getSupportedPreviewFpsRange();
    }

    @Override
    public void setPreviewFormat(final int pixelFormat) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPreviewFormat(pixelFormat);
            }
        });
    }

    @Override
    public int getPreviewFormat() {
        int format = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                format = mParameters.getPreviewFormat();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return format;
    }

    @Override
    public List<Integer> getSupportedPreviewFormats() {
        return mParameters.getSupportedPreviewFormats();
    }

    @Override
    public void setPictureSize(final int width, final int height) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPictureSize(width, height);
            }
        });
    }

    @Override
    public Size getPictureSize() {
        Size size = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                size = mParameters.getPictureSize();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        Log.d(TAG, "getPictureSize, size:" + size);
        return size;
    }

    @Override
    public List<Size> getSupportedPictureSizes() {
        return mParameters.getSupportedPictureSizes();
    }

    @Override
    public void setPictureFormat(final int pixelFormat) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setPictureFormat(pixelFormat);
            }
        });
    }

    @Override
    public int getPictureFormat() {
        int format = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                format = mParameters.getPictureFormat();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return format;
    }

    @Override
    public List<Integer> getSupportedPictureFormats() {
        return mParameters.getSupportedPictureFormats();
    }

    @Override
    public void setRotation(final int rotation) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRotation(rotation);
            }
        });
    }

    @Override
    public void setGpsLatitude(final double latitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsLatitude(latitude);
            }
        });
    }

    @Override
    public void setGpsLongitude(final double longitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsLongitude(longitude);
            }
        });
    }

    @Override
    public void setGpsAltitude(final double altitude) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsAltitude(altitude);
            }
        });
    }

    @Override
    public void setGpsTimestamp(final long timestamp) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsTimestamp(timestamp);
            }
        });

    }

    @Override
    public void setGpsProcessingMethod(final String processingMethod) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setGpsProcessingMethod(processingMethod);
            }
        });
    }

    @Override
    public void removeGpsData() {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.removeGpsData();
            }
        });
    }

    @Override
    public String getWhiteBalance() {
        String value = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getWhiteBalance();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setWhiteBalance(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setWhiteBalance(value);
            }
        });
    }

    @Override
    public List<String> getSupportedWhiteBalance() {
        return mParameters.getSupportedWhiteBalance();
    }

    @Override
    public String getColorEffect() {
        String value = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getColorEffect();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setColorEffect(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setColorEffect(value);
            }
        });
    }

    @Override
    public List<String> getSupportedColorEffects() {
        return mParameters.getSupportedColorEffects();
    }

    @Override
    public String getAntibanding() {
        String value = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getAntibanding();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setAntibanding(final String antibanding) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAntibanding(antibanding);
            }
        });
    }

    @Override
    public List<String> getSupportedAntibanding() {
        return mParameters.getSupportedAntibanding();
    }

    @Override
    public String getEisMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getEisMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setEisMode(final String eis) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEisMode(eis);
            }
        });
    }

    @Override
    public List<String> getSupportedEisMode() {
        return mParameters.getSupportedEisMode();
    }

    @Override
    public String getAFLampMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getAFLampMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setAFLampMode(final String aflamp) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAFLampMode(aflamp);
            }
        });
    }

    @Override
    public List<String> getSupportedAFLampMode() {
        return mParameters.getSupportedAFLampMode();
    }

    @Override
    public String getSceneMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getSceneMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setSceneMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setSceneMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedSceneModes() {
        return mParameters.getSupportedSceneModes();
    }

    @Override
    public String getFlashMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getFlashMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setFlashMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFlashMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFlashModes() {
        return mParameters.getSupportedFlashModes();
    }

    @Override
    public String getFocusMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getFocusMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setFocusMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFocusModes() {
        return mParameters.getSupportedFocusModes();
    }

    @Override
    public float getFocalLength() {
        float value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getFocalLength();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public float getHorizontalViewAngle() {
        float value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getHorizontalViewAngle();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public float getVerticalViewAngle() {
        float value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getVerticalViewAngle();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public int getExposureCompensation() {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getExposureCompensation();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setExposureCompensation(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setExposureCompensation(value);
            }
        });
    }

    @Override
    public int getMaxExposureCompensation() {
        return mParameters.getMaxExposureCompensation();
    }

    @Override
    public int getMinExposureCompensation() {
        return mParameters.getMinExposureCompensation();
    }

    @Override
    public float getExposureCompensationStep() {
        return mParameters.getExposureCompensationStep();
    }

    @Override
    public void setAutoExposureLock(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAutoExposureLock(toggle);
            }
        });
    }

    @Override
    public boolean getAutoExposureLock() {
        boolean lock = false;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                lock = mParameters.getAutoExposureLock();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return lock;
    }

    @Override
    public boolean isAutoExposureLockSupported() {
        return mParameters.isAutoExposureLockSupported();
    }

    @Override
    public void setAutoWhiteBalanceLock(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setAutoWhiteBalanceLock(toggle);
            }
        });
    }

    @Override
    public boolean getAutoWhiteBalanceLock() {
        boolean lock = false;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                lock = mParameters.getAutoWhiteBalanceLock();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return lock;
    }

    @Override
    public boolean isAutoWhiteBalanceLockSupported() {
        return mParameters.isAutoWhiteBalanceLockSupported();
    }

    @Override
    public int getZoom() {
        int value = 0;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getZoom();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return value;
    }

    @Override
    public void setZoom(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setZoom(value);
            }
        });
    }

    @Override
    public boolean isZoomSupported() {
        return mParameters.isZoomSupported();
    }

    @Override
    public int getMaxZoom() {
        return mParameters.getMaxZoom();
    }

    @Override
    public List<Integer> getZoomRatios() {
        return mParameters.getZoomRatios();
    }

    @Override
    public boolean isSmoothZoomSupported() {
        return mParameters.isSmoothZoomSupported();
    }

    @Override
    public void setCameraMode(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCameraMode(value);
            }
        });
    }

    @Override
    public String getISOSpeed() {
        String value = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                value = mParameters.getISOSpeed();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }

        return value;
    }

    @Override
    public void setISOSpeed(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setISOSpeed(value);
            }
        });
    }

    @Override
    public List<String> getSupportedISOSpeed() {
        return mParameters.getSupportedISOSpeed();
    }

    @Override
    public int getMaxNumDetectedObjects() {
        return mParameters.getMaxNumDetectedObjects();
    }

    @Override
    public String getFDMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getFDMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setFDMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFDMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedFDMode() {
        return mParameters.getSupportedFDMode();
    }

    @Override
    public String getEdgeMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getEdgeMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setEdgeMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setEdgeMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedEdgeMode() {
        return mParameters.getSupportedEdgeMode();
    }

    @Override
    public String getHueMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getHueMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setHueMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setHueMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedHueMode() {
        return mParameters.getSupportedHueMode();
    }

    @Override
    public String getSaturationMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getSaturationMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setSaturationMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setSaturationMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedSaturationMode() {
        return mParameters.getSupportedSaturationMode();
    }

    @Override
    public String getBrightnessMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getBrightnessMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setBrightnessMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setBrightnessMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedBrightnessMode() {
        return mParameters.getSupportedBrightnessMode();
    }

    @Override
    public String getContrastMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getContrastMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setContrastMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setContrastMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedContrastMode() {
        return mParameters.getSupportedContrastMode();
    }

    @Override
    public String getCaptureMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParameters() not successfull.", ex);
            } finally {
                mode = mParameters.getCaptureMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setCaptureMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCaptureMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedCaptureMode() {
        return mParameters.getSupportedCaptureMode();
    }

    @Override
    public void setCapturePath(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setCapturePath(value);
            }
        });
    }

    @Override
    public void setBurstShotNum(final int value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setBurstShotNum(value);
            }
        });
    }

    @Override
    public void setExposureMeterMode(final String mode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setExposureMeterMode(mode);
            }
        });
    }

    @Override
    public String getExposureMeterMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                mode = mParameters.getExposureMeterMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setContinuousSpeedMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setContinuousSpeedMode(value);
            }
        });
    }

    @Override
    public String getZSDMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                mode = mParameters.getZSDMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setZSDMode(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setZSDMode(value);
            }
        });
    }

    @Override
    public List<String> getSupportedZSDMode() {
        return mParameters.getSupportedZSDMode();
    }

    @Override
    public void getFocusDistances(float[] output) {
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                mParameters.getFocusDistances(output);
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
    }

    @Override
    public List<Area> getFocusAreas() {
        List<Area> area = new ArrayList<Area>();
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                area = mParameters.getFocusAreas();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return area;
    }

    @Override
    public void setFocusAreas(final List<Area> focusAreas) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setFocusAreas(focusAreas);
            }
        });
    }

    @Override
    public int getMaxNumMeteringAreas() {
        return mParameters.getMaxNumMeteringAreas();
    }

    @Override
    public List<Area> getMeteringAreas() {
        List<Area> area = new ArrayList<Area>();
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                area = mParameters.getMeteringAreas();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return area;
    }

    @Override
    public void setMeteringAreas(final List<Area> meteringAreas) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setMeteringAreas(meteringAreas);
            }
        });
    }

    @Override
    public int getMaxNumDetectedFaces() {
        return mParameters.getMaxNumDetectedFaces();
    }

    @Override
    public void setRecordingHint(final boolean hint) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRecordingHint(hint);
            }
        });
    }

    @Override
    public boolean isVideoSnapshotSupported() {
        return mParameters.isVideoSnapshotSupported();
    }

    @Override
    public void enableRecordingSound(final String value) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.enableRecordingSound(value);
            }
        });
    }

    @Override
    public void setVideoStabilization(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setVideoStabilization(toggle);
            }
        });
    }

    @Override
    public boolean getVideoStabilization() {
        boolean toggle = false;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                toggle = mParameters.getVideoStabilization();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return toggle;
    }

    @Override
    public boolean isVideoStabilizationSupported() {
        return mParameters.isVideoStabilizationSupported();
    }

    @Override
    public List<Integer> getPIPFrameRateZSDOn() {
        return mParameters.getPIPFrameRateZSDOn();
    }

    @Override
    public List<Integer> getPIPFrameRateZSDOff() {
        return mParameters.getPIPFrameRateZSDOff();
    }

    @Override
    public boolean getDynamicFrameRate() {
        boolean toggle = false;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                toggle = mParameters.getDynamicFrameRate();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return toggle;
    }

    @Override
    public void setDynamicFrameRate(final boolean toggle) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDynamicFrameRate(toggle);
            }
        });
    }

    @Override
    public boolean isDynamicFrameRateSupported() {
        return mParameters.isDynamicFrameRateSupported();
    }

    @Override
    public void setRefocusJpsFileName(final String fineName) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRefocusJpsFileName(fineName);
            }
        });
    }

    @Override
    public String getDepthAFMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                mode = mParameters.getDepthAFMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public String getDistanceMode() {
        String mode = null;
        if (mCameraDevice != null) {
            boolean lockedParameters = false;
            try {
                mCameraDevice.lockParameters();
                lockedParameters = true;
            } catch (InterruptedException ex) {
                Log.e(TAG, "lockParametersRun() not successfull.", ex);
            } finally {
                mode = mParameters.getDistanceMode();
                if (lockedParameters) {
                    mCameraDevice.unlockParameters();
                }
            }
        }
        return mode;
    }

    @Override
    public void setDepthAFMode(final boolean isDepthAfMode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDepthAFMode(isDepthAfMode);
            }
        });
    }

    @Override
    public void setDistanceMode(final boolean isDistanceMode) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setDistanceMode(isDistanceMode);
            }
        });
    }

    /**
     * Set refocus mode.
     * @param isOpen True open refocus mode.
     */
    public void setRefocusMode(final boolean isOpen) {
        lockRun(new Runnable() {
            @Override
            public void run() {
                mParameters.setRefocusMode(isOpen);
            }
        });
    }

    private void lockRun(Runnable runnable) {
        if (mCameraDevice != null) {
            mCameraDevice.lockParametersRun(runnable);
        }
    }
}
