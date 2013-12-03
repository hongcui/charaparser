/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

import java.util.Comparator;

/**
 * @author hong
 *
 */
public class Term{
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

	@Override
	public boolean equals(Object object) {
		if(object==null) return false;
		if(object instanceof Term){
			Term o = (Term)object;
			if(o.getTerm().compareToIgnoreCase(this.term) == 0 &&
					o.getCategory().compareToIgnoreCase(this.category)==0) return true;
			else return false;
		}else{
			return false;
		}		
	}

	public int hashCode(){
		return (this.term+" "+this.category).hashCode();
	}

	
}
