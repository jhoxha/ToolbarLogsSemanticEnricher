package triple.analyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import triple.parser.Quad;
import triple.parser.Triple;
import util.Constants;

public class MetadataAnalyzer {
	final static Logger logger = Logger.getLogger(MetadataAnalyzer.class);

	Map<String, ArrayList<Triple>> urls = new HashMap<String,  ArrayList<Triple>>();
	
	public MetadataAnalyzer(String filename){
		File file = new File(filename); 
		urls = new HashMap<String,  ArrayList<Triple>>();
		MetadataParser parser = new MetadataParser();

		if(file.isDirectory()){
			File[] files =file.listFiles();
			for(File childFile: files){
				try {
					urls.putAll(parser.getQuadsOfFile(childFile.getPath()));
				} catch (IOException e) {
					e.printStackTrace();
				}					
			}
		}else {
			try {
				urls= parser.getQuadsOfFile(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
		logger.info("Total unique_urls: " + urls.size());
	}
	
	private void getClassTypes(){
		List<Triple> url_triples = new ArrayList<Triple>();
		List<String> type_classes = new ArrayList<String>();
		
		String main_type_Class="", subject="";
		int count_withType=0;
		for (String url :  urls.keySet()) {
			logger.info(url);
			url_triples = new ArrayList<Triple>();
			type_classes = new ArrayList<String>();
			main_type_Class="";
			
			for(Triple triple: urls.get(url)){
				subject = triple.subject;
				if( (triple.predicate.contains(Constants.RDFS_TYPE)	|| triple.predicate.contains(Constants.OG_TYPE)) 
				  ){
//					logger.info("\t "+ triple.subject+" "+  triple.predicate+" "+triple.object);
					url_triples.add(triple);
					if(subject.trim().equals(url.trim())){
						main_type_Class = triple.object;
						logger.info("\t type: "+ main_type_Class);
					} else {
						type_classes.add(triple.object);
					}
				}
			}
			
			if(!main_type_Class.isEmpty()){
				count_withType++;
			} else {
				logger.info("\t type_classes: "+ type_classes);
			}
		}
		
		logger.info(" Total urls with Type "+count_withType +" out of " + urls.keySet().size());
	}
	
	public void getNullTypedUrls()  {
		logger.info("************	getNullTypedUrls	*******************");
		List<Triple> url_triples = new ArrayList<Triple>();
		int count_url_withNoTriples=0;
		int count_url_withTriplesbutNoType=0;
		int typePredicate=0;

		List<String> urls_noTriple = new ArrayList<String>();
		List<String> urls_noType = new ArrayList<String>();
		
		for(String unique_url: urls.keySet()){
			url_triples = urls.get(unique_url);
			if(url_triples.isEmpty()){
				//logger.info("url no triples:"+unique_url);
				count_url_withNoTriples++;
				urls_noTriple.add(unique_url);
			}else{
				for(Triple triple: url_triples){
					if(triple.predicate.toString().contains("type")){
						typePredicate++;
					}
				}

				if(typePredicate==0){
					//logger.info("url not typed:"+unique_url);
					count_url_withTriplesbutNoType++;
					urls_noType.add(unique_url);
				}
			}
			typePredicate=0;
		}	
		
		int total = count_url_withNoTriples+ count_url_withTriplesbutNoType;
		logger.info("************	count_url_withNoTriples Subtotal: "+count_url_withNoTriples+"	*******************");		
		logger.info("************	count_url_withTriplesbutNoType Subtotal: "+count_url_withTriplesbutNoType+"	*******************");		
		logger.info("************	getNullTypedUrls Total: "+total+"	*******************");

		
		logger.info("************	GROUPED urls_noTriple	*******************");
		groupURLsByPattern(urls_noTriple);
		logger.info("************	GROUPED urls_noType	*******************");
		groupURLsByPattern(urls_noType);
	}
	
	public void getEmptyTripleUrls()  {
		logger.info("************	getEmptyTripleUrls	*******************");
		int count=0;
		for(String unique_url: urls.keySet()){
			if(urls.get(unique_url).isEmpty()){
				//logger.info(unique_url);
				count++;
			}
		}	
		logger.info("************	getEmptyTripleUrls Total: "+count+"	*******************");
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
	
//	private int getURLsOfType(String predType, boolean printTriple){
//		String typeClass="";
//		Map<String, Integer> typeClasses = new HashMap<String, Integer>();
//		List<String> predTypeList= new ArrayList<String> ();
//		
//		int count=0;
//		int total_urls=0;
//		int freqTypeClass=0;
//		for(String unique_url: urls.keySet()){
//			logger.info(unique_url);
//			for(Triple triple: urls.get(unique_url)){
//				//logger.info("\t"+triple.subject + " "+triple.predicate+" "+triple.object);
//
//				if(triple.predicate.contains(predType) ){//&& triple.subject.equals(unique_url)
//					if(!predTypeList.contains(triple.predicate.toString())){
//						predTypeList.add(triple.predicate.toString());
//					}
//					if(printTriple){
//						logger.info("\t" + triple.subject +"\t" +  triple.predicate +"\t" +  triple.object);
//					}
//
//					typeClass = triple.object;
//					if(!typeClasses.containsKey(typeClass)){
//						typeClasses.put(typeClass, 1);
//					}else{
//						freqTypeClass=typeClasses.get(typeClass);
//						typeClasses.remove(typeClass);
//						freqTypeClass++;
//						typeClasses.put(typeClass,freqTypeClass);
//					}
//					
//					count++;
//				}
//			}
//			total_urls++;
//		}
//		
//		return count;
//	}

	 
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
	 

	public static void main(String[] args) {
//		MetadataAnalyzer analyser = new MetadataAnalyzer(DomainFile.EVENTFUL1);
			
		MetadataAnalyzer analyser = new MetadataAnalyzer(Constants.KNOWLEDGE_BASE );//+"//k-base-pig-20120806-1326792930" 
		analyser.getClassTypes();
//		analyser.printAllTriples();
//		analyser.getEmptyTripleUrls();
//		analyser.getNullTypedUrls();

//		analyser.getURLsOfType("type", true);
//		analyser.getURLsOfType(Constants.RDFS_TYPE, true);
//		analyser.getURLsOfType(Constants.OG_TYPE, true);
	}	 
}
