package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.dato.Evento.DeventoSync;
import com.grupo12.securitysystemmovil.negocio.NloginFacial;
import com.grupo12.securitysystemmovil.negocio.loginFacial.NfaceRecognition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PloginFacial extends AppCompatActivity {

    //        Iniciar Eventos al backend
    private DeventoSync devSync;

    private EditText editCi;
    private Button btnVerificar, btnCapturar;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private Bitmap referenciaBitmap;
    private ImageAnalysis imageAnalysis;
    private boolean rostroDetectado = false;
    private ProcessCameraProvider cameraProvider;
    private boolean loginCompletado = false;
    private JsonObject usuarioActual;
    private String ciActual;
    private ImageProxy lastImageProxy;
    private NloginFacial nloginFacial;

    private final ActivityResultLauncher<String[]> permisosLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // Verificamos que todos los permisos fueron concedidos
                boolean permisosOk = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.CAMERA, false)) &&
                        Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) &&
                        Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));

                if (!permisosOk) {
                    Toast.makeText(PloginFacial.this, "Permisos requeridos no concedidos", Toast.LENGTH_SHORT).show();
                    btnVerificar.setEnabled(true);
                    return;
                }

                // Ahora sí continuamos con el procesamiento facial
                nloginFacial = new NloginFacial();
                nloginFacial.procesarUsuarioPorCI(PloginFacial.this, ciActual, new NloginFacial.OnUsuarioPreparadoListener() {
                    @Override
                    public void onPreparado(@NonNull JsonObject userInfo, @NonNull Bitmap referencia) {
                        runOnUiThread(() -> {
                            referenciaBitmap = referencia;
                            usuarioActual = userInfo;
                            nloginFacial.setReferenciaBitmap(referencia);
                            iniciarCamara();
                            btnCapturar.setEnabled(true); // ← No lo olvides, también va aquí
                        });
                    }


                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(PloginFacial.this, error, Toast.LENGTH_SHORT).show());
                        btnVerificar.setEnabled(true);
                    }
                });
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plogin_facial);

//        Iniciar Eventos al backend
        devSync = new DeventoSync(getApplicationContext());
        devSync.iniciar();

        editCi = findViewById(R.id.editCi);
        btnVerificar = findViewById(R.id.btnVerificar);
        btnCapturar = findViewById(R.id.btnCapturar);
        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        btnVerificar.setOnClickListener(v -> manejarVerificacion());

        btnCapturar.setOnClickListener(v -> {
            if (lastImageProxy == null) {
                Toast.makeText(this, "Esperando imagen de cámara...", Toast.LENGTH_SHORT).show();
                return;
            }
            btnCapturar.setEnabled(false);

            ImageProxy image = lastImageProxy;
            lastImageProxy = null;

            Bitmap bitmap = NloginFacial.convertirImageProxyABitmap(image);
            if (bitmap == null) {
                Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show();
                btnCapturar.setEnabled(true);
                image.close();
                return;
            }

            nloginFacial.getRecognizer(this).procesarImagenCamara(bitmap, this, new NfaceRecognition.OnEmbeddingsGeneradosListener() {
                @Override
                public void onEmbeddingsGenerados(float[] embeddingsActuales) {
                    boolean coincide = nloginFacial.compararEmbeddings(ciActual, embeddingsActuales);
                    runOnUiThread(() -> {
                        if (coincide) {
                            Toast.makeText(PloginFacial.this, "¡Acceso concedido!", Toast.LENGTH_SHORT).show();
                            cameraProvider.unbindAll();
                            cameraExecutor.shutdownNow();
                            nloginFacial.guardarLocal(PloginFacial.this, usuarioActual, referenciaBitmap);
                        } else {
                            Toast.makeText(PloginFacial.this, "Rostro no coincide", Toast.LENGTH_SHORT).show();
                            btnCapturar.setEnabled(true);
                        }
                        image.close();
                    });
                }

                @Override
                public void onError(String mensaje) {
                    runOnUiThread(() -> {
                        Toast.makeText(PloginFacial.this, mensaje, Toast.LENGTH_SHORT).show();
                        btnCapturar.setEnabled(true);
                        image.close();
                    });
                }
            });
        });

    }

    private void manejarVerificacion() {
        String ci = editCi.getText().toString().trim();
        if (ci.isEmpty()) {
            Toast.makeText(this, "Ingrese su CI", Toast.LENGTH_SHORT).show();
            return;
        }

        ciActual = ci;
        btnVerificar.setEnabled(false);

        permisosLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (lastImageProxy != null) lastImageProxy.close();
                    lastImageProxy = image;
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("NloginFacial", "Error iniciando cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}