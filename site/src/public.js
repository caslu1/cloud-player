// Páginas do cliente: início (MAC + chave), meu aparelho (listas) e pagamento Pix.
import { esc, html, redirect, json, now, DAY, normMac, normKey, fmtDate, money, makeCookie, clearCookie, readCookie, settings, deviceStatus, STATUS_LABEL } from "./util.js";
import { page, publicNav, ic } from "./ui.js";
import { createPix, getPayment } from "./mp.js";

const site = (env) => ({ siteName: env.SITE_NAME });

export async function currentDevice(env, req) {
  const mac = await readCookie(env, req, "dev");
  if (!mac) return null;
  return env.DB.prepare("SELECT * FROM devices WHERE mac = ?").bind(mac).first();
}

function plansHtml(s) {
  const plans = [
    ["Teste grátis", `${s.trial_days} dias`, "Ao abrir o app pela primeira vez", false, ["Todas as funções", "Sem cartão"]],
    ["Anual", money(s.price_annual), "por aparelho / ano", true, ["1 aparelho", `Até ${s.max_lists} listas`, "Atualizações incluídas"]],
    ["Vitalício", money(s.price_lifetime), "pagamento único", false, ["1 aparelho", "Listas ilimitadas", "Para sempre"]],
  ];
  return plans.map(([t, p, sub, hl, fs]) => `<div class="card plan ${hl ? "hl" : ""}" style="padding:22px">
<div class="row"><div class="o" style="font-size:16px">${t}</div>${hl ? '<span class="chip" style="background:var(--accent);margin-left:auto">Mais vendido</span>' : ""}</div>
<div class="o" style="font-size:28px;margin-top:10px">${p}</div><div class="dim" style="font-size:12px">${sub}</div>
<div style="margin-top:14px;display:grid;gap:7px">${fs.map((f) => `<div class="row" style="gap:8px;font-size:13px;color:#cfc8ea">${ic("check", 15, "#7ee2a8")}${f}</div>`).join("")}</div></div>`).join("");
}

export async function home(env, req, msg = "", prefill = {}) {
  const s = await settings(env);
  const logged = !!(await currentDevice(env, req));
  const body = `${publicNav("/", logged)}
<div class="wrap" style="padding-top:54px;padding-bottom:60px">
<div class="grid2">
 <div style="padding-top:10px">
  <span class="chip" style="background:var(--soft);color:#c9bbff">Android TV · Fire TV · TV Box · Roku</span>
  <h1 style="font-size:clamp(34px,5vw,52px);line-height:1.08;margin-top:18px">Ative seu aparelho e adicione sua lista em segundos</h1>
  <p class="mut" style="font-size:17px;line-height:1.6;margin-top:16px">Abra o ${esc(env.SITE_NAME)} na TV, anote o <b style="color:#fff">MAC</b> e a <b style="color:#fff">chave do aparelho</b> que aparecem na tela e informe aqui — ou aponte a câmera do celular para o QR code da TV.</p>
  <div class="row" style="gap:22px;margin-top:30px;flex-wrap:wrap">${[[1, "Informe MAC e chave"], [2, "Adicione sua lista"], [3, "A TV atualiza sozinha"]].map(([n, t]) => `<div class="row" style="gap:10px"><div class="o" style="width:34px;height:34px;border-radius:50%;background:var(--soft);color:var(--accent2);display:flex;align-items:center;justify-content:center">${n}</div><div style="font-size:13px;color:#cfc8ea;width:130px">${t}</div></div>`).join("")}</div>
 </div>
 <form id="entrar" class="card" style="padding:32px" method="post" action="/entrar">
  <div class="o" style="font-size:22px">Acessar meu aparelho</div><div class="mut" style="font-size:13px;margin-top:4px">Os dados aparecem na tela de login do app.</div>
  ${msg ? `<div class="alert err" style="margin:16px 0 0">${esc(msg)}</div>` : ""}
  <div style="margin-top:22px"><label class="lab" for="mac">ENDEREÇO MAC</label><input class="in mono" id="mac" name="mac" placeholder="00:1A:79:XX:XX:XX" value="${esc(prefill.mac || "")}" autocomplete="off" required></div>
  <div style="margin-top:16px"><label class="lab" for="key">CHAVE DO APARELHO</label><input class="in mono" id="key" name="key" inputmode="numeric" placeholder="000000" value="${esc(prefill.key || "")}" autocomplete="off" required style="letter-spacing:3px"></div>
  <button class="btn full" style="margin-top:22px">Entrar ${ic("play", 14, "#fff")}</button>
  <div class="row" style="gap:8px;margin-top:16px;font-size:12px;color:var(--dim)">${ic("lock", 14, "#6d6788")}Sua lista fica protegida e só aparece no seu aparelho.</div>
 </form>
</div>
<div id="planos" style="margin-top:56px"><div class="o" style="font-size:20px;margin-bottom:14px">Planos</div><div class="plans">${plansHtml(s)}</div></div>
</div>`;
  return html(page("Ativar aparelho", body, site(env)));
}

