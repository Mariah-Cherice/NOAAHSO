package pdgt.cat.com.noaahso;

import pdgt.cat.com.noaahso.BusinessObjects.WeatherEvent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.joda.time.DateTime;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import pdgt.cat.com.noaahso.BusinessObjects.WeatherEvent;


/**
 * Created by smithm24 on 10/29/2015. This is an asyntask class that, combined with the GetWeather.java
 * class mirrors the EventProcessor/DataService. Can be changed to service if it freezes the UI too much.
 * <p/>
 * Json data retrieved from GetWeather.java returns current conditions, hourly forecast up to the
 * shift end time, and any forecasted alerts.
 */

//params passed from execute, progess, result returned
public class WeatherTask extends AsyncTask<Void, Void, Void> {

    public static int shift_hours = MainActivity.viewCount;
    public int startIndex = 0;
    public static int shiftStart = MainActivity.shiftStart;
    public static int shiftEnd = 23;


    public long curr_epoch;
    public int dataLength;

    //views passed from mainactivity
    LinearLayout[] child;
    TextView[] hoursView;
    TextView[] tempsView;
    ImageView[] iconsView;

    //data being retrieved from api via GetWeather.java
    String[] hoursData;
    String[] tempsData;
    Bitmap[] iconsBit;
    Boolean[] epochs = null; //severe weather present booleans

    Context context;

    //current conditions variables
    TextView currentTemp;
    Bitmap currIcon;
    ImageView currIc;

    Button button;
    Activity act;

    JSONObject json; //json data retrieved from api

    String AM_PM = "";

    Double lat;
    Double lon;

    Button alertBut;
    ArrayList<WeatherEvent> dailyinfo = new ArrayList<WeatherEvent>();

    private DateTimeFormatter hourFormat = DateTimeFormat.forPattern("h:mm a");

    WeatherTask(Context context, Activity activity, LinearLayout[] child, TextView[] hours,
                TextView[] temps, ImageView[] icons, Button button, Double latitude,
                Double longitude, TextView currTemp, ImageView currIcon, Button alertButton) {

        this.act = activity;
        this.child = child;
        this.hoursView = hours;
        this.tempsView = temps;
        this.iconsView = icons;
        this.button = button;

        this.context = context;
        this.lat = latitude;
        this.lon = longitude;

        this.currentTemp = currTemp;
        this.currIc = currIcon;
        this.alertBut = alertButton;
    }


