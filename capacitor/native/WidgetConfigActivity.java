package __PKG__;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

// Réglages d'un widget (appui long sur le widget) : arrière-plan clair, sombre ou selon le téléphone, et transparence.
public class WidgetConfigActivity extends AppCompatActivity {
    private int id = AppWidgetManager.INVALID_APPWIDGET_ID, bg = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        Intent in = getIntent();
        if (in != null) id = in.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        setContentView(R.layout.widget_config);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final SharedPreferences p = WidgetStore.prefs(this);
        bg = state != null ? state.getInt("bg", 0) : p.getInt("bg_" + id, 0);
        WidgetUi.choices(this, (LinearLayout) findViewById(R.id.k_bg), new String[] {"Téléphone", "Clair", "Sombre"}, bg, true, new WidgetUi.Pick() {
            @Override public void on(int index) { bg = index; }
        });
        final SeekBar alpha = findViewById(R.id.k_alpha);
        alpha.setProgress(100 - Math.round(p.getInt("alpha_" + id, 255) * 100f / 255f));

        findViewById(R.id.k_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.k_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                p.edit().putInt("bg_" + id, bg).putInt("alpha_" + id, Math.round((100 - alpha.getProgress()) * 255f / 100f)).apply();
                try { AppWidgetManager.getInstance(WidgetConfigActivity.this).updateAppWidget(id, CalendarWidget.build(WidgetConfigActivity.this, id)); } catch (Exception ignored) {}
                setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("bg", bg);
    }
}
