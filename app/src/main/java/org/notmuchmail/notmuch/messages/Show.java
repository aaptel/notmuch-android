package org.notmuchmail.notmuch.messages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.notmuchmail.notmuch.helpers.utils;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHException;
import org.notmuchmail.notmuch.ssh.SSHService;

import java.util.ArrayList;
import java.util.List;

public class Show {
    String inputQuery;
    List<ThreadMessage> results;

    public Show(String q) {
        inputQuery = q;
    }

    static private boolean isText(JSONObject part) throws JSONException {
        String t = part.getString("content-type");
        if (t == null)
            return false;
        if (!t.contains("text"))
            return false;
        return true;
    }

    public int run(SSHService ssh) {
        int id = ssh.addCommand(utils.makeCmd(
                "notmuch",
                "show",
                "--format=json", "--format-version=2",
                inputQuery));
        return id;
    }

    // reused in ComposeActivity to parse original message
    public static void parseThreadNode(JSONArray node, List<ThreadMessage> results) throws JSONException {
        JSONObject o = node.getJSONObject(0);
        {
            ThreadMessage m = new ThreadMessage();
            m.id = o.getString("id");
            m.timestamp = o.getInt("timestamp");
            m.date_relative = o.getString("date_relative");
            m.tags = new ArrayList<>();
            JSONArray jtags = o.getJSONArray("tags");
            for (int i = 0; i < jtags.length(); i++) {
                m.tags.add(jtags.getString(i));
            }
            JSONObject jhdrs = o.getJSONObject("headers");
            m.subject = jhdrs.getString("Subject");
            m.from = jhdrs.getString("From");
            m.to = jhdrs.getString("To");
            m.date = jhdrs.getString("Date");
            StringBuilder text = new StringBuilder();

            JSONArray jparts = o.getJSONArray("body");
            for (int i = 0; i < jparts.length(); i++) {
                JSONObject p = jparts.getJSONObject(i);
                if (isText(p)) {
                    String content = p.getString("content");
                    if (content != null)
                        text.append(content);
                }
            }
            m.text = text.toString();
            results.add(m);
        }

        JSONArray children = node.getJSONArray(1);
        for (int i = 0; i < children.length(); i++) {
            parseThreadNode(children.getJSONArray(i), results);
        }
    }

    public List<ThreadMessage> getResults() {
        return results;
    }

    public int parse(CommandResult r) throws SSHException {
        int added = 0;
        if (results == null) {
            results = new ArrayList<>();
        }

        if (r.stdout.isEmpty() && r.exit == 0)
            return added;

        try {
            JSONArray jset = new JSONArray(r.stdout);
            for (int i = 0; i < jset.length(); i++) {
                JSONArray jthread = jset.getJSONArray(i);
                for (int j = 0; j < jthread.length(); j++) {
                    parseThreadNode(jthread.getJSONArray(j), results);
                }
            }
        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return added;
    }
}
