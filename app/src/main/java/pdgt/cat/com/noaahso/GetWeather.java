package pdgt.cat.com.noaahso;

/**
 * Created by smithm24 on 10/30/2015. Acceses api and parses into json that is returned to
 * WeatherTask.java
 */

import android.location.Location;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



//import javax.net.ssl.HttpsURLConnection;


//import org.json.JSONObject;

public class GetWeather {
    private String lon;
    private String lat;
    private StringBuilder builder;

    private static final int CONNECTION_TIMEOUT = 1000;
    private static final int DATARETRIEVAL_TIMEOUT = 1000;

    private static final String TAG = "GetWeather";

    private static StringBuilder builderL;

    // authorization token

    private String key = "9af43623a5459ac3"; //wunderground token

    private String baseUrl = "https://api.wunderground.com/api/" + key + "/conditions/hourly/alerts/q/";// 35.689,139.691.json";
    private String urlStr;

    public JSONObject finalJson = new JSONObject();

    public GetWeather(Double latitude, Double longitude) {
        lat = Double.toString(latitude);
        lon = Double.toString(longitude);

        urlStr = baseUrl + lat + "," + lon + ".json";
        //urlStr = "http://api.wunderground.com/api/9af43623a5459ac3/conditions/hourly/alerts/q/MA/Boston.json";

    }

    public JSONObject jsonWeather() {
        Log.d("AsyncTask", "got to jsonWeather");

        try {
            URL url = new URL(urlStr);
            URLConnection con = url.openConnection();
            if (con instanceof HttpURLConnection) {

                HttpURLConnection httpsURLConnection = (HttpURLConnection) con;
                httpsURLConnection.setRequestMethod("GET");
                int response = -1;
                // httpURLConnection.setRequestProperty("token", token);
                httpsURLConnection.connect();

                response = httpsURLConnection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    BufferedReader br = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
                    builder = new StringBuilder();
                    String line;

                    while((line = br.readLine()) != null){

                        builder.append(line);

                    }
                    br.close();
                }
            }
            //parse out the info we need

            JSONObject data = new JSONObject(builder.toString());//return is;

            //parse out current observation
            JSONObject curr_obs = data.getJSONObject("current_observation");

            Integer temp_f = curr_obs.getInt("temp_f");
            String weat = curr_obs.getString("weather");
            String icon_url = curr_obs.getString("icon_url");
            long curr_epoch = curr_obs.getLong("local_epoch");

            int ha = 1;
            int hh = 2;
            finalJson.put("Current Temperature",temp_f.toString());
            finalJson.put("Current Conditions", weat);
            finalJson.put("Current Icon", icon_url);
            finalJson.put("Local Epoch", curr_epoch);

            //parse out hourly info
            JSONArray hourarr = data.getJSONArray("hourly_forecast");
            JSONArray hourly = new JSONArray();
            int len = hourarr.length();

            //loop through hourly forecast array
            for (int i = 0; i < 12; i++) {
                JSONObject temp_o = hourarr.getJSONObject(i);
                String hour = temp_o.getJSONObject("FCTTIME").getString("civil");
                String epoch = temp_o.getJSONObject("FCTTIME").getString("epoch");
                int numHour = temp_o.getJSONObject("FCTTIME").getInt("hour");

                JSONObject vals = new JSONObject();

                String temp_h = temp_o.getJSONObject("temp").getString("english");
                String cond = temp_o.getString("condition");
                String ic = temp_o.getString("icon_url");
                String pop = temp_o.getString("pop");
                String qpf = temp_o.getJSONObject("qpf").getString("english");


                vals.put("Hour", hour);
                vals.put("Hour Temp", temp_h);
                vals.put("Condition", cond);
                vals.put("Icon", ic);
                vals.put("Precipitation Probability", pop);
                vals.put("Water Accumulation", qpf);
                vals.put("Epoch", epoch);
                vals.put("Hour_Number", numHour);

                hourly.put(i, vals);
            }
            finalJson.put("Hourly Forecast", hourly);
            System.out.println("Mariah: Before Alerts");

            //parse out alert info

            JSONArray alertArr = new JSONArray();
            try {
                if (data.getJSONArray("alerts").getJSONObject(0) != null) {
                    for (int i = 0; i < data.getJSONArray("alerts").length(); i++) {
                        Log.d("GetWeather", "saying alerts is not null");
                        JSONObject alerts = data.getJSONArray("alerts").getJSONObject(i);
                        String alert_begin = alerts.getString("date");
                        String alert_expire = alerts.getString("expires");
                        String desc = alerts.getString("description");
                        String message = alerts.getString("message");
                        long epoch_start = alerts.getLong("date_epoch");
                        long epoch_end = alerts.getLong("expires_epoch");
                        DateTime dt = new DateTime(epoch_start);


                        JSONObject alertMess = new JSONObject();

                        alertMess.put("Begin", alert_begin);
                        alertMess.put("Expires", alert_expire);
                        alertMess.put("Description", desc);
                        alertMess.put("Message", message);
                        alertMess.put("Epoch Start", epoch_start);
                        alertMess.put("Epoch Expire", epoch_end);

                        alertArr.put(alertMess);

                    }
                    finalJson.put("Alerts", alertArr);
                }
            } catch (JSONException e) {
                //e.printStackTrace();
            }
            System.out.println("Mariah: After Alerts");



        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d("GetWeather", String.valueOf(finalJson));
        return finalJson;
        //return builder.toString();
    }

