package data.training;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;

import org.apache.log4j.Logger;

import svm.features.FeatureVectorBuilder;
import triple.parser.Quad;
import util.Constants;

public class CopyOfTrainingDataBuilderBinary {//extends TrainingDataBuilder
	final static Logger logger = Logger.getLogger(CopyOfTrainingDataBuilderBinary.class);

	static Map<String, List<Log>> sessions;// = new HashMap<String, List<Log>>();
	Map<String, List<Log>> pruned_sessions;// = new HashMap<String, List<Log>>();

	static List<Resource> uniqueResources = new ArrayList<Resource>();
	static KnowledgeBase KNOWLEDGE_BASE;
	static float threshold_relevance=0;

	
	public CopyOfTrainingDataBuilderBinary(){
		sessions = new HashMap<String, List<Log>>();
		pruned_sessions = new HashMap<String, List<Log>>();
		
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
		for (String sessid : sessions.keySet()) {
			for (Log log : sessions.get(sessid)) {
				nrLogs++;
				url_quads= KNOWLEDGE_BASE.getResourceTriples(log.getUrl());
				//logger.info("Url: " + log.getUrl() +" with url_quads size:" + url_quads.size());
				resource = new Resource(log.getUrl(), url_quads);
				//logger.info("\t Resource : " + resource.print());

				if(resource.isResource()){
					if(!resource.containedIn(uniqueResources) ){
							uniqueResources.add(resource);
						logger.info(nrResource+"\t ADD Resource: " + resource.print());
						nrResource++;
					}else{
						//logger.info("\t ALREADY contained");
						nrUrlContained++;
					}
				}else{
					if(resource.getType().equals("wam:ResourceGrouping")){
						//logger.info("\t type Resource Grouping " + resource.getType());
						nrResourceGrouping++;
					}else{
						nrNotResource++;
						logger.info("\t NOT resource url=" + log.getUrl());
						for(Quad quad: url_quads){
							if(quad.predicate.equals("wam:hasResource")){
								logger.info("\t "+quad.print());
							}
							
						}

					}
				}
			}
		}
		logger.info("*** Total UniqueResources: " + uniqueResources.size());
		logger.info("\t nrResourceGrouping: " + nrResourceGrouping);
		logger.info("\t nrNotResource: " + nrNotResource);
		logger.info("\t nrUrlContained: " + nrUrlContained);
		logger.info("\t nrLogs: " + nrLogs);
	}
	
	private List<Pair> createUniquePairs(List<Resource> unique_resources){
		List<Pair> unique_pairs = new ArrayList<Pair>();
		logger.info("*************** createUniquePairs ***************");
		Pair pair;
		for(int i=0; i<unique_resources.size()-1; i++){
			//logger.info(" i : " + i + " out of " + unique_resources.size());
			
			for(int j=i+1; j<unique_resources.size(); j++){
				//logger.info("\t j : " + j);
				pair = new Pair(unique_resources.get(i), unique_resources.get(j));
				if(!pair.containedIn(unique_pairs)){
					unique_pairs.add(pair);
				}
			}			
		}

		logger.info("*** Total unique pairs: " + unique_pairs.size());
		return unique_pairs;
	}
	
	private Map<Pair, String> createLabeledPairs(List<Pair> unique_pairs){
		Map<Pair, String> labeled_pairs = new HashMap<Pair, String>();
		String relevance_value_str="-1";
		float relevance_value=0;
		int nr_pairs_relevant=0;;
		Pair pair; 

		Map<Pair, Float> pair_ERR_relevance_values = compute_ERR(unique_pairs);
		int nrPairs_sameSession=0;
		
		for(int k=0; k<unique_pairs.size(); k++){ // unique_pairs.size()
			relevance_value_str="-1";
			pair =unique_pairs.get(k);
			
			if(count_SAME_SESSION(pair)>0 ){ 
				nrPairs_sameSession++;
			}
			
			/*
			 * Compute Relevance Value - ERR Metrich Scheme
			 * Define the scale and Threshold here: e.g. (2.5 / 5.0)
			 */
			//threshold_relevance=2.5
			relevance_value = pair_ERR_relevance_values.get(pair);
			if(relevance_value >= threshold_relevance){
				relevance_value_str = "+1";
				nr_pairs_relevant++;
			}
			
			labeled_pairs.put(pair, relevance_value_str);
			//logger.info(" pair: " + pairs.get(k).URL1 + ", "+ pairs.get(k).URL2);
		}

		logger.info("*** Total SAME_SESSION pairs: " + nrPairs_sameSession);
		logger.info("*** NR_pairs_relevant: " + nr_pairs_relevant);	
		
		return labeled_pairs;
	}
	
