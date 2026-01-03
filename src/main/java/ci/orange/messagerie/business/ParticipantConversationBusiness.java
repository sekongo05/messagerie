                                                                                        																				
/*
 * Java business for entity table participant_conversation 
 * Created on 2026-01-03 ( Time 17:01:33 )
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
import ci.orange.messagerie.dao.entity.ParticipantConversation;
import ci.orange.messagerie.dao.entity.User;
import ci.orange.messagerie.dao.entity.Conversation;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.*;

/**
BUSINESS for table "participant_conversation"
 * 
 * @author Geo
 *
 */
@Log
@Component
public class ParticipantConversationBusiness implements IBasicBusiness<Request<ParticipantConversationDto>, Response<ParticipantConversationDto>> {

	private Response<ParticipantConversationDto> response;
	@Autowired
	private ParticipantConversationRepository participantConversationRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ConversationRepository conversationRepository;
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

	public ParticipantConversationBusiness() {
		dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	}
	
	/**
	 * create ParticipantConversation by using ParticipantConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ParticipantConversationDto> create(Request<ParticipantConversationDto> request, Locale locale)  throws ParseException {
		log.info("----begin create ParticipantConversation-----");

		Response<ParticipantConversationDto> response = new Response<ParticipantConversationDto>();
		List<ParticipantConversation>        items    = new ArrayList<ParticipantConversation>();
			
		for (ParticipantConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("conversationId", dto.getConversationId());
			fieldsToVerify.put("userId", dto.getUserId());
			fieldsToVerify.put("deletedAt", dto.getDeletedAt());
			fieldsToVerify.put("deletedBy", dto.getDeletedBy());
			fieldsToVerify.put("recreatedAt", dto.getRecreatedAt());
			fieldsToVerify.put("recreatedBy", dto.getRecreatedBy());
			fieldsToVerify.put("leftAt", dto.getLeftAt());
			fieldsToVerify.put("leftBy", dto.getLeftBy());
			fieldsToVerify.put("definitivelyLeftAt", dto.getDefinitivelyLeftAt());
			fieldsToVerify.put("definitivelyLeftBy", dto.getDefinitivelyLeftBy());
			fieldsToVerify.put("hasLeft", dto.getHasLeft());
			fieldsToVerify.put("hasDefinitivelyLeft", dto.getHasDefinitivelyLeft());
			fieldsToVerify.put("hasCleaned", dto.getHasCleaned());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if participantConversation to insert do not exist
			ParticipantConversation existingEntity = null;

/*
			if (existingEntity != null) {
				response.setStatus(functionalError.DATA_EXIST("participantConversation id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

*/
			// Verify if user exist
			User existingUser = null;
			if (dto.getUserId() != null && dto.getUserId() > 0){
				existingUser = userRepository.findOne(dto.getUserId(), false);
				if (existingUser == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("user userId -> " + dto.getUserId(), locale));
					response.setHasError(true);
					return response;
				}
			}
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
				ParticipantConversation entityToSave = null;
			entityToSave = ParticipantConversationTransformer.INSTANCE.toEntity(dto, existingUser, existingConversation);
			entityToSave.setCreatedAt(Utilities.getCurrentDate());
			entityToSave.setCreatedBy(request.getUser());
			entityToSave.setIsDeleted(false);
