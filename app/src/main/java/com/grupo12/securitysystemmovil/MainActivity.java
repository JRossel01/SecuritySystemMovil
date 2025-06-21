package com.grupo12.securitysystemmovil;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.grupo12.securitysystemmovil.dato.Evento.DeventoSync;
import com.grupo12.securitysystemmovil.presentacion.PeditVehiculo;
import com.grupo12.securitysystemmovil.presentacion.Pruta;
import com.grupo12.securitysystemmovil.presentacion.PshowVehiculo;


public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        findViewById(R.id.btnSomnolencia).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.grupo12.securitysystemmovil.presentacion.Psomnolencia.class);
            startActivity(intent);
        });

        findViewById(R.id.btnUbicacion).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Pruta.class);
            startActivity(intent);
        });

        findViewById(R.id.btnVelocidad).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.grupo12.securitysystemmovil.presentacion.Pvelocidad.class);
            startActivity(intent);
        });
        findViewById(R.id.btnEditarUnidad).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PeditVehiculo.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMostrarUnidad).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PshowVehiculo.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLoginFacial).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.grupo12.securitysystemmovil.presentacion.PloginFacial.class);
            startActivity(intent);
        });
    }
}