package org.notmuchmail.notmuch;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("org.notmuchmail.notmuch", appContext.getPackageName());
    }

    @Test
    public void jsonArray() throws JSONException {
        String input = "[{\"foo\":1},{\"foo\":2}]";
        //JSONObject jo = new JSONObject(input);
        JSONArray ar = new JSONArray(input);
        assertEquals(ar.getJSONObject(0).getInt("foo"), 1);
    }
}
