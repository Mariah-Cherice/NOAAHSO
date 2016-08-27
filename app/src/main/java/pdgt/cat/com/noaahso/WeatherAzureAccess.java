package pdgt.cat.com.noaahso;

/**
 * Created by smithm24 on 12/3/2015.
 */

import pdgt.cat.com.noaahso.BusinessObjects.WeatherEvent;

import android.content.Context;
import android.util.Log;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

import pdgt.cat.com.noaahso.BusinessObjects.WeatherEvent;


public class WeatherAzureAccess{
    public static Context context;

    MobileServiceClient mClient;
    MobileServiceJsonTable weatherTable;

    WeatherSync weatherThread = null;

    private static WeatherAzureAccess instance = null;

    public static WeatherAzureAccess Instance()
    {
        if(instance == null)
            instance = new WeatherAzureAccess();

        return instance;
    }

    public WeatherAzureAccess() {

        try {
            mClient = new MobileServiceClient(
                    "https://weatherpractice.azure-mobile.net/",
                    "fdrrufdHqeWnvGUuMxeolBYASslNpI63",
                    context
            );
        } catch (MalformedURLException e) {
            Log.d("WeatherAzure", "OnEventRaised MalformedURLException: " + e.toString());
        }

        weatherTable = mClient.getTable("weatherpractice_table");

        if(weatherThread == null) {
            weatherThread = new WeatherSync();
            weatherThread.start();
        }

    }


    public void WeatherEventSyncSuccess(WeatherEvent wEvent, JsonObject result) {
        WeatherDBAccess.Instance().MarkAsSynchedEvent(wEvent);
    }

    public void WeatherEventSyncFailure(WeatherEvent wEvent, Throwable exc) {

    }

    //todo: this
    public void weatherFinalize(){

        if(weatherThread !=null) {
            weatherThread.mEndTask = true;
            weatherThread.interrupt();

        }
    }

    public void SyncWeatherWithAzure(WeatherEvent wEvent) {
        JsonObject wAzureEvent = new JsonObject();
        wAzureEvent.addProperty("site_guid", "todo: generate a guid");
        wAzureEvent.addProperty("time_stamp", WeatherDBAccess.Instance().GetTimeStamp(wEvent.Time));
        wAzureEvent.addProperty("latitude",wEvent.Latitude);
        wAzureEvent.addProperty("longitude", wEvent.Longitude);
        wAzureEvent.addProperty("current_temp", wEvent.CurrTemp);
        wAzureEvent.addProperty("current_description", wEvent.CurrDesc);
       // wAzureEvent.add("current_icon", wEvent.CurrIcon);


        if (wEvent.SevereWeatherPresent) {
            wAzureEvent.addProperty("Severe Weather Present", true);
            wAzureEvent.add("Severe Weather Data", wEvent.SevereWeatherData); //adds all weather data at once

        } else {
            wAzureEvent.addProperty("Severe Weather Present", false);
        }

        ListenableFuture<JsonObject> wEventsResult = weatherTable.insert(wAzureEvent);
        Futures.addCallback(wEventsResult, wEvent);

    }
    private class WeatherSync extends Thread {
        //call syncwithazure
        public boolean mEndTask = false;
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Log.d("AzureAccess", "WeatherSync thread STARTED!");

            this.setName("Azure Weather Sync Thread");

            //while (!mEndTask) {
                /*if (interrupted()) {
                    Log.d("AzureAccess", "AzureSync thread INTURRUPTED!");
                    return;
                }*/
                ArrayList<WeatherEvent> unsychedWeatherEvents = WeatherDBAccess.Instance().GetUnsychedWeatherEvent();
                for (WeatherEvent event : unsychedWeatherEvents) {
                    SyncWeatherWithAzure(event);
                }

                //SyncWeatherWithAzure(wEvent);
                //todo: create queue of unsynced events

           // }
        }
    }
}

