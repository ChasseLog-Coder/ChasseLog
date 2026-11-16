package __PKG__;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

// Données du widget calendrier, gardées sur le téléphone uniquement (préférences de l'app) :
// copie des journées envoyée par l'app, journées ajoutées depuis le widget en attente, réglages par widget.
final class WidgetStore {
    static final String PREFS = "chasselog_widget";
    private WidgetStore() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static JSONObject data(Context c) {
        try { return new JSONObject(prefs(c).getString("data", "{}")); } catch (Exception e) { return new JSONObject(); }
    }

    static JSONArray pending(Context c) {
        try { return new JSONArray(prefs(c).getString("pending", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static void addPending(Context c, JSONObject day) {
        JSONArray a = pending(c);
        a.put(day);
        prefs(c).edit().putString("pending", a.toString()).apply();
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, CalendarWidget.class));
        if (ids == null || ids.length == 0) return;
        Intent i = new Intent(c, CalendarWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(i);
    }

    // Lever et coucher du soleil (heure locale, en minutes depuis minuit) ; null si le soleil ne se lève ou ne se couche pas.
    static int[] sun(double lat, double lng, Calendar day) {
        int n = day.get(Calendar.DAY_OF_YEAR);
        double tz = TimeZone.getDefault().getOffset(day.getTimeInMillis()) / 3600000.0;
        int[] out = new int[2];
        for (int k = 0; k < 2; k++) {
            double lngHour = lng / 15.0;
            double t = n + ((k == 0 ? 6 : 18) - lngHour) / 24.0;
            double m = 0.9856 * t - 3.289;
            double l = m + 1.916 * Math.sin(Math.toRadians(m)) + 0.020 * Math.sin(Math.toRadians(2 * m)) + 282.634;
            l = ((l % 360) + 360) % 360;
            double ra = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(l))));
            ra = ((ra % 360) + 360) % 360;
            ra += Math.floor(l / 90) * 90 - Math.floor(ra / 90) * 90;
            ra /= 15.0;
            double sinDec = 0.39782 * Math.sin(Math.toRadians(l));
            double cosDec = Math.cos(Math.asin(sinDec));
            double cosH = (Math.cos(Math.toRadians(90.833)) - sinDec * Math.sin(Math.toRadians(lat))) / (cosDec * Math.cos(Math.toRadians(lat)));
            if (cosH > 1 || cosH < -1) return null;
            double h = (k == 0 ? 360 - Math.toDegrees(Math.acos(cosH)) : Math.toDegrees(Math.acos(cosH))) / 15.0;
            double local = h + ra - 0.06571 * t - 6.622 - lngHour + tz;
            local = ((local % 24) + 24) % 24;
            out[k] = (int) Math.round(local * 60) % 1440;
        }
        return out;
    }
}
