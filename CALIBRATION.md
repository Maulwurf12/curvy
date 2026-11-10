# Score mit bekannten Strecken abstimmen

Der Referenzwert 180 Grad/km ist vorläufig. Für eine Kalibrierung brauchen wir
echte Strecken und deine Einschätzung ihrer Kurvigkeit. Die künstlichen Strecken
der automatischen Tests prüfen die Mathematik, liefern aber keinen Maßstab für
das Fahrgefühl.

## Messungen sammeln

1. Curvy starten und `calibration.http` in IntelliJ öffnen.
2. Die Beispielkoordinaten durch Start und Ziel einer bekannten Strecke ersetzen.
3. Die Anfrage ausführen und die zurückgegebene Route prüfen. Curvy wählt die
   kurvigste der drei Alternativen; Start und Ziel allein legen den Straßenverlauf
   nicht eindeutig fest.
4. Im Antwortskript `desiredScore = null` durch deine Bewertung ersetzen, etwa
   `desiredScore = 50`, und erneut ausführen. Prüfen, ob weiterhin die bewertete
   Geometrie zurückgegeben wird.
5. Die Antwort mit ihren Koordinaten speichern und die Ergebnisse notieren.

Verwende mehrere bekannte Strecken mit Bewertungen zwischen 0 und 100, etwa
25, 50 und 75. Eine fast gerade und eine besonders kurvige Strecke dienen als
zusätzliche Kontrolle. Die Anfrage zeigt die Messergebnisse im HTTP-Client-Log
und verändert keine Konfiguration. Sie setzt bewusst keinen `targetScore`.

| Bekannte Strecke | Gemessene Grad/km | Persönlicher Score | Referenzvorschlag |
| --- | ---: | ---: | ---: |
| Noch einzutragen | | | |

## Gemeinsamen Referenzwert finden

Für Bewertungen größer als 0 und kleiner als 100 gilt:

```text
Referenzwert = gemessene Grad/km × 100 / persönlicher Score
```

Erfundenes Rechenbeispiel: 60 Grad/km und gewünschte 50 Punkte ergeben einen
Referenzwert von 120 Grad/km. Das ist keine Kalibrierung anhand echter Straßen.

Vergleiche die Vorschläge mehrerer Strecken. Liegen sie nahe beieinander,
verwende zunächst den mittleren der sortierten Vorschläge (Median; bei gerader
Anzahl den Mittelwert der beiden mittleren Werte). Weichen sie stark ab,
überprüfe die Geometrien und die Bewertungen. Widersprüchliche Einschätzungen
lassen sich nicht mit einer einzigen linearen Skala passend machen.

Bewertungen von 0 gehören nicht in die Berechnung, weil eine Division durch 0
unmöglich ist. Bewertungen von 100 legen wegen der Deckelung nur eine Grenze
fest: Der Referenzwert darf höchstens so groß wie der gemessene Rohwert sein.
Verwende diese Fälle zur Kontrolle, nicht für den Median.

Trage den gemeinsamen Wert unter `curvy.degrees-per-km-for-full-score` in
`src/main/resources/application.yaml` ein. Nach dem Neustart die bekannten
Strecken und zusätzlich eine noch nicht zur Kalibrierung verwendete Strecke
prüfen. Die automatischen Tests verwenden eigene feste Referenzen und prüfen
weiter die Mathematik, nicht das Fahrgefühl echter Straßen.

Die Umwegbegrenzung folgt als nächster Schritt nach dieser Abstimmung.
