package com.myapp.quicknotes.data;

// Which way across the edge of its circle sets a location reminder off.
public enum PlaceTrigger {
    // Coming into the circle from outside.
    ARRIVING,
    // Going out of the circle from inside.
    LEAVING
}
