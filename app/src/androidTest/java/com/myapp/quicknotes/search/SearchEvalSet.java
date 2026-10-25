package com.myapp.quicknotes.search;

import java.util.Arrays;
import java.util.List;

// Labelled data for measuring search quality: everyday notes, and queries with the notes a person
// would expect each one to find. Most queries share no word with their answers, so only search by
// meaning can find them; some are exact strings that only search by words can; a few have no
// answer, to see whether search returns noise.
final class SearchEvalSet {
    private SearchEvalSet() {
    }

    static final class EvalNote {
        final String title;
        final String content;

        EvalNote(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    static final class EvalQuery {
        final String text;
        // Titles of the notes that answer it; empty when nothing should be found.
        final List<String> relevant;

        EvalQuery(String text, String... relevant) {
            this.text = text;
            this.relevant = Arrays.asList(relevant);
        }
    }

    static final List<EvalNote> NOTES = Arrays.asList(
            new EvalNote("Cardiology follow-up", "Dr. Patel, Thursday 3:40 PM at Mass General. "
                    + "Bring the list of blood pressure readings and ask about the new beta blocker."),
            new EvalNote("Dentist", "Cleaning on the 14th at 9 AM. Ask about the sensitive molar "
                    + "on the left."),
            new EvalNote("Oil change", "Car is due at 45,000 miles. Jiffy Lube on Route 9 does "
                    + "walk-ins before noon. Also check tire pressure."),
            new EvalNote("Biscuit's vet visit", "Annual shots for the dog. Ask the vet about the "
                    + "itchy paw and switch to grain-free kibble."),
            new EvalNote("Grocery list", "Spinach\nGreek yogurt\nCoffee beans\nLemons\nOat milk"),
            new EvalNote("Wifi at the cabin", "Network: PineLodge5G\nPassword: maple-syrup-77"),
            new EvalNote("Flight to Lisbon", "TAP 218 leaves Boston Logan 9:15 PM on May 3. "
                    + "Seat 23A. Confirmation code QX7P2L."),
            new EvalNote("Lisbon itinerary", "Day 1: Alfama walk and fado dinner.\n\n"
                    + "Day 2: Tram 28 and the castle.\n\n"
                    + "Day 3: Day trip to Sintra, Pena Palace."),
            new EvalNote("Mortgage refinance", "Rate quote from the credit union: 5.9% for 30 "
                    + "years. Compare closing costs with the bank before Friday."),
            new EvalNote("Taxes", "Gather W-2, 1099-INT from the savings account, and the daycare "
                    + "receipts for the dependent care credit. Deadline April 15."),
            new EvalNote("Book club", "Next pick: Klara and the Sun by Kazuo Ishiguro. Meeting at "
                    + "Priya's on the 22nd. Bring wine."),
            new EvalNote("Workout plan", "Mon: legs\nWed: push\nFri: pull\nRun 5k on weekends. "
                    + "Stretch hamstrings daily."),
            new EvalNote("Lasagna recipe", "Layer noodles, ricotta with egg and parsley, meat "
                    + "sauce, mozzarella. Bake 45 minutes at 375°F, rest 15 minutes."),
            new EvalNote("Kid's soccer", "Practice Tuesdays 5:30 at Ellis Field. Shin guards and "
                    + "water bottle. Snack duty on October 18."),
            new EvalNote("Passport renewal", "Fill in form DS-82, new photo 2x2 inches, check for "
                    + "$130. Mail to the National Passport Processing Center. Takes 6-8 weeks."),
            new EvalNote("Leaky faucet", "Kitchen tap drips. Probably the cartridge. Call Mike the "
                    + "plumber: 617-555-0142."),
            new EvalNote("Gift ideas for Mom", "She liked the ceramic class. Maybe a pottery wheel "
                    + "workshop, or the cookbook from the Portuguese bakery."),
            new EvalNote("Quarterly review prep", "Wins: shipped the reminders redesign. Metrics: "
                    + "retention up 4%. Ask for budget for a second designer."),
            new EvalNote("1:1 talking points", "Career goals for next year. Conference budget. "
                    + "Feedback on the on-call rotation."),
            new EvalNote("Pay rent", "Transfer $2,150 to the landlord by the 1st. Check the new "
                    + "amount after the lease renewal."),
            new EvalNote("Plants", "Water the fern and pothos every week. Cactus once a month. "
                    + "Repot the monstera in spring."),
            new EvalNote("Car insurance", "Policy renews in June. Get quotes from two other "
                    + "insurers; current premium is $1,240 a year."),
            new EvalNote("Allergy meds", "Take cetirizine every morning in pollen season. Refill "
                    + "the prescription at CVS, 3 refills left."),
            new EvalNote("Wedding RSVP", "Sam and Jordan's wedding, September 6 in Portland, "
                    + "Maine. RSVP by July 1, chicken for me, fish for Alex."),
            new EvalNote("Laptop backup", "Back up photos to the external drive monthly. Time "
                    + "Machine disk is almost full; buy a 4TB drive."),
            new EvalNote("Babysitter", "Emma is free Friday and Saturday evenings, $20 an hour. "
                    + "Her number: 617-555-0199."),
            new EvalNote("Running shoes", "Brooks Ghost size 10.5. Replace every 400 miles; the "
                    + "current pair is at about 350."),
            new EvalNote("Ideas for the app", "Search notes by meaning. Widgets for the home "
                    + "screen. Share a note as a checklist."),
            new EvalNote("Sintra tickets", "Buy Pena Palace tickets online for the 10 AM slot; "
                    + "they sell out. Park entry is separate."),
            new EvalNote("Cholesterol results", "LDL 142, HDL 51. Doctor wants a recheck in three "
                    + "months after diet changes."));

    static final List<EvalQuery> QUERIES = Arrays.asList(
            // Meaning: no shared word with the answer (or only with some of the answers).
            new EvalQuery("doctor", "Cardiology follow-up", "Cholesterol results"),
            new EvalQuery("heart appointment", "Cardiology follow-up"),
            new EvalQuery("car maintenance", "Oil change"),
            new EvalQuery("pet", "Biscuit's vet visit"),
            new EvalQuery("dog food", "Biscuit's vet visit"),
            new EvalQuery("internet password", "Wifi at the cabin"),
            new EvalQuery("trip to Portugal", "Flight to Lisbon", "Lisbon itinerary",
                    "Sintra tickets"),
            new EvalQuery("plane ticket", "Flight to Lisbon"),
            new EvalQuery("home loan", "Mortgage refinance"),
            new EvalQuery("IRS paperwork", "Taxes"),
            new EvalQuery("novel to read", "Book club"),
            new EvalQuery("gym routine", "Workout plan"),
            new EvalQuery("what to cook for dinner", "Lasagna recipe"),
            new EvalQuery("children's sports", "Kid's soccer"),
            new EvalQuery("travel documents", "Passport renewal"),
            new EvalQuery("plumbing problem", "Leaky faucet"),
            new EvalQuery("present for my mother", "Gift ideas for Mom"),
            new EvalQuery("performance review", "Quarterly review prep", "1:1 talking points"),
            new EvalQuery("houseplants", "Plants"),
            new EvalQuery("hay fever", "Allergy meds"),
            new EvalQuery("marriage invitation", "Wedding RSVP"),
            new EvalQuery("computer storage", "Laptop backup"),
            new EvalQuery("childcare", "Babysitter"),
            new EvalQuery("sneakers", "Running shoes"),
            // Words: exact strings that carry no meaning a model could know.
            new EvalQuery("QX7P2L", "Flight to Lisbon"),
            new EvalQuery("Ishiguro", "Book club"),
            new EvalQuery("617-555-0142", "Leaky faucet"),
            new EvalQuery("Priya", "Book club"),
            new EvalQuery("DS-82", "Passport renewal"),
            new EvalQuery("landlord", "Pay rent"),
            // No answer in these notes.
            new EvalQuery("quantum physics homework"),
            new EvalQuery("skiing in the Alps"),
            new EvalQuery("guitar lessons"),
            new EvalQuery("chess tournament"));
}
