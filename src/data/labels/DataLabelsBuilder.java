package data.labels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;

import triple.parser.Quad;
import util.Constants;


public class DataLabelsBuilder {
	final static Logger logger = Logger.getLogger(DataLabelsBuilder.class);

	static Map<String, List<Log>> sessions;// = new HashMap<String, List<Log>>();
	Map<String, List<Log>> pruned_sessions;// = new HashMap<String, List<Log>>();
	Map<String, List<Resource>> samesession_resources_map; 

	static List<Resource> uniqueResources = new ArrayList<Resource>();
	static KnowledgeBase KNOWLEDGE_BASE;

	public DataLabelsBuilder(){
		sessions = new HashMap<String, List<Log>>();
		pruned_sessions = new HashMap<String, List<Log>>();
		samesession_resources_map = new HashMap<String, List<Resource>>();
		
		KNOWLEDGE_BASE = new KnowledgeBase();
		KNOWLEDGE_BASE.readKNOWLEDGE_BASE();		
	}
	
	public void run(String filename) {
		pruned_sessions.putAll(LogParser.readEventSessionsFromFile(filename));

		for(String sessid : pruned_sessions.keySet()){
			if(pruned_sessions.get(sessid).size() >= 3){//Constants.SESSION_LENGTH_LOWER_LIMIT
				sessions.put(sessid, pruned_sessions.get(sessid));
			}

		}
		logger.info("*** Total Nr. Sessions " + sessions.size());
		
		Resource resource;
		List<Quad> url_quads;
		int nrResource=0, nrLogs=0;
		int nrResourceGrouping=0, nrNotResource=0, nrUrlContained=0;
		
		List<Resource> relatedResources;
		List<Resource> samesession_resources;
		
		//samesession_resources
		for (String sessid : sessions.keySet()) {
			samesession_resources = new ArrayList<Resource>();
			
			for (Log log : sessions.get(sessid)) {
				nrLogs++;
				url_quads= KNOWLEDGE_BASE.getResourceTriples(log.getUrl());
				resource = new Resource(log.getUrl(), url_quads);

				if(resource.isResource()  && !resource.isVideo()){
					logger.info("\t Resource : " + resource.print());
					if(!resource.containedIn(uniqueResources) ){ 
						uniqueResources.add(resource);
						nrResource++;

						samesession_resources.add(resource);
						logger.info(nrResource+"\t 1.ADD samesession_resources: " + resource.print());

//						relatedResources = getRelatedResources(resource, url_quads);
//						for(Resource relatedRes: relatedResources){
//							if(!relatedRes.containedIn(uniqueResources)){ 
//								uniqueResources.add(relatedRes);
//								//logger.info(nrResource+"\t 2. ADD related samesession_resources: " + relatedRes.print());
//								//logger.info(nrResource+"\t\t ADD Related resource: " + relatedRes.print());
//								nrResource++;
//							}
//						}
					}else{
						nrUrlContained++;
					}
				}else{
					if(resource.getType().equals("wam:ResourceGrouping")){
						nrResourceGrouping++;
					}else{
						nrNotResource++;
						for(Quad quad: url_quads){
							if(quad.predicate.equals("wam:hasResource")){
							}
							
						}

					}
				}
			}
			
			logger.info("\t samesession_resources size : " + samesession_resources.size());
			samesession_resources_map.put(sessid, samesession_resources);
		}
		
		logger.info("\t samesession_resources_map size : " + samesession_resources_map.size());
		/*
		 * We also add Resources related to these ones, from the Knowledge Base
		 */
		logger.info("*** Total UniqueResources: " + uniqueResources.size());
		logger.info("\t nrResourceGrouping: " + nrResourceGrouping);
		logger.info("\t nrNotResource: " + nrNotResource);
		logger.info("\t nrUrlContained: " + nrUrlContained);
		logger.info("\t nrLogs: " + nrLogs);
	}
	
