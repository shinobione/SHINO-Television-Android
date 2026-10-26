# Validation Freebox Pop — procédure préparée, installation non autorisée

État au 26 octobre 2026 : **GO préparation ; HOLD connexion ADB nouvelle, installation, lancement et tests physiques tant que l'accord correspondant n'a pas été donné.** Aucun appareil contacté pendant cet audit. Aucune image d'émulateur installée et aucune licence acceptée.

## 1. APK conservé et revérifié

Fichier : `C:\Users\jerry\Documents\SHINO Television Android\artifacts\SHINO-TV-HOME-v0.1-debug.apk`.

- Taille : 8 332 040 octets.
- SHA-256 : `696e67a54af7144f7092a6f01f1d810a29feea4608ec88091015230abae5ff0c`.
- Signature et alignement : revérifiés sur ce fichier livré, sans reconstruire ni le remplacer.
- Package : `fr.shinobiwan.tvhome`, version 0.1.0/code 1, API 26 minimum, compile/target 35.
- Manifeste compilé : Leanback présent, HOME absent, aucun receiver de boot. L'application ne remplace pas Projectivy et ne demande pas à devenir l'accueil.
- ADB local déjà présent : 37.0.1. Aucune installation ADB système nécessaire.

Preuves locales : `artifacts/hardware-validation/apk-integrity.json`, `signature.txt`, `alignment.txt`, `manifest.txt`. Le rapport du premier build demeure historique dans `docs/VALIDATION.md` ; ses tests ne sont pas présentés comme réexécutés aujourd'hui.

## 2. Première action sur la télévision : relever, sans activer

Avec la télécommande, relever dans les Paramètres du Player (les intitulés dépendent de son système) :

1. À propos : modèle, version Android TV et numéro de build. API minimum nécessaire : 26.
2. Réseau et Internet : IPv4 du **Player**, pas celle du Server Freebox.
3. Affichage : mode HDMI sélectionné 1920×1080 et fréquence ; noter tout overscan/zoom de la TV Philips sans le changer pendant la première mesure.
4. Si les options développeur sont déjà visibles, noter la présence de « Débogage réseau », « Débogage sans fil » ou seulement « Débogage USB », et l'état actuel. Ne rien activer à cette étape.
5. Noter l'accès actuel à Projectivy et au launcher Free. Aucune modification de leurs permissions, interception Home ou configuration.

Les menus réels de cette Pop restent à confirmer. Une instruction Android générique ne prouve pas qu'un port ADB est ouvert sur ce modèle.

## 3. Choisir la méthode après identification

| Situation observée | Méthode préparée |
| --- | --- |
| Débogage sans fil avec code d'association et ports affichés | Appairage ADB, puis connexion au port de connexion affiché, après accord |
| Débogage réseau classique réellement proposé | Connexion à l'IPv4/port exacts proposés par le Player, après accord |
| Seulement débogage USB, aucun endpoint réseau confirmé | Pas de supposition que 5555 est ouvert ; privilégier la clé USB pour installer l'APK |
| Pas de débogage disponible, mais gestionnaire de fichiers capable d'ouvrir un APK depuis USB | Installation manuelle avec confirmation Android, après accord |
| Ni ADB accessible ni gestionnaire de fichiers adéquat | Identifier ce qui manque ; aucune application auxiliaire installée sans accord |

La documentation ADB actuelle distingue TV Android 13+ pour l'appairage sans fil ; la seule présence d'Android 11 sur une TV n'en garantit pas la disponibilité. Sur d'autres systèmes, le fabricant peut proposer une méthode réseau différente. Ne pas exécuter `adb tcpip 5555` à l'aveugle : la procédure générique requiert déjà une liaison ADB utilisable pour cette commande. Ne pas utiliser de câble USB-A vers USB-A entre PC et Player.

## 4. Procédure ADB — blocs non exécutés, après accord précis

L'accord doit couvrir l'activation éventuelle du débogage et l'autorisation de ce PC. Selon la règle AGENTS.md, consultation Advisor puis accord humain avant toute nouvelle autorisation ou installation assistée. Aucun scan LAN, règle pare-feu, redirection de port ou changement de launcher n'est inclus.