    //this method returns a LatLng list of lightning strikes based on a location, a radius boundary (meters) from that location, and the past seconds you
    //you would like to query in the past to
    public ArrayList<LatLng> getLightning(Location centroid, int rad, int past){
        ArrayList<LatLng> strikesList = new ArrayList<>();

        HttpURLConnection urlConnection;

        JsonParser parser = new JsonParser();
        DateTime now = new DateTime(DateTimeZone.UTC);

        String url = "https://maps.toasystems.com/svcs/activity/v1/snapshot/centroid?lat="+ centroid.getLatitude() +
                "&lon=" + centroid.getLongitude() + "&st="+now.minusSeconds(past) + "&dur="+ past + "&rad=" + rad;

        try {
            String auth = "";

                String passkey = "mariahsmith@caterpillar:c98c83c1-3eef-46e4-bfdb-d08bc3f029e9";

                String encoding = Base64.encodeToString(passkey.getBytes("UTF-8"), Base64.NO_WRAP);
                auth = "basic " + encoding;


            // create connection
            URL urlToRequest = new URL(url);
            urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);


            urlConnection.setRequestProperty("Authorization", auth);


            // handle issues
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // handle unauthorized (if service requires user login)
            } else if (statusCode != HttpURLConnection.HTTP_OK) {
                // handle any other errors, like 404, 500,..
            }else if (statusCode == HttpURLConnection.HTTP_OK) {

                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                builderL = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {

                    builderL.append(line);
                    System.out.println(line);

                }
                br.close();

                JsonArray strikes = parser.parse(builderL.toString()).getAsJsonObject().getAsJsonArray("snapshotResults");
                for (JsonElement strike : strikes) {
                    LatLng lightStrike = new LatLng(strike.getAsJsonObject().get("lat").getAsDouble(),strike.getAsJsonObject().get("lon").getAsDouble());
                    strikesList.add(lightStrike);
                }

            }


        } catch (MalformedURLException e) {
            Log.e(TAG,e.getMessage());
        } catch (SocketTimeoutException e) {
            // data retrieval or connection timed out
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            // could not read response body
            // (could not create input stream)
            Log.e(TAG, e.getMessage());
        } catch (JsonParseException e) {
            System.out.println(e.toString());
        }


        return strikesList;
    }

}
