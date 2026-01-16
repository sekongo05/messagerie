

/*
 * Java controller for entity table message 
 * Created on 2026-01-03 ( Time 17:01:33 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2017 Savoir Faire Linux. All Rights Reserved.
 */

package ci.orange.messagerie.rest.api;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.enums.FunctionalityEnum;
import ci.orange.messagerie.business.*;
import ci.orange.messagerie.rest.fact.ControllerFactory;
import ci.orange.messagerie.service.FileUploadService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
Controller for table "message"
 * 
 * @author SFL Back-End developper
 *
 */
@Log

@CrossOrigin("*")
@RestController
@RequestMapping(value="/message")
public class MessageController {

	@Autowired
    private ControllerFactory<MessageDto> controllerFactory;
	@Autowired
	private MessageBusiness messageBusiness;
	@Autowired
	private FileUploadService fileUploadService;
	@Autowired
	private FunctionalError functionalError;
	@Autowired
	private HttpServletRequest requestBasic;

	@RequestMapping(
			value="/create",
			method=RequestMethod.POST,
			consumes = {"application/json"},
			produces={"application/json"})

    public Response<MessageDto> create(@RequestBody Request<MessageDto> request) {
    	log.info("start method /message/create");
        Response<MessageDto> response = controllerFactory.create(messageBusiness, request, FunctionalityEnum.CREATE_MESSAGE);
		log.info("end method /message/create");
        return response;
    }

