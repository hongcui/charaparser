/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

/**
 * @author Hong Cui
 * 
 * need to review the results of markupRest (set breakpoint for ps.length>2
 *
 */
public class TaxonNameProcessor {
	Pattern subspecificptn = Pattern.compile("(.*?)("+Text2XML.ranks+")(.*)");
	/**
	 * 
	 */
	public TaxonNameProcessor() {
	}

	public void process(Element treatment){		
		Element name = treatment.getChild("taxon_identification");
		if(name!=null){
			Element ti = new Element("taxon_identification").setAttribute("status", "ACCEPTED");
			int i = treatment.indexOf(name);
			name.detach();
			treatment.addContent(i, ti);
			String text = name.getTextNormalize();
			String rank = text.substring(text.indexOf("#")+1);
			String namestr = Text2XML.deAccent(text.substring(0, text.indexOf("#")).toLowerCase());
			System.out.println("namestr: "+namestr);
			if(rank.startsWith("fam")){ //one word name without authority
				markupFamilyName(ti, rank, namestr); 
			}else if(rank.compareTo("genus")==0 || rank.startsWith("subgen") || rank.compareTo("series")==0 ||rank.compareTo("section")==0 || rank.compareTo("tribe")==0){//one word name with authority
				markupGenusName(ti, rank, namestr);
			}else if(rank.compareTo("species")==0){//two-word name
				markupSpeciesName(ti, rank, namestr);
			}else{//sub-specific ranks
				Matcher m = subspecificptn.matcher(namestr);
				if(m.matches()){
				String species = m.group(1).trim();
				String r = m.group(2).trim();
				String subsp = m.group(3).trim();
				if(species.length()>0) markupSpeciesName(ti, "species", species);
				if(subsp.length()>0) markupVarName(ti, r, namestr);
				}								
			}
			TaxonHierarchyBuilder thb = new TaxonHierarchyBuilder();
			ti.addContent(thb.taxonHierarchy(ti));
		}
	}

	private void markupVarName(Element ti, String rank, String namestr) {
		namestr = namestr.replaceFirst(rank, "").trim();
		int end = namestr.indexOf(" ")>0? namestr.indexOf(" ") : namestr.length()-1;
		String n = namestr.substring(0, end).replaceAll("\\W+$", "");
		if(rank.contains(".")) rank = spellOut(rank);
		ti.addContent(new Element(rank+"_name").addContent(n));
		String rest = namestr.substring(end).trim();
		markupRest(ti, rank, rest);
	}

	private void markupSpeciesName(Element ti, String rank, String namestr) {
		String n = namestr.substring(0, namestr.indexOf(" "));
		if(n.length()==1 ||(n.length()==2 && n.matches(".*\\W$"))){
			if(Text2XML.lastgenusname!=null && n.toLowerCase().charAt(0)==Text2XML.lastgenusname.toLowerCase().charAt(0))
				n = Text2XML.lastgenusname;
			else
				System.out.println("genus name expansion problem for: "+n);
		}
		ti.addContent(new Element("genus_name").addContent(n));
		namestr = namestr.substring(namestr.indexOf(" ")).trim();
		n = namestr.substring(0, namestr.indexOf(" ")).replaceAll("\\W+$", "");
		ti.addContent(new Element("species_name").addContent(n));
		String rest = namestr.substring(namestr.indexOf(" ")).trim(); //authority and common name
		//L. Sour cherry, pie-cherry.
		if(rest.length()>0)
		markupRest(ti, rank, rest);
	}

	private void markupRest(Element ti, String rank, String rest) {
		//nom. conserv.
		if(rest.contains("nom. conserv.")){
			ti.setAttribute("status", "nom.conserv.");
			String [] parts = rest.split("nom. conserv.");
			if(parts[0].trim().length()>0){
				ti.addContent(new Element(rank+"_authority").addContent(parts[0]));
			}
			if(parts.length>1 && parts[1].trim().length()>0){
				ti.addContent(new Element("other_name").addContent(parts[1]));
			}
			return;
		}
		String pauth = "";
		//(authority): must followed by another authority
		if(rest.startsWith("(")){
			pauth = rest.substring(rest.indexOf("("), rest.indexOf(")")+1);
			rest = rest.substring(rest.indexOf(")")+1).trim();
		}

		rest = rest.replaceAll("(\\.\\s*){2,}", ".").replaceAll("\\.$", "");//multiple '.' into one and remove the last '.'
		
		
		rest = hidePeroidsInBrackets(rest);
		//namestr: duchesnea j. e. smith. => no common name
		//potentilla l. cinquefoil or five-fingers, names applied to the spp. with 5 lfls.
		// potentilla robbinsiana oakes. white-mt. potentilla.
		//rubus allegheniensis t. c. porter =>no common name
		String [] ps = rest.split("\\. "); //need to have the space: e.g "sorbus aucuparia c. k. schneider. european m.-a.; rowan-tree."	
		for(int i = 0; i < ps.length; i++) ps[i] = unhidePeriodsInBrackets(ps[i]);
		String joint = "";
		for(int i = 0; i < ps.length-1; i++) joint += ps[i]+". ";
		String[] parts = null;
		if(joint.length()>0){
			parts = new String[2];
			parts[0] = joint.trim();
			parts[1] = ps[ps.length-1];
		}else{
			parts = new String[1];
			parts[0] = ps[ps.length-1];
		}
		if(parts.length>1){//authority and common name
			ti.addContent(new Element(rank+"_authority").addContent((pauth+" "+parts[0]).trim()));
			ti.addContent(new Element("other_name").addContent(parts[1])); //commonname
		}else{
			if(parts[0].contains("(")){ // maxim, (with leaves of viburnum opulus). in Gray's.
				String otherinfo = parts[0].substring(parts[0].indexOf("("));
				parts[0] = parts[0].substring(0, parts[0].indexOf("("));
				ti.addContent(new Element(rank+"_authority").addContent((pauth+" "+parts[0]).trim()));
				ti.addContent(new Element("other_name").addContent(otherinfo)); //commonname
			}else
			ti.addContent(new Element(rank+"_authority").addContent((pauth+" "+parts[0]).trim()));
		}
	}

