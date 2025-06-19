package com.grupo12.securitysystemmovil.presentacion;

import android.os.Bundle;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.dato.Dvehiculo;
import com.grupo12.securitysystemmovil.negocio.Nvehiculo;

public class PshowVehiculo extends AppCompatActivity {

    private TextView txtNombreVehiculo, txtPlacaVehiculo;
    private Nvehiculo nvehiculo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pshow_vehiculo);

        txtNombreVehiculo = findViewById(R.id.txtNombreVehiculo);
        txtPlacaVehiculo = findViewById(R.id.txtPlacaVehiculo);
        nvehiculo = new Nvehiculo(this);

        mostrarDatosVehiculo();

    }

    private void mostrarDatosVehiculo() {
        Dvehiculo.VehiculoData vehiculo = nvehiculo.obtenerVehiculoLocal();

        if (vehiculo != null) {
            txtNombreVehiculo.setText(vehiculo.nombre);
            txtPlacaVehiculo.setText(vehiculo.placa);
        } else {
            Toast.makeText(this, "No hay vehículo guardado", Toast.LENGTH_SHORT).show();
        }
    }

}