### Préparer les chemins et l'endpoint vérifié

```powershell
$TvRoot = 'C:\Users\jerry\Documents\SHINO Television Android'
$Adb = Join-Path $TvRoot '.local-build\android-sdk\platform-tools\adb.exe'
$Apk = Join-Path $TvRoot 'artifacts\SHINO-TV-HOME-v0.1-debug.apk'
$Endpoint = 'IP_DU_PLAYER:PORT_DE_CONNEXION_CONFIRME'
Get-FileHash -LiteralPath $Apk -Algorithm SHA256
```

Le bloc est un gabarit : remplacer l'endpoint par la valeur vérifiée ; ne pas copier un exemple IP d'un autre appareil. Utiliser un LAN de confiance. Ne jamais ouvrir ADB sur Internet.

### Variante A : menu avec code d'appairage

Après accord, le propriétaire ouvre le menu d'association sur TV. Saisir le code localement dans le terminal quand ADB le demande ; ne pas le coller dans un rapport/chat.

```powershell
& $Adb pair 'IP_DU_PLAYER:PORT_ASSOCIATION_AFFICHE'
& $Adb connect $Endpoint
& $Adb -s $Endpoint get-state
```

Les ports d'association et de connexion peuvent être différents. La clé ADB locale peut être créée par le premier démarrage du serveur ; rien de cela n'a été généré ni appairé par cet audit.

### Variante B : menu de débogage réseau classique

Après accord et activation par le propriétaire, utiliser l'adresse affichée/confirmée. Le port 5555 est une possibilité technique, pas une valeur démontrée sur cette Pop.

```powershell
& $Adb connect $Endpoint
& $Adb -s $Endpoint get-state
```

Accepter uniquement la demande d'autorisation du PC prévu à l'écran du Player. Pour cet essai, ne pas demander une autorisation permanente si elle n'est pas nécessaire. Si l'état est `unauthorized`, régler cela sur la télévision ; ne pas répéter des installations. Si refus/timeout, vérifier menu, endpoint et réseau invité/isolation, puis passer à USB plutôt que modifier le routeur.

### Préflight connecté : lecture seule, avant installation