export async function login(env, req) {
  const f = await req.formData();
  return doLogin(env, f.get("mac"), f.get("key"));
}

export async function doLogin(env, macIn, keyIn) {
  const mac = normMac(macIn), key = normKey(keyIn);
  if (!mac || !key) return home(env, new Request("https://x"), "Confira o MAC e a chave do aparelho.", { mac: macIn, key: keyIn });
  const d = await env.DB.prepare("SELECT * FROM devices WHERE mac = ?").bind(mac).first();
  if (!d || d.dkey !== key) return home(env, new Request("https://x"), "Aparelho não encontrado. Abra o app na TV uma vez e confira os dados.", { mac: macIn, key: keyIn });
  return redirect("/aparelho", { "set-cookie": await makeCookie(env, "dev", mac) });
}

export const logout = () => redirect("/", { "set-cookie": clearCookie("dev") });

function statusBox(d, st) {
  const t = now();
  let line = "";
  if (st === "trial") { const days = Math.max(0, Math.ceil((d.trial_until - t) / DAY)); line = `vence em ${days} dia${days === 1 ? "" : "s"} (${fmtDate(d.trial_until)})`; }
  else if (st === "active") line = d.plan === "lifetime" ? "Vitalício" : `Anual · até ${fmtDate(d.expires_at)}`;
  else if (st === "expired") line = "o app está bloqueado até a ativação";
  else if (st === "blocked") line = "fale com o suporte";
  const cta = st === "active" && d.plan === "lifetime" ? "" : st === "blocked" ? "" : `<a class="btn" href="/pagar" style="padding:0 18px">${st === "active" ? "Renovar" : "Ativar agora"}</a>`;
  return `<div class="card row" style="padding:14px 20px;gap:16px;flex-wrap:wrap"><div><div class="dim" style="font-size:12px">STATUS</div><div class="row" style="gap:8px;margin-top:3px;flex-wrap:wrap"><span class="chip st-${st}">${STATUS_LABEL[st]}</span><span style="font-size:13px;color:#cfc8ea">${line}</span></div></div>${cta}</div>`;
}

