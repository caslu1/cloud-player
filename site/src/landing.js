// Página inicial do site (vitrine do app + ativação + planos), pensada para celular e computador.
import { esc } from "./util.js";
import { WORDMARK } from "./brand.js";

export const APK_URL = "https://github.com/caslu1/cloud-player/releases/download/orbita-play/Orbita-Play.apk";

// ícones de linha simples (SVG) usados só na página inicial
const L = {
  tv: '<rect x="3" y="5" width="18" height="12" rx="2"/><path d="M8 21h8M12 17v4"/>',
  shield: '<path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2"/><path d="M11 18h2"/>',
  play: '<path d="M8 5v14l11-7z"/>',
  resume: '<path d="M5 12h12M13 6l6 6-6 6"/>',
  heart: '<path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"/>',
  cc: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M7 15h4M13 15h4M7 11h10"/>',
  lock: '<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
  list: '<path d="M4 6h16M4 12h16M4 18h9"/><path d="M16 15v6l5-3z"/>',
  guide: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M8 4v16"/>',
  search: '<circle cx="11" cy="11" r="6"/><path d="M20 20l-4.5-4.5"/>',
  grid: '<rect x="4" y="4" width="7" height="7" rx="1.5"/><rect x="13" y="4" width="7" height="7" rx="1.5"/><rect x="4" y="13" width="7" height="7" rx="1.5"/><rect x="13" y="13" width="7" height="7" rx="1.5"/>',
  box: '<rect x="3" y="7" width="18" height="11" rx="2"/><path d="M7 21h10"/><circle cx="17" cy="12.5" r="1"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v4h1"/>',
  warn: '<circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16.5h.01"/>',
};
const li = (n, s = 20) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${L[n]}</svg>`;
const icoBox = (n) => `<span class="ibox">${li(n)}</span>`;

// telas ilustrativas desenhadas (títulos fictícios, sem dados do aparelho)
const G = ["#2f7fd6,#173a8a", "#d64553,#6e1f2a", "#3fae5c,#1b4d2a", "#d9a12b,#7a4a12", "#9b55e8,#3d1a78", "#22a7a7,#0f4a52", "#e0702e,#6a2a10", "#5b6cf0,#24296e"];
const MOV = ["O Último Farol", "Cidade de Vidro", "Rota 47", "Verão em Lisboa", "Constelação", "Maré Alta", "Noite Azul", "Horizonte"];
const CH = ["Canal Notícias", "Esporte Total", "Cine Max", "Kids Mundo", "Música Hits", "Documentários", "Novelas", "Viagem & Cia"];
const poster = (i, t, sel) => `<div class="mp${sel ? " sel" : ""}" style="background:linear-gradient(160deg,${G[i % G.length]})"><span class="mpc"></span><b>${t}</b></div>`;
const mockTop = (active) => `<div class="mtop"><span class="mlogo">▶</span>${["Início", "Ao vivo", "Filmes", "Séries", "Minha lista"].map((t, i) => `<span class="${i === active ? "on" : ""}">${t}</span>`).join("")}</div>`;
const MOCKS = {
  inicio: () => `${mockTop(0)}<div class="mhero"><div><small>DESTAQUE</small><h4>Maré Alta</h4></div><span class="msun"></span></div>
<div class="mrow-t">Continuar assistindo</div><div class="mrow">${[0, 1, 2, 3, 4].map((i) => poster(i, MOV[i], i === 0)).join("")}</div>`,
  canais: () => `${mockTop(1)}<div class="mch"><div class="mcats">${["Favoritos", "Todos os canais", "Notícias", "Esportes", "Filmes", "Infantil"].map((t, i) => `<span class="${i === 1 ? "on" : ""}">${t}</span>`).join("")}</div>
<div class="mlist">${[0, 1, 2, 3, 4].map((i) => `<div class="mli${i === 1 ? " on" : ""}"><span class="mn">${101 + i}</span><span class="mlg" style="background:linear-gradient(160deg,${G[i]})"></span><div><b>${CH[i]}</b><small>Programa ao vivo</small><i><em style="width:${30 + i * 12}%"></em></i></div></div>`).join("")}</div>
<div class="mprev"><div class="mvid" style="background:linear-gradient(160deg,${G[1]})"><span class="mplay">▶</span></div><b>102 · ${CH[1]}</b><small>Agora · A seguir</small></div></div>`,
  filmes: () => `${mockTop(2)}<div class="mhero sm"><div><small>FILME</small><h4>Cidade de Vidro</h4></div><span class="msun v"></span></div>
