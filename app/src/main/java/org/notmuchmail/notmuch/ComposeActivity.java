package org.notmuchmail.notmuch;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
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
                    getSupportActionBar().setTitle(rm.subject);
                    ((TextView) findViewById(R.id.from)).setText(rm.from);
                    ((TextView) findViewById(R.id.to)).setText(rm.to);
                    ((TextView) findViewById(R.id.cc)).setText(rm.cc);
                    ((TextView) findViewById(R.id.bcc)).setText(rm.bcc);
                    ((CheckedTextView) findViewById(R.id.message)).setText(rm.original.quotedText());
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

        if (savedInstanceState != null) {
            paramBundle = savedInstanceState;
        } else {
            paramBundle = getIntent().getExtras();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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
