/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
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
import android.hardware.camera2.CameraManager;

import androidx.annotation.Nullable;

@TargetApi(21)
public class CameraFCapturer extends CameraCapturer {
  private final Context context;
  @Nullable private final CameraManager cameraManager;

  public CameraFCapturer(Context context, String cameraName, CameraEventsHandler eventsHandler) {
    super(cameraName, eventsHandler, new CameraFEnumerator(context));

    this.context = context;
    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
  }

  @Override
  protected void createCameraSession(CameraSession.CreateSessionCallback createSessionCallback,
      CameraSession.Events events, Context applicationContext,
      SurfaceTextureHelper surfaceTextureHelper, String cameraName, int width, int height,
      int framerate) {
    CameraFSession.create(createSessionCallback, events, applicationContext, cameraManager,
        surfaceTextureHelper, cameraName, width, height, framerate);
  }
}
