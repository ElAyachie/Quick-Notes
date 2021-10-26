package QuickNotes.Dialogs;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import QuickNotes.MapFragment;
import QuickNotes.Note;
import QuickNotes.Reminder;
import QuickNotes.ReminderFileOperations;
import QuickNotes.Services.ReminderBroadcast;
import QuickNotes.R;

// Dialog presents the user with a google map and allows them to set a marker where they would like to be sent a reminder.
// This dialog is used in the specific notes edit.
public class LocationReminderDialog extends AlertDialog.Builder {
    AlertDialog optionDialog;

    @SuppressLint("MissingPermission")
    public LocationReminderDialog(Context context, FragmentManager fragmentManger, Note note) {
        super(context);
        View alertView = View.inflate(context, R.layout.alertdialog_location, null);
        AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        Button setLocationButton = alertView.findViewById(R.id.setLocationButton);
        Button cancelButton = alertView.findViewById(R.id.cancelButton);
        MapFragment mapFragment = (MapFragment) fragmentManger.findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(mapFragment);
        cancelButton.setOnClickListener(view -> {
            optionDialog.dismiss();
            fragmentManger.beginTransaction().remove(mapFragment).commit();
        });
        setLocationButton.setOnClickListener(view -> {
            Marker marker = mapFragment.getMarker();
            if (marker != null) {
                LatLng destination = marker.getPosition();
                double destinationLatitude = destination.latitude;
                double destinationLongitude = destination.longitude;
                SharedPreferences pref = context.getSharedPreferences("MyPref", 0);
                SharedPreferences.Editor editor = pref.edit();
                int notificationID = pref.getInt("notificationID", 0);
                editor.putInt("notificationID", notificationID + 1);
                editor.apply();
                Reminder reminder = new Reminder("Location", note.getFolderName(), note.getNoteName(), note.getNoteContent(), notificationID);
                Intent intent = new Intent(context, ReminderBroadcast.class);
                intent.putExtra("reminder", reminder);
                PendingIntent pendingIntent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pendingIntent = PendingIntent.getBroadcast(context, notificationID, intent, PendingIntent.FLAG_IMMUTABLE);
                }
                mapFragment.getLocationManager().addProximityAlert(destinationLatitude, destinationLongitude, 1000, 10000, pendingIntent);
                ReminderFileOperations.saveReminder(context, reminder);
            }
            Toast.makeText(context, "Reminder set.", Toast.LENGTH_LONG).show();
            optionDialog.dismiss();
            fragmentManger.beginTransaction().remove(mapFragment).commit();
        });
        builder.setView(alertView);
        optionDialog = builder.create();
    }

    public AlertDialog show() {
        optionDialog.show();
        return null;
    }
}
