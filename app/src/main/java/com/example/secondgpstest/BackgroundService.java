package com.example.secondgpstest;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "my_channel";
    private static final String FILE_NAME = "example.txt";
    private static final long UPDATE_INTERVAL_IN_MIL = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MIL = UPDATE_INTERVAL_IN_MIL/2;
    private static final int NOTI_ID = 1223;
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.example.secondgpstest"+
            ".started_from_notification";
    private final IBinder mBinder = new LocalBinder();

    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler mServiceHandler;
    private Location mLocation;
    private String cAddress, homeAddress;
    private Date cDateLeft;
    private Long homeLong = null;
    private boolean returnHome = false;
    private int school = -1;
    private String username;

    public BackgroundService() {

    }

    @Override
    public void onCreate() {
        Log.e("DATE", Calendar.getInstance().get(HOUR_OF_DAY) + " " + Calendar.getInstance().get(MINUTE));
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        try {
            FileInputStream fs;
            fs = openFileInput(FILE_NAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            for (int i=0; i<10; i++)
                System.out.println(br.readLine());
        } catch (Exception e) {System.out.println("end");}
        try {
            FileInputStream fs;
            fs = openFileInput(FILE_NAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            school = Integer.parseInt(br.readLine());
            username = br.readLine();
            cAddress = br.readLine();
            homeAddress = cAddress;
            fs.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread("GPSTest");
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }



    public void removeLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException e) {
            Common.setRequestingLocationUpdates(this, true);
            Log.e("GPSTest", "Lost location permission. Could not remove updates. "+e);
            e.printStackTrace();
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.e("GPSTest", "Failed to get location");
                            }
                        }
                    });
        } catch (SecurityException e) {
            Log.e("GPSTest", "Lost location permission. "+e);
            e.printStackTrace();
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MIL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location lastLocation) {
        mLocation = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));

        //Update notification content if running as a foreground service
        if (serviceIsRunningInForeGround(this)) {
            mNotificationManager.notify(NOTI_ID, getNotification());
        }

        if (!homeAddress.equals("null")) {
            addressUpdate(mLocation);
        } else {
            findHome(mLocation);
        }

    }

    private void findHome(Location loc) {
        Log.e("DATE", "findHome");
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            String address = addresses.get(0).getAddressLine(0);
            Date date = Calendar.getInstance().getTime();
            long dateLeft = date.getTime();
            if (!cAddress.equals(address) || homeLong == null) {
                homeLong = dateLeft;
                cAddress = address;
                Log.e("TAG", "Change home " + address);
            } else {
                long time = dateLeft - homeLong;
                double timeLeft = ((double)time / 3600000);
                if (timeLeft >= 0.04) {
                    homeAddress = address;
                    cAddress = homeAddress;
                    findSchool(homeAddress);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findSchool(String address) {
        Log.e("TAG", address);
        String streetAddress=address.split(",")[0].replaceAll("\\s+", "+").toLowerCase();
        String zipcode=address.split(",")[2].split("\\s+")[2].toLowerCase();
        String gradeId="";

        if (school==0) gradeId="12";
        else if (school==1) gradeId="15";

        try {
            FileOutputStream writer = openFileOutput(FILE_NAME, MODE_PRIVATE);
            writer.write((school+"\n"+username+"\n"+address).getBytes());

            OkHttpClient client = new OkHttpClient();

            String url = "http://www.infofinderi.com/ifi/ws/SearchRequest.asmx/SearchAddress?clientId=HCP2IOASIJVW&streetAddress=" +
                    streetAddress +
                    "&zipCode=" + zipcode + "&schoolId=-1&gradeId=" + gradeId;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String text = response.body().string();
                        String[] manip = text.substring(text.indexOf("schoolName") + 13, text.indexOf("schoolAddress") - 3)
                                .split("\\s+");
                        text = "";
                        for (int i = 0; i < manip.length - 1; i++) {
                            text += manip[i].substring(0, 1);
                        }
                        text += manip[manip.length - 1];
                        Log.d("SCHOOL", text);

                        FileOutputStream fos = openFileOutput(FILE_NAME, MODE_APPEND | MODE_PRIVATE);
                        fos.write("\n".getBytes());
                        fos.write(text.getBytes());
                        fos.close();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addressUpdate(Location loc) {
        Log.e("DATE", "addressUpdate");
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            String address = addresses.get(0).getAddressLine(0);
            System.out.println("cAddress " + cAddress);
            System.out.println("address " + address);
            if (cAddress.equals(homeAddress) && !address.equals(homeAddress)) {
                Date date = Calendar.getInstance().getTime();
                leftHome();
                long dateLeft = date.getTime();
                cDateLeft = date;
                //txtloc += "\nLeft home. " + date.toString();

                FileOutputStream fos = null;
                try {
                    fos = openFileOutput(FILE_NAME, MODE_APPEND | MODE_PRIVATE);
                    fos.write("\n".getBytes());
                    fos.write(("l " + dateLeft).getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (!cAddress.equals(homeAddress) && address.equals(homeAddress)) {
                Date date = Calendar.getInstance().getTime();
                Log.d("DATE", ""+date.toString());
                Log.d("DATE", ""+cDateLeft.toString());
                long dateReturn = date.getTime();
                //txtloc += "\nBack home. " + date.toString();

                FileOutputStream fos = null;
                try {
                    fos = openFileOutput(FILE_NAME, MODE_APPEND | MODE_PRIVATE);
                    fos.write("\n".getBytes());
                    fos.write(("r " + dateReturn).getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (cDateLeft != null) {
                    //TODO: This looks bad.
                    long time = date.getTime() - cDateLeft.getTime();
                    long minutes = 60000;
                    double timeLeft = ((double)time / minutes);
                    timeLeft = Math.floor(timeLeft * 100) / 100;
                    Log.d("DATE", "TIME_LEFT "+timeLeft);

                    sendData(timeLeft, "data");
                    if (returnHome) {
                        returnHome = false;
                        returnHome(Calendar.getInstance());
                    }

                } else {
                    Log.e("DATE", "DATE_LEFT NULL");
                }

            }
            cAddress = address;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void leftHome() {
        Calendar c = Calendar.getInstance();
        if (school == 0) {
            if (c.get(HOUR_OF_DAY) == 7 && c.get(MINUTE) > 29 && c.get(MINUTE) < 60) {
                returnHome = true;
            }
        } else {
            if (c.get(HOUR_OF_DAY) == 7 && c.get(MINUTE) < 26) {
                returnHome = true;
            }
        }
    }

    private void returnHome(Calendar c) {
        if (school == 0) {
            if (HOUR_OF_DAY > 15 && c.getInstance().MINUTE > 30) {
                Log.e("DATE", HOUR_OF_DAY + " " + MINUTE);
                double timeLeft = 60*(HOUR_OF_DAY - 14) + MINUTE - 45;
                sendData(timeLeft, "reopen");
            }
        } else {
            if (HOUR_OF_DAY > 15) {
                double timeLeft = 60*(HOUR_OF_DAY - 14) + MINUTE - 15;
                sendData(timeLeft, "reopen");
            }
        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, BackgroundService.class);
        String text = Common.getLocationText(mLocation);
        
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, GPSLocator.class), 0);
        //TODO: Change.

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_baseline_launch_24, "Launch", activityPendingIntent)
                .addAction(R.drawable.ic_baseline_cancel_24, "Remove", servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true);

        //Set the channel id for Android O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        return builder.build();
    }

    private boolean serviceIsRunningInForeGround(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground)
                    return true;
            }
        }
        return false;
    }

    public void requestLocationUpdates() {
        Common.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), BackgroundService.class));
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            Log.e("GPSTest", "Lost location permission. Could not request it. "+e);
            e.printStackTrace();
        }
    }

    public class LocalBinder extends Binder {
        BackgroundService getService() {return BackgroundService.this; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mChangingConfiguration && Common.requestingLocationUpdates(this))
            startForeground(NOTI_ID, getNotification());
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacks(null);
        super.onDestroy();
    }

    private void sendData(double timeLeft, String post) {
        Log.e("TAG", post);
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

            new SendDeviceDetails().execute("http://stayhomeorder.org:8000/"+post, json);

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
