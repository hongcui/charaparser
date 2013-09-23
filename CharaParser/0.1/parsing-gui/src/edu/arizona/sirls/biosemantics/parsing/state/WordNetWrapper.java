package edu.arizona.sirls.biosemantics.parsing.state;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;

public class WordNetWrapper {
	private static final Logger LOGGER = Logger.getLogger(WordNetWrapper.class);
	Hashtable<String, Integer> pos = new Hashtable<String, Integer>();
	public boolean formchange = false;
	
	public WordNetWrapper(String word){
		String[] command = new String[]{"wn",  word, "-over"};
	//	String output = "";
		String s = null;
		String ptnpos = "Overview of (\\w+) (\\w+)";
		Pattern ppos = Pattern.compile(ptnpos);
		String ptncount = "^\\d+\\. \\((\\d+)\\) .*";
		Pattern pcount = Pattern.compile(ptncount);
 
		try{
			Process r = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new 
                InputStreamReader(r.getInputStream()));

        /*BufferedReader stdError = new BufferedReader(new 
                InputStreamReader(p.getErrorStream()));*/
			String tag = "";
			int count = 0;
			while ((s= stdInput.readLine()) != null) {
				//output += s;
				Matcher mpos = ppos.matcher(s);
				Matcher mcount = pcount.matcher(s);
				if(mpos.find()){
					if(tag.compareTo("") != 0){
						pos.put(tag, new Integer(count));
						count = 0;
					}
					tag = mpos.group(1);
					if(mpos.group(2).compareToIgnoreCase(word) != 0){
						this.formchange = true;
					}
				}
				if(mcount.find()){
					count += Integer.parseInt(mcount.group(1));
				}
			}
			pos.put(tag, new Integer(count));
			/*if(output.length() > 0){
				Pattern p = Pattern.compile(ptn);
				Matcher m = p.matcher(output);
				while(m.find()){
					pos.add(m.group(1));
				}
			}*/

			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
	}
	
	public boolean formchange(){
		return this.formchange;
	}
	public boolean isN(){
		return pos.containsKey("noun");
	}
	
	public boolean isAdj(){
		return pos.containsKey("adj");
	}
	public boolean isAdv(){
		return pos.containsKey("adv");
	}
	public boolean isV(){
		return pos.containsKey("verb");
	}
	public String mostlikelyPOS(){
		int max = 0;
		String tag = null;
		Enumeration<String> en = pos.keys();
		while(en.hasMoreElements()){
			String k = en.nextElement();
			Integer count = pos.get(k);
			int c = count.intValue();
			if(c>=max){
				max = c;
				tag = k;
			}
		}
		return tag;
	}
	
	public static void main(String[] args) {
		//WordNetWrapper wnw = new WordNetWrapper("1-(2+)-pinnately"); 
		WordNetWrapper wnw = new WordNetWrapper("edged"); 
		System.out.println("adj? "+ wnw.isAdj());
		System.out.println("adv? "+ wnw.isAdv());
		System.out.println("n? "+ wnw.isN());
		System.out.println("v? "+wnw.isV());
		System.out.println("most likely =" + wnw.mostlikelyPOS());
		System.out.println("form changed "+ wnw.formchange());
	}

}