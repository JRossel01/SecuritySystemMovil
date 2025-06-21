package com.grupo12.securitysystemmovil.presentacion;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.dato.Dvehiculo;
import com.grupo12.securitysystemmovil.negocio.Nvehiculo;

public class PshowVehiculo extends AppCompatActivity {

    //    private DeventoSync devSync;

    private TextView txtNombreVehiculo, txtPlacaVehiculo;
    private Nvehiculo nvehiculo;

    private GestureDetector gestureDetector;

    private static final int REQUEST_ALL_PERMISSIONS = 2001;
    private final String[] PERMISOS_REQUERIDOS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pshow_vehiculo);

//        devSync = new DeventoSync(getApplicationContext());
//        devSync.iniciar();

        txtNombreVehiculo = findViewById(R.id.txtNombreVehiculo);
        txtPlacaVehiculo = findViewById(R.id.txtPlacaVehiculo);
        nvehiculo = new Nvehiculo(this);

        pedirPermisos();

//        Intent intent = new Intent(this, SeguimientoService.class);
//        startService(intent);

        mostrarDatosVehiculo();

        inicializarGestos();

    }

    private void mostrarDatosVehiculo() {
        Dvehiculo.VehiculoData vehiculo = nvehiculo.obtenerVehiculoLocal();

        if (vehiculo == null || vehiculo.id == -1) {
            Toast.makeText(this, "No hay vehículo guardado", Toast.LENGTH_SHORT).show();
        } else {
            txtNombreVehiculo.setText(vehiculo.nombre);
            txtPlacaVehiculo.setText(vehiculo.placa);
        }
    }

    private void pedirPermisos() {
        boolean faltaPermiso = false;
        for (String permiso : PERMISOS_REQUERIDOS) {
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                faltaPermiso = true;
                break;
            }
        }

        if (faltaPermiso) {
            ActivityCompat.requestPermissions(this, PERMISOS_REQUERIDOS, REQUEST_ALL_PERMISSIONS);
        }
    }

    private void inicializarGestos() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > SWIPE_THRESHOLD || Math.abs(diffY) > SWIPE_THRESHOLD) {
                    irPloginFacial(); // ✅ Se activa con cualquier deslizamiento
                    return true;
                }
                return false;
            }
        });
    }


    private void irPloginFacial() {
        Intent intent = new Intent(this, PloginFacial.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

}