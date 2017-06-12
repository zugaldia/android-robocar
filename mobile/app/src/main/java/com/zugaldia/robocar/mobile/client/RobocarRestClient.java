package com.zugaldia.robocar.mobile.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zugaldia.robocar.software.webserver.RobocarService;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RobocarRestClient implements RobocarClient {

    RobocarService mRobocarService;

    public RobocarRestClient(String baseUrl) {
            mRobocarService = retrofit(baseUrl).create(RobocarService.class);
    }

    @Override
    public void setSpeed(Integer leftSpeed, Integer rightSpeed) {

        RobocarSpeed speed = new RobocarSpeed ();
        speed.setLeft(leftSpeed);
        speed.setRight(rightSpeed);

        Call<RobocarResponse> call = this.mRobocarService.postSpeed(speed);
        call.enqueue(new Callback<RobocarResponse>() {
            @Override
            public void onResponse(Call<RobocarResponse> call, Response<RobocarResponse> response) {
            }

            @Override
            public void onFailure(Call<RobocarResponse> call, Throwable t) {
            }
        });
    }

    private Retrofit retrofit(String baseUrl) {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }
}
