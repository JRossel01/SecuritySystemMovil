package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.MainActivity;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.NpreViaje;

public class PpreViaje extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private TextView txtRuta, txtVehiculo, txtPlaca, txtConductores, txtParadas;
    private Button btnIniciarViaje;

    private FusedLocationProviderClient fusedLocationClient;
    private NpreViaje npreViaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppre_viaje);

        txtRuta = findViewById(R.id.txtRuta);
        txtVehiculo = findViewById(R.id.txtVehiculo);
        txtPlaca = findViewById(R.id.txtPlaca);
        btnIniciarViaje = findViewById(R.id.btnIniciarViaje);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        npreViaje = new NpreViaje(this);

        botonVolver();

        verificarPermisos();

        mostrarDatos();

    }

    private void mostrarDatos() {
        txtRuta.setText(npreViaje.getNombreRuta());
        txtVehiculo.setText(npreViaje.getNombreVehiculo());
        txtPlaca.setText(npreViaje.getPlacaVehiculo());

        LinearLayout layoutConductores = findViewById(R.id.layoutConductores);
        for (String nombreCompleto : npreViaje.getConductores()) {
            TextView tv = new TextView(this);
            tv.setText(nombreCompleto);
            tv.setTextColor(Color.parseColor("#3C3C3C"));
            tv.setTextSize(20);
            tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutConductores.addView(tv);
        }


        LinearLayout layoutParadas = findViewById(R.id.layoutParadas);
        layoutParadas.removeAllViews(); // por si se recarga

        if (npreViaje.getParadas().isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No hay paradas");
            tv.setTextColor(Color.parseColor("#3C3C3C"));
            tv.setTextSize(20);
            tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutParadas.addView(tv);
        } else {
            for (String nombreParada : npreViaje.getParadas()) {
                TextView tv = new TextView(this);
                tv.setText(nombreParada);
                tv.setTextColor(Color.parseColor("#3C3C3C"));
                tv.setTextSize(20);
                tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                layoutParadas.addView(tv);
            }
        }


        btnIniciarViaje.setEnabled(false);
        btnIniciarViaje.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    private void verificarPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            verificarUbicacion();
        }
    }

    private void verificarUbicacion() {
        npreViaje.verificarUbicacion((estaCerca) -> {
            if (estaCerca) {
                runOnUiThread(() -> {
                    btnIniciarViaje.setEnabled(true);
                    btnIniciarViaje.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                    btnIniciarViaje.setText("Iniciar Viaje");
                    btnIniciarViaje.setOnClickListener(v -> {
                        npreViaje.registrarEvento(PpreViaje.this);
                        Intent intent = new Intent(PpreViaje.this, MainActivity.class);
                        startActivity(intent);
                    });
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            verificarUbicacion();
        } else {
            btnIniciarViaje.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            btnIniciarViaje.setText("Iniciar Viaje");
        }
    }

    private void botonVolver() {
        Button btnEmergencia = findViewById(R.id.btnVolver);
        btnEmergencia.setOnClickListener(v -> {
            Intent intent = new Intent(PpreViaje.this, PshowVehiculo.class);
            startActivity(intent);
            finish();
        });
    }

}