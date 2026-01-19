                                                    											
/*
 * Java business for entity table conversation 
 * Created on 2026-01-03 ( Time 17:01:31 )
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
import ci.orange.messagerie.utils.ExcelExportUtil;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.IBasicBusiness;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.dto.transformer.*;
import ci.orange.messagerie.dao.entity.Conversation;
import ci.orange.messagerie.dao.entity.TypeConversation;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.*;

/**
BUSINESS for table "conversation"
 * 
 * @author Geo
 *
 */
@Log
@Component
public class ConversationBusiness implements IBasicBusiness<Request<ConversationDto>, Response<ConversationDto>> {

	private Response<ConversationDto> response;
	@Autowired
	private ConversationRepository conversationRepository;
	@Autowired
	private ParticipantConversationRepository participantConversationRepository;
	@Autowired
	private TypeConversationRepository typeConversationRepository;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private TypeMessageRepository typeMessageRepository;
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

	public ConversationBusiness() {
		dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	}
	
	/**
	 * create Conversation by using ConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ConversationDto> create(Request<ConversationDto> request, Locale locale)  throws ParseException {
		log.info("----begin create Conversation-----");

		Response<ConversationDto> response = new Response<ConversationDto>();
		List<Conversation>        items    = new ArrayList<Conversation>();
		List<ConversationDto>     itemsDto = new ArrayList<ConversationDto>();
		

		Integer createurIdGlobal = request.getUser();
		if (createurIdGlobal == null || createurIdGlobal <= 0) {
			response.setStatus(functionalError.FIELD_EMPTY("user (créateur)", locale));
			response.setHasError(true);
			return response;
		}
		
		// Valider que le créateur existe dans la base de données
		User createurVerification = validateUserExists(createurIdGlobal, locale, "user (créateur)");
		if (createurVerification == null) {
			response.setStatus(functionalError.DATA_NOT_EXIST("user createdBy -> " + createurIdGlobal + " n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être utilisé.", locale));
			response.setHasError(true);
			return response;
		}
			
		for (ConversationDto dto : request.getDatas()) {

			TypeConversation existingTypeConversation = null;
			Integer typeConversationIdToUse = dto.getTypeConversationId();
			
			// Si typeConversation est fourni en String, chercher par code
			if (Utilities.notBlank(dto.getTypeConversation())) {
				String typeCode = dto.getTypeConversation().toUpperCase().trim();
				String originalTypeCode = typeCode;
				
				// Liste des codes possibles à chercher selon le type demandé
				List<String> codesToTry = new ArrayList<>();
				
				if ("PRIVATE".equals(typeCode) || "PRIVEE".equals(typeCode) || "P".equals(typeCode)) {
					codesToTry.add("PRIVEE");
					codesToTry.add("PRIVATE");
					codesToTry.add("PRIV");
					codesToTry.add("P");
					typeCode = "PRIVEE"; // Code normalisé pour la suite
				} else if ("GROUP".equals(typeCode) || "GROUPE".equals(typeCode) || "G".equals(typeCode)) {

					codesToTry.add("GROUP");
					codesToTry.add("GROUPE");
					codesToTry.add("GRP");
					codesToTry.add("G");
					typeCode = "GROUP"; // Code normalisé pour la suite
				} else {
					codesToTry.add(originalTypeCode);
				}
				
				// Chercher le type de conversation en essayant tous les codes possibles
				for (String code : codesToTry) {
					existingTypeConversation = typeConversationRepository.findByCode(code, false);
					if (existingTypeConversation != null) {
						log.info("TypeConversation trouvé avec le code: " + code);
						typeCode = code; // Utiliser le code trouvé
						break;
					}
				}
				
				// Si toujours pas trouvé, créer automatiquement le TypeConversation
				if (existingTypeConversation == null) {
					log.info("TypeConversation avec code '" + typeCode + "' introuvable parmi les éléments actifs. Vérification des éléments supprimés...");
					
					// Déterminer le libellé approprié selon le type
					String libelle = "";
					if ("PRIVEE".equals(typeCode) || "PRIVATE".equals(typeCode)) {
						libelle = "Conversation Privée";
					} else if ("GROUP".equals(typeCode) || "GROUPE".equals(typeCode)) {
						libelle = "Groupe de Discussion";
					} else {
						libelle = "Type de Conversation " + typeCode;
					}
					
					// Vérifier si un TypeConversation avec ce code existe mais est supprimé
					TypeConversation deletedTypeConversation = typeConversationRepository.findByCode(typeCode, true);
					
					if (deletedTypeConversation != null && deletedTypeConversation.getIsDeleted() != null && deletedTypeConversation.getIsDeleted()) {
						// Le TypeConversation existe mais est supprimé : le réactiver
						log.info("TypeConversation avec code '" + typeCode + "' existe mais est supprimé. Réactivation...");
						deletedTypeConversation.setIsDeleted(false);
						deletedTypeConversation.setDeletedAt(null);
						deletedTypeConversation.setDeletedBy(null);
						deletedTypeConversation.setUpdatedAt(Utilities.getCurrentDate());
						deletedTypeConversation.setUpdatedBy(createurIdGlobal);
						if (Utilities.isBlank(deletedTypeConversation.getLibelle())) {
							deletedTypeConversation.setLibelle(libelle);
						}
						existingTypeConversation = typeConversationRepository.save(deletedTypeConversation);
						log.info("TypeConversation réactivé avec code: " + typeCode + ", id: " + existingTypeConversation.getId());
					} else {
						// Le TypeConversation n'existe pas du tout : le créer automatiquement
						log.info("TypeConversation avec code '" + typeCode + "' n'existe pas. Création automatique...");
						existingTypeConversation = new TypeConversation();
						existingTypeConversation.setCode(typeCode);
						existingTypeConversation.setLibelle(libelle);
						existingTypeConversation.setCreatedAt(Utilities.getCurrentDate());
						existingTypeConversation.setCreatedBy(createurIdGlobal);
						existingTypeConversation.setIsDeleted(false);
						
						// Sauvegarder le nouveau TypeConversation
						existingTypeConversation = typeConversationRepository.save(existingTypeConversation);
						log.info("TypeConversation créé automatiquement avec code: " + typeCode + ", libelle: " + libelle + ", id: " + existingTypeConversation.getId());
					}
				}
				
				// Mettre à jour le typeConversationId avec celui trouvé
				typeConversationIdToUse = existingTypeConversation.getId();
				dto.setTypeConversationId(typeConversationIdToUse);
			} else if (typeConversationIdToUse != null && typeConversationIdToUse > 0) {
				existingTypeConversation = typeConversationRepository.findOne(typeConversationIdToUse, false);
				if (existingTypeConversation == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("typeConversation typeConversationId -> " + typeConversationIdToUse, locale));
					response.setHasError(true);
					return response;
				}
			}
			
			// Vérification du typeConversationId (obligatoire)
			if (typeConversationIdToUse == null || typeConversationIdToUse <= 0) {
				response.setStatus(functionalError.FIELD_EMPTY("typeConversationId", locale));
				response.setHasError(true);
				return response;
			}

			//Vérification du type de conversation
			boolean isPrivateConversation = false;
			boolean isGroupConversation = false;
			User interlocuteur = null;
			Integer createurId = request.getUser();
			String titreFinal = dto.getTitre();
			
			if (existingTypeConversation != null) {
				String typeCode = existingTypeConversation.getCode();
				if (typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode))) {
					isPrivateConversation = true;
					
					// Pour une conversation privée, l'interlocuteurId est obligatoire
					if (dto.getInterlocuteurId() == null || dto.getInterlocuteurId() <= 0) {
						response.setStatus(functionalError.FIELD_EMPTY("interlocuteurId (obligatoire pour une conversation privée)", locale));
						response.setHasError(true);
						return response;
					}
					
					//  Vérifier que l'interlocuteur existe dans la BD
					interlocuteur = validateUserExists(dto.getInterlocuteurId(), locale, "interlocuteurId");
					if (interlocuteur == null) {
						response.setStatus(functionalError.DATA_NOT_EXIST("user interlocuteurId -> " + dto.getInterlocuteurId() + 
								" n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être ajouté à une conversation.", locale));
				response.setHasError(true);
				return response;
			}

					if (interlocuteur.getId().equals(createurId)) {
						response.setStatus(functionalError.DATA_EXIST("Vous ne pouvez pas créer une conversation privée avec vous-même", locale));
					response.setHasError(true);
					return response;
					}
					
					// CONVERSATION PRIVÉE : Le titre doit être automatiquement le nom de l'interlocuteur
					String nomComplet = "";
					if (Utilities.notBlank(interlocuteur.getPrenoms())) {
						nomComplet = interlocuteur.getPrenoms();
					}
					if (Utilities.notBlank(interlocuteur.getNom())) {
						nomComplet += (Utilities.notBlank(nomComplet) ? " " : "") + interlocuteur.getNom();
					}
					if (Utilities.isBlank(nomComplet)) {
						nomComplet = "Utilisateur " + interlocuteur.getId();
					}
					titreFinal = nomComplet; // Remplacer le titre par le nom de l'interlocuteur
					
					// Chercher si une conversation privée existe déjà entre ces 2 utilisateurs
					Conversation conversationExistante = participantConversationRepository
						.findPrivateConversationBetweenUsers(
							Math.min(createurId, dto.getInterlocuteurId()),
							Math.max(createurId, dto.getInterlocuteurId()),
							typeCode,
							false
						);
					
					if (conversationExistante != null) {
						//  Réutiliser la conversation existante
						log.info("Conversation privée existante trouvée entre userId=" + createurId + 
								" et userId=" + dto.getInterlocuteurId() + ", conversationId=" + conversationExistante.getId());
						
						// Convertir en DTO pour la réponse
						ConversationDto dtoExistante = ConversationTransformer.INSTANCE.toDto(conversationExistante);
						itemsDto.add(dtoExistante);
						
						// Envoyer le message dans la conversation existante (seulement si un message est fourni)
						boolean hasTextMessage = Utilities.notBlank(dto.getMessageContent());
						boolean hasImageMessage = Utilities.notBlank(dto.getMessageImgUrl());
						
						if (hasTextMessage || hasImageMessage) {
							sendInitialMessage(conversationExistante, dto.getMessageContent(), dto.getMessageImgUrl(), createurId);
						}
						
						// Passer à la conversation suivante (ne pas créer de nouvelle)
						continue;
					}
				} else {
					isGroupConversation = true;
					titreFinal = dto.getTitre(); // Garder le titre fourni pour les groupes
					
					// Pour un groupe, le titre est obligatoire
					if (Utilities.isBlank(titreFinal)) {
						response.setStatus(functionalError.FIELD_EMPTY("titre (obligatoire pour un groupe)", locale));
						response.setHasError(true);
						return response;
					}
				}
			}

			// Vérification finale : le titre doit être défini
			if (Utilities.isBlank(titreFinal)) {
				response.setStatus(functionalError.FIELD_EMPTY("titre", locale));
				response.setHasError(true);
				return response;
			}

			// Créer la nouvelle conversation avec le titre approprié
			if (Utilities.isBlank(dto.getDeletedAt())) {
				dto.setDeletedAt(null);
			}
			if (dto.getDeletedBy() == null || dto.getDeletedBy() <= 0) {
				dto.setDeletedBy(null);
			}
			
			Conversation entityToSave = null;
			dto.setTitre(titreFinal);
			entityToSave = ConversationTransformer.INSTANCE.toEntity(dto, existingTypeConversation);
			entityToSave.setCreatedAt(Utilities.getCurrentDate());
			entityToSave.setCreatedBy(createurId);
			entityToSave.setIsDeleted(false);
			items.add(entityToSave);
		}

		// Sauvegarder les nouvelles conversations
		if (!items.isEmpty()) {
			List<Conversation> itemsSaved = null;
			itemsSaved = conversationRepository.saveAll((Iterable<Conversation>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("conversation", locale));
				response.setHasError(true);
				return response;
			}
			
			// Pour chaque conversation sauvegardée, gérer les participants et messages
			for (int i = 0; i < itemsSaved.size(); i++) {
				Conversation conversationSaved = itemsSaved.get(i);
				ConversationDto dtoOriginal = request.getDatas().get(i);
				
				TypeConversation typeConversation = conversationSaved.getTypeConversation();
				boolean isPrivate = false;
				boolean isGroup = false;
				if (typeConversation != null) {
					String typeCode = typeConversation.getCode();
					if (typeCode != null && ("PRIVEE".equalsIgnoreCase(typeCode) || "PRIVATE".equalsIgnoreCase(typeCode))) {
						isPrivate = true;
					} else if (typeCode != null && ("GROUP".equalsIgnoreCase(typeCode) || "GROUPE".equalsIgnoreCase(typeCode))) {
						isGroup = true;
					}
				}
				
				if (isPrivate && dtoOriginal.getInterlocuteurId() != null) {
					// CRÉATION DES PARTICIPANTS POUR CONVERSATION PRIVÉE

					// 1. Validation du créateur : Vérifier qu'il existe dans la base de données
					Integer createurId = conversationSaved.getCreatedBy();
					User createur = validateUserExists(createurId, locale, "createdBy");
					if (createur == null) {
						response.setStatus(functionalError.DATA_NOT_EXIST("user createdBy -> " + createurId + 
								" n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être utilisé.", locale));
						response.setHasError(true);
						return response;
					}
					
					// 2. Validation de l'interlocuteur : Vérifier qu'il existe dans la base de données
					User interlocuteur = validateUserExists(dtoOriginal.getInterlocuteurId(), locale, "interlocuteurId");
					if (interlocuteur == null) {
						response.setStatus(functionalError.DATA_NOT_EXIST("user interlocuteurId -> " + dtoOriginal.getInterlocuteurId() + 
								" n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être ajouté à une conversation.", locale));
						response.setHasError(true);
						return response;
					}
					
					// 3. Vérifier que l'interlocuteur n'est pas le créateur
					if (interlocuteur.getId().equals(createurId)) {
						response.setStatus(functionalError.DATA_EXIST("Vous ne pouvez pas créer une conversation privée avec vous-même", locale));
						response.setHasError(true);
						return response;
					}
				

					
					// Vérifier qu'il n'y a pas déjà de participants dans cette conversation (sécurité)
					List<ParticipantConversation> existingParticipants = 
						participantConversationRepository.findByConversationId(conversationSaved.getId(), false);
					
					if (existingParticipants == null || existingParticipants.isEmpty()) {
							// CRÉATION DU PARTICIPANT POUR LE CRÉATEUR

							// On crée uniquement un participant qui référence cet utilisateur existant.
							ParticipantConversation participantCreateur = new ParticipantConversation();
							participantCreateur.setConversation(conversationSaved);
							participantCreateur.setUser(createur); // Référence vers l'utilisateur EXISTANT
							participantCreateur.setCreatedAt(Utilities.getCurrentDate());
							participantCreateur.setCreatedBy(conversationSaved.getCreatedBy());
							participantCreateur.setIsDeleted(false);
							participantCreateur.setHasLeft(false);
							participantCreateur.setHasDefinitivelyLeft(false);
							participantCreateur.setHasCleaned(false);
							participantCreateur.setIsAdmin(false); // Pas admin pour les conversations privées
							
							// CRÉATION DU PARTICIPANT POUR L'INTERLOCUTEUR
							// On crée uniquement un participant qui référence cet utilisateur existant.

							ParticipantConversation participantInterlocuteur = new ParticipantConversation();
							participantInterlocuteur.setConversation(conversationSaved);
							participantInterlocuteur.setUser(interlocuteur); // Référence vers l'utilisateur EXISTANT (non créé ici)
							participantInterlocuteur.setCreatedAt(Utilities.getCurrentDate());
							participantInterlocuteur.setCreatedBy(conversationSaved.getCreatedBy());
							participantInterlocuteur.setIsDeleted(false);
							participantInterlocuteur.setHasLeft(false);
							participantInterlocuteur.setHasDefinitivelyLeft(false);
							participantInterlocuteur.setHasCleaned(false);
							participantInterlocuteur.setIsAdmin(false); // Pas admin pour les conversations privées
							
							// SAUVEGARDE DES PARTICIPANTS
							List<ParticipantConversation> participantsToSave = new ArrayList<>();
							participantsToSave.add(participantCreateur);
							participantsToSave.add(participantInterlocuteur);
							participantConversationRepository.saveAll(participantsToSave);
							
							log.info("Conversation privée créée entre userId=" + createur.getId() + 
									" et userId=" + interlocuteur.getId() + ", conversationId=" + conversationSaved.getId() + 
									" (utilisateurs validés comme existants dans la base de données)");
					}
					

					// Envoyer le message initial dans la conversation privée
					boolean hasTextMessage = Utilities.notBlank(dtoOriginal.getMessageContent());
					boolean hasImageMessage = Utilities.notBlank(dtoOriginal.getMessageImgUrl());
					
					if (hasTextMessage || hasImageMessage) {
						sendInitialMessage(conversationSaved, dtoOriginal.getMessageContent(), dtoOriginal.getMessageImgUrl(), createurId);
					}
				} else if (isGroup) {
					// CRÉATION AUTOMATIQUE DU PARTICIPANT POUR LE CRÉATEUR DU GROUPE
					
					// 1. Validation du créateur : Vérifier qu'il existe dans la base de données
					Integer createurId = conversationSaved.getCreatedBy();
					User createur = validateUserExists(createurId, locale, "createdBy");
					if (createur == null) {
						response.setStatus(functionalError.DATA_NOT_EXIST("user createdBy -> " + createurId + 
								" n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être utilisé.", locale));
						response.setHasError(true);
						return response;
					}
					
					// 2. Vérifier si le créateur n'est pas déjà participant (sécurité)
					List<ParticipantConversation> existingParticipants = 
						participantConversationRepository.findByConversationId(conversationSaved.getId(), false);
					
					boolean createurAlreadyParticipant = false;
					if (existingParticipants != null && !existingParticipants.isEmpty()) {
						for (ParticipantConversation participant : existingParticipants) {
							if (participant.getUser() != null && participant.getUser().getId() != null 
									&& participant.getUser().getId().equals(createurId)) {
								createurAlreadyParticipant = true;
								break;
							}
						}
					}
					
					// 3. Créer le participant pour le créateur s'il n'existe pas déjà
					if (!createurAlreadyParticipant) {
						ParticipantConversation participantCreateur = new ParticipantConversation();
						participantCreateur.setConversation(conversationSaved);
						participantCreateur.setUser(createur); // Référence vers l'utilisateur EXISTANT
						participantCreateur.setCreatedAt(Utilities.getCurrentDate());
						participantCreateur.setCreatedBy(conversationSaved.getCreatedBy());
						participantCreateur.setIsDeleted(false);
						participantCreateur.setHasLeft(false);
						participantCreateur.setHasDefinitivelyLeft(false);
						participantCreateur.setHasCleaned(false);
						participantCreateur.setIsAdmin(true); // Le créateur est automatiquement admin du groupe
						
						// SAUVEGARDE DU PARTICIPANT
						participantConversationRepository.save(participantCreateur);
						
						log.info("Groupe créé - Le créateur (userId=" + createur.getId() + 
								") a été automatiquement ajouté comme participant et admin du groupe (conversationId=" + conversationSaved.getId() + ")");
					} else {
						log.info("Groupe créé - Le créateur (userId=" + createur.getId() + 
								") est déjà participant du groupe (conversationId=" + conversationSaved.getId() + ")");
					}
				}

				// Convertir en DTO pour la réponse
				ConversationDto dtoSaved = ConversationTransformer.INSTANCE.toDto(conversationSaved);
				itemsDto.add(dtoSaved);
			}
		}

		// Préparer la réponse finale
		final int size = itemsDto.size();
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

		log.info("----end create Conversation-----");
		return response;
	}

	/**
	 * update Conversation by using ConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ConversationDto> update(Request<ConversationDto> request, Locale locale)  throws ParseException {
		log.info("----begin update Conversation-----");

		Response<ConversationDto> response = new Response<ConversationDto>();
		List<Conversation>        items    = new ArrayList<Conversation>();
			
		for (ConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la conversation existe
			Conversation entityToSave = null;
			entityToSave = conversationRepository.findOne(dto.getId(), false);
			if (entityToSave == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("conversation id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if typeConversation exist
			if (dto.getTypeConversationId() != null && dto.getTypeConversationId() > 0){
				TypeConversation existingTypeConversation = typeConversationRepository.findOne(dto.getTypeConversationId(), false);
				if (existingTypeConversation == null) {
					response.setStatus(functionalError.DATA_NOT_EXIST("typeConversation typeConversationId -> " + dto.getTypeConversationId(), locale));
					response.setHasError(true);
					return response;
				}
				entityToSave.setTypeConversation(existingTypeConversation);
			}
			if (Utilities.notBlank(dto.getTitre())) {
				entityToSave.setTitre(dto.getTitre());
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
			List<Conversation> itemsSaved = null;
			// maj les donnees en base
			itemsSaved = conversationRepository.saveAll((Iterable<Conversation>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("conversation", locale));
				response.setHasError(true);
				return response;
			}
			List<ConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ConversationTransformer.INSTANCE.toLiteDtos(itemsSaved) : ConversationTransformer.INSTANCE.toDtos(itemsSaved);

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

		log.info("----end update Conversation-----");
		return response;
	}

	/**
	 * delete Conversation by using ConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ConversationDto> delete(Request<ConversationDto> request, Locale locale)  {
		log.info("----begin delete Conversation-----");

		Response<ConversationDto> response = new Response<ConversationDto>();
		List<Conversation>        items    = new ArrayList<Conversation>();
			
		for (ConversationDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la conversation existe
			Conversation existingEntity = null;

			existingEntity = conversationRepository.findOne(dto.getId(), false);
			if (existingEntity == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("conversation -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			// participantConversation
			List<ParticipantConversation> listOfParticipantConversation = participantConversationRepository.findByConversationId(existingEntity.getId(), false);
			if (listOfParticipantConversation != null && !listOfParticipantConversation.isEmpty()){
				response.setStatus(functionalError.DATA_NOT_DELETABLE("(" + listOfParticipantConversation.size() + ")", locale));
				response.setHasError(true);
				return response;
			}
			// message
			List<Message> listOfMessage = messageRepository.findByConversationId(existingEntity.getId(), false);
			if (listOfMessage != null && !listOfMessage.isEmpty()){
				response.setStatus(functionalError.DATA_NOT_DELETABLE("(" + listOfMessage.size() + ")", locale));
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
			conversationRepository.saveAll((Iterable<Conversation>) items);

			response.setHasError(false);
		}

		log.info("----end delete Conversation-----");
		return response;
	}

	/**
	 * get Conversation by using ConversationDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<ConversationDto> getByCriteria(Request<ConversationDto> request, Locale locale)  throws Exception {
		log.info("----begin get Conversation-----");

		Response<ConversationDto> response = new Response<ConversationDto>();
		List<Conversation> items 			 = conversationRepository.getByCriteria(request, em, locale);

		if (items != null && !items.isEmpty()) {
			List<ConversationDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? ConversationTransformer.INSTANCE.toLiteDtos(items) : ConversationTransformer.INSTANCE.toDtos(items);

			final int size = items.size();

			List<String>  listOfError      = Collections.synchronizedList(new ArrayList<String>());
			// Utiliser map() au lieu de forEach() pour que les modifications soient persistées
			itemsDto = itemsDto.parallelStream().map(dto -> {
				try {
					dto = getFullInfos(dto, size, request.getIsSimpleLoading(), locale);

					// Récupérer l'interlocuteur (ID et nom) pour les conversations privées
					if (dto.getId() != null) {
						// Vérifier si c'est une conversation privée (typeConversationCode = PRIVEE ou PRIVATE)
						if (dto.getTypeConversationCode() != null && 
						    ("PRIVEE".equalsIgnoreCase(dto.getTypeConversationCode()) || 
						     "PRIVATE".equalsIgnoreCase(dto.getTypeConversationCode()))) {
							
							// Récupérer l'interlocuteur (ID et nom) pour la conversation privée
							Integer currentUserId = request.getUser();
							if (currentUserId != null) {
								try {
									List<Object[]> interlocutorResult = conversationRepository.findInterlocutorForPrivateConversation(
											dto.getId(),
											currentUserId
									);
									
									if (!interlocutorResult.isEmpty()) {
										Object[] interlocutorData = interlocutorResult.get(0);
										Integer interlocutorId = (Integer) interlocutorData[0];
										String interlocutorNom = (String) interlocutorData[1];
										String interlocutorPrenoms = (String) interlocutorData[2];
										
										// Construire le nom complet de l'interlocuteur
										String interlocuteurName = "";
										if (Utilities.notBlank(interlocutorPrenoms)) {
											interlocuteurName = interlocutorPrenoms;
										}
										if (Utilities.notBlank(interlocutorNom)) {
											interlocuteurName += (Utilities.notBlank(interlocuteurName) ? " " : "") + interlocutorNom;
										}
										if (Utilities.isBlank(interlocuteurName)) {
											interlocuteurName = "Utilisateur " + interlocutorId;
										}
										
										dto.setInterlocuteurId(interlocutorId);
										dto.setInterlocuteurName(interlocuteurName);
										log.info("Interlocuteur assigné - ID: " + interlocutorId + ", Nom: " + interlocuteurName);
									}
								} catch (Exception e) {
									log.warning("Erreur lors de la récupération de l'interlocuteur pour la conversation privée id=" + dto.getId() + " : " + e.getMessage());
									// Ne pas faire échouer la requête si cette récupération échoue
								}
							}
						}
						
						// Récupérer le dernier message de la conversation
						Message lastMessage = conversationRepository.findLastMessageByConversationId(dto.getId());
						if (lastMessage != null) {
							dto.setMessageContent(lastMessage.getContent());
							dto.setMessageImgUrl(lastMessage.getImgUrl());
							
							// Assigner le champ lastMessage avec le contenu du message ou un indicateur
							String lastMessageContent = null;
							if (Utilities.notBlank(lastMessage.getContent())) {
								lastMessageContent = lastMessage.getContent();
							} else if (Utilities.notBlank(lastMessage.getImgUrl())) {
								lastMessageContent = "[Image]";
							}
							dto.setLastMessage(lastMessageContent);
							

							
							// Récupérer les noms de l'expéditeur et du destinataire pour une conversation privée
							// Vérifier si c'est une conversation privée (typeConversationCode = PRIVEE ou PRIVATE)
							if (dto.getTypeConversationCode() != null && 
							    ("PRIVEE".equalsIgnoreCase(dto.getTypeConversationCode()) || 
							     "PRIVATE".equalsIgnoreCase(dto.getTypeConversationCode()))) {
								
								Integer messageCreatedBy = lastMessage.getCreatedBy();
								if (messageCreatedBy != null) {
									try {
										log.info("Recherche des noms pour conversationId=" + dto.getId() + ", messageCreatedBy=" + messageCreatedBy);
										List<Object[]> result = conversationRepository.findSenderAndRecipientNamesForPrivateConversation(
												dto.getId(),
												messageCreatedBy
										);

										log.info("Résultat de la requête: " + (result != null ? result.size() + " résultats" : "null"));
										if (!result.isEmpty()) {
											Object[] names = result.get(0);
											String senderNom = (String) names[0];
											String senderPrenoms = (String) names[1];
											String recipientNom = (String) names[2];
											String recipientPrenoms = (String) names[3];
											
											// Construire le nom complet de l'expéditeur
											String senderFullName = "";
											if (Utilities.notBlank(senderPrenoms)) {
												senderFullName = senderPrenoms;
											}
											if (Utilities.notBlank(senderNom)) {
												senderFullName += (Utilities.notBlank(senderFullName) ? " " : "") + senderNom;
											}
											
											// Construire le nom complet du destinataire
											String recipientFullName = "";
											if (Utilities.notBlank(recipientPrenoms)) {
												recipientFullName = recipientPrenoms;
											}
											if (Utilities.notBlank(recipientNom)) {
												recipientFullName += (Utilities.notBlank(recipientFullName) ? " " : "") + recipientNom;
											}

											dto.setSenderFullName(senderFullName);
											dto.setRecipientFullName(recipientFullName);
											log.info("Noms assignés - Expéditeur: " + senderFullName + ", Destinataire: " + recipientFullName);
										} else {
											log.warning("Aucun résultat trouvé pour conversationId=" + dto.getId() + ", messageCreatedBy=" + messageCreatedBy);
										}
									} catch (Exception e) {
										log.warning("Erreur lors de la récupération des noms pour la conversation privée id=" + dto.getId() + " : " + e.getMessage());
										// Ne pas faire échouer la requête si cette récupération échoue
									}
								}
							}
						}
					}
					return dto;
				} catch (Exception e) {
					listOfError.add(e.getMessage());
					e.printStackTrace();
					return dto; // Retourner le DTO même en cas d'erreur
				}
			}).collect(java.util.stream.Collectors.toList());
			
			if (Utilities.isNotEmpty(listOfError)) {
				Object[] objArray = listOfError.stream().distinct().toArray();
				throw new RuntimeException(StringUtils.join(objArray, ", "));
			}
			response.setItems(itemsDto);
			response.setCount(conversationRepository.count(request, em, locale));
			response.setHasError(false);
		} else {
			response.setStatus(functionalError.DATA_EMPTY("conversation", locale));
			response.setHasError(false);
			return response;
		}




		log.info("----end get Conversation-----");
		return response;
	}

	/**
	 * Exporte les conversations vers un fichier Excel
	 * 
	 * @param request La requête contenant les critères de recherche
	 * @param locale La locale pour les messages d'erreur
	 * @return ByteArrayOutputStream contenant le fichier Excel
	 * @throws Exception En cas d'erreur lors de l'export
	 */
	public java.io.ByteArrayOutputStream export(Request<ConversationDto> request, Locale locale) throws Exception {
		log.info("----begin export Conversation to Excel-----");
		
		// Récupérer les conversations via getByCriteria
		Response<ConversationDto> response = getByCriteria(request, locale);
		
		if (response.isHasError()) {
			log.warning("Erreur lors de la récupération des conversations pour l'export : " + 
					(response.getStatus() != null ? response.getStatus().getMessage() : "Erreur inconnue"));
			throw new Exception("Impossible de récupérer les conversations pour l'export : " + 
					(response.getStatus() != null ? response.getStatus().getMessage() : "Erreur inconnue"));
		}
		
		if (response.getItems() == null || response.getItems().isEmpty()) {
			log.info("Aucune conversation à exporter");
			// Créer un fichier Excel vide avec juste les en-têtes
			String[] headers = {"ID", "Titre", "Type de Conversation", "Date de création", "Créateur ID", "Dernier message", "Interlocuteur"};
			return ExcelExportUtil.exportToExcel("Conversations", headers, new ArrayList<>());
		}
		
		// Préparer les données pour l'export
		String[] headers = {"ID", "Titre", "Type de Conversation", "Date de création", "Créateur ID", "Dernier message", "Interlocuteur"};
		List<Map<String, Object>> data = new ArrayList<>();
		
		for (ConversationDto conv : response.getItems()) {
			Map<String, Object> row = new HashMap<>();
			row.put("ID", conv.getId());
			row.put("Titre", conv.getTitre() != null ? conv.getTitre() : "");
			row.put("Type de Conversation", conv.getTypeConversationCode() != null ? conv.getTypeConversationCode() : "");
			row.put("Date de création", conv.getCreatedAt());
			row.put("Créateur ID", conv.getCreatedBy());
			row.put("Dernier message", conv.getLastMessage() != null ? conv.getLastMessage() : "");
			row.put("Interlocuteur", conv.getInterlocuteurName() != null ? conv.getInterlocuteurName() : "");
			data.add(row);
		}
		
		// Générer le fichier Excel
		java.io.ByteArrayOutputStream outputStream = ExcelExportUtil.exportToExcel("Conversations", headers, data);
		
		log.info("----end export Conversation to Excel - " + data.size() + " conversations exportées-----");
		return outputStream;
	}

