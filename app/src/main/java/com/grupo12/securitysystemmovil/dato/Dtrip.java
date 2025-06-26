package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;
import com.grupo12.securitysystemmovil.movilBD.movilBD;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Dtrip {

    private Context context;
    private ApiService apiService;
    private SQLiteDatabase db;


    public Dtrip(Context context) {
        this.context = context;
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.db = new movilBD(context).getWritableDatabase();
    }

    public interface TripCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void guardarTrip(int userId, int idConductorActual, TripCallback callback) {
        Call<JsonObject> call = apiService.getTripActivo(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("success").getAsBoolean()) {
                    try {
                        procesarRespuestaTrip(response.body().getAsJsonObject("trip"), idConductorActual);
                        callback.onSuccess();
                    } catch (Exception e) {
                        Log.e("Dtrip", "Error procesando la respuesta", e);
                        callback.onFailure("Error procesando la respuesta");
                    }
                } else {
                    Log.e("Dtrip", "Respuesta fallida del servidor");
                    callback.onFailure("Respuesta fallida del servidor");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("Dtrip", "Fallo en la llamada HTTP", t);
                callback.onFailure("Error de red");
            }
        });
    }

    private void procesarRespuestaTrip(JsonObject trip, int idConductorActual) {
        guardarDatosViaje(trip);
        if (trip.has("route")) {
            JsonObject route = trip.getAsJsonObject("route");
            guardarDatosRuta(route);
            guardarParadas(route.getAsJsonArray("stops"));
        }
        if (trip.has("conductores")) {
            guardarConductores(trip.getAsJsonArray("conductores"), idConductorActual);
        }
    }

    private void guardarDatosViaje(JsonObject trip) {
        int tripId = trip.get("id").getAsInt();
        String fechaInicio = trip.get("fecha_inicio").getAsString();
        String horaInicio = trip.get("hora_inicio").getAsString();
        int vehicleId = trip.get("vehicle_id").getAsInt();

        ContentValues valuesTrip = new ContentValues();
        valuesTrip.put("id", tripId);
        valuesTrip.put("fecha_inicio", fechaInicio);
        valuesTrip.put("hora_inicio", horaInicio);
        valuesTrip.put("vehicle_id", vehicleId);
        db.insert("viaje", null, valuesTrip);
    }

    private void guardarDatosRuta(JsonObject route) {
        ContentValues valuesRuta = new ContentValues();
        valuesRuta.put("id", route.get("id").getAsInt());
        valuesRuta.put("nombre", route.get("nombre").getAsString());
        valuesRuta.put("origen_lat", route.get("origen_lat").getAsString());
        valuesRuta.put("origen_lng", route.get("origen_lng").getAsString());
        valuesRuta.put("destino_lat", route.get("destino_lat").getAsString());
        valuesRuta.put("destino_lng", route.get("destino_lng").getAsString());
        db.insert("ruta", null, valuesRuta);
    }

    private void guardarParadas(JsonArray stops) {
        if (stops == null) return;
        for (int i = 0; i < stops.size(); i++) {
            JsonObject stop = stops.get(i).getAsJsonObject();
            ContentValues valuesParada = new ContentValues();
            valuesParada.put("nombre", stop.get("nombre").getAsString());
            valuesParada.put("latitud", stop.get("latitud").getAsString());
            valuesParada.put("longitud", stop.get("longitud").getAsString());

            if (stop.has("posicion") && !stop.get("posicion").isJsonNull()) {
                valuesParada.put("posicion", stop.get("posicion").getAsInt());
            } else {
                valuesParada.putNull("posicion");
            }

            db.insert("paradas", null, valuesParada);
        }
    }

    private void guardarConductores(JsonArray conductores, int idConductorActual) {
        for (int i = 0; i < conductores.size(); i++) {
            JsonObject conductor = conductores.get(i).getAsJsonObject();
            int id = conductor.get("id").getAsInt();
            if (id == idConductorActual) continue; // Saltar al conductor que ya está guardado

            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("ci", conductor.get("ci").getAsString());
            values.put("nombre", conductor.get("nombre").getAsString());
            values.put("apellido", conductor.get("apellido").getAsString());
            values.put("rol", conductor.get("rol").getAsInt());
            values.put("ruta_imagen", conductor.get("foto_url").getAsString());
            values.put("activo", 0);

            db.insert("conductores", null, values);
        }
    }


    public int obtenerVehicleId() {
        int vehicleId = -1;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = new movilBD(context).getReadableDatabase();
            cursor = db.rawQuery("SELECT vehicle_id FROM viaje LIMIT 1", null);
            if (cursor.moveToFirst()) {
                vehicleId = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("Dtrip", "Error al obtener vehicle_id del viaje", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return vehicleId;
    }

}
