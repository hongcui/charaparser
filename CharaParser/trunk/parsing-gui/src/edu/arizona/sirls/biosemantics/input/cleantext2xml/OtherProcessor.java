/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import org.jdom.Element;

/**
 * @author Hong Cui
 * * If there are distribution, habitat, or flowering time info, mark them up
 *
 */
public class OtherProcessor {
	String floweringtime = "Jan\\.|January|Feb\\.|Febuary|March|Mar\\.|April|Apr\\.|May|June|Jun\\.|July|Jul\\.|August|Aug\\.|September|Sept\\.|October|Oct\\.|November|Nov\\.|December|Dec\\.|year";
    
	/**
	 * 
	 */
	public OtherProcessor() {
		// TODO Auto-generated constructor stub
	}

	public void process(Element treatment){
		Element other = treatment.getChild("habitat_elevation_distribution_or_ecology");
		if(other!=null){
		int i = treatment.indexOf(other);
		//
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
