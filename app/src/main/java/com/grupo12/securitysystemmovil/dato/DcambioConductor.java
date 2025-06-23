package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class DcambioConductor {
    private SQLiteDatabase db;
    private final Context context;

    public DcambioConductor(Context context) {
        this.context = context;
        this.db = new movilBD(context).getWritableDatabase();
    }

    // Obtener lista de conductores inactivos
    public List<Dconductor> obtenerConductoresInactivos() {
        List<Dconductor> lista = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id, nombre, apellido, ruta_imagen FROM conductores WHERE activo = 0", null);

        if (cursor.moveToFirst()) {
            do {
                Dconductor c = new Dconductor();
                c.setId(cursor.getInt(0));
                c.setNombre(cursor.getString(1));
                c.setApellido(cursor.getString(2));
                c.setFoto(cursor.getString(3));
                lista.add(c);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    // Obtener conductor activo actual
    public Dconductor obtenerConductorActivo() {
        Cursor cursor = db.rawQuery("SELECT id, nombre, apellido, ruta_imagen FROM conductores WHERE activo = 1 LIMIT 1", null);
        if (cursor.moveToFirst()) {
            Dconductor c = new Dconductor();
            c.setId(cursor.getInt(0));
            c.setNombre(cursor.getString(1));
            c.setApellido(cursor.getString(2));
            c.setFoto(cursor.getString(3)); // ruta_imagen
            cursor.close();
            return c;
        }
        cursor.close();
        return null;
    }

    // Cambiar conductor activo
    public boolean activarConductor(int nuevoId) {
        try {
            db.execSQL("UPDATE conductores SET activo = 0 WHERE activo = 1");

            ContentValues valores = new ContentValues();
            valores.put("activo", 1);
            int filasAfectadas = db.update("conductores", valores, "id = ?", new String[]{String.valueOf(nuevoId)});

            return filasAfectadas > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registrarEvento(String mensaje) {
        obtenerUbicacion((latitud, longitud) -> {
            guardarEvento(mensaje, "Ruta", "Informacion", latitud, longitud);
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

    private void obtenerUbicacion(BiConsumer<Double, Double> callback) {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
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

    public List<LatLng> obtenerParadasComoLatLng() {
        List<LatLng> paradas = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery("SELECT latitud, longitud FROM paradas ORDER BY posicion ASC", null);
            if (cursor.moveToFirst()) {
                do {
                    double lat = Double.parseDouble(cursor.getString(0));
                    double lng = Double.parseDouble(cursor.getString(1));
                    paradas.add(new LatLng(lat, lng));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("DcambioConductor", "Error al leer paradas", e);
            paradas = new ArrayList<>();
        }
        return paradas;
    }

}
