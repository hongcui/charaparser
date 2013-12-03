/**
 * 
 */
package edu.arizona.sirls.biosemantics.charactermarkup;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;

/**
 * @author updates
 *
 */
public class StructureNameNormalizer {
	private static final Logger LOGGER = Logger.getLogger(StructureNameNormalizer.class);
	private String tableprefix;
	private Connection conn;
	private static XPath statementpath;
	//private static XPath clausestartpath;
	private static Hashtable<String, ArrayList<String>> partof = new Hashtable<String, ArrayList<String>>();
	
	static{
		try{
			statementpath = XPath.newInstance(".//statement");
			//clausestartpath = XPath.newInstance(".//structure[@clausestart]");
			
			//read in serialized partof hashtable
			File file = new File(ApplicationUtilities.getProperty("ontopartof.bin"));
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					file));
			// Deserialize the object
		    partof = (Hashtable<String, ArrayList<String>>) in.readObject();  
		}catch(Exception e){
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator"), e);
		}
	}
	/**
	 * 
	 */
	public StructureNameNormalizer(Connection conn, String tableprefix) {
		this.conn = conn;
		this.tableprefix = tableprefix;
	}
	
	/**
	 * use part_of relations and the clues of 'comma' and capitalization to determine the parentorgan of a structure 
	 * for example, Leaves large, blade smooth.
	 * 
	 * Assuming a Capitalized structure is a structure that doesn't need a parentorgan to modify (e.g. Stem) [note: can normalize ;-separated sentences by capitalizing the leading structure in a clause]
	 *  
	 * 
	 * 
	 * how are comma and capitalization used? 
	 * with the assumption that Capitalized structure is the main structure (leaves) and parts are described after ',', we can attach 'leaf' as the parent organ to 'blade' (leaf blade);
	 * this may not be a reliable solution at all, (eg, Blade smooth, petiole hairy) but may work for some descriptions (especially human annotated descriptions to meet this expectation)
	 * @param startsent
	 */
	@SuppressWarnings("unchecked")
	public Element normalize(Element description) {
		try{
			String parentorgan=null;
			//XMLOutputter xo1 = new XMLOutputter(Format.getPrettyFormat());
			//System.out.println(xo1.outputString(description));
			List<Element> statements = statementpath.selectNodes(description);
			for(Element statement: statements){
				if(statement.getChild("text").getText().matches("^[A-Z].*")){ //record parentorgan
					Element structure = statement.getChild("structure"); //get 1st structure
					if(structure!=null){
						//attach parent organ to other structures in this statement, return parentorgan used.
						parentorgan = attachPOto(description, statement, structure, "");		
					}
				}else{//sentences not starting with a capitalized structure names => those structures after ';'
					if(parentorgan!=null){
						Element struct = statement.getChild("structure");
						if(struct!=null){
							//apply parentorgan + localpo(struct) to other structures in the statement
							attachPOto(description,statement, struct, parentorgan);
							
							//then apply parentorgan to the first structure 
							String pocp = parentorgan;
							String pchain = Utilities.getStructureChain(description,"//relation[@name='part_of'][@from='"+struct.getAttributeValue("id")+"']", 3).replace(" of ", ",").trim(); //part of organ of organ
							if(pchain.length()>0){ //use explicit part_of 
								System.out.println("===>[pchain] use '"+pchain+"' as constraint to '"+Utilities.getStructureName(description, struct.getAttributeValue("id"))+"'");
								Utilities.addAttribute(struct, "constraint", formatParentOrgan(pchain)); 
							}else{
								String part = Utilities.getStructureName(description, struct.getAttributeValue("id"));								
								parentorgan = hasPart(parentorgan, part);
								if(parentorgan.length()>0){
									System.out.println("===>[part of 2] use '"+parentorgan+"' as constraint to '"+Utilities.getStructureName(description, struct.getAttributeValue("id"))+"'");
									Utilities.addAttribute(struct, "constraint", formatParentOrgan(parentorgan));
								}else{
									//quite strong an assumption that the organ of the first clause is the parent organ of all following clauses.
									//If this is not true, or the part_of hashtable is complete, comment out this part.
									System.out.println("===>[default] use '"+pocp+"' as constraint to '"+Utilities.getStructureName(description, struct.getAttributeValue("id"))+"'");
									Utilities.addAttribute(struct, "constraint", formatParentOrgan(pocp));
									parentorgan = pocp;
								}
							}
							
						}
					}
				}
			}
			
		}catch(Exception e){
			LOGGER.error("",e);
		}
		return description;
	}
	
	/**
	 * form and apply a parent organ to parts
	 * 
	 * 
	 * 
	 * 
	 * @param description: 
	 * @param statement
	 * @param parentstruct element holding a candidate parent
	 * @param parentofparentstructure string of the name of the parent organ of the parentstructure
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String attachPOto(Element description, Element statement, Element parentstruct, String parentofparentstructure) {
		String parentorgan = null;
		String porgan = null;
		if(parentstruct!=null){
			//check for 'part_of' relation on parentstructure
			String pchain = Utilities.getStructureChain(description,"//relation[@name='part_of'][@from='"+parentstruct.getAttributeValue("id")+"']", 3).replace(" of ", ",").trim(); //part of organ of organ
			if(pchain.length()>0){ //use explicit part_of 
				parentofparentstructure = pchain;
			}
			//add constraint organ to parentorgan list 
			String constraint = parentstruct.getAttribute("constraint") !=null? parentstruct.getAttributeValue("constraint") : null;
			if(constraint!=null){ 
				if(Utilities.isOrgan(constraint, conn, tableprefix)){
					//parentorgan = constraint; //use the constraint of parentstruct as parentorgan, e.g. leaf blade ..., petiole ..., vein ....
					parentofparentstructure = constraint +","+parentofparentstructure; //blade, leaf
				}
			}
			//add name organ to parentorgan list
				//parentorgan = parentofparentstructure+" "+parentstruct.getAttributeValue("name");//leaf blade
			porgan = parentstruct.getAttributeValue("name")+","+parentofparentstructure; //blade, leaf
			
			//parentorgan = parentorgan.trim();
			//attach parentorgan to other 'structures' in this statement
			List<Element> structures = statement.getChildren(); //could include 'relation' too
			for(Element struct: structures){ 
				if(struct.getName().compareTo("structure")==0){
					if(!struct.equals(parentstruct)){//skip the 1st structure which is parentstruct
						String partpchain = Utilities.getStructureChain(description,"//relation[@name='part_of'][@from='"+struct.getAttributeValue("id")+"']", 3).replace(" of ", ",").trim(); //part of organ of organ
						String part = Utilities.getStructureName(description, struct.getAttributeValue("id"))+","+partpchain;								
						//
						parentorgan = hasPart(porgan, part);
						if(parentorgan.length()>0){
							System.out.println("===>[part of1] use '"+parentorgan+"' as constraint to '"+Utilities.getStructureName(description, struct.getAttributeValue("id"))+"'");
							Utilities.addAttribute(struct, "constraint", formatParentOrgan(parentorgan));
						}else if(possess(parentstruct, struct, description)){
							parentorgan = formatParentOrgan(porgan);
							System.out.println("===>[possess] use '"+parentorgan+"' as constraint to '"+Utilities.getStructureName(description, struct.getAttributeValue("id"))+"'");
							Utilities.addAttribute(struct, "constraint", formatParentOrgan(parentorgan));
						}

					}
				}
			}
		}
		return parentorgan !=null? parentorgan : porgan.replaceAll("(^,|,$)", "");
	}
	
	/**
	 * 
	 * @param parentorgan a list of organ, separated by ',', listed from suborgan to parent organ.
	 * @param partorgans a list of organ, separated by ',', listed from suborgan to parent organ.
	 * @return appropriate parentorgan for 'name' of <structure>. The parentorgan may be from the parentorgan list or an empty string. Format: leaf blade.
	 */
	private String hasPart(String parentorgans, String partorgans){
		parentorgans = parentorgans.trim().replaceAll("(^,|,$)", "");
		partorgans = partorgans.trim().replaceAll("(^,|,$)", "");
		
		//non-specific organ parts
		String nonspecificparts = "\\b(component|part|section|area|portion|apex|side|margin|edge|body|base|center|centre|surface)\\b";
		if(partorgans.matches(nonspecificparts)){
			parentorgans = parentorgans.replaceAll("(apex|side|margin|edge|base|center|centre|surface|,)+", ""); //"base of surface, stem" does not make sense.
			return parentorgans;
		}
		String[] parts = partorgans.replaceFirst(".*(\\b"+nonspecificparts+"\\b| |,)+", "").replaceFirst("^\\s*,", "").trim().split("\\s*,\\s*");
		String[] parents = parentorgans.split("\\s*,\\s*");
		
		int cut = -1;
		for(String part:  parts){
			for(int i = 0; i < parents.length; i++){
				if(isPart(part, parents[i])){
					cut = i;
					break;
				}
			}
			if(cut>=0) break;
		}

		if(cut >=0){
			String po = "";
			for(int i = cut; i<parents.length; i++){
				if(parents[i].length()>0)
					po += parents[i]+" , ";
			}
			return po.replaceFirst(" , $", "");
		}

		return "";
	}
	
	/**
	 * 
	 * @param parentorgans  "blade,leaf"
	 * @return "leaf blade"
	 */
	private String formatParentOrgan(String parentorgans) {
		String formatted = "";
		String[] words = parentorgans.split("\\s*,\\s*");
		for(int i = words.length-1; i>=0; i--){
			formatted += words[i]+" ";
		}
 		return formatted.trim();
	}

	private boolean isPart(String part, String parent){
		return StructureNameNormalizer.partof.get(part)==null? false : StructureNameNormalizer.partof.get(part).contains(parent);
	}
	/**
	 * 
	 * @param structure
	 * @param struct
	 * @return true if structure possess [with, has, posses] struct. This could be expressed as relation or as character constraint
	 */
	
	private boolean possess(Element structure, Element struct, Element description) {
		String idw = structure.getAttributeValue("id");
		String idp = struct.getAttributeValue("id");
		try{
			XPath rel = XPath.newInstance(".//relation[@name='with'][@from='"+idw+"'][@to='"+idp+"']|"
					+ ".//relation[@name='has'][@from='"+idw+"'][@to='"+idp+"']|"
					+ ".//relation[@name='consist_of'][@from='"+idw+"'][@to='"+idp+"']|"
					+ ".//relation[@name='possess'][@from='"+idw+"'][@to='"+idp+"']");
			if(rel.selectNodes(description).size()>0) return true;
			
			rel = XPath.newInstance(".//character[contains(@constraintid,'"+idp+"')]");
			
			List<Element>characters = rel.selectNodes(description);
			for(Element character: characters){
				if(character.getParentElement().getAttributeValue("id").compareTo(idw)==0 &&
				(character.getAttributeValue("constraint").matches("^with\\b.*")||character.getAttributeValue("constraint").matches("^consist[_ -]of\\b.*")))
					return true;
			}
		}catch(Exception e){
			LOGGER.error("",e);
		}
		return false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection conn = null;
		String inputdir = "C:/Users/updates/CharaParserTest/GoldenGATE_no_schema/target/final4matrix";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				//String URL = "jdbc:mysql://localhost/"+database+"?user=ApplicationUtilities.getProperty("database.username")&password=termspassword&connectTimeout=0&socketTimeout=0&autoReconnect=true";
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			String prefix = "gg_noschema";
			StructureNameNormalizer snn = new StructureNameNormalizer(conn, prefix);
			XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
			File[] files = new File(inputdir).listFiles();
			for(File file: files){
				if(file.getName().compareTo("dry.pdf.4matrix-after-partoftable_preferredterms.xml")!=0) continue;
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();
				Element description = snn.normalize((Element)XPath.selectSingleNode(root, "//description"));
				System.out.println();
				System.out.println(xo.outputString(description));
			}
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
