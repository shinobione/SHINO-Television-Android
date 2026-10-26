# MISSION — BUILD SHINO // TV HOME v0.1
## Android TV Launcher + SHINOBIWAN Dashboard

Tu es mon ingénieur Android senior, architecte logiciel et designer TV UX.

**MISSION : concevoir, développer, compiler et tester SHINO // TV HOME, un launcher Android TV personnel, modulaire et premium, destiné à mon Player Freebox Pop.**

Ne te limite pas à une étude ou une maquette. Je veux un véritable projet Android exécutable avec APK debug, tests, documentation et instructions d'installation.

Travaille de manière autonome, en réutilisant les ressources existantes. Ne prétends jamais qu'une fonctionnalité est opérationnelle si elle n'a pas été vérifiée.

---

## 1. MATÉRIEL ET ENVIRONNEMENT

Matériel cible :

- Freebox Player Pop, référence F-BG02A0-BN
- Android TV, version exacte à détecter
- Télévision Philips 40PFS6000/12, Full HD
- Télécommande Freebox avec navigation directionnelle
- PC Windows sur le même réseau local
- Codex CLI et projets SHINO existants

Projectivy Launcher est déjà installé et fonctionne.

L'interception de la touche Home par Projectivy via son service d'accessibilité a été validée sur le véritable appareil.

**Contrainte absolue : conserver Projectivy fonctionnel comme solution de secours.**

Ne pas désactiver le launcher Free, modifier le firmware, utiliser root ou installer un service d'accessibilité intrusif sans autorisation explicite.

Ce projet est distinct du firmware SHINO // TV destiné au GeekMagic SmallTV-Ultra 240×240. Ne pas écraser ni modifier ce dernier.

---

## 2. IDENTITÉ GRAPHIQUE

Nom du produit : **SHINO // TV HOME**

Logo : utiliser exclusivement mon **logo SHINOBIWAN officiel existant**.

Chercher les ressources graphiques disponibles dans les projets accessibles et les fichiers du workspace.

- Ne jamais inventer de logo SHINOBIWAN.
- Ne jamais remplacer le logo par une création IA approximative.
- Conserver les proportions, les couleurs et l'identité originale.
- Prévoir son intégration dans l'en-tête, l'icône de l'application et éventuellement une animation de démarrage discrète.
- Si l'original est introuvable, laisser un emplacement identifiable et documenter l'asset manquant. Ne pas bloquer le développement.

Direction artistique :

- Dark cinematic / premium / minimal
- Noir profond, graphite, gris anthracite
- Accents violet et magenta
- Typographie moderne et très lisible
- Animations fluides et discrètes
- Effets de focus élégants
- Aucune publicité
- Aucun carrousel commercial

Éviter absolument le look générique de dashboard administratif, la surcharge de néons, les cartes minuscules et les éléments illisibles.

L'interface doit être conçue pour un écran 16:9 Full HD consulté depuis un canapé.

---

## 3. HOME SCREEN — ARCHITECTURE VISUELLE

Proposer une composition originale et cohérente :

**Zone supérieure**

- Logo SHINOBIWAN
- Heure très lisible
- Date
- Température et conditions météo
- Indicateur discret de connexion au PC

**Zone principale**

Raccourcis configurables vers les applications installées :

- Free TV
- Netflix
- Disney+
- Prime Video
- YouTube
- Autres applications choisies par l'utilisateur

Utiliser les véritables applications installées, détectées via les API Android compatibles.

Les raccourcis doivent pouvoir être réordonnés, masqués et configurés.

**Zone widgets**

Prévoir une composition modulaire, avec un à trois widgets visibles simultanément selon leur taille :

- Codex Periscope
- Now Playing
- Horloge / météo
- SHINO // LINK / télémétrie PC
- Autres modules futurs

Les widgets ne doivent pas empêcher l'accès rapide aux applications de streaming.

Créer une abstraction commune permettant à terme d'ajouter des widgets sans réécrire l'écran d'accueil.

Pour v0.1, privilégier un layout stable, configurable et utilisable à la télécommande plutôt qu'un éditeur drag-and-drop complexe.

---

## 4. WIDGET CODEX PERISCOPE

Je dispose déjà d'un outil Windows natif de monitoring des quotas Codex.

Rechercher et analyser prioritairement les projets accessibles correspondant à :

- SHINO-CODEX-MONITOR
- CODEX PERISCOPE
- Les sources C#/.NET et éventuels historiques JSON/JSONL
- Les interfaces existantes avec Codex CLI

Un emplacement historique possible est :

`C:\Users\jerry\Documents\SHINO-CODEX-MONITOR`

Ne pas supposer que ce chemin ou les anciennes versions correspondent nécessairement à la version active. Vérifier.

**Objectif : réutiliser l'existant plutôt que développer un deuxième système de mesure des quotas.**

Le widget TV doit afficher :

