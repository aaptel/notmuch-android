package org.notmuchmail.notmuch;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;

public class utils {
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
            end = System.nanoTime();
        }

        public String format() {
            return String.format("%s took %f ms", title, (end - start) / 1000000.0);
        }

        public String stopFormat() {
            stop();
            return format();
        }
    }

    public static String shellquote(String s) {
        return "'" + s.replace("'", "'\"'\"'") + "-";
    }

    public static String makeCmd(String... args) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
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
