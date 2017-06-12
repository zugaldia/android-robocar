package com.zugaldia.robocar.software.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;

import timber.log.Timber;

/**
 * Callback for a capture event
 */
public class CaptureCallback extends CameraCaptureSession.CaptureCallback {

  private CameraCaptureSession session;

  public CameraCaptureSession getSession() {
    return session;
  }

  public void closeSession() {
    if (session != null) {
      session.close();
      session = null;
    }
  }

  @Override
  public void onCaptureStarted(@NonNull CameraCaptureSession session,
                               @NonNull CaptureRequest request, long timestamp, long frameNumber) {
    super.onCaptureStarted(session, request, timestamp, frameNumber);
    Timber.d("Capture started.");
    this.session = session;
  }

  @Override
  public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                 @NonNull CaptureRequest request,
                                 @NonNull TotalCaptureResult result) {
    super.onCaptureCompleted(session, request, result);
    Timber.d("Capture completed.");
    this.session = null;
  }

  @Override
  public void onCaptureFailed(@NonNull CameraCaptureSession session,
                              @NonNull CaptureRequest request,
                              @NonNull CaptureFailure failure) {
    super.onCaptureFailed(session, request, failure);
    this.session = null;
    switch (failure.getReason()) {
      case CaptureFailure.REASON_ERROR:
        Timber.e("Capture failed: REASON_ERROR");
        break;
      case CaptureFailure.REASON_FLUSHED:
        Timber.e("Capture failed: REASON_FLUSHED");
        break;
      default:
        Timber.e("Capture failed: UNKNOWN");
        break;
    }
  }
}
