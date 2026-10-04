package com.myapp.quicknotes.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.ZoneId;
import java.util.Objects;

// A reminder points at a note instead of copying its text, so the notification always shows the
// note as it is when the reminder fires. Deleting the note deletes its reminders.
//
// A TIME reminder has a trigger time and no place; a LOCATION reminder has a place (latitude,
// longitude, radius, optional name) and no trigger time, and says whether it fires on arriving
// at the place or on leaving it.
//
// A reminder repeating on chosen days of the week (Repeat.DAYS_OF_WEEK) also stores which days.
//
// A row is one occurrence. It fires once and is then kept, stamped with the moment it fired, as
// history. A repeating reminder continues as a new row for its next occurrence.
@Entity(
        tableName = "reminders",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "note_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("note_id"))
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private final long id;
    @ColumnInfo(name = "note_id")
    private final long noteId;
    @NonNull
    private final ReminderType type;
    @ColumnInfo(name = "trigger_at")
    @Nullable
    private final Long triggerAt;
    @Nullable
    private final Double latitude;
    @Nullable
    private final Double longitude;
    @ColumnInfo(name = "radius_meters")
    @Nullable
    private final Float radiusMeters;
    @ColumnInfo(name = "place_name")
    @Nullable
    private final String placeName;
    @ColumnInfo(name = "place_trigger", defaultValue = "ARRIVING")
    @NonNull
    private final PlaceTrigger placeTrigger;
    @ColumnInfo(defaultValue = "NONE")
    @NonNull
    private final Repeat repeat;
    @ColumnInfo(name = "repeat_days", defaultValue = "0")
    private final int repeatDays;
    @ColumnInfo(name = "first_trigger_at")
    @Nullable
    private final Long firstTriggerAt;
    @ColumnInfo(name = "fired_at")
    @Nullable
    private final Long firedAt;

    public Reminder(long id, long noteId, @NonNull ReminderType type, @Nullable Long triggerAt,
                    @Nullable Double latitude, @Nullable Double longitude,
                    @Nullable Float radiusMeters, @Nullable String placeName,
                    @NonNull PlaceTrigger placeTrigger, @NonNull Repeat repeat, int repeatDays,
                    @Nullable Long firstTriggerAt, @Nullable Long firedAt) {
        this.id = id;
        this.noteId = noteId;
        this.type = type;
        this.triggerAt = triggerAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.placeName = placeName;
        this.placeTrigger = placeTrigger;
        this.repeat = repeat;
        this.repeatDays = repeatDays;
        this.firstTriggerAt = firstTriggerAt;
        this.firedAt = firedAt;
    }

    // repeat is NONE or one of the fixed intervals (DAILY, WEEKLY, MONTHLY, YEARLY).
    @Ignore
    public static Reminder atTime(long noteId, long triggerAt, @NonNull Repeat repeat) {
        return atTime(noteId, triggerAt, repeat, DaysOfWeek.NONE);
    }

    // As above, or repeat is DAYS_OF_WEEK and repeatDays says which days.
    @Ignore
    public static Reminder atTime(long noteId, long triggerAt, @NonNull Repeat repeat,
                                  int repeatDays) {
        return new Reminder(0, noteId, ReminderType.TIME, triggerAt, null, null, null, null,
                PlaceTrigger.ARRIVING, repeat, repeatDays, triggerAt, null);
    }

    // Fires on arriving at the place. repeat is NONE or EVERY_ARRIVAL.
    @Ignore
    public static Reminder atPlace(long noteId, double latitude, double longitude,
                                   float radiusMeters, @Nullable String placeName,
                                   @NonNull Repeat repeat) {
        return atPlace(noteId, latitude, longitude, radiusMeters, placeName,
                PlaceTrigger.ARRIVING, repeat);
    }

    // As above, on arriving at the place or on leaving it.
    @Ignore
    public static Reminder atPlace(long noteId, double latitude, double longitude,
                                   float radiusMeters, @Nullable String placeName,
                                   @NonNull PlaceTrigger placeTrigger, @NonNull Repeat repeat) {
        return new Reminder(0, noteId, ReminderType.LOCATION, null, latitude, longitude,
                radiusMeters, placeName, placeTrigger, repeat, DaysOfWeek.NONE, null, null);
    }

    public Reminder withId(long id) {
        return new Reminder(id, noteId, type, triggerAt, latitude, longitude, radiusMeters,
                placeName, placeTrigger, repeat, repeatDays, firstTriggerAt, firedAt);
    }

    // This reminder moved to another time. The series of a repeating reminder starts over from
    // the new time.
    public Reminder rescheduled(long triggerAt, @NonNull Repeat repeat, int repeatDays) {
        return new Reminder(id, noteId, type, triggerAt, latitude, longitude, radiusMeters,
                placeName, placeTrigger, repeat, repeatDays, triggerAt, firedAt);
    }

    // This reminder moved to another place, or changed between arriving and leaving.
    public Reminder moved(double latitude, double longitude, float radiusMeters,
                          @Nullable String placeName, @NonNull PlaceTrigger placeTrigger,
                          @NonNull Repeat repeat) {
        return new Reminder(id, noteId, type, triggerAt, latitude, longitude, radiusMeters,
                placeName, placeTrigger, repeat, repeatDays, firstTriggerAt, firedAt);
    }

    // The occurrence that follows this one, not yet stored; null when this reminder doesn't
    // repeat. A time reminder moves on to its next due time; a location reminder waits for the
    // next arrival at the same place, or the next time it is left.
    @Nullable
    public Reminder nextOccurrence(long now, ZoneId zone) {
        if (repeat == Repeat.NONE) {
            return null;
        }
        Long nextTriggerAt = null;
        if (type == ReminderType.TIME) {
            long previous = Objects.requireNonNull(triggerAt);
            long first = firstTriggerAt != null ? firstTriggerAt : previous;
            nextTriggerAt = repeat == Repeat.DAYS_OF_WEEK
                    ? DaysOfWeek.nextAfter(repeatDays, first, Math.max(previous, now), zone)
                    : repeat.nextAfter(first, previous, now, zone);
        }
        return new Reminder(0, noteId, type, nextTriggerAt, latitude, longitude, radiusMeters,
                placeName, placeTrigger, repeat, repeatDays, firstTriggerAt, null);
    }

    public long getId() {
        return id;
    }

    public long getNoteId() {
        return noteId;
    }

    @NonNull
    public ReminderType getType() {
        return type;
    }

    // When a TIME reminder fires, in milliseconds since the epoch.
    @Nullable
    public Long getTriggerAt() {
        return triggerAt;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

    // How close to the place counts as being there: arriving is coming within this distance,
    // leaving is going beyond it.
    @Nullable
    public Float getRadiusMeters() {
        return radiusMeters;
    }

    // What the user called the place, if they named it.
    @Nullable
    public String getPlaceName() {
        return placeName;
    }

    // Whether a LOCATION reminder fires on arriving at its place or on leaving it. Means nothing
    // for a TIME reminder.
    @NonNull
    public PlaceTrigger getPlaceTrigger() {
        return placeTrigger;
    }

    @NonNull
    public Repeat getRepeat() {
        return repeat;
    }

    // The days a DAYS_OF_WEEK reminder repeats on, as a DaysOfWeek set; empty otherwise.
    public int getRepeatDays() {
        return repeatDays;
    }

    // The trigger time of the first occurrence of a repeating time reminder; later occurrences
    // are counted from it.
    @Nullable
    public Long getFirstTriggerAt() {
        return firstTriggerAt;
    }

    // When the reminder fired, in milliseconds since the epoch; null while it is still waiting.
    @Nullable
    public Long getFiredAt() {
        return firedAt;
    }

    public boolean hasFired() {
        return firedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder)) return false;
        Reminder other = (Reminder) o;
        return id == other.id
                && noteId == other.noteId
                && type == other.type
                && placeTrigger == other.placeTrigger
                && repeat == other.repeat
                && repeatDays == other.repeatDays
                && Objects.equals(triggerAt, other.triggerAt)
                && Objects.equals(latitude, other.latitude)
                && Objects.equals(longitude, other.longitude)
                && Objects.equals(radiusMeters, other.radiusMeters)
                && Objects.equals(placeName, other.placeName)
                && Objects.equals(firstTriggerAt, other.firstTriggerAt)
                && Objects.equals(firedAt, other.firedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, noteId, type, triggerAt, latitude, longitude, radiusMeters,
                placeName, placeTrigger, repeat, repeatDays, firstTriggerAt, firedAt);
    }
}
