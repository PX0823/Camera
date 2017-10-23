package com.mediatek.camera.v2.stream.multicamera;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.android.camera.R;

import com.mediatek.camera.v2.control.ControlHelper;
import com.mediatek.camera.v2.control.IControl.AutoFocusState;
import com.mediatek.camera.v2.control.IControl.FocusStateListener;
import com.mediatek.camera.v2.control.focus.AutoFocusRotateLayout;
import com.mediatek.camera.v2.control.focus.IFocus;
import com.mediatek.camera.v2.module.ModuleListener.CaptureType;
import com.mediatek.camera.v2.module.ModuleListener.RequestType;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.platform.device.CameraDeviceManager;
import com.mediatek.camera.v2.services.ISoundPlayback;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class used for control remote camera focus.
 */
public class RemoteTouchFocus implements IFocus {

    /**
     * Remote camera device focus listener.
     */
    public interface IRemoteAaaListener {
        /**
         * Get current repeat request type.
         * @return The request type.
         */
        public RequestType getRepeatingRequestType();

        /**
         * change current request,such as preview and capture.
         * @param id
         *            which camera need to configure the request.
         * @param sync
         *            true request immediately. false wait all requests been
         *            submitted and remove the same request current design is only
         *            for
         *            {@link ISettingChangedListener#onSettingChanged(java.util.Map)}
         * @param requestType
         *            the required request, which is one of the {@link RequestType}
         * @param captureType
         *            the required capture, which is one of the {@link CaptureType}
         */
        public void requestChangeCaptureRequets(boolean sync, String id, RequestType requestType,
                CaptureType captureType);
    }

    private static final String TAG = RemoteTouchFocus.class.getSimpleName();
    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = 500;
    private static final int FOCUS_TIME_OUT_TIME = 5000;
    private boolean mTapToFocusWaitForActiveScan = false;
    private boolean mIsAfTriggerRequestSubmitted = false;
    // make sure focus box will not be cleared by face detection during auto
    // focusing.
    private boolean mAfTriggerEnabled = false;
    private int mAfMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    private int mLastResultAFState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    private int mLocalOrientation = 0;
    private int mRemoteOrientation = 0;
    // Last frame for which CONTROL_AF_STATE was received.
    private long mLastControlAfStateFrameNumber = -1;
    private String mCameraId;

    private final Activity mActivity;
    private final AppController mAppController;
    private final Handler mMainHandler;
    private final ISoundPlayback mSoundPlayer;

    private ArrayList<String> mCaredSettingChangedKeys = new ArrayList<String>();
    private IRemoteAaaListener mAaaListener;

    private MeteringRectangle[] mAFRegions = ControlHelper.ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAERegions = ControlHelper.ZERO_WEIGHT_3A_REGION;
    private RectF mPreviewArea;
    private RectF mFocusArea = new RectF();
    private Matrix mPointMatrix;

    private AutoFocusRotateLayout mAutoFocusRotateLayout;
    private ViewGroup mParentViewGroup;
    private ViewGroup mAutoFocusParentViewGroup;
    private View mFocusIndicator;
    private ArrayList<FocusStateListener> mFocusStateListener = new ArrayList<FocusStateListener>();

    /**
     * The remote touch focus constructor.
     * @param soundPlayer  Focus status sound player.
     * @param app APP controller.
     * @param aaaListener Listener the focus status change.
     * @param parentViewGroup Focus view root.
     */
    public RemoteTouchFocus(ISoundPlayback soundPlayer, AppController app,
            IRemoteAaaListener aaaListener, ViewGroup parentViewGroup) {
        mAaaListener = aaaListener;
        mSoundPlayer = soundPlayer;
        mAppController = app;
        mActivity = app.getActivity();
        mMainHandler = new Handler(mActivity.getMainLooper());
        mParentViewGroup = parentViewGroup;
    }

    @Override
    public void open(Activity activity, ViewGroup parentView, boolean isCaptureIntent) {
        intializeFocusUi(mParentViewGroup);
        updateCaredSettingChangedKeys(SettingKeys.KEY_CAMERA_ID);
    }

