# SHINO // TV HOME v0.1

Application Android TV personnelle pour Freebox Pop. Cahier des charges : pièce jointe utilisateur du 26 octobre 2026. Aucun remplacement de HOME en v0.1. Projectivy reste le secours.

Choix de réalisation autonomes : un module Gradle Kotlin, packages séparés par responsabilité, Compose for TV, ViewModel/Flow, DataStore. Trois widgets au maximum, rangée d'applications prioritaires, réglages par télécommande. L'alternative multi-module apporterait trop de configuration à ce MVP ; les vues Android classiques seraient moins adaptées à l'évolution des widgets.

La passerelle Python indépendante lit uniquement l'historique public de métriques de Periscope (pas les sessions Codex ni auth.json). Elle filtre les champs, conserve le temps source et rend UNVERIFIED pour l'activité. Les quotas d'historique sont déjà des pourcentages restants. Pas de deuxième collecteur App Server. L'historique existant écrit au maximum toutes les cinq minutes et peut être désactivé.

Liaison : JSON v1, GET uniquement, HTTPS pour LAN, IPv4 RFC1918, secret d'appairage saisi explicitement et certificat SHA-256 épinglé sur TV. Configuration secrète hors Git, cache TV sans secret, token chiffré via AndroidKeyStore. Le serveur n'est jamais démarré sur le LAN par le build. Aucun pare-feu modifié.

Météo Open-Meteo, géocodage explicite de la ville, cache 30 minutes. Média TV : NotificationListener facultatif, contrôle des actions autorisées, repli sans permission. Média PC : adaptateur SMTC facultatif, indisponible si dépendance absente. Télémétrie : adaptateur opt-in du serveur loopback SHINO LINK existant, aucune collecte matérielle nouvelle.

Validation : tests des quotas, âges, contrat, authentification, absence de fuite, méthodes HTTP, reconnexion et cache ; compilation APK debug et lint. Tests télécommande instrumentés si un émulateur TV existe ; sinon recette précise et statut NON TESTÉ. Ne jamais déduire la compatibilité physique des seuls tests source.

Limites : Android exact et disponibilité Notification Listener sur Pop inconnus avant essai physique ; aucune installation, activation de permission spéciale, publication ou transition HOME automatique.
