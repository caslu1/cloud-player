// Site público: página inicial e página de Downloads (cores do logo: branco + roxo), feito para celular e computador.
import { esc } from "./util.js";
import { WORDMARK } from "./brand.js";

export const APK_URL = "https://github.com/caslu1/cloud-player/releases/download/orbita-play/Orbita-Play.apk";

const L = {
  tv: '<rect x="3" y="5" width="18" height="12" rx="2"/><path d="M8 21h8M12 17v4"/>',
  shield: '<path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2"/><path d="M11 18h2"/>',
  box: '<rect x="3" y="8" width="18" height="10" rx="2"/><path d="M7 21h10"/><circle cx="17" cy="13" r="1"/>',
  fire: '<rect x="9" y="2.5" width="6" height="19" rx="3"/><path d="M12 6v2"/>',
  roku: '<rect x="3" y="6" width="18" height="12" rx="3"/><path d="M8 10v4M8 10h2a1.5 1.5 0 0 1 0 3H8M11 13l1.5 1"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 8h.01M11 12h1v4h1"/>',
  warn: '<circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16.5h.01"/>',
  down: '<path d="M12 4v11M7 10l5 5 5-5M5 20h14"/>',
  key: '<circle cx="8" cy="12" r="3.5"/><path d="M11.5 12H21M18 12v3M15 12v2"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
};
const li = (n, s = 20) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${L[n]}</svg>`;
const icoBox = (n, cls = "") => `<span class="ibox ${cls}">${li(n)}</span>`;

const LINKS = [["/#como", "Como funciona"], ["/download", "Downloads"], ["/#planos", "Planos"], ["/ajuda", "Ajuda"]];

export function siteNav(env, active, logged) {
  const name = esc(env.SITE_NAME);
  const a = (h, t) => `<a href="${h}" class="${active === h ? "on" : ""}">${t}</a>`;
  return `<header class="lnav"><div class="wrap navrow">
<a href="/" class="logo"><img src="${WORDMARK}" alt="${name}" height="30"></a>
<nav class="lnk">${LINKS.map(([h, t]) => a(h, t)).join("")}</nav>
<a class="btn sm navcta" href="${logged ? "/aparelho" : "/#entrar"}">${logged ? "Minhas listas" : "Gerenciar meu aparelho"}</a>
<input type="checkbox" id="mm" class="mmchk" aria-label="Abrir menu"><label for="mm" class="burger" aria-hidden="true"><span></span><span></span><span></span></label>
<div class="mpanel">${LINKS.map(([h, t]) => a(h, t)).join("")}<a href="${logged ? "/aparelho" : "/#entrar"}" class="btn full" style="margin-top:8px">${logged ? "Minhas listas" : "Gerenciar meu aparelho"}</a>${logged ? `<a href="/sair">Sair</a>` : ""}</div>
</div></header>`;
}

export function siteFooter(env) {
  const name = esc(env.SITE_NAME);
  const col = (t, links) => `<div class="fcol"><b>${t}</b>${links.map(([h, x]) => `<a href="${h}">${x}</a>`).join("")}</div>`;
  return `<footer class="foot"><div class="wrap">
<div class="ftop">
 <div class="fbrand"><a href="/" class="logo"><img src="${WORDMARK}" alt="${name}" height="28"></a>
  <p>Player de mídia para Android TV, TV Box, Fire TV e Roku. Você usa a sua própria lista.</p>
  <a class="btn sm" href="/download">${li("down", 16)} Baixar o app</a></div>
 ${col("Aplicativo", [["/download", "Downloads"], ["/#como", "Como funciona"], ["/#planos", "Planos"]])}
 ${col("Minha conta", [["/#entrar", "Gerenciar meu aparelho"], ["/aparelho", "Minhas listas"], ["/ajuda", "Ajuda"]])}
</div>
<div class="fbot"><span>© ${new Date().getFullYear()} ${name}</span><span>${name} é um player de mídia e não fornece conteúdo.</span></div>
</div></footer>`;
}

// planeta com órbita (decoração do topo, nas cores do logo)
const ORBIT = `<div class="orb" aria-hidden="true"><span class="ring r1"></span><span class="ring r2"></span><span class="planet"></span><span class="moon"></span></div>`;

export function landing(env, { formHtml, plansHtml, logged }) {
  const name = esc(env.SITE_NAME);
  const hero = `<section class="hero"><div class="wrap hgrid">
