# Guide Postman - Upload de Fichier dans Message Create

## 📋 Configuration

### 1. Importer la collection

1. Ouvrir Postman
2. Cliquer sur **Import**
3. Sélectionner le fichier : `src/main/resources/json/rest_api.postman_collection.json`
4. La collection "ci_orange_messagerie" sera importée

### 2. Configurer les variables d'environnement

1. Créer un nouvel environnement (ou utiliser l'environnement par défaut)
2. Ajouter la variable :
   - **Variable** : `appUrl`
   - **Valeur initiale** : `http://localhost:8080`
   - **Valeur actuelle** : `http://localhost:8080`

## 🚀 Utilisation

### Option 1 : Message avec fichier (Multipart)

1. Dans la collection, ouvrir **message** → **message.create (avec fichier)**

2. **Configuration de la requête** :
   - **Méthode** : `POST`
   - **URL** : `{{appUrl}}/message/create`
   - **Body** : Sélectionner `form-data`

3. **Remplir les champs** :
   - **file** : 
     - Type : `File`
     - Cliquer sur "Select Files" et choisir une image (jpg, jpeg, png, gif, webp)
   - **conversationId** : 
     - Type : `Text`
     - Valeur : `1` (remplacer par un ID de conversation valide)
   - **content** : 
     - Type : `Text`
     - Valeur : `Message avec image` (optionnel)
   - **typeMessage** : 
     - Type : `Text`
     - Valeur : `1` (optionnel)
   - **user** : 
     - Type : `Text`
     - Valeur : `1` (optionnel)

4. **Headers** :
   - `lang: fr` (déjà configuré)

5. **Envoyer la requête**

### Option 2 : Message sans fichier (JSON classique)

1. Utiliser **message** → **message.create** (requête JSON classique)
2. Le champ `imgUrl` peut contenir une URL d'image déjà existante

## 📝 Exemple de requête JSON (sans fichier)

```json
{
  "user": "1",
  "datas": [
    {
      "content": "Message de test",
      "imgUrl": "/files/images/existing-image.jpg",
      "conversationId": "1",
      "typeMessage": "1"
    }
  ]
}
```

## ✅ Réponse attendue

### Succès (200 OK)

```json
{
  "hasError": false,
  "status": {
    "code": "800",
    "message": "Operation effectuee avec succes: ..."
  },
  "items": [
    {
      "id": 123,
      "conversationId": 1,
      "content": "Message avec image",
      "imgUrl": "/files/images/uuid-fichier.jpg",
      "typeMessage": 1,
      "createdAt": "08/01/2026",
      "createdBy": 1,
      "isDeleted": false
    }
  ],
  "itemsNumber": 1
}
```

### Erreur (exemple)

```json
{
  "hasError": true,
  "status": {
    "code": "924",
    "message": "Erreur lors de l'upload de fichier: Le fichier est trop volumineux. Taille max: 10MB"
  },
  "items": [],
  "itemsNumber": 0
}
```

## 🔍 Vérifications

### Après un upload réussi

1. **Vérifier le fichier sauvegardé** :
   - Le fichier est dans : `uploads/images/`
   - Nom du fichier : UUID généré automatiquement

2. **Accéder à l'image** :
   - URL : `http://localhost:8080/files/images/{nom-fichier}`
   - Exemple : `http://localhost:8080/files/images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`

3. **Vérifier dans la base de données** :
   ```sql
   SELECT id, content, img_url, conversation_id, created_at 
   FROM message 
   ORDER BY created_at DESC 
   LIMIT 1;
   ```

## ⚠️ Erreurs courantes

### 1. Fichier trop volumineux
- **Erreur** : "Le fichier est trop volumineux. Taille max: 10MB"
- **Solution** : Réduire la taille de l'image ou augmenter `app.upload.max-size` dans `application.properties`

### 2. Type de fichier non autorisé
- **Erreur** : "Type de fichier non autorisé"
- **Solution** : Utiliser uniquement jpg, jpeg, png, gif, webp

### 3. ConversationId manquant
- **Erreur** : "Champ non renseigné: conversationId"
- **Solution** : Remplir le champ `conversationId` avec un ID valide

### 4. Fichier vide
- **Erreur** : "Le fichier est vide ou null"
- **Solution** : Sélectionner un fichier valide

## 💡 Astuces

1. **Tester avec différents formats** : jpg, png, gif, webp
2. **Tester avec et sans contenu texte** : Le message peut être uniquement une image
3. **Tester les limites** : Fichier de 10MB, fichier très petit, etc.
4. **Utiliser les variables Postman** : Créer des variables pour `conversationId` et `user` pour faciliter les tests

## 📸 Capture d'écran de configuration Postman

```
┌─────────────────────────────────────────┐
│ POST {{appUrl}}/message/create         │
├─────────────────────────────────────────┤
│ Headers                                 │
│ lang: fr                                │
├─────────────────────────────────────────┤
│ Body (form-data)                        │
│                                         │
│ file          [File] [Select Files]    │
│ conversationId [Text] 1                 │
│ content       [Text] Message avec image │
│ typeMessage   [Text] 1                  │
│ user          [Text] 1                  │
└─────────────────────────────────────────┘
```

## 🔗 Endpoints disponibles

- **POST** `/message/create` (JSON) - Création classique avec JSON
- **POST** `/message/create` (multipart) - Création avec upload de fichier
- **POST** `/message/upload` - Endpoint dédié upload (alternative)

Les deux méthodes (`/message/create` avec multipart et `/message/upload`) fonctionnent de la même manière.

