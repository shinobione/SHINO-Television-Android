# SHINO // TV HOME

## Version actuelle : v2.0.1 — Import USB SHINO HUB v2

Application Android TV existante conservée, package `fr.shinobiwan.tvhome`, code 4.
Connexion HTTPS au HUB par `GET /hub/v1/snapshot`, domaines PC/musique/Codex
indépendants, pochette JPEG authentifiée, fraîcheur par domaine et quotas
indisponibles affichés honnêtement. Horloge, météo, applications et D-pad conservés.

- APK debug local : `artifacts/v2.0.1/SHINO-TV-HOME-v2.0.1-debug.apk`.
- [Import USB V2.0.1](docs/V2.0.1-USB-IMPORT.md), [validation V2.0.1](docs/V2.0.1-DELIVERY.md) et [appairage HUB v2](docs/V2-PAIRING.md).
- CA publique, pin SHA-256 et jeton **lecteur TV_HOME** à renseigner dans Réglages.
  Aucun secret réel ni certificat personnel n'est distribué avec les sources/APK.
- Valeurs par défaut HUB : `192.168.1.195:8767`. Le mode passerelle v1 reste
  disponible ; les installations ayant déjà un appairage gardent leur mode v1.
- L'état musique distingue Lecture/Pause/Arrêté. Le HUB étant en lecture seule,
  les boutons de contrôle distant PC ne sont pas proposés. Les commandes TV
  existantes restent disponibles lorsque l'application média les autorise.
- Build : `.\build-local.ps1` ; vérification :
  `.\verify-build.ps1 -ArtifactTag v2.0.1 -Version 2.0.1 -VersionCode 4`.

Aucune installation ni aucun lancement automatique sur Freebox Pop.
Les anciens documents ci-dessous décrivent leurs versions historiques.

## Historique : v0.2 — Premium TV Experience

Refonte native Compose : identité et heure agrandies, météo réunie dans le bandeau, applications par pages de cinq tuiles complètes, bannières TV natives, widgets différenciés et détails accessibles au D-pad. Le modèle métier, le cache et les préférences existants sont conservés.

- APK v0.2 : `artifacts/v0.2/SHINO-TV-HOME-v0.2-debug.apk`.
- [Rapport v0.2 et mise à jour](docs/V0.2-REPORT.md).
- Rendus du même code Compose sur Android API29 simulé par Robolectric : `artifacts/v0.2/native/`. Ce ne sont pas des captures de la Freebox ; applications et médias des tests sont synthétiques.
- Galerie locale : `artifacts/v0.2/preview.html`.
- `preview/index.html` reste l'ancienne maquette HTML v0.1, pas la référence v0.2.
- Compilation : `.\build-local.ps1` ; vérification séparée : `.\verify-build.ps1` (sorties v0.2, aucun écrasement du livrable v0.1).

La v0.2 n'est ni installée ni lancée automatiquement sur la Pop. Une nouvelle autorisation est requise pour chaque opération matérielle. Le binaire v0.1 installé reste inchangé.

## Référence v0.1 et documentation existante

Application Android TV personnelle, indépendante de SHINO SmallTV. Kotlin + Compose for TV. Package `fr.shinobiwan.tvhome`, Android 8.0/API 26 minimum, SDK cible 35. **Application LEANBACK standard, pas un remplacement HOME.**

Le logo officiel fourni est utilisé sans modification. Les raccourcis sont les applications TV réellement installées ; aucun raccourci commercial factice. Les widgets absents restent explicitement indisponibles. DEMO est désactivé par défaut.

## Livrables locaux

Racine : `C:\Users\jerry\Documents\SHINO Television Android`