	private static List<String> generateLines(Map<Pair, String> labeled_pairs){
		/*
		 * <item1>URL1 \t <wam:type>Type \t <wam:attr1>Attr1 \t <item2>URL2 \t <wam:type>Type … <label>REL_VL(0/1) 
		 */
		List<String> lines = new ArrayList<String>();
		String line = "";
		
		for(Pair pair: labeled_pairs.keySet()){
			line = "<item1>"+pair.getURL1() + "\t<wam:type>Type\t<item2>"+pair.getURL2() + "\t<wam:type>Type\t<label>"+labeled_pairs.get(pair)+"\n";
			lines.add(line);
		}
		return lines;
	}
	
	private static int count_SAME_SESSION(Pair pair){
		boolean occursURL1=false, occursURL2=false;
		int count_sameSess=0;
		for (String sessid : sessions.keySet()) {
			occursURL1=false;
			occursURL2=false;
					
			for(Log alog: sessions.get(sessid)){
				if(alog.getUrl().equals(pair.getURL1())){
					occursURL1=true;
				}
				if(alog.getUrl().equals(pair.getURL2())){
					occursURL2=true;
				}
			}
			
			if(occursURL1 && occursURL2){
				count_sameSess++;
				// logger.info("same session " + sessid + " : " + pair.URL1 + ", " + pair.URL2);
				// logger.info("count_sameSess =" + count_sameSess);
			}
		}		
		
		if(count_sameSess>0){
			// logger.info("\n mypair " + pair.URL1 + ", " + pair.URL2 + " in " +count_sameSess+ " sessions together");
		}
		return count_sameSess;
	}
	
