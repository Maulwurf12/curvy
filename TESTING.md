# Curvy testen

## Automatisch und ohne BRouter-Zugriff

Alle Tests ausführen:

```bash
./mvnw test
```

Nur die nachvollziehbaren Testdaten und die zugehörige API-Auswahl prüfen:

```bash
./mvnw -Dtest=RouteFixtureTests test
```

In IntelliJ kannst du dieselben Tests über das grüne Ausführen-Symbol neben
`RouteFixtureTests` oder einem einzelnen Test starten. Ein laufender Curvy-Server
ist dafür nicht nötig.

Die Dateien unter `src/test/resources/routing/fixtures/` enthalten vollständig
synthetische BRouter-Antworten. Alle drei Strecken beginnen bei Breitengrad 0,
Längengrad 0 und enden bei Breitengrad 1, Längengrad 1. GeoJSON speichert Punkte
als `[Längengrad, Breitengrad]`.

| Datei | Strecke | Länge in km | Winkelsumme | Score in °/km |
| --- | --- | ---: | ---: | ---: |
| `alternative-0.geojson` | Direkte Verbindung | 157,249598 | 0° | 0 |
| `alternative-1.geojson` | Einmal 90° abbiegen | 222,390160 | 90° | 0,404694164 |
| `alternative-2.geojson` | Langer Umweg mit Kehrtwende | 2.446,291765 | 270° | 0,110371136 |

**Erwarteter Gewinner: Alternative 1.** Alternative 0 ist kürzer und Alternative
2 hat mehr Kurvenwinkel, aber Alternative 1 hat den höchsten Score pro Kilometer.
Wenn Alternative 1 ausfällt, muss Alternative 2 gewinnen.

Die Sollwerte stehen in `expected-scores.json`; die vollständige erwartete
API-Antwort steht in `expected-response.json`. `request.json` enthält die Anfrage,
die der Test an den Controller sendet. Diese Koordinaten sind künstlich und für
den Test mit simulierten Antworten gedacht.

Die Sollwerte sind unabhängig vom Java-Scorer berechnet: Mit dem Erdradius
6.371,0088 km beträgt ein Grad auf dem Äquator oder einem Meridian
`u = π × 6371,0088 / 180` Kilometer. Alternative 1 hat Länge `2 × u` und Winkel
90°, Alternative 2 Länge `22 × u` und Winkel 270°. Die direkte Verbindung hat
Länge `6371,0088 × acos(cos(π/180)²)` und keine inneren Punkte für einen
Richtungswechsel. Die Tests erlauben numerische Abweichungen bis `1e-8`.

`RouteFixtureTests` prüft die Zahlen aller drei Kandidaten und die gesamte Kette
von der JSON-Anfrage über den BRouter-Client, Koordinatenextraktion, Bewertung und
Auswahl bis zur JSON-Antwort. Nur die HTTP-Antworten von BRouter werden simuliert.
Die übrigen Testklassen decken zusätzlich Gleichstände, ungültige Geometrien,
Netzwerkfehler, HTTP 400/502 und Sonderfälle der Winkelberechnung ab.

## Laufende API in IntelliJ prüfen

1. Curvy mit `./mvnw spring-boot:run` starten.
2. `testreq.http` öffnen; bei Bedarf `baseUrl` anpassen.
3. Die Anfragen einzeln oder gemeinsam ausführen.
4. Die Ergebnisse der automatischen Prüfungen im HTTP Client ansehen.

Die erste Anfrage verwendet echtes BRouter und prüft HTTP 200, Koordinaten,
Alternativindex, endliche Zahlen und die Score-Formel. Die beiden ungültigen
Anfragen müssen HTTP 400 liefern und benötigen keinen BRouter-Zugriff.

Bei echten Routen gibt es keinen festen erwarteten Score oder Gewinner: Die
Antwort hängt von den gelieferten BRouter-Geometrien ab. Die HTTP-Prüfungen
kontrollieren die Antwort auf Plausibilität; die korrekte Auswahl zwischen
bekannten Kandidaten prüfen die reproduzierbaren Tests oben.
