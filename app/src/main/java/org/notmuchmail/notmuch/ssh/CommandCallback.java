package org.notmuchmail.notmuch.ssh;

public interface CommandCallback {
    void onError(Exception e);

    void onResult(CommandResult r);
}
