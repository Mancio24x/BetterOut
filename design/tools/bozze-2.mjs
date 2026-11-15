// Bozze: dettaglio uscita, meteo, preparazione, suggerimenti, checklist, inventario, impostazioni, backup, dialoghi.
import { ic, bottomNav } from './lib.mjs';
import { bar, closebar, steps, field, seg, segField, chips, item, banner, btn, bottom, stepper, cbx, kv, chev } from './ui.mjs';
import { dettaglio } from './direzioni.mjs';

const S = [];
const add = (id, title, note, body, extra = {}) => S.push({ id, title, note, body, ...extra });

// ---------- 10 Dettaglio uscita ----------
add('10a-dettaglio', 'Dettaglio uscita', 'Stato, data, mappa, statistiche, meteo per punto, zaino; azione principale in basso', dettaglio(), { group: 'uscite' });
add('10b-dettaglio-scuro', 'Dettaglio uscita — tema scuro', 'Stessa schermata con il tema scuro', dettaglio(), { group: 'uscite', mode: 'dark' });
add('10c-dettaglio-menu', 'Dettaglio uscita — altre azioni', 'Duplicazione, cambio di stato, eliminazione', dettaglio() + `<div class="scrim" style="background:rgba(20,16,10,.25)"></div><div class="menu"><div>${ic('copy', 'sm')}Duplica</div><div>${ic('check', 'sm')}Segna come completata</div><div>${ic('x', 'sm')}Annulla uscita</div><div>${ic('share', 'sm')}Condividi riepilogo</div><div class="red">${ic('trash', 'sm')}Elimina</div></div>`, { group: 'uscite' });
add('10d-duplica', 'Duplica uscita', 'La copia è indipendente: nuova data e spunte azzerate senza toccare l’originale', dettaglio() + `<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Duplica uscita</h2><p class="lead">Crea una nuova bozza con gli stessi dati. L’originale non cambia.</p>
${field('Nome', 'Lagazuoi dal Passo Falzarego (copia)')}
${segField('Data della copia', ['Senza data', 'Scegli data'], 0)}
<div class="set" style="border:0"><div class="grow"><div class="t">Azzera le spunte dello zaino</div><div class="s">Mantiene oggetti e quantità</div></div><span class="switch on"></span></div>
<div class="btn-row">${btn('Annulla', 'secondary')}${btn('Duplica', 'primary', 'copy')}</div></div></div>`, { group: 'uscite' });

