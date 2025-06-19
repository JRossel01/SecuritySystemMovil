package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Dvelocidad {

    private Context context;
    private MediaPlayer mediaPlayer;

    public void setContext(Context context) {
        this.context = context;
    }

    public void crearEvento(String mensaje, String tipo, String nivel, double latitud, double longitud) {
        if (context == null) return;

        movilBD dbHelper = new movilBD(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        ContentValues values = new ContentValues();
        values.put("mensaje", mensaje);
        values.put("tipo", tipo);
        values.put("nivel", nivel);
        values.put("fecha", fecha);
        values.put("hora", hora);
        values.put("latitud", latitud);
        values.put("longitud", longitud);

        // Obtener vehicle_id
        int vehicleId = -1;
        try {
            android.database.Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
            if (cursor.moveToFirst()) {
                vehicleId = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("Dvelocidad", "Error al obtener vehicle_id", e);
        }
        values.put("vehicle_id", vehicleId);

        // Obtener user_id
        int userId = -1;
        try {
            android.database.Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
            if (cursor.moveToFirst()) {
                userId = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("Dvelocidad", "Error al obtener user_id", e);
        }
        values.put("user_id", userId);

        // Asignar trip_id fijo
        values.put("trip_id", 1);

        // enviado se crea con 0
        values.put("enviado", 0);

        long resultado = db.insert("eventos", null, values);

        if (resultado != -1) {
            Log.i("Velocidad", "Evento registrado: " + mensaje);
            Toast.makeText(context, "Evento registrado", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("Velocidad", "Error al registrar evento: " + mensaje);
            Toast.makeText(context, "Error al registrar evento", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    public void activarAlertaSonora() {
        if (context == null || mediaPlayer != null && mediaPlayer.isPlaying()) return;

        mediaPlayer = MediaPlayer.create(context, R.raw.alerta_velocidad);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Log.i("Velocidad", "Alerta sonora activada.");
    }

    public void detenerAlertaSonora() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            Log.i("Velocidad", "Alerta sonora detenida.");
        }
    }
}