<div>
 <div class="eyebrow">PLAYER DE MÍDIA PARA TV</div>
 <h1 class="h1">Sua lista na TV, <span class="grad">do seu jeito.</span></h1>
 <p class="lead">O ${name} é um player para Android TV, TV Box, Fire TV e Roku. Você adiciona a lista que já é sua e assiste com uma interface simples, rápida e bonita.</p>
 <div class="row wrapf" style="gap:12px;margin-top:28px"><a class="btn lg" href="#entrar">Gerenciar meu aparelho</a><a class="btn g lg" href="/download">${li("down", 18)} Baixar o app</a></div>
 <div class="note">${li("info", 18)}<div><b>O ${name} não inclui nenhum conteúdo.</b> Não fornece, não vende e não hospeda vídeos: só reproduz o que vem da lista que você mesmo adiciona.</div></div>
</div>
${ORBIT}
</div></section>
<section class="strip"><div class="wrap sgrid">
${[["tv", "Feito para TV", "Navegação rápida com o controle remoto."], ["shield", "Sem conteúdo próprio", "Você usa só a sua própria lista."], ["phone", "Configure pelo celular", "Leia o QR code e pronto."]]
  .map(([i, t, d]) => `<div class="row" style="gap:14px">${icoBox(i)}<div><b>${t}</b><div class="mut">${d}</div></div></div>`).join("")}
</div></section>`;

  const steps = `<section id="como" class="sec"><div class="wrap">
<div class="eyebrow">COMO FUNCIONA</div><h2 class="h2">Três passos e você já está assistindo</h2>
<p class="lead sm">Você só precisa da lista que recebeu do seu provedor: endereço, usuário e senha (ou o link da lista de reprodução).</p>
<div class="g3">
${[["Instale na sua TV", `Baixe o ${name} na página de <a class="lnk2" href="/download">Downloads</a> e abra o app. Na tela aparecem um QR code, o endereço MAC e a chave do dispositivo.`],
   ["Envie sua lista", "Leia o QR code com o celular ou entre neste site com o MAC e a chave e cadastre sua lista. Também dá para digitar direto na TV."],
   ["Assista", "A lista aparece na TV em segundos. Você pode guardar mais de uma playlist e trocar quando quiser."]]
  .map(([t, d], i) => `<div class="card step"><span class="num">${i + 1}</span><h3>${t}</h3><p class="mut">${d}</p></div>`).join("")}
</div></div></section>`;

  const dl = `<section class="sec"><div class="wrap"><div class="card cta">
<div><div class="eyebrow">DOWNLOADS</div><h2 class="h2" style="font-size:clamp(24px,3vw,32px)">Baixe o ${name} no seu aparelho</h2>
<p class="mut" style="margin-top:8px">Android TV, Google TV, TV Box e Fire TV disponíveis. Roku, LG e Samsung em breve.</p></div>
<a class="btn lg" href="/download">${li("down", 18)} Ver downloads</a></div></div></section>`;

  const activate = `<section id="entrar" class="sec"><div class="wrap g2 act">
<div><div class="eyebrow">GERENCIAR APARELHO</div><h2 class="h2">Já instalou o app?</h2>
<p class="lead sm">Abra o ${name} na TV, anote o <b>endereço MAC</b> e a <b>chave do dispositivo</b> que aparecem na tela e entre aqui. Ou aponte a câmera do celular para o QR code da TV.</p>
<div class="g3m">${["Informe MAC e chave", "Adicione sua lista", "A TV atualiza sozinha"].map((t, i) => `<div class="row" style="gap:12px"><span class="num sm">${i + 1}</span><span class="mut">${t}</span></div>`).join("")}</div></div>
${formHtml}
</div></section>
<section id="planos" class="sec"><div class="wrap"><div class="eyebrow">PLANOS</div><h2 class="h2">Comece com o teste grátis</h2><div class="plans" style="margin-top:24px">${plansHtml}</div></div></section>`;

  const notice = `<section class="sec"><div class="wrap"><div class="card imp">
