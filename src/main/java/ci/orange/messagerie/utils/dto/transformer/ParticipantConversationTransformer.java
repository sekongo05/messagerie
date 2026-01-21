

/*
 * Java transformer for entity table participant_conversation 
 * Created on 2026-01-03 ( Time 17:01:33 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.utils.dto.transformer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.dao.entity.*;


/**
 * TRANSFORMER for table "participant_conversation"
 * 
 * @author Geo
 *
 */
@Mapper
public interface ParticipantConversationTransformer {

	ParticipantConversationTransformer INSTANCE = Mappers.getMapper(ParticipantConversationTransformer.class);

	@FullTransformerQualifier
	@Mappings({
		@Mapping(source="entity.createdAt", dateFormat="dd/MM/yyyy",target="createdAt"),
		@Mapping(source="entity.updatedAt", dateFormat="dd/MM/yyyy",target="updatedAt"),
		@Mapping(source="entity.deletedAt", dateFormat="dd/MM/yyyy",target="deletedAt"),
		@Mapping(source="entity.recreatedAt", dateFormat="dd/MM/yyyy HH:mm:ss",target="recreatedAt"),
		@Mapping(source="entity.recreatedBy", target="recreatedBy"),
		@Mapping(source="entity.leftAt", dateFormat="dd/MM/yyyy HH:mm:ss",target="leftAt"),
		@Mapping(source="entity.leftBy", target="leftBy"),
		@Mapping(source="entity.definitivelyLeftAt", dateFormat="dd/MM/yyyy HH:mm:ss",target="definitivelyLeftAt"),
		@Mapping(source="entity.definitivelyLeftBy", target="definitivelyLeftBy"),
		@Mapping(source="entity.hasLeft", target="hasLeft"),
		@Mapping(source="entity.hasDefinitivelyLeft", target="hasDefinitivelyLeft"),
		@Mapping(source="entity.hasCleaned", target="hasCleaned"),
		@Mapping(source="entity.isDeleted", target="isDeleted"),
		@Mapping(source="entity.createdBy", target="createdBy"),
		@Mapping(source="entity.updatedBy", target="updatedBy"),
		@Mapping(source="entity.deletedBy", target="deletedBy"),
		@Mapping(source="entity.user.id", target="userId"),
		@Mapping(source="entity.user.nom", target="userNom"),
		@Mapping(source="entity.user.prenoms", target="userPrenoms"),
		@Mapping(source="entity.conversation.id", target="conversationId"),
		@Mapping(source="entity.isAdmin", target="isAdmin"),
	})
	ParticipantConversationDto toDto(ParticipantConversation entity) throws ParseException;

	@IterableMapping(qualifiedBy = {FullTransformerQualifier.class})
    List<ParticipantConversationDto> toDtos(List<ParticipantConversation> entities) throws ParseException;

    default ParticipantConversationDto toLiteDto(ParticipantConversation entity) {
		if (entity == null) {
			return null;
		}
		ParticipantConversationDto dto = new ParticipantConversationDto();
		dto.setId( entity.getId() );
		return dto;
    }

	default List<ParticipantConversationDto> toLiteDtos(List<ParticipantConversation> entities) {
		if (entities == null || entities.stream().allMatch(o -> o == null)) {
			return null;
		}
		List<ParticipantConversationDto> dtos = new ArrayList<ParticipantConversationDto>();
		for (ParticipantConversation entity : entities) {
			dtos.add(toLiteDto(entity));
		}
		return dtos;
	}

	@Mappings({
		@Mapping(source="dto.id", target="id"),
		@Mapping(source="dto.createdAt", dateFormat="dd/MM/yyyy",target="createdAt"),
		@Mapping(source="dto.updatedAt", dateFormat="dd/MM/yyyy",target="updatedAt"),
		@Mapping(source="dto.deletedAt", dateFormat="dd/MM/yyyy",target="deletedAt"),
		@Mapping(source="dto.createdBy", target="createdBy"),
		@Mapping(source="dto.updatedBy", target="updatedBy"),
		@Mapping(source="dto.deletedBy", target="deletedBy"),
		@Mapping(source="dto.isDeleted", target="isDeleted"),
		@Mapping(source="dto.recreatedAt", dateFormat="dd/MM/yyyy",target="recreatedAt"),
		@Mapping(source="dto.recreatedBy", target="recreatedBy"),
		@Mapping(source="dto.leftAt", dateFormat="dd/MM/yyyy",target="leftAt"),
		@Mapping(source="dto.leftBy", target="leftBy"),
		@Mapping(source="dto.definitivelyLeftAt", dateFormat="dd/MM/yyyy",target="definitivelyLeftAt"),
		@Mapping(source="dto.definitivelyLeftBy", target="definitivelyLeftBy"),
		@Mapping(source="dto.hasLeft", target="hasLeft"),
		@Mapping(source="dto.hasDefinitivelyLeft", target="hasDefinitivelyLeft"),
		@Mapping(source="dto.hasCleaned", target="hasCleaned"),
		@Mapping(source="dto.isAdmin", target="isAdmin"),
		@Mapping(source="user", target="user"),
		@Mapping(source="conversation", target="conversation"),
	})
    ParticipantConversation toEntity(ParticipantConversationDto dto, User user, Conversation conversation) throws ParseException;

    //List<ParticipantConversation> toEntities(List<ParticipantConversationDto> dtos) throws ParseException;

}
