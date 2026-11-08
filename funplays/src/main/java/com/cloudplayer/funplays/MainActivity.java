package com.cloudplayer.funplays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** MAC + device key -> add, list, edit and delete playlists on funplays.app. */
public class MainActivity extends Activity {

    // Palette
    private static final int PRIMARY = 0xFF2F54EB;
    private static final int PRIMARY_DARK = 0xFF1A2FA8;
    private static final int PRIMARY_SOFT = 0xFFE8EDFF;
    private static final int BG = 0xFFF4F6FB;
    private static final int SURFACE = 0xFFFFFFFF;
    private static final int TEXT = 0xFF141A2E;
    private static final int TEXT_2 = 0xFF5B6478;
    private static final int BORDER = 0xFFD9DEEA;
    private static final int SUCCESS = 0xFF1E8E5A;
    private static final int SUCCESS_SOFT = 0xFFE3F6EC;
    private static final int DANGER = 0xFFD32F2F;
    private static final int DANGER_SOFT = 0xFFFDECEC;

    private EditText macField, keyField;
    private TextView banner, deviceStatus, listTitle, headerDevice;
    private LinearLayout listBox;
    private ProgressBar progress;
    private TextView connectButton, addButton;
    private boolean busy;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    /** Something that talks to the server with a fresh login token; returns a success message or null. */
    private interface Job { String run(String token) throws Exception; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("funplays", MODE_PRIVATE);
        installCrashReporter();
        try {
            setupWindow();
        } catch (Throwable t) {
            // Cosmetic only (status bar colours); never let it stop the app from opening.
        }

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);

