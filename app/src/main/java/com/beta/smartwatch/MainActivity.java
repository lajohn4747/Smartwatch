package com.beta.smartwatch;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    GoogleApiClient mGoogleApiClient;
    String mLatitudeText;
    String mLongitudeText;
    private static MainActivity instance;
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
    public static final String SERVICECMD = "com.android.music.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDSTOP = "stop";

    public MainActivity() {
        mGoogleApiClient = null;
        mLatitudeText = null;
        mLongitudeText = null;
        instance = this;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }
        Log.d("Google API Client", mGoogleApiClient.toString());
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.beta.smartwatch/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.beta.smartwatch/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d("Location", mLastLocation.toString());
            if (mLastLocation != null) {
                mLatitudeText = String.valueOf(mLastLocation.getLatitude());
                mLongitudeText = String.valueOf(mLastLocation.getLongitude());
                Log.d("Latitude",mLatitudeText);
                Log.d("Longitude", mLongitudeText);
                Toast.makeText(MainActivity.this, "Latitude: " + mLatitudeText + "\n" + "Longitude: " + mLongitudeText, Toast.LENGTH_LONG);
                GetWeather weatherRequest = new GetWeather();
                try {
                    String weatherResponse = weatherRequest.execute(mLatitudeText,mLongitudeText).get();
                    JSONObject weather = new JSONObject(weatherResponse);
                    Log.d("Weather", weather.toString());
                    JSONObject data = weather.getJSONObject("data");
                    Log.d("Data", data.toString());
                    JSONArray current_condition = data.getJSONArray("current_condition");
                    JSONObject current = (JSONObject) current_condition.get(0);
                    Log.d("Current Condition", current.toString());
                    String feels_like = current.getString("FeelsLikeF");
                    TextView weatherTextView = (TextView) findViewById(R.id.Weather);
                    weatherTextView.setText("Weather\nFeels Like: " + feels_like);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "Location Service Not Connected", Toast.LENGTH_SHORT).show();
    }

    public void updateLastText(String update) {
        TextView lastText = (TextView) findViewById(R.id.last_text);
        lastText.setText(update);

    }

    public void previous(View view) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d("Previous","Clicked");
        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDPREVIOUS );
            MainActivity.this.sendBroadcast(i);
        }
    }

    public void togglePause(View view) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d("TogglePause","Clicked");
        Log.d("AudioMode",Integer.toString(mAudioManager.getMode()));

        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDPAUSE );
            MainActivity.this.sendBroadcast(i);
        }
        Log.d("AudioMode",Integer.toString(mAudioManager.getMode()));
    }

    public void next(View view) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d("Next","Clicked");
        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDNEXT );
            MainActivity.this.sendBroadcast(i);
        }
    }
}
