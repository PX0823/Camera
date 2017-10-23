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

package com.mediatek.camera.mode.stereocamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.MetadataCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.Size;
import android.view.MotionEvent;

import com.android.camera.R;
import com.mediatek.camera.ICameraAddition.AdditionActionType;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraMode.CameraModeType;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.mode.PhotoMode;
import com.mediatek.camera.platform.ICameraAppUi.GestureListener;
import com.mediatek.camera.platform.ICameraAppUi.SpecViewType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.StereoDataCallback;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.StereoWarningCallback;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.platform.IFileSaver.OnFileSavedListener;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.ParametersHelper;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingItem.Record;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.preference.ListPreference;
import com.mediatek.camera.util.Log;
import com.mediatek.xmp.XmpOperator;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.os.Bundle;
import android.content.ComponentName;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.content.ContentResolver;
/**
 * This class used for Stereo Camera capture.
 */
public class StereoCameraMode extends PhotoMode implements StereoView.Listener {
    private static final String TAG = "StereoCameraMode";
    // Stereo Photo warning message
    private static final int DUAL_CAMERA_LENS_COVERED = 0;
    private static final int DUAL_CAMERA_LOW_LIGHT = 1;
    private static final int DUAL_CAMERA_TOO_CLOSE = 2;
    private static final int DUAL_CAMERA_READY = 3;
    private static final int PASS_NUM = 0;
    private static final int FAIL_NUM = 2;
    private static final int MSG_INIT_VIEW = 10003;
    private static final int MSG_WRITE_XMP = 10004;
    private static final int MSG_CONFIGURATION_CHANGED = 10005;
    private static final int TAG_REFOCUS_IMAGE = 1;
    private static final int TAG_NORAML_IMAGE = 0;
    private static final int VS_DOF_CALLBACK_NUM = 6;
    private static final int REFOCUS_CALLBACK_NUM = 3;
    private static final int TIME_MILLS = 1000;

    private static final String KEY_REFOCUS_PICTURE_SIZE = "refocus-picture-size";
    private static final String KEY_VS_DOF_LEVEL = "stereo-dof-level";
    private static final String REFOCUS_TAG = "refocus";
    private static final String SUBFFIX_JPG_TAG = ".jpg";
    private static final String SUBFFIX_DNG_TAG = ".dng";
    private static final String SUBFFIX_STEREO_TAG = "_STEREO";
    private static final String SUBFFIX_RAW_TAG = "_RAW";
    private static final String GEO_QUALITY = "Geometric Quality: ";
    private static final String PHO_QUALITY = "Photo Quality: ";
    private static final String PASS = "Pass";
    private static final String WARN = "Pass(warnning)";
    private static final String FAIL = "Fail";

    private boolean mIsDualCameraReady = true;
    private boolean mIsStereoCapture = true;
    private boolean mIsDngCapture = false;
    private final StereoPhotoDataCallback mStereoPhotoDataCallback = new StereoPhotoDataCallback();
    private final WarningCallback mStereoCameraWarningCallback = new WarningCallback();
    private final Handler mHandler;
    private final XmpOperator mOperator;
    private final SaveHandler mSaveHandler;

    private ICameraView mStereoView;
    private int mCurrentNum = 0;
    private byte[] mJpsData;
    private byte[] mMaskAndConfigData;
    private byte[] mDepthMap;
    private byte[] mClearImage;
    private byte[] mLdcData;
    private byte[] mOriginalJpegData;
    private byte[] mXmpJpegData;

    private long mRawPictureCallbackTime;
    private long mShutterCallbackTime;
    private Date mCaptureDate = new Date();
    private SimpleDateFormat mFormat;
    private String mImageName;
    private long mLastDate = 0;
    private int mSameSecondCount = 0;
    private Thread mWaitSavingDoneThread;

    /**
     * Create a stereo camera mode.
     * @param cameraContext camera context instance.
     */
    public StereoCameraMode(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[StereoCameraMode]constructor...");
        mHandler = new StereoPhotoHandler(mActivity.getMainLooper());
        mCameraCategory = new StereoPhotoCategory();
        mOperator = new XmpOperator();
        mFormat = new SimpleDateFormat(mActivity.getString(R.string.image_file_name_format),
                Locale.ENGLISH);
        HandlerThread ht = new HandlerThread("Stereo Save Handler Thread");
        ht.start();
        mSaveHandler = new SaveHandler(ht.getLooper());
        setRefocusSettingRules(cameraContext);
    }

