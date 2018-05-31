package org.notmuchmail.notmuch;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import org.notmuchmail.notmuch.helpers.SSHActivityHelper;
import org.notmuchmail.notmuch.messages.Show;
import org.notmuchmail.notmuch.messages.ThreadMessage;
import org.notmuchmail.notmuch.ssh.CommandCallback;
import org.notmuchmail.notmuch.ssh.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class ThreadActivity extends AppCompatActivity {
    private static final String TAG = "nmth";
    RecyclerView rv;
    List<ThreadMessage> messages;
    SSHActivityHelper sshHelper;
    Show show;
    String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
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

        messages = new ArrayList<>();
        rv = (RecyclerView) findViewById(R.id.recycler_view_thread);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ThreadMessagesAdapter(this, messages));


        if (savedInstanceState != null) {
            query = savedInstanceState.getString("query");
        } else {
            query = getIntent().getExtras().getString("query");
        }
        show = new Show(query);

        sshHelper = new SSHActivityHelper(this, new SSHActivityHelper.OnConnectedCallback() {
            @Override
            public void onConnected() {
                sshHelper.addCommand(show.run(sshHelper.getSsh()), new CommandCallback() {
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ThreadActivity.this.getApplicationContext(), "Failed to run " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onResult(CommandResult r) {
                        try {
                            show.parse(r);
                            messages.addAll(show.getResults());
                            rv.getAdapter().notifyDataSetChanged();
                        } catch (Exception e) {
                            Toast.makeText(ThreadActivity.this.getApplicationContext(), "Error while parsing notmuch output for " + query + " (" + e.toString() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
