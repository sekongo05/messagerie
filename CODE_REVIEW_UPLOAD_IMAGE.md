# 📋 Code Review - Upload de Fichiers Images

## 🎯 Scope
Revue complète du système d'upload de fichiers images dans l'application de messagerie.

---

## 📁 Fichiers Analysés

1. **FileUploadService.java** - Service de gestion des uploads
2. **FileUploadConfig.java** - Configuration Spring pour servir les fichiers
3. **MessageController.java** - Endpoints d'upload
4. **application.properties** - Configuration multipart et stockage

---

## ✅ Points Positifs

### 1. **Architecture bien structurée**
- Service dédié pour la gestion des fichiers
- Configuration séparée pour le mapping des ressources
- Endpoints REST clairs

### 2. **Sécurité de base**
- Validation du type MIME (`image/*`)
- Validation de l'extension de fichier
- Limitation de la taille des fichiers
- Génération de noms uniques (UUID)

### 3. **Gestion des répertoires**
- Création automatique du répertoire s'il n'existe pas

---

## 🔴 PROBLÈMES CRITIQUES

### 1. **Incohérence entre configuration Spring et validation personnalisée**

**Localisation** : `application.properties` ligne 15-16 vs `FileUploadService` ligne 81

**Problème** :
- Spring Boot limite à **30MB** : `spring.servlet.multipart.max-file-size=30MB`
- Votre validation limite à **90485760 bytes = ~86MB** : `app.upload.max-size=90485760`
- **Spring rejettera le fichier AVANT même d'arriver à votre validation**

**Impact** : Les fichiers entre 30MB et 86MB seront rejetés par Spring avec une erreur 500, sans message d'erreur personnalisé.

**Solution** :
```properties
# Aligner les deux limites
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
app.upload.max-size=10485760  # 10MB en bytes
```

### 2. **PDF dans les types autorisés mais validation MIME refuse les PDF**

**Localisation** : 
- `application.properties` ligne 22 : `app.upload.allowed-types=jpg,jpeg,png,gif,webp,pdf`
- `FileUploadService.java` ligne 100 : `if (contentType == null || !contentType.startsWith("image/"))`

**Problème** :
- Configuration autorise `pdf`
- Validation MIME rejette tout ce qui n'est pas `image/*`
- **Un PDF sera accepté par l'extension mais rejeté par le MIME**

**Impact** : Confusion, erreurs incohérentes.

**Solution** :
```java
// Si on veut vraiment autoriser PDF (peu probable pour images)
if (contentType != null && !contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
    throw new IllegalArgumentException("Le fichier doit être une image");
}
// OU mieux : Retirer PDF de la config si c'est pour les images uniquement
```

**Recommandation** : Retirer `pdf` de `allowed-types` car le service s'appelle `saveImageFile`.

### 3. **Pas de validation de la signature magique du fichier**

**Localisation** : `FileUploadService.validateFile()` ligne 98-102

**Problème** : 
- Seule l'extension et le type MIME sont vérifiés
- Un attaquant peut renommer un fichier malveillant en `.jpg` et changer le Content-Type
- **Pas de vérification des bytes réels du fichier**

**Risque Sécurité** : Upload de fichiers malveillants (exécutables, scripts) masqués en images.

**Solution** :
```java
private void validateFileSignature(MultipartFile file) throws IOException, IllegalArgumentException {
    byte[] header = new byte[12];
    try (InputStream is = file.getInputStream()) {
        is.read(header);
    }
    
    // Vérifier les signatures magiques (Magic Numbers)
    String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
    
    if ("jpg".equals(extension) || "jpeg".equals(extension)) {
        // JPEG: FF D8 FF
        if (header[0] != (byte)0xFF || header[1] != (byte)0xD8 || header[2] != (byte)0xFF) {
            throw new IllegalArgumentException("Le fichier n'est pas une image JPEG valide");
        }
    } else if ("png".equals(extension)) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header[0] != (byte)0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
            throw new IllegalArgumentException("Le fichier n'est pas une image PNG valide");
        }
    } else if ("gif".equals(extension)) {
        // GIF: 47 49 46 38 (GIF8)
        if ((header[0] != 0x47 || header[1] != 0x49 || header[2] != 0x46 || header[3] != 0x38)) {
            throw new IllegalArgumentException("Le fichier n'est pas une image GIF valide");
        }
    } else if ("webp".equals(extension)) {
        // WEBP: RIFF...WEBP
        String headerStr = new String(header, StandardCharsets.US_ASCII);
        if (!headerStr.startsWith("RIFF") || !new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            throw new IllegalArgumentException("Le fichier n'est pas une image WebP valide");
        }
    }
}
```

