package com.mediatek.camera.v2.stream.multicamera.renderer;

import com.mediatek.camera.debug.LogHelper;
import com.mediatek.camera.debug.LogHelper.Tag;

/**
 * do rotation animation.
 */
public class RotateAnimation {
    private static final Tag TAG = new Tag(RotateAnimation.class.getSimpleName());

    private static final int ROTATE_DEGREE_90 = 90;
    private static final int ROTATE_DEGREE_180 = 180;
    private static final int ROTATE_DEGREE_270 = 270;
    private static final int ROTATE_DEGREE_360 = 360;
    private static final long NO_ANIMATION = -1;

    private final int mAnimationDuration = 300;

    private long mAnimationStartTime = NO_ANIMATION;

    private AnimationRect mOriRenderRect = null;
    private AnimationRect mRenderRect = null;

    private float mCurScale = 1;
    private float mFromScale = 0;
    private float mToScale = 0;

    private int mOrientation = 0;
    private int mCurOrientation = 0;
    private int mFromOrientation = 0;
    private int mToOrientation = 0;

    /**
     * update render rect, if orientation is not zero, rotate rect.
     *
     * @param rect
     *            new render rect
     */
    public void setRenderRect(AnimationRect rect) {
        mOriRenderRect = rect;
        if (mOrientation != 0) {
            double width = mOriRenderRect.getRectF().width();
            double height = mOriRenderRect.getRectF().height();
            float scale = (float) (Math.min(width, height) / Math.max(width, height));

            mRenderRect = mOriRenderRect.copy();
            if (mOrientation % ROTATE_DEGREE_180 != 0) {
                mRenderRect.scale(scale, false);
            }
            mRenderRect.rotate(mOrientation);
        } else {
            mRenderRect = mOriRenderRect.copy();
        }
    }

    /**
     * get current rect after rotate.
     *
     * @return current render rect
     */
    public AnimationRect getRenderRect() {
        advanceAnimation();
        return mRenderRect.copy();
    }

    /**
     * update orientation, it will rotate rect according new orientation.
     *
     * @param orientation
     *            new orientation
     */
    public void updateOrientation(int orientation) {
        int toOrientation = (ROTATE_DEGREE_360 - orientation) % ROTATE_DEGREE_360;
        if (mOrientation != orientation && mOriRenderRect != null) {
            LogHelper.d(TAG, "updateOrientation, orientation:" + orientation + ",mOrientation:"
                    + mOrientation);
            double width = mOriRenderRect.getRectF().width();
            double height = mOriRenderRect.getRectF().height();
            float scale = (float) (Math.min(width, height) / Math.max(width, height));

            if ((toOrientation - mOrientation + ROTATE_DEGREE_360) % ROTATE_DEGREE_180 != 0) {
                int from = ((mOrientation == ROTATE_DEGREE_270 && toOrientation !=
                        ROTATE_DEGREE_180) ? -ROTATE_DEGREE_90 : mOrientation);
                int to = ((mOrientation != ROTATE_DEGREE_180 && toOrientation == ROTATE_DEGREE_270)
                        ? -ROTATE_DEGREE_90 : toOrientation);

                if (toOrientation % ROTATE_DEGREE_180 != 0) {
                    doAnimation(from, to, 1.0f, scale);
                } else {
                    doAnimation(from, to, scale, 1.0f);
                }
            } else {
                mRenderRect = mOriRenderRect.copy();
                if (toOrientation % ROTATE_DEGREE_180 != 0) {
                    mRenderRect.scale(scale, false);
                }
                mRenderRect.rotate(toOrientation);
            }
        }
        mOrientation = toOrientation;
    }

    private void doAnimation(int fromOrientation, int toOrientation, float fromScale,
            float toScale) {
        mFromOrientation = fromOrientation;
        mToOrientation = toOrientation;
        mFromScale = fromScale;
        mToScale = toScale;
        mAnimationStartTime = AnimationTime.startTime();
        advanceAnimation();
    }

    private void advanceAnimation() {
        if (mAnimationStartTime == NO_ANIMATION) {
            return;
        }

        float progress;
        if (mAnimationDuration == 0) {
            progress = 1;
        } else {
            long now = AnimationTime.get();
            progress =
                    (float) (now - mAnimationStartTime) / mAnimationDuration;
        }

        if (progress >= 1) {
            progress = 1;
        }

        boolean done = interpolate(progress);

        mRenderRect = mOriRenderRect.copy();
        LogHelper.d(TAG, "advanceAnimation,mCurOrientation:" + mCurOrientation + ",mCurScale:"
                + mCurScale + ",progress:" + progress);
        mRenderRect.scale(mCurScale, false);
        mRenderRect.rotate(mCurOrientation);

        if (done) {
            mAnimationStartTime = NO_ANIMATION;
        }
    }

    private boolean interpolate(float progress) {
        if (progress >= 1) {
            mCurScale = mToScale;
            mCurOrientation = mToOrientation;
            return true;
        } else {
            mCurScale = mFromScale + progress * (mToScale - mFromScale);
            mCurOrientation = (int) (mFromOrientation + progress
                    * (mToOrientation - mFromOrientation));

            return (mCurScale == mToScale && mCurOrientation == mToOrientation);
        }
    }
}
