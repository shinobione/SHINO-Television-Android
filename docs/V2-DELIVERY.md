# SHINO // TV HOME v2.0.0 — livraison

Application native existante conservée : package `fr.shinobiwan.tvhome`, code 3,
minSdk 26, target/compile SDK 35, Java 17, Gradle 8.11.1, AGP 8.9.2 et Compose.
Les deux commits v0.1/v0.2 restent les ancêtres du commit d'intégration v2.

## APK

Fichier local : `artifacts/v2/SHINO-TV-HOME-v2-debug.apk` (8 362 971 octets).

SHA-256 : `b8591a64ab8b527c38021d9b89c421e17327adf0d2b8d1bfd8432ff9dc1882d1`.

Signature debug, alignement 16 Kio, version/package et manifeste Leanback
vérifiés par `verify-build.ps1 -ArtifactTag v2 -Version 2.0.0 -VersionCode 3`.
Logo officiel inchangé. Aucun HOME ni service d'accessibilité ajouté.
Les APK, outils, caches et preuves locales restent exclus de Git.

## Fonctionnalités

- Mode SHINO HUB v2 sur HTTPS, route exacte `/hub/v1/snapshot`, et mode v1 conservé.
- PC : CPU, GPU, RAM `ramUsedGiB` affichée en Go, température GPU ; chaque
  capteur absent garde une valeur indisponible, sans faux zéro.
- Musique PC : titre/artiste, pochette JPEG authentifiée, progression observée
  nullable, états lecture/pause/arrêt. Identifiant contenu SHA-256, taille
  maximale 40 Kio, dimensions maximales 512, décodage et hash vérifiés.
- Quotas réels s'ils existent, sinon indisponibles ; provenance DIRECT/HISTORY
  conservée, activité de mission toujours UNVERIFIED.
- Fraîcheur par domaine : 6/10/900 secondes, limite exacte et resets Codex ;
  cache lié à l'appairage, déconnexion explicite, TEST/DEMO visibles.
- CA dédiée à l'app + nom/SAN standard + pin DER, jeton TV_HOME chiffré localement.
- Horloge, météo, applications, réglages et télécommande conservés.
- Correction finale des textes coupés : hauteur suffisante pour température,
  fraîcheur et provenance Codex, vérifiée avec le moteur de texte Compose.

## Vérification

Avant l'interruption quota : 27 tests JVM de modèle/contrat/transport/présentation
validés (HubTransport 7, HubV2 9, Model 7, TvPresentation 4), dont HTTPS TLS1.3
sur loopback avec certificats synthétiques générés par les tests ; 20 tests
Python validés, y compris validation du transcript TEST avec le schéma RELAY.

À la reprise : les 10 tests `NativeHomeTest` ont été exécutés et passent,
dont navigation D-pad, retour de focus et deux cas HUB anti-troncature.
Les tests déjà validés n'ont pas été relancés inutilement.
Compilation finale `:app:assembleDebug :app:lintDebug` réussie : zéro erreur
lint, 17 avertissements. APK d'instrumentation compilé précédemment ; aucun test
d'instrumentation exécuté sur appareil.

Rendus contrôlés visuellement sous Robolectric API29 :
`artifacts/v2/native/native-api29-hub-v2-test.png` et
`native-api29-hub-v2-telemetry-only.png`. Données/apps synthétiques, pas des
captures Freebox. Rapports et vérifications restent sous `artifacts/v2/`.

## Limites réelles

L'APK est à appairer avec la CA publique, l'empreinte serveur et le jeton lecteur
TV_HOME existants : voir [V2-PAIRING.md](V2-PAIRING.md). Aucun secret réel n'a
été récupéré, embarqué ou publié. L'appairage au HUB réel et TLS1.3 sur Pop
restent à qualifier sur l'appareil.

Le HUB n'expose aucune commande PC play/pause : l'application affiche cet état,
sans inventer de route d'écriture. Les commandes média TV existantes subsistent.
Une réception récente ne rend pas une source ancienne fraîche. Aucun état
TEST/DEMO n'est présenté comme LIVE. Aucune installation/lancement sur Freebox,
modification de RELAY/HUB/LINK/Periscope/SmallTV ni configuration HOME.

## Publication

Sources et assets nécessaires uniquement ; historique local préservé, sans force
push. Contrôle de la liste candidate et de l'historique par motifs de credentials
et fichiers privés, complété par une revue ciblée Advisor. Ce contrôle ne prouve
pas l'absence de toute chaîne sensible possible ; les fichiers retenus ne
contiennent aucune configuration personnelle ni credential réel identifié.
