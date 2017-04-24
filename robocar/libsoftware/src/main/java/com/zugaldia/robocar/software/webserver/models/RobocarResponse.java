package com.zugaldia.robocar.software.webserver.models;

/**
 * Created by antonio on 4/5/17.
 */

public class RobocarResponse {

  private int code;
  private String message;

  public RobocarResponse() {
  }

  public RobocarResponse(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
