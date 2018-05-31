package org.notmuchmail.notmuch;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.notmuchmail.notmuch.messages.ThreadMessage;

import java.util.List;

public class ThreadMessagesAdapter extends RecyclerView.Adapter<ThreadMessagesAdapter.MyViewHolder> {
    private Context mContext;
    private List<ThreadMessage> messages;


    public ThreadMessagesAdapter(Context ctx, List<ThreadMessage> msgs) {
        this.mContext = ctx;
        this.messages = msgs;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.thread_message, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.display(messages.get(position));
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView from;
        private final TextView subject;
        private final TextView content;
        private ThreadMessage msg;

        public MyViewHolder(final View itemView) {
            super(itemView);

            from = ((TextView) itemView.findViewById(R.id.from));
            subject = ((TextView) itemView.findViewById(R.id.subject));
            content = ((TextView) itemView.findViewById(R.id.content));

//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    new AlertDialog.Builder(itemView.getContext())
//                            .setTitle(currentPair.first)
//                            .setMessage(currentPair.second)
//                            .show();
//                }
//            });
        }

        public void display(ThreadMessage m) {
            msg = m;
            from.setText(m.from);
            subject.setText(m.subject);
            content.setText(m.text);
        }
    }

}