/**
 * 
 */
package edu.arizona.sirls.biosemantics.input.cleantext2xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hong Cui
 * Convert abbreviated expressions to fully spelled words for Gray's
 *
 */
public class GrayAbbreviationHandler implements Comparator{
	
	static Hashtable<String, String> distr = new Hashtable<String, String>();
	static Hashtable<String, String> abbs = new Hashtable<String, String>();
	static ArrayList<String> keys = new ArrayList<String>();
	static{ 
		//this mapping was taken directly from Gray's
		//"X" crossed with, for a hybrid
		//"§" section
		//abbs used in descriptions
		abbs.put("Abund.", "abundant");
		abbs.put("Adj.", "adjacent");
		abbs.put("Adv.", "adventive");
		abbs.put("Auth.", "authors");
		abbs.put("Cleist. fls.", "cleistogamous flowers");
		abbs.put("Cosmop.", "cosmopolitan");		
		abbs.put("Esc.", "escaping or escaped");
		abbs.put("F.", "forma");
		abbs.put("ff.", "formae");
		abbs.put("Fls.", "flowers");
		abbs.put("stam. fls.", "staminate flowers");
		abbs.put("pist. fls.", "pistillate flowers");
		abbs.put("Fr.", "fruit");
		abbs.put("Freq.", "frequent");
		abbs.put("Natzd.", "naturalized");
		abbs.put("Indig.", "indigenous");
		abbs.put("Infreq.", "infrequent");
		abbs.put("Introd.", "introduced");
		abbs.put("Sd.", "Sound");
		abbs.put("Ser.", "Series");
		abbs.put("Subgen.", "subgenus");
		abbs.put("Subsp.", "subspecies");
		abbs.put("Syst.", "system");
		abbs.put("Temp.", "temperate");
		
		//distribution
		distr.put("Fla.", "Florida");
		distr.put("Colo.", "Colorado");
		distr.put("B.C.", "British Columbia");
		distr.put("Calif.", "California");
		distr.put("Can.", "Canada");
		distr.put("C.B.", "Cape Breton I, Nova Scotia");
		distr.put("Centr.", "central");
		distr.put("Ci.", "Connecticut");
		distr.put("Ci. val.", "valley of the Connecticut River, New England");
		distr.put("D.C.", "District of Columbia");
		distr.put("Del.", "Delaware");
		distr.put("E.", "eastern");
		distr.put("Eastw.", "eastward");
		distr.put("Ga.", "Georgia");
		distr.put("Greenl.", "Greenland");
		distr.put("(?<!("+Text2XML.ranks+"))\\s+I.", "island");
		distr.put("Id.", "island");
		distr.put("Ia.", "Iowa");
		distr.put("Ida.", "Ihado");
		distr.put("Ill.", "Illinois");
		distr.put("Ind.", "Indiana");
		distr.put("Kans.", "Kansas");
		distr.put("Ky.", "Kentucky");
		distr.put("L.", "lake");
		distr.put("L.I.", "Long Island, New York");
		distr.put("La.", "Louisiana");
		distr.put("Lab.", "Labrador");
		distr.put("L. Sup.", "Lake Superior");
		distr.put("Mackenz.", "Mackenzie District, Canada");
		distr.put("M.I.", "Magdalen Islands, Quebec, Canada");
		distr.put("Man.", "Manitoba");
		distr.put("Mass.", "Massachusetts");
		distr.put("Md.", "Maryland");
		distr.put("Me.", "Maine");
		distr.put("Mediterr. reg.", "Mediterranean region");
		distr.put("Mex.", "Mexico");
		distr.put("Mich.", "Michigan");
		distr.put("Minn.", "Minnesota");
		distr.put("Miss.", "Mississippi");
		distr.put("Mo.", "Missouri");
		distr.put("Mont.", "Montana");
		distr.put("Mt.", "mountain");
		distr.put("mts.", "mountains");
		distr.put("N.", "north");
		distr.put("N. Am.", "North America");
		distr.put("N.B.", "New Brunswick");
		distr.put("N.C.", "North Carolina");
		distr.put("N.D.", "North Dakota");
		distr.put("N.E.", "New England");
		distr.put("N.H.", "New Hampshire");
		distr.put("N.J.", "New Jersey");
		distr.put("N.M.", "New Mexico");
		distr.put("N.S.", "Nova Scotia");
		distr.put("N.Y.", "New York");
		distr.put("Ne.", "northeast");
		distr.put("Neb.", "Nebraska");
		distr.put("Nev.", "Nevada");
		distr.put("Nfld.", "Newfoundland");
		distr.put("Northw.", "northward");
		distr.put("Nw.", "northwest");
		distr.put("O.", "Ohio");
		distr.put("Okla.", "Oklahoma");
		distr.put("Ont.", "Ontario");
		distr.put("Oreg.", "Oregon");
		distr.put("Pa.", "Pennsylvania");
		distr.put("P.E.I.", "Prince Edward Island");
		distr.put("Pen.", "peninsula");
		distr.put("Que.", "province of Quebec");
		distr.put("R.", "river");
		distr.put("R.I.", "Rhode Island");
		distr.put("Reg.", "region");
		distr.put("S.", "south");
		distr.put("S. Am.", "South America");
		distr.put("S.C.", "South Carolina");
		distr.put("S.D.", "South Dakota");
		distr.put("Sask.", "Saskatchewan");
		distr.put("Scotl.", "Scotland");
		distr.put("Tenn.", "Tennessee");
		distr.put("Tex.", "Texas");
		distr.put("Trop.", "tropical");
		distr.put("Ung.", "Ungava District, Canada");
		distr.put("Va.", "Virginia");
		distr.put("Val.", "valley");
		distr.put("Vt.", "Vermont");
		distr.put("W.", "western");
		distr.put("W.I.", "West Indies");
		distr.put("W. Va.", "West Virginia");
		distr.put("Wash.", "Washington");
		distr.put("Westw.", "westward");
		distr.put("Wisc.", "Wisconsin");
		distr.put("Wyo.", "Wyoming");
		distr.put("Yuk.", "Yukon Territory");
		distr.put("Afr.", "Africa");
		distr.put("Ala.", "Alabama");
		distr.put("Alta.", "Alberta");
		distr.put("Alt.", "Altitude");
		distr.put("Am.", "America");
		distr.put("Arct.", "Arctic");
		distr.put("Ariz.", "Arizona");
		distr.put("Ark.", "Arkansas");
		distr.put("Atl.", "Atlantic");
		distr.put("Austral.", "Australia");
		distr.put("Southw.", "southward");
		distr.put("Southwestw.", "southwestward");
		distr.put("St. P. et Miq.", "St. Pierre et Miquelon, south of Newfoundland");
		distr.put("Subtrop.", "subtropical");
		distr.put("Sw.", "southwest");
		
		keys.addAll(distr.keySet());
		keys.addAll(abbs.keySet());
		
	}
	
