 /* $Id: SyntacticParser.java 1086 2012-05-09 23:37:20Z huangfengq@gmail.com $ */
/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

/**
 * @author hongcui
 *
 */
public interface SyntacticParser {


	public void POSTagging() throws Exception;
	public void parsing() throws Exception;
	public void extracting() throws Exception;

}
