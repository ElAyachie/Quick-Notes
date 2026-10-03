package com.myapp.quicknotes.reminders;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

// What the app is currently allowed to know about the device's location.
public final class LocationAccess {
    private LocationAccess() {
    }

    // Approximate location is not enough to tell when a place has been reached.
    public static boolean hasPreciseLocation(Context context) {
        return isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    // "Allow all the time": needed for a reminder to fire while the app is closed.
    public static boolean hasBackgroundLocation(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static boolean canWatchPlaces(Context context) {
        return hasPreciseLocation(context) && hasBackgroundLocation(context);
    }

    // False when location is switched off for the whole device.
    public static boolean isLocationOn(Context context) {
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager);
    }

    private static boolean isGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