// ---------- 11 Meteo ----------
const wxHead = (pt = 0) => `${bar('Meteo', ['refresh'])}<main class="content">
<div class="tabs"><div class="${pt < 2 ? 'on' : ''}">Giorni</div><div class="${pt === 2 ? 'on' : ''}">Ore</div></div><div class="gap"></div>
${chips(['Partenza · 2105 m', 'Quota max · 2752 m'], [pt === 1 ? 1 : 1])}<div class="gap"></div>`;
const wxFoot = `<p class="help" style="margin-top:10px">Aggiornato oggi alle 09:12 · Fonte: Open-Meteo (CC BY 4.0)</p>`;
add('11a-meteo-giorno', 'Meteo giornaliero', 'Previsione per punto e quota, con fonte e ora di aggiornamento', `${wxHead(1)}
<div class="panel"><h3>sab 21 nov <span>Quota massima · 2752 m</span></h3>
<div class="rowx" style="gap:14px;margin-bottom:6px">${ic('snow', 'lg')}<div class="grow"><div style="font:600 30px var(--f-display);line-height:1">−8° / −4°</div><div class="muted" style="font-size:14px">Neve debole · percepita −14°</div></div></div>
${kv('Precipitazioni', '40 % · 1,2 mm')}${kv('Neve', '2 cm')}${kv('Vento · raffiche', '28 km/h da NO · 45 km/h')}${kv('Zero termico', '1900 m')}${kv('Alba · tramonto', '07:21 · 16:33')}</div>
<div class="gap"></div>
<div class="panel"><h3>Giorni vicini</h3>
<div class="day"><div class="d">ven 20<small>nov</small></div>${ic('cloud')}<div class="w">Nuvoloso<small>vento 18 km/h</small></div><div class="tt">−6° / −1°</div></div>
<div class="day"><div class="d">dom 22<small>nov</small></div>${ic('sun')}<div class="w">Sereno<small>vento 12 km/h</small></div><div class="tt">−9° / −3°</div></div></div>
${wxFoot}</main>`, { group: 'meteo' });
const hrs = [['07', 'snow', '−8°', '35 %'], ['08', 'snow', '−8°', '40 %'], ['09', 'snow', '−7°', '40 %'], ['10', 'cloud', '−6°', '25 %'], ['11', 'cloud', '−5°', '20 %'], ['12', 'cloud', '−4°', '15 %'], ['13', 'sun', '−4°', '5 %'], ['14', 'sun', '−4°', '5 %'], ['15', 'cloud', '−5°', '10 %'], ['16', 'cloud', '−6°', '10 %'], ['17', 'moon', '−7°', '10 %'], ['18', 'moon', '−8°', '10 %']];
add('11b-meteo-ore', 'Meteo orario', 'Ora per ora nella giornata dell’uscita, con vento e raffiche', `${wxHead(2)}
<div class="panel"><h3>sab 21 nov, ora per ora <span>ora locale</span></h3>
<div class="hours">${hrs.map(h => `<div><span class="p">${h[0]}</span>${ic(h[1], 'sm')}<b>${h[2]}</b><span class="p">${h[3]}</span></div>`).join('')}</div>
<p class="help" style="margin-top:8px">Sotto la temperatura: probabilità di precipitazione.</p></div>
<div class="gap"></div>
<div class="panel"><h3>Vento in quota</h3>${kv('Mattina (07–11)', '28 km/h · raffiche 45')}${kv('Pomeriggio (12–16)', '20 km/h · raffiche 32')}${kv('Partenza 07:30 → rientro stimato', '11:50')}${kv('Tramonto', '16:33 · 4 h 40 di margine')}</div>
${wxFoot}</main>`, { group: 'meteo' });
add('11c-meteo-non-disponibile', 'Meteo — previsioni non ancora disponibili', 'Data oltre l’orizzonte: nessun dato inventato; il clima del periodo è separato e dichiarato', `${bar('Meteo', ['refresh'])}<main class="content">
${banner('', 'calendar', 'Previsioni non ancora disponibili', 'L’uscita è il 5 dicembre: le previsioni arrivano fino a 16 giorni. Saranno consultabili dal 19 novembre.')}
<div class="gap"></div>
<div class="panel" style="border:1.500px dashed var(--ink-2);box-shadow:none"><h3>Clima del periodo <span>non è una previsione</span></h3>
<p class="help" style="margin-bottom:8px">Medie di inizio dicembre negli ultimi anni a 2575 m. Il tempo reale può essere molto diverso.</p>
${kv('Minima tipica', '−9 °C')}${kv('Massima tipica', '−2 °C')}${kv('Giorni con precipitazioni', '2 su 10')}</div>
<p class="help" style="margin-top:10px">Fonte: archivio storico Open-Meteo</p></main>`, { group: 'meteo' });
add('11d-meteo-offline', 'Meteo — senza rete e caricamento', 'Offline si vede l’ultimo dato con la sua età; l’errore non blocca nulla', `${bar('Meteo', ['refresh'])}<main class="content">
${banner('warn', 'wifioff', 'Sei senza rete', 'Mostro l’ultima previsione salvata, di 2 giorni fa. Può non essere più valida.', ['Riprova'])}
<div class="gap"></div>
<div class="panel"><h3>sab 21 nov <span>salvata gio 19 nov, 18:40</span></h3>
<div class="rowx" style="gap:14px;margin-bottom:6px">${ic('cloud', 'lg')}<div class="grow"><div style="font:600 30px var(--f-display);line-height:1">−7° / −3°</div><div class="muted" style="font-size:14px">Nuvoloso · percepita −12°</div></div></div>
${kv('Precipitazioni', '20 % · 0,2 mm')}${kv('Vento · raffiche', '22 km/h da NO · 38 km/h')}</div>
<div class="sub-h"><span>Durante il caricamento</span></div>
<div class="panel"><div class="skel" style="width:40%"></div><div class="gap-s"></div><div class="skel" style="height:34px;width:60%"></div><div class="gap-s"></div><div class="skel"></div><div class="gap-s"></div><div class="skel" style="width:80%"></div></div>
</main>`, { group: 'meteo' });

