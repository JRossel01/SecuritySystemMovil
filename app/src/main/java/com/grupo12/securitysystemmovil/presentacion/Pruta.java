package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;


import com.grupo12.securitysystemmovil.R;
import com.grupo12.securitysystemmovil.negocio.Nruta;
import com.grupo12.securitysystemmovil.utils.MapaIconos;

import java.util.ArrayList;
import java.util.List;

public class Pruta extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private Nruta nruta;
    private GoogleMap mapa;
    private Marker marcadorActual;
    private FusedLocationProviderClient locationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private SensorManager sensorManager;
    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth;
    private List<LatLng> puntosRecorridos = new ArrayList<>();
    private Polyline lineaRecorrido;
    private TextView tvDistanciaParada;
    private TextView tvDistanciaDestino;
    private List<LatLng> paradasPendientes;
    private List<LatLng> paradasAlcanzadas = new ArrayList<>();
    private boolean salidaRegistrada = false;
    private static final double UMBRAL_PARADA_METROS = 50;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pruta);

        nruta = new Nruta(getApplicationContext());

        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, renderer -> {
            Log.d("Maps", "Renderizador inicializado: " + renderer.name());
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        tvDistanciaParada = findViewById(R.id.tvDistanciaParada);
        tvDistanciaDestino = findViewById(R.id.tvDistanciaDestino);


    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        ubicacionActual();
        actualizarUbicacion();

        // Mostrar marcador de origen (inicio)
        LatLng origen = nruta.getOrigen();
        mapa.addMarker(new MarkerOptions()
                .position(origen)
                .title("Inicio")
                .icon(MapaIconos.IconoOrigen()));

        // Mostrar marcadores de paradas
        List<LatLng> paradas = nruta.getParadas();
        for (int i = 0; i < paradas.size(); i++) {
            String numero = String.valueOf(i + 1);
            mapa.addMarker(new MarkerOptions()
                    .position(paradas.get(i))
                    .title("Parada " + numero)
                    .icon(MapaIconos.IconoParada(numero)));
        }

        // Mostrar marcador de destino
        LatLng destino = nruta.getDestino();
        mapa.addMarker(new MarkerOptions().position(destino).title("Destino"));

        // Dibujar ruta con polyline (línea azul)
        List<LatLng> rutaCompleta = nruta.getRutaCompleta();
        PolylineOptions opcionesRuta = new PolylineOptions()
                .addAll(rutaCompleta)
                .color(Color.parseColor("#2962FF")) // celeste
                .width(10f); // grosor de la línea
        mapa.addPolyline(opcionesRuta);

        paradasPendientes = new ArrayList<>(nruta.getParadas());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapa);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

            actualizarUbicacion();
        }
    }

    private void ubicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());

                    if (marcadorActual == null) {
                        marcadorActual = mapa.addMarker(new MarkerOptions()
                                .position(posicion)
                                .icon(MapaIconos.IconoUbicacionActual())
                                .title("Mi posición")
                                .anchor(0.5f, 0.5f)
                                .flat(true));
                    } else {
                        marcadorActual.setPosition(posicion);
                    }

                    mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 18));
                }
            });
        }
    }

    private void actualizarUbicacion() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500) // mínimo medio segundo
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());

                    runOnUiThread(() -> {
                        if (marcadorActual == null) {
                            marcadorActual = mapa.addMarker(new MarkerOptions()
                                    .position(posicion)
                                    .icon(MapaIconos.IconoUbicacionActual())
                                    .title("Mi posición"));
                        } else {
                            marcadorActual.setPosition(posicion);
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(posicion)         // posición actual
                                    .zoom(18f)                // nivel de zoom (ajústalo si quieres más o menos cerca)
                                    .bearing(azimuth)         // dirección hacia la que se mueve el vehículo
                                    .tilt(45f)                // inclinación de la cámara para un efecto 3D
                                    .build();

                            mapa.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }

                        puntosRecorridos.add(posicion);

                        if (!salidaRegistrada) {
                            double distanciaDesdeOrigen = SphericalUtil.computeDistanceBetween(posicion, nruta.getOrigen());
                            if (distanciaDesdeOrigen > UMBRAL_PARADA_METROS) {
                                salidaRegistrada = true;
                                nruta.crearEventoSalida("origen", posicion.latitude, posicion.longitude, 0, true);
                            }
                        }

                        // Detectar salida desde cada parada ya alcanzada
                        for (LatLng parada : new ArrayList<>(paradasAlcanzadas)) {
                            double distancia = SphericalUtil.computeDistanceBetween(posicion, parada);
                            if (distancia > UMBRAL_PARADA_METROS) {
                                int numeroParada = nruta.getNumeroParada(parada);
                                nruta.crearEventoSalida("parada", posicion.latitude, posicion.longitude, numeroParada, false);
                                paradasAlcanzadas.remove(parada);
                            }
                        }

                        // Detectar llegada a paradas
                        for (LatLng parada : new ArrayList<>(paradasPendientes)) {
                            double distancia = SphericalUtil.computeDistanceBetween(posicion, parada);
                            if (distancia < UMBRAL_PARADA_METROS) {
                                paradasPendientes.remove(parada);
                                paradasAlcanzadas.add(parada); // importante para detectar salida después
                                int numeroParada = nruta.getNumeroParada(parada);
                                nruta.crearEventoLlegada(null, posicion.latitude, posicion.longitude, numeroParada, false);
                            }
                        }

                        for (LatLng parada : new ArrayList<>(paradasPendientes)) {
                            double distancia = SphericalUtil.computeDistanceBetween(posicion, parada);
                            if (distancia < UMBRAL_PARADA_METROS) {
                                paradasPendientes.remove(parada);
                                int numeroParada = nruta.getNumeroParada(parada);
                                nruta.crearEventoLlegada(null, posicion.latitude, posicion.longitude, numeroParada, false);
                            }
                        }

                        LatLng destino = nruta.getDestino();
                        if (SphericalUtil.computeDistanceBetween(posicion, destino) < UMBRAL_PARADA_METROS && !nruta.isDestinoRegistrado()) {
                            nruta.setDestinoRegistrado(true);
                            nruta.crearEventoLlegada(null, posicion.latitude, posicion.longitude, 0, true);
                        }

                        if (lineaRecorrido != null) lineaRecorrido.remove();
                        lineaRecorrido = mapa.addPolyline(new PolylineOptions()
                                .addAll(puntosRecorridos)
                                .color(Color.GRAY)
                                .width(10f));

                        destino = nruta.getDestino();
                        double distanciaDestino = SphericalUtil.computeDistanceBetween(posicion, destino);

                        LatLng proximaParada = obtenerProximaParada(posicion);
                        double distanciaParada = SphericalUtil.computeDistanceBetween(posicion, proximaParada);

                        String textoDestino;
                        if (distanciaDestino >= 1000) {
                            textoDestino = String.format("Hasta destino: %.2f km", distanciaDestino / 1000);
                        } else {
                            textoDestino = String.format("Hasta destino: %.0f m", distanciaDestino);
                        }

                        String textoParada;
                        if (distanciaParada >= 1000) {
                            textoParada = String.format("Hasta próxima parada: %.2f km", distanciaParada / 1000);
                        } else {
                            textoParada = String.format("Hasta próxima parada: %.0f m", distanciaParada);
                        }

                        tvDistanciaDestino.setText(textoDestino);
                        tvDistanciaParada.setText(textoParada);
                    });
                }
            }
        };
        // Iniciar actualizaciones solo si se tienen permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                if (marcadorActual != null) {
                    // Suavizar la rotación
                    float rotacionAnterior = marcadorActual.getRotation();
                    float diferencia = azimuth - rotacionAnterior;
                    if (Math.abs(diferencia) > 180) {
                        if (diferencia > 0) diferencia -= 360;
                        else diferencia += 360;
                    }
                    marcadorActual.setRotation(rotacionAnterior + diferencia * 0.1f); // Suaviza el giro
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es obligatorio que tenga lógica, pero debe estar implementado
    }

    private LatLng obtenerProximaParada(LatLng posicionActual) {
        if (paradasPendientes.isEmpty()) {
            return nruta.getDestino();
        }

        // Recorre y elimina las paradas ya alcanzadas
        List<LatLng> copia = new ArrayList<>(paradasPendientes);
        for (LatLng parada : copia) {
            double d = SphericalUtil.computeDistanceBetween(posicionActual, parada);
            if (d < UMBRAL_PARADA_METROS) {
                paradasPendientes.remove(parada); // ya fue alcanzada
            }
        }

        // Devuelve la siguiente si queda alguna, o el destino
        if (!paradasPendientes.isEmpty()) {
            return paradasPendientes.get(0);
        } else {
            return nruta.getDestino();
        }
    }

}