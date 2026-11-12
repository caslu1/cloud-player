# Orbita Play

Player IPTV para **Android TV, Google TV, Fire TV e TV box**, feito para o controle remoto.
O app **não fornece conteúdo**: você entra com os dados do seu provedor (Xtream Codes ou lista M3U).

**Download do APK:** https://github.com/caslu1/cloud-player/releases/download/orbita-play/Orbita-Play.apk

## Instalar na TV

- **Fire TV / Android TV com o app Downloader:** digite o endereço acima, permita "Instalar apps desconhecidos" e instale.
- **TV box:** baixe o APK num pendrive ou pelo navegador e abra com o gerenciador de arquivos.

## Telas

| Aba | O que tem |
|---|---|
| Canais | Categorias (Favoritos, Recentes, Todos), lista com número/logo/programa atual, prévia ao vivo e programação de hoje |
| Filmes | Destaque, Continuar assistindo, Lançamentos, Minha lista e todas as categorias |
| Séries | Destaque, Continuar assistindo, temporadas e episódios com progresso |
| Futebol | Jogos encontrados no guia (EPG) dos canais esportivos, ao vivo agora, próximos dias, times que você segue, onde assistir |
| Buscar | Canais, filmes e séries |
| Biblioteca | Favoritos, minha lista, continuar assistindo, recentes |
| ⚙ | Conta (validade, conexões), formato TS/HLS, próximo episódio automático, prévia automática, recarregar, sair |

## Controle remoto

| Tecla | Listas | Player ao vivo | Player filme/série |
|---|---|---|---|
| OK | abrir / prévia → tela cheia | lista de canais | pausar / continuar |
| Segurar OK | favoritar canal, adicionar à minha lista, seguir time | – | – |
| ▲▼ | navegar | canal anterior / próximo | mostrar informações / opções |
| ◀▶ | trocar coluna | lista de canais / informações | voltar / avançar 10 s (segure para acelerar) |
| 0–9 | – | digitar número do canal | – |
| CH+ / CH− | – | trocar canal | – |
| MENU | – | áudio, legenda, tela | áudio, legenda, tela, próximo episódio |
| VOLTAR | voltar / ir para o menu do topo; 2× sai | sair da tela cheia | sair (o progresso fica salvo) |

## Compilar

```
./gradlew :orbita:assembleRelease
```
O workflow `.github/workflows/orbita.yml` compila e publica o APK no release `orbita-play` a cada alteração.
