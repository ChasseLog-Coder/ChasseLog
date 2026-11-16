package __PKG__;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONObject;

// Ajout rapide d'une journée depuis le widget, sans ouvrir l'app.
// La journée est gardée en attente ; l'app la reprend dans ses données à sa prochaine ouverture.
public class QuickAddActivity extends AppCompatActivity {
    private static final String[] MODE_LABELS = {"Battue", "Approche", "Affût", "Traque-affût", "Autre"};
    private static final String[] MODE_VALUES = {"Battue", "Approche", "Affût", "Traque-affût", ""};
    private static final String[] ROLE_VALUES = {"part", "guest", "trq"};
    private final Calendar date = Calendar.getInstance();
    private int mode = 0, role = 0, hour = -1, minute = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.quick_add);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Uri u = getIntent() == null ? null : getIntent().getData();
        String d = u == null ? null : u.getQueryParameter("d");
        if (d != null && d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date.set(Integer.parseInt(d.substring(0, 4)), Integer.parseInt(d.substring(5, 7)) - 1, Integer.parseInt(d.substring(8, 10)));
        }
        if (state != null) {
            date.setTimeInMillis(state.getLong("date", date.getTimeInMillis()));
            mode = state.getInt("mode", 0); role = state.getInt("role", 0); hour = state.getInt("hour", -1); minute = state.getInt("minute", 0);
        }

        final EditText host = findViewById(R.id.q_host);
        host.setVisibility(role == 1 ? View.VISIBLE : View.GONE);
        WidgetUi.choices(this, (LinearLayout) findViewById(R.id.q_modes), MODE_LABELS, mode, false, new WidgetUi.Pick() {
            @Override public void on(int index) { mode = index; }
        });
        boolean fr = "fr".equals(WidgetStore.data(this).optString("reg", "be"));
        WidgetUi.choices(this, (LinearLayout) findViewById(R.id.q_roles), new String[] {fr ? "Sociétaire" : "Actionnaire", "Invité", "Traqueur"}, role, true, new WidgetUi.Pick() {
            @Override public void on(int index) { role = index; host.setVisibility(index == 1 ? View.VISIBLE : View.GONE); }
        });

        final TextView dateView = findViewById(R.id.q_date), timeView = findViewById(R.id.q_time);
        showDate(dateView);
        showTime(timeView);
        dateView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new DatePickerDialog(QuickAddActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker p, int y, int m, int day) { date.set(y, m, day); showDate(dateView); }
                }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        timeView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new TimePickerDialog(QuickAddActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker p, int h, int m) { hour = h; minute = m; showTime(timeView); }
                }, hour < 0 ? 8 : hour, minute, true).show();
            }
        });
        findViewById(R.id.q_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.q_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("date", date.getTimeInMillis());
        out.putInt("mode", mode); out.putInt("role", role); out.putInt("hour", hour); out.putInt("minute", minute);
    }

    private void showDate(TextView v) {
        v.setText(new SimpleDateFormat("EEE d MMM", Locale.FRENCH).format(date.getTime()));
    }

    private void showTime(TextView v) {
        v.setText(hour < 0 ? "" : String.format(Locale.US, "%02d:%02d", hour, minute));
    }

    private String text(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }

    private void save() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", "w" + System.currentTimeMillis());
            o.put("d", String.format(Locale.US, "%04d-%02d-%02d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)));
            o.put("t", text(R.id.q_title));
            o.put("m", MODE_VALUES[mode]);
            o.put("r", ROLE_VALUES[role]);
            o.put("host", role == 1 ? text(R.id.q_host) : "");
            if (hour >= 0) o.put("h", String.format(Locale.US, "%02d:%02d", hour, minute));
            o.put("pl", text(R.id.q_place));
            o.put("n", text(R.id.q_notes));
            WidgetStore.addPending(this, o);
            WidgetStore.refresh(this);
        } catch (Exception ignored) {}
        finish();
    }
}