    @Override
    public boolean open() {
        Log.i(TAG, "[openMode] ...");
        super.open();
        mHandler.sendEmptyMessage(MSG_INIT_VIEW);
        mICameraAppUi.setGestureListener(mStereoGestureListener);
        return true;
    }

    @Override
    public boolean close() {
        Log.i(TAG, "[closeMode]...");
        if (mICameraDevice != null && ParametersHelper.isVsDofSupported(
                mICameraDevice.getParameters())) {
            if (CameraModeType.EXT_MODE_VIDEO_STEREO != mIModuleCtrl.getNextMode()) {
                mStereoView.reset();
            }
            uninitStereoView();
        }
        mICameraAppUi.setGestureListener(null);
        mWaitSavingDoneThread = new WaitSavingDoneThread();
        mWaitSavingDoneThread.start();
        if (mSaveHandler != null) {
            mSaveHandler.getLooper().quit();
        }
        super.close();
        return true;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        if (type != ActionType.ACTION_ORITATION_CHANGED) {
            Log.i(TAG, "[execute]type = " + type);
        }
        switch (type) {
        case ACTION_SHUTTER_BUTTON_LONG_PRESS:
            mICameraAppUi.showInfo(mActivity.getString(R.string.accessibility_switch_to_dual_camera)
                            + mActivity.getString(R.string.camera_continuous_not_supported));
            break;

        case ACTION_ON_CAMERA_OPEN:
            updateDevice();
            mCameraClosed = false;
            mICameraDevice.setStereoWarningCallback(mStereoCameraWarningCallback);
            break;

        case ACTION_ON_START_PREVIEW:
            super.execute(type, arg);
            mHandler.sendEmptyMessage(MSG_INIT_VIEW);
            break;

        case ACTION_ON_CONFIGURATION_CHANGED:
            if (mHandler != null) {
                mHandler.sendEmptyMessage(MSG_CONFIGURATION_CHANGED);
            }
            break;

        case ACTION_FACE_DETECTED:
            break;

        default:
            return super.execute(type, arg);
        }

        return true;
    }
    @Override
    public void resume() {

    }
    @Override
    public void pause() {
    }

    @Override
    public void notifyVsDofLevel(String level) {
        setVsDofLevelParameter(level);
    }

    @Override
    protected PictureCallback getUncompressedImageCallback() {
        return null;
    }

    @Override
    public boolean capture() {
        Log.i(TAG, "capture()");
        mCurrentNum = 0;
        mIsStereoCapture = mIsDualCameraReady;
        if ("on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_DNG))) {
            mIFileSaver.setRawFlagEnabled(true);
            mIFileSaver.init(FILE_TYPE.RAW, 0, null, -1);
            mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
            mICameraDevice.setRawImageCallback(
                    mRawMetadataCallback, mRawPictureCallback);
            mIsDngCapture = true;
        } else {
            mIFileSaver.setRawFlagEnabled(false);
            mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
            mICameraDevice.setRawImageCallback(null, null);
            mIsDngCapture = false;
        }
        mCaptureStartTime = System.currentTimeMillis();
        mCaptureDate.setTime(mCaptureStartTime);
        mImageName = createName();
        mICameraAppUi.showRemaining();
        mCameraCategory.takePicture();
        setModeState(ModeState.STATE_CAPTURING);
        return true;
    }

