# Istruzioni per Claude

- Dopo ogni modifica, quando compila e i test passano: commit su `main` e `git push`, senza chiedere. Niente rami nuovi.
- Autore dei commit: `git -c user.name=Mancio24x -c user.email=Mancio24x@users.noreply.github.com commit ...` (su questo Mac Git non ha un'identità globale).
- Messaggi di commit in italiano, nello stile della storia del repository.
- Ogni modifica all'app va anche in una release, senza chiedere: l'utente aggiorna con Obtainium. Dopo il push, tag annotato `vX.Y.Z` (patch per correzioni, minor per funzioni nuove) e `git push origin vX.Y.Z`; il flusso `release.yml` pubblica gli APK firmati. Prima del tag provare `./gradlew assembleRelease -Pbetterout.version=X.Y.Z` (R8). Poi verificare che il flusso finisca con successo (`gh run watch`). Le modifiche solo a documenti o a questo file non hanno bisogno di release.
