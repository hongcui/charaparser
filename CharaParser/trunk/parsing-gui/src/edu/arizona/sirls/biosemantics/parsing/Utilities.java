package edu.arizona.sirls.biosemantics.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class Utilities {
	private static final Logger LOGGER = Logger.getLogger(Utilities.class);

	public static void resetFolder(File folder, String subfolder) {
		File d = new File(folder, subfolder);
		if(!d.exists()){
			d.mkdir();
		}else{ //empty folder
			Utilities.emptyFolder(d);
		}
	}
	
	public static void emptyFolder(File f){
			File[] fs = f.listFiles();
			for(int i =0; i<fs.length; i++){
				fs[i].delete();
			}
	}
	
	public static void copyFile(String f, File fromfolder, File tofolder){
		try{
			  File f1 = new File(fromfolder, f);
			  File f2 = new File(tofolder, f);
			  InputStream in = new FileInputStream(f1);
			  OutputStream out = new FileOutputStream(f2);

			  byte[] buf = new byte[1024];
			  int len;
			  while ((len = in.read(buf)) > 0){
				  out.write(buf, 0, len);
			  }
			  in.close();
			  out.close();
			  System.out.println("File copied.");
		}
		catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}
	
    private static boolean hasUnmatchedBrackets(String text) {
    	String[] lbrackets = new String[]{"\\[", "(", "{"};
    	String[] rbrackets = new String[]{"\\]", ")", "}"};
    	for(int i = 0; i<lbrackets.length; i++){
    		int left1 = text.replaceAll("[^"+lbrackets[i]+"]", "").length();
    		int right1 = text.replaceAll("[^"+rbrackets[i]+"]", "").length();
    		if(left1!=right1) return true;
    	}
		return false;
	}
    
    /**
	 * trace part_of relations of structid to get all its parent structures,
	 * separated by , in order
	 * 
	 * TODO limit to 3 commas
	 * TODO treat "in|on" as part_of? probably not
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @count number of rounds in the iteration
	 * @return organ, organ, organ (from specific to more general parent organ)
	 */
	@SuppressWarnings("unchecked")
	public static String getStructureChain(Element root, String xpath, int count) {
		String path = "";
		try{
			List<Element> relations = XPath.selectNodes(root, xpath);			
			xpath = "";
			for (Element r : relations) {
				String pid = r.getAttributeValue("to");
				path += Utilities.getStructureName(root, pid) + ",";
				String[] pids = pid.split("\\s+");
				for (String id : pids) {
					if (id.length() > 0)
						xpath += "//relation[@name='part_of'][@from='" + id + "']|//relation[@name='in'][@from='" + id + "']|//relation[@name='on'][@from='" + id + "']|";
				}
			}
			if (xpath.length() > 0 && count < 3) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChain(root, xpath, count++);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		return path.replaceFirst(",$", "");
	}

	/**
	 * trace part_of relations of structid to get all its parent structure ids,
	 * separated by , in order
	 * 
	 * TODO limit to 3 commas
	 * TODO treat "in|on" as part_of? probably not
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @return
	 */
	/*@SuppressWarnings("unchecked")
	public static String getStructureChainIds(Element root, String xpath, int count) {
		String path = "";
		try{
			List<Element> relations = XPath.selectNodes(root, xpath);			
			xpath = "";
			for (Element r : relations) {
				String pid = r.getAttributeValue("to");
				path += pid + ",";
				String[] pids = pid.split("\\s+");
				for (String id : pids) {
					if (id.length() > 0)
						xpath += "//relation[@name='part_of'][@from='" + id + "']|//relation[@name='in'][@from='" + id + "']|//relation[@name='on'][@from='" + id + "']|";
				}
			}
			if (xpath.length() > 0 && count < Utilities.relationlength ) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChainIds(root, xpath, count++);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		return path.replaceFirst(",$", "").trim();
	}*/

	/**
	 * Get structure names for 1 or more structids from the XML results of CharaParser.
	 * 
	 * @param root
	 * @param structids
	 *            : 1 or more structids
	 * @return
	 */
	public static String getStructureName(Element root, String structids) {
		String result = "";

		String[] ids = structids.split("\\s+");
		for (String structid : ids) {
			try{
				Element structure = (Element) XPath.selectSingleNode(root, "//structure[@id='" + structid + "']");
				String sname = "";
				if (structure == null) {
					System.out.println((new XMLOutputter(Format.getPrettyFormat())).outputString(root));
					sname = "ERROR"; // this should never happen
				} else {
					sname = ((structure.getAttribute("constraint") == null ? "" : structure.getAttributeValue("constraint")) + " " + structure.getAttributeValue("name").replaceAll("\\s+", "_"));
				}
				result += sname + ",";
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(sw.toString());
			}
		}
		result = result.replaceAll("\\s+", " ").replaceFirst(",$", "").trim();
		return result;
	}

	
	public static void main(String[] args) {
    	String text = "Trees, aromatic and resinous, glabrous or with simple hairs. Bark gray-brown, deeply furrowed; liquid, and Arabic ambar, amber] twigs and branches sometimes corky-winged. Dormant buds scaly, pointed, shiny, resinous, sessile. Leaves long-petiolate. Leaf blade fragrant when crushed, (3-)5(-7)-lobed, palmately veined, base deeply cordate to truncate, margins glandular-serrate, apex of each lobe long-acuminate. Inflorescences terminal, many-flowered heads; staminate heads in pedunculate racemes, each head a cluster of many stamens; pistillate heads pendent, long-pedunculate, the flowers ± coalesced. Flowers unisexual, staminate and pistillate on same plant, appearing with leaves; calyx and corolla absent. Staminate flowers: anthers dehiscing longitudinally; staminodes absent. Pistillate flowers pale green to greenish yellow; staminodes 5-8; styles indurate and spiny in fruit, incurved. Capsules many, fused at base into long-pedunculate, spheric, echinate heads, 2-beaked, glabrous, septicidal. Seeds numerous, mostly aborting, 1-2 viable in each capsule, winged. x = 16.";
    	if(hasUnmatchedBrackets(text)) System.out.println("unmatched");
    }
}
