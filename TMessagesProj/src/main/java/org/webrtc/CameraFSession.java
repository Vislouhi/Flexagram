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
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.flexatar.FlexatarRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraFSession  implements CameraSession{
    private static final Histogram camera2StartTimeMsHistogram =
            Histogram.createCounts("WebRTC.Android.Camera2.StartTimeMs", 1, 10000, 50);
    static String TAG = "FLX_INJECT";
    private class CameraStateCallback extends CameraDevice.StateCallback {


        @Override
        public void onDisconnected(CameraDevice camera) {
//            checkIsOnCameraThread();
//            final boolean startFailure = (captureSession == null) && (state != Camera2Session.SessionState.STOPPED);
//            state = CameraFSession.SessionState.STOPPED;
//            stopInternal();
           /* if (startFailure) {
                callback.onFailure(FailureType.DISCONNECTED, "Camera disconnected / evicted.");
            } else {
                events.onCameraDisconnected(CameraFSession.this);
            }*/
        }

        @Override
        public void onError(CameraDevice camera, int errorCode) {

        }

        @Override
        public void onOpened(CameraDevice camera) {
            Logging.d(TAG, "flexatar Camera opened.");
            surfaceTextureHelper.setTextureSize(640, 480);
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
//            try {
//                camera.createCaptureSession(
//                        Arrays.asList(surface), new CameraFSession.CaptureSessionCallback(), cameraThreadHandler);
//            } catch (CameraAccessException e) {
////                reportError("Failed to create capture session. " + e);
//                return;
//            }
        }

        @Override
        public void onClosed(CameraDevice camera) {

        }
    }

    private class CaptureSessionCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
//            checkIsOnCameraThread();
            session.close();
//            reportError("Failed to configure capture session.");
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            FlexatarRenderer.isFlexatarRendering = true;
            surfaceTextureHelper.startListening((VideoFrame frame) -> {

                if (!firstFrameReported) {

                    firstFrameReported = true;
                    final int startTimeMs =
                            (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - constructionTimeNs);
                    camera2StartTimeMsHistogram.addSample(startTimeMs);
                }

                // Undo the mirror that the OS "helps" us with.
                // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
                // Also, undo camera orientation, we report it as rotation instead.
                final VideoFrame modifiedFrame =
                        new VideoFrame(CameraSession.createTextureBufferWithModifiedTransformMatrix(
                                (TextureBufferImpl) frame.getBuffer(),
                                /* mirror= */ false,
                                /* rotation= */ 0),
                                /* rotation= */ 0, frame.getTimestampNs());
                events.onFrameCaptured(CameraFSession.this, modifiedFrame);
                modifiedFrame.release();
            });
            surfaceTextureHelper.startFrameTimer();
            Logging.d(TAG, "Camera device successfully started.");
            callback.onDone(CameraFSession.this);
        }

        // Prefers optical stabilization over software stabilization if available. Only enables one of
        // the stabilization modes at a time because having both enabled can cause strange results.

    }

    private static class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {
        @Override
        public void onCaptureFailed(
                CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Logging.d(TAG, "Capture failed: " + failure);
        }
    }
    @Override
    public void stop() {

    }

    public static void create(CreateSessionCallback callback, Events events,
                              Context applicationContext, CameraManager cameraManager,
                              SurfaceTextureHelper surfaceTextureHelper, String cameraId, int width, int height,
                              int framerate) {
        new CameraFSession(callback, events, applicationContext, cameraManager, surfaceTextureHelper,
                cameraId, width, height, framerate);
    }


    private final Handler cameraThreadHandler;
    private final CreateSessionCallback callback;
    private final Events events;
    private final Context applicationContext;
    private final CameraManager cameraManager;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final String cameraId;
    private final int width;
    private final int height;
    private final int framerate;

    private OrientationHelper orientationHelper;

    // Initialized at start
    private CameraCharacteristics cameraCharacteristics;
    private int cameraOrientation;
    private boolean isCameraFrontFacing;
    private int fpsUnitFactor;
    private CameraEnumerationAndroid.CaptureFormat captureFormat;

    // Initialized when camera opens
    @Nullable
    private CameraDevice cameraDevice;
    @Nullable private Surface surface;

    // Initialized when capture session is created
    @Nullable private CameraCaptureSession captureSession;

    // State
//    private CameraFSession.SessionState state = CameraFSession.SessionState.RUNNING;
    private boolean firstFrameReported;

    // Used only for stats. Only used on the camera thread.
    private final long constructionTimeNs; // Construction time of this class.
    private CameraFSession(CreateSessionCallback callback, Events events, Context applicationContext,
                           CameraManager cameraManager, SurfaceTextureHelper surfaceTextureHelper, String cameraId,
                           int width, int height, int framerate) {
        Logging.d(TAG, "Create new camera2 session on camera " + cameraId);

        constructionTimeNs = System.nanoTime();

        this.cameraThreadHandler = new Handler();
        this.callback = callback;
        this.events = events;
        this.applicationContext = applicationContext;
        this.cameraManager = cameraManager;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        this.orientationHelper = new OrientationHelper();

        Logging.d(TAG, "Opening camera " + cameraId);
        events.onCameraOpening();
        CameraStateCallback cameraStateCallback = new CameraStateCallback();
        cameraStateCallback.onOpened(null);
//        start();
    }
}
