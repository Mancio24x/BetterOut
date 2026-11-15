// Bozze: uscite, creazione, località, date, percorso, mappa, GPX. Dati d'esempio.
import { ic, logo, topo, profile, bottomNav, PROFILE_LAGAZUOI } from './lib.mjs';
import { bar, closebar, steps, field, seg, segField, chips, item, banner, btn, bottom, stepper, cbx, kv, chev, status } from './ui.mjs';
import { home, dettaglio } from './direzioni.mjs';

const S = [];
const add = (id, title, note, body, extra = {}) => S.push({ id, title, note, body, ...extra });

// ---------- 01 Home ----------
add('01a-home', 'Home', 'Prossima uscita, altre uscite, pulsante "+"', home(), { group: 'uscite' });
add('01b-home-vuota', 'Home — stato vuoto', 'Primo avvio: nessuna uscita', `
<header class="appbar"><div class="brand">${logo()}<span>BetterOut</span></div></header>
<main class="content"><h1 class="display">Le tue<br>uscite</h1>
<div class="empty"><div class="art">${ic('mountain')}</div><h2>Nessuna uscita, per ora</h2><p>Scegli dove andare: poi aggiungi percorso, meteo e zaino quando vuoi.</p>${btn('Nuova uscita', 'primary', 'plus')}</div></main>
${bottomNav('uscite')}`, { group: 'uscite' });
add('01c-home-scuro', 'Home — tema scuro', 'Stessa schermata con il tema scuro', home(), { group: 'uscite', mode: 'dark' });

// ---------- 02 Elenco ----------
const tripRow = (t, s, st) => `<li><div class="trip-main"><b>${t}</b><span>${s}</span></div>${status(st)}</li>`;
add('02a-elenco', 'Elenco uscite', 'Tutte le uscite con filtri per stato e ordinamento', `
${bar('Tutte le uscite', ['search'])}
<main class="content">
${chips(['Tutte · 6', 'Programmate · 2', 'Bozze · 2', 'Completate · 1', 'Annullate · 1'], [0], 'scroll')}
<div class="sub-h"><span>Ordinate per data</span><b class="rowx" style="gap:4px">${ic('filter', 'xs')}Ordina</b></div>
<ul class="trips">
${tripRow('Lagazuoi dal Passo Falzarego', 'sab 21 nov · 9,6 km · ↑ 690 m', 'prog')}
${tripRow('Rifugio Nuvolau', 'sab 5 dic · 7,2 km · ↑ 520 m', 'prog')}
${tripRow('Anello del Sorapis', 'Senza data · 12,8 km · ↑ 950 m', 'bozza')}
${tripRow('Alta Via 1, tappe 1–3', 'Senza data · nessun percorso', 'bozza')}
${tripRow('Monte Rite', 'lun 2 nov · 8,4 km · ↑ 610 m', 'ok')}
${tripRow('Croda da Lago', 'dom 25 ott · 13,1 km · ↑ 880 m', 'ann')}
</ul></main>`, { group: 'uscite' });
add('02b-elenco-filtro-vuoto', 'Elenco uscite — filtro senza risultati', 'Un filtro attivo che non trova nulla', `
${bar('Tutte le uscite', ['search'])}
<main class="content">
${chips(['Tutte · 5', 'Programmate · 2', 'Bozze · 2', 'Completate · 1', 'Annullate · 0'], [4], 'scroll')}
<div class="empty"><div class="art">${ic('filter')}</div><h2>Nessuna uscita annullata</h2><p>Le uscite che annulli restano qui, così puoi riprenderle o duplicarle.</p>${btn('Mostra tutte', 'secondary')}</div></main>`, { group: 'uscite' });

