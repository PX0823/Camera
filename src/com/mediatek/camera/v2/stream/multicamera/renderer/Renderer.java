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
package com.mediatek.camera.v2.stream.multicamera.renderer;

import android.app.Activity;
import android.view.Surface;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;

import junit.framework.Assert;

/**
 * Almost all Renderer's methods should be called in GL Thread.
 */
public abstract class Renderer {
    private static final Tag TAG = new Tag(Renderer.class.getSimpleName());

    private static final int INTERVALS = 300;
    private static final float MINISECOND_PER_SECOND = 1000.0f;

    protected final RendererConfig mConfig;

    private final Activity mActivity;
    private final RotateAnimation mRotateAnimaiton;

    private SurfaceTextureWrapper mPreviewStWrapper = null;
    private SurfaceTextureWrapper mCaptureStWrapper = null;

    private AnimationRect mRenderRect = null;

    private long mDrawStartTime = 0;
    protected boolean mIsNeedPortrait = false;
    protected boolean mIsMultiPreview = false;

    // debug info for draw preview.
    private int mDrawFrameCount = 0;
    private int mTextureId = -1;
    private int mOrientation;
    private int mRendererWidth = 0;
    private int mRendererHeight = 0;

    /**
     * Constructor.
     *
     * @param activity
     *            activity
     * @param config
     *            config
     */
    public Renderer(Activity activity, RendererConfig config) {
        Assert.assertNotNull(config);
        mActivity = activity;
        mConfig = config;
        mRotateAnimaiton = new RotateAnimation();
    }

    /**
     * setPreviewStWrapper,can get preview surface texture from this.
     *
     * @param stWrapper
     *            surface texture
     */
    public void setPreviewStWrapper(SurfaceTextureWrapper stWrapper) {
        mPreviewStWrapper = stWrapper;
    }

    /**
     * getConfig, get renderer configuration.
     *
     * @return config
     */
    public RendererConfig getConfig() {
        return mConfig.copy();
    }

    /**
     * if portrait,render width smaller than render height.
     *
     * @param isNeedPortrait
     *            whether portrait
     */
    public void setNeedPortrait(boolean isNeedPortrait) {
        mIsNeedPortrait = isNeedPortrait;
    }

    public void setMultiPreview(boolean isMultiPreview) {
        mIsMultiPreview = isMultiPreview;
    }

    /**
     * getPreviewStWrapper, can get preivew surface texture from this.
     *
     * @return surface texture
     */
    public SurfaceTextureWrapper getPreviewStWrapper() {
        return mPreviewStWrapper;
    }

    /**
     * getCaptureWrapper, can get capture surface texture from this.
     *
     * @return surface texture
     */
    public SurfaceTextureWrapper getCaptureWrapper() {
        return mCaptureStWrapper;
    }

    /**
     * setCaptureStWrapper.
     *
     * @param stWrapper
     *            surface texture
     */
    public void setCaptureStWrapper(SurfaceTextureWrapper stWrapper) {
        mCaptureStWrapper = stWrapper;
    }

    /**
     * setImageTextureId,set image texture id.
     *
     * @param id
     *            image texture id
     */
    public void setTextureId(int id) {
        mTextureId = id;
    }

    /**
     * getImageTextureId,get image texture id.
     *
     * @return texture id
     */
    public int getTextureId() {
        return mTextureId;
    }

    /**
     * set renderer region, if orientation is not 0, will rotation and scale
     * base on orientation.
     *
     * @param rect
     *            animation rect
     */
    public void setRenderRect(AnimationRect rect) {
        mRenderRect = rect;
        if (mConfig.isNeedRotate()) {
            mRotateAnimaiton.setRenderRect(rect);
        }
    }

    /**
     * get renderer region, if animation is doing, will get new region which
     * will rotate and scale base on new orientation.
     *
     * @return animation rect
     */
    public AnimationRect getRenderRect() {
        if (mRenderRect == null) {
            return null;
        }
        if (mConfig.isNeedRotate() && !mIsMultiPreview) {
            return mRotateAnimaiton.getRenderRect();
        } else {
            return mRenderRect.copy();
        }
    }

    /**
     * judge render rect is valid.
     *
     * @return valid->true
     */
    public boolean isValidRenderRect() {
        if (mRenderRect == null) {
            return false;
        }
        float width = mRenderRect.getRectF().width();
        float height = mRenderRect.getRectF().height();
        return (width > 1 && height > 1);
    }

    /**
     * getActivity.
     *
     * @return activity
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * getRendererWidth.
     *
     * @return width
     */
    public int getRendererWidth() {
        return mRendererWidth;
    }

    /**
     * getRendererHeight.
     *
     * @return height
     */
    public int getRendererHeight() {
        return mRendererHeight;
    }

    /**
     * debugFrameRate, get current draw frame rate.
     *
     * @param tag
     *            tag
     */
    public void debugFrameRate(Tag tag) {
        mDrawFrameCount++;
        if (mDrawFrameCount % INTERVALS == 0) {
            long currentTime = System.currentTimeMillis();
            int intervals = (int) (currentTime - mDrawStartTime);
            LogHelper.i(tag, "[Wrapping-->" + tag + "]" + "[Preview] Drawing frame, fps = "
                    + (mDrawFrameCount * MINISECOND_PER_SECOND) / intervals + " in last "
                    + intervals + " millisecond.");
            mDrawStartTime = currentTime;
            mDrawFrameCount = 0;
        }
    }

    /**
     * release.
     */
    public void release() {
        if (mPreviewStWrapper != null) {
            mPreviewStWrapper.release();
            mPreviewStWrapper = null;
        }
        if (mCaptureStWrapper != null) {
            mCaptureStWrapper.release();
            mCaptureStWrapper = null;
        }
    }

    /**
     * update orientation, and do animation.
     *
     * @param orientation
     *            new orientation
     */
    public void updateOrientation(int orientation) {
        if (mConfig.isNeedRotate() && !mIsMultiPreview) {
            mRotateAnimaiton.updateOrientation(orientation);
        }
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    /**
     * set render width and height.
     *
     * @param width
     *            render width
     * @param height
     *            render height
     */
    public void setRendererSize(int width, int height) {
        mRendererWidth = width;
        mRendererHeight = height;
    }

    /**
     * initialize render.
     */
    public void init() {
    }

    /**
     * set surface to renderer, render draw texture to this surface.
     *
     * @param surface
     *            surface
     */
    public void setSurface(Surface surface) {
    }

    /**
     * start do something.
     */
    public void start() {
    }

    /**
     * stop do something.
     */
    public void stop() {
    }

    /**
     * draw texture to surface.
     */
    abstract public void draw();
}
