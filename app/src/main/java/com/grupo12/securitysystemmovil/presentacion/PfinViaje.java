package com.grupo12.securitysystemmovil.presentacion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.dato.Seguimiento.SeguimientoService;
import com.grupo12.securitysystemmovil.negocio.NfinViaje;

public class PfinViaje extends AppCompatActivity {

    private NfinViaje nfinViaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pfin_viaje);

        // Detener Seguimiento
        Intent intent = new Intent(this, SeguimientoService.class);
        stopService(intent);

        TextView txtEstado = findViewById(R.id.txtEstadoFin);
        nfinViaje = new NfinViaje(this);

        txtEstado.setText("Registrando fin del viaje...");

        nfinViaje.registrarEvento(completado -> {
            runOnUiThread(() -> {
                if (completado) {
                    txtEstado.setText("Viaje finalizado correctamente.");
                    Toast.makeText(this, "Datos limpiados. Redirigiendo...", Toast.LENGTH_LONG).show();

                    Intent intentActivity = new Intent(PfinViaje.this, PshowVehiculo.class);
                    startActivity(intentActivity);
                    finish();
                } else {
                    txtEstado.setText("No se pudo finalizar correctamente.");
                    Toast.makeText(this, "Error al verificar envío de eventos.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}