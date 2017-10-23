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

import android.graphics.Matrix;
import android.graphics.RectF;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;

/**
 *
 * AnimationRect .
 *
 */
public class AnimationRect {
    private static final Tag TAG = new Tag(
            AnimationRect.class.getSimpleName());
    private static final float MAX_SCALE_VALUE = 1.4f;
    private static final float MIN_SCALE_VALUE = 0.6f;
    private float mCurrentScaleValue = 1.0f;
    private Matrix mAnimationMatrix;
    private float mOriginalDistance = 0f;
    private RectF mRectF;
    private int mPreviewWidth = -1;
    private int mPreviewHeight = -1;
    private float mCurrentRotationValue = 0f;
    private float[] mLeftTop = new float[] { 0f, 0f };
    private float[] mRightTop = new float[] { 0f, 0f };
    private float[] mLeftBottom = new float[] { 0f, 0f };
    private float[] mRightBottom = new float[] { 0f, 0f };
    private boolean mIsHighlightEnable = false;

    /**
     * getLeftTop .
     *
     * @return left top
     */
    public float[] getLeftTop() {
        return mLeftTop;
    }

    /**
     * getRightTop .
     *
     * @return right top
     */
    public float[] getRightTop() {
        return mRightTop;
    }

    /**
     * getLeftBottom .
     *
     * @return left bottom data
     */
    public float[] getLeftBottom() {
        return mLeftBottom;
    }

    /**
     * getRightBottom .
     *
     * @return right bottom
     */
    public float[] getRightBottom() {
        return mRightBottom;
    }

    /**
     * constructor .
     */
    public AnimationRect() {
        mAnimationMatrix = new Matrix();
        mRectF = new RectF();
    }

