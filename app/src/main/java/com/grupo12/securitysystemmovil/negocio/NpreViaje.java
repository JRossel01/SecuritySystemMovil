package com.grupo12.securitysystemmovil.negocio;


import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.dato.DpreViaje;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NpreViaje {
    private final Context context;
    private final DpreViaje.ViajeData datos;
    private final FusedLocationProviderClient locationClient;


    public NpreViaje(Context context) {
        this.context = context;
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
        DpreViaje dpreViaje = new DpreViaje(context);
        this.datos = dpreViaje.obtenerDatosViaje();
    }

    public String getNombreRuta() {
        return datos.nombreRuta;
    }

    public List<String> getParadas() {
        return datos.paradas;
    }

    public List<String> getConductores() {
        return datos.conductores;
    }

    public String getNombreVehiculo() {
        return datos.nombreVehiculo;
    }

    public String getPlacaVehiculo() {
        return datos.placaVehiculo;
    }

    public String getFechaInicio() {
        return datos.fechaInicio;
    }

    public String getHoraInicio() {
        return datos.horaInicio;
    }

    public double getLatitudOrigen() {
        return datos.origenLat;
    }

    public double getLongitudOrigen() {
        return datos.origenLng;
    }

    public int getVehicleId() {
        return datos.vehicleId;
    }

    @SuppressLint("MissingPermission")
    public void obtenerUbicacion(Consumer<Location> callback) {
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.accept(location);
                    } else {
                        callback.accept(null);
                    }
                });
    }

    public void verificarUbicacion(Consumer<Boolean> callback) {
        obtenerUbicacion(location -> {
            if (location != null) {
                double distancia = calcularDistancia(location.getLatitude(), location.getLongitude(),
                        datos.origenLat, datos.origenLng);
                callback.accept(distancia <= 50.0);
            } else {
                callback.accept(false);
            }
        });
    }

    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        float[] resultados = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, resultados);
        return resultados[0];
    }

    public void registrarEvento(Context context) {
        obtenerUbicacion(location -> {
            if (location != null) {
                String mensaje = "Se inicio un viaje";
                String tipo = "Viaje";
                String nivel = "Informacion";
                double latitud = location.getLatitude();
                double longitud = location.getLongitude();

                DpreViaje dpreViaje = new DpreViaje(context);
                dpreViaje.registrarEvento(mensaje, tipo, nivel, latitud, longitud);
            }
        });
    }

}