	private static Map<Pair, Float> compute_ERR(List<Pair> unique_pairs){
		float ERR_value =0;
		float g_i_j=0, g_max=0, rel_i_j=0, 
				F_frec_i_j = 0, f_i_j=0, f_i_k=0, f_j_k=0;
		int nr_co_items=0;
		int n=5; 
		/*
		 * f_i_j : number of sessions i&j appear together 
		 * f_i_k : number of sessions i&k appear together, for k in the same sessions with i&j
		 * F_frec_i_j: number of items k, accessed less frequenty together with i when compared to i&j frequency. i.e. f_i_j >= f_i_k
		 */
		boolean occursURL1=false, occursURL2=false;
		//Map<String, Integer> i_k_frequencies = new HashMap<String, Integer>();
		List<Resource> items_k_set = new ArrayList<Resource>();
		List<Log> items_k_set_withinSession = new ArrayList<Log>();
		Map<Pair, Float> pair_g_values = new HashMap<Pair, Float>();
		
		for(Pair pair: unique_pairs){
			f_i_j = 0;
			items_k_set = new ArrayList<Resource>();
			//logger.info("Pair " +pair.toString() );
			nr_co_items =0;
			
			for (String sessid : sessions.keySet()) {
				occursURL1=false;
				occursURL2=false;
				items_k_set_withinSession = new ArrayList<Log>();
				//i_k_frequencies = new HashMap<String, Integer>();
				
				for(Log alog: sessions.get(sessid)){
					if(alog.getUrl().equals(pair.getURL1())){
						occursURL1=true;
					}else if(alog.getUrl().equals(pair.getURL2())){
						occursURL2=true;
					} 
				}
				
				if(occursURL1 && occursURL2){
					f_i_j++;
					for(Log k_log: sessions.get(sessid)){
						if(!k_log.containedIn(items_k_set_withinSession)){
							items_k_set_withinSession.add(k_log);
						}
					}
					
					//nr_co_items += items_k_set_withinSession.size();
					
					//nr_co_items+=2;
				}
				
				logger.info("\t items_k_set_withinSession: " + items_k_set_withinSession.size());

				logger.info("\t nr_co_items: " + nr_co_items);
				Resource item_k_resource;
				List<Quad> item_k_quads;
				for(Log k_logs: items_k_set_withinSession){
					item_k_quads= KNOWLEDGE_BASE.getResourceTriples(k_logs.getUrl());
					item_k_resource = new Resource(k_logs.getUrl(), item_k_quads);
					logger.info("\t item_k_resource =" + item_k_resource.print());

					if(!k_logs.getUrl().equals(pair.getURL1()) && !k_logs.getUrl().equals(pair.getURL2())){
						if(item_k_resource.isResource()){
							items_k_set.add(item_k_resource);
							nr_co_items ++;
						}
						logger.info("\t\t add to items_k_set: " + item_k_resource.print());
						logger.info("\t\t nr_co_items: " + nr_co_items);
					}
//					if(items_k_set_withinSession.contains(pair.URL2)){
//						items_k_set_withinSession.remove(items_k_set_withinSession.indexOf(pair.URL2));
//					}

				}
			}
			
			//logger.info("\t f_i_j=" + f_i_j);

			/*
			 * Count f_i_k
			 * count_less_freq_k =  Nr. sessions were i,k co-ocurr less frequently 
			 */
			Pair pair_i_k = null;
			Pair pair_j_k = null;
			float count_less_freq_k=0;
			count_less_freq_k += f_i_j;
			
			for(Resource item_k : items_k_set){
				pair_i_k = new Pair (pair.getResource1(), item_k);
				f_i_k = count_SAME_SESSION (pair_i_k)-1;
				logger.info("\t f_i_k =" + f_i_k);
				
				count_less_freq_k += f_i_k;
				
//				if(f_i_k <= f_i_j && f_i_k>=1){
//					count_less_freq_k++;
//				}
				
				pair_j_k = new Pair (pair.getResource2(), item_k);
				f_j_k = count_SAME_SESSION (pair_j_k)-1;
				count_less_freq_k += f_j_k;
//				if(f_j_k <= f_i_j && f_j_k>=1){
//					count_less_freq_k++;
//				}
				logger.info("\t f_j_k =" + f_j_k);

			}

			nr_co_items = sessions.size();

			logger.info("\t Nr. sessions were i,k co-ocurr less frequently - count_less_freq_k =" + count_less_freq_k);
			logger.info("\t size items_k_set =" + items_k_set.size());
			logger.info("\t nr_co_items =" + nr_co_items);

			/*
			 * rel_i_j = n * F_frec_i_j
			 */ 
			if(nr_co_items==0){
				F_frec_i_j=0;	
			}else{
				F_frec_i_j = count_less_freq_k / nr_co_items;
			}
			logger.info("F_frec_i_j = " +F_frec_i_j);
			rel_i_j = n * F_frec_i_j;
			
			
			/*
			 * g_i_j = max(0, rel_i_j)
			 */
			g_i_j = rel_i_j;
			logger.info("g_i_j="+ g_i_j + " for pair " + pair.toString());
			if(g_i_j > g_max){
				g_max = g_i_j;
			}
			pair_g_values.put(pair, g_i_j);

			if(rel_i_j>0){
				logger.info("rel_i_j="+ rel_i_j + " for pair " + pair.toString());
			}
		}
		
		/*
		 * SET n/2 * g_max as threshold
		 */
		threshold_relevance = g_max/2;
		/*
		 * g_max = max (g_i_j) for all pairs
		 * 
		 */
		logger.info("g_max="+ g_max);
		logger.info("threshold_relevance="+ threshold_relevance);
		
		
		/*
		 * ERR_value = (2^g_i_j - 1) / 2^g_max 
		 */
		for(Pair apair: pair_g_values.keySet()){
			
			ERR_value = (float) (Math.pow(2, pair_g_values.get(apair)) / (Math.pow(2, g_max)));
			//logger.info("ERR_value="+ ERR_value);
			//logger.info("ERR_value=" + ERR_value + " pair " + apair.toString());
			//logger.info("\t g_i_j=" + pair_g_values.get(apair)+ " , g_max= " + g_max);
		}

		double minProb = Math.pow(2, 0) / (Math.pow(2, g_max));	
		double medProb = Math.pow(2, threshold_relevance) / (Math.pow(2, g_max));	
		double maxProb = Math.pow(2, g_max) / (Math.pow(2, g_max));	
		logger.info("minProb ="+minProb);
		logger.info("medProb ="+ medProb);
		logger.info("maxProb ="+ maxProb);
		
		return pair_g_values;
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
		for(int i=1; i<2 ; i++){//training_filenames.size()
			training_file = training_filenames.get(i);
			logger.info("File " + training_file+ " (" + i +" out of "+ training_filenames.size() +")");
			/*
			 * Unique resources are generated here
			 */
			run(training_file);
		}
		
		/*
		 * Unique pairs of resources are generated here
		 */
		List<Pair> unique_pairs = createUniquePairs(uniqueResources);
		
		/*
		 * Labeled pairs of resources are generated here
		 */
		Map<Pair, String> labeled_pairs =createLabeledPairs(unique_pairs);
		
		/*
		 * Features and respective values are created here
		 * Two files are produced:
		 * 1- file with the full string pairs of item (needed for evaluation later)
		 * 2- file containing same pairs as the first one, but in the SVM format
		 */
		List<String> text_lines = generateLines(labeled_pairs);
		boolean COLLAB_SIM_CONDITIONAL = false;
		boolean COLLAB_SAME_USER=false;
		boolean COLLAB_SIMILAR_USERS=false;
		boolean COLLAB_SAME_USERCLUSTER=false;
		
		boolean CONTENT_SAME_TYPE=true;
		boolean CONTENT_SHARE_RELATION=true;
		boolean CONTENT_SYNTACTIC_SIMILARITY=false;
		boolean CONTENT_SEMANTIC_SIMILARITY=false;

		FeatureVectorBuilder feature_vector_builder = 
				new FeatureVectorBuilder(sessions, labeled_pairs, KNOWLEDGE_BASE, 
				COLLAB_SAME_USER, COLLAB_SIMILAR_USERS, COLLAB_SIM_CONDITIONAL,
				CONTENT_SAME_TYPE, CONTENT_SHARE_RELATION,
				CONTENT_SYNTACTIC_SIMILARITY, CONTENT_SEMANTIC_SIMILARITY);
		List<String> feature_vectors = feature_vector_builder.generateFeatureVectors();
		
		//divide in 3 different sets
		Date now = new Date();
	    String saveToFile = Constants.TRAININGDATA_FOLDER+"trainingset"+now.hashCode();
		String saveToSVMFile = saveToFile+"_svm";
		try {
			saveDatasetToFile(saveToFile, text_lines);
			saveDatasetToFile(saveToSVMFile, feature_vectors);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	protected static void saveDatasetToFile(String filename, List<String> lines)
			throws IOException {
		
		System.out.println("**SAVE DATA to: " + filename
				+ "********************************");
		Writer out = new OutputStreamWriter(new FileOutputStream(filename));
		for (String line : lines) {
			out.write(line);
		}

		out.close();
	}

	private void readLabeledDataset() throws IOException{
		String filename = Constants.LABELDATA_FOLDER+"labeled_set_1.csv";
		
		DataInputStream input1 = new DataInputStream(new FileInputStream(filename));
		BufferedReader br = new BufferedReader(new InputStreamReader(input1));
		
		logger.info("Reading file: "+filename);
		String strLine="";
		List<Pair> pairs = new  ArrayList<Pair>();
		String[] fields;
		List<Quad> url_quads ;
		String url1="", url2="";
		Resource res1, res2;
		
		int i=1;
		
		while ((strLine = br.readLine()) != null)   {
			fields = strLine.split(";");
			if(fields.length==3){
				url1 = fields[0];
				url_quads = KNOWLEDGE_BASE.getResourceTriples(url1);
				res1 = new Resource(url1, url_quads);
				
				url2 = fields[1];
				url_quads = KNOWLEDGE_BASE.getResourceTriples(url2);
				res2 = new Resource(url2, url_quads); 
				pairs.add(new Pair(res1, res2));
				if(url1.contains(url2) ||  url2.contains(url1)){
					logger.info(i + " dup: " +url1+", "+url2);
					i++;
				}
			}else{
				logger.info("Warning: readLabeledDataset incorrect file");
				
			}

		}

		getDuplicates(pairs);
	}
	
	private void getDuplicates(List<Pair> unique_pairs){
		logger.info("*** getDuplicates ***");
		for(Pair pair: unique_pairs){
			if(pair.getResource1().getTitle().equals(pair.getResource2().getTitle()) ){//&& pair.getResource1().getType().equals(pair.getResource2().getType()
				logger.info("\t: " + pair.getURL1() +"("+pair.getResource1().getType()+")"+", "+ pair.getURL2() +"("+pair.getResource1().getType()+")");
			}
		}
	}
	
	public static void main(String[] args) {
		CopyOfTrainingDataBuilderBinary builder = new CopyOfTrainingDataBuilderBinary();
		Date t1 = new Date();
		logger.info("Start building training dataset " +  t1.toLocaleString() );
//		builder.runDir(Constants.EVENTLOGS_FOLDER, Constants.TESTINGDATA_LOGS_FOLDER);
		//String filename = Constants.EVENTLOGS_FOLDER+"pruned-toolbar-events-20120702";
		//builder.run(filename);
		
		try {
			builder.readLabeledDataset();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Date t2 = new Date();

        long diff = t2.getTime() - t1.getTime();
        long diffSeconds = diff / 1000 % 60;  
        long diffMinutes = diff / (60 * 1000) % 60; 
		logger.info("Ended building training dataset " + t2.toLocaleString() + ", duration:" +diffMinutes+"min, "+ diffSeconds+ " secs");
		
//		t1 = new Date();
//		filename = Constants.EVENTLOGS_FOLDER+"list-toolbar-events.pig-20120806_v03";
//		builder.run(filename);
//		t2 = new Date();
//		diff = t2.getTime() - t1.getTime();
//		diffSeconds = diff / 1000 % 60; 
//		diffMinutes = diff / (60 * 1000) % 60; 
//		logger.info("Ended bulding training dataset " + t2.toLocaleString() + ", duration:" +diffMinutes+"min, "+ diffSeconds+ " secs");
		
	}

}