<div class="mrow-t">Lançamentos</div><div class="mrow">${[1, 4, 2, 6, 3].map((i, k) => poster(i, MOV[i], k === 0)).join("")}</div>`,
};
const shot = (src, alt) => `<div class="tvf"><img src="/img/${src}.webp" alt="${esc(alt)}" loading="lazy" width="1600" height="900"></div>`;

export function landing(env, { formHtml, plansHtml, logged, host }) {
  const name = esc(env.SITE_NAME);
  const nav = `<header class="lnav"><div class="wrap row" style="height:72px">
<a href="/" class="logo"><img src="${WORDMARK}" alt="${name}" height="32"></a>
<nav class="lnk"><a href="#como">Como funciona</a><a href="#instalar">Instalar</a><a href="#planos">Planos</a><a href="/ajuda">Ajuda</a></nav>
${logged ? `<a class="btn sm" href="/aparelho">Minhas listas</a>` : `<a class="btn sm" href="#entrar">Ativar aparelho</a>`}
<details class="mmenu"><summary aria-label="Menu">${li("grid", 22)}</summary><div><a href="#como">Como funciona</a><a href="#instalar">Instalar</a><a href="#planos">Planos</a><a href="/ajuda">Ajuda</a>${logged ? `<a href="/sair">Sair</a>` : ""}</div></details>
</div></header>`;

  const hero = `<section class="hero"><div class="wrap hgrid">
<div>
 <div class="eyebrow">PLAYER DE MÍDIA PARA TV</div>
 <h1 class="h1">Sua lista na TV, <span class="hl">do seu jeito.</span></h1>
 <p class="lead">O ${name} é um player para Android TV, TV Box, Fire TV e Roku, feito para o controle remoto. Você adiciona a lista que já é sua e assiste com uma interface simples, rápida e bonita.</p>
 <div class="row wrapf" style="gap:12px;margin-top:26px"><a class="btn lg" href="#entrar">Ativar meu aparelho</a><a class="btn g lg" href="#como">Como funciona</a></div>
 <div class="note">${li("info", 18)}<div><b>O ${name} não inclui nenhum conteúdo.</b> Não fornece, não vende e não hospeda vídeos: só reproduz o que vem da lista que você mesmo adiciona.</div></div>
</div>
</div></section>
<section class="strip"><div class="wrap sgrid">
${[["tv", "Feito para TV", "Navegação rápida com o controle remoto."], ["shield", "Sem conteúdo próprio", "Você usa só a sua própria lista."], ["phone", "Configure pelo celular", "Leia o QR code e pronto."]]
  .map(([i, t, d]) => `<div class="row" style="gap:14px">${icoBox(i)}<div><b>${t}</b><div class="mut">${d}</div></div></div>`).join("")}
</div></section>`;

  const steps = `<section id="como" class="sec"><div class="wrap">
<div class="eyebrow">COMO FUNCIONA</div><h2 class="h2">Três passos e você já está assistindo</h2>
<p class="lead sm">Você só precisa da lista que recebeu do seu provedor: endereço, usuário e senha (ou o link da lista de reprodução).</p>
<div class="g3">
${[["Instale na sua TV", `Instale o ${name} no aparelho e abra o app. Na tela aparecem um QR code, o endereço MAC e a chave do dispositivo.`],
   ["Envie sua lista", "Leia o QR code com o celular ou entre neste site com o MAC e a chave e cadastre sua lista. Também dá para digitar direto na TV."],
   ["Assista", "A lista aparece na TV em segundos. Você pode guardar mais de uma playlist e trocar quando quiser."]]
  .map(([t, d], i) => `<div class="card step"><span class="num">${i + 1}</span><h3>${t}</h3><p class="mut">${d}</p></div>`).join("")}
</div></div></section>`;

  const screens = [["inicio", "Tela inicial", "Cinco temas para escolher: Vitrine, Padrão, Lite, Redondo e Quadrado."],
    ["canais", "Canais ao vivo", "Categorias, favoritos, recentes, prévia do canal e a programação de hoje."],
    ["filmes", "Filmes", "Capas grandes, notas, sinopse e busca por título ou categoria."]];
  const gallery = `<section id="telas" class="sec"><div class="wrap">
