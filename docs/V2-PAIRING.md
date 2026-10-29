# Appairage SHINO HUB v2

Depuis l'application TV : Réglages → SHINO HUB v2.

1. Utiliser l'adresse privée du HUB, par défaut `192.168.1.195`, port `8767`.
2. Importer le certificat **public de la CA** qui signe le HUB, au format PEM,
   depuis un fichier accessible au sélecteur Android (USB possible). Le champ
   CA permet aussi une saisie PEM. Maximum 16 Ko / quatre certificats CA.
   Ne jamais transférer de clé privée. La CA reste dans le stockage de l'app.
3. Saisir l'empreinte SHA-256 du certificat serveur DER, vérifiée directement
   auprès du propriétaire du HUB. Les séparateurs deux-points sont normalisés.
4. Saisir le jeton existant de l'identité **TV_HOME avec droit de lecture**.
   Format réel : `[A-Za-z0-9_-]{16,128}`. Ne pas employer une identité de
   publication LINK/PERISCOPE ou un jeton OpenAI. Le jeton est chiffré avec
   AndroidKeyStore/AES-GCM comme dans l'application précédente.
5. Enregistrer. Le lecteur ne fait que des GET sur les routes snapshot/artwork.

La connexion HUB exige la chaîne CA, le nom/IP SAN du certificat et son pin.
Aucun contournement du nom ni confiance globale envers les CA utilisateur.
Le contexte TLS v1.3 doit encore être qualifié sur la Pop réelle.

Les réponses 401/403 demandent une correction d'appairage et suspendent les
rafraîchissements automatiques pour cet appairage. Même suspension pour une
configuration incomplète ou un échec TLS. « Actualiser le PC » autorise une
nouvelle tentative après correction. Le 503 `NO_DATA` signifie HUB connecté
mais vide, avec effacement de l'ancien snapshot visible. Les autres erreurs
503/réseau conservent un cache explicitement déconnecté. Une requête est bornée
à cinq secondes de connexion et cinq secondes de lecture, une seule à la fois.
Le polling HUB attend trois secondes entre opportunités de rafraîchissement.

Le cache v2 est distinct du v1 et lié au protocole, hôte, port, CA, pin et
sélection du jeton chiffré. Changer l'appairage ou oublier le PC efface les
caches ; les timestamps source ne sont jamais renouvelés par un téléchargement.

Pour revenir au compagnon historique : choisir Passerelle v1 puis renseigner
son propre appairage. Les paramètres ville, applications et widgets subsistent.

Ni récupération automatique de secrets, ni création d'identité côté HUB, ni
modification du serveur n'est effectuée. L'installation de l'APK et la recette
matérielle restent des actions manuelles distinctes.
