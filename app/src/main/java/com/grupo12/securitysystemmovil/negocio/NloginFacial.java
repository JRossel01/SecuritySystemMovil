package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.DloginFacial;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.ByteBuffer;
import java.util.List;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class NloginFacial {

    public interface OnUsuarioPreparadoListener {
        void onPreparado(JsonObject userInfo, Bitmap referenciaBitmap);
        void onError(String error);
    }

    public interface OnComparacionListener {
        void onResultado(boolean coincide);
    }

    public void procesarUsuarioPorCI(String ci, OnUsuarioPreparadoListener listener) {
        Log.d("NloginFacial", "Iniciando búsqueda del usuario con CI: " + ci);
        DloginFacial dlogin = new DloginFacial();
        dlogin.buscarUsuarioPorCI(ci, new DloginFacial.OnUserFoundListener() {
            @Override
            public void onSuccess(JsonObject userData) {
                Log.d("NloginFacial", "Datos del usuario recibidos: " + userData.toString());
                String urlFoto = userData.get("foto_url").getAsString();
                Log.d("NloginFacial", "URL de la foto obtenida: " + urlFoto);

                dlogin.descargarImagen(urlFoto, new DloginFacial.OnImagenDescargadaListener() {
                    @Override
                    public void onDescargada(Bitmap bitmap) {
                        Log.d("NloginFacial", "Imagen de referencia descargada correctamente");
                        listener.onPreparado(userData, bitmap);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("NloginFacial", "Fallo al descargar imagen: " + error);
                        listener.onError("No se pudo descargar la imagen: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    public void compararConReferencia(Bitmap referencia, Bitmap actual, OnComparacionListener listener) {
        Log.d("NloginFacial", "Iniciando comparación de rostros");
        InputImage imagenReferencia = InputImage.fromBitmap(referencia, 0);
        InputImage imagenActual = InputImage.fromBitmap(actual, 0);

        FaceDetectorOptions opciones = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .enableTracking()
                .build();

        FaceDetector detector = FaceDetection.getClient(opciones);

        Task<List<Face>> tareaReferencia = detector.process(imagenReferencia);
        tareaReferencia.addOnSuccessListener(carasReferencia -> {
            Log.d("NloginFacial", "Detección en imagen de referencia completada. Rostros encontrados: " + carasReferencia.size());

            Task<List<Face>> tareaActual = detector.process(imagenActual);
            tareaActual.addOnSuccessListener(carasActual -> {
                Log.d("NloginFacial", "Detección en imagen actual completada. Rostros encontrados: " + carasActual.size());
                boolean resultado = false;

                if (!carasReferencia.isEmpty() && !carasActual.isEmpty()) {
                    Face faceRef = carasReferencia.get(0);
                    Face faceAct = carasActual.get(0);

                    float diferencia = Math.abs(faceRef.getBoundingBox().centerX() - faceAct.getBoundingBox().centerX())
                            + Math.abs(faceRef.getBoundingBox().centerY() - faceAct.getBoundingBox().centerY());

                    Log.d("NloginFacial", "Diferencia entre centros de rostros: " + diferencia);
                    resultado = diferencia < 50; // Umbral ajustable
                } else {
                    Log.w("NloginFacial", "No se detectaron suficientes rostros para comparar");
                }

                listener.onResultado(resultado);
            }).addOnFailureListener(e -> {
                Log.e("NloginFacial", "Error procesando imagen actual", e);
                listener.onResultado(false);
            });
        }).addOnFailureListener(e -> {
            Log.e("NloginFacial", "Error procesando imagen de referencia", e);
            listener.onResultado(false);
        });
    }

    public static Bitmap convertirImageProxyABitmap(ImageProxy image) {
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, out);
        byte[] jpegBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    public void detectarRostro(Bitmap bitmap, java.util.function.Consumer<Boolean> callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    boolean hayRostro = !faces.isEmpty();
                    callback.accept(hayRostro);
                })
                .addOnFailureListener(e -> {
                    Log.e("NloginFacial", "Error al detectar rostro", e);
                    callback.accept(false);
                });
    }

    public void guardarConductorLocal(Context context, JsonObject userInfo, Bitmap imagen) {
        String ci = userInfo.get("ci").getAsString();
        String nombre = userInfo.get("nombre").getAsString();
        String apellido = userInfo.get("apellido").getAsString();
        int rol = userInfo.get("rol").getAsInt();

        // Guardar imagen en almacenamiento interno
        String nombreArchivo = "conductor_" + ci + ".jpg";
        File archivoImagen = new File(context.getFilesDir(), nombreArchivo);

        try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
            imagen.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            // Guardar datos en SQLite
            Nconductores nconductores = new Nconductores(context);
            boolean resultado = nconductores.registrarConductor(
                    ci,
                    nombre,
                    apellido,
                    rol,
                    archivoImagen.getAbsolutePath(),
                    1
            );

            if (resultado) {
                Log.d("NloginFacial", "Conductor guardado correctamente");
            } else {
                Log.e("NloginFacial", "Error al guardar conductor en BD");
            }

        } catch (IOException e) {
            Log.e("NloginFacial", "Error al guardar imagen del conductor", e);
        }
    }
}
