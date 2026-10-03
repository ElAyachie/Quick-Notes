package com.myapp.quicknotes.ui.placepicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.reminders.LocationAccess;

// Asks for what a location reminder needs, in the order Android requires: precise location
// first, then permission to use it while the app is closed. Create it together with the fragment
// (as a field), because that is the only time result callbacks can be registered.
public class LocationPermissionRequest {
    private final Fragment fragment;
    private final Runnable onGranted;
    private final ActivityResultLauncher<String[]> preciseLocation;
    private final ActivityResultLauncher<String> backgroundLocation;

    public LocationPermissionRequest(Fragment fragment, Runnable onGranted) {
        this.fragment = fragment;
        this.onGranted = onGranted;
        preciseLocation = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (LocationAccess.hasPreciseLocation(context())) {
                        askForBackgroundLocation();
                    } else {
                        explainRefusal(R.string.precise_location_needed);
                    }
                });
        backgroundLocation = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        onGranted.run();
                    } else {
                        explainRefusal(R.string.background_location_needed);
                    }
                });
    }

    // Runs onGranted once everything is allowed; otherwise tells the user what is missing.
    public void start() {
        if (LocationAccess.hasPreciseLocation(context())) {
            askForBackgroundLocation();
        } else {
            // Android only offers "precise" when approximate location is requested alongside it.
            preciseLocation.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    // The system gives no context for this request (on Android 11+ it opens a settings page), so
    // the app says first why it is asking and which option to choose.
    private void askForBackgroundLocation() {
        if (LocationAccess.hasBackgroundLocation(context())) {
            onGranted.run();
            return;
        }
        new MaterialAlertDialogBuilder(context())
                .setTitle(R.string.background_location_title)
                .setMessage(fragment.getString(R.string.background_location_message,
                        allTheTimeLabel()))
                .setPositiveButton(R.string.continue_label, (dialog, which) ->
                        backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    private void explainRefusal(@StringRes int message) {
        new MaterialAlertDialogBuilder(context())
                .setMessage(message)
                .setPositiveButton(R.string.open_settings, (dialog, which) ->
                        fragment.startActivity(new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + context().getPackageName()))))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // The wording of the "all the time" option on this device, so the message matches the screen.
    private CharSequence allTheTimeLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context().getPackageManager().getBackgroundPermissionOptionLabel();
        }
        return fragment.getString(R.string.allow_all_the_time);
    }

    private Context context() {
        return fragment.requireContext();
    }
}
