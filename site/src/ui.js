// Visual do site (mesmo do protótipo aprovado): cores, fontes e ícones.
import { esc } from "./util.js";

const P = {
  check: "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
  lock: "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
  play: "M8 5v14l11-7z",
  list: "M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z",
  pix: "M12 2l4.5 4.5-4.5 4.5L7.5 6.5zM6.5 7.5L11 12l-4.5 4.5L2 12zm11 0L22 12l-4.5 4.5L13 12zM12 13l4.5 4.5L12 22l-4.5-4.5z",
  chart: "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z",
  dev: "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z",
  money: "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
  gear: "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
  search: "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
  logout: "M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z",
};
export const ic = (n, s = 16, c = "currentColor") => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" aria-hidden="true"><path fill="${c}" d="${P[n]}"/></svg>`;

const LOGO = `<svg width="34" height="34" viewBox="0 0 64 64" aria-hidden="true"><circle cx="30" cy="36" r="17" fill="none" stroke="#7c5cff" stroke-width="9"/><circle cx="30" cy="36" r="5" fill="#7c5cff"/><path d="M27 40 L58 6 L33 42 Z" fill="#22d3ee"/></svg>`;

export const CSS = `
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Outfit:wght@700&display=swap');
*{box-sizing:border-box;margin:0;padding:0}
:root{--bg:#07060d;--card:#100d1c;--line:#221d33;--line2:#2a2440;--field:#0a0814;--accent:#7c5cff;--accent2:#9b84ff;--soft:#1d1638;--text:#e6e1f5;--mut:#a59fc0;--dim:#6d6788;--ok:#7ee2a8;--warn:#ffc56b;--bad:#ff8095}
body{background:var(--bg);color:#fff;font-family:Inter,system-ui,sans-serif;font-size:14px;min-height:100vh}
body:before{content:"";position:fixed;inset:0;pointer-events:none;background:radial-gradient(ellipse at 75% 0%,rgba(124,92,255,.22),transparent 55%),radial-gradient(ellipse at 0% 100%,rgba(77,201,255,.07),transparent 50%)}
a{color:inherit;text-decoration:none}
.o{font-family:Outfit,Inter,sans-serif;font-weight:700}.mut{color:var(--mut)}.dim{color:var(--dim)}
.row{display:flex;align-items:center}.wrap{max-width:1312px;margin:0 auto;padding:0 24px;position:relative}
.card{background:var(--card);border:1px solid var(--line);border-radius:16px}
.in{width:100%;height:46px;border-radius:10px;background:var(--field);border:1px solid var(--line2);padding:0 14px;color:var(--text);font:inherit;font-size:15px;outline:none}
.in:focus{border-color:var(--accent)}
.mono{font-family:ui-monospace,Menlo,monospace;letter-spacing:1px}
.lab{display:block;font-size:12px;color:var(--mut);font-weight:600;margin:0 0 7px;letter-spacing:.3px}
.btn{height:46px;border-radius:10px;background:var(--accent);display:inline-flex;align-items:center;justify-content:center;gap:8px;font-weight:600;font-size:14px;color:#fff;border:0;padding:0 20px;cursor:pointer;font-family:inherit}
.btn:hover{filter:brightness(1.1)}.btn.g{background:transparent;border:1px solid var(--line2);color:#cfc8ea}.btn.red{color:var(--bad)}.btn.sm{height:36px;padding:0 14px;font-size:13px}
.btn.full{width:100%}
.chip{display:inline-flex;align-items:center;gap:6px;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:600;white-space:nowrap}
.st-active{background:#163a2a;color:var(--ok)}.st-trial{background:#3a2a10;color:var(--warn)}.st-expired{background:#3a1620;color:var(--bad)}.st-blocked{background:#22202c;color:var(--mut)}
.logo{display:flex;align-items:center;gap:10px;font-family:Outfit,Inter;font-weight:700;font-size:20px}.logo span{color:var(--accent2)}
nav.top{height:76px;border-bottom:1px solid #16121f;position:relative;z-index:2}
nav.top .links{display:flex;gap:30px;color:var(--mut);font-weight:500;margin-left:auto}nav.top .links a:hover,nav.top .links a.on{color:#fff}
.alert{padding:12px 16px;border-radius:10px;font-size:14px;margin-bottom:16px}.alert.err{background:#3a1620;color:#ffc2cc}.alert.ok{background:#163a2a;color:#c9f5dc}
h1{font-family:Outfit,Inter;font-weight:700}
.grid2{display:grid;grid-template-columns:1fr 520px;gap:40px}
.plans{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}
.plan.hl{border-color:var(--accent);box-shadow:0 0 0 1px var(--accent),0 20px 50px rgba(124,92,255,.25)}
.tabs{display:flex;gap:6px;padding:4px;background:var(--field);border-radius:10px;border:1px solid var(--line)}
.tabs label{flex:1;text-align:center;height:36px;line-height:36px;border-radius:8px;color:var(--mut);cursor:pointer;font-size:13px;font-weight:600}
.tabs input{display:none}.tabs input:checked+label{background:var(--accent);color:#fff}
table{width:100%;border-collapse:collapse}th{font-size:12px;color:var(--dim);font-weight:600;text-align:left;padding:12px 16px;border-bottom:1px solid var(--line)}
td{padding:14px 16px;border-bottom:1px solid #191528;font-size:13.5px;color:var(--text)}tr:last-child td{border-bottom:0}
.side{position:fixed;left:0;top:0;bottom:0;width:250px;background:#0b0915;border-right:1px solid #191528;padding:24px 16px}
.side a{display:flex;align-items:center;gap:12px;padding:11px 14px;border-radius:10px;margin-bottom:4px;color:var(--mut);font-weight:500}
.side a.on{background:var(--accent);color:#fff}.side a:hover:not(.on){background:#16121f}
.main{margin-left:250px;padding:28px 36px;position:relative}
.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}
@media (max-width:900px){
 .grid2,.plans,.stats{grid-template-columns:1fr !important}
 .grid2>*{min-width:0}
 nav.top .links{display:none}
 .side{position:static;width:auto;display:flex;flex-wrap:wrap;gap:4px;padding:12px}.side .logo,.side .sec{display:none}.side a{margin:0}
 .main{margin-left:0;padding:20px 16px}
 .hide-m{display:none}
 table{font-size:12px}td,th{padding:10px 8px}
 .wrap{padding:0 16px}
}`;

