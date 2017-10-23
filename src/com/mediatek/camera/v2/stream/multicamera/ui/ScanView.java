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
package com.mediatek.camera.v2.stream.multicamera.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.android.camera.R;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;
import com.mediatek.camera.v2.platform.device.IMultiCameraDeviceAdapter;
import com.mediatek.camera.v2.platform.device.IRemoteDevice;
import com.mediatek.camera.v2.util.Utils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Scan view is a view for device scan and connect.
 */
public class ScanView implements IMultiCameraView {
    /**
     * Scan view listener.
     */
    public interface IScanViewListener {
        /**
         * Start the cross mount scan.
         */
        public void onDiscoverStated();
        /**
         * Stop the cross mount scan.
         */
        public void onDiscoverStoped();
        /**
         * When a device is selected, invoke it to notify the event.
         * @param deviceKey The selected camera key value.
         */
        public void onDeviceCameraSelected(String deviceKey);
        /**
         * When the selected camera is connected, invoke it to notify the event.
         * @param cameraId The camera id value of the connected device.
         * @param deviceId The device id value of the connected device.
         */
        public void onDeviceCameraConnected(String cameraId, String deviceId);
        /**
         * When the scan view is shown, invoke it to notify the event.
         */
        public void onScanViewShow();
        /**
         * When the scan view is hided, invoke it to notify the event.
         */
        public void onScanViewDissmiss();
        /**
         * When the scan view is shown, should hide other UI.
         * When hide scan view, show other UI.
         * @param show Is show common UI no not.
         */
        public void updateCommonUiVisibility(boolean show);
    }

    private static final Tag TAG = new Tag(
            ScanView.class.getSimpleName());
    private Activity mActivity;
    private ViewGroup mParentView;
    private ViewGroup mScanRootLayout;
    private ViewGroup mScanContainer;
    private ViewGroup mAvailableDevicesView;
    private HorizontalScrollView mScrollContainer;
    private View mDismissView;
    private ProgressBar mScanProgress;
    private LayoutInflater mInflater;

    private SelectorItem mConnectingItem;

    private IScanViewListener mScanViewListener;

    private  String mRecentConnectedKey;
    private  int mBiggestSerialNumber;

    private boolean mIsShowConnectedDevice = false;
    private boolean mIsOpenRecentConnectedDevice = false;
    private boolean mIsShowScanViewWhenScanStarted = false;

    private LinkedHashMap<String, IRemoteDevice> mDevicesList;

    /**
     * Scan view contructor.
     * @param scanViewListener Scan view lisntener instance.
     */
    public ScanView(IScanViewListener scanViewListener) {
        LogHelper.i(TAG, "ScanView");
        mScanViewListener = scanViewListener;
    }

    @Override
    public void open(Activity activity, ViewGroup parentView) {
        mActivity = activity;
        mParentView = parentView;
        mActivity.getLayoutInflater().inflate(R.layout.multi_camera_scan,
                mParentView, true);
        mScanRootLayout = (ViewGroup) mParentView
                .findViewById(R.id.scan_root_layout);
        mScanContainer = (ViewGroup) mParentView
                .findViewById(R.id.scan_container);
        mDismissView = mScanContainer.findViewById(R.id.dismiss);
        mDismissView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        mScanProgress = (ProgressBar) mScanContainer
                .findViewById(R.id.scan_progress);
        mScrollContainer = (HorizontalScrollView) mScanContainer
                .findViewById(R.id.scroll_container);
        mAvailableDevicesView = (ViewGroup) mScanContainer
                .findViewById(R.id.availableDevices);
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mIsShowConnectedDevice = true;
        mIsOpenRecentConnectedDevice = true;
        mIsShowScanViewWhenScanStarted = true;
    }

    @Override
    public void resume() {
        LogHelper.i(TAG, "resume");
        mScanViewListener.onDiscoverStated();
    }

    @Override
    public void pause() {
        LogHelper.i(TAG, "pause");
        if (mScanRootLayout.getVisibility() == View.VISIBLE) {
            mIsShowScanViewWhenScanStarted = true;
        }
        hide();
        mScanViewListener.onDiscoverStoped();
    }

    @Override
    public void close() {
        LogHelper.i(TAG, "close");
        mParentView.removeView(mScanRootLayout);
        mScanRootLayout = null;
    }

