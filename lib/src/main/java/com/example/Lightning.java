package com.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by smithm24 on 4/15/2016.
 */
public class Lightning {

    private static final int CONNECTION_TIMEOUT = 1000;
    private static final int DATARETRIEVAL_TIMEOUT = 1000;
    private static final Log log = LogFactory.getLog(Lightning.class);
    private static StringBuilder builderL;

    public static JsonArray getData(String url, boolean useAuth) {
        HttpURLConnection urlConnection = null;
        JsonObject data = new JsonObject();
        JsonArray strikes = new JsonArray();
        JsonParser parser = new JsonParser();


        try {
            String auth = "";
            if ( useAuth ) {
                String passkey = "mariahsmith@caterpillar:c98c83c1-3eef-46e4-bfdb-d08bc3f029e9";

                String encoding = Base64.encodeBase64String(passkey.getBytes("UTF-8"));
                auth = "basic " + encoding;
            }

            // create connection
            URL urlToRequest = new URL(url);
            urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);

            if ( useAuth ) {
                urlConnection.setRequestProperty("Authorization", auth);
            }

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

                while((line = br.readLine()) != null){

                    builderL.append(line);
                    System.out.println(line);

                }
                br.close();
            }

            strikes = parser.parse(builderL.toString()).getAsJsonObject().getAsJsonArray("snapshotResults");
            for (JsonElement strike: strikes){
                System.out.println(strike.getAsJsonObject().get("lat").getAsString());
            }
                    //get(0).getAsJsonObject().get("lat").getAsString();
            //System.out.println(lat);

            DateTime now = new DateTime(DateTimeZone.UTC);
            System.out.println(" " + now.getMillis()/1000);


        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (SocketTimeoutException e) {
            // data retrieval or connection timed out
            log.error(e.getMessage());
        } catch (IOException e) {
            // could not read response body
            // (could not create input stream)
            log.error(e.getMessage());
        } catch (JsonParseException e) {
            System.out.println(e.toString());
        }
        return strikes;
    }

    public static void main(String[] args){
        String url = "https://maps.toasystems.com/svcs/activity/v1/snapshot/centroid?lat=29.97&lon=-95.34&st=1440201420&dur=60&rad=16000";
        getData(url,true);

    }
}
