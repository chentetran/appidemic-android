package com.example.chente.appidemic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class LocationService extends Service
{
    private static final String TAG = "GPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1500;    // milliseconds
    private static final float LOCATION_DISTANCE = 100;   // meters
    private String id;
    private Context this_context;
    private int notificationId = 123;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private long[] pattern = {0, 300};
    private Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }


        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            // Check sharedPrefs and see if user was infected without knowing
            // If so, notify!
            boolean infectedLocal = prefs.getBoolean("infected", false);
            if (!infectedLocal) {
                // Check infection on server
                JsonObject json_id = new JsonObject();
                json_id.addProperty("id", id);
                Ion.with(getApplicationContext())
                        .load("http://appidemic.herokuapp.com/checkInfection")
                        .setJsonObjectBody(json_id)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                if (e != null) {
                                    Toast.makeText(LocationService.this, "Check internet connection", Toast.LENGTH_SHORT).show();
                                    Log.d("PostException", e.toString());
                                } else {
                                    int status = result.get("status").getAsInt();
                                    if (status == 2) { // infected!
                                        // Change infection locally
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean("infected", true);
                                        editor.apply();

                                        // Notify user of infection
                                        Notification.Builder builder = new Notification.Builder(this_context)
                                                .setSmallIcon(R.drawable.ic_stat_name)
                                                .setContentText("You were infected!")
                                                .setContentTitle("The Appidemic grows...")
                                                .setLights(Color.RED, 500, 500)
                                                .setVibrate(pattern)
                                                .setSound(alarmSound);
                                        Notification notification = builder.build();
                                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        manager.notify(notificationId, notification);
                                    }
                                }
                            }
                        });
            }

            // Send location to spread or get infection
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("lat", location.getLatitude());
            json.addProperty("lng", location.getLongitude());

            Ion.with(getApplicationContext())
                    .load("http://appidemic.herokuapp.com/sendLocation")
                    .setJsonObjectBody(json)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (e != null) {
                                // Handle
                            }
                            else {
                                int status = result.get("result").getAsInt();
                                String message;

                                Notification.Builder builder = new Notification.Builder(this_context)
                                        .setContentTitle("The Appidemic spreads...")
                                        .setSmallIcon(R.drawable.ic_stat_name);
                                Notification notification;
                                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                // Make the notification clickable
                                Intent intent = new Intent(this_context, MainActivity.class);
                                PendingIntent pending = PendingIntent.getActivity(this_context, 0, intent, 0);
                                builder.setContentIntent(pending);

                                // Sounds and Lights
                                builder.setLights(Color.RED, 500, 500);
                                builder.setVibrate(pattern);
                                builder.setSound(alarmSound);

                                switch (status) {
                                    case 1:     // User is infected and infected nobody
                                        break;
                                    case 2:     // User is infected and infected people

                                        // Notification to show that you have infected people
                                        message = result.get("message").getAsString();
                                        builder.setContentText(message);
                                        notification = builder.build();
                                        manager.notify(notificationId, notification);
                                            // TODO: make sound and visible dropdown notification
                                        break;
                                    case 3:     // User is healthy and got infected

                                        // Notification to show that you have been infected
                                        message = result.get("message").getAsString();
                                        builder.setContentText(message);
                                        notification = builder.build();
                                        manager.notify(notificationId, notification);
                                            // TODO: make sound and visible dropdown notification

                                        // Change sharedprefs to indicate you were infected
                                        editor.putBoolean("infected", true);
                                        editor.apply();


                                        break;
                                    case 4:     // User is healthy and didn't get infected
                                        break;
                                }
                            }
                        }
                    });

        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);


        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        // get facebook uid from sharedprefs
        prefs = getSharedPreferences("Appidemic", MODE_PRIVATE);
        id = prefs.getString("id", "No ID Error");
        Log.e(TAG, "onCreate");

        // open sharedprefs editor
        editor = prefs.edit();

        // get context
        this_context = this;

        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    // Request permission for location
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(LocationService.this, "Turn location services on", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}
