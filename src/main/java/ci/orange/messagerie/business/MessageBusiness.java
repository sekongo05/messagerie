                                                            													
/*
 * Java business for entity table message 
 * Created on 2026-01-03 ( Time 17:01:32 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.business;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.enums.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.IBasicBusiness;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.dto.transformer.*;
import ci.orange.messagerie.dao.entity.Message;
import ci.orange.messagerie.dao.entity.Conversation;
import ci.orange.messagerie.dao.entity.TypeMessage;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.*;

/**
BUSINESS for table "message"
 * 
 * @author Geo
 *
 */
@Log
@Component
public class MessageBusiness implements IBasicBusiness<Request<MessageDto>, Response<MessageDto>> {

	private Response<MessageDto> response;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private HistoriqueSuppressionMessageRepository historiqueSuppressionMessageRepository;
	@Autowired
	private ConversationRepository conversationRepository;
	@Autowired
	private TypeMessageRepository typeMessage2Repository;
	@Autowired
	private FunctionalError functionalError;
	@Autowired
	private TechnicalError technicalError;
	@Autowired
	private ExceptionUtils exceptionUtils;
	@PersistenceContext
	private EntityManager em;

	private SimpleDateFormat dateFormat;
	private SimpleDateFormat dateTimeFormat;

	public MessageBusiness() {
		dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	}
	
	/**
	 * create Message by using MessageDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<MessageDto> create(Request<MessageDto> request, Locale locale)  throws ParseException {
		log.info("----begin create Message-----");

		Response<MessageDto> response = new Response<MessageDto>();
		List<Message>        items    = new ArrayList<Message>();
			
		for (MessageDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("content", dto.getContent());
			fieldsToVerify.put("imgUrl", dto.getImgUrl());
			fieldsToVerify.put("deletedAt", dto.getDeletedAt());
			fieldsToVerify.put("deletedBy", dto.getDeletedBy());
			fieldsToVerify.put("typeMessage", dto.getTypeMessage());
			fieldsToVerify.put("conversationId", dto.getConversationId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if message to insert do not exist
			Message existingEntity = null;

/*
			if (existingEntity != null) {
				response.setStatus(functionalError.DATA_EXIST("message id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

*/
			// Verify if conversation exist
			Conversation existingConversation = null;
			if (dto.getConversationId() != null && dto.getConversationId() > 0){
				existingConversation = conversationRepository.findOne(dto.getConversationId(), false);
				if (existingConversation == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("conversation conversationId -> " + dto.getConversationId(), locale));
					response.setHasError(true);
					return response;
				}
			}
			// Verify if typeMessage2 exist
			TypeMessage existingTypeMessage2 = null;
			if (dto.getTypeMessage() != null && dto.getTypeMessage() > 0){
				existingTypeMessage2 = typeMessage2Repository.findOne(dto.getTypeMessage(), false);
				if (existingTypeMessage2 == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("typeMessage2 typeMessage -> " + dto.getTypeMessage(), locale));
					response.setHasError(true);
					return response;
				}
			}
				Message entityToSave = null;
			entityToSave = MessageTransformer.INSTANCE.toEntity(dto, existingConversation, existingTypeMessage2);
			entityToSave.setCreatedAt(Utilities.getCurrentDate());
			entityToSave.setCreatedBy(request.getUser());
			entityToSave.setIsDeleted(false);
