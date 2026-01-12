#!/bin/bash

# Script de test pour l'upload d'images
# Usage: ./test-upload.sh [chemin-vers-image] [conversationId] [content] [userId]

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
ENDPOINT="/message/upload"

# Paramètres
IMAGE_PATH="${1:-}"
CONVERSATION_ID="${2:-1}"
CONTENT="${3:-Message de test}"
USER_ID="${4:-1}"

echo -e "${YELLOW}=== Test Upload d'Image ===${NC}\n"

# Vérifier si curl est installé
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Erreur: curl n'est pas installé${NC}"
    exit 1
fi

# Vérifier si le fichier existe
if [ -z "$IMAGE_PATH" ]; then
    echo -e "${YELLOW}Usage: $0 <chemin-image> [conversationId] [content] [userId]${NC}"
    echo -e "${YELLOW}Exemple: $0 ~/Desktop/test.jpg 1 \"Mon message\" 1${NC}"
    exit 1
fi

if [ ! -f "$IMAGE_PATH" ]; then
    echo -e "${RED}Erreur: Le fichier '$IMAGE_PATH' n'existe pas${NC}"
    exit 1
fi

# Afficher les paramètres
echo -e "📁 Fichier: ${GREEN}$IMAGE_PATH${NC}"
echo -e "💬 Conversation ID: ${GREEN}$CONVERSATION_ID${NC}"
echo -e "📝 Contenu: ${GREEN}$CONTENT${NC}"
echo -e "👤 User ID: ${GREEN}$USER_ID${NC}"
echo -e "🌐 URL: ${GREEN}${BASE_URL}${ENDPOINT}${NC}\n"

# Effectuer la requête
echo -e "${YELLOW}Envoi de la requête...${NC}\n"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}${ENDPOINT}" \
  -F "file=@${IMAGE_PATH}" \
  -F "conversationId=${CONVERSATION_ID}" \
  -F "content=${CONTENT}" \
  -F "user=${USER_ID}" \
  -H "lang: fr")

# Séparer le body et le code HTTP
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

# Afficher le résultat
echo -e "Code HTTP: ${GREEN}$HTTP_CODE${NC}\n"

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Succès!${NC}\n"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    
    # Extraire l'URL de l'image si possible
    IMG_URL=$(echo "$BODY" | grep -o '"imgUrl":"[^"]*"' | cut -d'"' -f4)
    if [ ! -z "$IMG_URL" ]; then
        echo -e "\n${GREEN}🖼️  Image accessible à: ${BASE_URL}${IMG_URL}${NC}"
    fi
else
    echo -e "${RED}❌ Erreur!${NC}\n"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
fi

