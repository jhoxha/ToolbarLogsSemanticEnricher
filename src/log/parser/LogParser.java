package log.parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import triple.analyzer.MetadataParser;
import triple.analyzer.MetadataParser.DomainFile;
import triple.parser.Triple;
import util.Constants;
import util.Utils;



public class LogParser {
	final static Logger logger = Logger.getLogger(LogParser.class);
	
	/*
	 * SessionID = guid@d_date = guidSessionIdentifierdate
	 * SessionIdentifier="@d_"
	 */
	final static String SessionIdentifier="@d_";
	
	private Map<String, ArrayList<Log>> sessions = new HashMap<String,  ArrayList<Log>>();
	private Map<String, List<Log>> event_sessions = new HashMap<String,  List<Log>>();
	private Map<String, List<Log>> pruned_event_sessions = new HashMap<String,  List<Log>>();
	
	private String date="";
	
	public LogParser(){
		sessions = new HashMap<String,  ArrayList<Log>>();
		event_sessions = new HashMap<String,  List<Log>>();
		pruned_event_sessions = new HashMap<String,  List<Log>>();
		date="";
	}
	
	public void readFolderAndStoreEventLogs(String folder) {
		File[] files = new File(folder).listFiles();
		
		File last_fileEntry=new File("");
		int i=1;
		for(File fileEntry: files){
    		 try {
    			 if(fileEntry.isFile()){
    				 logger.info("File " + fileEntry+ " (" + i +" out of "+ files.length+")");
        			 readAllLogs(fileEntry.getPath());
    			 }
    		} catch (IOException e) {
    			e.printStackTrace();
    		}             
    		 last_fileEntry = fileEntry;
    		 i++;
		}
		
		try {
			saveEventLogs(last_fileEntry.getParentFile().getName(), pruned_event_sessions);
		} catch (IOException e) {
			 e.printStackTrace();
		}
		
	}
	
	public void readFileAndStoreEventLogs(String filename) {
		File file = new File(filename);
		logger.info("**Parsing file: "+file);
		try {
			 if(file.isFile()){
    			 readAllLogs(file.getPath());
    			 saveEventLogs(file.getName(), pruned_event_sessions);
			 }
		} catch (IOException e) {
			e.printStackTrace();
		}             
	}	

	private void readAllLogs(String filename) throws IOException{
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		
		logger.info("Reading file: "+filename);
		String strLine="", guid="", prev_guid="", sessId="";
		ArrayList<Log> logs = new  ArrayList<Log>();
		
		while ((strLine = br.readLine()) != null )   {
			  if(strLine.contains("{")){
				  guid=strLine.substring(0,strLine.indexOf("\t")).trim();
				  
				  if(!guid.equals(prev_guid) && !prev_guid.isEmpty()&& !sessions.containsKey(prev_guid)){
					  /*
					   * SESSION ID DEFINED HERE
					   */
					  sessId=prev_guid+SessionIdentifier+date;
					  sessions.put(sessId, logs);
				  }
				  prev_guid = guid;
				  logs = new  ArrayList<Log>();
			  }
			  
			  logs = readLogsOfSession(strLine, guid);
		  }
		input1.close();
		br.close();
		

		/*
		 * Filter only Events-related logs here
		 */
		extractEventLogs();
		
		/*
		 * Prune those logs missing valid medatada
		 */
		pruneValidMetadataLogs();		
	}	
	