//			entityToSave.setStatusId(StatusEnum.ACTIVE);
			items.add(entityToSave);
		}

		if (!items.isEmpty()) {
			List<Message> itemsSaved = null;
			// inserer les donnees en base de donnees
			itemsSaved = messageRepository.saveAll((Iterable<Message>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("message", locale));
				response.setHasError(true);
				return response;
			}
			List<MessageDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? MessageTransformer.INSTANCE.toLiteDtos(itemsSaved) : MessageTransformer.INSTANCE.toDtos(itemsSaved);

			final int size = itemsSaved.size();
			List<String>  listOfError      = Collections.synchronizedList(new ArrayList<String>());
			itemsDto.parallelStream().forEach(dto -> {
				try {
					dto = getFullInfos(dto, size, request.getIsSimpleLoading(), locale);
				} catch (Exception e) {
					listOfError.add(e.getMessage());
					e.printStackTrace();
				}
			});
			if (Utilities.isNotEmpty(listOfError)) {
				Object[] objArray = listOfError.stream().distinct().toArray();
				throw new RuntimeException(StringUtils.join(objArray, ", "));
			}
			response.setItems(itemsDto);
			response.setHasError(false);
		}

		log.info("----end create Message-----");
		return response;
	}

	/**
	 * update Message by using MessageDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<MessageDto> update(Request<MessageDto> request, Locale locale)  throws ParseException {
		log.info("----begin update Message-----");

		Response<MessageDto> response = new Response<MessageDto>();
		List<Message>        items    = new ArrayList<Message>();
			
		for (MessageDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la message existe
			Message entityToSave = null;
			entityToSave = messageRepository.findOne(dto.getId(), false);
			if (entityToSave == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("message id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if conversation exist
			if (dto.getConversationId() != null && dto.getConversationId() > 0){
				Conversation existingConversation = conversationRepository.findOne(dto.getConversationId(), false);
				if (existingConversation == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("conversation conversationId -> " + dto.getConversationId(), locale));
					response.setHasError(true);
					return response;
				}
				entityToSave.setConversation(existingConversation);
			}
			// Verify if typeMessage2 exist
			if (dto.getTypeMessage() != null && dto.getTypeMessage() > 0){
				TypeMessage existingTypeMessage2 = typeMessage2Repository.findOne(dto.getTypeMessage(), false);
				if (existingTypeMessage2 == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("typeMessage2 typeMessage -> " + dto.getTypeMessage(), locale));
					response.setHasError(true);
					return response;
				}
				entityToSave.setTypeMessage2(existingTypeMessage2);
			}
			if (Utilities.notBlank(dto.getContent())) {
				entityToSave.setContent(dto.getContent());
			}
			if (Utilities.notBlank(dto.getImgUrl())) {
				entityToSave.setImgUrl(dto.getImgUrl());
			}
			if (Utilities.notBlank(dto.getDeletedAt())) {
				entityToSave.setDeletedAt(dateFormat.parse(dto.getDeletedAt()));
			}
			if (dto.getCreatedBy() != null && dto.getCreatedBy() > 0) {
				entityToSave.setCreatedBy(dto.getCreatedBy());
			}
			if (dto.getUpdatedBy() != null && dto.getUpdatedBy() > 0) {
				entityToSave.setUpdatedBy(dto.getUpdatedBy());
			}
			if (dto.getDeletedBy() != null && dto.getDeletedBy() > 0) {
				entityToSave.setDeletedBy(dto.getDeletedBy());
			}
			entityToSave.setUpdatedAt(Utilities.getCurrentDate());
			entityToSave.setUpdatedBy(request.getUser());
			items.add(entityToSave);
		}

		if (!items.isEmpty()) {
			List<Message> itemsSaved = null;
			// maj les donnees en base
			itemsSaved = messageRepository.saveAll((Iterable<Message>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("message", locale));
				response.setHasError(true);
				return response;
			}
			List<MessageDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? MessageTransformer.INSTANCE.toLiteDtos(itemsSaved) : MessageTransformer.INSTANCE.toDtos(itemsSaved);

			final int size = itemsSaved.size();
			List<String>  listOfError      = Collections.synchronizedList(new ArrayList<String>());
			itemsDto.parallelStream().forEach(dto -> {
				try {
					dto = getFullInfos(dto, size, request.getIsSimpleLoading(), locale);
				} catch (Exception e) {
					listOfError.add(e.getMessage());
					e.printStackTrace();
				}
			});
			if (Utilities.isNotEmpty(listOfError)) {
				Object[] objArray = listOfError.stream().distinct().toArray();
				throw new RuntimeException(StringUtils.join(objArray, ", "));
			}
			response.setItems(itemsDto);
			response.setHasError(false);
		}

		log.info("----end update Message-----");
		return response;
	}

	/**
	 * delete Message by using MessageDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<MessageDto> delete(Request<MessageDto> request, Locale locale)  {
		log.info("----begin delete Message-----");

		Response<MessageDto> response = new Response<MessageDto>();
		List<Message>        items    = new ArrayList<Message>();
			
		for (MessageDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la message existe
			Message existingEntity = null;

			existingEntity = messageRepository.findOne(dto.getId(), false);
			if (existingEntity == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("message -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// -----------------------------------------------------------------------
			// ----------- CHECK IF DATA IS USED
			// -----------------------------------------------------------------------

			// historiqueSuppressionMessage
			List<HistoriqueSuppressionMessage> listOfHistoriqueSuppressionMessage = historiqueSuppressionMessageRepository.findByMessageId(existingEntity.getId(), false);
			if (listOfHistoriqueSuppressionMessage != null && !listOfHistoriqueSuppressionMessage.isEmpty()){
				response.setStatus(functionalError.DATA_NOT_DELETABLE("(" + listOfHistoriqueSuppressionMessage.size() + ")", locale));
				response.setHasError(true);
				return response;
			}


			existingEntity.setDeletedAt(Utilities.getCurrentDate());
			existingEntity.setDeletedBy(request.getUser());
			existingEntity.setIsDeleted(true);
			items.add(existingEntity);
		}

		if (!items.isEmpty()) {
			// supprimer les donnees en base
			messageRepository.saveAll((Iterable<Message>) items);

			response.setHasError(false);
		}

		log.info("----end delete Message-----");
		return response;
	}

	/**
	 * get Message by using MessageDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<MessageDto> getByCriteria(Request<MessageDto> request, Locale locale)  throws Exception {
		log.info("----begin get Message-----");

		Response<MessageDto> response = new Response<MessageDto>();
		List<Message> items 			 = messageRepository.getByCriteria(request, em, locale);

		if (items != null && !items.isEmpty()) {
			List<MessageDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? MessageTransformer.INSTANCE.toLiteDtos(items) : MessageTransformer.INSTANCE.toDtos(items);

			final int size = items.size();
			List<String>  listOfError      = Collections.synchronizedList(new ArrayList<String>());
			itemsDto.parallelStream().forEach(dto -> {
				try {
					dto = getFullInfos(dto, size, request.getIsSimpleLoading(), locale);
				} catch (Exception e) {
					listOfError.add(e.getMessage());
					e.printStackTrace();
				}
			});
			if (Utilities.isNotEmpty(listOfError)) {
				Object[] objArray = listOfError.stream().distinct().toArray();
				throw new RuntimeException(StringUtils.join(objArray, ", "));
			}
			response.setItems(itemsDto);
			response.setCount(messageRepository.count(request, em, locale));
			response.setHasError(false);
		} else {
			response.setStatus(functionalError.DATA_EMPTY("message", locale));
			response.setHasError(false);
			return response;
		}

		log.info("----end get Message-----");
		return response;
	}

	/**
	 * get full MessageDto by using Message as object.
	 * 
	 * @param dto
	 * @param size
	 * @param isSimpleLoading
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	private MessageDto getFullInfos(MessageDto dto, Integer size, Boolean isSimpleLoading, Locale locale) throws Exception {
		// put code here

		if (Utilities.isTrue(isSimpleLoading)) {
			return dto;
		}
		if (size > 1) {
			return dto;
		}

		return dto;
	}
}