- Quota Codex 5 h restant en %
- Quota Codex hebdomadaire restant en %
- Progression des deux quotas
- Heure de réinitialisation 5 h
- Date et heure de réinitialisation hebdomadaire
- Dernière synchronisation
- État de connexion
- Activité Codex si réellement observable

Reprendre les seuils de couleur utilisés dans Codex Periscope :

- 76–100 % : violet
- 51–75 % : turquoise
- 26–50 % : jaune doré
- 11–25 % : orange
- 0–10 % : rouge

Afficher explicitement le **quota restant**, et non le pourcentage utilisé.

Étudier la récupération des informations par le protocole Codex App Server, notamment `account/rateLimits/read`, directement ou via le monitoring existant.

Ne jamais créer de fausse API OpenAI, de quotas simulés présentés comme réels ou de calculs basés sur des données non vérifiées.

Ne pas confondre activité observée, activité supposée et exécution confirmée. Conserver les statuts de fiabilité existants, notamment UNVERIFIED lorsqu'ils sont pertinents.

### Liaison Windows → Freebox

Concevoir un compagnon local minimal, compatible avec l'outil Windows existant.

Solution préférée :

1. L'outil Windows collecte les quotas.
2. Un module passerelle expose uniquement les données nécessaires.
3. L'application Android TV récupère les données via le réseau local.
4. Les données sont horodatées et mises en cache.

Prévoir un contrat JSON versionné pour les métriques, états et horodatages.

Sécurité :

- Aucune clé API ni aucun jeton de session OpenAI transmis à la TV.
- Aucun accès direct depuis la TV aux fichiers sensibles Codex.
- API en lecture seule.
- Appairage explicite et authentification locale.
- Accès réseau limité au LAN.
- Pas d'exposition à Internet.
- Pas de commandes arbitraires exécutables à distance.

Actualisation des quotas environ toutes les 60 secondes, ou selon le fonctionnement réel de la source.

PC éteint ou inaccessible : afficher « PC hors ligne » et l'âge des dernières données, sans les présenter comme actuelles.

Ne jamais modifier ou casser Codex Periscope.

---

## 5. WIDGET NOW PLAYING

Créer un widget musique affichant :

- Pochette d'album
- Titre
- Artiste
- Lecture ou pause
- Barre de progression
- Durée et position lorsque disponibles
- Source de lecture

Prévoir deux sources distinctes.

### Source A — Android TV

Étudier MediaSessionManager et les API média Android.

Vérifier les autorisations requises, y compris le Notification Listener si nécessaire et effectivement disponible sur la Freebox Pop.

Ne jamais supposer qu'une application de streaming expose toutes ses métadonnées.

Utiliser les contrôles lecture/pause/suivant uniquement si la session multimédia autorise ces actions.

### Source B — PC Windows

Étudier GlobalSystemMediaTransportControlsSessionManager (SMTC) de Windows.

Exploiter éventuellement le compagnon local pour transmettre :

- Titre
- Artiste
- Pochette
- État lecture/pause
- Progression si disponible
- Nom de l'application source

Tester notamment la lecture provenant d'applications musicales Windows compatibles.

Séparer clairement les sources « TV » et « PC ».

Permettre à l'utilisateur de choisir une source préférée et prévoir un mode automatique fiable.

Si aucune lecture n'est détectée, afficher un état vide élégant plutôt qu'une fausse pochette ou un titre inventé.

Mettre en place une solution de repli sans permission spéciale si le monitoring média TV n'est pas disponible.

---

## 6. AUTRES WIDGETS

### Météo et horloge

- Fuseau Europe/Paris
- Grande horloge HH:mm
- Date localisée en français
- Ville configurable, Lézignan-Corbières par défaut
- Température actuelle
- Icône des conditions météo
- Dernière actualisation
- Gestion du mode hors ligne

Choisir une source météorologique documentée et légitime, avec cache et gestion des erreurs.

### SHINO // LINK

Étudier le compagnon Windows existant SHINO // LINK avant de développer quoi que ce soit.

Prévoir un widget optionnel :

- Statut PC connecté / hors ligne
- CPU en %
- GPU en %
- RAM utilisée en Go, jamais sous forme de pourcentage
- Température GPU si disponible

Réutiliser les données existantes. Ne pas dupliquer inutilement le monitoring matériel.

### Évolutivité

Préparer l'architecture pour de futurs modules :

- Calendrier
- État réseau
- Notifications importantes
- Widgets personnalisés
- Raccourcis domotiques éventuels

Ces modules restent hors périmètre fonctionnel de la v0.1.

---

## 7. TECHNOLOGIES

Priorité :

- Kotlin
- Android TV SDK
- Jetpack Compose for TV
- Gradle
- Kotlin Coroutines / Flow
- ViewModel
- DataStore
- Client HTTP léger pour la passerelle
- Stockage local uniquement pour les préférences et le cache

Utiliser des versions de dépendances compatibles et vérifiées.

Architecture recommandée :

