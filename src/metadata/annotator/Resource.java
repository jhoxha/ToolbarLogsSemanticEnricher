package metadata.annotator;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import triple.parser.Quad;
import util.Constants;
import util.GetPLD;

public class Resource {
	final static Logger logger = Logger.getLogger(Resource.class);

	public String URI="";
	private String Type="";
	private String title="";
	private String domain="";
	boolean isResource=false;
	List<Quad> resource_quads = new ArrayList<Quad>();
	
	public Resource(String uri, List<Quad> quads){
		isResource=false;
		for(Quad quad: quads){
			
			if( quad.subject.equals(uri) && quad.predicate.equals("wam:type") && 
				( (quad.object.equals("wam:Event") || quad.object.equals("wam:Venue") || quad.object.equals("wam:Performer") || quad.object.equals("wam:Video")) )
			 ){
				isResource=true;
				URI = uri;
				Type = quad.object;
				resource_quads = quads;
				try {
					domain = GetPLD.computePLD(URI.toString());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				//logger.info("\t type: "+Type);
			} 
			
			
			if( quad.subject.equals(uri) && quad.predicate.equals("wam:type") && (quad.object.equals("wam:ResourceGrouping"))){
				isResource=false;
				this.URI = uri;
				this.Type = quad.object;
				resource_quads = quads;
			}

			if(quad.predicate.equals(Constants.NS_OG+"title") || quad.predicate.equals(Constants.NS_DC+"title")){
				title = quad.getObject();
			}
		}
		//logger.info("\t type: "+Type + ", title:"+ title);
	}

	public boolean isResource() {
		return isResource;
	}	
	public boolean isEmpty() {
		return URI.isEmpty();
	}

	public void setType(String type){
		Type= type;
	}
	
	public String getType(){
		return Type;
	}
	
	public String getTitle(){
		return title;
	}
	public void setTitle(String aTitle){
		title= aTitle;
	}
	
	public String getDomain(){
		return domain;
	}
	
	public boolean containedIn(Map<Resource, Integer> resourceMap) {
		boolean contained=false;
		for(Resource key: resourceMap.keySet()){
			if(URI.equals(key.getURI())){
				 contained=true;
			}
		}
		return contained;
	}
	
	public boolean containedIn(List<Resource> resourceList) {
		boolean contained=false;
		for(Resource resource:resourceList){
			if(URI.equals(resource.getURI())){
				 contained=true;
			}
		}
		return contained;
	}		

	public Integer getResourceIDFromMap(Map<Resource, Integer> resourceMap) {
		Integer resID=0; 
		for(Resource key: resourceMap.keySet()){
			if(URI.equals(key.getURI())){
				resID = resourceMap.get(key);
			}
		}

		return resID;
	}
	
	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}
	
	public String print() {
		return ""+URI+ ", "+Type + ", " + title;
	}
	
}
