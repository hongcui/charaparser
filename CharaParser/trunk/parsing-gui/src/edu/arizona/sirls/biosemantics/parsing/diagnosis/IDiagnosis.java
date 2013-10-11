/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing.diagnosis;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * @author updates
 *
 */
public interface IDiagnosis {

	/**
	 * is this a diagnosis element?
	 * @return
	 */
	boolean isADiagnosis(Object o);
	
	/**
	 * parser diagnosis text and derive reg exp
	 * patterns for finding sentences of diagnosis style
	 * 
	 * @return
	 * @throws JDOMException 
	 */
	ArrayList<Pattern> parseDiagnosis() throws JDOMException;
}
