
/*
 * Created on 2026-01-03 ( Time 17:01:50 )
 * Generator tool : Telosys Tools Generator ( version 3.3.0 )
 * Copyright 2018 Geo. All Rights Reserved.
 */

package ci.orange.messagerie.utils.contract;

import java.util.List;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.*;

/**
 * Response
 * 
 * @author Geo
 *
 */
@Data
@ToString
@NoArgsConstructor
@XmlRootElement
@JsonInclude(Include.NON_NULL)
public class Response<T> extends ResponseBase {

	protected List<T> items;
}
