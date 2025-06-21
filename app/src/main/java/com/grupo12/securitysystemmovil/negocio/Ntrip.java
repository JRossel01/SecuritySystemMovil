package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Dtrip;

import retrofit2.Call;
import retrofit2.Response;

public class Ntrip {

    private Dtrip dtrip;

    public Ntrip(Context context) {
        this.dtrip = new Dtrip(context);
    }

    // Llama a la descarga y guardado local del trip activo
    public void guardarTrip(int userId, int idConductorActual, TripCallback callback) {
        dtrip.guardarTrip(userId, idConductorActual, new Dtrip.TripCallback() {
            @Override
            public void onSuccess() {
                Log.i("Ntrip", "Viaje activo guardado correctamente");
                callback.onSuccess();
            }

            @Override
            public void onFailure(String error) {
                Log.e("Ntrip", "Error al guardar viaje activo: " + error);
                callback.onFailure(error);
            }
        });
    }


    // Callback para comunicar resultado a la capa de presentación
    public interface TripCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public int obtenerVehicleId() {
        return dtrip.obtenerVehicleId();
    }


}
