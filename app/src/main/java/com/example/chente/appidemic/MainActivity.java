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
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 1337;

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

        Firebase.setAndroidContext(getApplicationContext());


        // get facebook uid from sharedprefs
        SharedPreferences prefs = getSharedPreferences("Appidemic", MODE_PRIVATE);
        String id = prefs.getString("id", "No ID Error");

        if (id.equals("No ID Error")) {             // user id was deleted from shared prefs
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        // Start location-tracking service
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("id", id);
        startService(intent);

        // Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference user = database.getReference(id);

        user.child("infected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TextView status = (TextView) findViewById(R.id.status);
                if (!dataSnapshot.exists()) {
                    user.child("infected").setValue(false);
                }
                else if ((boolean)dataSnapshot.getValue()) {      // infected~
                    status.setText("INFECTED");
                    status.setTextColor(Color.RED);
                }
                else {
                    user.child("infected").setValue(false);
                    status.setText("HEALTHY");
                    status.setTextColor(Color.GREEN);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
