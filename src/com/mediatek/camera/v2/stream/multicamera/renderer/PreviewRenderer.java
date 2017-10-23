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
package com.mediatek.camera.v2.stream.multicamera.renderer;

import android.app.Activity;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;

import java.nio.FloatBuffer;

/**
 * GraphicRenderer.
 *
 */
public class PreviewRenderer extends Renderer {
    private static final Tag TAG = new Tag(
            PreviewRenderer.class.getSimpleName());
    private static final int TEMP_SIZE_NUM2 = 2;
    private static final int TEMP_SIZE_NUM3 = 3;
    private static final int TEMP_SIZE_NUM4 = 4;
    private static final int TEMP_SIZE_NUM6 = 6;
    private static final int ORITATION_NUM_180 = 180;
    private static final float TRANSLATE_HALF_RECT = 0.5f;

    // position
    private FloatBuffer mVtxBuf;
    private FloatBuffer mTexCoordBuf;
    // matrix
    private float[] mMVPMtx = GLUtil.createIdentityMtx();
    private float[] mMMtx = GLUtil.createIdentityMtx();
    private float[] mVMtx = GLUtil.createIdentityMtx(); // view
    private float[] mPMtx = GLUtil.createIdentityMtx(); // projection

    private int mPositionHandle = -1;
    private int mTexCoordHandle = -1;
    private int mPosMtxHandle = -1;
    private int mTexMtxHandle = -1;
    private int mTexRotateMtxHandle = -1;

    private int mPreviewSamplerHandle = -1;

    final String mVertexShader = "attribute vec4   aPosition;\n"
            + "attribute vec4   aTexCoord;\n"
            + "uniform   mat4   uPosMtx;\n"
            + "uniform   mat4   uTexMtx;\n"
            + "uniform   mat4   uTexRotateMtx;\n"
            + "varying   vec2   vTexCoord;\n"
            + "void main() {\n"
            + "    gl_Position    = uPosMtx * aPosition;\n"
            + "    vTexCoord     = (uTexRotateMtx * uTexMtx * aTexCoord).xy;\n"
            + "}\n";

    final String mFragmentShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform   samplerExternalOES uPreviewSampler;\n"
            + "varying   vec2      vTexCoord;\n"
            + "void main() {\n"
            + "            gl_FragColor = texture2D(uPreviewSampler, vTexCoord);\n"
            + "}\n";

    /**
     * GraphicRenderer.
     *
     * @param activity
     *            activity
     * @param config
     *            render configuration
     */
    public PreviewRenderer(Activity activity, RendererConfig config) {
        super(activity, config);
        LogHelper.i(TAG, "TopGraphicRenderer");
        initProgram();
        mTexCoordBuf = GLUtil.createFloatBuffer(mTexCoordBuf, GLUtil.createTexCoord());
    }

    @Override
    public void setRendererSize(int width, int height) {
        LogHelper.i(TAG, "setRendererSize width = " + width + " height = " + height);
        int selectedWidth;
        int selectedHeight;
        if (mIsNeedPortrait) {
            selectedWidth = Math.min(width, height);
            selectedHeight = Math.max(width, height);
        } else {
            selectedWidth = Math.max(width, height);
            selectedHeight = Math.min(width, height);
        }
        resetMatrix();
        Matrix.orthoM(mPMtx, 0, 0, selectedWidth, 0, selectedHeight, -1, 1);
        if (mIsNeedPortrait) {
            Matrix.translateM(mMMtx, 0, 0, selectedHeight, 0);
            Matrix.scaleM(mMMtx, 0, mMMtx, 0, 1, -1, 1);
        }
        Matrix.multiplyMM(mMVPMtx, 0, mMMtx, 0, mMVPMtx, 0);
        Matrix.multiplyMM(mMVPMtx, 0, mVMtx, 0, mMVPMtx, 0);
        Matrix.multiplyMM(mMVPMtx, 0, mPMtx, 0, mMVPMtx, 0);
        super.setRendererSize(selectedWidth, selectedHeight);
    }

