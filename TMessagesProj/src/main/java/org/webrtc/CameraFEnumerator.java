/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.SystemClock;
import android.util.AndroidException;
import android.util.Range;

import androidx.annotation.Nullable;

import org.webrtc.CameraEnumerationAndroid.CaptureFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class CameraFEnumerator implements CameraEnumerator {
  private final static String TAG = "CameraFEnumerator";
  private final static double NANO_SECONDS_PER_SECOND = 1.0e9;

  // Each entry contains the supported formats for a given camera index. The formats are enumerated
  // lazily in getSupportedFormats(), and cached for future reference.
  private static final Map<String, List<CaptureFormat>> cachedSupportedFormats =
      new HashMap<String, List<CaptureFormat>>();

  final Context context;
  @Nullable final CameraManager cameraManager;

  public CameraFEnumerator(Context context) {
    this.context = context;
    this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
  }

  @Override
  public String[] getDeviceNames() {
    return new String[]{"flexatar","flexatar1"};
  }

  @Override
  public boolean isFrontFacing(String deviceName) {
    return true;
  }

  @Override
  public boolean isBackFacing(String deviceName) {
    return false;
  }

  @Nullable
  @Override
  public List<CaptureFormat> getSupportedFormats(String deviceName) {
    return getSupportedFormats(context, deviceName);
  }

  @Override
  public CameraVideoCapturer createCapturer(
      String deviceName, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
    return new CameraFCapturer(context, deviceName, eventsHandler);
  }

  private @Nullable CameraCharacteristics getCameraCharacteristics(String deviceName) {
    return null;
  }

  /**
   * Checks if API is supported and all cameras have better than legacy support.
   */
  public static boolean isSupported(Context context) {

    return true;
  }

  static int getFpsUnitFactor(Range<Integer>[] fpsRanges) {
    if (fpsRanges.length == 0) {
      return 1000;
    }
    return fpsRanges[0].getUpper() < 1000 ? 1000 : 1;
  }

  static List<Size> getSupportedSizes(CameraCharacteristics cameraCharacteristics) {
    return null;
  }

  @Nullable
  static List<CaptureFormat> getSupportedFormats(Context context, String cameraId) {
    return getSupportedFormats(
        (CameraManager) context.getSystemService(Context.CAMERA_SERVICE), cameraId);
  }

  @Nullable
  static List<CaptureFormat> getSupportedFormats(CameraManager cameraManager, String cameraId) {
    synchronized (cachedSupportedFormats) {

      return null;
    }
  }

  // Convert from android.util.Size to Size.
  private static List<Size> convertSizes(android.util.Size[] cameraSizes) {
    final List<Size> sizes = new ArrayList<Size>();
    for (android.util.Size size : cameraSizes) {
      sizes.add(new Size(size.getWidth(), size.getHeight()));
    }
    return sizes;
  }

  // Convert from android.util.Range<Integer> to CaptureFormat.FramerateRange.
  static List<CaptureFormat.FramerateRange> convertFramerates(
      Range<Integer>[] arrayRanges, int unitFactor) {

    return null;
  }
}
