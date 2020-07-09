package com.example.secondgpstest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String FILE_NAME = "example.txt";
    private static FileOutputStream fos;

    EditText mEditText;
    boolean isMiddle;
    Button middle, high;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);

        mEditText = findViewById(R.id.edit_text);
        middle = (Button) findViewById(R.id.button_middle);
        high = (Button) findViewById(R.id.button_high);

        clear();
    }

    private void clear() {
        FileOutputStream writer = null;
        try {
            writer = openFileOutput(FILE_NAME, MODE_PRIVATE);
            writer.write("-1".getBytes());
            Log.d("DEBUG", "CLEAR");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void middle(View v) {
        FileOutputStream writer = null;

        try {
            writer = openFileOutput(FILE_NAME, MODE_PRIVATE);
            writer.write("0".getBytes());
            Log.d("DEBUG", "MIDDLE");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        isMiddle = true;
        setButtonState(isMiddle);
    }

    public void high(View v) {
        FileOutputStream writer = null;

        try {
            writer = openFileOutput(FILE_NAME, MODE_PRIVATE);
            writer.write("1".getBytes());
            Log.d("DEBUG", "HIGH");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        isMiddle = false;
        setButtonState(isMiddle);
    }

    private void setButtonState(boolean isMiddle) {
        if (isMiddle) {
            middle.setEnabled(false);
            high.setEnabled(true);
        } else {
            middle.setEnabled(true);
            high.setEnabled(false);
        }
    }

    public void save(View v) {
        final String text = mEditText.getText().toString();
        FileOutputStream writer = null;
        FileInputStream fis = null;

        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            final int school = Integer.parseInt(br.readLine());
            br.close();
            writer = openFileOutput(FILE_NAME, MODE_APPEND | MODE_PRIVATE);
            writer.write("\n".getBytes());
            //writer.write(System.lineSeparator());
            writer.write(text.getBytes());
            writer.write("\nnull\nnull".getBytes());

            //TODO: remove
            mEditText.getText().clear();
//            Toast.makeText(this, "Saved to " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();

//            GPStracker g = new GPStracker(getApplicationContext());
//            Location l = g.getLocation();
//            ((Global) this.getApplication()).setmCurrentLocation(l);
            if (!text.equals("") && school != -1) {
//                double lat = l.getLatitude();
//                double lon = l.getLongitude();
//
//                Geocoder geocoder;
//                List<Address> addresses;
//                geocoder = new Geocoder(this, Locale.getDefault());
//
//                addresses = geocoder.getFromLocation(lat, lon, 1);
//                String address = addresses.get(0).getAddressLine(0);
//                Log.d("ADDRESS", address);
//
//                String streetAddress=address.split(",")[0].replaceAll("\\s+", "+").toLowerCase();
//                String zipcode=address.split(",")[2].split("\\s+")[2].toLowerCase();
//                String gradeId="";
//
//                if (school==0) gradeId="12";
//                else if (school==1) gradeId="15";
//
//                writer.write("\n".getBytes());
//                writer.write(address.getBytes());
//
//                final String print = text+"\n"+address;
//
//                try {
//                    OkHttpClient client=new OkHttpClient();
//
//                    String url = "http://www.infofinderi.com/ifi/ws/SearchRequest.asmx/SearchAddress?clientId=HCP2IOASIJVW&streetAddress="+
//                            streetAddress+
//                            "&zipCode="+zipcode+"&schoolId=-1&gradeId="+gradeId;
//
//                    Request request = new Request.Builder()
//                            .url(url)
//                            .build();
//
//                    client.newCall(request).enqueue(new Callback() {
//                        @Override
//                        public void onFailure(Call call, IOException e) {
//                            e.printStackTrace();
//                        }
//
//                        @Override
//                        public void onResponse(Call call, Response response) throws IOException {
//                            if (response.isSuccessful()){
//                                String text = response.body().string();
//                                String[] manip = text.substring(text.indexOf("schoolName")+13, text.indexOf("schoolAddress")-3)
//                                        .split("\\s+");
//                                text = "";
//                                for (int i=0;i<manip.length-1;i++) {
//                                    text += manip[i].substring(0,1);
//                                }
//                                text += manip[manip.length-1];
//                                Log.d("SCHOOL", text);
//
//                                fos = openFileOutput(FILE_NAME, MODE_APPEND | MODE_PRIVATE);
//                                fos.write("\n".getBytes());
//                                fos.write(text.getBytes());
//                                fos.close();
//
//                                String print1 = "\n" + text;
//
                                Intent intent = new Intent(MainActivity.this, GPSLocator.class);
//                                intent.putExtra("key", print+print1);
                                startActivity(intent);
//                            }
//                        }
//                    });
//                } catch (Exception e) {}
            } else if (text.equals("")) {
                Toast.makeText(this, "Please enter school username.", Toast.LENGTH_LONG).show();

            } else if (school == -1) {
                Toast.makeText(this, "Please select a school level.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}