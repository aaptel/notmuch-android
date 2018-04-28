package org.notmuchmail.notmuch.ssh;

import android.content.SharedPreferences;

public class SSHConf {
    public String user, host, pw, pubkey, privkey;
    public int port;

    public SSHConf() {
    }

    public SSHConf(SharedPreferences prefs) {
        host = prefs.getString("ssh_host", null);
        user = prefs.getString("ssh_username", null);
        pw = prefs.getString("ssh_password", null);
        try {
            port = prefs.getInt("ssh_port", 22);
        } catch (ClassCastException e) {
            String n = prefs.getString("ssh_port", "22");
            port = Integer.parseInt(n);
        }
    }

    private static boolean validString(String... args) {
        for (String s : args) {
            if (s == null || s.isEmpty())
                return false;
        }
        return true;
    }
    public boolean isComplete(){
        return validString(host, user, pw);
    }
}
