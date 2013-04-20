package metrics.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import metrics.similarity.SpreadedVector.TermWeightPair;

public class SetSpreadingSimilarity {
	final static Logger logger = Logger.getLogger(SetSpreadingSimilarity.class);
	SpreadedVector termi1_vec;
	SpreadedVector termi2_vec;

	public SetSpreadingSimilarity(){
	}
	
	public double similarity(SpreadedVector vec1, SpreadedVector vec2){
		termi1_vec = vec1;
		termi2_vec = vec2;

		double similarity =0;
		double iterations_similarity =0;
		
		List<Double> iterations_similarity_list = new ArrayList<Double>();
		int iter=0;
		List<TermWeightPair> set1, set2;
		double dot_product=0;
		
		for(Entry<List<TermWeightPair>, Integer> entry1: termi1_vec.getSpreaded_vector().entrySet()){
			iter = entry1.getValue();
			set1 = entry1.getKey();
		
			//logger.info("iter: " + iter);
			for(Entry<List<TermWeightPair>, Integer> entry2: termi2_vec.getSpreaded_vector().entrySet()){
				set2 = entry2.getKey();
				
				//logger.info("\t iter here: " + entry2.getValue());
				if(entry2.getValue()==iter){
					try {
						dot_product = dotProduct(set1, set2);
						//logger.info("\t dot_product: " + dot_product);
						iterations_similarity = dot_product / ( magnitude(set1)*magnitude(set2));
						//logger.info("\t iterations_similarity: " + iterations_similarity);
						iterations_similarity_list.add(iterations_similarity);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			}
		}
		
		for(Double iter_sim: iterations_similarity_list){
			similarity  += iter_sim;
		}
		similarity = similarity / iterations_similarity_list.size();
		return similarity;
	}
	
    public static double dotProduct(List<TermWeightPair> set1, List<TermWeightPair> set2) throws Exception {
        
//        if(vectorOne.length != vectorTwo.length){ throw new Exception(
//              "Input Vectors do not have the" + "same number of dimensions.");
//        }
       
        double dotProduct = 0;
        for(int i = 0; i < set1.size(); i++){
        	if(set1.get(i).term.equals(set2.get(i).term)){
        		dotProduct += (set1.get(i).weight * set2.get(i).weight);
        	}
        }

//   	   	logger.info("dotProduct: "+ dotProduct);
        return dotProduct;
     }

    public static double magnitude(List<TermWeightPair> spreaded_set){
        double magnitude = 0;
        for(int i = 0; i < spreaded_set.size(); i++){
           magnitude += Math.pow(spreaded_set.get(i).weight, 2);
        }
        //logger.info("\t magnitude : " + magnitude);
        return Math.sqrt(magnitude);
     }
}
