package com.zugaldia.robocar.software.webserver;

import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by antonio on 4/5/17.
 */

public interface RobocarService {

  @GET(LocalWebServer.ENDPOINT_GET_STATUS)
  Call<RobocarStatus> getStatus();

  @POST(LocalWebServer.ENDPOINT_POST_MOVE)
  Call<RobocarResponse> postMove(@Body RobocarMove move);

}
