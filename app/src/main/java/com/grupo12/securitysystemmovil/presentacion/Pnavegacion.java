package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.maps.android.SphericalUtil;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.dato.Dconductor;
import com.grupo12.securitysystemmovil.negocio.NcambioConductor;
import com.grupo12.securitysystemmovil.negocio.Nruta;
import com.grupo12.securitysystemmovil.negocio.Nsomnolencia;
import com.grupo12.securitysystemmovil.utils.MapaIconos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class Pnavegacion extends AppCompatActivity {

    //Somnolencia
    private Nsomnolencia nsomnolencia;
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private FaceDetector detector;


    //Cambio Conductor
    private NcambioConductor ncambio;
    private Spinner spinnerConductores;
    private Button btnCambiarConductor;
    private List<Dconductor> listaConductores;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pnavegacion);

        //Somnolencia
        nsomnolencia = new Nsomnolencia(this);
        // Initialize the face detector
        setupFaceDetector();


        //Cambio Conductor
        btnCambiarConductor = findViewById(R.id.btnCambiarConductor);
        spinnerConductores = findViewById(R.id.spinnerConductores);



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

        //Cambio Conductor
        btnCambiarConductor.setOnClickListener(v -> {
            int pos = spinnerConductores.getSelectedItemPosition();
            if (pos != Spinner.INVALID_POSITION && !ncambio.obtenerConductoresInactivos().isEmpty()) {
                // Asegurarnos de que estamos obteniendo el conductor correcto
                Dconductor conductorSeleccionado = ncambio.obtenerConductoresInactivos().get(pos);
                if (conductorSeleccionado != null) {
                    int idSeleccionado = conductorSeleccionado.getId();
                    boolean exito = ncambio.activarConductor(idSeleccionado);
                    if (exito) {
                        Toast.makeText(this, "Conductor cambiado correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error al cambiar de conductor", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Selecciona un conductor de la lista", Toast.LENGTH_SHORT).show();
            }
        });

        verificarUbicacion();
    }

    //Somnolencia
    private void setupFaceDetector() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();

        detector = FaceDetection.getClient(options);
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                Executor executor = ContextCompat.getMainExecutor(this);
                analysis.setAnalyzer(executor, this::procesarImagen);

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                // Removed PreviewView to hide camera
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, analysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void procesarImagen(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Permiso", "Permiso de ubicación no otorgado. Evento no registrado.");
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // Now detector is initialized properly
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    nsomnolencia.evaluarSomnolencia(this, faces);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> imageProxy.close());
    }

    //Cambio Conductor
    private void verificarUbicacion() {
        ncambio.verificarUbicacion(this, cerca -> {
            if (cerca) {
                btnCambiarConductor.setEnabled(true);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde));
                spinnerConductores.setVisibility(View.VISIBLE);

                List<String> nombres = ncambio.obtenerNombresConductores();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerConductores.setAdapter(adapter);
            } else {
                btnCambiarConductor.setEnabled(false);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gris));
                spinnerConductores.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean camaraOK = false;
            boolean ubicacionOK = false;

            // Iterar a través de los permisos y verificar si han sido concedidos
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    camaraOK = true;
                }

                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    ubicacionOK = true;
                }
            }

            // Si ambos permisos fueron concedidos, proceder
            if (camaraOK && ubicacionOK) {
                iniciarCamara();
            } else {
                // Si el permiso de cámara es denegado, mostrar mensaje
                if (!camaraOK) {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
                // Si el permiso de ubicación es denegado, puedes agregar un mensaje adicional si lo deseas
                if (!ubicacionOK) {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ncambio.detenerMonitoreoUbicacion();
    }


}