
/*
 * Service de gestion de l'upload de fichiers
 * Created on 2026-01-08
 */

package ci.orange.messagerie.service;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        // Normaliser le chemin pour éviter les problèmes de chemin relatif
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            // Si chemin relatif, utiliser le répertoire de travail comme base
            path = Paths.get(System.getProperty("user.dir"), uploadDir);
        }
        this.uploadPath = path.toAbsolutePath().normalize();
        
        // Créer le répertoire s'il n'existe pas
        try {
            Files.createDirectories(uploadPath);
            log.info("Répertoire d'upload configuré: " + uploadPath);
        } catch (IOException e) {
            log.severe("Impossible de créer le répertoire d'upload: " + uploadPath);
            throw new RuntimeException("Erreur d'initialisation du service d'upload", e);
        }
    }

    /**
     * Sauvegarde un fichier image uploadé
     * 
     * @param file Le fichier multipart
     * @return L'URL relative du fichier sauvegardé
     * @throws Exception Si l'upload échoue
     */
    public String saveImageFile(MultipartFile file) throws Exception {
        // Validation du fichier
        validateFile(file);

        // Sanitizer et extraire l'extension
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        
        // Valider la signature magique du fichier
        validateFileSignature(file, extension);

        // Générer un nom de fichier unique
        String uniqueFilename = generateUniqueFilename(extension);

        // Sauvegarder le fichier avec protection contre Path Traversal
        Path filePath = uploadPath.resolve(uniqueFilename).normalize();
        
        // Sécurité : vérifier que le fichier reste dans uploadPath
        if (!filePath.startsWith(uploadPath)) {
            throw new SecurityException("Tentative de sauvegarde en dehors du répertoire autorisé");
        }
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Fichier sauvegardé avec succès: {}"+ uniqueFilename);

        // Retourner l'URL relative
        return urlPrefix + "/" + uniqueFilename;
    }

    /**
     * Valide le fichier uploadé
     */
    private void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        // Vérifier la taille
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Le fichier est trop volumineux. Taille max: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Vérifier le type de fichier
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Le nom du fichier est null");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        
        // Nettoyer et valider les extensions autorisées
        List<String> allowedExtensions = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(ext -> !ext.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Types autorisés: " + allowedTypes);
        }

        // Vérifier le type MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image (type MIME: image/*)");
        }
    }

    /**
     * Sanitize le nom de fichier pour éviter les injections
     */
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
        // Retirer les caractères dangereux
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return filename;
    }

    /**
     * Valide la signature magique (Magic Numbers) du fichier pour sécurité
     */
    private void validateFileSignature(MultipartFile file, String extension) throws IOException, IllegalArgumentException {
        // Lire les premiers bytes pour vérifier la signature
        // Utiliser getBytes() pour éviter les problèmes de stream non réinitialisable
        byte[] fileBytes = file.getBytes();
        if (fileBytes.length < 4) {
            throw new IllegalArgumentException("Le fichier est trop petit pour être une image valide");
        }
        
        // Vérifier les signatures magiques selon l'extension
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                // JPEG: FF D8 FF
                if (fileBytes[0] != (byte)0xFF || fileBytes[1] != (byte)0xD8 || fileBytes[2] != (byte)0xFF) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image JPEG valide");
                }
                break;
                
            case "png":
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                if (fileBytes[0] != (byte)0x89 || fileBytes[1] != 0x50 || fileBytes[2] != 0x4E || fileBytes[3] != 0x47) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image PNG valide");
                }
                break;
                
            case "gif":
                // GIF: 47 49 46 38 (GIF8)
                if (fileBytes[0] != 0x47 || fileBytes[1] != 0x49 || fileBytes[2] != 0x46 || fileBytes[3] != 0x38) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image GIF valide");
                }
                break;
                
            case "webp":
                // WEBP: RIFF...WEBP (12 bytes minimum)
                if (fileBytes.length < 12) {
                    throw new IllegalArgumentException("Le fichier est trop petit pour être une image WebP");
                }
                String riff = new String(fileBytes, 0, 4, StandardCharsets.US_ASCII);
                String webp = new String(fileBytes, 8, 4, StandardCharsets.US_ASCII);
                if (!"RIFF".equals(riff) || !"WEBP".equals(webp)) {
                    throw new IllegalArgumentException("Le fichier n'est pas une image WebP valide");
                }
                break;
                
            default:
                log.warning("Extension non supportée pour validation Magic Number: " + extension);
                break;
        }
    }

    /**
     * Récupère l'extension d'un fichier
     */
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

    /**
     * Génère un nom de fichier unique
     */
    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension;
    }

    /**
     * Supprime un fichier avec protection contre Path Traversal
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }
        
        try {
            String filename = extractFilename(fileUrl);
            
            // Validation sécurité : vérifier les caractères dangereux
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warning("Tentative de suppression avec nom de fichier suspect: " + filename);
                return false;
            }
            
            // Construire le chemin et le normaliser
            Path filePath = uploadPath.resolve(filename).normalize();
            
            // Sécurité : vérifier que le fichier est bien dans uploadPath
            if (!filePath.startsWith(uploadPath)) {
                log.severe("Tentative de suppression de fichier en dehors du répertoire autorisé: " + filename);
                return false;
            }
            
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Fichier supprimé avec succès: {}" + filename);
            }
            return deleted;
            
        } catch (IOException e) {
            log.severe("Erreur lors de la suppression du fichier: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrait le nom de fichier d'une URL
     */
    private String extractFilename(String fileUrl) {
        int lastSlash = fileUrl.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlash + 1);
        }
        return fileUrl;
    }

    /**
     * Retourne l'URL complète d'un fichier
     */
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