### 4. **Pas de nettoyage si l'upload échoue après la sauvegarde**

**Localisation** : `MessageController.createWithFile()` ligne 100 et 124

**Problème** :
```java
String imageUrl = fileUploadService.saveImageFile(file); // Fichier sauvegardé
// ... création message ...
response = controllerFactory.create(...); // Peut échouer
// Si échec → fichier reste orphelin sur le disque
```

**Impact** : Accumulation de fichiers orphelins, consommation d'espace disque.

**Solution** :
```java
String savedImageUrl = null;
try {
    savedImageUrl = fileUploadService.saveImageFile(file);
    // ... création message ...
    response = controllerFactory.create(...);
    
    if (response.isHasError() && savedImageUrl != null) {
        // Rollback : supprimer le fichier
        fileUploadService.deleteFile(savedImageUrl);
        log.warning("Fichier supprimé suite à l'échec de création du message: " + savedImageUrl);
    }
} catch (Exception e) {
    if (savedImageUrl != null) {
        fileUploadService.deleteFile(savedImageUrl);
    }
    throw e;
}
```

### 5. **Chemin relatif sans validation du répertoire de base**

**Localisation** : `FileUploadConfig.java` ligne 23

**Problème** :
```java
.addResourceLocations("file:" + uploadDir + "/");
```

Si `uploadDir` est un chemin relatif comme `uploads/images`, cela pourrait pointer vers différents répertoires selon où l'application est lancée.

**Impact** : Fichiers sauvegardés dans un endroit, servis depuis un autre.

**Solution** :
```java
@Value("${app.upload.dir}")
private String uploadDir;

@PostConstruct
public void init() {
    Path uploadPath = Paths.get(uploadDir);
    if (!uploadPath.isAbsolute()) {
        // Convertir en chemin absolu
        uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir).toAbsolutePath().normalize();
    }
    this.uploadDir = uploadPath.toString();
}
```

---

## 🟡 PROBLÈMES IMPORTANTS

### 6. **Pas de gestion des chemins absolus vs relatifs**

**Localisation** : `FileUploadService.saveImageFile()` ligne 52

**Problème** :
```java
Path uploadPath = Paths.get(uploadDir);
```

Si `uploadDir` est relatif, le répertoire dépend du répertoire de travail actuel.

**Solution** :
```java
private Path getUploadPath() {
    Path path = Paths.get(uploadDir);
    if (!path.isAbsolute()) {
        // Utiliser un répertoire de base fixe
        path = Paths.get(System.getProperty("user.home"), "messagerie", uploadDir);
    }
    return path.toAbsolutePath().normalize();
}
```

### 7. **Extension vide non gérée proprement**

**Localisation** : `FileUploadService.getFileExtension()` ligne 108-112

**Problème** :
```java
if (filename == null || filename.lastIndexOf(".") == -1) {
    return "";  // Extension vide
}
```

Si extension vide, `generateUniqueFilename("")` retournera `uuid.` (point final).

**Solution** :
```java
private String getFileExtension(String filename) {
    if (filename == null || filename.lastIndexOf(".") == -1) {
        throw new IllegalArgumentException("Le fichier doit avoir une extension");
    }
    String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    if (ext.isEmpty()) {
        throw new IllegalArgumentException("L'extension du fichier est vide");
    }
    return ext;
}
```

### 8. **Pas de validation du nom de fichier original**

**Localisation** : `FileUploadService.saveImageFile()` ligne 58

