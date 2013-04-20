package triple.analyzer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import triple.parser.Triple;
import util.Constants;

public class MetadataParser {
	final static Logger logger = Logger.getLogger(MetadataParser.class);

	final static String EVENTFUL_TRIPLES_FILE1=Constants.TRIPLES_FOLDER+"eventful-extract.pig-part-m-00000";
	final static String EVENTFUL_TRIPLES_FILE2=Constants.TRIPLES_FOLDER+"eventful-extract.pig-part-m-00001";
	
	final static String UPCOMING_TRIPLES_FILE=Constants.TRIPLES_FOLDER+"upcoming-extract.pig-part-m-00000";
	final static String EVENTBRITE_TRIPLES_FILE=Constants.TRIPLES_FOLDER+"eventbrite-extract.pig-part-m-00000";
	
	Map<String, ArrayList<Triple>> urls = new HashMap<String,  ArrayList<Triple>>();
	public enum DomainFile {
	    EVENTFUL1,  EVENTFUL2, UPCOMING, EVENTBRITE 
	}		

	public MetadataParser(DomainFile file)  {
		urls = new HashMap<String,  ArrayList<Triple>>();
			try {
				switch(file){
					case EVENTFUL1:
						getTriplesOfFile(EVENTFUL_TRIPLES_FILE1);
						break;
					case EVENTFUL2:
						getTriplesOfFile(EVENTFUL_TRIPLES_FILE2);
						break;
					case UPCOMING:
						getTriplesOfFile(UPCOMING_TRIPLES_FILE);
						break;
					case EVENTBRITE:
						getTriplesOfFile(EVENTBRITE_TRIPLES_FILE);
						break;
					default:
						break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public MetadataParser(){
		urls = new HashMap<String,  ArrayList<Triple>>();
	}
	
	public MetadataParser(String filename){
		File file = new File(filename); 
		urls = new HashMap<String,  ArrayList<Triple>>();

		if(file.isDirectory()){
			//List<String> files = new ArrayList<String>();
			File[] files =file.listFiles();
			for(File childFile: files){
				try {
					getTriplesOfFile(childFile.getPath());
				} catch (IOException e) {
					e.printStackTrace();
				}					
			}
		}else {
			try {
				getTriplesOfFile(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
	}

	public boolean URLFoundInFile(String url){
		return (urls.keySet().contains(url));
	}
	
	public List<Triple> getTypedTriplesOfURL(String myurl){
		List<Triple> typed_triples = new ArrayList<Triple>();
		List<Triple> url_triples = new ArrayList<Triple>();
		
		for(String unique_url: urls.keySet()){
			if(unique_url.contains(myurl)){ //EDIT THIS ???
				url_triples.addAll(urls.get(unique_url));
			} 
		}
		for(Triple triple: url_triples){
			if( (triple.predicate.equals(Constants.NS_RDF+"type")	|| triple.predicate.equals(Constants.NS_OG+"type")) 
					//&&
				//(triple.subject.toString().equals(unique_url))
			  ){
				//logger.info("\t type: "+ triple.object.toString());
				typed_triples.add(triple);
			}
		}
		
		return typed_triples;
	}
	
	public List<Triple> getAllTriplesOfURL(String myurl){
		List<Triple> myurl_triples = new ArrayList<Triple>();
		
		for(String unique_url: urls.keySet()){
			if(unique_url.equals(myurl)){ 
				for(Triple triple: urls.get(unique_url)){
						myurl_triples.add(triple);
				}
			}
		}
		
		return myurl_triples;
	}	
	
	public List<Triple> getAllFilteredTriplesOfURL(String myurl){
		/*
		 * http://schema.org/MusicGroup/mainContentOfPage
		 * http://schema.org/MusicGroup/interactionCount
		 * http://schema.org/Offer/url
		 * http://schema.org/WebPage/breadcrumb
		 * 
		 */
		List<Triple> myurl_triples = new ArrayList<Triple>();
		
		for(String unique_url: urls.keySet()){
			if(unique_url.equals(myurl)){ 
				for(Triple triple: urls.get(unique_url)){
					if(	!triple.predicate.equals(Constants.NS_SCHEMA_ORG+"/MusicGroup/mainContentOfPage") &&
						!triple.predicate.equals(Constants.NS_SCHEMA_ORG+"/MusicGroup/interactionCount") 	&&
						!triple.predicate.equals(Constants.NS_SCHEMA_ORG+"/Offer/url") 	&&
						!triple.predicate.equals(Constants.NS_SCHEMA_ORG+"/WebPage/breadcrumb") 	
					  ){
						myurl_triples.add(triple);
					}
				}
			}
		}
		
		return myurl_triples;
	}	
	
	public Map<String, ArrayList<Triple>> getQuadsOfFile(String filename) throws IOException{
		// Get the object of DataInputStream
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		logger.info("Reading file: "+filename);
		String singleUrl="";
		int countUrls=0;
		
		ArrayList<Triple> triples = new  ArrayList<Triple>();
		Triple triple = null;
		String prevUrl="";
		String strLine=""; 
		
		String[] fields;
		
		while ((strLine = br.readLine()) != null)   {
			fields = strLine.split("\t");
			singleUrl = fields[3].trim();
			  if(!singleUrl.equals(prevUrl) && !prevUrl.isEmpty() && !urls.containsKey(prevUrl)){
				  urls.put(prevUrl, triples);
				  countUrls++;
				  triples = new  ArrayList<Triple>();
			  }
			  triple = new Triple(fields[0].trim(),fields[1].trim(),fields[2].trim());
			  if (triple!=null){
				  triples.add(triple);
			  }
			  prevUrl = singleUrl;

		  }
		input1.close();
		br.close();
		
		logger.info("************	Total Resource URLs: "+countUrls+"*******************");	
		
		return urls;
	}
	
	private void getTriplesOfFile(String filename) throws IOException{
		// Get the object of DataInputStream
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		logger.info("Reading file: "+filename);
		String singleUrl="";
		int countUrls=0;
		
		ArrayList<Triple> triples = new  ArrayList<Triple>();
		Triple triple = null;
		String prevUrl="";
		String strLine=""; 
		
		while ((strLine = br.readLine()) != null)   {
			  if(strLine.contains("{") && strLine.contains("\t")){
				  singleUrl=strLine.substring(0,strLine.indexOf("\t")).trim();
				  
				  if(!singleUrl.equals(prevUrl) && !prevUrl.isEmpty() && !urls.containsKey(prevUrl)){
					  urls.put(prevUrl, triples);
					  countUrls++;
				  }
				  prevUrl = singleUrl;
				  triples = new  ArrayList<Triple>();
			  }
			  
			  triple = getURLTriple(strLine, filename);
			  if (triple!=null){
				  triples.add(triple);
			  }

		  }
		input1.close();
		br.close();
		
		//logger.info("************	Total Resource URLs: "+countUrls+"*******************");		
	}
	
	private Triple getURLTriple(String strLine, String filename){
		String tripleStr="";
		Triple triple = null;
		Node[] nodes=new Node[3];
		
		try {
			if(!strLine.contains("{") || 
					(strLine.contains("\"") && strLine.indexOf("\"")<strLine.indexOf("{")) || 
					(strLine.contains("<") && strLine.indexOf("<")<strLine.indexOf("{")) ){
				tripleStr = strLine;
			}else{
				tripleStr= strLine.substring(strLine.indexOf("{")+2);
			}
			
			if(!tripleStr.equals(")}") ){
				if(tripleStr.contains("_:node")){
					String bnode1Str = tripleStr.substring(0, tripleStr.indexOf(" "));
					String bnode2Str = tripleStr.substring(tripleStr.lastIndexOf("_"),tripleStr.lastIndexOf(".")-1);
					tripleStr = tripleStr.replace(bnode1Str, "<"+bnode1Str+">");
					if(bnode2Str.contains("_:node")){
						tripleStr = tripleStr.replace(bnode2Str, "<"+bnode2Str+">");

					}
				}

				nodes = NxParser.parseNodes(tripleStr);
				triple = new Triple(nodes[0].toString(),nodes[1].toString(),nodes[2].toString());
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
			//logger.info("strLine:"+strLine);
			//logger.info("\t tripleStr:"+tripleStr);
		} 
		
		return triple;
	}
	
	
	
	private void groupURLsByPattern(List<String> urls){
		Map<Pattern, ArrayList<String>> urls_byPattern = new HashMap<Pattern, ArrayList<String>>();
		ArrayList<String> urls_noPattern = new ArrayList<String>();
		for (Pattern pattern: Pattern.values()){  
    		urls_byPattern.put(pattern, new ArrayList<String>());
        }  		
		
		Collections.sort(urls);
		boolean matched=false;

		for(String url:urls){
			matched=false;
			
			logger.info("URL "+ url);  
			for (Pattern pattern: Pattern.values()){  
	            //logger.info("\t Pattern "+ pattern);  
				if(url.matches(pattern.description())){
					logger.info("\t YES: "+pattern + " matched in " + url);  
					urls_byPattern.get(pattern).add(url);
					matched=true;
				}
	        }
			if(!matched){
				logger.info("\t NO: noPattern matched in " + url);  
				urls_noPattern.add(url);
			}
			
//			if(url.matches(PATTERN_SEARCH)){
//				urls_byPattern.get(PATTERN_SEARCH).add(url);
//			}else if(url.matches(PATTERN_PERFORMERS)){
//				urls_byPattern.get(PATTERN_PERFORMERS).add(url);
//			}else if(url.matches(PATTERN_DEMAND)){
//				urls_byPattern.get(PATTERN_DEMAND).add(url);
//			}else if(url.matches(PATTERN_VENUE)){
//				urls_byPattern.get(PATTERN_VENUE).add(url);
//			}else if(url.matches(PATTERN_TICKETS)){
//				urls_byPattern.get(PATTERN_TICKETS).add(url);
//			}else if(url.matches(PATTERN_GROUPED_LOCATION_EVENTS)){
//				urls_byPattern.get(PATTERN_GROUPED_LOCATION_EVENTS).add(url);
//			}else{
//				urls_noPattern.add(url);
//			}
		}
		
		int nrGroupedUrls = 0;
		for(Pattern pattern: urls_byPattern.keySet()){
			logger.info(urls_byPattern.get(pattern).size()+" urls of pattern :"+pattern);
			nrGroupedUrls = nrGroupedUrls +  urls_byPattern.get(pattern).size();
		}
		
		logger.info(" There are still ("+ urls.size()+"-"+nrGroupedUrls+") = "+urls_noPattern.size()+" left with NO pattern group: ");
		for(String url: urls_noPattern){
			logger.info("\t"+url);
		}
			
	}	
	
	public void printAllTriples(){
		logger.info("********************	printAllTriples	*************************************");
		for(String unique_url: urls.keySet()){
			logger.info("unique_url:"+unique_url);
			for(Triple tr: urls.get(unique_url)){
				logger.info("\t tr: "+tr.print());
			}
			
		}		
	}


	public List<String> getClassTypeOfURL(String url, List<Triple> url_triples, boolean printTriple){
			String main_type_Class="";
			List<String> typeClasses = new ArrayList<String>();
			
			String pred_type_simple = "type";
			String pred_type_rdfs = Constants.NS_RDF+"type";
			String pred_type_og = Constants.NS_OG+"type";

			
			for(Triple triple: url_triples){
				if(triple.predicate.contains(pred_type_simple) ||  
				   triple.predicate.equals(pred_type_rdfs) || 
				   triple.predicate.equals(pred_type_og)){
				
					if(triple.subject.equals(url) || (triple.object.equals("activity") 
							|| triple.object.equals("musician") || triple.object.equals("politician") || triple.object.equals("eventbriteog:event"))){
						main_type_Class = triple.object.toString();
						logger.info("\t main_type_Class: " + main_type_Class);
					}else{
						typeClasses.add(triple.object.toString());
					}
					
					
					if(printTriple){
						logger.info(url+"\t" + triple.subject +"\t" +  triple.predicate +"\t" +  triple.object);
					}

				}
			}
			
			if(!main_type_Class.isEmpty()){
				typeClasses = new ArrayList<String>();
				typeClasses.add(main_type_Class);
			}
			
			return typeClasses;
		}
	
	 
	 private enum Pattern{  
		 	PATTERN_PERFORMERS (0,".*performers/.*"),
			PATTERN_DEMAND (1,".*demand/.*"),
			PATTERN_VENUE (2,".*venues/.*"),
			PATTERN_TICKETS (3,".*tickets/.*"),
			PATTERN_GROUPED_LOCATION_EVENTS (4,".*/events/*.*"),
			
			PATTERN_COMPETITION (5,".*competitions/.*"),
			PATTERN_JOIN (6,".*join[/|\\?].*"),
			PATTERN_PASSWORDRESET (7,".*password_reset.*"),
			PATTERN_UNSUBSCRIBE (8,".*preferences/email/unsubscribe\\?.*"),
			PATTERN_SIGNIN_GOTO (9,".*signin?goto.*"),
			PATTERN_TOOLS_CLICK (10,".*tools/click/url.*\\?token.*"),
			PATTERN_TRACKER (11,".*tracker\\?.*"),
			PATTERN_MOVIES (12,".*movies.eventful.com.*"),
			PATTERN_SEARCH_EVENTS (13, ".*/events\\?.*=.*"),
		 	PATTERN_GROUPED_LOCATION_CONCERTS (14,".*/concerts\\?*.*");
	          
	        private final int nr;  
	        private final String description;  
	          
	        Pattern(int nr, String desc){  
	            this.nr = nr;  
	            this.description = desc;  
	        }  
	        public int nr(){  
	            return this.nr;  
	        }  
	        public String description(){  
	            return this.description;  
	        }
	 }
	 
	public boolean isValidURL(String url, String urlhost){
//		String host = Utils.
		boolean is_valid=true;
		if(urlhost.equals("eventful.com")){
			/*
			 * Pattern: /events?
			 * Pattern: events/categories
			 * /categories/
			 * /search
			 * now-playing?
			 * widgets/demands
			 * http://eventful.com/performers
			 * demand/hottest
			 * /preferences
			 * utm_source=email
			 * page_number
			 * theaters-showtimes
			 * /tools/
			 * /reviews/
			 * /users/
			 * ?geo=
			 * favorite-theaters
			 * /showtimes
			 */
			if( url.matches(".*\\/my") ||
				url.matches(".*\\/demand.*") ||
				url.matches(".*\\/events\\?.*") ||
				url.matches(".*\\/events\\/categories.*") ||
				url.matches(".*\\/categories\\/.*") ||
				url.matches(".*\\/search.*") ||
				url.matches(".*\\/now\\-playing\\?.*") ||
				url.matches(".*widgets\\/demands.*") ||
				url.equals("http://eventful.com/performers") ||
				url.matches(".*demand\\/hottest.*") ||
				url.matches(".*\\/preferences.*") ||
				url.matches(".*utm_source\\=email.*") ||
				url.matches(".*utm_source.*") ||
				url.matches(".*page_number.*") ||
				url.matches(".*\\/tools\\/.*") ||
				url.matches(".*\\/reviews\\/.*") ||
				url.matches(".*\\/users\\/.*") ||
				url.matches(".*\\?geo\\=.*") ||
				url.matches(".*favorite\\-theaters.*") ||
				url.matches(".*\\/theaters\\-showtimes.*") ||
				url.matches(".*\\/showtimes.*") 
				){
				is_valid = false;
			}			
			
			
		} else if(urlhost.equals("eventbrite.com")){
			/*
			 * Pattern: /edit?
			 * /mytickets/
			 * /myevent or /myevents?
			 * /login
			 * /reports?
			 * /charts?
			 * /attendee-delete?
			 * /create
			 * /myprofile
			 * /order-refund
			 * err=
			 * /signup
			 * &error=
			 * ?discount
			 * ?tbc=
			 * efblike?
			 * invite=
			 * register?
			 * invite-friends
			 * contact-organizer?
			 * ?ref=enivtefor
			 * register?orderid=
			 * /mycontacts
			 * /editcontacts?
			 * /checkin?
			 * /attendees-
			 * utm_source=
			 * /publish?
			 * /invites
			 * /news
			 * /saveinvite
			 * /facebook-publish
			 */
			if( url.matches(".*\\/edit\\?.*") ||
					url.matches(".*\\/mytickets.*") ||
					url.matches(".*\\/myevent.*") ||
					url.matches(".*\\/login.*") ||
					url.matches(".*\\/reports\\?.*") ||
					url.matches(".*\\/charts\\?.*") ||
					url.matches(".*\\/attendee\\-delete\\?.*") ||
					url.matches(".*\\/attendees\\-add\\?.*") ||
					url.matches(".*\\/create.*") ||
					url.matches(".*\\/myprofile.*") ||
					url.matches(".*\\/order\\-refund.*") ||
					url.matches(".*err\\=.*") ||
					url.matches(".*\\/signup.*") ||
					url.matches(".*\\&error\\=.*") ||
					url.matches(".*\\?discount.*") ||
					url.matches(".*\\?tbc=.*") ||
					url.matches(".*\\/efblike\\?.*") ||
					url.matches(".*invite\\=.*") ||
					url.matches(".*register\\?.*") ||
					url.matches(".*invite\\-friends.*") ||
					url.matches(".*contact\\-organizer\\?.*") ||
					url.matches(".*\\?ref\\=enivtefor.*") ||
					url.matches(".*register\\?orderid\\=.*") ||
					url.matches(".*\\/mycontacts.*") ||
					url.matches(".*\\/editcontacts.*") ||
					url.matches(".*\\/checkin\\?.*")||
					url.matches(".*\\/attendees\\-.*") ||
					url.matches(".*utm\\_source\\=.*") ||
					url.matches(".*\\/publish\\?.*") ||
					url.matches(".*\\/invite.*") ||
					url.matches(".*\\/news.*") ||
					url.matches(".*\\/saveinvite.*") ||
					url.matches(".*\\/facebook\\-publish.*") 
					){
				is_valid = false;		
			}				
		} else if(urlhost.equals("yahoo.com")) {
			/*
			 * Pattern: search/?
			 * /user/
			 * /maps
			 * /tag/
			 */
			if( url.matches(".*search\\/\\?.*") ||
					url.matches(".*\\/user\\/.*") ||
					url.matches(".*\\/maps.*")	||
					url.matches(".*\\/tag\\/.*") ){
				is_valid = false;
			}				
		}
		
		if( url.matches(".*\\.pdf.*") ||
			url.matches(".*\\.jpg.*")){
			is_valid = false;	
		}
		
		if(!is_valid){
			logger.info("NOT valid url: "+ url);
		}


		return is_valid;
	}

	public static void main(String[] args) {
		MetadataParser parser = new MetadataParser(DomainFile.EVENTFUL1);
		//parser.printAllTriples();
		
	}	 
}
