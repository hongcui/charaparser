package edu.arizona.sirls.biosemantics.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.arizona.sirls.biosemantics.beans.ContextBean;
import edu.arizona.sirls.biosemantics.beans.TermsDataBean;
import edu.arizona.sirls.biosemantics.parsing.ApplicationUtilities;
import edu.arizona.sirls.biosemantics.parsing.MainForm;

@SuppressWarnings({ "unused" })
public class CharacterStateDBAccess {

	/**
	 * @param args
	 */
	private static final Logger LOGGER = Logger.getLogger(CharacterStateDBAccess.class);	
	private static String url = ApplicationUtilities.getProperty("database.url");
	private String prefix = null;
	private String glossarytable = null;
	private Connection conn = null;
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} 
	}
	
	public CharacterStateDBAccess(String prefix, String glossarytable){
		try{
			this.conn = DriverManager.getConnection(url);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		this.prefix = prefix;
		this.glossarytable = glossarytable;
	}
	
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		Connection conn = DriverManager.getConnection(url);
		System.out.println(conn);

	}
	
	public void getDecisionCategory(ArrayList<String> decisions) throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			
				//conn = DriverManager.getConnection(url);
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				
				//String sql = "SELECT distinct category FROM " + tablePrefix+"_character order by category";
				String sql = "SELECT distinct category FROM "+this.glossarytable+" order by category";
				stmt = conn.prepareStatement(sql);
				rset = stmt.executeQuery();
				while(rset.next()) {
					decisions.add(rset.getString(1));
				}
				
				
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getDecisionCategory", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			
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
//gets the default decision category which is the antglossary when an empty glossary is used
public void getDefaultDecisionCategory(ArrayList<String> decisions) throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			
				//conn = DriverManager.getConnection(url);
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				
				//String sql = "SELECT distinct category FROM " + tablePrefix+"_character order by category";
				String sql = "SELECT distinct category FROM "+this.glossarytable+" order by category";  //This line is changed to the following
				//String sql = "SELECT distinct category FROM antglossaryfixed order by category"; // The default glossary
				stmt = conn.prepareStatement(sql);
				rset = stmt.executeQuery();
				while(rset.next()) {
					decisions.add(rset.getString(1));
				}
				
				
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getDecisionCategory", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			
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

	
	public ArrayList<TermsDataBean> getTerms(String group) throws SQLException {
		//Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		
		group = group.substring(group.indexOf("_")+1);
		ArrayList<TermsDataBean > coOccurrences =null;
		if(group!=null && group.trim()!=""){
			
		coOccurrences = new ArrayList<TermsDataBean>();
				
		try {
			
			//conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select * from " + tablePrefix +"_grouped_terms " +
					"where groupId=" + group+ " order by frequency desc";
			
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			while(rset.next()) {
				TermsDataBean  tbean = new TermsDataBean();
				tbean.setGroupId(rset.getInt("groupId"));
				tbean.setTerm1(rset.getString("term"));
				tbean.setTerm2(rset.getString("cooccurTerm"));
				tbean.setFrequency(rset.getInt("frequency"));
				String files = rset.getString("sourceFiles");
				String [] sourceFiles = files.split(",");
				tbean.setSourceFiles(sourceFiles);
				tbean.setKeep(rset.getString("keep"));
				coOccurrences.add(tbean);
			}
			
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getTerms", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			
		}
		
		finally {
			if (rset != null) {
				rset.close();
			}
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		}
		return coOccurrences;
		
	}
	
	public ArrayList<ContextBean> getContext(String [] sourceFiles) throws Exception {
		//Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		ArrayList<ContextBean> contexts = new ArrayList<ContextBean>();
		String sql = "SELECT source, originalsent FROM "+ 
			MainForm.dataPrefixCombo.getText().trim() +"_sentence where source in (";
		for (String source : sourceFiles) {
			sql += "'" + source + "',";
		}
		
		sql = sql.substring(0, sql.lastIndexOf(",")) + ")";
		try {
			//conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			
			while(rset.next()){
				ContextBean cbean = new ContextBean(rset.getString("source"), rset.getString("originalsent"));
				contexts.add(cbean);
			}
						
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getTerms", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		return contexts;
	}
	
	public boolean saveTerms(ArrayList<TermsDataBean> terms) throws SQLException {
		
		if(terms == null || terms.size()==0) {
			return false;
		}
		//Connection conn = null;
		PreparedStatement pstmt = null; 
		String sql = "delete from " + MainForm.dataPrefixCombo.getText().trim() +"_grouped_terms where groupId=?";
		try {
			//conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, terms.get(0).getGroupId());
			pstmt.execute();
			
			sql = "insert into " + MainForm.dataPrefixCombo.getText().trim() +"_grouped_terms values(?,?,?,?,?,?)";
			
			pstmt = conn.prepareStatement(sql);
			
			for (TermsDataBean tbean : terms) {
				String t1 = tbean.getTerm1()==null?"":tbean.getTerm1();
				String t2 = tbean.getTerm2()==null?"":tbean.getTerm2();
				
					pstmt.setInt(1, tbean.getGroupId());
					pstmt.setString(2, t1);
					pstmt.setString(3, t2);
					pstmt.setInt(4, tbean.getFrequency());
					pstmt.setString(5, tbean.getKeep()==null?"":tbean.getKeep());
					
					String [] files = tbean.getSourceFiles();
					String sourceFile = "";
					for (String file : files) {
						sourceFile += file + ",";
					}
					sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf(","));
					
					pstmt.setString(6, sourceFile);
					pstmt.addBatch();
				
			}
			
			pstmt.executeBatch();
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:saveTerms", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			
		} finally {
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		
		return true;
	}
	
	public String getDecision(int groupId) throws SQLException {
		//Connection conn = null;
		PreparedStatement pstmt = null;
		String decision = "";
		ResultSet rset = null;
		String sql = "select category from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions where groupId=?" ;
		try {
			//conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			rset = pstmt.executeQuery();
			if(rset.next()) {
				decision = rset.getString(1);
			}
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getDecision", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		
		return decision;
		
	}
	
	public ArrayList<String> getProcessedGroups() throws SQLException {
		ArrayList<String> processedGroups = new ArrayList<String>();
		//Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "select groupId from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions order by groupId";
		ResultSet rset = null;
		try {
			//conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			
			while (rset.next()){
				processedGroups.add("Group_"+rset.getInt(1));
			}
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getProcessedGroups", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		return processedGroups;
	}
	
	public boolean saveDecision(int groupId, String decision) throws SQLException {
		
		//Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "delete from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions where groupId=?" ;
		try {
			
			/*Delete existing information */
			//conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			pstmt.execute();
			
			/* Insert the new decision */
			sql = "insert into " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions values (?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			pstmt.setString(2, decision);
			pstmt.execute();
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:saveDecision", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		return true;
	}

	/**
	 * save term/category to term_category table that is created in StateMatrix.java 
	 * @param term
	 * @param decision
	 */
	public boolean saveTermCategory(String groupID, String term, String decision) {
		//Connection conn = null;

		PreparedStatement pstmt = null;
		
		
		try {
			String prefix = MainForm.dataPrefixCombo.getText().trim();
			
			if(decision == null || decision.trim().length() <=0) { //terms without a decision, save to non-eq
				//pstmt = conn.prepareStatement("insert into "+ prefix +"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+"(term, source) values(?, ?)");
				//pstmt.setString(1, term);
				//pstmt.setString(2, prefix);
				//pstmt.execute();
				return false;
			}
			String sql = "delete from " + prefix +"_"+ApplicationUtilities.getProperty("TERMCATEGORY")+" where term=?" ;
			/*Delete existing information */
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, term);
			pstmt.execute();
			
			/* Insert the new decision */
			sql = "insert into " + prefix +"_"+ApplicationUtilities.getProperty("TERMCATEGORY")+"(term, category) values (?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, term);
			pstmt.setString(2, decision);
			pstmt.execute();

			
			/*savedecision for groupID*/
			this.saveDecision(Integer.parseInt(groupID), "done");
			
			
			/* if decision=structure, update wordrole table too.*/
			if(decision.compareToIgnoreCase("structure")==0){
				sql = "update " + prefix +"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" set semanticrole = 'op' where word = ?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, term);
				pstmt.execute();
			}
			
			
			return true;
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:saveTermCategory", exe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);exe.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally{
			try{
				if(pstmt!=null) pstmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
		return false;
		
	}

}
