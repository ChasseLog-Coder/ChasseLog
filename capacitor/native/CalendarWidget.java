package __PKG__;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

// Widget « calendrier du mois » : journées de chasse du mois (traque en orange), ajout rapide.
public class CalendarWidget extends AppWidgetProvider {
    static final String NAV = "__PKG__.WIDGET_NAV";
    private static final String[] MONTHS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
    private static final int[] WEEKDAYS = {R.id.wd0, R.id.wd1, R.id.wd2, R.id.wd3, R.id.wd4, R.id.wd5, R.id.wd6};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            try { m.updateAppWidget(id, build(c, id)); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (NAV.equals(i.getAction())) {
            int id = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
            SharedPreferences p = WidgetStore.prefs(c);
            int d = i.getIntExtra("delta", 0);
            p.edit().putInt("off_" + id, d == 0 ? 0 : p.getInt("off_" + id, 0) + d).apply();
            try { AppWidgetManager.getInstance(c).updateAppWidget(id, build(c, id)); } catch (Exception ignored) {}
            return;
        }
        super.onReceive(c, i);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        SharedPreferences.Editor e = WidgetStore.prefs(c).edit();
        for (int id : ids) e.remove("off_" + id).remove("bg_" + id).remove("alpha_" + id);
        e.apply();
    }

