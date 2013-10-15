/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author updates
 *
 */
public class TaxonNameCollector4GoldenGATEnoSchema extends
		TaxonNameCollector4TaxonX {

	/**
	 * @param conn
	 * @param transformeddir
	 * @param outputtablename
	 * @param volume
	 * @throws Exception
	 */
	public TaxonNameCollector4GoldenGATEnoSchema(Connection conn,
			String transformeddir, String outputtablename, String volume)
			throws Exception {
		super(conn, transformeddir, outputtablename, volume);
	}
	/**
	 * populate this.names
	 * @param file
	 */
	protected void collectNames(File xmlfile){
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xmlfile);
			Element root = doc.getRootElement();
			List<Element> names = XPath.selectNodes(root, "//taxonomicName");
			for(Element name: names){
				if(name.getAttribute("_evidence_details")!=null){
					String text = name.getAttributeValue("_evidence_details"); //_evidence_details="genus=Lactarius, species=dryadophilus"
					String[] ns = text.split("\\s+,\\s+");
					for(String n: ns){
						n = n.replaceAll(".*?=", "").trim();
						this.names.add(n);
					}
				}
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
