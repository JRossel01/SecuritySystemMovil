package com.grupo12.securitysystemmovil.dato.Api;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("user-by-ci/{ci}")
    Call<JsonObject> getUserByCI(@Path("ci") String ci);

    @GET("vehiculos")
    Call<List<JsonObject>> getVehiculos();
}
