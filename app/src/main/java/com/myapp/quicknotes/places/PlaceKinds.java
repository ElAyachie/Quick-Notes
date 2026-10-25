package com.myapp.quicknotes.places;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// What a place on OpenStreetMap is, in the words people search with. OpenStreetMap describes a
// place with tags ("amenity=pharmacy", "shop=chemist"); this turns them into a kind ("pharmacy")
// and gives each kind the other words that mean it ("drugstore", "prescription").
public final class PlaceKinds {
    // Tags worth recognising a place by. Anything else named is ignored: benches, bins, parking
    // entrances and the like name nothing anyone would search for.
    private static final Map<String, String> KINDS = new HashMap<>();
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();
    // Street types; footpaths, alleys and car park lanes are left out.
    private static final Set<String> STREETS = new HashSet<>(Arrays.asList(
            "motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
            "residential", "living_street", "pedestrian"));

    static {
        kind("amenity=pharmacy", "pharmacy", "drugstore", "chemist", "medicine", "prescription");
        kind("shop=chemist", "pharmacy", "drugstore", "chemist", "medicine", "prescription");
        kind("shop=supermarket", "supermarket", "grocery", "groceries", "food", "shopping");
        kind("shop=grocery", "grocery store", "grocery", "groceries", "food", "shopping");
        kind("shop=greengrocer", "grocery store", "grocery", "groceries", "vegetables", "fruit");
        kind("shop=convenience", "convenience store", "store", "shop", "snacks");
        kind("shop=bakery", "bakery", "bread", "pastry");
        kind("shop=butcher", "butcher", "meat");
        kind("shop=department_store", "department store", "store", "shopping");
        kind("shop=mall", "mall", "shopping");
        kind("shop=hardware", "hardware store", "tools", "diy");
        kind("shop=doityourself", "hardware store", "tools", "diy");
        kind("shop=clothes", "clothes shop", "clothing", "fashion");
        kind("shop=shoes", "shoe shop", "shoes");
        kind("shop=books", "bookshop", "books", "bookstore");
        kind("shop=electronics", "electronics shop", "electronics");
        kind("shop=mobile_phone", "phone shop", "phone");
        kind("shop=hairdresser", "hairdresser", "haircut", "barber", "salon");
        kind("shop=beauty", "beauty salon", "salon");
        kind("shop=florist", "florist", "flowers");
        kind("shop=pet", "pet shop", "pet", "pets");
        kind("shop=alcohol", "liquor store", "wine", "beer");
        kind("shop=dry_cleaning", "dry cleaner", "laundry");
        kind("shop=laundry", "laundromat", "laundry");
        kind("shop=car_repair", "garage", "car", "mechanic");
        kind("amenity=school", "school");
        kind("amenity=kindergarten", "kindergarten", "preschool", "nursery", "daycare");
        kind("amenity=childcare", "daycare", "childcare", "nursery");
        kind("amenity=college", "college", "school");
        kind("amenity=university", "university", "campus", "school");
        kind("amenity=library", "library", "books");
        kind("amenity=hospital", "hospital", "doctor", "medical");
        kind("amenity=clinic", "clinic", "doctor", "medical");
        kind("amenity=doctors", "doctor's office", "doctor", "medical");
        kind("amenity=dentist", "dentist", "teeth", "dental");
        kind("amenity=veterinary", "vet", "veterinarian", "pet", "animal");
        kind("amenity=restaurant", "restaurant", "food", "dinner", "lunch", "eat");
        kind("amenity=fast_food", "fast food", "food", "takeaway", "eat");
        kind("amenity=cafe", "café", "cafe", "coffee");
        kind("amenity=bar", "bar", "drinks");
        kind("amenity=pub", "pub", "bar", "drinks");
        kind("amenity=ice_cream", "ice cream shop", "ice cream", "dessert");
        kind("amenity=bank", "bank", "money");
        kind("amenity=atm", "cash machine", "atm", "cash", "money");
        kind("amenity=post_office", "post office", "mail", "post", "parcel");
        kind("amenity=fuel", "gas station", "gas", "petrol", "fuel");
        kind("amenity=charging_station", "charging station", "charger", "ev");
        kind("amenity=place_of_worship", "place of worship", "church", "mosque", "synagogue",
                "temple");
        kind("amenity=cinema", "cinema", "movies", "movie theater");
        kind("amenity=theatre", "theatre", "theater", "show");
        kind("amenity=townhall", "town hall", "city hall");
        kind("amenity=police", "police station", "police");
        kind("amenity=fire_station", "fire station");
        kind("amenity=community_centre", "community centre", "community center");
        kind("amenity=marketplace", "market", "farmers market");
        kind("amenity=car_wash", "car wash", "car");
        kind("leisure=park", "park");
        kind("leisure=playground", "playground", "park", "kids");
        kind("leisure=fitness_centre", "gym", "fitness", "workout");
        kind("leisure=sports_centre", "sports centre", "gym", "sports");
        kind("leisure=swimming_pool", "swimming pool", "pool", "swimming");
        kind("leisure=stadium", "stadium", "sports");
        kind("leisure=pitch", "sports field", "field", "sports");
        kind("leisure=garden", "garden", "park");
        kind("tourism=museum", "museum");
        kind("tourism=hotel", "hotel");
        kind("tourism=attraction", "landmark", "attraction", "sight");
        kind("tourism=gallery", "gallery", "art");
        kind("tourism=zoo", "zoo", "animals");
        kind("historic=monument", "monument", "landmark");
        kind("historic=memorial", "memorial", "landmark");
        kind("railway=station", "train station", "train", "station");
        kind("public_transport=station", "station", "transit");
        kind("aeroway=aerodrome", "airport", "flight");
        SYNONYMS.put("street", Arrays.asList("street", "road", "avenue"));
        SYNONYMS.put("neighbourhood", Arrays.asList("neighbourhood", "neighborhood", "area"));
        SYNONYMS.put("town", Arrays.asList("town", "city"));
    }

