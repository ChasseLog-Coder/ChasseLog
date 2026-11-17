package __PKG__;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
}
