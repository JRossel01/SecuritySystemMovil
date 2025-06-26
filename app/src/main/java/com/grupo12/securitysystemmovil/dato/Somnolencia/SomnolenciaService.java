package com.grupo12.securitysystemmovil.dato.Somnolencia;

import com.grupo12.securitysystemmovil.negocio.Nsomnolencia;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.face.Face;
import com.grupo12.securitysystemmovil.negocio.Nsomnolencia;

import java.util.List;

public class SomnolenciaService extends Service {

    private Nsomnolencia nsomnolencia;

    @Override
    public void onCreate() {
        super.onCreate();
        nsomnolencia = new Nsomnolencia(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Este servicio no es diseñado para ser vinculado, retornamos null
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Realizamos el proceso de detección de somnolencia aquí
        // Por ejemplo, puedes pasar una lista de caras para procesar
        List<Face> faces = (List<Face>) intent.getSerializableExtra("faces");
        if (faces != null) {
            nsomnolencia.evaluarSomnolencia(this, faces);
        }

        // El servicio continuará corriendo en segundo plano
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("SomnolenciaService", "Servicio destruido");
    }
}
