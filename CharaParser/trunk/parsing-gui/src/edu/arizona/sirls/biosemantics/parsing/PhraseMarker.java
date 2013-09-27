package edu.arizona.sirls.biosemantics.parsing;

/**
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.ParsingException;
import edu.arizona.sirls.biosemantics.parsing.TaxonIndexer;
import edu.arizona.sirls.biosemantics.charactermarkup.Utilities;

/**
 * @author Hong Cui
 * convert a sentence to a format so the phrases matching terms in a knowledge source such as an ontology are marked.
 * A phrase is represented by words connected using "_"
 *
 */
public class PhraseMarker {
	private Pattern phrasepattern; //"dorsal_fin|leaf_blade"
	//public static String allstructureterms;
	private String phrasestr;
	public ArrayList<String> phrases;
	//public Hashtable<String, String> newphrases = new Hashtable<String, String>(); //plural => singular
	
	//private static final String BIN_FILE = "PO_phrases.bin";
	/**
	 * @param termsource: filepath to the serialized arraylist, holding the phrases 
	 */
	@SuppressWarnings("unchecked")
	public PhraseMarker(/*String termsourcepath*/) {
		try {
			File file = new File(ApplicationUtilities.getProperty("ontophrases.bin"));
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					file));
			// Deserialize the object
		    phrases = (ArrayList<String>) in.readObject(); //phrases are words connected with " "
			in.close();			
			Collections.sort(phrases, new PhraseComparable()); //longest phrases first
			phrasestr = "";
			for(String phrase: phrases){
				//hyomandibula-opercle joint
				phrase = phrase.replaceAll("\\([^)]*?\\)", "").trim();
				//if(phrase.length()>0) allstructureterms += phrase+"|";
				if(phrase.length()>0 && phrase.indexOf(" ")>0){ //don't allow single-word phrase, one-word phrases are still in onto_structures table and are used in Perl learning process. 
					phrase = phrase.replaceAll("-", "_");//hyomandibula_opercle joint
					//phrase = phraseForms(phrase);//added plural forms, moved to OntologyClassFetcher
					phrasestr += phrase+"|";
				}
			}
			//phrasestr="";
			phrasestr = phrasestr.replaceFirst("\\|$", ""); //space separated words in phrases
			this.phrasepattern =Pattern.compile("(.*?\\b)("+phrasestr+")(\\b.*)", Pattern.CASE_INSENSITIVE);
			//add plurals
			//phrases.addAll(newphrases.keySet()); //add plurals
			//serialize merged phrases
			//ObjectOutput out = new ObjectOutputStream(
					//new FileOutputStream(new File(ApplicationUtilities.getProperty("ontophrases.update.bin")))); //avoid increase the size of the original
			//out.writeObject(phrases);
			//out.close();
			
		} catch (Exception e) {
			//LOGGER.error("Load the updated TaxonIndexer failed.", e);
			//StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			e.printStackTrace();
		}
	}

	

	/**
	 * @param sentence : leaf blade rounded
	 * @return leaf_blade rounded
	 */
	public String markPhrases(String sentence){
		Matcher m = phrasepattern.matcher(sentence);
		//System.out.println(this.phrasestr);
		while(m.matches()){
			//System.out.println(phrasepattern);
			sentence = m.group(1)+m.group(2).replaceAll("\\s+", "_")+m.group(3);
			m = phrasepattern.matcher(sentence);
		}				
		return sentence;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		long time1 = System.currentTimeMillis();
		String termpath = "C:/Users/updates/CharaParserTest/Ontologies/ext_terms.bin";
		PhraseMarker pm = new PhraseMarker(/*termpath*/);
		long time2 = System.currentTimeMillis();
		System.out.println("read term list took "+(time2-time1)+" ms");
		//System.out.println(pm.markPhrases("hyomandibula-opercle joint and anal fin absent . female gonad present."));
		System.out.println(pm.markPhrases("Medioventral endochondral elements of the shoulder girdle"));
		long time3 = System.currentTimeMillis();
		System.out.println("mark the sentence took "+(time3-time2)+" ms");
	}
}
