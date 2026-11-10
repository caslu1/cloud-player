# Cloud Player no Media Station X (TVs Samsung e LG)

TVs Samsung (Tizen) e LG (webOS) não instalam APK. Para elas existe uma versão simples do Cloud Player que roda dentro do app **Media Station X** (MSX). Ela mostra os canais da sua lista M3U no player do MSX.

> Esta versão é bem mais simples que o app Android: só lista M3U, sem perfis, guia completo, pular abertura etc.

## 1. Ativar o GitHub Pages (uma vez, no computador ou celular)
1. Abra https://github.com/caslu1/cloud-player/settings/pages
2. Em **Source** escolha **Deploy from a branch**.
3. Em **Branch** escolha o branch do projeto e a pasta **/docs**, depois **Save**.
4. Espere 1–2 minutos. Teste: https://caslu1.github.io/cloud-player/msx/start.json deve mostrar um texto com "Cloud Player".

## 2. Criar o endereço curto `caslu1.github.io` (uma vez)
O teclado do MSX não tem a barra `/`, então ele só aceita um endereço sem caminho. Para isso:
1. Crie um repositório **público** chamado exatamente **`caslu1.github.io`**: https://github.com/new
2. Nele, clique em **Add file › Create new file**, digite o nome **`msx/start.json`** e cole:
```json
{
    "name": "Cloud Player",
    "version": "1.0.1",
    "parameter": "menu:request:interaction:init@https://caslu1.github.io/cloud-player/msx/plugin.html"
}
```
3. Clique em **Commit changes**. Em 1–2 minutos, https://caslu1.github.io/msx/start.json deve mostrar esse texto.

## 3. Configurar a TV
1. Instale o **Media Station X** pela loja da TV (Samsung Apps / LG Content Store) e abra.
2. Vá em **Settings › Start Parameter › Setup**.
3. Digite: `caslu1.github.io` e confirme no **✓**.
4. O MSX reinicia e mostra o menu do **Cloud Player**.

## 4. Adicionar a sua lista M3U
1. No menu, abra **TV ao vivo › Adicionar minha lista M3U**.
2. Digite o link da sua lista no teclado da tela (os botões `get.php?username=`, `&password=` e `&type=m3u_plus` ajudam em links do tipo Xtream). O botão `abc/ABC` alterna maiúsculas.
3. Escolha **Salvar** e depois **Abrir canais agora**.

O link fica salvo **apenas nesta TV**. Para trocar, use **Configurações › Trocar lista M3U**.

## Testar no computador
Abra no navegador:
`http://msx.benzac.de/?start=menu:request:interaction:init@https://caslu1.github.io/cloud-player/msx/plugin.html`

## Privacidade
O link da lista é enviado ao serviço oficial de listas M3U do Media Station X (`msx.benzac.de`), que converte a lista para o formato do MSX. Ele nunca é gravado neste repositório.
