package com.grupo12.securitysystemmovil.presentacion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.OptIn;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.maps.android.SphericalUtil;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.grupo12.securitysystemmovil.MainActivity;
import com.grupo12.securitysystemmovil.R;
import com.google.mlkit.vision.face.FaceDetector;
import com.grupo12.securitysystemmovil.dato.Dconductor;
import com.grupo12.securitysystemmovil.dato.Seguimiento.SeguimientoService;
import com.grupo12.securitysystemmovil.negocio.NcambioConductor;
import com.grupo12.securitysystemmovil.negocio.Nruta;
import com.grupo12.securitysystemmovil.negocio.Nsomnolencia;
import com.grupo12.securitysystemmovil.negocio.Nvelocidad;
import com.grupo12.securitysystemmovil.utils.MapaIconos;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class Pnavegacion extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    /////////////////Velocidad////////////////////
    private static final int REQUEST_LOCATION_PERMISSION2 = 1002;
    private TextView tvVelocidad;
    private Nvelocidad nvelocidad;
    private TextView tvAlertaVelocidad;



    /////////////////CambioConductor////////////////////
    private Spinner spinnerConductores;
    private Button btnCambiarConductor;
    private NcambioConductor ncambio;
    private List<Dconductor> listaConductores;


    /////////////////Somnolencia////////////////////
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private PreviewView previewView;
    private FaceDetector detector;
    private double latitud = 0;
    private double longitud = 0;
    private Nsomnolencia nsomnolencia;

    /////////////////Ruta////////////////////
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

    private Button btnFinalizar;
    private float distanciaDestino = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pnavegacion);

        /////////////////Seguimiento////////////////////
        //        Iniciar Seguimiento
//        Intent intent = new Intent(this, SeguimientoService.class);
//        startService(intent);

        /////////////////Velocidad////////////////////
        tvVelocidad = findViewById(R.id.tvVelocidad);
        nvelocidad = new Nvelocidad();
        nvelocidad.setContextoBD(this);

        nvelocidad.umbralVelocidad(this);

        tvAlertaVelocidad = findViewById(R.id.tvAlertaVelocidad);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        solicitarPermisos();



        /////////////////CambioConductor////////////////////
        spinnerConductores = findViewById(R.id.spinnerConductores);
        btnCambiarConductor = findViewById(R.id.btnCambiarConductor);


        ncambio = new NcambioConductor(getApplicationContext());

        listaConductores = ncambio.obtenerConductoresInactivos(); // Interna
        List<String> nombres = ncambio.obtenerNombresConductores(); // Para el Spinner

        if (nombres.isEmpty()) {
            Toast.makeText(this, "No hay conductores inactivos registrados", Toast.LENGTH_LONG).show();
            btnCambiarConductor.setEnabled(false);
            btnCambiarConductor.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerConductores.setAdapter(adapter);
        }

        btnCambiarConductor.setOnClickListener(v -> {
            int pos = spinnerConductores.getSelectedItemPosition();
            if (pos != Spinner.INVALID_POSITION && !listaConductores.isEmpty()) {
                int idSeleccionado = listaConductores.get(pos).getId();
                boolean exito = ncambio.activarConductor(idSeleccionado);
                if (exito) {
                    Toast.makeText(this, "Conductor cambiado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error al cambiar de conductor", Toast.LENGTH_SHORT).show();
                }
            }
        });

        verificarUbicacion();


        /////////////////Somnolencia////////////////////
        previewView = findViewById(R.id.previewView);
        nsomnolencia = new Nsomnolencia(this);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        setupFaceDetector();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CAMERA_PERMISSION);
        }


        /////////////////Ruta////////////////////
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

        // Botón para finalizar
        btnFinalizar = findViewById(R.id.btnFinViaje);

        // Llama a tu función de verificar ubicaciones
        nruta.verificarUbicacion(this, (posicion, distanciaParada, distanciaDestino) -> {
            this.distanciaDestino = distanciaDestino;
            // Si la distancia al destino es menor a 50 metros, muestra el botón "Finalizar"
            if (distanciaDestino < 50) {
                btnFinalizar.setVisibility(View.VISIBLE);
            } else {
                btnFinalizar.setVisibility(View.GONE);
            }
        });

        btnFinalizar.setOnClickListener(v -> {
            // Redirige al MainActivity o la actividad que necesites
            if (distanciaDestino < 50) {
                Intent intentFin = new Intent(this, PfinViaje.class);
                startActivity(intentFin);
                finish();
            } else {
                Toast.makeText(this, "Debes estar cerca del destino para finalizar el viaje.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /////////////////Velocidad////////////////////
    private void solicitarPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION2);
        } else {
            iniciarMedicion();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void iniciarMedicion() {
        Log.d("Pvelocidad", "Iniciando medición de ubicación...");
        LocationRequest request = new LocationRequest.Builder(1000)
                .setMinUpdateIntervalMillis(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            if (location != null) {
                float velocidadKmh = location.getSpeed() * 3.6f;
                tvVelocidad.setText(String.format("Velocidad: %d km/h", Math.round(velocidadKmh)));

                nvelocidad.procesarVelocidad(location);

                if (velocidadKmh > nvelocidad.getUmbralVelocidad()) {
                    tvAlertaVelocidad.setVisibility(View.VISIBLE);
                } else {
                    tvAlertaVelocidad.setVisibility(View.GONE);
                }
                Log.d("Pvelocidad", "Medición: Velocidad: " + Math.round(velocidadKmh) + " km/h");
            }
        }
    };



    /////////////////CambioConductor////////////////////
    private void verificarUbicacion() {
        ncambio.verificarUbicacion(this, cerca -> {
            if (cerca) {
                btnCambiarConductor.setEnabled(true);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde));
                spinnerConductores.setVisibility(View.VISIBLE);
                actualizarSpinner();
            } else {
                btnCambiarConductor.setEnabled(false);
                btnCambiarConductor.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gris));
                spinnerConductores.setVisibility(View.GONE);
            }
        });
    }

    private void actualizarSpinner() {
        listaConductores = ncambio.obtenerConductoresInactivos();
        List<String> nombres = ncambio.obtenerNombresConductores();

        if (nombres.isEmpty()) {
            Toast.makeText(this, "No hay conductores inactivos registrados", Toast.LENGTH_LONG).show();
            btnCambiarConductor.setEnabled(false);
            btnCambiarConductor.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerConductores.setAdapter(adapter);
        }
    }


    /////////////////Somnolencia////////////////////
    private void setupFaceDetector() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();

        detector = FaceDetection.getClient(options);
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                Executor executor = ContextCompat.getMainExecutor(this);
                analysis.setAnalyzer(executor, this::procesarImagen);

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void procesarImagen(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Permiso", "Permiso de ubicación no otorgado. Evento no registrado.");
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    nsomnolencia.evaluarSomnolencia(this, faces);
                    imageProxy.close();
                })
                .addOnFailureListener(e -> imageProxy.close());
    }


    /////////////////Ruta////////////////////
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
                    String.format("Parada: %.2f km", distanciaParada / 1000.0) :
                    String.format("Parada: %.0f m", distanciaParada);

            String textoDestino = (distanciaDestino >= 1000) ?
                    String.format("Destino: %.2f km", distanciaDestino / 1000.0) :
                    String.format("Destino: %.0f m", distanciaDestino);

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
        nruta.detenerMonitoreo();
        ncambio.detenerMonitoreoUbicacion();
        Log.d("Pvelocidad", "Deteniendo medición de ubicación...");
        locationClient.removeLocationUpdates(locationCallback);
    }


}