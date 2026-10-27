package com.myapp.quicknotes.places;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PhotonParserTest {
    // Copley Square, Boston: where the fixtures were searched from (2026-10-27).
    private static final SearchArea COPLEY =
            new SearchArea(42.3497, -71.0786, 5000.0, 42.3497, -71.0786);

    private static String fixture(String name) throws Exception {
        try (InputStream in = PhotonParserTest.class.getClassLoader().getResourceAsStream(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Test
    public void aNamedPlaceShowsItsStreetAddressAndDistance() throws Exception {
        List<PlaceSuggestion> places =
                PhotonParser.parse(fixture("photon_walgreens_copley.json"), COPLEY);

        assertEquals(6, places.size());
        PlaceSuggestion first = places.get(0);
        assertEquals("Walgreens", first.getName());
        assertEquals("24 School Street, Boston, Massachusetts", first.getAddress());
        assertTrue(first.hasLocation());
        assertEquals(42.3573365, first.getLatitude(), 1e-9);
        assertEquals(-71.0589528, first.getLongitude(), 1e-9);
        // About 1.8 km from Copley Square to School Street.
        assertNotNull(first.getDistanceMeters());
        assertTrue(first.getDistanceMeters() > 1500 && first.getDistanceMeters() < 2100);
        // No house number: the street alone.
        assertEquals("Park Street, Somerville, Massachusetts", places.get(1).getAddress());
    }

    @Test
    public void nearestComeFirst() throws Exception {
        List<PlaceSuggestion> places =
                PhotonParser.parse(fixture("photon_walgreens_copley.json"), COPLEY);

        // Photon listed the Everett store (7.7 km) before the one in Dorchester (7.6 km).
        for (int i = 1; i < places.size(); i++) {
            assertTrue(places.get(i - 1).getDistanceMeters()
                    <= places.get(i).getDistanceMeters());
        }
        assertEquals("585 Washington Street, Boston, Massachusetts",
                places.get(4).getAddress());
    }

    @Test
    public void aBareAddressIsNamedByItsStreetAndShowsItsTown() throws Exception {
        List<PlaceSuggestion> places =
                PhotonParser.parse(fixture("photon_100_boylston.json"), COPLEY);

        assertEquals("Colonial Building", places.get(0).getName());
        assertEquals("100 Boylston Street", places.get(1).getName());
        assertEquals("Boston, Massachusetts", places.get(1).getAddress());
        // Two points at the same Clinton address are listed once.
        int clinton = 0;
        for (PlaceSuggestion place : places) {
            if ("100 Boylston Street".equals(place.getName())
                    && "Clinton, Massachusetts".equals(place.getAddress())) {
                clinton++;
            }
        }
        assertEquals(1, clinton);
    }

    @Test
    public void withoutTheUsersPositionThereIsNoDistance() throws Exception {
        SearchArea noOrigin = new SearchArea(42.3497, -71.0786, 5000.0, null, null);

        PlaceSuggestion first =
                PhotonParser.parse(fixture("photon_walgreens_copley.json"), noOrigin).get(0);

        assertNull(first.getDistanceMeters());
    }
}