    private PlaceKinds() {
    }

    private static void kind(String tag, String kind, String... synonyms) {
        KINDS.put(tag, kind);
        if (synonyms.length > 0 || !SYNONYMS.containsKey(kind)) {
            SYNONYMS.put(kind, Arrays.asList(synonyms));
        }
    }

    // The kind of a named place from its tags, or null if it isn't one worth keeping. Tag keys are
    // tried in a fixed order, since a place can carry several ("amenity=pharmacy" and
    // "shop=chemist" both mean pharmacy).
    @Nullable
    public static String kindOf(Map<String, String> tags) {
        for (String key : new String[]{"amenity", "shop", "leisure", "tourism", "historic",
                "railway", "public_transport", "aeroway"}) {
            String value = tags.get(key);
            if (value != null) {
                String kind = KINDS.get(key + "=" + value);
                if (kind != null) {
                    return kind;
                }
            }
        }
        String shop = tags.get("shop");
        if (shop != null && !shop.equals("yes") && !shop.equals("vacant")) {
            // A shop of a kind not listed: "shop=bicycle" is a "bicycle shop".
            return shop.replace('_', ' ') + " shop";
        }
        String highway = tags.get("highway");
        if (highway != null && STREETS.contains(highway)) {
            return "street";
        }
        String place = tags.get("place");
        if (place != null) {
            switch (place) {
                case "neighbourhood":
                case "suburb":
                case "quarter":
                    return "neighbourhood";
                case "city":
                case "town":
                case "village":
                case "hamlet":
                    return "town";
                default:
                    return null;
            }
        }
        return null;
    }

    // Words that, searched for, should find a place of this kind; the kind's own words included.
    public static List<String> wordsFor(String kind) {
        List<String> synonyms = SYNONYMS.get(kind);
        return synonyms != null ? synonyms : Collections.emptyList();
    }
}
