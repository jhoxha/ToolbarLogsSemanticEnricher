package itembasedCF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;
import metrics.similarity.Cosine;

import org.apache.log4j.Logger;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixIO;
import set.maximization.Recommendation;
import util.Constants;
import util.FileUtils;

public class ItemBasedCF {
	final static Logger logger = Logger.getLogger(ItemBasedCF.class);
	Map<String, List<Log>> sessions = new HashMap<String, List<Log>>();
	static List<String> uniqueURLs = new ArrayList<String>();
	Map<String, Integer> sessions_indexes= new HashMap<String, Integer>(); 
	Map<String, Integer> URLs_indexes= new HashMap<String, Integer>(); 
	static KnowledgeBase KNOWLEDGE_BASE;

	DenseMatrix64F U=null;
	String PATH="J://myworkspace//ToolbarLogsSemanticEnricher//data//results/";
	String matrix_filename="J://myworkspace//ToolbarLogsSemanticEnricher//data//matrices//matrix_sessions_resources.csv"; 
	public ItemBasedCF(){
		
		KNOWLEDGE_BASE = new KnowledgeBase();
		KNOWLEDGE_BASE.readKNOWLEDGE_BASE();

		if(U==null){
			createUserSessionsMatrix(PATH+"pruned-logs//");
		}
	}
	
	public DenseMatrix64F getUserSessionsMatrix(){
		return U;
	}
	
