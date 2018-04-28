package org.notmuchmail.notmuch.messages;

import org.json.JSONArray;
import org.json.JSONObject;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHClient;
import org.notmuchmail.notmuch.ssh.SSHException;

import java.util.ArrayList;
import java.util.List;

public class Search {
    String inputQuery;

    public Search(String query) {
        this.inputQuery = query;
    }

    public List<SearchResult> run(SSHClient ssh) throws SSHException {
        ArrayList<SearchResult> sr = new ArrayList<SearchResult>();
        CommandResult r = null;//ssh.runQuoted("notmuch", "search", inputQuery);
        try {
            JSONArray jar = new JSONArray(r.stdout);
            for (int i = 0; i < jar.length(); i++) {
                JSONObject o = jar.getJSONObject(i);
                String tid = o.getString("thread");
                int timestamp = o.getInt("timestamp");
                String daterelative = o.getString("date_relative");
                int matched = o.getInt("matched");
                int total = o.getInt("total");
                String authors = o.getString("authors");
                String subject = o.getString("subject");
                JSONArray qar = o.getJSONArray("query");
                ArrayList<String> queries = new ArrayList<String>();
                for (int j = 0; j < qar.length(); j++) {
                    String s = qar.getString(j);
                    if (s != null) {
                        queries.add(s);
                    }
                }
                JSONArray tar = o.getJSONArray("tags");
                ArrayList<String> tags = new ArrayList<String>();
                for (int j = 0; j < tar.length(); j++) {
                    String s = tar.getString(j);
                    if (s != null) {
                        tags.add(s);
                    }
                }
                sr.add(new SearchResult(tid, timestamp, daterelative, matched, total, authors, subject, queries, tags));
            }
        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return sr;
    }


}
