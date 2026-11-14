// Painel admin: visão geral, aparelhos, pagamentos e configurações.
import { safeEq, tooMany, failed, clientIp, esc, html, redirect, now, DAY, normMac, fmtDate, fmtDateTime, money, makeCookie, clearCookie, readCookie, settings, deviceStatus, STATUS_LABEL, PLAN_LABEL, DEFAULTS } from "./util.js";
import { page, adminLayout, ic, logo } from "./ui.js";
import { applyPayment } from "./public.js";

const site = (env) => ({ siteName: env.SITE_NAME });
export const isAdmin = async (env, req) => (await readCookie(env, req, "adm")) === "1";

export function loginPage(env, msg = "") {
  const body = `<div style="min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;position:relative">
<form method="post" action="/admin/entrar" class="card" style="width:400px;max-width:100%;padding:32px">${logo()}
<div class="o" style="font-size:22px;margin-top:24px">Painel admin</div>
${msg ? `<div class="alert err" style="margin-top:14px">${esc(msg)}</div>` : ""}
<label class="lab" style="margin-top:18px">SENHA</label><input class="in" type="password" name="senha" autofocus required>
<button class="btn full" style="margin-top:20px">Entrar</button></form></div>`;
  return html(page("Admin", body, site(env)));
}

export async function doLogin(env, req) {
  const f = await req.formData();
  if (!env.ADMIN_PASSWORD) return loginPage(env, "Defina a senha do painel (ADMIN_PASSWORD) antes de entrar.");
  const who = "adm:" + clientIp(req);
  if (await tooMany(env, who)) return loginPage(env, "Muitas tentativas. Aguarde 15 minutos.");
  if (!safeEq(String(f.get("senha")), env.ADMIN_PASSWORD)) { await failed(env, who); return loginPage(env, "Senha incorreta."); }
  return redirect("/admin", { "set-cookie": await makeCookie(env, "adm", "1", 60 * 60 * 12) });
}
export const logout = () => redirect("/admin", { "set-cookie": clearCookie("adm") });

const chip = (st) => `<span class="chip st-${st}">${STATUS_LABEL[st]}</span>`;
const sqlStatus = {
  active: "blocked=0 AND (plan='lifetime' OR (plan='annual' AND expires_at > :t))",
  trial: "blocked=0 AND plan='trial' AND trial_until > :t",
  expired: "blocked=0 AND NOT (plan='lifetime' OR (plan='annual' AND expires_at > :t) OR (plan='trial' AND trial_until > :t))",
  blocked: "blocked=1",
};
const where = (f) => (sqlStatus[f] || "1=1").replaceAll(":t", String(now()));

function deviceTable(rows) {
  return `<table><tr><th>MAC</th><th class="hide-m">APARELHO</th><th>STATUS</th><th class="hide-m">PLANO</th><th>VENCIMENTO</th><th class="hide-m">LISTAS</th><th class="hide-m">ÚLTIMO ACESSO</th><th></th></tr>
${rows.map((d) => { const st = deviceStatus(d); const venc = d.plan === "lifetime" ? "—" : d.plan === "annual" ? fmtDate(d.expires_at) : fmtDate(d.trial_until);
  return `<tr><td class="mono">${esc(d.mac)}</td><td class="hide-m">${esc(d.model || d.platform || "—")}</td><td>${chip(st)}</td><td class="hide-m">${PLAN_LABEL[d.plan] || "—"}</td><td>${venc}</td><td class="hide-m">${d.lists ?? 0}</td><td class="dim hide-m">${fmtDateTime(d.last_seen)}</td><td><a href="/admin/aparelho/${encodeURIComponent(d.mac)}" style="color:var(--accent2);font-weight:600">Gerenciar</a></td></tr>`; }).join("")}
${rows.length ? "" : `<tr><td colspan="8" class="dim" style="text-align:center;padding:30px">Nenhum aparelho</td></tr>`}</table>`;
}

const LIST_SQL = "SELECT d.*, (SELECT COUNT(*) FROM playlists p WHERE p.mac = d.mac) lists FROM devices d";