	@SuppressWarnings("unchecked")
	public GrayAbbreviationHandler(){
		Collections.sort(keys, this); //sort according to the length of the keys, longest first
		System.out.println();
	}
	/**
	 * 
	 */
	public String toFullSpelling(String text){
		String original = text;
		for(String key: keys){
			String full = distr.get(key);
			if(full == null) full = abbs.get(key);
			key = key.replaceAll("\\.", "\\\\.");			
			Pattern p = Pattern.compile("(.*?)\\b("+key+")(.*)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(text);
			String result ="";
			while(m.matches()){
				String replacement = caseProper(m.group(2), full);
				result += m.group(1)+replacement;
				text = m.group(3);
				m = p.matcher(text);
			}
			result +=text;
			text = result;
		}
		
		if(!original.equals(text)){
			System.out.println("abb proccessed [o]: "+original);
			System.out.println("abb proccessed [r]: "+text);
		}
		return text;
	}
	
	
	/*
	 * if group starts with a lower case letter, 
	 * return full in lower case
	 * if group is capitalized
	 * return full capitalized
	 */
	private static String caseProper(String group, String full) {
		String firstchar = full.trim().charAt(0)+"";
		if(Character.isLowerCase(group.trim().charAt(0))){
			return full.replaceFirst(firstchar, firstchar.toLowerCase());
		}else{
			return full.replaceFirst(firstchar, firstchar.toUpperCase());
		}
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


	@Override
	public int compare(Object str1, Object str2) {
		if(((String)str1).length() > ((String)str2).length()) return -1;
		if(((String)str1).length() < ((String)str2).length()) return 1;
		return 0;
	}

}
