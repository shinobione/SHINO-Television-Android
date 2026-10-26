# Audit ciblé — 26 octobre 2026

- Workspace neuf, initialement uniquement `logo SHINOBIWAN.png`. Logo officiel repris octet pour octet ; aucune génération d'image.
- Instructions utilisateur quota-first : exécution principale seule, pas de publication/installation système. Aucun AGENTS.md supplémentaire trouvé dans les ancêtres accessibles.
- Profil réel `C:\Users\jerry\.codex\BUILD.config.toml` : `gpt-6.1-sol`, effort `medium`. La commande collée ne modifie pas la session de l'application. Configuration globale non modifiée.
- Outils réutilisés de KiloWaze : JDK Temurin 17.0.20.1+1 et Gradle 8.11.1 sur G:, SDK 35 / Build Tools 35.0.0 copiés dans `.local-build/`. Cache Maven isolé. Aucun PATH système changé.
- Sources Periscope : `C:\Users\jerry\Documents\SHINO-CODEX-MONITOR\src\LocalStore.cs`, `Models.cs`, `CodexReader.cs`. Branche trouvée dans `.git/HEAD` : `codex/obsidian-visual-parity`, référence locale `369aa68294e05e4dd4e6af7d3be17bd26d6350a2`. L'outil Git était bloqué hors workspace ; aucun changement dans ce dépôt.
- Installation Periscope trouvée sous `%LOCALAPPDATA%\Programs\SHINO Codex Monitor`. Ceci ne prouve pas l'identité du processus actif. Historique `%LOCALAPPDATA%\SHINO-CODEX-MONITOR\history.jsonl` présent et daté du jour. Aucun contenu brut ni session Codex collecté dans les livrables.
- Source App Server réelle dans `CodexReader.cs` : `account/rateLimits/read`. Non appelée par ce projet : la passerelle réutilise l'historique Periscope et n'ouvre aucun fichier d'authentification Codex. `LocalStore.Add` espace les écritures de cinq minutes minimum ; la passerelle/TV ne doivent pas promettre une mesure nouvelle chaque minute.
- LINK : sources `C:\Users\jerry\SHINO-LINK\companion\metrics_server.py`. Serveur historique loopback `127.0.0.1:8765/metrics`, champs CPU/GPU/RAM vérifiés dans le code. Installations B52 et V1.0.1 retrouvées. Version active et compatibilité runtime non prouvées ; adaptateur facultatif sans démarrage du collecteur.
- Média Windows : API SMTC documentée, projection Python `winsdk` absente dans Python 3.12 local. Adaptateur optionnel conservé, jamais présenté comme validé.
- Média TV : `getActiveSessions` nécessite permission système de contrôle média ou Notification Listener autorisé. Service facultatif déclaré, aucune permission accordée automatiquement. Disponibilité du réglage sur Pop non prouvée.
- API/Android exact de la Freebox : inconnu sans essai autorisé. Min SDK du projet = 26. Aucun scan LAN ni contact SmallTV/Freebox.

## Sources officielles consultées
- [Application TV et LEANBACK_LAUNCHER](https://developer.android.com/training/tv/get-started/create)
- [Compose TV et versions](https://developer.android.com/jetpack/androidx/releases/tv)
- [AGP 8.9 : Java 17, Gradle 8.11.1, SDK 35](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [MediaSessionManager et permissions](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [SMTC Windows](https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssessionmanager)
- [Open-Meteo météo](https://open-meteo.com/en/docs)
- [OpenAI Docs : option --profile](https://learn.chatgpt.com/docs/cli/reference)

Les dépendances sont figées sur un ensemble compatible SDK 35, pas sur toutes les dernières versions disponibles. La compilation/lint constitue la preuve de compatibilité de build ; pas une preuve Freebox.
