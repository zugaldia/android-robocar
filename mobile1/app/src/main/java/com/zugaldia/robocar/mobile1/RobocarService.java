package com.zugaldia.robocar.mobile1;

import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RobocarService {
    @POST("api/speed")
    Call<RobocarSpeed> changeSpeed(@Body RobocarSpeed speed);
}
