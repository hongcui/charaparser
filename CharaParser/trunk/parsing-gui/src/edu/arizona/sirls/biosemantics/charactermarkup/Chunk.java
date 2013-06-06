 /* $Id: Chunk.java 1482 2013-04-15 16:20:01Z hong1.cui@gmail.com $ */
/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

import java.util.ArrayList;

/**
 * @author hongcui
 *
 */
public class Chunk {
	protected String text = null;
	protected ArrayList<String> chunkedTokens;
	public Chunk(String text){
		this.text = text;	
	}
	
	public String toString(){
		return this.text;
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public void setChunkedTokens(ArrayList<String> tokens){
		this.chunkedTokens = tokens;
	}
	
	public ArrayList<String> getChunkedTokens(){
		return this.chunkedTokens ;
	}

}
