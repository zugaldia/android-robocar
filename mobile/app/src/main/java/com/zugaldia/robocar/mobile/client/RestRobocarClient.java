package com.zugaldia.robocar.mobile.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestRobocarClient implements RobocarClient {

    RetrofitRobocarClient mRetrofitRobocarClient;

    public RestRobocarClient(String baseUrl) {
            mRetrofitRobocarClient = retrofit(baseUrl).create(RetrofitRobocarClient.class);
    }

    @Override
    public void changeSpeed(Integer leftSpeed, Integer rightSpeed) {

        RobocarSpeed speed = new RobocarSpeed ();
        speed.setLeft(leftSpeed);
        speed.setRight(rightSpeed);

        Call<RobocarSpeed> call = this.mRetrofitRobocarClient.changeSpeed(speed);
        call.enqueue(new Callback<RobocarSpeed>() {
            @Override
            public void onResponse(Call<RobocarSpeed> call, Response<RobocarSpeed> response) {
            }

            @Override
            public void onFailure(Call<RobocarSpeed> call, Throwable t) {
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