    @Override
    public void draw() {
        final int textureId = super.getPreviewStWrapper().getTextureId();
        final float[] preTexMtx = super.getPreviewStWrapper().getBufferTransformMatrix();
        final float[] rotatebuf = GLUtil.createIdentityMtx();
        final AnimationRect vtxRect = super.getRenderRect();
        final int rotation = super.getOrientation();
        if (textureId <= 0 || vtxRect == null) {
            return;
        }
        float scale = 1.0f;
        float sWidth = vtxRect.getRectF().width();
        float sHeigth = vtxRect.getRectF().height();
        float pWidth = getRendererWidth();
        float pHeight = getRendererHeight();

        if (mIsMultiPreview && mConfig.isNeedRotate()) {
            Matrix.translateM(rotatebuf, 0, TRANSLATE_HALF_RECT, TRANSLATE_HALF_RECT, 0);
            Matrix.rotateM(rotatebuf, 0, rotation, 0, 0, 1);
            Matrix.translateM(rotatebuf, 0, -TRANSLATE_HALF_RECT, -TRANSLATE_HALF_RECT, 0);
            if (rotation % ORITATION_NUM_180 != 0) {
                pWidth = getRendererHeight();
                pHeight = getRendererWidth();
            }
            scale = Math.min(pWidth, pHeight) / Math.max(sWidth, sHeigth);
        } else {
            scale = Math.min(pWidth / sWidth, pHeight / sHeigth);
        }
        float lowWidth = (pWidth - sWidth * scale) / (2 * pWidth);
        float highWidth = (pWidth + sWidth * scale) / (2 * pWidth);
        float lowHeight = (pHeight - sHeigth * scale) / (2 * pHeight);
        float highHeight = (pHeight + sHeigth * scale) / (2 * pHeight);

        mTexCoordBuf = GLUtil.createFloatBuffer(mTexCoordBuf,
                GLUtil.createReverseStandTexCoord(lowWidth, highWidth, lowHeight, highHeight));
        mVtxBuf = GLUtil.createFloatBuffer(mVtxBuf, GLUtil.createTopRightRect(vtxRect));
        mVtxBuf.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, TEMP_SIZE_NUM3, GLES20.GL_FLOAT, false,
                TEMP_SIZE_NUM4 * TEMP_SIZE_NUM3, mVtxBuf);
        mTexCoordBuf.position(0);
        GLES20.glVertexAttribPointer(mTexCoordHandle, TEMP_SIZE_NUM2, GLES20.GL_FLOAT, false,
                TEMP_SIZE_NUM4 * TEMP_SIZE_NUM2,
                mTexCoordBuf);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        // draw matrix
        GLES20.glUniformMatrix4fv(mPosMtxHandle, 1, false, mMVPMtx, 0);
        GLES20.glUniformMatrix4fv(mTexMtxHandle, 1, false,
                (preTexMtx == null) ? GLUtil.createIdentityMtx() : preTexMtx, 0);
        GLES20.glUniformMatrix4fv(mTexRotateMtxHandle, 1, false, rotatebuf, 0);

        // sampler
        GLES20.glUniform1i(mPreviewSamplerHandle, 0);
        // texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture((preTexMtx == null) ? GLES20.GL_TEXTURE_2D
                : GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, TEMP_SIZE_NUM6);
        GLUtil.checkGlError("TopGraphicRenderer draw end");
    }

    private void initProgram() {
        int program = GLUtil.createProgram(mVertexShader, mFragmentShader);
        GLES20.glUseProgram(program);
        // position
        mPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        mPosMtxHandle = GLES20.glGetUniformLocation(program, "uPosMtx");
        mTexMtxHandle = GLES20.glGetUniformLocation(program, "uTexMtx");
        mTexRotateMtxHandle = GLES20.glGetUniformLocation(program, "uTexRotateMtx");
        // sampler
        mPreviewSamplerHandle = GLES20.glGetUniformLocation(program, "uPreviewSampler");
    }

    private void resetMatrix() {
        mMVPMtx = GLUtil.createIdentityMtx();
        mPMtx = GLUtil.createIdentityMtx();
        mVMtx = GLUtil.createIdentityMtx();
        mMMtx = GLUtil.createIdentityMtx();
    }
}
