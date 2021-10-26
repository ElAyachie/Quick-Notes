package QuickNotes;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import QuickNotes.Services.MyLocationListener;

// Google maps api.
// Shows the user a map with marker over their current location and allows them to put another marker.
public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {
    Context context;
    Activity activity;
    LocationManager locationManager;
    MyLocationListener locationListener;
    Marker marker;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        context = getContext();
        activity = getActivity();
        // Add a marker to the users location.
        assert context != null;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener(context, activity);
        // The current location of the user.
        double currentLatitude = locationListener.getLatitude();
        double currentLongitude = locationListener.getLongitude();
        LatLng currentLocation = new LatLng(currentLatitude, currentLongitude);
        googleMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title("Your location"));
        float zoomLevel = 12.0f;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel));
        // Setting up the dynamic marker.
        googleMap.setOnMapClickListener((LatLng arg0) -> {
            if (marker != null) {
                marker.remove();
            }
            marker = googleMap.addMarker(new MarkerOptions().position(
                    new LatLng(arg0.latitude, arg0.longitude)).draggable(true).visible(true).title("Destination"));
        });
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public Marker getMarker() {
        return marker;
    }
}