// ---------- 12 Preparazione guidata ----------
add('12a-preparazione-percorso', 'Preparazione guidata — percorso e date', 'Passo 1: da dove arrivano i dati; si può preparare anche senza percorso', `
${closebar('Prepara lo zaino')}
<main class="content">${steps(1, 3)}
<h2 class="h2">Da cosa partiamo</h2><p class="lead">Controlla i dati dell’uscita: da qui nascono stima e suggerimenti.</p>
<div class="list">
${item({ icon: 'route', t: 'Lagazuoi, anello', s: '9,6 km · ↑ 690 m · ↓ 690 m · quota max 2752 m', trail: chev })}
${item({ icon: 'calendar', t: 'sab 21 novembre 2026', s: 'Partenza 07:30 · 1 giorno', trail: chev })}
${item({ icon: 'snow', t: 'Previsione disponibile', s: '−8° / 1° · neve debole · raffiche 45 km/h', trail: chev })}
</div>
<div class="sub-h"><span>Senza percorso</span></div>
<div class="three">${field('km', '—', { ph: true })}${field('Salita (m)', '—', { ph: true })}${field('Quota max', '—', { ph: true })}</div>
<p class="help">Senza percorso si assume andata e ritorno, con discesa uguale alla salita.</p>
</main>${bottom(btn('Continua'))}`, { group: 'preparazione' });
add('12b-preparazione-ritmo', 'Preparazione guidata — ritmo, notti, acqua', 'Passo 2: la stima si aggiorna a ogni scelta ed è correggibile a mano', `
${closebar('Prepara lo zaino')}
<main class="content">${steps(2, 3)}
${segField('Ritmo', ['Lento', 'Medio', 'Veloce'], 1)}
<div class="field"><label>Ore di cammino al giorno</label><div class="rowx"><span class="grow help">Soste escluse</span>${stepper('6')}</div></div>
${segField('Dove dormi', ['Non dormo', 'Tenda', 'Rifugio', 'Bivacco'], 0)}
${segField('Acqua lungo il percorso', ['Frequente', 'Scarsa', 'Assente'], 1)}
<div class="panel"><h3>Stima <span>metodo escursionistico</span></h3>
<div class="big3"><div><b>4 h 20</b><span>di cammino</span></div><div><b>1</b><span>giorno</span></div><div><b>0</b><span>notti</span></div></div>
<div class="rowx" style="margin-top:10px"><span class="grow" style="font:600 14px var(--f-body)">Correggi i giorni</span>${stepper('1')}</div>
<p class="help" style="margin-top:6px">4 km/h in piano, 300 m/h in salita, 500 m/h in discesa. È una stima: non tiene conto di neve, soste e allenamento.</p></div>
</main>${bottom(btn('Vedi cosa portare'))}`, { group: 'preparazione' });

// ---------- 13 Suggerimenti ----------
const sug = (name, why, tag, acts) => `<div class="sug"><div class="rowx"><div class="t grow" style="font:700 16px var(--f-body)">${name}</div>${tag}</div><div class="why">${ic('info', 'xs')}<span>${why}</span></div><div class="acts">${acts}</div></div>`;
const A = (l, on = false, icon = '') => `<span class="chip ${on ? 'on' : ''}">${icon ? ic(icon, 'xs') : ''}${l}</span>`;
add('13a-suggerimenti', 'Suggerimenti attrezzatura — da decidere', 'Ogni voce ha il suo motivo; si distinguono mancanti, disponibili in inventario e alternative', `
${closebar('Prepara lo zaino')}
<main class="content">${steps(3, 3)}
<h2 class="h2">Cosa portare</h2>
<div class="tabs"><div class="on">Da decidere · 9</div><div>Già nello zaino · 14</div></div><div class="gap"></div>
<div class="list">
${sug('Ramponcini', 'Minima prevista −8 °C a 2752 m: ghiaccio possibile.', `<span class="tag warn">${ic('warn', 'xs')}Mancante</span>`, A('Aggiungi · 450 g stimati', false, 'plus') + A('Non serve'))}
${sug('Guscio antivento', 'Raffiche previste fino a 45 km/h in quota.', `<span class="tag">${ic('box', 'xs')}In inventario</span>`, A('Giacca hardshell · 380 g', true, 'check') + A('2 alternative'))}
${sug('Ghette', 'Neve prevista: 2 cm.', `<span class="tag">${ic('box', 'xs')}In inventario</span>`, A('Aggiungi · 210 g', false, 'plus') + A('Non serve', true, 'check'))}
</div>
</main><div class="bottombar">${btn('Applica 2 scelte', 'primary', 'check')}<div class="hint">Le voci non decise restano qui. Nessun oggetto viene aggiunto due volte.</div></div>`, { group: 'preparazione' });
add('13b-suggerimenti-volume', 'Suggerimenti — volume, peso e avvertenza', 'Volume stimato rispetto allo zaino, con i dati mancanti in evidenza', `
${closebar('Prepara lo zaino')}
<main class="content">${steps(3, 3)}
<h2 class="h2">Cosa portare</h2>
<div class="panel"><h3>Entra nello zaino? <span>Zaino 30 L</span></h3>
<div class="volbar"><i style="width:68%"></i><i class="unk" style="width:12%"></i></div>
<div class="kv" style="border:0;padding-bottom:2px"><span>Volume stimato</span><b>20,5 L su 30 L</b></div>
<p class="help">${ic('warn', 'xs')} 3 oggetti senza volume (tratteggio): il totale è parziale.</p></div>
<div class="gap"></div>
<div class="panel"><h3>Peso con le scelte fatte</h3><div class="big3"><div><b>7,2 kg</b><span>nello zaino</span></div><div><b>2,3 kg</b><span>indossato</span></div><div><b>1,0 L</b><span>acqua</span></div></div>
<p class="help" style="margin-top:8px">Acqua scarsa lungo il percorso: 1,0 L per 4 h 20 di cammino. 1 peso è una stima tipica.</p></div>
<div class="gap"></div>
${banner('warn', 'shield', 'Sono suggerimenti, non garanzie', 'Nascono da regole su percorso, date e meteo. Non conoscono lo stato del sentiero, la neve al suolo né la tua esperienza: decidi tu.')}
</main>${bottom(btn('Vai alla checklist', 'primary', 'backpack'))}`, { group: 'preparazione' });
add('13c-suggerimenti-alternative', 'Suggerimenti — alternative per la stessa funzione', 'Più oggetti dell’inventario con lo stesso uso: si sceglie quale portare', `
${closebar('Prepara lo zaino')}
<main class="content">${steps(3, 3)}<h2 class="h2">Cosa portare</h2></main>
<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Guscio antivento</h2><p class="lead">Hai 3 oggetti con questo uso. Ne basta uno.</p>
<div class="list">
${item({ lead: '<span class="radio on"></span>', t: 'Giacca hardshell', r: '380 g', rs: '1,2 L', cls: 'sel' })}
${item({ lead: '<span class="radio"></span>', t: 'Antivento leggero', r: '120 g', rs: '0,4 L' })}
${item({ lead: '<span class="radio"></span>', t: 'Poncho', s: '<span class="tag est">peso stimato</span>', r: '300 g', rs: 'volume —' })}
</div><div class="gap"></div>
<div class="btn-row">${btn('Non serve', 'secondary')}${btn('Usa questo', 'primary', 'check')}</div></div></div>`, { group: 'preparazione' });

