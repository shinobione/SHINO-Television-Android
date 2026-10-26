# SHINO // TV HOME Implementation Plan

Goal: produire le projet exécutable, APK debug, passerelle et preuves, sans toucher aux outils existants.

Architecture: un module Android léger + passerelle Python indépendante ; lecture des métriques existantes et contrat JSON v1. Stack: Kotlin, Compose TV, Coroutines, DataStore, Python standard library.

Spec: [DESIGN.md](DESIGN.md), mission utilisateur jointe.

## Contraintes et revue
- Aucun HOME, root, accessibilité, changement système, installation ou publication.
- Aucun secret OpenAI, prompt ou identifiant de session dans le contrat.
- Quotas absents, invalides, futurs ou périmés ne doivent jamais sembler actuels.
- Appareil sans média ou sans application : état vide et paramètres accessibles.
- Authentification, TLS et IP privée exigés sur LAN ; aucun redirect réseau.
- Focus conservé après application/settings, aucun écran uniquement tactile.

## Étapes (exécution native demandée)
- [x] Audit ciblé des outils/assets et sources existantes.
- [x] Tests puis passerelle : validation, filtrage, fraîcheur, HTTP, SMTC optionnel, télémétrie opt-in.
- [x] Projet Kotlin : modèles testés, préférences, réseau épinglé et cache, météo, applications, média.
- [x] Home Compose TV et réglages D-pad, focus et widgets implémentés ; validation native ci-dessous.
- [x] Compilation, tests unitaires/contrat, lint, signature, alignement et inspection APK.
- [x] Prévisualisation statique, recette physique rédigée ; rapport modules et procédures installation/retour.
- [x] Versionnement Git local explicite sans secret ni données privées.
- [ ] Exécution native / recette physique : aucun appareil ADB ; image TV arrêtée sur licence non acceptée.
- [ ] Appairage TLS LAN réel, SMTC avec lecteur Windows et télémétrie LINK active : accord/exécution distincts.
