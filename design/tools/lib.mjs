// Strumenti condivisi per le bozze grafiche di BetterOut.
// Le bozze sono pagine HTML/CSS renderizzate in PNG con Chrome headless (vedi build.mjs).

const ICONS = {
  mountain: '<path d="M3 19 9.5 8l4 6.5 2-3L21 19z"/>',
  map: '<path d="M3 6.5 9 4l6 2.5L21 4v13.5L15 20l-6-2.5L3 20z"/><path d="M9 4v13.5M15 6.5V20"/>',
  backpack: '<path d="M7 9.5a5 5 0 0 1 10 0V19a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2z"/><path d="M9.5 5a2.5 2.5 0 0 1 5 0M9.5 13.5h5V17h-5z"/>',
  settings: '<path d="M4 7h9M19 7h1M4 17h3M13 17h7"/><circle cx="16" cy="7" r="2.5"/><circle cx="10" cy="17" r="2.5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>',
  chevron: '<path d="m9 6 6 6-6 6"/>',
  chevdown: '<path d="m6 9 6 6 6-6"/>',
  back: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
  more: '<circle cx="12" cy="5" r="1.4" fill="currentColor"/><circle cx="12" cy="12" r="1.4" fill="currentColor"/><circle cx="12" cy="19" r="1.4" fill="currentColor"/>',
  edit: '<path d="M4 20h4L19.5 8.5l-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
  calendar: '<rect x="4" y="5.5" width="16" height="15" rx="2"/><path d="M4 10h16M8 3v4M16 3v4"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7"/>',
  x: '<path d="M6 6l12 12M18 6 6 18"/>',
  pin: '<path d="M12 21s-6.5-6.2-6.5-11.2a6.5 6.5 0 0 1 13 0C18.5 14.8 12 21 12 21z"/><circle cx="12" cy="9.8" r="2.2"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  wind: '<path d="M3 8.5h9.5A2.5 2.5 0 1 0 10 6M3 12.5h14.5a3 3 0 1 1-3 3M3 16.5h6"/>',
  snow: '<path d="M7 15a4 4 0 0 1 .6-7.9 5 5 0 0 1 9.5 1.3A3.3 3.3 0 0 1 17 15z"/><path d="M8.5 18.5v.01M12 20v.01M15.5 18.5v.01"/>',
  cloud: '<path d="M7 18a4.5 4.5 0 0 1 .6-8.9 5.5 5.5 0 0 1 10.4 1.4A3.8 3.8 0 0 1 17.5 18z"/>',
  rain: '<path d="M7 14a4 4 0 0 1 .6-7.9 5 5 0 0 1 9.5 1.3A3.3 3.3 0 0 1 17 14z"/><path d="m8.5 17-1 3M12.5 17l-1 3M16.5 17l-1 3"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8"/>',
  sunrise: '<path d="M4 18h16M7 14a5 5 0 0 1 10 0M12 4v4M9.5 6.5 12 4l2.5 2.5M4.5 11.5l1.5 1M19.5 11.5l-1.5 1"/>',
  drop: '<path d="M12 3s6.5 7 6.5 11.5a6.5 6.5 0 0 1-13 0C5.5 10 12 3 12 3z"/>',
  route: '<circle cx="6" cy="18" r="2"/><circle cx="18" cy="6" r="2"/><path d="M8 18h6a3 3 0 0 0 0-6h-4a3 3 0 0 1 0-6h6"/>',
  up: '<path d="M12 19V5M6 11l6-6 6 6"/>',
  down: '<path d="M12 5v14M6 13l6 6 6-6"/>',
  weight: '<path d="M6.5 8h11L20 20H4z"/><path d="M9 8a3 3 0 0 1 6 0"/>',
  layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 13 9 5 9-5"/>',
  locate: '<circle cx="12" cy="12" r="6.5"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/><path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3"/>',
  undo: '<path d="M8 5 4 9l4 4"/><path d="M4 9h10a5 5 0 0 1 0 10h-3"/>',
  redo: '<path d="m16 5 4 4-4 4"/><path d="M20 9H10a5 5 0 0 0 0 10h3"/>',
  trash: '<path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13M10 11v5.5M14 11v5.5"/>',
  share: '<circle cx="6" cy="12" r="2.3"/><circle cx="17.5" cy="6" r="2.3"/><circle cx="17.5" cy="18" r="2.3"/><path d="m8 11 7.5-4M8 13l7.5 4"/>',
  download: '<path d="M12 4v11M7 10.5l5 5 5-5M5 20h14"/>',
  upload: '<path d="M12 16V5M7 9.5l5-5 5 5M5 20h14"/>',
  filter: '<path d="M4 6h16M7 12h10M10 18h4"/>',
  drag: '<circle cx="9" cy="7" r="1.3" fill="currentColor"/><circle cx="15" cy="7" r="1.3" fill="currentColor"/><circle cx="9" cy="12" r="1.3" fill="currentColor"/><circle cx="15" cy="12" r="1.3" fill="currentColor"/><circle cx="9" cy="17" r="1.3" fill="currentColor"/><circle cx="15" cy="17" r="1.3" fill="currentColor"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.6v.01"/>',
  warn: '<path d="M12 4 21.5 20h-19z"/><path d="M12 10v4.5M12 17.2v.01"/>',
  tent: '<path d="M12 4 3 20h18zM12 4v16M12 13l-3.5 7M12 13l3.5 7"/>',
  hut: '<path d="M3.5 12 12 4.5l8.5 7.5M6 10.5V20h12v-9.5M10 20v-5h4v5"/>',
  moon: '<path d="M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z"/>',
  copy: '<rect x="8.5" y="8.5" width="11" height="12" rx="2"/><path d="M15.5 8.5v-3a2 2 0 0 0-2-2h-7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
  loop: '<path d="M17 4.5 20 7.5l-3 3"/><path d="M20 7.5H9a5 5 0 0 0-5 5M7 19.5l-3-3 3-3"/><path d="M4 16.5h11a5 5 0 0 0 5-5"/>',
  swap: '<path d="M7 4v16M3.5 7.5 7 4l3.5 3.5M17 20V4M13.5 16.5 17 20l3.5-3.5"/>',
  file: '<path d="M6 3.5h8l4.5 4.5V20.5H6z"/><path d="M14 3.5V8h4.5"/>',
  flag: '<path d="M6 21V4M6 5h11l-2.5 4 2.5 4H6"/>',
  wifioff: '<path d="M3 3l18 18M5 12.5a10 10 0 0 1 4-2.6M19 12.5a10 10 0 0 0-5-2.9M8.5 16a5 5 0 0 1 7 0M12 19.5v.01"/>',
  refresh: '<path d="M20 5v5h-5"/><path d="M19.5 10A8 8 0 1 0 20 14"/>',
  peak: '<path d="M4 19 12 5l8 14z"/>',
  hand: '<path d="M8 12V6a1.5 1.5 0 0 1 3 0v5M11 11V5a1.5 1.5 0 0 1 3 0v6M14 11V6.5a1.5 1.5 0 0 1 3 0V15a6 6 0 0 1-6 6 6 6 0 0 1-5-2.5L4 14a1.5 1.5 0 0 1 2.5-1.5L8 14"/>',
  shield: '<path d="M12 3.5 19 6v6c0 4.5-3 7.5-7 8.5-4-1-7-4-7-8.5V6z"/>',
  lang: '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c3 3 3 14 0 17M12 3.5c-3 3-3 14 0 17"/>',
  shirt: '<path d="M8.5 4 4 6.5 5.5 11 8 10v10h8V10l2.5 1L20 6.5 15.5 4a3.5 3.5 0 0 1-7 0z"/>',
  flame: '<path d="M12 21a6 6 0 0 1-6-6c0-3 2-4.500 3-7 .5 2 1.500 3 2.500 3.500C11 8 12 5 14 3c0 4 4 6 4 12a6 6 0 0 1-6 6z"/>',
  bag: '<path d="M5 8h14l1 12H4z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  box: '<path d="M4 8 12 4l8 4v8l-8 4-8-4z"/><path d="m4 8 8 4 8-4M12 12v8"/>',
};

