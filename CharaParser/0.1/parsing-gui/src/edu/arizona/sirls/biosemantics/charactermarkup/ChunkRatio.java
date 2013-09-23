 /* $Id: ChunkRatio.java 1482 2013-04-15 16:20:01Z hong1.cui@gmail.com $ */
/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

/**
 * @author hongcui
 *
 */
public class ChunkRatio extends Chunk {
	String name;
	/**
	 * @param text
	 */
	public ChunkRatio(String text) {
		super(text);

	}
	
	public void setName(String name){
		this.name = name;
	}


	public String getName() {

		return name;
	}
}