    @Override
    public void resume() {
        Log.i(TAG, "[resume]+");
        mIsAfTriggerRequestSubmitted = false;
        mTapToFocusWaitForActiveScan = false;
        mLastControlAfStateFrameNumber = -1;
        mAfMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        mLastResultAFState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
        resetTouchFocus();
        Log.i(TAG, "[resume]-");
    }

    @Override
    public void pause() {

    }

    @Override
    public void close() {
        Log.i(TAG, "[close]+");
        unIntializeFocusUi(mParentViewGroup);
        Log.i(TAG, "[close]-");
    }

    @Override
    public void onOrientationCompensationChanged(int orientationCompensation) {
        mAutoFocusRotateLayout.setOrientation(orientationCompensation, true);
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        Log.i(TAG, "onPreviewAreaChanged width = " + previewArea.width() + " height = "
                + previewArea.height());
        mPointMatrix = new Matrix();
        mPreviewArea = previewArea;
        // Set the length of focus indicator according to preview frame size.
        int len = Math.min((int) mPreviewArea.width(), (int) mPreviewArea.height()) / 4;
        ViewGroup.LayoutParams layout = mFocusIndicator.getLayoutParams();
        layout.width = len;
        layout.height = len;
        mFocusIndicator.requestLayout();
    }

    public void setLocalOrientation(int orientation) {
        mLocalOrientation = orientation;
    }

    public void setRemoteOrientation(int orientation) {
        mRemoteOrientation = orientation;
    }

    /**
     * The single tap up.
     * @param x The touch x position
     * @param y The touch y position
     * @param id Current preview camera id.
     * @param cameraManager Current camera manager instance.
     */
    public void onSingleTapUp(float x, float y, String id,
                                       CameraDeviceManager cameraManager) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("x = ").append(x).append(" y = ").append(y).append("id = ").append(id)
                .append(" mPreviewArea = ").append(mPreviewArea)
                .append(" mIsAfTriggerRequestSubmitted = ").append(mIsAfTriggerRequestSubmitted);
        Log.i(TAG, "onSingleTapUp " + stringBuilder.toString());

        mCameraId = id;

        if (mPreviewArea == null) {
            return;
        }

        // set the focus area to full buffer preview.
        mFocusArea.set(0, 0, mPreviewArea.right - mPreviewArea.left,
                mPreviewArea.bottom - mPreviewArea.top);
        // if (x,y) is not in previewArea, should return directly.
        if (!isTouchinPreviewArea(x, y)) {
            return;
        }
        CameraCharacteristics characteristics = Utils
                .getCameraCharacteristics(mActivity, mCameraId);

        if (!hasFocuser(characteristics)) {
            return;
        }
        mAfTriggerEnabled = true;
        // Cancel any scheduled auto focus target UI actions.
        mMainHandler.removeCallbacks(mReturnToContinuousAFRunnable);
        // resume to continuous focus if auto focus is time out.
        mMainHandler.removeCallbacks(mFocusTimeOutRunnable);
        mMainHandler.postDelayed(mFocusTimeOutRunnable, FOCUS_TIME_OUT_TIME);
        mAppController.getCameraAppUi().setAllCommonViewEnable(false);
        // notify multiCameraModule disable module UI.
        notifyFocusStateChanged(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
        resetTouchFocus();
        Log.i(TAG, "[onSingleTapUp] mIsAfTriggerRequestSubmitted=" + mIsAfTriggerRequestSubmitted);
        if (mIsAfTriggerRequestSubmitted) {
            sendAutoFocusCancelCaptureRequest(mCameraId);
        }
        mTapToFocusWaitForActiveScan = true;
        Math.min(mPreviewArea.width(), mPreviewArea.height());

        // Use margin to set the focus indicator to the touched area.
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mAutoFocusRotateLayout
                .getLayoutParams();
        int left = 0;
        int top = 0;
        int focusWidth = mAutoFocusRotateLayout.getWidth();
        int focusHeight = mAutoFocusRotateLayout.getHeight();
        Log.i(TAG, "focus area width = " + focusWidth + " height = " + focusHeight);
        left = clamp((int) x - focusWidth / 2, 0, (int) mPreviewArea.width() - focusWidth);
        top = clamp((int) y - focusHeight / 2, 0, (int) mPreviewArea.height() - focusHeight);
        left += mPreviewArea.left;
        top += mPreviewArea.top;
        Log.i(TAG, "left = " + left + " top = " + top + " focus area width = " + focusWidth
                + " height = " + focusHeight);
        if (p.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL) {
            p.setMargins(left, top, 0, 0);
        } else {
            // since in RTL language, framework will use marginRight as
            // standard.
            int right = (int) mPreviewArea.width() - (left + focusWidth);
            p.setMargins(0, top, right, 0);
        }
        // Disable "center" rule because we no longer want to put it in the
        // center.
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = 0;
        mMainHandler.removeCallbacks(mClearAutoFocusUIRunnable);
        mAutoFocusRotateLayout.requestLayout();
        mAutoFocusRotateLayout.onFocusStarted();
        // if remote preview orientation is 90 or 270, need map points to source.
        mapFocusPointsToSrc(x, y);
        // Normalize coordinates to [0,1] .
        float points[] = new float[2];
        points[0] = (x - mPreviewArea.left) / mPreviewArea.width();
        points[1] = (y - mPreviewArea.top) / mPreviewArea.height();
        // Rotate coordinates to portrait orientation .
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(Utils.getDisplayRotation(mActivity), 0.5f, 0.5f);
        rotationMatrix.mapPoints(points);

        Log.i(TAG, "onSingleTapUp points[0]:" + points[0] + " points[1]:" + points[1]);
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Rect cropRegion = Utils.cropRegionForZoom(mActivity, mCameraId, 1f);
        mAERegions = ControlHelper.aeRegionsForNormalizedCoord(points[0], points[1], cropRegion,
                sensorOrientation);
        mAFRegions = ControlHelper.afRegionsForNormalizedCoord(points[0], points[1], cropRegion,
                sensorOrientation);
        sendAutoFocusTriggerCaptureRequest(mCameraId);
    }

