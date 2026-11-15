// Genera le bozze grafiche in PNG.
// Uso:  node design/tools/build.mjs direzioni      (tre direzioni visive a confronto)
//       node design/tools/build.mjs bozze [filtro] (tutte le schermate nella direzione scelta)
// Richiede Google Chrome (variabile CHROME per indicare un altro eseguibile).
import { writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { page } from './lib.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const BUILD = join(ROOT, '.build');
const CHROME = process.env.CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const PROFILE = process.env.CHROME_PROFILE || join(process.env.TMPDIR || '/tmp', 'betterout-chrome');
mkdirSync(BUILD, { recursive: true });

export function shot(htmlPath, pngPath, w, h, scale = 2) {
  mkdirSync(dirname(pngPath), { recursive: true });
  execFileSync(CHROME, ['--headless=new', '--disable-gpu', '--no-first-run', `--user-data-dir=${PROFILE}`, '--hide-scrollbars',
    `--force-device-scale-factor=${scale}`, `--window-size=${w},${h}`, '--virtual-time-budget=1500', `--screenshot=${pngPath}`, `file://${htmlPath}`], { stdio: 'ignore' });
  if (!existsSync(pngPath)) throw new Error('rendering fallito: ' + pngPath);
}

export function render(name, outDir, opts) {
  const html = join(BUILD, name + '.html');
  writeFileSync(html, page(opts));
  shot(html, join(ROOT, outDir, name + '.png'), opts.w || 412, opts.h || 915);
  return name + '.png';
}

// Foglio di confronto: più PNG affiancati con didascalia.
export function sheet(name, outDir, title, cols, items, scale = 1) {
  const cw = 412 * 0.62, ch = 915 * 0.62, gap = 28, pad = 36;
  const rows = Math.ceil(items.length / cols);
  const w = Math.round(pad * 2 + cols * cw + (cols - 1) * gap), h = Math.round(pad * 2 + 60 + rows * (ch + 54) + (rows - 1) * gap);
  const html = `<!doctype html><html lang="it"><meta charset="utf-8"><style>
  @font-face{font-family:"Archivo";src:url("../fonts/Archivo-Variable.ttf");font-weight:100 900;font-stretch:62% 125%}
  body{margin:0;background:#D9D7D0;font-family:Archivo,sans-serif;color:#17201B;width:${w}px;height:${h}px;overflow:hidden}
  .wrap{padding:${pad}px}h1{font-size:26px;font-weight:800;margin:0 0 26px;height:34px}
  .grid{display:grid;grid-template-columns:repeat(${cols},${cw}px);gap:${gap}px}
  figure{margin:0}img{width:${cw}px;height:${ch}px;display:block;border-radius:18px;box-shadow:0 6px 24px rgba(0,0,0,.25)}
  figcaption{font-size:15px;font-weight:600;margin-top:12px;height:42px;line-height:1.3}figcaption small{display:block;font-weight:400;color:#565E57;font-size:13px}
  </style><div class="wrap"><h1>${title}</h1><div class="grid">${items.map(i => `<figure><img src="../${outDir}/${i.png}"><figcaption>${i.label}<small>${i.sub || ''}</small></figcaption></figure>`).join('')}</div></div></html>`;
  const p = join(BUILD, name + '.html');
  writeFileSync(p, html);
  shot(p, join(ROOT, outDir, name + '.png'), w, h, scale);
}

const mode = process.argv[2];
if (mode === 'direzioni') {
  const { home, dettaglio } = await import('./direzioni.mjs');
  const names = { a: 'A · Carta', b: 'B · Rifugio', c: 'C · Segnavia' };
  const light = [], dark = [];
  for (const skin of ['a', 'b', 'c']) {
    const U = skin.toUpperCase();
    light.push({ png: render(`${U}-home-chiaro`, 'direzioni', { skin, body: home() }), label: `${names[skin]} — Home`, sub: 'tema chiaro' });
    light.push({ png: render(`${U}-dettaglio-chiaro`, 'direzioni', { skin, body: dettaglio() }), label: `${names[skin]} — Dettaglio uscita`, sub: 'tema chiaro' });
    dark.push({ png: render(`${U}-home-scuro`, 'direzioni', { skin, mode: 'dark', body: home() }), label: `${names[skin]} — Home`, sub: 'tema scuro' });
    dark.push({ png: render(`${U}-dettaglio-scuro`, 'direzioni', { skin, mode: 'dark', body: dettaglio() }), label: `${names[skin]} — Dettaglio uscita`, sub: 'tema scuro' });
  }
  sheet('confronto-chiaro', 'direzioni', 'BetterOut — tre direzioni visive, tema chiaro', 6, light);
  sheet('confronto-scuro', 'direzioni', 'BetterOut — tre direzioni visive, tema scuro', 6, dark);
  console.log('direzioni: ok');
} else if (mode === 'bozze') {
  const { buildAll } = await import('./bozze.mjs');
  await buildAll({ render, sheet, filter: process.argv[3] });
} else {
  console.error('uso: node design/tools/build.mjs direzioni|bozze [filtro]');
  process.exit(1);
}
