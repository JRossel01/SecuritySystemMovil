package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Druta {
    public LatLng origen;
    public List<LatLng> paradas;
    public LatLng destino;
    private String idVehiculo;
    private movilBD dbHelper;

    public Druta(Context context) {
        dbHelper = new movilBD(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Obtener origen y destino desde tabla ruta
        try {
            Cursor cursor = db.rawQuery("SELECT origen_lat, origen_lng, destino_lat, destino_lng FROM ruta LIMIT 1", null);
            if (cursor.moveToFirst()) {
                double origenLat = Double.parseDouble(cursor.getString(0));
                double origenLng = Double.parseDouble(cursor.getString(1));
                double destinoLat = Double.parseDouble(cursor.getString(2));
                double destinoLng = Double.parseDouble(cursor.getString(3));

                origen = new LatLng(origenLat, origenLng);
                destino = new LatLng(destinoLat, destinoLng);
            } else {
                // Valores por defecto si no existe ruta
                origen = new LatLng(0, 0);
                destino = new LatLng(0, 0);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("Druta", "Error al leer ruta desde la BD", e);
            origen = new LatLng(0, 0);
            destino = new LatLng(0, 0);
        }

        // Obtener paradas desde tabla paradas
        paradas = new ArrayList<>();
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
            Log.e("Druta", "Error al leer paradas desde la BD", e);
            paradas = new ArrayList<>(); // Asegura que no sea null
        }

        db.close();
    }


    public LatLng getOrigen() {
        return origen;
    }

    public List<LatLng> getParadas() {
        return paradas;
    }

    public LatLng getDestino() {
        return destino;
    }

    public void guardarEvento(String mensaje, double latitud, double longitud) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (db != null) {
            ContentValues values = new ContentValues();
            values.put("mensaje", mensaje);
            values.put("tipo", "Ruta"); // o el tipo real según contexto
            values.put("nivel", "Informacion");   // o "advertencia", "critico", según contexto

            // Fecha y hora actual
            String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            values.put("fecha", fecha);
            values.put("hora", hora);
            values.put("latitud", latitud);
            values.put("longitud", longitud);

            // Obtener vehicle_id desde tabla vehiculo
            int vehicleId = -1;
            try {
                android.database.Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
                if (cursor.moveToFirst()) {
                    vehicleId = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("Druta", "Error al obtener vehicle_id", e);
            }
            values.put("vehicle_id", vehicleId);

            // Obtener user_id desde tabla conductores (activo = 1)
            int userId = -1;
            try {
                android.database.Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
                if (cursor.moveToFirst()) {
                    userId = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("Druta", "Error al obtener user_id", e);
            }
            values.put("user_id", userId);

            // Obtener trip_id
            int tripId = -1;
            try {
                Cursor cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
                if (cursor.moveToFirst()) {
                    tripId = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("Druta", "Error al obtener trip_id", e);
            }
            values.put("trip_id", tripId);

            // enviado se crea con 0
            values.put("enviado", 0);

            // Insertar evento
            long resultado = db.insert("eventos", null, values);
            db.close();

            if (resultado != -1) {
                Log.i("RutaEvento", "Evento guardado correctamente: " +
                        "Mensaje='" + mensaje + "', Fecha=" + fecha + ", Hora=" + hora +
                        ", Lat=" + latitud + ", Lon=" + longitud +
                        ", vehicle_id=" + vehicleId + ", user_id=" + userId);
            } else {
                Log.e("RutaEvento", "Error al guardar el evento: " + mensaje);
            }
        }
    }

    public String obtenerNombreVehiculo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String nombre = "vehículo";
        Cursor cursor = db.rawQuery("SELECT nombre FROM vehiculo LIMIT 1", null);
        if (cursor.moveToFirst()) nombre = cursor.getString(0);
        cursor.close();
        db.close();
        return nombre;
    }

    public String obtenerNombreConductor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String nombre = "conductor";
        Cursor cursor = db.rawQuery("SELECT nombre, apellido FROM conductores WHERE activo = 1 LIMIT 1", null);
        if (cursor.moveToFirst()) nombre = cursor.getString(0) + " " + cursor.getString(1);
        cursor.close();
        db.close();
        return nombre;
    }
}
