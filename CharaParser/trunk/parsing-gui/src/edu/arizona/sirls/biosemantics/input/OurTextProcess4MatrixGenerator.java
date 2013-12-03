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
 * This class converts the description-annotated file with our style taxon_hierarchy element to a format required by MatrixGernation
 * It converts the nomenclature section mainly (to http://biosemantics.googlecode.com/svn/trunk/characterStatements/FnaSchemaJSTOR.xsd),
 *  as the dsecription section follows the same schema.
 *  
 * Also outputs singular plural mapping file
 *
 */
public class OurTextProcess4MatrixGenerator {
	static XPath mdscrpath; //marked-up xml
	static XPath structure;
	static{
		try{
			mdscrpath = XPath.newInstance(".//description");
			structure = XPath.newInstance(".//structure");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	StringBuffer spmapping = new StringBuffer();
	HashSet<String> strterms = new HashSet<String>();
	/**
	 * 
	 */
	public OurTextProcess4MatrixGenerator(String inputdir, String outputdir, String spfilepath) {
		File output = new File(outputdir);
		if(!output.exists()) output.mkdir();
		File[] taxonxs = new File(inputdir).listFiles();
		ArrayList<Element> transformeds = new ArrayList<Element>();
		
		try{
			for(File taxon: taxonxs){
				System.out.println(taxon.getName());
				transformeds.add(convert(taxon));
			}

			
			//output transformed files
			int i = 0;
			for(Element transformed: transformeds){
				ParsingUtil.outputXML(transformed, new File(outputdir, taxonxs[i].getName()), new Comment("converted FILE "+taxonxs[i].getName()+"from internal format"));			
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

	@SuppressWarnings("unchecked")
	private Element convert(File taxon) throws Exception{
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(taxon);
		Element root = doc.getRootElement();
		Element transformed = new Element("treatment");

		//merge all <description> content (ignore free text) into one <description>
		Element description = new Element("description");
		List<Element> dscrps = XPath.selectNodes(root, "//description");
		for(Element dscrip : dscrps ){
			List<Element> statements = dscrip.getChildren(); //ignore free text, only collect statements
			for(int i = 0; i <statements.size();){
				Element statement = statements.get(i);
				statement.detach();  //implicitely i++
				description.addContent(statement);
				getSingularPlural(statement);
			}
		}

		//deal with names
		/*<nomenclature><name>Order Enoplida</name>
		 * <rank>Order</rank>
		 * <taxon_hierarchy>Order Enoplida</taxon_hierarchy>
		 * <name_info>Order Enoplida</name_info>
		 * </nomenclature>
		 * 
		 * 
		 * <TaxonIdentification id="urn:lsid:biosci.ohio-state.edu:osuc_concepts:135560 HNS" Status="ACCEPTED">
		 * <genus_name>Camponotus</genus_name>
		 * <species_name>langi</species_name>
		 * </TaxonIdentification>
		 */
		Element taxonidentification = new Element("TaxonIdentification");
		taxonidentification.setAttribute("Status", "ACCEPTED");
		
		String h = ((Element) XPath.selectSingleNode(root, "//taxon_hierarchy")).getTextNormalize();
		String[] names = h.split("\\s*;\\s*");
		for(String name: names){
			String rank = name.substring(0, name.indexOf(" "));
			name = name.replaceFirst(rank, "").trim();
			String n = name.indexOf(" ")>0? name.substring(0, name.indexOf(" ")) : name;
			Element rankedname = new Element(rank.toLowerCase()+"_name");
			rankedname.setText(n.toLowerCase());
			taxonidentification.addContent(rankedname);
		}
		transformed.addContent(taxonidentification);
		transformed.addContent(description);

		return transformed;
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
		String input ="C:\\Users\\updates\\CharaParserTest\\Abebe_FreshwaterNematodes\\target\\final";
		String output = "C:\\Users\\updates\\CharaParserTest\\Abebe_FreshwaterNematodes\\target\\final4matrix";	
		String spfile = "C:\\Users\\updates\\CharaParserTest\\Abebe_FreshwaterNematodes\\target\\singluar-plural.txt";	
		//manual copy
		//String input = "C:/Users/updates/CharaParserTest/GoldenGATE_no_schema/target/final";
		//String output = "C:/Users/updates/CharaParserTest/GoldenGATE_no_schema/target/final4matrix";
		//String spfile = "C:/Users/updates/CharaParserTest/GoldenGATE_no_schema/target/singular-plural.txt";
		OurTextProcess4MatrixGenerator t4m = new OurTextProcess4MatrixGenerator(input, output, spfile);

	}

}
