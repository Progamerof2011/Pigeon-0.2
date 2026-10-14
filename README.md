# Pigeon 0.1 (bêta)

Messagerie sans Internet ni serveur : les téléphones Android à portée Bluetooth
(BLE) échangent les messages du canal général.

## Ouvrir et lancer

1. Ouvrir Android Studio, **Open**, puis choisir le dossier `pigeon`.
2. Attendre la synchronisation Gradle.
3. Brancher un téléphone (débogage USB activé) ou lancer un émulateur, puis **Run**.
4. Pour tester la communication, il faut **deux téléphones réels** : l'émulateur ne gère pas le BLE.

## Structure

```
app/src/main/java/fr/pigeon/app/
 ├── PigeonApplication.kt     → services partagés (créés une seule fois)
 ├── MainActivity.kt          → permissions Bluetooth, affichage
 ├── core/AppServices.kt      → conteneur des services (BLE, dépôt de messages)
 ├── ble/                     → Bluetooth : publicité, scan, GATT (client + serveur)
 ├── protocol/                → format des messages, fragments, réassemblage
 ├── data/                    → historique des messages
 ├── features/                → une fonction = un onglet
 │    ├── FeatureRegistry.kt  → liste des fonctions (ordre de la barre du bas)
 │    ├── chat/ChatScreen.kt
 │    └── settings/SettingsScreen.kt
 └── ui/
      ├── PigeonApp.kt        → coquille (barre du bas)
      ├── theme/Theme.kt      → couleurs à modifier ici
      └── components/         → composants réutilisables
```

## Ajouter une fonction (ex. NFC)

1. Créer `features/nfc/NfcScreen.kt` avec `@Composable fun NfcScreen(services: AppServices)`.
2. Si la fonction a besoin d'un service, l'ajouter dans `core/AppServices.kt`
   et le créer dans `PigeonApplication.kt`.
3. Dans `FeatureRegistry.kt`, décommenter le bloc NFC et passer `enabled = true`.

## Fonctionnement BLE (0.1)

- Chaque appareil annonce le service Pigeon et scanne les autres.
- Un message est découpé en fragments et écrit sur chaque appareil connecté.
- La taille d'un message n'est pas limitée (le débit, lui, l'est).
- Les messages reçus sont dédupliqués par identifiant.
- Pas de relais : un message atteint les appareils à portée directe de l'expéditeur.
- Le Bluetooth ne fonctionne que tant que l'app est ouverte (pas encore de service en arrière-plan).

## Distribution hors Play Store

1. Créer une clé de signature : **Build > Generate Signed App Bundle / APK > APK > Create new**.
   Garder le fichier `.jks` et ses mots de passe.
2. Créer `keystore.properties` à la racine (ne pas le partager) :
   ```
   storeFile=chemin/vers/pigeon.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```
3. **Build > Build APK(s)** en mode release, ou `./gradlew assembleRelease`.
   L'APK est dans `app/build/outputs/apk/release/`.
4. Envoyer l'APK aux testeurs (Firebase App Distribution, Drive, etc.).
5. À chaque version, incrémenter `versionCode` et `versionName` dans `app/build.gradle.kts`.

## Limites connues de la 0.1

- Non compilée ni testée sur appareil au moment de l'écriture : les premiers tests peuvent révéler des bugs BLE.
- Pas d'icône d'application (icône par défaut).
- Historique perdu à la fermeture de l'app.
- Un message perdu pendant l'envoi n'est pas renvoyé (pas d'accusé de réception).
- Certains téléphones limitent le nombre de connexions BLE simultanées.