	private List<Resource>  getRelatedResources(Resource resource, List<Quad> url_quads){
		List<Resource> related_resources = new ArrayList<Resource>();
		Resource related_resource ;
		String related_url="";
		List<Quad> relatedurl_quads = new ArrayList<Quad>();
		
		for(Quad quad: url_quads){
			if(quad.subject.equals(resource.getURI()) && 
					(quad.predicate.equals("wam:hasVenue") || quad.predicate.equals("wam:hasPerformer") || quad.predicate.equals("wam:hasVideo"))
			   ){
				related_url = quad.object;
				relatedurl_quads= KNOWLEDGE_BASE.getResourceTriples(related_url);
				
				related_resource = new Resource(related_url, relatedurl_quads);
				if(!related_resource.containedIn(related_resources) &&  !related_resource.getURI().isEmpty()){
					related_resources.add(related_resource);
				}
			}
			
		}
		return related_resources; 
	}
	
	private static List<String> generateLines(List<Pair> unique_pairs){
		List<String> lines = new ArrayList<String>();
		String line = "";
		int i=1;

		for(Pair pair: unique_pairs){
			line = "" +
			"INSERT INTO `listjulia`.`pairs-labels` ("+
					"`id` ,"+
					"`method` ,"+
					"`listid` ,"+
					"`url1` ,"+
					"`url2` ,"+
					"`relevance` ,"+
					"`contributor1` ,"+
					"`contributor2` ,"+
					"`contributor3` ) "+
					"VALUES (NULL , 'SVM', '0', '"+pair.getURL1()+"', '"+pair.getURL2()+"', '"+pair.getRelevance()+"', NULL , NULL , NULL); " +
					"\n";
			//logger.info(i + ": " +pair.getURL1()+" , "+pair.getURL2());
			i++;

			lines.add(line);
		}
		return lines;
	}	
	
	protected static void saveDatasetToFile(String filename, List<String> lines)
			throws IOException {
		System.out.println("**SAVE DATA to: " + filename + "**************************");
		Writer out = new OutputStreamWriter(new FileOutputStream(filename));
		for (String line : lines) {
			out.write(line);
		}

		out.close();
	}
	
