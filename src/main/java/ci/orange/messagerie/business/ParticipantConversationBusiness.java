                                                                                        																				
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
			

			// Pour un groupe, seul un admin peut ajouter des membres
			// Une conversation privée ne peut avoir que 2 participants maximum
			if (existingConversation != null && existingConversation.getTypeConversation() != null) {
				String typeCode = existingConversation.getTypeConversation().getCode();
				boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
				boolean isPrivate = typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode));
				
				if (isGroup) {
					// Pour un groupe, seul un admin peut ajouter des membres
					Integer currentUserId = request.getUser();
					
					if (currentUserId == null) {
						response.setStatus(functionalError.FIELD_EMPTY("user (utilisateur connecté)", locale));
						response.setHasError(true);
						return response;
					}
					
					Integer createurId = existingConversation.getCreatedBy();
					
					// Vérifier que l'utilisateur actuel est admin du groupe
					List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
					boolean isCurrentUserAdmin = false;
					
					if (existingParticipants != null && !existingParticipants.isEmpty()) {
						for (ParticipantConversation participant : existingParticipants) {
							if (participant.getUser() != null && participant.getUser().getId() != null 
									&& participant.getUser().getId().equals(currentUserId)
									&& Boolean.TRUE.equals(participant.getIsAdmin())) {
								isCurrentUserAdmin = true;
								break;
							}
						}
						
						// Si pas admin mais que c'est le créateur et qu'il est déjà participant, le promouvoir
						if (!isCurrentUserAdmin && createurId != null && createurId.equals(currentUserId)) {
							for (ParticipantConversation participant : existingParticipants) {
								if (participant.getUser() != null && participant.getUser().getId() != null 
										&& participant.getUser().getId().equals(currentUserId)) {
									participant.setIsAdmin(true);
									participantConversationRepository.save(participant);
									log.info("Créateur (userId=" + currentUserId + ") promu admin du groupe id=" + existingConversation.getId());
									isCurrentUserAdmin = true;
									break;
								}
							}
						}
					}
					
					// Si aucun participant n'existe encore OU si c'est le créateur qui ajoute (même s'il n'est pas encore participant)
					if (!isCurrentUserAdmin) {
						if (createurId != null && createurId.equals(currentUserId)) {
							// Le créateur peut ajouter des membres même s'il n'est pas encore participant (première fois)
							log.info("Créateur (userId=" + currentUserId + ") autorisé à ajouter des membres au groupe id=" + existingConversation.getId() + " (première fois ou pas encore participant)");
						} else {
							// Ce n'est pas le créateur et ce n'est pas un admin
							response.setStatus(functionalError.DATA_NOT_EXIST("Seul un admin du groupe (ou le créateur) peut ajouter des membres. Vous n'êtes pas autorisé à ajouter des membres à ce groupe.", locale));
							response.setHasError(true);
							return response;
						}
					} else {
						log.info("Vérification des permissions : L'admin (userId=" + currentUserId + ") est autorisé à ajouter des membres au groupe id=" + existingConversation.getId());
					}
				} else if (isPrivate) {
					// Pour une conversation privée, vérifier qu'elle n'a pas déjà 2 participants
					List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
					
					if (existingParticipants != null && existingParticipants.size() >= 2) {
						// Vérifier si l'utilisateur à ajouter est déjà un participant
						boolean userAlreadyParticipant = false;
						if (dto.getUserId() != null) {
							for (ParticipantConversation participant : existingParticipants) {
								if (participant.getUser() != null && participant.getUser().getId() != null 
										&& participant.getUser().getId().equals(dto.getUserId())) {
									userAlreadyParticipant = true;
									break;
								}
							}
						}
						
						if (!userAlreadyParticipant) {
							response.setStatus(functionalError.DATA_NOT_EXIST("Impossible d'ajouter un participant : une conversation privée ne peut avoir que 2 participants maximum (le créateur et l'interlocuteur). Cette conversation a déjà " + existingParticipants.size() + " participant(s).", locale));
							response.setHasError(true);
							return response;
						} else {
							response.setStatus(functionalError.DATA_EXIST("Cet utilisateur est déjà participant de cette conversation privée.", locale));
							response.setHasError(true);
							return response;
						}
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
			
			// GESTION DU STATUT ADMIN
			if (existingConversation != null && existingConversation.getTypeConversation() != null) {
				String typeCode = existingConversation.getTypeConversation().getCode();
				boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
				
				if (isGroup) {
					// Pour un groupe, vérifier si le participant ajouté est le créateur
					Integer createurId = existingConversation.getCreatedBy();
					Integer participantUserId = dto.getUserId();
					
					// Si le participant est le créateur du groupe, il devient automatiquement admin
					if (createurId != null && participantUserId != null && createurId.equals(participantUserId)) {
						entityToSave.setIsAdmin(true);
						log.info("Créateur (userId=" + participantUserId + ") ajouté comme admin du groupe id=" + existingConversation.getId());
					} else {
						// Pour les autres membres, isAdmin = false par défaut (sauf si explicitement défini à true par un admin)
						if (entityToSave.getIsAdmin() == null) {
							entityToSave.setIsAdmin(false);
						}
						
						// Si isAdmin est défini à true par un admin, loguer l'action
						if (Boolean.TRUE.equals(entityToSave.getIsAdmin())) {
							log.info("Nouveau membre (userId=" + participantUserId + ") ajouté comme admin par un admin existant dans le groupe id=" + existingConversation.getId());
						}
					}
				} else {
					// Pour les conversations privées, isAdmin n'a pas de sens, donc false
					entityToSave.setIsAdmin(false);
				}
			} else {
				// Si pas de conversation, isAdmin = false par défaut
				if (entityToSave.getIsAdmin() == null) {
					entityToSave.setIsAdmin(false);
				}
			}
			
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
				
				// Pour un groupe, seul un admin peut modifier des participants
				// Gestion spéciale pour la modification du statut admin
				if (existingConversation.getTypeConversation() != null) {
					String typeCode = existingConversation.getTypeConversation().getCode();
					boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
					
					if (isGroup) {
						Integer currentUserId = request.getUser();
						
						// Si modification du statut admin, vérifier que l'utilisateur qui modifie est admin
						if (dto.getIsAdmin() != null) {
							List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(existingConversation.getId(), false);
							boolean isCurrentUserAdmin = false;
							
							if (existingParticipants != null) {
								for (ParticipantConversation participant : existingParticipants) {
									if (participant.getUser() != null && participant.getUser().getId() != null 
											&& participant.getUser().getId().equals(currentUserId)
											&& Boolean.TRUE.equals(participant.getIsAdmin())) {
										isCurrentUserAdmin = true;
										break;
									}
								}
							}
							
							if (!isCurrentUserAdmin) {
								response.setStatus(functionalError.DATA_NOT_EXIST("Seul un admin du groupe peut promouvoir ou destituer d'autres membres comme admin. Vous n'êtes pas admin de ce groupe.", locale));
								response.setHasError(true);
								return response;
							}
							
							if (Boolean.TRUE.equals(dto.getIsAdmin())) {
								log.info("Admin (userId=" + currentUserId + ") promeut le participant id=" + entityToSave.getId() + " comme admin du groupe id=" + existingConversation.getId());
							} else {
								log.info("Admin (userId=" + currentUserId + ") destitue le participant id=" + entityToSave.getId() + " du statut admin du groupe id=" + existingConversation.getId());
							}
						}
						
						log.info("Vérification des permissions : L'admin (userId=" + currentUserId + ") est autorisé à modifier des participants du groupe id=" + existingConversation.getId());
					}
				}
				
				entityToSave.setConversation(existingConversation);
			} else {
				// Même si le conversationId n'est pas dans le DTO, vérifier la conversation existante
				Conversation currentConversation = entityToSave.getConversation();
				if (currentConversation != null && currentConversation.getTypeConversation() != null) {
					String typeCode = currentConversation.getTypeConversation().getCode();
					boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
					
					if (isGroup && dto.getIsAdmin() != null) {
						// Pour un groupe, seul un admin peut modifier le statut admin
						Integer currentUserId = request.getUser();
						List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(currentConversation.getId(), false);
						boolean isCurrentUserAdmin = false;
						
						if (existingParticipants != null) {
							for (ParticipantConversation participant : existingParticipants) {
								if (participant.getUser() != null && participant.getUser().getId() != null 
										&& participant.getUser().getId().equals(currentUserId)
										&& Boolean.TRUE.equals(participant.getIsAdmin())) {
									isCurrentUserAdmin = true;
									break;
								}
							}
						}
						
						if (!isCurrentUserAdmin) {
							response.setStatus(functionalError.DATA_NOT_EXIST("Seul un admin du groupe peut promouvoir ou destituer d'autres membres comme admin.", locale));
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
			if (dto.getIsAdmin() != null) {
				entityToSave.setIsAdmin(dto.getIsAdmin());
			} else {
				// Si isAdmin n'est pas spécifié, conserver la valeur actuelle
				entityToSave.setIsAdmin(entityToSave.getIsAdmin() != null ? entityToSave.getIsAdmin() : false);
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
			

			// Pour un groupe, seul un admin peut supprimer des membres

			Conversation conversation = existingEntity.getConversation();
			if (conversation != null && conversation.getTypeConversation() != null) {
				String typeCode = conversation.getTypeConversation().getCode();
				boolean isGroup = typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode));
				
				if (isGroup) {
					// Pour un groupe, seul un admin peut supprimer des membres
					Integer currentUserId = request.getUser();
					
					if (currentUserId == null) {
						response.setStatus(functionalError.FIELD_EMPTY("user (utilisateur connecté)", locale));
						response.setHasError(true);
						return response;
					}
					
					// Vérifier que l'utilisateur actuel est admin du groupe
					List<ParticipantConversation> existingParticipants = participantConversationRepository.findByConversationId(conversation.getId(), false);
					boolean isCurrentUserAdmin = false;
					
					if (existingParticipants != null) {
						for (ParticipantConversation participant : existingParticipants) {
							if (participant.getUser() != null && participant.getUser().getId() != null 
									&& participant.getUser().getId().equals(currentUserId)
									&& Boolean.TRUE.equals(participant.getIsAdmin())) {
								isCurrentUserAdmin = true;
								break;
							}
						}
					}
					
					// Si pas admin, vérifier si c'est le créateur (pour compatibilité)
					if (!isCurrentUserAdmin) {
						Integer createurId = conversation.getCreatedBy();
						if (createurId != null && createurId.equals(currentUserId)) {
							// Le créateur n'est pas encore marqué comme admin, le promouvoir
							if (existingParticipants != null) {
								for (ParticipantConversation participant : existingParticipants) {
									if (participant.getUser() != null && participant.getUser().getId() != null 
											&& participant.getUser().getId().equals(currentUserId)) {
										participant.setIsAdmin(true);
										participantConversationRepository.save(participant);
										log.info("Créateur (userId=" + currentUserId + ") promu admin lors de la suppression d'un membre");
										isCurrentUserAdmin = true;
										break;
									}
								}
							}
						}
					}
					
					if (!isCurrentUserAdmin) {
						response.setStatus(functionalError.DATA_NOT_EXIST("Seul un admin du groupe peut supprimer des membres. Vous n'êtes pas admin de ce groupe.", locale));
						response.setHasError(true);
						return response;
					}
					
					log.info("Vérification des permissions : L'admin (userId=" + currentUserId + ") est autorisé à supprimer des membres du groupe id=" + conversation.getId());
				}
				// Pour les conversations privées, les participants peuvent se retirer eux-mêmes
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
