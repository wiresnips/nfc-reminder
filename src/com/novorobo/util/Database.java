package com.novorobo.util.database;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import android.util.Log;



public class Database extends SQLiteOpenHelper {
    private static final Class<?> StringType = "String".getClass();


    public interface Entry {
        long getID ();
        void setID (long id);

        void setValues (Cursor cursor);
        ContentValues getValues();


        // this is a SCRATCH CLASS, meant for quickly assembling working prototypes
        // it uses a LOT of reflection to acheive what could otherwise have been hardcoded

        public class Auto implements Entry {
            protected long _id;

            public long getID () {
                return _id;
            }

            public void setID (long id) {
                _id = id;
            }

            public Bundle bundle () {
                Bundle values = new Bundle();
                Class<?> clazz = this.getClass();

                for (Field field : getAllInstanceFields(clazz) ) 
                    readField(field, values);
                return values;
            }

            public ContentValues getValues () {
                ContentValues values = new ContentValues();
                Class<?> clazz = this.getClass();

                for (Field field : getAllInstanceFields(clazz) ) 
                    readField(field, values);
                return values;
            }

            public void setValues (Cursor values) {
                Class<?> clazz = this.getClass();
                for (Field field : getAllInstanceFields(clazz) )
                    writeField(field, values);
            }

            public void setValues (Bundle values) {
                Class<?> clazz = this.getClass();
                for (Field field : getAllInstanceFields(clazz) )
                    writeField(field, values);
            }


            private void readField (Field field, ContentValues values) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                String name = field.getName();

                try {
                    if      (type == Boolean.TYPE)   values.put( name, field.getBoolean(this) );
                    else if (type == Byte.TYPE)      values.put( name, field.getByte(this) );
                    else if (type == Short.TYPE)     values.put( name, field.getShort(this) );
                    else if (type == Integer.TYPE)   values.put( name, field.getInt(this) );
                    else if (type == Long.TYPE)      values.put( name, field.getLong(this) );
                    else if (type == Float.TYPE)     values.put( name, field.getFloat(this) );
                    else if (type == Double.TYPE)    values.put( name, field.getDouble(this) );
                    else if (type == Character.TYPE) values.put( name, field.getChar(this) +"" );
                    else if (type == StringType)     values.put( name, (String) field.get(this) );
                    
                    else if (type.isEnum()) {
                        Enum e = (Enum) field.get(this);
                        if (e != null)
                            values.put( name, e.name() );
                    }
                } 
                catch (IllegalArgumentException e) {} 
                catch (IllegalAccessException e) {}
            }

            private void writeField (Field field, Cursor cursor) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                String name = field.getName();
                Object value = null;

                int i = cursor.getColumnIndex(name);
                if (i == -1)
                    return;

                if      (type == Boolean.TYPE)   value = (cursor.getInt(i) != 0);
                else if (type == Byte.TYPE)      value = (byte) cursor.getInt(i);
                else if (type == Short.TYPE)     value = cursor.getShort(i);
                else if (type == Integer.TYPE)   value = cursor.getInt(i);
                else if (type == Long.TYPE)      value = cursor.getLong(i);
                else if (type == Float.TYPE)     value = cursor.getFloat(i);
                else if (type == Double.TYPE)    value = cursor.getDouble(i);
                else if (type == StringType)     value = cursor.getString(i);
                else if (type == Character.TYPE) value = cursor.getString(i).charAt(0);

                // enums are a bit fiddly 
                else if (type.isEnum()) {
                    String enumConst = cursor.getString(i);
                    if (enumContains( (Class<? extends Enum>) type, enumConst ))
                         value = Enum.valueOf( (Class<? extends Enum>) type, enumConst );
                }

                try { 
                    field.set( this, value ); 
                } catch (IllegalAccessException e) {}
            }

            private void readField (Field field, Bundle values) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                String name = field.getName();

