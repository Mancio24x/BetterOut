#!/usr/bin/env python3
"""
Genera l'elenco offline delle cime e dei rifugi delle Alpi italiane, francesi e svizzere (Dolomiti comprese).

Fonte: OpenStreetMap tramite l'API Overpass. Dati © OpenStreetMap contributors, licenza ODbL 1.0.
Uscita: app/src/main/assets/luoghi-alpi.tsv (testo: l'APK lo comprime da sé), una riga per luogo: tipo (C cima, R rifugio), latitudine, longitudine, nome, nome normalizzato per la ricerca.

Uso:  python3 tools/luoghi/genera.py
"""
import os
import unicodedata
import sys
import urllib.parse
import urllib.request

OVERPASS = "https://overpass-api.de/api/interpreter"
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "luoghi-alpi.tsv")

# Arco alpino approssimato (lat lon): esclude Giura, Appennino e pianura. L'Austria resta fuori per il filtro sui paesi.
ALPI = [
    (43.75, 5.60), (44.60, 5.10), (45.20, 5.30), (45.80, 5.60), (46.20, 6.00), (46.80, 6.80), (47.10, 7.80),
    (47.40, 8.80), (47.55, 9.60), (47.60, 10.50), (47.20, 12.50), (46.90, 13.95), (46.00, 13.95), (46.00, 12.50),
    (45.75, 11.50), (45.60, 10.80), (45.65, 9.50), (45.75, 8.60), (45.40, 7.80), (45.00, 7.35), (44.45, 7.50),
    (44.15, 8.10), (43.95, 8.00), (43.75, 7.50), (43.70, 7.00),
]

QUERY = """
[out:csv(::lat,::lon,name,natural,tourism;false;"\\t")][timeout:600];
(area["ISO3166-1"="IT"][admin_level=2];area["ISO3166-1"="FR"][admin_level=2];area["ISO3166-1"="CH"][admin_level=2];)->.paesi;
(
  nwr["natural"="peak"]["name"](area.paesi)(poly:"{poly}");
  nwr["tourism"~"^(alpine_hut|wilderness_hut)$"]["name"](area.paesi)(poly:"{poly}");
);
out center;
""".replace("{poly}", " ".join(f"{lat} {lon}" for lat, lon in ALPI))


def search_key(name):
    """Come OfflinePlaces.normalize nell'app: minuscole, senza accenti, lettere e cifre separate da uno spazio."""
    s = unicodedata.normalize("NFD", name.lower())
    s = "".join(c for c in s if not unicodedata.category(c).startswith("M"))
    return " ".join("".join(c if c.isalnum() else " " for c in s).split())


def main():
    body = urllib.parse.urlencode({"data": QUERY}).encode()
    request = urllib.request.Request(OVERPASS, data=body, headers={"User-Agent": "BetterOut-genera-luoghi/1.0"})
    with urllib.request.urlopen(request, timeout=900) as response:
        text = response.read().decode("utf-8")

    rows = set()
    for line in text.splitlines():
        parts = line.split("\t")
        if len(parts) != 5 or not parts[0] or not parts[2].strip():
            continue
        lat, lon, name, natural, tourism = parts
        kind = "C" if natural == "peak" else "R" if tourism else None
        if kind is None:
            continue
        # Overpass mette tra virgolette (raddoppiando quelle interne) i nomi che contengono virgolette.
        if len(name) >= 2 and name[0] == '"' and name[-1] == '"':
            name = name[1:-1].replace('""', '"')
        name = " ".join(name.split())  # niente tabulazioni o a capo nel nome
        rows.add((kind, f"{float(lat):.5f}", f"{float(lon):.5f}", name))

    if len(rows) < 1000:
        sys.exit(f"Troppo pochi luoghi ({len(rows)}): risposta di Overpass incompleta?\n{text[:500]}")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    data = "".join("\t".join(r + (search_key(r[3]),)) + "\n" for r in sorted(rows, key=lambda r: (r[3].lower(), r[0], r[1], r[2])))
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(data)
    peaks = sum(1 for r in rows if r[0] == "C")
    print(f"{len(rows)} luoghi ({peaks} cime, {len(rows) - peaks} rifugi), {os.path.getsize(OUT) / 1e6:.1f} MB -> {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
