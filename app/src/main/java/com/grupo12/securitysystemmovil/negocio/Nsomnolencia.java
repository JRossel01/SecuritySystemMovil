package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.Manifest;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.grupo12.securitysystemmovil.dato.Dsomnolencia;

import java.util.List;

public class Nsomnolencia {

    private Dsomnolencia dsomnolencia;
    private int ojosCerradosFrames = 0;
    private static final int OJOS_UMBRAL_FRAMES = 10; // Ajustable
    private boolean ojosCerradosDetectado = false;

    private int bocaAbiertaFrames = 0;
    private static final int BOCA_UMBRAL_FRAMES = 10;
    private static final float BOCA_UMBRAL_NORMALIZADO = 0.05f;
    private boolean bostezoRegistrado = false;

    private int microsuenoFrames = 0;
    private static final float INCLINACION_UMBRAL = 15f;
    private static final int MICROSUENO_UMBRAL_FRAMES = 15;
    private boolean microsuenoDetectado = false;

    private Float ultimoHeadEulerX = null;
    private boolean detectandoCabeceo = false;
    private boolean alarmaActiva = false;
    private double latitudActual;
    private double longitudActual;


    public Nsomnolencia(Context context) {
        dsomnolencia = new Dsomnolencia(context);
    }


    public void evaluarSomnolencia(Context context, List<Face> faces) {
        if (faces == null || faces.isEmpty()) {
            Log.d("Rostro", "Rostro no detectado - asegúrese de que la cámara está correctamente posicionada.");
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Nsomnolencia", "Permiso de ubicación no otorgado.");
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitudActual = location.getLatitude();
                        longitudActual = location.getLongitude();

                        Face face = faces.get(0);
                        detectarMicrosueno(face);
                        detectarBostezo(face);
                        detectarCabeceo(face);
                    } else {
                        Log.w("Nsomnolencia", "No se pudo obtener ubicación en tiempo real.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Nsomnolencia", "Error al obtener ubicación: " + e.getMessage());
                });
    }


    private void detectarMicrosueno(Face face) {
        Float ojoIzq = face.getLeftEyeOpenProbability();
        Float ojoDer = face.getRightEyeOpenProbability();
        float headEulerX = face.getHeadEulerAngleX();

        if (ojoIzq != null && ojoDer != null) {
            boolean ojosCerrados = ojoIzq < 0.3 && ojoDer < 0.3;
            boolean cabezaInclinada = headEulerX < -INCLINACION_UMBRAL;

            // ----- MICROSUENO -----
            if (ojosCerrados && cabezaInclinada) {
                microsuenoFrames++;
                ojosCerradosFrames = 0;
                Log.d("Somnolencia", "Microsueno - Frame #" + microsuenoFrames);

                if (!microsuenoDetectado && microsuenoFrames >= MICROSUENO_UMBRAL_FRAMES) {
                    String mensaje = "Microsueño detectado";
                    dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Critico", latitudActual, longitudActual);

                    activarAlarma();
                    Log.d("Somnolencia", mensaje);
                    microsuenoDetectado = true;
                }
                // ----- SOLO OJOS CERRADOS -----
            } else if (ojosCerrados) {
                ojosCerradosFrames++;
                microsuenoFrames = 0;
                Log.d("Somnolencia", "Ojos cerrados - Frame #" + ojosCerradosFrames);

                if (!ojosCerradosDetectado && ojosCerradosFrames >= OJOS_UMBRAL_FRAMES) {
                    String mensaje = "Ojos cerrados detectado";
                    dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Advertencia", latitudActual, longitudActual);
                    activarAlarma();
                    Log.d("Somnolencia", mensaje);
                    ojosCerradosDetectado = true;
                }

            } else {
                // Microsueño finaliza
                if (microsuenoDetectado) {
                    float segundos = microsuenoFrames / 5.0f;
                    String mensaje = "Microsueño — duración: " + segundos + " segundos";
                    dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Critico", latitudActual, longitudActual);
                    Log.d("Somnolencia", mensaje);
                }

                // Ojos cerrados finaliza
                if (ojosCerradosDetectado) {
                    float segundos = ojosCerradosFrames / 5.0f;
                    String mensaje = "Ojos cerrados — duración: " + segundos + " segundos";
                    dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Advertencia", latitudActual, longitudActual);
                    Log.d("Somnolencia", mensaje);
                }

                // Reiniciar todo
                detenerAlarma();
                microsuenoFrames = 0;
                ojosCerradosFrames = 0;
                microsuenoDetectado = false;
                ojosCerradosDetectado = false;
            }
        }
    }


    private void detectarBostezo(Face face) {
        List<PointF> labioSuperior = face.getContour(FaceContour.UPPER_LIP_BOTTOM) != null ?
                face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints() : null;
        List<PointF> labioInferior = face.getContour(FaceContour.LOWER_LIP_TOP) != null ?
                face.getContour(FaceContour.LOWER_LIP_TOP).getPoints() : null;

        if (labioSuperior != null && labioInferior != null &&
                !labioSuperior.isEmpty() && !labioInferior.isEmpty()) {

            float ySuperior = (labioSuperior.get(4).y + labioSuperior.get(5).y + labioSuperior.get(6).y) / 3f;
            float yInferior = (labioInferior.get(4).y + labioInferior.get(5).y + labioInferior.get(6).y) / 3f;
            float distancia = Math.abs(ySuperior - yInferior);
            float alturaCara = face.getBoundingBox().height();
            float proporcion = distancia / alturaCara;

            if (proporcion > BOCA_UMBRAL_NORMALIZADO) {
                bocaAbiertaFrames++;
                Log.d("Somnolencia", "Boca abierta - Frame #" + bocaAbiertaFrames +
                        (bostezoRegistrado ? " (bostezo ya registrado)" : "") +
                        " (Proporción: " + proporcion + ")");

                if (!bostezoRegistrado && bocaAbiertaFrames >= BOCA_UMBRAL_FRAMES) {
                    String mensaje = "Bostezo detectado, duración: " + (BOCA_UMBRAL_FRAMES / 5f) + " segundos)";
                    dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Advertencia", latitudActual, longitudActual);
                    Log.d("Somnolencia", mensaje);
                    bostezoRegistrado = true;
                }
            } else {
                bocaAbiertaFrames = 0;
                bostezoRegistrado = false;
            }
        }
    }


    private void detectarCabeceo(Face face) {
        float headEulerX = face.getHeadEulerAngleX();

        if (ultimoHeadEulerX != null) {
            float delta = headEulerX - ultimoHeadEulerX;

            if (delta > 15) {
                detectandoCabeceo = true;
                Log.d("Somnolencia", "Caída rápida de cabeza detectada (Δ: " + delta + ")");
            }

            if (detectandoCabeceo && headEulerX < 10) {
                String mensaje = "Cabeceo detectado: caída y regreso rápido de la cabeza";
                dsomnolencia.registrarEvento(mensaje, "Somnolencia", "Advertencia", latitudActual, longitudActual);
                Log.d("Somnolencia", mensaje);
                detectandoCabeceo = false;
            }
        }
        ultimoHeadEulerX = headEulerX;
    }

    private void activarAlarma() {
        if (!alarmaActiva) {
            dsomnolencia.activarAlertaSonora();
            alarmaActiva = true;
        }
    }

    private void detenerAlarma() {
        if (alarmaActiva) {
            dsomnolencia.detenerAlertaSonora();
            alarmaActiva = false;
        }
    }

}
