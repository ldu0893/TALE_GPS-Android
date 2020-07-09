package com.example.secondgpstest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GPSLocator extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BackgroundService.LocalBinder binder = (BackgroundService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    //TODO: Clean.
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private static final String FILE_NAME = "example.txt";

    private static String txtloc;

    protected Boolean requestingLocationUpdates;

    private TextView textView, txtLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private String cAddress, homeAddress;
    private Date cDateLeft;

    Button requestLocation, removeLocation;
    BackgroundService mService = null;
    boolean mBound = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.e("DATE", Calendar.getInstance().getTime().toString());
        Log.e("DATE", String.valueOf(Calendar.getInstance().getTimeInMillis()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g_p_s_locator);

        Intent intent = getIntent();
        textView = (TextView) findViewById(R.id.print_text);
        textView.setText(intent.getStringExtra("key"));

        try {
            FileInputStream fs;
            fs = openFileInput(FILE_NAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            for (int i = 0; i < 2; ++i) {
                System.out.println(br.readLine());
            }
            cAddress = br.readLine();
            homeAddress = cAddress;
            fs.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Dexter.withActivity(this)
                .withPermissions(Arrays.asList(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        requestLocation = (Button) findViewById(R.id.request_location_updates_button);
                        removeLocation = (Button) findViewById(R.id.remove_location_updates_button);
                        setButtonState(true);

                        requestLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mService.requestLocationUpdates();
                            }
                        });
                        removeLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mService.removeLocationUpdates();
                            }
                        });

                        setButtonState(Common.requestingLocationUpdates(GPSLocator.this));
                        bindService(new Intent(GPSLocator.this,
                                BackgroundService.class),
                                mServiceConnection,
                                Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //TODO: Fix.
                mService.requestLocationUpdates();
            }
        }, 5000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES, false));
        }
    }

    private void setButtonState(boolean isRequestEnable) {
        if (isRequestEnable) {
            requestLocation.setEnabled(false);
            removeLocation.setEnabled(true);
        } else {
            requestLocation.setEnabled(true);
            removeLocation.setEnabled(false);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event) {
        if (event != null) {


            //TODO: Delete.
//            String data = new StringBuilder()
//                    .append(event.getLocation().getLatitude())
//                    .append("/")
//                    .append(event.getLocation().getLongitude())
//                    .toString();
//            Toast.makeText(mService, data, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendData(double timeLeft) {
        try {
            FileInputStream fs;
            fs = openFileInput(FILE_NAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            br.readLine();
            String email = br.readLine()+"@inst.hcpss.org";
            br.readLine();
            String school = br.readLine();
            String json = "school="+school.toLowerCase()+"&email="+email.toLowerCase()+"&minutes_away="+timeLeft;
//            JSONObject obj = new JSONObject(json);
            //TODO: Increase robustness of JSON using JSONObject.put
            Log.d("JSON", json);
            fs.close();
            br.close();

            new SendDeviceDetails().execute("http://stayhomeorder.org:8000/data", json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SendDeviceDetails extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            Log.e("TAG", params[0]);
            Log.e("TAG", params[1]);
            String data = "";

            HttpURLConnection httpURLConnection = null;
            try {

                httpURLConnection = (HttpURLConnection) new URL(params[0]).openConnection();
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(httpURLConnection.getOutputStream());
                wr.write(params[1]);
                wr.close();

                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(in);

                int inputStreamData = inputStreamReader.read();
                while (inputStreamData != -1) {
                    char current = (char) inputStreamData;
                    inputStreamData = inputStreamReader.read();
                    data += current;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                } else {
                    Log.e("TAG", "CONNECTION NULL");
                }
            }

            Log.e("TAG", data);
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("TAG", result); // this is expecting a response code to be sent from your server upon receiving the POST data
        }
    }
}