    private static String iso(Calendar d) {
        return String.format(Locale.US, "%04d-%02d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
    }

    private static PendingIntent nav(Context c, int id, int delta) {
        Intent i = new Intent(c, CalendarWidget.class);
        i.setAction(NAV);
        i.setData(Uri.parse("chasselog://nav/" + id + "/" + delta));
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        i.putExtra("delta", delta);
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent add(Context c, String day) {
        Intent i = new Intent(c, QuickAddActivity.class);
        i.setData(Uri.parse("chasselog://add?d=" + day));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent open(Context c, String day) {
        Intent i = new Intent(c, MainActivity.class);
        i.setAction(Intent.ACTION_VIEW);
        i.setData(Uri.parse("chasselog://day?d=" + day));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Texte d'une journée : heure en gras, titre, puis le premier invité (comme dans le calendrier de l'app)
    private static CharSequence label(String[] e) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        if (e.length > 2 && !e[2].isEmpty()) {
            b.append(e[2]);
            b.setSpan(new StyleSpan(Typeface.BOLD), 0, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.append("\n");
        }
        String t = e[0].replaceFirst("(?i)^Chasse\\s+(?:du|de la|des|de|aux|au|à la)\\s+|^Chasse\\s+d'\\s*", "");
        int at = b.length();
        b.append(t.isEmpty() ? e[0] : t);
        b.setSpan(new StyleSpan(Typeface.BOLD), at, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (e.length > 3 && !e[3].isEmpty()) b.append("\n").append(e[3]);
        return b;
    }

    static RemoteViews build(Context c, int id) {
        SharedPreferences p = WidgetStore.prefs(c);
        int mode = p.getInt("bg_" + id, 0);
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = mode == 2 || (mode == 0 && night);
        int card = dark ? 0xFF20261B : 0xFFFBFAF5, ink = dark ? 0xFFE8EAD9 : 0xFF26301F, mut = dark ? 0xFF9AA28A : 0xFF6D7461;
        int line = dark ? 0xFF333B2B : 0xFFD9D6C5, red = dark ? 0xFFFF7B72 : 0xFFD32F2F, onAcc = dark ? 0xFF20261B : 0xFFFBFAF5;
        int alpha = Math.max(0, Math.min(255, p.getInt("alpha_" + id, 255)));

        Calendar today = Calendar.getInstance();
        String td = iso(today);
        Calendar first = Calendar.getInstance();
        first.set(Calendar.DAY_OF_MONTH, 1);
        first.add(Calendar.MONTH, p.getInt("off_" + id, 0));
        int month = first.get(Calendar.MONTH);

        // journées par date : celles de l'app, puis celles ajoutées depuis le widget et pas encore reprises par l'app
        Map<String, List<String[]>> days = new HashMap<>();
        try {
            JSONArray ev = WidgetStore.data(c).optJSONArray("ev");
            for (int k = 0; ev != null && k < ev.length(); k++) {
                JSONObject e = ev.getJSONObject(k);
                String d = e.optString("d", "");
                if (d.length() != 10) continue;
                boolean nh = e.optInt("nh", 0) == 1;
                String st = nh ? "nh" : e.optInt("tq", 0) == 1 ? "tq" : e.optInt("f", 0) == 1 ? "ok" : d.compareTo(td) < 0 ? "todo" : "fut";
                List<String[]> l = days.get(d);
                if (l == null) { l = new ArrayList<>(); days.put(d, l); }
                l.add(new String[] {e.optString("t", ""), st, e.optString("h", ""), e.optString("g", "")});
            }
            JSONArray pe = WidgetStore.pending(c);
            for (int k = 0; k < pe.length(); k++) {
                JSONObject e = pe.getJSONObject(k);
                String d = e.optString("d", "");
                if (d.length() != 10) continue;
                String t = e.optString("t", "");
                if (t.isEmpty()) t = e.optString("m", "");
                if (t.isEmpty()) t = "Chasse";
                String h = e.optString("h", "").replace(":", "h");
                boolean tq = "trq".equals(e.optString("r", ""));
                if (tq && !t.toLowerCase(Locale.FRENCH).contains("traque")) t = "Traque " + t.replaceFirst("(?i)^Chasse\\s+", "");
                List<String[]> l = days.get(d);
                if (l == null) { l = new ArrayList<>(); days.put(d, l); }
                l.add(new String[] {t, tq ? "tq" : d.compareTo(td) < 0 ? "todo" : "fut", h, ""});
            }
        } catch (Exception ignored) {}

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_calendar);
        v.setInt(R.id.w_bg, "setColorFilter", card);
        v.setInt(R.id.w_bg, "setImageAlpha", alpha);
        v.setTextViewText(R.id.w_title, MONTHS[month] + " " + first.get(Calendar.YEAR));
        v.setTextColor(R.id.w_title, ink);
        v.setTextColor(R.id.w_prev, mut);
        v.setTextColor(R.id.w_next, mut);
        v.setTextColor(R.id.w_add, ink);
        v.setOnClickPendingIntent(R.id.w_prev, nav(c, id, -1));
        v.setOnClickPendingIntent(R.id.w_next, nav(c, id, 1));
        v.setOnClickPendingIntent(R.id.w_title, nav(c, id, 0));
        v.setOnClickPendingIntent(R.id.w_add, add(c, td));
        for (int k = 0; k < 7; k++) v.setTextColor(WEEKDAYS[k], k == 6 ? red : mut);

        Calendar d = (Calendar) first.clone();
        d.add(Calendar.DAY_OF_MONTH, -((first.get(Calendar.DAY_OF_WEEK) + 5) % 7));
        Calendar last = (Calendar) first.clone();
        last.set(Calendar.DAY_OF_MONTH, first.getActualMaximum(Calendar.DAY_OF_MONTH));
        v.removeAllViews(R.id.w_rows);
        for (int w = 0; w < 6 && !d.after(last); w++) {
            RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.widget_row);
            row.setInt(R.id.r_line, "setColorFilter", line);
            for (int k = 0; k < 7; k++) {
                RemoteViews cell = new RemoteViews(c.getPackageName(), R.layout.widget_cell);
                String ds = iso(d);
                boolean out = d.get(Calendar.MONTH) != month, isToday = ds.equals(td);
                cell.setTextViewText(R.id.c_day, String.valueOf(d.get(Calendar.DAY_OF_MONTH)));
                cell.setTextColor(R.id.c_day, isToday ? onAcc : out ? line : k == 6 ? red : ink);
                cell.setInt(R.id.c_day, "setBackgroundResource", isToday ? (dark ? R.drawable.w_today_d : R.drawable.w_today_l) : 0);
                List<String[]> l = out ? null : days.get(ds);
                int n = l == null ? 0 : l.size();
                // comme dans l'app : deux journées au plus, chacune occupe sa part de la case (heure, titre, invités), puis « +n »
                int[] slots = {R.id.c_e1, R.id.c_e2};
                for (int s = 0; s < 2; s++) {
                    if (s >= n) { cell.setViewVisibility(slots[s], View.GONE); continue; }
                    String[] e = l.get(s);
                    boolean ok = "ok".equals(e[1]), tq = "tq".equals(e[1]), fut = tq || "fut".equals(e[1]);
                    cell.setViewVisibility(slots[s], View.VISIBLE);
                    cell.setTextViewText(slots[s], label(e));
                    cell.setInt(slots[s], "setMaxLines", n > 1 ? 3 : 6);
                    cell.setTextColor(slots[s], ok ? onAcc : fut ? ink : mut);
                    cell.setInt(slots[s], "setBackgroundResource", tq ? R.drawable.w_tq : ok ? (dark ? R.drawable.w_ok_d : R.drawable.w_ok_l) : fut ? (dark ? R.drawable.w_fut_d : R.drawable.w_fut_l) : (dark ? R.drawable.w_todo_d : R.drawable.w_todo_l));
                }
                cell.setViewVisibility(R.id.c_more, n > 2 ? View.VISIBLE : View.GONE);
                if (n > 2) { cell.setTextViewText(R.id.c_more, "+" + (n - 2)); cell.setTextColor(R.id.c_more, mut); }
                if (!out) cell.setOnClickPendingIntent(R.id.c_root, n > 0 ? open(c, ds) : add(c, ds));
                row.addView(R.id.r_cells, cell);
                d.add(Calendar.DAY_OF_MONTH, 1);
            }
            v.addView(R.id.w_rows, row);
        }
        return v;
    }
}
