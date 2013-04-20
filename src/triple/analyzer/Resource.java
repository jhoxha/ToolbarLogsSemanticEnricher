package triple.analyzer;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class Resource {
	final static Logger logger = Logger.getLogger(Resource.class);

	public String URI;
	public String type;
	public String name;
	
	public Resource(String URI, String type){
		this.URI=URI;
		this.type=type;
	}

	public boolean isEmpty() {
		return URI.isEmpty();
	}

	public String print() {
		return ""+URI+" "+type;
	}

	public boolean containedIn(Map<Resource, Integer> resourceMap) {
		boolean contained=false;
		for(Resource key: resourceMap.keySet()){
			if(URI.equals(key.URI)){
				 contained=true;
			}
		}
		return contained;
	}

	public Integer getResourceIDFromMap(Map<Resource, Integer> resourceMap) {
		Integer resID=0; 
		for(Resource key: resourceMap.keySet()){
			if(URI.equals(key.URI)){
				resID = resourceMap.get(key);
			}
		}

		return resID;
	}
	
	public class Event extends Resource{
		String from_date;
		String to_date;
		Host host;
		Venue venue;

		public Event(String URI) {
			super(URI, "wam:Event");
		}
	}
	
	public class EventGrouping extends Resource{
		List<Event> events;
		public EventGrouping(String URI) {
			super(URI, "wam:EventGrouping");
		}
	}	
	public class Host extends Resource{
		public Host(String URI) {
			super(URI, "wam:Host");
		}
	}
	
	public class Venue extends Resource{
		public Venue(String URI) {
			super(URI, "wam:Venue");
		}
	}
	
	public class Performer extends Resource{
		public Performer(String URI) {
			super(URI, "wam:Performer");
		}
	}
	
	public class Photo extends Resource{
		public Photo(String URI) {
			super(URI, "wam:Photo");
		}
	}
	
	public class Video extends Resource{
		public Video(String URI) {
			super(URI, "wam:Video");
		}
	}	

}