// ---------- 14 Checklist ----------
const row = (on, name, q, w, extra = '') => `<div class="item ${on ? 'done' : ''}">${cbx(on)}<div class="grow"><div class="t">${name}</div>${extra ? `<div class="s">${extra}</div>` : ''}</div><div class="r">${w}${q > 1 ? `<small>× ${q}</small>` : ''}</div></div>`;
const ckSummary = (n, pct) => `<div class="panel"><h3>${n} di 22 pronti <span>6,8 kg nello zaino</span></h3><div class="bar"><i style="width:${pct}%"></i></div>
<div class="big4" style="margin-top:10px"><div><b>4,9 kg</b><span>base</span></div><div><b>1,9 kg</b><span>consumabili</span></div><div><b>2,3 kg</b><span>indossato</span></div><div><b>9,1 kg</b><span>totale</span></div></div></div>`;
add('14a-checklist', 'Checklist zaino', 'Avanzamento, pesi per tipo, oggetti per categoria con subtotale, quantità e spunte', `
${bar('Zaino · Lagazuoi', ['plus', 'more'])}
<main class="content">${ckSummary(14, 64)}
<div class="cat-h">Strati <span>3 di 4 · 1,24 kg</span></div>
<div class="list">
${row(true, 'Giacca hardshell', 1, '380 g')}
${row(true, 'Piumino', 1, '310 g')}
${row(false, 'Calze di ricambio', 2, '140 g', '70 g l’una')}
${row(true, 'Pile', 1, '410 g', '<span class="tag">indossato</span>')}
</div>
<div class="cat-h">Cucina e acqua <span>1 di 3 · 1,31 kg</span></div>
<div class="list">
${row(true, 'Borraccia 1 L (vuota)', 1, '160 g')}
${row(false, 'Acqua', 1, '1000 g', '1,0 L · <span class="tag">consumabile</span>')}
${row(false, 'Barrette', 3, '150 g', '<span class="tag">consumabile</span>')}
</div></main>`, { group: 'preparazione' });
add('14b-checklist-riga', 'Checklist — dettaglio di una riga', 'Quantità, tipo e nota valgono solo per questa uscita; l’inventario non cambia', `
${bar('Zaino · Lagazuoi', ['plus', 'more'])}<main class="content">${ckSummary(14, 64)}</main>
<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Calze di ricambio</h2><p class="lead">70 g l’una · Strati · dall’inventario</p>
<div class="field"><label>Quantità</label><div class="rowx"><span class="grow" style="font:700 16px var(--f-body)">140 g in tutto</span>${stepper('2')}</div></div>
${segField('Tipo per questa uscita', ['Base', 'Indossato', 'Consumabile'], 0)}
${field('Nota per questa uscita', 'Un paio pesante per la cima', { opt: true })}
<div class="btn-row">${btn('Togli dallo zaino', 'secondary', 'trash')}${btn('Fatto')}</div></div></div>`, { group: 'preparazione' });
add('14c-checklist-aggiungi', 'Checklist — aggiungi dall’inventario', 'Ricerca e selezione multipla; ciò che è già nello zaino è indicato', `
${closebar('Aggiungi allo zaino')}
<main class="content"><div class="search">${ic('search', 'sm')}<span class="ph grow">Cerca nell’inventario</span></div><div class="gap"></div>
${chips(['Tutte', 'Strati', 'Notte', 'Cucina e acqua', 'Sicurezza'], [0], 'scroll')}<div class="gap"></div>
<div class="list">
${item({ lead: cbx(true), t: 'Frontale', s: 'Sicurezza', r: '86 g' })}
${item({ lead: cbx(true), t: 'Kit primo soccorso', s: 'Sicurezza', r: '210 g' })}
${item({ lead: cbx(false), t: 'Bastoncini', s: 'Trasporto · <span class="tag est">peso stimato</span>', r: '480 g' })}
${item({ lead: `<span class="cbx on" style="opacity:.45">${ic('check')}</span>`, t: 'Giacca hardshell', s: 'Già nello zaino', r: '380 g' })}
${item({ lead: cbx(false), t: 'Thermos 0,5 L', s: 'Cucina e acqua', r: '290 g' })}
</div></main>${bottom(btn('Aggiungi 2 oggetti · 296 g', 'primary', 'plus'))}`, { group: 'preparazione' });
add('14d-checklist-completa', 'Checklist — tutto pronto e menu', 'Stato completo dichiarato a parole; azzera spunte ed esporta CSV dal menu', `
${bar('Zaino · Lagazuoi', ['plus', 'more'])}
<main class="content"><div class="panel" style="background:var(--accent);color:var(--on-accent)"><h3 style="font-size:17px">${ic('check')} Tutto pronto: 22 di 22</h3><div style="font-size:14px">7,2 kg nello zaino · 9,5 kg in tutto</div></div>
<div class="cat-h">Strati <span>4 di 4 · 1,24 kg</span></div>
<div class="list">${row(true, 'Giacca hardshell', 1, '380 g')}${row(true, 'Piumino', 1, '310 g')}${row(true, 'Calze di ricambio', 2, '140 g')}</div></main>
<div class="menu"><div>${ic('plus', 'sm')}Aggiungi dall’inventario</div><div>${ic('refresh', 'sm')}Rivedi i suggerimenti</div><div>${ic('undo', 'sm')}Azzera le spunte</div><div>${ic('download', 'sm')}Esporta CSV</div></div>`, { group: 'preparazione' });