	@RequestMapping(
		value = "/create-with-file",
		method = RequestMethod.POST,
		consumes = {"multipart/form-data"},
		produces = {"application/json"}
	)
	public Response<MessageDto> createWithFile(
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "conversationId", required = true) Integer conversationId,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "imgUrl", required = false) String imgUrl,
			@RequestParam(value = "typeMessage", required = false) Integer typeMessage,
			@RequestParam(value = "user", required = false) Integer user) {
		
		log.info("start method /message/create-with-file - conversationId: " + conversationId);
		
		Response<MessageDto> response = new Response<MessageDto>();
		String languageID = (String) requestBasic.getAttribute("CURRENT_LANGUAGE_IDENTIFIER");
		Locale locale = new Locale(languageID != null ? languageID : "fr", "");

		String savedImageUrl = null; // Pour rollback en cas d'échec

		try {
			// Validation conversationId
			if (conversationId == null || conversationId <= 0) {
				response.setStatus(functionalError.FIELD_EMPTY("conversationId", locale));
				response.setHasError(true);
				return response;
			}
			
			// Gestion du fichier et de l'URL
			String finalImgUrl = imgUrl;
			
			// Avertir si les deux sont fournis
			if (file != null && !file.isEmpty() && imgUrl != null && !imgUrl.isEmpty()) {
				log.warning("Les deux 'file' et 'imgUrl' sont fournis. Le fichier uploadé sera utilisé et 'imgUrl' sera ignoré.");
			}
			
			// Sauvegarder le fichier si fourni
			if (file != null && !file.isEmpty()) {
				savedImageUrl = fileUploadService.saveImageFile(file);
				finalImgUrl = savedImageUrl;
				log.info("Fichier uploadé et sauvegardé: {}"+ savedImageUrl);
			}
			
			// Validation qu'au moins content ou image est fourni
			boolean hasContent = content != null && !content.trim().isEmpty();
			boolean hasImage = finalImgUrl != null && !finalImgUrl.trim().isEmpty();
			
			if (!hasContent && !hasImage) {
				// Rollback si fichier sauvegardé
				if (savedImageUrl != null) {
					fileUploadService.deleteFile(savedImageUrl);
				}
				response.setStatus(functionalError.FIELD_EMPTY("content ou file/imgUrl (au moins un doit être fourni)", locale));
				response.setHasError(true);
				return response;
			}
			
			// Créer le DTO du message
			MessageDto messageDto = new MessageDto();
			messageDto.setConversationId(conversationId);
			if (hasImage) {
				messageDto.setImgUrl(finalImgUrl);
			}
			if (hasContent) {
				messageDto.setContent(content.trim());
			}
			if (typeMessage != null && typeMessage > 0) {
				messageDto.setTypeMessage(typeMessage);
			}
			
			// Créer la requête
			Request<MessageDto> request = new Request<MessageDto>();
			request.setUser(user);
			request.setDatas(new ArrayList<MessageDto>());
			request.getDatas().add(messageDto);
			
			// Utiliser le business existant pour créer le message
			response = controllerFactory.create(messageBusiness, request, FunctionalityEnum.CREATE_MESSAGE);
			
			// Rollback si création du message échoue
			if (response.isHasError() && savedImageUrl != null) {
				log.warning("Rollback: suppression du fichier car création du message a échoué: " + savedImageUrl);
				fileUploadService.deleteFile(savedImageUrl);
			}
			
			log.info("end method /message/create-with-file - success");
			
		} catch (IllegalArgumentException e) {
			log.severe("Erreur de validation lors de l'upload: " + e.getMessage());
			// Rollback en cas d'erreur de validation
			if (savedImageUrl != null) {
				fileUploadService.deleteFile(savedImageUrl);
			}
			response.setHasError(true);
			response.setStatus(functionalError.FILE_UPLOAD_ERROR(e.getMessage(), locale));
		} catch (IOException e) {
			log.severe("Erreur d'écriture fichier: " + e.getMessage());
			// Rollback en cas d'erreur IO
			if (savedImageUrl != null) {
				fileUploadService.deleteFile(savedImageUrl);
			}
			response.setHasError(true);
			response.setStatus(functionalError.FILE_UPLOAD_ERROR(e.getMessage(), locale));
		} catch (Exception e) {
			log.severe("Erreur lors de l'upload: " + e.getMessage());
			// Rollback en cas d'erreur inattendue
			if (savedImageUrl != null) {
				fileUploadService.deleteFile(savedImageUrl);
			}
			response.setHasError(true);
			response.setStatus(functionalError.FILE_UPLOAD_ERROR(e.getMessage(), locale));
		}
		
		return response;
	}

	@RequestMapping(value="/update",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<MessageDto> update(@RequestBody Request<MessageDto> request) {
    	log.info("start method /message/update");
        Response<MessageDto> response = controllerFactory.update(messageBusiness, request, FunctionalityEnum.UPDATE_MESSAGE);
		log.info("end method /message/update");
        return response;
    }

	@RequestMapping(value="/delete",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<MessageDto> delete(@RequestBody Request<MessageDto> request) {
    	log.info("start method /message/delete");
        Response<MessageDto> response = controllerFactory.delete(messageBusiness, request, FunctionalityEnum.DELETE_MESSAGE);
		log.info("end method /message/delete");
        return response;
    }

	@RequestMapping(value="/getByCriteria",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<MessageDto> getByCriteria(@RequestBody Request<MessageDto> request) {
    	log.info("start method /message/getByCriteria");
        Response<MessageDto> response = controllerFactory.getByCriteria(messageBusiness, request, FunctionalityEnum.VIEW_MESSAGE);
		log.info("end method /message/getByCriteria");
        return response;
    }

		@RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = {"multipart/form-data"}, produces = {"application/json"})
		public Response<MessageDto> uploadMessageWithImage(
				@RequestParam("file") MultipartFile file,
				@RequestParam(value = "conversationId", required = true) Integer conversationId,
				@RequestParam(value = "content", required = false) String content,
				@RequestParam(value = "typeMessage", required = false) Integer typeMessage,
				@RequestParam(value = "user", required = false) Integer user) {

			log.info("start method /message/upload - conversationId: " + conversationId);

			Response<MessageDto> response = new Response<MessageDto>();
			String languageID = (String) requestBasic.getAttribute("CURRENT_LANGUAGE_IDENTIFIER");
			Locale locale = new Locale(languageID != null ? languageID : "fr", "");

			try {
				// Sauvegarder le fichier
				String imageUrl = fileUploadService.saveImageFile(file);

				// Créer le DTO du message
				MessageDto messageDto = new MessageDto();
				messageDto.setConversationId(conversationId);
				messageDto.setImgUrl(imageUrl);
				messageDto.setContent(content); // Peut être null si seulement image
				messageDto.setTypeMessage(typeMessage);

				// Créer la requête
				Request<MessageDto> request = new Request<MessageDto>();
				request.setUser(user);
				request.setDatas(new ArrayList<MessageDto>());
				request.getDatas().add(messageDto);

				// Utiliser le business existant pour créer le message
				response = controllerFactory.create(messageBusiness, request, FunctionalityEnum.CREATE_MESSAGE);

				log.info("end method /message/upload - success");

			} catch (IllegalArgumentException e) {
				log.severe("Erreur de validation lors de l'upload: " + e.getMessage());
				response.setHasError(true);
				response.setStatus(functionalError.FILE_UPLOAD_ERROR(e.getMessage(), locale));
			} catch (Exception e) {
				log.severe("Erreur lors de l'upload: " + e.getMessage());
				response.setHasError(true);
				response.setStatus(functionalError.FILE_UPLOAD_ERROR(e.getMessage(), locale));
			}

			return response;
		}
}
