package com.grupo12.securitysystemmovil.presentacion;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.Nvehiculo;

import java.util.ArrayList;
import java.util.List;

public class PeditVehiculo extends AppCompatActivity {

    private Spinner spinnerVehiculos;
    private Button btnGuardarVehiculo;
    private Nvehiculo nvehiculo;
    private List<JsonObject> listaVehiculos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedit_vehiculo);

        spinnerVehiculos = findViewById(R.id.spinnerVehiculos);
        btnGuardarVehiculo = findViewById(R.id.btnGuardarVehiculo);

        nvehiculo = new Nvehiculo(this);

        cargarVehiculos();

        btnGuardarVehiculo.setOnClickListener(v -> guardarVehiculo());
    }

    private void cargarVehiculos() {
        nvehiculo.obtenerVehiculos(new Nvehiculo.OnVehiculosRecibidosListener() {
            @Override
            public void onExito(List<JsonObject> vehiculos) {
                listaVehiculos = vehiculos;
                List<String> nombres = new ArrayList<>();
                for (JsonObject v : vehiculos) {
                    String nombre = v.get("nombre").getAsString();
                    String placa = v.get("placa").getAsString();
                    nombres.add(nombre + " - " + placa);
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            PeditVehiculo.this,
                            android.R.layout.simple_spinner_item,
                            nombres
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerVehiculos.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> Toast.makeText(PeditVehiculo.this, mensaje, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void guardarVehiculo() {
        int pos = spinnerVehiculos.getSelectedItemPosition();
        if (pos < 0 || pos >= listaVehiculos.size()) {
            Toast.makeText(this, "Seleccione un vehículo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject vehiculoSeleccionado = listaVehiculos.get(pos);
        nvehiculo.guardarVehiculo(this, vehiculoSeleccionado); // Este método lo implementamos ahora

        Toast.makeText(this, "Vehículo guardado correctamente", Toast.LENGTH_SHORT).show();
        finish();
    }
}