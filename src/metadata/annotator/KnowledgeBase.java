package metadata.annotator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;

import org.apache.log4j.Logger;

import triple.analyzer.MetadataParser;
import triple.analyzer.MetadataParser.DomainFile;
import triple.parser.Quad;
import triple.parser.Triple;
import util.Constants;

public class KnowledgeBase {
	final static Logger logger = Logger.getLogger(KnowledgeBase.class);
	Map<String, List<Log>> sessions = new HashMap<String, List<Log>>();
	static List<String> uniqueURLs = new ArrayList<String>();
	String logsFile="";

	static List<String> urls_not_enriched= new ArrayList<String>();
	LogSemanticEnricher annotator = new LogSemanticEnricher();
	
	private List<Quad> KNOWLEDGE_BASE = new ArrayList<Quad>();
	
	public KnowledgeBase(String logsFile){
		this.logsFile = logsFile;
		sessions = LogParser.readEventSessionsFromFile(logsFile);
		KNOWLEDGE_BASE = new ArrayList<Quad>();
	}
	
	public KnowledgeBase(){
		KNOWLEDGE_BASE = new ArrayList<Quad>();
	}	
	
	private void countURLWithoutTriples(){
		Map<String, Integer> url_triples_count = new HashMap<String, Integer>();
		logger.info("************* URLs with no triples **********");
		int count=0;
		for(String unique_url: uniqueURLs){
			if(!url_triples_count.keySet().contains(unique_url)){
				logger.info("\t" + unique_url);
				count++;
			}
		}
		logger.info("Total URLs with no triples: " + count);
	}
	
