// Registro delle bozze: renderizza ogni schermata, i fogli riassuntivi e l'indice consultabile.
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { SCREENS_1 } from './bozze-1.mjs';
import { SCREENS_2 } from './bozze-2.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
export const SKIN = 'b';
export const GROUPS = [
  ['uscite', 'Uscite'], ['creazione', 'Creazione e modifica dell’uscita'], ['mappa', 'Mappa, percorsi e GPX'], ['meteo', 'Meteo'],
  ['preparazione', 'Preparazione, suggerimenti e checklist'], ['attrezzatura', 'Attrezzatura'], ['impostazioni', 'Impostazioni, backup ed esportazioni'], ['dialoghi', 'Dialoghi'],
];
export const ALL = [...SCREENS_1, ...SCREENS_2].sort((a, b) => a.id.localeCompare(b.id));

export async function buildAll({ render, sheet, filter }) {
  const list = filter ? ALL.filter(s => s.id.includes(filter) || s.group === filter) : ALL;
  for (const s of list) { render(s.id, 'bozze', { skin: SKIN, mode: s.mode || 'light', body: s.body, css: ['app.css'] }); process.stdout.write('.'); }
  if (!filter) {
    for (const [g, name] of GROUPS) {
      const items = ALL.filter(s => s.group === g).map(s => ({ png: s.id + '.png', label: s.title, sub: s.id }));
      sheet(`foglio-${g}`, 'bozze', `BetterOut — ${name}`, Math.min(6, items.length), items);
    }
    let md = '# Bozze grafiche\n\nDirezione visiva scelta: **B · Rifugio**. Ogni immagine è una bozza con dati d’esempio, generata da `design/tools` (vedi `design/README.md`).\n';
    let html = `<!doctype html><html lang="it"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>BetterOut — bozze</title><style>body{font-family:system-ui,sans-serif;margin:0;background:#F3ECDF;color:#2A2620}main{max-width:1280px;margin:0 auto;padding:24px 16px}h1{font-size:28px}h2{margin-top:40px;border-bottom:2px solid #2A2620;padding-bottom:6px}.g{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:20px}figure{margin:0}img{width:100%;border-radius:16px;box-shadow:0 4px 16px rgba(0,0,0,.2);display:block}figcaption{font-size:14px;margin-top:8px}figcaption b{display:block}figcaption span{color:#6A6155}</style><main><h1>BetterOut — bozze grafiche</h1><p>Direzione visiva B · Rifugio. Dati d’esempio. Tocca un’immagine per ingrandirla.</p>`;
    for (const [g, name] of GROUPS) {
      md += `\n## ${name}\n\n![${name}](bozze/foglio-${g}.png)\n\n| Bozza | Schermata | Cosa mostra |\n|---|---|---|\n`;
      html += `<h2>${name}</h2><div class="g">`;
      for (const s of ALL.filter(x => x.group === g)) {
        md += `| [${s.id}](bozze/${s.id}.png) | ${s.title} | ${s.note} |\n`;
        html += `<figure><a href="bozze/${s.id}.png"><img loading="lazy" src="bozze/${s.id}.png" alt="${s.title}"></a><figcaption><b>${s.title}</b><span>${s.note}</span></figcaption></figure>`;
      }
      html += '</div>';
    }
    writeFileSync(join(ROOT, 'INDICE.md'), md);
    writeFileSync(join(ROOT, 'index.html'), html + '</main></html>');
  }
  console.log(`\nbozze: ${list.length} schermate`);
}