    @Override
    public void onOrientationChanged(int gsensorOrientation) {
        Utils.setRotatableOrientation(mScanContainer, gsensorOrientation, true);
    }

    @Override
    public void show() {
        startScanDevicesAndShowUILater();
    }

    @Override
    public void hide() {
        hideScanViewAndShowCommonUI();
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        // TODO Auto-generated method stub
    }

    @Override
    public void updatePreviewCamera(List<String> cameraId,
            boolean isRemoteOpened) {
        LogHelper.i(TAG, "[updatePreviewCamera] list size = " + cameraId.size()
                + ",RemoteOpened = " + isRemoteOpened);
        boolean needShow = false;
        //if already opened remote camera, no need to show connected device in scan view.
        //The connected device will show in the device tag view.
        if (isRemoteOpened) {
            needShow = false;
        } else {
            needShow = true;
        }
        //When show flag is changed ,need refresh the device list in scan view.
        if (needShow != mIsShowConnectedDevice) {
            mIsShowConnectedDevice = needShow;
            if (mScanRootLayout.getVisibility() == View.VISIBLE) {
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        initializeAvailableDevices();
                    }
                });
            }
        }
    }

    @Override
    public void updateCapturingStatus(boolean isCapturing) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onDisplayChanged(int displayRotation) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onAvalibleDevicesUpdate(
            LinkedHashMap<String, IRemoteDevice> devices) {
        LogHelper.i(TAG, "onAvalibleDevicesUpdate");
        mDevicesList = devices;
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                initializeAvailableDevices();
            }
        });
    }

    @Override
    public void onConnectedDeviesUpdate(
            LinkedHashMap<String, IRemoteDevice> devices) {
        LogHelper.i(TAG, "onConnectedDeviesUpdate");
        mDevicesList = devices;
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (mAvailableDevicesView.getChildCount() != 0) {
                    mAvailableDevicesView.removeAllViews();
                    mScrollContainer.scrollTo(0, 0); // move to the left of the list.
                }

                if (mDevicesList.isEmpty()) {
                    // if device list is empty, show the scan progress.
                    mScanProgress.setVisibility(View.VISIBLE);
                } else {
                    updateAvailableDeviceView();
                }

                if (mConnectingItem != null) {
                    // check the connecting device status.
                    IRemoteDevice device = mDevicesList.get(mConnectingItem.getKey());
                    if (device == null) {
                        mConnectingItem = null;
                        return;
                    }

                    int status = device.get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
                    int cameraID = device.get(IRemoteDevice.KEY_REMOTE_CAMERA_ID);

                    LogHelper.i(TAG, "onConnectedDeviesUpdate status " + status
                            + " cameraID " + cameraID);
                    if (isDeviceCameraConnected(status)) {
                        //show connected background.
                        mConnectingItem.setConnectStatus(SelectorItem.Status.CONNECTED);
                        // notify camera id to module.
                        mScanViewListener.onDeviceCameraConnected(
                                String.valueOf(cameraID),
                                mConnectingItem.getKey());
                        //clear flag.
                        mConnectingItem = null;
                    } else if (isDeviceCameraConnecting(status)) {
                        mConnectingItem.setConnectStatus(SelectorItem.Status.CONNECTING);
                    } else {
                        mConnectingItem.setConnectStatus(SelectorItem.Status.AVALIABLE);
                        mConnectingItem = null;
                    }
                }
            }
        });
    }

    private void initializeAvailableDevices() {
        LogHelper.i(TAG, "initializeAvailableDevices");
        if (mAvailableDevicesView.getChildCount() != 0) {
            mAvailableDevicesView.removeAllViews();
            mScrollContainer.scrollTo(0, 0); // move to the left of the list.
        }

        if (mDevicesList.isEmpty()) {
            // if device list is empty, show the scan progress.
            mScanProgress.setVisibility(View.VISIBLE);
        } else {
            updateAvailableDeviceView();
            if (mIsOpenRecentConnectedDevice) {
                if (mBiggestSerialNumber > IMultiCameraDeviceAdapter.REMOTE_DEVICE_SERIAL_UNKNOWN) {
                    LogHelper.d(TAG, "Find the recent connected device  key "
                            + mRecentConnectedKey + " mBiggestSerialNumber "
                            + mBiggestSerialNumber);
                    IRemoteDevice device = mDevicesList.get(mRecentConnectedKey);
                    int cameraID = device.get(IRemoteDevice.KEY_REMOTE_CAMERA_ID);
                    // notify camera id to module.
                    mScanViewListener.onDeviceCameraConnected(
                            String.valueOf(cameraID), mRecentConnectedKey);
                    hide();
                    mIsOpenRecentConnectedDevice = false;
                    mIsShowScanViewWhenScanStarted = false;
                    return;
                } else {
                    LogHelper.d(TAG, "Do not find recent connected device!");
                }
            }
        }
        if (mIsShowScanViewWhenScanStarted) {
            showScanViewAndHideCommonUI();
        }
        mIsOpenRecentConnectedDevice = false;
    }

    private void updateAvailableDeviceView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mRecentConnectedKey = null;
        mBiggestSerialNumber = IMultiCameraDeviceAdapter.REMOTE_DEVICE_SERIAL_UNKNOWN;
        for (Entry<String, IRemoteDevice> entry : mDevicesList.entrySet()) {
            String key = entry.getKey();
            IRemoteDevice device = entry.getValue();
            int status = device.get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
            if (!mIsShowConnectedDevice && isDeviceCameraConnected(status)) {
                // if need not show the connected device and current device
                // status is connected,
                // do not add the device to the list.
                continue;
            }
            String name = device.get(IRemoteDevice.KEY_REMOTE_DEVICE_NAME);
            int type = device.get(IRemoteDevice.KEY_REMOTE_DEVICE_TYPE);
            final SelectorItem deviceIcon = (SelectorItem) mInflater.inflate(
                    R.layout.multi_camera_device_selector, null, false);
            deviceIcon.setName(name);
            deviceIcon.setKey(key);
            deviceIcon.setImageResource(getDeviceTypeIconResId(type));
            if (isDeviceCameraConnected(status)) {
                deviceIcon.setConnectStatus(SelectorItem.Status.CONNECTED);
            } else if (isDeviceCameraConnecting(status)) {
                deviceIcon.setConnectStatus(SelectorItem.Status.CONNECTING);
            } else {
                deviceIcon.setConnectStatus(SelectorItem.Status.AVALIABLE);
            }
            if (mConnectingItem != null && mConnectingItem.getKey().equals(key)) {
                mConnectingItem = deviceIcon;
            }
            deviceIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemSelected(deviceIcon);
                }
            });
            mAvailableDevicesView.addView(deviceIcon, params);
            if (isDeviceCameraConnected(status)) {
                int serialNumber = device
                        .get(IRemoteDevice.KEY_REMOTE_DEVICE_SERIAL);
                if (serialNumber > mBiggestSerialNumber) {
                    mRecentConnectedKey = key;
                    mBiggestSerialNumber = serialNumber;
                }
            }
            LogHelper.d(TAG, "updateAvailableDeviceView [" + key + ", " + name
                    + ", " + type + ", " + status + "]");
        }
        // If has no available devices need to be shown, show scan progress.
        if (mAvailableDevicesView.getChildCount() != 0) {
            mScanProgress.setVisibility(View.GONE);
        } else {
            mScanProgress.setVisibility(View.VISIBLE);
        }
    }

    private void onItemSelected(SelectorItem item) {
        LogHelper.i(TAG, "onItemSelected");
        // check the connecting device status.
        if (mConnectingItem != null) {
            IRemoteDevice device = mDevicesList.get((mConnectingItem.getKey()));
            int status = device.get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
            if (isDeviceCameraConnecting(status)) {
                LogHelper.i(TAG, "onItemSelected already has a connecting device!");
                return;
            }
        }

        IRemoteDevice device = mDevicesList.get((item.getKey()));
        int status = device.get(IRemoteDevice.KEY_REMOTE_SERVICE_STATUS);
        // check the connecting device status.
        if (isDeviceCameraConnected(status)) {
            int cameraID = device.get(IRemoteDevice.KEY_REMOTE_CAMERA_ID);
            // notify camera id to module.
            mScanViewListener.onDeviceCameraConnected(String.valueOf(cameraID),
                    item.getKey());
        } else {
            mConnectingItem = item;
            device.mountCamera();
            if (isDeviceCameraConnecting(status)) {
                mConnectingItem.setConnectStatus(SelectorItem.Status.CONNECTING);
            }
        }
    }

    private int[] getDeviceTypeIconResId(int type) {
        LogHelper.i(TAG, "getDeviceTypeIconResId type " + type);
        switch (type) {
        case IMultiCameraDeviceAdapter.REMOTE_DEVICE_TYPE_PHONE:
            return new int[] { R.drawable.ic_crossmount_phone,
                    R.drawable.ic_crossmount_phone_on };
        case IMultiCameraDeviceAdapter.REMOTE_DEVICE_TYPE_TV:
            return new int[] { R.drawable.ic_crossmount_tv,
                    R.drawable.ic_crossmount_tv_on };
        case IMultiCameraDeviceAdapter.REMOTE_DEVICE_TYPE_TABLET:
            return new int[] { R.drawable.ic_crossmount_tablet,
                    R.drawable.ic_crossmount_tablet_on };
        case IMultiCameraDeviceAdapter.REMOTE_DEVICE_TYPE_WATCH:
            return new int[] { R.drawable.ic_crossmount_watch,
                    R.drawable.ic_crossmount_watch_on };
         default:
             return new int[] { R.drawable.ic_crossmount_phone,
                     R.drawable.ic_crossmount_phone_on };
        }
    }

    private void showScanViewAndHideCommonUI() {
        LogHelper.i(TAG, "showScanViewAndHideCommonUI");
        mScanRootLayout.setVisibility(View.VISIBLE);
        // when scan view is showing ,will hide all common UI
        mScanViewListener.updateCommonUiVisibility(false);
        mScanViewListener.onScanViewShow();
        mIsShowScanViewWhenScanStarted = false;
    }

    private void hideScanViewAndShowCommonUI() {
        LogHelper.i(TAG, "hideScanViewAndShowCommonUI");
        mScanRootLayout.setVisibility(View.GONE);
        // when scan view is hide ,will show all common UI
        mScanViewListener.updateCommonUiVisibility(true);
        mScanViewListener.onScanViewDissmiss();
    }

    private void startScanDevicesAndShowUILater() {
        LogHelper.i(TAG, "startScanDevicesAndShowUILater");
        mIsShowScanViewWhenScanStarted = true;
        mScanViewListener.onDiscoverStated();
    }

    private boolean isDeviceCameraConnected(int status) {
        LogHelper.i(TAG, "isDeviceCameraConnected status " + status);
        if (status == IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDeviceCameraConnecting(int status) {
        LogHelper.i(TAG, "isDeviceCameraConnecting status " + status);
        if (status == IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_CONNECTING) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDeviceCameraAvailable(int status) {
        if (status == IMultiCameraDeviceAdapter.REMOTE_CAMERA_STATUS_AVALIABLE) {
            return true;
        } else {
            return false;
        }
    }

    private int getStatusIntValue(Object object) {
        int status = IMultiCameraDeviceAdapter.REMOTE_CAEMRA_STATUS_UNKNOWN;
        if (object != null && object instanceof Integer) {
            status = (Integer) object;
        }
        return status;
    }

    private int getTypeIntValue(Object object) {
        int type = IMultiCameraDeviceAdapter.REMOTE_DEVICE_TYPE_UNKNOWN;
        if (object != null && object instanceof Integer) {
            type = (Integer) object;
        }
        return type;
    }

    private int getSerialIntValue(Object object) {
        int serial = IMultiCameraDeviceAdapter.REMOTE_DEVICE_SERIAL_UNKNOWN;
        if (object != null && object instanceof Integer) {
            serial = (Integer) object;
        }
        return serial;
    }

    private int getCameraIdIntValue(Object object) {
        int cameraID = -1;
        if (object != null && object instanceof Integer) {
            cameraID = (Integer) object;
        }
        return cameraID;
    }

    /**
     *  A comparator for sort device list.
     */
    private class KeyValueComparator implements
            Comparator<Map.Entry<String, Object>> {
        @Override
        public int compare(Entry<String, Object> lhs, Entry<String, Object> rhs) {
            return ((String) lhs.getKey()).compareToIgnoreCase((String) rhs
                    .getKey());
        }
    }

    @Override
    public void updateRecordingStatus(boolean isStarted) {
        if (isStarted) {
            mConnectingItem = null;
        }
    }

    @Override
    public void startCountdown(int sec) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFocusStatus(boolean isFocusing) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelCountDown() {
        // TODO Auto-generated method stub

    }
}