	public void addToKB(String toFile, DomainFile domain) {
		for (String sessid : sessions.keySet()) {
			for (Log log : sessions.get(sessid)) {
				if (!uniqueURLs.contains(log.getUrl())) {
					uniqueURLs.add(log.getUrl()); //"http://"+
				}
			}
		}
		logger.info("uniqueURLs nr="+uniqueURLs.size());
		
		List<Quad> quads = new ArrayList<Quad>();
		List<Quad> all_quads = new ArrayList<Quad>();
		List<Triple> url_typedtriples = new ArrayList<Triple>();
		List<Triple> url_triples = new ArrayList<Triple>();
		MetadataParser parser = new MetadataParser(domain);

		List<String> url_typeclasses= new ArrayList<String>();
		
		for (String url : uniqueURLs) {
			logger.info(" " + url);
			quads = new ArrayList<Quad>();
			url_typedtriples = parser.getTypedTriplesOfURL(url);
			url_typeclasses= new ArrayList<String>();

			if(url_typedtriples.size()>0){
				logger.info("found " + url_typedtriples.size() +" typed-triples for url " + url);

				url_typeclasses= parser.getClassTypeOfURL(url, url_typedtriples, false);
				logger.info("\t type is: " + url_typeclasses);
				//typeclasses.addAll(url_typeclasses);
			}
			
			if(url_typeclasses.size()>1){
				logger.info("\t url_typeclasses.size : " + url_typeclasses.size());
			}
			if(url_typeclasses.size()>=1){ //url_typeclasses.size()==1
				/*
				 * ENRICH HERE WITH MORE TRIPLES
				 * AND MAKE ALIGNMENT
				 */
				logger.info("\t url_typeclass  : " + url_typeclasses);
				url_triples = parser.getAllFilteredTriplesOfURL(url);
				quads= annotator.generateMetadata(url, url_triples);
				urls_not_enriched.remove(url);
		
				if (quads.size() > 0) {
					all_quads.addAll(quads);
				}else {
					logger.info("No triples found in " + domain + " for url: "+ url);
				}
			}
		}

		
		try {
			saveMetadata(toFile, all_quads);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveMetadata(String filename, List<Quad> quads) throws IOException {
		Date date = new Date();
		//filename += date.hashCode();
		
		logger.info("**SAVE : " + filename + "********************************");
		FileWriter fstream = new FileWriter(filename,true);
		BufferedWriter out = new BufferedWriter(fstream);
		
		for (Quad quad : quads) {
			out.write(quad.print().replace("\n", "") + " \n");
		}

		out.close();
	}
	

	
	public void readKNOWLEDGE_BASE(){
		List<Quad> all_quads = new ArrayList<Quad>();
		File file = new File(Constants.KNOWLEDGE_BASE); 
		File childFile;
		if(file.isDirectory()){
			File[] files =file.listFiles();
			for(int i=0; i<files.length; i++){//files.length
				childFile =files[i];
				try {
					all_quads.addAll(getQuadsOfFile(childFile.getPath()));
				} catch (IOException e) {
					e.printStackTrace();
				}					
			}
		}
		
		if(!all_quads.isEmpty()){
			setKNOWLEDGE_BASE(all_quads);
		}
		logger.info("Total resource triples: " + KNOWLEDGE_BASE.size());
	}
	
	private List<Quad> getQuadsOfFile(String filename) throws IOException{
		List<Quad> quads = new ArrayList<Quad>();
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		String[] fields;
		String strLine=""; 
		Quad quad;
		
		while ((strLine = br.readLine()) != null)   {
			strLine=strLine.replace("\t",",");//.replace(" ",",");
			fields= strLine.split(",");
			quad = new Quad(fields[0].trim(), fields[1].trim(), fields[2].trim(), fields[3].trim());
			KNOWLEDGE_BASE.add(quad);
		}
		
		return quads;
			
	}
	
	public void setKNOWLEDGE_BASE(List<Quad> quads) {
		KNOWLEDGE_BASE = quads;
	}	

	public List<Quad> getKNOWLEDGE_BASE() {
		return KNOWLEDGE_BASE;
	}

	
	public List<Quad> getResourceTriples(String URI){
		List<Quad> url_quads = new ArrayList<Quad>();
		for(Quad quad: KNOWLEDGE_BASE){
			if(quad.subject.equals(URI) && !URI.isEmpty()){ //context
				url_quads.add(quad);
			}
		}
		
		return url_quads;
	}
	
	public void constructKB(String dirname){
		File dir = new File(dirname);
		File childFile;
		String logsFile;
		urls_not_enriched.addAll(uniqueURLs);
		
		if(dir.isDirectory()){
			File[] files = dir.listFiles();
			for(int i=32; i<files.length; i++){ //files.length
				childFile =files[i];
				logsFile = childFile.getPath();
				String toKBFile = Constants.KNOWLEDGE_BASE + "k-base-" + logsFile.substring(logsFile.length()-8);
				logger.info("Reading file: " + logsFile + " (" + i +" out of "+ files.length+")");

				KnowledgeBase kbase = new KnowledgeBase(logsFile);
				kbase.addToKB(toKBFile, DomainFile.EVENTFUL1);

				kbase.addToKB(toKBFile, DomainFile.EVENTFUL2);

				kbase.addToKB(toKBFile, DomainFile.EVENTBRITE);
				kbase.addToKB(toKBFile, DomainFile.UPCOMING);					
				logger.info("Finished with file: " + toKBFile + " (" + i +" out of "+ files.length+")");
			}
		}
		
		logger.info("*************** " );
		for (String url_not_enriched : urls_not_enriched) {
			logger.info("DEAL WITH: " + url_not_enriched);
		}
		
		
		//appendOntology();
	}	
	
	private void appendOntology(){
		List<String> localities = new ArrayList<String>();
		List<String> regions = new ArrayList<String>();
		
		
		for(Quad quad: KNOWLEDGE_BASE){
			if(quad.predicate.equals(Constants.NS_SCHEMA_ORG+"addressLocality") ||
			   quad.predicate.equals(Constants.NS_OG+"locality") ||
			   quad.predicate.equals(Constants.NS_VCARD+"locality")){ 
			
				if(localities.contains( quad.object)){
					localities.add( quad.object);
				}
			}
			
			if(quad.predicate.equals(Constants.NS_SCHEMA_ORG+"addressRegion") ||
			   quad.predicate.equals(Constants.NS_OG+"region") ||
			   quad.predicate.equals(Constants.NS_VCARD+"region")){ 
				if(regions.contains( quad.object)){
					regions.add( quad.object);
				}
			}			
		}
		
		logger.info("localities : ");
		for(String locality: localities){
			logger.info("\t"+ locality);
		}
		
		logger.info("regions : ");
		for(String region: regions){
			logger.info("region: "+ region);
		}
		
	}
	public static void main(String[] args) {
		Date t1 = new Date();
		logger.info("Start KnowledgeBase-constructKB " +  t1.toLocaleString() );
		
		KnowledgeBase kbase = new KnowledgeBase();
		kbase.constructKB(Constants.EVENTLOGS_FOLDER);

		Date t2 = new Date();
        long diff = t2.getTime() - t1.getTime();
        long diffSeconds = diff / 1000 % 60;  
        long diffMinutes = diff / (60 * 1000) % 60; 
		logger.info("Ended KnowledgeBase-constructKB " + t2.toLocaleString() + ", duration:" +diffMinutes+"min, "+ diffSeconds+ " secs");
		
//		KnowledgeBase kb = new KnowledgeBase();
//		kb.readKNOWLEDGE_BASE();
//		
//		String url="http://upcoming.yahoo.com/event/9035830/PA/Hershey/Mamma-Mia-National-Tour/Hershey-Theater/";
//		List<Quad> url_quads= kb.getResourceTriples(url);
//		for(Quad quad: url_quads){
//			logger.info(" "+quad.print());
//		}
		
	}


}