export function page(title, body, { siteName = "Orbita Play" } = {}) {
  return `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${esc(title)} · ${esc(siteName)}</title><meta name="theme-color" content="#07060d"><style>${CSS}</style></head><body>${body}</body></html>`;
}

export const logo = () => `<a href="/" class="logo">${LOGO}ORBITA <span>PLAY</span></a>`;

export function publicNav(active, logged) {
  const l = (h, t) => `<a href="${h}" class="${active === h ? "on" : ""}">${t}</a>`;
  return `<nav class="top"><div class="wrap row" style="height:76px">${logo()}
<div class="links">${l("/", "Ativar aparelho")}${l("/aparelho", "Minhas listas")}${l("/#planos", "Planos")}${l("/ajuda", "Ajuda")}</div>
${logged ? `<a class="btn g sm" style="margin-left:24px" href="/sair">${ic("logout", 15)} Sair</a>` : `<a class="btn sm" style="margin-left:24px" href="/#entrar">Gerenciar meu aparelho</a>`}</div></nav>`;
}

export function adminLayout(active, title, body) {
  const l = (h, i, t) => `<a href="${h}" class="${active === h ? "on" : ""}">${ic(i, 18)}${t}</a>`;
  return `<div class="side">${logo()}<div class="dim sec" style="font-size:11px;font-weight:600;letter-spacing:1.5px;margin:30px 0 10px 14px">PAINEL ADMIN</div>
${l("/admin", "chart", "Visão geral")}${l("/admin/aparelhos", "dev", "Aparelhos")}${l("/admin/pagamentos", "money", "Pagamentos")}${l("/admin/config", "gear", "Configurações")}${l("/admin/sair", "logout", "Sair")}</div>
<div class="main">${body}</div>`;
}