- `app/`
- `core/designsystem/`
- `core/models/`
- `core/network/`
- `data/apps/`
- `data/weather/`
- `data/media/`
- `data/codex/`
- `data/telemetry/`
- `feature/home/`
- `feature/widgets/`
- `feature/settings/`
- `companion/windows/` si nécessaire

Adapter la structure si une organisation plus simple est techniquement préférable.

L'architecture doit rester légère, compte tenu des ressources du Player Pop.

---

## 8. COMPATIBILITÉ TÉLÉCOMMANDE

C'est une exigence majeure.

Implémenter et tester :

- Navigation haut / bas / gauche / droite
- Validation OK
- Retour Back
- Indicateur de focus clairement visible
- Conservation du focus après retour d'une application
- Défilement automatique vers l'élément sélectionné
- Aucun piège de focus
- Aucun contrôle nécessitant une souris

L'ensemble de l'interface doit rester utilisable sans clavier ni écran tactile.

Garantir la lisibilité à distance et respecter les marges de sécurité d'affichage TV.

---

## 9. SÉCURITÉ DU LAUNCHER

**Étape initiale : application Android TV standard.**

Créer une activité visible dans le launcher Android TV grâce à `LEANBACK_LAUNCHER`.

Ne pas confondre une activité TV classique avec une véritable application système HOME.

Ne pas commencer par désactiver Projectivy ou le launcher Free.

Après validation complète de l'application :

- Étudier la déclaration HOME si adaptée.
- Vérifier le comportement réel du Player Pop.
- Documenter une procédure réversible de transition.
- Préserver l'accès aux paramètres Android.
- Préserver l'accès à Free TV et aux applications existantes.
- Préparer une procédure de restauration.

Ne pas activer automatiquement des permissions d'accessibilité ni désactiver des packages système.

Toute intervention risquée exige mon autorisation préalable.

---

## 10. PHASES DE DÉVELOPPEMENT

### Phase A — Audit

- Examiner les ressources disponibles.
- Rechercher mon logo SHINOBIWAN original.
- Identifier les projets Windows existants.
- Identifier les possibilités de réutilisation de Codex Periscope et SHINO // LINK.
- Vérifier SDK, Gradle, Java et outils ADB.
- Établir les limites techniques réelles.

### Phase B — MVP Android TV

Créer un APK avec :

- Design finalisable
- Logo original s'il est disponible
- Horloge
- Météo ou état indisponible explicite
- Véritables raccourcis d'applications
- Navigation télécommande
- Paramètres simples
- Widgets Codex et Now Playing avec données simulées **clairement signalées comme DEMO**, uniquement pour les tests visuels

### Phase C — Intégrations réelles

- Passerelle Windows
- Données quota Codex authentiques
- Now Playing Android TV si accessible
- Now Playing Windows si accessible
- Télémétrie optionnelle
- États de connexion, cache et déconnexion

### Phase D — Validation

- Compilation complète
- Tests unitaires
- Tests de navigation et focus
- Tests hors ligne
- Tests avec données absentes ou périmées
- Validation du contrat JSON
- Tests de reconnexion
- Captures d'écran ou prévisualisations
- APK debug installable

Si aucun appareil ADB n'est disponible, réaliser les validations possibles sur émulateur et documenter précisément les tests qui restent à effectuer sur la Freebox physique.

---

## 11. LIVRABLES OBLIGATOIRES

Produire :

1. Projet Android TV complet.
2. Code source versionné.
3. APK debug compilé et signé pour installation de développement.
4. Passerelle Windows fonctionnelle ou prototype minimal documenté, selon les sources réellement accessibles.
5. Documentation d'installation.
6. Documentation de configuration et d'appairage.
7. Tableau indiquant les modules opérationnels, partiels et bloqués.
8. Procédure de retour vers Projectivy.
9. Rapport de compilation et des tests exécutés.
10. Liste des améliorations proposées pour v0.2.

Donner les chemins exacts des livrables.

Ne pas annoncer BUILD SUCCESSFUL sans compilation effective.

Ne pas afficher de métriques factices sans mention DEMO.

Ne pas publier sur GitHub, remplacer un projet existant ou déployer sur la Freebox sans instruction explicite.

---

## 12. RÈGLES DE TRAVAIL

- Exécuter le travail, ne pas répondre uniquement par une proposition.
- Ne pas inventer d'API, de permissions ou de compatibilités.
- Consulter la documentation officielle pertinente.
- Préférer une solution simple et robuste à une dépendance complexe.
- Prioriser la compatibilité Freebox Pop.
- Garder le projet indépendant du firmware SmallTV.
- Minimiser la consommation CPU/RAM et les rafraîchissements inutiles.
- Préserver les outils Windows existants.
- Ne jamais contourner les DRM des applications de streaming.
- Éviter les changements système irréversibles.
- Continuer de manière autonome avec des choix raisonnables lorsque les informations disponibles suffisent.

**COMMENCE MAINTENANT.**

Effectue l'audit rapide, puis construis SHINO // TV HOME v0.1.

Je veux une application fonctionnelle, pas simplement un concept visuel.