export async function overview(env) {
  const t = now(), month = Math.floor(new Date(new Date().getFullYear(), new Date().getMonth(), 1).getTime() / 1000);
  const c = async (sql) => (await env.DB.prepare(sql).first()).n;
  const total = await c("SELECT COUNT(*) n FROM devices");
  const week = await c(`SELECT COUNT(*) n FROM devices WHERE created_at > ${t - 7 * DAY}`);
  const active = await c(`SELECT COUNT(*) n FROM devices WHERE ${where("active")}`);
  const trial = await c(`SELECT COUNT(*) n FROM devices WHERE ${where("trial")}`);
  const soon = await c(`SELECT COUNT(*) n FROM devices WHERE ${where("trial")} AND trial_until < ${t + 2 * DAY}`);
  const rev = await env.DB.prepare(`SELECT COALESCE(SUM(amount),0) s, COUNT(*) n FROM payments WHERE status='approved' AND paid_at >= ${month}`).first();
  const { results } = await env.DB.prepare(`${LIST_SQL} ORDER BY COALESCE(last_seen, created_at) DESC LIMIT 10`).all();
  const card = (t1, v, s, color) => `<div class="card" style="padding:20px"><div class="dim" style="font-size:12px;font-weight:600">${t1}</div><div class="o" style="font-size:28px;margin-top:6px">${v}</div><div style="font-size:12px;color:${color};margin-top:4px">${s}</div></div>`;
  const pct = total ? ((active / total) * 100).toFixed(1).replace(".", ",") : "0";
  const body = `<div class="row" style="flex-wrap:wrap;gap:12px"><h1 style="font-size:26px">Visão geral</h1>
<form action="/admin/aparelhos" style="margin-left:auto;width:360px;max-width:100%"><input class="in" name="q" placeholder="Buscar MAC, chave ou modelo…" style="height:40px"></form></div>
<div class="stats" style="margin-top:22px">${card("APARELHOS", total.toLocaleString("pt-BR"), `+${week} nesta semana`, "var(--ok)")}${card("ATIVOS (PAGOS)", active.toLocaleString("pt-BR"), `${pct}% do total`, "var(--mut)")}${card("EM TESTE", trial.toLocaleString("pt-BR"), `${soon} vencem em 2 dias`, "var(--warn)")}${card("RECEITA DO MÊS", money(rev.s), `${rev.n} pagamentos Pix`, "var(--ok)")}</div>
<div class="card" style="margin-top:22px;overflow:auto"><div class="row" style="padding:18px 20px"><div class="o" style="font-size:16px">Aparelhos recentes</div><a href="/admin/aparelhos" style="margin-left:auto;color:var(--accent2);font-weight:600">Ver todos</a></div>${deviceTable(results)}</div>`;
  return html(page("Visão geral", adminLayout("/admin", "", body), site(env)));
}

export async function devices(env, req) {
  const u = new URL(req.url);
  const q = (u.searchParams.get("q") || "").trim(), f = u.searchParams.get("f") || "";
  const qq = `%${q.replace(/[^0-9a-zA-Z:. -]/g, "")}%`;
  const sql = `${LIST_SQL} WHERE ${where(f)} ${q ? "AND (d.mac LIKE ?1 OR d.dkey LIKE ?1 OR d.model LIKE ?1 OR d.note LIKE ?1 OR REPLACE(d.mac, ':', '') LIKE ?2)" : ""} ORDER BY COALESCE(last_seen, created_at) DESC LIMIT 200`;
  const stmt = env.DB.prepare(sql);
  const { results } = q ? await stmt.bind(qq, `%${q.replace(/[^0-9a-zA-Z]/g, "").toUpperCase()}%`).all() : await stmt.all();
  const tab = (k, t) => `<a href="/admin/aparelhos?f=${k}${q ? "&q=" + encodeURIComponent(q) : ""}" class="chip" style="background:${f === k ? "var(--accent)" : "#16121f"};color:${f === k ? "#fff" : "var(--mut)"}">${t}</a>`;
  const body = `<div class="row" style="flex-wrap:wrap;gap:12px"><h1 style="font-size:26px">Aparelhos</h1>
<form style="margin-left:auto;width:360px;max-width:100%"><input type="hidden" name="f" value="${esc(f)}"><input class="in" name="q" value="${esc(q)}" placeholder="Buscar MAC, chave ou modelo…" style="height:40px"></form></div>
<div class="card" style="margin-top:22px;overflow:auto"><div class="row" style="padding:18px 20px;gap:8px;flex-wrap:wrap">${tab("", "Todos")}${tab("active", "Ativos")}${tab("trial", "Teste")}${tab("expired", "Vencidos")}${tab("blocked", "Bloqueados")}
<form method="post" action="/admin/aparelho/novo" class="row" style="margin-left:auto;gap:8px"><input class="in mono" name="mac" placeholder="Novo MAC" style="height:36px;width:200px"><button class="btn sm">Adicionar</button></form></div>${deviceTable(results)}</div>`;
  return html(page("Aparelhos", adminLayout("/admin/aparelhos", "", body), site(env)));
}