    /**
     * setRendererSize .
     *
     * @param width
     *            width
     * @param height
     *            height
     */
    public void setRendererSize(int width, int height) {
        // reduce edge / 2
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    /**
     * initialize .
     *
     * @param left
     *            left
     * @param top
     *            top
     * @param right
     *            right
     * @param bottom
     *            bottom
     */
    public void initialize(float left, float top, float right, float bottom) {
        mRectF.set(left, top, right, bottom);
        setVetex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mOriginalDistance = (float) Math.sqrt((centerX() - mRightBottom[0])
                * (centerX() - mRightBottom[0]) + (centerY() - mRightBottom[1])
                * (centerY() - mRightBottom[1]));
    }

    /**
     * getRectF .
     *
     * @return get rect
     */
    public RectF getRectF() {
        return mRectF;
    }

    /**
     * scale .
     *
     * @param scale
     *            scale data
     * @param checkScale
     *            whether check scale data
     */
    public void scale(float scale, boolean checkScale) {
        LogHelper.i(TAG, "Before setScale scale = " + scale + " getMaxScaleValue = " +
                   getMaxScaleValue() + " getMinScaleValue = " + getMinScaleValue());
        if (checkScale) {
            float scaleValue = mCurrentScaleValue * scale;
            // check max scale value
            if (scale > 1) {
                scale = scaleValue > getMaxScaleValue() ? 1f : scale;
            }
            // check minimal scale value
            if (scale < 1) {
                scale = scaleValue < getMinScaleValue() ? 1f : scale;
            }
            mCurrentScaleValue = mCurrentScaleValue * scale;
        }
        LogHelper.i(TAG, "setScale mCurrentScaleValue = " + mCurrentScaleValue);
        mAnimationMatrix.reset();
        mAnimationMatrix.setScale(scale, scale, mRectF.centerX(), mRectF.centerY());
        mAnimationMatrix.mapRect(mRectF);
        setVetex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mOriginalDistance = (float) Math.sqrt((centerX() - mRightBottom[0])
                * (centerX() - mRightBottom[0]) + (centerY() - mRightBottom[1])
                * (centerY() - mRightBottom[1]));
        LogHelper.i(TAG, "After setScale scale = " + scale);
    }

    /**
     * rotate .
     *
     * @param degrees
     *            rotate degree
     */
    public void rotate(float degrees) {
        rotate(degrees, mRectF.centerX(), mRectF.centerY());
    }

    /**
     * copy .
     *
     * @return new AnimationRect
     */
    public AnimationRect copy() {
        AnimationRect resultRect = new AnimationRect();
        resultRect.mCurrentScaleValue = this.mCurrentScaleValue;
        resultRect.mAnimationMatrix.set(this.mAnimationMatrix);
        resultRect.mOriginalDistance = this.mOriginalDistance;
        resultRect.mRectF.set(this.mRectF);
        resultRect.mPreviewWidth = this.mPreviewWidth;
        resultRect.mPreviewHeight = this.mPreviewHeight;
        resultRect.mCurrentRotationValue = this.mCurrentRotationValue;
        resultRect.setLeftTop(this.getLeftTop());
        resultRect.setRightTop(this.getRightTop());
        resultRect.setLeftBottom(this.getLeftBottom());
        resultRect.setRightBottom(this.getRightBottom());
        resultRect.setHighLightEnable(this.mIsHighlightEnable);
        return resultRect;
    }

    private void setLeftTop(float[] lefttop) {
        mLeftTop[0] = lefttop[0];
        mLeftTop[1] = lefttop[1];
    }

    private void setHighLightEnable(boolean highlight) {
        mIsHighlightEnable = highlight;
    }


    private float centerX() {
        return (mRightTop[0] + mLeftBottom[0]) / 2;
    }

    private float centerY() {
        return (mRightTop[1] + mLeftBottom[1]) / 2;
    }

    private float getMaxScaleValue() {
        float maxScaleValue = Math.min(getXMaxScaleValue(), getYMaxScaleValue());
        LogHelper.i(TAG, "getMaxScaleValue maxScaleValue = " + maxScaleValue +
                " mCurrentScaleValue = " + mCurrentScaleValue);
        maxScaleValue = mCurrentScaleValue * maxScaleValue;
        return maxScaleValue > MAX_SCALE_VALUE ? MAX_SCALE_VALUE : maxScaleValue;
    }

    private float getMinScaleValue() {
        return MIN_SCALE_VALUE;
    }

    private float getXMaxScaleValue() {
        return Math.min(
                (float) Math.sqrt((centerX() * centerX())
                        / ((centerX() - mLeftTop[0]) * (centerX() - mLeftTop[0]))),
                (float) Math.sqrt(((mPreviewWidth - centerX()) * (mPreviewWidth - centerX()))
                        / ((mRightBottom[0] - centerX())) * ((mRightBottom[0] - centerX()))));
    }

    private float getYMaxScaleValue() {
        return Math.min(
                (float) Math.sqrt((centerY() * centerY())
                        / ((centerY() - mLeftTop[1]) * (centerY() - mLeftTop[1]))),
                (float) Math.sqrt(((mPreviewHeight - centerY()) * (mPreviewHeight - centerY()))
                        / ((mRightBottom[1] - centerY())) * ((mRightBottom[1] - centerY()))));
    }

    private static void dumpVertex(AnimationRect rect) {
        LogHelper.i(TAG, "Dump Vertex Animation Rect begin");
        LogHelper.i(TAG, "(" + rect.getLeftTop()[0] + " , " + rect.getLeftTop()[1] + ")" +
                   " , " + "(" + rect.getRightTop()[0] + " , " + rect.getRightTop()[1] + ")");
        LogHelper.i(TAG, "(" + rect.getLeftBottom()[0] + " , " + rect.getLeftBottom()[1] + ")" +
                   "," + "(" + rect.getRightBottom()[0] + " , " + rect.getRightBottom()[1] + ")");
        LogHelper.i(TAG, "Dump Vertex Animation Rect end");
        LogHelper.i(TAG, "(centerX , centerY) = " + "(" + rect.centerX() + " , " +
                   rect.centerY() + ")");
    }

    private void setLeftBottom(float[] leftbottom) {
        mLeftBottom[0] = leftbottom[0];
        mLeftBottom[1] = leftbottom[1];
    }

    private void setRightTop(float[] righttop) {
        mRightTop[0] = righttop[0];
        mRightTop[1] = righttop[1];
    }

    private void setRightBottom(float[] rightbottom) {
        mRightBottom[0] = rightbottom[0];
        mRightBottom[1] = rightbottom[1];
    }

    private void setVetex(float left, float top, float right, float bottom) {
        mLeftTop[0] = left;
        mLeftTop[1] = top;
        mRightTop[0] = right;
        mRightTop[1] = top;
        mLeftBottom[0] = left;
        mLeftBottom[1] = bottom;
        mRightBottom[0] = right;
        mRightBottom[1] = bottom;
    }

    private void rotate(float degrees, float centerX, float centerY) {
        LogHelper.i(TAG, "setRotate");
        setVetex(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom);
        mAnimationMatrix.reset();
        mAnimationMatrix.setRotate(degrees, centerX, centerY);
        mAnimationMatrix.mapPoints(mLeftTop);
        mAnimationMatrix.mapPoints(mRightTop);
        mAnimationMatrix.mapPoints(mLeftBottom);
        mAnimationMatrix.mapPoints(mRightBottom);
        mCurrentRotationValue = degrees;
    }
}
