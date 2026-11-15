// Le due schermate di confronto (Home e dettaglio uscita), identiche nei contenuti per le tre direzioni visive.
// I dati mostrati sono d'esempio e servono solo a valutare l'aspetto.
import { ic, logo, topo, bottomNav } from './lib.mjs';

export function home() {
  return `
<header class="appbar"><div class="brand">${logo()}<span>BetterOut</span></div><span class="grow"></span><button class="iconbtn" aria-label="Cerca">${ic('search')}</button></header>
<main class="content">
  <h1 class="display">Le tue<br>uscite</h1>
  <section class="hero card">
    <div class="hero-top"><span class="label">Prossima uscita</span><span class="pill solid">${ic('calendar', 'xs')}tra 6 giorni</span></div>
    <h2 class="hero-title">Lagazuoi dal Passo Falzarego</h2>
    <p class="hero-meta">sab 21 nov · partenza 07:30<br>Passo Falzarego, Cortina d’Ampezzo</p>
    <div class="hero-wx">${ic('snow', 'lg')}<div><b>−6° / −1°</b> · Neve debole<br><span class="muted">Vento 25 km/h · aggiornato 2 h fa</span></div></div>
    <div class="stats"><div><b>9,6 km</b><span>distanza</span></div><div><b>690 m</b><span>salita</span></div><div><b>4 h 20</b><span>stimata</span></div></div>
    <div class="prep"><div class="prep-row"><span>Zaino: 14 di 22 pronti</span><b>6,8 kg</b></div><div class="bar"><i style="width:64%"></i></div></div>
  </section>
  <div class="section-h"><h3>Altre uscite</h3><span class="link">Vedi tutte · 5</span></div>
  <ul class="trips">
    <li><div class="trip-main"><b>Rifugio Nuvolau</b><span>sab 5 dic · 7,2 km · ↑ 520 m</span></div><span class="status s-prog">${ic('calendar', 'xs')}Programmata</span></li>
    <li><div class="trip-main"><b>Anello del Sorapis</b><span>Senza data · 12,8 km · ↑ 950 m</span></div><span class="status s-bozza">${ic('edit', 'xs')}Bozza</span></li>
    <li><div class="trip-main"><b>Monte Rite</b><span>lun 2 nov · 8,4 km · ↑ 610 m</span></div><span class="status s-ok">${ic('check', 'xs')}Completata</span></li>
  </ul>
</main>
<button class="fab" aria-label="Nuova uscita">${ic('plus')}<span>Nuova uscita</span></button>
${bottomNav('uscite')}`;
}

export function dettaglio() {
  return `
<header class="appbar sub"><button class="iconbtn" aria-label="Indietro">${ic('back')}</button><span class="appbar-title">Uscita</span><span class="grow"></span><button class="iconbtn" aria-label="Modifica">${ic('edit')}</button><button class="iconbtn" aria-label="Altre azioni">${ic('more')}</button></header>
<main class="content">
  <div class="status-row"><span class="status s-prog">${ic('calendar', 'xs')}Programmata</span><span class="pill solid">tra 6 giorni</span></div>
  <h1 class="h1">Lagazuoi dal Passo Falzarego</h1>
  <div class="meta"><div>${ic('clock', 'xs')}sab 21 nov 2026 · partenza 07:30</div><div>${ic('pin', 'xs')}Passo Falzarego, Cortina d’Ampezzo (BL)</div></div>
  <div class="mapcard">${topo(372, 150)}<button class="map-btn">${ic('map', 'xs')}Apri mappa</button><span class="map-attr">© OpenStreetMap</span></div>
  <div class="stats4"><div><b>9,6 km</b><span>distanza</span></div><div><b>690 m</b><span>salita</span></div><div><b>690 m</b><span>discesa</span></div><div><b>4 h 20</b><span>stimata</span></div></div>
  <section class="block card">
    <div class="block-h"><h3>Meteo · sab 21 nov</h3><span>Open-Meteo · 2 h fa</span></div>
    <div class="wx-row">${ic('snow')}<div><div class="where">Partenza · 2105 m</div><div class="what">Neve debole, 2 cm</div></div><div class="temp">−3° / 1°<small>vento 20 km/h</small></div></div>
    <div class="wx-row">${ic('wind')}<div><div class="where">Quota massima · 2752 m</div><div class="what">Raffiche fino a 45 km/h</div></div><div class="temp">−8° / −4°<small>percepita −14°</small></div></div>
  </section>
  <section class="block card">
    <div class="block-h"><h3>Zaino</h3><span>14 di 22 pronti</span></div>
    <div class="bar"><i style="width:64%"></i></div>
    <div class="weights"><div><b>4,9 kg</b><span>Base</span></div><div><b>1,9 kg</b><span>Consumabili</span></div><div><b>2,3 kg</b><span>Indossato</span></div></div>
    <div class="total"><span>Peso nello zaino</span><b>6,8 kg</b></div>
  </section>
</main>
<div class="bottombar"><button class="btn primary">${ic('backpack')}Prepara lo zaino</button></div>`;
}
