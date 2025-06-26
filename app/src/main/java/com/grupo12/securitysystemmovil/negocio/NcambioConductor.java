package com.grupo12.securitysystemmovil.negocio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.grupo12.securitysystemmovil.dato.DcambioConductor;
import com.grupo12.securitysystemmovil.dato.Dconductor;

import java.util.ArrayList;
import java.util.List;

public class NcambioConductor {
    private DcambioConductor dc;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedClient;

    public NcambioConductor(Context context) {
        this.dc = new DcambioConductor(context);
    }

    public interface Callback {
        void onResultado(boolean cerca);
    }

    public List<Dconductor> obtenerConductoresInactivos() {
        return dc.obtenerConductoresInactivos();
    }

    public boolean activarConductor(int idNuevo) {
        Dconductor antiguo = dc.obtenerConductorActivo();
        Dconductor nuevo = null;

        for (Dconductor c : dc.obtenerConductoresInactivos()) {
            if (c.getId() == idNuevo) {
                nuevo = c;
                break;
            }
        }

        if (nuevo == null) return false;

        boolean exito = dc.activarConductor(idNuevo);

        if (exito && antiguo != null) {
            String mensaje = "Cambio de conductor, sale " + antiguo.getNombre() + " " + antiguo.getApellido()
                    + " por " + nuevo.getNombre() + " " + nuevo.getApellido();
            dc.registrarEvento(mensaje);
        }

        return exito;
    }

    public List<String> obtenerNombresConductores() {
        List<Dconductor> lista = dc.obtenerConductoresInactivos();
        List<String> nombres = new ArrayList<>();
        for (Dconductor c : lista) {
            nombres.add(c.getNombre() + " " + c.getApellido());
        }
        return nombres;
    }

    public boolean verificarParada(double lat, double lng) {
        List<LatLng> paradas = dc.obtenerParadasComoLatLng();
        for (LatLng parada : paradas) {
            float[] distancia = new float[1];
            Location.distanceBetween(lat, lng, parada.latitude, parada.longitude, distancia);
            if (distancia[0] < 50) return true;
        }
        return false;
    }

    public void verificarUbicacion(Context context, Callback callback) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context);

        LocationRequest request = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000); // Cada 2 segundos

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    boolean cerca = verificarParada(lat, lon);
                    callback.onResultado(cerca);
                } else {
                    callback.onResultado(false);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(request, locationCallback, null);
        }
    }

    public void detenerMonitoreoUbicacion() {
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

}
