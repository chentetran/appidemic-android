package com.example.chente.appidemic;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 1337;
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permission for location
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSION_ACCESS_FINE_LOCATION);
        }

        // get facebook uid from sharedprefs
        SharedPreferences prefs = getSharedPreferences("Appidemic", MODE_PRIVATE);
        id = prefs.getString("id", "No ID Error");

        if (id.equals("No ID Error")) {             // user id was deleted from shared prefs
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        // Start location-tracking service
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("id", id);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // POST id to server, get "infected" status as response
        JsonObject json = new JsonObject();
        json.addProperty("id", id);

        Ion.with(this)
                .load("http://appidemic.herokuapp.com/checkInfection")
                .setJsonObjectBody(json)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(MainActivity.this, "Error getting status", Toast.LENGTH_SHORT).show();
                            Log.d("PostException", e.toString());
                        }
                        else {
                            TextView infectionStatus = (TextView) findViewById(R.id.status);
                            int status = result.get("status").getAsInt();
                            switch (status) {
                                case 0: // healthy
                                    // default to case 1
                                case 1: // healthy
                                    infectionStatus.setText("HEALTHY");
                                    infectionStatus.setTextColor(Color.GREEN);
                                    break;
                                case 2: // infected
                                    infectionStatus.setText("INFECTED");
                                    infectionStatus.setTextColor(Color.RED);
                            }
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Appidemic needs your location", Toast.LENGTH_SHORT).show();
                    finish();
                }

                break;
        }
    }
}