//			entityToSave.setStatusId(StatusEnum.ACTIVE);
			items.add(entityToSave);
		}

		if (!items.isEmpty()) {
			List<ParticipantConversation> itemsSaved = null;
			// inserer les donnees en base de donnees
			itemsSaved = participantConversationRepository.saveAll((Iterable<ParticipantConversation>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("participantConversation", locale));
				response.setHasError(true);
				return response;
			}
			List<ParticipantConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ParticipantConversationTransformer.INSTANCE.toLiteDtos(itemsSaved) : ParticipantConversationTransformer.INSTANCE.toDtos(itemsSaved);

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

		log.info("----end create ParticipantConversation-----");
		return response;
	}

	/**
	 * update ParticipantConversation by using ParticipantConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ParticipantConversationDto> update(Request<ParticipantConversationDto> request, Locale locale)  throws ParseException {
		log.info("----begin update ParticipantConversation-----");

		Response<ParticipantConversationDto> response = new Response<ParticipantConversationDto>();
		List<ParticipantConversation>        items    = new ArrayList<ParticipantConversation>();
			
		for (ParticipantConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la participantConversation existe
			ParticipantConversation entityToSave = null;
			entityToSave = participantConversationRepository.findOne(dto.getId(), false);
			if (entityToSave == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("participantConversation id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if user exist
			if (dto.getUserId() != null && dto.getUserId() > 0){
				User existingUser = userRepository.findOne(dto.getUserId(), false);
				if (existingUser == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("user userId -> " + dto.getUserId(), locale));
					response.setHasError(true);
					return response;
				}
				entityToSave.setUser(existingUser);
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
			if (Utilities.notBlank(dto.getRecreatedAt())) {
				entityToSave.setRecreatedAt(dateFormat.parse(dto.getRecreatedAt()));
			}
			if (dto.getRecreatedBy() != null && dto.getRecreatedBy() > 0) {
				entityToSave.setRecreatedBy(dto.getRecreatedBy());
			}
			if (Utilities.notBlank(dto.getLeftAt())) {
				entityToSave.setLeftAt(dateFormat.parse(dto.getLeftAt()));
			}
			if (dto.getLeftBy() != null && dto.getLeftBy() > 0) {
				entityToSave.setLeftBy(dto.getLeftBy());
			}
			if (Utilities.notBlank(dto.getDefinitivelyLeftAt())) {
				entityToSave.setDefinitivelyLeftAt(dateFormat.parse(dto.getDefinitivelyLeftAt()));
			}
			if (dto.getDefinitivelyLeftBy() != null && dto.getDefinitivelyLeftBy() > 0) {
				entityToSave.setDefinitivelyLeftBy(dto.getDefinitivelyLeftBy());
			}
			if (dto.getHasLeft() != null) {
				entityToSave.setHasLeft(dto.getHasLeft());
			}
			if (dto.getHasDefinitivelyLeft() != null) {
				entityToSave.setHasDefinitivelyLeft(dto.getHasDefinitivelyLeft());
			}
			if (dto.getHasCleaned() != null) {
				entityToSave.setHasCleaned(dto.getHasCleaned());
			}
			entityToSave.setUpdatedAt(Utilities.getCurrentDate());
			entityToSave.setUpdatedBy(request.getUser());
			items.add(entityToSave);
		}

		if (!items.isEmpty()) {
			List<ParticipantConversation> itemsSaved = null;
			// maj les donnees en base
			itemsSaved = participantConversationRepository.saveAll((Iterable<ParticipantConversation>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("participantConversation", locale));
				response.setHasError(true);
				return response;
			}
			List<ParticipantConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ParticipantConversationTransformer.INSTANCE.toLiteDtos(itemsSaved) : ParticipantConversationTransformer.INSTANCE.toDtos(itemsSaved);

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

		log.info("----end update ParticipantConversation-----");
		return response;
	}

	/**
	 * delete ParticipantConversation by using ParticipantConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ParticipantConversationDto> delete(Request<ParticipantConversationDto> request, Locale locale)  {
		log.info("----begin delete ParticipantConversation-----");

		Response<ParticipantConversationDto> response = new Response<ParticipantConversationDto>();
		List<ParticipantConversation>        items    = new ArrayList<ParticipantConversation>();
			
		for (ParticipantConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la participantConversation existe
			ParticipantConversation existingEntity = null;

			existingEntity = participantConversationRepository.findOne(dto.getId(), false);
			if (existingEntity == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("participantConversation -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// -----------------------------------------------------------------------
			// ----------- CHECK IF DATA IS USED
			// -----------------------------------------------------------------------



			existingEntity.setDeletedAt(Utilities.getCurrentDate());
			existingEntity.setDeletedBy(request.getUser());
			existingEntity.setIsDeleted(true);
			items.add(existingEntity);
		}

		if (!items.isEmpty()) {
			// supprimer les donnees en base
			participantConversationRepository.saveAll((Iterable<ParticipantConversation>) items);

			response.setHasError(false);
		}

		log.info("----end delete ParticipantConversation-----");
		return response;
	}

	/**
	 * get ParticipantConversation by using ParticipantConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ParticipantConversationDto> getByCriteria(Request<ParticipantConversationDto> request, Locale locale)  throws Exception {
		log.info("----begin get ParticipantConversation-----");

		Response<ParticipantConversationDto> response = new Response<ParticipantConversationDto>();
		List<ParticipantConversation> items 			 = participantConversationRepository.getByCriteria(request, em, locale);

		if (items != null && !items.isEmpty()) {
			List<ParticipantConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ParticipantConversationTransformer.INSTANCE.toLiteDtos(items) : ParticipantConversationTransformer.INSTANCE.toDtos(items);

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
			response.setCount(participantConversationRepository.count(request, em, locale));
			response.setHasError(false);
		} else {
			response.setStatus(functionalError.DATA_EMPTY("participantConversation", locale));
			response.setHasError(false);
			return response;
		}

		log.info("----end get ParticipantConversation-----");
		return response;
	}

	/**
	 * get full ParticipantConversationDto by using ParticipantConversation as object.
	 * 
	 * @param dto
	 * @param size
	 * @param isSimpleLoading
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	private ParticipantConversationDto getFullInfos(ParticipantConversationDto dto, Integer size, Boolean isSimpleLoading, Locale locale) throws Exception {
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