	private void pruneValidMetadataLogs(){
		List<String> urls = new ArrayList<String>();
		List<String> urls_not_checked = new ArrayList<String>();
		
		for(String sessid: event_sessions.keySet()){
			for(Log log: event_sessions.get(sessid)){
				if(!urls.contains(log.getUrl()) ){
					urls.add(log.getUrl());
				}
			}
		}
		
		List<String> valid_urls = new ArrayList<String>();
		
		List<Triple> url_triples = new ArrayList<Triple>();
		List<DomainFile> domain_files = new ArrayList<DomainFile>();
		domain_files.add(DomainFile.EVENTFUL1);
		domain_files.add(DomainFile.EVENTFUL2);
		domain_files.add(DomainFile.EVENTBRITE);
		domain_files.add(DomainFile.UPCOMING);
		
		urls_not_checked.addAll(urls);
		
		for (DomainFile domain: domain_files){
			logger.info("\t **** Try with domain " + domain);
			if(urls_not_checked.size()!=0 ){
				MetadataParser parser = new MetadataParser(domain);
				for(String url: urls){
					if( parser.URLFoundInFile(url) && urls_not_checked.contains(url)){
						url_triples = parser.getTypedTriplesOfURL(url);
						if(url_triples.size() >0){
							valid_urls.add(url);
							//logger.info("\t "+url_triples.size()+ " found for url: " + url);
						}
						urls_not_checked.remove(url);	
					} 

					//logger.info(" Remaining urls: " + urls_not_checked.size());
				}
			}
		}
		
		for(String url_notchecked: urls_not_checked){
			logger.info(" NOT found in triples files this url  " + url_notchecked);

		}
		
		Map<String, List<Log>> valid_event_sessions = new HashMap<String,  List<Log>>();
		int nr_removed_urls=0;
		for(String sessid: event_sessions.keySet()){
			List<Log> valid_logs = new ArrayList<Log>();
			for(Log log: event_sessions.get(sessid)){
				if(valid_urls.contains(log.getUrl())){
					valid_logs.add(log);
				} else {
					nr_removed_urls++;
				}
			}
			if(valid_logs.size() > Constants.SESSION_LENGTH_LOWER_LIMIT){
				valid_event_sessions.put(sessid, valid_logs);
			}
		}		

		List<Log> pruned_event_logs = new  ArrayList<Log>();
		String prevUrl="", currentUrl="";

		for(String sessid: valid_event_sessions.keySet()){
			pruned_event_logs = new ArrayList<Log>();
			
			/*
			 * The following actions are taken to Collapse Subsequent Identical Requests in a session
			 */
			for(Log log: valid_event_sessions.get(sessid)){
				currentUrl = log.getUrl().trim();
				String currentUrl_without_www=currentUrl.replace("www.", "").trim();
				String prevUrl_without_www=prevUrl.replace("www.", "").trim();

				if(!currentUrl.equals(prevUrl) && !currentUrl_without_www.equals(prevUrl_without_www) ){
					logger.info("add log: " +log.getUrl());
					pruned_event_logs.add(log);
				} 
				prevUrl = currentUrl;
				
			}
			
			/*
			 * Put an upper limit on the Session Length
			 * to filter out spamming behavior 
			 */
			if (pruned_event_logs.size() >= Constants.SESSION_LENGTH_LOWER_LIMIT && pruned_event_logs.size() <= Constants.SESSION_LENGTH_UPPER_LIMIT  ){
				Collections.sort(pruned_event_logs, new CustomComparator());
				pruned_event_sessions.put(sessid, pruned_event_logs);
			}
			
		}
		
		for(String sessid: pruned_event_sessions.keySet()){
			logger.info("PRUNED sessid: " + sessid);
			for(Log log: pruned_event_sessions.get(sessid)){
				logger.info("\t " + log.getUrl());
			}
		}
		
		int nr_removed_sessions=event_sessions.size()-pruned_event_sessions.size();
		logger.info("Nr removed sessions: " + nr_removed_sessions + ", nr_removed_urls="+nr_removed_urls);
	}
	
	private ArrayList<Log> readLogsOfSession(String strLine, String guid){
		Log log = null;
		String[] sessionTuples; 
		String[] logFields;
		ArrayList<Log> logs = new  ArrayList<Log>();
		
		sessionTuples=strLine.split("\\(,");
		for(int j=1; j < sessionTuples.length; j++){
			logFields = sessionTuples[j].split(",");
			if(logFields.length==8){
				log = new Log("", guid, logFields[1], logFields[2], logFields[3], logFields[7].replace(")", "").replace("}", ""));
				logs.add(log);
				date=logFields[1];
			}
		}

		return logs;
	}
	
	private void extractEventLogs(){
		List<Log> event_logs = new  ArrayList<Log>();
		List<Log> temp_logs = new  ArrayList<Log>();
		
		Log log = null;
		for(String sessId: sessions.keySet()){
			temp_logs = sessions.get(sessId);
			Collections.sort(temp_logs, new CustomComparator());
			
			event_logs = new ArrayList<Log>();
			MetadataParser aparser = new MetadataParser();

			for(int c=0; c < sessions.get(sessId).size(); c++){
				log = sessions.get(sessId).get(c);
				log.setSessId(sessId);
				
				if(Utils.isInDomainOfInterest(log.getUrl()) ){
					/*
					 * PRUNE BY: valid host, URL not Homepage, valid URL pattern
					 */
					if(Utils.hasValidHost(log.getUrl(), log.getHost()) && !Utils.isHomepage(log.getUrl()) && aparser.isValidURL(log.getUrl(), log.getHost()) ){
						if(!log.getSessId().isEmpty() ){
							event_logs.add(log);
						}
					}
				}
			}

			Collections.sort(event_logs, new CustomComparator());
			event_sessions.put(sessId, event_logs);
	
		}
	}
	
