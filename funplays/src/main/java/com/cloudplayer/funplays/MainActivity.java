package com.cloudplayer.funplays;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One screen: MAC + device key + M3U link -> playlist added on funplays.app. */
public class MainActivity extends Activity {

    private EditText macField, keyField, nameField, urlField, epgField;
    private TextView output;
    private Button addButton, listButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("funplays", MODE_PRIVATE);

        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Adicionar playlist no Funplays");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A3FE0"));
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);

        macField = field(root, "MAC do aparelho (ex: aa:bb:cc:dd:ee:ff)", InputType.TYPE_CLASS_TEXT, "mac");
        keyField = field(root, "Device Key", InputType.TYPE_CLASS_TEXT, "key");
        nameField = field(root, "Nome da playlist", InputType.TYPE_CLASS_TEXT, "name");
        if (nameField.getText().length() == 0) nameField.setText("Playlist");
        urlField = field(root, "Link M3U", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE, null);
        urlField.setMinLines(2);

        Button paste = new Button(this);
        paste.setText("Colar link M3U");
        paste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = cm != null ? cm.getPrimaryClip() : null;
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence t = clip.getItemAt(0).coerceToText(this);
                if (t != null) urlField.setText(t.toString().trim());
            }
        });
        root.addView(paste);

        epgField = field(root, "Link EPG (opcional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, null);

        addButton = new Button(this);
        addButton.setText("ADICIONAR PLAYLIST");
        addButton.setTextColor(Color.WHITE);
        addButton.setBackgroundColor(Color.parseColor("#1A3FE0"));
        addButton.setOnClickListener(v -> run(true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        root.addView(addButton, lp);

        listButton = new Button(this);
        listButton.setText("Ver playlists do aparelho");
        listButton.setOnClickListener(v -> run(false));
        root.addView(listButton);

        output = new TextView(this);
        output.setTextSize(15);
        output.setTextColor(Color.DKGRAY);
        output.setPadding(0, dp(12), 0, 0);
        output.setTextIsSelectable(true);
        root.addView(output);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private EditText field(LinearLayout parent, String hint, int type, String prefKey) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextColor(Color.GRAY);
        label.setPadding(0, dp(8), 0, 0);
        parent.addView(label);
        EditText e = new EditText(this);
        e.setInputType(type);
        e.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        e.setTextColor(Color.BLACK);
        e.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        if (prefKey != null) {
            e.setText(prefs.getString(prefKey, ""));
            e.setTag(prefKey);
        }
        parent.addView(e);
        return e;
    }

    private void run(boolean add) {
        final String mac = FunplaysApi.normalizeMac(macField.getText().toString());
        final String key = keyField.getText().toString().trim();
        final String name = nameField.getText().toString().trim();
        final String url = urlField.getText().toString().trim();
        final String epg = epgField.getText().toString().trim();

        if (mac.isEmpty() || key.isEmpty()) { show("Preencha o MAC e a Device Key."); return; }
        if (add && url.isEmpty()) { show("Cole o link M3U."); return; }
        if (add && !(url.startsWith("http://") || url.startsWith("https://"))) {
            show("O link M3U precisa começar com http:// ou https://"); return;
        }

        prefs.edit().putString("mac", mac).putString("key", key)
                .putString("name", name.isEmpty() ? "Playlist" : name).apply();
        macField.setText(mac);
        setBusy(true);
        show(add ? "Enviando..." : "Consultando...");

        worker.execute(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                JSONObject info = FunplaysApi.validateMac(mac);
                sb.append("Aparelho: ").append(info.optString("model")).append(" (id ")
                        .append(info.optInt("id")).append(")\n");
                String token = FunplaysApi.login(mac, key);
                if (add) {
                    JSONObject pl = FunplaysApi.addPlaylist(token, mac, url,
                            name.isEmpty() ? "Playlist" : name, epg);
                    sb.append("\n✅ Playlist \"").append(pl.optString("name"))
                            .append("\" adicionada! Reinicie o app na TV.\n");
                }
                JSONArray list = FunplaysApi.listPlaylists(token);
                sb.append("\nPlaylists no aparelho (").append(list.length()).append("):\n");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject p = list.getJSONObject(i);
                    sb.append(" • ").append(p.optString("name")).append("\n");
                }
            } catch (Exception e) {
                sb.append("\n❌ Erro: ").append(e.getMessage());
            }
            final String text = sb.toString();
            ui.post(() -> {
                setBusy(false);
                show(text);
                if (add && text.contains("✅")) urlField.setText("");
            });
        });
    }

    private void setBusy(boolean busy) {
        addButton.setEnabled(!busy);
        listButton.setEnabled(!busy);
    }

    private void show(String s) { output.setText(s); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
