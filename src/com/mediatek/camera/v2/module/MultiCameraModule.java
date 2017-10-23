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
package com.mediatek.camera.v2.module;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.android.camera.R;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;
import com.mediatek.camera.v2.control.ControlImpl;
import com.mediatek.camera.v2.control.IControl.AutoFocusState;
import com.mediatek.camera.v2.control.IControl.FocusStateListener;
import com.mediatek.camera.v2.control.IControl.IAaaController;
import com.mediatek.camera.v2.detection.DetectionManager;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.platform.app.AppUi.ShutterEventsListener;
import com.mediatek.camera.v2.platform.device.CameraDeviceManager;
import com.mediatek.camera.v2.platform.device.CameraDeviceManager.CameraStateCallback;
import com.mediatek.camera.v2.platform.device.CameraDeviceProxy;
import com.mediatek.camera.v2.platform.device.CameraDeviceProxy.CameraSessionCallback;
import com.mediatek.camera.v2.platform.device.IMultiCameraDeviceAdapter;
import com.mediatek.camera.v2.platform.device.IMultiCameraDeviceAdapter.IDevicesStatusListener;
import com.mediatek.camera.v2.platform.device.IRemoteDevice;
import com.mediatek.camera.v2.setting.SettingCtrl;
import com.mediatek.camera.v2.stream.multicamera.RemoteTouchFocus;
import com.mediatek.camera.v2.stream.multicamera.ui.IMultiCameraViewManager.IMultiCameraViewListener;
import com.mediatek.camera.v2.stream.multicamera.ui.IMultiCameraViewManager.IStatusUpdateListener;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class control open and close camera,and configure session and request.
 */
public class MultiCameraModule extends AbstractCameraModule {

    private static final Tag TAG = new Tag(
            MultiCameraModule.class.getSimpleName());
    private static final boolean ONLY_SUPPORTE_ONE_REMOTE_CAMERA = true;
    private static final int ONLY_ONE_PREVIEW_CAMERA = 1;
    private static final int RESET_SHUTTER_BUTTON_ENABLE_MS = 1000;
    private static final String MULTI_PREVIEW_ON = "on";
    private static final String MULTI_PREVIEW_OFF = "off";

    private int mGSensorOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The activity is going to switch to the specified camera id. This is
    // needed because texture copy is done in GL thread. -1 means camera is not
    // switching.
    private int mPendingSwitchCameraId = UNKNOWN;

    // when in single remote preview and multi-preview, not show the setting
    // icon and picker manager UI
    private boolean mIsCanShowCommonUi = true;
    private boolean mIsLandScape = false;
    /**
     * if is capturing, mIsCapturing will be set to true.
     */
    private boolean mIsCapturing = false;
    /**
     * if is recording, mIsRecording will be set to true.
     */
    private boolean mIsRecording = false;

    private boolean mTapToFocusWaitForActiveScan = false;
    private boolean mShutterButtonIsReady = false;

    private String mLocalDeviceIdKeyPref = "camera_id_key_";

    private CameraDeviceManager mCameraManager;
    private CameraIdManager mCameraIdManager;
    // this handler used for deal with the open status callback
    private HandlerThread mCameraHandlerThread;
    private Handler mCameraHandler;
    // for open camera
    private HandlerThread mOpenCamThread;
    private Handler mOpenCameraHandler;
    // For reset the shutter to enable
    private Handler mMainHandler;

    private IMultiCameraDeviceAdapter mIMultiCameraDeviceAdapter;
    private IStatusUpdateListener mScanResultToViewListener;

    private RemoteTouchFocus mRemoteTouchFocus;

    private RequestType mCurrentRequestType = RequestType.PREVIEW;
    private RectF mPreviewArea;
    private FocusStateListenerMangaer mFocusStateListenerMangaer;
    private CameraSemaphoreCtrl mCameraSemaphoreCtrl;
    private LinkedHashMap<String, IRemoteDevice> mRemotedeviceInfo =
                    new LinkedHashMap<String, IRemoteDevice>();

    private ShutterEventsListener mPhotoShutterEventsListener = new ShutterEventsListener() {
        @Override
        public void onShutterReleased() {
            mCurrentMode.onShutterReleased(false);
        }
        @Override
        public void onShutterPressed() {
            mCurrentMode.onShutterPressed(false);
        }
        @Override
        public void onShutterLongPressed() {
            mCurrentMode.onShutterLongPressed(false);
        }
        @Override
        public void onShutterClicked() {

            if (!checkSatisfyCaptureCondition()) {
                return;
            }
            if (!mShutterButtonIsReady) {
                return;
            }
            final List<String> previewId = new ArrayList<String>();
            List<String> previewCameraId = mCameraIdManager.getPreviewCamera();
            previewId.addAll(previewCameraId);

            if (ONLY_ONE_PREVIEW_CAMERA < previewId.size()
                    || (ONLY_ONE_PREVIEW_CAMERA == previewId.size() && MultiCameraModuleUtil
                            .hasRemoteCamera(previewId))) {
                mCurrentMode.onShutterClicked(false);
            } else {
                String seflTimer = mSettingServant.getSettingValue(SettingKeys.KEY_SELF_TIMER);
                LogHelper.i(TAG, "seflTimer = " + seflTimer);
                int mTimerDuration = Integer.valueOf(seflTimer) / 1000;
                if (mTimerDuration > 0) {
                    switchCommonUiByCountingDown(true);
                    mAbstractModuleUI.setCountdownFinishedListener(MultiCameraModule.this);
                    mAbstractModuleUI.startCountdown(mTimerDuration);
                } else {
                    mCurrentMode.onShutterClicked(false);
                }
            }
        }
    };

    private ShutterEventsListener mVideoShutterEventsListener = new ShutterEventsListener() {
        @Override
        public void onShutterReleased() {
            mCurrentMode.onShutterReleased(true);
        }
        @Override
        public void onShutterPressed() {
            mCurrentMode.onShutterPressed(true);
        }
        @Override
        public void onShutterLongPressed() {
            mCurrentMode.onShutterLongPressed(true);
        }
        @Override
        public void onShutterClicked() {
            if (!mShutterButtonIsReady) {
                return;
            }
            mCurrentMode.onShutterClicked(true);
        }
    };

    /**
     * Instantiates a camera module for Multi camera.
     * @param app
     *            A controller which is App level.can notify lock orientation
     *            and get the UI.
     */
    public MultiCameraModule(AppController app) {
        super(app);
        mDetectionManager = new DetectionManager(app, this, null);
        mAaaControl = new ControlImpl(app, this, true, null);
        mRemoteTouchFocus = new RemoteTouchFocus(app.getServices().getSoundPlayback(), app,
                new RemoteAaaListener(), app.getCameraAppUi().getModuleLayoutRoot());
        mCameraIdManager = new CameraIdManager();
        mFocusStateListenerMangaer = new FocusStateListenerMangaer();
        mCameraSemaphoreCtrl = new CameraSemaphoreCtrl();
    }

