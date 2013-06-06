package edu.arizona.sirls.biosemantics.parsing;
	
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 * @author Hong Updates
 *
 */
@SuppressWarnings("unchecked")
public class OpenXMLZipFile
{
	//  CreateZipFile method which will take the zipFileName and ToCompressFiles as arguments
	//   and will go through the array of ToCompressFiles and pack it into zipFileName 
	 private static final Logger LOGGER = Logger.getLogger(OpenXMLZipFile.class);  

 	public static void CreateZipFile(String zipFileName, File[] ToCompressFiles)
	{
		try
		{
			File[] fileNames = ToCompressFiles;

			FileInputStream inStream;

			// "C:\\ZipExample1.zip"
			FileOutputStream outStream = new FileOutputStream(zipFileName);
			ZipOutputStream zipOStream = new ZipOutputStream(outStream);

			zipOStream.setLevel ( Deflater.BEST_COMPRESSION );

			for (int loop=0;loop < fileNames.length; loop++)
			{
				inStream = new FileInputStream(fileNames[loop]);
				zipOStream.putNextEntry(new ZipEntry(fileNames[loop].getAbsolutePath()));

				int i=0;
				while ((i=inStream.read())!=-1)
				{
			 	   zipOStream.write(i);
			 	}

				zipOStream.closeEntry();
				inStream.close();
			}
			zipOStream.flush();
			zipOStream.close();
		}
		catch (Exception e){
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
