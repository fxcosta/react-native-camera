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

import android.app.Activity;
import android.content.Context;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
<<<<<<< HEAD
import android.util.Range;
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.SurfaceTexture;
import android.widget.Toast;
import android.util.Log;
=======
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.SurfaceTexture;

import com.facebook.react.bridge.ReadableMap;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;

public class CameraView extends FrameLayout {

    /** The camera device faces the opposite direction as the device's screen. */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /** The camera device faces the same direction as the device's screen. */
    public static final int FACING_FRONT = Constants.FACING_FRONT;

    /** Direction the camera faces relative to device screen. */
<<<<<<< HEAD
    @IntDef({ FACING_BACK, FACING_FRONT })
=======
    @IntDef({FACING_BACK, FACING_FRONT})
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /** Flash will not be fired. */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /** Flash will always be fired during snapshot. */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /** Flash will be fired automatically when required. */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /** Flash will be fired in red-eye reduction mode. */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /** The mode for for the camera device's flash control */
    @Retention(RetentionPolicy.SOURCE)
<<<<<<< HEAD
    @IntDef({ FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE })
=======
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    public @interface Flash {
    }

    CameraViewImpl mImpl;

    private final CallbackBridge mCallbacks;

    private boolean mAdjustViewBounds;

    private Context mContext;

    private final DisplayOrientationDetector mDisplayOrientationDetector;

    public CameraView(Context context, boolean fallbackToOldApi) {
        this(context, null, fallbackToOldApi);
    }

    public CameraView(Context context, AttributeSet attrs, boolean fallbackToOldApi) {
        this(context, attrs, 0, fallbackToOldApi);
    }

    @SuppressWarnings("WrongConstant")
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, boolean fallbackToOldApi) {
        super(context, attrs, defStyleAttr);
<<<<<<< HEAD
        if (isInEditMode()) {
=======
        if (isInEditMode()){
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            mCallbacks = null;
            mDisplayOrientationDetector = null;
            return;
        }
        mAdjustViewBounds = true;
        mContext = context;

        // Internal setup
        final PreviewImpl preview = createPreviewImpl(context);
        mCallbacks = new CallbackBridge();
        if (fallbackToOldApi || Build.VERSION.SDK_INT < 21) {
<<<<<<< HEAD
            mImpl = new Camera1(mCallbacks, preview, context);
=======
            mImpl = new Camera1(mCallbacks, preview);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        } else if (Build.VERSION.SDK_INT < 23) {
            mImpl = new Camera2(mCallbacks, preview, context);
        } else {
            mImpl = new Camera2Api23(mCallbacks, preview, context);
        }

        // Display orientation detector
        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
<<<<<<< HEAD
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
=======
            public void onDisplayOrientationChanged(int displayOrientation, int deviceOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
                mImpl.setDeviceOrientation(deviceOrientation);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        };
    }

    @NonNull
    private PreviewImpl createPreviewImpl(Context context) {
        PreviewImpl preview;
        if (Build.VERSION.SDK_INT < 14) {
            preview = new SurfaceViewPreview(context, this);
        } else {
            preview = new TextureViewPreview(context, this);
        }
        return preview;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
<<<<<<< HEAD
        if (isInEditMode()) {
=======
        if (isInEditMode()){
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                mCallbacks.reserveRequestLayoutOnOpen();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
<<<<<<< HEAD
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
=======
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
<<<<<<< HEAD
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
=======
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
<<<<<<< HEAD
            mImpl.getView().measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(), MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(), MeasureSpec.EXACTLY),
=======
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        state.focusDepth = getFocusDepth();
        state.zoom = getZoom();
<<<<<<< HEAD
        state.exposureCompensation = getExposureCompensation();
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        state.whiteBalance = getWhiteBalance();
        state.scanning = getScanning();
        state.pictureSize = getPictureSize();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
<<<<<<< HEAD
            Log.e("MyComponent", "restore vai retornar direto");
            super.onRestoreInstanceState(state);
            return;
        }
        Log.e("MyComponent", "restore vai setar as paradas");
=======
            super.onRestoreInstanceState(state);
            return;
        }
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setAspectRatio(ss.ratio);
        setAutoFocus(ss.autoFocus);
        setFlash(ss.flash);
        setFocusDepth(ss.focusDepth);
        setZoom(ss.zoom);
<<<<<<< HEAD
        setExposureCompensation(ss.exposureCompensation);
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        setWhiteBalance(ss.whiteBalance);
        setScanning(ss.scanning);
        setPictureSize(ss.pictureSize);
    }

    public void setUsingCamera2Api(boolean useCamera2) {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }

        boolean wasOpened = isCameraOpened();
        Parcelable state = onSaveInstanceState();

