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
// and gives each kind the other names for the same thing ("drugstore"). Only names for the same
// kind of place, never related topics: a city has a bank and a cleaner near every place, so
// "money" or "laundry" would find every location reminder.
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
        kind("amenity=pharmacy", "pharmacy", "drugstore", "chemist");
        kind("shop=chemist", "pharmacy", "drugstore", "chemist");
        kind("shop=supermarket", "supermarket", "grocery store", "groceries");
        kind("shop=grocery", "grocery store", "groceries");
        kind("shop=greengrocer", "greengrocer", "grocery store", "groceries");
        kind("shop=convenience", "convenience store", "corner shop");
        kind("shop=bakery", "bakery");
        kind("shop=butcher", "butcher");
        kind("shop=department_store", "department store");
        kind("shop=mall", "mall", "shopping centre", "shopping center");
        kind("shop=hardware", "hardware store");
        kind("shop=doityourself", "hardware store", "diy store");
        kind("shop=clothes", "clothes shop", "clothing store");
        kind("shop=shoes", "shoe shop", "shoe store");
        kind("shop=books", "bookshop", "bookstore");
        kind("shop=electronics", "electronics shop", "electronics store");
        kind("shop=mobile_phone", "phone shop", "phone store");
        kind("shop=hairdresser", "hairdresser", "barber", "hair salon");
        kind("shop=beauty", "beauty salon");
        kind("shop=florist", "florist", "flower shop");
        kind("shop=pet", "pet shop", "pet store");
        kind("shop=alcohol", "liquor store", "off licence");
        kind("shop=dry_cleaning", "dry cleaner", "dry cleaning", "cleaners");
        kind("shop=laundry", "laundromat", "launderette");
        kind("shop=car_repair", "garage", "mechanic", "car repair");
        kind("amenity=school", "school");
        kind("amenity=kindergarten", "kindergarten", "preschool", "nursery school");
        kind("amenity=childcare", "daycare", "nursery");
        kind("amenity=college", "college");
        kind("amenity=university", "university", "campus");
        kind("amenity=library", "library");
        kind("amenity=hospital", "hospital");
        kind("amenity=clinic", "clinic");
        kind("amenity=doctors", "doctor's office", "doctor", "gp");
        kind("amenity=dentist", "dentist");
        kind("amenity=veterinary", "vet", "veterinarian");
        kind("amenity=restaurant", "restaurant");
        kind("amenity=fast_food", "fast food");
        kind("amenity=cafe", "café", "cafe", "coffee shop");
        kind("amenity=bar", "bar");
        kind("amenity=pub", "pub");
        kind("amenity=ice_cream", "ice cream shop");
        kind("amenity=bank", "bank");
        kind("amenity=atm", "cash machine", "atm");
        kind("amenity=post_office", "post office");
        kind("amenity=fuel", "gas station", "petrol station", "filling station");
        kind("amenity=charging_station", "charging station");
        kind("amenity=place_of_worship", "place of worship", "church", "mosque", "synagogue",
                "temple");
        kind("amenity=cinema", "cinema", "movie theater", "movie theatre");
        kind("amenity=theatre", "theatre", "theater");
        kind("amenity=townhall", "town hall", "city hall");
        kind("amenity=police", "police station");
        kind("amenity=fire_station", "fire station");
        kind("amenity=community_centre", "community centre", "community center");
        kind("amenity=marketplace", "market", "farmers market");
        kind("amenity=car_wash", "car wash");
        kind("leisure=park", "park");
        kind("leisure=playground", "playground");
        kind("leisure=fitness_centre", "gym", "fitness centre", "fitness center");
        kind("leisure=sports_centre", "sports centre", "sports center");
        kind("leisure=swimming_pool", "swimming pool", "pool");
        kind("leisure=stadium", "stadium");
        kind("leisure=pitch", "sports field");
        kind("leisure=garden", "garden");
        kind("tourism=museum", "museum");
        kind("tourism=hotel", "hotel");
        kind("tourism=attraction", "landmark");
        kind("tourism=gallery", "gallery", "art gallery");
        kind("tourism=zoo", "zoo");
        kind("historic=monument", "monument");
        kind("historic=memorial", "memorial");
        kind("railway=station", "train station", "station");
        kind("public_transport=station", "station");
        kind("aeroway=aerodrome", "airport");
    }

    private PlaceKinds() {
    }

    // Words in street names that say only that it is a street. "Street" would otherwise find
    // every reminder with a Something Street nearby.
    private static final Set<String> STREET_WORDS = new HashSet<>(Arrays.asList(
            "street", "st", "avenue", "ave", "road", "rd", "boulevard", "blvd", "lane",
            "ln", "drive", "dr", "way", "place", "pl", "square", "sq", "court", "ct",
            "terrace", "highway", "parkway", "row", "alley"));

    public static boolean isStreetWord(String word) {
        return STREET_WORDS.contains(word);
    }

    // Streets, neighbourhoods and towns: what a place is beside or inside, rather than at.
    public static boolean isArea(String kind) {
        return kind.equals("street") || kind.equals("neighbourhood") || kind.equals("town");
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
    // None for streets and areas, which are found by their names: "street" or "city" would match
    // every place.
    public static List<String> wordsFor(String kind) {
        if (isArea(kind)) {
            return Collections.emptyList();
        }
        List<String> synonyms = SYNONYMS.get(kind);
        return synonyms != null ? synonyms : Collections.emptyList();
    }
}
