# Validation — SHINO // TV HOME v0.1

Rapport du premier build. Pour les nouvelles vérifications d'intégrité, les procédures Freebox et les alternatives d'émulation stable trouvées ensuite, voir [HARDWARE-VALIDATION.md](HARDWARE-VALIDATION.md). Le blocage rencontré avec l'image API 30 ne signifie pas qu'une licence Preview est indispensable à toute émulation TV.

**GO : livraison locale du projet et de l'APK debug. HOLD : installation et validation Freebox, ouverture LAN et permissions spéciales.**

Le build est réel. L'application Android n'a pas été lancée sur appareil ni émulateur ; sa navigation et son rendu natif ne sont pas déclarés validés. L'aperçu HTML est une composition statique distincte, visuellement inspectée dans le navigateur.

## Preuves exécutées

| Vérification | Résultat et portée |
| --- | --- |
| `:app:testDebugUnitTest` | 7 tests Kotlin, 0 échec : seuils quota, expiration/futur, IPv4 LAN, contrat, fiabilité UNVERIFIED, valeurs hors plage, déconnexion |
| Tests Python | 19 tests, 0 échec : filtrage privé, pourcentage restant, dates, JSONL partiel, absence source, IP, GPU absent, auth, méthodes HTTP, reconnexion, JSON Schema Draft 2020-12 et RFC3339, négociation TLS lente synthétique |
| `:app:assembleDebug` | BUILD SUCCESSFUL ; APK debug produit |
| `:app:lintDebug` | 0 erreur, 14 avertissements conservés : versions plus récentes, orientation TV, pinning TLS explicite, suggestions KTX |
| `:app:assembleDebugAndroidTest` | APK de tests D-pad compilé ; 2 tests instrumentés **non exécutés** |
| `apksigner verify --verbose --print-certs` | Signature debug valide |
| `zipalign -c -P 16 -v 4` | Alignement APK valide, y compris 16 Kio |
| Manifest compilé | Leanback présent ; HOME et service d'accessibilité absents ; Internet/état réseau ; Notification Listener facultatif protégé par permission système |
| Logo | SHA-256 de l'asset original identique à celui intégré |
| Historique Periscope réel | Lecture ponctuelle filtrée : deux fenêtres présentes, timestamp source présent, statut AVAILABLE au contrôle, activité UNVERIFIED ; pas d'historique privé exporté |
| Open-Meteo réel depuis Windows | Géocodage de Lézignan-Corbières trouvé ; température/code/horodatage présents ; ceci ne prouve pas le fonctionnement Android |
| ADB | 0 transport détecté via serveur loopback isolé ensuite arrêté ; aucune installation sur appareil |
| Émulateur | SDK retrouvé sans émulateur/image TV. Tentative d'installation locale arrêtée sur `android-sdk-preview-license` non acceptée ; aucune licence acceptée à la place du propriétaire |
| Aperçu HTML | Logo et composition 16:9 inspectés ; DEMO visible et avertissement « pas capture Android » présent |

Les tests HTTP sont exécutés uniquement sur `127.0.0.1` avec un secret synthétique. HTTPS LAN, vérification du certificat sur Android et reprise après coupure PC réelle restent à tester. La vérification d'alignement n'est pas une preuve de fonctionnement du runtime sur une machine à pages 16 Kio.

Consultation Advisor unique en lecture seule : GO remise locale, HOLD installation/LAN/permissions. Réserve identifiée sur une négociation TLS pouvant bloquer l'acceptation : corrigée ensuite par workers bornés (16 connexions maximum, timeout 5 secondes avant handshake, quatre snapshots simultanés). Test de régression avec négociation synthétique bloquée puis second client servi : PASS. Aucune nouvelle consultation et aucune ouverture LAN ; ce test ne remplace pas un test TLS complet sur TV.

## Identité de l'APK

- Package : `fr.shinobiwan.tvhome`
- Version : `0.1.0`, code `1`
- Min SDK : `26`, cible/compilation : `35`
- ABI présentes : arm64-v8a, armeabi-v7a, x86, x86_64
- Taille : **8 332 040 octets**
- SHA-256 : `696e67a54af7144f7092a6f01f1d810a29feea4608ec88091015230abae5ff0c`
- Logo original SHA-256 : `318a4703d2fcae0664b4262cf2411a6458c53ee777955c4e54f88db2996e41ba`

## État des modules

| Module | État | Ce qui reste |
| --- | --- | --- |
| Projet Kotlin / APK | Compilé, tests de modèle validés | Exécution native |
| Home Compose TV / D-pad | Implémenté, tests instrumentés compilés | Navigation/focus/overscan/retour réels |
| Raccourcis et préférences | API Leanback, ordre/masquage, DataStore implémentés | Inventaire et persistance sur Pop |
| Horloge / météo | Heure Paris implémentée, endpoints météo vérifiés Windows, cache/erreurs implémentés | Rendu/cache Android et ville via télécommande |
| Passerelle / quotas | Prototype fonctionnel testé loopback, lecture source réelle confirmée | Appairage HTTPS et réception Android réels |
| Activité Codex | UNVERIFIED uniquement | Pas de déduction depuis le quota |
| Now Playing TV | API, permission facultative, artwork borné, contrôles autorisés implémentés | Autorisation disponible sur Pop et lecteur réel |
| Now Playing PC | Prototype SMTC facultatif, repli indisponible | Projection `winsdk` absente ; lecteur réel et artwork Windows non implémenté |
| SHINO LINK | Adaptateur whitelist du serveur existant, modèle RAM Go testé | Serveur actif et fraîcheur des capteurs non vérifiés |
| DEMO | Mode séparé désactivé par défaut, données fictives identifiées | Contrôle visuel Android |
| HOME | Non déclaré, volontairement hors v0.1 | Nouvelle validation/autorisation si transition souhaitée |

## Fichiers de preuve

Sous `C:\Users\jerry\Documents\SHINO Television Android\artifacts\` :

- `SHINO-TV-HOME-v0.1-debug.apk`
- `SHINO-TV-HOME-v0.1-androidTest.apk`
- `apk-verification.json`, `signature.txt`, `alignment.txt`, `apk-metadata.txt`, `apk-manifest.txt`
- `build.log`, `gateway-tests.log`, `kotlin-tests.xml`, `lint.txt`
- `periscope-read-check.json`, `weather-provider-check.json`, `device-check.json`
- `preview-demo-browser.png` — capture de l'aperçu HTML, pas de l'APK

Les outils, caches, clés debug et artefacts binaires restent exclus de Git. Aucun remote/push/publication. Aucun fichier Periscope, LINK, SmallTV ou Projectivy modifié.

## Limites de la livraison

Le cahier des charges n'est pas entièrement validé sur cible : l'absence d'appareil et la licence d'image TV non acceptée bloquent la validation native. Les intégrations SMTC/LINK et TLS sur TV restent partielles. Les étapes restantes sont détaillées dans [DEVICE-CHECKLIST.md](DEVICE-CHECKLIST.md), [PAIRING.md](PAIRING.md) et [INSTALLATION.md](INSTALLATION.md).
