package pdgt.cat.com.noaahso;

import com.google.android.gms.maps.model.LatLng;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

import pdgt.cat.com.noaahso.BusinessObjects.HaulEvent;
import pdgt.cat.com.noaahso.BusinessObjects.WeatherEvent;


/**
 * Created by smithm24 on 12/7/2015. Accesses sqlite db. Mirrors DataAccess.java class.
 */
public class WeatherDBAccess extends SQLiteAssetHelper
            implements MainActivity.OnNewLocationRaisedListener {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "HSO_033016.sqlite";

    private static final String TABLE_WEATHER = "weather_events";
    private static final String TABLE_ROUTE = "haul_route";

    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_LATITUDE = "latitude";

    private static final String COLUMN_CURRENT_TEMPERATURES = "current_temperatures";
    private static final String COLUMN_CURRENT_WEATHER_DESCRIPTION = "current_weather_description";
    private static final String COLUMN_CURRENT_ICONS =  "current_icons";


    private static final String COLUMN_SYNCHED = "synched"; //synched to azure

    private static final String COLUMN_EVENT_ID = "event_id";

    private static final String COLUMN_SEVERE_WEATHER_PRESENT = "severe_weather_present";
    private static final String COLUMN_SEVERE_WEATHER_DESCRIPTION = "severe_weather_description";
    private static final String COLUMN_SEVERE_WEATHER_START = "severe_weather_start";
    private static final String COLUMN_SEVERE_WEATHER_END = "severe_weather_end";

    private static final String COLUMN_TIME_STAMP = "time_stamp";

    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_LOAD_NUMBER = "l_to_d_num";
    private static final String COLUMN_DUMP_NUMBER = "d_to_l_num";

    private DateTimeFormatter timeStampFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private SQLiteDatabase db = null;

    public static Context _context = null;
    public static WeatherDBAccess _instance = null;


    public static WeatherDBAccess Instance()
    {
        if(_context == null)
            throw new NullPointerException("You must supply the WeatherDBAccess._context prior to calling Instance()");

        if(_instance == null)
            _instance = new WeatherDBAccess(_context);


        return _instance;
    }

    public WeatherDBAccess(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        db = this.getWritableDatabase();
        Log.d("WeatherDBAccess", "Instance Created " + db.getPath());

    }

    @Override
     protected void finalize() throws Throwable {
        try{
            db.close();
        } catch (Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }

    public DateTime GetCurrentTime() {
        // TODO: use global time from radios
        return new DateTime();
    }

    public String GetTimeStamp(DateTime date) { return timeStampFormat.print(date); }

    public void EnsureDBIsOpen(){

        //check to see the DB is open
        if (!db.isOpen())
            db = this.getWritableDatabase();
    }

    //haul route stuff
    public void addTravelData(HaulEvent event) {
        ContentValues values = new ContentValues();

        switch(event.type) {
            case TravelEmpty: {

                values.put(COLUMN_TYPE, "TravelEmpty");
                values.put(COLUMN_LATITUDE, event.Latitude);
                values.put(COLUMN_LONGITUDE, event.Longitude);
                values.put(COLUMN_TIME_STAMP, GetTimeStamp(event.Time));
                values.put(COLUMN_DUMP_NUMBER, event.d_to_l_num);

                event.ID = db.insert(TABLE_ROUTE, null, values);

                break;
            }
            case TravelFull: {
                values.put(COLUMN_TYPE, "TravelFull");
                values.put(COLUMN_LATITUDE, event.Latitude);
                values.put(COLUMN_LONGITUDE, event.Longitude);
                values.put(COLUMN_TIME_STAMP, GetTimeStamp(event.Time));
                values.put(COLUMN_LOAD_NUMBER, event.l_to_d_num);

                event.ID = db.insert(TABLE_ROUTE, null, values);

                break;
            }
            case DeadTravel: {
                break;
            }
        }
    }

    public ArrayList<LatLng> getBaseRoute(int cycle, MainActivity.HaulType type) {

        ArrayList<LatLng> locs = new ArrayList<>();
        String query = "";
        if(type== MainActivity.HaulType.TravelFull){
            query = "Select latitude, longitude from " + TABLE_ROUTE + " WHERE " + COLUMN_LOAD_NUMBER
                    + " = " + cycle;
        }else if(type == MainActivity.HaulType.TravelEmpty){
            query = "Select latitude, longitude from " + TABLE_ROUTE + " WHERE " + COLUMN_DUMP_NUMBER
                    + " = " + cycle;
        }

        Cursor cursor = db.rawQuery(query, null);

        while(cursor.moveToNext()){

            LatLng loc = new LatLng(cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE)));
            // System.out.println("points from db" + loc.toString());
            locs.add(loc);
        }
        cursor.close();
        return locs;
    }

    @Override
    public synchronized void OnNewLocationRaised(HaulEvent event, LatLng latlon ) {
        addTravelData(event);

    }


    //weather stuff
    //add weather event to db
    public void AddWeatherEvent(WeatherEvent wEvent) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, wEvent.Latitude);
        values.put(COLUMN_LONGITUDE, wEvent.Longitude);
        values.put(COLUMN_CURRENT_TEMPERATURES, wEvent.CurrTemp);
        //System.out.println("Current Temp: " + wEvent.CurrTemp);
        values.put(COLUMN_CURRENT_WEATHER_DESCRIPTION, wEvent.CurrDesc);
        values.put(COLUMN_SEVERE_WEATHER_PRESENT, wEvent.SevereWeatherPresent);
        values.put(COLUMN_SEVERE_WEATHER_START, wEvent.SevereWeatherStart);
        values.put(COLUMN_SEVERE_WEATHER_END, wEvent.SevereWeatherEnd);
        values.put(COLUMN_SEVERE_WEATHER_DESCRIPTION, wEvent.SevereWeatherDesc);
        values.put(COLUMN_CURRENT_ICONS, wEvent.CurrIcon);
        values.put(COLUMN_SYNCHED, wEvent.Synched ? 1 : 0);
        values.put(COLUMN_TIME_STAMP, GetTimeStamp(wEvent.Time));
        //todo: time_stamp
        wEvent.ID = db.insert(TABLE_WEATHER, null, values);
        System.out.println("Weather Event Added");

    }
    public void MarkAsSynchedEvent(WeatherEvent wevent)
    {
        wevent.Synched = true;
        ContentValues values = new ContentValues();
        values.put(COLUMN_SYNCHED, 1);
        wevent.ID = db.update(TABLE_WEATHER, values, COLUMN_EVENT_ID + " = " + wevent.ID, null);
       // WeatherAzureAccess.Instance().weatherFinalize();
    }

    public ArrayList<WeatherEvent> GetUnsychedWeatherEvent() {
        String query = COLUMN_SYNCHED + " = 0 ";
        Cursor cursor = db.query(TABLE_WEATHER, null, query, null, null, null, null);

        ArrayList<WeatherEvent> unsychedWeather = new ArrayList<>();
        while (cursor.moveToNext()) {
            unsychedWeather.add( GetWeatherEvent(cursor) );
        }
        cursor.close();

        return unsychedWeather;

    }

    //get weather event from db for azure
    //TODO: finish populating this
    public WeatherEvent GetWeatherEvent(Cursor cursor) {
        WeatherEvent wEvent = new WeatherEvent();
        wEvent.Latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE));
        wEvent.Longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE));
        wEvent.CurrTemp = cursor.getString(cursor.getColumnIndex(COLUMN_CURRENT_TEMPERATURES));
        wEvent.CurrDesc = cursor.getString(cursor.getColumnIndex(COLUMN_CURRENT_WEATHER_DESCRIPTION));
        //System.out.println("From DB, current temp: " + wEvent.CurrTemp);
        try {
            wEvent.Time = timeStampFormat.parseDateTime(cursor.getString(cursor.getColumnIndex(COLUMN_TIME_STAMP)));
        } catch (Exception e) {
            Log.d("GetEvent()", "Error parsing date " + e.toString());
        }

        return wEvent;
    }

    public ArrayList<WeatherEvent> DailyWeatherEvents() {
            Log.d("DB Access", "got to dailyweatherevents");
        ArrayList<WeatherEvent> dailyWeatherEvents = new ArrayList<>();

        Cursor dailycursor = db.rawQuery("SELECT\n" +
                "avg_temp,\n" +
                COLUMN_CURRENT_WEATHER_DESCRIPTION + ", " +
                COLUMN_CURRENT_ICONS + ", hour, " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE +
                ",sum_severe\n" +
                "FROM (\n" +
                "SELECT\n" +
                "hour,\n" +
                "SUM(total_temp) / SUM(event_count) AS avg_temp,\n" +
               COLUMN_CURRENT_WEATHER_DESCRIPTION + ", " +
                COLUMN_CURRENT_ICONS + ", " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + ", \n" +
                " MAX(event_count) AS max_event_count,\n" +
                "\t  SUM(severe_weather) as sum_severe\n" +
                "FROM (\n" +
                "SELECT\n" +
                "strftime('%H', " + COLUMN_TIME_STAMP + ", '+30 minutes') AS hour,\n" +
                COLUMN_CURRENT_WEATHER_DESCRIPTION + ", \n" +
                COLUMN_CURRENT_ICONS + ", " + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + ", \n" +
                "COUNT(*) AS event_count,\n" +
                "SUM(" + COLUMN_CURRENT_TEMPERATURES + ") AS total_temp,\n" +
                "\t\t  SUM(" + COLUMN_SEVERE_WEATHER_PRESENT + ") AS severe_weather\n" +
                "FROM " + TABLE_WEATHER + "\n" +
                "\t\t  WHERE " + COLUMN_TIME_STAMP + "  < datetime('now') AND " + COLUMN_TIME_STAMP+
                " > datetime('now', 'start of day')  \n AND (" + COLUMN_LATITUDE + " > 0 OR " +
                COLUMN_LONGITUDE + " > 0 )\n" +
                "GROUP BY strftime('%H', " + COLUMN_TIME_STAMP + ", '+30 minutes'),\n" +
                COLUMN_CURRENT_WEATHER_DESCRIPTION +
                ") AS s\n" +
                "GROUP BY hour) AS t\n" +
                "ORDER BY hour", null);


        try {
            if (dailycursor.moveToFirst()) {
                while (dailycursor.moveToNext()) {
                    dailyWeatherEvents.add(GetDailyWeatherEvent(dailycursor));

                }
                dailycursor.close();

            }
            } catch (Exception e) {
            e.printStackTrace();
        }
        return dailyWeatherEvents;
    }

    public WeatherEvent GetDailyWeatherEvent(Cursor dailycursor) {
        WeatherEvent wEvent = new WeatherEvent();
        wEvent.Latitude = dailycursor.getDouble(dailycursor.getColumnIndex(COLUMN_LATITUDE));
        wEvent.Longitude = dailycursor.getDouble(dailycursor.getColumnIndex(COLUMN_LONGITUDE));
        wEvent.CurrTemp = dailycursor.getString(dailycursor.getColumnIndex("avg_temp"));
        //System.out.println(wEvent.CurrTemp + " mariah this comes from dailycursor");
       wEvent.CurrDesc = dailycursor.getString(dailycursor.getColumnIndex(COLUMN_CURRENT_WEATHER_DESCRIPTION));
        wEvent.Hour = dailycursor.getInt(dailycursor.getColumnIndex("hour"));
        System.out.println(Integer.toString(wEvent.Hour) + " From GetDailyWeatherEvent()");
        wEvent.CurrIcon = dailycursor.getString(dailycursor.getColumnIndex(COLUMN_CURRENT_ICONS));
        wEvent.SevereWeatherPresent = dailycursor.getInt(dailycursor.getColumnIndex("sum_severe"))>0;

        return wEvent;

    }


}
