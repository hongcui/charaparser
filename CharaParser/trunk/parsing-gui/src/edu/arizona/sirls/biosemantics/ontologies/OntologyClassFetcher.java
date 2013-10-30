/**
 * 
 */
package edu.arizona.sirls.biosemantics.ontologies;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import edu.arizona.sirls.biosemantics.charactermarkup.Utilities;
import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.ParsingException;
import edu.arizona.sirls.biosemantics.parsing.state.SentenceOrganStateMarker;

/**
 * @author hong cui
 * This class fetches selected classes from an OWL ontology and put them in a relational database table
 *
 */
public abstract class OntologyClassFetcher {
	protected static final Logger LOGGER = Logger.getLogger(SentenceOrganStateMarker.class);
	protected Set<OWLClass> allclasses=new HashSet<OWLClass>();
	protected Set<OWLOntology> onts=new HashSet<OWLOntology>();
	protected OWLOntology ontology;
	protected OWLDataFactory df;
	protected Connection conn;
	protected String table;
	//the following three arraylists are synchronised on their indexes: 1 class will have id, label, and categories
	protected ArrayList<String> selectedClassLabels = new ArrayList<String>();
	protected ArrayList<String> selectedClassCategories = new ArrayList<String>();
	protected ArrayList<String> selectedClassIds = new ArrayList<String>();
	protected HashSet<String> terms = new HashSet<String>();
	protected Hashtable<String, String> newphrases = new Hashtable<String, String>(); //plural => singular
	/**
	 * table: must have at least three fields: ontoid, term, category, underscored. 
	 */
	public OntologyClassFetcher(String ontopath, Connection conn, String table) {
		this.table = table;
		this.conn = conn;
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		File ontofile = new File(ontopath);
		//IRI iri = IRI.create(ontoURL);

		try {
			//fetch all classes
			ontology = manager.loadOntologyFromOntologyDocument(ontofile);
			onts = ontology.getImportsClosure();
			for (OWLOntology ont:onts){
				allclasses.addAll((Collection<? extends OWLClass>) ont.getClassesInSignature(true));
			}	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * populate the arraylists selectedClassLabels and selectedClassCategories
	 */
	public abstract void selectClasses();
	
	public ArrayList<String> getSynonymLabels(OWLClass c) {
		ArrayList<String> labels = new ArrayList<String>();
		Set<OWLAnnotation> anns = c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym"))));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym"))));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym"))));
		
		Iterator<OWLAnnotation> it = anns.iterator();
		while (it.hasNext()) {
			//String label = this.getRefinedOutput(it.next().toString());
			String label = ((OWLLiteral)it.next().getValue()).getLiteral();
			labels.add(label);
		}
		return labels;
	}
	
	/**
	 * 
	 * @param condition to determine whether a term be saved in the arraylist or now
	 */
	public void saveSelectedClass(String condition){
		PreparedStatement stmt = null;
		try{
			//table: must have at least three fields: ontoid, term, category, 
			stmt = conn.prepareStatement("insert into "+table+"(ontoid, term, category, underscored) values (?, ?, ?, ?)");
			for(int i = 0; i < this.selectedClassIds.size(); i++){
				String label = this.selectedClassLabels.get(i).trim();
				//remove () from label if () is not in the middle of the term. If it is, ignore the term. 
				if(!label.matches(".*?\\).+")){
					if(label.indexOf("(")>0){
						label = label.substring(0, label.indexOf("(")).trim();
					}
					label = cleanup(label);
					if(label.length()>0){
						stmt.setString(1, this.selectedClassIds.get(i));
						stmt.setString(2, label);
						stmt.setString(3, this.selectedClassCategories.get(i));
						stmt.setString(4, label.replaceAll("\\s+", "_")); //underscored: anal fin => anal_fin
						stmt.execute();
						if(this.selectedClassIds.get(i).matches(".*?("+condition+").*"))
							terms.add(label);
						}
				}
			}
			
			//after original labels are inserted, add plurals (getting plural needs singular info)
			for(int i = 0; i < this.selectedClassIds.size(); i++){
				String label = this.selectedClassLabels.get(i).trim();
				//remove () from label if () is not in the middle of the term. If it is, ignore the term. 
				if(!label.matches(".*?\\).+")){
					if(label.indexOf("(")>0){
						label = label.substring(0, label.indexOf("(")).trim();
					}
					if(label.length()>0){
						String pl = getPl(label);
						if(pl!=null){
							pl = cleanup(pl);
							if(pl.length()>0){
							stmt.setString(1, this.selectedClassIds.get(i)+"_pl");
							stmt.setString(2, pl);
							stmt.setString(3, this.selectedClassCategories.get(i));
							stmt.setString(4, pl.replaceAll("\\s+", "_")); //underscored: anal fin => anal_fin
							stmt.execute();
							if(this.selectedClassIds.get(i).matches(".*?("+condition+").*"))
								terms.add(pl);
							}
						}
					}
				}
			}
				
			//serialize the plural-singular mapping
			/*File file = new File(ApplicationUtilities.getProperty("ontophrases.p2s.bin"));
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(file));
			out.writeObject(newphrases);
			out.close();*/
		}catch (Exception e){
			e.printStackTrace();
		}finally{
			try{
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
	}
	
	/**
	 * endochondral element => endochondral elem
	 * @param phrase: one-word or multiple-word phrase, typically in singular form
	 * @return pl form
	 */
	private String getPl(String phrase) {
		String pl = null;
		String modifier = "";
		if(phrase.indexOf(" ")>0){
			modifier = phrase.substring(0, phrase.lastIndexOf(" ")).trim();
			phrase = phrase.substring(phrase.lastIndexOf(" ")).trim();
		}
		//phrase now = last noun in the original phrase
		String pnoun = phrase;
		if(!phrase.matches("\\d+") && !Utilities.isPlural(phrase, this.conn)) pnoun = Utilities.plural(phrase);
		if(pnoun!=null && pnoun.compareTo(phrase)!=0){
			pl = (modifier+" "+pnoun).trim();
			this.newphrases.put(modifier+" "+pnoun, phrase); //plural=>singluar
			return pl;
		}
		return pl;
	}
	

	
	public void serializeTermArrayList(String filepath){
		try {
			File file = new File(filepath);
			ObjectOutput out = new ObjectOutputStream(
					new FileOutputStream(file));
			out.writeObject(this.terms);
			out.close();
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	public void serializePSArrayList(String filepath){
		//serialize the plural-singular mapping
		try {
		File file = new File(filepath);
		ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream(file));
		out.writeObject(newphrases);
		out.close();
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	/**
	 * remove the following: [;\]\[,+'/\.&%@<>=`:]
	 * @param str
	 * @return "" or the str without those symbols.
	 */
	public String cleanup(String str){
		while(str.matches(".*?\\[.*?\\].*")){
			str = str.replaceAll("\\[.*?\\]", "").trim();
		}
		while(str.matches(".*?<.*?>.*")){
			str = str.replaceAll("<.*?>", "").trim();
		}
		str = str.replaceAll("`", "'");
		if(str.contains("%")) str = "";
		str = str.replaceFirst("[@=+;,.&:<>\\]\\[].*", ""); //@fr, x=y, x+y+z
		if(str.matches(".*?[;\\]\\[,+\\.&%@<>=`:].*")){
			System.out.print(str+ " ");
		}
		return str;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
