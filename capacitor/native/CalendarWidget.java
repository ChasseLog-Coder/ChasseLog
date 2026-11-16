package __PKG__;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
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

// Widget « calendrier du mois » : journées de chasse du mois, lever et coucher du soleil, ajout rapide.
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

    private static String hm(int minutes) {
        return String.format(Locale.US, "%02dh%02d", minutes / 60, minutes % 60);
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
        String placeName = null;
        double lat = 0, lng = 0;
        String best = null;
        try {
            JSONArray ev = WidgetStore.data(c).optJSONArray("ev");
            for (int k = 0; ev != null && k < ev.length(); k++) {
                JSONObject e = ev.getJSONObject(k);
                String d = e.optString("d", "");
                if (d.length() != 10) continue;
                boolean nh = e.optInt("nh", 0) == 1;
                String st = nh ? "nh" : e.optInt("f", 0) == 1 ? "ok" : d.compareTo(td) < 0 ? "todo" : "fut";
                List<String[]> l = days.get(d);
                if (l == null) { l = new ArrayList<>(); days.put(d, l); }
                l.add(new String[] {e.optString("t", ""), st});
                if (!nh && !e.isNull("la") && d.compareTo(td) >= 0 && (best == null || d.compareTo(best) < 0)) {
                    best = d; lat = e.optDouble("la"); lng = e.optDouble("lo"); placeName = e.optString("pn", "");
                }
            }
            JSONArray pe = WidgetStore.pending(c);
            for (int k = 0; k < pe.length(); k++) {
                JSONObject e = pe.getJSONObject(k);
                String d = e.optString("d", "");
                if (d.length() != 10) continue;
                String t = e.optString("t", "");
                if (t.isEmpty()) t = e.optString("m", "");
                if (t.isEmpty()) t = "Chasse";
                List<String[]> l = days.get(d);
                if (l == null) { l = new ArrayList<>(); days.put(d, l); }
                l.add(new String[] {t, d.compareTo(td) < 0 ? "todo" : "fut"});
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

        int[] sun = best == null ? null : WidgetStore.sun(lat, lng, today);
        if (sun == null) {
            v.setViewVisibility(R.id.w_sun_row, View.GONE);
        } else {
            v.setViewVisibility(R.id.w_sun_row, View.VISIBLE);
            v.setTextViewText(R.id.w_sun, "Lever " + hm(sun[0]) + " · Coucher " + hm(sun[1]));
            v.setTextViewText(R.id.w_place, placeName == null ? "" : placeName);
            v.setTextColor(R.id.w_sun, mut);
            v.setTextColor(R.id.w_place, mut);
        }

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
                int[] slots = {R.id.c_e1, R.id.c_e2};
                for (int s = 0; s < 2; s++) {
                    if (s >= n) { cell.setViewVisibility(slots[s], View.GONE); continue; }
                    cell.setViewVisibility(slots[s], View.VISIBLE);
                    if (s == 1 && n > 2) {
                        cell.setTextViewText(slots[s], "+" + (n - 1));
                        cell.setTextColor(slots[s], mut);
                        cell.setInt(slots[s], "setBackgroundResource", 0);
                        continue;
                    }
                    String[] e = l.get(s);
                    boolean ok = "ok".equals(e[1]), fut = "fut".equals(e[1]);
                    cell.setTextViewText(slots[s], e[0]);
                    cell.setTextColor(slots[s], ok ? onAcc : fut ? ink : mut);
                    cell.setInt(slots[s], "setBackgroundResource", ok ? (dark ? R.drawable.w_ok_d : R.drawable.w_ok_l) : fut ? (dark ? R.drawable.w_fut_d : R.drawable.w_fut_l) : (dark ? R.drawable.w_todo_d : R.drawable.w_todo_l));
                }
                if (!out) cell.setOnClickPendingIntent(R.id.c_root, n > 0 ? open(c, ds) : add(c, ds));
                row.addView(R.id.r_cells, cell);
                d.add(Calendar.DAY_OF_MONTH, 1);
            }
            v.addView(R.id.w_rows, row);
        }
        return v;
    }
}
