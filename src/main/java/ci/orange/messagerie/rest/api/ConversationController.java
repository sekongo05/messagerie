

/*
 * Java controller for entity table conversation 
 * Created on 2026-01-03 ( Time 17:01:31 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2017 Savoir Faire Linux. All Rights Reserved.
 */

package ci.orange.messagerie.rest.api;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.enums.FunctionalityEnum;
import ci.orange.messagerie.business.*;
import ci.orange.messagerie.rest.fact.ControllerFactory;

/**
Controller for table "conversation"
 * 
 * @author SFL Back-End developper
 *
 */
@Log
@CrossOrigin("*")
@RestController
@RequestMapping(value="/conversation")
public class ConversationController {

	@Autowired
    private ControllerFactory<ConversationDto> controllerFactory;
	@Autowired
	private ConversationBusiness conversationBusiness;
	@Autowired
	private HttpServletRequest requestBasic;

	@RequestMapping(value="/create",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ConversationDto> create(@RequestBody Request<ConversationDto> request) {
    	log.info("start method /conversation/create");
        Response<ConversationDto> response = controllerFactory.create(conversationBusiness, request, FunctionalityEnum.CREATE_CONVERSATION);
		log.info("end method /conversation/create");
        return response;
    }

	@RequestMapping(value="/update",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ConversationDto> update(@RequestBody Request<ConversationDto> request) {
    	log.info("start method /conversation/update");
        Response<ConversationDto> response = controllerFactory.update(conversationBusiness, request, FunctionalityEnum.UPDATE_CONVERSATION);
		log.info("end method /conversation/update");
        return response;
    }

	@RequestMapping(value="/delete",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ConversationDto> delete(@RequestBody Request<ConversationDto> request) {
    	log.info("start method /conversation/delete");
        Response<ConversationDto> response = controllerFactory.delete(conversationBusiness, request, FunctionalityEnum.DELETE_CONVERSATION);
		log.info("end method /conversation/delete");
        return response;
    }

	@RequestMapping(value="/getByCriteria",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ConversationDto> getByCriteria(@RequestBody Request<ConversationDto> request) {
    	log.info("start method /conversation/getByCriteria");
        Response<ConversationDto> response = controllerFactory.getByCriteria(conversationBusiness, request, FunctionalityEnum.VIEW_CONVERSATION);
		log.info("end method /conversation/getByCriteria");
        return response;
    }

	@RequestMapping(value="/export", method=RequestMethod.POST, consumes = {"application/json"}, produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> exportConversations(@RequestBody Request<ConversationDto> request) throws Exception {
		log.info("start method /conversation/export");
		
		// Déterminer la locale
		String languageID = (String) requestBasic.getAttribute("CURRENT_LANGUAGE_IDENTIFIER");
		if (languageID == null) {
			languageID = "fr";
		}
		Locale locale = new Locale(languageID, "");
		
		// Appeler la méthode export du business
		java.io.ByteArrayOutputStream outputStream = conversationBusiness.export(request, locale);
		
		// Configurer les headers HTTP
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		httpHeaders.setContentDisposition(
			ContentDisposition.attachment()
				.filename("conversations_export.xlsx")
				.build()
		);
		httpHeaders.setContentLength(outputStream.size());
		
		log.info("end method /conversation/export - Fichier de " + outputStream.size() + " octets généré");
		
		return ResponseEntity.ok()
			.headers(httpHeaders)
			.body(outputStream.toByteArray());
	}
}
