package com.zugaldia.robocar.software.options;

import com.google.gson.Gson;
import com.zugaldia.robocar.software.RobocarConstants;
import com.zugaldia.robocar.software.storage.RobocarStorage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Each Robocar can be different in many ways and might need custom settings to work (e.g. motor
 * driver, bluetooth controller...). This class is in charge of managing all these options
 * via a local JSON file (for now). In the future we might support other methods like:
 * - Remote JSON file
 * - Via the companion phone app
 * - Integrated with official Android Things deployment tools
 * - Your own (PRs are welcome :)
 */
public class RobocarOptions {

  private final static String OPTIONS_FILE = "robocar.json";

  private OptionsCallback callback;
  private OptionsModel options;

  public RobocarOptions(OptionsCallback callback) {
    this.callback = callback;
    this.options = new OptionsModel();
  }

  public OptionsModel getOptions() {
    return options;
  }

  /**
   * Options are read from disk (in the future can be a remote server) and, therefore,
   * delivered asynchronously to avoid blocking the UI thread.
   */
  public void load() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        loadLocalJsonFile();
      }
    }).run();
  }

  private void loadLocalJsonFile() {
    if (!RobocarStorage.isExternalStorageReadable()) {
      callback.onError("External storage is not readable.");
      return;
    }

    File optionsFile = new File(RobocarStorage.getDownloadsStorageDirectory(), OPTIONS_FILE);
    if (!optionsFile.exists()) {
      callback.onError(String.format(RobocarConstants.DEFAULT_LOCALE,
          "No options found on external storage (%s).",
          optionsFile.getAbsolutePath()));
      return;
    }

    FileReader reader = null;
    OptionsModel options = null;

    try {
      reader = new FileReader(optionsFile);
      options = new Gson().fromJson(reader, OptionsModel.class);
    } catch (Exception e) {
      callback.onError(e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          callback.onError(e.getMessage());
        }
      }
    }

    if (options == null) {
      callback.onError("Error parsing options file, or the file was empty.");
      return;
    }

    this.options = options;
    callback.onLoad();
  }
}
