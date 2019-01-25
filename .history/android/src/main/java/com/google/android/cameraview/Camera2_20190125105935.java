/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.cameraview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.widget.Toast;

import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

@SuppressWarnings("MissingPermission")
@TargetApi(21)
class Camera2 extends CameraViewImpl implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {

    private static final String TAG = "Camera2";

    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final CameraManager mCameraManager;

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (mCamera == null) {
                return;
            }
            mCaptureSession = session;
            mInitialCropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            updateAutoFocus();
            updateFlash();
            updateFocusDepth();
            updateWhiteBalance();
            updateZoom();
            updateExposureCompensation();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                mCaptureSession = null;
            }
        }

    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {

        @Override
        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, null);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to run precapture sequence.", e);
            }
        }

        @Override
        public void onReady() {
            captureStillPicture();
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    if (image.getFormat() == ImageFormat.JPEG) {
                        // @TODO: implement deviceOrientation
                        mCallback.onPictureTaken(data, 0);
                    } else {
                        mCallback.onFramePreview(data, image.getWidth(), image.getHeight(), mDisplayOrientation);
                    }
                    image.close();
                }
            }
        }

    };

    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    CameraDevice mCamera;

    CameraCaptureSession mCaptureSession;

    CaptureRequest.Builder mPreviewRequestBuilder;

    Set<String> mAvailableCameras = new HashSet<>();

    private ImageReader mStillImageReader;

    private ImageReader mScanImageReader;

    private int mImageFormat;

    private MediaRecorder mMediaRecorder;

    private String mVideoPath;

    private boolean mIsRecording;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private Size mPictureSize;

    private int mFacing;

    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;

    private AspectRatio mInitialRatio;

    private boolean mAutoFocus;

    private int mFlash;

    private int mCameraOrientation;

    private int mDisplayOrientation;

    private int mDeviceOrientation;

    private float mFocusDepth;

    private float mZoom;

    private float mExposureCompensation;

    private int mWhiteBalance;

    private boolean mIsScanning;

    private Surface mPreviewSurface;

    private Rect mInitialCropRegion;

    private Context mContext;
    
    Camera2(Callback callback, PreviewImpl preview, Context context) {
        super(callback, preview);
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(new CameraManager.AvailabilityCallback() {
            @Override
            public void onCameraAvailable(@NonNull String cameraId) {
                super.onCameraAvailable(cameraId);
                mAvailableCameras.add(cameraId);
            }

            @Override
            public void onCameraUnavailable(@NonNull String cameraId) {
                super.onCameraUnavailable(cameraId);
                mAvailableCameras.remove(cameraId);
            }
        }, null);
        mImageFormat = mIsScanning ? ImageFormat.YUV_420_888 : ImageFormat.JPEG;
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                startCaptureSession();
            }

            @Override
            public void onSurfaceDestroyed() {
                stop();
            }
        });
    }

    @Override
    boolean start() {
        if (!chooseCameraIdByFacing()) {
            mAspectRatio = mInitialRatio;
            return false;
        }
        collectCameraInfo();
        setAspectRatio(mInitialRatio);
        mInitialRatio = null;
        prepareStillImageReader();
        prepareScanImageReader();
        startOpeningCamera();
        return true;
    }

    @Override
    void stop() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mStillImageReader != null) {
            mStillImageReader.close();
            mStillImageReader = null;
        }

        if (mScanImageReader != null) {
            mScanImageReader.close();
            mScanImageReader = null;
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            if (mIsRecording) {
                // @TODO: implement videoOrientation and deviceOrientation calculation
                mCallback.onVideoRecorded(mVideoPath, 0, 0);
                mIsRecording = false;
            }
        }
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    Set<AspectRatio> getSupportedAspectRatios() {
        return mPreviewSizes.ratios();
    }

    @Override
    SortedSet<Size> getAvailablePictureSizes(AspectRatio ratio) {
        return mPictureSizes.sizes(ratio);
    }

    @Override
    public boolean checkIfHasCamera2() {
        return isHardwareLevelSupported();
    }

    private boolean isHardwareLevelSupported() {
        try {
            final String[] ids = mCameraManager.getCameraIdList();
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("No camera available.");
        }
    }

    @Override
    void setPictureSize(Size size) {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mStillImageReader != null) {
            mStillImageReader.close();
        }
        if (size == null) {
          if (mAspectRatio == null) {
            return;
          }
          mPictureSizes.sizes(mAspectRatio).last();
        } else {
          mPictureSize = size;
        }
        prepareStillImageReader();
        startCaptureSession();
    }

    @Override
    Size getPictureSize() {
        return mPictureSize;
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio) {
        if (ratio != null && mPreviewSizes.isEmpty()) {
            mInitialRatio = ratio;
            return false;
        }
<<<<<<< HEAD
        if (ratio == null || ratio.equals(mAspectRatio) || !mPreviewSizes.ratios().contains(ratio)) {
=======
        if (ratio == null || ratio.equals(mAspectRatio) ||
                !mPreviewSizes.ratios().contains(ratio)) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            // TODO: Better error handling
            return false;
        }
        mAspectRatio = ratio;
        prepareStillImageReader();
        prepareScanImageReader();
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
            startCaptureSession();
        }
        return true;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        mAutoFocus = autoFocus;
        if (mPreviewRequestBuilder != null) {
            updateAutoFocus();
            if (mCaptureSession != null) {
                try {
<<<<<<< HEAD
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
=======
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                } catch (CameraAccessException e) {
                    mAutoFocus = !mAutoFocus; // Revert
                }
            }
        }
    }

    @Override
    boolean getAutoFocus() {
        return mAutoFocus;
    }

    @Override
    void setFlash(int flash) {
        if (mFlash == flash) {
            return;
        }
        int saved = mFlash;
        mFlash = flash;
        if (mPreviewRequestBuilder != null) {
            updateFlash();
            if (mCaptureSession != null) {
                try {
<<<<<<< HEAD
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
=======
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                } catch (CameraAccessException e) {
                    mFlash = saved; // Revert
                }
            }
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

    @Override
<<<<<<< HEAD
    void takePicture() {
=======
    void takePicture(ReadableMap options) {
        mCaptureCallback.setOptions(options);

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        if (mAutoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    @Override
<<<<<<< HEAD
    boolean record(String path, int maxDuration, int maxFileSize, boolean recordAudio, CamcorderProfile profile) {
=======
    boolean record(String path, int maxDuration, int maxFileSize, boolean recordAudio, CamcorderProfile profile, int orientation) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        if (!mIsRecording) {
            setUpMediaRecorder(path, maxDuration, maxFileSize, recordAudio, profile);
            try {
                mMediaRecorder.prepare();

                if (mCaptureSession != null) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }

                Size size = chooseOptimalSize();
                mPreview.setBufferSize(size.getWidth(), size.getHeight());
                Surface surface = getPreviewSurface();
                Surface mMediaRecorderSurface = mMediaRecorder.getSurface();

                mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mPreviewRequestBuilder.addTarget(surface);
                mPreviewRequestBuilder.addTarget(mMediaRecorderSurface);
<<<<<<< HEAD
                mCamera.createCaptureSession(Arrays.asList(surface, mMediaRecorderSurface), mSessionCallback, null);
=======
                mCamera.createCaptureSession(Arrays.asList(surface, mMediaRecorderSurface),
                    mSessionCallback, null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                mMediaRecorder.start();
                mIsRecording = true;
                return true;
            } catch (CameraAccessException | IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    void stopRecording() {
        if (mIsRecording) {
            stopMediaRecorder();

            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            startCaptureSession();
        }
    }

    @Override
    public void setFocusDepth(float value) {
<<<<<<< HEAD
        // Toast.makeText(mContext, "entrei no setfocusdepth 2",
        // Toast.LENGTH_LONG).show();

        if (mFocusDepth == value) {
            return;
        }

        float saved = mFocusDepth;
        mFocusDepth = value;
        mFocusDepth = value;
        if (mCaptureSession != null) {
            updateFocusDepth();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mFocusDepth = saved; // Revert
=======
        if (mFocusDepth == value) {
            return;
        }
        float saved = mFocusDepth;
        mFocusDepth = value;
        if (mCaptureSession != null) {
            updateFocusDepth();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mFocusDepth = saved;  // Revert
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        }
    }

    @Override
    float getFocusDepth() {
        return mFocusDepth;
    }

    @Override
    public void setZoom(float zoom) {
<<<<<<< HEAD
        // Toast.makeText(mContext, "entrei no setzoom 2", Toast.LENGTH_LONG).show();
        if (mZoom == zoom) {
            return;
        }
        float saved = mZoom;
        mZoom = zoom;
        if (mCaptureSession != null) {
            updateZoom();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mZoom = saved; // Revert
            }
        }
=======
      if (mZoom == zoom) {
          return;
      }
      float saved = mZoom;
      mZoom = zoom;
      if (mCaptureSession != null) {
          updateZoom();
          try {
              mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                  mCaptureCallback, null);
          } catch (CameraAccessException e) {
              mZoom = saved;  // Revert
          }
      }
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    @Override
    float getZoom() {
        return mZoom;
    }

    @Override
<<<<<<< HEAD
    public void setExposureCompensation(float exposureCompensation) {
        if (mExposureCompensation == exposureCompensation) {
            return;
        }
        float saved = mExposureCompensation;
        mExposureCompensation = exposureCompensation;
        if (mCaptureSession != null) {
            updateExposureCompensation();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mExposureCompensation = saved; // Revert
            }
        }
    }

    @Override
    public float getExposureCompensation() {
        return mExposureCompensation;
    }

    @Override
    Range<Integer> getMinAndMaxExposureCompensation() {
        Range<Integer> range1 = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        return range1;
    }

    @Override
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    public void setWhiteBalance(int whiteBalance) {
        if (mWhiteBalance == whiteBalance) {
            return;
        }
        int saved = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        if (mCaptureSession != null) {
            updateWhiteBalance();
            try {
<<<<<<< HEAD
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mWhiteBalance = saved; // Revert
=======
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                    mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mWhiteBalance = saved;  // Revert
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        }
    }

    @Override
    public int getWhiteBalance() {
        return mWhiteBalance;
    }

    @Override
    void setScanning(boolean isScanning) {
        if (mIsScanning == isScanning) {
            return;
        }
        mIsScanning = isScanning;
        if (!mIsScanning) {
            mImageFormat = ImageFormat.JPEG;
        } else {
            mImageFormat = ImageFormat.YUV_420_888;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        startCaptureSession();
    }

    @Override
    boolean getScanning() {
        return mIsScanning;
    }

    @Override
<<<<<<< HEAD
=======
    int getCameraOrientation() {
        return mCameraOrientation;
    }

    @Override
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }

<<<<<<< HEAD
    /**
     * <p>
     * Chooses a camera ID by the specified camera facing ({@link #mFacing}).
     * </p>
     * <p>
     * This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and
     * optionally {@link #mFacing}.
     * </p>
=======

    @Override
    void setDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
        mPreview.setDisplayOrientation(mDeviceOrientation);
    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    private boolean chooseCameraIdByFacing() {
        try {
            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            final String[] ids = mCameraManager.getCameraIdList();
            if (ids.length == 0) { // No camera
                throw new RuntimeException("No camera available.");
            }
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
<<<<<<< HEAD
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
=======
                Integer level = characteristics.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                    continue;
                }
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                if (internal == internalFacing) {
                    mCameraId = id;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }
            // Not found
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
<<<<<<< HEAD
            Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
=======
            Integer level = mCameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                return false;
            }
            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("Unexpected state: LENS_FACING null");
            }
            for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    mFacing = INTERNAL_FACINGS.keyAt(i);
                    return true;
                }
            }
            // The operation can reach here when the only camera device is an external one.
            // We treat it as facing back.
            mFacing = Constants.FACING_BACK;
            return true;
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
    }

    /**
<<<<<<< HEAD
     * <p>
     * Collects some information from {@link #mCameraCharacteristics}.
     * </p>
     * <p>
     * This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes}, and optionally,
     * {@link #mAspectRatio}.
     * </p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
=======
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes},
     * {@link #mCameraOrientation}, and optionally, {@link #mAspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                mPreviewSizes.add(new Size(width, height));
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        if (mPictureSize == null) {
<<<<<<< HEAD
            // Toast.makeText(mContext, "PONTO 1", Toast.LENGTH_LONG).show();
            Log.e("MyComponent", "PONTO 1: " + String.valueOf(mAspectRatio));
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            mPictureSize = mPictureSizes.sizes(mAspectRatio).last();
        }
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            if (!mPictureSizes.ratios().contains(ratio)) {
                mPreviewSizes.remove(ratio);
            }
        }

        if (!mPreviewSizes.ratios().contains(mAspectRatio)) {
            mAspectRatio = mPreviewSizes.ratios().iterator().next();
        }
<<<<<<< HEAD
=======

        mCameraOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(mImageFormat)) {
<<<<<<< HEAD
            Log.e("MyComponent", "PICTURE SIZE: " + String.valueOf(size));
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    private void prepareStillImageReader() {
        if (mStillImageReader != null) {
            mStillImageReader.close();
        }
<<<<<<< HEAD
        mStillImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(), ImageFormat.JPEG,
                1);
=======
        mStillImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(),
                ImageFormat.JPEG, 1);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        mStillImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    private void prepareScanImageReader() {
        if (mScanImageReader != null) {
            mScanImageReader.close();
        }
<<<<<<< HEAD
        // Toast.makeText(mContext, "PONTO 2", Toast.LENGTH_LONG).show();
        Log.e("MyComponent", "PONTO 2: " + String.valueOf(mAspectRatio));
        Size largest = mPreviewSizes.sizes(mAspectRatio).last();
        mScanImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.YUV_420_888, 1);
=======
        Size largest = mPreviewSizes.sizes(mAspectRatio).last();
        mScanImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.YUV_420_888, 1);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        mScanImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    /**
<<<<<<< HEAD
     * <p>
     * Starts opening a camera device.
     * </p>
     * <p>
     * The result will be processed in {@link #mCameraDeviceCallback}.
     * </p>
=======
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceCallback}.</p>
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    private void startOpeningCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mCameraId, e);
        }
    }

    /**
<<<<<<< HEAD
     * <p>
     * Starts a capture session for camera preview.
     * </p>
     * <p>
     * This rewrites {@link #mPreviewRequestBuilder}.
     * </p>
     * <p>
     * The result will be continuously processed in {@link #mSessionCallback}.
     * </p>
=======
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    void startCaptureSession() {
        if (!isCameraOpened() || !mPreview.isReady() || mStillImageReader == null || mScanImageReader == null) {
            return;
        }
        Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = getPreviewSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            if (mIsScanning) {
                mPreviewRequestBuilder.addTarget(mScanImageReader.getSurface());
            }
<<<<<<< HEAD
            mCamera.createCaptureSession(
                    Arrays.asList(surface, mStillImageReader.getSurface(), mScanImageReader.getSurface()),
                    mSessionCallback, null);
=======
            mCamera.createCaptureSession(Arrays.asList(surface, mStillImageReader.getSurface(),
                    mScanImageReader.getSurface()), mSessionCallback, null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        } catch (CameraAccessException e) {
            mCallback.onMountError();
        }
    }

    @Override
    public void resumePreview() {
<<<<<<< HEAD
        startCaptureSession();
=======
        unlockFocus();
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    @Override
    public void pausePreview() {
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Surface getPreviewSurface() {
        if (mPreviewSurface != null) {
            return mPreviewSurface;
        }
        return mPreview.getSurface();
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (surfaceTexture != null) {
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewSurface = previewSurface;
        } else {
            mPreviewSurface = null;
        }

        // it may be called from another thread, so make sure we're in main looper
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mCaptureSession != null) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                startCaptureSession();
            }
        });
    }

    @Override
    public Size getPreviewSize() {
        return new Size(mPreview.getWidth(), mPreview.getHeight());
    }

    /**
<<<<<<< HEAD
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the
     * surface size.
=======
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the surface size.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     *
     * @return The picked size for camera preview.
     */
    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
<<<<<<< HEAD

        // Toast.makeText(mContext, String.valueOf(mAspectRatio),
        // Toast.LENGTH_LONG).show();

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
<<<<<<< HEAD
        // Toast.makeText(mContext, "PONTO 3", Toast.LENGTH_LONG).show();
        Log.e("MyComponent", "PONTO 3: " + String.valueOf(mAspectRatio));
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        return candidates.last();
    }

    /**
     * Updates the internal state of auto-focus to {@link #mAutoFocus}.
     */
    void updateAutoFocus() {
        if (mAutoFocus) {
<<<<<<< HEAD
            int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0
                    || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
=======
            int[] modes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
<<<<<<< HEAD
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
=======
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        }
    }

    /**
     * Updates the internal state of flash to {@link #mFlash}.
     */
    void updateFlash() {
        switch (mFlash) {
<<<<<<< HEAD
        case Constants.FLASH_OFF:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            break;
        case Constants.FLASH_ON:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            break;
        case Constants.FLASH_TORCH:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            break;
        case Constants.FLASH_AUTO:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            break;
        case Constants.FLASH_RED_EYE:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            break;
=======
            case Constants.FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        }
    }

    /**
     * Updates the internal state of focus depth to {@link #mFocusDepth}.
     */
    void updateFocusDepth() {
<<<<<<< HEAD
        // Toast.makeText(mContext, "eu entrei no focus 2", Toast.LENGTH_LONG).show();

        if (mAutoFocus) {
            // Toast.makeText(mContext, "tem autofocus", Toast.LENGTH_LONG).show();
            return;
        }

        Float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (minimumLens == null) {
            throw new NullPointerException("Unexpected state: LENS_INFO_MINIMUM_FOCUS_DISTANCE null");
        }
        // float value = mFocusDepth * minimumLens;
        float value = 1 / mFocusDepth;
        // mFocusDepth = value;
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 0);
=======
        if (mAutoFocus) {
          return;
        }
        Float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (minimumLens == null) {
          throw new NullPointerException("Unexpected state: LENS_INFO_MINIMUM_FOCUS_DISTANCE null");
        }
        float value = mFocusDepth * minimumLens;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, value);
    }

    /**
     * Updates the internal state of zoom to {@link #mZoom}.
     */
    void updateZoom() {
<<<<<<< HEAD
        // Toast.makeText(mContext, "eu entrei aqui no zoom", Toast.LENGTH_LONG).show();

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        float scaledZoom = mZoom * (maxZoom - 1.0f) + 1.0f;
        Rect currentPreview = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (currentPreview != null) {
            int currentWidth = currentPreview.width();
            int currentHeight = currentPreview.height();
            int zoomedWidth = (int) (currentWidth / scaledZoom);
            int zoomedHeight = (int) (currentHeight / scaledZoom);
            int widthOffset = (currentWidth - zoomedWidth) / 2;
            int heightOffset = (currentHeight - zoomedHeight) / 2;

<<<<<<< HEAD
            Rect zoomedPreview = new Rect(currentPreview.left + widthOffset, currentPreview.top + heightOffset,
                    currentPreview.right - widthOffset, currentPreview.bottom - heightOffset);

            // ¯\_(ツ)_/¯ for some devices calculating the Rect for zoom=1 results in a bit
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            //
            // different
=======
            Rect zoomedPreview = new Rect(
                currentPreview.left + widthOffset,
                currentPreview.top + heightOffset,
                currentPreview.right - widthOffset,
                currentPreview.bottom - heightOffset
            );

            // ¯\_(ツ)_/¯ for some devices calculating the Rect for zoom=1 results in a bit different
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            // Rect that device claims as its no-zoom crop region and the preview freezes
            if (scaledZoom != 1.0f) {
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomedPreview);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mInitialCropRegion);
            }
        }
    }

<<<<<<< HEAD
    void updateExposureCompensation() {
        // Toast.makeText(mContext, "eu entrei na exposure " +
        // String.valueOf(mExposureCompensation), Toast.LENGTH_LONG)
        // .show();
        // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
        // CaptureRequest.CONTROL_AE_MODE_OFF);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) mExposureCompensation);
        // mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
        // null);
    }

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    /**
     * Updates the internal state of white balance to {@link #mWhiteBalance}.
     */
    void updateWhiteBalance() {
        switch (mWhiteBalance) {
<<<<<<< HEAD
        case Constants.WB_AUTO:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            break;
        case Constants.WB_CLOUDY:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
            break;
        case Constants.WB_FLUORESCENT:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
            break;
        case Constants.WB_INCANDESCENT:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
            break;
        case Constants.WB_SHADOW:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_SHADE);
            break;
        case Constants.WB_SUNNY:
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
            break;
=======
            case Constants.WB_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
                break;
            case Constants.WB_CLOUDY:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
                break;
            case Constants.WB_FLUORESCENT:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
                break;
            case Constants.WB_INCANDESCENT:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
                break;
            case Constants.WB_SHADOW:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_SHADE);
                break;
            case Constants.WB_SUNNY:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
                break;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        }
    }

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
<<<<<<< HEAD
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
=======
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to lock focus.", e);
        }
    }

    /**
     * Captures a still picture.
     */
    void captureStillPicture() {
        try {
<<<<<<< HEAD
            CaptureRequest.Builder captureRequestBuilder = mCamera
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
=======
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            if (mIsScanning) {
                mImageFormat = ImageFormat.JPEG;
                captureRequestBuilder.removeTarget(mScanImageReader.getSurface());
            }
            captureRequestBuilder.addTarget(mStillImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
<<<<<<< HEAD

            float focus = 1 / mFocusDepth;
            captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus);

            switch (mFlash) {
            case Constants.FLASH_OFF:
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                break;
            case Constants.FLASH_TORCH:
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                break;
            case Constants.FLASH_RED_EYE:
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                break;
            }
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOutputRotation());
            // captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,
                    mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                        @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            }, null);
