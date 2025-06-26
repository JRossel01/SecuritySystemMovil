package com.grupo12.securitysystemmovil.dato.Seguimiento;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.Api.ApiClient;
import com.grupo12.securitysystemmovil.dato.Api.ApiService;
import com.grupo12.securitysystemmovil.movilBD.movilBD;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeguimientoService extends Service {
    private FusedLocationProviderClient locationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        iniciarSeguimiento();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // El servicio se mantiene en ejecución incluso si la activity es cerrada
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Detener el seguimiento cuando el servicio se destruye
        locationClient.removeLocationUpdates(locationCallback);
    }

    private void iniciarSeguimiento() {
        LocationRequest request = new LocationRequest.Builder(1000)
                .setMinUpdateIntervalMillis(1000)  // Intervalo de 500 ms entre actualizaciones
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("UbicacionServicio", "Permiso de ubicación no concedido");
            return;
        }
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@Nullable LocationResult locationResult) {
            if (locationResult != null) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Obtener la velocidad en km/h (de metros por segundo a kilómetros por hora)
                    int velocidadKmh = (int) (location.getSpeed() * 3.6f);

                    // Obtener user_id, vehicle_id, trip_id desde la BD
                    int userId = obtenerUserId();
                    int vehicleId = obtenerVehicleId();
                    int tripId = obtenerTripId();

                    // Obtener la fecha y hora del dispositivo
                    String fecha = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                    String hora = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

                    // Llamar al método para enviar los datos al backend
                    enviarUbicacion(location.getLatitude(), location.getLongitude(), velocidadKmh, fecha, hora, userId, vehicleId, tripId);
                }
            }
        }
    };


    private int obtenerUserId() {
        SQLiteDatabase db = new movilBD(this).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM conductores WHERE activo = 1 LIMIT 1", null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0); // Obtener el ID del conductor activo
        }
        cursor.close();
        db.close();
        return userId;
    }

    private int obtenerVehicleId() {
        SQLiteDatabase db = new movilBD(this).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM vehiculo LIMIT 1", null);
        int vehicleId = -1;
        if (cursor.moveToFirst()) {
            vehicleId = cursor.getInt(0); // Obtener el ID del vehículo
        }
        cursor.close();
        db.close();
        return vehicleId;
    }

    private int obtenerTripId() {
        SQLiteDatabase db = new movilBD(this).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM viaje LIMIT 1", null);
        int tripId = -1;
        if (cursor.moveToFirst()) {
            tripId = cursor.getInt(0); // Obtener el ID del viaje
        }
        cursor.close();
        db.close();
        return tripId;
    }

    private void enviarUbicacion(double latitud, double longitud, float velocidad, String fecha, String hora, int userId, int vehicleId, int tripId) {
        GpsLocationRequest gpsData = new GpsLocationRequest(latitud, longitud, velocidad, userId, vehicleId, tripId, fecha, hora);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<JsonObject> call = apiService.enviarGpsLocation(gpsData);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.i("UbicacionServicio", "Ubicación enviada correctamente.");
                } else {
                    Log.e("UbicacionServicio", "Error al enviar ubicación.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("UbicacionServicio", "Error de red al enviar ubicación.", t);
            }
        });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Este servicio no tiene que ser vinculado a ninguna actividad
        return null;
    }
}
