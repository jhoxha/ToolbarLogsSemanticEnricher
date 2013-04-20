package metrics.similarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class SpreadedVector {
	final static Logger logger = Logger.getLogger(SpreadedVector.class);
	private Map<List<TermWeightPair>, Integer> spreaded_vector;
	
	public SpreadedVector(){
		spreaded_vector = new HashMap<List<TermWeightPair>, Integer>();
	}

	
	public void appendSpreadedSet(List<TermWeightPair> spreaded_set, int iteration ) {
		spreaded_vector.put(spreaded_set, iteration);
	}


	public Map<List<TermWeightPair>, Integer> getSpreaded_vector() {
		return spreaded_vector;
	}

	public void setSpreaded_vector(Map<List<TermWeightPair>, Integer> spreaded_vector) {
		this.spreaded_vector = spreaded_vector;
	}

	public String print(){
		String spreaded_vector_str="";
		String set="";
		int iter =0;
		
		for(List<TermWeightPair> iter_spreaded_set: spreaded_vector.keySet()){
			iter = spreaded_vector.get(iter_spreaded_set);
			spreaded_vector_str += "\n "+iter+": ";
			
			set="";
			for(TermWeightPair apair: iter_spreaded_set){
				set += "\t <" + apair.resourceURI+ ", " + apair.term + ", " + apair.weight+ ", " + apair.relation+">";
			}
			spreaded_vector_str += "("+iter_spreaded_set.size()+"){ " + set +" }";
		}
		
		return spreaded_vector_str;
	}
	
	public static class TermWeightPair implements Comparable<TermWeightPair>{
		String resourceURI;
		String term;
		double weight;
		String relation;
		boolean isURI;
		
		public TermWeightPair(String aResourceURI, String aTerm,  double aWeight, String aRelation, boolean _isURI){
			resourceURI = aResourceURI;
			term = aTerm;
			weight = aWeight;
			relation = aRelation;
			isURI = _isURI;
		}
		
		public int compareTo(TermWeightPair pair) {  
		  return this.relation.compareTo(pair.relation);  
		} 
	}
	
     

}
