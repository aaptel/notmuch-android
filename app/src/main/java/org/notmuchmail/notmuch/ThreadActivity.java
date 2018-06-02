package org.notmuchmail.notmuch;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.messages.ShowCmd;
import org.notmuchmail.notmuch.messages.ThreadMessage;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class ThreadActivity extends AppCompatActivity {
    private static final String TAG = "nmth";
    RecyclerView rv;
    List<ThreadMessage> messages;
    FloatingActionButton replybtn;
    SSHActivityHelper sshHelper;
    ShowCmd showCmd;
    String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        messages = new ArrayList<>();
        rv = (RecyclerView) findViewById(R.id.recycler_view_thread);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ThreadMessagesAdapter(this, messages));

        Bundle bundle = savedInstanceState;
        if (bundle == null) {
            bundle = getIntent().getExtras();
        }


        query = bundle.getString("query");
        showCmd = new ShowCmd(query);

        replybtn = (FloatingActionButton) findViewById(R.id.fab);
        replybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ThreadActivity.this, ComposeActivity.class);
                intent.putExtra("query", query);
                intent.putExtra("replyall", true);
                startActivity(intent);
            }
        });

        sshHelper = new SSHActivityHelper(this, new SSHActivityHelper.OnConnectedCallback() {
            @Override
            public void onConnected() {
                sshHelper.addCommand(showCmd.run(sshHelper.getSsh()), new CommandCallback() {
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ThreadActivity.this.getApplicationContext(), "Failed to run " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResult(CommandResult r) {
                        try {
                            showCmd.parse(r);
                            messages.addAll(showCmd.getResults());
                            rv.getAdapter().notifyDataSetChanged();
                        } catch (Exception e) {
                            Toast.makeText(ThreadActivity.this.getApplicationContext(), "Error while parsing notmuch output for " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        sshHelper.onCreate();

        // TODO add option to replyCmd in every message and run the compose activity
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