    @Override
    protected Void doInBackground(Void... params) {
        Log.d("AsyncTask", "got to doInBackground");


        GetWeather weather = new GetWeather(lat, lon);
        json = weather.jsonWeather(); //organized by hour

        try {

            //parse Current Condition
            currIcon = null;
            InputStream icurr = new URL(json.getString("Current Icon")).openStream();
            currIcon = BitmapFactory.decodeStream(icurr);
            curr_epoch = json.getLong("Local Epoch");

            //parse out hourly forecast info
            int forecastStart = json.getJSONArray("Hourly Forecast").getJSONObject(0).getInt("Hour_Number");
            startIndex = forecastStart - shiftStart; //startIndex of forecast
            dataLength = shift_hours - startIndex;

            //data arrays initialized after calculating the number of forecasted data actually needed
            //instead of creating arrays with null values
            hoursData = new String[dataLength];
            tempsData = new String[dataLength];
            iconsBit = new Bitmap[dataLength];

            //this only accounts for entered shift time. if we want to continue past planned shift times then
            //modifications will be needed
            for (int i = 0; i < dataLength; i++) {
                JSONObject tempHour = json.getJSONArray("Hourly Forecast").getJSONObject(i);
                hoursData[i] = tempHour.getString("Hour"); //get hour
                tempsData[i] = tempHour.getString("Hour Temp");

                //retrieve weather icons
                iconsBit[i] = null;
                InputStream in = new URL(tempHour.getString("Icon")).openStream();
                iconsBit[i] = BitmapFactory.decodeStream(in);
                //ToDo: save all possible images locally

            }

            //parse out alert data if available

            //todo: switch to datetime
            Calendar time = Calendar.getInstance();
            int minutes = time.get(Calendar.MINUTE);
            int hours = time.get(Calendar.HOUR);

            //todo: get rid of this. API gives this
            if (time.get(Calendar.AM_PM) == 1) {
                AM_PM = "PM";
            } else {
                AM_PM = "AM";
            }

            try {
                //create weather event object from api call
                WeatherEvent wEvent = new WeatherEvent();
                wEvent.Time = new DateTime(curr_epoch * 1000); //datetime uses milliseconds
                System.out.println(wEvent.Time);
                wEvent.CurrTemp = json.getString("Current Temperature");
                wEvent.CurrDesc = json.getString("Current Conditions");
                wEvent.Latitude = lat;
                wEvent.Longitude = lon;
                wEvent.CurrIcon = getStringFromBitmap(currIcon);

                if (json.has("Alerts")) {

                    JsonParser jsonParser = new JsonParser();
                    wEvent.SevereWeatherData = jsonParser.parse(json.getJSONArray("Alerts").getJSONObject(0).toString());
                    wEvent.SevereWeatherPresent = true;

                    epochs = new Boolean[dataLength];

                    long alert_start = json.getJSONArray("Alerts").getJSONObject(0).getLong("Epoch Start");
                    long alert_end = json.getJSONArray("Alerts").getJSONObject(0).getLong("Epoch Expire");

                    wEvent.SevereWeatherStart = json.getJSONArray("Alerts").getJSONObject(0).getString("Begin");
                    wEvent.SevereWeatherEnd = json.getJSONArray("Alerts").getJSONObject(0).getString("Expires");
                    wEvent.SevereWeatherDesc = json.getJSONArray("Alerts").getJSONObject(0).getString("Description");

                    for (int i = 0; i < dataLength; i++) {
                        JSONObject tempHour = json.getJSONArray("Hourly Forecast").getJSONObject(i);
                        //determines if there's an alert forecasted for that hour
                        if (alert_start < tempHour.getLong("Epoch") && tempHour.getLong("Epoch") < alert_end) {
                            epochs[i] = true;

                        } else {
                            epochs[i] = false;
                        }
                    }

                }
                //adds event to db as unsynched event
                WeatherDBAccess._context = this.context;
                WeatherDBAccess.Instance().AddWeatherEvent(wEvent);
                //starts azure
                //WeatherAzureAccess.context = this.context;
                // WeatherAzureAccess.Instance();
            } catch (Exception e) {
                e.printStackTrace();


                //send wevent to weatherazure to be synced to azure
                //then send wevent to local db
            }

            //prep dailyinfo arraylist

            dailyinfo = WeatherDBAccess.Instance().DailyWeatherEvents();


            ArrayList<Integer> hoursAv = new ArrayList<>();
            for (WeatherEvent event : dailyinfo) {
                hoursAv.add(event.Hour);

            }

            for (int i = hoursAv.size() - 1; i >= 0; i--) {
                //System.out.println(a.get(i));
                if (hoursAv.get(i) < shiftStart || hoursAv.get(i) > forecastStart - 1) {
                    //System.out.println(a.get(i));
                    dailyinfo.remove(i);
                    hoursAv.remove(i);

                }

            }
            dailyinfo.addAll(findMissingHours(hoursAv, shiftStart, forecastStart - 1));


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getStringFromBitmap(Bitmap bitmapPicture) {
 /*
 * This functions converts Bitmap picture to a string which can be
 * JSONified.
 * */
        final int COMPRESSION_QUALITY = 100;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    private Bitmap getBitmapFromString(String jsonString) {
/*
* This Function converts the String back to Bitmap
* */
        byte[] decodedString = Base64.decode(jsonString, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    static WeatherEvent noDataAvailable(int hour) {
        WeatherEvent event = new WeatherEvent();
        event.Hour = hour;
        event.CurrTemp = "Weather Data Not Available";
        return event;
    }

    static ArrayList<WeatherEvent> findMissingHours(ArrayList<Integer> a, int first, int last) {
        ArrayList<WeatherEvent> missingData = new ArrayList<WeatherEvent>();
        // before the array: numbers between first and a[0]-1
        if (a.isEmpty()) {
            //System.out.println("Oh no a was empty");
            a.add(shiftStart);
            missingData.add(noDataAvailable(a.get(0)));
        }
        for (int i = first; i < a.get(0); i++) {
            missingData.add(noDataAvailable(i));

        }
        // inside the array: at index i, a number is missing if it is between a[i-1]+1 and a[i]-1
        for (int i = 1; i < a.size(); i++) {
            for (int j = 1 + a.get(i - 1); j < a.get(i); j++) {
                missingData.add(noDataAvailable(j));
            }
        }
        // after the array: numbers between a[a.length-1] and last
        for (int i = 1 + a.get(a.size() - 1); i <= last; i++) {
            missingData.add(noDataAvailable(i));
        }
        return missingData;
    }

    //first method called in asynctask with .execute() in main ui thread
    @Override
    protected void onPreExecute() {

        button.setText("Getting weather");
        Log.d("AsyncTask", "got to preexecute");
        dailyinfo.clear();

    }


    @Override
    protected void onPostExecute(Void v) {
        //super.onPostExecute(j);
        //button says "weather updated @timestamp
        //imageView downloads icon image last
        //textView shows current temp and forecasted hour
        Log.d("AsyncTask", "got to onpostExecute");

        LinearLayout parentLayout = (LinearLayout) act.findViewById(R.id.parentLayout);

        parentLayout.requestLayout();

        try {
            //if 4G is turned off the device may still have a cell connection that doesn't allow for
            //internet connection.
            if (hoursData[0] == null) {

                button.setText("No internet connection");
                Toast toast = Toast.makeText(context, "No internet connection...", Toast.LENGTH_LONG);
                toast.show();

                button.setText("Get Weather");
                throw new IllegalArgumentException("No data, internet connection compromised");

            } else {
                currentTemp.setText("Current Temperature: " + json.getString("Current Temperature") + "\u2109"
                        + "; " + json.getString("Current Conditions"));
                currIc.setImageBitmap(currIcon);
                //DateTime now = new DateTime();

                //replace hours passed
                for (WeatherEvent event : dailyinfo) {
                    String time = new LocalTime(event.Hour, 0).toString(hourFormat);

                    int pastIndex = event.Hour - shiftStart;
                    hoursView[pastIndex].setText(time);
                    hoursView[pastIndex].setTextColor(Color.GRAY);

                    if (event.CurrTemp.equals("Weather Data Not Available")) {
                        tempsView[pastIndex].setText("N/A");
                        tempsView[pastIndex].setTextColor(Color.GRAY);
                    } else {
                        tempsView[pastIndex].setText(event.CurrTemp + "\u2109");
                        tempsView[pastIndex].setTextColor(Color.GRAY);

                    }
                    if (event.CurrIcon != null) {
                        iconsView[pastIndex].setImageBitmap(getBitmapFromString(event.CurrIcon));
                    } else {
                        iconsView[pastIndex].setImageBitmap(BitmapFactory.decodeResource(context.getResources(),
                                R.mipmap.ic_no_weather));
                    }

                }
                //forecast views
                for (int i = 0; i < hoursData.length; i++) {

                    hoursView[i + startIndex].setText(hoursData[i]);
                    hoursView[i + startIndex].setTextColor(Color.BLACK);
                    if(tempsData[i].equals("null")){
                        tempsView[i + startIndex].setText("Forecast Not Available");
                    }else {
                        tempsView[i + startIndex].setText(tempsData[i] + "\u2109");
                        iconsView[i + startIndex].setImageBitmap(iconsBit[i]);
                    }
                    tempsView[i + startIndex].setTextColor(Color.BLACK);

                    if (epochs != null) {
                        if (epochs[i]) {
                            //sets hours that fall within alert timeline to red
                            hoursView[i + startIndex].setTextColor(Color.RED);
                            tempsView[i + startIndex].setTextColor(Color.RED);
                        }
                    }

                    parentLayout.requestLayout();
                    //Todo: fix 12pm coming in as 0 for the hour

                }

                Calendar time = Calendar.getInstance();
                //replace with joda datetime

                button.setText("Weather Updated at " + time.get(Calendar.HOUR) + ":" + String.format("%02d", time.get(Calendar.MINUTE))
                        + " " + AM_PM);
                System.out.println("weather updated");


                if (json.has("Alerts")) {
                    alertBut.setVisibility(View.VISIBLE);

                    //alert button that will display message

                    alertBut.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            try {
                                String[] alerts = new String[json.getJSONArray("Alerts").length()];

                                for(int i = 0; i < json.getJSONArray("Alerts").length(); i++) {
                                    String alertMessage = "Start: " + json.getJSONArray("Alerts").getJSONObject(i).getString("Begin")
                                            + "\nExpires: " + json.getJSONArray("Alerts").getJSONObject(i).getString("Expires")
                                            + "\nDescription: " + json.getJSONArray("Alerts").getJSONObject(i).getString("Description")
                                            + "\n ";
                                    alerts[i] = alertMessage;
                                }

                                //String alertMessage = "Start" + json.getJSONObject("alerts").getString("date");

                                final AlertDialog.Builder alertBuild = new AlertDialog.Builder(context);
                                alertBuild.setTitle("Severe Weather Alert. Click alert for more details.")
                                        .setPositiveButton("BACK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        });

                                alertBuild.setItems(alerts, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {

                                        //Toast.makeText(context, " " + item, Toast.LENGTH_LONG).show();
                                        AlertDialog.Builder detailAlert = new AlertDialog.Builder(context);
                                        try {
                                            detailAlert.setTitle("Alert Details")
                                                    .setMessage(json.getJSONArray("Alerts").getJSONObject(item).getString("Message"))
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    })
                                                    .show();
                                        } catch (Exception e) {

                                        }
                                    }
                                }).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}




