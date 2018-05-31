package org.notmuchmail.notmuch.messages;

import org.json.JSONException;
import org.json.JSONObject;
import org.notmuchmail.notmuch.helpers.utils;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHException;
import org.notmuchmail.notmuch.ssh.SSHService;

import java.util.ArrayList;

public class Reply {
    String inputQuery;
    ReplyMessage result;

    // TODO add option for reply type (all vs sender)
    public Reply(String q) {
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
                "reply",
                "--format=json", "--format-version=2",
                inputQuery));
        return id;
    }

    public ReplyMessage getResult() {
        return result;
    }

    public int parse(CommandResult r) throws SSHException {
        if (r.stdout.isEmpty() && r.exit == 0)
            throw new SSHException("command error");

        try {
            JSONObject jroot = new JSONObject(r.stdout);
            JSONObject jhdrs = jroot.getJSONObject("reply-headers");
            result = new ReplyMessage();
            result.inputQuery = inputQuery;
            result.subject = jhdrs.getString("Subject");
            result.from = jhdrs.getString("From");
            result.to = jhdrs.getString("To");
            result.cc = jhdrs.getString("Cc");
            result.bcc = ""; // TODO: lookup notmuch specs for complete possible fields
            result.inreplyto = jhdrs.getString("In-reply-to");
            result.references = jhdrs.getString("References");

            ArrayList<ThreadMessage> tmp = new ArrayList<>();
            Show.parseThreadNode(jroot.getJSONArray("original"), tmp);
            result.original = tmp.get(0);

        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return 1;
    }
}
