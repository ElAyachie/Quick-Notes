package com.myapp.quicknotes.places;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Writes requests to Google's Places API (New) and reads its answers. Kept apart from the network
// code so it can be tested on the computer.
final class GooglePlacesParser {
    private GooglePlacesParser() {
    }

    // The body of an autocomplete request: what was typed, preferring places in the area when
    // there is one (Google otherwise guesses from the connection), with distances from the user
    // when their position is known. Only places are asked for, not suggested search phrases.
    static String autocompleteRequest(String input, SearchArea area, String sessionToken,
                                      @Nullable String languageCode) throws JSONException {
        JSONObject body = new JSONObject()
                .put("input", input)
                .put("sessionToken", sessionToken)
                .put("includeQueryPredictions", false);
        if (area.hasCenter()) {
            JSONObject circle = new JSONObject()
                    .put("center", latLng(area.getLatitude(), area.getLongitude()))
                    .put("radius", area.getRadiusMeters());
            body.put("locationBias", new JSONObject().put("circle", circle));
        }
        if (area.hasOrigin()) {
            body.put("origin", latLng(area.getOriginLatitude(), area.getOriginLongitude()));
        }
        if (languageCode != null) {
            body.put("languageCode", languageCode);
        }
        return body.toString();
    }

    // The suggested places, in Google's order.
    static List<PlaceSuggestion> parseAutocomplete(String json) throws JSONException {
        List<PlaceSuggestion> suggestions = new ArrayList<>();
        JSONArray items = new JSONObject(json).optJSONArray("suggestions");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject prediction = items.getJSONObject(i).optJSONObject("placePrediction");
            if (prediction == null) {
                continue;
            }
            String placeId = prediction.optString("placeId");
            JSONObject format = prediction.optJSONObject("structuredFormat");
            String name = format != null ? text(format.optJSONObject("mainText")) : null;
            String address = format != null ? text(format.optJSONObject("secondaryText")) : null;
            if (name == null) {
                name = text(prediction.optJSONObject("text"));
            }
            if (placeId.isEmpty() || name == null) {
                continue;
            }
            Integer distance = prediction.has("distanceMeters")
                    ? prediction.getInt("distanceMeters") : null;
            suggestions.add(PlaceSuggestion.foundByGoogle(placeId, name, address, distance));
        }
        return suggestions;
    }

    // A place's coordinates from a details answer, or null when it has none.
    @Nullable
    static LatLng parseLocation(String json) throws JSONException {
        JSONObject location = new JSONObject(json).optJSONObject("location");
        if (location == null || !location.has("latitude") || !location.has("longitude")) {
            return null;
        }
        return new LatLng(location.getDouble("latitude"), location.getDouble("longitude"));
    }

    private static JSONObject latLng(double latitude, double longitude) throws JSONException {
        return new JSONObject().put("latitude", latitude).put("longitude", longitude);
    }

    @Nullable
    private static String text(@Nullable JSONObject formatted) {
        if (formatted == null) {
            return null;
        }
        String text = formatted.optString("text").trim();
        return text.isEmpty() ? null : text;
    }
}
