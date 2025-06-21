package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import com.grupo12.securitysystemmovil.MainActivity;
import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.NloginFacial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PloginFacial extends AppCompatActivity {

    private EditText editCi;
    private Button btnVerificar;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private Bitmap referenciaBitmap;
    private ImageAnalysis imageAnalysis;
    private boolean rostroDetectado = false;
    private ProcessCameraProvider cameraProvider;
    private boolean loginCompletado = false;
    private JsonObject usuarioActual;
    private NloginFacial nloginFacial;

    private final ActivityResultLauncher<String[]> permisosLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> iniciarCamara()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plogin_facial);

        editCi = findViewById(R.id.editCi);
        btnVerificar = findViewById(R.id.btnVerificar);
        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        permisosLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        btnVerificar.setOnClickListener(v -> manejarVerificacion());
    }

    private void manejarVerificacion() {
        String ci = editCi.getText().toString().trim();
        if (ci.isEmpty()) {
            Toast.makeText(this, "Ingrese su CI", Toast.LENGTH_SHORT).show();
            return;
        }

        btnVerificar.setEnabled(false);

        nloginFacial = new NloginFacial();
        nloginFacial.procesarUsuarioPorCI(ci, new NloginFacial.OnUsuarioPreparadoListener() {
            @Override
            public void onPreparado(@NonNull JsonObject userInfo, @NonNull Bitmap referencia) {
                referenciaBitmap = referencia;
                usuarioActual = userInfo;
                iniciarCamara();
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PloginFacial.this, error, Toast.LENGTH_SHORT).show());
                btnVerificar.setEnabled(true);
            }
        });
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (referenciaBitmap == null) {
                        image.close();
                        return;
                    }

                    Bitmap bitmap = NloginFacial.convertirImageProxyABitmap(image);
                    image.close();

                    nloginFacial.detectarRostro(bitmap, hayRostro -> {
                        if (hayRostro && !rostroDetectado && !loginCompletado) {
                            rostroDetectado = true;

                            nloginFacial.compararConReferencia(referenciaBitmap, bitmap, coincide -> {
                                runOnUiThread(() -> {
                                    if (coincide) {
                                        loginCompletado = true;
                                        Toast.makeText(PloginFacial.this, "¡Acceso concedido!", Toast.LENGTH_SHORT).show();
                                        Log.d("PloginFacial", "¡Acceso concedido!");
                                        cameraProvider.unbindAll();
                                        cameraExecutor.shutdownNow(); // detener el executor

                                        nloginFacial.guardarLocal(
                                                PloginFacial.this,
                                                usuarioActual,
                                                referenciaBitmap
                                        );
                                    } else {
                                        Toast.makeText(PloginFacial.this, "Rostro no coincide", Toast.LENGTH_SHORT).show();
                                        rostroDetectado = false;
                                    }
                                });
                            });
                        }

                    });
                });


                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (Exception e) {
                Log.e("PloginFacial", "Error iniciando cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}