	private List<Pair> createUniquePairs(List<Resource> unique_resources){
		List<Pair> unique_pairs = new ArrayList<Pair>();
		logger.info("*************** createUniquePairs ***************");
		Pair pair;
		logger.info("unique_resources: " + unique_resources.size());

		/*
		 * Pair up each url with a random half of the other urls 
		 */
		Map<Resource, Integer> uniform_resources = new HashMap<Resource, Integer>();
		//List<Resource> uniform_resources = new ArrayList<Resource>();
		Integer expected_relevance=0;
		int j=0;
		for(int i=0; i<unique_resources.size()-1; i++){
			uniform_resources = getUniformDistribution(unique_resources.get(i), unique_resources);
			logger.info("final uniform_resources: " + uniform_resources.size());
			
			for(Resource res_to_pair: uniform_resources.keySet()){
				expected_relevance = uniform_resources.get(res_to_pair);
				logger.info("expected_relevance: " + expected_relevance);
		
//			for(int j=0; j<uniform_resources.keySet().size(); j++){//j<unique_resources.size()
				
				pair = new Pair(unique_resources.get(i), res_to_pair);
				pair.setRelevance(expected_relevance);
				
				/*
				 * Put selection restrictions here
				 * 1. distribution of urls from all domains
				 * 2. pairs of urls in same session and different sessions
				 * 3. have different entity types
				 */
				if(!pair.containedIn(unique_pairs) ){
					unique_pairs.add(pair);
					logger.info("\t"+ j+ " unique_pair : " + pair.getURL1() + ", " + pair.getURL2() +", rel="+ pair.getRelevance());
				}
				j++;
			}			
		}
//		int nr_pairs_samesession = (int) Math.ceil (pairs_samesession.size()); 
//		int nr_pairs_differentsession = (int) Math.ceil (pairs_differentsessions.size()); 
//		Collections.shuffle(pairs_samesession);
//		Collections.shuffle(pairs_differentsessions);
//		unique_pairs.addAll(pairs_samesession.subList(0, nr_pairs_samesession));
//		unique_pairs.addAll(pairs_differentsessions.subList(0, nr_pairs_differentsession));
		
		int nr_pairs_samesession =0;
		int nr_pairs_differentsession =0;
		
		for(Pair unique_pair: unique_pairs){
			if(pair_SAME_SESSION(unique_pair, sessions)){
				nr_pairs_samesession++;
				logger.info("samesession pair: " + unique_pair.getURL1() + ", " +  unique_pair.getURL2());
			}else {
				nr_pairs_differentsession++;
			}
		}
		logger.info("*** nr_pairs_samesession: " + nr_pairs_samesession);
		logger.info("*** nr_pairs_differentsession: " + nr_pairs_differentsession);
	
		
		logger.info("*** Total unique pairs: " + unique_pairs.size());
		return unique_pairs;
	}
	

	
	/*
	 * Dataset of selected pairs of URLs should fulfill the following:
	 * 1. be from different domains
	 * 2. have different entity types
	 * 3. be from same session as well as different sessions
	 */
	public Map<Resource, Integer> getUniformDistribution(Resource query_resource, List<Resource> uniqueResources) {
		logger.info("query_resource: " + query_resource.print());
		List<Resource> eventful_uniqueURLs = new ArrayList<Resource>();
		List<Resource> upcoming_uniqueURLs = new ArrayList<Resource>();
		List<Resource> eventbrite_uniqueURLs = new ArrayList<Resource>();

		for (Resource res : uniqueResources) {
			if(!res.getURI().equals(query_resource.getURI())){
				if(res.getURI().contains("eventful") && !res.containedIn(eventful_uniqueURLs)){
					eventful_uniqueURLs.add(res);
				}else if(res.getURI().contains("yahoo") && !res.containedIn(upcoming_uniqueURLs)){
					upcoming_uniqueURLs.add(res);
				}else if(res.getURI().contains("eventbrite") && !res.containedIn(eventbrite_uniqueURLs)){
					eventbrite_uniqueURLs.add(res);
				}
			}
		}
		
		float nr_eventful_uniqueURLs = eventful_uniqueURLs.size();
		float nr_upcoming_uniqueURLs = upcoming_uniqueURLs.size();
		float nr_eventbrite_uniqueURLs = eventbrite_uniqueURLs.size();
		
		float totalUniqueUrls=nr_eventful_uniqueURLs  + nr_upcoming_uniqueURLs + nr_eventbrite_uniqueURLs ;
		logger.info("*** TotalUniqueUrls: " + totalUniqueUrls);

		Collections.shuffle(eventful_uniqueURLs);
		Collections.shuffle(upcoming_uniqueURLs);
		Collections.shuffle(eventbrite_uniqueURLs);

		int nr_eventfulRandomURL = (int) Math.ceil (nr_eventful_uniqueURLs/totalUniqueUrls*10); 
		int nr_upcomingRandomURL = (int) Math.ceil (nr_upcoming_uniqueURLs/totalUniqueUrls*10); 
		int nr_eventbriteRandomURL = (int) Math.ceil (nr_eventbrite_uniqueURLs/totalUniqueUrls*10); 
		int total_RandomURLs = nr_eventfulRandomURL + nr_upcomingRandomURL + nr_eventbriteRandomURL;
		
		logger.info("*** Total_RandomURLs: " + total_RandomURLs);
		logger.info("\t* nr_eventful_uniqueURLs: " + nr_eventful_uniqueURLs +" , get random: " + nr_eventfulRandomURL);
		logger.info("\t* nr_upcoming_uniqueURLs: " + nr_upcoming_uniqueURLs+" , get random: " + nr_upcomingRandomURL);
		logger.info("\t* nr_eventbrite_uniqueURLs: " + nr_eventbrite_uniqueURLs+" , get random: " + nr_eventbriteRandomURL);

		//List<Resource> uniform_uniqueURLs = new ArrayList<Resource>();
		Map<Resource, Integer> uniform_uniqueURLs = new HashMap<Resource, Integer>();
		for(int i=0; i<nr_eventfulRandomURL; i++){
			uniform_uniqueURLs.put(eventful_uniqueURLs.get(i), 0);
		}
		for(int i=0; i<nr_upcomingRandomURL; i++){
			uniform_uniqueURLs.put(upcoming_uniqueURLs.get(i), 0);
		}
		for(int i=0; i<nr_eventbriteRandomURL; i++){
			uniform_uniqueURLs.put(eventbrite_uniqueURLs.get(i), 0);
		}

//		uniform_uniqueURLs.putAll(upcoming_uniqueURLs.subList(0, nr_upcomingRandomURL), 0);
//		uniform_uniqueURLs.putAll(eventbrite_uniqueURLs.subList(0, nr_eventbriteRandomURL), 0);

		/*
		 * I also add the related resources , 
		 * to pair them up with the given resource
		 */
		List<Quad >url_quads= KNOWLEDGE_BASE.getResourceTriples(query_resource.getURI());
		List<Resource >relatedResources = getRelatedResources(query_resource, url_quads);
		
		for(Resource relatedRes: relatedResources){
			if(!relatedRes.containedIn(uniform_uniqueURLs)){ 
				uniform_uniqueURLs.put(relatedRes, 1);
				logger.info("\t\t ADD Related resource: " + relatedRes.print());
			}
		}

		/*
		 * I further add the same sessions resources , 
		 * to pair them up with the given resource
		 */
		for(String sessionId: samesession_resources_map.keySet()){
			if(query_resource.containedIn(samesession_resources_map.get(sessionId))){
				for(Resource same_session_res: samesession_resources_map.get(sessionId)){
					if(!same_session_res.getURI().equals(query_resource.getURI())){ //&& !same_session_res.containedIn(uniform_uniqueURLs) && 
						uniform_uniqueURLs.put(same_session_res, 1);
						logger.info("\t\t ADD sameSession resource: " + same_session_res.print());
						logger.info("\t\t sameSession relevance =1 ");
					}

				}
				
			}
		}

		return uniform_uniqueURLs;
	}
	
	
	private boolean pair_SAME_SESSION(Pair pair, Map<String, List<Log>> sessions){
		boolean occursURL1=false, occursURL2=false, occur_samesession=false;
		
		for (String sessid : sessions.keySet()) {
			occursURL1=false; occursURL2=false;
			for(Log alog: sessions.get(sessid)){

				if(alog.getUrl().equals(pair.getURL1())){
					occursURL1=true;
				}

				if(alog.getUrl().equals(pair.getURL2())){
					occursURL2=true;
				}
			}
			
			if(occursURL1 && occursURL2){
				occur_samesession=true;
			}
		}		
		
		
		return occur_samesession;
	}
	