// ---------- 15 Inventario ----------
add('15a-inventario', 'Inventario attrezzatura', 'Ricerca, filtri per categoria, peso e tipo di ogni oggetto, stime riconoscibili', `
<header class="appbar"><span class="appbar-title" style="margin:0;font-size:19px;font-weight:800">Attrezzatura</span><span class="grow"></span><span class="muted" style="margin-right:14px;font:600 14px var(--f-body)">31 oggetti · 11,4 kg</span></header>
<main class="content"><div class="search">${ic('search', 'sm')}<span class="ph grow">Cerca per nome o categoria</span></div><div class="gap"></div>
${chips(['Tutte', 'Trasporto', 'Strati', 'Notte', 'Cucina e acqua', 'Sicurezza'], [0], 'scroll')}
<div class="cat-h">Trasporto <span>3 oggetti</span></div>
<div class="list">
${item({ icon: 'backpack', t: 'Zaino 30 L', s: 'Base · capacità 30 L', r: '980 g', trail: chev })}
${item({ icon: 'bag', t: 'Sacca stagna 10 L', s: 'Base · 0,3 L', r: '60 g', trail: chev })}
</div>
<div class="cat-h">Strati <span>9 oggetti</span></div>
<div class="list">
${item({ icon: 'shirt', t: 'Giacca hardshell', s: 'Base · guscio antipioggia', r: '380 g', trail: chev })}
${item({ icon: 'shirt', t: 'Piumino', s: 'Base · strato caldo · <span class="tag est">peso stimato</span>', r: '310 g', trail: chev })}
${item({ icon: 'shirt', t: 'Guanti', s: 'Indossato · peso mancante', r: '—', trail: chev })}
</div></main>
<button class="fab" aria-label="Nuovo oggetto">${ic('plus')}<span>Nuovo</span></button>${bottomNav('attrezzatura')}`, { group: 'attrezzatura' });
add('15b-inventario-vuoto', 'Inventario — stato vuoto', 'Inventario vuoto con catalogo iniziale facoltativo', `
<header class="appbar"><span class="appbar-title" style="margin:0;font-size:19px;font-weight:800">Attrezzatura</span></header>
<main class="content"><div class="empty"><div class="art">${ic('backpack')}</div><h2>L’inventario è vuoto</h2><p>Aggiungi ciò che possiedi una volta sola: lo ritrovi in ogni uscita, con il suo peso.</p>${btn('Nuovo oggetto', 'primary', 'plus')}${btn('Parti da un catalogo di base', 'secondary')}<p class="help">Il catalogo usa pesi tipici, segnati come stime finché non li correggi.</p></div></main>${bottomNav('attrezzatura')}`, { group: 'attrezzatura' });