<div class="eyebrow">TELAS DO APP</div><h2 class="h2">Fácil de usar, direto do sofá</h2>
<div class="g2s">${screens.map(([s, t, d]) => `<div class="scard">${shot(s, t)}<h3>${t}</h3><p class="mut">${d}</p></div>`).join("")}</div>
<p class="dim" style="font-size:12px;margin-top:14px">Imagens ilustrativas do aplicativo · títulos fictícios.</p>
</div></section>`;

  const features = `<section id="recursos" class="sec"><div class="wrap">
<div class="eyebrow">RECURSOS</div><h2 class="h2">Tudo o que um bom player precisa</h2>
<div class="g3">
${[["resume", "Continuar assistindo", "Volte exatamente de onde parou."], ["heart", "Minha lista e favoritos", "Guarde filmes, séries e canais para achar rápido."],
   ["cc", "Áudio e legendas", "Escolha o idioma e a legenda de cada título."], ["lock", "Controle parental", "Bloqueie categorias adultas com senha."],
   ["list", "Várias playlists", "Mais de uma lista no mesmo aparelho, com nome."], ["guide", "Guia de programação", "Veja o que passa agora e a seguir em cada canal."],
   ["search", "Busca geral", "Ache canais, filmes e séries em um só lugar."], ["grid", "Temas e papéis de parede", "Deixe a tela inicial com a sua cara."],
   ["phone", "Ativação pelo celular", "Nada de digitar senha com o controle."]]
  .map(([i, t, d]) => `<div class="card feat">${icoBox(i)}<div><h3>${t}</h3><p class="mut">${d}</p></div></div>`).join("")}
</div></div></section>`;

  const install = `<section id="instalar" class="sec"><div class="wrap">
<div class="eyebrow">ONDE INSTALAR</div><h2 class="h2">Na sua TV e no seu TV Box</h2>
<p class="lead sm">Baixe direto no aparelho. No Fire TV e no TV Box, use o app <b>Downloader</b> e digite o endereço abaixo.</p>
<div class="g2">
 <div class="card inst"><div class="row" style="gap:14px">${icoBox("tv")}<div><h3>Android TV, Google TV e TV Box</h3><p class="mut">Também funciona no Fire TV Stick</p></div></div>
  <div class="row wrapf" style="gap:10px;margin-top:16px"><code class="addr">${esc(host)}/android</code><a class="btn" href="${APK_URL}">Baixar APK</a></div></div>
 <div class="card inst"><div class="row" style="gap:14px">${icoBox("box")}<div><h3>Roku</h3><p class="mut">Canal ${name} para Roku</p></div></div>
  <div class="row wrapf" style="gap:10px;margin-top:16px"><span class="pill">Em breve na loja de canais da Roku</span></div></div>
</div>
<p class="dim" style="font-size:12px;margin-top:12px">No Fire TV ou Android, se o aparelho pedir, permita instalar apps de fontes desconhecidas para o Downloader ou o navegador.</p>
</div></section>`;

  const activate = `<section id="entrar" class="sec"><div class="wrap g2 act">
<div><div class="eyebrow">ATIVAR APARELHO</div><h2 class="h2">Já instalou o app?</h2>
<p class="lead sm">Abra o ${name} na TV, anote o <b>endereço MAC</b> e a <b>chave do dispositivo</b> que aparecem na tela e entre aqui. Ou aponte a câmera do celular para o QR code da TV.</p>
<div class="g3m">${[["1", "Informe MAC e chave"], ["2", "Adicione sua lista"], ["3", "A TV atualiza sozinha"]].map(([n, t]) => `<div class="row" style="gap:10px"><span class="num sm">${n}</span><span class="mut">${t}</span></div>`).join("")}</div></div>
${formHtml}
</div></section>
<section id="planos" class="sec"><div class="wrap"><div class="eyebrow">PLANOS</div><h2 class="h2">Comece com o teste grátis</h2><div class="plans" style="margin-top:22px">${plansHtml}</div></div></section>`;

  const notice = `<section class="sec"><div class="wrap"><div class="card imp">
<span class="ibox warn">${li("warn", 22)}</span><div><h3 style="font-size:22px">Importante: o ${name} é apenas o player</h3>
<p class="mut" style="margin-top:8px">O app funciona como um reprodutor de vídeo. Todo o conteúdo exibido vem exclusivamente da lista que você adiciona.</p>
<ul><li>O ${name} não fornece, não vende e não hospeda vídeos nem listas.</li><li>O app vem sem conteúdo: sem uma lista sua, ele não mostra nada.</li><li>Use apenas conteúdo que você tem direito de assistir.</li></ul></div></div></div></section>`;

  const footer = `<footer class="foot"><div class="wrap row wrapf" style="gap:20px">
