package org.notmuchmail.notmuch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.messages.Show;
import org.notmuchmail.notmuch.messages.ThreadMessage;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHConf;
import org.notmuchmail.notmuch.ssh.SSHException;

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
    SSHActivityHelper sshHelper;

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "activ onstart");
        sshHelper.onStart();
    }

    @Override
    protected void onStop() {
        sshHelper.onStop();
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

        send_btn.setOnClickListener(new View.OnClickListener() {
            Show show = new Show("thread:000000000000c6f3");

            @Override
            public void onClick(View v) {

                sshHelper.addCommand(show.run(sshHelper.getSsh()), new CommandCallback() {
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "error", e);
                            }

                            @Override
                            public void onResult(CommandResult r) {
                                try {
                                    show.parse(r);
                                    for (ThreadMessage m : show.getResults()) {
                                        cmd_output.append(m.toString());
                                    }
                                } catch (SSHException e) {
                                    Log.e(TAG, "error", e);
                                }
                            }
                        }
                );
            }
        });
        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SSHConf conf = new SSHConf(prefs);
                if (conf.isComplete()) {
                    sshHelper.getSsh().setSSHConf(conf);
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
        sshHelper = new SSHActivityHelper(this, null);
        sshHelper.onCreate();
    }

    @Override
    protected void onDestroy() {
        sshHelper.onDestroy();
        super.onDestroy();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
