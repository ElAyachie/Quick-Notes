package com.myapp.quicknotes.places;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

public class GooglePlacesParserTest {
    // Shaped like an answer from places:autocomplete, as documented by Google: two places, one
    // without a structured name or distance, and a search phrase, which the picker can't use.
    private static final String AUTOCOMPLETE = "{\"suggestions\": ["
            + "{\"placePrediction\": {\"place\": \"places/ChIJw1\", \"placeId\": \"ChIJw1\","
            + " \"text\": {\"text\": \"Walgreens, Boylston Street, Boston, MA, USA\"},"
            + " \"structuredFormat\": {\"mainText\": {\"text\": \"Walgreens\","
            + " \"matches\": [{\"endOffset\": 9}]},"
            + " \"secondaryText\": {\"text\": \"Boylston Street, Boston, MA, USA\"}},"
            + " \"types\": [\"pharmacy\", \"store\"], \"distanceMeters\": 420}},"
            + "{\"placePrediction\": {\"place\": \"places/ChIJw2\", \"placeId\": \"ChIJw2\","
            + " \"text\": {\"text\": \"Walgreens Pharmacy\"}}},"
            + "{\"queryPrediction\": {\"text\": {\"text\": \"walgreens near me\"}}}"
            + "]}";

    @Test
    public void readsPlacesWithTheirAddressAndDistance() throws Exception {
        List<PlaceSuggestion> places = GooglePlacesParser.parseAutocomplete(AUTOCOMPLETE);

        assertEquals(2, places.size());
        PlaceSuggestion first = places.get(0);
        assertEquals(PlaceSuggestion.Kind.FOUND, first.getKind());
        assertEquals("ChIJw1", first.getGooglePlaceId());
        assertEquals("Walgreens", first.getName());
        assertEquals("Boylston Street, Boston, MA, USA", first.getAddress());
        assertEquals(Integer.valueOf(420), first.getDistanceMeters());
        // Google gives coordinates only when asked for the place's details.
        assertFalse(first.hasLocation());
    }

    @Test
    public void fallsBackToTheFullTextWithoutAStructuredName() throws Exception {
        PlaceSuggestion second = GooglePlacesParser.parseAutocomplete(AUTOCOMPLETE).get(1);

        assertEquals("Walgreens Pharmacy", second.getName());
        assertNull(second.getAddress());
        assertNull(second.getDistanceMeters());
    }

    @Test
    public void anAnswerWithNoSuggestionsIsEmpty() throws Exception {
        assertTrue(GooglePlacesParser.parseAutocomplete("{}").isEmpty());
    }

    @Test
    public void requestPrefersTheAreaAndMeasuresFromTheUser() throws Exception {
        SearchArea area = new SearchArea(42.35, -71.08, 8000.0, 42.36, -71.06);

        JSONObject body = new JSONObject(
                GooglePlacesParser.autocompleteRequest("walgreens", area, "token-1", "en-US"));

        assertEquals("walgreens", body.getString("input"));
        assertEquals("token-1", body.getString("sessionToken"));
        assertFalse(body.getBoolean("includeQueryPredictions"));
        JSONObject circle = body.getJSONObject("locationBias").getJSONObject("circle");
        assertEquals(42.35, circle.getJSONObject("center").getDouble("latitude"), 1e-9);
        assertEquals(8000, circle.getDouble("radius"), 1e-9);
        assertEquals(-71.06, body.getJSONObject("origin").getDouble("longitude"), 1e-9);
        assertEquals("en-US", body.getString("languageCode"));
    }

    @Test
    public void requestWithoutAnAreaLeavesItToGoogle() throws Exception {
        SearchArea nowhere = new SearchArea(null, null, SearchArea.MAX_RADIUS_METERS, null, null);

        JSONObject body = new JSONObject(
                GooglePlacesParser.autocompleteRequest("cvs", nowhere, "token-2", null));

        assertFalse(body.has("locationBias"));
        assertFalse(body.has("origin"));
        assertFalse(body.has("languageCode"));
    }

    @Test
    public void circleIsCappedAtWhatGoogleAccepts() {
        SearchArea wide = new SearchArea(42.35, -71.08, 400_000.0, null, null);

        assertEquals(SearchArea.MAX_RADIUS_METERS, wide.getRadiusMeters(), 1e-9);
    }

    @Test
    public void readsTheLocationFromPlaceDetails() throws Exception {
        LatLng location = GooglePlacesParser.parseLocation(
                "{\"location\": {\"latitude\": 42.3497, \"longitude\": -71.0786}}");

        assertEquals(42.3497, location.latitude, 1e-9);
        assertEquals(-71.0786, location.longitude, 1e-9);
        assertNull(GooglePlacesParser.parseLocation("{}"));
    }
}
