package org.notmuchmail.notmuch.messages;

import org.json.JSONArray;
import org.json.JSONObject;
import org.notmuchmail.notmuch.helpers.utils;
import org.notmuchmail.notmuch.ssh.CommandResult;
import org.notmuchmail.notmuch.ssh.SSHException;
import org.notmuchmail.notmuch.ssh.SSHService;

import java.util.ArrayList;
import java.util.List;

public class Search {
    public static final int SEARCH_LIMIT = 10;

    public enum ResultOrder {
        NEWEST_FIRST("newest-first"),
        OLDEST_FIRST("oldest-first");

        public final String arg;

        ResultOrder(String a) {
            arg = a;
        }
    }

    String inputQuery;
    ResultOrder order;
    int offset;
    List<SearchResult> results;

    public Search(String query, ResultOrder orderType) {
        this.inputQuery = query;
        order = orderType;
        offset = 0;
    }

    public void reset() {
        results = new ArrayList<>();
        offset = 0;
    }

    public int runMore(SSHService ssh) {
        int id = ssh.addCommand(utils.makeCmd(
                "notmuch",
                "search",
                "--format=json", "--format-version=2",
                "--limit=" + SEARCH_LIMIT, "--sort=" + order.arg,
                "--offset=" + offset,
                inputQuery));
        offset += SEARCH_LIMIT;
        return id;
    }

    public int parse(CommandResult r) throws SSHException {
        int added = 0;
        if (results == null) {
            results = new ArrayList<>();
        }

        if (r.stdout.isEmpty() && r.exit == 0)
            return added;

        try {
            JSONArray jar = new JSONArray(r.stdout);
            for (int i = 0; i < jar.length(); i++) {
                JSONObject o = jar.getJSONObject(i);
                String tid = o.getString("thread");
                int timestamp = o.getInt("timestamp");
                String daterelative = o.getString("date_relative");
                int matched = o.getInt("matched");
                int total = o.getInt("total");
                String authors = o.getString("authors");
                String subject = o.getString("subject");
                JSONArray qar = o.getJSONArray("query");
                ArrayList<String> queries = new ArrayList<String>();
                for (int j = 0; j < qar.length(); j++) {
                    String s = qar.getString(j);
                    if (s != null) {
                        queries.add(s);
                    }
                }
                JSONArray tar = o.getJSONArray("tags");
                ArrayList<String> tags = new ArrayList<String>();
                for (int j = 0; j < tar.length(); j++) {
                    String s = tar.getString(j);
                    if (s != null) {
                        tags.add(s);
                    }
                }
                results.add(new SearchResult(tid, timestamp, daterelative, matched, total, authors, subject, queries, tags));
                added++;
            }
        } catch (Exception e) {
            throw new SSHException("error while parsing search query json output", e);
        }
        return added;
    }

    public String getInputQuery() {
        return inputQuery;
    }

    public int parseSample() throws SSHException {
        CommandResult r = new CommandResult(SAMPLE_JSON, "", 0);
        return parse(r);
    }

    public List<SearchResult> getResults() {
        return results;
    }

