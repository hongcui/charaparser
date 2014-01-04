/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import edu.arizona.sirls.biosemantics.parsing.ParsingUtil;

/**
 * @author Hong Cui
 * created for Gleason_and_Cronquist_1991
 *
 */
public class Text2XML {

	String processorname = "Cui, Hong using Text2XML java application init commit, validated against beeSchema.xsd revision f0a80a8516a06e51224d01314403eb26d60f881d";
	public static String ranks = "family\\b|genus\\b|species\\b|var\\."; //\. and \b won't match at the same time.
	public static int keycount = 0;
	static int numberh = 0;
	static int numberl = 0;
	/**
	 * 
	 */
	public Text2XML(String inputpath, String source, String outfoldert, String outfolderk) {
		
		File file = new File(inputpath);
		IdentificationKeyProcessor keyprocessor = new IdentificationKeyProcessor();
		TaxonDescriptionProcessor tdprocessor = new TaxonDescriptionProcessor();
		TaxonNameProcessor tnprocessor = new TaxonNameProcessor();
		try{
		//read file line by line
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		boolean inkey = false;
		boolean intaxon = false;
		String rank = "";

		Document doc = newdocument(source);
		int taxonparacount = 0;
		while ((line = br.readLine()) != null) {
			line=line.trim();
		   if(line.contains("from pdf]")) continue; //skip: [page 25 from pdf]
		   else if(line.contains("[KEY]")) inkey = true;
		   else if(line.contains("[END OF KEY]")){
			   inkey = false;
			   keyprocessor.process(doc.getRootElement());
			   outputKey(doc, outfolderk); //add keyfile
		   }
		  else if(inkey) addKey(doc, line);
		  else if(isTaxonPara(line)){//a taxon paragraph: a taxon name or a full description of a taxon including the name 
			   if(doc.getRootElement().getChild("taxon_identification")!=null || 
					   doc.getRootElement().getChild("full_description")!=null  ){
				   tnprocessor.process(doc.getRootElement()); //process name first
				   tdprocessor.process(doc.getRootElement());
				   String title = doc.getRootElement().getChild("taxon_identification").getChild("taxon_hierarchy").getTextNormalize().replaceAll("\\W", "_").replaceFirst("_+$", "");
				   Element number =  doc.getRootElement().getChild("number");
				   if(number != null)
					   title = (number.getTextNormalize()+" "+title).trim();
				   writeDocument(doc, outfoldert, title);
				   doc = newdocument(source);
			   }
			   String cleanline = processNumberEtc(line, doc); //genus and below often has a number e.g. "1. PHYSOCARPUS"
			   rank = findRank(cleanline);
			   intaxon = true;
			   if(fullDescription(cleanline)){ //this line contains name, description and other info about a taxon
				   taxonparacount++;
				   doc.getRootElement().addContent(new Element("full_description").addContent(cleanline+"#"+rank));				   
			   }else{ //this line only contains name info
				   doc.getRootElement().addContent(new Element("taxon_identification").addContent(cleanline+"#"+rank));				   				   
			   }
		   }else if(intaxon){
			   if(isDescription(line, taxonparacount, intaxon)){
				   taxonparacount++;
				   doc.getRootElement().addContent(new Element("description").addContent(line));				   
			   }else{
				   if(line.matches(".*?\\w.*"))
					   doc.getRootElement().addContent(new Element("discussion").addContent(line));				   
			   }
		   }	   
		}
		br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String processNumberEtc(String line, Document doc) {
		if(line.matches("^\\d+\\.[\\t ]+[A-Z].*")){
			String num = line.substring(0, line.indexOf(".")); 
			line = line.substring(line.indexOf(".")+1).trim();
			doc.getRootElement().addContent(new Element("number").addContent(num));
		}
		return line;
	}

	private String findRank(String name) {
		Pattern p = Pattern.compile("^("+ranks+").*"); //family
		Matcher m = p.matcher(name.toLowerCase());
		if(m.matches())
			return m.group(1).trim();
		
		p = Pattern.compile(".*?([A-Z]\\S+) .*"); //ALLCAP is a genus
		m = p.matcher(name);
		if(m.matches() && m.group(1).matches("[^a-z1-9]{3,}"))
			return "genus";
		
		p = Pattern.compile("\\b("+ranks+")\\b.*"); //ranks lower than species
		m = p.matcher(name.toLowerCase());
		if(m.matches())
			return m.group(1).trim();

		return "species";
	}

	/**
	 * 
	 * @param line
	 * @param taxonparacount : number of paragraphs before this line and after a taxon name
	 * @param intaxon : appears after a taxon name
	 * @return
	 */
	private boolean isDescription(String line, int taxonparacount,
			boolean intaxon) {
		if(intaxon && taxonparacount==0) return true;
		return false;
	}

	/**
	 * the paragraph line contain a complete description: name + description + [distribution etc.]
	 * @param line
	 * @return
	 */
	private boolean fullDescription(String line) {
		return line.indexOf("—")>0;
	}

	/**
	 * 
	 * @param line
	 * @return
	 */
	private boolean isTaxonPara(String line) {
		return line.matches("^\\d+\\.[\\t ]+.*") || line.toLowerCase().matches("^(family|var\\.)\\s+.*");
	}

	/**
	 * save the key in a separate file
	 * leave in doc a reference to the key_file
	 * @param doc
	 * @param name
	 * @param rank
	 */

	private void outputKey(Document doc, String outfolder) {
		Element treatment = doc.getRootElement();
		Element key = treatment.getChild("key");
		int i = treatment.indexOf(key);
		key.detach();
		String title = "";
		Element heading = key.getChild("key_heading");
		if(heading==null){
			title = "Key "+Text2XML.keycount++;
			key.addContent(0, new Element("key_heading").addContent(title));
		}else{
			title = heading.getTextNormalize();
		}
		treatment.addContent(i, new Element("key_file").addContent(title));
		writeDocument(new Document().addContent(key), outfolder, title);
		
	}
	

	private void writeDocument(Document doc, String outputfolder, String title) {
		Element root = doc.getRootElement();
		root.detach();
		if(root.getName().compareTo("treatment")==0){
		try{
			//move other_name from taxon_identification to treatment
			List<Element> others = XPath.selectNodes(root, ".//taxon_identification/other_name");
			List<Element> tis = XPath.selectNodes(root, ".//taxon_identification");
			int i = root.indexOf(tis.get(tis.size()-1)); 
			for(Element other: others) root.addContent(i+1, (Content) other.clone());
			for(Element other: others) other.detach();
			
			//move number to after taxon_identification
			Element number = (Element) XPath.selectSingleNode(root, ".//number");
			if(number!=null){
				number.detach();
				root.addContent(i+1, (Content)number.clone());
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		}
		ParsingUtil.outputXML(root, 
				new File(outputfolder, title+".xml"), null);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		System.out.println();
		System.out.println("write "+title);
		System.out.println(outputter.outputString(root));
	}

	/**
	 * add <key>line</key> in root element 'treatment'
	 * @param doc
	 * @param line
	 */
	private void addKey(Document doc, String line) {
		//3 Cal deciduous; styles 3-4; fr scarcely 1 cm; intr .... 3. P. sieboldii.
		//=>3###Cal deciduous; styles 3-4; fr scarcely 1 cm; intr ### 3. P. sieboldii.
		line = line.trim().replaceFirst("(?<=^\\d+) ", "###");
		line = line.trim().replaceFirst("[...]{3,}", "@@@");
		doc.getRootElement().addContent(new Element("key").addContent(line));		
	}

	/**
	 * create required elements
	 * @param source
	 * @return
	 */
	private Document newdocument(String source) {
		Document doc = new Document();
		Element treatment = new Element("treatment");
		doc.addContent(treatment);
		//meta
		Element meta = new Element("meta");
		meta.addContent(new Element("source").addContent(source));
		Element processor = new Element("processor").setAttribute("process_type", "format_conversion");
		meta.addContent(new Element("processed_by").addContent(processor.addContent(processorname)));
		treatment.addContent(meta);
		//taxon_identification
		//treatment.addContent(new Element("taxon_identification"));
		//description
		//treatment.addContent(new Element("description"));
		return doc;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputpath = "C:/Users/updates/CharaParserTest/Gleason_and_Cronquist/Gleason_and_Cronquist_Rosaceae_1991-reformated.txt";
		String source = "Gleason and Cronquist 1991";
		String outfoldert = "C:/Users/updates/CharaParserTest/Gleason_and_Cronquist/taxonomy";
		String outfolderk = "C:/Users/updates/CharaParserTest/Gleason_and_Cronquist/key";
		Text2XML t2x = new Text2XML(inputpath, source, outfoldert, outfolderk);
	}

}

