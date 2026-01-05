                                                            													
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
import ci.orange.messagerie.dao.entity.ParticipantConversation;
import ci.orange.messagerie.dao.entity.HistoriqueSuppressionMessage;
import ci.orange.messagerie.dao.entity.User;
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
	private ParticipantConversationRepository participantConversationRepository;
	@Autowired
	private UserRepository userRepository;
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
//			fieldsToVerify.put("deletedAt", dto.getDeletedAt());
//			fieldsToVerify.put("deletedBy", dto.getDeletedBy());
//			fieldsToVerify.put("typeMessage", dto.getTypeMessage());
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
			
			// Pour une conversation privée, seuls le créateur et l'interlocuteur peuvent envoyer des messages

			if (existingConversation != null && existingConversation.getTypeConversation() != null) {
				String typeCode = existingConversation.getTypeConversation().getCode();
				boolean isPrivate = typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode));
				
				if (isPrivate) {
					// Pour une conversation privée, vérifier que l'utilisateur est soit le créateur, soit l'interlocuteur
					Integer currentUserId = request.getUser();
					Integer createurId = existingConversation.getCreatedBy();
					
					if (currentUserId == null) {
						response.setStatus(functionalError.FIELD_EMPTY("user (utilisateur connecté)", locale));
						response.setHasError(true);
						return response;
					}
					
					// Récupérer tous les participants de la conversation privée
					// Dans une conversation privée, il ne doit y avoir que 2 participants
					List<ParticipantConversation> participants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
					
					if (participants == null || participants.isEmpty()) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Aucun participant trouvé dans cette conversation privée", locale));
						response.setHasError(true);
						return response;
					}
					
					// Vérifier qu'il y a exactement 2 participants dans une conversation privée
					if (participants.size() != 2) {
						log.warning("Conversation privée id=" + existingConversation.getId() + " a " + participants.size() + " participants au lieu de 2. Cela pourrait indiquer une erreur de données.");
					}
					
					// Vérifier que l'utilisateur actuel est un des deux participants
					boolean isAuthorized = false;
					
					for (ParticipantConversation participant : participants) {
						if (participant.getUser() != null && participant.getUser().getId() != null 
								&& participant.getUser().getId().equals(currentUserId)) {
							isAuthorized = true;
							break;
						}
					}
					
					if (!isAuthorized) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Vous n'êtes pas autorisé à envoyer des messages dans cette conversation privée. Seuls les deux participants (créateur et interlocuteur) peuvent converser.", locale));
						response.setHasError(true);
						return response;
					}
					
					log.info("Vérification des permissions : L'utilisateur " + currentUserId + " est autorisé à envoyer un message dans la conversation privée id=" + existingConversation.getId());
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

	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<MessageDto> delete(Request<MessageDto> request, Locale locale)  {
		log.info("----begin delete Message (suppression unilatérale)-----");

		Response<MessageDto> response = new Response<MessageDto>();
		Integer currentUserId = request.getUser();
		
		if (currentUserId == null || currentUserId <= 0) {
			response.setStatus(functionalError.FIELD_EMPTY("user (utilisateur connecté)", locale));
			response.setHasError(true);
			return response;
		}
		
		List<HistoriqueSuppressionMessage> historiquesToSave = new ArrayList<>();
			
		for (MessageDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si le message existe
			Message existingMessage = messageRepository.findOne(dto.getId(), false);
			if (existingMessage == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("message -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}


			// VÉRIFICATION DES PERMISSIONS : L'utilisateur doit être participant de la conversation
			if (existingMessage.getConversation() == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("Le message n'appartient à aucune conversation", locale));
				response.setHasError(true);
				return response;
			}
			
			Conversation conversation = existingMessage.getConversation();
			List<ParticipantConversation> participants = participantConversationRepository.findByConversationId(conversation.getId(), false);
			
			// Vérifier que l'utilisateur actuel est participant de la conversation
			boolean isParticipant = false;
			User currentUser = null;
			
			if (participants != null && !participants.isEmpty()) {
				for (ParticipantConversation participant : participants) {
					if (participant.getUser() != null && participant.getUser().getId() != null 
							&& participant.getUser().getId().equals(currentUserId)) {
						isParticipant = true;
						currentUser = participant.getUser();
						break;
					}
				}
			}
			
			if (!isParticipant) {
				response.setStatus(functionalError.DATA_NOT_EXIST("Vous n'êtes pas participant de cette conversation. Vous ne pouvez pas supprimer ce message.", locale));
				response.setHasError(true);
				return response;
			}
			

			// vérification que L'utilisateur n'a pas déjà supprimé ce message
			HistoriqueSuppressionMessage existingHistorique = historiqueSuppressionMessageRepository.findByMessageIdAndUserId(
				existingMessage.getId(), currentUserId, false);
			
			if (existingHistorique != null) {
				log.info("L'utilisateur " + currentUserId + " a déjà supprimé le message id=" + existingMessage.getId() + ". Ignorer la suppression.");
				continue;
			}

			// Lorsqu'un utilisateur supprime un message, on enregistre cela dans l'historique
			HistoriqueSuppressionMessage historique = new HistoriqueSuppressionMessage();
			historique.setMessage(existingMessage);
			historique.setUser(currentUser);
			historique.setCreatedAt(Utilities.getCurrentDate());
			historique.setCreatedBy(currentUserId);
			historique.setIsDeleted(false);
			
			historiquesToSave.add(historique);
			
			log.info("Historique de suppression créé pour message id=" + existingMessage.getId() + " et utilisateur id=" + currentUserId);
		}

		// Sauvegarder tous les historiques de suppression
		if (!historiquesToSave.isEmpty()) {
			historiqueSuppressionMessageRepository.saveAll(historiquesToSave);
			response.setHasError(false);
			log.info("Nombre d'historiques de suppression créés: " + historiquesToSave.size());
		} else {
			response.setHasError(false);
			log.info("Aucun historique de suppression à créer (tous les messages étaient déjà supprimés).");
		}

		log.info("----end delete Message (suppression unilatérale)-----");
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
