package org.notmuchmail.notmuch.helpers;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public class utils {
    public static String join(String delim, Iterable<? extends CharSequence> array) {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends CharSequence> it = array.iterator();
        if (!it.hasNext()) {
            return "";
        }
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delim);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static class TimeIt {
        long start, end;
        String title;

        public TimeIt() {
            title = "";
            start = end = System.nanoTime();
        }

        public TimeIt(String t) {
            title = t;
            start = end = System.nanoTime();
        }

        public void start(String s) {
            title = s;
            start = System.nanoTime();
        }

        public void stop() {
            end = System.currentTimeMillis();
        }

        public String format() {
            return String.format("%s took %f ms", title, (end - start));
        }

        public String stopFormat() {
            stop();
            return format();
        }
    }

    public static String shellquote(String s) {
        if (s == null)
            return "";
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }

    public static String makeCmd(String cmd, String... args) {
        StringBuilder out = new StringBuilder();
        out.append(shellquote(cmd));
        for (int i = 0; i < args.length; i++) {
            out.append(" ");
            out.append(shellquote(args[i]));
        }
        return out.toString();
    }

    public static class SSHKeyPair {
        public String priv, pub;

        public SSHKeyPair(String passphrase) throws JSchException {
            JSch jsch = new JSch();
            KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            ByteArrayOutputStream privateKeyBuff = new ByteArrayOutputStream(2048);
            ByteArrayOutputStream publicKeyBuff = new ByteArrayOutputStream(2048);

            keyPair.writePublicKey(publicKeyBuff, "notmuch");
            keyPair.writePrivateKey(privateKeyBuff);
            priv = privateKeyBuff.toString();
            pub = publicKeyBuff.toString();
        }
    }
}