// ---------- 16 Oggetto ----------
const objForm = (err) => `
${closebar(err ? 'Nuovo oggetto' : 'Modifica oggetto')}
<main class="content">
${field('Nome', err ? 'Es. Giacca hardshell' : 'Giacca hardshell', err ? { ph: true, cls: 'err', help: ic('warn', 'xs') + 'Scrivi un nome per salvare', helpCls: 'err' } : {})}
<div class="two">${field('Peso (g)', err ? '—' : '380', { ph: err })}${field('Volume (L)', err ? '—' : '1,2', { opt: true, ph: err })}</div>
<div class="rowx" style="margin:-6px 0 12px"><span class="link grow">${'Cerca il peso online'}</span><span class="help">apre il browser</span></div>
${field('Categoria', 'Strati', { trail: ic('chevdown', 'sm') })}
${segField('Tipo', ['Base', 'Indossato', 'Consumabile'], 0)}
${field('Uso', 'Guscio antipioggia', { trail: ic('chevdown', 'sm'), help: 'Serve ai suggerimenti per riconoscere cosa hai già' })}
${field('Note', 'Tre strati, cappuccio regolabile', { opt: true })}
</main>${err ? bottom(btn('Salva oggetto', 'disabled')) : `<div class="bottombar two">${btn('Elimina', 'secondary', 'trash')}${btn('Salva')}</div>`}`;
add('16a-oggetto', 'Modifica oggetto', 'Nome, peso, volume, categoria, tipo, uso, note', objForm(false), { group: 'attrezzatura' });
add('16b-oggetto-errore', 'Nuovo oggetto — campo mancante', 'L’errore è scritto accanto al campo, non solo colorato', objForm(true), { group: 'attrezzatura' });
add('16c-oggetto-zaino', 'Nuovo oggetto — zaino e categorie', 'Per gli zaini compare la capacità; le categorie si possono aggiungere', `
${closebar('Nuovo oggetto')}<main class="content">${field('Nome', 'Zaino 45 L')}</main>
<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Categoria</h2>
<div class="list" style="margin-top:8px">
${['Trasporto', 'Ricambi', 'Cucina e acqua', 'Strati', 'Notte', 'Sicurezza'].map((c, i) => item({ lead: `<span class="radio ${i === 0 ? 'on' : ''}"></span>`, t: c })).join('')}
${item({ icon: 'plus', t: 'Nuova categoria', s: 'Es. Fotografia, Arrampicata' })}
</div><div class="gap"></div>${field('Capacità dello zaino (L)', '45', { help: 'Compare solo per gli oggetti con uso "zaino"' })}</div></div>`, { group: 'attrezzatura' });

// ---------- 18 Impostazioni ----------
add('18-impostazioni', 'Impostazioni', 'Lingua, tema, unità, soglia del dislivello e accesso alle sezioni di servizio', `
<header class="appbar"><span class="appbar-title" style="margin:0;font-size:19px;font-weight:800">Impostazioni</span></header>
<main class="content">
${segField('Lingua', ['Italiano', 'English'], 0)}
${segField('Tema', ['Chiaro', 'Scuro', 'Di sistema'], 2)}
<div class="field"><label>Unità di peso</label>${seg(['g', 'hg', 'kg'], 2)}<div class="help">Esempio: 7,45 kg. Sotto il chilo si mostrano i grammi.</div></div>
<div class="field"><label>Soglia del dislivello</label><div class="rowx"><span class="grow help">Variazione minima contata. Consigliato 3 m.</span>${stepper('3 m')}</div></div>
<div class="panel" style="padding:4px 16px">
<div class="set">${ic('clock')}<div class="grow"><div class="t">Stima della durata</div><div class="s">4 km/h · 300 m/h · 500 m/h</div></div>${chev}</div>
<div class="set">${ic('download')}<div class="grow"><div class="t">Backup ed esportazioni</div><div class="s">Ultimo backup: 12 nov 2026</div></div>${chev}</div>
<div class="set">${ic('map')}<div class="grow"><div class="t">Servizi, mappe offline e cache</div><div class="s">Tutti raggiungibili · 38 MB</div></div>${chev}</div>
<div class="set">${ic('shield')}<div class="grow"><div class="t">Privacy e attribuzioni</div><div class="s">Dati solo sul dispositivo</div></div>${chev}</div>
</div></main>${bottomNav('impostazioni')}`, { group: 'impostazioni' });
add('18b-servizi', 'Impostazioni — stato di dati e servizi', 'Cosa funziona senza rete, quali servizi rispondono, cache e chiavi facoltative', `
${bar('Servizi e cache')}
<main class="content">
<div class="panel"><h3>Dati</h3>${kv('Dove sono', 'Solo su questo dispositivo')}${kv('Uso senza rete', `<span class="dot">${ic('check', 'xs')}Pronto</span>`)}${kv('Spazio usato', '4,2 MB + 38 MB di mappe')}</div>
<div class="gap"></div>
<div class="panel"><h3>Servizi <span>controllati ora</span></h3>
${kv('Mappe · OpenFreeMap', `<span class="dot">${ic('check', 'xs')}Raggiungibile</span>`)}${kv('Meteo · Open-Meteo', `<span class="dot">${ic('check', 'xs')}Raggiungibile</span>`)}${kv('Sentieri · BRouter', `<span class="dot" style="color:var(--danger)">${ic('warn', 'xs')}Non risponde</span>`)}${kv('Ricerca · Open-Meteo', `<span class="dot">${ic('check', 'xs')}Raggiungibile</span>`)}</div>
<div class="gap"></div>
${field('Servizio per i sentieri', 'BRouter (senza chiave)', { trail: ic('chevdown', 'sm') })}
${field('Chiave openrouteservice', 'Non impostata', { ph: true, opt: true, help: 'Resta sul dispositivo e non entra nei backup' })}
<div class="btn-row">${btn('Svuota cache meteo', 'secondary sm')}${btn('Mappe offline', 'secondary sm')}</div>
</main>`, { group: 'impostazioni' });

