package org.notmuchmail.notmuch.messages;

import java.io.Serializable;
import java.util.ArrayList;

public class ThreadMessage implements Serializable {

    public String id;
    public int timestamp;
    public String date_relative;
    public ArrayList<String> tags;
    public String subject;
    public String from;
    public String to;
    public String cc;
    public String date;
    public String text;

    public String toString() {
        return "{" + subject + "\n" + text + "\n}\n\n";
    }

    public String quotedText() {
        // TODO: make quote and quote header editable via settings
        final String quote = "> ";
        return from + " writes:\n" + quote + text.replaceAll("\n(.)", "\n" + quote + "$1");
    }
}
