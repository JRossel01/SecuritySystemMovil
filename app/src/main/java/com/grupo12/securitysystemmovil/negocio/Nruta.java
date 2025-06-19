package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.grupo12.securitysystemmovil.dato.Druta;

import java.util.ArrayList;
import java.util.List;

public class Nruta {

    private Druta druta;

    public Nruta(Context context) {
        druta = new Druta(context);
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

    public void crearEventoSalida(String ubicacion, double lat, double lon, int numeroParada, boolean esOrigen) {
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

}