        // ---- Header ----
        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable hbg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{PRIMARY, PRIMARY_DARK});
        header.setBackground(hbg);
        TextView title = text("Funplays Playlists", 24, Color.WHITE, true);
        header.addView(title);
        headerDevice = text("Adicione e gerencie as listas da sua TV", 14, 0xD9FFFFFF, false);
        headerDevice.setPadding(0, dp(4), 0, 0);
        header.addView(headerDevice);
        screen.addView(header, matchWrap());

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(PRIMARY));
        progress.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        screen.addView(progress, plp);

        // ---- Scrollable content ----
        final ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(24));
        scroll.addView(content);
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        banner = text("", 14, TEXT, false);
        banner.setPadding(dp(14), dp(12), dp(14), dp(12));
        banner.setVisibility(View.GONE);
        banner.setOnClickListener(v -> banner.setVisibility(View.GONE));
        LinearLayout.LayoutParams blp = matchWrap();
        blp.bottomMargin = dp(12);
        content.addView(banner, blp);

        // Device card
        LinearLayout device = card(content);
        device.addView(cardTitle("📺  Aparelho", null));
        deviceStatus = text("Use os dados que aparecem na tela do app na TV.", 13, TEXT_2, false);
        deviceStatus.setPadding(0, dp(2), 0, dp(4));
        device.addView(deviceStatus);
        macField = input(device, "MAC", "aa:bb:cc:dd:ee:ff",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        macField.setText(prefs.getString("mac", ""));
        keyField = input(device, "Device Key", "ex: 123456",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        keyField.setText(prefs.getString("key", ""));
        connectButton = primaryButton("Conectar");
        connectButton.setOnClickListener(v -> { hideKeyboard(); run("Conectando...", t -> null); });
        LinearLayout.LayoutParams clp = matchWrap();
        clp.topMargin = dp(16);
        device.addView(connectButton, clp);

        // Playlists card
        LinearLayout lists = card(content);
        addButton = tonalButton("＋  Nova");
        addButton.setOnClickListener(v -> playlistDialog(null));
        listTitle = cardTitleText("🎬  Playlists");
        lists.addView(cardTitle(null, addButton));
        ((LinearLayout) lists.getChildAt(0)).addView(listTitle, 0,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        lists.addView(listBox, matchWrap());
        showEmpty("Conecte um aparelho para ver as playlists.");

        TextView foot = text("As alterações aparecem na TV depois de reabrir o app.", 12, TEXT_2, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(4), 0, 0);
        content.addView(foot, matchWrap());

        // Edge-to-edge: header under the status bar, content above navigation bar and keyboard.
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                top = bars.top;
                bottom = Math.max(bars.bottom, ime.bottom);
                v.setPadding(bars.left, 0, bars.right, 0);
            } else {
                // Before Android 11 the window is not edge-to-edge; adjustResize handles the keyboard.
                top = 0;
                bottom = 0;
            }
            header.setPadding(dp(20), top + dp(20), dp(20), dp(24));
            scroll.setPadding(0, 0, 0, bottom);
            return insets;
        });

        setContentView(screen);
        showLastCrash();

        // Saved device: load its playlists straight away.
        if (!macField.getText().toString().trim().isEmpty() && !keyField.getText().toString().trim().isEmpty()) {
            run("Carregando playlists...", t -> null);
        }
    }

    // ---------------------------------------------------------------- actions

    /** Add (pl == null) or edit a playlist in a dialog. */
    private void playlistDialog(final JSONObject pl) {
        if (!hasDevice()) { showBanner("Preencha o MAC e a Device Key primeiro.", false); return; }
        final boolean editing = pl != null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(8));

        final EditText name = input(box, "Nome", "ex: Minha lista", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText url = input(box, "Link M3U", "http://...", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        url.setMinLines(2);
        url.setMaxLines(4);
        TextView paste = textButton("📋  Colar link", PRIMARY);
        paste.setOnClickListener(v -> { String t = clipboard(); if (t != null) url.setText(t); });
        box.addView(paste, wrapRight());
        final EditText epg = input(box, "Link EPG (opcional)", "http://...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        if (editing) {
            name.setText(pl.optString("name"));
            url.setText(pl.optString("url"));
            String e = pl.optString("epg_url");
            epg.setText("null".equals(e) ? "" : e);
        } else {
            name.setText(prefs.getString("name", "Playlist"));
            String clip = clipboard();
            if (clip != null && validUrl(clip)) url.setText(clip);
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Editar playlist" : "Nova playlist")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(editing ? "Salvar" : "Adicionar", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PRIMARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
            // Validate before closing, so typed data is not lost.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final String n = name.getText().toString().trim().isEmpty() ? "Playlist" : name.getText().toString().trim();
                final String u = url.getText().toString().trim();
                final String e = epg.getText().toString().trim();
                if (!validUrl(u)) { url.setError("O link precisa começar com http:// ou https://"); return; }
                if (!e.isEmpty() && !validUrl(e)) { epg.setError("Link EPG inválido"); return; }
                dialog.dismiss();
                if (editing) {
                    final long id = pl.optLong("id");
                    run("Salvando...", t -> {
                        FunplaysApi.editPlaylist(t, id, n, u, e);
                        return "Playlist \"" + n + "\" atualizada.";
                    });
                } else {
                    prefs.edit().putString("name", n).apply();
                    final String mac = currentMac();
                    run("Adicionando...", t -> {
                        FunplaysApi.addPlaylist(t, mac, u, n, e);
                        return "Playlist \"" + n + "\" adicionada! Reabra o app na TV.";
                    });
                }
            });
        });
        dialog.show();
    }

    private void deleteDialog(final JSONObject pl) {
        final String name = pl.optString("name");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Apagar playlist?")
                .setMessage("\"" + name + "\" será removida deste aparelho.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (d, w) -> {
                    final long id = pl.optLong("id");
                    run("Apagando...", t -> {
                        FunplaysApi.deletePlaylist(t, id);
                        return "Playlist \"" + name + "\" apagada.";
                    });
                })
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(DANGER);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
        });
        dialog.show();
    }

    /** Validates the MAC, logs in, runs the job and reloads the playlist list. */
    private void run(String busyText, Job job) {
        if (busy) return;
        if (!hasDevice()) { showBanner("Preencha o MAC e a Device Key.", false); return; }
        final String mac = currentMac();
        final String key = keyField.getText().toString().trim();
        prefs.edit().putString("mac", mac).putString("key", key).apply();
        macField.setText(mac);
        setBusy(true, busyText);

        worker.execute(() -> {
            String message = null, error = null, model = null;
            JSONArray list = null;
            try {
                JSONObject info = FunplaysApi.validateMac(mac);
                model = info.optString("model", "aparelho");
                String token = FunplaysApi.login(mac, key);
                message = job.run(token);
                list = FunplaysApi.listPlaylists(token);
            } catch (Exception e) {
                error = friendlyError(e.getMessage());
            }
            final String msg = message, err = error, mdl = model;
            final JSONArray playlists = list;
            ui.post(() -> {
                if (isFinishing()) return;
                setBusy(false, null);
                if (err != null) {
                    showBanner(err, false);
                    if (mdl == null) {
                        deviceStatus.setText("Não conectado");
                        deviceStatus.setTextColor(DANGER);
                    }
                    return;
                }
                deviceStatus.setText("✓  Conectado · " + mdl);
                deviceStatus.setTextColor(SUCCESS);
                headerDevice.setText(mdl + " · " + mac);
                if (msg != null) showBanner(msg, true); else banner.setVisibility(View.GONE);
                if (playlists != null) renderList(playlists);
            });
        });
    }

    // ---------------------------------------------------------------- list

    private void renderList(JSONArray list) {
        listBox.removeAllViews();
        listTitle.setText("🎬  Playlists (" + list.length() + ")");
        if (list.length() == 0) { showEmpty("Nenhuma playlist ainda. Toque em ＋ Nova."); return; }
        for (int i = 0; i < list.length(); i++) {
            JSONObject pl = list.optJSONObject(i);
            if (pl != null) listBox.addView(row(pl, i > 0));
        }
    }

    private void showEmpty(String msg) {
        listBox.removeAllViews();
        TextView empty = text(msg, 14, TEXT_2, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(8), dp(24), dp(8), dp(24));
        listBox.addView(empty, matchWrap());
    }

    private View row(final JSONObject pl, boolean divider) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        if (divider) {
            View line = new View(this);
            line.setBackgroundColor(0xFFEDF0F6);
            wrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        String name = pl.optString("name", "Playlist");
        TextView avatar = text(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), 18, PRIMARY, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(rounded(PRIMARY_SOFT, dp(22), 0));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);
        TextView n = text(name, 16, TEXT, true);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(n);
        TextView u = text(shortHost(pl.optString("url")), 13, TEXT_2, false);
        u.setSingleLine(true);
        u.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(u);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (pl.optBoolean("is_protected", false)) {
            row.addView(text("🔒 PIN", 13, TEXT_2, false));
        } else {
            TextView edit = iconButton("✏️", PRIMARY_SOFT);
            edit.setContentDescription("Editar");
            edit.setOnClickListener(v -> playlistDialog(pl));
            row.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(44)));
            TextView del = iconButton("🗑️", DANGER_SOFT);
            del.setContentDescription("Apagar");
            del.setOnClickListener(v -> deleteDialog(pl));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(44), dp(44));
            dlp.leftMargin = dp(8);
            row.addView(del, dlp);
        }
        wrap.addView(row, matchWrap());
        return wrap;
    }

    // ---------------------------------------------------------------- UI building blocks

    private void setupWindow() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.TRANSPARENT);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                // Light icons on the blue header, dark icons on the light navigation bar.
                c.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            w.setStatusBarColor(PRIMARY_DARK);
            w.setNavigationBarColor(Color.BLACK);
        }
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.setBackground(rounded(SURFACE, dp(16), 0));
        c.setElevation(dp(2));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.bottomMargin = dp(14);
        parent.addView(c, lp);
        return c;
    }

    private TextView cardTitleText(String title) {
        return text(title, 18, TEXT, true);
    }

    private LinearLayout cardTitle(String title, View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (title != null) row.addView(cardTitleText(title),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) row.addView(action);
        return row;
    }

    private EditText input(LinearLayout parent, String label, String hint, int type) {
        TextView l = text(label, 13, TEXT_2, true);
        l.setPadding(dp(2), dp(14), 0, dp(6));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setInputType(type);
        e.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        e.setHint(hint);
        e.setHintTextColor(0xFFA3AABB);
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setMinHeight(dp(52));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, rounded(SURFACE, dp(12), PRIMARY, dp(2)));
        bg.addState(new int[]{}, rounded(0xFFF8F9FC, dp(12), BORDER, dp(1)));
        e.setBackground(bg);
        parent.addView(e, matchWrap());
        return e;
    }

    private TextView primaryButton(String label) {
        TextView b = text(label, 16, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(52));
        b.setBackground(ripple(rounded(PRIMARY, dp(14), 0), 0x33FFFFFF));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView tonalButton(String label) {
        TextView b = text(label, 15, PRIMARY, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setMinHeight(dp(40));
        b.setBackground(ripple(rounded(PRIMARY_SOFT, dp(20), 0), 0x332F54EB));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView textButton(String label, int color) {
        TextView b = text(label, 14, color, true);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setBackground(ripple(rounded(Color.TRANSPARENT, dp(10), 0), 0x22000000));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView iconButton(String icon, int bg) {
        TextView b = text(icon, 17, TEXT, false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(ripple(rounded(bg, dp(22), 0), 0x22000000));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    private void showBanner(String msg, boolean ok) {
        banner.setText((ok ? "✅  " : "⚠️  ") + msg);
        banner.setTextColor(ok ? SUCCESS : DANGER);
        banner.setBackground(rounded(ok ? SUCCESS_SOFT : DANGER_SOFT, dp(12), 0));
        banner.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean b, String label) {
        busy = b;
        progress.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
        connectButton.setText(b ? label : "Conectar");
        connectButton.setAlpha(b ? 0.7f : 1f);
        connectButton.setEnabled(!b);
        addButton.setEnabled(!b);
        addButton.setAlpha(b ? 0.5f : 1f);
        listBox.setAlpha(b ? 0.5f : 1f);
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        return rounded(color, radius, stroke, stroke == 0 ? 0 : dp(1));
    }

    private GradientDrawable rounded(int color, int radius, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    private Drawable ripple(GradientDrawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    // ---------------------------------------------------------------- crash report

    /** Saves the error of a crash, so the next start can show it instead of silently closing. */
    private void installCrashReporter() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                error.printStackTrace(new java.io.PrintWriter(sw));
                String report = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), "
                        + Build.MANUFACTURER + " " + Build.MODEL + ", app " + BuildInfo.VERSION + "\n\n" + sw;
                prefs.edit().putString("crash", report).commit();
            } catch (Throwable ignored) {
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    private void showLastCrash() {
        final String report = prefs.getString("crash", null);
        if (report == null) return;
        prefs.edit().remove("crash").apply();
        TextView t = text(report, 11, TEXT, false);
        t.setTextIsSelectable(true);
        t.setPadding(dp(20), dp(8), dp(20), dp(8));
        ScrollView sv = new ScrollView(this);
        sv.addView(t);
        new AlertDialog.Builder(this)
                .setTitle("O app fechou com um erro")
                .setView(sv)
                .setPositiveButton("Copiar erro", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("erro", report));
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    // ---------------------------------------------------------------- helpers

    private boolean hasDevice() {
        return !macField.getText().toString().trim().isEmpty() && !keyField.getText().toString().trim().isEmpty();
    }

    private String currentMac() { return FunplaysApi.normalizeMac(macField.getText().toString()); }

    private static boolean validUrl(String u) { return u.startsWith("http://") || u.startsWith("https://"); }

    private static String shortHost(String url) {
        try {
            String host = new java.net.URL(url).getHost();
            return host == null || host.isEmpty() ? url : host;
        } catch (Exception e) {
            return url;
        }
    }

    private static String friendlyError(String raw) {
        if (raw == null) return "Erro desconhecido.";
        String r = raw.toLowerCase();
        if (r.contains("unable to resolve host") || r.contains("failed to connect") || r.contains("timeout"))
            return "Sem conexão com a internet ou servidor fora do ar.";
        return raw;
    }

    private String clipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm != null ? cm.getPrimaryClip() : null;
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence t = clip.getItemAt(0).coerceToText(this);
        return t == null ? null : t.toString().trim();
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        if (f == null) return;
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
        f.clearFocus();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapRight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        return lp;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