export async function device(env, req, msg = null) {
  const d = await currentDevice(env, req);
  if (!d) return redirect("/#entrar");
  const s = await settings(env);
  const st = deviceStatus(d);
  const { results: lists } = await env.DB.prepare("SELECT * FROM playlists WHERE mac = ? ORDER BY position, id").bind(d.mac).all();
  const url = new URL(req.url);
  const editId = Number(url.searchParams.get("editar") || 0);
  const ed = lists.find((l) => l.id === editId) || null;
  const max = d.plan === "lifetime" ? 999 : Number(s.max_lists);
  const rows = lists.map((l, i) => `<div class="row" style="gap:14px;padding:16px;border-radius:12px;background:var(--field);border:1px solid ${i === 0 ? "var(--accent)" : "var(--line)"};margin-bottom:10px;flex-wrap:wrap">
<div style="width:40px;height:40px;border-radius:10px;background:var(--soft);display:flex;align-items:center;justify-content:center">${ic("list", 20, "#9b84ff")}</div>
<div style="flex:1;min-width:180px"><div class="row" style="gap:8px"><b style="font-size:15px">${esc(l.name)}</b>${i === 0 ? '<span class="chip st-active">Em uso</span>' : ""}${l.pin ? `<span class="chip" style="background:#16121f;color:var(--mut)">${ic("lock", 12)} PIN</span>` : ""}</div>
<div class="dim" style="font-size:12.5px;margin-top:2px;word-break:break-all">${l.type === "xtream" ? "Xtream Codes · " + esc(l.server) : "M3U · " + esc(l.url)}</div></div>
${i > 0 ? `<form method="post" action="/aparelho/lista/${l.id}/usar"><button class="btn g sm">Usar esta</button></form>` : ""}
<a class="btn g sm" href="/aparelho?editar=${l.id}#form">Editar</a>
<form method="post" action="/aparelho/lista/${l.id}/remover" onsubmit="return confirm('Remover esta lista?')"><button class="btn g sm red">Remover</button></form></div>`).join("");
  const v = (k) => esc(ed ? ed[k] : "");
  const isM3u = ed && ed.type === "m3u";
  const body = `${publicNav("/aparelho", true)}
<div class="wrap" style="padding-top:40px;padding-bottom:60px">
 <div class="row" style="gap:20px;flex-wrap:wrap"><div style="flex:1;min-width:260px"><h1 style="font-size:30px">Meu aparelho</h1><div class="mut" style="margin-top:4px">MAC <span class="mono">${esc(d.mac)}</span>${d.model ? " · " + esc(d.model) : ""}</div></div>${statusBox(d, st)}</div>
 ${msg ? `<div class="alert ${msg.ok ? "ok" : "err"}" style="margin-top:20px">${esc(msg.text)}</div>` : ""}
 <div class="grid2" style="margin-top:24px;grid-template-columns:1fr 520px">
  <div class="card" style="padding:26px;align-self:start"><div class="row"><div class="o" style="font-size:18px">Minhas listas</div><span class="dim" style="font-size:12px;margin-left:auto">${lists.length} de ${max === 999 ? "∞" : max}</span></div>
   <div style="margin-top:16px">${rows || `<div class="mut" style="padding:20px 0">Nenhuma lista ainda. Adicione a sua ao lado — ela aparece na TV em alguns segundos.</div>`}</div></div>
  <form id="form" class="card" style="padding:26px" method="post" action="/aparelho/lista">
   <div class="o" style="font-size:18px">${ed ? "Editar lista" : "Adicionar lista"}</div>
   ${ed ? `<input type="hidden" name="id" value="${ed.id}">` : ""}
   <div class="tabs" style="margin-top:16px"><input type="radio" name="type" id="tx" value="xtream" ${isM3u ? "" : "checked"}><label for="tx">Xtream Codes</label><input type="radio" name="type" id="tm" value="m3u" ${isM3u ? "checked" : ""}><label for="tm">Link M3U</label></div>
   <div style="margin-top:16px"><label class="lab">NOME DA LISTA</label><input class="in" name="name" value="${v("name")}" placeholder="Minha IPTV" required></div>
   <div class="fx"><div style="margin-top:14px"><label class="lab">SERVIDOR (URL)</label><input class="in" name="server" value="${v("server")}" placeholder="http://servidor.com:8080"></div>
   <div class="row" style="gap:12px;margin-top:14px"><div style="flex:1"><label class="lab">USUÁRIO</label><input class="in" name="username" value="${v("username")}"></div><div style="flex:1"><label class="lab">SENHA</label><input class="in" name="password" type="password" value="${v("password")}"></div></div></div>
   <div class="fm"><div style="margin-top:14px"><label class="lab">LINK M3U</label><input class="in" name="url" value="${v("url")}" placeholder="http://servidor.com/get.php?username=...&password=..."></div></div>
   <div style="margin-top:14px"><label class="lab">PIN PARA PROTEGER (OPCIONAL)</label><input class="in mono" name="pin" inputmode="numeric" maxlength="6" value="${v("pin")}" placeholder="Deixe vazio para não pedir"></div>
   <button class="btn full" style="margin-top:20px">${ed ? "Salvar alterações" : "Salvar lista"}</button>
   ${ed ? `<a class="btn g full" style="margin-top:10px" href="/aparelho">Cancelar</a>` : ""}
  </form>
 </div>
</div>
<style>.fm{display:none} form:has(#tm:checked) .fx{display:none} form:has(#tm:checked) .fm{display:block}</style>`;
  return html(page("Meu aparelho", body, site(env)));
}

