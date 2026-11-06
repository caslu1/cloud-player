package com.cloudplayer.app

// Traduções para português do Brasil (parte 5/5): linhas alemão¦português, complemento aos idiomas em DictA–J
val DictPt5 = """
Dunkler Kasten hinter der Uhr. Aus = nur die Zahlen.¦Caixa escura atrás do relógio. Desativado = só os números.
Puffer (bei langsamem Internet)¦Buffer (para internet lenta)
Bildrate anpassen¦Ajustar taxa de quadros
Ruckelt – Puffer wird beim nächsten Start größer¦Travando – o buffer será maior na próxima vez
App-Aktualisierung¦Atualização do app
Adresse der Update-Datei (https://…/update.json)¦Endereço do arquivo de atualização (https://…/update.json)
Nach Update suchen¦Verificar atualização
Herunterladen und installieren¦Baixar e instalar
Du hast die neueste Version.¦Você tem a versão mais recente.
Bitte warten …¦Aguarde …
Installation gestartet¦Instalação iniciada
Verbindung hängt – neuer Versuch¦Conexão travada – tentando novamente
Titel pro Zeile (Kino)¦Títulos por linha (cinema)
Rechts = nächste Staffel  ·  Links = Staffeln¦Direita = próxima temporada  ·  Esquerda = temporadas
Outfit¦Outfit
Inter¦Inter
System¦Sistema
Noch keine Favoriten¦Nenhum favorito ainda
Zu Live TV¦Ir para TV ao vivo
Noch nichts in deiner Liste¦Nada na sua lista ainda
Filme entdecken¦Descobrir filmes
Noch nichts angesehen¦Nada assistido ainda
Hier erscheinen die zuletzt gestarteten Filme und Serien.¦Os filmes e séries iniciados recentemente aparecem aqui.
Auf einem Sender OK gedrückt halten, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.¦Mantenha OK pressionado em um canal; para filmes e séries, escolha “Adicionar aos favoritos” na página de detalhes.
Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.¦Escolha “Minha lista” na página de um filme ou série.
Keine Treffer¦Nenhum resultado
Mit diesen Filtern gibt es hier keine Titel.¦Não há títulos aqui com esses filtros.
Filter zurücksetzen¦Redefinir filtros
Diese Liste ist noch leer¦Esta lista ainda está vazia
Auf einem Sender OK gedrückt halten und „Zu Favoriten“ wählen.¦Mantenha OK pressionado em um canal e escolha “Adicionar aos favoritos”.
In „Alle Sender“ oder einer Kategorie auf einem Sender OK gedrückt halten und „Zu …“ wählen.¦Em “Todos os canais” ou em uma categoria, mantenha OK pressionado em um canal e escolha “Adicionar a …”.
In dieser Kategorie gibt es keine Sender. Links eine andere Kategorie wählen.¦Não há canais nesta categoria. Escolha outra categoria à esquerda.
Prüfe die Schreibweise oder suche nach einem Teil des Titels. Kleine Tippfehler findet die Suche trotzdem.¦Verifique a grafia ou pesquise parte do título. Pequenos erros de digitação ainda são encontrados.
Outfit (Standard) und Inter sind in die App eingebaut, die übrigen Schriften stammen von deinem Gerät.¦Outfit (padrão) e Inter vêm integradas ao app; as outras fontes vêm do seu dispositivo.
Top 10 heute¦Top 10 de hoje
Danach¦A seguir
Für diesen Sender gibt es kein Programm.¦Não há guia de programação para este canal.
FILM¦FILME
SERIE¦SÉRIE
Runter für Details und Abspielen¦Para baixo: detalhes e reprodução
Links/Rechts = {#0} s springen¦Esquerda/Direita = pular {#0} s
2020er¦Anos 2020
2010er¦Anos 2010
2000er¦Anos 2000
1990er¦Anos 1990
Infos auf dem Poster¦Informações no pôster
Breites Szenenbild¦Imagem de cena larga
Neu: Version {0}¦Novo: versão {0}
Fehler {#0}¦Erro {#0}
Keine Update-Adresse eingetragen¦Nenhum endereço de atualização informado
Update-Datei unvollständig¦Arquivo de atualização incompleto
Leere Antwort¦Resposta vazia
Lade … {#0} %¦Carregando … {#0} %
Erlaube der App einmal „Apps installieren“ und tippe dann erneut.¦Permita uma vez que o app “Instale apps” e tente novamente.
Trage die Adresse einer kleinen Datei update.json ein, die du selbst bereitstellst: {"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}. Die neue APK muss mit demselben Schlüssel signiert sein wie die installierte, sonst lehnt Android sie ab.¦Informe o endereço de um pequeno arquivo update.json que você mesmo disponibiliza: {"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}. O novo APK precisa ser assinado com a mesma chave do instalado, senão o Android o rejeita.
Startet die nächste Folge, sobald der Abspann beginnt. Geht nur, wenn für die Serie Abspann-Zeiten bekannt sind (TheIntroDB); sonst bleibt es beim Hinweis in den letzten 30 Sekunden.¦Inicia o próximo episódio assim que os créditos começam. Só funciona se os tempos dos créditos da série forem conhecidos (TheIntroDB); caso contrário, aparece apenas o aviso nos últimos 30 segundos.
Zeigt die aktuelle Uhrzeit beim Abspielen in einer Ecke (nicht, solange die Steuerleiste offen ist).¦Mostra a hora atual em um canto durante a reprodução (não enquanto a barra de controle estiver aberta).
Oben rechts¦Superior direito
Oben links¦Superior esquerdo
Unten rechts¦Inferior direito
Unten links¦Inferior esquerdo
Die Uhr folgt der Zeitzone und dem 12/24-Stunden-Format aus den Einstellungen. Im Player geht es auch über Einstellungen > Uhrzeit.¦O relógio segue o fuso horário e o formato de 12/24 horas das configurações. No player, também é possível em Configurações > Relógio.
Ein größerer Puffer lädt mehr Film im Voraus und verhindert Ruckeln bei schwacher Leitung; dafür dauert der Start etwas länger und es wird mehr Speicher gebraucht. „Automatisch“ beginnt normal und vergrößert den Puffer von selbst, wenn es mehrfach ruckelt (aktuell Stufe {#0}). Gilt ab dem nächsten gestarteten Titel.¦Um buffer maior carrega mais do filme com antecedência e evita travamentos em conexões fracas; em troca, o início demora um pouco mais e usa mais memória. “Automático” começa normal e aumenta o buffer sozinho se travar várias vezes (nível atual {#0}). Vale a partir do próximo título iniciado.
Passt die Bildwiederholrate des Fernsehers an den Film an (z. B. 24 Bilder), wenn der Fernseher das ohne Bildaussetzer kann. Bei Fire TV zusätzlich in den Geräteeinstellungen „Originale Bildrate anpassen“ prüfen.¦Ajusta a taxa de atualização da TV ao filme (ex.: 24 quadros), se a TV conseguir fazer isso sem cortes de imagem. No Fire TV, verifique também “Ajustar taxa de quadros original” nas configurações do dispositivo.
{#0} min¦{#0} min
Quelle {#0}¦Fonte {#0}
Tonspur „{0}“ ist mit diesem Ausgang nicht abspielbar – andere Spur im Menü wählen¦A faixa de áudio “{0}” não pode ser reproduzida nesta saída – escolha outra faixa no menu
Diese Tonspur kann hier nicht abgespielt werden – andere Spur gewählt¦Esta faixa de áudio não pode ser reproduzida aqui – outra faixa foi selecionada
Verbindung hängt – neuer Versuch ({#0}/2)¦Conexão travada – tentando novamente ({#0}/2)
Video-Rate (laut Datei): {0}¦Taxa de vídeo (segundo o arquivo): {0}
Ton: {0}¦Áudio: {0}
Puffer: {0}¦Buffer: {0}
Auto (5)¦Auto (5)
Weil du „{0}“ geschaut hast¦Porque você assistiu “{0}”
Ähnlich wie {0}¦Parecidos com {0}
Oben findest du alle {0}, Neuheiten, Top bewertet und Kategorien.¦Acima você encontra todos os {0}, novidades, mais bem avaliados e categorias.
Fortsetzen · {#0} Min¦Retomar · {#0} min
Weiter mit S{#0} · Folge {#1}¦Continuar com T{#0} · Episódio {#1}
noch nichts gemerkt¦nada salvo ainda
Liste angelegt. Mit „Sender hinzufügen“ füllen.¦Lista criada. Preencha com “Adicionar canal”.
Zu „¦Adicionar a “
“ hinzufügen: OK auf einem Sender fügt ihn hinzu bzw. entfernt ihn wieder¦”: OK em um canal adiciona ou remove
Entfernt¦Removido
Hinzugefügt¦Adicionado
Diese Playlist ist sehr groß: {0} Titel in {#1} Sprachen.¦Esta playlist é muito grande: {0} títulos em {#1} idiomas.
„Alles laden“ lädt alle {0} Titel. Das kann einige Minuten dauern.¦“Carregar tudo” carrega todos os {0} títulos. Isso pode levar alguns minutos.
Gewählt: {0} Titel¦Selecionados: {0} títulos
Das dauert gerade sehr lange.¦Isso está demorando muito.
Sehr große Playlists brauchen lange. Mit der Sprachauswahl lädst du nur die Titel, die du wirklich brauchst.¦Playlists muito grandes demoram bastante. Use a seleção de idiomas para carregar só os títulos de que você precisa.
Server nicht gefunden (DNS)¦Servidor não encontrado (DNS)
Sichere Verbindung fehlgeschlagen. Datum und Uhrzeit des Geräts prüfen¦Falha na conexão segura. Verifique a data e a hora do dispositivo
Zeitüberschreitung, Server antwortet nicht¦Tempo esgotado, o servidor não responde
API-Schlüssel¦Chaves de API
TMDB, IMDb und Rotten Tomatoes¦TMDB, IMDb e Rotten Tomatoes
Die App enthält keine eigenen Schlüssel. Mit deinen eigenen, kostenlosen Schlüsseln zeigt sie Poster, Beschreibungen, Besetzung, Trailer, automatische Genres sowie IMDb- und Rotten-Tomatoes-Wertungen. Ohne Schlüssel übernimmt sie Beschreibung, Cover und Bewertung von deinem Anbieter (bei Xtream-Zugängen).¦O app não vem com chaves próprias. Com suas próprias chaves gratuitas, ele mostra pôsteres, sinopses, elenco, trailers, gêneros automáticos e notas do IMDb e do Rotten Tomatoes. Sem chaves, usa a sinopse, a capa e a nota do seu provedor (em acessos Xtream).
TMDB (Film-Infos)¦TMDB (informações de filmes)
TMDB API-Schlüssel (v3)¦Chave de API do TMDB (v3)
Kostenlos unter themoviedb.org: Konto anlegen, anmelden, dann Einstellungen > API > API-Schlüssel anfordern und den 32-stelligen „API Key“ eintragen. Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert.¦Grátis em themoviedb.org: crie uma conta, entre, depois vá em Settings > API, solicite uma chave de API e informe a “API Key” de 32 caracteres. Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB.
OMDb (IMDb- und Rotten-Tomatoes-Wertung)¦OMDb (notas do IMDb e do Rotten Tomatoes)
OMDb API-Schlüssel¦Chave de API do OMDb
Kostenlos unter omdbapi.com > API Key (FREE). Danach den Aktivierungslink in der E-Mail anklicken, sonst funktioniert der Schlüssel nicht. Geht auch ohne TMDB-Schlüssel.¦Grátis em omdbapi.com > API Key (FREE). Depois clique no link de ativação no e-mail, senão a chave não funciona. Funciona também sem chave do TMDB.
Speichern und prüfen¦Salvar e verificar
Schlüssel entfernen¦Remover chaves
Schlüssel gültig¦Chave válida
Schlüssel ungültig¦Chave inválida
Keine Verbindung¦Sem conexão
Die Schlüssel bleiben nur auf diesem Gerät. Nach dem Eintragen die App einmal neu starten, damit alle Bereiche die Infos laden. Intro- und Abspann-Zeiten kommen von TheIntroDB und brauchen keinen Schlüssel.¦As chaves ficam apenas neste dispositivo. Depois de informá-las, reinicie o app uma vez para que todas as seções carreguem as informações. Os tempos de abertura e créditos vêm do TheIntroDB e não precisam de chave.
Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert. Wertungen: OMDb. Intro-Zeiten: TheIntroDB.¦Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB. Notas: OMDb. Tempos de abertura: TheIntroDB.
Cloud Player ist freie Software unter der GNU General Public License v3 (GPL-3.0). Verwendete Bibliotheken: AndroidX/Jetpack Compose, Media3 (Apache 2.0), OkHttp, Coil (Apache 2.0), Jellyfin FFmpeg-Decoder (GPL-3.0), Schriften Outfit und Inter (SIL Open Font License).¦Cloud Player é software livre sob a GNU General Public License v3 (GPL-3.0). Bibliotecas usadas: AndroidX/Jetpack Compose, Media3 (Apache 2.0), OkHttp, Coil (Apache 2.0), decodificador FFmpeg do Jellyfin (GPL-3.0), fontes Outfit e Inter (SIL Open Font License).
Automatische Genres brauchen einen TMDB-Schlüssel (Einstellungen > API-Schlüssel).¦Gêneros automáticos precisam de uma chave do TMDB (Configurações > Chaves de API).
Tipp: Unter Einstellungen > API-Schlüssel einen eigenen TMDB-Schlüssel eintragen, dann erscheinen hier Beschreibung, Cover und Bewertungen.¦Dica: informe sua própria chave do TMDB em Configurações > Chaves de API para ver aqui a sinopse, a capa e as notas.
Zugang wird geprüft …¦Verificando acesso …
Sender werden geladen …¦Carregando canais …
Filme werden geladen …¦Carregando filmes …
Serien werden geladen …¦Carregando séries …
Serien werden geladen ({#0}/{#1}) …¦Carregando séries ({#0}/{#1}) …
Die Schnittstelle des Anbieters antwortet nicht wie erwartet.¦A interface do provedor não responde como esperado.
Zugang abgelehnt. Benutzername, Passwort oder Abo prüfen.¦Acesso negado. Verifique usuário, senha ou assinatura.
Der Server hat keine gültige Playlist geliefert.¦O servidor não retornou uma playlist válida.
Update verfügbar: Version {0}¦Atualização disponível: versão {0}
Update wird geladen …¦Baixando atualização …
Installiert: Version {0}¦Instalada: versão {0}
Automatisch nach Updates suchen¦Verificar atualizações automaticamente
Einmal am Tag. Ein neues Update erscheint in der Glocke oben rechts.¦Uma vez por dia. Uma nova atualização aparece no sino no canto superior direito.
Jetzt nach Update suchen¦Verificar atualizações agora
Updates kommen von der Projektseite auf GitHub. Deine Playlists, Favoriten, der Verlauf und die Einstellungen bleiben bei einem Update erhalten. Android fragt vor der Installation einmal nach.¦As atualizações vêm da página do projeto no GitHub. Suas playlists, favoritos, histórico e configurações são mantidos ao atualizar. O Android pede confirmação uma vez antes de instalar.
Eigene Update-Adresse (leer = GitHub)¦Endereço de atualização próprio (vazio = GitHub)
Noch keine Veröffentlichung gefunden¦Nenhuma versão publicada encontrada ainda
Dieses Stream-Format wird nicht unterstützt.¦Este formato de stream não é compatível.
Verbindung hängt – neuer Versuch ({#0})¦Conexão travada – tentando novamente ({#0})
Verbindung unterbrochen – neuer Versuch ({#0})¦Conexão perdida – tentando novamente ({#0})
Stream beendet – verbinde neu …¦Stream encerrado – reconectando …
Das Portal hat keine Abspiel-Adresse geliefert. Später erneut versuchen.¦O portal não retornou um endereço de reprodução. Tente novamente mais tarde.
Zu wenig freier Speicher für den Live-Puffer¦Espaço livre insuficiente para o buffer ao vivo
Zu wenig freier Speicher¦Espaço livre insuficiente
Aufnahme läuft weiter – dafür braucht dein Anbieter 2 gleichzeitige Verbindungen¦A gravação continua – para isso seu provedor precisa permitir 2 conexões simultâneas
30 s zurück¦Voltar 30 s
Aufnehmen¦Gravar
Aufnahme beenden¦Parar gravação
Aufnahme gestartet¦Gravação iniciada
Aufnahme beendet¦Gravação encerrada
Aufnahme läuft¦Gravando
Sender unbekannt¦Canal desconhecido
Aufnahmen ({#0})¦Gravações ({#0})
Zurückspulen und Aufnehmen¦Retroceder e gravar
Live TV zurückspulen¦Retroceder TV ao vivo
Speichert den laufenden Sender auf dem Gerät. Dann kannst du pausieren, ohne etwas zu verpassen, mit den Spultasten zurückspringen und mit „Zurück zu Live“ wieder aufholen. Gilt für normale Live-Streams (nicht HLS).¦Armazena o canal atual no dispositivo. Assim você pode pausar sem perder nada, voltar com as teclas de retroceder e alcançar a transmissão com “Voltar ao vivo”. Funciona com streams ao vivo normais (não HLS).
Puffergröße¦Tamanho do buffer
Ungefähr: 512 MB reichen für 10–20 Minuten in HD. Der Puffer wird beim Schließen des Players gelöscht.¦Aproximadamente: 512 MB duram 10–20 minutos em HD. O buffer é apagado ao fechar o player.
Speicherort für Aufnahmen¦Local das gravações
Nachlauf nach Sendungsende¦Tempo extra após o fim do programa
Aufnehmen: im Player bei einem Sender auf den roten Knopf. Die laufende Sendung wird bis zu ihrem Ende laut TV-Guide aufgenommen, beginnt sie schon im Puffer, auch von ihrem Anfang an. Schaust du währenddessen einen anderen Sender, braucht das eine zweite Verbindung zu deinem Anbieter. Aufnahmen liegen im App-Ordner und werden beim Deinstallieren der App gelöscht.¦Para gravar: no player, pressione o botão vermelho em um canal. O programa atual é gravado até o fim indicado no guia de programação, e desde o início se ele ainda estiver no buffer. Assistir a outro canal enquanto isso exige uma segunda conexão com seu provedor. As gravações ficam na pasta do app e são apagadas ao desinstalar o app.
Noch keine Aufnahmen.¦Nenhuma gravação ainda.
Stoppen¦Parar
Abspielen: Live TV > „Aufnahmen“.¦Para assistir: TV ao vivo > “Gravações”.
Interner Speicher¦Armazenamento interno
USB / Speicherkarte {#0}¦USB / cartão de memória {#0}
frei¦livre
Verbindung beendet¦Conexão encerrada
Puffer beendet¦Buffer encerrado
Puffer-Adresse ungültig¦Endereço do buffer inválido
Mit anzeigen¦Mostrar junto
Nicht mehr mit anzeigen¦Não mostrar mais junto
wird mit angezeigt¦mostrada junto
„Mit anzeigen“ zeigt eine Playlist zusätzlich zur aktiven: Filme und Serien erscheinen zusammen (doppelte Filme nur einmal, gleiche Serien als eine Serie mit mehreren Quellen). In Filme und Serien filterst du über „Quelle“, im Live TV wechselst du die Quelle mit dem Knopf oben. Jede weitere Playlist braucht zusätzlich Speicher.¦“Mostrar junto” adiciona uma playlist à ativa: filmes e séries aparecem juntos (filmes duplicados só uma vez, séries iguais como uma só série com várias fontes). Em Filmes e Séries, filtre por “Fonte”; na TV ao vivo, troque a fonte com o botão no topo. Cada playlist extra usa mais memória.
Weitere Playlist wird geladen: {0}¦Carregando outra playlist: {0}
Zu wenig Speicher für eine weitere Playlist auf diesem Gerät.¦Memória insuficiente para outra playlist neste dispositivo.
Kartengröße, Seitenleiste, Raster¦Tamanho dos cards, barra lateral, grade
Karten¦Cards
Beim Markieren einer Auswahl zeigt die Vorschau sofort die Wirkung, OK übernimmt sie. Schriftgröße und Farben findest du unter „Design“.¦Ao destacar uma opção, a prévia mostra o efeito na hora; OK aplica. Tamanho do texto e cores ficam em “Design”.
Seitenleiste¦Barra lateral
Position¦Posição
Links¦Esquerda
Rechts¦Direita
Oben¦Topo
Immer mit Text¦Sempre com texto
Aus: nur Symbole, die Texte erscheinen beim Hineingehen. An: Symbole und Texte sind immer zu sehen (braucht mehr Platz).¦Desativado: só ícones, os textos aparecem ao entrar. Ativado: ícones e textos sempre visíveis (ocupa mais espaço).
Zurück bringt dich jederzeit in die Seitenleiste – auch wenn sie rechts oder oben steht.¦Voltar sempre leva você à barra lateral – mesmo quando ela está à direita ou no topo.
Sport¦Esportes
Jetzt live¦Ao vivo agora
Gleich¦Em breve
Sportsender¦Canais de esportes
Keine Sportsender gefunden¦Nenhum canal de esportes encontrado
Sportsender werden an Kategorie und Namen erkannt (z. B. „Sport“, „DAZN“, „Eurosport“). Sport-Sendungen erscheinen, sobald der TV-Guide geladen ist.¦Os canais de esportes são identificados pela categoria e pelo nome (ex.: “Sport”, “DAZN”, “Eurosport”). Os programas esportivos aparecem quando o guia de programação terminar de carregar.
TV-Guide wird noch geladen¦O guia de programação ainda está carregando
Stalker-Portal¦Portal Stalker
Portal-Adresse (z. B. http://portal.tld:8080/c/)¦Endereço do portal (ex.: http://portal.tld:8080/c/)
MAC-Adresse (z. B. 00:1A:79:12:34:56)¦Endereço MAC (ex.: 00:1A:79:12:34:56)
Sender und Filme werden geladen (Serien noch nicht). Die MAC-Adresse bekommst du von deinem Anbieter.¦Canais e filmes são carregados (séries ainda não). Você recebe o endereço MAC do seu provedor.
Angaben unvollständig: Portal-Adresse und MAC-Adresse (z. B. 00:1A:79:12:34:56) nötig.¦Dados incompletos: endereço do portal e endereço MAC (ex.: 00:1A:79:12:34:56) são necessários.
Das Portal antwortet nicht. Adresse prüfen.¦O portal não responde. Verifique o endereço.
Das Portal lehnt diese MAC-Adresse ab. MAC und Abo beim Anbieter prüfen.¦O portal rejeita este endereço MAC. Verifique o MAC e a assinatura com seu provedor.
Das Portal hat keine Sender geliefert. MAC-Adresse und Abo beim Anbieter prüfen.¦O portal não retornou canais. Verifique o endereço MAC e a assinatura com seu provedor.
Filme werden geladen ({#0}) …¦Carregando filmes ({#0}) …
Datei vom Gerät¦Arquivo do dispositivo
Auf diesem Gerät gibt es keine Dateiauswahl. Lade die Datei über die Handy-Seite rechts hoch.¦Este dispositivo não tem seletor de arquivos. Envie o arquivo pela página do celular à direita.
Das ist keine gültige M3U-Datei.¦Este não é um arquivo M3U válido.
Die Playlist-Datei ist nicht mehr da. Bitte erneut hochladen.¦O arquivo da playlist não existe mais. Envie-o novamente.
Playlist wird gelesen …¦Lendo playlist …
Zugangsdaten eintragen oder eine M3U-Datei hochladen und senden. Die Playlist wird danach automatisch geladen.¦Informe seus dados de acesso ou envie um arquivo M3U. A playlist será carregada automaticamente em seguida.
Auf ein anderes Gerät übertragen¦Transferir para outro dispositivo
Verlauf, Weiterschauen, Favoriten, Listen und Einstellungen mitnehmen – z. B. vom Fire TV auf das Handy. Auf Wunsch auch die Playlists mit Zugangsdaten.¦Leve seu histórico, Continuar assistindo, favoritos, listas e configurações – ex.: do Fire TV para o celular. Se quiser, também as playlists com dados de acesso.
Übertragung im WLAN starten¦Iniciar transferência por Wi-Fi
Übertragung beenden¦Parar transferência
Das Gerät ist nicht mit einem WLAN verbunden.¦O dispositivo não está conectado a uma rede Wi-Fi.
Am Handy (im selben WLAN) öffnen:¦Abra no celular (mesma rede Wi-Fi):
Altes Gerät: Sicherung herunterladen. Neues Gerät: hier ebenfalls starten und die Datei einspielen. Danach startet die App neu.¦Dispositivo antigo: baixe o backup. Dispositivo novo: inicie aqui também e envie o arquivo. Depois o app reinicia.
Google Drive¦Google Drive
Mit Google verbinden¦Conectar com o Google
Am Handy öffnen und den Code eingeben:¦Abra no celular e digite o código:
Die App speichert nur in ihrem eigenen, versteckten Ordner in deinem Google Drive und sieht sonst nichts.¦O app salva apenas na própria pasta oculta do seu Google Drive e não vê mais nada.
Nicht verbunden (abgelehnt oder Zeit abgelaufen).¦Não conectado (recusado ou tempo esgotado).
Verbunden.¦Conectado.
Letzte Sicherung: {0}¦Último backup: {0}
Playlists mit Zugangsdaten mitsichern¦Incluir playlists com dados de acesso
Praktisch für ein neues Gerät. Die Zugangsdaten liegen dann in deinem Google Drive.¦Prático para um dispositivo novo. Os dados de acesso ficam salvos no seu Google Drive.
Jetzt in Drive sichern¦Fazer backup no Drive agora
Aus Drive wiederherstellen¦Restaurar do Drive
Trennen¦Desconectar
Sichern …¦Fazendo backup …
Gesichert.¦Backup feito.
Laden …¦Carregando …
In Drive gibt es noch keine Sicherung.¦Ainda não há backup no Drive.
Die Sicherung in Drive ist ungültig.¦O backup no Drive é inválido.
Google antwortet nicht¦O Google não responde
Nicht mit Google verbunden¦Não conectado ao Google
Auf dem Fernseher lässt sich die Zwischenablage oft nicht nutzen: Unter Einstellungen > Speicher > „Übertragung im WLAN starten“ kannst du den Bericht am Handy öffnen (Seite „Letzten Absturzbericht anzeigen“) und dort kopieren.¦Na TV, muitas vezes não dá para usar a área de transferência: em Configurações > Armazenamento > “Iniciar transferência por Wi-Fi” você pode abrir o relatório no celular (página “Mostrar último relatório de falha”) e copiá-lo lá.
Auf dem Handy im Querformat¦Modo paisagem no celular
Die App ist für den Fernseher gebaut und wirkt quer am besten. Aus: dreht mit dem Gerät. Im Player: Tippen zeigt die Steuerung, Doppeltippen links/rechts spult, in der Mitte Pause.¦O app foi feito para TV e fica melhor na horizontal. Desativado: gira com o dispositivo. No player: tocar mostra os controles, tocar duas vezes à esquerda/direita avança ou retrocede, no meio pausa.
Oder mit der Handy-Kamera scannen¦Ou escaneie com a câmera do celular
QR-Code scannen¦Escanear QR code
Spende¦Doação
Hallo, ich bin der nigerianische Prinz und brauche dringend deine Hilfe! Spaß beiseite: Die App kostet dich nichts, mich aber ein paar Nächte Schlaf und sehr viel Kaffee. Wenn du mir ein paar Euro schicken möchtest, tanzt mein Kontostand vor Freude. Kein Zwang: Die App bleibt kostenlos, werbefrei und GPL-lizenziert.¦Olá, eu sou o príncipe nigeriano e preciso urgentemente da sua ajuda! Brincadeiras à parte: o app não custa nada para você, mas me custou algumas noites de sono e MUITO café. Se quiser me mandar alguns euros, minha conta bancária vai dançar de alegria. Sem obrigação: o app continua gratuito, sem anúncios e com licença GPL.
Mit der Handy-Kamera scannen¦Escaneie com a câmera do celular
Folgen werden geladen …¦Carregando episódios …
Die Folgen konnten nicht geladen werden. Verbindung oder Zugang prüfen.¦Não foi possível carregar os episódios. Verifique sua conexão ou acesso.
Dieser QR-Code ist der PayPal-Link für meine Spende. Mit der Handy-Kamera scannen, dann öffnet sich PayPal.¦Este QR code é o link do PayPal para doações. Escaneie com a câmera do celular e o PayPal será aberto.
Unterstütze Cloud Player¦Apoie o Cloud Player
Raster & Ansicht¦Grade e visualização
American Football¦Futebol americano
Basketball¦Basquete
Eishockey¦Hóquei no gelo
Fußball¦Futebol
Tennis¦Tênis
Motorsport¦Automobilismo
Kampfsport¦Lutas
Golf¦Golfe
Handball & Volleyball¦Handebol e vôlei
Rugby & Cricket¦Rúgbi e críquete
Radsport¦Ciclismo
Wintersport¦Esportes de inverno
Darts & Snooker¦Dardos e sinuca
Nächste 3 Std.¦Próximas 3 h
Morgen¦Amanhã
Keine Sendungen für diese Auswahl¦Nenhum programa para esta seleção
Zweiter Stream¦Segundo stream
Anordnung¦Layout
Bild-in-Bild¦Picture-in-picture
Geteilt¦Tela dividida
Ton von¦Áudio de
Hauptstream¦Stream principal
Streams tauschen¦Trocar streams
Zweiter Stream nicht abspielbar¦Não é possível reproduzir o segundo stream
Alle Playlists¦Todas as playlists
Fenstergröße¦Tamanho da janela
Gilt für Bild-in-Bild. Deine Auswahl wird gespeichert.¦Vale para picture-in-picture. Sua escolha fica salva.
"""
