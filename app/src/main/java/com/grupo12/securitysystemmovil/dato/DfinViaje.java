package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DfinViaje {
    private SQLiteDatabase db;

    public DfinViaje(Context context) {
        this.db = new movilBD(context).getWritableDatabase();
    }

    public void registrarEvento(double latitud, double longitud) {
        ContentValues valores = new ContentValues();
        valores.put("mensaje", "Viaje finalizado exitosamente");
        valores.put("tipo", "Viaje");
        valores.put("nivel", "Informacion");

        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        valores.put("fecha", fecha);
        valores.put("hora", hora);
        valores.put("latitud", latitud);
        valores.put("longitud", longitud);
        valores.put("user_id", obtenerIdConductorActivo());
        valores.put("vehicle_id", obtenerVehicleId());
        valores.put("trip_id", obtenerTripId());
        valores.put("enviado", 0);

        db.insert("eventos", null, valores);
    }

    public boolean todosLosEventosEnviados() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM eventos WHERE enviado = 0", null);
        boolean todos = true;

        if (cursor.moveToFirst()) {
            todos = cursor.getInt(0) == 0;
        }

        cursor.close();
        return todos;
    }

    public void limpiarBDExceptoVehiculo() {
        db.execSQL("DELETE FROM viaje");
        db.execSQL("DELETE FROM ruta");
        db.execSQL("DELETE FROM paradas");
        db.execSQL("DELETE FROM eventos");
        db.execSQL("DELETE FROM gestor");
        db.execSQL("DELETE FROM conductores");
    }

    private int obtenerIdConductorActivo() {
        Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1", null);
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    private int obtenerVehicleId() {
        Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    private int obtenerTripId() {
        Cursor cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }
}