    @Override
    public void open(Activity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.open(activity, isSecureCamera, isCaptureIntent);
        mAppUi.setShutterEventListener(mPhotoShutterEventsListener, false);
        mAppUi.setShutterEventListener(mVideoShutterEventsListener, true);
        mIsLandScape = MultiCameraModuleUtil.isLandScape(activity);
        mRemoteTouchFocus.open(null, null, false);
        getCameraStaticInfo(activity);
        mMainHandler = new Handler(activity.getMainLooper());

        // initialize module UI
        mAbstractModuleUI = new MultiCameraModuleUi(activity, this, mAppUi.getModuleLayoutRoot(),
                mStreamManager.getPreviewCallback(), new MultiCameraViewListenerImpl());

        // set the preview area changed listener
        mAppController.setModuleUiListener(mAbstractModuleUI);
        mAppController.addPreviewAreaSizeChangedListener(this);

        mIMultiCameraDeviceAdapter = mAppController.getMultiCameraDeviceAdapter();
        mIMultiCameraDeviceAdapter.regisiterStatusUpdateListener(new DevicesStatusListener());

        // Notify Module UI current Module is opened
        mAbstractModuleUI.open(activity, isSecureCamera, isCaptureIntent);
    }

    @Override
    public void resume() {
        LogHelper.i(TAG, "[resume]+");
        //Check camera id and device id,also need update the device proxy.
        checkCameraBeforeOpen();
        super.resume();
        //first set the shutter button disable.
        mShutterButtonIsReady = false;
        mMainHandler.removeCallbacks(mResetShutterEnableRunnable);
        mMainHandler.postDelayed(mResetShutterEnableRunnable, RESET_SHUTTER_BUTTON_ENABLE_MS);
        mCameraHandlerThread = new HandlerThread("Cross Mount Skin Camera");
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());

        mOpenCamThread = new HandlerThread("tmpThread_openCam");
        mOpenCamThread.start();
        mOpenCameraHandler = new Handler(mOpenCamThread.getLooper());

