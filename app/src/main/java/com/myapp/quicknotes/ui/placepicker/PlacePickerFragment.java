package com.myapp.quicknotes.ui.placepicker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.databinding.FragmentPlacePickerBinding;
import com.myapp.quicknotes.reminders.LocationAccess;
import com.myapp.quicknotes.ui.common.Formats;
import com.myapp.quicknotes.ui.common.Screens;

// Chooses where a location reminder fires: tap the map to place the pin, drag the slider to set
// how close counts as arriving. The circle on the map shows that distance. The switch chooses
// between reminding on the next arrival only and on every arrival.
public class PlacePickerFragment extends Fragment {
    private static final float STREET_ZOOM = 15;
    private static final double METERS_PER_DEGREE_OF_LATITUDE = 111_320;
    private static final int CIRCLE_MARGIN_DP = 32;

    private FragmentPlacePickerBinding binding;
    private PlacePickerViewModel viewModel;
    @Nullable
    private GoogleMap map;
    @Nullable
    private Marker pin;
    @Nullable
    private Circle circle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlacePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(PlacePickerViewModel.class);

        binding.radiusSlider.setValue(viewModel.getRadiusMeters());
        binding.radiusSlider.setLabelFormatter(
                meters -> Formats.distance(requireContext(), meters));
        binding.radiusSlider.addOnChangeListener((slider, meters, fromUser) -> {
            viewModel.setRadiusMeters(meters);
            showPlace();
        });
        binding.radiusSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                keepCircleInView();
            }
        });
        binding.setReminderButton.setOnClickListener(button -> setReminder());
        if (viewModel.isEditing()) {
            binding.setReminderButton.setText(R.string.save_changes);
        }
        boolean firstTime = savedInstanceState == null;
        viewModel.getEditedReminder().observe(getViewLifecycleOwner(), reminder -> {
            // The text field and switch keep what the user entered across a rotation.
            if (firstTime) {
                binding.placeNameInput.setText(reminder.getPlaceName());
                binding.repeatSwitch.setChecked(reminder.getRepeat() == Repeat.EVERY_ARRIVAL);
            }
            binding.radiusSlider.setValue(viewModel.getRadiusMeters());
            showPlace();
            if (firstTime) {
                showEditedPlaceOnMap();
            }
        });
        showPlace();

        // The map restores its own camera position after a rotation; it only needs pointing at
        // the user's surroundings, or the reminder's place, the first time.
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(readyMap -> onMapReady(readyMap, firstTime));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        map = null;
        pin = null;
        circle = null;
    }

    @SuppressLint("MissingPermission")
    private void onMapReady(GoogleMap readyMap, boolean firstTime) {
        if (binding == null) {
            return;
        }
        map = readyMap;
        map.setOnMapClickListener(place -> {
            viewModel.setPlace(place);
            showPlace();
            keepCircleInView();
        });
        showPlace();
        if (firstTime) {
            showEditedPlaceOnMap();
        }
        if (!LocationAccess.hasPreciseLocation(requireContext())) {
            return;
        }
        map.setMyLocationEnabled(true);
        if (firstTime && !viewModel.isEditing() && viewModel.getPlace() == null) {
            LocationServices.getFusedLocationProviderClient(requireContext())
                    .getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && map != null && viewModel.getPlace() == null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()),
                                    STREET_ZOOM));
                        }
                    });
        }
    }

    // Brings the pin, the circle and the text below the map in line with the chosen place and
    // distance. Safe to call before the map is ready or before a place is chosen.
    private void showPlace() {
        LatLng place = viewModel.getPlace();
        float radius = viewModel.getRadiusMeters();
        binding.setReminderButton.setEnabled(place != null);
        binding.statusText.setText(place == null
                ? getString(R.string.tap_map_to_choose)
                : getString(R.string.remind_within, Formats.distance(requireContext(), radius)));
        if (map == null || place == null) {
            return;
        }
        if (pin == null || circle == null) {
            int red = ContextCompat.getColor(requireContext(), R.color.app_bar);
            pin = map.addMarker(new MarkerOptions().position(place));
            circle = map.addCircle(new CircleOptions()
                    .center(place)
                    .radius(radius)
                    .strokeColor(red)
                    .strokeWidth(4)
                    .fillColor(ColorUtils.setAlphaComponent(red, 48)));
        } else {
            pin.setPosition(place);
            circle.setCenter(place);
            circle.setRadius(radius);
        }
    }

    // When changing a reminder, the map opens on that reminder's place. The map and the reminder
    // arrive separately, so this runs after each and acts once both are there.
    private void showEditedPlaceOnMap() {
        LatLng place = viewModel.getPlace();
        if (map == null || place == null || viewModel.getEditedReminder().getValue() == null) {
            return;
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, STREET_ZOOM));
        // The map has to be laid out before it can say what is visible.
        binding.getRoot().post(() -> {
            if (binding != null) {
                keepCircleInView();
            }
        });
    }

    // Zooms out when the circle doesn't fit on screen, so the whole area stays visible. The map
    // is otherwise left where the user put it.
    private void keepCircleInView() {
        LatLng place = viewModel.getPlace();
        if (map == null || place == null) {
            return;
        }
        float radius = viewModel.getRadiusMeters();
        double latitudeSpan = radius / METERS_PER_DEGREE_OF_LATITUDE;
        // A degree of longitude shrinks towards the poles.
        double longitudeSpan = latitudeSpan / Math.cos(Math.toRadians(place.latitude));
        LatLngBounds circleBounds = new LatLngBounds(
                new LatLng(place.latitude - latitudeSpan, place.longitude - longitudeSpan),
                new LatLng(place.latitude + latitudeSpan, place.longitude + longitudeSpan));
        LatLngBounds visible = map.getProjection().getVisibleRegion().latLngBounds;
        if (!visible.contains(circleBounds.southwest) || !visible.contains(circleBounds.northeast)) {
            int margin = Math.round(CIRCLE_MARGIN_DP * getResources().getDisplayMetrics().density);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(circleBounds, margin));
        }
    }

    private void setReminder() {
        String name = String.valueOf(binding.placeNameInput.getText()).trim();
        if (!viewModel.save(name.isEmpty() ? null : name, binding.repeatSwitch.isChecked())) {
            return;
        }
        // With location switched off the reminder is stored but can't fire, so say so.
        int message;
        if (!LocationAccess.isLocationOn(requireContext())) {
            message = R.string.location_reminder_set_but_location_off;
        } else if (viewModel.isEditing()) {
            message = R.string.reminder_updated;
        } else {
            message = R.string.location_reminder_set;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        Screens.hideKeyboard(binding.getRoot());
        NavHostFragment.findNavController(this).popBackStack();
    }
}
