package log.analyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;

import org.apache.log4j.Logger;

import util.Constants;

public class LogAnalyzer {
	final static Logger logger = Logger.getLogger(LogAnalyzer.class);

	Map<String, List<Log>> sessions = new HashMap<String,  List<Log>>();
	Map<String, List<Log>> cross_websites_sessions = new HashMap<String,  List<Log>>();

	static List<Log> uniqueURLs = new ArrayList<Log>();
	private static int nrSessions=0, nrLogs=0, minSessionLength=3,
			maxSessionLength=0, modeSessionLength=0;
	float avgSessionLength=0;

	public LogAnalyzer()  {
	}
	
	public void run(String location_name){
		File location = new File(location_name);
		if(location.isDirectory()){
			sessions = LogParser.readEventSessionsFromDir(location_name);
		}else if(location.isFile()){
			sessions = LogParser.readEventSessionsFromFile(location_name);
		}
		
	}
	public void printUserStats(){
		List<String> user_ids = new ArrayList<String>();
		for(String sessId: sessions.keySet()){
			for (Log log : sessions.get(sessId)) {
				if (!user_ids.contains(log.getGuid()) ) {
					user_ids.add(log.getGuid());
				}
			}
		}
		logger.info("total unique user_ids " + user_ids.size());
	}
	
	public void printURLAccessFrequencies(){
		Map<String, Integer> urlAccessFrequencies =  new HashMap<String, Integer>();
		List<String> urls = new ArrayList<String>();
		for(String sessId: sessions.keySet()){
			for (Log log : sessions.get(sessId)) {
				if (!urls.contains(log.getUrl()) ) {
					urls.add(log.getUrl());
				}
			}
		}
		
		int freq=1;
		for(String sessId: sessions.keySet()){
			for (Log log : sessions.get(sessId)) {
				for(String unique_url: urls){
					freq=1;
					if(log.getUrl().equals(unique_url)){
						if(urlAccessFrequencies.containsKey(unique_url)){
							freq=urlAccessFrequencies.get(unique_url);
							urlAccessFrequencies.remove(unique_url);
							freq++;
						}
						urlAccessFrequencies.put(unique_url, freq);
					}
				}
			}
		}
		int nr_url=1;
		logger.info("total unique urls " + urls.size());
		Map<Integer, Integer> frequency_URLNr =  new HashMap<Integer, Integer>();
		for(String url: urlAccessFrequencies.keySet()){
			nr_url=1;
			freq = urlAccessFrequencies.get(url);
			logger.info("\t url " + url + ", freq:"+freq);
			if(frequency_URLNr.containsKey(freq)){
				nr_url = frequency_URLNr.get(freq);
				nr_url++;
			}
			frequency_URLNr.put(freq, nr_url);
		}
		
		for(Integer url_freq: frequency_URLNr.keySet()){
			logger.info("**There are "+frequency_URLNr.get(url_freq)+" urls with access freq " + url_freq);
		}
	}

	public void printSessionsStats(){
		Map<Integer, Float> sessionLengthFrequencies =  new HashMap<Integer, Float>();
		int sessLength=0;
		float length_freq=0;
		boolean fromEventful=false, fromEventbrite=false, fromUpcoming=false;
		for(String sessId: sessions.keySet()){
			fromEventful=false; fromEventbrite=false; fromUpcoming=false;
			for (Log log : sessions.get(sessId)) {
				if (!log.containedIn(uniqueURLs) ) {
					uniqueURLs.add(log);
				}
				
				if(log.getHost().contains("eventful") ){
					fromEventful = true;
				}else if(log.getHost().contains("eventbrite")){
					fromEventbrite = true;
				}else if(log.getHost().contains("upcoming.yahoo")){
					fromUpcoming = true;
				}
			}
			
			if((fromEventful && fromEventbrite) || (fromEventful && fromUpcoming) || (  fromEventbrite && fromUpcoming)){
				cross_websites_sessions.put(sessId,sessions.get(sessId));
			}
			
			sessLength = sessions.get(sessId).size();
			nrLogs += sessLength;
			
			if(sessLength<minSessionLength){
				minSessionLength = sessLength;
			}

			if(sessLength>maxSessionLength){
				maxSessionLength = sessLength;
			}
			
			avgSessionLength +=sessLength;
			if(sessionLengthFrequencies.keySet().contains(sessLength)){
				length_freq = sessionLengthFrequencies.get(sessLength);
				sessionLengthFrequencies.remove(sessLength);
				length_freq++;
				sessionLengthFrequencies.put(sessLength, length_freq);
			}else{
				sessionLengthFrequencies.put(sessLength, (float)1);
			}
			
		}
		
		float maxFreqSessionLength=0;
		for(Integer sessLn: sessionLengthFrequencies.keySet() ){
			logger.info("sessLn: " + sessLn+": " + sessionLengthFrequencies.get(sessLn).intValue());	
			if(sessionLengthFrequencies.get(sessLn)>maxFreqSessionLength){
				maxFreqSessionLength = sessionLengthFrequencies.get(sessLn);
				
				modeSessionLength = sessLn;
			}			
		}
		
		nrSessions = sessions.size();
		avgSessionLength = avgSessionLength / nrSessions;
		

		float minSessLnFreq=(sessionLengthFrequencies.get(minSessionLength)/nrSessions)*100;
		
		logger.info("***Total nrSessions: " +nrSessions);	
		logger.info("***Total nrLogs: " + nrLogs);	
		logger.info("***Average session length: " +avgSessionLength + " in " + (sessionLengthFrequencies.get((int)Math.ceil(avgSessionLength))/nrSessions)*100 +"% of sessions");	
		logger.info("***Mode session length: " +modeSessionLength + " in "+(maxFreqSessionLength/nrSessions)*100+ "% of sessions");	
		logger.info("***Min session length: " +minSessionLength + " in " +minSessLnFreq+"% of sessions");	
		logger.info("***Session length: " +3 + " in " +(sessionLengthFrequencies.get(3)/nrSessions)*100+"% of sessions");	
		logger.info("***Max session length: " +maxSessionLength + " in " +(sessionLengthFrequencies.get(maxSessionLength)/nrSessions)*100+"% of sessions");	
		
		float nr_cross_websites_sessions=cross_websites_sessions.size();
		logger.info("***Nr. cross websites Sessions: " +nr_cross_websites_sessions + " in " +(nr_cross_websites_sessions/nrSessions)*100+"% of sessions");		

		logger.info("***Total uniqueURLs " + uniqueURLs.size());
		for(Log log: uniqueURLs){
			logger.info("\t: " + log.getLog());	
		}
	}

	public static void main(String[] args) {
		LogAnalyzer analyser = new LogAnalyzer();
		analyser.run(Constants.EVENTLOGS_FOLDER);
		analyser.printUserStats();
		
//		String filename ="list-toolbar-events.pig-20120806_v03";
//		analyser.run(Constants.EVENTLOGS_FOLDER+filename);
//		analyser.printSessionsStats();


	}
	

}