    /**
     * This class used for Stereo view.
     */
    private class StereoPhotoHandler extends Handler {
        public StereoPhotoHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage]msg.what= " + msg.what);
            switch (msg.what) {
            case MSG_INIT_VIEW:
                if (mICameraDevice != null &&
                ParametersHelper.isVsDofSupported(mICameraDevice.getParameters())) {
                    initStereoView();
                }
				mICameraAppUi.showToast(R.string.dual_cam_tip_toast);
                break;
            case MSG_CONFIGURATION_CHANGED:
                // because configuration change,so need re-inflate the view
                // layout
                reInitStereoView();
                break;
            default:
                break;
            }
        }
    }

    /**
     * This class used for write jpeg to xmp and saving.
     */
    private class SaveHandler extends Handler {
        SaveHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {
            Log.i(TAG, "Save handleMessage msg.what = " + msg.what
                 + ", msg.obj = " + msg.obj);
            switch (msg.what) {
            case MSG_WRITE_XMP:
                StereoDataGroup mDataGroup = (StereoDataGroup) msg.obj;
                mXmpJpegData = mOperator.writeStereoCaptureInfoToJpg(
                        mDataGroup.getPictureName(),
                        mDataGroup.getOriginalJpegData(), mDataGroup.getJpsData(),
                        mDataGroup.getMaskAndConfigData(), mDataGroup.getClearImage(),
                        mDataGroup.getDepthMap(), mDataGroup.getLdcData());
                Log.d(TAG, "notifyMergeData mXmpJpegData: " + mXmpJpegData);
                if (mXmpJpegData != null) {
                    saveFile(mXmpJpegData, TAG_REFOCUS_IMAGE, mDataGroup.getPictureName());
                }
                break;
            default:
                break;
            }
        }
    }

    private OnFileSavedListener mFileSaverListener = new OnFileSavedListener() {
        @Override
        public void onFileSaved(Uri uri) {
            Log.d(TAG, "[onFileSaved]uri= " + uri);
			//need grant android.permission.READ_EXTERNAL_STORAGE permission
			boolean isDualCamCo = "on".equals(mISettingCtrl.getSettingValue(SettingConstants.KEY_DUAL_CAM_MODE));
			Log.d("xiayy", "isDualCamCo:"+isDualCamCo);
			if(!isDualCamCo){
				GotoGallery(uri);
			}
		}
    };
	private void GotoGallery(Uri uri){
		Log.d("xiayy", "<GotoGalleryRefocusActivity> uri:"+uri);

		String[] proj = {
        ImageColumns.TITLE,         // 1
        ImageColumns.MIME_TYPE,     // 2
        ImageColumns.DATA,          // 8
        ImageColumns.ORIENTATION,   // 9
        MediaColumns.WIDTH,         // 12
        MediaColumns.HEIGHT,        // 13
		ImageColumns.CAMERA_REFOCUS, //17
		};
        Cursor cursor = null;
        String fileName = null;
		String fileType = null;
		String filePath = null;
		int fileOri = -1;
		int fileWidth = -1;
		int fileHeight = -1;
		int depth_image = -1;

        cursor = mActivity.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) {
            return;
        }
        for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext())
		{
			int colummIndex1 = cursor.getColumnIndexOrThrow(ImageColumns.TITLE);
			int colummIndex2 = cursor.getColumnIndexOrThrow(ImageColumns.MIME_TYPE);
			int colummIndex3 = cursor.getColumnIndexOrThrow(ImageColumns.DATA);
			int colummIndex4 = cursor.getColumnIndexOrThrow(ImageColumns.ORIENTATION);
			int colummIndex5 = cursor.getColumnIndexOrThrow(MediaColumns.WIDTH);
			int colummIndex6 = cursor.getColumnIndexOrThrow(MediaColumns.HEIGHT);
			int colummIndex7 = cursor.getColumnIndexOrThrow(ImageColumns.CAMERA_REFOCUS);
			fileName = cursor.getString(colummIndex1);
			fileType = cursor.getString(colummIndex2);
			filePath = cursor.getString(colummIndex3);
			fileOri = cursor.getInt(colummIndex4);
			fileWidth = cursor.getInt(colummIndex5);
			fileHeight = cursor.getInt(colummIndex6);
			depth_image = cursor.getInt(colummIndex7);
			
		}
        Log.d("xiayy","depth_image:"+depth_image+"--fileName:"+fileName+ "--fileType:"+fileType+ "--filePath:"+filePath+ "--fileOri:"+fileOri+ "--fileWidth:"+fileWidth+ "--fileHeight:"+fileHeight);

        if (cursor != null) {
            cursor.close();
        }
		if(depth_image == 1){
			Intent intent = new Intent("com.android.gallery3d.action.REFOCUS");
			intent.setComponent(new ComponentName("com.android.gallery3d", "com.mediatek.galleryfeature.stereo.refocus.RefocusActivity"));
			intent.setDataAndType(uri, fileType);
			Bundle bundle = new Bundle();
			bundle.putInt("image-width", fileWidth);
			bundle.putInt("image-height", fileHeight);
			bundle.putInt("image-orientation",fileOri);
			bundle.putString("image-name", fileName);
			bundle.putString("image-path", filePath);
			intent.putExtras(bundle);
			mActivity.startActivity(intent);
			//mActivity.sendBroadcast(intent);
		}
	}
	
    public static String dcsMimeType;
    public static int dcsImageWidth;
    public static int dcsImageHeight;
    public static int dcsImageOrientation;
    public static String dcsImageName;
    public static String dcsImagePath;
    public void launchRefocusActivity(Uri uri) {
        Intent intent = new Intent("com.android.gallery3d.action.REFOCUS");
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        intent.setDataAndType(uri, dcsMimeType);
        Bundle bundle = new Bundle();
        bundle.putInt("image-width", dcsImageWidth);
        bundle.putInt("image-height", dcsImageHeight);
        bundle.putInt("image-orientation", dcsImageOrientation);
        bundle.putString("image-name", dcsImageName);
        bundle.putString("image-path", dcsImagePath);
        intent.putExtras(bundle);
        mActivity.startActivity(intent);
    }

    /**
     * This class used for wait file saving done.
     */
    private class WaitSavingDoneThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "[WaitSavingDoneThread]wait");
            mIFileSaver.waitDone();
            Log.d(TAG, "[WaitSavingDoneThread]waitDone!");
        }
    }

    private void saveFile(byte[] data, int refocus, String fileName) {
        Log.i(TAG, "[saveFile]...");
        Location location = mIModuleCtrl.getLocation();
        mIFileSaver.savePhotoFile(data, fileName, mCaptureStartTime, location, refocus,
                mFileSaverListener);
    }

    private void setRefocusSettingRules(ICameraContext cameraContext) {
        Log.d(TAG, "[setRefocusSettingRules]...");
        StereoPreviewSizeRule previewSizeRule = new StereoPreviewSizeRule(cameraContext);
        previewSizeRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_REFOCUS,
                SettingConstants.KEY_PICTURE_RATIO, previewSizeRule);
        StereoPictureSizeRule pictureSizeRule = new StereoPictureSizeRule(cameraContext);
        pictureSizeRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_REFOCUS,
                SettingConstants.KEY_PICTURE_SIZE, pictureSizeRule);
        StereoZsdRule zsdRule = new StereoZsdRule(cameraContext);
        zsdRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_REFOCUS,
                SettingConstants.KEY_CAMERA_ZSD, zsdRule);
        StereoVideoQualityRule videoQualityRule = new StereoVideoQualityRule(
                cameraContext, SettingConstants.KEY_REFOCUS);
        videoQualityRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_REFOCUS,
                SettingConstants.KEY_VIDEO_QUALITY, videoQualityRule);
    }

    /**
     * This class used for Stereo data callback.
     */
    private class StereoPhotoDataCallback implements StereoDataCallback {
        public void onJpsCapture(byte[] jpsData) {
if (jpsData == null) {
                Log.w(TAG, "JPS data is null");
                return;
            }
            Log.i(TAG, "onJpsCapture jpsData:" + jpsData.length);
            mJpsData = jpsData;
            notifyMergeData();
        }
        public void onMaskCapture(byte[] maskData) {
            if (maskData == null) {
                Log.w(TAG, "Mask data is null");
                return;
            }
            Log.i(TAG, "onMaskCapture maskData:" + maskData.length);
            mMaskAndConfigData = maskData;
            mOperator.setJsonBuffer(mMaskAndConfigData);
            showQualityStatus(mOperator.getGeoVerifyLevel(), mOperator.getPhoVerifyLevel());
            notifyMergeData();
        }
        public void onDepthMapCapture(byte[] depthData) {
            if (depthData == null) {
                Log.w(TAG, "depth data is null");
                return;
            }
            Log.i(TAG, "onDepthMapCapture depthData:" + depthData.length);
            mDepthMap = depthData;
            notifyMergeData();
        }
        public void onClearImageCapture(byte[] clearImageData) {
            if (clearImageData == null) {
                Log.w(TAG, " clearImage data is null");
                return;
            }
            Log.i(TAG, "onClearImageCapture clearImageData:" + clearImageData.length);
            mClearImage = clearImageData;
            notifyMergeData();
        }
        public void onLdcCapture(byte[] ldcData) {
            if (ldcData == null) {
                Log.w(TAG, " ldc data is null");
                return;
            }
            Log.i(TAG, "onLdcCapture ldcData:" + ldcData.length);
            mLdcData = ldcData;
            notifyMergeData();
        }
    }

    private void notifyMergeData() {
        Log.i(TAG, "notifyMergeData mCurrentNum = " + mCurrentNum);
        mCurrentNum++;
        if (ParametersHelper.isVsDofSupported(mICameraDevice.getParameters())) {
            if (mCurrentNum == VS_DOF_CALLBACK_NUM) {
                Log.i(TAG, "notifyMergeData Vs Dof");
                restartPreview(true);
                if (mIsStereoCapture) {
                    String dofName = generateName(SUBFFIX_STEREO_TAG);
                    StereoDataGroup mDataGroup = new StereoDataGroup(dofName,
                            mOriginalJpegData, mJpsData, mMaskAndConfigData,
                            mDepthMap, mClearImage, mLdcData);
                    mSaveHandler.obtainMessage(MSG_WRITE_XMP, mDataGroup).sendToTarget();
                }
                mCurrentNum = 0;
            }
        } else {
           if (mCurrentNum == REFOCUS_CALLBACK_NUM) {
                Log.i(TAG, "notifyMergeData refocus");
                restartPreview(true);
                if (mIsStereoCapture) {
                    String refocusMame = generateName(SUBFFIX_STEREO_TAG);
                    mXmpJpegData = mOperator.writeStereoCaptureInfoToJpg(
                            refocusMame,
                            mOriginalJpegData, mJpsData, mMaskAndConfigData,
                            null, null, null);
                    if (mXmpJpegData != null) {
                        saveFile(mXmpJpegData, TAG_REFOCUS_IMAGE, refocusMame);
                    }
                }
                mCurrentNum = 0;
            }
        }
    }

    /**
     * This class used for Stereo warning callback.
     */
    private class WarningCallback implements StereoWarningCallback {
        public void onWarning(int type) {
            Log.i(TAG, "onWarning type = " + type);
            switch (type) {
            case DUAL_CAMERA_LOW_LIGHT:
                mICameraAppUi.showToast(R.string.dual_camera_lowlight_toast);
                mIsDualCameraReady = false;
                break;
            case DUAL_CAMERA_READY:
                mIsDualCameraReady = true;
                break;
            case DUAL_CAMERA_TOO_CLOSE:
                mICameraAppUi.showToast(R.string.dual_camera_too_close_toast);
                mIsDualCameraReady = false;
                break;
            case DUAL_CAMERA_LENS_COVERED:
                mICameraAppUi.showToast(R.string.dual_camera_lens_covered_toast);
                mIsDualCameraReady = false;
                break;
            default:
                Log.w(TAG, "Warning message don't need to show");
                break;
            }
        }
    };

    private final PictureCallback mJpegPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.d(TAG, "[mJpegPictureCallback]");
            if (mCameraClosed) {
                Log.w(TAG, "[mJpegPictureCallback] mCameraClosed:" + mCameraClosed);
                mICameraAppUi.setSwipeEnabled(true);
                mICameraAppUi.restoreViewState();
                return;
            }
            if (jpegData == null) {
                Log.w(TAG, "[mJpegPictureCallback] jpegData is null");
                mICameraAppUi.setSwipeEnabled(true);
                mICameraAppUi.restoreViewState();
                restartPreview(false);
                return;
            }
            mOriginalJpegData = jpegData;
            mIFocusManager.updateFocusUI(); // Ensure focus indicator
            if (!mIsStereoCapture) {
                saveFile(mOriginalJpegData, TAG_NORAML_IMAGE, null);
            }
            notifyMergeData();
            Log.d(TAG, "[mJpegPictureCallback] end");
        }
    };

    private String generateName(String type) {
        String name = null;
        if (type == SUBFFIX_RAW_TAG) {
            if (mICameraDevice != null
                && ParametersHelper.isVsDofSupported(mICameraDevice
                           .getParameters())) {
                if (mIsStereoCapture) {
                    name = mImageName + SUBFFIX_STEREO_TAG + SUBFFIX_RAW_TAG
                            + SUBFFIX_DNG_TAG;
                }
            }
        } else {
            if (mICameraDevice != null
                    && ParametersHelper.isVsDofSupported(mICameraDevice
                        .getParameters())) {
                if (mIsDngCapture && mIsStereoCapture) {
                    name = mImageName + SUBFFIX_STEREO_TAG + SUBFFIX_RAW_TAG
                            + SUBFFIX_JPG_TAG;
                } else if (!mIsDngCapture && mIsStereoCapture) {
                    name = mImageName + SUBFFIX_STEREO_TAG + SUBFFIX_JPG_TAG;
                }
             }
        }
        Log.i(TAG, "generateName type = " + type + ", name = " + name);
        return name;
    }

    private final ShutterCallback mShutterCallback = new ShutterCallback() {
        @Override
        public void onShutter() {
            mShutterCallbackTime = System.currentTimeMillis();
            long shutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.d(TAG, "[mShutterCallback] mShutterLag = " + shutterLag + "ms");
            Log.d(TAG, "[mShutterCallback]");
        }
    };

    private final PictureCallback mRawPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.d(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
            if (data == null) {
                Log.d(TAG, "[mRawPictureCallback] data is null ");
                return;
            }
            mDngHelper.setRawdata(data);
            getDngImageAndSaved(generateName(SUBFFIX_RAW_TAG));
        }
    };
    private final MetadataCallback mRawMetadataCallback = new MetadataCallback() {
        public void onMetadataReceived(CaptureResult result,
                CameraCharacteristics characteristic) {
            if (result == null || characteristic == null) {
                Log.w(TAG, "onMetadataReceived, invalid callback value, return null");
                return;
            }
            mDngHelper.setMetadata(result, characteristic);
            getDngImageAndSaved(generateName(SUBFFIX_RAW_TAG));
        }
    };

    /**
     * This class used for Stereo photo take picture.
     */
    class StereoPhotoCategory extends CameraCategory {
        public StereoPhotoCategory() {
        }

        public void takePicture() {
            mAdditionManager.execute(AdditionActionType.ACTION_TAKEN_PICTURE);
            mICameraDevice.setStereoDataCallback(mStereoPhotoDataCallback);
            mICameraDevice.getParameters().setRefocusJpsFileName(REFOCUS_TAG);
            mICameraDevice.takePicture(mShutterCallback, null, null, mJpegPictureCallback);
            mICameraAppUi.setViewState(ViewState.VIEW_STATE_CAPTURE);
        }
    }

    /**
     * This class used for Stereo preview size rule.
     */
    private class StereoPreviewSizeRule implements ISettingRule {
        private SettingItem mCurrentSettingItem;
        private ICameraContext mCameraContext;
        private ISettingCtrl mISettingCtrl;
        private SettingItem mPictureRatioSetting;

        public StereoPreviewSizeRule(ICameraContext cameraContext) {
            mCameraContext = cameraContext;
        }
        @Override
        public void execute() {
            mISettingCtrl = mCameraContext.getSettingController();
            mCurrentSettingItem = mISettingCtrl.getSetting(SettingConstants.KEY_REFOCUS);
            mPictureRatioSetting = mISettingCtrl
                    .getSetting(SettingConstants.KEY_PICTURE_RATIO);
            String resultValue = mPictureRatioSetting.getValue();
            String currentValue = mCurrentSettingItem.getValue();
            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            ICameraDevice cameraDevice = mICameraDeviceManager
                    .getCameraDevice(currentCameraId);
            Parameters parameters = cameraDevice.getParameters();
            ListPreference pref = mPictureRatioSetting.getListPreference();
            if ("on".equals(currentValue)) {
                String overrideValue = "1.7778";
                resultValue = "1.7778";
                if (mPictureRatioSetting.isEnable()) {
                    mPictureRatioSetting.setValue(resultValue);
                    if (pref != null) {
                         pref.setOverrideValue(overrideValue, true);
                    }
                    SettingUtils.setPreviewSize(mCameraContext.getActivity(),
                            parameters, overrideValue);
                }

                Record record = mPictureRatioSetting.new Record(resultValue, overrideValue);
                mPictureRatioSetting.addOverrideRecord(SettingConstants.KEY_REFOCUS, record);
            } else {
                mPictureRatioSetting.removeOverrideRecord(SettingConstants.KEY_REFOCUS);
                int count = mPictureRatioSetting.getOverrideCount();
                if (count > 0) {
                    Record topRecord = mPictureRatioSetting.getTopOverrideRecord();

                    if (topRecord != null) {
                        String value = topRecord.getValue();
                        String overrideValue = topRecord.getOverrideValue();
                        mPictureRatioSetting.setValue(value);
                        pref = mPictureRatioSetting.getListPreference();
                        if (pref != null) {
                             pref.setOverrideValue(overrideValue);
                        }
                    }
                } else {
                    pref = mPictureRatioSetting.getListPreference();
                    if (pref != null) {
                        pref.setOverrideValue(null);
                   }
                    mPictureRatioSetting.setValue(resultValue);
                    SettingUtils.setPreviewSize(mICameraContext.getActivity(),
                            parameters, resultValue);
                }
            }
        }

        @Override
        public void addLimitation(String condition, List<String> result,
                MappingFinder mappingFinder) {
            Log.i(TAG, "[addLimitation]condition = " + condition);
        }
    }

    /**
     * This class used for Stereo picture size rule.
     */
    private class StereoPictureSizeRule implements ISettingRule {
        private SettingItem mCurrentSettingItem;
        private ICameraContext mCameraContext;
        private ISettingCtrl mISettingCtrl;
        private SettingItem mPictureSize;

        public StereoPictureSizeRule(ICameraContext cameraContext) {
            mCameraContext = cameraContext;
        }
        @Override
        public void execute() {
            mISettingCtrl = mCameraContext.getSettingController();
            mCurrentSettingItem = mISettingCtrl
                    .getSetting(SettingConstants.KEY_REFOCUS);
            mPictureSize = mISettingCtrl
                    .getSetting(SettingConstants.KEY_PICTURE_SIZE);
            String resultValue = mPictureSize.getValue();
            String currentValue = mCurrentSettingItem.getValue();
            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            ICameraDevice cameraDevice = mICameraDeviceManager
                    .getCameraDevice(currentCameraId);
            Parameters parameters = cameraDevice.getParameters();
            ListPreference pref = mPictureSize.getListPreference();			
            if ("on".equals(currentValue)) {
				//500W 16:9 3072x1728
				//500W 4:3 2560x1920
                String overrideValue = "3072x1728";//parameters.get(KEY_REFOCUS_PICTURE_SIZE);
                resultValue = "3072x1728";	
				mPictureSize.setEnable(true);				
                if (mPictureSize.isEnable()) {
                    mPictureSize.setValue(resultValue);
                    if (pref != null) {
                         pref.setOverrideValue(overrideValue, true);
                    }
                }
				mPictureSize.setEnable(false);
                ParametersHelper.setParametersValue(parameters, currentCameraId,
                        SettingConstants.KEY_PICTURE_SIZE, resultValue);
                Record record = mPictureSize.new Record(resultValue, overrideValue);
                mPictureSize.addOverrideRecord(SettingConstants.KEY_REFOCUS, record);
            } else {
                mPictureSize.removeOverrideRecord(SettingConstants.KEY_REFOCUS);
                int count = mPictureSize.getOverrideCount();
                if (count > 0) {
                    Record topRecord = mPictureSize.getTopOverrideRecord();

                    if (topRecord != null) {
                        String value = topRecord.getValue();
                        String overrideValue = topRecord.getOverrideValue();
                        mPictureSize.setValue(value);
                        pref = mPictureSize.getListPreference();
                        if (pref != null) {
                             pref.setOverrideValue(overrideValue);
                        }
                    }
                } else {
                    pref = mPictureSize.getListPreference();
                    if (pref != null) {
                        pref.setOverrideValue(null);
                   }
                    mPictureSize.setValue(resultValue);
                }
            }
        }
        @Override
        public void addLimitation(String condition, List<String> result,
                MappingFinder mappingFinder) {
            Log.i(TAG, "[addLimitation]condition = " + condition);
        }
    }

    /**
     * This class used for Stereo Zsd rule.
     */
    private class StereoZsdRule implements ISettingRule {
        private SettingItem mCurrentSettingItem;
        private ICameraContext mCameraContext;
        private ISettingCtrl mISettingCtrl;
        private SettingItem mZsdItem;

        public StereoZsdRule(ICameraContext cameraContext) {
            mCameraContext = cameraContext;
        }
        @Override
        public void execute() {
            mISettingCtrl = mCameraContext.getSettingController();
            mCurrentSettingItem = mISettingCtrl
                    .getSetting(SettingConstants.KEY_REFOCUS);
            mZsdItem = mISettingCtrl
                    .getSetting(SettingConstants.KEY_CAMERA_ZSD);
            String resultValue = mZsdItem.getValue();
            String currentValue = mCurrentSettingItem.getValue();
            int currentCameraId = mICameraDeviceManager.getCurrentCameraId();
            ICameraDevice cameraDevice = mICameraDeviceManager
                    .getCameraDevice(currentCameraId);
            Parameters parameters = cameraDevice.getParameters();
            ListPreference pref = mZsdItem.getListPreference();
            if ("on".equals(currentValue)) {
                String overrideValue = "on";
                resultValue = overrideValue;
                if (mZsdItem.isEnable()) {
                    mZsdItem.setValue(resultValue);
                    if (pref != null) {
                         pref.setOverrideValue(overrideValue, true);
                    }
                }
                Record record = mZsdItem.new Record(resultValue, overrideValue);
                mZsdItem.addOverrideRecord(SettingConstants.KEY_REFOCUS, record);
            } else {
                mZsdItem.removeOverrideRecord(SettingConstants.KEY_REFOCUS);
                int count = mZsdItem.getOverrideCount();
                if (count > 0) {
                    Record topRecord = mZsdItem.getTopOverrideRecord();

                    if (topRecord != null) {
                        String value = topRecord.getValue();
                        String overrideValue = topRecord.getOverrideValue();
                        mZsdItem.setValue(value);
                        pref = mZsdItem.getListPreference();
                        if (pref != null) {
                             pref.setOverrideValue(overrideValue);
                        }
                    }
                } else {
                    pref = mZsdItem.getListPreference();
                    if (pref != null) {
                        pref.setOverrideValue(null);
                        resultValue = pref.getValue();
                   }
                    mZsdItem.setValue(resultValue);
                }
            }
            parameters.set("zsd-mode", resultValue);
        }
        @Override
        public void addLimitation(String condition, List<String> result,
                MappingFinder mappingFinder) {
            Log.d(TAG, "[addLimitation]condition = " + condition);
        }
    }

    private void showQualityStatus(int geoFlag, int photoFlag) {
        if (!isDebugOpened()) {
            return;
        }
        String msg = null;
        msg = formateShow(geoFlag, photoFlag);
        DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        new AlertDialog.Builder(mActivity).setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon).setTitle("")
                .setMessage(msg)
                .setNeutralButton(R.string.dialog_ok, buttonListener)
                .show();
    }

    private String formateShow(int geoFlag, int photoFlag) {
        Log.i(TAG, "geoFlag = " + geoFlag +  "photoFlag = " + photoFlag);
        String geo = null;
        String photo = null;
        if (geoFlag == PASS_NUM) {
            geo = PASS;
        } else if (geoFlag == FAIL_NUM) {
            geo = FAIL;
        } else {
            geo = WARN;
        }
        if (photoFlag == PASS_NUM) {
            photo = PASS;
        } else if (photoFlag == FAIL_NUM) {
            photo = FAIL;
        } else {
            photo = WARN;
        }
        return GEO_QUALITY + geo + "\n" + PHO_QUALITY + photo;
    }

    private boolean isDebugOpened() {
        boolean enable = SystemProperties
                .getInt("debug.STEREO.enable_verify", 0) == 1 ? true : false;
        Log.d(TAG, "[isDebugOpened]return :" + enable);
        return enable;
    }

    private void initStereoView() {
        if (mStereoView == null) {
            mStereoView = mICameraAppUi.getCameraView(SpecViewType.MODE_STEREO);
            mStereoView.init(mActivity, mICameraAppUi, mIModuleCtrl);
            mStereoView.setListener(this);
            mStereoView.show();			
        } else {
            mStereoView.refresh();
        }				
    }

    private void reInitStereoView() {
        if (mStereoView != null) {
            mStereoView.uninit();
            mStereoView = mICameraAppUi.getCameraView(SpecViewType.MODE_STEREO);
            mStereoView.init(mActivity, mICameraAppUi, mIModuleCtrl);
            mStereoView.setListener(this);
            mStereoView.show();
        }
    }

    private void uninitStereoView() {
        if (mStereoView != null) {
            mStereoView.uninit();
        }
    }

    private void setVsDofLevelParameter(String level) {
        Log.i(TAG, "[setVsDofLevelParameter] level = " + level);
        mICameraDevice.setParameter(KEY_VS_DOF_LEVEL, level);
        mICameraDevice.applyParameters();
    }

    private String createName() {
        String result = mFormat.format(mCaptureDate);
        // If the last name was generated for the same second,
        // we append _1, _2, etc to the name.
        long captureTime = mCaptureStartTime;
        if (captureTime / TIME_MILLS == mLastDate / TIME_MILLS) {
            mSameSecondCount++;
            result += "_" + mSameSecondCount;
        } else {
            mLastDate = captureTime;
            mSameSecondCount = 0;
        }
        Log.i(TAG, "[createName] result = " + result);
        return result;
    }

    private GestureListener mStereoGestureListener = new GestureListener() {

        @Override
        public boolean onDown(float x, float y, int width, int height) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(float x, float y) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(float x, float y) {
            return true;
        }

        @Override
        public boolean onUp() {
            return true;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            return true;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            return true;
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            return true;
        }

        @Override
        public boolean onLongPress(float x, float y) {
            return false;
        }
    };
}
