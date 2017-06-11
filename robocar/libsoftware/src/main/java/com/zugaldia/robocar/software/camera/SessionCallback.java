package com.zugaldia.robocar.software.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.support.annotation.NonNull;

import timber.log.Timber;

/**
 * Callback for a camera session
 */
public class SessionCallback extends CameraCaptureSession.StateCallback {

  public interface SessionCallbackListener {
    void onConfigured();
  }

  private SessionCallbackListener listener;

  private CameraCaptureSession session;

  public SessionCallback(SessionCallbackListener listener) {
    this.listener = listener;
  }

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
  public void onConfigured(@NonNull CameraCaptureSession session) {
    Timber.d("Session configured.");
    this.session = session;
    listener.onConfigured();
  }

  @Override
  public void onConfigureFailed(@NonNull CameraCaptureSession session) {
    Timber.d("Session configuration failed.");
    closeSession();
  }
}