// ---------- 03 Creazione e modifica ----------
add('03a-nuova-uscita', 'Nuova uscita — titolo e località', 'Primo passo del flusso "+"; la bozza si può salvare in ogni momento', `
${closebar('Nuova uscita', 'Salva bozza')}
<main class="content">${steps(1, 6)}
<h2 class="h2">Dove vai?</h2><p class="lead">Basta un nome per salvare. Il resto lo aggiungi quando vuoi.</p>
${field('Nome dell’uscita', 'Lagazuoi dal Passo Falzarego', { cls: 'focus' })}
${field('Località', 'Passo Falzarego · 2105 m', { icon: 'pin', trail: '<span class="link">Cambia</span>', help: 'Veneto, Italia · serve per meteo e fuso orario' })}
<div class="field"><label>Tipo di attività</label>${chips(['Escursione', 'Trekking di più giorni', 'Via ferrata', 'Invernale', 'Corsa', 'Altro'], [0])}</div>
</main>${bottom(btn('Continua'))}`, { group: 'creazione' });
add('03b-modifica-uscita', 'Modifica uscita', 'Tutti i campi in una pagina, raggiungibile dal dettaglio', `
${bar('Modifica uscita')}
<main class="content">
${field('Nome dell’uscita', 'Lagazuoi dal Passo Falzarego')}
<div class="field"><label>Stato</label>${chips(['Bozza', 'Programmata', 'Completata', 'Annullata'], [1])}</div>
<div class="two">${field('Data', 'sab 21 nov 2026', { icon: 'calendar' })}${field('Partenza', '07:30', { icon: 'clock' })}</div>
${field('Località', 'Passo Falzarego · 2105 m', { icon: 'pin', trail: chev })}
${field('Percorso', 'Lagazuoi, anello · 9,6 km', { icon: 'route', trail: chev })}
${segField('Pernottamento', ['Nessuno', 'Tenda', 'Rifugio', 'Bivacco'], 0)}
${segField('Acqua lungo il percorso', ['Frequente', 'Scarsa', 'Assente'], 1)}
${segField('Ritmo', ['Lento', 'Medio', 'Veloce'], 1)}
</main>${bottom(btn('Salva modifiche'))}`, { group: 'creazione' });
add('03c-riepilogo', 'Nuova uscita — riepilogo', 'Ultimo passo: controllo e salvataggio', `
${closebar('Nuova uscita', 'Salva bozza')}
<main class="content">${steps(6, 6)}
<h2 class="h2">Tutto pronto?</h2><p class="lead">Puoi cambiare ogni voce anche dopo.</p>
<div class="list">
${item({ icon: 'pin', t: 'Lagazuoi dal Passo Falzarego', s: 'Passo Falzarego · Escursione', trail: chev })}
${item({ icon: 'calendar', t: 'sab 21 nov 2026 · 07:30', s: '1 giorno · fuso Europe/Rome', trail: chev })}
${item({ icon: 'route', t: 'Lagazuoi, anello', s: '9,6 km · ↑ 690 m · 4 h 20 stimata', trail: chev })}
${item({ icon: 'snow', t: 'Neve debole, −8° / 1°', s: 'Previsione per sab 21 nov', trail: chev })}
${item({ icon: 'backpack', t: 'Zaino da preparare', s: '9 suggerimenti da decidere', trail: chev })}
</div>
<div class="gap"></div>${segField('Salva come', ['Bozza', 'Programmata'], 1)}
</main>${bottom(btn('Salva uscita', 'primary', 'check'))}`, { group: 'creazione' });

