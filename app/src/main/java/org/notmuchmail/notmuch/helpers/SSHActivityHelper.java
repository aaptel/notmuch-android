package org.notmuchmail.notmuch.helpers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import org.notmuchmail.notmuch.R;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHConf;
import org.notmuchmail.notmuch.ssh.SSHService;

public class SSHActivityHelper {
    private static final String TAG = "nmsshhelper";
    BroadcastReceiver recv;
    SSHService ssh = null;
    boolean bounded = false;
    boolean boundingStarted = false;
    Activity activity;
    SparseArray<CommandCallback> commandCallbacks;
    OnConnectedCallback cb;
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ssh = ((SSHService.SSHBinder) service).getService();
            bounded = true;
            Log.i(TAG, "service connected (ssh=" + ssh.toString() + ")");
            if (cb != null)
                cb.onConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "service disconnected");
            ssh = null;
            bounded = false;
            boundingStarted = false;
        }
    };

    public SSHActivityHelper(Activity activ, OnConnectedCallback cb) {
        activity = activ;
        this.cb = cb;
        commandCallbacks = new SparseArray<>();
    }

    private void bind() {
        if (!bounded) {
            if (boundingStarted) {
                Log.w(TAG, "bounding already started");
                return;
            }
            Log.i(TAG, "bind service");
            Intent intent = new Intent(activity, SSHService.class);
            boolean r = activity.bindService(intent, connection, activity.BIND_AUTO_CREATE);
            if (r == false)
                Log.wtf(TAG, "bindservice returned false");
            boundingStarted = true;
        }
    }

    private void unbind() {
        if (bounded) {
            Log.i(TAG, "unbinding");
            activity.unbindService(connection);
        }
    }

    private void registerBroadcast() {
        recv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                Log.i(TAG, "recv broadcast intent " + b.toString());

                int cmdid = intent.getIntExtra("id", -1);
                if (cmdid < 0) {
                    Log.e(TAG, "negative id?!, skipping results...");
                    return;
                }

                CommandCallback cb = commandCallbacks.get(cmdid);
                if (cb == null) {
                    Log.i(TAG, "received command result from unregistered id, skipping");
                    return;
                }

                Exception e = (Exception) intent.getSerializableExtra("error");
                if (e != null) {
                    Log.e(TAG, "cmd error output <" + e.getMessage() + ">", e);
                    cb.onError(e);
                    removeCommand(cmdid);
                    return;
                }

                CommandResult res = (CommandResult) intent.getSerializableExtra("result");
                if (res != null) {
                    Log.i(TAG, "cmd command output <" + res.stdout + ">");
                    cb.onResult(res);
                    removeCommand(cmdid);
                    return;
                }
                Log.wtf(TAG, "unreachable code");
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(recv, new IntentFilter("msg"));
    }

    private void unregisterBroadcast() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(recv);
    }

    public SSHService getSsh() {
        if (ssh == null) {
            Log.wtf(TAG, "ssh null, boundingStarted=" + boundingStarted + " bounded=" + bounded);
            onStart();
        }
        return ssh;
    }

    public void connect() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SSHConf conf = new SSHConf(prefs);
        if (conf.isComplete()) {
            ssh.setSSHConf(conf);
        } else {
            Toast.makeText(activity.getApplicationContext(), R.string.ssh_conf_incomplete, Toast.LENGTH_LONG).show();
        }
    }

    public void addCommand(String cmd, CommandCallback cb) {
        int id = ssh.addCommand(cmd);
        commandCallbacks.append(id, cb);
    }

    public void addCommand(int id, CommandCallback cb) {
        commandCallbacks.append(id, cb);
    }

    private void removeCommand(int id) {
        commandCallbacks.delete(id);
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public void onCreate() {
        bind();
        registerBroadcast();
    }

    public void onDestroy() {
        unbind();
        unregisterBroadcast();
    }

    public interface OnConnectedCallback {
        void onConnected();
    }
}