<<<<<<< HEAD
        // useCamera2 = true;

        // if (mImpl.checkIfHasCamera2() == false) {
        // useCamera2 = false;
        // } else {
        // }

        Log.e("MyComponent", "camera2api: " + String.valueOf(useCamera2));

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        if (useCamera2) {
            if (wasOpened) {
                stop();
            }
<<<<<<< HEAD
            Log.e("MyComponent", "vai virar camera2");
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            if (Build.VERSION.SDK_INT < 23) {
                mImpl = new Camera2(mCallbacks, mImpl.mPreview, mContext);
            } else {
                mImpl = new Camera2Api23(mCallbacks, mImpl.mPreview, mContext);
            }
        } else {
            if (mImpl instanceof Camera1) {
                return;
            }

            if (wasOpened) {
                stop();
            }
<<<<<<< HEAD
            Log.e("MyComponent", "vai virar camera1");
            mImpl = new Camera1(mCallbacks, mImpl.mPreview, getContext());
        }

        Log.e("MyComponent", "tem hard: " + String.valueOf(mImpl.checkIfHasCamera2()));

        if (mImpl.checkIfHasCamera2() == false) {
            if (mImpl instanceof Camera1) {
                return;
            }

            // Toast.makeText(mContext, "ERA PRA COMEÇAR TUDO DE NOVO",
            // Toast.LENGTH_LONG).show();
            if (wasOpened) {
                Log.e("MyComponent", "vai parar");
                stop();
            }

            PreviewImpl preview = createPreviewImpl(getContext());
            mImpl = new Camera1(mCallbacks, preview, getContext());
        }

        onRestoreInstanceState(state);
        if (wasOpened) {
            Log.e("MyComponent", "vai iniciar");
            start();
        }
    }

    /**
     * Open a camera device and start showing camera preview. This is typically
     * called from {@link Activity#onResume()}.
=======
            mImpl = new Camera1(mCallbacks, mImpl.mPreview);
        }
        start();
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    public void start() {
        if (!mImpl.start()) {
            if (mImpl.getView() != null) {
                this.removeView(mImpl.getView());
            }
<<<<<<< HEAD
            // store the state and restore this state after fall back to Camera1
            Parcelable state = onSaveInstanceState();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            mImpl = new Camera1(mCallbacks, createPreviewImpl(getContext()), getContext());
=======
            //store the state and restore this state after fall back to Camera1
            Parcelable state=onSaveInstanceState();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            mImpl = new Camera1(mCallbacks, createPreviewImpl(getContext()));
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            onRestoreInstanceState(state);
            mImpl.start();
        }
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mImpl.stop();
    }

    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     * Add a new callback.
     *
     * @param callback The {@link Callback} to add.
     * @see #removeCallback(Callback)
     */
    public void addCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Remove a callback.
     *
     * @param callback The {@link Callback} to remove.
     * @see #addCallback(Callback)
     */
    public void removeCallback(@NonNull Callback callback) {
        mCallbacks.remove(callback);
    }

    /**
<<<<<<< HEAD
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its
     *                         bounds to preserve the aspect ratio of camera.
=======
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its bounds to
     *                         preserve the aspect ratio of camera.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds != adjustViewBounds) {
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    /**
<<<<<<< HEAD
     * @return True when this CameraView is adjusting its bounds to preserve the
     *         aspect ratio of camera.
=======
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    public View getView() {
<<<<<<< HEAD
        if (mImpl != null) {
            return mImpl.getView();
        }
        return null;
=======
      if (mImpl != null) {
        return mImpl.getView();
      }
      return null;
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mImpl.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
<<<<<<< HEAD
        // noinspection WrongConstant
=======
        //noinspection WrongConstant
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
<<<<<<< HEAD
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is
     *         opened yet.
=======
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }
    
    /**
<<<<<<< HEAD
     * Gets all the picture sizes for particular ratio supported by the current
     * camera.
     *
     * @param ratio {@link AspectRatio} for which the available image sizes will be
     *              returned.
=======
     * Gets all the picture sizes for particular ratio supported by the current camera.
     *
     * @param ratio {@link AspectRatio} for which the available image sizes will be returned.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    public SortedSet<Size> getAvailablePictureSizes(@NonNull AspectRatio ratio) {
        return mImpl.getAvailablePictureSizes(ratio);
    }
    
    /**
     * Sets the size of taken pictures.
     *
     * @param size The {@link Size} to be set.
     */
    public void setPictureSize(@NonNull Size size) {
        mImpl.setPictureSize(size);
<<<<<<< HEAD
        if (mImpl.checkIfHasCamera2() == true) {
            // mImpl.setPictureSize(Size.parse("16x9"));
            // vou setar um size estatico porque isso n ta funcionando direito usando a
            // regra que defini
        }
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }
    
    /**
     * Gets the size of pictures that will be taken.
     */
    public Size getPictureSize() {
        return mImpl.getPictureSize();
    }

    /**
<<<<<<< HEAD
     * Enables or disables the continuous auto-focus mode. When the current camera
     * doesn't support auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode.
     *                  {@code false} to disable it.
=======
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
<<<<<<< HEAD
     * @return {@code true} if the continuous auto-focus mode is enabled.
     *         {@code false} if it is disabled, or if it is not supported by the
     *         current camera.
=======
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
     */
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mImpl.setFlash(flash);
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
<<<<<<< HEAD
        // noinspection WrongConstant
        return mImpl.getFlash();
    }

