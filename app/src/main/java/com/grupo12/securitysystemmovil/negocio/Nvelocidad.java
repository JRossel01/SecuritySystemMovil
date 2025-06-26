package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;
import java.util.Locale;
import android.util.Log;
import android.view.View;
import android.location.Location;
import android.widget.TextView;

import com.grupo12.securitysystemmovil.dato.Dvelocidad;

public class Nvelocidad {

    private boolean enExceso = false;
    private long tiempoInicioExceso = 0;
    private double latitud = 0;
    private double longitud = 0;
    private float umbralVelocidad = -1f;
    private float velocidadAnterior = -1f;
    private long tiempoAnterior = 0;
    private static final float UMBRAL_FRENO_BRUSCO = 15f; // en km/h

    private Dvelocidad dvelocidad;

    public Nvelocidad() {
        dvelocidad = new Dvelocidad(); // Lo conectaremos desde Pvelocidad con contexto
    }

    public void setContextoBD(Context context) {
        dvelocidad.setContext(context);
    }

    public void procesarVelocidad(Location location) {
        if (location == null) return;

        float velocidadMps = location.getSpeed();
        float velocidadKmh = velocidadMps * 3.6f;

        long tiempoActual = System.currentTimeMillis();

        setUbicacion(location.getLatitude(), location.getLongitude());
        detectarVelocidadMaxima(velocidadKmh, umbralVelocidad);
        detectarFrenoBrusco(velocidadKmh, tiempoActual);
    }

    public void detectarVelocidadMaxima(float velocidadActual, float umbral) {
        long tiempoActual = System.currentTimeMillis();

        if (velocidadActual > umbral) {
            if (!enExceso) {
                // Entró en exceso de velocidad
                enExceso = true;
                tiempoInicioExceso = tiempoActual;
                String mensaje = String.format(Locale.getDefault(),"Velocidad máxima excedida");
                Log.i("Velocidad", mensaje);
                dvelocidad.crearEvento(mensaje, "Velocidad", "Advertencia", latitud, longitud);
                dvelocidad.activarAlertaSonora();
            }
        } else {
            if (enExceso) {
                // Salió del exceso de velocidad
                enExceso = false;
                long duracion = tiempoActual - tiempoInicioExceso;
                long segundos = duracion / 1000;
                long minutos = segundos / 60;
                segundos = segundos % 60;

                String mensaje = String.format(Locale.getDefault(),"Conducción en exceso durante %d min %d seg", minutos, segundos);
                Log.i("Velocidad", "Mensaje generado: " + mensaje);
                dvelocidad.crearEvento(mensaje, "Velocidad", "Advertencia", latitud, longitud);
                dvelocidad.detenerAlertaSonora();
            }
        }
    }

    private void detectarFrenoBrusco(float velocidadActual, long tiempoActual) {
        if (velocidadAnterior >= 0) {
            float diferenciaVelocidad = velocidadAnterior - velocidadActual;
            long deltaTiempo = tiempoActual - tiempoAnterior;

            if (diferenciaVelocidad >= UMBRAL_FRENO_BRUSCO && deltaTiempo <= 1500) {
                if (velocidadActual < 1.0f) {
                    // Posible colisión
                    String mensaje = String.format(Locale.getDefault(),
                            "Freno brusco detectado (%d km/h en %.1f s)",
                            Math.round(diferenciaVelocidad), deltaTiempo / 1000f);

                    Log.e("Velocidad", mensaje);
                    dvelocidad.crearEvento(mensaje, "Colision", "Critico", latitud, longitud);
                } else {
                    // ⚠Freno brusco común
                    String mensaje = String.format(Locale.getDefault(),
                            "Freno brusco detectado (%d km/h en %.1f s)",
                            Math.round(diferenciaVelocidad), deltaTiempo / 1000f);

                    Log.w("Velocidad", mensaje);
                    dvelocidad.crearEvento(mensaje, "Velocidad", "Critico", latitud, longitud);
                }
            }
        }

        velocidadAnterior = velocidadActual;
        tiempoAnterior = tiempoActual;
    }

    public void umbralVelocidad(Context context) {
        Nvehiculo nvehiculo = new Nvehiculo(context);
        this.umbralVelocidad = nvehiculo.obtenerVehiculoLocal().velocidadMaxima;
    }

    public void setUbicacion(double lat, double lon) {
        this.latitud = lat;
        this.longitud = lon;
    }


    public float getUmbralVelocidad() {
        return umbralVelocidad;
    }

    public boolean esFrenoBrusco(float vAnterior, float vActual, long tAnterior, long tActual) {
        float diferencia = vAnterior - vActual;
        long deltaTiempo = tActual - tAnterior;
        return diferencia >= 15f && deltaTiempo <= 1500 && vActual < 1.0f;
    }

}
