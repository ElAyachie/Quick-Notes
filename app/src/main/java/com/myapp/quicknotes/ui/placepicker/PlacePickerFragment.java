package com.myapp.quicknotes.ui.placepicker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchView;
import com.google.android.material.slider.Slider;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Repeat;
import com.myapp.quicknotes.data.SavedPlace;
import com.myapp.quicknotes.databinding.DialogPlaceNameBinding;
import com.myapp.quicknotes.databinding.FragmentPlacePickerBinding;
import com.myapp.quicknotes.places.OverpassParser;
import com.myapp.quicknotes.places.PlaceSearchRepository;
import com.myapp.quicknotes.places.PlaceSuggestion;
import com.myapp.quicknotes.places.SearchArea;
import com.myapp.quicknotes.reminders.LocationAccess;
import com.myapp.quicknotes.ui.common.Formats;
import com.myapp.quicknotes.ui.common.Screens;

import java.util.List;
import java.util.function.Consumer;

// Chooses where a location reminder fires. The search box at the top finds a place by name or
// address, nearest first, like a map app's; the chips beneath it are the user's saved and recent
// places. The map can be tapped too (tapping a shop or landmark's icon also fills in its name).
// The slider sets how close counts as being there, shown by the circle on the map. The two buttons
// at the top of the panel choose between reminding on arriving at the place and on leaving it; the
// switch chooses between reminding the next time only and every time. The bookmark in the name
// field saves the place for next time.
public class PlacePickerFragment extends Fragment {
    private static final float STREET_ZOOM = 15;
    private static final double METERS_PER_DEGREE_OF_LATITUDE = 111_320;
    private static final int CIRCLE_MARGIN_DP = 32;
    // Zoomed out further than this, the map shows too much to say where to search.
    private static final float MIN_SEARCH_ZOOM = 6;
    // A zoomed-in map still searches the neighbourhood, not just the few streets on screen.
    private static final double MIN_SEARCH_RADIUS_METERS = 5_000;

    private FragmentPlacePickerBinding binding;
    private PlacePickerViewModel viewModel;
    @Nullable
    private GoogleMap map;
    @Nullable
    private Marker pin;
    @Nullable
    private Circle circle;
    // The name last filled in from a tapped shop or landmark, a search result or a saved place.
    // Choosing another replaces it, but never a name the user typed.
    @Nullable
    private String filledInName;
    // Where the user is, when known: search results are measured from here.
    @Nullable
    private LatLng userLocation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlacePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(PlacePickerViewModel.class);