                try {
                    if      (type == Boolean.TYPE)   values.putBoolean( name, field.getBoolean(this) );
                    else if (type == Byte.TYPE)      values.putByte(    name, field.getByte(this) );
                    else if (type == Short.TYPE)     values.putShort(   name, field.getShort(this) );
                    else if (type == Integer.TYPE)   values.putInt(     name, field.getInt(this) );
                    else if (type == Long.TYPE)      values.putLong(    name, field.getLong(this) );
                    else if (type == Float.TYPE)     values.putFloat(   name, field.getFloat(this) );
                    else if (type == Double.TYPE)    values.putDouble(  name, field.getDouble(this) );
                    else if (type == Character.TYPE) values.putChar(    name, field.getChar(this) );
                    else if (type == StringType)     values.putString(  name, (String) field.get(this) );
                    
                    else if (type.isEnum()) {
                        Enum e = (Enum) field.get(this);
                        if (e != null)
                            values.putString( name, e.name() );
                    }
                } 
                catch (IllegalArgumentException e) {} 
                catch (IllegalAccessException e) {}
            }


            private void writeField (Field field, Bundle bundle) {
                field.setAccessible(true);
                Class<?> type = field.getType();
                String name = field.getName();
                Object value = null;

                if      (type == Boolean.TYPE)   value = bundle.getBoolean(name);
                else if (type == Byte.TYPE)      value = bundle.getByte(name);
                else if (type == Short.TYPE)     value = bundle.getShort(name);
                else if (type == Integer.TYPE)   value = bundle.getInt(name);
                else if (type == Long.TYPE)      value = bundle.getLong(name);
                else if (type == Float.TYPE)     value = bundle.getFloat(name);
                else if (type == Double.TYPE)    value = bundle.getDouble(name);
                else if (type == Character.TYPE) value = bundle.getChar(name);
                else if (type == StringType)     value = bundle.getString(name);

                // enums are a bit fiddly 
                else if (type.isEnum()) {
                    String enumConst = bundle.getString(name);
                    if (enumContains( (Class<? extends Enum>) type, enumConst ))
                         value = Enum.valueOf( (Class<? extends Enum>) type, enumConst );
                }

                try { 
                    field.set( this, value ); 
                } catch (IllegalAccessException e) {}
            }



            // honestly, I don't know why this isn't built into Enum
            private static <E extends Enum<E>> boolean enumContains (Class<E> enumeration, String name) {
                for (E enumConst : enumeration.getEnumConstants()) 
                    if (enumConst.name().equals(name)) 
                        return true;
                return false;
            }

        } // Auto
    } // Entry




    public static class TableSchema {
        private static Map<Class<?>, TableSchema> schemas = new HashMap<Class<?>, TableSchema>();

        public static final String ID_TYPE_DECLARATION = "integer primary key autoincrement not null";
        private static final String COLUMN_NAMES_FIELD = "COLUMN_NAMES";
        private static final String COLUMN_TYPES_FIELD = "COLUMN_TYPES";

        /*
        You can avoid the reflective auto-columns by providing a column-declaration as follows:
        public static final String[] COLUMN_NAMES = new String[] { "_id", "column_1" }
        public static final String[] COLUMN_TYPES = new String[] { Database.TableSchema.ID_TYPE_DECLARATION, "integer"|"real"|"text" }
        */

        public static TableSchema getFor (Class<?> clazz) {
            TableSchema schema = schemas.get(clazz);

            if (schema == null) {
                //Map<String, String> columns;
                String[] columnNames, columnTypes;

                // try to grab an existing column-declaraction. If there is none, fall back on reflection
                try {
                    columnNames = (String[]) clazz.getField(COLUMN_NAMES_FIELD).get(null);
                    columnTypes = (String[]) clazz.getField(COLUMN_TYPES_FIELD).get(null);
                } 

                // so, we fall back on reflection
                catch (Exception e) {
                    Map<String, String> columns = describeColumns(clazz);

                    columnNames = new String[ columns.size() ];
                    columnTypes = new String[ columns.size() ];
                    int count = 0;

                    for (Map.Entry<String, String> entry : columns.entrySet()) {
                        columnNames[count] = entry.getKey();
                        columnTypes[count] = entry.getValue();
                        count++;
                    }
                }

                schema = new TableSchema( clazz.getSimpleName(), columnNames, columnTypes );
                schemas.put( clazz, schema );
            }

            return schema;
        }


        public final String NAME;
        public final String[] COLUMN_NAMES;
        public final String[] COLUMN_TYPES;
        public final String CREATE;

        private TableSchema (String name, String[] columnNames, String[] columnTypes) {
            NAME = name;
            COLUMN_NAMES = columnNames;
            COLUMN_TYPES = columnTypes;
            CREATE = getCreateCommand();
        }

        private String getCreateCommand () {
            String fields = "";

            for (int i = 0; i < COLUMN_NAMES.length; i++)
                fields += ", " + COLUMN_NAMES[i] + " " + COLUMN_TYPES[i];

            if (fields.length() > 0)
                fields = fields.substring(2) ; // remove the last ", "

            return "create table if not exists '" + NAME + "' (" + fields + ")";
        }

        private static Map<String, String> describeColumns (Class<?> clazz) {
            Map<String, String> columns = new HashMap<String, String>();

            for (Field field : getAllInstanceFields(clazz) ) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                if (fieldName.equals(BaseColumns._ID)) {
                    columns.put( fieldName, ID_TYPE_DECLARATION );
                    continue;
                }

                // find a compatible type, if we can
                for (Map.Entry<Class<?>, String> mapping : FIELD_TYPE_MAP.entrySet()) {
                    if (mapping.getKey().isAssignableFrom(fieldType)) {
                        columns.put( fieldName, mapping.getValue() );
                        break;
                    }
                }
            }
            return columns;
        }

        private static final Map<Class<?>, String> FIELD_TYPE_MAP = new HashMap<Class<?>, String>() {{
            put(Boolean.TYPE,   "integer");
            put(Byte.TYPE,      "integer");
            put(Short.TYPE,     "integer");
            put(Integer.TYPE,   "integer");
            put(Long.TYPE,      "integer");
            put(Float.TYPE,     "real");
            put(Double.TYPE,    "real");
            put(Character.TYPE, "text");
            put(StringType,     "text");
            put(Enum.class,     "text");
        }};
    } // TableSchema


    // stupid helper method
    private static List<Field> getAllInstanceFields (Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();

        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                int f = field.getModifiers();
                if (!Modifier.isStatic(f) && !Modifier.isFinal(f))
                    fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }




    public static final String NAME = "database";   // if you want more than one db, write your own damn helpers.
    public static final int VERSION = 1;            // less obviously a good idea: I'm ditching versions/migrations.

    private static Context context = null;
    public static void init (Context ctx) {
        context = ctx.getApplicationContext();
    }

    private static Database instance = null;
    public static Database getInstance () {
        if (instance == null)
            instance = new Database();
        return instance;
    }

    private Database () {
        super( context, NAME, null, VERSION );
    }

    // neither of these is relevant to the usage model I envision
    public void onCreate (SQLiteDatabase db) {}
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {}




    private static void execSQL (String sql) {
        getInstance().getWritableDatabase().execSQL(sql);
    }

    private static boolean hasTable (String table) {
        SQLiteDatabase db = getInstance().getReadableDatabase();
        Cursor cursor = db.rawQuery( "select distinct tbl_name from sqlite_master where tbl_name = '" + table + "'", null);
        boolean foundTable = cursor.getCount() > 0;
        cursor.close();
        return foundTable;
    }


    private static Set<Class<?>> initializedTables = new HashSet<Class<?>>();

    public static void initTable (Class<?> clazz) {

        if (initializedTables.contains(clazz))
            return;
        initializedTables.add(clazz);

        TableSchema schema = TableSchema.getFor(clazz);


        if (!hasTable(schema.NAME))
            execSQL(schema.CREATE);

        else {
            SQLiteDatabase db = getInstance().getReadableDatabase();
            Cursor cursor = db.query(schema.NAME, null, null, null, null, null, null);

            ArrayList<String> columns = new ArrayList<String>();
            for (int i = 0; i < cursor.getColumnCount(); i++)
                columns.add( cursor.getColumnName(i) );

            cursor.close();

            for (int i = 0; i < schema.COLUMN_NAMES.length; i++)
                if (!columns.contains(schema.COLUMN_NAMES[i]))
                    execSQL( "alter table '" + schema.NAME + "' add column '" + schema.COLUMN_NAMES[i] + "' " + schema.COLUMN_TYPES[i] );
        }
    }






    public static <E extends Entry> List<E> get (Class<E> clazz, String selection, String[] selectionArgs, String order, String limit) {
        initTable(clazz);

        SQLiteDatabase db = getInstance().getReadableDatabase();
        Cursor cursor = db.query( TableSchema.getFor(clazz).NAME, null, selection, selectionArgs, null, null, order, limit );

        ArrayList<E> entries = new ArrayList<E>(cursor.getCount());

        try {
            while (cursor.moveToNext()) {
                E entry = clazz.newInstance();
                entry.setValues( cursor );
                entries.add( entry );
            }
        } 
        catch (IllegalAccessException e) {}
        catch (InstantiationException e) {
            throw new RuntimeException(
                "Unable to instantiate Entry " + clazz.getName() + 
                ": make sure that it has a public, empty constructor.");
        }

        cursor.close();
        return entries;
    }

    public static <E extends Entry> List<E> get (Class<E> clazz, String selection, String[] selectionArgs, String order, int limit) {
        return get(clazz, selection, selectionArgs, order, ""+limit);
    }

    public static <E extends Entry> List<E> get (Class<E> clazz, String selection, String[] selectionArgs, String order) {
        return get(clazz, selection, selectionArgs, order, null);
    }

    public static <E extends Entry> List<E> get (Class<E> clazz, String selection, String[] selectionArgs) {
        return get(clazz, selection, selectionArgs, null, null);
    }

    public static <E extends Entry> List<E> get (Class<E> clazz) {
        return get(clazz, null, null, null, null);
    }

    public static <E extends Entry> E get (Class<E> clazz, long id) {
        List<E> entries = get( clazz, BaseColumns._ID + " = ?", new String[] {"" + id}, null, null );
        if (entries.size() == 1)
            return entries.get(0);
        return null;
    }



    public static <E extends Entry> void put (E entry) {
        initTable(entry.getClass());

        TableSchema table = TableSchema.getFor(entry.getClass());
        SQLiteDatabase db = getInstance().getWritableDatabase();

        long id = entry.getID();
        ContentValues values = entry.getValues();

        if (id != 0)
            db.update( table.NAME, values, BaseColumns._ID + " = ?", new String[] {"" + id} );

        else {
            if (values.containsKey(BaseColumns._ID))
                values.putNull( BaseColumns._ID );

            entry.setID( db.insert(table.NAME, null, values) );
        }
    }


    public static <E extends Entry> void delete (Class<E> clazz, String where, String[] args) {
        initTable(clazz);
        TableSchema table = TableSchema.getFor(clazz);
        SQLiteDatabase db = getInstance().getWritableDatabase();
        db.delete( table.NAME, where, args );
    }

    public static <E extends Entry> void delete (Class<E> clazz, long id) {
        delete( clazz, BaseColumns._ID + " = ?", new String[] {"" + id } );
    }

    public static <E extends Entry> void delete (E entry) {
        delete( entry.getClass(), BaseColumns._ID + " = ?", new String[] {"" + entry.getID() } );
    }



    public static <E extends Entry> int count (Class<E> clazz, String where, String[] args) {
        initTable(clazz);
        SQLiteDatabase db = getInstance().getReadableDatabase();

        TableSchema table = TableSchema.getFor(clazz);
        String[] select = new String[] { "count(" + BaseColumns._ID + ")" };
        Cursor cursor = db.query( table.NAME, select, where, args, null, null, null );

        cursor.moveToFirst();
        int entryCount = cursor.getInt(0);
        cursor.close();

        return entryCount;
    }


    public static <E extends Entry> int count (Class<E> clazz) {
        return count(clazz, null, null);
    }

}
