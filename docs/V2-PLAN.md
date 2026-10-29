# TV HOME v2 — plan d'intégration

Demande : conserver l'application native existante, son package et son historique,
ajouter le lecteur HUB v2 puis livrer un APK debug sans installation physique.

Architecture : conserver Compose/Leanback, les widgets, la météo et le lecteur v1.
Ajouter un parseur v2 strict et des métadonnées de fraîcheur indépendantes ;
le transport partage TLS, authentification et limites avec le téléchargement JPEG.
Le mode HUB v2 utilise une CA explicite importée dans l'application, le contrôle
SAN standard et le pin DER. Aucun secret ni certificat personnel dans Git.

1. Tests JVM : contrat réel copié de RELAY, domaines facultatifs, capteurs typés,
   échéances 6/10/900 secondes, resets, données absentes, timeline nullable,
   référence JPEG, TEST/DEMO et cache déconnecté. Observer l'échec avant ajout.
2. Implémenter le parseur et les états ; conserver parseSnapshot v1.
3. Ajouter le transport HUB, erreurs HTTP distinctes, cache v2 lié à l'appairage,
   JPEG borné et vérifié SHA-256, réglages de protocole et import CA public.
4. Adapter les cartes sans refonte, afficher température et provenance/âge,
   rafraîchir toutes les 3 secondes, bloquer les répétitions sur 401/403.
5. Tests JVM/Compose, lint, APK debug et APK instrumentation compilés ; vérifier
   signature/alignement/package. Aucun ADB ni test physique implicite.
6. Revue des fichiers et historique avant publication publique, consultation
   Advisor ciblée pour cette publication, push main sans force ni réécriture.

Points de revue : futures dates, TTL exact, ancien cache après nouvel appairage,
CA/SAN/pin erronés, absence de pochette et changement de piste, erreur auth,
données synthétiques identifiées, préservation des réglages v1.

Limite protocolaire : le HUB expose uniquement la lecture de l'état média ;
aucune commande PC play/pause n'est définie. Les commandes TV existantes restent.