export async function deviceEdit(env, macIn, msg = "") {
  const mac = normMac(macIn);
  const d = await env.DB.prepare("SELECT * FROM devices WHERE mac=?").bind(mac).first();
  if (!d) return redirect("/admin/aparelhos");
  const { results: lists } = await env.DB.prepare("SELECT * FROM playlists WHERE mac=? ORDER BY position, id").bind(mac).all();
  const { results: pays } = await env.DB.prepare("SELECT * FROM payments WHERE mac=? ORDER BY id DESC LIMIT 20").bind(mac).all();
  const st = deviceStatus(d);
  const dt = (s) => (s ? new Date(s * 1000).toISOString().slice(0, 10) : "");
  const body = `<a href="/admin/aparelhos" class="mut">← Aparelhos</a><div class="row" style="gap:12px;margin-top:10px;flex-wrap:wrap"><h1 style="font-size:26px" class="mono">${esc(d.mac)}</h1>${chip(st)}</div>
${msg ? `<div class="alert ok" style="margin-top:16px">${esc(msg)}</div>` : ""}
<div class="grid2" style="margin-top:20px;grid-template-columns:1fr 1fr">
<form method="post" class="card" style="padding:24px">
 <div class="o" style="font-size:18px">Ativação</div>
 <div class="row" style="gap:12px;margin-top:16px"><div style="flex:1"><label class="lab">PLANO</label><select class="in" name="plan">${["trial", "annual", "lifetime"].map((p) => `<option value="${p}" ${d.plan === p ? "selected" : ""}>${PLAN_LABEL[p]}</option>`).join("")}</select></div>
 <div style="flex:1"><label class="lab">VENCIMENTO (ANUAL)</label><input class="in" type="date" name="expires" value="${dt(d.expires_at)}"></div></div>
 <div style="margin-top:14px"><label class="lab">FIM DO TESTE</label><input class="in" type="date" name="trial" value="${dt(d.trial_until)}"></div>
 <div style="margin-top:14px"><label class="lab">CHAVE DO APARELHO</label><input class="in mono" name="dkey" value="${esc(d.dkey)}"></div>
 <div style="margin-top:14px"><label class="lab">OBSERVAÇÃO</label><input class="in" name="note" value="${esc(d.note)}" placeholder="Nome do cliente, telefone…"></div>
 <label class="row" style="gap:10px;margin-top:16px;color:#cfc8ea"><input type="checkbox" name="blocked" value="1" ${d.blocked ? "checked" : ""}> Bloquear este aparelho</label>
 <div class="row" style="gap:10px;margin-top:20px;flex-wrap:wrap"><button class="btn" name="do" value="save">Salvar</button><button class="btn g" name="do" value="year">+1 ano</button><button class="btn g" name="do" value="life">Vitalício</button><button class="btn g red" name="do" value="delete" onclick="return confirm('Apagar aparelho e listas?')">Apagar</button></div>
</form>
<div class="card" style="padding:24px"><div class="o" style="font-size:18px">Informações</div>
 ${[["Plataforma", d.platform], ["Modelo", d.model], ["Cadastrado", fmtDateTime(d.created_at)], ["Último acesso", fmtDateTime(d.last_seen)]].map(([k, v]) => `<div class="row" style="padding:8px 0;border-bottom:1px solid #191528"><span class="dim" style="width:140px">${k}</span>${esc(v || "—")}</div>`).join("")}
 <div class="o" style="font-size:16px;margin-top:20px">Listas (${lists.length})</div>
 ${lists.map((l) => `<div style="padding:10px 0;border-bottom:1px solid #191528"><b>${esc(l.name)}</b> <span class="dim">· ${l.type === "xtream" ? esc(l.server) + " · " + esc(l.username) : esc(l.url)}</span></div>`).join("") || '<div class="dim" style="padding:10px 0">Nenhuma</div>'}
 <div class="o" style="font-size:16px;margin-top:20px">Pagamentos</div>
 ${pays.map((p) => `<div class="row" style="padding:10px 0;border-bottom:1px solid #191528;gap:10px"><span>${fmtDateTime(p.created_at)}</span><span>${PLAN_LABEL[p.plan]}</span><span>${money(p.amount)}</span><span class="chip ${p.status === "approved" ? "st-active" : "st-blocked"}" style="margin-left:auto">${p.status === "approved" ? "Pago" : p.status === "pending" ? "Pendente" : "Cancelado"}</span></div>`).join("") || '<div class="dim" style="padding:10px 0">Nenhum</div>'}
</div></div>`;
  return html(page(d.mac, adminLayout("/admin/aparelhos", "", body), site(env)));
}