export function ic(name, cls = '') {
  if (!ICONS[name]) throw new Error('icona mancante: ' + name);
  return `<svg class="ic ${cls}" viewBox="0 0 24 24" aria-hidden="true">${ICONS[name]}</svg>`;
}

// Marchio provvisorio e originale: tre curve di livello annidate.
export function logo(cls = '') {
  return `<svg class="logo ${cls}" viewBox="0 0 28 28" aria-hidden="true"><path class="lg1" d="M14 2.500c6 0 11.500 4 11.500 11S20.500 25.500 14 25.500 2.500 21 2.500 13.500 8 2.500 14 2.500z"/><path class="lg2" d="M14.500 7.500c3.500 0 6.500 2.300 6.500 6.300s-3 6.700-7 6.700-6.500-2.700-6.500-6.500 3.200-6.500 7-6.500z"/><path class="lg3" d="M14.200 12c1.300 0 2.300.8 2.300 2.100s-1 2.300-2.500 2.300-2.300-1-2.300-2.200 1-2.200 2.500-2.200z"/></svg>`;
}

// ---------- Mappa topografica finta ma plausibile (curve di livello con marching squares) ----------
function field(nx, ny) {
  // nx, ny in 0..1 — somma di rilievi gaussiani
  const g = (cx, cy, sx, sy, a) => a * Math.exp(-(((nx - cx) ** 2) / (2 * sx * sx) + ((ny - cy) ** 2) / (2 * sy * sy)));
  return g(0.70, 0.30, 0.20, 0.24, 1.0) + g(0.40, 0.18, 0.16, 0.14, 0.62) + g(0.16, 0.74, 0.20, 0.26, 0.55)
    + g(0.92, 0.82, 0.16, 0.20, 0.48) + g(0.52, 0.62, 0.30, 0.12, 0.22) - g(0.36, 0.48, 0.10, 0.16, 0.12);
}

