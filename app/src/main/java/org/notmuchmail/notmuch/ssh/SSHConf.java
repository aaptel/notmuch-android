package org.notmuchmail.notmuch.ssh;

public class SSHConf {
    static SSHConf singleton;

    public String user, host, pw, pubkey, privkey;
    public int port;

    public SSHConf() {
    }
}
