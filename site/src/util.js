// Funções auxiliares: HTML, cookies assinados, datas, dinheiro, configurações.
export const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
export const now = () => Math.floor(Date.now() / 1000);
export const DAY = 86400;

export function html(body, status = 200, headers = {}) {
  return new Response(body, { status, headers: { "content-type": "text/html; charset=utf-8", ...headers } });
}
export function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*" } });
}
export function redirect(to, headers = {}) {
  return new Response(null, { status: 303, headers: { location: to, ...headers } });
}

// MAC no formato AA:BB:CC:DD:EE:FF
export function normMac(s) {
  const hex = String(s || "").toUpperCase().replace(/[^0-9A-F]/g, "");
  if (hex.length !== 12) return null;
  return hex.match(/../g).join(":");
}
export const normKey = (s) => String(s || "").replace(/\D/g, "").slice(0, 8);

export function fmtDate(sec) {
  if (!sec) return "—";
  const d = new Date(sec * 1000);
  return d.toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" });
}
export function fmtDateTime(sec) {
  if (!sec) return "—";
  const d = new Date(sec * 1000);
  return d.toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}
export const money = (v) => "R$ " + Number(v).toFixed(2).replace(".", ",").replace(/\B(?=(\d{3})+(?!\d),)/g, ".");

// --- cookies assinados (HMAC) ---
async function hmac(secret, data) {
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(data));
  return btoa(String.fromCharCode(...new Uint8Array(sig))).replace(/[+/=]/g, (c) => ({ "+": "-", "/": "_", "=": "" }[c]));
}
export async function makeCookie(env, name, value, maxAge = 60 * 60 * 24 * 30) {
  const payload = `${value}|${now() + maxAge}`;
  if (!env.SESSION_SECRET) throw new Error("SESSION_SECRET não configurado");
  const sig = await hmac(env.SESSION_SECRET, payload);
  return `${name}=${encodeURIComponent(payload + "|" + sig)}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=${maxAge}`;
}
export const clearCookie = (name) => `${name}=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0`;
export async function readCookie(env, req, name) {
  const raw = (req.headers.get("cookie") || "").split(/;\s*/).find((c) => c.startsWith(name + "="));
  if (!raw) return null;
  const [value, exp, sig] = decodeURIComponent(raw.slice(name.length + 1)).split("|");
  if (!value || !exp || !sig || Number(exp) < now()) return null;
  if (!env.SESSION_SECRET) return null;
  return safeEq(await hmac(env.SESSION_SECRET, `${value}|${exp}`), sig) ? value : null;
}

// --- configurações (preços, dias de teste) ---
export const DEFAULTS = { trial_days: "7", price_annual: "29.90", price_lifetime: "59.90", max_lists: "5", payer_email: "", support: "" };
export async function settings(env) {
  const { results } = await env.DB.prepare("SELECT k, v FROM settings").all();
  const s = { ...DEFAULTS };
  for (const r of results) s[r.k] = r.v;
  return s;
}

// Situação do aparelho
export function deviceStatus(d) {
  const t = now();
  if (!d) return "unknown";
  if (d.blocked) return "blocked";
  if (d.plan === "lifetime") return "active";
  if (d.plan === "annual" && d.expires_at && d.expires_at > t) return "active";
  if (d.trial_until > t && d.plan === "trial") return "trial";
  return "expired";
}
export const STATUS_LABEL = { active: "Ativo", trial: "Teste grátis", expired: "Vencido", blocked: "Bloqueado", unknown: "—" };
export const PLAN_LABEL = { trial: "Teste", annual: "Anual", lifetime: "Vitalício" };

// comparação em tempo constante (senhas, chaves, assinaturas)
export function safeEq(a, b) {
  a = String(a ?? ""); b = String(b ?? "");
  let d = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) d |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return d === 0;
}

// Limite de tentativas erradas (chave do aparelho, senha do painel): 8 erros em 15 min bloqueiam por 15 min
export async function tooMany(env, who) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS attempts (who TEXT NOT NULL, ts INTEGER NOT NULL)").run();
  const r = await env.DB.prepare("SELECT COUNT(*) AS n FROM attempts WHERE who=? AND ts>?").bind(who, now() - 900).first();
  return (r?.n || 0) >= 8;
}
export async function failed(env, who) {
  await env.DB.prepare("CREATE TABLE IF NOT EXISTS attempts (who TEXT NOT NULL, ts INTEGER NOT NULL)").run();
  await env.DB.prepare("INSERT INTO attempts (who, ts) VALUES (?, ?)").bind(who, now()).run();
  await env.DB.prepare("DELETE FROM attempts WHERE ts<?").bind(now() - 86400).run();
}
export const clientIp = (req) => req.headers.get("cf-connecting-ip") || "?";
