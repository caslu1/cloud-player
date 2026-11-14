// Orbita Play – site de ativação (cliente + admin + API dos apps)
import { html, redirect, json } from "./util.js";
import * as pub from "./public.js";
import * as adm from "./admin.js";
import * as app from "./app.js";
import { IMGS } from "./imgs.js";

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const p = url.pathname.replace(/\/+$/, "") || "/";
    const m = req.method;
    // painel admin separado do site: roda em outro Worker/endereço (ROLE=panel); o site não tem /admin
    const isPanel = env.ROLE === "panel";
    if (isPanel && !p.startsWith("/admin")) return Response.redirect(`${url.origin}/admin`, 302);
    if (!isPanel && p.startsWith("/admin")) return new Response("Não encontrado", { status: 404 });
    try {
      if (m === "OPTIONS") return new Response(null, { headers: { "access-control-allow-origin": "*", "access-control-allow-headers": "content-type", "access-control-allow-methods": "GET,POST" } });
      // API dos apps
      if (p === "/api/app/register" && m === "POST") return app.register(env, req);
      if (p === "/api/app/status") return app.status(env, req);
      if (p === "/api/qr") return app.qr(req);
      if (p === "/webhook/mercadopago") return pub.webhook(env, req);
      let r;
      if ((r = p.match(/^\/api\/pagamento\/(\d+)$/))) return pub.paymentStatus(env, req, Number(r[1]));
      // cliente
      if (p === "/" && m === "GET") return pub.home(env, req);
      if (p.startsWith("/img/")) {
        const b64 = IMGS[p.slice(5)];
        if (!b64) return new Response("not found", { status: 404 });
        const bin = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
        return new Response(bin, { headers: { "content-type": "image/webp", "cache-control": "public, max-age=604800" } });
      }
      // atalho para o app Downloader (Fire TV / TV Box)
      if (p === "/download" || p === "/downloads") return pub.downloads(env, req);
      if (p === "/android" || p === "/apk") return Response.redirect(pub.APK_URL, 302);
      if (p === "/entrar" && m === "POST") return pub.login(env, req);
      if (p === "/ativar") return pub.doLogin(env, url.searchParams.get("mac"), url.searchParams.get("key"));
      if (p === "/sair") return pub.logout();
      if (p === "/ajuda") return pub.help(env, req);
      if (p === "/aparelho") return pub.device(env, req, url.searchParams.get("ok") ? { ok: true, text: "Lista salva. A TV atualiza em alguns segundos." } : url.searchParams.get("pago") ? { ok: true, text: "Pagamento confirmado! Seu aparelho está ativo." } : null);
      if (p === "/aparelho/lista" && m === "POST") return pub.saveList(env, req);
      if ((r = p.match(/^\/aparelho\/lista\/(\d+)\/(remover|usar)$/)) && m === "POST") return pub.listAction(env, req, Number(r[1]), r[2]);
      if (p === "/pagar") return pub.payPage(env, req);
      // admin
      if (p.startsWith("/admin")) {
        if (p === "/admin/entrar" && m === "POST") return adm.doLogin(env, req);
        if (p === "/admin/sair") return adm.logout();
        if (!(await adm.isAdmin(env, req))) return adm.loginPage(env);
        if (p === "/admin") return adm.overview(env);
        if (p === "/admin/aparelhos") return adm.devices(env, req);
        if (p === "/admin/aparelho/novo" && m === "POST") return adm.deviceNew(env, req);
        if ((r = p.match(/^\/admin\/aparelho\/([^/]+)$/))) return m === "POST" ? adm.deviceSave(env, req, decodeURIComponent(r[1])) : adm.deviceEdit(env, decodeURIComponent(r[1]));
        if (p === "/admin/pagamentos") return adm.payments(env);
        if (p === "/admin/config") return m === "POST" ? adm.configSave(env, req) : adm.config(env);
      }
      return html("<h1 style='font-family:sans-serif;color:#fff;background:#07060d;padding:40px'>Página não encontrada</h1>", 404);
    } catch (e) {
      console.error(e);
      if (p.startsWith("/api/")) return json({ error: "erro interno" }, 500);
      return html(`<pre style="color:#fff;background:#07060d;padding:40px">Erro: ${String(e.message).replace(/</g, "&lt;")}</pre>`, 500);
    }
  },
};