// ---------- 04 Località ----------
const searchTop = (q, focus = true) => `<header class="appbar sub"><button class="iconbtn">${ic('back')}</button><div class="search grow" style="${focus ? 'border:2.500px solid var(--accent)' : ''}">${ic('search', 'sm')}<span class="grow">${q}</span>${ic('x', 'sm')}</div><span style="width:14px"></span></header>`;
add('04a-localita', 'Ricerca località', 'Risultati con tipo, quota e regione; alternativa "scegli sulla mappa"', `
${searchTop('falzarego')}
<main class="content"><div class="sub-h"><span>3 risultati</span></div>
<div class="list">
${item({ icon: 'peak', t: 'Passo Falzarego', s: 'Valico · 2105 m · Veneto, Italia', cls: 'sel', trail: cbx(true) })}
${item({ icon: 'peak', t: 'Piccolo Lagazuoi', s: 'Cima · 2778 m · Veneto, Italia' })}
${item({ icon: 'pin', t: 'Falzarego', s: 'Località · 1988 m · Trentino-Alto Adige, Italia' })}
${item({ icon: 'map', t: 'Scegli un punto sulla mappa', s: 'Se il luogo non è in elenco', trail: chev })}
</div>
<p class="help" style="margin-top:12px">Ricerca: Open-Meteo, dati GeoNames.</p>
</main>${bottom(btn('Usa Passo Falzarego'))}`, { group: 'creazione' });
add('04b-localita-errore', 'Ricerca località — errore di rete', 'Senza rete la ricerca fallisce ma l’uscita resta salvabile', `
${searchTop('rifugio lagazuoi', false)}
<main class="content"><div class="gap"></div>
${banner('err', 'wifioff', 'Ricerca non riuscita', 'Non riesco a raggiungere il servizio di ricerca. L’uscita si può salvare comunque: la località la aggiungi dopo.', ['Riprova', 'Scegli sulla mappa'])}
<div class="sub-h"><span>Usate di recente</span></div>
<div class="list">${item({ icon: 'clock', t: 'Passo Falzarego', s: 'Valico · 2105 m' })}${item({ icon: 'clock', t: 'Passo Giau', s: 'Valico · 2236 m' })}</div>
</main>`, { group: 'creazione' });

// ---------- 05 Date ----------
const cal = () => { let h = '<div style="display:grid;grid-template-columns:repeat(7,1fr);row-gap:2px;text-align:center;font:600 14px var(--f-body)">' + ['L', 'M', 'M', 'G', 'V', 'S', 'D'].map(d => `<span class="muted" style="height:28px;font-size:12px">${d}</span>`).join(''); for (let i = 0; i < 6; i++) h += '<span></span>'; for (let d = 1; d <= 30; d++) { const st = d === 21 ? 'background:var(--accent);color:var(--on-accent);font-weight:800' : d === 15 ? 'outline:1.500px solid var(--ink);outline-offset:-4px' : d < 15 ? 'color:var(--ink-2);opacity:.55' : ''; h += `<span style="height:44px;display:grid;place-items:center;border-radius:50%;${st}">${d}</span>`; } return h + '</div>'; };
add('05-date', 'Date e orari', 'Calendario, ora di partenza, durata, fuso orario della destinazione; "senza data" sempre possibile', `
${closebar('Nuova uscita', 'Salva bozza')}
<main class="content">${steps(2, 6)}
<h2 class="h2">Quando parti?</h2>
<div class="set" style="border:0;min-height:52px;padding-top:0"><div class="grow"><div class="t">Non ho ancora una data</div><div class="s">L’uscita resta in bozza, senza previsioni</div></div><span class="switch"></span></div>
<div class="panel"><h3><span class="rowx" style="color:var(--ink);font:800 15.500px var(--f-body)">${ic('back', 'sm')} Novembre 2026</span><span style="transform:rotate(180deg);display:inline-flex;color:var(--ink)">${ic('back', 'sm')}</span></h3>${cal()}</div>
<div class="gap"></div>
<div class="two">${field('Ora di partenza', '07:30', { icon: 'clock' })}<div class="field"><label>Giorni</label>${stepper('1')}</div></div>
${field('Fuso orario', 'Europe/Rome', { icon: 'lang', help: 'È quello della destinazione, non del telefono', trail: chev })}
</main>${bottom(btn('Continua'))}`, { group: 'creazione' });

