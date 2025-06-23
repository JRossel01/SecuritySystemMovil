package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private SensorManager sensorManager;
    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth;
    private TextView tvDistanciaParada;
    private TextView tvDistanciaDestino;
    private List<LatLng> paradasPendientes;
    private List<LatLng> recorridoRealizado = new ArrayList<>();
    private Polyline polylineRecorrido;
    private FusedLocationProviderClient locationClient;


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

    private Bitmap rotarIcono(Bitmap original, float angulo) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angulo);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
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

        // Dibujar ruta con Polyline (obtenida de Google Directions API o fallback)
        nruta.getRutaGoogleAsync(ruta -> {
            if (!ruta.isEmpty()) {
                // Aquí agregas la polilínea de la ruta decodificada
                PolylineOptions opcionesRuta = new PolylineOptions()
                        .addAll(ruta)
                        .color(Color.parseColor("#2962FF")) // Azul
                        .width(10f);
                mapa.addPolyline(opcionesRuta);
            } else {
                Log.e("Pruta", "No se pudo obtener la ruta optimizada. Mostrando ruta básica.");
                List<LatLng> rutaFallback = nruta.getRutaCompleta();
                // Aquí, también puedes agregar la ruta de fallback (si es necesario)
                PolylineOptions opcionesFallback = new PolylineOptions()
                        .addAll(rutaFallback)
                        .color(Color.RED)
                        .width(10f);
                mapa.addPolyline(opcionesFallback);
            }
        });

        paradasPendientes = new ArrayList<>(nruta.getParadas());

        nruta.verificarUbicacion(this, (posicion, distanciaParada, distanciaDestino) -> {


            if (marcadorActual == null) {
                Bitmap flecha = rotarIcono(MapaIconos.IconoUbicacionActual(), azimuth);
                marcadorActual = mapa.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title("Mi posición")
                        .icon(BitmapDescriptorFactory.fromBitmap(flecha))
                        .anchor(0.5f, 0.5f)
                        .flat(true));


                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(posicion)
                        .zoom(18f)
                        .bearing(azimuth)
                        .tilt(45f)
                        .build();
                mapa.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            } else {
                marcadorActual.setPosition(posicion);
            }

            // Aquí se vuelve a usar el estilo Google Maps con inclinación y rotación
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(posicion)
                    .zoom(18f)
                    .bearing(azimuth)
                    .tilt(45f)
                    .build();
            mapa.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            // Dibuja línea gris del recorrido
            recorridoRealizado.add(posicion);
            if (polylineRecorrido != null) polylineRecorrido.remove();
            polylineRecorrido = mapa.addPolyline(new PolylineOptions()
                    .addAll(recorridoRealizado)
                    .color(Color.GRAY)
                    .width(8f));

            // Actualiza los textos de distancia
            String textoParada = (distanciaParada >= 1000) ?
                    String.format("Distancia a parada: %.2f km", distanciaParada / 1000.0) :
                    String.format("Distancia a parada: %.0f m", distanciaParada);

            String textoDestino = (distanciaDestino >= 1000) ?
                    String.format("Distancia a destino: %.2f km", distanciaDestino / 1000.0) :
                    String.format("Distancia a destino: %.0f m", distanciaDestino);

            tvDistanciaParada.setText(textoParada);
            tvDistanciaDestino.setText(textoDestino);

        });
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
        }
    }

    private void ubicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());

                    if (marcadorActual == null) {
                        Bitmap flecha = rotarIcono(MapaIconos.IconoUbicacionActual(), azimuth);
                        marcadorActual = mapa.addMarker(new MarkerOptions()
                                .position(posicion)
                                .title("Mi posición")
                                .icon(BitmapDescriptorFactory.fromBitmap(flecha)) // ✅ Agrega ícono también aquí
                                .anchor(0.5f, 0.5f)
                                .flat(true));

                    } else {
                        marcadorActual.setPosition(posicion);
                    }

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(posicion)
                            .zoom(18f)
                            .bearing(azimuth)
                            .tilt(45f)
                            .build();
                    mapa.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                }
            });
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);

            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                if (marcadorActual != null) {
                    marcadorActual.setRotation(azimuth);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        nruta.detenerMonitoreo(); // Detiene las actualizaciones de ubicación
    }


}
