                                                                                        																				
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
			
		// ========================================================================
		// VÉRIFICATION GLOBALE : Vérifier les conversations privées avant traitement
		// ========================================================================
		// Regrouper les participants par conversation pour vérifier les limites
		Map<Integer, List<Integer>> participantsByConversation = new HashMap<>();
		Map<Integer, Conversation> conversationsCache = new HashMap<>();
		
		for (ParticipantConversationDto dto : request.getDatas()) {
			if (dto.getConversationId() != null && dto.getUserId() != null) {
				Integer convId = dto.getConversationId();
				Integer userId = dto.getUserId();
				
				// Récupérer la conversation si pas encore en cache
				if (!conversationsCache.containsKey(convId)) {
					Conversation conv = conversationRepository.findOne(convId, false);
					if (conv != null) {
						conversationsCache.put(convId, conv);
					}
				}
				
				// Grouper les participants par conversation
				if (!participantsByConversation.containsKey(convId)) {
					participantsByConversation.put(convId, new ArrayList<>());
				}
				participantsByConversation.get(convId).add(userId);
			}
		}
		
		// Vérifier les limites pour chaque conversation privée
		for (Map.Entry<Integer, List<Integer>> entry : participantsByConversation.entrySet()) {
			Integer convId = entry.getKey();
			Conversation conv = conversationsCache.get(convId);
			
			if (conv != null && conv.getTypeConversation() != null) {
				String typeCode = conv.getTypeConversation().getCode();
				boolean isPrivate = typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode));
				
				if (isPrivate) {
					// Vérifier les participants existants
					List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(convId, false);
					int currentCount = (existingParticipants != null) ? existingParticipants.size() : 0;
					
					// Vérifier les doublons dans la requête
					Set<Integer> uniqueUserIds = new HashSet<>(entry.getValue());
					int newUniqueCount = uniqueUserIds.size();
					
					if (currentCount + newUniqueCount > 2) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Impossible d'ajouter " + newUniqueCount + " participant(s) : une conversation privée ne peut avoir que 2 participants maximum (le créateur et l'interlocuteur). Cette conversation a déjà " + currentCount + " participant(s).", locale));
						response.setHasError(true);
						return response;
					}
				}
			}
		}
		
		for (ParticipantConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("conversationId", dto.getConversationId());
			fieldsToVerify.put("userId", dto.getUserId());
//			fieldsToVerify.put("deletedAt", dto.getDeletedAt());
//			fieldsToVerify.put("deletedBy", dto.getDeletedBy());
//			fieldsToVerify.put("recreatedAt", dto.getRecreatedAt());
//			fieldsToVerify.put("recreatedBy", dto.getRecreatedBy());
//			fieldsToVerify.put("leftAt", dto.getLeftAt());
//			fieldsToVerify.put("leftBy", dto.getLeftBy());
//			fieldsToVerify.put("definitivelyLeftAt", dto.getDefinitivelyLeftAt());
//			fieldsToVerify.put("definitivelyLeftBy", dto.getDefinitivelyLeftBy());
//			fieldsToVerify.put("hasLeft", dto.getHasLeft());
//			fieldsToVerify.put("hasDefinitivelyLeft", dto.getHasDefinitivelyLeft());
//			fieldsToVerify.put("hasCleaned", dto.getHasCleaned());
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
			

			// Vérifier si l'utilisateur est déjà participant de la conversation
			if (existingConversation != null && dto.getUserId() != null) {
				List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
				
				if (existingParticipants != null && !existingParticipants.isEmpty()) {
					// Vérifier si l'utilisateur à ajouter est déjà un participant
					boolean userAlreadyParticipant = false;
					String userEmail = null;
					if (existingUser != null && existingUser.getEmail() != null) {
						userEmail = existingUser.getEmail();
					}
					
					for (ParticipantConversation participant : existingParticipants) {
						if (participant.getUser() != null && participant.getUser().getId() != null 
								&& participant.getUser().getId().equals(dto.getUserId())) {
							userAlreadyParticipant = true;
							if (userEmail == null && participant.getUser().getEmail() != null) {
								userEmail = participant.getUser().getEmail();
							}
							break;
						}
					}
					
					if (userAlreadyParticipant) {
						String message = userEmail != null 
							? "L'utilisateur avec l'email '" + userEmail + "' est déjà participant de cette conversation"
							: "Cet utilisateur est déjà participant de cette conversation";
						response.setStatus(functionalError.DATA_EXIST(message, locale));
						response.setHasError(true);
						return response;
					}
				}
			}
			
			//  Seul le créateur peut ajouter des membres à un groupe
			// Une conversation privée ne peut avoir que 2 participants maximum
			if (existingConversation != null && existingConversation.getTypeConversation() != null) {
				String typeCode = existingConversation.getTypeConversation().getCode();
				boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
				boolean isPrivate = typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode));
				
				if (isGroup) {
					// Pour un groupe, seul le créateur peut ajouter des membres
					Integer createurId = existingConversation.getCreatedBy();
					Integer currentUserId = request.getUser();
					
					if (createurId == null || currentUserId == null || !createurId.equals(currentUserId)) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Seul le créateur du groupe peut ajouter des membres. Vous n'êtes pas autorisé à ajouter des membres à ce groupe.", locale));
						response.setHasError(true);
						return response;
					}
					log.info("Vérification des permissions : L'utilisateur " + currentUserId + " (créateur du groupe " + existingConversation.getId() + ") est autorisé à ajouter des membres.");
				} else if (isPrivate) {
					// Pour une conversation privée, vérifier qu'elle n'a pas déjà 2 participants
					List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
					
					if (existingParticipants != null && existingParticipants.size() >= 2) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Impossible d'ajouter un participant : une conversation privée ne peut avoir que 2 participants maximum (le créateur et l'interlocuteur). Cette conversation a déjà " + existingParticipants.size() + " participant(s).", locale));
						response.setHasError(true);
						return response;
					}
					
					log.info("Vérification : Conversation privée id=" + existingConversation.getId() + " a actuellement " + 
							(existingParticipants != null ? existingParticipants.size() : 0) + " participant(s). Ajout autorisé.");
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
				
				//  Seul le créateur peut modifier des participants dans un groupe

				if (existingConversation.getTypeConversation() != null) {
					String typeCode = existingConversation.getTypeConversation().getCode();
					boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
					
					if (isGroup) {
						Integer createurId = existingConversation.getCreatedBy();
						Integer currentUserId = request.getUser();
						
						if (createurId == null || currentUserId == null || !createurId.equals(currentUserId)) {
							response.setStatus(functionalError.DATA_NOT_EXIST("Seul le créateur du groupe peut modifier les membres. Vous n'êtes pas autorisé à modifier des participants de ce groupe.", locale));
							response.setHasError(true);
							return response;
						}
						log.info("Vérification des permissions : L'utilisateur " + currentUserId + " (créateur du groupe " + existingConversation.getId() + ") est autorisé à modifier des participants.");
					}
				}
				
				entityToSave.setConversation(existingConversation);
			} else {
				// Même si le conversationId n'est pas dans le DTO, vérifier la conversation existante
				Conversation currentConversation = entityToSave.getConversation();
				if (currentConversation != null && currentConversation.getTypeConversation() != null) {
					String typeCode = currentConversation.getTypeConversation().getCode();
					boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
					
					if (isGroup) {
						// Pour un groupe, seul le créateur peut modifier les participants
						Integer createurId = currentConversation.getCreatedBy();
						Integer currentUserId = request.getUser();
						
						if (createurId == null || currentUserId == null || !createurId.equals(currentUserId)) {
							response.setStatus(functionalError.DATA_NOT_EXIST("Seul le créateur du groupe peut modifier les membres. Vous n'êtes pas autorisé à modifier des participants de ce groupe.", locale));
							response.setHasError(true);
							return response;
						}
					}
				}
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
			fieldsToVerify.put("conversationId", dto.getConversationId());
			fieldsToVerify.put("userId", dto.getUserId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la participantConversation existe
			ParticipantConversation existingEntity = null;
			
			// Si l'id est fourni, chercher par id, sinon chercher par conversationId et userId
			if (dto.getId() != null && dto.getId() > 0) {
				existingEntity = participantConversationRepository.findOne(dto.getId(), false);
			} else {
				// Chercher le participant par conversationId et userId
				List<ParticipantConversation> participants = participantConversationRepository.findByConversationId(dto.getConversationId(), false);
				if (participants != null && !participants.isEmpty()) {
					for (ParticipantConversation participant : participants) {
						if (participant.getUser() != null && participant.getUser().getId() != null 
								&& participant.getUser().getId().equals(dto.getUserId())) {
							existingEntity = participant;
							break;
						}
					}
				}
			}
			
			if (existingEntity == null) {
				String errorMsg = dto.getId() != null && dto.getId() > 0 
					? "participantConversation -> " + dto.getId()
					: "participantConversation avec conversationId -> " + dto.getConversationId() + " et userId -> " + dto.getUserId();
				response.setStatus(functionalError.DATA_NOT_EXIST(errorMsg, locale));
				response.setHasError(true);
				return response;
			}
			

			// Vérifier les permissions de suppression
			Conversation conversation = existingEntity.getConversation();
			if (conversation != null && conversation.getTypeConversation() != null) {
				Integer currentUserId = request.getUser();
				Integer participantUserId = dto.getUserId();
				Integer createurId = conversation.getCreatedBy();
				String typeCode = conversation.getTypeConversation().getCode();
				boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
				
				// Autoriser l'auto-suppression (peu importe le type de conversation)
				boolean isSelfDeletion = currentUserId != null && participantUserId != null && currentUserId.equals(participantUserId);
				
				// Pour les groupes, le créateur peut aussi supprimer d'autres participants
				boolean isCreatorDeleting = isGroup && createurId != null && currentUserId != null && createurId.equals(currentUserId);
				
				if (!isSelfDeletion && !isCreatorDeleting) {
					String errorMsg = isGroup 
						? "Seul le créateur du groupe peut supprimer des membres"
						: "Vous ne pouvez supprimer que vous-même de cette conversation.";
					response.setStatus(functionalError.DATA_NOT_EXIST(errorMsg, locale));
					response.setHasError(true);
					return response;
				}
				
				if (isSelfDeletion) {
					log.info("Auto-suppression : L'utilisateur " + currentUserId + " se retire de la conversation " + conversation.getId());
				} else if (isCreatorDeleting) {
					log.info("Suppression par créateur : L'utilisateur " + currentUserId + " (créateur du groupe " + conversation.getId() + ") supprime le participant " + participantUserId);
				}
				
				// Vérifier si le participant qui        part est admin et promouvoir automatiquement un autre admin si nécessaire
				if (isGroup && Boolean.TRUE.equals(existingEntity.getIsAdmin())) {
					// Récupérer tous les participants de la conversation
					List<ParticipantConversation> allParticipants = participantConversationRepository.findByConversationId(conversation.getId(), false);
					
					if (allParticipants != null && !allParticipants.isEmpty()) {
						// Compter les admins restants (en excluant celui qui part)
						int adminCount = 0;
						for (ParticipantConversation participant : allParticipants) {
							if (!participant.getId().equals(existingEntity.getId()) 
									&& Boolean.TRUE.equals(participant.getIsAdmin())) {
								adminCount++;
							}
						}
						
						// S'il n'y a plus d'admin, promouvoir automatiquement le participant le plus ancien
						if (adminCount == 0) {
							ParticipantConversation oldestParticipant = null;
							Date oldestDate = null;
							
							for (ParticipantConversation participant : allParticipants) {
								// Exclure celui qui part
								if (participant.getId().equals(existingEntity.getId())) {
									continue;
								}
								
								// Trouver le participant avec le plus ancien createdAt
								if (participant.getCreatedAt() != null) {
									if (oldestDate == null || participant.getCreatedAt().before(oldestDate)) {
										oldestDate = participant.getCreatedAt();
										oldestParticipant = participant;
									}
								}
							}
							
							if (oldestParticipant != null) {
								// Promouvoir le participant le plus ancien comme admin
								oldestParticipant.setIsAdmin(true);
								oldestParticipant.setUpdatedAt(Utilities.getCurrentDate());
								oldestParticipant.setUpdatedBy(request.getUser());
								items.add(oldestParticipant);
								
								log.info("Auto-promotion admin : Le participant userId=" + oldestParticipant.getUser().getId() 
										+ " (participantConversationId=" + oldestParticipant.getId() 
										+ ") a été automatiquement promu admin du groupe (conversationId=" + conversation.getId() 
										+ ") car le dernier admin (userId=" + participantUserId + ") quitte le groupe.");
							} else {
								log.warning("Aucun participant disponible pour promotion admin : Le dernier admin (userId=" + participantUserId 
										+ ") quitte le groupe (conversationId=" + conversation.getId() + ") mais aucun autre participant n'a été trouvé.");
							}
						}
					}
				}
			}

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
	 * Promouvoir ou rétrograder un participant en admin
	 * Seuls les admins existants peuvent effectuer cette action
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	public Response<ParticipantConversationDto> promoteAdmin(Request<ParticipantConversationDto> request, Locale locale) throws ParseException {
		log.info("----begin promoteAdmin ParticipantConversation-----");

		Response<ParticipantConversationDto> response = new Response<ParticipantConversationDto>();
		List<ParticipantConversation> items = new ArrayList<ParticipantConversation>();
		
		for (ParticipantConversationDto dto : request.getDatas()) {
			// Vérifier les paramètres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("conversationId", dto.getConversationId());
			fieldsToVerify.put("userId", dto.getUserId());
			if (dto.getIsAdmin() == null) {
				response.setStatus(functionalError.FIELD_EMPTY("isAdmin", locale));
				response.setHasError(true);
				return response;
			}
			
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Vérifier si la conversation existe
			Conversation conversation = conversationRepository.findOne(dto.getConversationId(), false);
			if (conversation == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("conversation conversationId -> " + dto.getConversationId(), locale));
				response.setHasError(true);
				return response;
			}

			// Vérifier si l'utilisateur à promouvoir/rétrograder existe
			User userToPromote = userRepository.findOne(dto.getUserId(), false);
			if (userToPromote == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("user userId -> " + dto.getUserId(), locale));
				response.setHasError(true);
				return response;
			}

			// Vérifier que l'utilisateur qui fait la requête est admin de la conversation
			Integer currentUserId = request.getUser();
			List<ParticipantConversation> allParticipants = participantConversationRepository.findByConversationId(dto.getConversationId(), false);
			boolean currentUserIsAdmin = false;
			for (ParticipantConversation participant : allParticipants) {
				if (participant.getUser() != null && participant.getUser().getId() != null 
						&& participant.getUser().getId().equals(currentUserId)) {
					if (Boolean.TRUE.equals(participant.getIsAdmin())) {
						currentUserIsAdmin = true;
					}
					break;
				}
			}

			if (!currentUserIsAdmin) {
				response.setStatus(functionalError.DATA_NOT_EXIST("Seuls les admins de la conversation peuvent promouvoir ou rétrograder un participant.", locale));
				response.setHasError(true);
				return response;
			}

			// Chercher le participant à modifier
			ParticipantConversation participantToModify = null;
			for (ParticipantConversation participant : allParticipants) {
				if (participant.getUser() != null && participant.getUser().getId() != null 
						&& participant.getUser().getId().equals(dto.getUserId())) {
					participantToModify = participant;
					break;
				}
			}

			if (participantToModify == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("Le participant avec userId -> " + dto.getUserId() + " n'existe pas dans cette conversation.", locale));
				response.setHasError(true);
				return response;
			}

			// Mettre à jour le statut admin
			participantToModify.setIsAdmin(dto.getIsAdmin());
			participantToModify.setUpdatedAt(Utilities.getCurrentDate());
			participantToModify.setUpdatedBy(currentUserId);
			items.add(participantToModify);
		}

		if (!items.isEmpty()) {
			// Mettre à jour les données en base
			List<ParticipantConversation> itemsSaved = participantConversationRepository.saveAll((Iterable<ParticipantConversation>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("participantConversation", locale));
				response.setHasError(true);
				return response;
			}
			List<ParticipantConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ParticipantConversationTransformer.INSTANCE.toLiteDtos(itemsSaved) : ParticipantConversationTransformer.INSTANCE.toDtos(itemsSaved);

			final int size = itemsSaved.size();
			List<String> listOfError = Collections.synchronizedList(new ArrayList<String>());
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

		log.info("----end promoteAdmin ParticipantConversation-----");
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