```powershell
& $Adb -s $Endpoint shell getprop ro.product.model
& $Adb -s $Endpoint shell getprop ro.build.version.release
& $Adb -s $Endpoint shell getprop ro.build.version.sdk
& $Adb -s $Endpoint shell getprop ro.product.cpu.abilist
& $Adb -s $Endpoint shell wm size
& $Adb -s $Endpoint shell wm density
& $Adb -s $Endpoint shell pm path fr.shinobiwan.tvhome
& $Adb -s $Endpoint shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Ces commandes n'appliquent aucun réglage. `wm size` mesure la surface/logique Android ; confirmer le signal HDMI 1080p dans les menus du Player et de la TV. Si SHINO est déjà installé, relever sa version et sa signature avant de décider d'une mise à jour ; ne pas le désinstaller pour résoudre automatiquement un conflit.

### Installation de ce seul APK, après accord d'installation

```powershell
# Première installation, seulement si aucun package SHINO n'est présent.
& $Adb -s $Endpoint install $Apk
# Lancer uniquement après réponse Success.
& $Adb -s $Endpoint shell am start -W -n fr.shinobiwan.tvhome/.MainActivity
```

Ne pas ajouter `-g` pour accorder des permissions. Une mise à jour `install -r` exige une décision distincte, une signature compatible et la conservation voulue des préférences. Pas de `pm clear`, changement HOME, désactivation de package ou service d'accessibilité.

L'app peut aussi être ouverte manuellement depuis Projectivy. **Home reste destiné à Projectivy selon la configuration existante ; il ne doit pas devenir un raccourci direct vers SHINO.**

## 5. Repli par clé USB — sans débogage ADB

Procédure manuelle proposée, aucun transfert ou install effectué :

1. Utiliser une clé déjà compatible/lisible, idéalement FAT32, avec assez d'espace. Ne pas formater une clé contenant des fichiers.
2. Copier l'APK exact à sa racine, puis recalculer sur le PC le SHA-256 de la **copie USB** et comparer à celui ci-dessus. Éjecter proprement.
3. Brancher la clé au **Player Pop** ; la brancher au Server est un autre parcours et n'assure pas un accès local dans le gestionnaire Android.
4. Ouvrir un gestionnaire de fichiers déjà installé sachant transmettre un APK à l'installateur Android. La lecture multimédia USB confirmée par Free ne prouve pas que le navigateur de fichiers présent sait installer un APK.
5. Si aucun gestionnaire adéquat n'existe, identifier les options avec le propriétaire ; ne pas installer d'outil auxiliaire automatiquement.
6. Après accord, autoriser temporairement « Installer des applications inconnues » uniquement pour ce gestionnaire si Android le demande, ouvrir l'APK et confirmer l'installation de **SHINO // TV HOME**. Ne pas désactiver Play Protect ni contourner un blocage.
7. Ouvrir SHINO depuis la liste des applications TV/Projectivy. Retirer ensuite l'autorisation d'installation au gestionnaire si elle a été accordée pour cet essai.

Sans ADB, la vérification visuelle et télécommande reste possible ; PSS RAM et temps ActivityManager précis restent non mesurés. Ne pas les remplacer par des estimations.

## 6. Recette progressive sur le vrai matériel

| Étape | Action | Preuve attendue / résultat à noter |
| --- | --- | --- |
| R0 | Ouvrir SHINO depuis Projectivy ; appuyer sur Home | Projectivy récupéré, aucun changement de launcher |
| R1 | Confirmer 1080p HDMI, regarder depuis le canapé | Logo non déformé, marges/overscan, rangée et 1–3 widgets visibles, photo de SHINO seul si utile |
| R2 | Haut/bas/gauche/droite/OK/Retour, extrémités de rangée | Focus visible, défilement vers sélection, paramètres accessibles, pas de piège |
| R3 | Tester Free TV, Netflix, Disney+, Prime Video, YouTube un par un si installés | App réelle ouverte ; retour par Back si l'app le permet, sinon Home → Projectivy → SHINO ; focus au même raccourci à contrôler |
| R4 | Configurer ordre/masquage/widgets et quitter/revenir | Préférences conservées, focus récupéré ; panneaux purement informatifs sans arrêt de focus inutile |
| R5 | Mode normal sans PC et sans permission média | Absence de données explicite ; aucune donnée DEMO présentée comme réelle |
| R6 | Mesures de chargement et mémoire à 1, 10 et 30 min | Temps `am start -W`, PSS de SHINO, croissance éventuelle, crash/ANR/focus à noter |
| R7 | Reconnexion réseau, mise en veille/reprise normale | Rendu stable, horloge et états actualisés ; noter la politique de maintien d'écran éveillé de l'app |
| R8 | Redémarrage du Player choisi/manipulé par le propriétaire | Projectivy reste l'accueil ; SHINO ne démarre pas automatiquement ; ouverture manuelle et préférences préservées |

Le retour direct par Back dépend du comportement de chaque application de streaming. L'application SHINO ne peut pas imposer le comportement Home sans devenir HOME ou intercepter une touche, ce qui reste exclu. Ne pas changer Projectivy pour obtenir ce résultat.

Mesure RAM ciblée, après installation/lancement autorisés :

```powershell
& $Adb -s $Endpoint shell dumpsys meminfo fr.shinobiwan.tvhome
```

Pour distinguer démarrage froid et retour chaud : noter d'abord l'état ; un `am force-stop fr.shinobiwan.tvhome` puis `am start -W` peut servir au test froid, après accord sur ce test. Ne toucher qu'au package SHINO. Le temps Android n'est pas la latence télécommande perçue : mesurer/qualifier les deux séparément. Pas de capture de logs globale ni de capture des contenus DRM/comptes de streaming ; limiter les diagnostics au processus SHINO.

## 7. Retour arrière

- Home : conserver la configuration Projectivy actuelle.
- Si SHINO bloque, ouvrir Paramètres Android par le chemin habituel et forcer l'arrêt de SHINO uniquement ; ou utiliser, si ADB autorisé, `& $Adb -s $Endpoint shell am force-stop fr.shinobiwan.tvhome`.
- Désinstallation manuelle optionnelle de SHINO seulement, après accord, avec perte de ses préférences/cache/appairage. Ne pas supprimer Projectivy, Free TV ou un package système.
- Fin de session : `& $Adb disconnect $Endpoint`, puis désactiver uniquement le débogage activé pour cette session. En mode sans fil, oublier uniquement l'association de ce PC si elle n'est plus souhaitée ; ne pas révoquer les autres autorisations sans décision spécifique.
- `adb disconnect` ferme la liaison PC mais ne désactive pas le service ADB réseau sur la TV.

## 8. Émulateur stable : solution secondaire

Catalogue Google consulté : `https://dl.google.com/android/repository/sys-img/android-tv/sys-img2-1.xml`.

