package metadata.annotator;

import java.util.List;


public class Pair {
	//Resource
	private Resource resource1;
	private Resource resource2;
	
	private float semantic_distance;
	
	public Pair(Resource res1, Resource res2){
		resource1=res1;
		resource2=res2;
	}
	
	public boolean containedIn(List<Pair> pairs){
		boolean contained=false;
		for(Pair apair: pairs){
			if( (apair.getURL1().equals(this.getURL1()) && apair.getURL2().equals(this.getURL2()))
					||
				(apair.getURL2().equals(this.getURL1()) && apair.getURL1().equals(this.getURL2()))
			  ){
				contained= true;
			}
		}
		
		return contained;
	}

	
	public String toString(){
		return "("+getURL1() + ", " + getURL2() + ")";
	}


	public Resource getResource1() {
		return resource1;
	}

	public Resource getResource2() {
		return resource2;
	}
	
	public String getURL1() {
		return resource1.getURI();
	}


	public String getURL2() {
		return resource2.getURI();
	}
}
