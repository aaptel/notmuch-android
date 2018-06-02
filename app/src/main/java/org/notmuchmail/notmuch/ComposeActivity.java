package org.notmuchmail.notmuch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.messages.ReplyCmd;
import org.notmuchmail.notmuch.messages.ReplyMessage;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "nmcmp";
    Bundle paramBundle;
    SSHActivityHelper sshHelper;
    String query;
    ReplyCmd replyCmd;


    public void setupReply() {
        replyCmd = new ReplyCmd(paramBundle.getString("query"), paramBundle.getBoolean("replyall"));
        sshHelper.addCommand(replyCmd.run(sshHelper.getSsh()), new CommandCallback() {
            @Override
            public void onError(Exception e) {
                Toast.makeText(ComposeActivity.this.getApplicationContext(), "Failed to run " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResult(CommandResult r) {
                try {
                    replyCmd.parse(r);
                    ReplyMessage rm = replyCmd.getResult();
                    ((EditText) findViewById(R.id.subject)).setText(rm.subject);
                    ((EditText) findViewById(R.id.from)).setText(rm.from);
                    ((EditText) findViewById(R.id.to)).setText(rm.to);
                    ((EditText) findViewById(R.id.cc)).setText(rm.cc);
                    ((EditText) findViewById(R.id.bcc)).setText(rm.bcc);
                    ((EditText) findViewById(R.id.message)).setText(rm.original.quotedText());
                } catch (Exception e) {
                    Log.e(TAG, "error while parsing notmuch output", e);
                    Toast.makeText(ComposeActivity.this.getApplicationContext(), "Error while parsing notmuch output for " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void setupNew() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("New Message");

        if (savedInstanceState != null) {
            paramBundle = savedInstanceState;
        } else {
            paramBundle = getIntent().getExtras();
            if (paramBundle == null) {
                paramBundle = new Bundle();
            }
        }

        sshHelper = new SSHActivityHelper(this, new SSHActivityHelper.OnConnectedCallback() {
            @Override
            public void onConnected() {

                if (paramBundle.get("replyall") != null) {
                    setupReply();
                } else {
                    setupNew();
                }
            }
        });
        sshHelper.onCreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                // TODO: implement send
                Toast.makeText(this, "TODO: implement send :)", Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_attach:
                // TODO: implement attach
                Toast.makeText(this, "TODO: implement attach :)", Toast.LENGTH_LONG).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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

    @Override
    protected void onDestroy() {
        sshHelper.onDestroy();
        super.onDestroy();
    }
}