// ---------- 06 Scelta percorso ----------
add('06-scelta-percorso', 'Percorso — disegna o importa', 'Tre modi per associare un percorso, oppure nessuno', `
${closebar('Nuova uscita', 'Salva bozza')}
<main class="content">${steps(3, 6)}
<h2 class="h2">Che percorso fai?</h2><p class="lead">Dal percorso arrivano distanza, dislivello, durata stimata e suggerimenti per lo zaino.</p>
<div class="list">
${item({ icon: 'edit', t: 'Disegna sulla mappa', s: 'Tocca i punti: l’app segue i sentieri o li unisce a mano', trail: chev })}
${item({ icon: 'file', t: 'Importa un file GPX', s: 'Da Komoot, Outdooractive, Garmin e altri', trail: chev })}
${item({ icon: 'route', t: 'Scegli dall’archivio', s: '3 percorsi salvati', trail: chev })}
</div>
<div class="gap"></div>${btn('Salta per ora', 'text')}
</main>`, { group: 'creazione' });

// ---------- 07 Editor mappa ----------
const mapTop = `<div class="mapbar"><span class="mfab">${ic('back')}</span><div class="mpill grow">${ic('search', 'sm')}<span class="muted">Cerca una zona</span></div><span class="mfab">${ic('layers')}</span></div><div class="mcol" style="top:72px"><span class="mfab">${ic('locate')}</span></div>`;
const editSheet = (o) => `<div class="mapsheet"><div class="grab"></div>
<div class="rowx"><div class="mode grow" style="box-shadow:none"><div class="${o.manual ? '' : 'on'}" style="flex:1;justify-content:center">${ic('route', 'xs')}Segui sentieri</div><div class="${o.manual ? 'on' : ''}" style="flex:1;justify-content:center">${ic('hand', 'xs')}Manuale</div></div>
<span class="mfab ${o.undo ? '' : 'off'}" style="box-shadow:none">${ic('undo')}</span><span class="mfab ${o.redo ? '' : 'off'}" style="box-shadow:none">${ic('redo')}</span></div>
${o.body}</div>`;
const editStats = (d, a, t, pts) => `<div class="big3" style="margin-top:12px"><div><b>${d}</b><span>distanza</span></div><div><b>${a}</b><span>salita</span></div><div><b>${t}</b><span>stimata</span></div></div>
<div class="btn-row" style="margin-top:12px">${btn('Inverti', 'secondary sm', 'swap')}${btn('Anello', 'secondary sm', 'loop')}${btn('Salva', 'primary sm', 'check')}</div>`;
const mapScreen = (map, sheet) => `<main class="content tight"><div class="mapfull">${map}${mapTop}<span class="attr">OpenFreeMap © OpenMapTiles · dati © OpenStreetMap</span></div>${sheet}</main>`;
add('07a-editor-vuoto', 'Editor percorso — inizio', 'Mappa vuota con istruzione; annulla e ripristina non ancora attivi', mapScreen(
  topo(412, 620, { route: false }) + `<span class="tip" style="top:300px">Tocca la mappa per mettere la partenza</span>`,
  editSheet({ body: `<p class="lead" style="margin:12px 0 0">Nessun punto. La partenza è il primo che tocchi; ogni tocco successivo aggiunge un punto.</p>` })), { group: 'mappa' });
add('07b-editor-punti', 'Editor percorso — segui sentieri', 'Punti collegati lungo i sentieri; statistiche aggiornate a ogni punto', mapScreen(
  topo(412, 560, { points: true }),
  editSheet({ undo: true, body: editStats('4,8 km', '650 m', '2 h 25') + `<div class="legend"><span><i></i>su sentiero</span><span>7 punti</span></div>` })), { group: 'mappa' });
add('07c-editor-selezione', 'Editor percorso — punto selezionato', 'Trascinare sposta il punto; il riquadro offre inserimento ed eliminazione anche senza gesti', mapScreen(
  topo(412, 560, { points: true, selected: 3 }) + `<div class="callout" style="left:96px;top:176px"><div>${ic('hand', 'sm')}Sposta</div><div>${ic('plus', 'sm')}Inserisci</div><div>${ic('flag', 'sm')}Fine qui</div><div class="red">${ic('trash', 'sm')}Elimina</div></div><span class="tip" style="top:330px">Punto 4 di 7 · trascina per spostarlo</span>`,
  editSheet({ undo: true, redo: true, body: editStats('4,8 km', '650 m', '2 h 25') })), { group: 'mappa' });
