package com.zugaldia.robocar.mobile.client;

import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitRobocarClient {
    @POST("api/speed")
    Call<RobocarSpeed> changeSpeed(@Body RobocarSpeed speed);
}
