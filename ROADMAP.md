# SHINO // TV HOME — Roadmap

**Mise à jour : 1er novembre 2026**  
**Produit :** dashboard Android TV pour Freebox Pop, pas un launcher alternatif.

> **Attention — versions distinctes :** cette roadmap décrit l'état de l'installation locale validée le 01/11/2026, **pas** l'état des sources actuellement publiées sur la branche GitHub `main`. Le README distant décrit encore la v2.0.1. Ne pas supposer que les correctifs locaux 2.1.2 / RELAY 0.5.7 sont déjà synchronisés ici. Toute synchronisation devra comparer les bases sans écraser les sources locales récentes.

## 1. Référence fonctionnelle actuelle — VALIDÉE EN LIVE

| Composant | Version / état | Fonction |
| --- | --- | --- |
| Android TV | **2.1.2 / code10**, installée | Dashboard natif Compose, deux pages, navigation D-pad |
| SHINO HUB | **LIVE 0.5.1**, actif | Réception, validation, distribution des domaines |
| SHINO RELAY | **0.5.7**, actif à la validation | Publication Windows transitoire vers le HUB |
| Superviseur Windows | **1.0**, validé | Instance unique, redémarrage automatique après redémarrage réel de Windows |
| SHINO // LINK | Non démarré comme application séparée | Une copie de son collecteur est embarquée dans RELAY |
| CODEX PERISCOPE | Quotas publiés via RELAY | **Missions et états de travail non encore certifiés ni affichés** |

**Fonctions validées physiquement sur la Freebox :**
- **Monitoring PC :** CPU, GPU, RAM en **Go**, température GPU ; mesures réelles et fraîcheur par domaine.
- **CODEX PERISCOPE :** quotas 5 heures et hebdomadaire, jauges et réinitialisations.
- **NOW PLAYING :** pochette, titre, artiste, lecture/pause et progression réelles.
- **SHINO // NETWORK :** RX/TX réels du **PC Windows** en Mb/s, interface, IP locale, vitesse de liaison et historique ; pas un speedtest, pas le réseau de la Freebox.
- **ACTUALITÉS :** trois sources RSS réelles, cache et rafraîchissement (The Verge AI, Numerama, Jeuxvideo.com).
- **MÉTÉO :** données actualisées, indication de fraîcheur.
- **Exploitation :** redémarrage réel Windows, une seule instance RELAY, nouveaux ACK HTTP 201 et trois domaines CURRENT validés.

**Réserve :** l'affichage d'une mission Codex active ou terminée **n'est pas livré**. Le statut `UNVERIFIED` ne doit jamais devenir une activité fictive.

## 2. Priorité P1 — CODEX PERISCOPE : Missions & animations [À FAIRE]

**Objectif produit :** transformer la zone « Missions » de la carte Codex en vue **vivante et fidèle** des travaux réellement observés, sans dégrader les jauges de quotas.

### P1.1 — Source d'activité fiable
- Étudier **en lecture seule et de façon ciblée** les informations réellement disponibles dans le PERISCOPE Windows / les données Codex déjà utilisées.
- Définir l'unité « mission » comme un travail ou une session identifiable ; **pas chaque message** Codex.
- N'exposer que les états **prouvables** : mission identifiée, active, terminée, éventuellement en échec/interrompue **si la source le certifie**. Conserver l'heure de dernière confirmation et une identité stable lorsque disponibles.
- Si aucune donnée fiable n'existe : conserver **« Activité non vérifiée »**, sans simuler un travail en cours.

### P1.2 — Carte TV & radar animé
- Garder les jauges **5 h** et **hebdomadaire** et leurs dates/heures de reset.
- Afficher le **nom court** et l'**état vérifié** des missions, avec priorité à la lisibilité sur TV 1080p.
- **Radar / pulsations / glow** animés pour les missions réellement actives ; animations arrêtées ou changées selon l'état certifié.
- Plusieurs missions simultanées : **représentations distinctes**, identifiables individuellement, avec codes visuels différenciés et sans fausse simultanéité.
- États `UNVERIFIED`, données absentes ou périmées : affichage neutre et **aucune animation trompeuse**.
- Navigation D-pad et performances correctes ; animations légères, sans monopoliser l'attention.

### P1.3 — Transport minimal et validation
- Réutiliser autant que possible **PERISCOPE → publication transitoire RELAY → HUB → Android TV**. N'étendre les contrats que si les données réellement vérifiées l'exigent.
- Ne pas démarrer SHINO // LINK comme second producteur ; ne pas ouvrir un nouveau service.
- Tester : 0 mission, 1 mission, plusieurs, fin de mission, activité inconnue, absence / péremption, passage d'une page TV à l'autre, restauration après redémarrage Windows.
- Valider sur **vraie Freebox**, sans prendre un aperçu synthétique pour une preuve LIVE.
- Aucun changement de l'installation stable sans qualification et autorisation ciblée.

**Critère de livraison P1 :** une mission Codex effectivement observée produit la bonne indication visuelle et une animation associée ; sa fin ou sa disparition entraîne la transition correcte ; quotas, PC, musique, réseau, météo et RSS restent fonctionnels.

## 3. Priorité P2 — Finitions du dashboard [APRÈS P1]

- Affiner **lisibilité, proportions et espaces** de chaque carte sur 1920 × 1080 en conservant l'identité SHINOBIWAN et l'horloge lisible.
- Soigner les états absents, en cache, périmés et déconnectés ; jamais de nombres ou d'articles fictifs en LIVE.
- Réduire le bruit visuel et conserver une navigation D-pad simple entre **Monitoring** et **Réseau / Actualités**.
- Évaluer les optimisations d'affichage seulement après retour d'usage réel, sans refonte gratuite.

## 4. Priorité P3 — Producteurs natifs [PLUS TARD, MISSION DISTINCTE]

- Architecture **cible** : **LINK** publie PC / réseau / musique ; **PERISCOPE** publie quotas et missions vérifiées ; **HUB** valide et distribue ; **TV** lit exclusivement le HUB.
- Aujourd'hui, **RELAY 0.5.7** reste le publicateur intermédiaire opérationnel. Ne pas confondre réutilisation du code LINK et application LINK active.
- Migration progressive, **un domaine/producteur à la fois**, avec parité des données, absence de double publication, préservation des identités et watermarks, possibilité de repli qualifiée.
- **Pas un prérequis à P1** : on évite de transformer l'animation des missions en nouvelle refonte d'infrastructure.

## 5. Décisions conservées / hors périmètre

- SHINO // TV est un **dashboard**, **pas** un remplaçant du launcher Android TV ; la rangée Netflix/YouTube/etc. a été retirée volontairement.
- Pas de commandes musicales fictives, pas de faux états Codex, pas de latence réseau annoncée sans vraie mesure.
- RAM PC affichée en **Go** ; trafic en **Mb/s**, distinct de la vitesse de liaison.
- Aucun nouveau HUB, serveur, système de récupération ou chantier de sécurité ajouté pour une simple évolution UI.
- Maintenir la base 2.1.2 + HUB 0.5.1 + RELAY 0.5.7 + superviseur 1.0 tant qu'un changement n'est pas justifié et validé.

---

### Prochaine mission prioritaire

**P1 — CODEX PERISCOPE Missions & animations** : commencer par confirmer **quelles missions sont réellement observables**, puis afficher et animer **uniquement** ces informations sur la carte Codex. Ne pas réouvrir le chantier Réseau / Actualités déjà livré.
