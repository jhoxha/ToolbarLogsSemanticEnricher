package svm.features;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import triple.parser.Quad;
import util.Constants;

import log.parser.Log;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;
import metrics.similarity.Cosine;
import metrics.similarity.SemanticSetSpreader;
import metrics.similarity.SetSpreadingSimilarity;
import metrics.similarity.SpreadedVector;
import metrics.similarity.TFIDF;

public class FeatureVectorBuilder {
	final static Logger logger = Logger.getLogger(FeatureVectorBuilder.class);
	static Map<Pair, String> labeled_pairs = new HashMap<Pair, String>();
	static Map<String, List<Log>> sessions = new HashMap<String, List<Log>>();
	static KnowledgeBase KNOWLEDGE_BASE;
	static float threshold_relevance=0;

	boolean COLLAB_SAMESESSION;
	boolean COLLAB_SIM_CONDITIONAL;
	boolean COLLAB_SIMILAR_USERS;
	boolean COLLAB_ORD;
	
	boolean CONTENT_SAME_TYPE;
	boolean CONTENT_SHARE_RELATION;
	boolean CONTENT_SYNTACTIC_SIMILARITY;
	boolean CONTENT_SEMANTIC_SIMILARITY;
	static FileOutputStream fout;
	static ObjectOutputStream oos;

	public FeatureVectorBuilder (Map<String, List<Log>> logs_sessions, Map<Pair, String> labeledPairs, KnowledgeBase kBase, 
			boolean collab_same_session, boolean collab_similar_users, boolean collab_sim_conditional, boolean collab_ORD,
			boolean content_same_type, boolean content_share_relation,
			boolean content_synctactic_similarity, boolean content_semantic_similarity){
		
		COLLAB_SAMESESSION = collab_same_session;
		COLLAB_SIMILAR_USERS = collab_similar_users;
		COLLAB_SIM_CONDITIONAL = collab_sim_conditional;
		COLLAB_ORD = collab_ORD;
		
		CONTENT_SAME_TYPE =content_same_type;
		CONTENT_SHARE_RELATION = content_share_relation;
		CONTENT_SYNTACTIC_SIMILARITY = content_synctactic_similarity;
		CONTENT_SEMANTIC_SIMILARITY = content_semantic_similarity;
		
		labeled_pairs = labeledPairs;
		sessions = logs_sessions;
		KNOWLEDGE_BASE = kBase;
		
	}

	public List<String> generateFeatureVectors(){
		
		logger.info(" FEATURE VECTOR ");
		/*
		 * <line> .=. <target>  <feature>:<value> <feature>:<value> ... <feature>:<value> # <info>
		 * <line> .=. 0  1:0 2:0 ... 10:1 
		 */
		List<String> lines = new ArrayList<String>();
		int feature_nr=0;
		float feature_value=0;
		String line = "";
		String label=""; //{+1, -1}
		List<String> features = new ArrayList<String>();
		Map<Pair, Float> pair_ORD_relevance_values = new HashMap<Pair, Float>();
		if(COLLAB_ORD){
			//pair_ORD_relevance_values = compute_ORD(labeled_pairs.keySet());
		}

		/*
		 * Read pre-computed values of semantic similarity
		 */
		String filename_simsemvalues=Constants.LABELDATA_FOLDER+"1230pairs_sim_semantic";
		Map<Pair, Float> pair_semsim_values = readPairSimilarityvaluesFile(filename_simsemvalues, 4);

		Map<Pair, Float> pair_syntsim_values = readPairSimilarityvaluesFile(Constants.LABELDATA_FOLDER+"1230pairs_sim_syntactic", 4);
		int nrpair=1;
		for(Pair pair: labeled_pairs.keySet()){

			label = labeled_pairs.get(pair);
			features = new ArrayList<String>();
			
			if(COLLAB_SAMESESSION){
				feature_value = getFeatureValue_COLLAB_SAMESESSION (pair);
				logger.info("\t same_session: " + feature_value);
				features.add(String.valueOf(feature_value));
			}

			if(COLLAB_SIM_CONDITIONAL){
				feature_value = getFeatureValue_COLLAB_SIM_CONDITIONAL (pair);
				logger.info("\t sim_cond: " + feature_value);
				features.add(String.valueOf(feature_value));
			}

			if(COLLAB_ORD){
				feature_value = getFeatureValue_COLLAB_ORD (pair_ORD_relevance_values, pair);
				logger.info("\t ORD: " + feature_value);
				features.add(String.valueOf(feature_value));
			}

			if(CONTENT_SAME_TYPE){
				feature_value = getFeatureValue_CONTENT_SAME_TYPE (pair);
				logger.info("\t same_type: " + feature_value);
				features.add(String.valueOf(feature_value));
			}
			
			if(CONTENT_SHARE_RELATION){
				feature_value = getFeatureValue_CONTENT_SHARE_RELATION (pair);
				logger.info("\t share_relation: " + feature_value);
				features.add(String.valueOf(feature_value));
			}

			if(CONTENT_SEMANTIC_SIMILARITY){

				/*
				 * On the fly computation of semantic similarity
				 */
				SemanticSetSpreader set_spreader= new SemanticSetSpreader(KNOWLEDGE_BASE);
				//feature_value = getFeatureValue_CONTENT_SEMANTIC_SIMILARITY (pair, set_spreader);

				/*
				 * Read pre-computed values of semantic similarity
				 */
				feature_value = getFeatureValue_CONTENT_SEMANTIC_SIMILARITY (pair, pair_semsim_values);

				logger.info("\t semantic_sim: " + feature_value);
				features.add(String.valueOf(feature_value));
			}
			
			if(CONTENT_SYNTACTIC_SIMILARITY){
//				TFIDF tf_idf_scheme = new TFIDF();
//				feature_value = getFeatureValue_CONTENT_SYNTACTIC_SIMILARITY(pair, tf_idf_scheme);
				
				/*
				 * Read pre-computed values of semantic similarity
				 */
				feature_value = getFeatureValue_CONTENT_SYNTACTIC_SIMILARITY (pair, pair_semsim_values);

				logger.info("\t syntactic_sim: " + feature_value);
				features.add(String.valueOf(feature_value));
				logger.info(pair.getURL1()+ ","+pair.getURL2()+","+feature_value);
			}

			//logger.info("\t label: " + label);

			line = label +" ";
			feature_nr=1;
			for(String feat_value: features){
				line += feature_nr + ":" + feat_value + " " ; 
				feature_nr++;
			}
			 
			line += "\n";
			lines.add(line);
			
			//logger.info("nrpair "+nrpair + " out of " + labeled_pairs.size());
			nrpair++;
		}
		return lines;
	}	
	
