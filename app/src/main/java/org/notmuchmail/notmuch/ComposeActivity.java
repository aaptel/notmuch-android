package org.notmuchmail.notmuch;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.messages.Reply;
import org.notmuchmail.notmuch.messages.ReplyMessage;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "nmcmp";

    SSHActivityHelper sshHelper;
    String query;
    Reply reply;
    ReplyMessage result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        if (savedInstanceState != null) {
            query = savedInstanceState.getString("origin");
        } else {
            query = getIntent().getExtras().getString("origin");
        }
        reply = new Reply(query);

        sshHelper = new SSHActivityHelper(this, new SSHActivityHelper.OnConnectedCallback() {
            @Override
            public void onConnected() {
                sshHelper.addCommand(reply.run(sshHelper.getSsh()), new CommandCallback() {
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ComposeActivity.this.getApplicationContext(), "Failed to run " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResult(CommandResult r) {
                        try {
                            reply.parse(r);
                            setReply(reply.getResult());
                        } catch (Exception e) {
                            Toast.makeText(ComposeActivity.this.getApplicationContext(), "Error while parsing notmuch output for " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        sshHelper.onCreate();
    }

    public void setReply(ReplyMessage rm) {
        this.result = rm;
        getSupportActionBar().setTitle(result.subject);
        ((TextView) findViewById(R.id.from)).setText(result.from);
        ((TextView) findViewById(R.id.to)).setText(result.to);
        ((TextView) findViewById(R.id.cc)).setText(result.cc);
        ((TextView) findViewById(R.id.bcc)).setText(result.bcc);
        ((CheckedTextView) findViewById(R.id.message)).setText(result.original.quotedText());
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