=======
            switch (mFlash) {
                case Constants.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    break;
                case Constants.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case Constants.FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case Constants.FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case Constants.FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOutputRotation());
            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                @NonNull CaptureRequest request,
                                @NonNull TotalCaptureResult result) {
                            if (mCaptureCallback.getOptions().hasKey("pauseAfterCapture")
                              && !mCaptureCallback.getOptions().getBoolean("pauseAfterCapture")) {
                                unlockFocus();
                            }
                        }
                    }, null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot capture a still picture.", e);
        }
    }

    private int getOutputRotation() {
        @SuppressWarnings("ConstantConditions")
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
<<<<<<< HEAD
        return (sensorOrientation + mDisplayOrientation * (mFacing == Constants.FACING_FRONT ? 1 : -1) + 360) % 360;
    }

    private void setUpMediaRecorder(String path, int maxDuration, int maxFileSize, boolean recordAudio,
            CamcorderProfile profile) {
=======
        return (sensorOrientation +
                mDisplayOrientation * (mFacing == Constants.FACING_FRONT ? 1 : -1) +
                360) % 360;
    }

    private void setUpMediaRecorder(String path, int maxDuration, int maxFileSize, boolean recordAudio, CamcorderProfile profile) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if (recordAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }

        mMediaRecorder.setOutputFile(path);
        mVideoPath = path;

