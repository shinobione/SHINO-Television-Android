# Passerelle et appairage local

La passerelle ne contacte pas Codex App Server. Elle lit en lecture seule l'historique des métriques déjà collectées par Periscope. Aucun prompt, fichier de session Codex ou jeton OpenAI n'est lu. Le champ `source` doit identifier Codex CLI, sinon l'échantillon est rejeté.

## État par défaut

Rien n'est démarré automatiquement. `gateway.py` se lie par défaut à `127.0.0.1:8766`, exige un secret local et n'expose que `GET /v1/snapshot`. Les écritures sont refusées. Les tests HTTP ont utilisé un jeton fictif et uniquement loopback ; ce jeton ne doit jamais servir à un appairage.

## Préparer volontairement un appairage LAN

**Non exécuté par Codex.** La génération de clés/secrets, l'ouverture LAN et une règle de pare-feu éventuelle constituent une opération distincte : appliquer la règle Advisor puis accord humain précis avant toute exécution assistée. Ne pas exposer le port par NAT, UPnP, VPN public ou tunnel.

Garder les fichiers dans `private/` ignoré par Git ou un dossier privé utilisateur avec ACL limitées. Préparer avec les outils cryptographiques de confiance de votre poste un certificat TLS et sa clé privée en PEM. Exemple OpenSSL à adapter à l'IPv4 privée du PC :

```powershell
New-Item -ItemType Directory -Force private
openssl req -x509 -newkey rsa:3072 -sha256 -nodes -days 365 `
  -keyout private/tvhome.key -out private/tvhome.pem -subj '/CN=SHINO-TVHOME' `
  -addext 'subjectAltName=IP:192.168.1.20'
# PowerShell 7 : secret distinct de tout compte OpenAI, non enregistré dans le dépôt.
$env:TVHOME_TOKEN = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
python companion/windows/gateway.py --bind 192.168.1.20 --allow-lan --cert private/tvhome.pem --key private/tvhome.key
```

La clé PEM de cet exemple n'est pas chiffrée : protéger son dossier avec les ACL du compte propriétaire et ne pas la synchroniser/committer. Aucun certificat n'est ajouté au magasin système. Le script n'ouvre aucune règle de pare-feu. Si une règle est nécessaire, la limiter à l'interface LAN privée, au programme/port choisi et à l'adresse de la Freebox, après autorisation.

La console affiche uniquement l'empreinte SHA-256 du certificat. Dans la TV, Paramètres → saisir IPv4, port 8766, empreinte comparée directement au PC et secret local. Le secret n'est pas affiché par la passerelle ; le propriétaire peut le consulter localement pour la saisie, sans le coller dans un chat. Sur TV il est chiffré AES-GCM par AndroidKeyStore ; aucune sauvegarde ou migration des données autorisée. Le certificat est vérifié avant l'envoi HTTP du secret. Une empreinte différente ou un certificat expiré interdit la connexion.

Oublier le PC efface les références locales et le cache. Une modification de certificat/secret nécessite un nouvel appairage. Pour révoquer : arrêter la passerelle, changer son secret, supprimer l'appairage TV. Pas de stockage du secret par le script, ni autostart.

## Modules optionnels

- `--history CHEMIN` : historique Periscope choisi explicitement, sinon `%LOCALAPPDATA%\SHINO-CODEX-MONITOR\history.jsonl`. Si l'enregistrement d'historique est désactivé, la TV affichera les données périmées puis indisponibles ; la passerelle ne change pas ce réglage.
- `--link-loopback` : lecture du serveur LINK existant `http://127.0.0.1:8765/metrics`, avec timeout et whitelist des champs. Ne démarre aucun capteur, ne contacte aucun SmallTV. À vérifier avec votre installation LINK active.
- `--smtc` : projection Python `winsdk.windows.media.control` compatible requise. Absente de Python 3.12 ici : état `UNAVAILABLE`. Aucune installation pip système effectuée. Artwork Windows non implémenté en v0.1. Aucun contrôle média PC à distance.

TV média : service Notification Listener facultatif. Les paramètres Android sont ouverts à la demande ; le propriétaire active ou refuse. Aucune notification n'est exportée. Si la permission n'existe pas sur Pop ou est refusée, l'écran reste utilisable et permet la source PC/état vide. Les commandes TV lecture/pause/suivant n'apparaissent que si les actions de session le permettent.

## Contrat et fraîcheur

Schéma : [snapshot.schema.json](../companion/windows/snapshot.schema.json). `schemaVersion=1`, `generatedAt` UTC pour la réponse, `capturedAt` UTC pour le quota source, pourcentage **restant** déjà calculé par Periscope. Fenêtres absentes = `null`, réinitialisations absentes = `null` ; aucun zéro inventé.

Les quotas sont périmés après 15 minutes ou dès leur reset. La TV grise également les quotas dès perte de connexion et conserve l'âge source. Horodatage futur de plus de deux minutes rejeté ; réponse passerelle de plus de deux minutes signalée périmée. L'activité demeure `UNVERIFIED`.

Poll TV : 60 secondes tant que l'écran est actif ; aucune requête périodique en arrière-plan. Météo : actualisation cache 30 minutes, fournisseur Open-Meteo et attribution affichés. Source auto média : TV en lecture, puis PC en lecture si réponse fraîche, puis TV disponible, puis PC disponible. Les positions affichées sont des positions observées, pas une lecture extrapolée présentée comme certaine.
