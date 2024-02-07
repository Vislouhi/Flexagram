package org.webrtc;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.flexatar.FlexatarRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraFSession  implements CameraSession{
    private static final Histogram camera2StartTimeMsHistogram =
            Histogram.createCounts("WebRTC.Android.Camera2.StartTimeMs", 1, 10000, 50);
    private static final Histogram camera2StopTimeMsHistogram =
            Histogram.createCounts("WebRTC.Android.Camera2.StopTimeMs", 1, 10000, 50);
//    private static final Histogram camera2ResolutionHistogram = Histogram.createEnumeration(
//            "WebRTC.Android.Camera2.Resolution", CameraEnumerationAndroid.COMMON_RESOLUTIONS.size());
    static String TAG = "CameraFSession";
    private final OrientationHelper orientationHelper;
    private final CameraStateCallback cameraStateCallback;

    private static enum SessionState { RUNNING, STOPPED }
    private final Handler cameraThreadHandler;
    private final CreateSessionCallback callback;
    private final Events events;
//    private final Context applicationContext;
//    private final CameraManager cameraManager;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final String cameraId;
//    private final int width;
//    private final int height;
//    private final int framerate;

//    private OrientationHelper orientationHelper;

    // Initialized at start
//    private CameraCharacteristics cameraCharacteristics;
//    private int cameraOrientation;
//    private boolean isCameraFrontFacing;
//    private int fpsUnitFactor;
//    private CameraEnumerationAndroid.CaptureFormat captureFormat;

    // Initialized when camera opens
//    @Nullable
//    private CameraDevice cameraDevice;
//    @Nullable private Surface surface;

    // Initialized when capture session is created
//    @Nullable private CameraCaptureSession captureSession;

    // State
    private CameraFSession.SessionState state = CameraFSession.SessionState.RUNNING;
    private boolean firstFrameReported;

    // Used only for stats. Only used on the camera thread.
    private final long constructionTimeNs; // Construction time of this class.


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class CameraStateCallback extends CameraDevice.StateCallback {

        private String getErrorDescription(int errorCode) {
            switch (errorCode) {
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    return "Camera device has encountered a fatal error.";
                case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                    return "Camera device could not be opened due to a device policy.";
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    return "Camera device is in use already.";
                case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                    return "Camera service has encountered a fatal error.";
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    return "Camera device could not be opened because"
                            + " there are too many other open camera devices.";
                default:
                    return "Unknown camera error: " + errorCode;
            }
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            checkIsOnCameraThread();

            final boolean startFailure = (state != CameraFSession.SessionState.STOPPED);
            state = CameraFSession.SessionState.STOPPED;
            Logging.d(TAG, "On disconectsd session state set ot stop.");
//            if (state != )

            if (startFailure) {
                stopInternal();
                callback.onFailure(FailureType.DISCONNECTED, "Camera disconnected / evicted.");
            } else {
                events.onCameraDisconnected(CameraFSession.this);
            }
        }

        @Override
        public void onError(CameraDevice camera, int errorCode) {
            checkIsOnCameraThread();
            reportError(getErrorDescription(errorCode));
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(CameraDevice camera) {
            Logging.d(TAG, "flexatar Camera opened.");
            surfaceTextureHelper.setTextureSize(600, 400);
            CaptureSessionCallback captureSessionCallback = new CaptureSessionCallback();
            captureSessionCallback.onConfigured(new CameraCaptureSession() {
                @NonNull
                @Override
                public CameraDevice getDevice() {
                    return null;
                }

                @Override
                public void prepare(@NonNull Surface surface) throws CameraAccessException {

                }

                @Override
                public void finalizeOutputConfigurations(List<OutputConfiguration> list) throws CameraAccessException {

                }

                @Override
                public int capture(@NonNull CaptureRequest captureRequest, @Nullable CaptureCallback captureCallback, @Nullable Handler handler) throws CameraAccessException {
                    return 0;
                }

                @Override
                public int captureBurst(@NonNull List<CaptureRequest> list, @Nullable CaptureCallback captureCallback, @Nullable Handler handler) throws CameraAccessException {
                    return 0;
                }

                @Override
                public int setRepeatingRequest(@NonNull CaptureRequest captureRequest, @Nullable CaptureCallback captureCallback, @Nullable Handler handler) throws CameraAccessException {
                    return 0;
                }

                @Override
                public int setRepeatingBurst(@NonNull List<CaptureRequest> list, @Nullable CaptureCallback captureCallback, @Nullable Handler handler) throws CameraAccessException {
                    return 0;
                }

                @Override
                public void stopRepeating() throws CameraAccessException {

                }

                @Override
                public void abortCaptures() throws CameraAccessException {

                }

                @Override
                public boolean isReprocessable() {
                    return false;
                }

                @Nullable
                @Override
                public Surface getInputSurface() {
                    return null;
                }

                @Override
                public void close() {

                }
            });

        }

        @Override
        public void onClosed(CameraDevice camera) {
            checkIsOnCameraThread();
            events.onCameraClosed(CameraFSession.this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class CaptureSessionCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            checkIsOnCameraThread();
            session.close();
            reportError("Failed to configure capture session.");
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
//            FlexatarRenderer.isFlexatarRendering = true;
            surfaceTextureHelper.startListening((VideoFrame frame) -> {

                if (!firstFrameReported) {

                    firstFrameReported = true;
                    final int startTimeMs =
                            (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - constructionTimeNs);
                    camera2StartTimeMsHistogram.addSample(startTimeMs);
                }

                final VideoFrame modifiedFrame =
                        new VideoFrame(CameraSession.createTextureBufferWithModifiedTransformMatrix(
                                (TextureBufferImpl) frame.getBuffer(),
                                /* mirror= */ false,
                                /* rotation= */ 180),
                                /* rotation= */ getFrameOrientation(), frame.getTimestampNs());
                events.onFrameCaptured(CameraFSession.this, modifiedFrame);
                modifiedFrame.release();
            });




            surfaceTextureHelper.setTextureType(VideoFrame.TextureBuffer.Type.FLX);
            surfaceTextureHelper.startFrameTimer();
            Logging.d(TAG, "Camera device successfully started.");
            callback.onDone(CameraFSession.this);
        }

        // Prefers optical stabilization over software stabilization if available. Only enables one of
        // the stabilization modes at a time because having both enabled can cause strange results.

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Logging.d(TAG, "Capture failed: " + failure);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void create(CreateSessionCallback callback, Events events,
                              Context applicationContext, CameraManager cameraManager,
                              SurfaceTextureHelper surfaceTextureHelper, String cameraId, int width, int height,
                              int framerate) {
        new CameraFSession(callback, events, applicationContext, cameraManager, surfaceTextureHelper,
                cameraId, width, height, framerate);
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraFSession(CreateSessionCallback callback, Events events, Context applicationContext,
                           CameraManager cameraManager, SurfaceTextureHelper surfaceTextureHelper, String cameraId,
                           int width, int height, int framerate) {
        Logging.d(TAG, "Create new cameraF session on camera " + cameraId);

        constructionTimeNs = System.nanoTime();

        this.cameraThreadHandler = new Handler();
        this.callback = callback;
        this.events = events;
//        this.applicationContext = applicationContext;
//        this.cameraManager = cameraManager;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.cameraId = cameraId;
//        this.width = width;
//        this.height = height;
//        this.framerate = framerate;
        this.orientationHelper = new OrientationHelper();

        Logging.d(TAG, "Opening camera " + cameraId);
        events.onCameraOpening();
        cameraStateCallback = new CameraStateCallback();
        cameraStateCallback.onOpened(null);
        orientationHelper.start();


//        start();
    }
    @Override
    public void stop() {
        Logging.d(TAG, "Stop cameraF session on camera " + cameraId);
        Logging.d(TAG, "Session state: " + state);
        surfaceTextureHelper.stopFrameTimer();
        if (state != CameraFSession.SessionState.STOPPED) {
            Logging.d(TAG, "session statenot stoped");
            final long stopStartTime = System.nanoTime();
            state = CameraFSession.SessionState.STOPPED;
            stopInternal();
            final int stopTimeMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStartTime);
            camera2StopTimeMsHistogram.addSample(stopTimeMs);
        }

    }
    private void stopInternal() {
        Logging.d(TAG, "Stop internal");
        checkIsOnCameraThread();

        surfaceTextureHelper.stopListening();
        if (orientationHelper != null) {
            orientationHelper.stop();
        }
        cameraStateCallback.onDisconnected(null);
        Logging.d(TAG, "Stop done");
    }
    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }
    private void reportError(String error) {
        checkIsOnCameraThread();
        Logging.e(TAG, "Error: " + error);

        final boolean startFailure = (state != CameraFSession.SessionState.STOPPED);
        state = CameraFSession.SessionState.STOPPED;
        stopInternal();
        if (startFailure) {
            callback.onFailure(FailureType.ERROR, error);
        } else {
            events.onCameraError(this, error);
        }
    }
    private int getFrameOrientation() {
        int rotation = orientationHelper.getOrientation();
        OrientationHelper.cameraOrientation = rotation;

        return  (180 + rotation) % 360;
    }
}
