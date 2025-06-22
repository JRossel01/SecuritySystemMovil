package com.grupo12.securitysystemmovil.negocio;

import android.content.Context;

import com.grupo12.securitysystemmovil.dato.DbotonEmergencia;

public class NbotonEmergencia {
    private final Context context;

    public NbotonEmergencia(Context context) {
        this.context = context;
    }

    public void registrarEvento() {
        String mensaje = "Botón de emergencia activado";
        String tipo = "Emergencia";
        String nivel = "Critico";

        DbotonEmergencia devento = new DbotonEmergencia(context);
        devento.registrarEvento(mensaje, tipo, nivel);
    }
}
