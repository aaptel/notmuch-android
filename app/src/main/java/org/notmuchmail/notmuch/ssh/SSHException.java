package org.notmuchmail.notmuch.ssh;

import com.jcraft.jsch.JSchException;

public class SSHException extends Exception {
    public SSHException() {
        super();
    }

    public SSHException(String message) {
        super(message);
    }

    public SSHException(String message, Throwable cause) {
        super(message, cause);
    }

    public SSHException(Throwable cause) {
        super(cause);
    }

    public static boolean isJSchAuthException(Exception e) {

        return JSchExceptionMatches(e, ".*(" +
                "auth " +
                "|userauth fail" +
                "|password" +
                "|publickey" +
                ").*");
    }

    public static boolean isJSchDisconnectedException(Exception e) {
        return JSchExceptionMatches(e, ".*(" +
                "session is down" +
                "|session is not available" +
                "|session is not opened" +
                "|closed by foreign host" +
                "|channel is broken" +
                "|channel is down" +
                "|ssh_msg_disconnect" +
                "|channel request" +
                "|channelexec" +
                ").*");
    }


    public static boolean JSchExceptionMatches(Exception e, String regex) {
        if (!(e instanceof JSchException))
            return false;
        return e.getMessage().toLowerCase().matches(regex);

    }
}
