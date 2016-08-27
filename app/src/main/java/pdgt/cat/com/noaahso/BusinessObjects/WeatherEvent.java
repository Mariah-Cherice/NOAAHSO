package pdgt.cat.com.noaahso.BusinessObjects;


import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.joda.time.DateTime;

import java.util.Date;

import pdgt.cat.com.noaahso.MainActivity;
import pdgt.cat.com.noaahso.WeatherAzureAccess;


/**
 * Created by smithm24 on 12/3/2015. Business Object for weather event. Mirrors standard event
 * business object. Note: Forecast data not included becuase there's no reason to store it.
 * Forecast values are purely a user interface feature.
 */
public class WeatherEvent implements FutureCallback<JsonObject> {

    public double Latitude = 0.0;
    public double Longitude = 0.0;

    //current temp data
    public String CurrTemp = null;
    public String CurrDesc = null;
    public String CurrIcon = null;
    public boolean Synched = false;
    public boolean SevereWeatherPresent = false;

    public long ID = -1; //row number in local sql database

    //severe weather data if applicable
    public JsonElement SevereWeatherData;
    public String SevereWeatherStart = null;
    public String SevereWeatherEnd = null;
    public String SevereWeatherDesc = null;

    public int Hour;
    public DateTime Time;



    public WeatherEvent() {


    }

    @Override
    public void onSuccess(JsonObject result) {
        //Synched = true;
        WeatherAzureAccess.Instance().WeatherEventSyncSuccess(this, result);
    }

    @Override
    public void onFailure(Throwable t) {
        WeatherAzureAccess.Instance().WeatherEventSyncFailure(this, t);

    }
}
