package ist.solo.menu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Menu — a second toolbox for the apps LightOS shouldn't have to show,
 * drawn to match the real one: centred Akkurat Light on black, six rows to a
 * page, page dots down the right edge, swipe to page.
 *
 * Tap a row to launch. Long-press to rename, reorder or remove.
 */
public class MainActivity extends Activity {

    private Store store;
    private List<Store.Entry> entries;

    private LinearLayout rowColumn;
    private LinearLayout dotColumn;
    private GestureDetector gestures;

    private int page = 0;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);

        VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
        vibrator = vm != null ? vm.getDefaultVibrator() : null;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Style.BACKGROUND);

        rowColumn = new LinearLayout(this);
        rowColumn.setOrientation(LinearLayout.VERTICAL);
        rowColumn.setGravity(Gravity.CENTER);
        root.addView(rowColumn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        dotColumn = new LinearLayout(this);
        dotColumn.setOrientation(LinearLayout.VERTICAL);
        dotColumn.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        dotParams.setMarginEnd(dp(Style.DOT_MARGIN_END_DP));
        root.addView(dotColumn, dotParams);

        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dy) < dp(48)) return false;
                turnPage(dy < 0 ? 1 : -1);
                return true;
            }
        });

        setContentView(root);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestures.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always come back to the first page. Returning to Menu should feel
        // like opening it fresh, not resuming wherever you last paged to.
        page = 0;
        // Apps come and go behind our back; returning from a launch should
        // show current truth rather than a stale list.
        render();
    }

    // ---- rendering ---------------------------------------------------------

    /** Visible rows: installed entries, plus the add affordance at the end. */
    private List<Store.Entry> visibleRows() {
        List<Store.Entry> out = new ArrayList<>();
        for (Store.Entry e : entries) {
            if (store.isLaunchable(e.pkg)) out.add(e);
        }
        out.add(null); // sentinel for "+ add"
        return out;
    }

    private void render() {
        entries = store.load();
        List<Store.Entry> rows = visibleRows();

        int pageCount = Math.max(1, (int) Math.ceil(rows.size() / (double) Style.ROWS_PER_PAGE));
        if (page >= pageCount) page = pageCount - 1;
        if (page < 0) page = 0;

        rowColumn.removeAllViews();
        int start = page * Style.ROWS_PER_PAGE;
        int end = Math.min(start + Style.ROWS_PER_PAGE, rows.size());
        for (int i = start; i < end; i++) {
            final Store.Entry entry = rows.get(i);
            if (entry == null) {
                rowColumn.addView(row("+ add", Style.MUTED, this::addDialog, null));
            } else {
                rowColumn.addView(row(entry.label, Style.FOREGROUND,
                        () -> launch(entry), () -> editDialog(entry)));
            }
        }

        renderDots(pageCount);
    }

    /** One toolbox row: centred, Akkurat Light, fixed pitch. */
    private TextView row(String text, int color, final Runnable onTap, final Runnable onLongPress) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.create(Style.FONT_FAMILY, Typeface.NORMAL));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Style.ROW_TEXT_SP);
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int padH = dp(Style.ROW_PAD_HORIZONTAL_DP);
        tv.setPadding(padH, 0, padH, 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(Style.ROW_PITCH_DP)));
        tv.setOnClickListener(v -> onTap.run());
        if (onLongPress != null) {
            tv.setOnLongClickListener(v -> {
                onLongPress.run();
                return true;
            });
        }
        return tv;
    }

    private void renderDots(int pageCount) {
        dotColumn.removeAllViews();
        if (pageCount < 2) return; // LightOS hides the rail when there's one page
        int size = dp(Style.DOT_DIAMETER_DP);
        for (int i = 0; i < pageCount; i++) {
            View dot = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            if (i == page) {
                shape.setColor(Style.FOREGROUND);
            } else {
                shape.setColor(Style.BACKGROUND);
                shape.setStroke(Math.max(1, dp(1)), Style.FOREGROUND);
            }
            dot.setBackground(shape);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.topMargin = i == 0 ? 0 : dp(Style.DOT_PITCH_DP) - size;
            dotColumn.addView(dot, lp);
        }
    }

    private void turnPage(int delta) {
        List<Store.Entry> rows = visibleRows();
        int pageCount = Math.max(1, (int) Math.ceil(rows.size() / (double) Style.ROWS_PER_PAGE));
        int next = page + delta;
        // No feedback at the ends — the page didn't turn, so nothing happened.
        if (next < 0 || next >= pageCount) return;
        page = next;
        haptic();
        render();
    }

    /** The same 40ms one-shot the LightOS toolbox plays on a page turn. */
    private void haptic() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(
                VibrationEffect.createOneShot(Style.HAPTIC_MS, VibrationEffect.DEFAULT_AMPLITUDE),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH));
    }

    private void launch(Store.Entry entry) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(entry.pkg);
        if (intent == null) {
            Toast.makeText(this, entry.label + " is not installed", Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            // The target can disappear or become unlaunchable between resolving
            // the intent and starting it — uninstalled, disabled, or guarded by
            // a permission we do not hold. Report and re-render rather than die.
            Toast.makeText(this, "Could not open " + entry.label, Toast.LENGTH_SHORT).show();
            render();
        }
    }

    // ---- editing -----------------------------------------------------------

    private void editDialog(final Store.Entry entry) {
        final int index = indexOf(entry.pkg);
        String[] actions = {"Rename", "Move up", "Move down", "Remove"};
        new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: renameDialog(entry); break;
                        case 1: move(index, index - 1); break;
                        case 2: move(index, index + 1); break;
                        case 3: remove(index); break;
                    }
                })
                .show();
    }

    private void renameDialog(final Store.Entry entry) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(entry.label);
        input.setSelection(entry.label.length());

        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    int i = indexOf(entry.pkg);
                    if (i >= 0) {
                        entries.set(i, new Store.Entry(entry.pkg, name));
                        store.save(entries);
                        render();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addDialog() {
        final List<Store.Entry> candidates = installedApps();
        if (candidates.isEmpty()) {
            Toast.makeText(this, "Nothing left to add", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) labels[i] = candidates.get(i).label;

        new AlertDialog.Builder(this)
                .setTitle("Add to Menu")
                .setItems(labels, (d, which) -> {
                    entries.add(candidates.get(which));
                    store.save(entries);
                    render();
                })
                .show();
    }

    private void move(int from, int to) {
        if (from < 0 || to < 0 || from >= entries.size() || to >= entries.size()) return;
        Collections.swap(entries, from, to);
        store.save(entries);
        render();
    }

    private void remove(int index) {
        if (index < 0 || index >= entries.size()) return;
        entries.remove(index);
        store.save(entries);
        render();
    }

    // ---- helpers -----------------------------------------------------------

    private int indexOf(String pkg) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).pkg.equals(pkg)) return i;
        }
        return -1;
    }

    /** Launchable apps not already in the menu, by their own label, A-Z. */
    private List<Store.Entry> installedApps() {
        PackageManager pm = getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(probe, 0);

        List<Store.Entry> out = new ArrayList<>();
        for (ResolveInfo ri : resolved) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || indexOf(pkg) >= 0) continue;
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            out.add(new Store.Entry(pkg, pm.getApplicationLabel(ai).toString()));
        }
        Collections.sort(out, new Comparator<Store.Entry>() {
            @Override public int compare(Store.Entry a, Store.Entry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
