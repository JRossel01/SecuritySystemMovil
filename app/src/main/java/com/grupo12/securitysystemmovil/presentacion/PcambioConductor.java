package com.grupo12.securitysystemmovil.presentacion;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.grupo12.securitysystemmovil.MainActivity;
import com.grupo12.securitysystemmovil.R;

import com.grupo12.securitysystemmovil.dato.Dconductor;
import com.grupo12.securitysystemmovil.negocio.NcambioConductor;

import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.List;


public class PcambioConductor extends AppCompatActivity {

    private Spinner spinnerConductores;
    private Button btnCambiarConductor;
    private NcambioConductor ncambio;
    private List<Dconductor> listaConductores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcambio_conductor);

        spinnerConductores = findViewById(R.id.spinnerConductores);
        btnCambiarConductor = findViewById(R.id.btnCambiarConductor);


        ncambio = new NcambioConductor(getApplicationContext());

        listaConductores = ncambio.obtenerConductoresInactivos(); // Interna
        List<String> nombres = ncambio.obtenerNombresConductores(); // Para el Spinner

        if (nombres.isEmpty()) {
            Toast.makeText(this, "No hay conductores inactivos registrados", Toast.LENGTH_LONG).show();
            btnCambiarConductor.setEnabled(false);
            btnCambiarConductor.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerConductores.setAdapter(adapter);
        }

        btnCambiarConductor.setOnClickListener(v -> {
            int pos = spinnerConductores.getSelectedItemPosition();
            if (pos != Spinner.INVALID_POSITION && !listaConductores.isEmpty()) {
                int idSeleccionado = listaConductores.get(pos).getId();
                boolean exito = ncambio.activarConductor(idSeleccionado);
                if (exito) {
                    Toast.makeText(this, "Conductor cambiado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al cambiar de conductor", Toast.LENGTH_SHORT).show();
                }
            }
        });

        verificarUbicacion();

    }

    private void verificarUbicacion() {
        ncambio.verificarUbicacion(this, cerca -> {
            if (cerca) {
                btnCambiarConductor.setEnabled(true);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde));
                spinnerConductores.setVisibility(View.VISIBLE);
                actualizarSpinner();
            } else {
                btnCambiarConductor.setEnabled(false);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gris));
                spinnerConductores.setVisibility(View.GONE);
            }
        });
    }

    private void actualizarSpinner() {
        listaConductores = ncambio.obtenerConductoresInactivos();
        List<String> nombres = ncambio.obtenerNombresConductores();

        if (nombres.isEmpty()) {
            Toast.makeText(this, "No hay conductores inactivos registrados", Toast.LENGTH_LONG).show();
            btnCambiarConductor.setEnabled(false);
            btnCambiarConductor.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerConductores.setAdapter(adapter);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ncambio.detenerMonitoreoUbicacion();
    }

}