add('07d-editor-manuale', 'Editor percorso — tratto manuale', 'Il tratto manuale è tratteggiato e dichiarato non verificato', mapScreen(
  topo(412, 520, { points: true, manualFrom: 4 }),
  editSheet({ manual: true, undo: true, body: `<div class="gap"></div>${banner('warn', 'warn', 'Tratto manuale: 1,3 km', 'Linea retta fra i punti, non verificata su sentiero. Le quote di questo tratto mancano.', ['Chiedi le quote'])}${editStats('4,8 km', '— m', '— h')}<div class="legend"><span><i></i>su sentiero</span><span><i class="man"></i>manuale</span></div>` })), { group: 'mappa' });
add('07e-editor-errore', 'Editor percorso — routing non riuscito', 'L’errore è esplicito e la scelta resta all’utente: nessuna linea retta messa di nascosto', mapScreen(
  topo(412, 520, { points: true, routeN: [[0.13, 0.80], [0.22, 0.66], [0.33, 0.60], [0.41, 0.47]] }) + `<svg style="position:absolute;left:0;top:0" width="412" height="520"><circle cx="330" cy="130" r="11" fill="none" stroke="var(--danger)" stroke-width="3" stroke-dasharray="4 4"/><path d="M324 124l12 12M336 124l-12 12" stroke="var(--danger)" stroke-width="3" stroke-linecap="round"/></svg>`,
  editSheet({ undo: true, body: `<div class="gap"></div>${banner('err', 'warn', 'Nessun sentiero fino a questo punto', 'Il servizio di routing non ha trovato un collegamento. Il punto non è stato aggiunto.', ['Riprova', 'Usa tratto manuale'])}<div class="btn-row" style="margin-top:12px">${btn('Togli il punto', 'secondary sm', 'trash')}${btn('Salva', 'primary sm', 'check')}</div>` })), { group: 'mappa' });

