/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * This is based on the dry.pdf.xml sample file sent by Eva and Josefe for pro-ibiosphere pilot
 * No schema is associated with the xml input, so here we made several assumptions
 * 1. each file contains one treatment
 * 2. there are sections marked as type="description" and it may contain 0-n paragraphs
 * 
 * the result of the transformation is to form a <description id=""> element containing all text within a paragraph 
 * the description id takes the form of Type4 paragraph id 
 */
public class Type4Transformer4GoldenGATEnoSchema extends Type4Transformer {

	/**
	 * @param listener
	 * @param dataprefix
	 */
	public Type4Transformer4GoldenGATEnoSchema(ProcessListener listener,
			String dataprefix) {
		super(listener, dataprefix);
	}

	protected void transformXML(File[] files) {
		int number = 0;
		try{
			SAXBuilder builder = new SAXBuilder();
			for(int f = 0; f < files.length; f++) {
				Document doc = builder.build(files[f]);
				Element root = doc.getRootElement();
				root = formatDescription(root,
						".//*[@type='description']", "./paragraph", files[f].getName(), 0);
				root.detach();
				writeTreatment2Transformed(root, files[f].getName(), 0);
				listener.info((number++)+"", files[f].getName()+"_"+0+".xml"); // list the file on GUI here
		        getDescriptionFrom(".//*[@type='description']", root,files[f].getName(), 0);
					
				String transformeddir = Registry.TargetDirectory+"\\transformed\\";
				try{
					if(MainForm.conn == null){
						Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
						MainForm.conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
				//TaxonNameCollector4GoldenGATEnoSchema tnc = new TaxonNameCollector4GoldenGATEnoSchema(MainForm.conn, transformeddir, this.dataprefix+"_"+ApplicationUtilities.getProperty("TAXONNAMES"), this.dataprefix);
				//tnc.collect();
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}

	/**
	 * 
	 */


	/* do two things: 
	 * take out description element and save them in a separate folder.
	 * make sure the file names are mapped to numbers
	 * 
	 */
	/*@Override
	protected void transformXML(File[] files) {
		int number = 0;
		try{
			SAXBuilder builder = new SAXBuilder();
			for(int f = 0; f < files.length; f++) {
				int fn = f+1;
				Document doc = builder.build(files[f]);
				Element root = doc.getRootElement();
				formatDescription(root,"/treatment/description", null, fn, 0);
				root.detach();
				writeTreatment2Transformed(root, fn, 0);
				listener.info((number++)+"", fn+"_0.xml"); // list the file on GUI here
		        getDescriptionFrom(root,fn, 0);						
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}

	}*/
	/*protected void getDescriptionFrom(Element root, int fn,  int count) {

		try{
			List<Element> descriptions = XPath.selectNodes(root, "/treatment/description");
			Iterator<Element> it = descriptions.iterator();
			int i = 0;
			while(it.hasNext()){
				Element description = it.next();
				writeDescription2Descriptions(description.getTextNormalize(), fn+"_"+count+".txtp"+i); //record the position for each paragraph.
				i++;							
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
