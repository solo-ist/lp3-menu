package ist.solo.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu itself: an ordered list of {package, label} pairs, persisted as
 * JSON in SharedPreferences. Platform APIs only — no dependencies.
 */
final class Store {

    /** One row in the menu. */
    static final class Entry {
        final String pkg;
        /** What the user wants it called, which need not be the app's own label. */
        final String label;

        Entry(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private static final String PREFS = "menu";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_SEEDED = "seeded";

    /**
     * Seeded on first run, in this order. Anything not installed is skipped,
     * so a missing app costs nothing — it just doesn't appear.
     */
    private static final String[][] SEED = {
            {"im.mollylight.app",                          "Molly"},
            {"com.anthropic.claude",                       "Claude"},
            {"com.Slack",                                  "Slack"},
            {"com.spotify.music",                          "Spotify"},
            {"io.homeassistant.companion.android.minimal", "Home"},
            {"com.onepassword.android",                    "1Password"},
            {"com.todoist",                                "Todoist"},
            {"xyz.blueskyweb.app",                         "Bluesky"},
            {"com.sonos.acr2",                             "Sonos"},
            // gi-os BrightRemote — an Apple TV *remote*, not Apple's
            // streaming app (which is com.apple.atve.* and Android-TV shaped).
            {"com.gios.lightremote",                       "Apple TV"},
            {"com.gios.lightcamera",                       "Roll"},
            {"com.gios.lightcontrol",                      "Controls"},
            {"dev.imranr.obtainium",                       "Obtainium"},
            {"com.gios.brightmarket",                      "Market"},
            {"com.gios.brightmailbox",                     "Mailbox"},
            {"com.aurora.store",                           "Aurora"},
            {"com.zacksimpson.composer",                   "Composer"},
            {"com.gios.lightqr",                           "QR"},
    };

    private final SharedPreferences prefs;
    private final PackageManager pm;

    Store(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.pm = context.getPackageManager();
    }

    List<Entry> load() {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            seed();
        }
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_ENTRIES, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Entry(o.getString("pkg"), o.getString("label")));
            }
        } catch (JSONException ignored) {
            // Corrupt store is not worth crashing over; an empty menu is
            // recoverable from the UI.
        }
        return out;
    }

    void save(List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("pkg", e.pkg);
                o.put("label", e.label);
                arr.put(o);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply();
    }

    /** True if the package is installed and has something to launch. */
    boolean isLaunchable(String pkg) {
        return pm.getLaunchIntentForPackage(pkg) != null;
    }

    private void seed() {
        List<Entry> seeded = new ArrayList<>();
        for (String[] row : SEED) {
            if (isLaunchable(row[0])) {
                seeded.add(new Entry(row[0], row[1]));
            }
        }
        save(seeded);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }
}
