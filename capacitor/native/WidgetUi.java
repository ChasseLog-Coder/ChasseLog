package __PKG__;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

// Petits choix « un parmi plusieurs » partagés par l'ajout rapide et les réglages du widget.
final class WidgetUi {
    interface Pick { void on(int index); }
    private WidgetUi() {}

    static void choices(final Context c, final LinearLayout box, final String[] labels, int selected, final boolean stretch, final Pick pick) {
        box.removeAllViews();
        final float dp = c.getResources().getDisplayMetrics().density;
        for (int k = 0; k < labels.length; k++) {
            final int index = k;
            TextView t = new TextView(c);
            t.setText(labels[k]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(stretch ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, stretch ? 1f : 0f);
            if (k > 0) lp.leftMargin = (int) (6 * dp);
            t.setLayoutParams(lp);
            t.setGravity(android.view.Gravity.CENTER);
            t.setPadding((int) (12 * dp), (int) (10 * dp), (int) (12 * dp), (int) (10 * dp));
            t.setTextSize(15);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { mark(c, box, index); pick.on(index); }
            });
            box.addView(t);
        }
        mark(c, box, selected);
    }

    static void mark(Context c, LinearLayout box, int selected) {
        for (int k = 0; k < box.getChildCount(); k++) {
            TextView t = (TextView) box.getChildAt(k);
            boolean on = k == selected;
            int l = t.getPaddingLeft(), tp = t.getPaddingTop(), r = t.getPaddingRight(), b = t.getPaddingBottom();
            t.setBackgroundResource(on ? R.drawable.cl_chip_on : R.drawable.cl_chip);
            t.setPadding(l, tp, r, b);
            t.setTextColor(c.getResources().getColor(on ? R.color.cl_on_acc : R.color.cl_ink));
        }
    }
}
