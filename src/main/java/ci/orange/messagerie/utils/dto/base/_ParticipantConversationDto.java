
/*
 * Java dto for entity table participant_conversation 
 * Created on 2026-01-03 ( Time 17:01:33 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.utils.dto.base;

import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import lombok.*;

import ci.orange.messagerie.utils.contract.*;

/**
 * DTO customize for table "participant_conversation"
 * 
 * @author Smile Back-End generator
 *
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
public class _ParticipantConversationDto implements Cloneable {

    protected Integer    id                   ; // Primary Key

    protected Integer    conversationId       ;
    protected Integer    userId               ;
	protected String     createdAt            ;
	protected String     updatedAt            ;
	protected String     deletedAt            ;
    protected Integer    createdBy            ;
    protected Integer    updatedBy            ;
    protected Integer    deletedBy            ;
    protected Boolean    isDeleted            ;
	protected String     recreatedAt          ;
    protected Integer    recreatedBy          ;
	protected String     leftAt               ;
    protected Integer    leftBy               ;
	protected String     definitivelyLeftAt   ;
    protected Integer    definitivelyLeftBy   ;
    protected Boolean    hasLeft              ;
    protected Boolean    hasDefinitivelyLeft  ;
    protected Boolean    hasCleaned           ;
    protected Boolean    isAdmin              ;

    //----------------------------------------------------------------------
    // ENTITY LINKS FIELD ( RELATIONSHIP )
    //----------------------------------------------------------------------
	//protected Integer    user;
	protected String userNom;
	protected String userPrenoms;
	//protected Integer    conversation;

	// Search param
	protected SearchParam<Integer>  idParam               ;                     
	protected SearchParam<Integer>  conversationIdParam   ;                     
	protected SearchParam<Integer>  userIdParam           ;                     
	protected SearchParam<String>   createdAtParam        ;                     
	protected SearchParam<String>   updatedAtParam        ;                     
	protected SearchParam<String>   deletedAtParam        ;                     
	protected SearchParam<Integer>  createdByParam        ;                     
	protected SearchParam<Integer>  updatedByParam        ;                     
	protected SearchParam<Integer>  deletedByParam        ;                     
	protected SearchParam<Boolean>  isDeletedParam        ;                     
	protected SearchParam<String>   recreatedAtParam      ;                     
	protected SearchParam<Integer>  recreatedByParam      ;                     
	protected SearchParam<String>   leftAtParam           ;                     
	protected SearchParam<Integer>  leftByParam           ;                     
	protected SearchParam<String>   definitivelyLeftAtParam;                     
	protected SearchParam<Integer>  definitivelyLeftByParam;                     
	protected SearchParam<Boolean>  hasLeftParam          ;
	protected SearchParam<Boolean>  hasDefinitivelyLeftParam;
	protected SearchParam<Boolean>  hasCleanedParam       ;
	protected SearchParam<Boolean>  isAdminParam          ;
	protected SearchParam<Integer>  userParam             ;
	protected SearchParam<String>   userNomParam          ;                     
	protected SearchParam<String>   userPrenomsParam      ;                     
	protected SearchParam<Integer>  conversationParam     ;                     

	// order param
	protected String orderField;
	protected String orderDirection;




}
