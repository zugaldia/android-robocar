package com.zugaldia.robocar.software.camera;

import android.media.Image;
import android.os.Environment;

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

  /**
   * Checks if external storage is available for read and write.
   */
  public static boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

  /**
   * Get the directory for the user's public pictures directory.
   * This is /storage/emulated/0/Pictures/robocar
   */
  public static File getRoot(String robocarFolder) {
    File file = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), robocarFolder);

    if (file.mkdirs()) {
      Timber.d("Root folder created: %s.", file.getAbsolutePath());
    } else {
      Timber.e("Could not create root folder (it probably existed already): %s.",
          file.getAbsolutePath());
    }

    return file;
  }

  private static File getDestination(File root, String filename) {
    File destination = new File(root, filename);
    Timber.d("Saving to: %s", destination.getAbsolutePath());
    return destination;
  }
}