function contours(w, h, levels, cell = 5) {
  const cols = Math.ceil(w / cell), rows = Math.ceil(h / cell);
  const v = [];
  for (let j = 0; j <= rows; j++) { v.push([]); for (let i = 0; i <= cols; i++) v[j].push(field(i / cols, j / rows)); }
  const out = [];
  for (const L of levels) {
    let d = '';
    const ip = (a, b) => (L - a) / (b - a);
    for (let j = 0; j < rows; j++) for (let i = 0; i < cols; i++) {
      const a = v[j][i], b = v[j][i + 1], c = v[j + 1][i + 1], e = v[j + 1][i];
      const idx = (a > L ? 8 : 0) | (b > L ? 4 : 0) | (c > L ? 2 : 0) | (e > L ? 1 : 0);
      if (idx === 0 || idx === 15) continue;
      const x = i * cell, y = j * cell;
      const T = () => [x + ip(a, b) * cell, y], R = () => [x + cell, y + ip(b, c) * cell];
      const B = () => [x + ip(e, c) * cell, y + cell], Lf = () => [x, y + ip(a, e) * cell];
      const seg = (p, q) => { d += `M${p[0].toFixed(1)} ${p[1].toFixed(1)}L${q[0].toFixed(1)} ${q[1].toFixed(1)}`; };
      switch (idx) {
        case 1: case 14: seg(Lf(), B()); break;
        case 2: case 13: seg(B(), R()); break;
        case 3: case 12: seg(Lf(), R()); break;
        case 4: case 11: seg(T(), R()); break;
        case 6: case 9: seg(T(), B()); break;
        case 7: case 8: seg(Lf(), T()); break;
        case 5: seg(Lf(), T()); seg(B(), R()); break;
        case 10: seg(T(), R()); seg(Lf(), B()); break;
      }
    }
    out.push(d);
  }
  return out;
}