	private String unhidePeriodsInBrackets(String string) {
		return string.replaceAll("\\[DOT\\]", ".")
		.replaceAll("\\[QST\\]", "?")
		.replaceAll("\\[SQL\\]", ";")
		.replaceAll("\\[QLN\\]", ":")
		.replaceAll("\\[EXM\\]", "!");
	}

	/**
	 * avoid break up sentence by \. 
	 * @param rest
	 * @return
	 */
	private String hidePeroidsInBrackets(String rest) {		
			String text = rest.replaceAll("\\(", " ( ").replaceAll("\\(", " ( ")
					.replaceAll("\\)", " ) ").replaceAll("\\)", " ) ")
					.replaceAll("\\[", " [ ").replaceAll("\\[", " [ ")
					.replaceAll("\\]", " ] ").replaceAll("\\]", " ] ")
					.replaceAll("\\{", " { ").replaceAll("\\{", " { ")
					.replaceAll("\\}", " } ").replaceAll("\\}", " } ");

			int lround=0;
			int lsquare=0;
			int lcurly=0;

			String hidden= "";

			String[] tokens = text.split("\\s+");

			for (String t: tokens){
				if(t.equals("(")){
					lround++;
					hidden += "(";	
				}else if(t.equals(")")){
					lround--;
					hidden += ")";
				}else if(t.equals("[")){
					lsquare++;
					hidden += "[";
				}else if(t.equals("]")){
					lsquare--;
					hidden += "]";
				}else if(t.equals("{")){
					lcurly++;
					hidden += "{";
				}else if(t.equals("}")){
					lcurly--;
					hidden += "}";
				}else{
					if(lround+lsquare+lcurly>0){
						if(t.matches(".*?[\\.\\?\\;\\:\\!].*?")){
							t = t.replaceAll("\\.", "[DOT]");
							t = t.replaceAll("\\?", "[QST]");
							t = t.replaceAll(";", "[SQL]");
							t = t.replaceAll(":", "[QLN]");
							t = t.replaceAll("!", "[EXM]");
						}
					}
					hidden += t;
					hidden += " ";
				}
			}	
			return hidden.replaceAll("(?<=[\\(\\[\\{])\\s*", "").replaceAll("\\s*(?=[\\)\\]\\}])", "").replaceAll("\\.\\s*&", "[DOT] &");
		}


	private void markupGenusName(Element ti, String rank, String namestr) {
		//PRUNUS L.C.SOMEONE. Sour cherry, pie-cherry.
		namestr = namestr.replaceFirst("^"+rank+" ", "").trim();
		//Subgen. I. PRUNOPHORA Focke (PLUMS)
		//remove number, could als be roman numbers IV
		if(namestr.matches("^(\\d+|[ivx]+)\\.? .*"))
			namestr = namestr.substring(namestr.indexOf(" ")).trim();
		int end = namestr.indexOf(" ")>0? namestr.indexOf(" ") : namestr.length()-1;
		String n = namestr.substring(0, end).replaceAll("\\W+$", "");
		if(rank.contains(".")) rank = spellOut(rank);
		if(rank.equals("genus")) Text2XML.lastgenusname = n;
		ti.addContent(new Element(rank+"_name").addContent(n));
		if(namestr.indexOf(" ")>0){
			String rest = namestr.substring(namestr.indexOf(" ")).trim(); //authority and common name
			if(rest.length()>0){
				markupRest(ti, rank, rest);
			}
		}
	}

	private void markupFamilyName(Element ti, String rank, String namestr) {
		namestr = namestr.replaceFirst("^"+rank+" ", "").trim();
		//fam. 82. rosàceae (rose family)
		//remove number, could als be roman numbers IV
		if(namestr.matches("^(\\d+|[ivx]+)\\.? .*"))
			namestr = namestr.substring(namestr.indexOf(" ")).trim();
		int end = namestr.indexOf(" ")>0? namestr.indexOf(" ") : namestr.length()-1;
		String n = namestr.substring(0, end).replaceAll("\\W+$", "");
		if(rank.contains(".")) rank = spellOut(rank);
		ti.addContent(new Element(rank+"_name").addContent(n));
		String rest = namestr.substring(end).replaceAll("(\\W+$|^\\W+)", "").trim();
		if(rest.length()>0){
			ti.addContent(new Element("other_name").addContent(rest.replaceAll("(^\\W|\\W$)", ""))); //FAMILY ROSACEAE, the Rose Family
		}
	}
	
	/**
	 * var. => variety
	 * @param rank
	 * @return
	 */
	private String spellOut(String rank) {
		if(rank.toLowerCase().compareTo("var.")==0) return "variety";
		if(rank.toLowerCase().compareTo("fam.")==0) return "family";
		if(rank.toLowerCase().compareTo("subgen.")==0) return "subgenus";
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
