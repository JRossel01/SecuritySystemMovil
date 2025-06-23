package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.Preview;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.Nsomnolencia;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class Psomnolencia extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private PreviewView previewView;
    private FaceDetector detector;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitud = 0;
    private double longitud = 0;


    private Nsomnolencia nsomnolencia;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psomnolencia);

        previewView = findViewById(R.id.previewView);
        nsomnolencia = new Nsomnolencia(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupFaceDetector();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

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

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, analysis);

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

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    nsomnolencia.evaluarSomnolencia(this, faces);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> imageProxy.close());
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean camaraOK = false;
            boolean ubicacionOK = false;

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

            if (camaraOK && ubicacionOK) {
                iniciarCamara();
            }
        }
    }
}