                                                    											
/*
 * Java business for entity table user 
 * Created on 2026-01-03 ( Time 17:01:34 )
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.enums.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.IBasicBusiness;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.utils.dto.transformer.*;
import ci.orange.messagerie.dao.entity.User;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.*;

/**
BUSINESS for table "user"
 * 
 * @author Geo
 *
 */
@Log
@Component
public class UserBusiness implements IBasicBusiness<Request<UserDto>, Response<UserDto>> {

	private Response<UserDto> response;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ParticipantConversationRepository participantConversationRepository;
	@Autowired
	private HistoriqueSuppressionMessageRepository historiqueSuppressionMessageRepository;
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

	public UserBusiness() {
		dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	}

	// methode pour la verification du pattern de l'email

	private boolean isValidEmail(String email){
		if(email == null) return false;
		String regex = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}
	
	/**
	 * create User by using UserDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<UserDto> create(Request<UserDto> request, Locale locale)  throws ParseException {
		log.info("----begin create User-----");

		Response<UserDto> response = new Response<UserDto>();
		List<User>        items    = new ArrayList<User>();
			
		for (UserDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("nom", dto.getNom());
			fieldsToVerify.put("prenoms", dto.getPrenoms());
			fieldsToVerify.put("email", dto.getEmail());
//			fieldsToVerify.put("deletedAt", dto.getDeletedAt());
//			fieldsToVerify.put("deletedBy", dto.getDeletedBy());
			// new function for verify email

			if(!isValidEmail(dto.getEmail())){
				response.setHasError(true);
				response.setStatus(functionalError.SAVE_FAIL("le format de l'email est incorrect",locale));
				return response;
			}
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verify if user to insert do not exist - Vérifier si l'email existe déjà
			List<User> users = userRepository.findByEmail(dto.getEmail(), false);
			if (!users.isEmpty()){
				response.setStatus(functionalError.DATA_EXIST("Un utilisateur avec l'email '" + dto.getEmail() + "' existe déjà", locale));
				response.setHasError(true);
				return response;
			}

/*
			if (existingEntity != null) {
				response.setStatus(functionalError.DATA_EXIST("user id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

*/
				User entityToSave = null;
			entityToSave = UserTransformer.INSTANCE.toEntity(dto);
			entityToSave.setCreatedAt(Utilities.getCurrentDate());
			entityToSave.setCreatedBy(request.getUser());
			entityToSave.setIsDeleted(false);
//			entityToSave.setStatusId(StatusEnum.ACTIVE);
			items.add(entityToSave);
		}
		/***
		 *
		 *
		 * fin de la boucle for
		 */


		// enregistrement des element dans la bd
		if (!items.isEmpty()) {
			List<User> itemsSaved = null;
			// inserer les donnees en base de donnees
			itemsSaved = userRepository.saveAll((Iterable<User>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("user", locale));
				response.setHasError(true);
				return response;
			}

			///  tranformer les element en dto pour pouvoir les envoyé dans la réponse

			List<UserDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? UserTransformer.INSTANCE.toLiteDtos(itemsSaved) : UserTransformer.INSTANCE.toDtos(itemsSaved);

			final int size = itemsSaved.size();

			log.info("le nombre des conversation"+ size);
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

		log.info("----end create User-----");
		return response;
	}

	public Response<UserDto> login(Request<UserDto> request, Locale locale) throws Exception {
		log.info("begin login");
		Response<UserDto> response = new Response<>();
		List<UserDto> itemsDto= new ArrayList<>();

		if(request == null || request.getDatas() == null || request.getDatas().isEmpty()){
			response.setHasError(true);
			response.setStatus(functionalError.REQUEST_FAIL("requête invalide", locale));
			return response;
		}

		UserDto dto =request.getDatas().get(0);
		Map<String, Object> fieldsToVerify = new HashMap<>();
		fieldsToVerify.put("email", dto.getEmail());
		if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
			response.setHasError(true);
			response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
			return response;
		}

		if(!isValidEmail(dto.getEmail())){
			response.setStatus(functionalError.SAVE_FAIL("le format de l'email est incorrect", locale));
			response.setHasError(true);
			return response;
		}

		List<User> existingUsers= userRepository.findByEmail(dto.getEmail(), false);
		if(existingUsers.isEmpty()){
			response.setHasError(true);
			response.setStatus(functionalError.DATA_NOT_EXIST("l'utilisateur n'existe pas", locale));
			return response;
		}

		User user= existingUsers.get(0);

		UserDto userDto = UserTransformer.INSTANCE.toDto(user);


		userDto= getFullInfos(userDto, 1, request.getIsSimpleLoading(),locale);
		itemsDto.add(userDto);

		response.setItems(itemsDto);
		response.setHasError(false);
		response.setStatus(functionalError.SUCCESS("Connexion réussie", locale));


		log.info(" end login");
		return response;
	}

	/**
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<UserDto> update(Request<UserDto> request, Locale locale)  throws ParseException {
		log.info("----begin update User-----");

		Response<UserDto> response = new Response<UserDto>();
		List<User>        items    = new ArrayList<User>();
			
		for (UserDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la user existe
			User entityToSave = null;
			entityToSave = userRepository.findOne(dto.getId(), false);
			if (entityToSave == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("user id -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}

			if (Utilities.notBlank(dto.getNom())) {
				entityToSave.setNom(dto.getNom());
			}
			if (Utilities.notBlank(dto.getPrenoms())) {
				entityToSave.setPrenoms(dto.getPrenoms());
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
			List<User> itemsSaved = null;
			// maj les donnees en base
			itemsSaved = userRepository.saveAll((Iterable<User>) items);
			if (itemsSaved == null) {
				response.setStatus(functionalError.SAVE_FAIL("user", locale));
				response.setHasError(true);
				return response;
			}
			List<UserDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? UserTransformer.INSTANCE.toLiteDtos(itemsSaved) : UserTransformer.INSTANCE.toDtos(itemsSaved);

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

		log.info("----end update User-----");
		return response;
	}

	/**
	 * delete User by using UserDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<UserDto> delete(Request<UserDto> request, Locale locale)  {
		log.info("----begin delete User-----");

		Response<UserDto> response = new Response<UserDto>();
		List<User>        items    = new ArrayList<User>();
			
		for (UserDto dto : request.getDatas()) {
			// Definir les parametres obligatoires
			Map<String, java.lang.Object> fieldsToVerify = new HashMap<String, java.lang.Object>();
			fieldsToVerify.put("id", dto.getId());
			if (!Validate.RequiredValue(fieldsToVerify).isGood()) {
				response.setStatus(functionalError.FIELD_EMPTY(Validate.getValidate().getField(), locale));
				response.setHasError(true);
				return response;
			}

			// Verifier si la user existe
			User existingEntity = null;

			existingEntity = userRepository.findOne(dto.getId(), false);
			if (existingEntity == null) {
				response.setStatus(functionalError.DATA_NOT_EXIST("user -> " + dto.getId(), locale));
				response.setHasError(true);
				return response;
			}


			// participantConversation
			List<ParticipantConversation> listOfParticipantConversation = participantConversationRepository.findByUserId(existingEntity.getId(), false);
			if (listOfParticipantConversation != null && !listOfParticipantConversation.isEmpty()){
				response.setStatus(functionalError.DATA_NOT_DELETABLE("(" + listOfParticipantConversation.size() + ")", locale));
				response.setHasError(true);
				return response;
			}
			// historiqueSuppressionMessage
			List<HistoriqueSuppressionMessage> listOfHistoriqueSuppressionMessage = historiqueSuppressionMessageRepository.findByUserId(existingEntity.getId(), false);
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
			userRepository.saveAll((Iterable<User>) items);

			response.setHasError(false);
		}

		log.info("----end delete User-----");
		return response;
	}

	/**
	 * get User by using UserDto as object.
	 * 
	 * @param request
	 * @return response
	 * 
	 */
	@Override
	public Response<UserDto> getByCriteria(Request<UserDto> request, Locale locale)  throws Exception {
		log.info("----begin get User-----");

		Response<UserDto> response = new Response<UserDto>();
		List<User> items 			 = userRepository.getByCriteria(request, em, locale);

		if (items != null && !items.isEmpty()) {
			List<UserDto> itemsDto = (Utilities.isTrue(request.getIsSimpleLoading())) ? UserTransformer.INSTANCE.toLiteDtos(items) : UserTransformer.INSTANCE.toDtos(items);

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
			response.setCount(userRepository.count(request, em, locale));
			response.setHasError(false);
		} else {
			response.setStatus(functionalError.DATA_EMPTY("user", locale));
			response.setHasError(false);
			return response;
		}

		log.info("----end get User-----");
		return response;
	}

	/**
	 * get full UserDto by using User as object.
	 * 
	 * @param dto
	 * @param size
	 * @param isSimpleLoading
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	private UserDto getFullInfos(UserDto dto, Integer size, Boolean isSimpleLoading, Locale locale) throws Exception {
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