// ---------- 19 Backup ----------
add('19a-backup', 'Backup, ripristino ed esportazioni', 'Backup completo JSON, ripristino da file, CSV e GPX, condivisione', `
${bar('Backup ed esportazioni')}
<main class="content">
<div class="panel"><h3>Backup completo</h3><p class="help" style="margin-bottom:10px">Inventario, uscite, checklist, percorsi e impostazioni in un file JSON. Ultimo: 12 nov 2026, 21:04.</p>
<div class="btn-row">${btn('Esporta', 'primary sm', 'upload')}${btn('Ripristina', 'secondary sm', 'download')}</div></div>
<div class="gap"></div>
<div class="panel" style="padding:4px 16px">
<div class="set">${ic('file')}<div class="grow"><div class="t">Inventario in CSV</div><div class="s">31 oggetti, apribile con un foglio di calcolo</div></div>${ic('share', 'sm')}</div>
<div class="set">${ic('file')}<div class="grow"><div class="t">Checklist in CSV</div><div class="s">Scegli l’uscita</div></div>${chev}</div>
<div class="set">${ic('route')}<div class="grow"><div class="t">Percorso in GPX</div><div class="s">Scegli dall’archivio</div></div>${chev}</div>
</div>
<div class="gap"></div>
${banner('', 'info', 'Dove finiscono i file', 'Scegli tu la cartella o l’app con cui condividerli. BetterOut non li invia da nessuna parte.')}
</main>`, { group: 'impostazioni' });
add('19b-ripristino', 'Ripristino — anteprima e scelta', 'Validazione, conteggi, unione o sostituzione; prima di sostituire si salva un backup di sicurezza', `
${bar('Ripristina backup')}
<main class="content">
${item({ icon: 'file', t: 'betterout-2026-11-12.json', s: 'Formato versione 1 · 12 nov 2026 · 214 kB' })}
<div class="gap-s"></div>
${banner('', 'check', 'File valido', 'Formato riconosciuto, riferimenti fra uscite, percorsi e oggetti tutti presenti.')}
<div class="gap"></div>
<div class="panel"><h3>Contenuto <span>nel file · nuovi per te</span></h3>${kv('Uscite', '6 · 2 nuove')}${kv('Percorsi', '3 · 0 nuovi')}${kv('Oggetti in inventario', '31 · 4 nuovi')}${kv('Righe di checklist', '58 · 22 nuove')}</div>
<div class="sub-h"><span>Come ripristinare</span></div>
<div class="list">
${item({ lead: '<span class="radio on"></span>', t: 'Unisci', s: 'Aggiunge ciò che manca; a parità di elemento vince la modifica più recente. Non elimina nulla.', cls: 'sel' })}
${item({ lead: '<span class="radio"></span>', t: 'Sostituisci tutto', s: 'Prima salvo un backup di sicurezza dei dati attuali, poi li rimpiazzo.' })}
</div></main>${bottom(btn('Ripristina unendo', 'primary', 'download'))}`, { group: 'impostazioni' });
add('19c-ripristino-errore', 'Ripristino — file non valido', 'Il file viene rifiutato prima di toccare i dati', `
${bar('Ripristina backup')}
<main class="content">
${item({ icon: 'file', t: 'backup-vecchio.json', s: '88 kB' })}<div class="gap-s"></div>
${banner('err', 'warn', 'Questo backup non si può ripristinare', 'È stato creato da una versione più recente dell’app (formato 3, questa legge fino a 1). I tuoi dati non sono stati toccati.', ['Scegli un altro file'])}
<div class="gap"></div>
${banner('err', 'warn', 'Altri casi segnalati', '• File non JSON o troncato<br>• 3 righe di checklist puntano a un’uscita che manca<br>• Peso negativo nell’oggetto "Fornello"')}
</main>`, { group: 'impostazioni' });

