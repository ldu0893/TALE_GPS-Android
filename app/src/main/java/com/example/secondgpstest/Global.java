package com.example.secondgpstest;

import android.app.Application;
import android.location.Location;

public class Global extends Application {
    private Location mCurrentLocation;

    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public void setmCurrentLocation(Location location) {
        mCurrentLocation = location;
    }
}