export async function deviceSave(env, req, macIn) {
  const mac = normMac(macIn);
  const f = await req.formData();
  const act = f.get("do");
  if (act === "delete") {
    await env.DB.batch([env.DB.prepare("DELETE FROM playlists WHERE mac=?").bind(mac), env.DB.prepare("DELETE FROM devices WHERE mac=?").bind(mac)]);
    return redirect("/admin/aparelhos");
  }
  const d = await env.DB.prepare("SELECT * FROM devices WHERE mac=?").bind(mac).first();
  if (!d) return redirect("/admin/aparelhos");
  const day = (v) => (v ? Math.floor(new Date(v + "T23:59:59-03:00").getTime() / 1000) : null);
  let plan = String(f.get("plan") || d.plan), expires = day(f.get("expires")), trial = day(f.get("trial")) || d.trial_until;
  if (act === "year") { plan = "annual"; expires = (d.plan === "annual" && d.expires_at > now() ? d.expires_at : now()) + 365 * DAY; }
  if (act === "life") { plan = "lifetime"; expires = null; }
  if (plan === "annual" && !expires) expires = now() + 365 * DAY;
  await env.DB.prepare("UPDATE devices SET plan=?, expires_at=?, trial_until=?, dkey=?, note=?, blocked=? WHERE mac=?")
    .bind(plan, expires, trial, String(f.get("dkey") || d.dkey).replace(/\D/g, "") || d.dkey, String(f.get("note") || "").slice(0, 200), f.get("blocked") ? 1 : 0, mac).run();
  if (act === "year" || act === "life") await env.DB.prepare("INSERT INTO payments (mac, plan, amount, status, created_at, paid_at, mp_id) VALUES (?,?,0,'approved',?,?, 'manual')").bind(mac, plan, now(), now()).run();
  return deviceEdit(env, mac, "Alterações salvas.");
}

export async function deviceNew(env, req) {
  const f = await req.formData();
  const mac = normMac(f.get("mac"));
  if (!mac) return redirect("/admin/aparelhos");
  const s = await settings(env);
  const key = String(Math.floor(100000 + Math.random() * 900000));
  await env.DB.prepare("INSERT OR IGNORE INTO devices (mac, dkey, platform, created_at, trial_until, plan) VALUES (?,?, 'manual', ?, ?, 'trial')").bind(mac, key, now(), now() + Number(s.trial_days) * DAY).run();
  return redirect("/admin/aparelho/" + encodeURIComponent(mac));
}

