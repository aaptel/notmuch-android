package org.notmuchmail.notmuch.ssh;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;

public class SSHConnectionTest {
    private static final long TIMEOUT = 5000;

    static public boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static public void test(final SSHConf conf, Context ctx, final onTestDoneListener cb) {
        if (!conf.isComplete()) {
            cb.onError(new Exception("Incomplete configuration"));
            return;
        }
        if (!isNetworkAvailable(ctx)) {
            cb.onError(new Exception("Phone has no connectivity"));
            return;
        }

        new Thread() {
            JSch jsch = new JSch();
            Session ses;

            CommandResult runCmd(String cmd) throws Exception {
                InputStream stdout, stderr;
                ChannelExec chan;
                StringBuilder outbuf = new StringBuilder();
                StringBuilder errbuf = new StringBuilder();

                try {
                    chan = (ChannelExec) ses.openChannel("exec");
                    chan.setCommand(cmd);
                    chan.setInputStream(null);
                    stdout = chan.getInputStream();
                    stderr = chan.getExtInputStream();
                    chan.connect();
                } catch (Exception e) {
                    throw new Exception("Cannot open exec channel: " + e.toString(), e);
                }

                try {
                    byte[] readBuf = new byte[1024];
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < TIMEOUT) {
                        while (stdout.available() > 0) {
                            int i = stdout.read(readBuf, 0, readBuf.length);
                            if (i < 0) {
                                break;
                            }
                            outbuf.append(new String(readBuf, 0, i));
                        }
                        while (stderr.available() > 0) {
                            int i = stderr.read(readBuf, 0, readBuf.length);
                            if (i < 0) {
                                break;
                            }
                            errbuf.append(new String(readBuf, 0, i));
                        }
                        if (chan.isClosed() && stderr.available() == 0 && stdout.available() == 0) {
                            break;
                        }
                    }
                    if (System.currentTimeMillis() - start >= TIMEOUT) {
                        throw new Exception("SSH command timeout");
                    }
                } catch (Exception e) {
                    throw new Exception("SSH exec error: " + e.toString(), e);
                }
                return new CommandResult(outbuf.toString(), errbuf.toString(), chan.getExitStatus());
            }

            @Override
            public void run() {

                try {
                    ses = jsch.getSession(conf.user, conf.host, conf.port);
                } catch (Exception e) {
                    cb.onError(new Exception("Cannot create SSH session", e));
                    return;
                }

                try {
                    ses.setPassword(conf.pw);
                    ses.setConfig("StrictHostKeyChecking", "no");
                    ses.connect((int) (TIMEOUT));
                } catch (Exception e) {
                    cb.onError(new Exception("Connection error: " + e.toString(), e));
                    return;
                }

                CommandResult r;
                try {
                    r = runCmd("notmuch --version");
                } catch (Exception e) {
                    cb.onError(e);
                    return;
                }
                if (r.exit != 0) {
                    cb.onError(new Exception("Cannot run notmuch (exit status " + r.exit + ") " + r.stderr + r.stdout + ")"));
                    return;
                }
                cb.onSuccess(r);
            }
        }.start();
    }

    public interface onTestDoneListener {
        void onSuccess(CommandResult s);
        void onError(Exception e);
    }
}
