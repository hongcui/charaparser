package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.VolumeTransformer;

public class TaxonHierarchyBuilder {
	//for taxon hierarchy calculation
	private static ArrayList<String> taxonranks = new ArrayList<String>(); // from high to low, keep the current taxon_hierarchy.
	private static ArrayList<String> taxonnames = new ArrayList<String>(); // names corresponding to the ranks in the current hierarchy.
	public static ArrayList<String> ranksinorder = new ArrayList<String>(); //fixed order from high to low
	private static final Logger LOGGER = Logger.getLogger(TaxonHierarchyBuilder.class);
	static{
		ranksinorder.add("family");
		ranksinorder.add("subfamily");
		ranksinorder.add("tribe");
		ranksinorder.add("subtribe");
		ranksinorder.add("genus");
		ranksinorder.add("subgenus");
		ranksinorder.add("section");	
		ranksinorder.add("series");
		ranksinorder.add("species");
		ranksinorder.add("subspecies");
		ranksinorder.add("variety");
		ranksinorder.add("form");
		ranksinorder.add("forma"); //forma = form
	}
	
	public TaxonHierarchyBuilder() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * records higher taxa of this taxon
	 * @param taxonid : accepted taxon id
	 * 
	 * <TaxonIdentification Status="ACCEPTED">
    	<genus_name>Cycas</genus_name>
    	<species_name>multipinnata</species_name>
    	<species_authority>C. J. Chen &amp; S. Y. Yang</species_authority>
    	<place_of_publication>
      		<publication_title>Acta Phy totax. Sin.</publication_title>
      		<place_in_publication>32. 239. 1994</place_in_publication>
    	</place_of_publication>
  	   </TaxonIdentification>
  
	 * @return an element holding info on taxon hierarchy (Family A; Genus B; ...) 
	 */

	public Element taxonHierarchy(Element taxonid) {
		String th = "";
		List<Element> children = taxonid.getChildren();
		for(int i = children.size()-1; i>=0; i--){ //search backwards
			Element child = children.get(i);
			String name = child.getName();
			if(name.endsWith("_name") && name.compareTo("other_name")!=0){
				String rank = name.substring(0, name.indexOf("_"));
				String tname = child.getTextTrim();
				th = getHigherTaxonNames(rank, tname);
				//System.out.println("ranks are:");
				//for(String r: taxonranks)
				 //  System.out.println(r);
				th += rank +" "+ tname+";";
				break;
			}
		}	
		Element e = new Element(ApplicationUtilities.getProperty("taxonhierarchy"));
		e.setText(th);
		return e;
		
		
		/*String th = "";
		List<Element> children = taxonid.getChildren();
		for(Element child: children){
			String name = child.getName();
			if(name.endsWith("_name") && name.compareTo("other_name")!=0){
				String rank = name.substring(0, name.indexOf("_"));
				String tname = child.getTextTrim();
				th = getHigherTaxonNames(rank, tname);
				//System.out.println("ranks are:");
				//for(String r: taxonranks)
				 //  System.out.println(r);
				th += rank +" "+ tname+";";
			}
		}	
		Element e = new Element(ApplicationUtilities.getProperty("taxonhierarchy"));
		e.setText(th);
		return e;*/
	}

	
	/**
	 * if this rank is lower than the last rank, return all saved ranks then save this rank, 
	 * if this rank is higher than or equal to the last rank, 
	 * 		remove ranks lower than this rank 
	 *      update the rank equal to this rank with this name
	 *      return ranks higher than this rank
	 * @param rank
	 * @return
	 */
	private String getHigherTaxonNames(String rank, String name) {
		if(taxonranks.size()==0){
			taxonranks.add(rank);
			taxonnames.add(name);
			return "";
		}
		
		String lastrank = taxonranks.get(taxonranks.size()-1);
		int compare = ranksinorder.indexOf(lastrank) - ranksinorder.indexOf(rank); //compare to the last rank
		if(compare<0){ //rank is lower
			taxonranks.add(rank);
			taxonnames.add(name);
			return getRankNameInfo(taxonranks.size()-2); 
			
		}else if(compare>0){//rank is higher
			int pos = taxonranks.indexOf(rank);
			if(pos < 0){
				LOGGER.info("need to add a new rank: "+rank);
				int r = ranksinorder.indexOf(rank); //find its order
				do{
					pos = taxonranks.indexOf(ranksinorder.get(r--)); //find the next higher order that is in taxonranks
				}while (pos < 0);
				pos++;
			}
			//remove ranks lower than this rank 
			if(pos < taxonranks.size()-1){
				for(int i = pos+1; i < taxonranks.size(); ){
					taxonranks.remove(i);
					taxonnames.remove(i);
				}
			}
			//update the rank equal to this rank with this name
			taxonranks.set(pos, rank);
			taxonnames.set(pos, name);
			//return ranks higher than this rank
			return getRankNameInfo(pos-1); 
		}else{//same ranks
			int pos = taxonranks.indexOf(rank);
			//update the rank equal to this rank with this name
			taxonranks.set(pos, rank);
			taxonnames.set(pos, name);
			//return ranks higher than this rank
			return getRankNameInfo(pos-1); 
		}		
	}
	
	
	/**
	 * string together rank and name info for ranks up to uptoindex, separate ranks by ';'
	 * @param uptoindex
	 * @return
	 */
	private String getRankNameInfo(int uptoindex) {
		String str = "";
		for(int i=0; i <=uptoindex; i++){
			str += taxonranks.get(i) +" "+ taxonnames.get(i)+";";
		}
		return str;
	}


	
}
