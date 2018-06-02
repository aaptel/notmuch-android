package org.notmuchmail.notmuch.messages;

import java.io.Serializable;

// TODO: move all pure data classes in a model package
// TODO: rename ReplyCmd/ShowCmd/SearchCmd to ReplyCommand/ShowCommand/SearchCommand

public class ReplyMessage implements Serializable {
    public String inputQuery;
    public String subject;
    public String from;
    public String to;
    public String cc;
    public String inreplyto;
    public String references;
    public ThreadMessage original;
    public String bcc;

    public ReplyMessage() {
    }
}
