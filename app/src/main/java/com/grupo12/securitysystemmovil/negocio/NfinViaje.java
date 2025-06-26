package com.grupo12.securitysystemmovil.negocio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.dato.DfinViaje;

import java.util.function.Consumer;

public class NfinViaje {
    private final DfinViaje dfinViaje;
    private final FusedLocationProviderClient locationClient;

    public NfinViaje(Context context) {
        dfinViaje = new DfinViaje(context);
        locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void registrarEvento(Consumer<Boolean> callback) {
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        dfinViaje.registrarEvento(lat, lng);

                        // 2. Esperar 5 segundos y luego verificar si todos los eventos han sido enviados
                        new Handler().postDelayed(() -> {
                            boolean enviados = dfinViaje.todosLosEventosEnviados();
                            if (enviados) {
                                dfinViaje.limpiarBDExceptoVehiculo();
                            }
                            callback.accept(enviados);
                        }, 5000);
                    } else {
                        callback.accept(false);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(false);
                });
    }
}