	private static float getFeatureValue_CONTENT_SEMANTIC_SIMILARITY(Pair pair, SemanticSetSpreader set_spreader){
		float feature_value=0;
		SpreadedVector vec1 = set_spreader.getSpreadedVector(new Resource(pair.getResource1().getURI(), KNOWLEDGE_BASE.getResourceTriples(pair.getResource1().getURI())));
		SpreadedVector vec2 = set_spreader.getSpreadedVector(new Resource(pair.getResource2().getURI(), KNOWLEDGE_BASE.getResourceTriples(pair.getResource2().getURI())));
		
		feature_value = (float) new SetSpreadingSimilarity().similarity(vec1, vec2);
		
		return feature_value;
	}
	
	private static float getFeatureValue_CONTENT_SEMANTIC_SIMILARITY(Pair pair, Map<Pair, Float> pair_simvalues){
		float feature_value=0;
		for(Pair apair: pair_simvalues.keySet() ){
			if( apair.getURL1().equals(pair.getURL1()) &&
				apair.getURL2().equals(pair.getURL2())){
				feature_value = pair_simvalues.get(apair);
				//logger.info("add: "+ apair.getURL1() + ", " + apair.getURL2() + "), value=" + feature_value);
			}
			
		}
		return feature_value;
	}
	