**Problème** :
- Pas de vérification de la longueur du nom de fichier
- Pas de vérification des caractères spéciaux dangereux (`../`, `\0`, etc.)
- Bien que UUID soit utilisé, le nom original est quand même utilisé pour l'extension

**Solution** :
```java
private String sanitizeFilename(String filename) {
    if (filename == null) {
        throw new IllegalArgumentException("Le nom du fichier est null");
    }
    // Retirer les chemins relatifs
    filename = Paths.get(filename).getFileName().toString();
    // Limiter la longueur
    if (filename.length() > 255) {
        filename = filename.substring(0, 255);
    }
    return filename;
}
```

### 9. **deleteFile() fragile avec URL malformée**

**Localisation** : `FileUploadService.deleteFile()` ligne 128

**Problème** :
```java
String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
```

Si `fileUrl` ne contient pas `/`, `lastIndexOf("/")` retourne `-1`, donc `filename` devient `fileUrl` entier.

**Solution** :
```java
public boolean deleteFile(String fileUrl) {
    if (fileUrl == null || fileUrl.isEmpty()) {
        return false;
    }
    try {
        String filename;
        int lastSlash = fileUrl.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
            filename = fileUrl.substring(lastSlash + 1);
        } else {
            filename = fileUrl; // Fallback
        }
        
        // Validation supplémentaire
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.warning("Tentative de suppression avec nom de fichier suspect: " + filename);
            return false;
        }
        
        Path filePath = Paths.get(uploadDir, filename).normalize();
        
        // Sécurité : vérifier que le fichier est bien dans uploadDir
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!filePath.startsWith(uploadPath)) {
            log.severe("Tentative de suppression de fichier en dehors du répertoire autorisé!");
            return false;
        }
        
        return Files.deleteIfExists(filePath);
    } catch (IOException e) {
        log.severe("Erreur lors de la suppression du fichier: " + e.getMessage());
        return false;
    }
}
```

### 10. **Pas de limitation du nombre de fichiers par utilisateur**

**Problème** : Un utilisateur pourrait uploader des milliers de fichiers et saturer le disque.

**Solution** : Ajouter une limite par utilisateur/conversation :
```java
@Value("${app.upload.max-files-per-user:100}")
private int maxFilesPerUser;

public String saveImageFile(MultipartFile file, Integer userId) throws Exception {
    // Vérifier le nombre de fichiers existants
    if (userId != null) {
        long fileCount = countFilesByUser(userId);
        if (fileCount >= maxFilesPerUser) {
            throw new IllegalArgumentException("Limite de fichiers atteinte pour cet utilisateur");
        }
    }
    // ... reste du code
}
```

---

## 🟢 AMÉLIORATIONS MINEURES

### 11. **Logs avec informations sensibles**

**Localisation** : `FileUploadService.saveImageFile()` ligne 66

**Problème** :
```java
log.info("Fichier sauvegardé: " + filePath.toString());
```

Le chemin complet pourrait contenir des informations système.

**Solution** : Logger uniquement le nom du fichier ou masquer le chemin :
```java
log.info("Fichier sauvegardé avec succès: {}", uniqueFilename);
```

### 12. **Méthode getFullUrl() peu utilisée**

**Localisation** : `FileUploadService.getFullUrl()` ligne 140

**Problème** : La méthode existe mais n'est jamais appelée dans le code.

**Recommandation** : 
- Soit l'utiliser dans `saveImageFile()` pour retourner l'URL complète
- Soit la supprimer si non nécessaire

### 13. **Pas de compression d'images**

**Problème** : Les images sont stockées telles quelles, sans optimisation.

**Recommandation** : Ajouter une compression optionnelle :
```java
public String saveImageFile(MultipartFile file, boolean compress) throws Exception {
    // Sauvegarder
    // Si compress == true, réduire la qualité/taille
}
```

### 14. **Configuration hardcodée dans le code**

**Localisation** : `FileUploadService.saveImageFile()` ligne 69

**Problème** :
```java
return "/files/images/" + uniqueFilename;  // Hardcodé
```

**Solution** :
```properties
app.upload.url-prefix=/files/images
```

