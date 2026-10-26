# Recette appareil — à exécuter avec autorisation

## Émulateur TV et Pop : aucune validation physique implicite

- [ ] Enregistrer modèle, Android, API, résolution et densité.
- [ ] Ouvrir SHINO depuis Projectivy puis utiliser Home pour retrouver Projectivy.
- [ ] Vérifier logo, marges, horloge/date Europe/Paris et lisibilité depuis le canapé.
- [ ] Naviguer les quatre directions et OK dans toute la rangée ; tester défilement avec plus de six applications.
- [ ] Lancer Free TV, Netflix, Disney+, Prime Video, YouTube seulement si présents. Vérifier retour et focus du même raccourci.
- [ ] Atteindre Paramètres depuis chaque zone ; Back revient à l'accueil ; Back accueil retrouve le launcher précédent.
- [ ] Réordonner/masquer/réafficher, redémarrer SHINO et vérifier persistance.
- [ ] Choisir chaque combinaison de 1–3 widgets ; aucune zone ne déborde ni ne piège le focus.
- [ ] Saisir ville/IPv4/port/empreinte/secret avec le clavier Android et seulement la télécommande.
- [ ] Tester la météo en ligne, ville invalide, coupure Internet et cache âgé.
- [ ] Sans appairage : PC hors ligne, quotas absents, aucune métrique fictive.
- [ ] Avec passerelle autorisée : comparer quotas restants et resets au Periscope du PC ; vérifier timestamp de collecte.
- [ ] Couper PC : données grisées et âge visible ; reconnecter et attendre un poll.
- [ ] Mauvais token, mauvaise empreinte, certificat expiré, réponse trop grosse, version JSON inconnue : pas de données actuelles ni crash.
- [ ] Faire vieillir quota au-delà de 15 min ou reset : statut périmé.
- [ ] Média TV permission refusée, absente, puis autorisée volontairement : état vide fiable ; aucune notification personnelle affichée.
- [ ] Lecteur TV en lecture/pause puis arrêt : titre/artiste/pochette/action autorisée ; valider aucun contrôle non autorisé.
- [ ] Source PC/TV/AUTO et déconnexion : aucune ancienne lecture présentée comme actuelle.
- [ ] LINK facultatif : RAM en Go, GPU absent = indisponible ; jamais convertir l'absence en 0 % mesuré.
- [ ] DEMO activé puis désactivé : chaque donnée fictive identifiée, retour aux données réelles/absentes.
- [ ] Accès Paramètres Android et Projectivy toujours possible ; aucun package désactivé.

Tests instrumentés fournis : `app/src/androidTest/.../RemoteNavigationTest.kt`. Compilation distincte de leur exécution. N'exécuter `connectedDebugAndroidTest` qu'après sélection explicite d'un appareil de test autorisé ; cette tâche installe l'application et l'APK de test sur cet appareil.
