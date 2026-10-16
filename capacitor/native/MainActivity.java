package __PKG__;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.getcapacitor.BridgeActivity;
import java.util.Locale;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ChasseLogMediaPlugin.class);
        super.onCreate(savedInstanceState);
        // après super.onCreate : le thème de l'app est alors en place (sinon Android affiche une barre de titre)
        EdgeToEdge.enable(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) { pushInsets(0); pushInsets(1500); pushInsets(4000); }
    }

    @Override
    public void onResume() {
        super.onResume();
        pushInsets(800);
    }

    // Transmet à la page la hauteur réelle des barres d'Android (en dp), au cas où la WebView ne la donne pas.
    private void pushInsets(long delay) {
        final View decor = getWindow().getDecorView();
        decor.postDelayed(() -> {
            try {
                if (bridge == null) return;
                WebView wv = bridge.getWebView();
                WindowInsetsCompat w = ViewCompat.getRootWindowInsets(decor);
                if (wv == null || w == null) return;
                Insets i = w.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                int[] loc = new int[2];
                wv.getLocationOnScreen(loc);
                int top = Math.max(0, i.top - loc[1]);
                int below = Math.max(0, decor.getHeight() - (loc[1] + wv.getHeight()));
                int bottom = Math.max(0, i.bottom - below);
                float d = getResources().getDisplayMetrics().density;
                String js = String.format(Locale.US,
                    "window.__natInsets={t:%.1f,b:%.1f,l:%.1f,r:%.1f};window.__applyInsets&&window.__applyInsets();",
                    top / d, bottom / d, i.left / d, i.right / d);
                wv.evaluateJavascript(js, null);
            } catch (Exception ignored) {}
        }, delay);
    }
}
