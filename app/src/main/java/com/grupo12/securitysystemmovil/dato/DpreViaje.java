package com.grupo12.securitysystemmovil.dato;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.util.ArrayList;
import java.util.List;

public class DpreViaje {

    private final SQLiteDatabase db;

    public DpreViaje(Context context) {
        this.db = new movilBD(context).getReadableDatabase();
    }

    public ViajeData obtenerDatosViaje() {
        ViajeData data = new ViajeData();

        // Obtener datos del viaje
        Cursor cursor = db.rawQuery("SELECT * FROM viaje LIMIT 1", null);
        if (cursor.moveToFirst()) {
            data.fechaInicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio"));
            data.horaInicio = cursor.getString(cursor.getColumnIndexOrThrow("hora_inicio"));
            data.vehicleId = cursor.getInt(cursor.getColumnIndexOrThrow("vehicle_id"));
        }
        cursor.close();

        // Obtener datos del vehículo
        cursor = db.rawQuery("SELECT * FROM vehiculo", null);
        if (cursor.moveToFirst()) {
            data.nombreVehiculo = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            data.placaVehiculo = cursor.getString(cursor.getColumnIndexOrThrow("placa"));
        }
        cursor.close();

        // Obtener datos de la ruta
        cursor = db.rawQuery("SELECT * FROM ruta LIMIT 1", null);
        if (cursor.moveToFirst()) {
            data.nombreRuta = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            data.origenLat = Double.parseDouble(cursor.getString(cursor.getColumnIndexOrThrow("origen_lat")));
            data.origenLng = Double.parseDouble(cursor.getString(cursor.getColumnIndexOrThrow("origen_lng")));
        }
        cursor.close();

        // Obtener conductores
        cursor = db.rawQuery("SELECT * FROM conductores", null);
        while (cursor.moveToNext()) {
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido"));
            data.conductores.add(nombre + " " + apellido);
        }
        cursor.close();

        // Obtener paradas
        cursor = db.rawQuery("SELECT * FROM paradas ORDER BY posicion ASC", null);
        while (cursor.moveToNext()) {
            String nombreParada = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            data.paradas.add(nombreParada);
        }
        cursor.close();

        return data;
    }

    public static class ViajeData {
        public String fechaInicio;
        public String horaInicio;
        public int vehicleId;

        public String nombreVehiculo;
        public String placaVehiculo;

        public String nombreRuta;
        public double origenLat;
        public double origenLng;

        public List<String> conductores = new ArrayList<>();
        public List<String> paradas = new ArrayList<>();
    }

    public void registrarEvento(String mensaje, String tipo, String nivel, double latitud, double longitud) {
        // Obtener user_id activo
        int userId = -1;
        Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();

        // Obtener trip_id
        int tripId = -1;
        cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
        if (cursor.moveToFirst()) {
            tripId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();

        // Obtener vehicle_id
        int vehicleId = -1;
        cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
        if (cursor.moveToFirst()) {
            vehicleId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();

        // Fecha y hora actual
        String fecha = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String hora = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // Insertar en la tabla eventos
        db.execSQL("INSERT INTO eventos (mensaje, tipo, nivel, fecha, hora, latitud, longitud, user_id, vehicle_id, trip_id, enviado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{mensaje, tipo, nivel, fecha, hora, latitud, longitud, userId, vehicleId, tripId, 0});
    }

}
