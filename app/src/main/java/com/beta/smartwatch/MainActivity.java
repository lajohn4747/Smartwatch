package com.beta.smartwatch;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    private static final int REQUEST_ENABLE_BT = 1;
    private final String delimiter = "&;";

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    TextView textInfo;
    ListView listViewPairedDevice;
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    private final int timeInterval = 50000;
    private List<String> weatherArray;
    public String weatherDescription;
    public Calendar cal = Calendar.getInstance();
    public String date = cal.getTime().toString();

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

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
        setContentView(R.layout.activity_maintest);

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

        textInfo = (TextView)findViewById(R.id.info);
        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
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
        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
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
                Toast.makeText(MainActivity.this, "Latitude: " + mLatitudeText + "\n" + "Longitude: " + mLongitudeText, Toast.LENGTH_LONG).show();
                GetWeather weatherRequest = new GetWeather();
                try {
                    String weatherResponse = weatherRequest.execute(mLatitudeText,mLongitudeText).get();
                    JSONObject weather = new JSONObject(weatherResponse);
                    Log.d("Weather", weather.toString());
                    JSONObject data = weather.getJSONObject("data");
                    Log.d("Data", data.toString());
                    JSONArray current_condition = data.getJSONArray("current_condition");
                    JSONObject current = (JSONObject) current_condition.get(0);
                    JSONArray weatherDescArr = current.getJSONArray("weatherDesc");
                    JSONObject weatherDesc = (JSONObject) weatherDescArr.get(0);
                    JSONArray weatherArr = data.getJSONArray("weather");
                    JSONObject weat = (JSONObject) weatherArr.get(0);
                    JSONArray astronomyArr = weat.getJSONArray("astronomy");
                    JSONObject astronomy = (JSONObject) astronomyArr.get(0);

                    Log.d("Current Condition", current.toString());
                    String feels_like = current.getString("FeelsLikeF");
                    String humidity = current.getString("humidity");
                    String temperature = current.getString("temp_F");
                    String precipitation = current.getString("precipMM");
                    String description = weatherDesc.getString("value");
                    String sunrise = astronomy.getString("sunrise");
                    String sunset = astronomy.getString("sunset");
                    String high = weat.getString("maxtempF");
                    String low = weat.getString("mintempF");
                    weatherArray = Arrays.asList("fee",feels_like,"hum",humidity,"tmp",temperature,"pre",precipitation,"des",description,"sur",sunrise,"sus",sunset,"hig",high,"low",low);
                    weatherDescription = "fee " + feels_like+ "hum " + humidity + "tmp " + temperature
                            + "pre " + precipitation + "des " + description + "sur " + sunrise +
                            "sus " + sunset + "hig " + high + "low " + low;

                    //TextView weatherTextView = (TextView) findViewById(R.id.Weather);
                    //weatherTextView.setText("Weather\nFeels Like: " + feels_like);
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
        Log.d("AudioMode", Integer.toString(mAudioManager.getMode()));

        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDPAUSE );
            MainActivity.this.sendBroadcast(i);
        }
        Log.d("AudioMode",Integer.toString(mAudioManager.getMode()));
    }

    public void next(View view) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d("Next", "Clicked");
        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDNEXT );
            MainActivity.this.sendBroadcast(i);
        }
    }

    //start timer function
    public void startTimer(){
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, timeInterval);
    }
    //function to stop the timer
    public void stoptimertask(View v){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
    }

    public void textToBT(String msg){
        if(myThreadConnected != null){
            byte[] bytesToSend = msg.getBytes();
            myThreadConnected.write(bytesToSend);
            byte[] NewLine = "\n".getBytes();
            myThreadConnected.write(NewLine);
        }
    }

    public void initializeTimerTask(){
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable(){
                    public void run(){
                        if(myThreadConnected!=null && weatherArray != null){
                            int char_num = 0;
                            List<String> weatherStringsToSend = new ArrayList<String>();
                            String weatherString = "";
                            for (String weatherItem:weatherArray) {
                                Log.d("Weather Item",weatherItem);
                                char_num += weatherItem.length() + delimiter.length();
                                Log.d("Character Number",Integer.toString(char_num));
                                if (char_num > 95) {
                                    weatherStringsToSend.add(weatherString);
                                    Log.d("Weather String Added", weatherString);
                                    char_num = weatherItem.length() + delimiter.length();
                                    weatherString = weatherItem + delimiter;
                                } else {
                                    weatherString += weatherItem + delimiter;
                                    Log.d("Weather String Appended", weatherString);
                                }
                            }
                            Log.d("Weather Strings", weatherStringsToSend.toString());
                            if (weatherString.length() > 0) {
                                weatherStringsToSend.add(weatherString);
                            }
                            Log.d("Bluetooth Message(s)", weatherStringsToSend.toString());
                            for (String sendString:weatherStringsToSend) {
                                //byte[] bytesToSend = weatherDescription.getBytes();
                                byte[] bytesToSend = sendString.getBytes();
                                myThreadConnected.write(bytesToSend);
                                byte[] NewLine = "\n".getBytes();
                                myThreadConnected.write(NewLine);
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                byte[] dateBytes = date.getBytes();
                                myThreadConnected.write(dateBytes);
                                myThreadConnected.write(NewLine);
                            }
                        }
                    }
                });
            }
        };
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
            }

            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setup();
            }else{
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run(){
                        Toast.makeText(MainActivity.this, msgconnected, Toast.LENGTH_LONG).show();

                        listViewPairedDevice.setVisibility(View.GONE);
                    }
                });

                startThreadConnected(bluetoothSocket);

            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            String strRx = "";

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {

                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {

                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
