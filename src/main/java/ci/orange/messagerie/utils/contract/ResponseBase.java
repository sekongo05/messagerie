
/*
 * Created on 2026-01-03 ( Time 17:01:50 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.utils.contract;

import lombok.*;
import ci.orange.messagerie.utils.Status;

/**
 * Response Base
 * 
 * @author Geo
 *
 */
@Data
@ToString
@NoArgsConstructor
public class ResponseBase {

	protected Status	status = new Status();
	protected boolean	hasError;
	protected String	sessionUser;
	protected Long		count;
}
