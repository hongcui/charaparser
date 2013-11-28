/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

/**
 * @author hong
 *
 */
public class Term {
	String term;
	String category;
	
	public Term(String term, String category){
		this.term = term;
		this.category = category;
	}
	
	public String getTerm(){
		return this.term;
	}
	
	public String getCategory(){
		return this.category;
	}
	
	public String toString(){
		return this.term+"("+this.category+")";
	}
	
}
