package com.rogger.bipando.ui.commun;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
import com.rogger.bipando.model.RegistroData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SqlHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bipandoSavedData.db";
    private static final int DB_VERSION = 1;
    public static SqlHelper INSTACIA;
    private final String tableName = "bipandoTable";
    private final String Id = "id";
    private final String NAME = "name";
    private final String EMAIL = "email";
    private final String PROFILE_IMAGE_URI = "uri";
    private final String DateCreated = "created_date";

    public static SqlHelper getInstacia(Context context) {
        if (INSTACIA != null) {
            INSTACIA = new SqlHelper(context);
        }
        return INSTACIA;
    }

    public SqlHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + tableName + " ( id INTEGER primary key," +
                NAME + " TEXT," +
                EMAIL + "TEXT," +
                PROFILE_IMAGE_URI + "TEXT," +
                "" + DateCreated + " DATETIME);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.d("teste", "on Upgred disparado");
    }

    //Buscando dados
    @SuppressLint("Range")
    public List<RegistroData> getRegisterBy() {
        List<RegistroData> register = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName + " ORDER BY id DESC", new String[]{});
        try {
            if (cursor.moveToFirst()) {
                do {
                    RegistroData registro = new RegistroData();
                    registro.id = cursor.getInt(cursor.getColumnIndex(Id));
                    registro.name = cursor.getString(cursor.getColumnIndex(NAME));
                    //registro.day = cursor.getInt(cursor.getColumnIndex(Day));
                    //registro.valor = cursor.getString(cursor.getColumnIndex(Valor));
                    registro.createdate = cursor.getString(cursor.getColumnIndex(DateCreated));
                    register.add(registro);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SQLite", e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return register;
    }
    @SuppressLint("Range")
    public List<RegistroData> getUser() {
        List<RegistroData> register = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName + " ORDER BY id DESC", new String[]{});
        try {
            if (cursor.moveToFirst()) {
                    RegistroData registro = new RegistroData();
                    registro.id = cursor.getInt(cursor.getColumnIndex(Id));
                    registro.name = cursor.getString(cursor.getColumnIndex(NAME));
                    registro.profileImgUri = cursor.getString(cursor.getColumnIndex(PROFILE_IMAGE_URI));
                    register.add(registro);
            }
        } catch (Exception e) {
            Log.e("SQLite", e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return register;

    }
    public long addUser(String name,String imgProfileUri){
        SQLiteDatabase dd = this.getWritableDatabase();
        long calcId = 0;
        try {
            dd.beginTransaction();
            ContentValues values = new ContentValues();
            if(name != null){
                values.put(NAME,name );
            }
            if(imgProfileUri != null){
                values.put(PROFILE_IMAGE_URI,imgProfileUri);
            }
            calcId = dd.insertOrThrow(tableName, null, values);
            dd.setTransactionSuccessful();

        } catch (Exception ignored) {
        } finally {
            if (dd.isOpen()) {
                dd.endTransaction();
            }
        }
        return calcId;
    }
    // ADD DADOS...
    public long additem(String name, String valor, int day) {
        SQLiteDatabase dd = this.getWritableDatabase();
        long calcId = 0;
        try {
            dd.beginTransaction();
            ContentValues values = new ContentValues();

            values.put("name", name);
            values.put("valor", valor);
            values.put("day", day);

            Date dat = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String now = sdf.format(dat);

            values.put("created_date", now);
            calcId = dd.insertOrThrow("calc", null, values);

            dd.setTransactionSuccessful();

        } catch (Exception e) {
            //Toast.makeText(,"Error aqui"+ e, Toast.LENGTH_LONG).show();
        } finally {
            if (dd.isOpen()) {
                dd.endTransaction();
            }
        }
        return calcId;
    }

    // ATUALIZANDO DADOS........
    public long updateItem(String type, String response, int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long calcId = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(NAME, type);

            @SuppressLint("SimpleDateFormat")

            String now = new SimpleDateFormat("yyyy-MM-dd ").format(Calendar.getInstance().getTime());
            values.put(NAME, now);
            calcId = db.update(tableName, values, "id = ? and " + NAME + " = ?", new String[]{String.valueOf(id), type});
            db.setTransactionSuccessful();

        } catch (Exception e) {
            //Toast.makeText(null, e.toString(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
        return calcId;
    }

    //REMOVER DADOS....
    public long removeItem(String name, int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long calcId = 0;
        try {
            calcId = db.delete(tableName, "" + Id + " = ? and " + NAME + " = ?", new String[]{String.valueOf(id), name});
            db.setTransactionSuccessful();

        } catch (Exception e) {
            //Toast.makeText(null, e.toString(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
        return calcId;
    }

}
