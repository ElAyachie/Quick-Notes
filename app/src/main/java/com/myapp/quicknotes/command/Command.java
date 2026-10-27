package com.myapp.quicknotes.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Repeat;

// What a spoken or typed request asks for: a note, and maybe a reminder of it at a time or a
// place. A reminder whose time or place couldn't be read has it missing (null); the user then
// picks it.
public final class Command {
    public enum Kind {
        // Just a note.
        NOTE,
        TIME,
        LOCATION,
    }

    @NonNull
    public final Kind kind;
    // The note's text: what to be reminded of.
    @NonNull
    public final String text;
    // TIME: when, or null if no time was said.
    @Nullable
    public final Long triggerAt;
    // LOCATION: the place as said ("walgreens", "home"), or null if none was.
    @Nullable
    public final String place;
    @NonNull
    public final PlaceTrigger placeTrigger;
    // NONE, a time repeat, or EVERY_ARRIVAL for a place.
    @NonNull
    public final Repeat repeat;
    public final int repeatDays;

    Command(@NonNull Kind kind, @NonNull String text, @Nullable Long triggerAt,
            @Nullable String place, @NonNull PlaceTrigger placeTrigger, @NonNull Repeat repeat,
            int repeatDays) {
        this.kind = kind;
        this.text = text;
        this.triggerAt = triggerAt;
        this.place = place;
        this.placeTrigger = placeTrigger;
        this.repeat = repeat;
        this.repeatDays = repeatDays;
    }

    @NonNull
    @Override
    public String toString() {
        return kind + " \"" + text + "\" at=" + triggerAt + " place=" + place + " "
                + placeTrigger + " " + repeat + " " + repeatDays;
    }
}