// ---------- 08 GPX ----------
add('08a-gpx-anteprima', 'Importazione GPX — anteprima', 'File letto correttamente: mappa, statistiche e azioni', `
${bar('Importa GPX')}
<main class="content">
${item({ icon: 'file', t: 'lagazuoi-anello.gpx', s: 'GPX 1.1 · 412 kB · file originale conservato' })}
<div class="mapcard" style="height:170px">${topo(372, 170)}<span class="map-attr">© OpenStreetMap</span></div>
<div class="gap"></div>
<div class="big4"><div><b>9,6 km</b><span>distanza</span></div><div><b>690 m</b><span>salita</span></div><div><b>690 m</b><span>discesa</span></div><div><b>4 h 20</b><span>stimata</span></div></div>
<div class="gap"></div>
<div class="panel">${kv('Tracciati e segmenti', '1 tracciato · 1 segmento')}${kv('Punti', '2 846 (12 duplicati ignorati)')}${kv('Quote', 'presenti · 2105–2752 m')}${kv('Punti di interesse', '3')}</div>
${field('', 'Lagazuoi, anello', { help: 'Nome del percorso nell’archivio' })}
</main><div class="bottombar two">${btn('Solo archivio', 'secondary')}${btn('Associa all’uscita')}</div>`, { group: 'mappa' });
add('08b-gpx-avvisi', 'Importazione GPX — più tracciati, segmenti, quote assenti', 'I casi particolari sono dichiarati; i segmenti non vengono uniti', `
${bar('Importa GPX')}
<main class="content">
${item({ icon: 'file', t: 'alta-via-1.gpx', s: 'GPX 1.0 · 3,8 MB' })}
<div class="gap-s"></div>
${banner('warn', 'info', '2 tracciati, 3 segmenti separati', 'I segmenti restano separati: il vuoto fra loro non entra in distanza e dislivello.')}
<div class="sub-h"><span>Cosa importare</span><b>2 di 2</b></div>
<div class="list">
${item({ lead: cbx(true), t: 'Tappa 1 — Braies → Biella', s: '2 segmenti · 14,2 km · quote assenti' })}
${item({ lead: cbx(true), t: 'Tappa 2 — Biella → Fanes', s: '1 segmento · 12,7 km · ↑ 540 m' })}
</div>
<div class="gap"></div>
${banner('warn', 'warn', 'Tappa 1 senza quote', 'Dislivello e profilo non sono disponibili. Puoi chiederli a un modello del terreno: saranno indicati come stimati.', ['Chiedi le quote'])}
<div class="gap"></div>
<div class="big4"><div><b>26,9 km</b><span>distanza</span></div><div><b>—</b><span>salita</span></div><div><b>—</b><span>discesa</span></div><div><b>—</b><span>stimata</span></div></div>
</main><div class="bottombar two">${btn('Solo archivio', 'secondary')}${btn('Importa 2')}</div>`, { group: 'mappa' });
add('08c-gpx-errore', 'Importazione GPX — file non valido', 'Errore spiegato; nulla viene salvato', `
${bar('Importa GPX')}
<main class="content"><div class="empty"><div class="art" style="background:var(--err-bg);color:var(--err-ink)">${ic('warn')}</div><h2>Questo file non si può leggere</h2><p><b>foto-vetta.gpx</b> non contiene un GPX valido: manca l’elemento principale. Non è stato salvato nulla.</p>${btn('Scegli un altro file', 'primary', 'file')}${btn('Cosa posso importare?', 'text')}</div></main>`, { group: 'mappa' });
add('08d-gpx-lettura', 'Importazione GPX — lettura in corso', 'File grande: avanzamento visibile, annullabile', `
${bar('Importa GPX')}
<main class="content">
${item({ icon: 'file', t: 'traversata-dolomiti.gpx', s: 'GPX 1.1 · 48 MB' })}
<div class="gap"></div><div class="panel"><h3>Lettura del file <span>62 %</span></h3><div class="bar"><i style="width:62%"></i></div><p class="help" style="margin-top:8px">184 000 punti letti. Puoi continuare a usare l’app.</p></div>
<div class="gap"></div><div class="skel" style="height:170px;border-radius:26px"></div><div class="gap"></div><div class="skel" style="width:70%"></div><div class="gap-s"></div><div class="skel" style="width:45%"></div>
</main>${bottom(btn('Annulla importazione', 'secondary'))}`, { group: 'mappa' });

// ---------- 09 Dettaglio percorso ----------
add('09-percorso', 'Dettaglio percorso', 'Statistiche, profilo altimetrico, origine del percorso e uscite che lo usano', `
${bar('Percorso', ['share', 'more'])}
<main class="content">
<h1 class="h1" style="font-size:27px">Lagazuoi, anello</h1>
<div class="rowx" style="margin-top:8px;gap:6px"><span class="tag">${ic('edit', 'xs')}Disegnato</span><span class="tag">${ic('route', 'xs')}Su sentiero</span><span class="tag">Usato in 1 uscita</span></div>
<div class="mapcard" style="height:160px">${topo(372, 160)}<button class="map-btn">${ic('map', 'xs')}Apri mappa</button><span class="map-attr">© OpenStreetMap</span></div>
<div class="gap"></div>
<div class="big4"><div><b>9,6 km</b><span>distanza</span></div><div><b>690 m</b><span>salita</span></div><div><b>690 m</b><span>discesa</span></div><div><b>4 h 20</b><span>stimata</span></div></div>
<div class="gap"></div>
<div class="panel"><h3>Profilo altimetrico <span>soglia 3 m</span></h3>${profile(340, 130, PROFILE_LAGAZUOI, { marker: 8 })}
<div class="kv" style="border-top:0;padding-bottom:4px"><span>Quota minima · massima</span><b>2105 m · 2752 m</b></div><p class="help">Quote dal servizio di routing. Durata: metodo escursionistico, ritmo medio, senza soste.</p></div>
</main><div class="bottombar two">${btn('Modifica', 'secondary', 'edit')}${btn('Esporta GPX', 'primary', 'download')}</div>`, { group: 'mappa' });

