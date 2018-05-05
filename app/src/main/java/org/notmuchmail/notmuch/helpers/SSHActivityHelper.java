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
    SSHService ssh;
    boolean bounded = false;
    Activity activity;
    SparseArray<CommandCallback> commandCallbacks;

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ssh = ((SSHService.SSHBinder) service).getService();
            bounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ssh = null;
            bounded = false;
        }
    };

    public SSHActivityHelper(Activity activ) {
        activity = activ;
        commandCallbacks = new SparseArray<>();
    }

    public void onStart() {
        Intent intent = new Intent(activity, SSHService.class);
        activity.bindService(intent, connection, activity.BIND_AUTO_CREATE);
    }

    public void onStop() {
        if (bounded)
            activity.unbindService(connection);
    }

    public void onCreate() {
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
                    return;
                }

                CommandResult res = (CommandResult) intent.getSerializableExtra("result");
                if (res != null) {
                    Log.i(TAG, "cmd command output <" + res.stdout + ">");
                    cb.onResult(res);
                    return;
                }
                Log.wtf(TAG, "unreachable code");
            }
        };
        LocalBroadcastManager.getInstance(activity).registerReceiver(recv, new IntentFilter("msg"));
    }

    public void onDestroy() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(recv);
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

    public SSHService getSsh() {
        return ssh;
    }
}
