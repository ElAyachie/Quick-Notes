package com.myapp.quicknotes.places;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.myapp.quicknotes.data.NearbyPlace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverpassParserTest {
    // The centre of the circle the fixture was fetched for: Copley Square, Boston.
    private static final double LAT = 42.3497;
    private static final double LON = -71.0786;

    private static String fixture() throws Exception {
        try (InputStream in = OverpassParserTest.class.getClassLoader()
                .getResourceAsStream("overpass_copley_square.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static Map<String, NearbyPlace> byName(List<NearbyPlace> places) {
        Map<String, NearbyPlace> map = new HashMap<>();
        for (NearbyPlace place : places) {
            map.put(place.getName(), place);
        }
        return map;
    }

    @Test
    public void keepsNamedShopsLandmarksStreetsAndAreas() throws Exception {
        Map<String, NearbyPlace> places =
                byName(OverpassParser.parse(fixture(), "key", LAT, LON, 200));

        assertEquals("landmark", places.get("Boston Marathon Finish Line").getKind());
        assertEquals("fast food", places.get("Sweetgreen").getKind());
        assertEquals("library", places.get("Boston Public Library").getKind());
        assertEquals("bank", places.get("Bank of America").getKind());
        assertEquals("place of worship", places.get("Old South Church").getKind());
        assertEquals("chocolate shop", places.get("Lindt").getKind());
        assertEquals("street", places.get("Boylston Street").getKind());
        assertEquals("neighbourhood", places.get("Back Bay").getKind());
        assertEquals(0, places.get("Back Bay").getDistanceMeters());
        assertEquals("town", places.get("Boston").getKind());
        assertEquals("key", places.get("Sweetgreen").getPlaceKey());
    }

    @Test
    public void leavesOutAlleysBikeDocksAndArtwork() throws Exception {
        Map<String, NearbyPlace> places =
                byName(OverpassParser.parse(fixture(), "key", LAT, LON, 200));

        assertFalse(places.containsKey("Public Alley 440"));
        assertFalse(places.containsKey("Bluebikes"));
        assertFalse(places.containsKey("Bacchante and Infant Faun"));
    }

    @Test
    public void eachNameIsKeptOnceAtItsNearestAndNearestComeFirst() throws Exception {
        List<NearbyPlace> places = OverpassParser.parse(fixture(), "key", LAT, LON, 200);

        int boylston = 0;
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getName().equals("Boylston Street")) {
                boylston++;
            }
            if (i > 0) {
                assertTrue(places.get(i - 1).getDistanceMeters()
                        <= places.get(i).getDistanceMeters());
            }
        }
        assertEquals(1, boylston);
        assertTrue(byName(places).get("Boylston Street").getDistanceMeters() < 60);
    }

    @Test
    public void placesBeyondTheRadiusAreLeftOut() throws Exception {
        Map<String, NearbyPlace> places =
                byName(OverpassParser.parse(fixture(), "key", LAT, LON, 30));

        assertTrue(places.containsKey("Boston Marathon Finish Line"));
        assertFalse(places.containsKey("Boston Public Library"));
        // Areas the centre lies in are always kept.
        assertTrue(places.containsKey("Back Bay"));
    }

    @Test
    public void onlyTheNearestPlacesAreKept() throws Exception {
        JSONArray elements = new JSONArray();
        for (int i = 0; i < OverpassParser.MAX_PLACES + 10; i++) {
            elements.put(new JSONObject()
                    .put("type", "node")
                    .put("lat", LAT + i * 0.00001)
                    .put("lon", LON)
                    .put("tags", new JSONObject().put("name", "Shop " + i).put("shop", "bakery")));
        }
        String json = new JSONObject().put("elements", elements).toString();

        List<NearbyPlace> places = OverpassParser.parse(json, "key", LAT, LON, 500);

        assertEquals(OverpassParser.MAX_PLACES, places.size());
        assertEquals("Shop 0", places.get(0).getName());
    }

    @Test
    public void queryLooksNoFurtherThanTheLimit() {
        assertTrue(OverpassParser.query(LAT, LON, 2000).contains("around:500,"));
        assertTrue(OverpassParser.query(LAT, LON, 150).contains("around:150,"));
    }

    @Test
    public void kindsCoverSynonymsAndUnlistedShops() {
        Map<String, String> chemist = new HashMap<>();
        chemist.put("shop", "chemist");
        assertEquals("pharmacy", PlaceKinds.kindOf(chemist));
        assertTrue(PlaceKinds.wordsFor("pharmacy").contains("drugstore"));

        Map<String, String> bikes = new HashMap<>();
        bikes.put("shop", "bicycle");
        assertEquals("bicycle shop", PlaceKinds.kindOf(bikes));

        Map<String, String> footpath = new HashMap<>();
        footpath.put("highway", "footway");
        assertNull(PlaceKinds.kindOf(footpath));

        assertEquals(new ArrayList<String>(), PlaceKinds.wordsFor("nothing like this"));
    }
}