    private static final String SAMPLE_JSON = "[{\"thread\": \"000000000000c5b4\", \"timestamp\": 1525282035, \"date_relative\": \"Today 19:27\", \"matched\": 2, \"total\": 16, \"authors\": \"Jeremy Allison via samba-technical, Ralph Böhme via samba-technical| David Disseldorp via samba-technical, Uri Simchoni via samba-technical\", \"subject\": \"[PATCH] Add missing async fsync_send()/recv() code to ceph VFS.\", \"query\": [\"id:20180502163603.GB218929@jra3 id:20180502172713.d7qwlnwgq5tqofwz@kazak\", \"id:20180427230743.GB215322@jra3 id:20180428141204.jrrpblbmreoangwk@kazak id:20180430111258.18c73dae@samba.org id:20180430092913.fedsob6anvucsbsc@kazak id:20180430163041.GA208569@jra3 id:20180430170258.mzzhkgtqfvb5umzz@kazak id:20180430214625.GA30112@jra3 id:20180501190818.GB127818@jra3 id:20180501191025.GC127818@jra3 id:5d7eba9b-1193-1c40-712e-5fe5e55656aa@samba.org id:20180501201552.newogqmm7rqfkfv6@kazak id:20180501201654.yp6hsjzia2tyc73e@kazak id:20180501211054.wphtfos3ml3q3keb@jeremy-acer id:9bf729d0-257d-f8c8-9346-3ba9518bb25e@samba.org\"], \"tags\": [\"attachment\", \"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c5ab\", \"timestamp\": 1525281090, \"date_relative\": \"Today 19:11\", \"matched\": 1, \"total\": 2, \"authors\": \"Denis Cardon via samba| Vincent S. Cojot via samba\", \"subject\": \"[Samba] IP aliases of DCs to prevent DNS timeouts\", \"query\": [\"id:b607b5bb-e811-5b1f-c91f-6ed30dc9365c@tranquil.it\", \"id:alpine.LRH.2.21.1804271148120.19672@daltigoth.lasthome.solace.krynn\"], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c662\", \"timestamp\": 1525278768, \"date_relative\": \"Today 18:32\", \"matched\": 1, \"total\": 1, \"authors\": \"Kevan Barney\", \"subject\": \"News Release: SUSE Ready Certification for SUSE CaaS Platform Now Available for Partners’ Containerized Applications\", \"query\": [\"id:BY2PR18MB0373DDEB885F9090A3C0D5FCFF800@BY2PR18MB0373.namprd18.prod.outlook.com\", null], \"tags\": [\"attachment\", \"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c661\", \"timestamp\": 1525278747, \"date_relative\": \"Today 18:32\", \"matched\": 1, \"total\": 1, \"authors\": \"Katrin Murr\", \"subject\": \"[maxtorhof] B2Run Firmenlauf (Corporate Challenge): July 24, 2018\", \"query\": [\"id:C1D9C01E5ABAF34CB6DEE50FB19D2D7A3775FC81@NWBXMB02.microfocus.com\", null], \"tags\": [\"attachment\", \"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"00000000000004db\", \"timestamp\": 1525278128, \"date_relative\": \"Today 18:22\", \"matched\": 1, \"total\": 62, \"authors\": \"bugzilla_noreply@novell.com\", \"subject\": \"[samba-maintainers] [Bug 1071136] L3: kvno get out of sync after samba \\\"net ads changetrustpw\\\"\", \"query\": [\"id:bug-1071136-9908-ZrZCzn2P08@http.bugzilla.suse.com/\", \"id:bug-1071136-9908@http.bugzilla.suse.com/ id:bug-1071136-9908-1VwLDjiXgj@http.bugzilla.suse.com/ id:bug-1071136-9908-1gBMEgKxtf@http.bugzilla.suse.com/ id:bug-1071136-9908-pftTHwf8W7@http.bugzilla.suse.com/ id:bug-1071136-9908-2IGmWJ0gDZ@http.bugzilla.suse.com/ id:bug-1071136-9908-iqNfoiGY2z@http.bugzilla.suse.com/ id:bug-1071136-9908-PGzZ7IOvlf@http.bugzilla.suse.com/ id:bug-1071136-9908-vHfSynNUSP@http.bugzilla.suse.com/ id:bug-1071136-9908-rgqoWo1KEN@http.bugzilla.suse.com/ id:bug-1071136-9908-o1UGNjDUcw@http.bugzilla.suse.com/ id:bug-1071136-9908-hONA489z6j@http.bugzilla.suse.com/ id:bug-1071136-9908-IhYCfMBOVO@http.bugzilla.suse.com/ id:bug-1071136-9908-KoMqb9QzPD@http.bugzilla.suse.com/ id:bug-1071136-9908-Su5H9u4YlB@http.bugzilla.suse.com/ id:bug-1071136-9908-HrmoDD2zuH@http.bugzilla.suse.com/ id:bug-1071136-9908-fweJAh6CJB@http.bugzilla.suse.com/ id:bug-1071136-9908-uXVWCYNbw9@http.bugzilla.suse.com/ id:bug-1071136-9908-M68RiqnrYt@http.bugzilla.suse.com/ id:bug-1071136-9908-aK6W1Rxpki@http.bugzilla.suse.com/ id:bug-1071136-9908-c030MGHGLM@http.bugzilla.suse.com/ id:bug-1071136-9908-6bIGJs3iuS@http.bugzilla.suse.com/ id:bug-1071136-9908-tydcXPJCKf@http.bugzilla.suse.com/ id:bug-1071136-9908-qjKaJAQMQc@http.bugzilla.suse.com/ id:bug-1071136-9908-ObGl6LCI5q@http.bugzilla.suse.com/ id:bug-1071136-9908-qvn2JF7qX9@http.bugzilla.suse.com/ id:bug-1071136-9908-Dnp5NmDVbS@http.bugzilla.suse.com/ id:bug-1071136-9908-ZfYMpWr9rw@http.bugzilla.suse.com/ id:bug-1071136-9908-JMXdD01zet@http.bugzilla.suse.com/ id:bug-1071136-9908-gk7O8ukSSc@http.bugzilla.suse.com/ id:bug-1071136-9908-gVfc5jS8AL@http.bugzilla.suse.com/ id:bug-1071136-9908-gQVD8neuDL@http.bugzilla.suse.com/ id:bug-1071136-9908-7H0Yc1zdGF@http.bugzilla.suse.com/ id:bug-1071136-9908-RTL3WDTawE@http.bugzilla.suse.com/ id:bug-1071136-9908-0Li9c7pYXb@http.bugzilla.suse.com/ id:bug-1071136-9908-vqTZfeOhrB@http.bugzilla.suse.com/ id:bug-1071136-9908-nvVB1mwOv0@http.bugzilla.suse.com/ id:bug-1071136-9908-125kKAot4i@http.bugzilla.suse.com/ id:bug-1071136-9908-IQMStMg09E@http.bugzilla.suse.com/ id:bug-1071136-9908-50E0TM7OTw@http.bugzilla.suse.com/ id:bug-1071136-9908-y5ySQOi2Na@http.bugzilla.suse.com/ id:bug-1071136-9908-KmM6rfFkmh@http.bugzilla.suse.com/ id:bug-1071136-9908-EyLFLQgtCC@http.bugzilla.suse.com/ id:bug-1071136-9908-dCE1W1Fu5e@http.bugzilla.suse.com/ id:bug-1071136-9908-AaJ846ahL9@http.bugzilla.suse.com/ id:bug-1071136-9908-qfVcQJMNHz@http.bugzilla.suse.com/ id:bug-1071136-9908-A2Ty7poQDE@http.bugzilla.suse.com/ id:bug-1071136-9908-lSecOxndVj@http.bugzilla.suse.com/ id:bug-1071136-9908-kA3Z702DYn@http.bugzilla.suse.com/ id:bug-1071136-9908-NpDbNsit8A@http.bugzilla.suse.com/ id:bug-1071136-9908-8j7fwjuoRw@http.bugzilla.suse.com/ id:bug-1071136-9908-hFfKKrkzL7@http.bugzilla.suse.com/ id:bug-1071136-9908-duJa6uzLNL@http.bugzilla.suse.com/ id:bug-1071136-9908-5FTRfvCh9u@http.bugzilla.suse.com/ id:bug-1071136-9908-FQNJ1rBfHf@http.bugzilla.suse.com/ id:bug-1071136-9908-K3qmFG8mw2@http.bugzilla.suse.com/ id:bug-1071136-9908-Xw0DZbX2ZO@http.bugzilla.suse.com/ id:bug-1071136-9908-J5EsktOvSQ@http.bugzilla.suse.com/ id:bug-1071136-9908-raAEb4K9Cq@http.bugzilla.suse.com/ id:bug-1071136-9908-SByF9xcDEC@http.bugzilla.suse.com/ id:bug-1071136-9908-5nvmoP9sLD@http.bugzilla.suse.com/ id:bug-1071136-9908-ldBgRiJhKC@http.bugzilla.suse.com/\"], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c65f\", \"timestamp\": 1525277379, \"date_relative\": \"Today 18:09\", \"matched\": 8, \"total\": 8, \"authors\": \"Christian Brauner, Al Viro\", \"subject\": \"[PATCH 0/6 resend] statfs: handle mount propagation\", \"query\": [\"id:20180502154239.14013-1-christian.brauner@ubuntu.com id:20180502154239.14013-2-christian.brauner@ubuntu.com id:20180502154239.14013-3-christian.brauner@ubuntu.com id:20180502154239.14013-4-christian.brauner@ubuntu.com id:20180502154239.14013-5-christian.brauner@ubuntu.com id:20180502154239.14013-6-christian.brauner@ubuntu.com id:20180502154239.14013-7-christian.brauner@ubuntu.com id:20180502160939.GU30522@ZenIV.linux.org.uk\", null], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c660\", \"timestamp\": 1525276818, \"date_relative\": \"Today 18:00\", \"matched\": 1, \"total\": 1, \"authors\": \"emacs-devel-request@gnu.org\", \"subject\": \"Emacs-devel Digest, Vol 171, Issue 6\", \"query\": [\"id:mailman.109.1525276818.22367.emacs-devel@gnu.org\", null], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"0000000000000b26\", \"timestamp\": 1525275450, \"date_relative\": \"Today 17:37\", \"matched\": 2, \"total\": 5, \"authors\": \"Andrew Dumaresq via samba, Rowland Penny via samba| Andrew Bartlett via samba\", \"subject\": \"[Samba] Fwd: Samba broken after 4.8 upgrade\", \"query\": [\"id:CAAGr_Ph-dxgJqxSyuCeGef0t_7EecNkW0F4bj-a_Y550SkCXsw@mail.gmail.com id:20180502163730.560c3ea2@devstation.samdom.example.com\", \"id:CAAGr_Pjx819aiSmdGbNfcZBbg+awTKhPReiD0Q1jTYjSNy5oJw@mail.gmail.com id:1523606648.364143.9.camel@samba.org id:CAAGr_PhJ_htFF052FRaHqouKBFfAJs0_PMvyoos8G=9QRKfadw@mail.gmail.com\"], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c65e\", \"timestamp\": 1525275368, \"date_relative\": \"Today 17:36\", \"matched\": 1, \"total\": 1, \"authors\": \"Michal Kubecek\", \"subject\": \"[kernel] SLE15: problem with pre-commit hook\", \"query\": [\"id:20180502153608.yj5neegep7eb5p4o@unicorn.suse.cz\", null], \"tags\": [\"inbox\", \"new\", \"unread\"]},\n" +
            "{\"thread\": \"000000000000c434\", \"timestamp\": 1525275245, \"date_relative\": \"Today 17:34\", \"matched\": 2, \"total\": 12, \"authors\": \"Knut Krüger via samba, Rowland Penny via samba| L A Walsh via samba, L.P.H. van Belle via samba\", \"subject\": \"[Samba] Speedup windows client [was] What is the maximum speed for download from a samba share\", \"query\": [\"id:86d78c20-6b9e-fe68-2333-2eb599d06a38@knut-krueger.de id:20180502163405.755cd873@devstation.samdom.example.com\", \"id:cb57fc3d-3ccd-1f92-9b25-4d492754f8ca@knut-krueger.de id:610d44f7-ca39-d37c-2a16-86eced6ca495@knut-krueger.de id:3a782820-1629-0559-804c-dd455ecf3661@knut-krueger.de id:5faa9782-989a-8383-4a9d-25058b584ef2@knut-krueger.de id:5ADAB60D.60000@tlinx.org id:e0501161-d117-5e92-d1f9-29bb7a35dc0b@knut-krueger.de id:f3c0a167-4644-69de-0dc9-48610eb9d10e@knut-krueger.de id:5ADBF3DD.6030803@tlinx.org id:ccd3bc72-24c4-d86d-2f10-f89f81870032@knut-krueger.de id:vmime.5ae034ca.6feb.5357358627af06a@ms249-lin-003.rotterdam.bazuin.nl\"], \"tags\": [\"inbox\", \"new\", \"unread\"]}]\n";
}
