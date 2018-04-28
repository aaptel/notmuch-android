package org.notmuchmail.notmuch;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHConf;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nmssha";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    TextView cmd_output;
    Button send_btn;
    Button connect_btn;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cmd_output = findViewById(R.id.cmd_output);
        send_btn = findViewById(R.id.send_btn);
        connect_btn = findViewById(R.id.connect_btn);
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
                ssh.addCommand("echo $(hostname) $(date); sleep 10");
            }
        });
        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SSHConf conf = new SSHConf();
                conf.host = "192.168.2.111";
                conf.user = "aaptel";
                conf.pw = "xxxxxxxxx";
                conf.port = 22;
                ssh.setSSHConf(conf);
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