	private void createUserSessionsMatrix(String dirname){
		sessions = LogParser.readEventSessionsFromDir(dirname);
		logger.info("sessions.size() "+ sessions.size());

//		for (String sessid : sessions.keySet()) {
//			for (Log log : sessions.get(sessid)) {
//				if (!uniqueURLs.contains(log.getUrl()) ) {
//					uniqueURLs.add(log.getUrl());
//				}
//			}
//		}

//		Vector<String> all_labeled_resources = new FileUtils().fileToVector(Constants.EVAL_RECOMMENDATIONS+"100-resources.txt");
		Vector<String> all_labeled_resources = new FileUtils().fileToVector(Constants.EVAL_RECOMMENDATIONS+"all-resources.txt");
		logger.info("all_labeled_resources size: "+ all_labeled_resources.size());
		int rows=0;
		for (String url : all_labeled_resources) {
			if (!uniqueURLs.contains(url)) {// && rows<50
				uniqueURLs.add(url);
				rows++;
			}
		}

		/*
		 * Store also sessionid and resourceURI somewhere
		 */
		U = new DenseMatrix64F(sessions.size(),uniqueURLs.size());

		int r=0;
		int c=0;
		int value=0;
		int nr_uris=0;

		for(String sessId: sessions.keySet()){
			c=0;
			for(String resURI: uniqueURLs){
				value=0;
				for(Log log: sessions.get(sessId)){
					if(log.getUrl().equals(resURI)){
//						logger.info("sessId : " + sessId);
//						logger.info("resURI : " + resURI);
						value=1;
						nr_uris++;
					}
				}
				//U.set(r, c, value);
				U.set(r, c, value);
				//logger.info("set : r,c,value" + r+ "," + c +","+value);
				sessions_indexes.put(sessId, r);
				URLs_indexes.put(resURI, c);
				c++;
			}
			r++;
		}
		//Then, save the matrix
		logger.info("sessNr:" + sessions.size() + ", resNR:"+uniqueURLs.size() + ", nr_uris:" + nr_uris);
		logger.info("U rows:" + r + ", cols:"+c);
        try {
            MatrixIO.saveCSV(U, matrix_filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }		
	}

	private void readUserSessionsMatrix(){
        try {
            U = MatrixIO.loadCSV(matrix_filename);
            U.print();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}
	
	
	/*
	 * Get the COL in the matrix U, belonging to item with given index
	 */
	private double[] getItemSessionsVector(int item_index){
		double[] item_session_vector = new double[U.numRows];
		
		for(int r=0; r<U.numRows; r++){
			item_session_vector[r]=U.get(r, item_index);
		}
		
		return item_session_vector;
	}

	public double getSimValue(int ip_index, int iq_index){
		double sim_value=0;
		sim_value = new Cosine().cosine( getItemSessionsVector(ip_index), getItemSessionsVector(iq_index)); 
		
		return sim_value;
	}
	
	public double getPredictedWeightedSum(List<Integer> user_sessions_indexes, int i_t_index, int k){
		//First, get k most similar items to the target item i_t
		//Use the getSimValue method, iterating on matrix U to find top-k most similar items to i_t
		//List<Double> i_t_simvalues= new ArrayList<Double>();
		double weighted_sum=0;
		List<Map<Integer, Double>> all_mostsimilar_items= new ArrayList<Map<Integer, Double>>();
		List<Map<Integer, Double>> k_mostsimilar_items= new ArrayList<Map<Integer, Double>>();

		double simvalue=0;
		for(int r=0; r<U.numRows;r++){
			//logger.info("\t r: "+r + "out of" + U.numRows);
		}
		
		for(int c=0; c<U.numCols;c++){
			if(c!=i_t_index){
				simvalue = new Cosine().cosine( getItemSessionsVector(i_t_index), getItemSessionsVector(c));
				if(simvalue>0){
//					logger.info("\t c: "+c + "out of" + U.numCols);
//					logger.info("\t simvalue_cosine:"+simvalue + " c="+c + ", url:"+getURLFromIndex(c) + ", i_t_index="+i_t_index);
					Map<Integer, Double> pair = new LinkedHashMap<Integer, Double>();
					pair.put(c, simvalue);
					if(!all_mostsimilar_items.contains(pair)){
						all_mostsimilar_items.add(pair);
					}
				}
			} else {
				//logger.info("\t r: "+r + " = i_t_index:" + i_t_index);
			}
		}
		//logger.info("all_mostsimilar_items size: " + all_mostsimilar_items.size());
		Collections.sort(all_mostsimilar_items, new MapCustomComparator());
		for(Map<Integer, Double> simitem: all_mostsimilar_items){
			for(Integer item_index: simitem.keySet()){
				//logger.info("simitem: " + item_index + ", "+simitem.get(item_index));
			}
		}
		if(all_mostsimilar_items.size()>k){
			k_mostsimilar_items = all_mostsimilar_items.subList(0, k);
		} else {
			k_mostsimilar_items = all_mostsimilar_items.subList(0, all_mostsimilar_items.size());

		}
		//logger.info("k_mostsimilar_items size: " + k_mostsimilar_items.size());
		
		/*
		 * Now, compute the weighted sum over given user sessions indexes
		 */
		int item_j_index=0, sum_products=0, sum_sims=0;
		double sim_j_t=0;
		for(Map<Integer, Double> item_j: k_mostsimilar_items){
			//logger.info("k_mostsimilar item_j: " + item_j.keySet().toArray()[0]);
			item_j_index = (Integer) item_j.keySet().toArray()[0];
			sim_j_t = (Double) item_j.values().toArray()[0];
			for(int a: user_sessions_indexes){
				sum_products += U.get(a,item_j_index) * sim_j_t;
			}
			sum_sims += sim_j_t;
		}
		if(sum_sims>0){
			weighted_sum=sum_products/sum_sims;
		}
			
		return weighted_sum;
	}
	
	private class MapCustomComparator implements Comparator<Map<Integer, Double>> {
		Double c;
		Double sim_value1, sim_value2;
	    @Override
	    public int compare(Map<Integer, Double> r1, Map<Integer, Double> r2) {
	    	sim_value1 = (Double) r1.values().toArray()[0] * 1000;
	    	sim_value2 = (Double) r2.values().toArray()[0] * 1000;
	    	c = sim_value2 - sim_value1;
	    	
	        return c.intValue();
	    }
	}
	public List<Recommendation> getTopNRecommendations(String query_url_str, int N, int k){
		Resource query_url= new Resource(query_url_str, KNOWLEDGE_BASE.getResourceTriples(query_url_str));
		String userId="";
		for (String sessid : sessions.keySet()) {
			for (Log log : sessions.get(sessid)) {
					if (log.getUrl().equals(query_url.getURI()) ) {
						userId=log.getGuid();
					}
			}
		}

		List<Recommendation> topNrecs=new ArrayList<Recommendation>();
		List<Integer> user_sessions_indexes = new ArrayList<Integer>();
		for(String sessionId: sessions_indexes.keySet()){
			if(sessionId.contains(userId) && !userId.isEmpty()){
				//logger.info("sessionId: " +  sessionId);
				user_sessions_indexes.add(sessions_indexes.get(sessionId));
			}
		}
		logger.info("user_sessions_indexes: " +  user_sessions_indexes);

		logger.info("URLs_indexes size: " +  URLs_indexes.size());
		double weighted_sum=0;
		int i_t_index=0;
		//List<Double> weighted_sums= new ArrayList<Double>();
		List<Map<Integer, Double>> weighted_sums_items= new ArrayList<Map<Integer, Double>>();
		List<Map<Integer, Double>> topN_weighted_sums_items= new ArrayList<Map<Integer, Double>>();

		int url_nr=1;
		for(String url: URLs_indexes.keySet()){
			//if(url.equals(query_url.URI)){
				i_t_index = URLs_indexes.get(url);
				//logger.info("\t i_t_index: " +  i_t_index + "; " + url_nr+ " out of " + URLs_indexes.size());
				weighted_sum = getPredictedWeightedSum(user_sessions_indexes, i_t_index, k);
				//logger.info("\t weighted_sum: " +  weighted_sum);
				if(weighted_sum > 0){
					Map<Integer, Double> pair = new LinkedHashMap<Integer, Double>();
					pair.put(i_t_index, weighted_sum);
					weighted_sums_items.add(pair);
				}
			//}
				url_nr++;
		}
		
		logger.info("weighted_sums_items size: " +  weighted_sums_items.size());

		Collections.sort(weighted_sums_items, new MapCustomComparator());
		if(weighted_sums_items.size()>N){
			topN_weighted_sums_items = weighted_sums_items.subList(0, N);
		}else{
			topN_weighted_sums_items = weighted_sums_items.subList(0, weighted_sums_items.size());
		}
			
		for(Map<Integer, Double> simitem: topN_weighted_sums_items){
			for(Integer item_index: simitem.keySet()){
				logger.info("topN simitem: " + item_index + ", "+simitem.get(item_index));
			}
		}
		
		Recommendation topN_rec;
		String relevant_url="";
		float relevance_value;
		double relevance_value_double=0.0;
		for(Map<Integer, Double> topN_weighted_sums_item: topN_weighted_sums_items){
			relevant_url = getURLFromIndex((Integer) topN_weighted_sums_item.keySet().toArray()[0]);
			relevance_value_double = (Double) topN_weighted_sums_item.values().toArray()[0];
			//relevance_value = (Float) topN_weighted_sums_item.values().toArray()[0];
			//someLongValue.floatValue()
			relevance_value= (float) relevance_value_double;
//			logger.info("topN relevant_url: " + relevant_url + ", relevance_value="+relevance_value);
			topN_rec = new Recommendation(new Pair(query_url, new Resource(relevant_url,  KNOWLEDGE_BASE.getResourceTriples(relevant_url))), relevance_value);
			topNrecs.add(topN_rec);
			logger.info("topN_rec: " + topN_rec.getPair().getURL1() + ", "+ topN_rec.getPair().getURL2() + ", rel="+topN_rec.getRelevance());
		}
		
		return topNrecs;
	}
	
	private String getURLFromIndex(int index){
		String URL="";
		for(String item: URLs_indexes.keySet()){
			if (URLs_indexes.get(item)==index){
				URL = item;
			}
		}
		
		return URL;
	}
	
	public static void main( String args[] ) {
		ItemBasedCF itemBasedCF = new ItemBasedCF();
		DenseMatrix64F B = itemBasedCF.getUserSessionsMatrix();
	       // B.print();
		String query_url="http://upcoming.yahoo.com/event/9035830/PA/Hershey/Mamma-Mia-National-Tour/Hershey-Theater/";
		itemBasedCF.getTopNRecommendations(query_url, 5, 10);
	}
}
