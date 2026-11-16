package __PKG__;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// Lien entre l'app et le widget calendrier : l'app envoie une copie de ses journées, et reprend celles ajoutées depuis le widget.
@CapacitorPlugin(name = "ChasseLogWidget")
public class ChasseLogWidgetPlugin extends Plugin {
    @PluginMethod
    public void sync(PluginCall call) {
        String data = call.getString("data");
        if (data == null) { call.reject("Aucune donnée"); return; }
        WidgetStore.prefs(getContext()).edit().putString("data", data).apply();
        WidgetStore.refresh(getContext());
        call.resolve();
    }

    @PluginMethod
    public void pending(PluginCall call) {
        JSObject r = new JSObject();
        r.put("data", WidgetStore.pending(getContext()).toString());
        call.resolve(r);
    }

    @PluginMethod
    public void clear(PluginCall call) {
        WidgetStore.prefs(getContext()).edit().remove("pending").apply();
        WidgetStore.refresh(getContext());
        call.resolve();
    }
}
