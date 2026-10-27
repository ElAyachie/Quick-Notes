package com.myapp.quicknotes.places;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Reads the answers of Photon, the OpenStreetMap search used when Google's can't be. Kept apart
// from the network code so it can be tested on the computer.
final class PhotonParser {
    private PhotonParser() {
    }

    // The places found, nearest first. Photon's own order weighs how well known a place is about
    // as much as how close it is, which puts a shop 40 km away above one around the corner. A
    // named place shows its address beneath the name; a bare address shows its town. The same
    // name and address twice (a building and its entrance) is listed once.
    static List<PlaceSuggestion> parse(String json, SearchArea area) throws JSONException {
        List<Nearness> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JSONArray features = new JSONObject(json).optJSONArray("features");
        for (int i = 0; features != null && i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject properties = feature.optJSONObject("properties");
            JSONObject geometry = feature.optJSONObject("geometry");
            JSONArray coordinates = geometry != null ? geometry.optJSONArray("coordinates") : null;
            if (properties == null || coordinates == null || coordinates.length() < 2) {
                continue;
            }
            String street = join(" ", value(properties, "housenumber"),
                    value(properties, "street"));
            String town = firstOf(properties, "city", "town", "village", "locality", "district",
                    "county");
            String region = join(", ", town, value(properties, "state"));
            String name = value(properties, "name");
            String address;
            if (name != null) {
                address = join(", ", street, region);
            } else if (street != null) {
                name = street;
                address = region;
            } else {
                continue;
            }
            if (!seen.add(name + "\n" + address)) {
                continue;
            }
            // GeoJSON lists longitude first.
            double longitude = coordinates.getDouble(0);
            double latitude = coordinates.getDouble(1);
            PlaceSuggestion place = PlaceSuggestion.found(name, address, latitude, longitude,
                    area.distanceTo(latitude, longitude));
            suggestions.add(new Nearness(place, distanceFrom(area, latitude, longitude)));
        }
        // A stable sort: places at no known distance keep Photon's order.
        suggestions.sort(Comparator.comparingDouble(nearness -> nearness.meters));
        List<PlaceSuggestion> places = new ArrayList<>();
        for (Nearness nearness : suggestions) {
            places.add(nearness.place);
        }
        return places;
    }

    // From the user if their position is known, otherwise from the middle of the map.
    private static double distanceFrom(SearchArea area, double latitude, double longitude) {
        if (area.hasOrigin()) {
            return OverpassParser.distanceMeters(area.getOriginLatitude(),
                    area.getOriginLongitude(), latitude, longitude);
        }
        if (area.hasCenter()) {
            return OverpassParser.distanceMeters(area.getLatitude(), area.getLongitude(),
                    latitude, longitude);
        }
        return 0;
    }

    private static final class Nearness {
        final PlaceSuggestion place;
        final double meters;

        Nearness(PlaceSuggestion place, double meters) {
            this.place = place;
            this.meters = meters;
        }
    }

    @Nullable
    private static String value(JSONObject properties, String key) {
        String text = properties.optString(key, "").trim();
        return text.isEmpty() ? null : text;
    }

    @Nullable
    private static String firstOf(JSONObject properties, String... keys) {
        for (String key : keys) {
            String text = value(properties, key);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    // The parts that are there, with the separator between them; null when none are.
    @Nullable
    private static String join(String separator, @Nullable String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(separator);
            }
            joined.append(part);
        }
        return joined.length() > 0 ? joined.toString() : null;
    }
}
