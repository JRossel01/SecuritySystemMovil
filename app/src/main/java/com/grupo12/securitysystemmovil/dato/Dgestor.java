package com.grupo12.securitysystemmovil.dato;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.grupo12.securitysystemmovil.movilBD.movilBD;

public class Dgestor {

    private SQLiteDatabase db;

    public Dgestor(Context context) {
        this.db = new movilBD(context).getWritableDatabase();
    }

    public boolean guardarGestor(int id, String ci, String nombre, String apellido, int rol) {
        try {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("ci", ci);
            values.put("nombre", nombre);
            values.put("apellido", apellido);
            values.put("rol", rol);

            db.insert("gestor", null, values);
            db.close();

            return true;
        } catch (Exception e) {
            Log.e("Dgestor", "Error al guardar gestor", e);
            return false;
        }
    }

}