	private Map<Pair, Float> readPairSimilarityvaluesFile(String filename, int nrfields){
		DataInputStream input1;
		String strLine="";
		String[] fields;
		String url1, url2, sim;
		Map<Pair, Float> pair_values= new HashMap<Pair, Float>();
		List<Quad> url1_quads, url2_quads;

		logger.info("Reading PairSimilarityvaluesFile : "+filename);
		Pair pair;
		try {
			input1 = new DataInputStream(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(input1));
			while ((strLine = br.readLine()) != null)   {
				fields = strLine.split(",");
				if(fields.length==nrfields){
					url1 = fields[nrfields-3];
					url2 = fields[nrfields-2];
					sim = fields[nrfields-1];
					url1_quads = KNOWLEDGE_BASE.getResourceTriples(url1);
					url2_quads = KNOWLEDGE_BASE.getResourceTriples(url2);
					
					pair = new Pair(new Resource(url1, url1_quads), new Resource(url2, url2_quads));
					pair_values.put(pair, Float.parseFloat(sim));
					//logger.info("add: "+fields[0]+" (" + pair.getURL1() + ", " + pair.getURL2() + "), value=" +  Float.parseFloat(sim));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return pair_values;
	}

	private static float getFeatureValue_CONTENT_SYNTACTIC_SIMILARITY(Pair pair, Map<Pair, Float> pair_simvalues){
		float feature_value=0;
		for(Pair apair: pair_simvalues.keySet() ){
			if( apair.getURL1().equals(pair.getURL1()) &&
				apair.getURL2().equals(pair.getURL2())){
				feature_value = pair_simvalues.get(apair);
				
				
				//logger.info("add: "+ apair.getURL1() + ", " + apair.getURL2() + "), value=" + feature_value);
			}
		}
		return (float)Math.round(feature_value * 100000) / 100000;
	}	
	
	private static float getFeatureValue_CONTENT_SYNTACTIC_SIMILARITY(Pair pair, TFIDF tf_idf_scheme){
		float cosine_bow = 0;
		double[] vec1 = tf_idf_scheme.getTFIDFVectorStored(pair.getResource1().getURI());
		double[] vec2 = tf_idf_scheme.getTFIDFVectorStored(pair.getResource2().getURI());
		
		cosine_bow = (float) new Cosine().cosine(vec1, vec2);
		//logger.info("cosine " + cosine_bow);
		
    	try {
  	      fout = new FileOutputStream(Constants.LABELDATA_FOLDER+"1230pairs_sim_syntactic");
  	      oos = new ObjectOutputStream(fout);
  	      oos.writeObject(pair.getURL1()+","+pair.getURL2()+"," + cosine_bow);
  	      oos.close();
  	 }  catch (Exception e) { e.printStackTrace(); }
  	 
		return cosine_bow;
	}
	
	private static int getFeatureValue_COLLAB_SAMESESSION(Pair pair){
		int feature_value=0;
		
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
			feature_value =1;
		}
		
		return feature_value;
	}
	
	private static int getFeatureValue_CONTENT_SAME_TYPE(Pair pair){
		int feature_value=0;
		
		if(pair.getResource1().getType().equals(pair.getResource2().getType())){
			//logger.info("types: " +pair.getResource1().getType()+ " = "+pair.getResource2().getType());
			feature_value = 1;
		}else{
			//logger.info("types: " +pair.getResource1().getType()+ " != "+pair.getResource2().getType());
			feature_value = 0;
		}
		
		return feature_value;
	}	
	
	private static int getFeatureValue_CONTENT_SHARE_RELATION(Pair pair){
		int feature_value=0;
		boolean shareTriple=false;
		for(Quad quad: KNOWLEDGE_BASE.getKNOWLEDGE_BASE()){
			if( ( quad.subject.equals(pair.getURL1()) && quad.object.equals(pair.getURL2()) ) ||
			    ( quad.subject.equals(pair.getURL2()) && quad.object.equals(pair.getURL1()) )
			      && quad.predicate.contains("wam:")){
				//logger.info("quad: " + quad.print());
				shareTriple = true;
			}
		}
		
		if(shareTriple){
			feature_value = 1;
		}else{
			feature_value = 0;
		}
		
		return feature_value;
	}	
	
	private static float getFeatureValue_COLLAB_SIM_CONDITIONAL(Pair pair){
		float sim_conditional = 0, freq_url1=0, freq_pair=0;
		boolean url1_occurs_in_session=false, url2_occurs_in_session=false;
		int nrSessions=sessions.size();
		
		for(String sessid: sessions.keySet()){
			url1_occurs_in_session=false;
			url2_occurs_in_session=false;
			
			for(Log log: sessions.get(sessid)){
				if(log.getUrl().equals(pair.getURL1())){
					url1_occurs_in_session=true;
				}

				if(log.getUrl().equals(pair.getURL2())){
					url2_occurs_in_session=true;
				}
			}
			
			if(url1_occurs_in_session){
				freq_url1++;
			}
			
			if(url1_occurs_in_session && url2_occurs_in_session){
				freq_pair++;
			}
			
		}
		
		/*
		 * sim_conditional = freq (xy) / freq(x) 
		 * 				   = freq(pair.url1 & pair.url2) / freq (pair.url1) 
		 * 				   = (nrSess(pair.url1 & pair.url2)/nrAllSessions) / (nrSess(pair.url1)/nrAllSessions)
		 */
		//freq_url1 = freq_url1 / nrSessions;
		//freq_pair = freq_pair / nrSessions;
		
		//sim_conditional = (freq_url1 / nrSessions) / (freq_pair / nrSessions);
		if(freq_url1==0){
			sim_conditional = 0;
		}else {
			sim_conditional = freq_pair / freq_url1;
		}
		
		if(sim_conditional >0){
			//logger.info("sim_conditional=" + sim_conditional+ " for pair: " +pair.toString());
			//logger.info("\t freq_url1=" + freq_url1+ ", freq_pair=" +freq_pair + ", nrSessions=" + nrSessions);
		}
		

		return sim_conditional;
	}
	
	private static float getFeatureValue_COLLAB_ORD(Map<Pair, Float> pair_ERR_relevance_values, Pair pair){
		float relevance_value = pair_ERR_relevance_values.get(pair);
//		if(relevance_value >= threshold_relevance){
//			relevance_value_str = "+1";
//			nr_pairs_relevant++;
//		}
		
		return relevance_value;
	}

	private static Map<Pair, Float> compute_ORD(Set<Pair> unique_pairs){
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
		
		int pair_nr=1;
		for(Pair pair: unique_pairs){
			logger.info("pair " + pair_nr + " out of " + unique_pairs.size());
			pair_nr++;
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
				
				//logger.info("\t items_k_set_withinSession: " + items_k_set_withinSession.size());

				//logger.info("\t nr_co_items: " + nr_co_items);
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
			logger.info("ERR_value=" + ERR_value + " pair " + apair.toString());
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
}
