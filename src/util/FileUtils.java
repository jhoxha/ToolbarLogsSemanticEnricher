package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FileUtils {
	
	static List<File> files = new ArrayList<File>();

	public FileUtils(){
	}
	
	public static List<File>  listFilesForFolder(final File folder) {
		 for (final File fileEntry : folder.listFiles()) {
             if (fileEntry.isDirectory()) {
	             listFilesForFolder(fileEntry);
	         } else {
	        	 files.add(fileEntry);
	         }
	     }
		 
		 return files;
	 }

	  public Vector fileToVector(String fileName) {
		    Vector v = new Vector();
		    String inputLine;
		    try {
		      File inFile = new File(fileName);
		      BufferedReader br = new BufferedReader(new InputStreamReader(
		          new FileInputStream(inFile)));

		      while ((inputLine = br.readLine()) != null ) {
		        v.addElement(inputLine.trim());
		      }
		      br.close();
		    } catch (FileNotFoundException ex) {
		    	ex.printStackTrace();
		    } catch (IOException ex) {
		    	ex.printStackTrace();
		    }
		    return (v);
		  }	
	public static void main(String[] args) {
		List<File> files = FileUtils.listFilesForFolder(new File(Constants.PATH));
		for(File fileEntry: files){
             System.out.println(fileEntry.getPath() +" "+fileEntry.getName());
		 }
	}

}
