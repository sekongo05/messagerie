# Guide de Test - Upload d'Images

## Prérequis

1. L'application doit être démarrée sur le port 8080
2. Avoir une image de test (jpg, jpeg, png, gif, webp)
3. Avoir un `conversationId` valide dans la base de données
4. Le répertoire `uploads/images` sera créé automatiquement

## Méthode 1 : Test avec cURL (Terminal)

### Commande de base

```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/chemin/vers/votre/image.jpg" \
  -F "conversationId=1" \
  -F "content=Message avec image" \
  -F "user=1" \
  -H "lang: fr"
```

### Exemple complet avec tous les paramètres

```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/Users/macbookairm1/Desktop/test-image.jpg" \
  -F "conversationId=1" \
  -F "content=Voici une image de test" \
  -F "typeMessage=1" \
  -F "user=1" \
  -H "lang: fr" \
  -v
```

### Test avec seulement une image (sans texte)

```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/Users/macbookairm1/Desktop/test-image.png" \
  -F "conversationId=1" \
  -F "user=1" \
  -H "lang: fr"
```

### Vérifier la réponse

La réponse devrait ressembler à :
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
      "imgUrl": "/files/images/uuid-fichier.jpg",
      "content": "Message avec image",
      ...
    }
  ]
}
```

## Méthode 2 : Test avec Postman

1. **Créer une nouvelle requête**
   - Méthode : `POST`
   - URL : `http://localhost:8080/message/upload`

2. **Configurer les headers**
   - Ajouter : `lang: fr` (ou `en`)

3. **Configurer le body**
   - Sélectionner : `form-data`
   - Ajouter les champs :
     - `file` : Type `File`, sélectionner votre image
     - `conversationId` : Type `Text`, valeur : `1`
     - `content` : Type `Text`, valeur : `Message de test` (optionnel)
     - `user` : Type `Text`, valeur : `1` (optionnel)
     - `typeMessage` : Type `Text`, valeur : `1` (optionnel)

4. **Envoyer la requête**

## Méthode 3 : Test avec JavaScript/Fetch

### Code HTML/JavaScript simple

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Upload Image</title>
</head>
<body>
    <input type="file" id="fileInput" accept="image/*">
    <input type="number" id="conversationId" placeholder="Conversation ID" value="1">
    <input type="text" id="content" placeholder="Message (optionnel)">
    <input type="number" id="userId" placeholder="User ID" value="1">
    <button onclick="uploadImage()">Uploader</button>
    <div id="result"></div>

    <script>
        async function uploadImage() {
            const fileInput = document.getElementById('fileInput');
            const conversationId = document.getElementById('conversationId').value;
            const content = document.getElementById('content').value;
            const userId = document.getElementById('userId').value;

            if (!fileInput.files[0]) {
                alert('Veuillez sélectionner un fichier');
                return;
            }

            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            formData.append('conversationId', conversationId);
            if (content) formData.append('content', content);
            if (userId) formData.append('user', userId);

            try {
                const response = await fetch('http://localhost:8080/message/upload', {
                    method: 'POST',
                    headers: {
                        'lang': 'fr'
                    },
                    body: formData
                });

                const result = await response.json();
                document.getElementById('result').innerHTML = 
                    '<pre>' + JSON.stringify(result, null, 2) + '</pre>';
                
                if (!result.hasError) {
                    console.log('Image uploadée avec succès !');
                    console.log('URL de l\'image:', result.items[0].imgUrl);
                } else {
                    console.error('Erreur:', result.status.message);
                }
            } catch (error) {
                console.error('Erreur:', error);
                document.getElementById('result').innerHTML = 
                    '<p style="color: red;">Erreur: ' + error.message + '</p>';
            }
        }
    </script>
</body>
</html>
```

## Méthode 4 : Test avec HTTPie (si installé)

```bash
http POST http://localhost:8080/message/upload \
  file@/chemin/vers/image.jpg \
  conversationId:=1 \
  content="Message de test" \
  user:=1 \
  lang:fr
```

## Vérifications après l'upload

### 1. Vérifier que le fichier est sauvegardé

```bash
# Vérifier le répertoire
ls -la uploads/images/

# Voir le contenu
ls -lh uploads/images/
```

### 2. Accéder à l'image via l'URL

Ouvrir dans le navigateur :
```
http://localhost:8080/files/images/{nom-du-fichier}
```

### 3. Vérifier dans la base de données

```sql
SELECT id, content, img_url, conversation_id, created_at 
FROM message 
ORDER BY created_at DESC 
LIMIT 1;
```

## Tests d'erreurs

### Test 1 : Fichier trop volumineux (> 10MB)
```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/chemin/vers/gros-fichier.jpg" \
  -F "conversationId=1" \
  -H "lang: fr"
```
**Résultat attendu** : Erreur "Le fichier est trop volumineux"

### Test 2 : Type de fichier non autorisé
```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/chemin/vers/document.pdf" \
  -F "conversationId=1" \
  -H "lang: fr"
```
**Résultat attendu** : Erreur "Type de fichier non autorisé"

### Test 3 : Fichier vide
```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=" \
  -F "conversationId=1" \
  -H "lang: fr"
```
**Résultat attendu** : Erreur "Le fichier est vide ou null"

### Test 4 : ConversationId manquant
```bash
curl -X POST http://localhost:8080/message/upload \
  -F "file=@/chemin/vers/image.jpg" \
  -H "lang: fr"
```
**Résultat attendu** : Erreur de validation

## Vérification des logs

Surveiller les logs de l'application pour voir :
- Le chemin de sauvegarde du fichier
- Les erreurs éventuelles
- Les informations de debug

```bash
# Si vous utilisez Maven
tail -f logs/application.log

# Ou dans la console Spring Boot
```

## Exemple de réponse réussie

```json
{
  "hasError": false,
  "status": {
    "code": "800",
    "message": "Operation effectuee avec succes: Message créé avec succès"
  },
  "items": [
    {
      "id": 42,
      "conversationId": 1,
      "content": "Message avec image",
      "imgUrl": "/files/images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
      "typeMessage": 1,
      "createdAt": "08/01/2026",
      "createdBy": 1,
      "isDeleted": false
    }
  ],
  "itemsNumber": 1
}
```

## Exemple de réponse d'erreur

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

