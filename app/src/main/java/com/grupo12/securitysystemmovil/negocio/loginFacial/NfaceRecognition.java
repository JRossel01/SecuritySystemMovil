package com.grupo12.securitysystemmovil.negocio.loginFacial;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class NfaceRecognition {
    private Interpreter tfLite;
    private final int inputSize = 112;
    private final float IMAGE_MEAN = 128.0f;
    private final float IMAGE_STD = 128.0f;
    private final int OUTPUT_SIZE = 192;

    private final Context context;
    private final Map<String, float[]> registered = new HashMap<>();

    public NfaceRecognition(Context context) {
        this.context = context;
        try {
            cargarModelo(context);
        } catch (IOException e) {
            Log.e("NloginFacialS", "Error al cargar el modelo", e);
        }
    }

    private void cargarModelo(Context context) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd("mobile_face_net.tflite").getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd("mobile_face_net.tflite").getStartOffset();
        long declaredLength = context.getAssets().openFd("mobile_face_net.tflite").getDeclaredLength();
        MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        tfLite = new Interpreter(modelBuffer);
    }

    public float[] procesarImagen(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }

        float[][] embeddings = new float[1][OUTPUT_SIZE];
        tfLite.run(imgData, embeddings);
        return embeddings[0];
    }

    public Bitmap detectarYRecortar(Bitmap originalBitmap, Context context) throws Exception {
        if (originalBitmap == null || originalBitmap.getWidth() == 0 || originalBitmap.getHeight() == 0) {
            Log.e("NloginFacial", "❌ Bitmap inválido.");
            throw new Exception("La imagen de entrada está vacía o es inválida.");
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        Log.d("NloginFacial", "Tamaño de imagen: " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());
        InputImage image = InputImage.fromBitmap(originalBitmap, 0);
        Task<List<Face>> result = detector.process(image);
        List<Face> faces = com.google.android.gms.tasks.Tasks.await(result);

        Log.d("NloginFacial", "Cantidad de rostros detectados: " + faces.size());
        for (int i = 0; i < faces.size(); i++) {
            Rect box = faces.get(i).getBoundingBox();
            Log.d("NloginFacial", "Rostro[" + i + "]: left=" + box.left + ", top=" + box.top +
                    ", right=" + box.right + ", bottom=" + box.bottom);
        }

        if (faces.isEmpty()) {
            throw new Exception("❌ No se detectó ningún rostro en la imagen.");
        }

        if (faces.size() > 1) {
            Log.w("NloginFacial", "⚠️ Se detectaron múltiples rostros, se tomará el primero.");
        }

        RectF boundingBox = new RectF(faces.get(0).getBoundingBox());
        Bitmap recortado = getCropBitmapByCPU(originalBitmap, boundingBox);
        return getResizedBitmap(recortado, inputSize, inputSize);
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(), (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRect(new RectF(0, 0, cropRectF.width(), cropRectF.height()), paint);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);
        canvas.drawBitmap(source, matrix, paint);
        return resultBitmap;
    }

    public void generarEmbeddingsDesdeBitmap(Bitmap originalBitmap, String nombre) {
        try {
            Bitmap rostroProcesado = detectarYRecortar(originalBitmap, context);
            float[] embeddings = procesarImagen(rostroProcesado);
            registered.put(nombre, embeddings);
            Log.d("NloginFacial", "Embeddings generados y guardados para " + nombre);

        } catch (Exception e) {
            Log.e("NloginFacial", "Error en procesamiento de imagen: " + e.getMessage(), e);
        }
    }

    public float[] getEmbeddings(String nombre) {
        return registered.get(nombre);
    }

    public boolean contieneEmbeddings(String nombre) {
        return registered.containsKey(nombre);
    }

    public void procesarImagenCamara(Bitmap bitmapCamara, Context context, OnEmbeddingsGeneradosListener listener) {
        if (bitmapCamara == null) {
            listener.onError("❌ La imagen capturada es nula.");
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(bitmapCamara, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d("NloginFacial", "🟢 Rostros detectados en cámara: " + faces.size());

                    if (faces.isEmpty()) {
                        listener.onError("❌ No se detectó ningún rostro.");
                        return;
                    }

                    if (faces.size() > 1) {
                        Log.w("NloginFacial", "⚠️ Se detectaron múltiples rostros. Se usará el primero.");
                    }

                    RectF boundingBox = new RectF(faces.get(0).getBoundingBox());
                    Bitmap recortado = getCropBitmapByCPU(bitmapCamara, boundingBox);
                    Bitmap rostroProcesado = getResizedBitmap(recortado, inputSize, inputSize);
                    float[] embeddings = procesarImagen(rostroProcesado);

                    listener.onEmbeddingsGenerados(embeddings);
                })
                .addOnFailureListener(e -> {
                    Log.e("NloginFacial", "❌ Error en detección facial: " + e.getMessage(), e);
                    listener.onError("Error al procesar rostro: " + e.getMessage());
                });
    }

    public interface OnEmbeddingsGeneradosListener {
        void onEmbeddingsGenerados(float[] embeddings);
        void onError(String mensaje);
    }


}
