package org.notmuchmail.notmuch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHConf;
import org.notmuchmail.notmuch.ssh.SSHException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SSHService extends Service {
    static final String TAG = "nmsshservc";
    static final int CONNECTION_TIMEOUT = 6000; // milliseconds
    static final long CMD_TIMEOUT = 20 * 1000; // milliseconds
    static final int SESSION_LIFETIME = 120; // seconds
    static final int DEEPSLEEP = 60 * 60 * 48; // seconds

    // XXX: polling could be done via the condition waiting mechanism
    // but modern android is deprecating long running background
    // services.. might be more practical to do with
    // scheduledjobs... we'll see...
    static final int POLL_SERVER = 15 * 60;
    private static final int READ_BUF_SIZE = 2048;
    private static final int MAX_RECO_ATTEMPT = 5;
    private final IBinder binder = new SSHBinder();
    byte[] readBuf = new byte[READ_BUF_SIZE];
    Lock jobsLock;
    Condition notEmpty;
    JSch jsch;
    Session ses;
    SSHConf conf;
    boolean forceReco;
    LinkedList<Job> managedJobs;
    int recoAttempt;
    private long idCounter;
    ReaderThread readerThread;

    public SSHService() {
        super();
        Log.i(TAG, "ctor called");
        managedJobs = new LinkedList<>();
        jobsLock = new ReentrantLock();
        notEmpty = jobsLock.newCondition();
        forceReco = false;
        conf = null;//pconf;
        idCounter = 0;
        recoAttempt = 0;
        jsch = new JSch();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        if (readerThread != null) {
            Log.i(TAG, "stopping reader thread");
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "got interrupted exception from reader thread");
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return binder;
    }

    public void setSSHConf(SSHConf c) {
        Log.i(TAG, "setting ssh conf");
        if (readerThread != null) {
            Log.i(TAG, "stopping reader thread");
            try {
                readerThread.interrupt();
                readerThread.join();
            } catch (Exception e) {
                Log.e(TAG, "got except while interrupting", e);
            }
        }
        Log.i(TAG, "starting read thread");
        conf = c;
        readerThread = new ReaderThread();
        readerThread.start();
    }

    public long addCommand(String cmd) {
        jobsLock.lock();
        long id;
        try {
            id = idCounter++;
            Log.i(TAG, "add command id=<" + id + ">, cmd=<" + cmd + ">, waiting for lock");
            managedJobs.add(new Job(id, cmd));
            Log.i(TAG, "cmd added, signaling");
            notEmpty.signal();
        } finally {
            jobsLock.unlock();
        }
        return id;
    }

    private void sessionConnect() throws JSchException {
        Log.i(TAG, "session connecting");
        ses = jsch.getSession(conf.user, conf.host, conf.port);
        ses.setPassword(conf.pw);
        ses.setConfig("StrictHostKeyChecking", "no");
        ses.connect(CONNECTION_TIMEOUT);
        recoAttempt = 0;
        forceReco = false;
    }

    private boolean needReconnection() {
        return forceReco || ses == null || !ses.isConnected();
    }

    public class SSHBinder extends Binder {
        SSHService getService() {
            return SSHService.this;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    class Job {
        static final int NEW = 1; // registered, waiting to be started
        static final int READ = 2; // started, can be read from
        static final int HANDLE = 3; // fully read from, waiting to be passed to the handler
        static final int DONE = 4; // hander called, waiting to be removed
        int state;
        long id;
        ChannelExec chan;
        String cmd;
        CommandResult result;
        InputStream stdout, stderr;
        StringBuilder outbuf = new StringBuilder();
        StringBuilder errbuf = new StringBuilder();
        Exception error;
        long startTime, lastReadTime;

        Job(long id, String cmd) {
            this.id = id;
            this.cmd = cmd;
            state = NEW;
        }

        public void start() throws IOException, JSchException {
            // does network IO
            Log.i(TAG, "job openChannel()");
            chan = (ChannelExec) ses.openChannel("exec");
            chan.setCommand(cmd);
            chan.setInputStream(null);
            stdout = chan.getInputStream();
            stderr = chan.getExtInputStream();
            Log.i(TAG, "job connect()");
            startTime = System.currentTimeMillis();
            chan.connect();
            Log.i(TAG, "moving to READ state");
            state = Job.READ;
        }

        public void read() throws IOException {
            while (stdout.available() > 0) {
                Log.i(TAG, "stdout: avail data, reading");
                int i = stdout.read(readBuf, 0, readBuf.length);
                Log.i(TAG, "stdout: read " + Integer.toString(i));
                if (i < 0) {
                    Log.e(TAG, "stdout read() < 0");
                    break;
                }
                outbuf.append(new String(readBuf, 0, i));
                lastReadTime = System.currentTimeMillis();
            }
            while (stderr.available() > 0) {
                Log.i(TAG, "stderr: avail data, reading");
                int i = stderr.read(readBuf, 0, readBuf.length);
                Log.i(TAG, "stderr: read");
                if (i < 0) {
                    Log.e(TAG, "stderr read() < 0");
                    break;
                }
                errbuf.append(new String(readBuf, 0, i));
                lastReadTime = System.currentTimeMillis();
            }
        }

        public boolean isAllRead() throws IOException {
            // by checking EOF first we avoid a race condition where output becomes available right
            // after checking if there was any and right before checking EOF...
            // very unlikely i guess but well.
            return chan.isClosed() && stderr.available() == 0 && stdout.available() == 0;
        }

        public void sendResult() {
            Log.d(TAG, "sending result of cmd id <" + id + ">");
            Intent intent = new Intent("msg");
            intent.putExtra("id", id);
            if (error != null) {
                Log.i(TAG, "sending command error");
                intent.putExtra("error", error);

            } else {
                Log.d(TAG, "sending command results");
                intent.putExtra("result", result);
            }
            LocalBroadcastManager.getInstance(SSHService.this).sendBroadcast(intent);
        }
    }

    // DO NOT RUN WHILE ALREADY ITERATING ON THE MANAGED JOB LIST
    private void invalidateAllPendingJobs(String errstr) {
        Log.e(TAG, "invalidating all jobs (" + errstr + ")");
        jobsLock.lock();
        try {
            for (Iterator<Job> it = managedJobs.iterator(); it.hasNext(); ) {
                Job job = it.next();
                if (job.state == Job.HANDLE)
                    job.sendResult();
                else if (job.state == Job.DONE) {
                    // nothing
                } else {
                    job.error = new Exception(errstr);
                    job.sendResult();
                }
                it.remove();
            }
        } finally {
            jobsLock.unlock();
        }
    }

    class ReaderThread extends Thread {
        @Override
        public void run() {
            Log.i(TAG, "in runnerthread");
            long waitTime = SESSION_LIFETIME;
            while (true) {
                // lock within the loop and sleep a bit after the
                // unlock to let the session network thread do its thing
                jobsLock.lock();
                try {
                    Log.i(TAG, "starting job waiting loop");
                    while (managedJobs.isEmpty()) {
                        // else we wait for jobs or end of wait time
                        try {
                            Log.i(TAG, "waiting on job list signal");
                            notEmpty.await(waitTime, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            // if something external asks us to quit, do it
                            Log.e(TAG, "thread interrupted!");
                            invalidateAllPendingJobs("service thread interrupted");
                            return;
                        }

                        Log.i(TAG, "continuing");

                        if (!managedJobs.isEmpty()) {
                            Log.i(TAG, "new jobs, leaving job waiting loop");
                            // we have jobs!
                            break;
                        }

                        // no new jobs, we waited the full time we asked for
                        if (waitTime == SESSION_LIFETIME) {
                            Log.i(TAG, "session lifetime elapsed");
                            // if we waited the session lifetime, we need to kill the session
                            try {
                                Log.i(TAG, "disconnecting session");
                                ses.disconnect();
                            } catch (Exception e) {
                                Log.e(TAG, "ignoring session disconnect errors", e);
                            } finally {
                                ses = null;
                            }
                            // and go in deep sleep...
                            waitTime = DEEPSLEEP;
                        }
                        // else we woke up from deep sleep with still nothing to do
                        // lets loop back to sleep... *yawn*
                    }

                    Log.i(TAG, "exiting job waiting loop");
                    // we are about to do stuff with jobs, reset the
                    // wait time to usual session lifetime
                    waitTime = SESSION_LIFETIME;

                    // we should release the lock while we reconnect as reconnection doesnt touch
                    // the managed job list
                    jobsLock.unlock();
                    try {
                        if (needReconnection()) {
                            Log.i(TAG, "need reconnection");
                            try {
                                if (ses != null) {
                                    if (ses.isConnected()) {
                                        Log.i(TAG, "session disconnecting");
                                        try {
                                            ses.disconnect();
                                        } catch (Exception e) {
                                            Log.e(TAG, "ignoring session disconnect errors", e);
                                        } finally {
                                            ses = null;
                                        }

                                    }
                                }
                                sessionConnect();
                            } catch (Exception e) {
                                if (!isNetworkAvailable()) {
                                    Log.e(TAG, "no network available");
                                    invalidateAllPendingJobs("no network available");

                                } else if (SSHException.isJSchAuthException(e)) {
                                    Log.e(TAG, "ssh auth exception, no need to retry");
                                    invalidateAllPendingJobs("ssh auth error");
                                } else {
                                    // TODO: how to report session errors?
                                    recoAttempt++;
                                    Log.e(TAG, "session (re)connection error (attempt " + recoAttempt + ")", e);
                                    if (recoAttempt > MAX_RECO_ATTEMPT) {
                                        Log.e(TAG, "max reco reached");
                                        invalidateAllPendingJobs("max reco reached");
                                    }
                                }
                            }
                        }
                    } finally {
                        jobsLock.lock();
                    }
                    Log.i(TAG, "entering managed jobs loop");
                    Log.i(TAG, "---------------------------");
                    for (Iterator<Job> it = managedJobs.iterator(); it.hasNext(); ) {
                        Job job = it.next();
                        Log.i(TAG, "processing job <" + job.id + "> in state " + job.state);
                        switch (job.state) {
                            case Job.NEW:
                                try {
                                    job.start();
                                    Log.i(TAG, "moving to READ state");
                                    job.state = Job.READ;
                                } catch (Exception e) {
                                    Log.e(TAG, "job NEW state exception, moving to HANDLE state", e);
                                    job.error = e;
                                    job.state = Job.HANDLE;
                                    forceReco = true;
                                }
                                break;
                            case Job.READ:
                                try {
                                    // can fail
                                    job.read();
                                    if (job.isAllRead()) {
                                        job.result = new CommandResult(job.outbuf.toString(), job.errbuf.toString(), job.chan.getExitStatus());
                                        Log.i(TAG, "moving to HANDLE state");
                                        job.state = Job.HANDLE;
                                    } else if (System.currentTimeMillis() - job.startTime >= CMD_TIMEOUT) {
                                        // XXX: use duration from lastReadTime instead?
                                        throw new Exception("cmd timeout");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "job READ state exception, moving to HANDLE state", e);
                                    job.error = e;
                                    job.state = Job.HANDLE;
                                    forceReco = true;
                                }
                                break;
                            case Job.HANDLE:
                                // do this in a separate thread?
                                // if yes, need to introduce new state
                                try {
                                    job.sendResult();
                                } catch (Exception e) {
                                    Log.e(TAG, "job HANDLE state exception", e);
                                }
                                Log.i(TAG, "removing job");
                                // we can remove the job from the managed list
                                job.state = Job.DONE;
                                it.remove();
                                break;
                        }
                    }
                } finally {
                    jobsLock.unlock();
                }
                // sleep a little without the lock, let the network
                // packets flow, and the app commands comming...
                try {
                    Log.i(TAG, "sleeping");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // something external wants us to quit...
                    invalidateAllPendingJobs("interrupted");
                    return;
                }
            }
        }
    }


}