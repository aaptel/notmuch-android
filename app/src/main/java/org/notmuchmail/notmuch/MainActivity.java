package org.notmuchmail.notmuch;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.utils;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHConf;
import org.notmuchmail.notmuch.ssh.SSHService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nmssha";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    TextView cmd_output;
    Button send_btn;
    Button connect_btn;
    Button settings_btn;
    Button search_btn;
    BroadcastReceiver recv;
    SSHService ssh;
    boolean bounded = false;

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

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "activ onstart");
        Intent intent = new Intent(this, SSHService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cmd_output = findViewById(R.id.cmd_output);
        send_btn = findViewById(R.id.send_btn);
        connect_btn = findViewById(R.id.connect_btn);
        settings_btn = findViewById(R.id.settings_btn);
        search_btn = findViewById(R.id.search_btn);
        cmd_output.setText("");

        recv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                Log.i(TAG, "recv broadcast intent " + b.toString());


                Exception e = (Exception) intent.getSerializableExtra("error");
                if (e != null) {
                    Log.i(TAG, "add error output <" + e.getMessage() + ">");
                    cmd_output.setText(cmd_output.getText() + "error:" + e.getMessage() + "\n");
                }

                CommandResult res = (CommandResult) intent.getSerializableExtra("result");
                if (res != null) {
                    Log.i(TAG, "add command output <" + res.stdout + ">");
                    cmd_output.setText(cmd_output.getText() + res.stdout);
                }
            }
        };
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ssh.addCommand("echo $(hostname) $(date)");
                ssh.addCommand(utils.makeCmd("echo", "$(foo)", "f '''b \"foo"));
            }
        });
        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SSHConf conf = new SSHConf(prefs);
                if (conf.isComplete()) {
                    ssh.setSSHConf(conf);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.ssh_conf_incomplete, Toast.LENGTH_LONG).show();
                }
            }
        });
        settings_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        LocalBroadcastManager.getInstance(this).registerReceiver(recv, new IntentFilter("msg"));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(recv);
        super.onDestroy();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
