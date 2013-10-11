/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing.diagnosis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import edu.arizona.sirls.biosemantics.parsing.VolumeFinalizer;


/**
 * @author Hong Cui
 *
 */
public class TaxonXDiagnosis implements IDiagnosis {
	private static final Logger LOGGER = Logger.getLogger(TaxonXDiagnosis.class);
	List<Document> documents = new ArrayList<Document>();
	static XPath diagtype;
	static XPath nameptn;
	static Pattern period = Pattern.compile("\\.\\s*[A-Z]");
	final static String nametoken = "NAME";
	
	static{
		try {
			diagtype = XPath.newInstance(".//tax:div[@type='description_diagnosis']");
			nameptn = XPath.newInstance(".//tax:p/tax:name");
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * @throws IOException 
	 * @throws JDOMException 
	 * 
	 */
	public TaxonXDiagnosis(File sourcefolder) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		if(sourcefolder.isDirectory()){
			File[] fs = sourcefolder.listFiles();
			for(File f: fs){
				Document doc = builder.build(f);
				documents.add(doc);
			}
		}else{
			documents.add(builder.build(sourcefolder));
		}
	}
	
	public TaxonXDiagnosis(File[] sourcefolders) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
			for(File f: sourcefolders){
				Document doc = builder.build(f);
				documents.add(doc);
			}
	}

	/* (non-Javadoc)
	 * @see edu.arizona.sirls.biosemantics.parsing.diagnosis.IDiagnosis#isADiagnosis()
	 */
	@Override
	public boolean isADiagnosis(Object o) {
		try{
			if(diagtype.selectSingleNode((Element)o) !=null) return true;
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return false;
	}
	
	/**
	 * return the sentence containing the name element:
	 * 
	 * 
	 * Thorax much like that of
          <tax:name>
            <tax:xid identifier="urn:lsid:biosci.ohio-state.edu:osuc_concepts:34484" source="HNS" />
            <tax:xmldata>
              <dc:Genus>Polyrhachis</dc:Genus>
              <dc:Species>concava</dc:Species>
            </tax:xmldata>
            P. concava Ern. Andre
          </tax:name>
          , long and narrow, the dorsal surface concave with strong, upturned lateral carinae, notched at the pronounced, transverse premesonotal and mesoepinotal sutures. 
	 *
	 * returns: Thorax much like that of NAME, long and narrow, the dorsal surface concave with strong, upturned lateral carinae, notched at the pronounced, transverse premesonotal and mesoepinotal sutures. 
	 * 
	 * 
	 * @param parent
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getContainingSentence(Element name){
		Element parent = name.getParentElement();
		List<Content> content = parent.getContent();
		//get content before name
		int index = content.indexOf(name);
		ArrayList<Content> justbefore = new ArrayList<Content>();
		while(index > 0){
			Content before = content.get(index-1);
			justbefore.add(before);
			if(before instanceof Text){
				break;
			}
			index--;
		}
		if(justbefore.size()==0) justbefore.add(content.get(0));
		
	
		
		//get content after name
		ArrayList<Content> justafter = new ArrayList<Content>();
		while(index < content.size()-1){
			Content after = content.get(index+1);
			justafter.add(after);
			if(after instanceof Text){
				break;
			}
			index++;
		}
		if(justafter.size()==0) justafter.add(content.get(content.size()-1));

		String before = formString(justbefore);
		int semicolon = before.lastIndexOf(";");
		int period = -1;
		Matcher m = this.period.matcher(before);
		while(m.find()){ //last period
			period = m.start();
		}
		before = period > semicolon? before.substring(period+1) : before.substring(semicolon+1);
		
		String after = formString(justafter);
		semicolon = after.indexOf(";");
		period = 100000;
		m = this.period.matcher(after);
		if(m.find()){ //first period
			period = m.start();
		}
		after = period < semicolon? after.substring(0, period+1) : after.substring(0, semicolon+1);
		
		return before.trim() + " "+nametoken+" "+after.trim();
	}

	private String formString(ArrayList<Content> content) {
		StringBuffer text = new StringBuffer();
		for(Content b: content){
			if(b instanceof Text){
				text.append(((Text) b).getTextNormalize());
			}
			else if(b instanceof Element){
				if(((Element) b).getName().compareTo("tax:name")==0)
					text.append(" "+nametoken+" ");
				else text.append(((Element) b).getTextNormalize()); 
			}
		}
		return text.toString().trim();
	}

	/* (non-Javadoc)
	 * @see edu.arizona.sirls.biosemantics.parsing.diagnosis.IDiagnosis#parseDiagnosis()
	 */
	
	@Override
	public ArrayList<Pattern> parseDiagnosis() throws JDOMException {
		//collecting text
		ArrayList<String> texts = new ArrayList<String> ();
		for(Document doc: documents){
			List<Element> diagnoses = diagtype.selectNodes(doc);
			for(Element diag: diagnoses){
				List<Element> names = nameptn.selectNodes(diag);
				for(Element name: names){
					String t = getContainingSentence(name);
					texts.add(t);
					System.out.println(t);
				}
			}
		}
		//finding patterns
		ComparisonPatternFinder cpf = new ComparisonPatternFinder(texts);

		return cpf.extractPattern();
	}

	public static void main(String[] argv){
		//File f = new File("C:/Users/updates/CharaParserTest/proibioPilot/20597/target/final/3_97.xml");
		File f = new File("C:/Users/updates/CharaParserTest/proibioPilot/20597/target/transformed");
		try{
			TaxonXDiagnosis txd = new TaxonXDiagnosis(f);
			ArrayList<Pattern> ptns = txd.parseDiagnosis();
			for(Pattern ptn : ptns){
				System.out.println(ptn.toString());
			}
			
		}catch(Exception e){
			LOGGER.error("", e);
			e.printStackTrace();
		}
	}
}
