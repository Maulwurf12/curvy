# Curvy

Curvy berechnet mit BRouter drei Routenkandidaten zwischen Start und Ziel
(`alternativeidx` 0, 1 und 2) und bewertet sie mit einem Score von 0 bis 100.
Ohne Wunschwert gewinnt die kurvigste Route. Mit `targetScore` gewinnt die
Route, deren Score am nächsten am Wunschwert liegt.

## Starten

```bash
./mvnw spring-boot:run
```

Die API läuft auf Port 8081. Die BRouter-Adresse wird über `brouter.base-url`
in `application.yaml` oder die Umgebungsvariable `BROUTER_BASE_URL` konfiguriert.
Die Anfragen verwenden weiterhin das feste Profil `car-fast`.

## Route berechnen

```bash
curl -X POST http://localhost:8081/api/routes \
  -H 'Content-Type: application/json' \
  -d '{
    "start": {"latitude": 47.678, "longitude": 9.173},
    "destination": {"latitude": 47.566, "longitude": 10.700}
  }'
```

Für einen Wunschwert ergänzt du die Anfrage um `"targetScore": 70`:

```json
{
  "start": {"latitude": 47.678, "longitude": 9.173},
  "destination": {"latitude": 47.566, "longitude": 10.700},
  "targetScore": 70
}
```

`targetScore` ist optional. Erlaubt sind endliche Zahlen zwischen 0 und 100,
einschließlich Nachkommastellen. Ein fehlender Wert oder `null` wählt die
kurvigste Route. Die drei Kandidaten müssen den gewünschten Score nicht exakt
erreichen: Bei Scores 42, 68 und 89 gewinnt für Wunschwert 70 die Route mit 68.

Die Antwort enthält `coordinates`, `curviness`, den gewählten `alternativeIdx`
sowie `targetScore` und `scoreDeviation`. Die Abweichung ist
`abs(curviness.score - targetScore)`; ohne Wunschwert sind beide Zusatzfelder `null`.
Folgendes Beispiel zeigt nur eine synthetische Strecke mit einer 90°-Kurve,
ohne Wunschwert und mit der unten beschriebenen Standardkonfiguration:

```json
{
  "coordinates": [
    {"latitude": 0.0, "longitude": 0.0},
    {"latitude": 0.0, "longitude": 1.0},
    {"latitude": 1.0, "longitude": 1.0}
  ],
  "curviness": {
    "distanceKm": 222.3901604670658,
    "totalTurnAngleDegrees": 90.0,
    "degreesPerKm": 0.4046941636760421,
    "score": 0.2248300909311345
  },
  "alternativeIdx": 1,
  "targetScore": null,
  "scoreDeviation": null
}
```

## Bewertung und Auswahl

`CurvinessScorer` summiert die absoluten Richtungswechsel an den inneren Punkten
und teilt sie durch die Streckenlänge in Kilometern. Dieser Rohwert steht in
`degreesPerKm` und hat die Einheit **Grad pro Kilometer**. `score` enthält nun
die Bewertung von 0 bis 100. Die Länge wird mit der Haversine-Formel auf der
Erdoberfläche berechnet, ohne Höhenprofil. Doppelte aufeinanderfolgende Punkte
werden übersprungen; Links- und Rechtskurven zählen gleich.

```text
degreesPerKm = totalTurnAngleDegrees / distanceKm
score = min(100, 100 × degreesPerKm / Referenzwert)
```

Der Referenzwert steht in `application.yaml`:

```yaml
curvy:
  degrees-per-km-for-full-score: 180
```

**180 °/km ist ein vorläufiger Startwert und noch nicht anhand echter Strecken
kalibriert.** Bei dieser Einstellung ergeben 0 °/km den Score 0, 90 °/km den
Score 50 und mindestens 180 °/km den Score 100. Wunschwert 70 entspricht
126 °/km. Der Referenzwert muss endlich und größer als 0 sein und gilt für alle
Routen gleichermaßen. Die API rundet die Scores nicht.

`CurvyRoutingService` fragt die drei Alternativen nacheinander ab und bewertet
jeden gültigen Kandidaten. Ohne Wunschwert gewinnt der höchste Rohwert in °/km;
das unterscheidet auch zwei Routen, die beide schon Score 100 erreichen. Mit
Wunschwert gewinnt die kleinste Scoreabweichung. Bei Gleichstand bleibt der
kleinere Alternativindex erhalten, auch wenn BRouter dieselbe Route mehrfach
liefert. Eine gerade Route mit Score 0 ist ein gültiger Kandidat.

Fehlgeschlagene Anfragen, leere oder fehlerhafte GeoJSON-Antworten, ungültige
Koordinaten, Geometrien ohne mindestens zwei Punkte und Routen ohne Länge werden
übersprungen. Solange mindestens eine gültige Route vorliegt, wird diese Auswahl
mit HTTP `200` zurückgegeben. Wenn alle Kandidaten ausfallen, antwortet die API
mit HTTP `502`. Ungültige Start- oder Zielkoordinaten sowie Wunschwerte außerhalb
von 0 bis 100 führen vor den
BRouter-Anfragen zu HTTP `400`.

Die Auswahl erfolgt unter den drei geprüften Alternativen. Gleichmäßige
Messabstände, die Verteilung der Kurven, Kreuzungsabbiegungen und eine Begrenzung
unnötiger Umwege sind mögliche spätere Verbesserungen. Score 100 bedeutet aktuell,
dass die mittlere Kurvendichte den Referenzwert erreicht; er garantiert keine
durchgehend kurvige Strecke.

## Tests

```bash
./mvnw test
```

Die Tests verwenden simulierte BRouter-Antworten und benötigen keinen Zugriff
auf eine öffentliche BRouter-Instanz.

Eine Anleitung mit festen Sollwerten und ausführbaren HTTP-Anfragen findest du
in [TESTING.md](TESTING.md).
