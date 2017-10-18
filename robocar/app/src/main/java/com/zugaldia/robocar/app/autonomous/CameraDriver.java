package com.zugaldia.robocar.app.autonomous;

import android.content.Context;
import android.media.Image;
import android.media.ImageReader;

import com.getkeepsafe.relinker.ReLinker;
import com.zugaldia.robocar.cv.HistogramPosition;
import com.zugaldia.robocar.cv.LaneManager;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.camera.CameraOperator;
import com.zugaldia.robocar.software.camera.CameraOperatorListener;
import com.zugaldia.robocar.software.camera.ImageSaver;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;

import java.io.File;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * Autonomous driving using CV processing on camera photos.
 */
public class CameraDriver implements CameraOperatorListener, ImageReader.OnImageAvailableListener {

  private ReLinker.Logger logger = new ReLinker.Logger() {
    @Override
    public void log(String message) {
      Timber.d(message);
    }
  };

  private final static String PHOTO_FILENAME = "cv.jpg";

  private AdafruitMotorHat motorHat;
  private CameraOperator cameraOperator;
  private boolean requestSessionEnds = false;

  public CameraDriver(Context context, AdafruitMotorHat motorHat) {
    this.motorHat = motorHat;
    cameraOperator = new CameraOperator(context, this);
    ReLinker.log(logger).recursively().loadLibrary(context, "jniopencv_core");
    ReLinker.log(logger).recursively().loadLibrary(context, "opencv_core");
    ReLinker.log(logger).recursively().loadLibrary(context, "jniopencv_imgcodecs");
    ReLinker.log(logger).recursively().loadLibrary(context, "opencv_imgcodecs");
  }

  public void start() {
    requestSessionEnds = false;
    if (cameraOperator.isInSession()) {
      sessionStarted();
    } else {
      cameraOperator.startSession(this);
    }
  }

  public void stop() {
    requestSessionEnds = true;
  }

  @Override
  public void sessionStarted() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Timber.d("Camera is ready, taking a picture.");
        cameraOperator.takePicture();
      }
    }).start();
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {
    if (requestSessionEnds) {
      Timber.d("Ending session.");
      cameraOperator.endSession();
      requestSessionEnds = false;
      return;
    }

    Timber.d("Picture is available.");
    File root = ImageSaver.getRoot(CameraOperator.ROBOCAR_FOLDER);

    Image image = reader.acquireLatestImage();
    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    opencv_core.Mat encoded = opencv_imgcodecs.imread(new BytePointer(bytes));
    //opencv_core.Mat decoded = opencv_imgcodecs.imdecode(encoded, opencv_imgcodecs.IMREAD_UNCHANGED);
    LaneManager.writeImage(new File(root, "test.jpg").getAbsolutePath(), encoded);
    image.close();
    encoded.release();
    //decoded.release();
    cameraOperator.endSession();

//
//    new ImageSaver(reader.acquireLatestImage(), root, PHOTO_FILENAME).run();
//    processPhoto(root, PHOTO_FILENAME);
  }

  private void processPhoto(File root, String filename) {
    // Ideally we can do this without having to save the image first
    File path = new File(root, filename);
    opencv_core.Mat src = LaneManager.readImage(path.getAbsolutePath());
    HistogramPosition position = LaneManager.findLane(src);
    Timber.d("Processed: %d", position.getBinIndex());
    cameraOperator.takePicture();
  }
}