        showPlaceTrigger();
        binding.placeTriggerToggle.addOnButtonCheckedListener((group, buttonId, checked) -> {
            if (checked) {
                viewModel.setPlaceTrigger(buttonId == R.id.leaving_button
                        ? PlaceTrigger.LEAVING : PlaceTrigger.ARRIVING);
                showPlace();
            }
        });
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
        binding.placeNameLayout.setEndIconOnClickListener(button -> saveOrEditChosenPlace());
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
            showPlaceTrigger();
            showPlace();
            if (firstTime) {
                showEditedPlaceOnMap();
            }
        });
        setUpSearch();
        viewModel.getQuickPlaces().observe(getViewLifecycleOwner(), this::showQuickPlaces);
        // The bookmark shows whether the chosen place is saved.
        viewModel.getSavedPlaces().observe(getViewLifecycleOwner(), places -> showPlace());
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
        // The bottom panel overlaps the map's lower edge, and the search box and chips float over
        // its top; keep the map's own controls, its logo and the centre of the camera clear of
        // them.
        binding.mapTopControls.addOnLayoutChangeListener(
                (controls, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (map != null) {
                        map.setPadding(0, bottom, 0,
                                getResources().getDimensionPixelSize(R.dimen.map_overlap));
                    }
                });
        map.setPadding(0, binding.mapTopControls.getBottom(), 0,
                getResources().getDimensionPixelSize(R.dimen.map_overlap));
        map.setOnMapClickListener(place -> {
            viewModel.setPlace(place);
            showPlace();
            keepCircleInView();
        });
        map.setOnPoiClickListener(poi -> {
            viewModel.setPlace(poi.latLng);
            // Map labels can run over two lines.
            fillInName(poi.name.replace('\n', ' ').trim());
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
        LocationServices.getFusedLocationProviderClient(requireContext())
                .getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        return;
                    }
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (firstTime && map != null && !viewModel.isEditing()
                            && viewModel.getPlace() == null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,
                                STREET_ZOOM));
                    }
                });
    }

    private void setUpSearch() {
        PlaceSuggestionAdapter adapter =
                new PlaceSuggestionAdapter(this::choose, this::showPlaceOptions);
        binding.suggestionList.setAdapter(adapter);
        binding.searchView.setupWithSearchBar(binding.searchBar);
        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                viewModel.setQuery(text.toString(), searchArea());
            }
        });
        // Back closes the search, not the screen behind it.
        OnBackPressedCallback closeSearch = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                binding.searchView.hide();
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), closeSearch);
        binding.searchView.addTransitionListener((searchView, previous, next) -> {
            closeSearch.setEnabled(next == SearchView.TransitionState.SHOWING
                    || next == SearchView.TransitionState.SHOWN);
            if (next == SearchView.TransitionState.SHOWING) {
                // Saved and recent places are listed before anything is typed.
                viewModel.setQuery(String.valueOf(searchView.getText()), searchArea());
            }
        });
        viewModel.getSearchState().observe(getViewLifecycleOwner(), state -> {
            adapter.submitList(state.places);
            binding.searchProgress.setVisibility(state.searching ? View.VISIBLE : View.INVISIBLE);
            showSearchMessage(state);
            showSearchCredit(state.source);
        });
    }

    private void showSearchMessage(PlacePickerViewModel.SearchState state) {
        @StringRes int message = 0;
        if (state.failed) {
            message = R.string.place_search_failed;
        } else if (state.nothingFound) {
            message = R.string.no_places_found;
        } else if (state.places.isEmpty() && !state.searching) {
            message = R.string.search_places_empty;
        }
        binding.searchMessage.setVisibility(message != 0 ? View.VISIBLE : View.GONE);
        if (message != 0) {
            binding.searchMessage.setText(message);
        }
    }

    private void showSearchCredit(@Nullable PlaceSearchRepository.Source source) {
        binding.searchCredit.setVisibility(source != null ? View.VISIBLE : View.GONE);
        if (source != null) {
            binding.searchCredit.setText(source == PlaceSearchRepository.Source.GOOGLE
                    ? R.string.google_maps_credit : R.string.photon_credit);
        }
    }

    // Prefers places in what the map shows, measured from the user. A map showing half the world
    // says nothing about where to look; then the search goes by the user's position, if known.
    private SearchArea searchArea() {
        Double originLatitude = userLocation != null ? userLocation.latitude : null;
        Double originLongitude = userLocation != null ? userLocation.longitude : null;
        if (map != null && map.getCameraPosition().zoom >= MIN_SEARCH_ZOOM) {
            LatLng center = map.getCameraPosition().target;
            VisibleRegion visible = map.getProjection().getVisibleRegion();
            double radius = OverpassParser.distanceMeters(center.latitude, center.longitude,
                    visible.farLeft.latitude, visible.farLeft.longitude);
            return new SearchArea(center.latitude, center.longitude,
                    Math.max(radius, MIN_SEARCH_RADIUS_METERS), originLatitude, originLongitude);
        }
        return new SearchArea(originLatitude, originLongitude, SearchArea.MAX_RADIUS_METERS,
                originLatitude, originLongitude);
    }

    // Chooses a place from the search list or the chips: the pin goes there, the map follows, and
    // its name fills the name field.
    private void choose(PlaceSuggestion place) {
        binding.searchProgress.setVisibility(View.VISIBLE);
        viewModel.locate(place, location -> {
            if (binding == null) {
                return;
            }
            binding.searchProgress.setVisibility(View.INVISIBLE);
            if (location == null) {
                Toast.makeText(requireContext(), R.string.place_locate_failed,
                        Toast.LENGTH_LONG).show();
                return;
            }
            binding.searchView.hide();
            binding.searchBar.setText(place.getName());
            viewModel.setPlace(location);
            fillInName(place.getName());
            showPlace();
            if (map != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomToFit(location)));
            }
        });
    }

    private void fillInName(String name) {
        String typed = String.valueOf(binding.placeNameInput.getText()).trim();
        if (typed.isEmpty() || typed.equals(filledInName)) {
            filledInName = name;
            binding.placeNameInput.setText(name);
        }
    }

    private void showQuickPlaces(List<PlaceSuggestion> places) {
        binding.placeChips.removeAllViews();
        for (PlaceSuggestion place : places) {
            Chip chip = (Chip) getLayoutInflater().inflate(
                    R.layout.chip_place, binding.placeChips, false);
            chip.setText(place.getName());
            chip.setChipIconResource(PlaceSuggestionAdapter.iconFor(place.getKind()));
            chip.setOnClickListener(button -> choose(place));
            chip.setOnLongClickListener(button -> {
                showPlaceOptions(place);
                return true;
            });
            binding.placeChips.addView(chip);
        }
        binding.placeChipsScroll.setVisibility(places.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // On a long press: a saved place can be renamed or removed; any other place can be saved.
    private void showPlaceOptions(PlaceSuggestion place) {
        if (place.getKind() == PlaceSuggestion.Kind.SAVED) {
            SavedPlace saved = viewModel.savedPlaceWithId(place.getSavedPlaceId());
            if (saved != null) {
                showSavedPlaceOptions(saved);
            }
            return;
        }
        askForPlaceName(R.string.save_place_title, place.getName(), name -> {
            viewModel.locate(place, location -> {
                if (location != null) {
                    viewModel.savePlace(name, location);
                    toast(getString(R.string.place_saved, name));
                }
            });
        });
    }

    // The bookmark in the name field: saves the chosen place under the name typed (or asks for
    // one), or, when it is saved already, offers to rename or remove it.
    private void saveOrEditChosenPlace() {
        LatLng place = viewModel.getPlace();
        if (place == null) {
            return;
        }
        SavedPlace saved = viewModel.savedPlaceAt(place);
        if (saved != null) {
            showSavedPlaceOptions(saved);
            return;
        }
        String typed = String.valueOf(binding.placeNameInput.getText()).trim();
        askForPlaceName(R.string.save_place_title, typed, name -> {
            viewModel.savePlace(name, place);
            if (String.valueOf(binding.placeNameInput.getText()).trim().isEmpty()) {
                binding.placeNameInput.setText(name);
            }
            toast(getString(R.string.place_saved, name));
        });
    }

    private void showSavedPlaceOptions(SavedPlace saved) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(saved.getName())
                .setItems(new CharSequence[]{
                                getString(R.string.rename), getString(R.string.remove)},
                        (dialog, which) -> {
                            if (which == 0) {
                                askForPlaceName(R.string.rename_place_title, saved.getName(),
                                        name -> viewModel.renamePlace(saved, name));
                            } else {
                                viewModel.removePlace(saved);
                                toast(getString(R.string.place_removed, saved.getName()));
                            }
                        })
                .show();
    }

    private void askForPlaceName(@StringRes int title, @Nullable String suggestedName,
                                 Consumer<String> onName) {
        DialogPlaceNameBinding form = DialogPlaceNameBinding.inflate(getLayoutInflater());
        form.placeNameInput.setText(suggestedName);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        // The button is wired up after the dialog is shown so that an empty name keeps it open.
        dialog.setOnShowListener(shown ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    String name = String.valueOf(form.placeNameInput.getText()).trim();
                    if (name.isEmpty()) {
                        form.placeNameLayout.setError(getString(R.string.error_enter_name));
                        return;
                    }
                    onName.accept(name);
                    dialog.dismiss();
                }));
        dialog.show();
        form.placeNameInput.requestFocus();
    }

    private void toast(String message) {
        if (binding != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPlaceTrigger() {
        binding.placeTriggerToggle.check(viewModel.getPlaceTrigger() == PlaceTrigger.LEAVING
                ? R.id.leaving_button : R.id.arriving_button);
    }

    // Brings the pin, the circle and the text below the map in line with the chosen place and
    // distance, and with whether the reminder is for arriving or leaving. Safe to call before
    // the map is ready or before a place is chosen.
    private void showPlace() {
        LatLng place = viewModel.getPlace();
        float radius = viewModel.getRadiusMeters();
        boolean leaving = viewModel.getPlaceTrigger() == PlaceTrigger.LEAVING;
        binding.setReminderButton.setEnabled(place != null);
        binding.statusText.setText(place == null
                ? getString(R.string.tap_map_to_choose)
                : getString(leaving ? R.string.remind_beyond : R.string.remind_within,
                        Formats.distance(requireContext(), radius)));
        binding.repeatSwitch.setText(leaving
                ? R.string.remind_every_departure : R.string.remind_every_arrival);
        boolean saved = place != null && viewModel.savedPlaceAt(place) != null;
        binding.placeNameLayout.setEndIconVisible(place != null);
        binding.placeNameLayout.setEndIconDrawable(
                saved ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        binding.placeNameLayout.setEndIconContentDescription(
                saved ? R.string.saved_place_options : R.string.save_this_place);
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
        LatLngBounds circleBounds = circleBounds(place);
        LatLngBounds visible = map.getProjection().getVisibleRegion().latLngBounds;
        if (!visible.contains(circleBounds.southwest) || !visible.contains(circleBounds.northeast)) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(circleBounds, circleMargin()));
        }
    }

    // The zoom at which the circle fills the map between the search box and the panel. Worked out
    // here because some versions of the map leave its padding out when fitting bounds.
    private float zoomToFit(LatLng place) {
        View mapView = getChildFragmentManager().findFragmentById(R.id.map).requireView();
        float density = getResources().getDisplayMetrics().density;
        int width = mapView.getWidth() - 2 * circleMargin();
        int height = mapView.getHeight() - binding.mapTopControls.getBottom()
                - getResources().getDimensionPixelSize(R.dimen.map_overlap) - 2 * circleMargin();
        double spaceDp = Math.max(Math.min(width, height), 1) / density;
        double diameter = 2 * viewModel.getRadiusMeters();
        // At zoom 0 the world's 40,075 km fill 256 dp; each step halves the metres per dp.
        double metersPerDpAtZoom0 = 156_543.034 * Math.cos(Math.toRadians(place.latitude));
        float zoom = (float) (Math.log(metersPerDpAtZoom0 * spaceDp / diameter) / Math.log(2));
        return Math.min(zoom, map.getMaxZoomLevel());
    }

    // The square around the circle drawn at this place.
    private LatLngBounds circleBounds(LatLng place) {
        double latitudeSpan = viewModel.getRadiusMeters() / METERS_PER_DEGREE_OF_LATITUDE;
        // A degree of longitude shrinks towards the poles.
        double longitudeSpan = latitudeSpan / Math.cos(Math.toRadians(place.latitude));
        return new LatLngBounds(
                new LatLng(place.latitude - latitudeSpan, place.longitude - longitudeSpan),
                new LatLng(place.latitude + latitudeSpan, place.longitude + longitudeSpan));
    }

    private int circleMargin() {
        return Math.round(CIRCLE_MARGIN_DP * getResources().getDisplayMetrics().density);
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
        } else if (viewModel.getPlaceTrigger() == PlaceTrigger.LEAVING) {
            message = R.string.location_reminder_set_leaving;
        } else {
            message = R.string.location_reminder_set;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        Screens.hideKeyboard(binding.getRoot());
        NavHostFragment.findNavController(this).popBackStack();
    }
}
