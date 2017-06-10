package com.zugaldia.robocar.software.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import com.zugaldia.robocar.software.camera.utils.CameraHandler;
import com.zugaldia.robocar.software.camera.utils.ImagePreprocessor;
import com.zugaldia.robocar.software.camera.utils.ImageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

/**
 * This is wrapper around CameraHandler, we're avoiding calling this class CameraManager
 * which is already taken by the Android system.
 */
public class CameraOperator implements ImageReader.OnImageAvailableListener {

  private static final int INPUT_SIZE = 224;

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);

  private Context context;
  private Handler backgroundHandler;
  private File folder;

  private ImagePreprocessor imagePreprocessor;
  private CameraHandler cameraHandler;

  private String currentFilename = null;

  private boolean inTraining = false;

  /**
   * Constructor.
   */
  public CameraOperator(Context context) {
    this.context = context;
    HandlerThread backgroundThread = new HandlerThread("BackgroundThread");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
    backgroundHandler.post(initializeOnBackground);
    folder = getAlbumStorageDir("robocar");
  }

  private Runnable initializeOnBackground = new Runnable() {
    @Override
    public void run() {
      imagePreprocessor = new ImagePreprocessor(
          CameraHandler.IMAGE_WIDTH, CameraHandler.IMAGE_HEIGHT, INPUT_SIZE);
      cameraHandler = CameraHandler.getInstance();
      cameraHandler.initializeCamera(context, backgroundHandler, CameraOperator.this);

      // Debug
      CameraHandler.dumpFormatInfo(context);
      Timber.d("isExternalStorageWritable: %b", isExternalStorageWritable());
      Timber.d("isExternalStorageReadable: %b", isExternalStorageReadable());
      Timber.d("getAlbumStorageDir: %s", folder.getAbsolutePath());
    }
  };

  public void takePicture() {
    currentFilename = String.format("robocar-%s.jpg", getTimestamp());
    cameraHandler.takePicture();
  }

  private String getTimestamp() {
    return dateFormat.format(new Date());
  }

  /**
   * Starts a training session.
   */
  public void startTrainingSession(final SpeedOwner speedOwner) {
    if (inTraining) {
      return;
    }
    inTraining = true;

    final String sessionId = UUID.randomUUID().toString().replace("-", "");
    final int[] count = new int[] {0};
    final long delayMillis = 1000;
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        int[] speeds = speedOwner.getSpeeds();
        currentFilename = String.format(Locale.US,
            "robocar-%s-%d-%d-%d-%d-%d.jpg", sessionId, count[0],
            speeds[0], speeds[1], speeds[2], speeds[3]);
        Timber.d("Capturing picture: %s", currentFilename);
        cameraHandler.takePicture();
        count[0]++;
        backgroundHandler.postDelayed(this, delayMillis);
      }
    };

    backgroundHandler.postDelayed(runnable, delayMillis);
  }

  /**
   * Stops a training session.
   */
  public void stopTrainingSession() {
    if (!inTraining) {
      return;
    }

    inTraining = false;
    backgroundHandler.removeCallbacksAndMessages(null);
  }

  public void shutDown() {
    cameraHandler.shutDown();
  }

  @Override
  public void onImageAvailable(ImageReader reader) {
    Timber.d("Image is ready.");
    Image image = reader.acquireNextImage();
    Bitmap bitmap = imagePreprocessor.convertImage(image);
    ImageUtils.saveBitmap(bitmap, folder.getAbsolutePath(), currentFilename);
  }

  /**
   * Checks if external storage is available for read and write.
   */
  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      return true;
    }
    return false;
  }

  /**
   * Checks if external storage is available to at least.
   */
  public boolean isExternalStorageReadable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      return true;
    }
    return false;
  }

  /**
   * Get the directory for the user's public pictures directory.
   * This is /storage/emulated/0/Pictures/robocar
   */
  public File getAlbumStorageDir(String folder) {
    File file = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), folder);
    if (!file.mkdirs()) {
      Timber.e("mkdirs failed for: %s.", file.getAbsolutePath());
    }
    return file;
  }
}