const smooth = (pts) => {
  // Catmull-Rom -> Bezier, per linee morbide
  let d = `M${pts[0][0]} ${pts[0][1]}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || p2;
    const c1 = [p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6];
    const c2 = [p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6];
    d += `C${c1[0].toFixed(1)} ${c1[1].toFixed(1)} ${c2[0].toFixed(1)} ${c2[1].toFixed(1)} ${p2[0].toFixed(1)} ${p2[1].toFixed(1)}`;
  }
  return d;
};

export const ROUTE_N = [[0.13, 0.80], [0.22, 0.66], [0.33, 0.60], [0.41, 0.47], [0.52, 0.44], [0.60, 0.34], [0.69, 0.30]];
const TRAILS_N = [
  [[0.0, 0.52], [0.12, 0.50], [0.24, 0.42], [0.36, 0.30], [0.40, 0.18], [0.52, 0.06], [0.60, 0.0]],
  [[0.69, 0.30], [0.80, 0.40], [0.86, 0.55], [0.84, 0.70], [0.93, 0.84], [1.0, 0.92]],
  [[0.33, 0.60], [0.46, 0.68], [0.58, 0.80], [0.66, 0.95], [0.70, 1.0]],
];

/**
 * opts: route (bool), manualFrom (indice da cui il percorso è "manuale"), points (mostra i punti di controllo),
 * selected (indice punto selezionato), labels (bool), routeN (percorso alternativo), dim (attenua la base)
 */
export function topo(w, h, opts = {}) {
  const o = { route: true, labels: true, points: false, selected: -1, manualFrom: -1, routeN: ROUTE_N, ...opts };
  const levels = []; for (let l = 0.08; l < 1.15; l += 0.075) levels.push(l);
  const cs = contours(w, h, levels);
  const P = (n) => [+(n[0] * w).toFixed(1), +(n[1] * h).toFixed(1)];
  let s = `<svg class="topo" viewBox="0 0 ${w} ${h}" width="${w}" height="${h}" preserveAspectRatio="xMidYMid slice" aria-hidden="true">`;
  s += `<rect width="${w}" height="${h}" class="tp-land"/>`;
  s += `<path class="tp-wood" d="${smooth([[0, h * 0.62], [w * 0.18, h * 0.56], [w * 0.36, h * 0.72], [w * 0.5, h * 0.9], [w * 0.52, h]].map(p => [+p[0].toFixed(1), +p[1].toFixed(1)]))}L0 ${h}Z"/>`;
  s += `<path class="tp-wood" d="${smooth([[w, h * 0.50], [w * 0.9, h * 0.58], [w * 0.80, h * 0.80], [w * 0.84, h]].map(p => [+p[0].toFixed(1), +p[1].toFixed(1)]))}L${w} ${h}Z"/>`;
  cs.forEach((d, i) => { s += `<path class="tp-ct${i % 4 === 3 ? ' tp-ctb' : ''}" d="${d}"/>`; });
  s += `<path class="tp-water" d="${smooth([[w * 0.30, 0], [w * 0.33, h * 0.2], [w * 0.36, h * 0.48], [w * 0.30, h * 0.7], [w * 0.26, h]].map(p => [+p[0].toFixed(1), +p[1].toFixed(1)]))}"/>`;
  TRAILS_N.forEach(t => { s += `<path class="tp-trail" d="${smooth(t.map(P))}"/>`; });
  if (o.route) {
    const pts = o.routeN.map(P);
    if (o.manualFrom > 0) {
      const a = pts.slice(0, o.manualFrom + 1), b = pts.slice(o.manualFrom);
      s += `<path class="tp-rcase" d="${smooth(a)}"/><path class="tp-route" d="${smooth(a)}"/>`;
      s += `<path class="tp-rcase" d="M${b.map(p => p.join(' ')).join('L')}"/><path class="tp-route tp-manual" d="M${b.map(p => p.join(' ')).join('L')}"/>`;
    } else {
      s += `<path class="tp-rcase" d="${smooth(pts)}"/><path class="tp-route" d="${smooth(pts)}"/>`;
    }
    if (o.points) pts.forEach((p, i) => {
      if (i === 0 || i === pts.length - 1) return;
      s += `<circle class="tp-pt${i === o.selected ? ' tp-sel' : ''}" cx="${p[0]}" cy="${p[1]}" r="${i === o.selected ? 9 : 6}"/>`;
    });
    const a = pts[0], z = pts[pts.length - 1];
    s += `<g class="tp-start"><circle cx="${a[0]}" cy="${a[1]}" r="10"/><text x="${a[0]}" y="${a[1] + 4}" text-anchor="middle">P</text></g>`;
    s += `<g class="tp-end"><circle cx="${z[0]}" cy="${z[1]}" r="10"/><path d="M${z[0] - 3} ${z[1] + 5}v-10l7 3-7 3"/></g>`;
  }
  if (o.labels) {
    const pk = (n, name, q) => { const p = P(n); return `<g class="tp-peak"><path d="M${p[0] - 5} ${p[1] + 4}l5-9 5 9z"/><text x="${p[0] + 9}" y="${p[1] - 2}">${name}</text><text class="tp-q" x="${p[0] + 9}" y="${p[1] + 10}">${q}</text></g>`; };
    s += pk([0.735, 0.235], 'Lagazuoi', '2752');
    s += pk([0.40, 0.15], 'Col dei Bos', '2559');
    const hp = P([0.615, 0.385]);
    s += `<g class="tp-hut"><path d="M${hp[0] - 6} ${hp[1] + 5}v-6l6-5 6 5v6z"/><text x="${hp[0] + 10}" y="${hp[1] + 4}">Rif. Lagazuoi</text></g>`;
  }
  s += '</svg>';
  return s;
}

// Profilo altimetrico: pts = [[km, quota], ...]
export function profile(w, h, pts, opts = {}) {
  const o = { pad: 4, axis: true, marker: -1, ...opts };
  const xs = pts.map(p => p[0]), ys = pts.map(p => p[1]);
  const x0 = Math.min(...xs), x1 = Math.max(...xs), y0 = Math.min(...ys) - 40, y1 = Math.max(...ys) + 40;
  const left = o.axis ? 38 : 0, bottom = o.axis ? 18 : 0;
  const X = (x) => left + ((x - x0) / (x1 - x0)) * (w - left - o.pad), Y = (y) => o.pad + (1 - (y - y0) / (y1 - y0)) * (h - bottom - o.pad * 2);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${X(p[0]).toFixed(1)} ${Y(p[1]).toFixed(1)}`).join('');
  let s = `<svg class="prof" viewBox="0 0 ${w} ${h}" width="${w}" height="${h}" aria-hidden="true">`;
  if (o.axis) {
    const ticks = [Math.ceil((y0 + 40) / 100) * 100, Math.floor((y1 - 40) / 100) * 100];
    for (const t of ticks) s += `<path class="pf-grid" d="M${left} ${Y(t).toFixed(1)}H${w - o.pad}"/><text class="pf-t" x="${left - 6}" y="${(Y(t) + 4).toFixed(1)}" text-anchor="end">${t}</text>`;
    const kms = [x0, (x0 + x1) / 2, x1];
    kms.forEach((k, i) => { s += `<text class="pf-t" x="${X(k).toFixed(1)}" y="${h - 3}" text-anchor="${i === 0 ? 'start' : i === 2 ? 'end' : 'middle'}">${String(+k.toFixed(1)).replace('.', ',')} km</text>`; });
  }
  s += `<path class="pf-area" d="${line}L${X(x1).toFixed(1)} ${h - bottom}L${X(x0).toFixed(1)} ${h - bottom}Z"/><path class="pf-line" d="${line}"/>`;
  if (o.marker >= 0) { const p = pts[o.marker]; s += `<path class="pf-mk" d="M${X(p[0]).toFixed(1)} ${o.pad}V${h - bottom}"/><circle class="pf-dot" cx="${X(p[0]).toFixed(1)}" cy="${Y(p[1]).toFixed(1)}" r="5"/>`; }
  return s + '</svg>';
}