// ---------- 20 Dialoghi ----------
const over = (bg, dlg) => bg + `<div class="scrim center">${dlg}</div>`;
add('20a-elimina-uscita', 'Dialogo — eliminare un’uscita', 'Conferma con il nome e con ciò che viene (e non viene) eliminato', over(dettaglio(), `<div class="dialog"><div class="dic red">${ic('trash')}</div><h2>Eliminare “Lagazuoi dal Passo Falzarego”?</h2><p>Elimino l’uscita, la sua checklist e il meteo salvato. Il percorso resta nell’archivio e gli oggetti nell’inventario. Non si può annullare.</p>${btn('Elimina uscita', 'danger', 'trash')}${btn('Annulla', 'secondary')}</div>`), { group: 'dialoghi' });
add('20b-modifiche-non-salvate', 'Dialogo — modifiche non salvate', 'Tre uscite chiare: salvare come bozza, scartare, restare', `${closebar('Nuova uscita', 'Salva bozza')}<main class="content">${steps(3, 6)}<h2 class="h2">Che percorso fai?</h2></main>
<div class="scrim"><div class="sheet"><div class="grab"></div><h2>Hai modifiche non salvate</h2><p class="lead">Nome, località e data di questa uscita non sono ancora salvati.</p>${btn('Salva come bozza', 'primary', 'check')}<div class="gap-s"></div>${btn('Esci senza salvare', 'secondary')}<div class="gap-s"></div>${btn('Continua a modificare', 'text')}</div></div>`, { group: 'dialoghi' });
add('20c-permesso-posizione', 'Dialogo — permesso di posizione', 'Spiegazione prima della richiesta di sistema; rifiutare non blocca nulla', `<main class="content tight"><div class="mapfull"><div style="position:absolute;inset:0;background:var(--map-land)"></div></div></main>
<div class="scrim center"><div class="dialog"><div class="dic">${ic('locate')}</div><h2>Mostrare dove sei sulla mappa?</h2><p>Serve solo a centrare la mappa sulla tua posizione mentre la guardi. Non viene salvata né inviata. Puoi dire di no e usare tutto il resto.</p>${btn('Continua', 'primary')}${btn('Non ora', 'secondary')}<p class="help" style="margin:10px 0 0;text-align:center">Dopo "Continua" Android chiede il permesso.</p></div></div>`, { group: 'dialoghi' });
add('20d-elimina-oggetto', 'Dialogo — eliminare un oggetto dell’inventario', 'La regola è dichiarata: checklist e storico restano', `<header class="appbar"><span class="appbar-title" style="margin:0;font-size:19px;font-weight:800">Attrezzatura</span></header><main class="content"></main>
<div class="scrim center"><div class="dialog"><div class="dic red">${ic('trash')}</div><h2>Eliminare “Giacca hardshell”?</h2><p>È in 3 checklist. Lì resta con nome e peso attuali, segnata come “non più in inventario”. Sparisce solo dall’inventario e dai suggerimenti.</p>${btn('Elimina dall’inventario', 'danger', 'trash')}${btn('Annulla', 'secondary')}</div></div>`, { group: 'dialoghi' });
add('20e-azzera-spunte', 'Dialogo — azzerare le spunte', 'Azione reversibile solo a mano: conferma breve', `${bar('Zaino · Lagazuoi', ['plus', 'more'])}<main class="content">${ckSummary(22, 100)}</main>
<div class="scrim center"><div class="dialog"><div class="dic">${ic('undo')}</div><h2>Azzerare le 22 spunte?</h2><p>Oggetti, quantità e note restano. Vale solo per questa uscita.</p>${btn('Azzera le spunte', 'primary')}${btn('Annulla', 'secondary')}</div></div>`, { group: 'dialoghi' });
add('20f-percorso-in-uso', 'Dialogo — modificare un percorso in uso', 'Avviso quando il percorso è associato a più uscite', `${bar('Percorso', ['share', 'more'])}<main class="content"><h1 class="h1" style="font-size:27px">Lagazuoi, anello</h1></main>
<div class="scrim center"><div class="dialog"><div class="dic">${ic('route')}</div><h2>Questo percorso è usato in 2 uscite</h2><p>Se lo modifichi cambia in entrambe: “Lagazuoi dal Passo Falzarego” e la sua copia.</p>${btn('Modifica per tutte', 'primary')}${btn('Crea una copia da modificare', 'secondary')}${btn('Annulla', 'text')}</div></div>`, { group: 'dialoghi' });

export const SCREENS_2 = S;
