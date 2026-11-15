// Piccoli costruttori HTML condivisi dalle bozze.
import { ic } from './lib.mjs';

export const bar = (title, acts = []) => `<header class="appbar sub"><button class="iconbtn" aria-label="Indietro">${ic('back')}</button><span class="appbar-title">${title}</span><span class="grow"></span>${acts.map(a => `<button class="iconbtn">${ic(a)}</button>`).join('')}</header>`;
export const closebar = (title, right = '') => `<header class="appbar sub"><button class="iconbtn" aria-label="Chiudi">${ic('x')}</button><span class="appbar-title">${title}</span><span class="grow"></span>${right ? `<span class="link" style="margin-right:14px">${right}</span>` : ''}</header>`;
export const steps = (n, tot) => `<div class="steps"><span>Passo ${n} di ${tot}</span><div class="track">${Array.from({ length: tot }, (_, i) => `<i class="${i < n ? 'done' : ''}"></i>`).join('')}</div></div>`;
export const field = (label, value, o = {}) => `<div class="field"><label>${label}${o.opt ? ' <small>(facoltativo)</small>' : ''}</label><div class="input ${o.cls || ''}">${o.icon ? ic(o.icon, 'sm') : ''}<span class="grow ${o.ph ? 'ph' : ''}">${value}</span>${o.trail || ''}</div>${o.help ? `<div class="help ${o.helpCls || ''}">${o.help}</div>` : ''}</div>`;
export const seg = (opts, on) => `<div class="seg">${opts.map((o, i) => `<div class="${i === on ? 'on' : ''}">${i === on ? ic('check', 'xs') : ''}${o}</div>`).join('')}</div>`;
export const segField = (label, opts, on) => `<div class="field"><label>${label}</label>${seg(opts, on)}</div>`;
export const chips = (opts, on = [], cls = '') => `<div class="chips ${cls}">${opts.map((o, i) => `<span class="chip ${on.includes(i) ? 'on' : ''}">${on.includes(i) ? ic('check', 'xs') : ''}${o}</span>`).join('')}</div>`;
export const item = (o) => `<div class="item ${o.cls || ''}">${o.lead || (o.icon ? `<span class="ico-b">${ic(o.icon)}</span>` : '')}<div class="grow"><div class="t">${o.t}</div>${o.s ? `<div class="s">${o.s}</div>` : ''}</div>${o.r ? `<div class="r">${o.r}${o.rs ? `<small>${o.rs}</small>` : ''}</div>` : ''}${o.trail || ''}</div>`;
export const banner = (kind, icon, title, text = '', acts = []) => `<div class="banner ${kind}">${ic(icon)}<div><b>${title}</b>${text}${acts.length ? `<div>${acts.map(a => `<span class="act">${a}</span>`).join('')}</div>` : ''}</div></div>`;
export const btn = (label, cls = 'primary', icon = '') => `<button class="btn ${cls}">${icon ? ic(icon) : ''}${label}</button>`;
export const bottom = (...b) => `<div class="bottombar ${b.length > 1 ? 'two' : ''}">${b.join('')}</div>`;
export const stepper = (v) => `<div class="stepper"><button aria-label="Meno">${ic('minus')}</button><span>${v}</span><button aria-label="Più">${ic('plus')}</button></div>`;
export const cbx = (on) => `<span class="cbx ${on ? 'on' : ''}">${on ? ic('check') : ''}</span>`;
export const kv = (k, v) => `<div class="kv"><span>${k}</span><b>${v}</b></div>`;
export const chev = ic('chevron', 'sm');
export const status = (k) => ({ prog: `<span class="status s-prog">${ic('calendar', 'xs')}Programmata</span>`, bozza: `<span class="status s-bozza">${ic('edit', 'xs')}Bozza</span>`, ok: `<span class="status s-ok">${ic('check', 'xs')}Completata</span>`, ann: `<span class="status s-ann">${ic('x', 'xs')}Annullata</span>` }[k]);