export async function payments(env) {
  const { results } = await env.DB.prepare("SELECT * FROM payments ORDER BY id DESC LIMIT 200").all();
  const body = `<h1 style="font-size:26px">Pagamentos</h1><div class="card" style="margin-top:22px;overflow:auto"><table><tr><th>DATA</th><th>MAC</th><th>PLANO</th><th>VALOR</th><th>STATUS</th><th class="hide-m">MERCADO PAGO</th></tr>
${results.map((p) => `<tr><td>${fmtDateTime(p.created_at)}</td><td class="mono"><a href="/admin/aparelho/${encodeURIComponent(p.mac)}">${esc(p.mac)}</a></td><td>${PLAN_LABEL[p.plan]}</td><td>${money(p.amount)}</td><td><span class="chip ${p.status === "approved" ? "st-active" : p.status === "pending" ? "st-trial" : "st-blocked"}">${p.status === "approved" ? "Pago" : p.status === "pending" ? "Pendente" : "Cancelado"}</span></td><td class="dim hide-m">${esc(p.mp_id || "—")}</td></tr>`).join("")}
${results.length ? "" : '<tr><td colspan="6" class="dim" style="text-align:center;padding:30px">Nenhum pagamento ainda</td></tr>'}</table></div>`;
  return html(page("Pagamentos", adminLayout("/admin/pagamentos", "", body), site(env)));
}

export async function config(env, msg = "") {
  const s = await settings(env);
  const fld = (k, t, help = "") => `<div style="margin-top:14px"><label class="lab">${t}</label><input class="in" name="${k}" value="${esc(s[k])}">${help ? `<div class="dim" style="font-size:12px;margin-top:6px">${help}</div>` : ""}</div>`;
  const body = `<h1 style="font-size:26px">Configurações</h1>${msg ? `<div class="alert ok" style="margin-top:16px">${esc(msg)}</div>` : ""}
<div class="grid2" style="margin-top:22px;grid-template-columns:1fr 1fr"><form method="post" class="card" style="padding:24px"><div class="o" style="font-size:18px">Planos</div>
${fld("trial_days", "DIAS DE TESTE GRÁTIS")}${fld("price_annual", "PREÇO ANUAL (R$)")}${fld("price_lifetime", "PREÇO VITALÍCIO (R$)")}${fld("max_lists", "LISTAS POR APARELHO (ANUAL/TESTE)")}${fld("payer_email", "E-MAIL PARA O PIX", "E-mail enviado ao Mercado Pago como pagador (pode ser o seu).")}${fld("support", "CONTATO DE SUPORTE", "Ex.: WhatsApp. Aparece no app quando o aparelho vence.")}
<button class="btn" style="margin-top:20px">Salvar</button></form>
<div class="card" style="padding:24px"><div class="o" style="font-size:18px">Pix (Mercado Pago)</div>
<p class="mut" style="margin-top:10px;line-height:1.6">${env.MP_ACCESS_TOKEN ? `<span class="chip st-active">Configurado</span>` : `<span class="chip st-expired">Não configurado</span>`}</p>
<p class="mut" style="margin-top:10px;line-height:1.6">O token de acesso fica guardado como segredo no servidor, fora do código. Para configurar: no Mercado Pago vá em <b>Seu negócio → Configurações → Credenciais</b>, copie o <b>Access Token de produção</b> e rode <span class="mono">npx wrangler secret put MP_ACCESS_TOKEN</span>.</p></div></div>`;
  return html(page("Configurações", adminLayout("/admin/config", "", body), site(env)));
}

export async function configSave(env, req) {
  const f = await req.formData();
  const stmts = Object.keys(DEFAULTS).map((k) => env.DB.prepare("INSERT INTO settings (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v").bind(k, k.startsWith("price") ? String(f.get(k) ?? DEFAULTS[k]).trim().replace(",", ".") : String(f.get(k) ?? DEFAULTS[k]).trim()));
  await env.DB.batch(stmts);
  return config(env, "Configurações salvas.");
}

export { applyPayment };
