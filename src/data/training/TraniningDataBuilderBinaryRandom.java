package data.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import log.parser.Log;
import log.parser.LogParser;

import org.apache.log4j.Logger;

import util.Constants;

public class TraniningDataBuilderBinaryRandom extends TrainingDataBuilder{
	final static Logger logger = Logger.getLogger(TraniningDataBuilderBinaryRandom.class);
	static List<Log> random_uniqueURLs = new ArrayList<Log>();
	
	private static boolean isHomepage(String url){
		return(url.matches("www.eventbrite\\.com.") || (url.matches("upcoming\\.yahoo\\.com.")
				|| url.matches("www.eventful\\.com.")));
		
	}

	public void run(String filename) {
		sessions = LogParser.readEventSessionsFromFile(filename);

		List<Log> eventful_uniqueURLs = new ArrayList<Log>();
		List<Log> upcoming_uniqueURLs = new ArrayList<Log>();
		List<Log> eventbrite_uniqueURLs = new ArrayList<Log>();
		for (String sessid : sessions.keySet()) {
			
			for (Log log : sessions.get(sessid)) {

				if(!isHomepage(log.getUrl()) ){
					//logger.info("log: " + log.getLog());

					if(log.getUrl().contains("eventful") && !log.containedIn(eventful_uniqueURLs)){
						eventful_uniqueURLs.add(log);
					}else if(log.getUrl().contains("yahoo") && !log.containedIn(upcoming_uniqueURLs)){
						upcoming_uniqueURLs.add(log);
					}else if(log.getUrl().contains("eventbrite") && !log.containedIn(eventbrite_uniqueURLs)){
						eventbrite_uniqueURLs.add(log);
					}
					
				}

			}
		}

		float nr_eventful_uniqueURLs = eventful_uniqueURLs.size();
		float nr_upcoming_uniqueURLs = upcoming_uniqueURLs.size();
		float nr_eventbrite_uniqueURLs = eventbrite_uniqueURLs.size();
		
		float totalUniqueUrls=nr_eventful_uniqueURLs  + nr_upcoming_uniqueURLs + nr_eventbrite_uniqueURLs ;
		logger.info("*** totalUniqueUrls: " + totalUniqueUrls);

		Collections.shuffle(eventful_uniqueURLs);
		Collections.shuffle(upcoming_uniqueURLs);
		Collections.shuffle(eventbrite_uniqueURLs);

		int nr_eventfulRandomURL = (int) Math.ceil (nr_eventful_uniqueURLs/totalUniqueUrls*100); 
		int nr_upcomingRandomURL = (int) Math.ceil (nr_upcoming_uniqueURLs/totalUniqueUrls*100); 
		int nr_eventbriteRandomURL = (int) Math.ceil (nr_eventbrite_uniqueURLs/totalUniqueUrls*100); 
		int total_RandomURLs = nr_eventfulRandomURL + nr_upcomingRandomURL + nr_eventbriteRandomURL;
		
		logger.info("*** Total_RandomURLs: " + total_RandomURLs);
		logger.info("\t* nr_eventful_uniqueURLs: " + nr_eventful_uniqueURLs +" , get random: " + nr_eventfulRandomURL);
		logger.info("\t* nr_upcoming_uniqueURLs: " + nr_upcoming_uniqueURLs+" , get random: " + nr_upcomingRandomURL);
		logger.info("\t* nr_eventbrite_uniqueURLs: " + nr_eventbrite_uniqueURLs+" , get random: " + nr_eventbriteRandomURL);

		random_uniqueURLs.addAll(eventful_uniqueURLs.subList(0, nr_eventfulRandomURL));
		random_uniqueURLs.addAll(upcoming_uniqueURLs.subList(0, nr_upcomingRandomURL));
		random_uniqueURLs.addAll(eventbrite_uniqueURLs.subList(0, nr_eventbriteRandomURL));

		logger.info("*** RandomURLs: ");
		int i=1;
		for (Log randomLog : random_uniqueURLs) {
			logger.info(""+i+" " +randomLog.getSessId() + ", " +randomLog.getUrl());
			for(Log urlpair_samesession: getURLPair_SAME_SESSION(randomLog)){
				logger.info("\t "+urlpair_samesession.getLog());
			}

			i++;
		}
		
	}
	
	
	private static List<Log> getURLPair_SAME_SESSION(Log targetlog){
		List<Log> sameSession_logs = new ArrayList<Log>();
		int sessCount=0;
		for (String sessid : sessions.keySet()) {
			if(targetlog.getSessId().equals(sessid)){
				for(Log alog: sessions.get(sessid)){
					if(!alog.getUrl().equals(targetlog.getUrl())){
						sameSession_logs.add(alog);
					}
					
				}
				sessCount++;
			}
		}		
		
		Collections.shuffle(sameSession_logs);
		if(sameSession_logs.size()<1){
			return sameSession_logs.subList(0, 0);
		}else{
			return sameSession_logs.subList(0, 1);
		}
		
	}
	
	private static List<Log> getURLPair_SAME_FREQUENT_ITEMSET_ACROSS_USERS(Log targetlog){
		
		return null;
	}
	
	private static List<Log> getURLPair_SAME_FREQUENT_ITEMSET_SAME_USER(Log targetlog){
		
		return null;
	}

	public static void main(String[] args) {
		String filename = Constants.EVENTLOGS_FOLDER+"list-toolbar-events.pig-20120702_v02";
		new TraniningDataBuilderBinaryRandom().run(filename);


	}

}
