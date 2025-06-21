package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DloginFacial {
    public interface OnUserFoundListener {
        void onSuccess(JsonObject userData);

        void onError(String error);
    }

    public void buscarUsuarioPorCI(String ci, OnUserFoundListener listener) {
        Log.d("DloginFacial", "Iniciando búsqueda del usuario con CI: " + ci);
        ApiService service = ApiClient.getClient().create(ApiService.class);
        Call<JsonObject> call = service.getUserByCI(ci);

        Log.d("DloginFacial", "Llamada Retrofit creada. Ejecutando...");

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                Log.d("DloginFacial", "Respuesta recibida. Código: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    Log.d("DloginFacial", "Usuario encontrado: " + response.body().toString());

                    listener.onSuccess(response.body());
                } else {
                    listener.onError("Usuario no encontrado o respuesta inválida");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                listener.onError("Error de red: " + t.getMessage());
                Log.e("DloginFacial", "Error al consultar CI", t);
            }
        });
    }

    public interface OnImagenDescargadaListener {
        void onDescargada(Bitmap bitmap);

        void onError(String error);
    }

    public void descargarImagen(String urlString, OnImagenDescargadaListener listener) {
        new Thread(() -> {
            try {
                Log.d("DloginFacial", "Descargando imagen desde: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                listener.onDescargada(bitmap);
            } catch (Exception e) {
                Log.e("DloginFacial", "Error al descargar imagen", e);
                listener.onError(e.getMessage());
            }
        }).start();
    }

    public void limpiarBD(Context context) {
        try {
            movilBD dbHelper = new movilBD(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            db.execSQL("DELETE FROM conductores");
            db.execSQL("DELETE FROM gestor");
            db.execSQL("DELETE FROM viaje");
            db.execSQL("DELETE FROM ruta");
            db.execSQL("DELETE FROM paradas");

            db.close();
            Log.d("DloginFacial", "Tablas temporales limpiadas correctamente.");
        } catch (Exception e) {
            Log.e("DloginFacial", "Error al limpiar las tablas temporales", e);
        }
    }

    public void guardarEvento(Context context, int userId,
                              String mensaje, String tipo, String nivel,
                              double latitud, double longitud) {

        int vehicleId = vehicleIdLocal(context);
        int tripId = tripIdLocal(context);

        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        try {
            movilBD dbHelper = new movilBD(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("mensaje", mensaje);
            values.put("tipo", tipo);
            values.put("nivel", nivel);
            values.put("fecha", fecha);
            values.put("hora", hora);
            values.put("latitud", latitud);
            values.put("longitud", longitud);
            values.put("user_id", userId);
            values.put("vehicle_id", vehicleId);
            values.put("trip_id", tripId);
            values.put("enviado", 0);

            db.insert("eventos", null, values);
            db.close();

            Log.d("DloginFacial", "Evento guardado correctamente");
        } catch (Exception e) {
            Log.e("DloginFacial", "Error al guardar evento", e);
        }
    }



    private int vehicleIdLocal(Context context) {
        try {
            movilBD dbHelper = new movilBD(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                cursor.close();
                db.close();
                return id;
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("DloginFacial", "Error al obtener vehicle_id local", e);
        }
        return -1;
    }

    private int tripIdLocal(Context context) {
        try {
            movilBD dbHelper = new movilBD(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                cursor.close();
                db.close();
                return id;
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("DloginFacial", "Error al obtener trip_id local", e);
        }
        return -1;
    }




}
