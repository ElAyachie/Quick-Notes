package com.myapp.quicknotes.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.Collection;
import java.util.List;

@Dao
public interface PlaceDao {
    @Query("SELECT COUNT(*) > 0 FROM place_lookups WHERE place_key = :placeKey")
    boolean isLookedUp(String placeKey);

    @Query("SELECT * FROM nearby_places WHERE place_key IN (:placeKeys) "
            + "ORDER BY distance_meters")
    List<NearbyPlace> getNearby(Collection<String> placeKeys);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLookup(PlaceLookup lookup);

    @Insert
    void insertNearby(List<NearbyPlace> places);

    @Query("DELETE FROM nearby_places WHERE place_key = :placeKey")
    void deleteNearby(String placeKey);

    // Stores what was found around a place, in place of anything found there before.
    @Transaction
    default void saveLookup(PlaceLookup lookup, List<NearbyPlace> places) {
        insertLookup(lookup);
        deleteNearby(lookup.getPlaceKey());
        insertNearby(places);
    }

    // Lookups no waiting reminder needs any more; deleting one deletes its places.
    @Query("DELETE FROM place_lookups WHERE place_key NOT IN (:placeKeysInUse)")
    void deleteLookupsExcept(Collection<String> placeKeysInUse);
}