        for (String cameraId : mCameraIdManager.getBackUpPreviewCameraId()) {
            openCamera(cameraId);
        }
        mAbstractModuleUI.resume();
        mRemoteTouchFocus.resume();
        LogHelper.i(TAG, "[resume]-");
    }

    @Override
    public void pause() {
        LogHelper.i(TAG, "[pause]+");
        mPaused = true;
        mAbstractModuleUI.pause();
        closeCamera();
        super.pause();
        // Update the preview camera.
        mCameraIdManager.updateBackUpPreviewCameraId();
        // close have opened camera.
        mRemoteTouchFocus.pause();
        mMainHandler.removeCallbacks(mResetShutterEnableRunnable);
        LogHelper.i(TAG, "[pause]-");
    }

    @Override
    public void close() {
        LogHelper.i(TAG, "[close]+");
        super.close();
        //restore the UI when leave this module.
        mAppController.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAppUi.setAllCommonViewButShutterVisible(true);
            }
        });
        mAppController.removePreviewAreaSizeChangedListener(this);
        mAbstractModuleUI.close();
        mRemoteTouchFocus.close();
        updateMultiPreviewSetting(null);
        LogHelper.i(TAG, "[close]-");
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        super.onPreviewAreaChanged(previewArea);
        mPreviewArea = previewArea;
        mAbstractModuleUI.onPreviewAreaChanged(previewArea);
        mRemoteTouchFocus.onPreviewAreaChanged(previewArea);
    }

    @Override
    public void onSettingChanged(Map<String, String> result) {
        super.onSettingChanged(result);
        boolean cameraIdChanged = result != null && result.containsKey(SettingKeys.KEY_CAMERA_ID);
        if (cameraIdChanged) {
            String cameraId = result.get(SettingKeys.KEY_CAMERA_ID);
            if (cameraId != mCameraId) {
                mCameraId = cameraId;
                requestChangeCaptureRequets(false/* sync */, getRequestType(),
                        CaptureType.REPEATING_REQUEST);
            }
        }
    }

    @Override
    public void onCameraPicked(String newCameraId) {
        if (mPaused) {
            return;
        }
        super.onCameraPicked(newCameraId);
        mPendingSwitchCameraId = Integer.valueOf(newCameraId);
        switchCamera();
        mCurrentMode.switchCamera(newCameraId);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN || mPaused) {
            return;
        }
        int newOrientation = Utils.roundOrientation(orientation, mGSensorOrientation);
        if (mGSensorOrientation != newOrientation) {
            mGSensorOrientation = newOrientation;
        }
        mAbstractModuleUI.onOrientationChanged(mGSensorOrientation);
        mAaaControl.onOrientationChanged(mGSensorOrientation);
        mDetectionManager.onOrientationChanged(mGSensorOrientation);
        mCurrentMode.onOrientationChanged(mGSensorOrientation);
        mRemoteTouchFocus.setLocalOrientation(mGSensorOrientation);
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        LogHelper.d(TAG, "[onSingleTapUp]+++, x = " + x + ",y = " + y + ",mPreviewArea = "
                        + mPreviewArea + ",mIsLandScape = " + mIsLandScape);

        List<String> previewCameraId = mCameraIdManager.getPreviewCamera();
        if (!checkOnSingelTapUpCondition(previewCameraId)) {
            LogHelper.d(TAG, "onSingleTapUp,current condition is not ready");
            return false;
        }
        // First check need notify to remote touch focus
        if (ONLY_ONE_PREVIEW_CAMERA == previewCameraId.size()
                && MultiCameraModuleUtil.hasRemoteCamera(previewCameraId)) {
            mRemoteTouchFocus.onSingleTapUp(x, y, previewCameraId.get(0),
                    mCameraManager);
            LogHelper.d(TAG, "[onSingleTapUp] send the touch event to remote device");
            return true;
        }

        if (!super.onSingleTapUp(x, y)) {
            if (isNeedNotifyAAAManager(previewCameraId)) {
                mTapToFocusWaitForActiveScan = true;
                mAaaControl.onSingleTapUp(x, y);
                mDetectionManager.onSingleTapUp(x, y);
            } else {
                String clickedCamera = calculateClickedCameraId(x, y);
                previewAreaClicked(clickedCamera);
            }
            LogHelper.d(TAG, "[onSingleTapUp] is finished by module");
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongPress(float x, float y) {
        if (super.onLongPress(x, y)) {
            return true;
        }
        mDetectionManager.onLongPressed(x, y);
        // return true means this event is finished,not need transfer to others.
        return false;
    }

    @Override
    public void requestChangeCaptureRequets(boolean sync, RequestType requestType,
                    CaptureType captureType) {
        super.requestChangeCaptureRequets(sync, requestType, captureType);
        requestChangeCaptureRequets(true, sync, requestType, captureType);
    }

    @Override
    public void requestChangeCaptureRequets(boolean isMainCamera, boolean sync,
            RequestType requestType, CaptureType captureType) {
        List<String> previewCamera = mCameraIdManager.getPreviewCamera();
        if (RequestType.RECORDING == requestType
                || RequestType.PREVIEW == requestType) {
            mCurrentRequestType = requestType;
        }
        // Preview size maybe zero????
        for (int i = 0; i < previewCamera.size(); i++) {
            CameraDeviceProxy proxy = mCameraIdManager.getDeviceProxy().get(
                    previewCamera.get(i));
            if (proxy == null) {
                LogHelper.d(TAG,
                        "requestChangeCaptureRequets but camera is null!");
                continue;
            }
            proxy.requestChangeCaptureRequets(sync, requestType, captureType);
        }
    }

    @Override
    public void requestChangeSessionOutputs(boolean sync) {
        LogHelper.d(TAG, "[requestChangeSessionOutputs] sync = " + sync
                + ",previewCamera = " + mCameraIdManager.getPreviewCamera());
        requestChangeSessionOutputs(sync, true);
    }

    @Override
    public void requestChangeSessionOutputs(boolean sync, boolean isMainCamera) {
        List<String> previewCamera = mCameraIdManager.getPreviewCamera();
        LogHelper.d(TAG, "requestChangeSessionOutputs,isMainCamera = "
                + isMainCamera + ",previewCameraId = " + previewCamera);
        for (int i = 0; i < previewCamera.size(); i++) {
            CameraDeviceProxy proxy = mCameraIdManager.getDeviceProxy().get(
                    previewCamera.get(i));
            if (proxy == null) {
                LogHelper.d(TAG,
                        "requestChangeSessionOutputs but camera is null!");
                continue;
            }
            proxy.requestChangeSessionOutputs(sync);
        }
    }

    @Override
    public IAaaController get3AController(String cameraId) {
        // TODO
        if (SettingCtrl.BACK_CAMERA.equals(cameraId)) {
            return mAaaControl;
        } else {
            return mAaaControl;
        }
    }

    @Override
    public void requestUpdateRecordingStatus(boolean isRecordingStated) {
        super.requestUpdateRecordingStatus(isRecordingStated);
        mIsCanShowCommonUi = !isRecordingStated;
        mIsRecording = isRecordingStated;
        mAbstractModuleUI.updateRecordingStatus(isRecordingStated);
    }

    @Override
    public void requestUpdateCaptureStatus(boolean isCapturing) {
        mIsCapturing = isCapturing;
        super.requestUpdateCaptureStatus(isCapturing);
        mAbstractModuleUI.updateCaptureStatus(isCapturing);
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        mIsLandScape = isLandscape;
        mAbstractModuleUI.onLayoutOrientationChanged(isLandscape);
    }

    @Override
    public RequestType getRepeatingRequestType() {
        return getRequestType();
    }

    private void openCamera(final String cameraId) {
        LogHelper.d(TAG, "[openCamera],cameraId = " + cameraId);
        mCurrentRequestType = RequestType.PREVIEW;
        mOpenCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCameraManager == null) {
                    throw new IllegalStateException("openCamera, but CameraManager is null!");
                }
                // first update the correct orientation to stream or remote touch focus.
                updateOrientation(cameraId);

                mCameraSemaphoreCtrl.acquireClock(cameraId);
                CameraDeviceCallback callback = new CameraDeviceCallback(cameraId);
                mCameraManager.openSync(cameraId, callback, callback, mCameraHandler);

                LogHelper.i(TAG, "[openCamera]------cameraId = " + cameraId);
            } // end of run
        });
    }

    private void closeCamera() {
        closeLocalCamera();
        doCloseRemoteCameras(false);
    }

    private void closeLocalCamera() {
        LogHelper.d(TAG, "closeLocalCamera +++");
        mCameraSemaphoreCtrl.acquireClock(mCameraId);
        LinkedHashMap<String, CameraDeviceProxy> deviceProxy = mCameraIdManager.getDeviceProxy();
        if (deviceProxy.get(mCameraId) != null) {
            deviceProxy.get(mCameraId).close();
            deviceProxy.remove(mCameraId);
            mFocusStateListenerMangaer.clearFocusStateListener(mCameraId);
        }
        mCameraSemaphoreCtrl.releaseClock(mCameraId);
        mCameraSemaphoreCtrl.removeSemphore(mCameraId);
    }

    private RequestType getRequestType() {
        RequestType requestType = RequestType.PREVIEW;
        if (mCurrentRequestType == RequestType.RECORDING) {
            requestType = RequestType.RECORDING;
        }
        return requestType;
    }

    private void getCameraStaticInfo(Activity activity) {
        mCameraManager = mAppController.getCameraManager();
        mCameraId = mSettingController.getCurrentCameraId();
        if (!mCameraIdManager.getBackUpPreviewCameraId().contains(mCameraId)) {
            mCameraIdManager.getBackUpPreviewCameraId().add(mCameraId);
        }
    }

    private void notifyCameraIdChanged(List<String> previewCameraId, boolean isRemoteCameraOpened) {
        LogHelper.d(TAG, "[notifyCameraIdChanged] previewCameraId = " + previewCameraId
                + ",isRemoteCameraOpened = " + isRemoteCameraOpened + ",hasOpenedCameraId = "
                + mCameraIdManager.getDeviceProxy().keySet());

        List<String> previewIdCopy = new ArrayList<String>();
        previewIdCopy.addAll(previewCameraId);

        List<String> openedIdCopy = new ArrayList<String>();
        openedIdCopy.addAll(mCameraIdManager.getDeviceProxy().keySet());
        updateMultiPreviewSetting(previewIdCopy);
        mCurrentMode.updatePreviewCamera(previewIdCopy, openedIdCopy);
        mAbstractModuleUI.updatePreviewCamera(previewIdCopy, isRemoteCameraOpened);

        // hide the settings icon and picker manager icons when remote camera is
        // previewing
        final List<String> previewId = new ArrayList<String>();
        previewId.addAll(previewCameraId);
        updateUiVisibility(previewId);
    }

    private boolean isRemoteCameraOpened() {
        boolean hasOpened = false;
        List<String> openedCameraId = new ArrayList<String>();
        openedCameraId.addAll(mCameraIdManager.getDeviceId().keySet());
        hasOpened = MultiCameraModuleUtil.hasRemoteCamera(openedCameraId);
        return hasOpened;
    }

    private void doCloseRemoteCameras(boolean openOtherCamera) {
        boolean willUpdatePreviewCamera = false;
        List<String> remoteSemaphoresId = mCameraSemaphoreCtrl.getRemoteSemaphoresId();
        int semaphoreSize = remoteSemaphoresId.size();
        for (String semaphoreId : remoteSemaphoresId) {
            LogHelper.d(TAG, "[doCloseRemoteCameras] semaphoreId = " + semaphoreId);
            mCameraSemaphoreCtrl.acquireClock(semaphoreId);

            LinkedHashMap<String, CameraDeviceProxy> deviceProxy = mCameraIdManager
                            .getDeviceProxy();
            if (!deviceProxy.isEmpty()) {
                List<String> deleteItem = new ArrayList<String>();
                for (Entry<String, CameraDeviceProxy> entry : deviceProxy.entrySet()) {
                    String cameraId = entry.getKey();
                    if (!MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                        // Close camera
                        CameraDeviceProxy proxy = deviceProxy.get(cameraId);
                        proxy.close();
                        deleteItem.add(cameraId);
                        mFocusStateListenerMangaer.clearFocusStateListener(cameraId);
                        // when camera is paused not need to update the preview
                        // camera ID, such as pause camera and reopen the camera,the
                        // camera UI need update correctly by the camera device id.
                        if (!mPaused) {
                            mCameraIdManager.updateCameraId(false, cameraId, null);
                        }
                        willUpdatePreviewCamera = true;
                    }
                }
                for (String cameraId : deleteItem) {
                    deviceProxy.remove(cameraId);
                    // TODO When remote camera is closed,and proxy is removed,
                    // Need notify to remote touch focus,the received this message,
                    // can cancel the AF,also can do clear the focus UI directly.
                    // mRemoteTouchFocus.cameraIsClosed(cameraId);
                }
                // DeviceId maybe lager than device proxy,so need check again.
                if (!mPaused) {
                    deleteItem.clear();
                    LinkedHashMap<String, String> deviceId = mCameraIdManager.getDeviceId();
                    for (Entry<String, String> entry : deviceId.entrySet()) {
                        String deviceCameraId = entry.getKey();
                        if (!(MultiCameraModuleUtil.isLocalCamera(deviceCameraId) || deviceProxy
                                        .keySet().contains(deviceCameraId))) {
                            deleteItem.add(deviceCameraId);
                        }
                    }
                    for (String cameraId : deleteItem) {
                        deviceId.remove(cameraId);
                        mCameraIdManager.updateCameraId(false, cameraId, null);
                        willUpdatePreviewCamera = true;
                    }
                }
                // if deviceId is not equals deviceProxy,deviceId maybe lager than deviceProxy.
                LogHelper.d(TAG, "[doCloseRemoteCameras], willUpdatePreviewCamera = "
                                + willUpdatePreviewCamera + ",mPaused = " + mPaused
                                + ",openOther = " + openOtherCamera);
                if (willUpdatePreviewCamera && !mPaused && !openOtherCamera) {
                    backtoLocalCameraPreview();
                }
            }
            mCameraSemaphoreCtrl.releaseClock(semaphoreId);
            mCameraSemaphoreCtrl.removeSemphore(semaphoreId);
        }
    }

    private void switchCamera() {
        closeLocalCamera();
        // update the preview camera
        mCameraIdManager.updateCameraId(false, mCameraId, null);
        mCameraId = String.valueOf(mPendingSwitchCameraId);
        openCamera(mCameraId);
    }

    private boolean isNeedNotifyAAAManager(List<String> currentPreivewCamera) {
        boolean isNeed = false;
        if (ONLY_ONE_PREVIEW_CAMERA == currentPreivewCamera.size()
                && MultiCameraModuleUtil.hasLocalCamera(currentPreivewCamera)) {
            isNeed = true;
        }
        return isNeed;
    }

    private void updateUiVisibility(final List<String> previewId) {
        mAppController.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ONLY_ONE_PREVIEW_CAMERA < previewId.size()
                        || (ONLY_ONE_PREVIEW_CAMERA == previewId.size() && MultiCameraModuleUtil
                                .hasRemoteCamera(previewId))) {
                    mAppUi.hideSettingUi();
                    mAppUi.hidePickerManagerUi();
                    mAppUi.hideIndicatorManagerUi();
                    // Hide App UI and do not show UI by other case.
                    mAppUi.stopShowCommonUI(true);
                    mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_v2);
                } else {
                    if (mIsCanShowCommonUi) {
                        mAppUi.showSettingUi();
                        mAppUi.showPickerManagerUi();
                        mAppUi.showIndicatorManagerUi();
                        mAppUi.stopShowCommonUI(false);
                        mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_video_v2);
                    }
                }
            }
        });
    }

    private boolean checkCameraId(String tagregId) {
        String[] allCameraId = mCameraManager.getCameraIdList();
        boolean isIdCorrect = MultiCameraModuleUtil.checkCameraId(tagregId,
                allCameraId);
        if (!isIdCorrect) {
            // update the camera manager, because current Camera Manager not
            // have updated
            allCameraId = mCameraManager.getCameraIdList();
            // re-check the camera id
            isIdCorrect = MultiCameraModuleUtil.checkCameraId(tagregId,
                    allCameraId);
        }
        return isIdCorrect;
    }

    private String calculateClickedCameraId(float x, float y) {
        String camera = null;
        List<String> remotePreviewCamera = new ArrayList<>();
        int width = (int) mPreviewArea.right;
        int height = (int) mPreviewArea.bottom;
        for (String cameraId : mCameraIdManager.getPreviewCamera()) {
            if (!MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                remotePreviewCamera.add(cameraId);
            }
        }
        // Because the activity orientation is locked,so when rotate the
        // device,the original point not changed
        // TODO when have more than one remote camera ,how to
        // calculate this
        if (mIsLandScape) {
            if (x < width / 2 && remotePreviewCamera.size() > 0) {
                camera = remotePreviewCamera.get(0);
            } else {
                camera = mCameraId;
            }
        } else {
            if (y < height / 2 && remotePreviewCamera.size() > 0) {
                camera = remotePreviewCamera.get(0);
            } else {
                camera = mCameraId;
            }
        }
        return camera;
    }

    private void previewAreaClicked(String cameraID) {
        List<String> singlePreviewId = new ArrayList<String>();
        singlePreviewId.add(cameraID);
        mCameraIdManager.updatePreviewCameraId(singlePreviewId);
        notifyCameraIdChanged(singlePreviewId, isRemoteCameraOpened());
        // when preview camera have changed,need update the shutter UI
        mAppUi.switchShutterButtonLayout(MultiCameraModuleUtil
                .canShowVideoButton(singlePreviewId) ? R.layout.camera_shutter_photo_video_v2
                : R.layout.camera_shutter_photo_v2);
    }

    private String getKeyByValue(Map<String, String> map , Object value) {
        String key = null;
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String tempkey = iterator.next();
            if (map.get(tempkey).equals(value)) {
                key = tempkey;
                break;
            }
        }
        return key;
    }

    private void updateMultiPreviewSetting(List<String> previewCameraId) {
        String multiPreviewValue = MULTI_PREVIEW_OFF;
        if (MultiCameraModuleUtil.hasRemoteCamera(previewCameraId)) {
            multiPreviewValue = MULTI_PREVIEW_ON;
        }
        mSettingServant.doSettingChange(SettingKeys.KEY_MULTI_CAMERA_MULTI_PREVIEW,
                multiPreviewValue, false);
    }

    private boolean checkOnSingelTapUpCondition(List<String> previewCameraId) {
        if (mIsCapturing || mPaused) {
            LogHelper.d(TAG, "checkOnSingelTapUpCondition,isCapturing = " + mIsCapturing
                            + ",mPaused = " + mPaused);
            return false;
        }

        if (mAbstractModuleUI.isCountingDown()) {
            return false;
        }

        //Check device proxy
        if (ONLY_ONE_PREVIEW_CAMERA == previewCameraId.size()
                        && mCameraIdManager.getDeviceProxy().get(previewCameraId.get(0)) == null) {
            LogHelper.d(TAG, "[onSingleTapUp],current device proxy is null");
            return false;
        }

        return true;
    }

    // check the camera is correct before open camera
    private void checkCameraBeforeOpen() {
        // First get the back up camera
        List<String> backUpPreviewCamera = mCameraIdManager.getBackUpPreviewCameraId();
        // Second,get the remote camera and filter is connect camera
        LinkedHashMap<String, IRemoteDevice> remoteConnectedCamera = mIMultiCameraDeviceAdapter
                        .queryRemoteDevices();
        // key-value is cameraId-deviceId
        LinkedHashMap<String, String> deviceId = mCameraIdManager.getDeviceId();
        List<String> deleteItem = new ArrayList<String>();
        int status = IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_UNKNOWN;
        LogHelper.d(TAG, "[checkCameraBeforeOpen],before check camera, the camera id :"
                        + backUpPreviewCamera);
        // back up camera maybe not equals remote camera,such as pause camera
        // and disconnect remote camera from cross mount setting.
        for (String needOpenCamera : mCameraIdManager.getDeviceId().keySet()) {
            if (!MultiCameraModuleUtil.isLocalCamera(needOpenCamera)) {
                if (remoteConnectedCamera.size() == 0) {
                    deleteItem.add(needOpenCamera);
                } else {
                    // get the device id according camera id from mDeviceId
                    String needCheckdeviceId = deviceId.get(needOpenCamera);
                    IRemoteDevice device = remoteConnectedCamera.get(needCheckdeviceId);
                    if (device != null) {
                        status = device.get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
                    }
                    // If the remote camera status is not connected,need remove it.
                    if (IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_CONNECTED != status) {
                        deleteItem.add(needOpenCamera);
                    }
                }
            }
        }
        // Update the back up preview camera id and update the camera id.
        for (String cameraId : deleteItem) {
            backUpPreviewCamera.remove(cameraId);
            updateCameraBeforeOpened(false, cameraId, null);
        }
        // if the camera id is null,need add the local camera into the preview list
        if (backUpPreviewCamera.size() == 0) {
            backUpPreviewCamera.add(mCameraId);
            updateCameraBeforeOpened(true, mCameraId, mLocalDeviceIdKeyPref + mCameraId);
        }

        LogHelper.d(TAG, "[checkCameraBeforeOpen],after check camera, the camera id :"
                        + backUpPreviewCamera);
    }

    private final Runnable mResetShutterEnableRunnable = new Runnable() {
        @Override
        public void run() {
            mShutterButtonIsReady = true;
        }
    };

    private void updateOrientation(String cameraId) {
        if (mRemotedeviceInfo != null) {
            IRemoteDevice device = mRemotedeviceInfo.get(mCameraIdManager.getDeviceId()
                            .get(cameraId));
            if (device != null) {
                int orientatio = device.get(IRemoteDevice.KEY_REMOTE_DEVICE_ORIENTATION);
                mCurrentMode.onServiceEventUpdate(
                                IMultiCameraDeviceAdapter.REMOTE_CAMERA_ORIENTATION_CHANGE,
                                orientatio, cameraId);
                if (!MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                    mRemoteTouchFocus.setRemoteOrientation(orientatio);
                }
            }
        }
    }

    private void backtoLocalCameraPreview() {
        LinkedHashMap<String, CameraDeviceProxy> proxy = mCameraIdManager.getDeviceProxy();
        LogHelper.d(TAG, "backtoLocalCameraPreview,proxy = " + proxy);
        // if the proxy is null,need reopen the camera
        if (mCameraIdManager.getDeviceProxy().size() == 0) {
            openCamera(mCameraId);
        } else {
            // notify back to local single preview
            ArrayList<String> localPreviewId = new ArrayList<String>();
            localPreviewId.add(mCameraId);
            // when remote camera has closed,need update the preview camera id.
            mCameraIdManager.updateCameraId(true, mCameraId, mLocalDeviceIdKeyPref + mCameraId);
            notifyCameraIdChanged(localPreviewId, isRemoteCameraOpened());
        }
    }

    private void checkRemoteSemaphoreFofPreview() {
        List<String> remoteSemaphoresId = mCameraSemaphoreCtrl.getRemoteSemaphoresId();
        int semaphoreSize = remoteSemaphoresId.size();
        LinkedHashMap<String, String> deviceId = mCameraIdManager.getDeviceId();
        LogHelper.d(TAG, "[doCloseRemoteCameras] Semaphores size = " + semaphoreSize
                        + ",deviceId = " + deviceId);
        if (semaphoreSize == 0 && deviceId != null) {
            // if remote semaphore is 0,means the camera not have opened.
            // so just need update the camera id.
            List<String> removeId = new ArrayList<>();
            for (Entry<String, String> entry : deviceId.entrySet()) {
                String deviceCameraId = entry.getKey();
                if (!MultiCameraModuleUtil.isLocalCamera(deviceCameraId)) {
                    removeId.add(deviceCameraId);
                }
            }
            // remove the camera id
            for (int i = 0; i < removeId.size(); i++) {
                mCameraIdManager.updateCameraId(false, removeId.get(i), null);
                // just when the list have loop finished, update the preview
                // camera id.
                if (i == removeId.size() - 1) {
                    notifyCameraIdChanged(mCameraIdManager.getPreviewCamera(), false);
                }
            }
        }
    }

    private void updateCameraBeforeOpened(boolean isAdd, String cameraId, String deviceId) {
        mCameraIdManager.updateCameraId(isAdd, cameraId, deviceId);

        List<String> previewIdCopy = new ArrayList<String>();
        previewIdCopy.addAll(mCameraIdManager.getPreviewCamera());
        List<String> openedIdCopy = new ArrayList<String>();
        openedIdCopy.addAll(mCameraIdManager.getDeviceProxy().keySet());
        mCurrentMode.updatePreviewCamera(previewIdCopy, openedIdCopy);
    }

    /**
     * An implement class which detected remote camera device status.will be
     * notified when remote camera is connected or disconnected.
     */
    private class DevicesStatusListener implements IDevicesStatusListener {

        @Override
        public void onAvalibleDevicesUpdate(LinkedHashMap<String, IRemoteDevice> deviceInfo) {
            mRemotedeviceInfo = deviceInfo;
            mScanResultToViewListener.onAvalibleDevicesUpdate(deviceInfo);
        }

        @Override
        public void onConnectedDeviesUpdate(LinkedHashMap<String, IRemoteDevice> deviceInfo) {
            mRemotedeviceInfo = deviceInfo;
            mScanResultToViewListener.onConnectedDeviesUpdate(deviceInfo);
            checkRemoteSemaphoreFofPreview();

            for (Entry<String, CameraDeviceProxy> entry : mCameraIdManager
                    .getDeviceProxy().entrySet()) {
                String cameraId = entry.getKey();
                LogHelper.d(TAG, "[onConnectedDeviesUpdate] cameraId = "
                        + cameraId);
                if (!MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                    int status = IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_UNKNOWN;
                    IRemoteDevice device = deviceInfo.get(mCameraIdManager
                            .getDeviceId().get(cameraId));
                    if (device != null) {
                        status = device
                                .get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
                    }
                    if (status != IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_CONNECTED) {
                        LogHelper.d(TAG,
                                "device disconnected, close opened camera! cameraId = "
                                        + cameraId + ",status = " + status);
                        doCloseRemoteCameras(false);
                    }
                }
            }
        }

        @Override
        public void onServiceEventUpdate(int event, int value, String deviceId) {
            // get the camera id from deviceId
            String cameraId = getKeyByValue(mCameraIdManager.getDeviceId(), deviceId);
            LogHelper.d(TAG, "onServiceEventUpdate device id : " + deviceId + ",cameraId : "
                            + cameraId + ",all device id : " + mCameraIdManager.getDeviceId());
            if (cameraId != null) {
                mCurrentMode.onServiceEventUpdate(event, value, cameraId);
                mRemoteTouchFocus.setRemoteOrientation(value);
            }
        }
    }

    /**
     * this implement class will be triggered when UI have be touched,such as
     * user click the scan button or disconnect button.
     */
    private class MultiCameraViewListenerImpl implements IMultiCameraViewListener {

        @Override
        public void regisiterListener(IStatusUpdateListener result) {
            mScanResultToViewListener = result;
        }

        @Override
        public void startScanRemoteDevice() {
            if (mIMultiCameraDeviceAdapter != null) {
                mIMultiCameraDeviceAdapter.startScanDevice();
            }
        }

        @Override
        public void stopScanRemoteDevice() {
            if (mIMultiCameraDeviceAdapter != null) {
                mIMultiCameraDeviceAdapter.stopScanDevice();
            }
        }

        @Override
        public void connectRemoteDevice(String deviceKey) {
            if (mIMultiCameraDeviceAdapter != null) {
                mIMultiCameraDeviceAdapter.connectSelectedDevice(deviceKey);
            }
        }

        @Override
        public void openRemoteDevice(String cameraID, String deviceId) {
            LogHelper.d(TAG, "openRemoteDevice cameraID " + cameraID + ",deviceId " + deviceId
                            + ",mIsRecording = " + mIsRecording + ",IsCapturing = " + mIsCapturing);
            if (mIsRecording || mIsCapturing) {
                return;
            }
            // If the device camera already has been opened, return directly.
            if (deviceId.equals(mCameraIdManager.getDeviceId().get(cameraID))) {
                LogHelper.d(TAG, "[openRemoteCamera] The device camera already has been opened.");
                return;
            }

            // if just supported one remote camera, when open other remote
            // camera, need close the former one
            if (ONLY_SUPPORTE_ONE_REMOTE_CAMERA) {
                doCloseRemoteCameras(true);
            }
            // Update the cameraid-deviceid map
            mCameraIdManager.updateCameraId(true, cameraID, deviceId);

            if (checkCameraId(cameraID)) {
                openCamera(cameraID);
            } else {
                throw new RuntimeException("open camera id is error,need HAL Owner check");
            }

            LogHelper.d(TAG, "[openRemoteCamera] cameraID = " + cameraID);
        }

        @Override
        public void closeAllRemoteCameras() {
            LogHelper.d(TAG, "[closeAllRemoteCameras]");

            checkRemoteSemaphoreFofPreview();
            doCloseRemoteCameras(false);
            // Disconnect all linked camera service.so need notify adapter
            if (mIMultiCameraDeviceAdapter != null) {
                mIMultiCameraDeviceAdapter.disconnectAllRemoteCamera();
            }
            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_video_v2);
        }

        @Override
        public void backToMultiPreview() {
            mRemoteTouchFocus.clearFocusUi();
            List<String> previewCamera = mCameraIdManager.getPreviewCamera();
            LinkedHashMap<String, String> deviceId = mCameraIdManager.getDeviceId();
            LinkedHashMap<String, CameraDeviceProxy> proxy = mCameraIdManager.getDeviceProxy();

            for (int index = 0; index < deviceId.keySet().size(); index++) {
                String cameraId = (String) deviceId.keySet().toArray()[index];
                LogHelper.d(TAG, "[backToMultiPreview] cameraId = " + cameraId);
                if (!proxy.keySet().contains(cameraId)) {
                    openCamera(cameraId);
                }
            }
            // Shallow copy the list value
            List<String> hasOpened = new ArrayList<String>();
            hasOpened.addAll(deviceId.keySet());
            mCameraIdManager.updatePreviewCameraId(hasOpened);

            // how to notify the stream current preview need same multi-preview
            // before change to single preview
            notifyCameraIdChanged(mCameraIdManager.getPreviewCamera(), isRemoteCameraOpened());
            // because multi-preview not support video,so the button need hide
            mAppUi.switchShutterButtonLayout(R.layout.camera_shutter_photo_v2);
        }

        @Override
        public void updateAllViewVisibility(boolean visible) {
            List<String> previewCamera = mCameraIdManager.getPreviewCamera();
            mIsCanShowCommonUi = visible;
            if (visible) {
                mAppUi.showThumbnailManagerUi();
                mAppUi.showModeOptionsUi();
                //If current is multipreview,don't need show the setting/picker/indicator UI.
                final List<String> previewCameraCopy = new ArrayList<String>();
                previewCameraCopy.addAll(previewCamera);
                mAppController.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ONLY_ONE_PREVIEW_CAMERA == previewCameraCopy.size()
                                       && MultiCameraModuleUtil.hasLocalCamera(previewCameraCopy)) {
                            mAppUi.showSettingUi();
                            mAppUi.showPickerManagerUi();
                            mAppUi.showIndicatorManagerUi();
                        }
                    }
                });
            } else {
                mAppUi.setAllCommonViewButShutterVisible(false);
            }

            // When in scan view,need hide the shutter manager.otherwise need show it.
            if (visible) {
                mAppUi.showShutterButton();
            } else {
                mAppUi.hideShutterButton();
            }
            // Hide App UI and do not show UI by other case.
            mAppUi.stopShowCommonUI(!visible);

            LogHelper.i(TAG, "[updateAllViewVisibility],visible= " + visible);
        }
    }

    /**
     * A inner class used for maintain camera state such as opened and error
     * state. also detect the session state,such as configured and configure
     * fail.
     */
    private class CameraDeviceCallback extends CameraStateCallback
            implements CameraSessionCallback {
        private CameraDeviceProxy mCameraDeviceProxy;
        private String mDeviceCameraId;
        public CameraDeviceCallback(String cameraId) {
            mDeviceCameraId = cameraId;
        }
        // CameraStateCallback ---begin
        @Override
        public void onOpened(CameraDeviceProxy deviceProxy) {
            String cameraId = deviceProxy.getCameraId();
            LogHelper.d(TAG, "onOpened,mPaused = " + mPaused + ",mPreviewSurfaceIsReadyForOpen = "
                            + mPreviewSurfaceIsReadyForOpen + ",cameraId = " + cameraId);

            mFocusStateListenerMangaer.setFocusStateListener(cameraId);
            LinkedHashMap<String, String> deviceId = mCameraIdManager.getDeviceId();
            LinkedHashMap<String, CameraDeviceProxy> proxy = mCameraIdManager.getDeviceProxy();
            // Put the device proxy into the mDeviceProxy map {cameraid,proxy}.
            if (!proxy.containsValue(deviceProxy)) {
                proxy.put(cameraId, deviceProxy);
            }
            // Just put the local camera id - device id to the deviceId map.Becasue remote camera
            // device id is update at the beginning of the openRemoteCamera().
            if (MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                mCameraIdManager.updateCameraId(true, cameraId, mLocalDeviceIdKeyPref + cameraId);
            }
            mCameraDeviceProxy = deviceProxy;
            if (!mPaused && mPreviewSurfaceIsReadyForOpen) {
                mCameraDeviceProxy.requestChangeSessionOutputs(false);
            }
            mCameraSemaphoreCtrl.releaseClock(cameraId);
        }

        @Override
        public void onError(int error) {
            LogHelper.d(TAG, "onError, cameraid : " + mDeviceCameraId + ",error = " + error);
            mCameraSemaphoreCtrl.releaseClock(mDeviceCameraId);
        }

        @Override
        public void onDisconnected(CameraDeviceProxy camera) {
            super.onDisconnected(camera);
            LogHelper.d(TAG, "onOpened,onDisconnected = " + mDeviceCameraId);
            mCameraSemaphoreCtrl.releaseClock(mDeviceCameraId);
        }

        @Override
        public void onSessionConfigured() {
            mCameraDeviceProxy.requestChangeCaptureRequets(true, getRequestType(),
                    CaptureType.REPEATING_REQUEST);
        }
        // CameraStateCallback ---end

        @Override
        public void onSessionActive() {
        }

        @Override
        public void configuringSessionOutputs(List<Surface> sessionOutputSurfaces) {
            LogHelper.d(TAG, "configuringSessionOutputs,mIsCapturing = " + mIsCapturing
                    + ",mIsRecording =" + mIsRecording);
            if (!(mIsCapturing || mIsRecording)) {
                notifyCameraIdChanged(mCameraIdManager.getPreviewCamera(),
                        isRemoteCameraOpened());
            }
            mCurrentMode.configuringSessionOutputs(sessionOutputSurfaces,
                    mCameraDeviceProxy.getCameraId());
        }

        @Override
        public CaptureCallback configuringSessionRequests(Builder requestBuilder,
                RequestType requestType, CaptureType captureType) {
            if (mPaused) {
                return null;
            }

            CaptureCallback captureCallback = null;
            switch (requestType) {
            case RECORDING:
            case PREVIEW:
                captureCallback = mCaptureCallback;
                break;
            case STILL_CAPTURE:
                captureCallback = mCurrentMode.getCaptureCallback();
                break;
            default:
                break;
            }
            Map<RequestType, CaptureRequest.Builder> requestBuilders = new HashMap<RequestType,
                    CaptureRequest.Builder>();

            if (MultiCameraModuleUtil.isLocalCamera(mCameraDeviceProxy.getCameraId())) {
                Rect cropRegion = Utils.cropRegionForZoom(mAppController.getActivity(),
                        mCameraDeviceProxy.getCameraId(), 1f);
                // apply crop region
                requestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion);
                requestBuilders.put(requestType, requestBuilder);
                // 1. apply 3A control
                mAaaControl.configuringSessionRequests(requestBuilders, captureType, true);
                // 2. apply addition parameter
                mDetectionManager.configuringSessionRequests(requestBuilders, captureType);
            } else {
                requestBuilders.put(requestType, requestBuilder);
                mRemoteTouchFocus.configuringSessionRequests(requestBuilders, captureType, true);
            }
            // Apply mode parameter
            mCurrentMode.configuringSessionRequests(requestBuilders,
                    mCameraDeviceProxy.getCameraId());
            return captureCallback;
        }

        private CaptureCallback mCaptureCallback = new CaptureCallback() {
            @Override
            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                    long timestamp, long frameNumber) {
                if (MultiCameraModuleUtil.isLocalCamera(mCameraDeviceProxy.getCameraId())) {
                    mDetectionManager.onCaptureStarted(request, timestamp, frameNumber);
                } else {
                    mRemoteTouchFocus.onPreviewCaptureStarted(request, timestamp, frameNumber);
                }
                mCurrentMode.onPreviewCaptureStarted(request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                    CaptureResult partialResult) {
                if (MultiCameraModuleUtil.isLocalCamera(mCameraDeviceProxy.getCameraId())) {
                    // mAaaManager.onPreviewCaptureProgressed(request,
                    // partialResult);
                } else {
                    mRemoteTouchFocus.onPreviewCaptureProgressed(request, partialResult);
                }
            };

            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                    TotalCaptureResult result) {
                if (MultiCameraModuleUtil.isLocalCamera(mCameraDeviceProxy.getCameraId())) {
                    if (isNeedNotifyAAAManager(mCameraIdManager.getPreviewCamera())) {
                        mAaaControl.onPreviewCaptureCompleted(request, result);
                    } else {
                        // TODO clear action will be improved
                        mAaaControl.clearFocusUi();
                    }
                    mDetectionManager.onCaptureCompleted(request, result);
                } else {
                    mRemoteTouchFocus.onPreviewCaptureCompleted(request, result);
                }
                mCurrentMode.onPreviewCaptureCompleted(request, result);
            }
        };
    }

    /**
     * This class is used for remote touch focus.
     */
    private class RemoteAaaListener implements RemoteTouchFocus.IRemoteAaaListener {

        public RequestType getRepeatingRequestType() {
            return RequestType.PREVIEW;
        }

        public void requestChangeCaptureRequets(boolean sync, String cameraId,
                RequestType requestType, CaptureType captureType) {
            if (cameraId == null) {
                LogHelper.d(TAG,
                        "[RemoteAaaListener,requestChangeCaptureRequets] cameraId is null");
                return;
            }
            CameraDeviceProxy camera = mCameraIdManager.getDeviceProxy().get(cameraId);
            //In this case, the remote camera maybe closed:
            //When remote camera is do focusing,but focusing is not callback.
            //so disconnect the remote camera.when module receive this message, will close remote
            //Camera,and also will clear the remote camera proxy.
            //But remote camera will request cancel Touch AF when 5s time out.
            //In this case don't need respond the request.
            if (camera != null) {
                camera.requestChangeCaptureRequets(sync, requestType, captureType);
            }
        }
    }

    /**
     * This class is used for manage camera ID and device proxy.
     */
    private class CameraIdManager {
        private List<String> mBackUpPreviewCamera = new ArrayList<String>();
        private List<String> mPreviewCameraId = new ArrayList<String>();
        // key -value is cameraid -camera device proxy.
        private LinkedHashMap<String, CameraDeviceProxy> mDeviceProxy =
                new LinkedHashMap<String, CameraDeviceProxy>();

        // key-value is cameraId-deviceId
        private LinkedHashMap<String, String> mDeviceId = new LinkedHashMap<String, String>();

        private void updateCameraId(boolean isAdd, String cameraId, String deviceId) {
            if (isAdd) {
                if (!mDeviceId.containsKey(cameraId)) {
                    mDeviceId.put(cameraId, deviceId);
                }
                if (!mPreviewCameraId.contains(cameraId)) {
                    mPreviewCameraId.add(cameraId);
                }
            } else {
                mDeviceId.remove(cameraId);
                mPreviewCameraId.remove(cameraId);
            }

            LogHelper.d(TAG, "[updateCameraId] isAdd : " + isAdd + ",cameraId = " + cameraId
                    + ",mPreviewCameraId = " + mPreviewCameraId + ",mDeviceId = " + mDeviceId);
        }

        private void updateBackUpPreviewCameraId() {
            mBackUpPreviewCamera.clear();
            mBackUpPreviewCamera.addAll(mPreviewCameraId);
        }

        private void updatePreviewCameraId(List<String> previewCamera) {
            mPreviewCameraId = previewCamera;
        }

        private List<String> getBackUpPreviewCameraId() {
            return mBackUpPreviewCamera;
        }

        private List<String> getPreviewCamera() {
            return mPreviewCameraId;
        }

        private LinkedHashMap<String, CameraDeviceProxy> getDeviceProxy() {
            return mDeviceProxy;
        }

        private LinkedHashMap<String, String> getDeviceId() {
            return mDeviceId;
        }
    }

    /**
     * This class is used for manage focus state.
     */
    private class FocusStateListenerMangaer {
        LinkedHashMap<String, FocusStateListener> mFocusStateListenerImpl =
                        new LinkedHashMap<String, FocusStateListener>();

        private void setFocusStateListener(String cameraId) {
            if (MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                FocusStateListener focusStateListenerImpl = new LocalFocusStateListenerImpl();
                mFocusStateListenerImpl.put(cameraId, focusStateListenerImpl);
                mAaaControl.setFocusStateListener(focusStateListenerImpl);
            } else {
                FocusStateListener focusStateListenerImpl = new RemoteFocusStateListenerImpl();
                mFocusStateListenerImpl.put(cameraId, focusStateListenerImpl);
                mRemoteTouchFocus.setFocusStateListener(focusStateListenerImpl);
            }
        }

        private void clearFocusStateListener(String cameraId) {
            FocusStateListener focusStateListenerImpl = mFocusStateListenerImpl.get(cameraId);
            if (MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                mAaaControl.clearFocusStateListener(focusStateListenerImpl);
            } else {
                mRemoteTouchFocus.clearFocusStateListener(focusStateListenerImpl);
            }
        }
        /**
         * Classes implementing this interface will be called when the state of the
         * focus changes. Guaranteed not to stay stuck in scanning state past some
         * reasonable timeout even if Camera API is stuck.
         */
        private class LocalFocusStateListenerImpl implements FocusStateListener {
            @Override
            public void onFocusStatusUpdate(AutoFocusState state) {
                LogHelper.d(TAG, "state: " + state + " mTapToFocusWaitForActiveScan: "
                        + mTapToFocusWaitForActiveScan);
                switch (state) {
                    case ACTIVE_SCAN:
                        if (mTapToFocusWaitForActiveScan) {
                            mAbstractModuleUI.updateFocusStatus(true);
                            mTapToFocusWaitForActiveScan = false;
                        }
                        break;
                    case ACTIVE_FOCUSED:
                    case ACTIVE_UNFOCUSED:
                        mAbstractModuleUI.updateFocusStatus(false);
                        break;
                    default:
                        break;
                }
            }
        }
        /**
         * Classes implementing this interface will be called when the state of the
         * focus changes. Guaranteed not to stay stuck in scanning state past some
         * reasonable timeout even if Camera API is stuck.
         */
        private class RemoteFocusStateListenerImpl implements FocusStateListener {
            @Override
            public void onFocusStatusUpdate(AutoFocusState state) {
                LogHelper.d(TAG, "state: " + state);
                switch (state) {
                    case ACTIVE_SCAN:
                        mAbstractModuleUI.updateFocusStatus(true);
                        break;
                    case ACTIVE_FOCUSED:
                    case ACTIVE_UNFOCUSED:
                        mAbstractModuleUI.updateFocusStatus(false);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * this class used for make sure camera will be closed when camera have opened.
     */
    private class CameraSemaphoreCtrl {
        private static final int NUM_OF_PERMITS = 1;
        private static final int MAX_TIME_WAIT_PERMIT_MS = 5000;
        private LinkedHashMap<String, Semaphore> mSemaphoreCtrl =
                        new LinkedHashMap<String, Semaphore>();

        public void acquireClock(String cameraId) {
            Semaphore sp = mSemaphoreCtrl.get(cameraId);
            if (sp == null) {
                sp = new Semaphore(NUM_OF_PERMITS, true);
                mSemaphoreCtrl.put(cameraId, sp);
            }
            try {
                if (!sp.tryAcquire(MAX_TIME_WAIT_PERMIT_MS, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to acquire camera-open lock.");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(
                                "Interrupted while waiting to acquire camera-open lock.", e);
            }
            LogHelper.d(TAG, "acquireClock ----,cameraId = " + cameraId);
        }

        public void releaseClock(String cameraId) {
            Semaphore sp = mSemaphoreCtrl.get(cameraId);
            if (sp != null) {
                sp.release();
            }
        }

        public List<String> getRemoteSemaphoresId() {
            List<String> remotoeSemaphoresId = new ArrayList<>();
            for (Entry<String, Semaphore> entry : mSemaphoreCtrl.entrySet()) {
                String cameraId = entry.getKey();
                LogHelper.d(TAG, "[getRemoteSemaphores] cameraId = " + cameraId);
                if (!MultiCameraModuleUtil.isLocalCamera(cameraId)) {
                    remotoeSemaphoresId.add(cameraId);
                }
            }
            return remotoeSemaphoresId;
        }
        //should add a function to clear the mSemaphoreCtrl hashmap.
        public void removeSemphore(String cameraId) {
            mSemaphoreCtrl.remove(cameraId);
        }
    }
}
