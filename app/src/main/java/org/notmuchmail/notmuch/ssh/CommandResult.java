package org.notmuchmail.notmuch.ssh;

import java.io.Serializable;

public class CommandResult implements Serializable {
    public String stdout, stderr;
    public int exit;

    public CommandResult(String stdout, String stderr, int exit) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exit = exit;
    }

    @Override
    public String toString() {
        return String.format("{stdout=<%s> stderr=<%s> exit=%d}", stdout, stderr, exit);
    }
}
