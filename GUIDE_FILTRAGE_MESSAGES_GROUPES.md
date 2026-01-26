# Guide : Filtrage des Messages dans les Groupes (Comportement WhatsApp)

## Comportement Implémenté

Le système filtre automatiquement les messages dans les groupes selon l'historique de participation de l'utilisateur, exactement comme WhatsApp.

## Règles de Filtrage

### 1. Nouveau Participant (jamais quitté)
- **Comportement** : Ne voit **QUE** les messages envoyés **APRÈS** son ajout au groupe
- **Exemple** : Si un utilisateur est ajouté le 15 janvier à 10h00, il ne verra que les messages envoyés après cette date/heure
- **Logique backend** : `message.createdAt >= participant.createdAt`

### 2. Participant qui a Quitté (pas réintégré)
- **Comportement** : Ne voit **QUE** les messages envoyés **AVANT** son départ
- **Exemple** : Si un utilisateur a quitté le 20 janvier à 14h00, il ne verra que les messages envoyés avant cette date/heure
- **Logique backend** : `message.createdAt < participant.leftAt`

### 3. Participant Réintégré
- **Comportement** : Voit les messages d'**AVANT** son départ + les messages envoyés **APRÈS** sa réintégration
- **Exemple** : 
  - Ajouté le 10 janvier
  - Quitté le 20 janvier à 14h00
  - Réintégré le 25 janvier à 09h00
  - Voit : Messages du 10 au 20 janvier (avant départ) + Messages du 25 janvier et après (après réintégration)
  - Ne voit PAS : Messages du 20 au 25 janvier (période d'absence)
- **Logique backend** : `message.createdAt < participant.leftAt OR message.createdAt >= participant.recreatedAt`

## Implémentation Frontend

### ✅ Aucune Action Requise !

Le filtrage est **automatique** côté backend. Le frontend n'a **rien de spécial à faire**.

### Comment ça fonctionne

1. **Récupération des messages** : Le frontend appelle l'API comme d'habitude :
   ```javascript
   // Exemple de requête
   POST /message/getByCriteria
   {
     "user": 123,  // ID de l'utilisateur connecté
     "data": {
       "conversationId": 456  // ID du groupe
     }
   }
   ```

2. **Filtrage automatique** : Le backend filtre automatiquement les messages selon :
   - L'ID de l'utilisateur (`request.user`)
   - Le `conversationId` fourni
   - L'historique de participation de l'utilisateur dans ce groupe

3. **Réponse** : Le frontend reçoit uniquement les messages que l'utilisateur est autorisé à voir

### Points Importants

- ✅ Le filtrage s'applique **uniquement aux groupes** (pas aux conversations privées)
- ✅ Le `conversationId` doit être fourni dans la requête pour que le filtrage fonctionne
- ✅ Le `user` (ID de l'utilisateur connecté) doit être présent dans la requête
- ✅ Aucun changement nécessaire dans le code frontend existant

## Exemples de Scénarios

### Scénario 1 : Nouveau Membre
```
Groupe créé : 1er janvier
Messages envoyés : 1er au 10 janvier
Nouveau membre ajouté : 10 janvier à 15h00
Messages envoyés après : 10 janvier 15h01, 11 janvier, etc.

Résultat : Le nouveau membre voit uniquement les messages du 10 janvier 15h01 et après
```

### Scénario 2 : Membre qui Quitte
```
Membre dans le groupe : 1er au 20 janvier
Membre quitte : 20 janvier à 14h00
Messages envoyés après : 20 janvier 14h01, 21 janvier, etc.

Résultat : Le membre voit uniquement les messages du 1er au 20 janvier (avant 14h00)
```

### Scénario 3 : Membre Réintégré
```
Membre ajouté : 1er janvier
Membre quitte : 20 janvier à 14h00
Messages entre : 20 janvier 14h01 au 25 janvier 08h59
Membre réintégré : 25 janvier à 09h00
Messages après : 25 janvier 09h01, 26 janvier, etc.

Résultat : Le membre voit :
- Messages du 1er au 20 janvier (avant départ)
- Messages du 25 janvier 09h01 et après (après réintégration)
- Ne voit PAS les messages du 20 janvier 14h01 au 25 janvier 08h59
```

## API Endpoint

### Récupérer les Messages d'un Groupe

**Endpoint** : `POST /message/getByCriteria`

**Request Body** :
```json
{
  "user": 123,
  "data": {
    "conversationId": 456
  },
  "index": 0,
  "size": 50
}
```

**Response** :
```json
{
  "hasError": false,
  "items": [
    {
      "id": 789,
      "content": "Message visible",
      "createdAt": "2026-01-15T10:30:00",
      "conversationId": 456,
      "createdBy": 111
    }
    // ... seulement les messages que l'utilisateur peut voir
  ],
  "count": 25
}
```

## Notes Techniques

- Le filtrage utilise une sous-requête `NOT EXISTS` dans JPQL
- Les performances sont optimisées grâce aux index sur les champs de date (`createdAt`, `leftAt`, `recreatedAt`)
- Le filtrage est appliqué **avant** la pagination, donc le `count` reflète le nombre réel de messages visibles

## Support

Si vous rencontrez des problèmes ou avez des questions, vérifiez :
1. Que le `conversationId` est bien fourni dans la requête
2. Que le `user` (ID utilisateur) est présent dans la requête
3. Que la conversation est bien de type "GROUP" ou "GROUPE"
