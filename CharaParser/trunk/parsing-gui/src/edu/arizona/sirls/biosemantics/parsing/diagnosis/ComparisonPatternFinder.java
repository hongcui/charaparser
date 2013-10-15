/**
 * 
 */
package edu.arizona.sirls.biosemantics.parsing.diagnosis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.MainForm;



/**
 * @author Hong Cui
 */
public class ComparisonPatternFinder {
	private static final Logger LOGGER = Logger.getLogger(TaxonXDiagnosis.class);
	Connection conn;
	/**
	 * 
	 */
	public ComparisonPatternFinder(ArrayList<String> text) {
		if(MainForm.conn!=null){
			this.conn = MainForm.conn;
		}else{
			try {
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				String url = ApplicationUtilities.getProperty("database.url");
				this.conn = DriverManager.getConnection(url);
			} catch (Exception e) {
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator"), e);
			} 
		}
	}
	
	/*
	 * to do: find patterns from comparison sentences such as
	 * Differing from the maxima of the typical NAME 
	 */
	
	public ArrayList<Pattern> extractPattern(){
		ArrayList<Pattern> ptns = new ArrayList<Pattern>();
		ptns.add(Pattern.compile("as in"));
		ptns.add(Pattern.compile("taxonname"));

		try{
			Statement stmt = conn.createStatement();
			String table = ApplicationUtilities.getProperty("COMPARISON");
			stmt.execute("create table if not exists "+table+" (pattern varchar(200) not null primary key, count int(1) default 0)");
			for(Pattern ptn: ptns){
				String ptnst = ptn.toString().trim();
				if(ptnst.length()>0)
				stmt.execute("insert into "+table+
						" (pattern, count) values ('"+ ptnst+"', 0) "
								+ " on duplicate key update count=count+1");
			}
		}catch(Exception e){
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator"), e);
		}
		return ptns;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
