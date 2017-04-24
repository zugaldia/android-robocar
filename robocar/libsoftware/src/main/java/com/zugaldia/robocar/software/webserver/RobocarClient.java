package com.zugaldia.robocar.software.webserver;

import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by antonio on 4/5/17.
 */

public class RobocarClient {

  private RobocarService service;

  public RobocarClient() {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("http://localhost:8080")
      .addConverterFactory(GsonConverterFactory.create())
      .build();

    service = retrofit.create(RobocarService.class);
  }

  public void getStatus(Callback<RobocarStatus> callback) {
    service.getStatus().enqueue(callback);
  }

  public void postMove(RobocarMove move, Callback<RobocarResponse> callback) {
    service.postMove(move).enqueue(callback);
  }
}
