package org.notmuchmail.notmuch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.ssh.SSHConf;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nmssha";
    boolean checkAgain = true;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    SSHActivityHelper sshHelper;

    @Override
    protected void onStart() {
        super.onStart();
        sshHelper.onStart();
    }

    @Override
    protected void onStop() {
        sshHelper.onStop();
        super.onStop();
    }

    void welcomeCheck() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean first_run = prefs.getBoolean("first_run", true);
        prefs.edit().putBoolean("first_run", false).commit();
        SSHConf conf = new SSHConf(prefs);
        Log.i(TAG, "welcome check run");
        Log.i(TAG, "first_run == " + first_run);
        if (!first_run && conf.isComplete()) {
            sshHelper.getSsh().setSSHConf(conf);
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        } else {
            setContentView(R.layout.activity_main);
            Button b = findViewById(R.id.welcome_settings_btn);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sshHelper = new SSHActivityHelper(this, new SSHActivityHelper.OnConnectedCallback() {
            @Override
            public void onConnected() {
                welcomeCheck();
            }
        });
        sshHelper.onCreate();
        checkAgain = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkAgain)
            welcomeCheck();
        checkAgain = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkAgain = true;
    }

    @Override
    protected void onDestroy() {
        sshHelper.onDestroy();
        checkAgain = true;
        super.onDestroy();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
