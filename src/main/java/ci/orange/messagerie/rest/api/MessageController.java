

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
import org.springframework.web.bind.annotation.RestController;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.enums.FunctionalityEnum;
import ci.orange.messagerie.business.*;
import ci.orange.messagerie.rest.fact.ControllerFactory;

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

	@RequestMapping(value="/create",method=RequestMethod.POST,consumes = {"application/json"},produces={"application/json"})
    public Response<MessageDto> create(@RequestBody Request<MessageDto> request) {
    	log.info("start method /message/create");
        Response<MessageDto> response = controllerFactory.create(messageBusiness, request, FunctionalityEnum.CREATE_MESSAGE);
		log.info("end method /message/create");
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
}
