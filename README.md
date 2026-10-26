# Curvy

Curvy berechnet mit BRouter drei Routenkandidaten zwischen Start und Ziel
(`alternativeidx` 0, 1 und 2) und liefert den Kandidaten mit dem höchsten
Curviness-Score zurück.

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

Die Antwort enthält `coordinates`, `curviness` und den gewählten `alternativeIdx`.
Folgendes Beispiel zeigt nur eine synthetische Strecke mit einer 90°-Kurve,
kein echtes BRouter-Ergebnis zur obigen Anfrage:

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
    "score": 0.4046941636760421
  },
  "alternativeIdx": 1
}
```

## Bewertung und Auswahl

`CurvinessScorer` summiert die absoluten Richtungswechsel an den inneren Punkten
und teilt sie durch die Streckenlänge in Kilometern. Der Score hat die Einheit
**Grad pro Kilometer**. Die Länge wird mit der Haversine-Formel auf der
Erdoberfläche berechnet, ohne Höhenprofil. Doppelte aufeinanderfolgende Punkte
werden übersprungen; Links- und Rechtskurven zählen gleich.

`CurvyRoutingService` fragt die drei Alternativen nacheinander ab und bewertet
jeden gültigen Kandidaten. Der höchste Score gewinnt. Bei Gleichstand bleibt
der kleinere Alternativindex erhalten, auch wenn BRouter dieselbe Route
mehrfach liefert. Eine gerade Route mit Score 0 ist ein gültiger Kandidat.

Fehlgeschlagene Anfragen, leere oder fehlerhafte GeoJSON-Antworten, ungültige
Koordinaten, Geometrien ohne mindestens zwei Punkte und Routen ohne Länge werden
übersprungen. Solange mindestens eine gültige Route vorliegt, wird diese Auswahl
mit HTTP `200` zurückgegeben. Wenn alle Kandidaten ausfallen, antwortet die API
mit HTTP `502`. Ungültige Start- oder Zielkoordinaten führen vor den
BRouter-Anfragen zu HTTP `400`.

Damit wird die kurvigste der geprüften Alternativen gewählt. Punktabstände,
Kreuzungsabbiegungen und unnötige Umwege werden noch nicht gesondert gewichtet.

## Tests

```bash
./mvnw test
```

Die Tests verwenden simulierte BRouter-Antworten und benötigen keinen Zugriff
auf eine öffentliche BRouter-Instanz.
