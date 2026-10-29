# Contrat consommé

`snapshot-v2.schema.json` est une copie intégrale du contrat SHINO-RELAY
`contracts/snapshot-v2.schema.json`, lu pendant la mission TV HOME v2.
Les règles temporelles de `lab.py`, `hub/core.py` et `hub/reliable.py` complètent
le schéma : TTL telemetry=6, media=10, codex=900 secondes, expiration à la
borne exacte, reset Codex atteint périmé, pas de date source future actuelle.

Les noms JSON sont conservés : capteurs `available/value/unit`, `ramUsedGiB`,
états média, `timelineUnit=milliseconds`, `artwork.reference` de type CONTENT_ID.
Le HUB sert uniquement le JPEG par `GET /hub/v1/artwork/<id>` ; le schéma général
autorise aussi PNG, que ce lecteur ne télécharge pas depuis le HUB.

`app/src/test/resources/hub-scenario-test.json` provient du transcript synthétique
SHINO-LAB `fixtures/hub/scenario-test.json`. Aucun payload réel ni credential.
Les tests Kotlin le consomment sans traduction ; le test Python valide chaque
snapshot v2 du transcript avec le schéma copié.
