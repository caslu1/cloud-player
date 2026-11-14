// API usada pelos apps (Android e Roku).
import { json, now, DAY, normMac, normKey, settings, deviceStatus, safeEq, tooMany, failed, clientIp } from "./util.js";
import qrcode from "qrcode-generator";

async function body(req) {
  try { return await req.json(); } catch { return {}; }
}

function out(d, lists, s, origin) {
  const st = deviceStatus(d);
  return {
    mac: d.mac, key: d.dkey, status: st, plan: d.plan,
    trial_until: d.trial_until, expires_at: d.expires_at,
    days_left: st === "trial" ? Math.max(0, Math.ceil((d.trial_until - now()) / DAY)) : st === "active" && d.expires_at ? Math.max(0, Math.ceil((d.expires_at - now()) / DAY)) : null,
    activate_url: `${origin}/ativar?mac=${encodeURIComponent(d.mac)}&key=${d.dkey}`,
    site: origin, support: s.support,
    playlists: st === "blocked" ? [] : lists.map((l) => ({ id: l.id, name: l.name, type: l.type, server: l.server, username: l.username, password: l.password, url: l.url, pin: l.pin })),
  };
}

// POST /api/app/register {mac, key, platform, model}: cadastra (com teste grátis) ou atualiza o aparelho.
export async function register(env, req) {
  const b = await body(req);
  const mac = normMac(b.mac), key = normKey(b.key);
  if (!mac || key.length < 4) return json({ error: "dados inválidos" }, 400);
  const s = await settings(env);
  let d = await env.DB.prepare("SELECT * FROM devices WHERE mac=?").bind(mac).first();
  if (!d) {
    await env.DB.prepare("INSERT INTO devices (mac, dkey, platform, model, created_at, trial_until, plan, last_seen) VALUES (?,?,?,?,?,?, 'trial', ?)")
      .bind(mac, key, String(b.platform || "").slice(0, 30), String(b.model || "").slice(0, 60), now(), now() + Number(s.trial_days) * DAY, now()).run();
  } else {
    // segurança: a chave de um aparelho já cadastrado não é trocada por quem só sabe o MAC
    // (para trocar, o admin edita a chave no painel)
    if (!safeEq(d.dkey, key)) {
      if (await tooMany(env, "dev:" + mac)) return json({ error: "muitas tentativas" }, 429);
      await failed(env, "dev:" + mac);
      return json({ error: "chave diferente", code: "key_mismatch" }, 409);
    }
    await env.DB.prepare("UPDATE devices SET platform=?, model=?, last_seen=? WHERE mac=?").bind(String(b.platform || d.platform).slice(0, 30), String(b.model || d.model).slice(0, 60), now(), mac).run();
  }
  return status(env, req, mac, key);
}

// GET /api/app/status?mac=&key=
export async function status(env, req, macIn, keyIn) {
  const u = new URL(req.url);
  const mac = normMac(macIn ?? u.searchParams.get("mac")), key = normKey(keyIn ?? u.searchParams.get("key"));
  const d = mac && (await env.DB.prepare("SELECT * FROM devices WHERE mac=?").bind(mac).first());
  if (await tooMany(env, "ip:" + clientIp(req))) return json({ error: "muitas tentativas" }, 429);
  if (!d || !safeEq(d.dkey, key)) { await failed(env, "ip:" + clientIp(req)); return json({ error: "aparelho não encontrado" }, 404); }
  if (!macIn) await env.DB.prepare("UPDATE devices SET last_seen=? WHERE mac=?").bind(now(), mac).run();
  const { results } = await env.DB.prepare("SELECT * FROM playlists WHERE mac=? ORDER BY position, id").bind(mac).all();
  return json(out(d, results, await settings(env), u.origin));
}

// GET /api/qr?t=texto → matriz do QR code (para a Roku desenhar)
export function qr(req) {
  const t = new URL(req.url).searchParams.get("t") || "";
  const q = qrcode(0, "M");
  q.addData(t.slice(0, 300));
  q.make();
  const n = q.getModuleCount();
  const rows = [];
  for (let y = 0; y < n; y++) { let r = ""; for (let x = 0; x < n; x++) r += q.isDark(y, x) ? "1" : "0"; rows.push(r); }
  return json({ size: n, rows });
}
