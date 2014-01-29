/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.util.List;

import org.jdom.Element;

/**
 * @author Hong Cui
 * process a full_description elemment, which include name and other info (distribution, flowering time, discussion).
 *
 */
public class TaxonDescriptionProcessor {

	/**
	 * 
	 */
	public TaxonDescriptionProcessor() {
	}

	public void process(Element treatment){
		Element full = treatment.getChild("full_description");
		if(full!=null){
			OtherProcessor op = new OtherProcessor();
			TaxonNameProcessor tnp = new TaxonNameProcessor();
			String txt = full.getTextNormalize();
			if(txt.indexOf('—') == txt.lastIndexOf('—')) System.err.println("Discussion Problem "+txt);
			String rank = txt.substring(txt.indexOf("#")+1);
			txt = txt.substring(0, txt.indexOf("#"));
			String[] segs = txt.split("—"); //name, description, other
			int i = treatment.indexOf(full);
			full.detach();
			if(segs.length>2){
				treatment.addContent(i, new Element("habitat_elevation_distribution_or_ecology").addContent(segs[2].trim()));
			}
			for(int j = 3; j<segs.length; j++){
				treatment.addContent(i, new Element("discussion").addContent(segs[j].trim()));
			}
			treatment.addContent(i, new Element("description").addContent(segs[1].trim()));
			treatment.addContent(i, new Element("taxon_identification").addContent(segs[0].trim()+"#"+rank));

			tnp.process(treatment);
			op.process(treatment);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
