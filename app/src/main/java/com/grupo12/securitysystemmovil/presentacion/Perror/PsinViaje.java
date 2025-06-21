package com.grupo12.securitysystemmovil.presentacion.Perror;

import android.os.Bundle;

import android.content.Intent;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;


import com.grupo12.securitysystemmovil.MainActivity;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.presentacion.PshowVehiculo;

public class PsinViaje extends AppCompatActivity {

    private static final int DURACION_MENSAJE_MS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psin_viaje);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(PsinViaje.this, PshowVehiculo.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, DURACION_MENSAJE_MS);
    }
}