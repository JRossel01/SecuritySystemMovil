package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.media.MediaPlayer;
import android.util.Log;

import com.grupo12.securitysystemmovil.R;

public class Dsomnolencia {
    private movilBD dbHelper;
    private Context context;
    private MediaPlayer mediaPlayer;

    public Dsomnolencia(Context context) {
        this.context = context;
        dbHelper = new movilBD(context);
    }

    public void registrarEvento(String mensaje, String tipo, String nivel, double latitud, double longitud) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (db != null) {
            ContentValues values = new ContentValues();
            values.put("mensaje", mensaje);
            values.put("tipo", tipo);
            values.put("nivel", nivel);

            String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            values.put("fecha", fecha);
            values.put("hora", hora);
            values.put("latitud", latitud);
            values.put("longitud", longitud);

            // Obtener vehicle_id
            int vehicleId = -1;
            try {
                Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
                if (cursor.moveToFirst()) {
                    vehicleId = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("Dsomnolencia", "Error al obtener vehicle_id", e);
            }
            values.put("vehicle_id", vehicleId);

            // Obtener user_id
            int userId = -1;
            try {
                Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
                if (cursor.moveToFirst()) {
                    userId = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("Dsomnolencia", "Error al obtener user_id", e);
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
                Log.e("Dsomnolencia", "Error al obtener trip_id", e);
            }
            values.put("trip_id", tripId);


            // enviado se crea con 0
            values.put("enviado", 0);

            long resultado = db.insert("eventos", null, values);
            db.close();

            if (resultado != -1) {
                Log.i("Dsomnolencia", "Evento guardado: " + mensaje);
            } else {
                Log.e("Dsomnolencia", "Error al guardar el evento: " + mensaje);
            }
        }
    }

    public void activarAlertaSonora() {
        if (context == null || (mediaPlayer != null && mediaPlayer.isPlaying())) return;

        mediaPlayer = MediaPlayer.create(context, R.raw.alerta_velocidad);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Log.i("Somnolencia", "Alerta sonora activada.");
    }

    public void detenerAlertaSonora() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.i("Somnolencia", "Alerta sonora detenida.");
        }
    }

}