<div class="imph"><span class="ibox warn">${li("warn", 22)}</span><div><div class="eyebrow" style="color:#ffc56b">IMPORTANTE</div><h3>O ${name} é apenas o player</h3></div></div>
<p class="mut impp">O app funciona como um reprodutor de vídeo. Todo o conteúdo exibido vem exclusivamente da lista que você adiciona.</p>
<div class="impl">${["O " + name + " não fornece, não vende e não hospeda vídeos nem listas.", "O app vem sem conteúdo: sem uma lista sua, ele não mostra nada.", "Use apenas conteúdo que você tem direito de assistir."].map((t) => `<div class="impi">${li("check", 16)}<span>${t}</span></div>`).join("")}</div>
</div></div></section>`;

  return siteNav(env, "/", logged) + hero + steps + dl + activate + notice + siteFooter(env);
}

export function downloadsPage(env, { logged, host }) {
  const name = esc(env.SITE_NAME);
  const code = `${esc(host)}/android`;
  const ready = (ico, t, sub, extra) => `<div class="card dcard">
<div class="row" style="gap:14px">${icoBox(ico)}<div style="min-width:0"><h3>${t}</h3><p class="mut">${sub}</p></div><span class="tag ok">${li("check", 14)} Disponível</span></div>${extra}</div>`;
  const soon = (ico, t, sub) => `<div class="card dcard soon">
<div class="row" style="gap:14px">${icoBox(ico, "dim2")}<div style="min-width:0"><h3>${t}</h3><p class="mut">${sub}</p></div><span class="tag">${li("clock", 14)} Em breve</span></div></div>`;
  const apkBlock = `<div class="dl-row"><div class="addr"><small>No app Downloader, digite</small><b>${code}</b></div><a class="btn" href="${APK_URL}">${li("down", 18)} Baixar APK</a></div>`;
  const body = `<section class="hero sm"><div class="wrap">
