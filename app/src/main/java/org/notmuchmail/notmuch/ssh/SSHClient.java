package org.notmuchmail.notmuch.ssh;

import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.notmuchmail.notmuch.utils;

import java.io.InputStream;
import java.util.HashMap;

public class SSHClient {
    class CommandItem implements Runnable {
        Thread thread;
        int id;
        String cmd;
        ChannelExec chan;
        CommandResult result;
        boolean done;
        SSHException error;

        public CommandItem(String cmd, int id) {
            this.cmd = cmd;
            this.id = id;
            done = false;
            error = null;
        }

        @Override
        public void run() {
            try {
                InputStream stdout, stderr;
                StringBuilder outbuf, errbuf;
                utils.TimeIt t = new utils.TimeIt("ssh open exec chan");
                // first open an exec channel
                try {
                    synchronized (SSHClient.this) {
                        chan = (ChannelExec) ses.openChannel("exec");
                    }
                    chan.setCommand(cmd);
                    outbuf = new StringBuilder();
                    errbuf = new StringBuilder();
                    chan.setInputStream(null);
                    stdout = chan.getInputStream();
                    stderr = chan.getExtInputStream();
                    Log.i(TAG, "connect...");
                    chan.connect();
                    Log.i(TAG, "connect ok");
                } catch (Exception e) {
                    Log.i(TAG, "connect... error " + e.toString());
                    error = new SSHException("error while opening exec chan", e);
                    Log.i(TAG, t.stopFormat());
                    return;
                }
                try {
                    t.start("ssh cmd run and read");
                    byte[] tmp = new byte[1024];
                    while (true) {
                        while (stdout.available() > 0) {
                            Log.i(TAG, "stdout: avail data, reading");
                            int i = stdout.read(tmp, 0, 1024);
                            Log.i(TAG, "stdout: read " + Integer.toString(i));
                            if (i < 0) break;
                            outbuf.append(new String(tmp, 0, i));
                        }
                        while (stderr.available() > 0) {
                            Log.i(TAG, "stderr: avail data, reading");
                            int i = stderr.read(tmp, 0, 1024);
                            Log.i(TAG, "stderr: read");
                            if (i < 0) break;
                            errbuf.append(new String(tmp, 0, i));
                        }
                        if (chan.isClosed()) {
                            if ((stdout.available() > 0) || (stderr.available() > 0)) continue;
                            Log.i(TAG, "exit-status: " + chan.getExitStatus());
                            break;
                        }
                        try {
                            Log.i(TAG, "sleep");
                            Thread.sleep(100);
                        } catch (Exception ee) {
                        }
                    }
                    Log.d(TAG, "output: <" + outbuf.toString() + ">");
                    Log.d(TAG, "error: <" + errbuf.toString() + ">");
                } catch (Exception e) {
                    Log.i(TAG, "errr " + e.toString());
                    error = new SSHException("error while running cmd", e);
                    return;
                } finally {
                    chan.disconnect();
                    Log.i(TAG, t.stopFormat());
                }
                result = new CommandResult(outbuf.toString(), errbuf.toString(), chan.getExitStatus());
            } catch (Exception e) {
                error = new SSHException("unknown ssh exception", e);
            }
        }
    }

    JSch jsch;
    Session ses;
    SSHConf conf;
    int idCounter = 0;
    SSHException sessionError = null;
    Thread sessionThread = null;

    private static final String TAG = "nmssh";
    HashMap<Integer, CommandItem> xchans;

    public SSHClient(SSHConf conf) {
        this.conf = conf;
        xchans = new HashMap<>();
    }

    public void connect(boolean blocking) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                utils.TimeIt t = new utils.TimeIt("ssh open session");
                try {
                    jsch = new JSch();
                    ses = jsch.getSession(conf.user, conf.host, conf.port);
                    ses.setPassword(conf.pw);
                    ses.setConfig("StrictHostKeyChecking", "no");
                    ses.connect(1000);
                } catch (Exception e) {
                    sessionError = new SSHException("error while connecting", e);
                    Log.e(TAG, "ssh session connection error", e);
                } finally {
                    Log.i(TAG, t.stopFormat());
                }
            }
        };

        synchronized (SSHClient.this) {
            if (ses == null || !ses.isConnected()) {
                sessionThread = new Thread(runnable);
                sessionThread.start();
                if (blocking) {
                    try {
                        sessionThread.join();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "ssh session connection blocking thread interrupted");
                        sessionError = new SSHException("ssh session connection error", e);
                    }
                }
            }
        }
    }

    public boolean isConnected() {
        return ses.isConnected();
    }
}