- APK : `artifacts\SHINO-TV-HOME-v0.1-debug.apk`
- Rapport : [docs/VALIDATION.md](docs/VALIDATION.md)
- Installation et retour : [docs/INSTALLATION.md](docs/INSTALLATION.md)
- Préparation Freebox Pop / ADB réseau / USB : [docs/HARDWARE-VALIDATION.md](docs/HARDWARE-VALIDATION.md)
- Audit Windows pour v0.2 : [docs/V0.2-INTEGRATION-AUDIT.md](docs/V0.2-INTEGRATION-AUDIT.md)
- Appairage et contrat : [docs/PAIRING.md](docs/PAIRING.md), [snapshot.schema.json](companion/windows/snapshot.schema.json)
- Passerelle : [companion/windows/gateway.py](companion/windows/gateway.py)
- Audit : [docs/AUDIT.md](docs/AUDIT.md)
- Recette télécommande/Freebox : [docs/DEVICE-CHECKLIST.md](docs/DEVICE-CHECKLIST.md)
- Aperçu visuel HTML : `preview\index.html` — composition de référence statique, pas capture Android.

## Compiler

Dans PowerShell, à la racine :

```powershell
.\build-local.ps1
.\build-local.ps1 -Tasks ':app:assembleDebugAndroidTest'
$env:TEMP = Join-Path $PWD '.local-build\tmp'
$env:TMP = $env:TEMP
python -m unittest discover -s tests -v
```

Les 4 tests de schéma requièrent `python -m pip install --target .local-build/python-deps -r tests/requirements.txt` (installation locale au projet). Sans ce validateur, ces 4 tests sont marqués ignorés ; les 19 tests annoncés dans le rapport ont tous été exécutés avec ces dépendances.

Le script utilise les chemins Java/Gradle de `.local-build\tool-paths.json`, le SDK et les caches propres au projet. Pour un autre poste, installer volontairement les outils officiels Java 17, SDK Android 35, Build Tools 35.0.0, puis configurer ce fichier local avec `JavaHome` et `GradleHome`, ou utiliser `gradlew.bat` avec ses variables locales `JAVA_HOME`, `ANDROID_HOME` et `GRADLE_USER_HOME`. Wrapper Gradle 8.11.1 avec SHA-256 épinglé. Ne pas committer les outils, clés ou caches.

Versions figées : AGP 8.9.2, Kotlin/Compose compiler 2.0.21, Compose BOM 2024.12.01, TV Material 1.0.0, Activity 1.9.3, Lifecycle 2.8.7, DataStore 1.1.1, Coroutines 1.9.0. Versions plus récentes signalées par lint, mise à niveau hors de cette première compilation compatible SDK 35.

## Utilisation

1. Ouvrir depuis Projectivy ou le launcher TV habituel.
2. Les flèches naviguent et OK ouvre les applications. Retour dans les réglages revient à l'accueil ; Retour à l'accueil laisse Android revenir au launcher précédent.
3. Paramètres : réordonner/masquer les applications, choisir 1–3 widgets, ville, source média et appairage PC.
4. TV média facultatif : ouvrir le réglage Notification Listener et décider de l'autoriser sur l'appareil. Aucun service d'accessibilité.
5. Pour voir les états visuels fictifs, activer explicitement DEMO. Les quotas et le morceau portent cette mention.

Le compagnon lit l'historique Periscope existant, au plus toutes les 60 secondes côté TV. L'historique source n'est mis à jour que toutes les cinq minutes au minimum, si son enregistrement est activé. Une passerelle connectée ne signifie pas un quota nouvellement mesuré. Le timestamp source et le caractère périmé restent visibles. Activité toujours `UNVERIFIED` en v0.1.

## Périmètre et limites

Pas de publication, installation automatique, réglage HOME, changement du launcher Free/Projectivy, modification de LINK/Periscope, accès Codex auth ou commande distante. La passerelle n'est pas démarrée par le build.

Les tests instrumentés fournis doivent être exécutés sur un émulateur/TV autorisé. Un APK compilé et signé ne démontre ni les permissions disponibles sur Pop, ni son comportement de focus physique. Voir le rapport de validation pour les preuves et limites exactes.

## v0.2

- Recette physique Freebox Pop et focus de retour par application.
- Appairage QR/local simplifié après une revue spécifique, sans perdre TLS/pinning.
- Export officiel de snapshot par Periscope pour réduire le délai de l'historique, uniquement dans une mission séparée autorisée.
- SMTC Windows : projection compatible, pochette bornée, tests avec un lecteur réel.
- Widgets configurables par taille et sélection média plus détaillée.
- Transition HOME optionnelle après validation et procédure de restauration testée.
