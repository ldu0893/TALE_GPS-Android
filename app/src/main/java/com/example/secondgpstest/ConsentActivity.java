package com.example.secondgpstest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

public class ConsentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("TAG", "Consent");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //prefs.edit().clear().commit();
        boolean previouslyStarted = prefs.getBoolean("pref_previously", false);
        if (previouslyStarted) {
            Intent intent = new Intent(this, GPSLocator.class);
            startActivity(intent);
            finish();
        } else {
            Log.e("TAG", "PREF");
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("pref_previously", Boolean.TRUE);
            edit.commit();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent);
    }

    public void consent(View v) {
        if (((CheckBox) findViewById(R.id.check_student)).isChecked() && ((CheckBox) findViewById(R.id.check_guardian)).isChecked()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please confirm consent.", Toast.LENGTH_LONG).show();
        }
    }
}