package __PKG__;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.OutputStream;

// Range une photo prise avec l'appareil photo dans l'album « ChasseLog » de la galerie (Android 10 et plus).
@CapacitorPlugin(name = "ChasseLogMedia")
public class ChasseLogMediaPlugin extends Plugin {
    @PluginMethod
    public void savePhoto(PluginCall call) {
        String data = call.getString("data");
        String name = call.getString("name", "ChasseLog.jpg");
        String mime = call.getString("mime", "image/jpeg");
        if (data == null || data.isEmpty()) { call.reject("Aucune image"); return; }
        if (Build.VERSION.SDK_INT < 29) { call.reject("Android 10 ou plus requis"); return; }
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            ContentResolver cr = getContext().getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            v.put(MediaStore.Images.Media.MIME_TYPE, mime);
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChasseLog");
            v.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) { call.reject("Impossible de créer la photo"); return; }
            try (OutputStream out = cr.openOutputStream(uri)) {
                if (out == null) throw new Exception("flux indisponible");
                out.write(bytes);
            }
            v.clear();
            v.put(MediaStore.Images.Media.IS_PENDING, 0);
            cr.update(uri, v, null, null);
            JSObject ret = new JSObject();
            ret.put("uri", uri.toString());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Photo non enregistrée : " + e.getMessage());
        }
    }
}
