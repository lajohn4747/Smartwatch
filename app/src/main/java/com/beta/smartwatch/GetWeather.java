package com.beta.smartwatch;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Mike on 1/13/2016.
 */
public class GetWeather extends AsyncTask<String, String, String> {
    @Override
    protected String doInBackground(String... params) {
        //Sample Request: http://api.worldweatheronline.com/premium/v1/weather.ashx?key=xxxxxxxxxxxxx&q=48.85,2.35&num_of_days=2&tp=3&format=xml
        String URLString = "http://api.worldweatheronline.com/free/v2/weather.ashx?key=";
        String key = "cb0d2e574d06d01aca1c6cc9fbabb";
        String latitude = params[0].substring(0,params[0].indexOf('.')+4);
        String longitude = params[1].substring(0,params[1].indexOf('.')+4);
        String day_num = "2";
        String hourly = "3";
        String format = "json";
        String response = "No response";
        String URLRequest = URLString + key + "&q=" + latitude + "," + longitude + "&num_of_days=" + day_num + "&tp=" + hourly + "&format=" + format;
        Log.d("Request",URLRequest);
        try {

            URL url = new URL(URLRequest);
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            if (urlConnection == null) {
                Log.d("URL Connection","Null");
                return response;
            } else {
                Log.d("URL Connection", urlConnection.toString());
            }

            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            InputStreamReader in = new InputStreamReader(inputStream);
            if (in == null) {
                Log.d("Input Stream","Null");
                return response;
            }
            BufferedReader br = new BufferedReader(in);
            String line;
            response = "";
            while ((line = br.readLine()) != null) {
                response += line;
            }
            Log.d("Response", response);
        } catch (MalformedURLException e) {
            Log.d("URL", "Malformed URL");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("URL", "IOException");
            e.printStackTrace();
        }
        return response;
    }
}
