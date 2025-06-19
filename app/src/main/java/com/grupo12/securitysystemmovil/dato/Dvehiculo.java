package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Dvehiculo {
    private final movilBD dbHelper;

    public Dvehiculo(Context context) {
        dbHelper = new movilBD(context);
    }

    public interface OnVehiculosObtenidosListener {
        void onExito(List<JsonObject> listaVehiculos);
        void onError(String mensaje);
    }

    public void obtenerVehiculos(OnVehiculosObtenidosListener listener) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<List<JsonObject>> call = apiService.getVehiculos();

        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onExito(response.body());
                } else {
                    listener.onError("Error al obtener vehículos: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e("Dvehiculo", "Fallo en la conexión", t);
                listener.onError("Fallo en la conexión: " + t.getMessage());
            }
        });
    }

    public void guardarVehiculo(int id, String nombre, String placa, int velocidadMaxima) {
        try {

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            db.delete("vehiculo", null, null);

            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("nombre", nombre);
            values.put("placa", placa);
            values.put("velocidad_maxima", velocidadMaxima);

            db.insert("vehiculo", null, values);
            db.close();
        } catch (Exception e) {
            Log.e("Dvehiculo", "Error al guardar vehículo en SQLite: " + e.getMessage());
        }
    }

    public static class VehiculoData {
        public int id;
        public String nombre;
        public String placa;
        public int velocidadMaxima;
    }

    public VehiculoData obtenerVehiculoLocal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        VehiculoData vehiculo = null;

        try (Cursor cursor = db.rawQuery("SELECT id, nombre, placa, velocidad_maxima FROM vehiculo LIMIT 1", null)) {
            if (cursor.moveToFirst()) {
                vehiculo = new VehiculoData();
                vehiculo.id = cursor.getInt(0);
                vehiculo.nombre = cursor.getString(1);
                vehiculo.placa = cursor.getString(2);
                vehiculo.velocidadMaxima = cursor.getInt(3);
            }
        } catch (Exception e) {
            Log.e("Dvehiculo", "Error al leer vehículo desde SQLite", e);
        } finally {
            db.close();
        }

        return vehiculo;
    }

}