=======
        //noinspection WrongConstant
        return mImpl.getFlash();
    }

    /**
     * Gets the camera orientation relative to the devices native orientation.
     *
     * @return The orientation of the camera.
     */
    public int getCameraOrientation() {
        return mImpl.getCameraOrientation();
    }

>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    public void setFocusDepth(float value) {
        mImpl.setFocusDepth(value);
    }

<<<<<<< HEAD
    public float getFocusDepth() {
        return mImpl.getFocusDepth();
    }

    public void setZoom(float zoom) {
        mImpl.setZoom(zoom);
    }

    public float getZoom() {
        return mImpl.getZoom();
    }

    public void setExposureCompensation(float exposureCompensation) {
        mImpl.setExposureCompensation(exposureCompensation);
    }

    public float getExposureCompensation() {
        return mImpl.getExposureCompensation();
    }

    public Range<Integer> getMinAndMaxExposureCompensation() {
        return mImpl.getMinAndMaxExposureCompensation();
    }

    public boolean hasCamera2Api() {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }

        return true;
    }

    public Integer getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public void setWhiteBalance(int whiteBalance) {
        mImpl.setWhiteBalance(whiteBalance);
    }

    public int getWhiteBalance() {
        return mImpl.getWhiteBalance();
    }

    public void setScanning(boolean isScanning) {
        mImpl.setScanning(isScanning);
    }

    public boolean getScanning() {
        return mImpl.getScanning();
    }

    /**
     * Take a picture. The result will be returned to
     * {@link Callback#onPictureTaken(CameraView, byte[])}.
     */
    public void takePicture() {
        mImpl.takePicture();
=======
    public float getFocusDepth() { return mImpl.getFocusDepth(); }

    public void setZoom(float zoom) {
      mImpl.setZoom(zoom);
    }

    public float getZoom() {
      return mImpl.getZoom();
    }

    public void setWhiteBalance(int whiteBalance) {
      mImpl.setWhiteBalance(whiteBalance);
    }

    public int getWhiteBalance() {
      return mImpl.getWhiteBalance();
    }

    public void setScanning(boolean isScanning) { mImpl.setScanning(isScanning);}

    public boolean getScanning() { return mImpl.getScanning(); }

    /**
     * Take a picture. The result will be returned to
     * {@link Callback#onPictureTaken(CameraView, byte[], int)}.
     */
    public void takePicture(ReadableMap options) {
        mImpl.takePicture(options);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    /**
     * Record a video and save it to file. The result will be returned to
<<<<<<< HEAD
     * {@link Callback#onVideoRecorded(CameraView, String)}.
     * 
     * @param path        Path to file that video will be saved to.
     * @param maxDuration Maximum duration of the recording, in seconds.
     * @param maxFileSize Maximum recording file size, in bytes.
     * @param profile     Quality profile of the recording.
     */
    public boolean record(String path, int maxDuration, int maxFileSize, boolean recordAudio,
            CamcorderProfile profile) {
        return mImpl.record(path, maxDuration, maxFileSize, recordAudio, profile);
=======
     * {@link Callback#onVideoRecorded(CameraView, String, int, int)}.
     * @param path Path to file that video will be saved to.
     * @param maxDuration Maximum duration of the recording, in seconds.
     * @param maxFileSize Maximum recording file size, in bytes.
     * @param profile Quality profile of the recording.
     */
    public boolean record(String path, int maxDuration, int maxFileSize,
                          boolean recordAudio, CamcorderProfile profile, int orientation) {
        return mImpl.record(path, maxDuration, maxFileSize, recordAudio, profile, orientation);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

    public void stopRecording() {
        mImpl.stopRecording();
    }
    
    public void resumePreview() {
        mImpl.resumePreview();
    }
    
    public void pausePreview() {
        mImpl.pausePreview();
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        mImpl.setPreviewTexture(surfaceTexture);
    }

    public Size getPreviewSize() {
        return mImpl.getPreviewSize();
    }

    private class CallbackBridge implements CameraViewImpl.Callback {

        private final ArrayList<Callback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;

        CallbackBridge() {
        }

        public void add(Callback callback) {
            mCallbacks.add(callback);
        }

        public void remove(Callback callback) {
            mCallbacks.remove(callback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (Callback callback : mCallbacks) {
                callback.onCameraOpened(CameraView.this);
            }
        }

        @Override
        public void onCameraClosed() {
            for (Callback callback : mCallbacks) {
                callback.onCameraClosed(CameraView.this);
            }
        }

        @Override
<<<<<<< HEAD
        public void onPictureTaken(byte[] data) {
            for (Callback callback : mCallbacks) {
                callback.onPictureTaken(CameraView.this, data);
=======
        public void onPictureTaken(byte[] data, int deviceOrientation) {
            for (Callback callback : mCallbacks) {
                callback.onPictureTaken(CameraView.this, data, deviceOrientation);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        }

        @Override
<<<<<<< HEAD
        public void onVideoRecorded(String path) {
            for (Callback callback : mCallbacks) {
                callback.onVideoRecorded(CameraView.this, path);
=======
        public void onVideoRecorded(String path, int videoOrientation, int deviceOrientation) {
            for (Callback callback : mCallbacks) {
                callback.onVideoRecorded(CameraView.this, path, videoOrientation, deviceOrientation);
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            }
        }

        @Override
        public void onFramePreview(byte[] data, int width, int height, int orientation) {
            for (Callback callback : mCallbacks) {
                callback.onFramePreview(CameraView.this, data, width, height, orientation);
            }
        }

        @Override
        public void onMountError() {
            for (Callback callback : mCallbacks) {
                callback.onMountError(CameraView.this);
            }
        }

        public void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }

    protected static class SavedState extends BaseSavedState {

        @Facing
        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        float focusDepth;

        float zoom;

<<<<<<< HEAD
        int iso;

        float exposureCompensation;

=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        int whiteBalance;

        boolean scanning;
        
        Size pictureSize;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
            focusDepth = source.readFloat();
            zoom = source.readFloat();
<<<<<<< HEAD
            exposureCompensation = source.readFloat();
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            whiteBalance = source.readInt();
            scanning = source.readByte() != 0;
            pictureSize = source.readParcelable(loader);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
            out.writeFloat(focusDepth);
            out.writeFloat(zoom);
<<<<<<< HEAD
            out.writeFloat(exposureCompensation);
=======
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
            out.writeInt(whiteBalance);
            out.writeByte((byte) (scanning ? 1 : 0));
            out.writeParcelable(pictureSize, flags);
        }

<<<<<<< HEAD
        public static final Creator<SavedState> CREATOR = ParcelableCompat
                .newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }

                });
=======
        public static final Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349

    }

    /**
     * Callback for monitoring events about {@link CameraView}.
     */
    @SuppressWarnings("UnusedParameters")
    public abstract static class Callback {

        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraOpened(CameraView cameraView) {
        }

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated {@link CameraView}.
         */
        public void onCameraClosed(CameraView cameraView) {
        }

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param data       JPEG data.
         */
<<<<<<< HEAD
        public void onPictureTaken(CameraView cameraView, byte[] data) {
=======
        public void onPictureTaken(CameraView cameraView, byte[] data, int deviceOrientation) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        }

        /**
         * Called when a video is recorded.
         *
         * @param cameraView The associated {@link CameraView}.
         * @param path       Path to recoredd video file.
         */
<<<<<<< HEAD
        public void onVideoRecorded(CameraView cameraView, String path) {
=======
        public void onVideoRecorded(CameraView cameraView, String path, int videoOrientation, int deviceOrientation) {
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
        }

        public void onFramePreview(CameraView cameraView, byte[] data, int width, int height, int orientation) {
        }

<<<<<<< HEAD
        public void onMountError(CameraView cameraView) {
        }
=======
        public void onMountError(CameraView cameraView) {}
>>>>>>> 57bc327d847ccb7ea90ded51a5a82168388b1349
    }

}
