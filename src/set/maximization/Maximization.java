package set.maximization;

import java.util.ArrayList;
import java.util.List;

import metadata.annotator.Pair;


public class Maximization {
	List<Recommendation> recs_R ;
	List<Recommendation> recs_N ;

	float lambda=0;
	float diversity_set=0, relevance_fail_pair=0, relevance_set=0, set_max=0;

	public Maximization(List<Recommendation> recommendations_R, float a_lambda){
		recs_R=recommendations_R;
		lambda = a_lambda;
	}
	
	public void getMaxSubset(){
		/*
		 * S = 20
		 * N=10
		 */
		int N=4;
		Recommendation rec;
		recs_N = new ArrayList<Recommendation>();
		List<Recommendation> subset = new ArrayList<Recommendation>();
		
		String subsetStr="";
		for(int i=0; i<recs_R.size(); i++){
			subsetStr="";
			subset = new ArrayList<Recommendation>();
			
			rec = recs_R.get(i);
			
			relevance_fail_pair = 1-rec.getRelevance();
			relevance_set *= relevance_fail_pair;
			
			
			for(int j=i+1; j<i+N && j<recs_R.size(); j++){//recs_R.size()
				subset.add(rec);
				subsetStr+=rec.getRelevance()+", ";
				rec = recs_R.get(j);
				subset.add(rec);
				
//				relevance_fail_pair = 1-rec.getRelevance();
//				relevance_set *= relevance_fail_pair;
				subsetStr+=rec.getRelevance()+", ";
			}
			
			System.out.println("subset "+i+": " + subsetStr);
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<Recommendation> recommendations_R = new ArrayList<Recommendation>();
		recommendations_R.add(new Recommendation(new Pair("kot", "kot"), 1));
		recommendations_R.add(new Recommendation(new Pair("kot", "kot"), 2));
		recommendations_R.add(new Recommendation(new Pair("kot", "kot"), 3));
		recommendations_R.add(new Recommendation(new Pair("kot", "kot"), 4));
		recommendations_R.add(new Recommendation(new Pair("kot", "kot"), 5));
		
		Maximization max = new Maximization(recommendations_R, 0);
		max.getMaxSubset();

	}

}
