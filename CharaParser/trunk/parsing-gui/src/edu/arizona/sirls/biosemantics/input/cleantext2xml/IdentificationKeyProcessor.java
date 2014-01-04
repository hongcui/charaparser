/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;

/**
 * @author updates
 *
 */
public class IdentificationKeyProcessor {

	/**
	 * 
	 */
	public IdentificationKeyProcessor() {

	}

	/**
	 * First assemble the key element(s) <key></key>
	 * Then turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
	 * to:
	 * <key_statement>
	 * <statement_id>2</statement_id>
	 * <statement>Carpels and stamens more than 5; 
	 * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
	 * or leaf-opposed racemes or spikes</statement>
	 * <determination>3. Phytolacca (in part), p. 6</determination>
	 * </key_statement>
	 * 
	 * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	public void process(Element treatment) {
		if(treatment.getChild("key")!=null){
			assembleKeys(treatment);
			try{
				List<Element> keys = XPath.selectNodes(treatment, "./TaxonKey"); //"TaxonKey" used temporarily
				for(Element key: keys){//each key element is a complete key
					furtherMarkupKeyStatements(key);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

	}

	/* Turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
	 * To:
	 * <key_statement>
	 * <statement_id>2</statement_id>
	 * <statement>Carpels and stamens more than 5; 
	 * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
	 * or leaf-opposed racemes or spikes</statement>
	 * <determination>3. Phytolacca (in part), p. 6</determination>
	 * </key_statement>
	 * 
	 * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	private void furtherMarkupKeyStatements(Element taxonkey) {
		ArrayList<Element> allstatements = new ArrayList<Element>();
		Element marked = new Element("key");
		List<Element> states = taxonkey.getChildren();
		Pattern p1 = Pattern.compile("(.*?)(@@@.*)");//determ
		Pattern p2 = Pattern.compile("^(.*?###)(.*)");//id   2. "Ray” corollas
		String determ = null;
		String id = "";
		String broken = "";
		String preid = null;
		//process statements backwards
		for(int i = states.size()-1; i>=0; i--){
			Element state = states.get(i);
			if(state.getName().compareTo("key") == 0 || state.getName().compareTo("couplet") == 0){
				String text = state.getTextTrim()+broken;
				Matcher m = p1.matcher(text);
				if(m.matches()){
					text = m.group(1).trim();
					determ = m.group(2).trim();
				}
				m = p2.matcher(text);
				if(m.matches()){//good, statement starts with an id
					id = m.group(1).trim();
					text = m.group(2).trim();
					broken = "";
					//form a statement
					Element statement = new Element("key_statement");
					Element stateid = new Element("statement_id");
					stateid.setText(id.replaceAll("\\s*###\\s*", ""));
					Element stmt = new Element("statement");
					stmt.setText(text.replaceAll("\\s*[#|@]+\\s*", "").replaceAll("-\\s+(?=[a-zA-Z])", "-"));
					Element dtm = null;
					Element nextid = null;
					if(determ!=null) {
						dtm = new Element("determination");
						dtm.setText(determ.replaceAll("\\s*@@@\\s*", ""));
						determ = null;
					}else if(preid!=null){
						nextid = new Element("next_statement_id");
						nextid.setText(preid.replaceAll("\\s*###\\s*", ""));
						//preid = null;
					}
					preid = id;
					statement.addContent(stateid);
					statement.addContent(stmt);
					if(dtm!=null) statement.addContent(dtm);
					if(nextid!=null) statement.addContent(nextid);
					allstatements.add(statement);
				}else if(text.matches("^[a-z]+.*")){//a broken statement, save it
					broken = text;
				}
			}else{
				Element stateclone = (Element)state.clone();
				if(stateclone.getName().compareTo("run_in_sidehead")==0){
					stateclone.setName("key_head");
				}
				allstatements.add(stateclone);//"discussion" remains
			}
		}

		for(int i = allstatements.size()-1; i >=0; i--){
			marked.addContent(allstatements.get(i));
		}		
		taxonkey.getParentElement().addContent(marked);
		taxonkey.detach();
	}


	/**
	 * <treatment>
	 * <...>
	 * <references>...</references>
	 * <key>...</key>
	 * </treatment>
	 * deals with two cases:
	 * 1. the treatment contains one key with a set of "key/couplet" statements (no run_in_sidehead tags)
	 * 2. the treatment contains multiple keys that are started with <run_in_sidehead>Key to xxx (which may be also used to tag other content)
	 * @param treatment
	 */
	private void assembleKeys(Element treatment) {
		Element key = null;
		//removing individual statements from treatment and putting them in key
		List<Element> children = treatment.getChildren();////changes to treatment children affect elements too.
		Element[] elements = children.toArray(new Element[0]); //take a snapshot
		ArrayList<Element> detacheds = new ArrayList<Element>();
		boolean foundkey = false;
		for(int i = 0; i < elements.length; i++){
			Element e = elements[i];
			if(e.getName().compareTo("run_in_sidehead")==0 && ((e.getTextTrim().startsWith("Key to ")||(e.getTextTrim().startsWith("Key Based "))) || e.getTextTrim().matches("Group \\d+.*"))){
				foundkey = true;
				if(key!=null){
					treatment.addContent((Element)key.clone());	
				}
				key = new Element("TaxonKey");

			}
			if(!foundkey && (e.getName().compareTo("key")==0 || e.getName().compareTo("couplet")==0)){
				foundkey = true;	
				if(key==null){
					key = new Element("TaxonKey");
				}
			}
			if(foundkey){
				detacheds.add(e);
				//create key_heading if needed
				String text = e.getTextNormalize();
				if(!text.contains("###") && !text.contains("@@@") && text.replaceAll("[a-z1-9\\W]", "").trim().length() 
						> 0.5 * e.getTextNormalize().split("\\s+").length){ //more than half of the words are capitalized
					e.setName("key_heading");
				}
				key.addContent((Element)e.clone());//clone a statement and add it to 'key'
			}			
		}
		if(key!=null){
			treatment.addContent(key);					
		}
		for(Element e: detacheds){
			e.detach();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
