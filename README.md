# KORE Training

App Android personale (Kotlin + Jetpack Compose, tutto offline) per allenamento in palestra con
programma periodizzato annuale, progressioni automatiche dei carichi e monitoraggio dei progressi.

## Installare l'APK
Ogni modifica sul ramo `main` viene compilata da GitHub Actions. L'APK si scarica dalla pagina
**Releases → latest** del repository (file `KORE-Training.apk`). Sul telefono consenti
"Installa app sconosciute" per il browser o il gestore file.

## Funzioni
- **Profilo e onboarding**: sesso, età, altezza, peso, massa grassa, livello, split, giorni, metabolismo, massimali.
- **Programma annuale in 3 fasi** (Forza & Ipertrofia, Trasformazione Ipertrofica, Qualità Muscolare),
  ognuna con 3 mesocicli da 5 settimane di carico + 1 di scarico.
- **3 organizzazioni settimanali**: split 4 giorni A, split 4 giorni B, Push/Legs/Pull ×2.
- **Prescrizioni automatiche** per categoria (fondamentale, complementare, isolamento): serie, range di
  ripetizioni, RIR, recupero, tempo, tecniche di intensità (rest-pause, stripping, back-off, isometria).
- **Carichi suggeriti**: % del massimale sui fondamentali, doppia progressione sugli altri esercizi,
  ricalcolo da 1RM stimato quando cambia il range, -15% nelle settimane di scarico.
- **Allenamento live**: serie di riscaldamento generate, registrazione kg/ripetizioni/RIR, timer di recupero
  con vibrazione e notifica, sostituzione esercizi (anche permanente), prestazione della volta precedente.
- **Progressi**: tonnellaggio settimanale, serie per muscolo rispetto ai volumi di riferimento, record,
  grafico 1RM stimato e volume per esercizio, storico sedute.
- **Strumenti**: calcolatore 1RM con tabella percentuali, tabella RIR/RPE, calcolatore dischi,
  calorie e macro con calorie cicliche, diario del peso con medie settimanali e consigli.
- **Libreria esercizi** con indicazioni tecniche e link a video YouTube, **guida al metodo**.
- **Backup** esporta/importa in JSON.

## Compilare in locale
Apri la cartella con Android Studio (Ladybug o più recente) e premi Run, oppure:
```
./gradlew assembleDebug
```
L'APK è in `app/build/outputs/apk/debug/app-debug.apk`.

## Versione iPhone (web app)
La cartella `ios/` contiene la stessa app come web app installabile (PWA), senza bisogno di Mac
né di App Store. Ogni modifica su `main` viene pubblicata su GitHub Pages da
`.github/workflows/pages.yml`: https://carlo6666.github.io/gym-training/

Su iPhone apri il link con **Safari**, tocca **Condividi → Aggiungi alla schermata Home** e
usala dall'icona KORE: funziona offline e i dati restano sul telefono. Il backup JSON è
compatibile con l'app Android (Profilo → Backup), così puoi spostare i dati tra i due telefoni.

Prova in locale: `cd ios && python3 -m http.server 8000` e apri http://localhost:8000.