<div class="eyebrow">DOWNLOADS</div><h1 class="h1" style="font-size:clamp(34px,5vw,52px)">Baixe o <span class="grad">${name}</span></h1>
<p class="lead">Escolha o seu aparelho. Depois de instalar, abra o app e ative pelo celular ou entre com a sua lista direto na TV.</p>
</div></section>
<section class="sec" style="padding-top:10px"><div class="wrap">
<div class="dgrid">
${ready("tv", "Android TV e Google TV", "TVs com Android, Google TV e Chromecast", apkBlock)}
${ready("box", "TV Box Android", "Qualquer TV Box com Android 6 ou mais novo", apkBlock)}
${ready("fire", "Fire TV e Fire TV Stick", "Instale pelo app Downloader da Amazon", apkBlock)}
${soon("roku", "Roku", "Canal na loja da Roku")}
${soon("tv", "Smart TV LG", "webOS · na LG Content Store")}
${soon("tv", "Smart TV Samsung", "Tizen · na Samsung Smart Hub")}
</div>
<div class="card howto"><h3>Como instalar no Fire TV ou TV Box</h3>
<ol><li>Instale o app <b>Downloader</b> pela loja do aparelho.</li><li>Abra o Downloader e digite <b>${code}</b>.</li><li>Confirme a instalação. Se pedir, permita instalar apps de <b>fontes desconhecidas</b> para o Downloader.</li><li>Abra o ${name} e ative o aparelho.</li></ol></div>
</div></section>`;
  return siteNav(env, "/download", logged) + body + siteFooter(env);
}

export const LANDING_CSS = `
:root{--accent:#7c5cff;--accent2:#9b84ff;--soft:#1d1638}
html{scroll-behavior:smooth}html,body{overflow-x:clip;max-width:100%}
body:before{background:radial-gradient(ellipse at 80% -10%,rgba(124,92,255,.28),transparent 55%),radial-gradient(ellipse at -10% 110%,rgba(155,132,255,.10),transparent 50%)!important}
.g3>*,.g2>*,.hgrid>*,.act>*,.sgrid>*,.dgrid>*{min-width:0}
.lnav{position:sticky;top:0;z-index:30;background:rgba(7,6,13,.8);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);border-bottom:1px solid rgba(155,132,255,.12)}
.navrow{display:flex;align-items:center;height:72px;gap:20px}
.lnk{display:flex;gap:6px;margin-left:auto}.lnk a{padding:8px 14px;border-radius:10px;color:var(--mut);font-weight:500}.lnk a:hover{color:#fff;background:rgba(155,132,255,.08)}.lnk a.on{color:#fff;background:rgba(124,92,255,.16)}
.mmchk{display:none}.burger{display:none;margin-left:auto;width:44px;height:44px;border-radius:12px;border:1px solid rgba(155,132,255,.3);background:rgba(124,92,255,.12);cursor:pointer;flex-direction:column;align-items:center;justify-content:center;gap:5px}
.burger span{display:block;width:20px;height:2px;border-radius:2px;background:#fff;transition:transform .2s,opacity .2s}
.mmchk:checked+.burger span:nth-child(1){transform:translateY(7px) rotate(45deg)}.mmchk:checked+.burger span:nth-child(2){opacity:0}.mmchk:checked+.burger span:nth-child(3){transform:translateY(-7px) rotate(-45deg)}
.mpanel{display:none}
.eyebrow{color:var(--accent2);font-weight:700;font-size:12px;letter-spacing:2.4px}
.h1{font-family:Outfit,Inter;font-weight:700;font-size:clamp(38px,6vw,66px);line-height:1.04;margin-top:14px;letter-spacing:-.5px}
.grad{background:linear-gradient(90deg,#c9bbff,#9b84ff 45%,#7c5cff);-webkit-background-clip:text;background-clip:text;color:transparent}
.h2{font-family:Outfit,Inter;font-weight:700;font-size:clamp(28px,4vw,40px);line-height:1.12;margin-top:10px}
.lead{color:var(--mut);font-size:17px;line-height:1.65;margin-top:18px;max-width:620px}.lead.sm{font-size:16px;margin-top:12px}.lead b{color:#fff}
.btn.lg{height:52px;padding:0 24px;font-size:15px;border-radius:12px}
.btn:not(.g){background:linear-gradient(135deg,#8b6cff,#6a46f0);box-shadow:0 10px 30px rgba(124,92,255,.35)}
.btn.g{border-color:rgba(155,132,255,.35)}.btn.g:hover{background:rgba(124,92,255,.12)}
.wrapf{flex-wrap:wrap}.lnk2{color:var(--accent2);font-weight:600}
.hero{padding:72px 0 64px;position:relative}.hero.sm{padding:56px 0 24px}
.hgrid{display:grid;grid-template-columns:1.15fr .85fr;gap:40px;align-items:center}
.note{display:flex;gap:12px;margin-top:28px;padding:16px 18px;border-radius:14px;background:rgba(29,22,56,.55);border:1px solid rgba(155,132,255,.2);color:var(--mut);font-size:14px;line-height:1.55;max-width:580px}.note svg{flex:none;color:var(--accent2);margin-top:2px}.note b{color:#fff}
.orb{position:relative;aspect-ratio:1;width:100%;max-width:420px;justify-self:center}
.orb .planet{position:absolute;inset:27%;border-radius:50%;background:radial-gradient(circle at 32% 30%,#ffffff,#d9d0ff 18%,#9b84ff 45%,#5a36e0 75%,#2a1a6e);box-shadow:0 0 80px rgba(124,92,255,.55),inset -18px -22px 50px rgba(20,10,60,.6)}
.orb .ring{position:absolute;border-radius:50%;border:2px solid rgba(155,132,255,.55)}
.orb .r1{inset:6% -6%;transform:rotate(-24deg) scaleY(.36);border-width:3px;border-color:rgba(155,132,255,.85);box-shadow:0 0 30px rgba(124,92,255,.35)}
.orb .r2{inset:0;border-style:dashed;border-color:rgba(155,132,255,.18)}
.orb .moon{position:absolute;width:7%;height:7%;border-radius:50%;background:#fff;right:8%;top:30%;box-shadow:0 0 20px #fff}
.strip{border-top:1px solid rgba(155,132,255,.12);border-bottom:1px solid rgba(155,132,255,.12);background:linear-gradient(90deg,rgba(124,92,255,.14),rgba(124,92,255,.03));padding:26px 0}
.sgrid{display:grid;grid-template-columns:repeat(3,1fr);gap:24px}.sgrid b{font-size:15px}.sgrid .mut{margin-top:3px}
.ibox{flex:none;width:46px;height:46px;border-radius:13px;display:inline-flex;align-items:center;justify-content:center;background:linear-gradient(145deg,rgba(124,92,255,.28),rgba(124,92,255,.08));border:1px solid rgba(155,132,255,.3);color:#c9bbff}
.ibox.dim2{background:rgba(255,255,255,.04);border-color:rgba(255,255,255,.1);color:#8c88a8}
.ibox.warn{width:56px;height:56px;color:#ffc56b;background:#2a2112;border-color:#4a3a1c}
.card{border-color:rgba(155,132,255,.14)!important;background:linear-gradient(180deg,rgba(22,18,40,.9),rgba(16,13,28,.9))!important}
.sec{padding:80px 0 0}
.g3{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:30px}
.g2{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:26px}
.step{padding:28px}.step h3,.dcard h3,.howto h3,.imp h3{font-family:Outfit,Inter;font-size:19px}.step h3{margin-top:16px}.step p{margin-top:8px;line-height:1.6}
.num{display:inline-flex;width:36px;height:36px;border-radius:10px;background:linear-gradient(135deg,#8b6cff,#6a46f0);color:#fff;font-weight:700;align-items:center;justify-content:center}.num.sm{width:30px;height:30px;font-size:13px;flex:none}
.cta{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:34px 36px;background:linear-gradient(120deg,rgba(124,92,255,.22),rgba(22,18,40,.9) 60%)!important}
.act{align-items:center;grid-template-columns:1fr 520px}.g3m{display:grid;gap:14px;margin-top:24px}
.imp{padding:30px 32px}.imph{display:flex;align-items:center;gap:16px}.imp h3{font-size:22px;margin-top:4px}.impp{margin-top:14px;line-height:1.6}.impl{display:grid;gap:10px;margin-top:16px}.impi{display:flex;gap:10px;align-items:flex-start;padding:12px 14px;border-radius:12px;background:rgba(255,197,107,.06);border:1px solid rgba(255,197,107,.14);color:var(--text);font-size:14.5px;line-height:1.5}.impi svg{flex:none;color:#ffc56b;margin-top:2px}.ibox.warn{width:50px;height:50px}
.dgrid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}
.dcard{padding:22px;display:flex;flex-direction:column;gap:16px}.dcard h3{font-size:17px}.dcard p{font-size:13px;margin-top:2px}
.dcard .row{align-items:flex-start}.dcard .tag{margin-left:auto}
.dcard.soon{opacity:.75}
.tag{display:inline-flex;align-items:center;gap:5px;flex:none;padding:5px 10px;border-radius:20px;font-size:11.5px;font-weight:700;background:rgba(255,255,255,.06);color:#a59fc0;white-space:nowrap}.tag.ok{background:rgba(126,226,168,.12);color:#7ee2a8}
.dl-row{display:grid;gap:10px}.addr{display:flex;flex-direction:column;gap:3px;padding:10px 14px;border-radius:12px;background:var(--field);border:1px solid rgba(155,132,255,.2)}
.addr small{color:var(--dim);font-size:11px}.addr b{color:#c9bbff;font-family:ui-monospace,Menlo,monospace;font-size:13.5px;word-break:break-all;font-weight:600}
.dl-row .btn{width:100%}
.howto{margin-top:22px;padding:28px}.howto ol{margin:14px 0 0 20px;color:var(--text);line-height:2}.howto b{color:#c9bbff}
.plan.hl{border-color:#7c5cff!important;box-shadow:0 0 0 1px #7c5cff,0 20px 50px rgba(124,92,255,.25)}.plan.hl .o{color:#fff}.foot{margin-top:100px;border-top:1px solid rgba(155,132,255,.14);background:linear-gradient(180deg,rgba(124,92,255,.06),transparent 70%);color:var(--mut)}
.ftop{display:grid;grid-template-columns:1.6fr 1fr 1fr;gap:40px;padding:52px 0 40px}
.fbrand p{font-size:14px;line-height:1.6;margin:14px 0 18px;max-width:340px}
.fcol{display:flex;flex-direction:column;gap:12px}.fcol b{color:#fff;font-size:13px;letter-spacing:1.2px;text-transform:uppercase;margin-bottom:4px}
.fcol a{font-size:14.5px;width:fit-content}.fcol a:hover{color:#c9bbff}
.fbot{display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;padding:20px 0 30px;border-top:1px solid rgba(155,132,255,.1);font-size:12.5px;color:var(--dim)}
.dpage{padding-top:40px;padding-bottom:20px}
.dtop{display:flex;gap:22px;align-items:flex-end;justify-content:space-between;flex-wrap:wrap}
.dmeta{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}.dpill{display:inline-flex;align-items:center;gap:6px;padding:6px 12px;border-radius:20px;background:rgba(124,92,255,.1);border:1px solid rgba(155,132,255,.2);color:#cfc8ea;font-size:13px}
.dstat{display:flex;align-items:center;gap:18px;padding:18px 22px;flex-wrap:wrap}.dlab{color:var(--dim);font-size:11px;font-weight:700;letter-spacing:1.4px}.dline{font-size:13.5px;color:#cfc8ea}
.dgrid2>*,.dtop>*{min-width:0}.lact .btn{white-space:nowrap}.in.mono::placeholder{letter-spacing:0;font-family:Inter,sans-serif}
.dgrid2{display:grid;grid-template-columns:1fr 480px;gap:20px;margin-top:26px;align-items:start}
.dbox{padding:26px}.dh{font-family:Outfit,Inter;font-size:20px}.dcount{margin-left:auto;font-size:12.5px;color:var(--dim);padding:4px 10px;border-radius:20px;background:rgba(255,255,255,.04)}
.lrow{padding:16px;border-radius:14px;background:var(--field);border:1px solid var(--line)}.lrow.cur{border-color:rgba(124,92,255,.7);background:linear-gradient(135deg,rgba(124,92,255,.12),var(--field) 60%)}
.lhead{display:flex;gap:14px;align-items:center}.lico{flex:none;width:44px;height:44px;border-radius:12px;background:rgba(124,92,255,.18);display:flex;align-items:center;justify-content:center}
.linfo{min-width:0;flex:1}.linfo b{font-size:16px}.lsub{color:var(--dim);font-size:13px;margin-top:3px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.lact{display:flex;gap:8px;margin-top:14px;flex-wrap:wrap}.lact form{display:contents}
.fld{margin-top:16px}.fld2{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:16px}
.paybox{max-width:920px;margin:0 auto;padding:34px;display:flex;gap:34px;flex-wrap:wrap}.payr{width:340px;max-width:100%}
.plan .chip{height:24px;padding:0 10px;color:#fff;font-size:11px;line-height:24px}
@media (max-width:1000px){
 .lnk,.navcta{display:none}.burger{display:flex}
 .mpanel{position:fixed;left:0;right:0;top:72px;background:#0a0814;border-bottom:1px solid rgba(155,132,255,.2);padding:12px 16px 18px;flex-direction:column;gap:2px;box-shadow:0 30px 60px rgba(0,0,0,.6)}
 .mpanel a:not(.btn){padding:14px 12px;border-radius:10px;color:#e6e1f5;font-weight:500;font-size:16px}.mpanel a.on{background:rgba(124,92,255,.16)}
 .mmchk:checked~.mpanel{display:flex}
 .hgrid,.g3,.g2,.sgrid,.act,.dgrid{grid-template-columns:1fr !important}
 .orb{display:none}.hero{padding:40px 0 44px}.hero.sm{padding:36px 0 10px}
 .sec{padding-top:60px}.imp{padding:22px}.imp h3{font-size:19px}.cta{flex-direction:column;align-items:flex-start;padding:26px}
}
@media (max-width:760px){
 .ftop{grid-template-columns:1fr 1fr;gap:30px 20px;padding:40px 0 30px}.fbrand{grid-column:1/-1}
 .fbot{flex-direction:column;gap:6px}
}
@media (max-width:900px){
 .dgrid2{grid-template-columns:1fr}.dtop{align-items:stretch}.dstat{width:100%}
}
@media (max-width:520px){
 .dpage{padding-top:26px}.dbox{padding:20px}.dstat{padding:16px 18px}.dstat .btn{width:100%}
 .lact .btn{flex:1}.fld2{grid-template-columns:1fr}
 .paybox{padding:22px;gap:24px}.payr{width:100%}
 .tabs label{font-size:12.5px}
 .act form.card{padding:22px !important}.act .g3m{margin-top:18px;gap:10px}
 .plan{padding:20px !important}
 .dcard .row{flex-wrap:wrap}.dcard .row>div{flex:1 1 calc(100% - 64px)}.dcard .tag{margin-left:60px;order:3}
 .dcard h3{font-size:17px}.addr b{font-size:12.5px}
 .howto ol{margin-left:18px;line-height:1.7}.howto li{margin-bottom:8px}
  .fnav a{padding:10px 12px;border-radius:10px;background:rgba(124,92,255,.08);border:1px solid rgba(155,132,255,.14);font-size:14px}
 .lead{font-size:15.5px}.btn.lg{width:100%}.note{font-size:13px}.step,.dcard,.howto{padding:20px}.cta .btn{width:100%}
 .lnav .logo img{height:26px}
}`;
