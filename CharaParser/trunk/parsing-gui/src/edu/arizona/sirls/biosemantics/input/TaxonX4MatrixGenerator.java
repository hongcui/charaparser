/**
 * 
 */
package edu.arizona.sirls.biosemantics.input;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import edu.arizona.sirls.biosemantics.parsing.ParsingUtil;
import edu.arizona.sirls.biosemantics.parsing.VolumeTransformer;

/**
 * @author Hong Cui
 * This class converts the description-annotated TaxonX file to a format required by MatrixGernation
 * It converts the nomenclature section mainly (to http://biosemantics.googlecode.com/svn/trunk/characterStatements/FnaSchemaJSTOR.xsd),
 *  as the dsecription section follows the same schema.
 *  
 * Also outputs singular plural mapping file
 *
 */
public class TaxonX4MatrixGenerator {

	static XPath treatpath;
	static XPath dscrpath;
	static XPath mdscrpath; //marked-up xml
	static XPath namepath; //marked-up xml
	static XPath familyname;
	static XPath genusname;
	static XPath speciesname;
	static XPath nameid;
	static XPath structure;
	static XPath withtaxonconstraint;
	static{
		try{
			treatpath = XPath.newInstance(".//tax:taxonxBody/tax:treatment");
			namepath = XPath.newInstance(".//tax:nomenclature");
			dscrpath = XPath.newInstance(".//tax:div[@type='description']");
			mdscrpath = XPath.newInstance(".//description");
			familyname = XPath.newInstance(".//x:Family");
			familyname.addNamespace("x", "http://digir.net/schema/conceptual/darwin/2003/1.0");
			genusname = XPath.newInstance(".//x:Genus");
			genusname.addNamespace("x", "http://digir.net/schema/conceptual/darwin/2003/1.0");
			speciesname = XPath.newInstance(".//x:specificEpithet|.//x:Species");
			speciesname.addNamespace("x", "http://digir.net/schema/conceptual/darwin/2003/1.0");
			nameid = XPath.newInstance(".//tax:name/tax:xid");
			structure = XPath.newInstance(".//structure");
			withtaxonconstraint = XPath.newInstance(".//character[@taxon_constraint]");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	StringBuffer spmapping = new StringBuffer();
	HashSet<String> strterms = new HashSet<String>();
	int pseudorank = 100000; //don't touch this number
	
	boolean removeWithTaxonContraint = true;
	/**
	 * 
	 */
	public TaxonX4MatrixGenerator(String inputdir, String outputdir, String spfilepath) {
		File output = new File(outputdir);
		if(!output.exists()) output.mkdir();
		File[] taxonxs = new File(inputdir).listFiles();
		ArrayList<Element> transformeds = new ArrayList<Element>();
		
		try{
			for(File taxon: taxonxs){
				System.out.println(taxon.getName());
				transformeds.add(convert(taxon));
			}

			String pseudo = this.pseudorank>0 && this.pseudorank!=100000? VolumeTransformer.ranksinorder.get(this.pseudorank-1): null;
			
			//output transformed files
			int i = 0;
			for(Element transformed: transformeds){
				/*if(pseudo != null){
					Element pseudoe = new Element(pseudo+"_name");
					pseudoe.setText("pseudo");
					transformed.getChild("TaxonIdentification").addContent(0, pseudoe);
				}*/
				ParsingUtil.outputXML(transformed, new File(outputdir, taxonxs[i].getName()), new Comment("converted FILE "+taxonxs[i].getName()+"to TaxonX format"));			
				i++;
			}
			//output s-p mapping
			System.out.println("write sp file");
			FileWriter fw = new FileWriter(new File(spfilepath));
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(spmapping.toString());
			bw.close();
			System.out.println("done");
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * assuming taxon contains one treatment has one <tax:nomenclature> and multiple  <tax:div type="description">, which as multiple <description> 
	 * @param taxon
	 * @return
	 * @throws Exception
	 */

	private Element convert(File taxon) throws Exception{
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(taxon);
		Element root = doc.getRootElement();
		Element transformed = new Element("treatment");

		//merge all <description> content (ignore free text) into one <description>
		Element description = new Element("description");
		List<Element> dscrps = dscrpath.selectNodes(root);
		for(Element dscrip : dscrps ){
			List<Element> mdscrps = mdscrpath.selectNodes(dscrip);
			for(Element mdscrp: mdscrps){
				List<Element> statements = mdscrp.getChildren(); //ignore free text, only collect statements
				for(int i = 0; i <statements.size();){
					Element statement = statements.get(i);
					if(this.removeWithTaxonContraint){
						List<Element> chwtc = withtaxonconstraint.selectNodes(statement);
						for(Element ch: chwtc){
							ch.detach();
						}
					}
					statement.detach();//implicitely i++
					description.addContent(statement);
					getSingularPlural(statement);
				}
			}
		}

		//deal with names
		Element taxonidentification = new Element("TaxonIdentification");
		Element name = (Element) namepath.selectSingleNode(root); //assume there is one normenclature element
		Element f = (Element) familyname.selectSingleNode(name);
		Element g = (Element) genusname.selectSingleNode(name);
		Element s = (Element) speciesname.selectSingleNode(name);
		String famname =f!=null? f.getTextNormalize(): "";
		String gename = g!=null? g.getTextNormalize(): "";
		String spname = s!=null? s.getTextNormalize(): "";
		Element nameid = (Element) this.nameid.selectSingleNode(name);
		String id = nameid!=null && nameid.getAttribute("identifier")!=null? nameid.getAttributeValue("identifier"): "";
		String idsrc = nameid!=null && nameid.getAttribute("source")!=null? nameid.getAttributeValue("source"): "";
		String status = (id.length()>0? id+" "+idsrc: "").trim();
		taxonidentification.setAttribute("Status", "ACCEPTED");
		taxonidentification.setAttribute("id", status);
		if(famname.length()>0){
			Element fname = new Element("family_name");
			fname.setText(famname);
			taxonidentification.addContent(fname);
			updatePseudo("family");
		}

		if(gename.length()>0){
			Element gname = new Element("genus_name");
			gname.setText(gename);
			taxonidentification.addContent(gname);
			updatePseudo("genus");
		}

		if(spname.length()>0){
			Element sname = new Element("species_name");
			sname.setText(spname);
			taxonidentification.addContent(sname);
			updatePseudo("species");
		}

		transformed.addContent(taxonidentification);
		transformed.addContent(description);

		return transformed;
	}
	
	
	/**
	 * this.pseudo should hold the highest rank appeared in this collection
	 * @param string
	 */
	private void updatePseudo(String string) {	
		if(this.pseudorank >  VolumeTransformer.ranksinorder.indexOf(string)){
			this.pseudorank = VolumeTransformer.ranksinorder.indexOf(string);
		}
	}

	//collect singular-plural mapping from name and name_original attributes
	private void getSingularPlural(Element statement) throws Exception{
		List<Element> structs = structure.selectNodes(statement);
		for(Element struct: structs){
			String name = struct.getAttributeValue("name").replaceAll("[{}]", "");
			String oname = struct.getAttributeValue("name_original").replaceAll("[{}]", "");
			if(oname.length()==0) oname = name;
			if(!strterms.contains(name)){
				strterms.add(name);
				spmapping.append(name+"\t"+oname+System.getProperty("line.separator"));
			}
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String input = "C:\\Users\\updates\\CharaParserTest\\GoldenGATE_no_schema\\target\\final";
		//String output = "C:\\Users\\updates\\CharaParserTest\\GoldenGATE_no_schema\\target\\final4matrix";
		//String spfile ="C:\\Users\\updates\\CharaParserTest\\GoldenGATE_no_schema\\target\\singluar-plural.txt";
		String input = "C:\\Users\\updates\\CharaParserTest\\proibioPilot\\uncleaned\\20597\\target\\final";
		String output = "C:\\Users\\updates\\CharaParserTest\\proibioPilot\\uncleaned\\20597\\target\\final4matrix";	
		String spfile = "C:\\Users\\updates\\CharaParserTest\\proibioPilot\\uncleaned\\20597\\target\\singluar-plural.txt";	
		TaxonX4MatrixGenerator t4m = new TaxonX4MatrixGenerator(input, output, spfile);

	}

}
