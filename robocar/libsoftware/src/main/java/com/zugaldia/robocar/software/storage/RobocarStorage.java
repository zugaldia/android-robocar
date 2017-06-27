package com.zugaldia.robocar.software.storage;

import android.os.Environment;

import java.io.File;

import timber.log.Timber;

/**
 * We use Android's external storage options for a variety of reasons (e.g to load custom settings,
 * to store photos). This class takes care of shared stuff across components.
 */
public class RobocarStorage {

  private final static String PATH_PHOTOS = "robocar";

  /**
   * Checks if external storage is available for read and write.
   */
  public static boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);

  }

  /**
   * Checks if external storage is available to at least read.
   */
  public static boolean isExternalStorageReadable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
  }

  /**
   * Get the directory for the user's public pictures.
   * This is /storage/emulated/0/Pictures/robocar.
   */
  public static File getPicturesStorageDirectory() {
    File file = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), PATH_PHOTOS);

    if (file.mkdirs()) {
      Timber.d("Pictures directory (%s) created.", file.getAbsolutePath());
    } else {
      Timber.e("Pictures directory (%s) not created (it probably existed already).",
          file.getAbsolutePath());
    }

    return file;
  }

  /**
   * Get the directory for the user's public downloads.
   * This is /storage/emulated/0/Downloads
   */
  public static File getDownloadsStorageDirectory() {
    File file = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS), PATH_PHOTOS);

    if (file.mkdirs()) {
      Timber.d("Downloads directory (%s) created.", file.getAbsolutePath());
    } else {
      Timber.e("Downloads directory (%s) not created (it probably existed already).",
          file.getAbsolutePath());
    }

    return file;
  }
}
