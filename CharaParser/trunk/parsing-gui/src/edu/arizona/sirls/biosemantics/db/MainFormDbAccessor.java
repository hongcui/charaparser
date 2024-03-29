/**
 * MainFormDbAccessor.java
 *
 * Description : This performs all the database access needed by the MainForm
 * Version     : 1.0
 * @author     : Partha Pratim Sanyal
 * Created on  : Aug 29, 2009
 *
 * Modification History :
 * Date   | Version  |  Author  | Comments
 *
 * Confidentiality Notice :
 * This software is the confidential and,
 * proprietary information of The University of Arizona.
 */
package edu.arizona.sirls.biosemantics.db;

import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.MainForm;
import edu.arizona.sirls.biosemantics.parsing.ParsingException;


@SuppressWarnings({ "unused"})
public class MainFormDbAccessor {


	private static final Logger LOGGER = Logger.getLogger(MainFormDbAccessor.class);
	private static Connection conn = null;
	private String prefix;
	 
	public static void main(String[] args) throws Exception {

		Connection conn = DriverManager.getConnection(url);
		System.out.println(conn);
		//conn.close(); 
	}
	
	private static String url = ApplicationUtilities.getProperty("database.url");
	
	public MainFormDbAccessor(){
		//Statement stmt = null;
		//Connection conn = null;

		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} 
	}
	
	/**
	 * NonEQ table holds the terms that are not structure nor character/character states.
	 * These terms could be the adverbs and verbs filtered out by CharaParser (these terms are without a savedid)
	 * or the terms categorized as "neither" by a user
	 * or the terms the user didn't give a decision in character categorization step. 
	 */
	public void createNonEQTable(){
		//noneqterms table is refreshed for each data collection
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
			stmt.execute("drop table if exists "+this.prefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE"));
			stmt.execute("create table if not exists "+this.prefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+" (term varchar(100) not null, source varchar(200), savedid varchar(40))");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
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
	
	public void createTyposTable(){
		Statement stmt = null;
        try{
            stmt = conn.createStatement();
    		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
            String typotable = this.prefix+"_"+ApplicationUtilities.getProperty("TYPOS");
            stmt.execute("drop table if exists "+typotable);
            String query = "create table if not exists "+typotable+" (typo varchar(150), correction varchar(150), primary key (typo, correction))";
            stmt.execute(query);	           
        }catch(Exception e){
        	LOGGER.error("Problem in VolumeDehyphenizer:createWordTable", e);
            StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
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
	 * This method is used to remove the Bad structure names from Tab4, after they are marked RED,two steps are taken:
	 * First Step: Remove from the database (update the tag). Step Two: Keep the UI as it is with selected rows in Red color 
	 * @param removedTags: List of structures that should be removed
	 * @throws ParsingException
	 * @throws SQLException
	 */
	public boolean setUnknownTags(List <String> removedTags) throws ParsingException, SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			
				//Class.forName(driverPath);
				//conn = DriverManager.getConnection(url);
				
				String sql = "update "+this.prefix+"_sentence set tag = 'unknown' where tag = ?";
				stmt = conn.prepareStatement(sql);
				
				for (String tag : removedTags) {
					stmt.setString(1, tag);
					stmt.executeUpdate();
				}
				return true;
		} catch (SQLException sqlexe) {
			//LOGGER.error("Couldn't update sentence table in MainFormDbAccessor:removeMarkUpData", sqlexe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);sqlexe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			//throw new ParsingException("Error Accessing the database" , sqlexe);
			return false;
		} /*catch (ClassNotFoundException clexe) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:removeMarkUpData", clexe);
			throw new ParsingException("Couldn't load the db Driver" , clexe);
			
		} */finally {
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}			
			
		}
	}
	

	public void updateContextData(int sentid, StyledText contextStyledText) throws SQLException, ParsingException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		String min = "" + (sentid - 2);
		String max = "" + (sentid + 2);
		
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			String sql = "select * from "+this.prefix+"_sentence where sentid > ? and sentid < ?";
			stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, min);
			stmt.setString(2, max);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				String sid = rs.getString("sentid");
				String tag = rs.getString("tag");
				//String sentence = rs.getString("sentence");
				String sentence = rs.getString("originalsent");
				int start = contextStyledText.getText().length();
				
				contextStyledText.append(sentence + "\r\n");
				if (Integer.parseInt(sid) == sentid) {
					StyleRange styleRange = new StyleRange();
					styleRange.start = start;
					styleRange.length = sentence.length();
					styleRange.fontStyle = SWT.BOLD;
					// styleRange.foreground = display.getSystemColor(SWT.COLOR_BLUE);
					contextStyledText.setStyleRange(styleRange);
				}
			}

		} catch (SQLException exe) {
			//LOGGER.error("Couldn't execute db query in MainFormDbAccessor:updateContextData", exe);
			//StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:updateContextData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	
	
	//added March 1st
	public void glossaryPrefixRetriever(List <String> datasetPrefixes) 
	throws ParsingException, SQLException{
		//Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			rset = stmt.executeQuery("SELECT table_name FROM information_schema.tables where table_schema ='"+ApplicationUtilities.getProperty("database.name")+"' and table_name like '%glossaryfixed'");
			while (rset.next()) {
				datasetPrefixes.add(rset.getString("table_name"));
			}
		}catch (SQLException exe) {
			//LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
						
		}
		
		
	}
	
	//added March 1st ends
	public void datasetPrefixRetriever(List <String> datasetPrefixes) 
		throws ParsingException, SQLException{
		
		//Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		
		String createprefixTable = "CREATE TABLE  if not exists datasetprefix (" +
				 "prefix varchar(20) NOT NULL DEFAULT '', "+
				  "time_last_accessed timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
				  "tab_general varchar(1) DEFAULT NULL, "+
				  "tab_segm varchar(1) DEFAULT NULL, "+
				  "tab_verf varchar(1) DEFAULT NULL, "+
				  "tab_trans varchar(1) DEFAULT NULL, "+
				  "tab_struct_perl varchar(1) DEFAULT NULL, "+
				  "tab_struct_one varchar(1) DEFAULT NULL, "+
				  "tab_struct_two varchar(1) DEFAULT NULL, "+
				  "tab_struct_three varchar(1) DEFAULT NULL, "+
				  "tab_unknown varchar(1) DEFAULT NULL, "+
				  "tab_finalm varchar(1) DEFAULT NULL, "+
				  "tab_gloss varchar(1) DEFAULT NULL, "+
				  "glossary varchar(40) DEFAULT NULL, "+
				  "option_chosen varchar(1) DEFAULT '', "+
				  "PRIMARY KEY (prefix, time_last_accessed) ) " ; 
		
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute(createprefixTable);
			rset = stmt.executeQuery("select * from datasetprefix order by time_last_accessed desc");
			while (rset.next()) {
				datasetPrefixes.add(rset.getString("prefix"));
			}
		}catch (SQLException exe) {
			//LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
						
		}
		
		
	}
	
	
	public void savePrefixData(String prefix, String glossaryName, int optionChosen) 
	throws ParsingException, SQLException{
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		
		try {
			if (!prefix.equals("")) {
				stmt = conn.prepareStatement("select prefix from datasetprefix where prefix='"+prefix+"'");
				rset=stmt.executeQuery();
				if(rset.next()){
					//stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp, tab_general = 1,tab_segm=1," +
					//		"tab_verf =1,tab_trans =1,tab_struct =1,tab_unknown =1,tab_finalm =1,tab_gloss =1,glossary= '" +glossaryName+"',option_chosen='"+optionChosen+"' where prefix='"+prefix+"'");
					stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp where prefix='"+prefix+"'"); //keep the status of markup from a previous run
					stmt.executeUpdate();
				}
				else
				{
					//stmt = conn.prepareStatement("insert into datasetprefix values ('"+ 
					//		prefix + "', current_timestamp, 1, 1, 1 ,1, 1, 1, 1, 1,'"+glossaryName+"','"+optionChosen+"')");
					stmt = conn
							.prepareStatement("insert into datasetprefix (prefix,time_last_accessed,tab_general,tab_segm,tab_verf,tab_trans,tab_struct_perl,tab_struct_one,tab_struct_two,tab_struct_three,tab_unknown,tab_finalm,tab_gloss,glossary,option_chosen) values ('"
									+ prefix
									+ "', current_timestamp, 1, 0, 0 ,0, 0, 0, 0, 0, 0, 0, 0,'"
									+ glossaryName
									+ "','"
									+ optionChosen
									+ "')"); // a new prefix, set all but the
												// first flag to 0
					stmt.executeUpdate();
				}
			}
		}catch (SQLException exe) {
			
			if (exe.getMessage().contains("key 'PRIMARY'")) {
				stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp" +
						" where prefix = '" + prefix + "'" ); 
				stmt.executeUpdate();
			}
			if (!exe.getMessage().contains("key 'PRIMARY'")) {
				LOGGER.error("Couldn't execute db query in MainFormDbAccessor:savePrefixdata", exe);
				throw new ParsingException("Failed to execute the statement.", exe);
			}
			
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
		}
	}
	
	
	public void loadStatusOfMarkUp(boolean [] markUpStatus, String dataPrefix) 
		throws 	ParsingException, SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			if(dataPrefix != null && !dataPrefix.equals("")) {
			//	conn = DriverManager.getConnection(url);
				stmt = conn.prepareStatement("select * from datasetprefix where prefix ='" + dataPrefix + "'");
				rset = stmt.executeQuery();
				
				if (rset != null && rset.next()) {
					
					/* Segmentation tab */
					if (rset.getInt("tab_segm") == 0) {
						markUpStatus[1] = false;
					} else {
						markUpStatus[1] = true;
					}
					
					/* Verification tab */
					if (rset.getInt("tab_verf") == 0) {
						markUpStatus[2] = false;
					} else {
						markUpStatus[2] = true;
					}
					
					/* Transformation tab */
					if (rset.getInt("tab_trans") == 0) {
						markUpStatus[3] = false;
					} else {
						markUpStatus[3] = true;
					}
					
					/* Structure Name Correction tab */
					/*if (rset.getInt("tab_struct") == 0) {
						markUpStatus[4] = false;
					} else {
						markUpStatus[4] = true;
					}*/
					
					/* Structure Name Correction tab */
					if (rset.getInt("tab_struct_perl") == 0) {
						markUpStatus[4] = false;
					} else {
						markUpStatus[4] = true;
					}
					
					/* Structure Name Correction tab */
					if (rset.getInt("tab_struct_one") == 0) {
						markUpStatus[5] = false;
					} else {
						markUpStatus[5] = true;
					}
					
					/* Structure Name Correction tab */
					if (rset.getInt("tab_struct_two") == 0) {
						markUpStatus[6] = false;
					} else {
						markUpStatus[6] = true;
					}
					
					/* Structure Name Correction tab */
					if (rset.getInt("tab_struct_three") == 0) {
						markUpStatus[7] = false;
					} else {
						markUpStatus[7] = true;
					}
					
					/* Unknown removal tab */
					if (rset.getInt("tab_unknown") == 0) { //5 -> 8
						markUpStatus[8] = false;
					} else {
						markUpStatus[8] = true;
					}
					
					/* Finalizer tab */
					if (rset.getInt("tab_finalm") == 0) { //6 -> 9
						markUpStatus[9] = false;
					} else {
						markUpStatus[9] = true;
					}
					
					/* Glossary tab */
					if (rset.getInt("tab_gloss") == 0) {//7 -> 10
						markUpStatus[10] = false;
					} else {
						markUpStatus[10] = true;
					}
					
					
				}
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
	//		if (conn != null) {
		//		conn.close();
			//}
			
		}
		

	}
	
	public void saveStatus(String tab, String prefix, boolean status) throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		int tabStatus = 0;
		//Lookup
		{
			if (tab.equals(ApplicationUtilities.getProperty("tab.one.name"))) {
				tab = "tab_general";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
				tab = "tab_segm";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
				tab = "tab_verf";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
				tab = "tab_trans";
			}else
				/*tab.five.perl.name=Step 4.perl
					tab.five.one.name=Step 4.1
					tab.five.two.name=Step 4.2
					tab.five.three.name=Step 4.3
					*/
			//if (tab.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
			//	tab = "tab_struct";
			//}
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.perl.name"))) {
				tab = "tab_struct_perl";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.one.name"))) {
				tab = "tab_struct_one";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.two.name"))) {
				tab = "tab_struct_two";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.three.name"))) {
				tab = "tab_struct_three";
			}else			
			if (tab.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
				tab = "tab_unknown";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.seven.name"))) {
				tab = "tab_finalm";
			}else
			if (tab.equals(ApplicationUtilities.getProperty("tab.character"))) {
				tab = "tab_gloss";
			}
		}
		
		 if (status == true) {
			 tabStatus = 1;
			 //tabStatus = 0;//changed to 0 by Prasad. if status is 0 that means processed and can be loaded
			 //status of 1 means yet to be loaded
		 } 
		
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("update datasetprefix set " + tab + "= " + tabStatus + 
					" where prefix='" + prefix +"'");
			stmt.executeUpdate();
			
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		
	}
	

	
	
	public ArrayList<String> getUnknownWords()throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		ArrayList<String> unknownWords = new ArrayList<String>();
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try{
			//conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("select word from "+this.prefix+"_unknownwords " +
					"where flag = ?");
			stmt.setString(1, "unknown");
			rset = stmt.executeQuery();
			if(rset != null) {
				while(rset.next()) {
					unknownWords.add(rset.getString("word"));
				}
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
				//conn.close();
			//}
			
		}		
		
		return unknownWords;
	}
	
	/** 
	 * This function will save terms from the Markup - (Structure tab) to database
	 * @param terms
	 * @param type 
	 */
	public boolean saveTermRole
		(ArrayList<String> terms, String role, UUID last, UUID current, String type)throws SQLException {
		//Connection conn = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			//conn = DriverManager.getConnection(url);
			String wordrolesable = this.prefix+ "_"+ApplicationUtilities.getProperty("WORDROLESTABLE");
			stmt = conn.createStatement();
			stmt.execute("delete from "+wordrolesable+" where savedid='"+last.toString()+"'");
			pstmt = conn.prepareStatement("Insert into "+wordrolesable+" (word,semanticrole, savedid, sourcetab) values (?,?, ?, ?)");
			//stmt = conn.prepareStatement("Update "+postable+" set saved_flag ='green' where word = ?");
			for (String term : terms) {
				pstmt.setString(1, term);
				pstmt.setString(2, role);	
				pstmt.setString(3, current.toString());
				pstmt.setString(4, type);
				try {
					pstmt.execute();
				} catch (Exception exe) {
				 if (!exe.getMessage().contains("Duplicate entry")) { //only insert unique term-category pairs
					 throw exe;
				 }
				}			
			}
			return true;
			//stmt.executeBatch();
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			return false;
		} finally {
			if(stmt!=null) stmt.close();
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}		
	}

	/**
	 * since the user may change their minds on whether a term is a non-eq term, use UUID to allow overwrite of previous decisions
	 * also upldate postable, non-eq terms will have a red saved_flag.
	 * when adding filtered terms to nonEQtable,  last = null and current = null, when adding terms categorized as "neither", both values are non-null values
	 * @param words
	 * @param last
	 * @param current
	 * @param type 
	 * @throws SQLException
	 */
	public boolean recordNonEQTerms(ArrayList<String> words, UUID last, UUID current) throws SQLException {
		boolean success2 = updatePOSTableWithNonEQTerms(words, last, current);
		boolean success1 = updateNonEQTermsTable(words, last, current);
		return success2 && success1;
	}
	/**
	 * when adding filtered terms to nonEQtable,  last = null and current = null
	 * @param words
	 * @param last
	 * @param current
	 * @return
	 * @throws SQLException
	 */
	private boolean updatePOSTableWithNonEQTerms(ArrayList<String> words, UUID last, UUID current) throws SQLException{
		//Connection conn = null;
		PreparedStatement pstmt = null ;
		Statement stmt = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			stmt = conn.createStatement();
			if(last!=null){
				//clean up last saved info
				stmt.execute("update "+this.prefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag = '' where savedid='"+last.toString()+"'");
			}
			if(current==null){
			//set flag in pos table
				//pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red' where pos=? and word=?");
				pstmt = conn.prepareStatement("update "+this.prefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red' where word=?");
			}else{
				//pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red', savedid='"+current.toString()+"' where pos=? and word=?");
				pstmt = conn.prepareStatement("update "+this.prefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red', savedid='"+current.toString()+"' where word=?");
			}
			for (String word : words) {
				//pstmt.setString(1, "b");
				//pstmt.setString(2, word);
				pstmt.setString(1, word);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			return true;
		} catch (SQLException e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			return false;
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			if (stmt != null) {
				stmt.close();
			}			
			
		}
	}
	
	private boolean updateNonEQTermsTable(ArrayList<String> words, UUID last, UUID current) throws SQLException{
		//Connection conn = null;
		PreparedStatement pstmt = null ;
		Statement stmt = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			stmt = conn.createStatement();
			//insert words in noneqterms table	
			//clean up last saved info
			if(last!=null){
				stmt.execute("delete from "+this.prefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ " where savedid='"+last.toString()+"'");
			}
			if(last!=null){
				pstmt = conn.prepareStatement("insert into "+this.prefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ "(term, source, savedid) values(?, ?, ?)");
			}else{
				pstmt = conn.prepareStatement("insert into "+this.prefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ "(term, source) values(?, ?)");
			}
			for (String word : words) {
					pstmt.setString(1, word);
					pstmt.setString(2, this.prefix);
					if(current!=null) pstmt.setString(3, current.toString());
					pstmt.addBatch();
			}
			pstmt.executeBatch();
			return true;
		} catch (SQLException e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			return false;
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			if (stmt != null) {
				stmt.close();
			}			
		}
	}
	
	
	
	public void createWordRoleTable(){
		//Connection conn = null;
		Statement stmt = null ;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+this.prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE"));
			stmt.execute("create table if not exists "+this.prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+ " (word varchar(50), semanticrole varchar(2), savedid varchar(40), sourcetab varchar(20), primary key(word, semanticrole))");			
		} catch (SQLException e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			try{
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}		
			}catch(Exception e){
				LOGGER.error("Exception in MainFormDbAccessor", e);
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
			
		}
	}

	//added newly to load the styled context for step 4 (all 4 sub-tabs)
	public void getContextData(String word,StyledText context) throws SQLException, ParsingException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			word = word.replaceAll("[_-]", "[ _-]");
			String sql = "select source,originalsent from "+this.prefix+"_sentence where originalsent rlike '[[:<:]]"+word+"[[:>:]]'";
			//String sql = "select source,originalsent from "+this.prefix+"_sentence where originalsent rlike '[[:<:]]"+word+"[[:>:]]' or tag = '"+word+"'";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			context.cut();
			String text = "";
			int count = 0;
			while (rs.next()) { //collect sentences
				count++;
				String src = rs.getString("source");
				String sentence = rs.getString("originalsent");
				text += count+": "+sentence+" ["+src+"] \r\n";
				//System.out.println(src+"::"+sentence+" \r\n");
				//context.append(src+"::"+sentence+" \r\n");
			}	
			text = text.toLowerCase();
			if(text.length()==0) {text="No context avaialable for "+word + ". Please categorize it as 'neither'.";}
			
			//format sentences
			ArrayList<StyleRange> srs = new ArrayList<StyleRange>();
			String[] tokens = text.split("\\s");
			int currentindex = 0;
			//String newtext = "";
			for(String token: tokens){
				if(token.matches(".*?\\b"+word+"\\b.*")){
					StyleRange sr = new StyleRange();
					sr.start = currentindex;
					sr.length = token.length();
					sr.fontStyle = SWT.BOLD;
					srs.add(sr);
				}else if(token.matches("^\\[.*?\\]$")){
					StyleRange sr = new StyleRange();
					sr.start = currentindex;
					sr.length = token.length();
					sr.foreground = MainForm.grey;
					srs.add(sr);
				}
				//newtext+=token+" ";
				currentindex +=token.length() + 1;				
			}
			context.append(text);
			context.setStyleRanges(srs.toArray(new StyleRange[]{}));
		} catch (SQLException e) {
			//LOGGER.error("Couldn't execute db query in MainFormDbAccessor:updateContextData", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to execute the statement.", e);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:updateContextData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
		//	if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	/**
	 * merge grouped_terms and group_decision table and add data into 
	 * term_category table, which may already have data
	 * also add newly learned structure term to the table for category "structure"
	 * This makes term_category contain all new terms learned from a volume of text
	 */
	public int finalizeTermCategoryTable() {
		int count = 0;
		Statement stmt = null;
		Statement stmt2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try{
			stmt = conn.createStatement();
			stmt2 = conn.createStatement();
			String q = "select distinct groupId, category from "+ prefix+"_group_decisions where category !='done'"; //"done" was a fake decision for unpaired terms
			rs = stmt.executeQuery(q);
			while(rs.next()){
				int gid = rs.getInt(1);
				String cat = rs.getString(2);
				rs2 = stmt2.executeQuery("select term, cooccurTerm from "+prefix+"_grouped_terms where groupId ="+gid);
				while(rs2.next()){
					String t1 = rs2.getString(1);
					String t2 = rs2.getString(2);
					insert2TermCategoryTable(t1, cat);
					//count++;
					if(t2!=null && t2.trim().length()>0){
						insert2TermCategoryTable(t2, cat);
						//count++;
					}
				}
			}
			
			//insert structure terms
			q = "select distinct word from "+prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where semanticrole in ('op', 'os') and " +
					" word not in (select distinct term from "+MainForm.getGlossary(MainForm.glossaryPrefixCombo.getText())+" where category in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative', 'structure'))";
			rs = stmt.executeQuery(q);
			while(rs.next()){
				String t = rs.getString(1);
				insert2TermCategoryTable(t, "structure");
				//count++;
			}
			
			rs = stmt.executeQuery("select distinct term from " + this.prefix +"_"+ApplicationUtilities.getProperty("TERMCATEGORY"));
			while(rs.next()){
				count++;
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
				if(rs2!=null) rs2.close();
				if(stmt2!=null) stmt2.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
		return count;
	}
	
	private void insert2TermCategoryTable(String term, String cat) throws SQLException {
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		String sql = "insert into " + this.prefix +"_"+ApplicationUtilities.getProperty("TERMCATEGORY")+"(term, category) values (?,?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, term);
		pstmt.setString(2, cat);
		pstmt.execute();
		pstmt.close();
	}

	/**
	 * correct the typos in database
	 * @param typos
	 * @return hashtable of typos with associated source files
	 * 
	 */
	public Hashtable<String, TreeSet<String>> correctTyposInDB(Hashtable<String, String> typos) {
		Hashtable<String, TreeSet<String>> typosources = new Hashtable<String, TreeSet<String>>();
		Enumeration<String> en = typos.keys();
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		while(en.hasMoreElements()){
			String typo = en.nextElement();
			String correction = typos.get(typo);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				//find sources
				TreeSet<String> sources = typosources.get(typo);
				stmt = conn.prepareStatement("select source from "+
						this.prefix+"_"+ ApplicationUtilities.getProperty("SENTENCETABLE") +
						" where sentence REGEXP '[[:<:]]"+typo+"[[:>:]]'"); //originalsent already corrected
			    rs = stmt.executeQuery();
				while(rs.next()){
					String src = rs.getString(1);
					src = src.substring(0, src.lastIndexOf("-"));					
					if(sources == null){
						sources = new TreeSet<String>();
					}
					sources.add(src);					
				}
				if(sources!=null)
					typosources.put(typo, sources);
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}finally{
				try{
					if(rs!=null) rs.close();
					if(stmt!=null) stmt.close();
				}catch(Exception e){
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
					LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
							System.getProperty("line.separator")
							+sw.toString());
				}
			}
			//make corrections in db tables
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("POSTABLE"), "word", typo, correction);
			correctTypoInTableWordMatch(ApplicationUtilities.getProperty("SENTENCETABLE"), "sentence",  typo, correction, "sentid");
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("ALLWORDS"), "word", typo, correction);
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("UNKNOWNWORDS"), "word", typo, correction);
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("TAXONNAMES"), "name", typo, correction);
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("SINGULARPLURAL"), "singular", typo, correction);
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("SINGULARPLURAL"), "plural", typo, correction);
			correctTypoInTableExactMatch(ApplicationUtilities.getProperty("NONEQTERMSTABLE"), "term", typo, correction);
		}
		return typosources;
		
	}
	
	/**
	 * 
	 * @param table
	 * @param typo
	 * @param correction
	 * @param exactmatch
	 */
	public void correctTypoInTableWordMatch(String table, String column, String typo, String correction, String PK) {
		PreparedStatement stmt = null;
		PreparedStatement stmt1 = null;
		ResultSet rs = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try{
			//mysql can't do word-based match, so had to update sentence one by one
			stmt = conn.prepareStatement("select "+PK+", "+column+" from "+this.prefix+"_"+table+" where "+column+" REGEXP '[[:<:]]"+typo+"[[:>:]]'");
		    rs = stmt.executeQuery();
			while(rs.next()){
				String key = rs.getString(1);
				String text = rs.getString(2);
				String correctioncp = correction;
				Pattern p = Pattern.compile("(.*?)\\b("+typo+")\\b(.*)", Pattern.CASE_INSENSITIVE);
				//need be case insenstive, but keep the original case
				Matcher m = p.matcher(text);
				while(m.matches()){
					text =m.group(1);
					String w = m.group(2);
					if(w.matches("^[A-Z].*")){
						correction = correction.substring(0,1).toUpperCase()+correction.substring(1); 
					}else{
						correction = correctioncp;
					}
					text+=correction;
					text+=m.group(3);
					m = p.matcher(text);
				}
				//put corrected back
			    stmt1 = conn.prepareStatement("update "+this.prefix+"_"+table+" set "+column+"='"+text+"' where "+PK+"='"+key+"'");
				stmt1.execute();
			}
		}catch(Exception e){
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
				if(stmt1!=null) stmt1.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}		
	}
	/**
	 * 
	 * @param table
	 * @param typo
	 * @param correction
	 * @param exactmatch
	 */
	public void correctTypoInTableExactMatch(String table, String column, String typo, String correction) {
		try{
			if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select "+column+" from "+this.prefix+"_"+table+" where "+column+"='"+correction+"'");
			if(rs.next()){ //correction row exists, delete the typo row
				stmt.execute("delete from "+this.prefix+"_"+table+" where "+column+"='"+typo+"'");
			}else{
				String	where = column+"='"+typo+"'";
				String 	set = column+"='"+correction+"'";
				PreparedStatement stmt1 = conn.prepareStatement("update "+this.prefix+"_"+table+" set "+set+" where "+where);
				stmt1.executeUpdate();
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}

	/**
	 * insert a record into the known typo database table.
	 * if (correction, typo) exists in the table, just remove the record, not need to insert (typo, correction).
	 * @param typo
	 * @param correction
	 */
	public void insertTypo(String typo, String correction){
		PreparedStatement stmt = null;
		PreparedStatement stmt1 = null;
		ResultSet rs = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try{
			stmt = conn.prepareStatement("select * from  "+this.prefix+"_"+ApplicationUtilities.getProperty("TYPOS")+" where typo = ? and correction = ?");
			stmt.setString(1, correction);
			stmt.setString(2, typo);
			rs = stmt.executeQuery();
			if(rs.next()){
				//delete the record
				stmt = conn.prepareStatement("delete from  "+this.prefix+"_"+ApplicationUtilities.getProperty("TYPOS")+" where typo = ? and correction = ?");
				stmt.setString(1, correction);
				stmt.setString(2, typo);
				stmt.execute();	
				return;
			}
			
			stmt1 = conn.prepareStatement("insert into  "+this.prefix+"_"+ApplicationUtilities.getProperty("TYPOS")+" (typo, correction) values (?, ?)");
			stmt1.setString(1, typo);
			stmt1.setString(2, correction);
			stmt1.execute();
		}catch(Exception e){
			if(!e.toString().contains("Duplicate entry")){ //ignore this exception
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
				if(stmt1 != null) stmt1.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}	
	}
	
	/**
	 * retrieve the last savedid for the type from wordroles table.
	 * 
	 * @param type: structure, descriptor, other
	 * @param prefix: datatable prefix
	 * @param conn
	 * @return UUID: the last id used in saving categorized terms
	 */
	public UUID getLastSavedId(String type) {
		PreparedStatement statement = null;
		ResultSet rs = null;
		try{
			statement = conn.prepareStatement("select savedid from "+prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE") +" where sourcetab = ? and ! isnull(savedid)");
			statement.setString(1, type);
			rs = statement.executeQuery();
			if(rs.next())
				return UUID.fromString(rs.getString(1));
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(statement!=null) statement.close();
			}catch(Exception e1){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e1.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());				
			}
		}
		return UUID.randomUUID();
	}
	
	
	/**
	 * read typos from the known database table into the hashtable.
	 * @param typos
	 */
	public void readInTypos(Hashtable<String, String> typos) {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		if(this.prefix ==null) this.prefix = MainForm.dataPrefixCombo.getText().trim();
		try{
			stmt = conn.prepareStatement("select typo, correction from "+this.prefix+"_"+ApplicationUtilities.getProperty("TYPOS"));
			rs = stmt.executeQuery();
			while(rs.next()){
				typos.put(rs.getString(1), rs.getString(2));
			}
		}catch(Exception e){
			e.printStackTrace();
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}		
		
	}
	
	


}
