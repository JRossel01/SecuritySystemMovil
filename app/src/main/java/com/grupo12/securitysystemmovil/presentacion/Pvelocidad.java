package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.Nvelocidad;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class Pvelocidad extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1002;
    private static final float UMBRAL_KMH = 20f; // se podrá ajustar más adelante

    private FusedLocationProviderClient locationClient;
    private TextView tvVelocidad;
    private Nvelocidad nvelocidad;
    private TextView tvAlertaVelocidad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pvelocidad);

        tvVelocidad = findViewById(R.id.tvVelocidad);
        nvelocidad = new Nvelocidad();
        nvelocidad.setContextoBD(this);

        tvAlertaVelocidad = findViewById(R.id.tvAlertaVelocidad);
        nvelocidad.setTextViewAlerta(tvAlertaVelocidad); // se lo pasamos a la lógica

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            iniciarMedicion();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void iniciarMedicion() {
        LocationRequest request = new LocationRequest.Builder(1000)
                .setMinUpdateIntervalMillis(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        locationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    float velocidadKmh = location.getSpeed() * 3.6f;
                    tvVelocidad.setText(String.format("Velocidad: %.1f km/h", velocidadKmh));

                    nvelocidad.procesarVelocidad(location, UMBRAL_KMH);
                }
            }
        }, Looper.getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarMedicion();
        } else {
            Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
        }
    }

}