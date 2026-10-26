# Installation de développement et retour

Pour la procédure détaillée Freebox Pop, les variantes ADB/clé USB et l'état réel des essais, utiliser [HARDWARE-VALIDATION.md](HARDWARE-VALIDATION.md). Les blocs ci-dessous restent des exemples non exécutés ; `install -r` concerne une mise à jour autorisée, pas une première installation automatique.

**Ces étapes sont fournies pour exécution humaine. Aucune installation sur la Freebox n'a été effectuée.** Préserver Projectivy, ses réglages et le launcher Free. Ne désactiver aucun package.

## Préparer

Vérifier la somme SHA-256 de l'APK livré dans `artifacts\apk-verification.json`. Sur la Freebox, vérifier l'Android/API dans les paramètres ; API 26 minimum. Préserver un accès visible à Projectivy, Free TV et aux Paramètres Android.

Choisir volontairement une méthode d'installation locale autorisée : transfert USB/fichier avec confirmation Android, ou ADB déjà activé et autorisé par le propriétaire. Ne pas activer ADB réseau automatiquement et ne pas rechercher d'appareils sur le LAN.

## ADB si déjà autorisé

```powershell
# Remplacer SERIAL uniquement par l'appareil que vous avez explicitement autorisé.
adb -s SERIAL shell getprop ro.build.version.release
adb -s SERIAL shell getprop ro.build.version.sdk
adb -s SERIAL install -r '.\artifacts\SHINO-TV-HOME-v0.1-debug.apk'
adb -s SERIAL shell am start -n fr.shinobiwan.tvhome/.MainActivity
```

`-r` conserve les préférences de cette application si la signature debug est la même. Une autre clé debug ne peut pas remplacer la première sans procédure explicite. Ne pas désinstaller pour contourner un conflit de signature sans accepter la perte des données locales.

L'activité expose seulement `LEANBACK_LAUNCHER`. La touche Home reste gérée par votre configuration actuelle, dont l'interception Projectivy déjà validée par le propriétaire. Aucune promesse de comportement HOME propre à SHINO v0.1.

## Revenir à Projectivy

1. Appuyer sur Home : conserver la configuration d'interception Projectivy existante.
2. Ou appuyer sur Retour depuis l'accueil SHINO ; Android revient au launcher qui l'a ouvert.
3. Le raccourci Projectivy apparaît dans SHINO s'il expose une activité Leanback et n'a pas été masqué.
4. En cas de blocage, ouvrir Paramètres Android depuis les réglages SHINO ou la télécommande ; forcer l'arrêt de **SHINO // TV HOME** uniquement.
5. Désinstaller SHINO depuis les Paramètres Android si nécessaire. Cela ne modifie pas Projectivy ni le launcher Free. Les préférences/cache/appairage SHINO sont effacés.

En ADB propriétaire déjà autorisé, le retour non destructif peut commencer par `adb -s SERIAL shell am force-stop fr.shinobiwan.tvhome`, puis Home sur la télécommande. Ne pas utiliser `pm disable-user`, `set-home-activity`, root ou un nouveau service d'accessibilité.

## Validation physique avant usage quotidien

Suivre [DEVICE-CHECKLIST.md](DEVICE-CHECKLIST.md). La transition vers une véritable déclaration HOME est hors v0.1 et exige une nouvelle décision explicite.
