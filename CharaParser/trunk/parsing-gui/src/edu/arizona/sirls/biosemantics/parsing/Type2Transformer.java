/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.sql.DriverManager;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * For extracted XML formated descriptions from Treatise 
 */
@SuppressWarnings({ "unused" })
public class Type2Transformer extends Thread {
	
	private PhraseMarker pm; 
	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	private File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);

	//File target = new File("Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-ants-trash");
	//private String tableprefix = "plazi_ants";

	//target folder
	File target = new File(Registry.TargetDirectory);
	//private String tableprefix = "plazi_ants";
	

	private XMLOutputter outputter = null;
	// this is the dataprfix from general tab
	private String dataprefix = null;
	private ProcessListener listener;
	protected static final Logger LOGGER = Logger.getLogger(Type3Transformer.class);
	/**
	 * @param listener
	 * @param dataprefix
	 */
	public Type2Transformer(ProcessListener listener, String dataprefix) {
		if(ApplicationUtilities.getProperty("ontophrases.bin")!=null && 
				ApplicationUtilities.getProperty("ontophrases.bin").length()>0) this.pm = new PhraseMarker();
		this.listener = listener;
		this.dataprefix = dataprefix;
		File target = new File(Registry.TargetDirectory);
		Utilities.resetFolder(target, "descriptions");
		Utilities.resetFolder(target, "transformed");
		Utilities.resetFolder(target, "descriptions-dehyphened");
		Utilities.resetFolder(target, "markedup");
		Utilities.resetFolder(target, "final");
		Utilities.resetFolder(target, "co-occurrence");
	}

	public void run(){
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	/**
	 * just take the content of <description>s out and save them in the target folder 
	 */
	@SuppressWarnings({ "unchecked", "resource" })
	public void transform(){
		try{
			/*Runtime r = Runtime.getRuntime();
			String src = "\""+source.getAbsolutePath()+"\"";
			String tgt = "\""+target.getAbsolutePath()+"\\transformed\"";
			String cmd = "copy "+src+" "+tgt;
			Process p = r.exec(cmd);
			int exitVal = p.waitFor();
            if(exitVal>0){
            	throw new Exception("transformed not created");
            }*/
			File[] files =  source.listFiles();
			SAXBuilder builder = new SAXBuilder();
			listener.progress(1);
			int total = files.length;
			
			for(int i = 0; i<total; i++){
				File f = files[i];		
				String tgt = target.getAbsolutePath()+"/transformed";
				File newFile = new File(tgt, f.getName());
				if(!newFile.exists())
					newFile.createNewFile();
				FileChannel inputChannel = new FileInputStream(f).getChannel();
				FileChannel outputChannel = new FileOutputStream(newFile).getChannel();
				
				inputChannel.transferTo(0,inputChannel.size(),outputChannel);
				
				Document doc = builder.build(f);
				Element root = doc.getRootElement();
				
				/* assuming single description
				Element descrp = (Element)XPath.selectSingleNode(root, "/treatment/description");
				if(descrp != null) {
					String text = descrp.getTextNormalize();
					writeDescription2Descriptions(text,f.getName().replaceAll("xml$", "txt") ); //record the position for each paragraph. 10.txt-0
				}*/
				
				//allow multiple description and key elements
				int count = 0;
				List<Element> descrps = XPath.selectNodes(root, "/treatment/description");
				if(descrps != null) {
					for(Element descrp: descrps){
						//index descrp element, pids can be removed at the finalization step if needed
						String pid = f.getName().replaceAll("xml$", "txt")+"p"+count;
						Attribute index = new Attribute(ApplicationUtilities.getProperty("transformer.index"), pid);
						descrp.setAttribute(index);
						String text = descrp.getTextNormalize().replaceAll("-\\s+(?=[a-zA-Z])", "-"); //auto- matically =>auto-matically
						if(pm!=null) text = pm.markPhrases(text); //phrases are connected via "_" and become words.
						//file names for description text must not contain "-".
						//record the position for each paragraph. 10.txtp0.txt-0
						writeDescription2Descriptions(text,pid+".txt" ); 
						count++;
					}
				}
				
				descrps = XPath.selectNodes(root, "//key//statement");
				if(descrps != null) {
					for(Element descrp: descrps){
						//index descrp element, pids can be removed at the finalization step if needed
						String pid = f.getName().replaceAll("xml$", "txt")+"p"+count;
						Attribute index = new Attribute(ApplicationUtilities.getProperty("transformer.index"), pid);
						descrp.setAttribute(index);
						descrp.setName("description");
						String text = descrp.getTextNormalize().replaceAll("-\\s+(?=[a-zA-Z])", "-"); //auto- matically =>auto-matically;
						if(pm!=null) text = pm.markPhrases(text); //phrases are connected via "_" and become words.
						//file names for description text must not contain "-".
						//record the position for each paragraph. 10.txtp0.txt-0
						writeDescription2Descriptions(text,pid+".txt" ); 
						count++;
					}
				}
				
				root.detach();
				ParsingUtil.outputXML(root, new File(tgt, f.getName()), new Comment("added pid attribute in description elements"));
				listener.progress((i+1)*100/total);
				listener.info((i)+"", f.getName()); 
			}
			if(MainForm.conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				MainForm.conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			/*String transformeddir = Registry.TargetDirectory+"transformed/";
			TaxonNameCollector tnc = new TaxonNameCollector(MainForm.conn, transformeddir, this.dataprefix+"_"+ApplicationUtilities.getProperty("TAXONNAMES"), this.dataprefix);
			tnc.collect();*/
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	private void writeDescription2Descriptions(String textNormalize, String fn) {
		try {
			File file = new File(target+"/descriptions", fn);
			
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(textNormalize);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to output text file.", e);
		}		
	}
	/**
	 * 
	 */
	public Type2Transformer() {
		// TODO Auto-generated constructor stub
	}

}
