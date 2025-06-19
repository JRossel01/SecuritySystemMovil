package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Dvehiculo;

import java.util.List;

public class Nvehiculo {
    private final Dvehiculo dvehiculo;

    public interface OnVehiculosRecibidosListener {
        void onExito(List<JsonObject> vehiculos);
        void onError(String mensaje);
    }

    public Nvehiculo(Context context) {
        dvehiculo = new Dvehiculo(context);
    }

    public void obtenerVehiculos(OnVehiculosRecibidosListener listener) {
        dvehiculo.obtenerVehiculos(new Dvehiculo.OnVehiculosObtenidosListener() {
            @Override
            public void onExito(List<JsonObject> listaVehiculos) {
                listener.onExito(listaVehiculos);
            }

            @Override
            public void onError(String mensaje) {
                listener.onError(mensaje);
            }
        });
    }
    public void guardarVehiculo(Context context, JsonObject vehiculoJson) {
        int id = vehiculoJson.get("id").getAsInt();
        String nombre = vehiculoJson.get("nombre").getAsString();
        String placa = vehiculoJson.get("placa").getAsString();
        int velocidadMaxima = vehiculoJson.get("velocidad_maxima").getAsInt();

        dvehiculo.guardarVehiculo(id, nombre, placa, velocidadMaxima);
    }

    public Dvehiculo.VehiculoData obtenerVehiculoLocal() {
        return dvehiculo.obtenerVehiculoLocal();
    }

}