// Link M3U do tipo get.php vira Xtream (mesma lógica do app)
function m3uToXtream(u) {
  try {
    const x = new URL(u);
    const user = x.searchParams.get("username"), pass = x.searchParams.get("password");
    if (user && pass && /get\.php$/i.test(x.pathname)) return { server: x.origin, username: user, password: pass };
  } catch {}
  return null;
}

export async function saveList(env, req) {
  const d = await currentDevice(env, req);
  if (!d) return redirect("/#entrar");
  const f = await req.formData();
  const s = await settings(env);
  const id = Number(f.get("id") || 0);
  let type = f.get("type") === "m3u" ? "m3u" : "xtream";
  let server = String(f.get("server") || "").trim().replace(/\/+$/, ""), username = String(f.get("username") || "").trim(), password = String(f.get("password") || "").trim();
  const url = String(f.get("url") || "").trim(), name = String(f.get("name") || "").trim().slice(0, 60) || "Minha lista";
  const pin = String(f.get("pin") || "").replace(/\D/g, "").slice(0, 6);
  if (type === "m3u") {
    if (!/^https?:\/\//i.test(url)) return device(env, req, { ok: false, text: "Informe um link M3U válido (começando com http)." });
    const xt = m3uToXtream(url);
    if (xt) { type = "xtream"; ({ server, username, password } = xt); }
  } else {
    if (!server || !username || !password) return device(env, req, { ok: false, text: "Preencha servidor, usuário e senha." });
    if (!/^https?:\/\//i.test(server)) server = "http://" + server;
  }
  if (id) {
    await env.DB.prepare("UPDATE playlists SET name=?, type=?, server=?, username=?, password=?, url=?, pin=? WHERE id=? AND mac=?").bind(name, type, server, username, password, type === "m3u" ? url : "", pin, id, d.mac).run();
  } else {
    const { n } = await env.DB.prepare("SELECT COUNT(*) n FROM playlists WHERE mac = ?").bind(d.mac).first();
    const max = d.plan === "lifetime" ? 999 : Number(s.max_lists);
    if (n >= max) return device(env, req, { ok: false, text: `Limite de ${max} listas atingido.` });
    await env.DB.prepare("INSERT INTO playlists (mac, name, type, server, username, password, url, pin, position, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)")
      .bind(d.mac, name, type, server, username, password, type === "m3u" ? url : "", pin, n, now()).run();
  }
  return redirect("/aparelho?ok=1");
}

export async function listAction(env, req, id, action) {
  const d = await currentDevice(env, req);
  if (!d) return redirect("/#entrar");
  if (action === "remover") await env.DB.prepare("DELETE FROM playlists WHERE id=? AND mac=?").bind(id, d.mac).run();
  if (action === "usar") {
    await env.DB.prepare("UPDATE playlists SET position = position + 1 WHERE mac=?").bind(d.mac).run();
    await env.DB.prepare("UPDATE playlists SET position = 0 WHERE id=? AND mac=?").bind(id, d.mac).run();
  }
  return redirect("/aparelho");
}

// --- Pix ---
export async function payPage(env, req) {
  const d = await currentDevice(env, req);
  if (!d) return redirect("/#entrar");
  const s = await settings(env);
  const url = new URL(req.url);
  const plan = url.searchParams.get("plano") === "lifetime" ? "lifetime" : "annual";
  const amount = plan === "lifetime" ? s.price_lifetime : s.price_annual;
  let pay = null, err = "";
  if (url.searchParams.get("gerar")) {
    if (!env.MP_ACCESS_TOKEN) err = "O pagamento por Pix ainda não foi configurado. Fale com o suporte para ativar.";
    else {
      const r = await env.DB.prepare("INSERT INTO payments (mac, plan, amount, status, created_at) VALUES (?,?,?, 'pending', ?)").bind(d.mac, plan, Number(amount), now()).run();
      const id = r.meta.last_row_id;
      try {
        const email = s.payer_email || `cliente+${d.mac.replace(/:/g, "").toLowerCase()}@orbitaplay.app`;
        const mp = await createPix(env, { amount, description: `${env.SITE_NAME} – plano ${plan === "lifetime" ? "vitalício" : "anual"} (${d.mac})`, email, reference: id, notifyUrl: `${url.origin}/webhook/mercadopago` });
        await env.DB.prepare("UPDATE payments SET mp_id=?, qr=?, qr_b64=? WHERE id=?").bind(mp.id, mp.qr, mp.qrB64, id).run();
        pay = { id, qr: mp.qr, qrB64: mp.qrB64 };
      } catch (e) { err = "Não foi possível gerar o Pix agora: " + e.message; await env.DB.prepare("UPDATE payments SET status='cancelled' WHERE id=?").bind(id).run(); }
    }
  }
  const opt = (p, t, sub, v) => `<a href="/pagar?plano=${p}" class="row" style="gap:12px;padding:16px;border-radius:12px;border:1px solid ${plan === p ? "var(--accent)" : "var(--line)"};background:${plan === p ? "#16112c" : "transparent"}">
<div style="width:18px;height:18px;border-radius:50%;${plan === p ? "border:5px solid var(--accent);background:#fff" : "border:2px solid var(--dim)"}"></div><div style="flex:1"><b>${t}</b><div class="dim" style="font-size:12px">${sub}</div></div><b class="o" style="font-size:18px">${money(v)}</b></a>`;
  const right = pay ? `<div class="row" style="gap:8px;justify-content:center;font-weight:600">${ic("pix", 18, "#32bcad")}Pague com Pix</div>
<img src="data:image/png;base64,${pay.qrB64}" alt="QR code Pix" style="display:block;margin:16px auto 0;width:250px;height:250px;padding:12px;background:#fff;border-radius:14px">
<div class="in mono row" style="margin-top:16px;font-size:12px;justify-content:space-between;gap:10px"><span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap" id="pc">${esc(pay.qr)}</span><a href="#" style="color:var(--accent2);font-family:Inter;font-weight:600" onclick="navigator.clipboard.writeText(document.getElementById('pc').textContent);this.textContent='Copiado';return false">Copiar</a></div>
<div id="ps" class="row" style="gap:10px;justify-content:center;margin-top:16px;font-size:13px;color:var(--warn)"><span style="width:10px;height:10px;border-radius:50%;background:var(--warn)"></span>Aguardando pagamento…</div>
<script>setInterval(async()=>{const r=await fetch('/api/pagamento/${pay.id}');const j=await r.json();if(j.status==='approved'){location.href='/aparelho?pago=1'}},4000)</script>`
    : `<div class="row" style="gap:8px;justify-content:center;font-weight:600">${ic("pix", 18, "#32bcad")}Pagamento por Pix</div>
<p class="mut" style="text-align:center;margin:16px 0;font-size:13px;line-height:1.6">Escolha o plano e gere o código. A ativação é automática quando o pagamento for confirmado.</p>
${err ? `<div class="alert err">${esc(err)}</div>` : ""}<a class="btn full" href="/pagar?plano=${plan}&gerar=1">Gerar Pix de ${money(amount)}</a>`;
  const body = `${publicNav("/pagar", true)}<div class="wrap" style="padding:40px 24px 60px"><div class="card" style="max-width:920px;margin:0 auto;padding:34px;display:flex;gap:34px;flex-wrap:wrap">
<div style="flex:1;min-width:260px"><h1 style="font-size:26px">Ativar aparelho</h1><div class="mut mono" style="margin-top:4px">MAC ${esc(d.mac)}</div>
<div style="margin-top:22px;display:grid;gap:10px">${opt("annual", "Anual", "1 ano de uso neste aparelho", s.price_annual)}${opt("lifetime", "Vitalício", "Pagamento único", s.price_lifetime)}</div>
<div class="row" style="gap:10px;margin-top:22px;font-size:13px;color:#cfc8ea">${ic("check", 16, "#7ee2a8")}A ativação é automática assim que o Pix for confirmado.</div>
<div class="row" style="gap:10px;margin-top:8px;font-size:13px;color:#cfc8ea">${ic("check", 16, "#7ee2a8")}A TV libera sozinha em alguns segundos.</div></div>
<div style="width:340px;max-width:100%">${right}</div></div></div>`;
  return html(page("Pagamento", body, site(env)));
}

// Aplica o pagamento aprovado ao aparelho
export async function applyPayment(env, p) {
  if (!p || p.status === "approved") return;
  const d = await env.DB.prepare("SELECT * FROM devices WHERE mac=?").bind(p.mac).first();
  if (!d) return;
  if (p.plan === "lifetime") await env.DB.prepare("UPDATE devices SET plan='lifetime', expires_at=NULL WHERE mac=?").bind(p.mac).run();
  else {
    const base = d.plan === "annual" && d.expires_at > now() ? d.expires_at : now();
    await env.DB.prepare("UPDATE devices SET plan='annual', expires_at=? WHERE mac=?").bind(base + 365 * DAY, p.mac).run();
  }
  await env.DB.prepare("UPDATE payments SET status='approved', paid_at=? WHERE id=?").bind(now(), p.id).run();
}

export async function syncPayment(env, p) {
  if (!p || p.status !== "pending" || !p.mp_id || !env.MP_ACCESS_TOKEN) return p?.status;
  const mp = await getPayment(env, p.mp_id);
  if (mp?.status === "approved") { await applyPayment(env, p); return "approved"; }
  if (mp && ["cancelled", "rejected", "expired"].includes(mp.status)) { await env.DB.prepare("UPDATE payments SET status=? WHERE id=?").bind(mp.status === "rejected" ? "cancelled" : mp.status, p.id).run(); return mp.status; }
  return "pending";
}

export async function paymentStatus(env, req, id) {
  const d = await currentDevice(env, req);
  if (!d) return json({ error: "auth" }, 401);
  const p = await env.DB.prepare("SELECT * FROM payments WHERE id=? AND mac=?").bind(id, d.mac).first();
  return json({ status: await syncPayment(env, p) });
}

export async function webhook(env, req) {
  let id = new URL(req.url).searchParams.get("data.id");
  try { const b = await req.json(); id = b?.data?.id || id; } catch {}
  if (id) {
    const p = await env.DB.prepare("SELECT * FROM payments WHERE mp_id=?").bind(String(id)).first();
    if (p) await syncPayment(env, p);
  }
  return new Response("ok");
}

export async function help(env, req) {
  const logged = !!(await currentDevice(env, req));
  const q = [["Onde encontro o MAC e a chave?", "Na tela de login do app na TV. Você também pode apontar a câmera do celular para o QR code que aparece lá."],
    ["Adicionei a lista, quando aparece na TV?", "Em alguns segundos. O app consulta o site sozinho enquanto está na tela de login."],
    ["Como funciona o teste grátis?", "Ao abrir o app pela primeira vez o aparelho ganha dias de teste com todas as funções."],
    ["Paguei, e agora?", "A ativação é automática assim que o Pix é confirmado. Feche e abra o app se ele já estava aberto."],
    ["O app fornece canais?", "Não. O app é um player: você usa a lista do seu provedor, da qual tem direito de uso."]];
  const body = `${publicNav("/ajuda", logged)}<div class="wrap" style="padding:40px 24px 60px;max-width:820px"><h1 style="font-size:30px">Ajuda</h1>
${q.map(([a, b]) => `<div class="card" style="padding:20px;margin-top:14px"><b>${a}</b><p class="mut" style="margin-top:6px;line-height:1.6">${b}</p></div>`).join("")}</div>`;
  return html(page("Ajuda", body, site(env)));
}
