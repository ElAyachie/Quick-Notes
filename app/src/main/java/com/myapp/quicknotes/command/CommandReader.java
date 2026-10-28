package com.myapp.quicknotes.command;

import java.time.ZoneId;

// Turns what the user said or typed ("remind me to buy milk when I get to Walgreens") into a
// Command. RuleCommandReader does it with patterns on the phone; another reader (a language
// model, say) can take its place without the screen changing.
public interface CommandReader {
    Command read(String request, long now, ZoneId zone);
}
