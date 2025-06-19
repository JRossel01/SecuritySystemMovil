package com.grupo12.securitysystemmovil.dato;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
}
