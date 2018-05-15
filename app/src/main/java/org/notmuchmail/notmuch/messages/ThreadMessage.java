package org.notmuchmail.notmuch.messages;

import java.util.ArrayList;

public class ThreadMessage {

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
}