    @Override
    public void onSingleTapUp(float x, float y) {
    }

    /**
     * Configuring capture requests.
     * @param requestBuilders The builders of capture requests.
     * @param captureType The required capture, which is one of the {@link CaptureType}.
     * @param bottomCamera Whether it is the main camera or not.
     */
    public void configuringSessionRequests(Map<RequestType, Builder> requestBuilders,
            CaptureType captureType, boolean bottomCamera) {
        int controlMode = CaptureRequest.CONTROL_MODE_AUTO;
        Set<RequestType> keySet = requestBuilders.keySet();
        Iterator<RequestType> iterator = keySet.iterator();
        Log.i(TAG, "configuringSessionRequests control mode : " + controlMode);

        while (iterator.hasNext()) {
            RequestType requestType = iterator.next();
            CaptureRequest.Builder requestBuilder = requestBuilders.get(requestType);
            requestBuilder.set(CaptureRequest.CONTROL_MODE, controlMode);
            configuringSessionRequest(requestType, requestBuilder, captureType, bottomCamera);
        }
    }

    @Override
    public void configuringSessionRequest(RequestType requestType, Builder requestBuilder,
            CaptureType captureType, boolean bottomCamera) {
        Log.i(TAG, "[configuringSessionRequests] + ");
        // change auto focus mode for video record
        switch (requestType) {
        case RECORDING:
            if (mAfMode == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetTouchFocus();
                    }
                });
                mAfMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
            }
            break;
        case PREVIEW:
            if (mAfMode == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
                mAfMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            }
            break;
        default:
            break;
        }
        addBaselineCaptureKeysToRequest(requestBuilder);
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mAfMode);
        if ((requestType == RequestType.PREVIEW || requestType == RequestType.RECORDING)
                && captureType == CaptureType.CAPTURE
                && mAfMode == CameraMetadata.CONTROL_AF_MODE_AUTO) {
            if (mIsAfTriggerRequestSubmitted) {
                requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_START);
            }
        }
        Log.i(TAG, "[configuringSessionRequests]- requestType = " + requestType + " AFMode:"
                + mAfMode + " captureType:" + captureType + " mIsAfTriggerRequestSubmitted:"
                + mIsAfTriggerRequestSubmitted);
    }

    @Override
    public void onPreviewCaptureStarted(CaptureRequest request, long timestamp, long frameNumber) {
    }

    @Override
    public void onPreviewCaptureProgressed(CaptureRequest request, CaptureResult partialResult) {
    }

    @Override
    public void onPreviewCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        autofocusStateChangeDispatcher(result);
    }

    @Override
    public void clearFocusUi() {
        mIsAfTriggerRequestSubmitted = false;
        mTapToFocusWaitForActiveScan = false;
        mLastControlAfStateFrameNumber = -1;
        mLastResultAFState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAutoFocusRotateLayout.clear();
            }
        });
    }

    @Override
    public void setFocusStateListener(FocusStateListener listener) {
        if (!mFocusStateListener.contains(listener)) {
            mFocusStateListener.add(listener);
        }
    }

    @Override
    public void clearFocusStateListener(FocusStateListener listener) {
        if (mFocusStateListener.contains(listener)) {
            mFocusStateListener.remove(listener);
        }
    }

    private void intializeFocusUi(ViewGroup parentViewGroup) {
        mAutoFocusParentViewGroup = (ViewGroup) mActivity.getLayoutInflater().inflate(
                R.layout.focus_indicator_v2, null, false);
        Log.i(TAG, "intializeFocusUi  mAutoFocusParentViewGroup " + mAutoFocusParentViewGroup);
        mAutoFocusRotateLayout = (AutoFocusRotateLayout) mAutoFocusParentViewGroup
                .findViewById(R.id.focus_indicator_rotate_layout);
        Log.i(TAG, "intializeFocusUi  mAutoFocusRotateLayout " + mAutoFocusRotateLayout);
        mFocusIndicator = mAutoFocusRotateLayout.findViewById(R.id.focus_indicator);
        Log.i(TAG, "intializeFocusUi  mFocusIndicator " + mFocusIndicator);
        parentViewGroup.addView(mAutoFocusParentViewGroup);
    }

    private void unIntializeFocusUi(ViewGroup parentViewGroup) {
        if (mAutoFocusParentViewGroup != null) {
            mAutoFocusParentViewGroup.removeAllViewsInLayout();
            mParentViewGroup.removeView(mAutoFocusParentViewGroup);
        }
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * Request preview capture stream with auto focus trigger cycle.
     */
    private void sendAutoFocusTriggerCaptureRequest(String cameraId) {
        mAfMode = CameraMetadata.CONTROL_AF_MODE_AUTO;
        RequestType requiredRequestType = mAaaListener.getRepeatingRequestType();
        // make a single request to trigger auto focus
        mAaaListener.requestChangeCaptureRequets(true, cameraId, requiredRequestType,
                CaptureType.CAPTURE);
        // change focus mode to auto, tracking focus state
        mAaaListener.requestChangeCaptureRequets(true, cameraId, requiredRequestType,
                CaptureType.REPEATING_REQUEST);
        mIsAfTriggerRequestSubmitted = true;
    }

    /**
     * Request preview capture stream for canceling auto focus .
     */
    private void sendAutoFocusCancelCaptureRequest(String cameraId) {
        mAfMode = CameraMetadata.CONTROL_AF_MODE_AUTO;
        // make a single request to trigger auto focus
        mAaaListener.requestChangeCaptureRequets(true, cameraId,
                mAaaListener.getRepeatingRequestType(), CaptureType.CAPTURE);
        mIsAfTriggerRequestSubmitted = false;
    }

    /**
     * Adds current regions to CaptureRequest and base AF mode +
     * AF_TRIGGER_IDLE.
     * @param builder
     *            Build for the CaptureRequest
     */
    private void addBaselineCaptureKeysToRequest(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, mAFRegions);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, mAERegions);
        builder.set(CaptureRequest.CONTROL_AF_MODE, mAfMode);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
    }

    // Runnable that returns to CONTROL_AF_MODE = AF_CONTINUOUS_PICTURE.
    private final Runnable mReturnToContinuousAFRunnable = new Runnable() {
        @Override
        public void run() {
            mIsAfTriggerRequestSubmitted = false;
            resetTouchFocus();
            mAFRegions = ControlHelper.ZERO_WEIGHT_3A_REGION;
            mAfMode = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            mAaaListener.requestChangeCaptureRequets(true, mCameraId,
                    mAaaListener.getRepeatingRequestType(), CaptureType.REPEATING_REQUEST);
        }
    };

    private final Runnable mClearAutoFocusUIRunnable = new Runnable() {
        @Override
        public void run() {
            mAutoFocusRotateLayout.clear();
        }
    };

    private final Runnable mFocusTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "remote touch focus time out, resume to continuous");
            resumeContinuousAF();
        }
    };

    /**
     * This method takes appropriate action if camera2 AF state changes.
     * <ol>
     * <li>Reports changes in camera2 AF state to OneCamera.FocusStateListener.</li>
     * <li>Take picture after AF scan if mTakePictureWhenLensIsStopped true.</li>
     * </ol>
     */
    private void autofocusStateChangeDispatcher(CaptureResult result) {
        long currentFrameNumber = result.getFrameNumber();
        if (currentFrameNumber < mLastControlAfStateFrameNumber
                || result.get(CaptureResult.CONTROL_AF_STATE) == null) {
            Log.i(TAG, "frame number, last:current "
                    + mLastControlAfStateFrameNumber + ":" + currentFrameNumber
                    + " afState:" + result.get(CaptureResult.CONTROL_AF_STATE));
            return;
        }
        Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
        if (faces != null && faces.length > 0) {
            if (!mAfTriggerEnabled) {
                mMainHandler.post(mClearAutoFocusUIRunnable);
            }
            return;
        }

        if (!mAfTriggerEnabled) {
            // time out run end, so should not response auto focus any more.
            return;
        }

        mLastControlAfStateFrameNumber = result.getFrameNumber();
        int resultAFState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (mLastResultAFState != resultAFState) {
            onFocusStatusUpdate(resultAFState);
        }
        mLastResultAFState = resultAFState;
    }

    private void resetTouchFocus() {
        Log.i(TAG, "resetTouchFocus");
        mAutoFocusRotateLayout.clear();
        // Put focus indicator to the center.
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mAutoFocusRotateLayout
                .getLayoutParams();
        int[] rules = p.getRules();
        p.setMargins(0, 0, 0, 0);
        rules[RelativeLayout.CENTER_IN_PARENT] = RelativeLayout.TRUE;
        mAutoFocusRotateLayout.requestLayout();
    }

    private void resumeContinuousAF() {
        mAfTriggerEnabled = false;
        mAppController.getCameraAppUi().setAllCommonViewEnable(true);
        //If mStopShowCommonUi is true in CameraAppUI,
        //setAllCommonViewEnable(true) don't set thumbnail view enabled.
        mAppController.getCameraAppUi().setThumbnailManagerEnable(true);
        // notify multiCameraModule enable module UI.
        notifyFocusStateChanged(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
        mMainHandler.removeCallbacks(mReturnToContinuousAFRunnable);
        mMainHandler.postDelayed(mReturnToContinuousAFRunnable, RESET_TOUCH_FOCUS_DELAY_MILLIS);
    }

    private void onFocusStatusUpdate(final int resultAFState) {
        Log.i(TAG, "onFocusStatusUpdate,AFState: " + resultAFState + " cameraId:" + mCameraId);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (resultAFState) {
                case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                    /** 3 **/
                    mTapToFocusWaitForActiveScan = false;
                    break;
                case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                    /** 4 **/
                    if (!mTapToFocusWaitForActiveScan) {
                        mAutoFocusRotateLayout.onFocusSucceeded();
                        if (mAaaListener.getRepeatingRequestType() != RequestType.RECORDING) {
                            mSoundPlayer.play(ISoundPlayback.FOCUS_COMPLETE);
                        }
                        resumeContinuousAF();
                        mMainHandler.removeCallbacks(mFocusTimeOutRunnable);
                    }
                    break;
                case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                    /** 5 **/
                    if (!mTapToFocusWaitForActiveScan) {
                        mAutoFocusRotateLayout.onFocusFailed();
                        resumeContinuousAF();
                        mMainHandler.removeCallbacks(mFocusTimeOutRunnable);
                    }
                    break;
                default:
                    break;
                }
            }
        });
    }

    private boolean hasFocuser(CameraCharacteristics characteristics) {
        Float minFocusDistance = characteristics
                .get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (minFocusDistance != null && minFocusDistance > 0) {
            return true;
        }

        // Check available AF modes
        int[] availableAfModes = characteristics
                .get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        if (availableAfModes == null) {
            return false;
        }

        // Assume that if we have an AF mode which doesn't ignore AF trigger, we
        // have a focuser
        boolean hasFocuser = false;
        loop: for (int mode : availableAfModes) {
            switch (mode) {
            case CameraMetadata.CONTROL_AF_MODE_AUTO:
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
            case CameraMetadata.CONTROL_AF_MODE_MACRO:
                hasFocuser = true;
                break loop;
            default:
                break;
            }
        }
        return hasFocuser;
    }

    private void updateCaredSettingChangedKeys(String key) {
        if (key != null && !mCaredSettingChangedKeys.contains(key)) {
            mCaredSettingChangedKeys.add(key);
        }
    }

    private static final List<Integer> LIST_SUPPORTORIENTATION = Arrays.asList(
            0, 90, 180, 270);
    private static final int[][] MATRIX_ORIENTATIONCOMPENSATION = new int[][] {
            // 0, 90, 180, 270 (Local Orientation)
            { 0, 90, 180, 270 }, // 0 (Remote Orientation)
            { 270, 0, 90, 180 }, // 90 (Remote Orientation)
            { 180, 270, 0, 90 }, // 180 (Remote Orientation)
            { 90, 180, 270, 0 } // 270 (Remote Orientation)
    };

    private int getRemotePreviewOrientationValue(int localOrientaion,
            int remoteOrientation) {
        Log.d(TAG, "[getRemotePreviewOrientationValue] localOrientaion: "
                + localOrientaion + ", remoteOrientation: " + remoteOrientation);
        int orientation = 0;
        int localIndex = LIST_SUPPORTORIENTATION.indexOf(localOrientaion);
        int remoteIndex = LIST_SUPPORTORIENTATION.indexOf(remoteOrientation);
        orientation = MATRIX_ORIENTATIONCOMPENSATION[remoteIndex][localIndex];
        return orientation;
    }

    private boolean isTouchinPreviewArea(float x, float y) {
        float scale = (float) (Math
                .min(mFocusArea.width(), mFocusArea.height()) / Math.max(
                mFocusArea.width(), mFocusArea.height()));
        int orientation = getRemotePreviewOrientationValue(mLocalOrientation,
                mRemoteOrientation);
        if (orientation == 90 || orientation == 270) {
            mPointMatrix.reset();
            mPointMatrix.setScale(scale, scale, mFocusArea.centerX(),
                    mFocusArea.centerY());
            mPointMatrix.mapRect(mFocusArea);
            mPointMatrix.setRotate(orientation, mFocusArea.centerX(),
                    mFocusArea.centerY());
            mPointMatrix.mapRect(mFocusArea);
        }
        boolean isTouchin = mFocusArea.contains(x, y);
        Log.d(TAG, "[isTouchinPreviewArea] (" + x + ", " + y
                + "), mFocusArea = " + mFocusArea + ", isTouchin: " + isTouchin);
        return isTouchin;
    }

    private void mapFocusPointsToSrc(float x, float y) {
        int orientation = getRemotePreviewOrientationValue(mLocalOrientation,
                mRemoteOrientation);
        float tmpx = x;
        float tmpy = y;
        float scale = (float) (Math.max(mPreviewArea.width(),
                mPreviewArea.height()) / Math.min(mPreviewArea.width(),
                mPreviewArea.height()));
        // map the points in screen to the buffer show area according
        // orientation.
        if (orientation == 90) {
            x = (mFocusArea.bottom - tmpy) * scale;
            y = tmpx * scale;
        } else if (orientation == 270) {
            x = (tmpy - mFocusArea.top) * scale;
            y = (mFocusArea.right - tmpx) * scale;
        }
        Log.d(TAG, "[mapFocusPointsToSrc] orientation = " + orientation
                + ", (" + x + ", " + y + ")");
    }

    private void notifyFocusStateChanged(int afState) {
        AutoFocusState state = AutoFocusState.INACTIVE;
        switch (afState) {
            case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:/**3**/
                state = AutoFocusState.ACTIVE_SCAN;
                break;
            case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:/**4**/
                state = AutoFocusState.ACTIVE_FOCUSED;
                break;
            case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:/**5**/
                state = AutoFocusState.ACTIVE_UNFOCUSED;
               break;

            default:
                break;
        }

        for (FocusStateListener listener : mFocusStateListener) {
            listener.onFocusStatusUpdate(state);
        }
    }
}