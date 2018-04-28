package org.notmuchmail.notmuch.messages;

import java.util.List;

public class SearchResult {
    public String thread;
    public int timestamp;
    public String dateRelative;
    public int matched;
    public int total;
    public String authors;
    public String subject;
    public List<String> query;
    public List<String> tags;

    public SearchResult(String thread, int timestamp, String dateRelative, int matched, int total, String authors, String subject, List<String> query, List<String> tags) {
        this.thread = thread;
        this.timestamp = timestamp;
        this.dateRelative = dateRelative;
        this.matched = matched;
        this.total = total;
        this.authors = authors;
        this.subject = subject;
        this.query = query;
        this.tags = tags;
    }
}