| Image | Canal | Licence annoncée |
| --- | --- | --- |
| `system-images;android-30;android-tv;x86` | stable / channel-0 | android-sdk-preview-license |
| `system-images;android-33;android-tv;x86` | stable / channel-0 | android-sdk-license |
| `system-images;android-34;android-tv;x86` | stable / channel-0 | android-sdk-license |
| `system-images;android-36;android-tv;x86_64` | stable / channel-0 | android-sdk-license |

Choix candidat : **Android TV API 34 x86** pour les tests fonctionnels génériques. L'API d'exécution peut différer de compileSdk 35 : le projet accepte API 26+. Aucun changement de SDK compile ni Preview requis pour ce candidat. Cela ne reproduit ni le firmware Pop ni ses DRM, son ABI ARM ou ses permissions particulières. L'accélération compatible du poste reste à valider avant de promettre un émulateur utilisable.

L'ancienne sélection API 30 était donc évitable pour ces tests. Le besoin éventuel porte sur l'acceptation de la **licence SDK standard**, pas obligatoirement sur Preview. Son acceptation exacte reste à confirmer avec SDK Manager avant installation ; une comparaison locale indicative du texte a été conservée, pas une acceptation.

Texte intégral du candidat : `artifacts/hardware-validation/android-sdk-license.txt`. Résumé non substitutif : licence limitée et sans redevance pour développer des apps Android compatibles ; restrictions de redistribution/modification sauf licences open source applicables ; obligations de confidentialité/sécurité des utilisateurs ; collecte de statistiques soumise au consentement prévu ; fourniture sans garanties, limitation de responsabilité, indemnisation, conditions d'export et droit/juridiction de Californie. Lire les termes exacts du paquet avant toute demande d'accord. Aucun `sdkmanager --licenses` global, aucune réponse automatique `y`.

## 9. Tests exécutés / restants

| Contrôle | Exécuté dans cette mission ? |
| --- | --- |
| Intégrité, signature, alignement, manifeste APK livré | Oui, hôte Windows |
| Documentation ADB, USB et catalogue/licences TV | Oui, sources primaires et métadonnées officielles |
| Audit ponctuel Periscope et GET LINK loopback | Oui, voir V0.2-INTEGRATION-AUDIT.md |
| Nouvelle connexion/appairage ADB à la Pop | Non : endpoint et accord attendus |
| Installation APK/outil auxiliaire sur Pop | Non |
| Télécommande, apps, retour, 1080p réel, RAM, stabilité, redémarrage | Non : recette préparée |
| Émulateur et tests instrumentés | Non : aucune image installée |

## Références primaires

- [ADB : association, connexion et installation](https://developer.android.com/tools/adb)
- [Android TV : débogage et variantes réseau](https://developers.google.com/cast/docs/android_tv_receiver/debugging)
- [Free : clé USB sur le Player, distincte du Server](https://assistance.free.fr/articles/96)
- [Applications TV classiques et émulation](https://developer.android.com/training/tv/get-started/create)
- [Conditions SDK actuelles sur le site Android](https://developer.android.com/studio/terms) ; pour le paquet choisi, consulter aussi le texte exact de son dépôt archivé localement.