	/**
	 *


	 */
	private User validateUserExists(Integer userId, Locale locale, String fieldName) {
		if (userId == null || userId <= 0) {
			log.warning("Tentative de validation d'un utilisateur avec ID invalide : " + fieldName + " = " + userId);
			return null;
		}

		// Seuls les utilisateurs non supprimés sont retournés
		User user = userRepository.findOne(userId, false);

		if (user == null) {
			// L'utilisateur n'existe pas dans la base de données ou a été supprimé
			String errorMessage = "user " + fieldName + " -> " + userId +
					" n'existe pas dans la base de données. L'utilisateur doit être créé avant d'être utilisé.";
			log.warning(errorMessage);
			throw new RuntimeException(functionalError.DATA_NOT_EXIST(errorMessage, locale).getMessage());
		}

		if (user.getIsDeleted() != null && user.getIsDeleted()) {
			String errorMessage = "user " + fieldName + " -> " + userId + " a été supprimé";
			log.warning(errorMessage);
			throw new RuntimeException(functionalError.DATA_NOT_EXIST(errorMessage, locale).getMessage());
		}

		// L'utilisateur existe et est valide
		log.fine("Utilisateur validé avec succès : " + fieldName + " = " + userId);
		return user;
	}

	/**
	 * get full ConversationDto by using Conversation as object.
	 *
	 * @param dto
	 * @param size
	 * @param isSimpleLoading
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	private ConversationDto getFullInfos(ConversationDto dto, Integer size, Boolean isSimpleLoading, Locale locale) throws Exception {
		// put code here

		if (Utilities.isTrue(isSimpleLoading)) {
			return dto;
		}
		if (size > 1) {
			return dto;
		}

		return dto;
	}

	private void sendInitialMessage(Conversation conversation, String messageContent, String messageImgUrl, Integer createdBy) {
		if (conversation == null) {
			log.warning("sendInitialMessage: La conversation est nulle, impossible d'envoyer le message");
			return;
		}
		
		// Vérifier que la conversation a bien un ID
		if (conversation.getId() == null) {
			log.severe("sendInitialMessage: La conversation n'a pas d'ID, elle doit être sauvegardée avant d'envoyer un message. ConversationId=" + conversation.getId());
			return;
		}
		
		boolean hasTextMessage = Utilities.notBlank(messageContent);
		boolean hasImageMessage = Utilities.notBlank(messageImgUrl);
		
		if (!hasTextMessage && !hasImageMessage) {
			log.warning("sendInitialMessage: Aucun contenu de message (texte ou image) fourni pour la conversation id=" + conversation.getId() + ". Message non envoyé.");
			return; // Aucun message à envoyer
		}
		
		try {
			TypeMessage typeMessageDefault = null;
			String codeTypeMessage = null;
			String libelleTypeMessage = null;
			
			// Détection automatique du type de message
			if (hasImageMessage) {
				codeTypeMessage = "IMAGE";
				libelleTypeMessage = "Message Image";
				typeMessageDefault = typeMessageRepository.findByCode("IMAGE", false);
				if (typeMessageDefault == null) {
					typeMessageDefault = typeMessageRepository.findByCode("IMG", false);
				}
				if (typeMessageDefault == null) {
					typeMessageDefault = typeMessageRepository.findByCode("PHOTO", false);
				}
			} else if (hasTextMessage) {
				codeTypeMessage = "TEXT";
				libelleTypeMessage = "Message Texte";
				typeMessageDefault = typeMessageRepository.findByCode("TEXT", false);
				if (typeMessageDefault == null) {
					typeMessageDefault = typeMessageRepository.findByCode("TEXTE", false);
				}
				if (typeMessageDefault == null) {
					typeMessageDefault = typeMessageRepository.findByCode("MESSAGE", false);
				}
			}
			
			// Si toujours pas trouvé, créer automatiquement le TypeMessage
			if (typeMessageDefault == null) {
				log.info("TypeMessage avec code '" + codeTypeMessage + "' introuvable. Vérification des éléments supprimés...");
				
				// Vérifier si un TypeMessage avec ce code existe mais est supprimé
				TypeMessage deletedTypeMessage = typeMessageRepository.findByCode(codeTypeMessage, true);
				
				if (deletedTypeMessage != null && deletedTypeMessage.getIsDeleted() != null && deletedTypeMessage.getIsDeleted()) {
					// Le TypeMessage existe mais est supprimé : le réactiver
					log.info("TypeMessage avec code '" + codeTypeMessage + "' existe mais est supprimé. Réactivation...");
					deletedTypeMessage.setIsDeleted(false);
					deletedTypeMessage.setDeletedAt(null);
					deletedTypeMessage.setDeletedBy(null);
					deletedTypeMessage.setUpdatedAt(Utilities.getCurrentDate());
					deletedTypeMessage.setUpdatedBy(createdBy);
					if (Utilities.isBlank(deletedTypeMessage.getLibelle())) {
						deletedTypeMessage.setLibelle(libelleTypeMessage);
					}
					typeMessageDefault = typeMessageRepository.save(deletedTypeMessage);
					log.info("TypeMessage réactivé avec code: " + codeTypeMessage + ", id: " + typeMessageDefault.getId());
				} else {
					// Le TypeMessage n'existe pas du tout : le créer automatiquement
					log.info("TypeMessage avec code '" + codeTypeMessage + "' n'existe pas. Création automatique...");
					typeMessageDefault = new TypeMessage();
					typeMessageDefault.setCode(codeTypeMessage);
					typeMessageDefault.setLibelle(libelleTypeMessage);
					typeMessageDefault.setCreatedAt(Utilities.getCurrentDate());
					typeMessageDefault.setCreatedBy(createdBy);
					typeMessageDefault.setIsDeleted(false);
					
					// Sauvegarder le nouveau TypeMessage
					typeMessageDefault = typeMessageRepository.save(typeMessageDefault);
					log.info("TypeMessage créé automatiquement avec code: " + codeTypeMessage + ", libelle: " + libelleTypeMessage + ", id: " + typeMessageDefault.getId());
				}
			}
			
			// Maintenant, créer et sauvegarder le message
			if (typeMessageDefault != null) {
				Message messageInitial = new Message();
				
				// Si c'est une image, mettre l'URL dans imgUrl
				if (hasImageMessage) {
					messageInitial.setImgUrl(messageImgUrl);
					messageInitial.setContent(null); // Pas de contenu texte pour une image seule
				}
				
				// Si c'est du texte, mettre le contenu dans content
				if (hasTextMessage) {
					messageInitial.setContent(messageContent);
					// Si c'est du texte pur, imgUrl reste null
					if (!hasImageMessage) {
						messageInitial.setImgUrl(null);
					}
				}
				
				// Si les deux sont présents (texte + image), les deux sont remplis
				messageInitial.setConversation(conversation);
				messageInitial.setTypeMessage2(typeMessageDefault);
				messageInitial.setCreatedAt(Utilities.getCurrentDate());
				messageInitial.setCreatedBy(createdBy);
				messageInitial.setIsDeleted(false);
				
				// Sauvegarder le message
				Message messageSaved = messageRepository.save(messageInitial);
				
				if (messageSaved != null && messageSaved.getId() != null) {
					String messageType = hasImageMessage ? (hasTextMessage ? "texte + image" : "image") : "texte";
					log.info("Message initial (" + messageType + ") envoyé avec succès dans la conversation id=" + conversation.getId() + 
							", messageId=" + messageSaved.getId() + 
							", typeMessage=" + (typeMessageDefault.getCode() != null ? typeMessageDefault.getCode() : "ID=" + typeMessageDefault.getId()));
				} else {
					log.severe("ERREUR: Le message n'a pas été sauvegardé correctement. messageSaved=" + messageSaved);
				}
			} else {
				log.severe("ERREUR CRITIQUE: Impossible de créer ou trouver un TypeMessage. Le message ne peut pas être envoyé.");
			}
		} catch (Exception e) {
			log.severe("ERREUR lors de l'envoi du message initial pour conversation id=" + conversation.getId() + " : " + e.getMessage());
			e.printStackTrace();
			// Ne pas propager l'exception pour ne pas faire échouer la création de conversation
			// Le message sera juste loggé mais la conversation sera créée quand même
		}
	}
}
