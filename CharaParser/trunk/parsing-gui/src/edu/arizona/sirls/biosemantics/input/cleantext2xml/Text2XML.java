/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.text.Normalizer;

import edu.arizona.sirls.biosemantics.parsing.ParsingUtil;

/**
 * @author Hong Cui
 * created for Gleason_and_Cronquist_1991
 * Expected format: 
 * [Scientific Name, Description, OtherInfo]+
 * Scientific Names: 
 *       the name proper for a Family (or above Genus): must include rank e.g. 'Family'
 *       the name proper for a Genus description: all CAPITAL.
 *       the name proper for a Species description: follow a sequential number (e.g. 1. Genus Specific-epithet .)
 *       the end of scientific name info, if followed by a description in a shared paragraph, is marked by '—'. 
 * Descriptions:
 *       descriptions follow scientific name info (including name, authority, publication, name status, common name, meaning of the name) immediately in a shared paragraph or a new paragraph
 *       descriptions that appear in the same paragraph as the name are enclosed in a pair of "—". (e.g. 1. Genus Specific-epithet Authority. —Description. —OtherInfo)
 *       descriptions that appear in separate paragraphs
 *                if all description info is contained in one paragraph, set numberOfDescriptionParagraph = 1
 *                if multiple description paragraphs, set numberOfDescriptionParagraph = n, AND all non-description paragraphs should start with '—'.
 * Identification keys:
 * 		 Keys, including heading for the key, are enclosed in [KEY] and [END OF KEY] marks.	
 *       Everything after [KEY] and before the 1st key statement is considered a key_heading.
 *       The same format as FNA keys: all statements start with a number, end with either a determination or explicit/implicit next statement number. 
 *       Expects at least three dots (...) before determination or an explicit next statement number.
 *       
 *       1 statement
 *       2 statement .... 3
 *       2 statement .... determination
 *       3 statement 
 *       4 statement .... determination
 *       4 statement .....determination                      
 *
 *       1 -> 2 -> 3 ->4 ->d, d
 *            2 -> d
 */
public class Text2XML {

	String processorname = "Cui, Hong using Text2XML java application init commit r86, validated against beeSchema.xsd revision f0a80a8516a06e51224d01314403eb26d60f881d";
	public static String ranks = "family\\b|fam\\.|tribe\\b|subtribe\\b|genus\\b|subgen\\.|section\\b|series\\b|species\\b|subspecies\\b|var\\.|forma\\b"; //\. and \b won't match at the same time.
	public static int keycount = 0;
	static int numberh = 0;
	static int numberl = 0;
	String numberOfDescriptionParagraph = "n"; // "1" or "n"
	String nondescheadings ="#############"; //no such headings
	static Pattern headingptn;
	public static String lastgenusname;
	public static int sequence = 0;
	public GrayAbbreviationHandler gah;


