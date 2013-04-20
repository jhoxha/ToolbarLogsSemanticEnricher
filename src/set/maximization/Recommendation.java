package set.maximization;

import metadata.annotator.Pair;

public class Recommendation {
	public String predicted_url="";

	Pair pair;
	public float relevance=0;
	public int index=0;
	
	public Recommendation(Pair apair, float relevance_pair){
		pair = apair;
		relevance = relevance_pair;
		predicted_url = apair.getURL2();
	}

	public Recommendation(String predicted_url, Float relevance_value){
		this.predicted_url=predicted_url;
		this.relevance = relevance_value;
	}

	public Recommendation(String predicted_url, Float relevance_value, int rec_index){
		this.predicted_url=predicted_url;
		this.relevance = relevance_value;
		index = rec_index;
	}

	public Pair getPair(){
		return pair;
	}
	
	public float getRelevance(){
		return relevance;
	}
	
	public boolean contains(Object other) { // Right!
       return true;
    }

}