```java
@Value("${app.upload.url-prefix:/files/images}")
private String urlPrefix;

return urlPrefix + "/" + uniqueFilename;
```

---

## 🔐 SÉCURITÉ

### Failles de Sécurité Identifiées

| # | Faible | Gravité | Description |
|---|--------|---------|-------------|
| 1 | Validation Magic Number | **HAUTE** | Pas de vérification des signatures de fichiers |
| 2 | Path Traversal | **MOYENNE** | `deleteFile()` peut être vulnérable |
| 3 | Configuration incohérente | **MOYENNE** | PDF autorisé mais rejeté par MIME |
| 4 | Pas de rate limiting | **MOYENNE** | Pas de limite d'upload par utilisateur |
| 5 | Pas de scan antivirus | **MOYENNE** | Aucune vérification de virus |

### Recommandations Sécurité

1. ✅ **Valider les Magic Numbers** (Priorité 1)
2. ✅ **Sanitizer les noms de fichiers** (Priorité 1)
3. ✅ **Protection Path Traversal dans deleteFile** (Priorité 1)
4. ✅ **Retirer PDF des types autorisés** (Priorité 2)
5. ✅ **Ajouter rate limiting** (Priorité 2)
6. ⚠️ **Scanner les fichiers uploadés** (Priorité 3 - optionnel)

---

## 📊 Comparaison Configuration vs Validation

