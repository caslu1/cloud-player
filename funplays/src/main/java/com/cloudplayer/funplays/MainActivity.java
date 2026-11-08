package com.cloudplayer.funplays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** MAC + device key -> add, list, edit and delete playlists on funplays.app. */
public class MainActivity extends Activity {

    private static final int BLUE = Color.parseColor("#1A3FE0");
    private static final int RED = Color.parseColor("#C62828");

    private EditText macField, keyField, nameField, urlField, epgField;
    private TextView output;
    private LinearLayout listBox;
    private Button addButton, listButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    /** Something that talks to the server with a fresh login token. */
    private interface Job { String run(String token) throws Exception; }

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
        title.setText("Funplays - Playlists");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(BLUE);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        root.addView(section("Aparelho"));
        macField = field(root, "MAC (ex: aa:bb:cc:dd:ee:ff)", InputType.TYPE_CLASS_TEXT, "mac");
        keyField = field(root, "Device Key", InputType.TYPE_CLASS_TEXT, "key");

        listButton = new Button(this);
        listButton.setText("VER / GERENCIAR PLAYLISTS");
        listButton.setOnClickListener(v -> refreshList(null));
        root.addView(listButton);

        output = new TextView(this);
        output.setTextSize(15);
        output.setTextColor(Color.DKGRAY);
        output.setPadding(0, dp(8), 0, 0);
        output.setTextIsSelectable(true);
        root.addView(output);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        root.addView(section("Adicionar nova playlist"));
        nameField = field(root, "Nome da playlist", InputType.TYPE_CLASS_TEXT, "name");
        if (nameField.getText().length() == 0) nameField.setText("Playlist");
        urlField = field(root, "Link M3U", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE, null);
        urlField.setMinLines(2);

        Button paste = new Button(this);
        paste.setText("Colar link M3U");
        paste.setOnClickListener(v -> {
            String t = clipboard();
            if (t != null) urlField.setText(t);
        });
        root.addView(paste);