	public static ArrayList<Log> getEventLogsFromFile(String filename) throws IOException{
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		
		logger.info("Reading file: "+filename);
		String strLine="";
		ArrayList<Log> logs = new  ArrayList<Log>();
		Log log = null;
		String[] fields;
		
		while ((strLine = br.readLine()) != null)   {
			fields = strLine.split(",\t");
			if(fields.length==6){
				log = new Log(fields[0],fields[1],fields[2],fields[3],fields[4],fields[5]);
				logs.add(log);
			}else if(fields.length==5){
				log = new Log(fields[0],fields[1],fields[2],fields[3],fields[4],"");
				logs.add(log);
			}else{
				logger.info("Warning: getEventLogsOfFile corrupted log");
				
			}

		}
		
		return logs;
	}
	public static Map<String, List<Log>> readEventSessionsFromDir(String dirname){
		logger.info("readEventSessionsFromDir: "+ dirname);
		File sessions_dir = new File(dirname);
		List<String> filenames = new ArrayList<String>();
		if(sessions_dir.isDirectory()){
			File[] files = sessions_dir.listFiles();
			for(File file: files){
				if(file.isFile()){
					filenames.add(file.getAbsolutePath());
				}
			}
		}
		
		Map<String, List<Log>> sessions = new HashMap<String,  List<Log>>();
		for(String filename: filenames){
			sessions.putAll(readEventSessionsFromFile(filename));
		}

		return sessions;
	}
	
	public static Map<String, List<Log>> readEventSessionsFromFile(String filename){
		Map<String, List<Log>> event_sessions = new HashMap<String,  List<Log>>();
		
		String sessId="";
		String prevSessId="";
		List<Log> logs = new  ArrayList<Log>();
		List<Log> sesslogs = new  ArrayList<Log>();
		int nrLogs=0;
		try {
			logs= getEventLogsFromFile(filename);
			
			for(Log log: logs){
				sessId= log.getSessId();
				if(!sessId.equals(prevSessId) && !prevSessId.isEmpty()){
					if(!event_sessions.keySet().contains(prevSessId)){
						event_sessions.put(prevSessId, sesslogs);
						nrLogs += sesslogs.size();
						sesslogs = new  ArrayList<Log>();
					}
				}
				sesslogs.add(log);
				prevSessId=sessId;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		logger.info("File " + filename +", nr sessions: " + event_sessions.size() + ", nrlogs: " + nrLogs);
		return event_sessions;
	}
	
	private void saveEventLogs(String filename, Map<String, List<Log>> eventssessions) throws IOException{
		filename = filename.replace("list", "pruned");
		filename = filename.replace("pig", "");
		logger.info("**SAVE : "+filename+"********************************");
		int totalRecords=0;
	    Writer out = new OutputStreamWriter(new FileOutputStream(Constants.EVENTLOGS_FOLDER+filename));
		for(String sessId: eventssessions.keySet()){
			for(Log eventlog: eventssessions.get(sessId)){
			    out.write(eventlog.getLog()+"\n");
			}
			totalRecords += eventssessions.get(sessId).size();
		}
		logger.info("***************Total: "+totalRecords+"/ ("+eventssessions.size()+" sessions)********************************");
	    
	    out.close();
	}
	
	private class CustomComparator implements Comparator<Log> {
		int c=0;
		int t1=0, t2=0;
	    @Override
	    public int compare(Log o1, Log o2) {
	    	t1 = Integer.parseInt(o1.getTime());
	    	t2=Integer.parseInt(o2.getTime());
	    	c = t1-t2;
	        return c;
	    }
	}
	
	public static void main(String[] args) {
		LogParser parser = new LogParser();
		String date ="20120702";
		//date ="20120806";
		parser.readFolderAndStoreEventLogs(Constants.LOGS_FOLDER+"list-toolbar-events.pig-"+date);
	}

}

