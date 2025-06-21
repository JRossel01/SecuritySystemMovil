package com.grupo12.securitysystemmovil.dato.Api;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Evento.DeventoRequest;
import com.grupo12.securitysystemmovil.dato.Seguimiento.GpsLocationRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // Recibe datos del usuario
    @GET("user-by-ci/{ci}")
    Call<JsonObject> getUserByCI(@Path("ci") String ci);

    // Recibe vehiculos registrados
    @GET("vehiculos")
    Call<List<JsonObject>> getVehiculos();

    // Envía evento al backend
    @POST("eventos")
    Call<JsonObject> enviarEvento(@Body DeventoRequest evento);

    // Recibe datos del viaje del usuario
    @GET("trip/activo/{userId}")
    Call<JsonObject> getTripActivo(@Path("userId") int userId);

    // Enviar la ubicación al backend
    @POST("gpslocations")
    Call<JsonObject> enviarGpsLocation(@Body GpsLocationRequest gpsLocation);
}
