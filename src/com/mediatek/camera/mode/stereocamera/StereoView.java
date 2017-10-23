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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


import com.android.camera.CameraActivity;
import com.android.camera.CameraActivity.OnOrientationListener;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.Util;

import com.mediatek.camera.platform.ICameraAppUi;
import com.mediatek.camera.platform.IModuleCtrl;
import com.mediatek.camera.ui.CameraView;

/**
 * A view containing a seek bar for the user and will
 * be shown on the screen.
 *
 */
public class StereoView extends CameraView implements
                     SeekBar.OnSeekBarChangeListener, OnOrientationListener {
    private static final String TAG = "StereoView";

    private static final int MSG_DOF_HIDE = 0;
    private static final int MSG_DOF_SHOW = 1;
    private static final long HIDE_DELAY_TIME = 1000;
    private static final long PROGRESS_DELAY_TIME = 50;
    private static final int PROGRESS_PER_DOF = 20;
    private static final String LEVEL_DEFAULT = "7";
    private static final int SEEKBAR_PROCESS_INIT = 7 * PROGRESS_PER_DOF;
    private static int sProgress = SEEKBAR_PROCESS_INIT;
    private String mLevel = LEVEL_DEFAULT;

    private View mView;
    private Activity mContext;
    private int mOrientation;
    private long mProcessTime;

    private TextView mDofView;
    private SeekBar mStereoSeekBar;
    private ViewHandler mViewHandler;

    private ICameraAppUi mICameraAppUi;

    private Listener mListener;

    private static final String[] DOFDATA = { "F11", "F10",
            "F9.0", "F8.0", "F7.2", "F6.3", "F5.6", "F4.5", "F3.6", "F2.8",
            "F2.2", "F1.8", "F1.4", "F1.2", "F1.0", "F0.8"};
    /**
     * Create a stereo view.
     * @param context camera context instance.
     */
    public StereoView(Activity context) {
        super(context);
        mContext = context;
        mViewHandler = new ViewHandler(mContext.getMainLooper());
        ((CameraActivity) mContext).addOnOrientationListener(this);
    }

    @Override
    public void init(Activity activity, ICameraAppUi cameraAppUi,
            IModuleCtrl moduleCtrl) {
        super.init(activity, cameraAppUi, moduleCtrl);
        mICameraAppUi = cameraAppUi;
        mOrientation = moduleCtrl.getOrientationCompensation();
    }

    @Override
    public void uninit() {
        Log.i(TAG, "[uninit]...");
        super.uninit();
        ((CameraActivity) mContext).removeOnOrientationListener(this);
    }

    @Override
    protected View getView() {
        Log.d(TAG, "[getView]...");
        mView = inflate(R.layout.stereo_view);
        ImageView small = (ImageView) mView.findViewById(R.id.small_aperture);
        small.setVisibility(View.VISIBLE);
        ImageView big = (ImageView) mView.findViewById(R.id.big_aperture);
        big.setVisibility(View.VISIBLE);

        mDofView = (TextView) mView.findViewById(R.id.dof_view);
        mStereoSeekBar = (SeekBar) mView.findViewById(R.id.refocusSeekBar);
        mStereoSeekBar.setVisibility(View.VISIBLE);
        mStereoSeekBar.setProgress(sProgress);
        mStereoSeekBar.setOnSeekBarChangeListener(this);
        return mView;
    }

    @Override
    protected void addView(View view) {
        Log.d(TAG, "addView");
        mICameraAppUi.getNormalViewLayer().addView(view);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            Util.setOrientation(mView, mOrientation, true);
        }
    }

    @Override
    public void reset() {
        Log.d(TAG, "[reset]...");
        mLevel = LEVEL_DEFAULT;
        sProgress = SEEKBAR_PROCESS_INIT;
    }

    @Override
    public void refresh() {
        Log.d(TAG, "[refresh]...");
        mStereoSeekBar.setProgress(sProgress);
        mLevel = String.valueOf(sProgress / PROGRESS_PER_DOF);
        mListener.notifyVsDofLevel(mLevel);
    }

    @Override
    public void show() {
        Log.d(TAG, "[show]...");
        super.show();
        Util.setOrientation(mView, mOrientation, true);
        mLevel = String.valueOf(sProgress / PROGRESS_PER_DOF);
        mListener.notifyVsDofLevel(mLevel);
    }

    @Override
    public void onProgressChanged(SeekBar refocusSeekBar, int progress,
            boolean fromuser) {
        long now = System.currentTimeMillis();
        int depth = progress / PROGRESS_PER_DOF;
        mDofView.setText(DOFDATA[depth]);
        if (now - mProcessTime >= PROGRESS_DELAY_TIME) {
            if (!String.valueOf(depth).equals(mLevel)) {
                Log.d(TAG, "onProgressChanged level = " + mLevel);
                mLevel = String.valueOf(progress / PROGRESS_PER_DOF);
                mListener.notifyVsDofLevel(mLevel);
            }
        }
        mProcessTime = System.currentTimeMillis();
    }

    @Override
    public void onStartTrackingTouch(SeekBar refocusSeekBar) {
        Log.d(TAG, "onStartTrackingTouch");
        mViewHandler.removeMessages(MSG_DOF_HIDE);
        mViewHandler.sendEmptyMessage(MSG_DOF_SHOW);
    }

    @Override
    public void onStopTrackingTouch(SeekBar refocusSeekBar) {
        sProgress = mStereoSeekBar.getProgress();
        mLevel = String.valueOf(mStereoSeekBar.getProgress() / PROGRESS_PER_DOF);
        Log.d(TAG, "onStopTrackingTouch level = " + mLevel);
        mListener.notifyVsDofLevel(mLevel);
        mViewHandler.sendEmptyMessageDelayed(MSG_DOF_HIDE, HIDE_DELAY_TIME);
    }

    @Override
    public void setListener(Object listener) {
        mListener = (Listener) listener;
    }

    /**
     * This listener used for update info with mode.
     */
    public interface Listener {
        /**
         * This method used for notify mode dof level.
         * @param level Video shallow dof level
         */
        public void notifyVsDofLevel(String level);
    }

    /**
     * This class used for view update.
     */
    private class ViewHandler extends Handler {

        public ViewHandler(Looper looper) {
           super(looper);
        }
       @Override
       public void handleMessage(Message msg) {
           Log.i(TAG, "[handleMessage]msg.what= " + msg.what);
            switch (msg.what) {
            case MSG_DOF_SHOW:
                 mDofView.setVisibility(View.VISIBLE);
            break;
            case MSG_DOF_HIDE:
                mDofView.setVisibility(View.GONE);
            break;
            default:
            break;
            }
       }
    }
}