	//fullDescription, TaxonDescriptionProcessor
	//isDescription
	//isTaxonPara
	/**
	 * 
	 */
	public Text2XML(String inputpath, String source, String author, String date, String outfoldert, String outfolderk, String nondescheadings) {
		gah = new GrayAbbreviationHandler();
		//check outfoldert
		File outt = new File(outfoldert);
		if(!outt.exists()){
			if(!outt.mkdir()){
				System.err.println(outfoldert+": can not create directory");
				System.exit(1);
			}
		}else if(!outt.isDirectory()){
			if(!outt.mkdir()){
				System.err.println(outfoldert+": can not create directory");
				System.exit(1);
			}
		}

		//check outfolderk
		File outk = new File(outfolderk);
		if(!outk.exists()){
			if(!outk.mkdir()){
				System.err.println(outfoldert+": can not create directory");
				System.exit(1);
			}
		}else if(!outk.isDirectory()){
			if(!outk.mkdir()){
				System.err.println(outfoldert+": can not create directory");
				System.exit(1);
			}
		}

		if(nondescheadings.trim().length()>0) this.nondescheadings = nondescheadings; 

		headingptn = Pattern.compile("^("+this.nondescheadings+")(.*)");

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

			Document doc = newdocument(source, author, date);
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
						doc = newdocument(source, author, date);
					}
					String cleanline = processNumberEtc(line, doc); //genus and below often has a number e.g. "1. PHYSOCARPUS"
					rank = findRank(cleanline); //rank = "fam."
					intaxon = true;
					if(fullDescription(cleanline)){ //this line contains name, description and other info about a taxon
						taxonparacount++;
						doc.getRootElement().addContent(new Element("full_description").addContent(cleanline+"#"+rank));				   
					}else{ //this line only contains name info
						doc.getRootElement().addContent(new Element("taxon_identification").addContent(cleanline+"#"+rank).setAttribute("status", "ACCEPTED"));				   				   
					}
				}else if(intaxon){
					if(isDescription(line, taxonparacount, intaxon)){
						taxonparacount++;
						doc.getRootElement().addContent(new Element("description").addContent(line));				   
					}else{
						if(line.matches(".*?\\w.*")){
							Matcher m = this.headingptn.matcher(line.trim());
							Element discussion = new Element("discussion");
							if(m.matches()){
								discussion.setAttribute("type", m.group(1).toLowerCase().replaceAll("\\W"," ").replaceAll("\\s+", "_"));
								line = m.group(2).trim();
							}
							discussion.addContent(line);
							doc.getRootElement().addContent(discussion);	
						}
					}
				}	   
			}
			//last doc
			if(doc.getRootElement().getChild("taxon_identification")!=null || 
					doc.getRootElement().getChild("full_description")!=null  ){
				tnprocessor.process(doc.getRootElement()); //process name first
				tdprocessor.process(doc.getRootElement());
				String title = doc.getRootElement().getChild("taxon_identification").getChild("taxon_hierarchy").getTextNormalize().replaceAll("\\W", "_").replaceFirst("_+$", "");
				Element number =  doc.getRootElement().getChild("number");
				if(number != null)
					title = (number.getTextNormalize()+" "+title).trim();
				writeDocument(doc, outfoldert, title);
				doc = newdocument(source, author, date);
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
		Pattern p = Pattern.compile("^("+ranks+").*"); //family and other ranks at the start of the name string
		Matcher m = p.matcher(name.toLowerCase());
		if(m.matches())
			return m.group(1).trim();

		p = Pattern.compile(".*?([A-Z]\\S+)($|.*)"); //ALLCAP is a genus
		m = p.matcher(name);
		if(m.matches() && m.group(1).matches("[^a-z1-9]{3,}"))
			return "genus";

		p = Pattern.compile("\\b("+ranks+")\\b.*"); //other ranks appearing in the mid of a name string
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
		if(line.trim().matches("^("+this.nondescheadings+").*")) return false;
		if(this.numberOfDescriptionParagraph.compareTo("n")==0){
			if(line.trim().startsWith("—")) return false;
			else return true;
		}


		return false;
	}

	/**
	 * the paragraph line contain a complete description: name + description + [otherinfo, such as distribution etc.]
	 * @param line
	 * @return
	 */
	private boolean fullDescription(String line) {
		return line.indexOf("—")>0; //index can not = 0, because that would be a OtherInfo paragraph

	}

	/**
	 * 
	 * @param line
	 * @return
	 */
	private boolean isTaxonPara(String line) {
		return line.matches("^\\d+\\.[\\t ]+.*") || line.toLowerCase().matches("^("+Text2XML.ranks+")\\s+.*");
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
		//treatment.addContent(i, new Element("key_file").addContent(title));
		treatment.addContent(i, key); //add the keys
		//writeDocument(new Document().addContent(key), outfolder, title); //and write keyfiles

	}


	private void writeDocument(Document doc, String outputfolder, String title) {
		title = deAccent(title);
		Element root = doc.getRootElement();
		root.detach();
		if(root.getName().compareTo("treatment")==0){
			try{
				title = Text2XML.sequence++ +"_"+title;
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
					if(i+1<root.getChildren().size()){
						root.addContent(i+1, (Content)number.clone()); //add to a spec location
					}else{
						root.addContent((Content)number.clone()); //add to the end
					}
				}

				//replace abbreviations in key-statements and descriptions
				//the simple replacement is problematic: as descriptions and statements also contains abbreviated authority (e.g., L.) which may be replaced with "Lake"
				/*List<Element> elements = XPath.selectNodes(root, ".//description");
				for(Element element: elements){
					description.setText(gah.toFullSpelling(element.getText()));
				}
				elements = XPath.selectNodes(root, ".//key//statement");
				for(Element element: elements){
					statement.setText(gah.toFullSpelling(element.getText()));
				}
				elements = XPath.selectNodes(root, ".//habitat_elevation_distribution_or_ecology");
				for(Element element: elements){
					element.setText(gah.toFullSpelling(element.getText()));
				}
				*/
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
		line = line.trim().replaceFirst("(?<=^\\d+)[ \\.]", "###");
		
		String [] parts = line.split("\\s*[.]{3,}\\s*");
		if(parts.length>1){
			if(parts[1].matches("^\\d+\\.?")) line = parts[0]+"###"+parts[1]; //followed by next_statement_id
			else line = parts[0]+"@@@"+parts[1]; //followed by a determination
		}

		doc.getRootElement().addContent(new Element("key").addContent(line));		
	}

	/**
	 * create required elements
	 * @param source
	 * @return
	 */
	private Document newdocument(String source, String author, String date) {
		Document doc = new Document();
		Element treatment = new Element("treatment");
		doc.addContent(treatment);
		//meta
		Element meta = new Element("meta");
		Element src = new Element("source");
		src.addContent(new Element("author").addContent(author));
		src.addContent(new Element("date").addContent(date));
		src.addContent(new Element("title").addContent(source));
		meta.addContent(src);
		Element processedby = new Element("processed_by");
		Element processor = new Element("processor");
		processedby.addContent(processor);
		processor.addContent(new Element("date").addContent((new Date()).toString()));
		Element software = new Element("software").setAttribute("type", "format_conversion");
		software.setAttribute("version", "");
		processor.addContent(software);
		processor.addContent(new Element("operator").addContent("Hong Cui"));
		meta.addContent(processedby);
		treatment.addContent(meta);
		//taxon_identification
		//treatment.addContent(new Element("taxon_identification"));
		//description
		//treatment.addContent(new Element("description"));
		return doc;
	}
	
	public static String deAccent(String str) {
	    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	    return pattern.matcher(nfdNormalizedString).replaceAll("");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*String inputpath = "C:/Users/updates/CharaParserTest/Gray's/Gray_1950_reformatted.txt";
		String source = "Gray 1950";
		String outfoldert = "C:/Users/updates/CharaParserTest/Gray's/taxonomy";
		String outfolderk = "C:/Users/updates/CharaParserTest/Gray's/key";
		String nondescheadings ="";*/
		
		String inputpath = "C:/Users/updates/CharaParserTest/proibioPilot/Fungi/descripciones primer volumen Agaricus inglés-formatted.txt";
		String source = "descripciones primer volumen Agaricus inglés";
		String outfoldert = "C:/Users/updates/CharaParserTest/proibioPilot/Fungi/taxonomy";
		String outfolderk = "C:/Users/updates/CharaParserTest/proibioPilot/Fungi/key";
		String nondescheadings ="Chemical reactions:|Habit, habitat and distribution:"; 
		String author = "unknown";
		String date = "unknown";
		

		/*String inputpath = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/Gleason_and_Cronquist_Rosaceae_1991-reformated.txt";
		String source = "Gleason and Cronquist 1991";
		String outfoldert = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/taxonomy";
		String outfolderk = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/key";
		String nondescheadings =""; 
		 */
		/*String inputpath = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/Gleason_and_Cronquist_Rosaceae_1991-reformated.txt";
		String source = "Gleason and Cronquist 1991";
		String outfoldert = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/taxonomy";
		String outfolderk = "C:/Users/updates/Dropbox/for Illyong/to be processed in ETC/plants(retyped)/Gleason_and_Cronquist/key";
		String nondescheadings =""; 
		 */
		/*String inputpath="C:/Users/updates/CharaParserTest/hogdon_steele1966/hogdon_steele1966_reformatted.txt";
		String source="Hogdon Steele 1996";
		String outfoldert="C:/Users/updates/CharaParserTest/hogdon_steele1966/taxonomy";
		String outfolderk="C:/Users/updates/CharaParserTest/hogdon_steele1966/key";
		String nondescheadings =""; 
		 */
		Text2XML t2x = new Text2XML(inputpath, source, author, date, outfoldert, outfolderk, nondescheadings);
	}

}