// ---------- 17 Mappa e archivio ----------
add('17a-mappa', 'Mappa', 'Scheda Mappa: cartografia escursionistica, strati, posizione, nuovo percorso', `
<main class="content tight"><div class="mapfull">${topo(412, 810, { route: true })}
<div class="mapbar"><div class="mpill grow">${ic('search', 'sm')}<span class="muted">Cerca una zona</span></div><span class="mfab">${ic('layers')}</span></div>
<div class="mapbar" style="top:66px"><div class="tabs" style="width:216px;box-shadow:0 3px 10px rgba(0,0,0,.28)"><div class="on">Mappa</div><div>Percorsi · 3</div></div></div>
<div class="mcol" style="bottom:96px"><span class="mfab">${ic('locate')}</span></div>
<span class="attr" style="bottom:8px">OpenFreeMap © OpenMapTiles · dati © OpenStreetMap</span></div></main>
<button class="fab" style="width:auto;padding:0 20px 0 16px"><svg class="ic" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg><span style="display:inline">Nuovo percorso</span></button>
${bottomNav('mappa')}`, { group: 'mappa' });
add('17b-archivio-percorsi', 'Archivio percorsi', 'Percorsi salvati con origine, statistiche e stato dei dati', `
<header class="appbar"><span class="appbar-title" style="margin:0;font-size:19px;font-weight:800">Mappa</span><span class="grow"></span><button class="iconbtn">${ic('search')}</button></header>
<main class="content">
<div class="tabs"><div>Mappa</div><div class="on">Percorsi · 3</div></div>
<div class="gap"></div>
<div class="list">
${item({ icon: 'route', t: 'Lagazuoi, anello', s: 'Disegnato · 9,6 km · ↑ 690 m · 1 uscita', trail: chev })}
${item({ icon: 'file', t: 'Sorapis da Passo Tre Croci', s: 'GPX · 12,8 km · ↑ 950 m · nessuna uscita', trail: chev })}
${item({ icon: 'file', t: 'Alta Via 1 — tappa 1', s: 'GPX · 14,2 km · <span class="tag warn">quote assenti</span>', trail: chev })}
</div>
<div class="gap"></div>
${banner('', 'info', 'Tracce sempre disponibili', 'I percorsi salvati si aprono anche senza rete. Lo sfondo della mappa no, a meno di scaricare l’area.', ['Mappe offline'])}
</main>
<button class="fab" style="width:auto;padding:0 20px 0 16px"><svg class="ic" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg><span style="display:inline">Nuovo percorso</span></button>
${bottomNav('mappa')}`, { group: 'mappa' });
add('17c-strati-offline', 'Mappa — strati e mappe offline', 'Scelta dello sfondo e scaricamento esplicito di un’area con dimensione e rimozione', `
<main class="content tight"><div class="mapfull">${topo(412, 883, { route: false })}</div></main>
<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Strati della mappa</h2>
<div class="list" style="margin-top:8px">
${item({ lead: '<span class="radio on"></span>', t: 'Escursionistica', s: 'Sentieri, rifugi e cime · OpenFreeMap' })}
${item({ lead: '<span class="radio"></span>', t: 'Topografica', s: 'Curve di livello · OpenTopoMap · solo online' })}
</div>
<div class="set" style="border:0"><div class="grow"><div class="t">Rilievo ombreggiato</div><div class="s">Mapterhorn</div></div><span class="switch on"></span></div>
<div class="sub-h"><span>Mappe offline</span><b>1 area · 38 MB</b></div>
${item({ icon: 'download', t: 'Scarica l’area visibile', s: 'Circa 1 900 tessere · 31 MB · solo strato escursionistico', trail: chev })}
<div class="gap-s"></div>
${item({ icon: 'map', t: 'Falzarego e Lagazuoi', s: '38 MB · scaricata il 14 nov', trail: `<span style="color:var(--danger)">${ic('trash')}</span>` })}
</div></div>`, { group: 'mappa' });

export const SCREENS_1 = S;
export { dettaglio };