<<<<<<< HEAD
        if (CamcorderProfile.hasProfile(Integer.parseInt(mCameraId), profile.quality)) {
            setCamcorderProfile(profile, recordAudio);
        } else {
            setCamcorderProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH), recordAudio);
        }
=======
        CamcorderProfile camProfile = profile;
        if (!CamcorderProfile.hasProfile(Integer.parseInt(mCameraId), profile.quality)) {
            camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        }
        camProfile.videoBitRate = profile.videoBitRate;
        setCamcorderProfile(camProfile, recordAudio);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

        mMediaRecorder.setOrientationHint(getOutputRotation());

        if (maxDuration != -1) {
            mMediaRecorder.setMaxDuration(maxDuration);
        }
        if (maxFileSize != -1) {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        }

        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setOnErrorListener(this);
    }

    private void setCamcorderProfile(CamcorderProfile profile, boolean recordAudio) {
        mMediaRecorder.setOutputFormat(profile.fileFormat);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(profile.videoCodec);
        if (recordAudio) {
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioChannels(profile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setAudioEncoder(profile.audioCodec);
        }
    }

    private void stopMediaRecorder() {
        mIsRecording = false;
        try {
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mMediaRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;

        if (mVideoPath == null || !new File(mVideoPath).exists()) {
<<<<<<< HEAD
            mCallback.onVideoRecorded(null);
            return;
        }
        mCallback.onVideoRecorded(mVideoPath);
=======
            // @TODO: implement videoOrientation and deviceOrientation calculation
            mCallback.onVideoRecorded(null, 0 , 0);
            return;
        }
        // @TODO: implement videoOrientation and deviceOrientation calculation
        mCallback.onVideoRecorded(mVideoPath, 0, 0);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        mVideoPath = null;
    }

    /**
<<<<<<< HEAD
     * Unlocks the auto-focus and restart camera preview. This is supposed to be
     * called after capturing a still picture.
     */
    void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
=======
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still picture.
     */
    void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlash();
            if (mIsScanning) {
                mImageFormat = ImageFormat.YUV_420_888;
                startCaptureSession();
            } else {
<<<<<<< HEAD
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
=======
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                        null);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to restart camera preview.", e);
        }
    }

    /**
     * Called when an something occurs while recording.
     */
    public void onInfo(MediaRecorder mr, int what, int extra) {
<<<<<<< HEAD
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
=======
        if ( what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            stopRecording();
        }
    }

    /**
     * Called when an error occurs while recording.
     */
    public void onError(MediaRecorder mr, int what, int extra) {
        stopRecording();
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
     */
<<<<<<< HEAD
    private static abstract class PictureCaptureCallback extends CameraCaptureSession.CaptureCallback {
=======
    private static abstract class PictureCaptureCallback
            extends CameraCaptureSession.CaptureCallback {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;
<<<<<<< HEAD
=======
        private ReadableMap mOptions = null;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

        PictureCaptureCallback() {
        }

        void setState(int state) {
            mState = state;
        }

<<<<<<< HEAD
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                @NonNull CaptureResult partialResult) {
=======
        void setOptions(ReadableMap options) { mOptions = options; }

        ReadableMap getOptions() { return mOptions; }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            process(partialResult);
        }

        @Override
<<<<<<< HEAD
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                @NonNull TotalCaptureResult result) {
=======
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            process(result);
        }

        private void process(@NonNull CaptureResult result) {
            switch (mState) {
<<<<<<< HEAD
            case STATE_LOCKING: {
                Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                if (af == null) {
                    break;
                }
                if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                        || af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_CAPTURING);
                        onReady();
                    } else {
                        setState(STATE_LOCKED);
                        onPrecaptureRequired();
                    }
                }
                break;
            }
            case STATE_PRECAPTURE: {
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                        || ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                        || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    setState(STATE_WAITING);
                }
                break;
            }
            case STATE_WAITING: {
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    setState(STATE_CAPTURING);
                    onReady();
                }
                break;
            }
=======
                case STATE_LOCKING: {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) {
                        break;
                    }
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setState(STATE_CAPTURING);
                            onReady();
                        } else {
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }
                case STATE_PRECAPTURE: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_WAITING);
                    }
                    break;
                }
                case STATE_WAITING: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        }

        /**
         * Called when it is ready to take a still picture.
         */
        public abstract void onReady();

        /**
         * Called when it is necessary to run the precapture sequence.
         */
        public abstract void onPrecaptureRequired();

    }

<<<<<<< HEAD
}
=======
}
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
