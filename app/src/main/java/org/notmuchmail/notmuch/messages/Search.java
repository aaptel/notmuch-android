package org.notmuchmail.notmuch.messages;

import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.notmuchmail.notmuch.SSHService;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHException;
import org.notmuchmail.notmuch.utils;

import java.util.ArrayList;
import java.util.List;

public class Search {
    public static final int SEARCH_LIMIT = 10;

    public enum ResultOrder {
        NEWEST_FIRST("newest-first"),
        OLDEST_FIRST("oldest-first");

        public final String arg;

        ResultOrder(String a) {
            arg = a;
        }
    }

    String inputQuery;
    ResultOrder order;
    int offset;
    List<SearchResult> results;

    public Search(String query, ResultOrder orderType) {
        this.inputQuery = query;
        order = orderType;
        offset = 0;
    }

    public void reset () {
        results = new ArrayList<>();
        offset = 0;
    }

    public long runMore(SSHService ssh) {
        long id = ssh.addCommand(utils.makeCmd(
                "notmuch",
                "search",
                "--format=json", "--format-version=2",
                "--limit=" + SEARCH_LIMIT, "--sort=" + order.arg,
                "--offset=" + offset,
                inputQuery));
        offset += SEARCH_LIMIT;
        return id;
    }

    public int parse(CommandResult r) throws SSHException {
        int added = 0;
        if (results == null) {
            results = new ArrayList<>();
        }

        if (r.stdout.isEmpty() && r.exit == 0)
            return added;

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
                results.add(new SearchResult(tid, timestamp, daterelative, matched, total, authors, subject, queries, tags));
                added++;
            }
        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return added;
    }


}
