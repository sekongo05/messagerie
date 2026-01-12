
/*
 * Java dto for entity table conversation 
 * Created on 2026-01-03 ( Time 17:01:31 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.utils.dto;

import java.util.Date;
import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.*;

import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.dto.base._ConversationDto;

/**
 * DTO for table "conversation"
 *
 * @author Geo
 */
@Data
@ToString
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
public class ConversationDto extends _ConversationDto{

    private String    statusLibelle               ;
    private String    typeConversation;
    private Integer   interlocuteurId;
    private String    messageContent;
    private String    senderFullName;
    private String    lastMessage;// Nom complet de l'expéditeur du dernier message
    private String recipientFullName;
    private String    messageImgUrl;

    
	//----------------------------------------------------------------------
    // clone METHOD
    //----------------------------------------------------------------------
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}


}
