
/*
 * Configuration pour l'upload de fichiers
 * Created on 2026-01-08
 */

package ci.orange.messagerie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Normaliser le chemin pour éviter les problèmes de chemin relatif
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), uploadDir);
        }
        String normalizedPath = path.toAbsolutePath().normalize().toString();
        
        registry.addResourceHandler("/files/images/**")
                .addResourceLocations("file:" + normalizedPath + "/");
    }
}