	/*
	 * Exclude files located in the testing_dir
	 */
	public void runDir (String training_dir_str, String testing_dir_str){
		logger.info("training_dir logs: "+ training_dir_str);
		File testing_dir = new File(testing_dir_str);
		List<String> testing_filenames = new ArrayList<String>();
		if(testing_dir.isDirectory()){
			File[] testing_files = testing_dir.listFiles();
			for(File file: testing_files){
				if(file.isFile()){
					testing_filenames.add(file.getName());
				}
			}
		}
		
		File training_dir = new File(training_dir_str);
		List<String> training_filenames = new ArrayList<String>();
		if(training_dir.isDirectory()){
			File[] training_files = training_dir.listFiles();
			for(File file: training_files){
				/*
				 * Exclude Testing files from the Training Dataset
				 */
				
				if(file.isFile() && !testing_filenames.contains(file.getName())){
					training_filenames.add(file.getPath());
				}
			}
		}
		
		logger.info("Total: " + training_filenames.size() + " training files");
		String training_file="";
		int suffix=0;
		for(int i=0; i<training_filenames.size(); i++){//training_filenames.size()
			training_file = training_filenames.get(i);
			suffix = Integer.parseInt(training_file.substring(training_file.length()-2, training_file.length()));
			if(suffix==2 || suffix==3 || suffix==4 || suffix==6  || suffix==12 || suffix==23 || suffix==24 ){
				logger.info("File " + training_file+ " (" + i +" out of "+ training_filenames.size() +")");
				/*
				 * Unique resources are generated here
				 */
				run(training_file);
			}
		}
		
		/*
		 * Unique pairs of resources are generated here
		 */
		List<Pair> unique_pairs = createUniquePairs(uniqueResources);
		
		List<String> text_lines = generateLines(unique_pairs);
		Date now = new Date();
	    String saveToFile = Constants.LABELDATA_FOLDER+"labelingset"+now.hashCode();
		try {
			saveDatasetToFile(saveToFile, text_lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public static void main(String[] args) {
		DataLabelsBuilder builder = new DataLabelsBuilder();
		builder.runDir(Constants.EVENTLOGS_FOLDER, Constants.TESTINGDATA_LOGS_FOLDER);
	}

}
