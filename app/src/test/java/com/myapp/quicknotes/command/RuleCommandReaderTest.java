package com.myapp.quicknotes.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.myapp.quicknotes.data.DaysOfWeek;
import com.myapp.quicknotes.data.PlaceTrigger;
import com.myapp.quicknotes.data.Repeat;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RuleCommandReaderTest {
    private static final ZoneId ZONE = ZoneId.of("America/New_York");
    // Tuesday, 27 October 2026, 2:00 pm.
    private static final long NOW = at(2026, 10, 27, 14, 0);

    private final RuleCommandReader reader = new RuleCommandReader();

    private static long at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), ZONE)
                .toInstant().toEpochMilli();
    }

    private Command read(String request) {
        return reader.read(request, NOW, ZONE);
    }

    private static void assertPlace(Command command, String place, PlaceTrigger trigger,
                                    String text) {
        assertEquals(command.toString(), Command.Kind.LOCATION, command.kind);
        assertEquals(command.toString(), place, command.place);
        assertEquals(command.toString(), trigger, command.placeTrigger);
        assertEquals(command.toString(), text, command.text);
    }

    private static void assertTime(Command command, long triggerAt, String text) {
        assertEquals(command.toString(), Command.Kind.TIME, command.kind);
        assertEquals(command.toString(), Long.valueOf(triggerAt), command.triggerAt);
        assertEquals(command.toString(), text, command.text);
    }

    // Places.

    @Test
    public void theOwnersExample() {
        Command command = read("Set me a location reminder for walgreens to grab medicine and milk");
        assertPlace(command, "walgreens", PlaceTrigger.ARRIVING, "Grab medicine and milk");
        assertEquals(Repeat.NONE, command.repeat);
    }

    @Test
    public void arrivingPhrasings() {
        assertPlace(read("Remind me to buy milk when I get to Walgreens"), "Walgreens",
                PlaceTrigger.ARRIVING, "Buy milk");
        assertPlace(read("When I get to Walgreens remind me to grab medicine"), "Walgreens",
                PlaceTrigger.ARRIVING, "Grab medicine");
        assertPlace(read("remind me to buy stamps when I'm near the post office"), "post office",
                PlaceTrigger.ARRIVING, "Buy stamps");
        assertPlace(read("Remind me when I get home to take my pills"), "home",
                PlaceTrigger.ARRIVING, "Take my pills");
        assertPlace(read("once I arrive at the gym, stretch first"), "gym",
                PlaceTrigger.ARRIVING, "Stretch first");
        assertPlace(read("Remind me to return the book when I'm at the library."), "library",
                PlaceTrigger.ARRIVING, "Return the book");
    }

    @Test
    public void leaving() {
        assertPlace(read("Remind me to call mom when I leave work"), "work",
                PlaceTrigger.LEAVING, "Call mom");
        assertPlace(read("when I'm leaving the office remind me to grab my charger"), "office",
                PlaceTrigger.LEAVING, "Grab my charger");
    }

    @Test
    public void everyVisit() {
        Command command = read("Remind me every time I get to the gym to stretch");
        assertPlace(command, "gym", PlaceTrigger.ARRIVING, "Stretch");
        assertEquals(Repeat.EVERY_ARRIVAL, command.repeat);
        assertEquals(Repeat.EVERY_ARRIVAL,
                read("whenever I leave home remind me to lock the door").repeat);
    }

    @Test
    public void aNamedPlaceAfterAt() {
        assertPlace(read("remind me to buy milk at Target"), "Target", PlaceTrigger.ARRIVING,
                "Buy milk");
        // Lower case is a place only after "location reminder"...
        assertPlace(read("set a location reminder to pick up a prescription at cvs"), "cvs",
                PlaceTrigger.ARRIVING, "Pick up a prescription");
        // ...otherwise it reads as words.
        Command words = read("remind me to look at the report");
        assertEquals(Command.Kind.TIME, words.kind);
        assertNull(words.triggerAt);
        assertEquals("Look at the report", words.text);
    }

    @Test
    public void aLocationReminderWithNoPlaceLeavesItToBePicked() {
        Command command = read("set a location reminder to water the plants");
        assertEquals(Command.Kind.LOCATION, command.kind);
        assertNull(command.place);
        assertEquals("Water the plants", command.text);
    }

    @Test
    public void wordsThatAreNotPlaces() {
        // "When I get up" is a time of day, not somewhere to arrive.
        Command command = read("remind me to stretch when I get up");
        assertEquals(Command.Kind.TIME, command.kind);
        assertEquals(Command.Kind.TIME, read("remind me when I'm hungry to eat").kind);
    }

    // Times.

    @Test
    public void dayAndTime() {
        assertTime(read("Remind me tomorrow at 9am to call the dentist"),
                at(2026, 10, 28, 9, 0), "Call the dentist");
        assertTime(read("remind me at 5:30 pm to leave"), at(2026, 10, 27, 17, 30), "Leave");
        assertTime(read("Remind me at 5 p.m. to start dinner"), at(2026, 10, 27, 17, 0),
                "Start dinner");
        assertTime(read("Set a reminder for Friday at 3 to pay rent"), at(2026, 10, 30, 15, 0),
                "Pay rent");
        assertTime(read("remind me to look at the report tomorrow"), at(2026, 10, 28, 9, 0),
                "Look at the report");
    }

    @Test
    public void relativeTimes() {
        assertTime(read("Remind me in 20 minutes to check the oven"), NOW + 20 * 60_000,
                "Check the oven");
        assertTime(read("in an hour remind me to move the car"), NOW + 60 * 60_000,
                "Move the car");
        assertTime(read("remind me in half an hour to call back"), NOW + 30 * 60_000,
                "Call back");
    }

    @Test
    public void partsOfTheDay() {
        assertTime(read("Remind me tonight to call Sam"), at(2026, 10, 27, 20, 0), "Call Sam");
        assertTime(read("remind me tomorrow morning to bring the forms"),
                at(2026, 10, 28, 9, 0), "Bring the forms");
        assertTime(read("remind me this evening to water the plants"),
                at(2026, 10, 27, 18, 0), "Water the plants");
        assertTime(read("remind me tonight at 8 to take out the bins"),
                at(2026, 10, 27, 20, 0), "Take out the bins");
    }

    @Test
    public void aTimeAlreadyPastIsTomorrowAndAWeekdayAlreadyPastIsNextWeek() {
        assertTime(read("remind me at 9am to stretch"), at(2026, 10, 28, 9, 0), "Stretch");
        // Today is Tuesday; 9 this morning is gone.
        assertTime(read("remind me on Tuesday at 9am to stretch"), at(2026, 11, 3, 9, 0),
                "Stretch");
        assertTime(read("remind me next Tuesday to stretch"), at(2026, 11, 3, 9, 0), "Stretch");
    }

    @Test
    public void dates() {
        assertTime(read("remind me on October 30 to vote"), at(2026, 10, 30, 9, 0), "Vote");
        assertTime(read("remind me on the 15th to pay the card"), at(2026, 11, 15, 9, 0),
                "Pay the card");
        assertTime(read("remind me on 3rd of January to renew the passport"),
                at(2027, 1, 3, 9, 0), "Renew the passport");
    }

    @Test
    public void repeating() {
        Command trash = read("remind me to take out the trash every Monday at 7pm");
        assertTime(trash, at(2026, 11, 2, 19, 0), "Take out the trash");
        assertEquals(Repeat.DAYS_OF_WEEK, trash.repeat);
        assertEquals(DaysOfWeek.of(DayOfWeek.MONDAY), trash.repeatDays);

        Command plants = read("Remind me to water the plants every day at 8am");
        assertTime(plants, at(2026, 10, 28, 8, 0), "Water the plants");
        assertEquals(Repeat.DAILY, plants.repeat);

        Command gym = read("remind me every Monday and Thursday at 6 to go to the gym");
        assertEquals(DaysOfWeek.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), gym.repeatDays);
        assertEquals(Long.valueOf(at(2026, 10, 29, 18, 0)), gym.triggerAt);

        Command standup = read("remind me every weekday at 9:30 am to post my update");
        assertEquals(Repeat.DAYS_OF_WEEK, standup.repeat);
        assertEquals(Long.valueOf(at(2026, 10, 28, 9, 30)), standup.triggerAt);

        assertEquals(Repeat.MONTHLY, read("remind me monthly to pay rent").repeat);
    }

    @Test
    public void aReminderWithNoTimeLeavesItToBePicked() {
        Command command = read("remind me to buy milk");
        assertEquals(Command.Kind.TIME, command.kind);
        assertNull(command.triggerAt);
        assertEquals("Buy milk", command.text);
    }

    // Notes.

    @Test
    public void notes() {
        Command self = read("Note to self: the wifi password is in the drawer");
        assertEquals(Command.Kind.NOTE, self.kind);
        assertEquals("The wifi password is in the drawer", self.text);
        Command plain = read("buy eggs");
        assertEquals(Command.Kind.NOTE, plain.kind);
        assertEquals("Buy eggs", plain.text);
    }

    @Test
    public void politeOpenings() {
        assertPlace(read("Hey, can you please remind me to buy milk when I get to Target?"),
                "Target", PlaceTrigger.ARRIVING, "Buy milk");
        assertTime(read("Could you set a reminder for tomorrow at 10am to call the bank please"),
                at(2026, 10, 28, 10, 0), "Call the bank");
    }
}
