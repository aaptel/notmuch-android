package org.notmuchmail.notmuch.messages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.notmuchmail.notmuch.helpers.utils;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHException;
import org.notmuchmail.notmuch.ssh.SSHService;

import java.util.ArrayList;

public class ReplyCmd {
    boolean replyAll;
    String inputQuery;
    ReplyMessage result;

    public ReplyCmd(String query, boolean replyAll) {
        this.inputQuery = query;
        this.replyAll = replyAll;
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
            try {
                result.cc = jhdrs.getString("Cc");
            } catch (Exception e) {
            }
            result.bcc = null;
            result.inreplyto = jhdrs.getString("In-reply-to");
            result.references = jhdrs.getString("References");

            ArrayList<ThreadMessage> tmp = new ArrayList<>();
            JSONArray jnode = new JSONArray();
            jnode.put(jroot.getJSONObject("original"));
            ShowCmd.parseThreadNode(jnode, tmp);
            result.original = tmp.get(0);

        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return 1;
    }
}
