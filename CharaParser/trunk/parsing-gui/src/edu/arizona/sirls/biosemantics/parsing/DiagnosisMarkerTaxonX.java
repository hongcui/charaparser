package edu.arizona.sirls.biosemantics.parsing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.dom4j.io.OutputFormat;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.w3c.dom.Node;

public class DiagnosisMarkerTaxonX {

	static int count=0,desc_count=0;

	static boolean printStructure(Element child)
	{
		
		if(child.getName().equals("name"))
		{
			return true;
		} else if(child.getChildren().size()>0)
		{
			List<Element> children = child.getChildren();
			for(Element node:children)
			{
				if(printStructure(node))
				{
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws JDOMException, TransformerException, IOException {
		
		XPath pathDescription = XPath.newInstance(".//x:div[@type='description']");
		File unclean = new File("C:/Users/updates/CharaParserTest/proibioPilot/uncleaned/");
		for(File input:unclean.listFiles())
		{
		SAXBuilder builder = new SAXBuilder();
		Document xml;		
		xml = builder.build(input);
		Element root = xml.getRootElement();
		count=0;
		pathDescription.addNamespace("x", root.getNamespaceURI());
		
		List<Element> descriptions = (List<Element>)pathDescription.selectNodes(root);
		System.out.println("Input file name "+input.getName());
		System.out.println("Root node =="+root.getName());
		System.out.println("Total number of descriptions"+descriptions.size());
		
		for(Element des:descriptions)
		{
			List<Element> children = des.getChildren();
			
			for(Element child:children)
			{
				if(printStructure(child))
				{
					count++;
					des.setAttribute("type", "description_diagnosis");
					break;
				}
			}
		}
		
		System.out.println("Total description node changed "+count+" for "+input.getName());

		
		XMLOutputter output = new XMLOutputter();
		output.setFormat(Format.getRawFormat());
		output.output(xml.getDocument(), new FileWriter(new File("C:/Users/updates/CharaParserTest/proibioPilot/cleaned/"+input.getName().replaceAll("(\\.xml)", "_")+"modified.xml")));
		System.out.println(xml);
	}
	}


}