<a href="/" class="logo"><img src="${WORDMARK}" alt="${name}" height="28"></a>
<nav class="row wrapf" style="gap:18px;margin-left:auto"><a href="#entrar">Ativar aparelho</a><a href="/aparelho">Minhas listas</a><a href="#planos">Planos</a><a href="/ajuda">Ajuda</a><a href="${APK_URL}">Baixar APK</a></nav>
</div><div class="wrap dim" style="font-size:12px;margin-top:14px">${name} é um player de mídia e não fornece conteúdo.</div></footer>`;

  return nav + hero + steps + install + activate + notice + footer;
}

export const LANDING_CSS = `
html{scroll-behavior:smooth}html,body{overflow-x:clip;max-width:100%}
.g3>*,.g2>*,.g2s>*,.hgrid>*,.act>*,.sgrid>*{min-width:0}
.lnav{position:sticky;top:0;z-index:20;background:rgba(7,6,13,.82);backdrop-filter:blur(12px);border-bottom:1px solid #16121f}
.lnav .lnk{display:flex;gap:26px;margin-left:auto;margin-right:22px;color:var(--mut);font-weight:500}.lnav .lnk a:hover{color:#fff}
.mmenu{display:none;margin-left:12px}@media (max-width:520px){
 .inst .row.wrapf{flex-direction:column;align-items:stretch}.addr{min-width:0;width:100%;word-break:break-all;height:auto;min-height:46px;padding:10px 14px;font-size:13px}.inst .btn{width:100%}
 .inst h3{font-size:16px}.pill{height:auto;min-height:40px;padding:8px 14px}.mmenu{margin-left:auto}}.mmenu summary{list-style:none;cursor:pointer;color:#fff;display:flex;padding:8px;border:1px solid var(--line2);border-radius:10px}
.mmenu summary::-webkit-details-marker{display:none}
.mmenu>div{position:absolute;right:16px;top:64px;background:#100d1c;border:1px solid var(--line2);border-radius:14px;padding:8px;display:grid;min-width:220px;box-shadow:0 20px 50px rgba(0,0,0,.5)}
.mmenu>div a{padding:12px 14px;border-radius:9px;color:#e6e1f5}.mmenu>div a:hover{background:#1d1638}
.eyebrow{color:#00d4c8;font-weight:700;font-size:12px;letter-spacing:2.2px}
.h1{font-family:Outfit,Inter;font-weight:700;font-size:clamp(38px,6vw,64px);line-height:1.04;margin-top:14px;letter-spacing:-.5px}
.hl{background:linear-gradient(90deg,#9b84ff,#3fa8f0,#00d4c8);-webkit-background-clip:text;background-clip:text;color:transparent}
.h2{font-family:Outfit,Inter;font-weight:700;font-size:clamp(28px,4vw,40px);line-height:1.12;margin-top:10px}
.lead{color:var(--mut);font-size:17px;line-height:1.65;margin-top:18px;max-width:600px}.lead.sm{font-size:16px;margin-top:12px}.lead b{color:#fff}
.btn.lg{height:52px;padding:0 24px;font-size:15px;border-radius:12px}
.btn:not(.g){box-shadow:0 10px 30px rgba(124,92,255,.35)}
.wrapf{flex-wrap:wrap}
.hero{padding:64px 0 56px;position:relative}
.hero:after{content:"";position:absolute;right:0;top:-60px;width:60%;height:120%;background:radial-gradient(ellipse at 60% 40%,rgba(124,92,255,.28),transparent 60%);pointer-events:none;z-index:-1}
.hgrid{display:grid;grid-template-columns:minmax(0,820px);gap:48px;align-items:center}
.note{display:flex;gap:12px;margin-top:26px;padding:16px 18px;border-radius:14px;background:rgba(29,22,56,.6);border:1px solid var(--line2);color:var(--mut);font-size:14px;line-height:1.55;max-width:560px}.note svg{flex:none;color:#9b84ff;margin-top:2px}.note b{color:#fff}
.tvf{border-radius:16px;padding:8px;background:linear-gradient(160deg,#2a2440,#100d1c);border:1px solid #2a2440;box-shadow:0 30px 80px rgba(0,0,0,.55)}
.tvf img{display:block;width:100%;height:auto;border-radius:10px;aspect-ratio:16/9;object-fit:cover;background:#050409}
.hshot .tvf{box-shadow:0 40px 100px rgba(124,92,255,.25),0 30px 80px rgba(0,0,0,.6)}
.cap{color:var(--dim);font-size:13px;text-align:right;margin-top:14px}
.strip{border-top:1px solid #1c1730;border-bottom:1px solid #1c1730;background:linear-gradient(90deg,rgba(124,92,255,.14),rgba(63,168,240,.05));padding:26px 0}
.sgrid{display:grid;grid-template-columns:repeat(3,1fr);gap:24px}.sgrid b{font-size:15px}.sgrid .mut{margin-top:3px}
.ibox{flex:none;width:44px;height:44px;border-radius:12px;display:inline-flex;align-items:center;justify-content:center;background:#1d1638;border:1px solid #2a2440;color:#9b84ff}
.ibox.warn{width:56px;height:56px;color:#ffc56b;background:#2a2112;border-color:#4a3a1c}
.sec{padding:76px 0 0}
.g3{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:30px}
.g2{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:26px}
.g2s{display:grid;grid-template-columns:repeat(3,1fr);gap:26px 22px;margin-top:30px}
.step{padding:26px}.step h3,.feat h3,.scard h3,.inst h3{font-family:Outfit,Inter;font-size:19px;margin-top:14px}.step p,.feat p,.scard p{margin-top:8px;line-height:1.6}
.num{display:inline-flex;width:34px;height:34px;border-radius:9px;background:var(--accent);color:#fff;font-weight:700;align-items:center;justify-content:center}.num.sm{width:28px;height:28px;font-size:13px;flex:none}
.feat{padding:22px;display:flex;gap:16px}.feat h3{margin-top:2px;font-size:17px}
.scard h3{margin-top:16px}
.inst{padding:22px}.inst h3{margin-top:0;font-size:18px}.inst p{margin-top:2px}
.addr{flex:1;min-width:200px;height:46px;display:flex;align-items:center;padding:0 14px;border-radius:10px;background:var(--field);border:1px solid var(--line2);color:#00d4c8;font-family:ui-monospace,Menlo,monospace;font-size:14px}
.pill{display:inline-flex;align-items:center;height:40px;padding:0 16px;border-radius:20px;background:#1d1638;border:1px solid #2a2440;color:#cfc8ea;font-weight:600;font-size:13px}
.act{align-items:center;grid-template-columns:1fr 520px}.g3m{display:grid;gap:12px;margin-top:22px}
.imp{display:flex;gap:22px;padding:34px}.imp ul{margin:14px 0 0 18px;color:var(--text);line-height:2}
.foot{margin-top:80px;padding:34px 0 40px;border-top:1px solid #16121f;color:var(--mut)}.foot a:hover{color:#fff}
.tvf.mock{aspect-ratio:16/9;background:#0b0914;padding:14px;display:flex;flex-direction:column;gap:10px;overflow:hidden;font-family:Inter,sans-serif;container-type:inline-size}
.mock .mtop{display:flex;align-items:center;gap:4cqw;font-size:2.1cqw;color:#a59fc0;font-weight:600}.mock .mtop .on{background:#fff;color:#0b0914;padding:.6cqw 1.8cqw;border-radius:2cqw}
.mock .mlogo{width:4cqw;height:4cqw;border-radius:1cqw;background:linear-gradient(135deg,#9b84ff,#5a36e0);color:#ffc56b;display:inline-flex;align-items:center;justify-content:center;font-size:2cqw}
.mock .mhero{position:relative;flex:none;height:21%;border-radius:2.4cqw;overflow:hidden;background:linear-gradient(90deg,#1a1240,#4a1fa0 40%,#c46a2a 75%,#e8a33a);display:flex;align-items:center;padding:0 4cqw}
.mock .mhero.sm{background:linear-gradient(90deg,#0f1b3a,#1f4aa0 45%,#20a0b0 80%,#7ee2d8)}
.mock .mhero small{color:#ffc56b;font-weight:700;font-size:1.6cqw;letter-spacing:.3cqw}.mock .mhero h4{color:#fff;font-size:4.2cqw;font-weight:700;margin-top:.4cqw}
.mock .msun{position:absolute;right:12%;bottom:-40%;width:16cqw;height:16cqw;border-radius:50%;background:radial-gradient(circle,#ffe39a,#f2a33a 55%,transparent 72%);filter:blur(.6cqw)}.mock .msun.v{background:radial-gradient(circle,#c9fff6,#3fd0c0 55%,transparent 72%)}
.mock .mrow-t{color:#fff;font-weight:700;font-size:2.4cqw}
.mock .mrow{flex:1;display:grid;grid-template-columns:repeat(5,1fr);gap:1.6cqw;min-height:0}
.mock .mp{position:relative;border-radius:1.6cqw;overflow:hidden;display:flex;align-items:flex-end;padding:1.4cqw}.mock .mp.sel{box-shadow:0 0 0 .4cqw #ffc56b}
.mock .mp b{position:relative;color:#fff;font-size:1.7cqw;font-weight:700;text-shadow:0 1px 3px rgba(0,0,0,.6)}
.mock .mpc{position:absolute;left:50%;top:38%;width:9cqw;height:9cqw;transform:translate(-50%,-50%);border-radius:50%;background:rgba(255,255,255,.18)}
.mock .mp:after{content:"";position:absolute;inset:40% 0 0;background:linear-gradient(transparent,rgba(0,0,0,.55))}.mock .mp b{z-index:1}
.mock .mch{flex:1;display:grid;grid-template-columns:22% 1fr 32%;gap:1.6cqw;min-height:0}
.mock .mcats{display:flex;flex-direction:column;gap:1cqw}.mock .mcats span{font-size:1.7cqw;color:#cfc8ea;padding:1.2cqw 1.4cqw;border-radius:1.2cqw;background:rgba(255,255,255,.05)}.mock .mcats .on{background:#2a1d5e;color:#fff;box-shadow:inset .5cqw 0 #7c5cff}
.mock .mlist{display:flex;flex-direction:column;gap:1cqw}.mock .mli{display:flex;align-items:center;gap:1.4cqw;padding:1cqw 1.4cqw;border-radius:1.2cqw;background:rgba(255,255,255,.05)}.mock .mli.on{background:#2a1d5e;box-shadow:0 0 0 .3cqw #7c5cff}
.mock .mn{color:#6e6b8c;font-size:1.6cqw;width:3.6cqw}.mock .mlg{width:6cqw;height:4cqw;border-radius:.8cqw;flex:none}
.mock .mli div{flex:1;min-width:0}.mock .mli b,.mock .mprev b{display:block;color:#fff;font-size:1.7cqw}.mock .mli small,.mock .mprev small{display:block;color:#9d9abb;font-size:1.3cqw}
.mock .mli i{display:block;height:.5cqw;background:rgba(255,255,255,.15);border-radius:1cqw;margin-top:.6cqw}.mock .mli em{display:block;height:100%;background:#22d3ee;border-radius:1cqw}
.mock .mprev{display:flex;flex-direction:column;gap:.8cqw}.mock .mvid{flex:none;height:58%;border-radius:1.6cqw;display:flex;align-items:center;justify-content:center}.mock .mplay{width:6cqw;height:6cqw;border-radius:50%;background:rgba(255,255,255,.9);color:#0b0914;display:flex;align-items:center;justify-content:center;font-size:2.4cqw}
@media (max-width:1000px){
 .lnav .lnk{display:none}.mmenu{display:block}
 .hgrid,.g3,.g2,.g2s,.sgrid,.act{grid-template-columns:1fr !important}
 .hero{padding:34px 0 40px}.hshot .tvf{transform:none}.cap{text-align:right}
 .sec{padding-top:56px}.imp{flex-direction:column;padding:24px}
 .lnav .btn.sm{padding:0 12px}.lnav .logo img{height:26px}
}
@media (max-width:520px){
 .inst .row.wrapf{flex-direction:column;align-items:stretch}.addr{min-width:0;width:100%;word-break:break-all;height:auto;min-height:46px;padding:10px 14px;font-size:13px}.inst .btn{width:100%}
 .inst h3{font-size:16px}.pill{height:auto;min-height:40px;padding:8px 14px}
 .lnav .btn.sm{display:none}
 .lead{font-size:15.5px}.btn.lg{width:100%}.note{font-size:13px}
 .step,.feat,.inst{padding:18px}
}`;

export const PLAN_FIX = ".plan .chip{height:24px;padding:0 10px;color:#fff;font-size:11px;line-height:24px}";
