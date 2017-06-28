package com.zugaldia.robocar.software.camera;

import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * Save a photo to disk
 */
public class ImageSaver implements Runnable {

  private Image image;
  private File root;
  private String filename;

  public ImageSaver(Image image, File root, String filename) {
    this.image = image;
    this.root = root;
    this.filename = filename;
  }

  @Override
  public void run() {
    if (image == null) {
      Timber.w("Empty image, skipping.");
      return;
    }

    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(getDestination(root, filename));
      output.write(bytes);
    } catch (IOException e) {
      Timber.e(e, "Failed to save photo.");
    } finally {
      Timber.d("Photo saved.");
      image.close();
      if (null != output) {
        try {
          output.close();
        } catch (IOException e) {
          Timber.e(e, "Failed to close file system resources.");
        }
      }
    }
  }

  private static File getDestination(File root, String filename) {
    File destination = new File(root, filename);
    Timber.d("Saving to: %s", destination.getAbsolutePath());
    return destination;
  }
}
