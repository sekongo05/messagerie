

/*
 * Java controller for entity table participant_conversation 
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.enums.FunctionalityEnum;
import ci.orange.messagerie.business.*;
import ci.orange.messagerie.rest.fact.ControllerFactory;

/**
Controller for table "participant_conversation"
 * 
 * @author SFL Back-End developper
 *
 */
@Log
@CrossOrigin("*")
@RestController
@RequestMapping(value="/participantConversation")
public class ParticipantConversationController {

	@Autowired
    private ControllerFactory<ParticipantConversationDto> controllerFactory;
	@Autowired
	private ParticipantConversationBusiness participantConversationBusiness;
	@Autowired
	private HttpServletRequest requestBasic;

	@RequestMapping(value="/create",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> create(@RequestBody Request<ParticipantConversationDto> request) {
    	log.info("start method /participantConversation/create");
        Response<ParticipantConversationDto> response = controllerFactory.create(participantConversationBusiness, request, FunctionalityEnum.CREATE_PARTICIPANT_CONVERSATION);
		log.info("end method /participantConversation/create");
        return response;
    }

	@RequestMapping(value="/update",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> update(@RequestBody Request<ParticipantConversationDto> request) {
    	log.info("start method /participantConversation/update");
        Response<ParticipantConversationDto> response = controllerFactory.update(participantConversationBusiness, request, FunctionalityEnum.UPDATE_PARTICIPANT_CONVERSATION);
		log.info("end method /participantConversation/update");
        return response;
    }

	@RequestMapping(value="/delete",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> delete(@RequestBody Request<ParticipantConversationDto> request) {
    	log.info("start method /participantConversation/delete");
        Response<ParticipantConversationDto> response = controllerFactory.delete(participantConversationBusiness, request, FunctionalityEnum.DELETE_PARTICIPANT_CONVERSATION);
		log.info("end method /participantConversation/delete");
        return response;
    }

	@RequestMapping(value="/promoteAdmin",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> promoteAdmin(@RequestBody Request<ParticipantConversationDto> request) throws Exception {
    	log.info("start method /participantConversation/promoteAdmin");
        String languageID = (String) requestBasic.getAttribute("CURRENT_LANGUAGE_IDENTIFIER");
        if (languageID == null) {
            languageID = "fr";
        }
        Locale locale = new Locale(languageID, "");
        Response<ParticipantConversationDto> response = participantConversationBusiness.promoteAdmin(request, locale);
		log.info("end method /participantConversation/promoteAdmin");
        return response;
    }

	@RequestMapping(value="/leaveGroup",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> leaveGroup(@RequestBody Request<ParticipantConversationDto> request) throws Exception {
    	log.info("start method /participantConversation/leaveGroup");
        String languageID = (String) requestBasic.getAttribute("CURRENT_LANGUAGE_IDENTIFIER");
        if (languageID == null) {
            languageID = "fr";
        }
        Locale locale = new Locale(languageID, "");
        Response<ParticipantConversationDto> response = participantConversationBusiness.leaveGroup(request, locale);
		log.info("end method /participantConversation/leaveGroup");
        return response;
    }

	@RequestMapping(value="/getByCriteria",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<ParticipantConversationDto> getByCriteria(@RequestBody Request<ParticipantConversationDto> request) {
    	log.info("start method /participantConversation/getByCriteria");
        Response<ParticipantConversationDto> response = controllerFactory.getByCriteria(participantConversationBusiness, request, FunctionalityEnum.VIEW_PARTICIPANT_CONVERSATION);
		log.info("end method /participantConversation/getByCriteria");
        return response;
    }
}
