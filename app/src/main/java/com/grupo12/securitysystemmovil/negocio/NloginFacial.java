package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Consumer;

import com.google.android.gms.location.Priority;
import com.google.gson.JsonObject;
import com.grupo12.securitysystemmovil.dato.DloginFacial;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.nio.ByteBuffer;
import java.util.List;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.grupo12.securitysystemmovil.negocio.loginFacial.NfaceRecognition;
import com.grupo12.securitysystemmovil.presentacion.PeditVehiculo;
import com.grupo12.securitysystemmovil.presentacion.Perror.PsinViaje;
import com.grupo12.securitysystemmovil.presentacion.Perror.PvehiculoIncorrecto;
import com.grupo12.securitysystemmovil.presentacion.PpreViaje;

import android.Manifest;
import android.content.pm.PackageManager;

public class NloginFacial {

    private NfaceRecognition recognizerCache = null;
    private Bitmap referenciaBitmap;
    private FaceDetector detector;

    public interface OnUsuarioPreparadoListener {
        void onPreparado(JsonObject userInfo, Bitmap referenciaBitmap);
        void onError(String error);
    }

    public interface OnComparacionListener {
        void onResultado(boolean coincide);
    }

    public void procesarUsuarioPorCI(Context context, String ci, OnUsuarioPreparadoListener listener) {
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
                        entrenarDesdeImagenReferencia(context, bitmap, ci);
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

    public void entrenarDesdeImagenReferencia(Context context, Bitmap referencia, String nombre) {
        NfaceRecognition recognizer = getRecognizer(context);
        recognizer.generarEmbeddingsDesdeBitmap(referencia, nombre);
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

    public void detectarRostro(Bitmap bitmap, Consumer<Boolean> callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();

        detector = FaceDetection.getClient(options);

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

    public void guardarLocal(Context context, JsonObject userInfo, Bitmap imagen) {
        int rol = userInfo.get("rol").getAsInt();
        int userId = userInfo.get("id").getAsInt();
        String ci = userInfo.get("ci").getAsString();
        String nombre = userInfo.get("nombre").getAsString();
        String apellido = userInfo.get("apellido").getAsString();

        DloginFacial dlogin = new DloginFacial();
        dlogin.limpiarBD(context);

        if (rol == 1) {
            Ngestor ngestor = new Ngestor(context);
            boolean resultadoGestor = ngestor.guardarGestor(userId, ci, nombre, apellido, rol);

            if (resultadoGestor) {
                Log.d("NloginFacial", "Gestor guardado correctamente");

                guardarEvento(context, userId,
                        "Inicio de sesión exitoso del gestor", "Seguridad", "Informacion", dlogin,
                        () -> redirigirPorRol(context, rol));

            } else {
                Log.e("NloginFacial", "Error al guardar gestor en BD");
            }

            return;
        }

        Nvehiculo nvehiculo = new Nvehiculo(context);
        Ntrip ntrip = new Ntrip(context);

        String nombreArchivo = "conductor_" + ci + ".jpg";
        File archivoImagen = new File(context.getFilesDir(), nombreArchivo);

        try (FileOutputStream out = new FileOutputStream(archivoImagen)) {
            imagen.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            Nconductores nconductores = new Nconductores(context);
            boolean resultado = nconductores.registrarConductor(
                    userId, ci, nombre, apellido, rol, archivoImagen.getAbsolutePath(), 1
            );

            if (resultado) {
                Log.d("NloginFacial", "Conductor guardado correctamente");

                ntrip.guardarTrip(ci, userId, new Ntrip.TripCallback() {
                    @Override
                    public void onSuccess() {
                        int vehicleIdLocal = nvehiculo.obtenerVehiculoLocal().id;
                        int vehicleIdViaje = ntrip.obtenerVehicleId();

                        guardarEvento(context, userId,
                                "Inicio de sesión exitoso del conductor", "Seguridad", "Informacion", dlogin,
                                () -> {
                                    Log.d("NloginFacial", "vehicleIdLocal: " + vehicleIdLocal);
                                    Log.d("NloginFacial", "vehicleIdViaje: " + vehicleIdViaje);

                                    if (vehicleIdLocal != vehicleIdViaje) {
                                        Log.e("NloginFacial", "El vehículo del dispositivo no coincide con el del viaje.");

                                        guardarEvento(context, userId,
                                                "Inicio de sesión por conductor no autorizado",
                                                "Seguridad", "Advertencia", dlogin,
                                                () -> {

                                                    dlogin.limpiarBD(context);

                                                    Intent intent = new Intent(context, PvehiculoIncorrecto.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    context.startActivity(intent);
                                                });
                                        return;
                                    }

                                    Log.d("NloginFacial", "Conductor y viaje válidos. Procediendo con redirección.");
                                    redirigirPorRol(context, rol);
                                }
                        );
                    }


                    @Override
                    public void onFailure(String error) {
                        Log.e("NloginFacial", "Error al guardar el viaje activo: " + error);

                        if (error.contains("No hay viaje activo")) {
                            Intent intent = new Intent(context, PsinViaje.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(intent);
                        }
                    }
                });
            } else {
                Log.e("NloginFacial", "Error al guardar conductor en BD");
            }

        } catch (IOException e) {
            Log.e("NloginFacial", "Error al guardar imagen del conductor", e);
        }

    }

    private void redirigirPorRol(Context context, int rol) {
        Class<?> destino = (rol == 1) ? PeditVehiculo.class : PpreViaje.class;
        Intent intent = new Intent(context, destino);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private void guardarEvento(Context context, int userId, String mensaje, String tipo, String nivel, DloginFacial dlogin, Runnable onComplete) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("NloginFacial", "Permiso de ubicación denegado");
            dlogin.guardarEvento(context, userId, mensaje, tipo, nivel, 0, 0);
            onComplete.run();
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    double lat = (location != null) ? location.getLatitude() : 0;
                    double lon = (location != null) ? location.getLongitude() : 0;

                    dlogin.guardarEvento(context, userId, mensaje, tipo, nivel, lat, lon);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("NloginFacial", "Error al obtener ubicación", e);
                    dlogin.guardarEvento(context, userId, mensaje, tipo, nivel, 0, 0);
                    onComplete.run();
                });
    }

    public boolean compararEmbeddings(String nombreReferencia, float[] nuevoEmbedding) {
        if (!recognizerCache.contieneEmbeddings(nombreReferencia)) {
            Log.e("NloginFacial", "❌ No hay embeddings registrados para " + nombreReferencia);
            return false;
        }

        float[] embRef = recognizerCache.getEmbeddings(nombreReferencia);
        float distancia = calcularDistancia(embRef, nuevoEmbedding);
        Log.d("NloginFacial", "📏 Distancia calculada: " + distancia);
        return distancia <= 1.5f; // Ajusta si es necesario
    }


    private float calcularDistancia(float[] emb1, float[] emb2) {
        float suma = 0;
        for (int i = 0; i < emb1.length; i++) {
            float diff = emb1[i] - emb2[i];
            suma += diff * diff;
        }
        return (float) Math.sqrt(suma);
    }

    public NfaceRecognition getRecognizer(Context context) {
        if (recognizerCache == null) {
            recognizerCache = new NfaceRecognition(context);
        }
        return recognizerCache;
    }

    public void setReferenciaBitmap(Bitmap bitmap) {
        this.referenciaBitmap = bitmap;
    }

    public Bitmap getReferenciaBitmap() {
        return referenciaBitmap;
    }



}
