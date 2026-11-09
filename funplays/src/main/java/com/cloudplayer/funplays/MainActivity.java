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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Manage several funplays devices (MAC + key) and their M3U playlists. */
public class MainActivity extends Activity {

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

    private TextView banner, headerSubtitle, deviceStatus, listTitle;
    private LinearLayout deviceChips, listBox;
    private ProgressBar progress;
    private TextView addPlaylistButton;
    private boolean busy;

    private Devices devices;
    private SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private interface Job { String run(String token) throws Exception; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("funplays", MODE_PRIVATE);
        devices = new Devices(prefs);
        devices.migrateLegacy();
        installCrashReporter();
        try { setupWindow(); } catch (Throwable ignored) { }

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);

        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{PRIMARY, PRIMARY_DARK}));
        header.addView(text("Funplays Playlists", 24, Color.WHITE, true));
        headerSubtitle = text("Gerencie os aparelhos e as listas da sua TV", 14, 0xD9FFFFFF, false);
        headerSubtitle.setPadding(0, dp(4), 0, 0);
        header.addView(headerSubtitle);
        screen.addView(header, matchWrap());

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(PRIMARY));
        progress.setVisibility(View.INVISIBLE);
        screen.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));

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

        // ---- Devices card ----
        LinearLayout deviceCard = card(content);
        TextView addDevice = tonalButton("＋  Adicionar");
        addDevice.setOnClickListener(v -> deviceDialog(null));
        deviceCard.addView(cardTitleRow("📺  Meus aparelhos", addDevice));
        deviceChips = new LinearLayout(this);
        deviceChips.setOrientation(LinearLayout.VERTICAL);
        deviceChips.setPadding(0, dp(10), 0, dp(2));
        deviceCard.addView(deviceChips, matchWrap());
        deviceStatus = text("", 13, TEXT_2, false);
        deviceStatus.setPadding(dp(2), dp(6), 0, 0);
        deviceCard.addView(deviceStatus);
        TextView bulkMacButton = textButton("📋  Adicionar vários MACs", PRIMARY);
        bulkMacButton.setOnClickListener(v -> bulkAddDialog());
        LinearLayout.LayoutParams bmLp = matchWrap();
        bmLp.topMargin = dp(6);
        deviceCard.addView(bulkMacButton, bmLp);
        TextView dnsButton = textButton("🔧  Trocar DNS em massa", PRIMARY);
        dnsButton.setOnClickListener(v -> dnsDialog());
        deviceCard.addView(dnsButton, matchWrap());

        // ---- Playlists card ----
        LinearLayout listCard = card(content);
        addPlaylistButton = tonalButton("＋  Nova");
        addPlaylistButton.setOnClickListener(v -> playlistDialog(null));
        listTitle = text("🎬  Playlists", 18, TEXT, true);
        LinearLayout lHead = new LinearLayout(this);
        lHead.setOrientation(LinearLayout.HORIZONTAL);
        lHead.setGravity(Gravity.CENTER_VERTICAL);
        lHead.addView(listTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        lHead.addView(addPlaylistButton);
        listCard.addView(lHead);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(listBox, matchWrap());

        TextView foot = text("As alterações aparecem na TV depois de reabrir o app.", 12, TEXT_2, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(4), 0, 0);
        content.addView(foot, matchWrap());

        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                top = bars.top;
                bottom = Math.max(bars.bottom, ime.bottom);
                v.setPadding(bars.left, 0, bars.right, 0);
            } else {
                top = 0;
                bottom = 0;
            }
            header.setPadding(dp(20), top + dp(20), dp(20), dp(24));
            scroll.setPadding(0, 0, 0, bottom);
            return insets;
        });

        setContentView(screen);
        showLastCrash();
        renderDevices();

        JSONObject sel = devices.selected();
        if (sel != null) {
            devices.select(Devices.id(sel));
            loadPlaylists(sel);
        } else {
            showEmpty("Nenhum aparelho. Toque em ＋ Adicionar.");
            deviceStatus.setText("Adicione o MAC e a Device Key que aparecem na TV.");
        }
    }

    // ---------------------------------------------------------------- devices UI

    private void renderDevices() {
        deviceChips.removeAllViews();
        JSONArray list = devices.all();
        String selId = devices.selectedId();
        if (list.length() == 0) {
            TextView empty = text("Nenhum aparelho salvo", 14, TEXT_2, false);
            empty.setPadding(dp(2), dp(4), 0, dp(4));
            deviceChips.addView(empty);
            return;
        }
        for (int i = 0; i < list.length(); i++) {
            final JSONObject d = list.optJSONObject(i);
            if (d == null) continue;
            boolean active = Devices.id(d).equals(selId) || (selId == null && i == 0);
            deviceChips.addView(deviceRow(d, active, i > 0));
        }
    }

    private View deviceRow(final JSONObject d, boolean active, boolean divider) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wlp = matchWrap();
        wlp.bottomMargin = dp(8);
        wrap.setLayoutParams(wlp);

        LinearLayout rowL = new LinearLayout(this);
        rowL.setOrientation(LinearLayout.HORIZONTAL);
        rowL.setGravity(Gravity.CENTER_VERTICAL);
        rowL.setPadding(dp(12), dp(12), dp(8), dp(12));
        rowL.setBackground(ripple(rounded(active ? PRIMARY_SOFT : 0xFFF8F9FC, dp(12),
                active ? PRIMARY : BORDER, dp(active ? 2 : 1)), 0x22000000));
        rowL.setClickable(true);
        rowL.setFocusable(true);

        // Selection dot
        TextView dot = text(active ? "●" : "○", 16, active ? PRIMARY : 0xFFB8C0D0, true);
        dot.setPadding(0, 0, dp(10), 0);
        rowL.addView(dot);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(Devices.name(d), 16, TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(name);
        TextView mac = text(Devices.mac(d).toUpperCase(), 13, TEXT_2, false);
        mac.setTypeface(Typeface.MONOSPACE);
        mac.setSingleLine(true);
        info.addView(mac);
        if (!Devices.hasKey(d)) {
            TextView warn = text("🔑 falta a Device Key", 12, 0xFFB26A00, true);
            info.addView(warn);
        }
        rowL.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView edit = iconButton("✏️", active ? SURFACE : PRIMARY_SOFT);
        edit.setOnClickListener(v -> deviceDialog(d));
        rowL.addView(edit, new LinearLayout.LayoutParams(dp(42), dp(42)));

        rowL.setOnClickListener(v -> {
            if (busy) return;
            if (!Devices.id(d).equals(devices.selectedId())) {
                devices.select(Devices.id(d));
                renderDevices();
                loadPlaylists(d);
            }
        });
        wrap.addView(rowL, matchWrap());
        return wrap;
    }

    /** Add (d == null) or edit a saved device. */
    private void deviceDialog(final JSONObject d) {
        final boolean editing = d != null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(8));
        final EditText name = input(box, "Apelido do aparelho", "ex: Sala, Quarto", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText mac = input(box, "MAC", "aa:bb:cc:dd:ee:ff",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        final EditText key = input(box, "Device Key (pode preencher depois)", "ex: 123456",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (editing) {
            name.setText(Devices.name(d));
            mac.setText(Devices.mac(d));
            key.setText(Devices.key(d));
        } else {
            name.setText("Aparelho " + (devices.count() + 1));
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(editing ? "Editar aparelho" : "Novo aparelho")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(editing ? "Salvar" : "Adicionar", null);
        if (editing) b.setNeutralButton("Apagar", null);
        final AlertDialog dialog = b.create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PRIMARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
            if (editing) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(DANGER);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String n = name.getText().toString().trim();
                String m = FunplaysApi.normalizeMac(mac.getText().toString());
                String k = key.getText().toString().trim();
                if (!isValidMac(m)) { mac.setError("MAC inválido (ex: aa:bb:cc:dd:ee:ff)"); return; }
                // Device Key is optional: can be filled in later.
                if (n.isEmpty()) n = Devices.macTail(m);
                dialog.dismiss();
                String id = devices.save(editing ? Devices.id(d) : null, n, m, k);
                devices.select(id);
                renderDevices();
                loadPlaylists(devices.get(id));
            });
            if (editing) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                dialog.dismiss();
                confirmDeleteDevice(d);
            });
        });
        dialog.show();
    }

    /** Paste many MACs at once; the device key is filled in later per device. */
    private void bulkAddDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(8));
        TextView help = text("Cole um MAC por linha. Pode ser só o MAC, ou \"MAC, Apelido\". A Device Key você coloca depois, tocando no lápis de cada aparelho.",
                13, TEXT_2, false);
        help.setPadding(0, 0, 0, dp(4));
        box.addView(help);
        final EditText macs = input(box, "MACs", "aa:bb:cc:dd:ee:01\naa:bb:cc:dd:ee:02, Sala",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        macs.setMinLines(5);
        macs.setMaxLines(12);
        macs.setGravity(Gravity.TOP | Gravity.START);
        TextView paste = textButton("📋  Colar", PRIMARY);
        paste.setOnClickListener(v -> {
            String t = clipboard();
            if (t != null) {
                String cur = macs.getText().toString();
                macs.setText(cur.isEmpty() ? t : cur + "\n" + t);
            }
        });
        box.addView(paste, wrapRight());

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Adicionar vários MACs")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Adicionar", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PRIMARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                java.util.List<String[]> parsed = new java.util.ArrayList<>();
                int invalid = 0;
                for (String line : macs.getText().toString().split("\n")) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    String name = "";
                    String macPart = t;
                    int sep = indexOfAny(t, ",;\t");
                    if (sep >= 0) { macPart = t.substring(0, sep).trim(); name = t.substring(sep + 1).trim(); }
                    String mac = FunplaysApi.normalizeMac(macPart);
                    if (isValidMac(mac)) parsed.add(new String[]{mac, name});
                    else invalid++;
                }
                if (parsed.isEmpty()) { macs.setError("Nenhum MAC válido encontrado"); return; }
                dialog.dismiss();
                int added = devices.addMany(parsed);
                int dup = parsed.size() - added;
                renderDevices();
                if (devices.selected() == null && devices.count() > 0) {
                    JSONObject first = devices.first();
                    devices.select(Devices.id(first));
                    renderDevices();
                }
                StringBuilder m = new StringBuilder(added + " aparelho(s) adicionado(s).");
                if (dup > 0) m.append(" ").append(dup).append(" já existiam.");
                if (invalid > 0) m.append(" ").append(invalid).append(" linha(s) sem MAC válido.");
                m.append(" Agora toque no lápis de cada um para pôr a Device Key.");
                showBanner(m.toString(), added > 0);
            });
        });
        dialog.show();
    }

    private void confirmDeleteDevice(final JSONObject d) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Apagar aparelho?")
                .setMessage("Remover \"" + Devices.name(d) + "\" da lista deste app. As playlists na TV não são apagadas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (x, w) -> {
                    devices.delete(Devices.id(d));
                    renderDevices();
                    JSONObject next = devices.selected();
                    if (next != null) loadPlaylists(next);
                    else {
                        listBox.removeAllViews();
                        showEmpty("Nenhum aparelho. Toque em ＋ Adicionar.");
                        listTitle.setText("🎬  Playlists");
                        deviceStatus.setText("");
                    }
                })
                .create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(DANGER);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
        });
        dialog.show();
    }

    // ---------------------------------------------------------------- bulk DNS change

    private void dnsDialog() {
        if (devices.count() == 0) { showBanner("Adicione um aparelho primeiro.", false); return; }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(8));
        TextView help = text("Troca o servidor (DNS) no começo do link em TODAS as playlists de TODOS os aparelhos. O usuário e a senha são mantidos.",
                13, TEXT_2, false);
        help.setPadding(0, 0, 0, dp(4));
        box.addView(help);

        final EditText oldDns = input(box, "DNS atual (que caiu)", "http://servidor-antigo.com:8080",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        TextView p1 = textButton("📋  Colar", PRIMARY);
        p1.setOnClickListener(v -> { String t = clipboard(); if (t != null) oldDns.setText(t); });
        box.addView(p1, wrapRight());
        final EditText newDns = input(box, "DNS novo", "http://servidor-novo.com:8080",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        TextView p2 = textButton("📋  Colar", PRIMARY);
        p2.setOnClickListener(v -> { String t = clipboard(); if (t != null) newDns.setText(t); });
        box.addView(p2, wrapRight());

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Trocar DNS em massa")
                .setView(sv)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PRIMARY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String oldD = oldDns.getText().toString().trim();
                String newD = newDns.getText().toString().trim();
                if (Dns.hostPort(oldD).isEmpty()) { oldDns.setError("Informe o DNS atual"); return; }
                if (Dns.hostPort(newD).isEmpty()) { newDns.setError("Informe o DNS novo"); return; }
                if (Dns.hostPort(oldD).equals(Dns.hostPort(newD))) { newDns.setError("O DNS novo é igual ao atual"); return; }
                dialog.dismiss();
                bulkDnsChange(oldD, newD);
            });
        });
        dialog.show();
    }

    private void bulkDnsChange(final String oldDns, final String newDns) {
        if (busy) return;
        setBusy(true, "Trocando DNS...");
        banner.setVisibility(View.GONE);
        final JSONArray allDevices = devices.all();

        worker.execute(() -> {
            int changed = 0, skippedProtected = 0, deviceCount = allDevices.length(), deviceErrors = 0;
            StringBuilder errors = new StringBuilder();
            for (int i = 0; i < allDevices.length(); i++) {
                JSONObject device = allDevices.optJSONObject(i);
                if (device == null) continue;
                final String label = Devices.name(device);
                final int step = i + 1;
                ui.post(() -> setBusy(true, "Trocando DNS... (" + step + "/" + deviceCount + ") " + label));
                if (!Devices.hasKey(device)) {
                    deviceErrors++;
                    errors.append("\n• ").append(label).append(": sem Device Key (pulado)");
                    continue;
                }
                try {
                    String token = FunplaysApi.login(Devices.mac(device), Devices.key(device));
                    JSONArray playlists = FunplaysApi.listPlaylists(token);
                    for (int j = 0; j < playlists.length(); j++) {
                        JSONObject pl = playlists.optJSONObject(j);
                        if (pl == null) continue;
                        String newUrl = Dns.replace(pl.optString("url"), oldDns, newDns);
                        if (newUrl == null) continue;
                        if (pl.optBoolean("is_protected", false)) { skippedProtected++; continue; }
                        String epg = pl.optString("epg_url");
                        if ("null".equals(epg)) epg = "";
                        FunplaysApi.editPlaylist(token, pl.optLong("id"), pl.optString("name"), newUrl, epg);
                        changed++;
                    }
                } catch (Exception e) {
                    deviceErrors++;
                    errors.append("\n• ").append(label).append(": ").append(friendlyError(e.getMessage()));
                }
                // Small pause between devices to stay under the server's rate limit.
                try { Thread.sleep(400); } catch (InterruptedException ignored) { }
            }
            final int totalChanged = changed, totalSkipped = skippedProtected, totalErrors = deviceErrors;
            final String errText = errors.toString();
            ui.post(() -> {
                if (isFinishing()) return;
                setBusy(false, null);
                StringBuilder msg = new StringBuilder();
                msg.append(totalChanged).append(" playlist(s) atualizada(s) em ").append(deviceCount).append(" aparelho(s).");
                if (totalSkipped > 0) msg.append(" ").append(totalSkipped).append(" com PIN foram ignoradas.");
                boolean ok = totalErrors == 0;
                if (!ok) msg.append(" Falha em ").append(totalErrors).append(" aparelho(s):").append(errText);
                showBanner(msg.toString(), ok && totalChanged > 0);
                if (totalChanged == 0 && ok) showBanner("Nenhuma playlist usava esse DNS. Nada foi alterado.", false);
                JSONObject sel = devices.selected();
                if (sel != null) loadPlaylists(sel);
            });
        });
    }

    // ---------------------------------------------------------------- playlists

    private void loadPlaylists(JSONObject device) {
        if (device == null) return;
        if (!Devices.hasKey(device)) {
            // No key yet: do not hit the server. Prompt to add it.
            listBox.removeAllViews();
            listTitle.setText("🎬  Playlists");
            showEmpty("Este aparelho ainda não tem a Device Key.\nToque no lápis para adicioná-la.");
            deviceStatus.setText("🔑 " + Devices.name(device) + ": falta a Device Key");
            deviceStatus.setTextColor(0xFFB26A00);
            headerSubtitle.setText(devices.count() + " aparelho(s) · " + Devices.name(device));
            banner.setVisibility(View.GONE);
            return;
        }
        run(device, "Carregando...", t -> null, false);
    }

    private void playlistDialog(final JSONObject pl) {
        final JSONObject device = devices.selected();
        if (device == null) { showBanner("Adicione um aparelho primeiro.", false); return; }
        if (!Devices.hasKey(device)) { showBanner("Adicione a Device Key deste aparelho (lápis) antes de mexer nas playlists.", false); return; }
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
            name.setText(prefs.getString("lastName", "Playlist"));
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final String n = name.getText().toString().trim().isEmpty() ? "Playlist" : name.getText().toString().trim();
                final String u = url.getText().toString().trim();
                final String e = epg.getText().toString().trim();
                if (!validUrl(u)) { url.setError("O link precisa começar com http:// ou https://"); return; }
                if (!e.isEmpty() && !validUrl(e)) { epg.setError("Link EPG inválido"); return; }
                dialog.dismiss();
                if (editing) {
                    final long id = pl.optLong("id");
                    run(device, "Salvando...", t -> {
                        FunplaysApi.editPlaylist(t, id, n, u, e);
                        return "Playlist \"" + n + "\" atualizada.";
                    }, true);
                } else {
                    prefs.edit().putString("lastName", n).apply();
                    final String mac = Devices.mac(device);
                    run(device, "Adicionando...", t -> {
                        FunplaysApi.addPlaylist(t, mac, u, n, e);
                        return "Playlist \"" + n + "\" adicionada! Reabra o app na TV.";
                    }, true);
                }
            });
        });
        dialog.show();
    }

    private void deletePlaylistDialog(final JSONObject pl) {
        final JSONObject device = devices.selected();
        final String name = pl.optString("name");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Apagar playlist?")
                .setMessage("\"" + name + "\" será removida deste aparelho.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (d, w) -> {
                    final long id = pl.optLong("id");
                    run(device, "Apagando...", t -> {
                        FunplaysApi.deletePlaylist(t, id);
                        return "Playlist \"" + name + "\" apagada.";
                    }, true);
                })
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(DANGER);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(TEXT_2);
        });
        dialog.show();
    }

    /** Logs into the given device, runs the job and reloads its playlists. */
    private void run(final JSONObject device, String busyText, final Job job, final boolean showMsg) {
        if (busy || device == null) return;
        final String mac = Devices.mac(device);
        final String key = Devices.key(device);
        final String label = Devices.name(device);
        setBusy(true, busyText);

        worker.execute(() -> {
            String message = null, error = null;
            JSONArray list = null;
            try {
                // Only two requests (login + list) to stay under the server's rate limit.
                String token = FunplaysApi.login(mac, key);
                message = job.run(token);
                list = FunplaysApi.listPlaylists(token);
            } catch (Exception e) {
                error = friendlyError(e.getMessage());
            }
            final String msg = message, err = error;
            final JSONArray playlists = list;
            ui.post(() -> {
                if (isFinishing()) return;
                setBusy(false, null);
                if (err != null) {
                    showBanner(err, false);
                    deviceStatus.setText("⚠ " + label + ": não conectado");
                    deviceStatus.setTextColor(DANGER);
                    return;
                }
                deviceStatus.setText("✓  Conectado · " + label);
                deviceStatus.setTextColor(SUCCESS);
                headerSubtitle.setText(devices.count() + " aparelho(s) · " + label);
                if (showMsg && msg != null) showBanner(msg, true); else banner.setVisibility(View.GONE);
                if (playlists != null) renderList(playlists);
            });
        });
    }

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
            edit.setOnClickListener(v -> playlistDialog(pl));
            row.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(44)));
            TextView del = iconButton("🗑️", DANGER_SOFT);
            del.setOnClickListener(v -> deletePlaylistDialog(pl));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(44), dp(44));
            dlp.leftMargin = dp(8);
            row.addView(del, dlp);
        }
        wrap.addView(row, matchWrap());
        return wrap;
    }

    // ---------------------------------------------------------------- crash report

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

    // ---------------------------------------------------------------- UI helpers

    private void setupWindow() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.TRANSPARENT);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) c.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
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

    private LinearLayout cardTitleRow(String title, View action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(title, 18, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
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
        addPlaylistButton.setEnabled(!b);
        addPlaylistButton.setAlpha(b ? 0.5f : 1f);
        listBox.setAlpha(b ? 0.5f : 1f);
        deviceChips.setAlpha(b ? 0.6f : 1f);
        if (b && label != null) { deviceStatus.setText(label); deviceStatus.setTextColor(TEXT_2); }
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

    private static boolean validUrl(String u) { return u.startsWith("http://") || u.startsWith("https://"); }

    private static boolean isValidMac(String mac) {
        return mac != null && mac.matches("^([0-9a-f]{2}:){5}[0-9a-f]{2}$");
    }

    private static int indexOfAny(String s, String chars) {
        for (int i = 0; i < s.length(); i++) if (chars.indexOf(s.charAt(i)) >= 0) return i;
        return -1;
    }

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
        if (r.contains("429") || r.contains("muitas requisi"))
            return "O servidor pediu para esperar (muitas requisições). Tente de novo em alguns segundos.";
        return raw;
    }

    private String clipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm != null ? cm.getPrimaryClip() : null;
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence t = clip.getItemAt(0).coerceToText(this);
        return t == null ? null : t.toString().trim();
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
