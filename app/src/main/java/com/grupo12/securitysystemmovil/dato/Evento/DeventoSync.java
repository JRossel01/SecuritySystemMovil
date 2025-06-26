package com.grupo12.securitysystemmovil.dato.Evento;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeventoSync {
    private Context context;
    private SQLiteDatabase db;
    private Handler handler = new Handler();
    private long intervaloMillis = 3000; // Revisión cada 3 segundos
    private ApiService apiService;

    public DeventoSync(Context context) {
        this.context = context;
        this.db = new movilBD(context).getWritableDatabase();
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void iniciar() {
        handler.postDelayed(envioRunnable, intervaloMillis);
    }

    public void detener() {
        handler.removeCallbacks(envioRunnable);
    }

    public void setIntervaloMillis(long millis) {
        this.intervaloMillis = millis;
    }

    private Runnable envioRunnable = new Runnable() {
        @Override
        public void run() {
            Map<DeventoRequest, Integer> eventosPendientes = obtenerEventosNoEnviados();
            for (Map.Entry<DeventoRequest, Integer> entry : eventosPendientes.entrySet()) {
                DeventoRequest evento = entry.getKey();
                int id = entry.getValue();

                Call<JsonObject> call = apiService.enviarEvento(evento);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            marcarComoEnviado(id);
                            Log.i("Sync", "Evento enviado y marcado: " + id);
                        } else {
                            Log.e("Sync", "Respuesta fallida al enviar evento: " + id);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Log.e("Sync", "Error al enviar evento: " + id, t);
                    }
                });
            }

            handler.postDelayed(this, intervaloMillis);
        }
    };

    private Map<DeventoRequest, Integer> obtenerEventosNoEnviados() {
        Map<DeventoRequest, Integer> mapa = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT * FROM eventos WHERE enviado = 0", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));

                DeventoRequest evento = new DeventoRequest(
                        cursor.getString(cursor.getColumnIndexOrThrow("mensaje")),
                        cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nivel")),
                        cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                        cursor.getString(cursor.getColumnIndexOrThrow("hora")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                        obtenerIntSeguro(cursor, "vehicle_id"),
                        obtenerIntSeguro(cursor, "trip_id")
                );

                mapa.put(evento, id);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return mapa;
    }

    private void marcarComoEnviado(int id) {
        db.execSQL("UPDATE eventos SET enviado = 1 WHERE id = ?", new Object[]{id});
    }


    private Integer obtenerIntSeguro(Cursor cursor, String columna) {
        int index = cursor.getColumnIndexOrThrow(columna);
        if (cursor.isNull(index)) {
            return null;
        }
        int valor = cursor.getInt(index);
        return valor == -1 ? null : valor;
    }
}
