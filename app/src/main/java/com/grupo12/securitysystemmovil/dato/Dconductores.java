package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.grupo12.securitysystemmovil.movilBD.movilBD;

public class Dconductores {
    private final movilBD dbHelper;

    public Dconductores(Context context) {
        dbHelper = new movilBD(context);
    }

    public boolean guardarConductor(String ci, String nombre, String apellido, int rol, String rutaImagen, int activo) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("ci", ci);
            values.put("nombre", nombre);
            values.put("apellido", apellido);
            values.put("rol", rol);
            values.put("ruta_imagen", rutaImagen);
            values.put("activo", activo);

            long resultado = db.insert("conductores", null, values);
            db.close();

            return resultado != -1;
        } catch (Exception e) {
            Log.e("Dconductores", "Error al guardar conductor: " + e.getMessage());
            return false;
        }
    }
}