| Configuration | Valeur Spring | Valeur Custom | Problème |
|--------------|---------------|---------------|----------|
| Taille max fichier | 30MB | ~86MB | **Incohérence** |
| Taille max requête | 30MB | - | OK |
| Types autorisés | - | jpg,jpeg,png,gif,webp,pdf | PDF autorisé mais rejeté |
| Validation MIME | - | image/* uniquement | **Conflit avec PDF** |

---

## 🔧 Code Amélioré - FileUploadService

```java
@Log
@Service
public class FileUploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-size}")
    private long maxFileSize;

    @Value("${app.upload.allowed-types}")
    private String allowedTypes;

    @Value("${app.upload.url-prefix:/files/images}")
    private String urlPrefix;

    @Value("${app.base-url}")
    private String baseUrl;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        // Normaliser le chemin
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), uploadDir);
        }
        this.uploadPath = path.toAbsolutePath().normalize();
        
        // Créer le répertoire
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            log.severe("Impossible de créer le répertoire d'upload: " + uploadPath);
            throw new RuntimeException("Erreur d'initialisation du service d'upload", e);
        }
        
        log.info("Répertoire d'upload configuré: " + uploadPath);
    }

    public String saveImageFile(MultipartFile file) throws Exception {
        validateFile(file);

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        validateFileSignature(file, extension);
        
        String uniqueFilename = generateUniqueFilename(extension);
        Path filePath = uploadPath.resolve(uniqueFilename).normalize();
        
        // Sécurité : vérifier que le fichier reste dans uploadPath
        if (!filePath.startsWith(uploadPath)) {
            throw new SecurityException("Tentative de sauvegarde en dehors du répertoire autorisé");
        }
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Fichier sauvegardé: {}", uniqueFilename);
        
        return urlPrefix + "/" + uniqueFilename;
    }

    private void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Le fichier est trop volumineux. Taille max: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Le nom du fichier est null");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(allowedTypes.split(","));
        
        // Retirer les espaces
        allowedExtensions = allowedExtensions.stream()
            .map(String::trim)
            .filter(ext -> !ext.isEmpty())
            .collect(Collectors.toList());

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Types autorisés: " + allowedTypes);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image (type MIME: image/*)");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Le nom du fichier est null");
        }
        // Retirer les chemins relatifs
        filename = Paths.get(filename).getFileName().toString();
        // Limiter la longueur
        if (filename.length() > 255) {
            filename = filename.substring(0, 255);
        }
        return filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Le nom du fichier est null");
        }
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            throw new IllegalArgumentException("Le fichier doit avoir une extension valide");
        }
        String ext = filename.substring(lastDot + 1).toLowerCase();
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("L'extension du fichier est vide");
        }
        return ext;
    }

    private void validateFileSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new IllegalArgumentException("Le fichier est trop petit pour être une image valide");
            }
        }
        
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                if (header[0] != (byte)0xFF || header[1] != (byte)0xD8 || header[2] != (byte)0xFF) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image JPEG valide");
                }
                break;
            case "png":
                if (header[0] != (byte)0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image PNG valide");
                }
                break;
            case "gif":
                if (header[0] != 0x47 || header[1] != 0x49 || header[2] != 0x46 || header[3] != 0x38) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image GIF valide");
                }
                break;
            case "webp":
                if (header.length < 12) {
                    throw new IllegalArgumentException("Le fichier est trop petit pour être une image WebP");
                }
                String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
                String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
                if (!"RIFF".equals(riff) || !"WEBP".equals(webp)) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image WebP valide");
                }
                break;
        }
    }

    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }
        
        try {
            String filename = extractFilename(fileUrl);
            
            // Validation sécurité
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warning("Tentative de suppression avec nom de fichier suspect: " + filename);
                return false;
            }
            
            Path filePath = uploadPath.resolve(filename).normalize();
            
            // Sécurité : vérifier que le fichier est bien dans uploadPath
            if (!filePath.startsWith(uploadPath)) {
                log.severe("Tentative de suppression de fichier en dehors du répertoire autorisé: " + filename);
                return false;
            }
            
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Fichier supprimé: {}", filename);
            }
            return deleted;
            
        } catch (IOException e) {
            log.severe("Erreur lors de la suppression du fichier: " + e.getMessage());
            return false;
        }
    }
    
    private String extractFilename(String fileUrl) {
        int lastSlash = fileUrl.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlash + 1);
        }
        return fileUrl;
    }

    public String getFullUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return null;
        }
        if (relativeUrl.startsWith("http")) {
            return relativeUrl;
        }
        return baseUrl + relativeUrl;
    }
}
```

---

## 📋 Checklist de Corrections

### Priorité 1 (Sécurité)
- [ ] Ajouter validation des Magic Numbers
- [ ] Protéger contre Path Traversal dans deleteFile
- [ ] Sanitizer les noms de fichiers
- [ ] Aligner les limites de taille (Spring vs Custom)

### Priorité 2 (Fonctionnalité)
- [ ] Retirer PDF des types autorisés ou adapter la validation MIME
- [ ] Ajouter rollback des fichiers en cas d'échec
- [ ] Normaliser les chemins (absolu vs relatif)
- [ ] Gérer les extensions vides

### Priorité 3 (Améliorations)
- [ ] Ajouter rate limiting
- [ ] Ajouter compression d'images
- [ ] Améliorer les logs
- [ ] Configuration via properties au lieu de hardcodé

---

## 📈 Score Global Upload

| Critère | Score | Commentaire |
|---------|-------|-------------|
| **Sécurité** | 4/10 | **Manque validation Magic Numbers, Path Traversal** |
| **Fonctionnalité** | 7/10 | Fonctionne mais avec quelques bugs |
| **Robustesse** | 5/10 | Pas de rollback, gestion d'erreurs basique |
| **Configuration** | 6/10 | Incohérences entre Spring et custom |
| **Maintenabilité** | 7/10 | Code clair mais peut être amélioré |

**Score Moyen : 5.8/10** ⚠️

---

## ✅ Recommandations Finales

### Actions Immédiates (Cette semaine)
1. ✅ **Aligner les limites de taille** entre Spring et validation custom
2. ✅ **Retirer PDF** de allowed-types OU adapter validation MIME
3. ✅ **Ajouter validation Magic Numbers** pour la sécurité
4. ✅ **Ajouter rollback** des fichiers en cas d'échec de création

### Actions Court Terme (Ce mois)
5. ✅ Protéger contre Path Traversal
6. ✅ Normaliser les chemins
7. ✅ Ajouter rate limiting

### Actions Long Terme (Roadmap)
8. ⚠️ Compression d'images
9. ⚠️ Scanner antivirus
10. ⚠️ Migration vers stockage cloud (S3, etc.)

---

**Le système fonctionne mais nécessite des améliorations de sécurité critiques avant la mise en production.**

