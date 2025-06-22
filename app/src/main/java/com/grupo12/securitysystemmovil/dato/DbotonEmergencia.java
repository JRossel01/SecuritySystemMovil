package com.grupo12.securitysystemmovil.dato;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiConsumer;

public class DbotonEmergencia {

    private final Context context;

    public DbotonEmergencia(Context context) {
        this.context = context;
    }


    public void registrarEvento(String mensaje, String tipo, String nivel) {
        obtenerUbicacionActual((latitud, longitud) -> {
            guardarEvento(mensaje, tipo, nivel, latitud, longitud);
        });
    }

    private void guardarEvento(String mensaje, String tipo, String nivel, double latitud, double longitud) {
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        int userId = obtenerUserId();
        int vehicleId = obtenerVehicleId();
        int tripId = obtenerTripId();

        SQLiteDatabase db = new movilBD(context).getWritableDatabase();
        db.execSQL("INSERT INTO eventos (mensaje, tipo, nivel, fecha, hora, latitud, longitud, user_id, vehicle_id, trip_id, enviado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{mensaje, tipo, nivel, fecha, hora, latitud, longitud, userId, vehicleId, tripId, 0});
        db.close();
    }

    private void obtenerUbicacionActual(BiConsumer<Double, Double> callback) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // o mostrar un log
        }

        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitud = location.getLatitude();
                        double longitud = location.getLongitude();
                        callback.accept(latitud, longitud);
                    }
                });
    }

    private int obtenerUserId() {
        SQLiteDatabase db = new movilBD(context).getReadableDatabase();
        int id = -1;
        Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        db.close();
        return id;
    }

    private int obtenerVehicleId() {
        SQLiteDatabase db = new movilBD(context).getReadableDatabase();
        int id = -1;
        Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        db.close();
        return id;
    }

    private int obtenerTripId() {
        SQLiteDatabase db = new movilBD(context).getReadableDatabase();
        int id = -1;
        Cursor cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        db.close();
        return id;
    }

}