        epgField = field(root, "Link EPG (opcional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, null);

        addButton = new Button(this);
        addButton.setText("ADICIONAR PLAYLIST");
        addButton.setTextColor(Color.WHITE);
        addButton.setBackgroundColor(BLUE);
        addButton.setOnClickListener(v -> addPlaylist());
        LinearLayout.LayoutParams lp = matchWidth();
        lp.topMargin = dp(12);
        root.addView(addButton, lp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    // ---- actions ----

    private void addPlaylist() {
        final String name = nameField.getText().toString().trim().isEmpty()
                ? "Playlist" : nameField.getText().toString().trim();
        final String url = urlField.getText().toString().trim();
        final String epg = epgField.getText().toString().trim();
        if (url.isEmpty()) { show("Cole o link M3U."); return; }
        if (!validUrl(url)) { show("O link M3U precisa começar com http:// ou https://"); return; }
        prefs.edit().putString("name", name).apply();
        final String mac = currentMac();
        withLogin("Enviando...", token -> {
            JSONObject pl = FunplaysApi.addPlaylist(token, mac, url, name, epg);
            return "✅ Playlist \"" + pl.optString("name", name) + "\" adicionada! Reinicie o app na TV.";
        }, true);
    }

    private void refreshList(String message) {
        withLogin("Consultando...", token -> message, false);
    }

    private void editDialog(final JSONObject pl) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText n = field(box, "Nome", InputType.TYPE_CLASS_TEXT, null);
        n.setText(pl.optString("name"));
        final EditText u = field(box, "Link M3U", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE, null);
        u.setText(pl.optString("url"));
        final EditText e = field(box, "Link EPG (opcional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, null);
        String epg = pl.optString("epg_url");
        e.setText("null".equals(epg) ? "" : epg);
        Button paste = new Button(this);
        paste.setText("Colar link M3U");
        paste.setOnClickListener(v -> {
            String t = clipboard();
            if (t != null) u.setText(t);
        });
        box.addView(paste);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("Editar playlist")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (d, w) -> {
                    final String name = n.getText().toString().trim();
                    final String url = u.getText().toString().trim();
                    final String epgUrl = e.getText().toString().trim();
                    if (name.isEmpty() || !validUrl(url)) {
                        show("Nome vazio ou link inválido. Nada foi alterado.");
                        return;
                    }
                    final long id = pl.optLong("id");
                    withLogin("Salvando...", token -> {
                        FunplaysApi.editPlaylist(token, id, name, url, epgUrl);
                        return "✅ Playlist \"" + name + "\" atualizada.";
                    }, true);
                })
                .show();
    }

    private void deleteDialog(final JSONObject pl) {
        final String name = pl.optString("name");
        new AlertDialog.Builder(this)
                .setTitle("Apagar playlist")
                .setMessage("Apagar \"" + name + "\" deste aparelho?")
                .setNegativeButton("Não", null)
                .setPositiveButton("Apagar", (d, w) -> {
                    final long id = pl.optLong("id");
                    withLogin("Apagando...", token -> {
                        FunplaysApi.deletePlaylist(token, id);
                        return "🗑️ Playlist \"" + name + "\" apagada.";
                    }, true);
                })
                .show();
    }

    /**
     * Logs in, runs the job, then reloads the playlist list.
     * @param clearUrl clear the "new playlist" link field after success (only relevant after adding)
     */
    private void withLogin(String busyText, Job job, boolean clearUrl) {
        final String mac = currentMac();
        final String key = keyField.getText().toString().trim();
        if (mac.isEmpty() || key.isEmpty()) { show("Preencha o MAC e a Device Key."); return; }
        prefs.edit().putString("mac", mac).putString("key", key).apply();
        macField.setText(mac);
        setBusy(true);
        show(busyText);

        worker.execute(() -> {
            String msg;
            JSONArray list = null;
            boolean ok = false;
            try {
                JSONObject info = FunplaysApi.validateMac(mac);
                String token = FunplaysApi.login(mac, key);
                String result = job.run(token);
                list = FunplaysApi.listPlaylists(token);
                msg = "Aparelho: " + info.optString("model") + " (id " + info.optInt("id") + ")"
                        + (result != null ? "\n" + result : "");
                ok = true;
            } catch (Exception e) {
                msg = "❌ Erro: " + e.getMessage();
            }
            final String text = msg;
            final JSONArray playlists = list;
            final boolean success = ok;
            ui.post(() -> {
                setBusy(false);
                show(text);
                if (playlists != null) renderList(playlists);
                if (success && clearUrl && text.contains("adicionada")) urlField.setText("");
            });
        });
    }

    // ---- list ----

    private void renderList(JSONArray list) {
        listBox.removeAllViews();
        TextView head = new TextView(this);
        head.setText("Playlists no aparelho (" + list.length() + ")");
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setTextColor(Color.BLACK);
        head.setPadding(0, dp(8), 0, dp(4));
        listBox.addView(head);
        if (list.length() == 0) {
            TextView none = new TextView(this);
            none.setText("Nenhuma playlist.");
            none.setTextColor(Color.GRAY);
            listBox.addView(none);
            return;
        }
        for (int i = 0; i < list.length(); i++) {
            final JSONObject pl = list.optJSONObject(i);
            if (pl == null) continue;
            listBox.addView(row(pl));
        }
    }

    private LinearLayout row(final JSONObject pl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F1F4FF"));
        bg.setCornerRadius(dp(8));
        card.setBackground(bg);
        LinearLayout.LayoutParams clp = matchWidth();
        clp.bottomMargin = dp(8);
        card.setLayoutParams(clp);

        TextView name = new TextView(this);
        name.setText(pl.optString("name"));
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(Color.BLACK);
        card.addView(name);

        TextView url = new TextView(this);
        url.setText(pl.optString("url"));
        url.setTextSize(12);
        url.setTextColor(Color.GRAY);
        url.setSingleLine(true);
        url.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        card.addView(url);

        boolean locked = pl.optBoolean("is_protected", false);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        if (locked) {
            TextView t = new TextView(this);
            t.setText("🔒 Protegida por PIN: gerencie pelo site");
            t.setTextColor(Color.GRAY);
            buttons.addView(t);
        } else {
            Button edit = new Button(this);
            edit.setText("Editar");
            edit.setOnClickListener(v -> editDialog(pl));
            buttons.addView(edit);
            Button del = new Button(this);
            del.setText("Apagar");
            del.setTextColor(RED);
            del.setOnClickListener(v -> deleteDialog(pl));
            buttons.addView(del);
        }
        card.addView(buttons);
        return card;
    }

    // ---- helpers ----

    private TextView section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(BLUE);
        t.setPadding(0, dp(16), 0, 0);
        return t;
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
        if (prefKey != null) e.setText(prefs.getString(prefKey, ""));
        parent.addView(e);
        return e;
    }

    private String currentMac() { return FunplaysApi.normalizeMac(macField.getText().toString()); }

    private static boolean validUrl(String u) { return u.startsWith("http://") || u.startsWith("https://"); }

    private String clipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm != null ? cm.getPrimaryClip() : null;
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence t = clip.getItemAt(0).coerceToText(this);
        return t == null ? null : t.toString().trim();
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void setBusy(boolean busy) {
        addButton.setEnabled(!busy);
        listButton.setEnabled(!busy);
        for (int i = 0; i < listBox.getChildCount(); i++) listBox.getChildAt(i).setEnabled(!busy);
    }

    private void show(String s) { output.setText(s); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
