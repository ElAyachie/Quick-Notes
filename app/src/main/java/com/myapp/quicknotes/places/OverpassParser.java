package com.myapp.quicknotes.places;

import com.myapp.quicknotes.data.NearbyPlace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Turns an OpenStreetMap Overpass answer into the places worth keeping near a reminder: the areas
// its centre lies in, the nearest named streets, and the nearest named shops, schools, landmarks
// and the like. Each name is kept once, at its nearest.
public final class OverpassParser {
    // How far from the centre places are looked for. A larger circle in a city holds thousands of
    // places, slow to fetch and too many to be useful; the nearest ones say where the place is.
    static final int MAX_SEARCH_METERS = 500;
    static final int MAX_STREETS = 8;
    static final int MAX_PLACES = 40;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private OverpassParser() {
    }

    // The Overpass query for named things within the circle (up to MAX_SEARCH_METERS) and the
    // neighbourhood and town the centre is in.
    public static String query(double latitude, double longitude, float radiusMeters) {
        String around = String.format(Locale.US, "around:%d,%.6f,%.6f",
                Math.round(Math.min(radiusMeters, MAX_SEARCH_METERS)), latitude, longitude);
        String isIn = String.format(Locale.US, "%.6f,%.6f", latitude, longitude);
        return "[out:json][timeout:60];("
                + "nwr(" + around + ")[\"name\"][\"shop\"];"
                + "nwr(" + around + ")[\"name\"][\"amenity\"];"
                + "nwr(" + around + ")[\"name\"][\"leisure\"];"
                + "nwr(" + around + ")[\"name\"][\"tourism\"];"
                + "nwr(" + around + ")[\"name\"][\"historic\"];"
                + "nwr(" + around + ")[\"name\"][\"railway\"=\"station\"];"
                + "way(" + around + ")[\"name\"][\"highway\"];"
                + ");out center tags;"
                + "is_in(" + isIn + ")->.areas;"
                + "area.areas[\"name\"][\"place\"];out tags;";
    }

    public static List<NearbyPlace> parse(String json, String placeKey, double latitude,
                                          double longitude, float radiusMeters)
            throws JSONException {
        double limit = Math.min(radiusMeters, MAX_SEARCH_METERS);
        // Nearest first, so the first of each name seen is the one kept.
        List<Candidate> candidates = new ArrayList<>();
        JSONArray elements = new JSONObject(json).getJSONArray("elements");
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            Map<String, String> tags = tags(element.optJSONObject("tags"));
            String name = tags.get("name");
            String kind = PlaceKinds.kindOf(tags);
            if (name == null || name.trim().isEmpty() || kind == null) {
                continue;
            }
            double distance;
            if ("area".equals(element.optString("type"))) {
                // The centre lies inside it.
                distance = 0;
            } else {
                JSONObject point = element.has("lat") ? element : element.optJSONObject("center");
                if (point == null) {
                    continue;
                }
                distance = distanceMeters(latitude, longitude,
                        point.getDouble("lat"), point.getDouble("lon"));
                if (distance > limit) {
                    // A long street's centre can lie outside although the street passes by;
                    // anything else that far is beyond the circle.
                    if (!kind.equals("street") || distance > limit * 2) {
                        continue;
                    }
                }
            }
            candidates.add(new Candidate(name.trim(), kind, (int) Math.round(distance)));
        }
        candidates.sort((a, b) -> Integer.compare(a.distance, b.distance));

        Map<String, Candidate> byName = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            byName.putIfAbsent(candidate.name.toLowerCase(Locale.ROOT), candidate);
        }
        List<NearbyPlace> kept = new ArrayList<>();
        int streets = 0;
        int places = 0;
        for (Candidate candidate : byName.values()) {
            boolean area = candidate.kind.equals("neighbourhood") || candidate.kind.equals("town");
            if (candidate.kind.equals("street")) {
                if (streets++ >= MAX_STREETS) {
                    continue;
                }
            } else if (!area && places++ >= MAX_PLACES) {
                continue;
            }
            kept.add(new NearbyPlace(0, placeKey, candidate.name, candidate.kind,
                    candidate.distance));
        }
        return kept;
    }

    private static Map<String, String> tags(JSONObject json) throws JSONException {
        Map<String, String> tags = new HashMap<>();
        if (json != null) {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                tags.put(key, json.getString(key));
            }
        }
        return tags;
    }

    // Distance along the earth's surface (haversine).
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    private static final class Candidate {
        final String name;
        final String kind;
        final int distance;

        Candidate(String name, String kind, int distance) {
            this.name = name;
            this.kind = kind;
            this.distance = distance;
        }
    }
}
