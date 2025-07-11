package com.grupo12.securitysystemmovil.movilBD;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class movilBD extends SQLiteOpenHelper{


    private static final String DATABASE_NAME = "securitysystem.db";
    private static final int DATABASE_VERSION = 1;

    public movilBD(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE eventos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mensaje TEXT NOT NULL," +
                "tipo TEXT NOT NULL," +
                "nivel TEXT NOT NULL," +
                "fecha TEXT NOT NULL," +
                "hora TEXT NOT NULL," +
                "latitud REAL NOT NULL, " +
                "longitud REAL NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "vehicle_id INTEGER NOT NULL," +
                "trip_id INTEGER NOT NULL," +
                "enviado INTEGER NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE vehiculo (" +
                "id INTEGER PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "placa TEXT NOT NULL," +
                "velocidad_maxima INTEGER NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE conductores (" +
                "id INTEGER PRIMARY KEY," +
                "ci TEXT NOT NULL," +
                "nombre TEXT NOT NULL," +
                "apellido TEXT NOT NULL," +
                "rol INTEGER NOT NULL," +
                "ruta_imagen TEXT NOT NULL," +
                "activo INTEGER NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE gestor (" +
                "id INTEGER PRIMARY KEY," +
                "ci TEXT NOT NULL," +
                "nombre TEXT NOT NULL," +
                "apellido TEXT NOT NULL," +
                "rol INTEGER NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE viaje (" +
                "id INTEGER PRIMARY KEY," +
                "fecha_inicio TEXT NOT NULL," +
                "hora_inicio TEXT NOT NULL," +
                "vehicle_id INTEGER NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE ruta (" +
                "id INTEGER PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "origen_lat TEXT NOT NULL," +
                "origen_lng TEXT NOT NULL," +
                "destino_lat TEXT NOT NULL," +
                "destino_lng TEXT NOT NULL);";
        db.execSQL(createTable);

        createTable = "CREATE TABLE paradas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT NOT NULL," +
                "latitud TEXT NOT NULL," +
                "longitud TEXT NOT NULL," +
                "posicion INTEGER);";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS eventos");
        db.execSQL("DROP TABLE IF EXISTS vehiculo");
        db.execSQL("DROP TABLE IF EXISTS conductores");
        db.execSQL("DROP TABLE IF EXISTS gestor");
        db.execSQL("DROP TABLE IF EXISTS viaje");
        db.execSQL("DROP TABLE IF EXISTS ruta");
        db.execSQL("DROP TABLE IF EXISTS paradas");
        onCreate(db);
    }
}