export const PROFILE_LAGAZUOI = [[0, 2105], [0.6, 2150], [1.2, 2230], [1.9, 2310], [2.5, 2420], [3.1, 2510], [3.7, 2610], [4.3, 2700], [4.8, 2752], [5.4, 2690], [6.0, 2590], [6.8, 2470], [7.5, 2380], [8.2, 2260], [8.9, 2170], [9.6, 2105]];

// ---------- Involucro pagina ----------
export function statusBar() {
  return `<div class="statusbar"><span>9:41</span><span class="sb-icons"><svg viewBox="0 0 18 12" width="18" height="12"><path d="M1 11h2V8H1zM5.500 11h2V6h-2zM10 11h2V3.500h-2zM14.500 11h2V1h-2z" fill="currentColor"/></svg><svg viewBox="0 0 26 12" width="26" height="12"><rect x=".7" y=".7" width="21" height="10.600" rx="3" fill="none" stroke="currentColor" stroke-width="1.200"/><rect x="2.500" y="2.500" width="14" height="7" rx="1.500" fill="currentColor"/><path d="M23.500 4v4" stroke="currentColor" stroke-width="1.500" stroke-linecap="round"/></svg></span></div>`;
}

export function bottomNav(active) {
  const items = [['uscite', 'mountain', 'Uscite'], ['mappa', 'map', 'Mappa'], ['attrezzatura', 'backpack', 'Attrezzatura'], ['impostazioni', 'settings', 'Impostazioni']];
  return `<nav class="bottomnav">${items.map(([k, i, l]) => `<div class="nav-item${k === active ? ' on' : ''}"><span class="nav-ic">${ic(i)}</span><span class="nav-l">${l}</span></div>`).join('')}</nav>`;
}

export function page({ skin, mode = 'light', body, w = 412, h = 915, css = [] }) {
  const links = ['base.css', `skin-${skin}.css`, ...css].map(c => `<link rel="stylesheet" href="../src/${c}">`).join('');
  return `<!doctype html><html lang="it"><head><meta charset="utf-8">${links}<style>html,body{width:${w}px;height:${h}px}.phone{width:${w}px;height:${h}px}</style></head><body class="skin-${skin} ${mode}"><div class="phone">${statusBar()}${body}</div></body></html>`;
}
