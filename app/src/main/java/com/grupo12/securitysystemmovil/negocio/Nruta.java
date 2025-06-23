package com.grupo12.securitysystemmovil.negocio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.grupo12.securitysystemmovil.dato.Druta;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Nruta {
    private Druta druta;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private boolean salidaOrigenRegistrada = false;
    private List<LatLng> paradasRegistradas = new ArrayList<>();
    private List<LatLng> paradasLlegadaRegistrada = new ArrayList<>();


    public Nruta(Context context) {
        druta = new Druta(context);
    }

    public interface Callback {
        void onActualizarPosicion(LatLng posicion, float distanciaParada, float distanciaDestino);
    }

    public LatLng getOrigen() {
        return druta.getOrigen();
    }

    public List<LatLng> getParadas() {
        return druta.getParadas();
    }

    public LatLng getDestino() {
        return druta.getDestino();
    }

    private boolean destinoRegistrado = false;

    public List<LatLng> getRutaCompleta() {
        List<LatLng> ruta = new ArrayList<>();
        ruta.add(getOrigen());
        ruta.addAll(getParadas());
        ruta.add(getDestino());
        return ruta;
    }

    public interface CallbackRutaGoogle {
        void onRutaObtenida(List<LatLng> ruta);
    }

    public void getRutaGoogleAsync(NrutaCallback callback) {
        druta.obtenerRutaGoogleAsync(getOrigen(), getParadas(), getDestino(), ruta -> {
            callback.onRutaObtenida(ruta);
        });
    }

    public interface NrutaCallback {
        void onRutaObtenida(List<LatLng> ruta);
    }




    public boolean isDestinoRegistrado() {
        return destinoRegistrado;
    }

    public void setDestinoRegistrado(boolean registrado) {
        this.destinoRegistrado = registrado;
    }

    public int getNumeroParada(LatLng parada) {
        return druta.getParadas().indexOf(parada) + 1;
    }

    public void crearEventoLlegada(String mensajeBase, double latitud, double longitud, int numeroParada, boolean esDestino) {
        // Obtener nombre del vehículo
        String nombreVehiculo = druta.obtenerNombreVehiculo();

        // Obtener nombre del conductor activo
        String nombreConductor = druta.obtenerNombreConductor();

        // Armar mensaje final
        String mensaje;
        if (esDestino) {
            mensaje = "El conductor " + nombreConductor + " a bordo del vehículo " + nombreVehiculo + " acaba de llegar al destino";
        } else {
            mensaje = "El conductor " + nombreConductor + " a bordo del vehículo " + nombreVehiculo + " acaba de llegar a la parada " + numeroParada;
        }

        druta.guardarEvento(mensaje, latitud, longitud);
    }

    public void crearEventoSalida(double lat, double lon, int numeroParada, boolean esOrigen) {
        String nombreVehiculo = druta.obtenerNombreVehiculo();
        String nombreConductor = druta.obtenerNombreConductor();

        String mensaje;
        if (esOrigen) {
            mensaje = "El conductor " + nombreConductor + " a bordo del vehículo " + nombreVehiculo + " acaba de salir del punto de partida";
        } else {
            mensaje = "El conductor " + nombreConductor + " a bordo del vehículo " + nombreVehiculo + " acaba de salir de la parada " + numeroParada;
        }

        druta.guardarEvento(mensaje, lat, lon);
    }

    public void verificarUbicacion(Context context, Callback callback) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context);

        LocationRequest request = new LocationRequest.Builder(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) return;

                LatLng posicionActual = new LatLng(location.getLatitude(), location.getLongitude());

                // Calcular distancias
                float distanciaParada = calcularDistancia(posicionActual, getParadas());
                float distanciaDestino = calcularDistancia(posicionActual, List.of(getDestino()));

                // Detectar salida del origen
                LatLng origen = getOrigen();
                float distanciaAO = calcularDistancia(posicionActual, List.of(origen));
                if (!salidaOrigenRegistrada && distanciaAO > 50f) {
                    crearEventoSalida(location.getLatitude(), location.getLongitude(), 0, true);
                    salidaOrigenRegistrada = true;
                }

                // Detectar salida de paradas
                for (LatLng parada : getParadas()) {
                    float dParada = calcularDistancia(posicionActual, List.of(parada));

                    if (dParada < 50f && !paradasRegistradas.contains(parada)) {
                        // Está llegando, marcar como registrada para salida futura
                        paradasRegistradas.add(parada);
                    }

                    if (dParada > 70f && paradasRegistradas.contains(parada)) {
                        // Ya salió de esta parada
                        int numParada = getNumeroParada(parada);
                        crearEventoSalida(location.getLatitude(), location.getLongitude(), numParada, false);
                        paradasRegistradas.remove(parada); // prevenir repeticiones
                    }
                }

                // Detectar llegada a parada
                for (LatLng parada : getParadas()) {
                    float distancia = calcularDistancia(posicionActual, List.of(parada));

                    if (distancia < 50f && !paradasLlegadaRegistrada.contains(parada)) {
                        int numero = getNumeroParada(parada);
                        crearEventoLlegada("parada", location.getLatitude(), location.getLongitude(), numero, false);
                        paradasLlegadaRegistrada.add(parada);
                    }
                }

                // Detectar llegada a destino
                if (!destinoRegistrado && distanciaDestino < 50f) {
                    crearEventoLlegada("destino", location.getLatitude(), location.getLongitude(), 0, true);
                    destinoRegistrado = true;
                }

                // Callback visual
                callback.onActualizarPosicion(posicionActual, distanciaParada, distanciaDestino);
            }
        };

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(request, locationCallback, null);
        }
    }

    private float calcularDistancia(LatLng origen, List<LatLng> destinos) {
        float distanciaMinima = Float.MAX_VALUE;

        float[] resultados = new float[1];
        for (LatLng destino : destinos) {
            Location.distanceBetween(
                    origen.latitude, origen.longitude,
                    destino.latitude, destino.longitude,
                    resultados
            );
            if (resultados[0] < distanciaMinima) {
                distanciaMinima = resultados[0];
            }
        }
        return distanciaMinima;
    }

    public void detenerMonitoreo() {
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

}
