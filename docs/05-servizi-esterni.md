# Servizi esterni: cosa usa BetterOut, a che condizioni, con quali limiti

Verifiche fatte il 15 novembre 2026. "Provato" significa che è stata inviata una richiesta reale e letta la risposta; "letto" che sono state lette le condizioni pubblicate; "non verificato" è dichiarato come tale.

L'app non ha un proprio server: ogni richiesta parte dal telefono verso il servizio indicato.

## Ruoli distinti

| Ruolo | Scelta | Chiave | Stato della verifica |
|---|---|---|---|
| Libreria che disegna la mappa | MapLibre Native per Android | no | Pubblicata su Maven Central; versione da fissare alla creazione del progetto |
| Fornitore delle mappe (sfondo) | OpenFreeMap, stile vettoriale su dati OpenStreetMap | no | Provato: lo stile `liberty` risponde |
| Rilievo ombreggiato | Mapterhorn (tessere di quota) | no | Provato: il descrittore risponde, codifica "terrarium" |
| Carta topografica con curve di livello (strato facoltativo) | OpenTopoMap | no | Letto: uso in app ammesso senza carico eccessivo |
| Ricerca località | Open-Meteo Geocoding | no | Provato: trova anche cime (es. Tofana di Rozes, 3225 m) |
| Routing escursionistico | BRouter, server pubblico, profilo `hiking-mountain` | no | Provato: restituisce geometria con quote |
| Routing alternativo | openrouteservice, profilo `foot-hiking` | sì, dell'utente | Limiti letti in parte; quote giornaliere non verificate |
| Quote altimetriche | Open-Meteo Elevation (Copernicus GLO-90) | no | Provato |
| Meteo | Open-Meteo Forecast | no | Provato, anche con più punti e con data fuori orizzonte |

## Mappe

**OpenFreeMap** fornisce tessere vettoriali nello schema OpenMapTiles, senza chiave né registrazione. Lo schema contiene sentieri, cime e rifugi, ma non le curve di livello. Lo stile di BetterOut parte da quello pubblico e mette in evidenza i sentieri.

- Attribuzione obbligatoria e sempre visibile: OpenFreeMap, © OpenMapTiles, dati © OpenStreetMap.
- Scaricamento per l'uso offline: ammesso fino a 3000 tessere o 50 MB per scaricamento, con al massimo 4 richieste parallele e un identificativo dell'app riconoscibile. BetterOut applica questi tetti a ogni area e mostra dimensione stimata, avanzamento e pulsante di rimozione.

**Mapterhorn** fornisce le quote per il rilievo ombreggiato. Attribuzione "© Mapterhorn". Le condizioni d'uso complete non sono state lette: prima di abilitare lo strato nell'APK vanno verificate; in caso di dubbio lo strato resta spento.

**OpenTopoMap** è l'unica fonte senza chiave, fra quelle considerate, che mostra le curve di livello. È uno strato facoltativo ("Topografica"), solo online: niente scaricamento offline, perché il servizio chiede di evitare scaricamenti di massa e non garantisce disponibilità. Licenza CC-BY-SA; attribuzione: "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)".

Fornitori con chiave (MapTiler Outdoor, Thunderforest) darebbero curve di livello vettoriali, ma richiedono una chiave che non può stare nel repository: restano una possibile aggiunta con chiave inserita dall'utente.

## Ricerca località

**Open-Meteo Geocoding** (`geocoding-api.open-meteo.com/v1/search`), basato su GeoNames. Restituisce nome, coordinate, quota, fuso orario e regione; accetta `language=it`. Copre paesi e cime; rifugi e toponimi minori possono mancare, perciò la località si può anche fissare toccando la mappa.

Photon (komoot) è stato provato e scartato come fonte principale: non supporta l'italiano (solo predefinito, tedesco, inglese, francese) e chiede un uso contenuto senza numeri dichiarati.

## Routing

**BRouter** (`brouter.de/brouter`): nessuna chiave, profili escursionistici, quote incluse nella risposta. È un servizio gratuito gestito da volontari e **non pubblica condizioni d'uso né limiti**: è il rischio principale di questa scelta. Contromisure: una richiesta per tratto e solo su azione dell'utente, nessuna richiesta automatica o ripetuta, risultati salvati con il percorso, identificativo dell'app nelle richieste, fornitore sostituibile dalle impostazioni.

**openrouteservice**: profilo `foot-hiking`, fino a 50 punti per richiesta e 6000 km. Richiede una chiave personale gratuita. BetterOut non ne include una: chi vuole usarlo inserisce la propria nelle impostazioni, dove resta sul dispositivo ed è esclusa dai backup. Quote giornaliere e condizioni sulla distribuzione delle chiavi non sono state verificate (la pagina delle condizioni non era leggibile in automatico).

In entrambi i casi il risultato dipende dai dati OpenStreetMap: un sentiero mancante o chiuso non viene segnalato. Attribuzione: © OpenStreetMap.

## Quote altimetriche

**Open-Meteo Elevation** (`api.open-meteo.com/v1/elevation`): fino a 100 punti per richiesta, modello Copernicus GLO-90 a 90 m di risoluzione. Con questa risoluzione creste e forre risultano smussate: i valori sono etichettati "quote da modello del terreno" e non sostituiscono quelle registrate in un GPX. Per percorsi lunghi i punti sono campionati e richiesti a blocchi. Citazione richiesta: Copernicus DEM (doi:10.5270/ESA-c5d3d65) e Open-Meteo.

## Meteo

**Open-Meteo Forecast** (`api.open-meteo.com/v1/forecast`).

- Condizioni lette: gratuito per uso non commerciale, senza chiave; meno di 10 000 richieste al giorno, 5000 l'ora, 600 al minuto; dati con licenza CC-BY 4.0, quindi fonte sempre citata. Un'app senza pubblicità né abbonamenti rientra nell'uso non commerciale; se BetterOut diventasse commerciale servirebbe un piano a pagamento.
- Orizzonte: fino a 16 giorni. Provato: una data oltre l'orizzonte restituisce errore 400 con l'intervallo ammesso, che l'app traduce in "Previsioni non ancora disponibili".
- Più punti in una sola richiesta (coordinate separate da virgola) e quota esplicita per punto (`elevation`), così la previsione della cima è corretta per la quota. Provato con due punti.
- `timezone=auto` restituisce il fuso della località, usato come fuso della destinazione.
- I limiti valgono per indirizzo IP: su reti condivise si possono incontrare prima. L'app tiene la cache e non interroga più di una volta ogni tre ore per uscita, salvo richiesta esplicita.

I dati climatici del periodo verrebbero dall'archivio storico di Open-Meteo: da provare nella fase meteo, prima di mostrarli.

## Riservatezza

Ai servizi arrivano solo coordinate, date e testo cercato, mai dati personali o identificativi dell'utente. Nessuna richiesta parte senza un'azione collegata (aprire un'uscita, cercare, calcolare un tratto, muovere la mappa).
