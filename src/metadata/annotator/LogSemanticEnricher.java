package metadata.annotator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metrics.similarity.Levenshtein;

import org.apache.log4j.Logger;

//import com.javacodegeeks.youtube.YouTubeManager;
//import com.javacodegeeks.youtube.model.YouTubeVideo;

import triple.parser.MetadataEvalFunc;
import triple.parser.Quad;
import triple.parser.Triple;
import triple.parser.TripleNxParser;
import util.Constants;

public class LogSemanticEnricher {
	final static Logger logger = Logger.getLogger(LogSemanticEnricher.class);
	static List<Triple> valid_triples_eventful = new ArrayList<Triple>();
	static List<Triple> valid_triples_eventbrite = new ArrayList<Triple>();
	static List<Triple> valid_triples_upcoming = new ArrayList<Triple>();
	
	static String youtube_clientID = "semantic-recommender-kit";
	static int youtube_timeout = 2000;
	static int video_max_results = 3;

	public List<Quad> generateMetadata(String url, List<Triple> url_triples) {
		logger.info("generateMetadata: " + url);
		List<Quad> quads = new ArrayList<Quad>();
		List<Triple> valid_triples = new ArrayList<Triple>();
		
		URI uri = null;
		String type="";

		try {
			uri = new URI(url);
			type=getMainType(uri, url_triples);
			if(type.equals("Event") ){
				logger.info("** Event ** : " + type);
				
				if(uri.getHost().contains("eventful.com")){	
					valid_triples.addAll(generateTriples_ResourceEvent_eventful(uri, url_triples));
				} else if(uri.getHost().contains("eventbrite.com")){
					valid_triples.addAll(generateTriples_ResourceEvent_eventbrite(uri, url_triples));
				} else if(uri.getHost().contains("yahoo.com")){
					valid_triples.addAll(generateTriples_ResourceEvent_upcoming(uri, url_triples));
				} 					
			}else if(type.equals("Venue")){ 
				logger.info("** Venue ** : " + type);
				if(uri.getHost().contains("eventful.com")){	
					valid_triples.addAll(generateTriples_ResourceVenue_eventful(uri, url_triples));
				} else if(uri.getHost().contains("yahoo.com")){
					valid_triples.addAll(generateTriples_ResourceVenue_upcoming(uri, url_triples));
				}	
			
			} else if(type.equals("Performer")){
				if(uri.getHost().contains("eventful.com")){	
					valid_triples.addAll(generateTriples_ResourcePerformer_eventful(uri, url_triples));
				}					
			} else if(type.equals("ResourceGrouping")){
				valid_triples.addAll(generateTriples_ResourceGrouping(uri, url_triples));
			}else {
				logger.info("type: " + type);
				
			}
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		logger.info("URI: "+ uri.toString());
		for(Triple triple: valid_triples){
			logger.info("\t" + triple.print()); 
			quads.add(new Quad(triple,url));
			
		} 
		logger.info("*********Total " + quads.size() + " generated triples************");
		
		/*
		 * This is done for Duplicate Checking 
		 * Enable splitting for performance reasons
		 */
		if(uri.getHost().contains("eventful.com")){
			valid_triples_eventful.addAll(valid_triples);
		}else if(uri.getHost().contains("eventbrite.com")){
			valid_triples_eventbrite.addAll(valid_triples);
		}else if(uri.getHost().contains("yahoo.com")){
			valid_triples_upcoming.addAll(valid_triples);
		}

		return quads;
	}
	
	private static List<Triple> generateTriples_ResourceGrouping(URI uri, List<Triple> url_triples) {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:ResourceGrouping"));
		for (Triple triple : url_triples) {
			 valid_triples.add(triple);	
		}
		
		return valid_triples;
	}
	
	private static List<Triple> generateTriples_ResourceEvent_eventful(URI uri, List<Triple> url_triples) {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Event"));
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "eventful.com"));

		String title ="";
		String addressLocality ="", addressRegion="";
		String venue="";
		
		for (Triple triple : url_triples) {
			if ( triple.predicate.equals(Constants.NS_OG+"type") ||
				 triple.predicate.equals(Constants.NS_OG+"title") ||
				 triple.predicate.equals(Constants.NS_OG+"url") || 
				 triple.predicate.equals(Constants.NS_XHTML+"description") ||
				 triple.predicate.equals(Constants.NS_XHTML+"keywords") ||
				 triple.predicate.equals(Constants.NS_GEO+"Point") ||
				 triple.predicate.equals(Constants.NS_GEO+"lat") ||
				 triple.predicate.equals(Constants.NS_GEO+"long") ) {
				 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}

			/*
			 * Get Location
			 */
			if( triple.predicate.equals(Constants.NS_DC+"title") && title.isEmpty()){
				/*
				 * Example
				 * subject	http://purl.org/dc/terms/title	Tommee Profitt in Grand Haven, MI - Jul  1, 2012  7:00 pm | Eventful
				 */
				title =  triple.object.toString();
				try {
					if(title.contains(",") && title.indexOf(",")>title.indexOf("in ")){
						addressLocality = title.substring(title.lastIndexOf("in ")+3, title.indexOf(",")).trim();
					}
					if(title.contains("-") &&  title.indexOf("-")>title.indexOf(",")){
						addressRegion = title.substring(title.indexOf(",")+1, title.lastIndexOf("-")).trim();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
				valid_triples.add(new Triple(uri.toString(), Constants.NS_SCHEMA_ORG+"addressLocality", addressLocality));
				valid_triples.add(new Triple(uri.toString(), Constants.NS_SCHEMA_ORG+"addressRegion", addressRegion));
			}

			
			/*
			 * Get Venue
			 */
			if(triple.predicate.equals(Constants.NS_XHTML+"bookmark") && triple.object.matches(".*\\/venues\\/.*") && venue.isEmpty()){
				venue = triple.object;
				valid_triples.add(triple);
				valid_triples.add(new Triple(uri.toString(), "wam:hasVenue", (triple.object)));
				try {
					URI uri_venue = new URI(triple.object);
					valid_triples.addAll(generateTriples_ResourceVenue_eventful(uri_venue, new ArrayList<Triple>()));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}

		}
		
		
		/*
		 * Get Performer
		 * GET URLS BY HTML-EXTRACTION
		 */
		MetadataEvalFunc extractor = new MetadataEvalFunc();
		Map<String, String> performers = new HashMap<String, String>();
		logger.info(" GET URLS BY HTML-EXTRACTION: " + uri.toString());
		performers = extractor.scrapPage(uri.toString(), "eventful.com");
	
		String performer_name="";
		String video_text_query = "";
		for(String performer_url: performers.keySet()){
			logger.info("performer_url: "+ performer_url);
			try {
				URI uri_performer = new URI(performer_url);
				performer_name= performers.get(performer_url);
				valid_triples.add(new Triple(uri.toString(), "wam:hasPerformer", uri_performer.toString()));
				valid_triples.addAll(generateTriples_ResourcePerformer_eventful(uri_performer, new ArrayList<Triple>()));
				
				/*
				 * Get Video of Performer
				 */
				video_text_query = performer_name;
//				logger.info(" GET VIDEOS PERFORMER WITH API: '" + performer_name+"'" +", "+video_text_query);
//				YouTubeManager youtube_manager = new YouTubeManager();
//				List<YouTubeVideo> videos = youtube_manager.retrieveVideos(video_text_query, video_max_results, true, youtube_timeout);
//				for(YouTubeVideo video: videos){
//					URI uri_video = new URI(video.getWebPlayerUrl());
//					valid_triples.add(new Triple(uri_performer.toString(), "wam:hasVideo", uri_video.toString()));
//					valid_triples.addAll(generateTriples_ResourceVideo(uri_video, video.getTitle(), performer_name, "eventful.com"));
//					
//				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}				
		}
		
	
		/*
		 * Get Video of event (when no performer)
		 */
//		try {
//			if(performers.isEmpty() && !title.isEmpty()){
//				video_text_query = title;
//				logger.info(" GET VIDEOS EVENT WITH API: " + uri.toString() +", "+video_text_query);
//				YouTubeManager youtube_manager = new YouTubeManager();//youtube_clientID
//				List<YouTubeVideo> videos_event = youtube_manager.retrieveVideos(video_text_query, video_max_results, true, youtube_timeout);
//				for(YouTubeVideo video: videos_event){
//					logger.info("video: " + video.getWebPlayerUrl() + ", " + video.getTitle());
//					URI uri_video = new URI(video.getWebPlayerUrl());
//					valid_triples.add(new Triple(uri.toString(), "wam:hasVideo", uri_video.toString()));
//					valid_triples.addAll(generateTriples_ResourceVideo(uri_video, video.getTitle(), "", "eventful.com"));
//					
//				}
//			}
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Event", title, addressLocality);
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				logger.info("\t title: " + title);
				final_urltriples.addAll(valid_triples);
			}
		}
		return final_urltriples;
	}
	
	private static List<Triple> generateTriples_ResourceVideo(URI uri, String title, String performer_name,  String domain) {
		logger.info(" TRIPLES VIDEOS: " + uri.toString() + ", videoTitle:" + title);

		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:domain", domain));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Video"));
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), Constants.NS_DC+"title", title));
		
		if(!performer_name.isEmpty()){
			valid_triples.add(new Triple(uri.toString(), "wam:performerName", performer_name));
		}
		
		try {
			String n3s = MetadataEvalFunc.exec(uri.toString());
			List<Triple> url_triples= new TripleNxParser().parse(n3s);
			
			if(!url_triples.isEmpty()){
				logger.info(" GET MORE INFO WITH ANY23 for VIDEO : " + uri.toString());
				for (Triple triple : url_triples) {
					if ( triple.predicate.equals(Constants.NS_XHTML+"keywords") 
						 || triple.predicate.equals(Constants.NS_XHTML+"description") ){
						 valid_triples.add(new Triple(uri.toString() , triple.predicate, triple.object));
					}
				}
			}
		}  catch (Exception e) {
			e.printStackTrace();
		}	
		

		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Video", title, "");
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}			
		}
		
		return final_urltriples;		
	}
	
	private static List<Triple> generateTriples_ResourceVenue_eventful(URI uri, List<Triple> url_triples) throws URISyntaxException {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "eventful.com"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Venue"));
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		String title="", locality="";
		
		if(url_triples.isEmpty()){
			/*
			 * GET MORE INFO WITH ANY23
			 */
			logger.info(" GET MORE INFO WITH ANY23: " + uri.toString());
			String n3s = MetadataEvalFunc.exec(uri.toString());
			url_triples= new TripleNxParser().parse(n3s);
		}
		for (Triple triple : url_triples) {
			if ( triple.predicate.equals(Constants.NS_RDF+"type") ||
					 triple.predicate.equals(Constants.NS_OG+"url") || 
					 triple.predicate.equals(Constants.NS_OG+"type") ||
					 triple.predicate.equals(Constants.NS_OG+"street-address") ||
					 triple.predicate.equals(Constants.NS_OG+"region") ||
					 triple.predicate.equals(Constants.NS_OG+"postal-code") ||
					 triple.predicate.equals(Constants.NS_GEO+"lat") ||
					 triple.predicate.equals(Constants.NS_GEO+"long") ) {
					 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
				}
			if(triple.predicate.equals(Constants.NS_DC+"title")){
				title = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if(triple.predicate.equals(Constants.NS_OG+"locality")){
				locality = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}			
		}
		
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Venue", title, locality);
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}			
		}
		
		return final_urltriples;
	}
	
	
	private static List<Triple> generateTriples_ResourcePerformer_eventful(URI uri, List<Triple> url_triples) throws URISyntaxException {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "eventful.com"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Performer"));
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		String title="";
		
		if(url_triples.isEmpty()){
			/*
			 * GET MORE INFO WITH ANY23
			 */
			logger.info(" GET MORE INFO WITH ANY23: " +uri.toString());
			String n3s = MetadataEvalFunc.exec(uri.toString());
			url_triples= new TripleNxParser().parse(n3s);			
		}
		for (Triple triple : url_triples) {
			if ( triple.predicate.equals(Constants.NS_OG+"type") ||
				 (triple.subject.equals(uri.toString()) &&triple.predicate.equals(Constants.NS_OG+"url")) ) {
					 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if ( triple.predicate.equals(Constants.NS_OG+"title") ) {
				title =  triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
		}
	
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Performer", title, "");
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}
		}
		
		return final_urltriples;
	}
	
	private static List<Triple> generateTriples_ResourceEvent_eventbrite(URI uri, List<Triple> url_triples) {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Event"));
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "eventbrite.com"));
		String title="", locality="", venueID="";
		
		for (Triple triple : url_triples) {
			if ( (triple.subject.equals(uri.toString()) &&triple.predicate.equals(Constants.NS_RDF+"type")) ||
				 triple.predicate.equals(Constants.NS_ICAL+"summary") ||
				 triple.predicate.equals(Constants.NS_ICAL+"description") ||
				 triple.predicate.equals(Constants.NS_ICAL+"dtstart") ||
				 (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"latitude")) ||
				 (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"longitude")) ||
				 (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"street-address")) ||
				 (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"region")) ||
				 (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"postal-code")) ) {
				 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			
			if(triple.predicate.equals(Constants.NS_DC+"title")){
				title = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if((triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_OG+"locality"))){
				locality = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}

			/*
			 * Get Venue
			 * Do NOT create new Resource for Venue-Eventbrite, since they offer no URLs to display
			 */
			if(triple.predicate.equals(Constants.NS_VCARD+"adr") && venueID.isEmpty()){
				venueID = triple.object;
				if(!venueID.equals(uri.toString())){
					valid_triples.addAll(generateTriples_ResourceVenue_eventbrite(uri, venueID, url_triples));
				}
			}
			/*
			 * Get Video of event (when no performer given)
			 */
		}
		
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Event", title, locality);
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}
		}
		
		return final_urltriples;
	}
		
	private static List<Triple> generateTriples_ResourceVenue_eventbrite(URI event_uri,String venueID, List<Triple> url_triples) {
		List<Triple> valid_triples = new ArrayList<Triple>();
		String venue_URI = event_uri.toString()+"/"+venueID;
		valid_triples.add(new Triple(event_uri.toString(), "wam:hasVenue", venue_URI));
		valid_triples.add(new Triple(venue_URI, "wam:domain", "eventbrite.com"));
		valid_triples.add(new Triple(venue_URI, "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(venue_URI, "wam:type", "wam:Venue"));
		valid_triples.add(new Triple(venue_URI, "wam:URI", venue_URI));
		
		if(!url_triples.isEmpty()){
			logger.info(" GET MORE INFO for Venue : " + venue_URI);
			for (Triple triple : url_triples) {
				if ( (triple.subject.equals(venueID) && triple.predicate.equals(Constants.NS_RDF+"type")) || 
					 triple.predicate.equals(Constants.NS_VCARD+"organization-name") || 
					 (triple.subject.equals(venueID) && triple.predicate.equals(Constants.NS_VCARD+"street-address")) || 
					 (triple.subject.equals(venueID) && triple.predicate.equals(Constants.NS_VCARD+"locality")) || 
					 (triple.subject.equals(venueID) && triple.predicate.equals(Constants.NS_VCARD+"region")) ){
					 valid_triples.add(new Triple(venue_URI, triple.predicate, triple.object));
				}
			}
		}
	
		return valid_triples;
	}	
	
	private static List<Triple> generateTriples_ResourceEvent_upcoming(URI uri, List<Triple> url_triples) {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Event"));
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "upcoming.yahoo.com"));
		String title="", locality="", venue="";
		
		for (Triple triple : url_triples) {
			if ( (triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_RDF+"type")) ||
				 triple.predicate.equals(Constants.NS_ICAL+"summary") ||
				 triple.predicate.equals(Constants.NS_ICAL+"location") ||
				 triple.predicate.equals(Constants.NS_ICAL+"dtstart") ||
				 triple.predicate.equals(Constants.NS_ICAL+"categories") ||
				 triple.predicate.equals(Constants.NS_OG+"region") ||
				 triple.predicate.equals(Constants.NS_OG+"postal-code") ) {
				 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if((triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_DC+"title"))){
				title = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if( triple.predicate.equals(Constants.NS_OG+"locality")){
				locality = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			

			/*
			 * Get Venue
			 */
			if(triple.predicate.equals(Constants.NS_VCARD+"urlofvenue") && venue.isEmpty()){
				venue = triple.object;

				try {
					/*
					 * Check first for duplicate resources
					 */
					URI uri_venue = new URI(venue);
					valid_triples.add(new Triple(uri.toString(), "wam:hasVenue", uri_venue.toString()));
					valid_triples.addAll(generateTriples_ResourceVenue_upcoming(uri_venue, new ArrayList<Triple>()));

				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		
		/*
		 * Get Video of event (when no performer given)
		 * First, GET Performer-URLS BY HTML-EXTRACTION
		 */
		MetadataEvalFunc extractor = new MetadataEvalFunc();
		Map<String, String> performers = new HashMap<String, String>();
		logger.info(" GET URLS BY HTML-EXTRACTION: " + uri.toString());
		performers = extractor.scrapPage(uri.toString(), "upcoming.yahoo.com");

		String performer_name="";
		String video_text_query = "";
		for(String performer_url: performers.keySet()){
			logger.info("performer_url: "+ performer_url);
			try {
				URI uri_performer = new URI(performer_url);
				performer_name= performers.get(performer_url);
				valid_triples.add(new Triple(uri.toString(), "wam:hasPerformer", uri_performer.toString()));
				valid_triples.addAll(generateTriples_ResourcePerformer_eventful(uri_performer, new ArrayList<Triple>()));
				
				/*
				 * Get Video of Performer
				 */
				video_text_query = performer_name;
//				logger.info(" GET VIDEOS PERFORMER WITH API: '" + performer_name+"'" +", "+video_text_query);
//				YouTubeManager youtube_manager = new YouTubeManager();
//				List<YouTubeVideo> videos = youtube_manager.retrieveVideos(video_text_query, video_max_results, true, youtube_timeout);
//				for(YouTubeVideo video: videos){
//					URI uri_video = new URI(video.getWebPlayerUrl());
//					valid_triples.add(new Triple(uri_performer.toString(), "wam:hasVideo", uri_video.toString()));
//					valid_triples.addAll(generateTriples_ResourceVideo(uri_video, video.getTitle(), performer_name, "upcoming.yahoo.com"));
//					
//				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}				
		}
		
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Event", title, locality);
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}
		}
		
		return final_urltriples;
	}
	
	private static List<Triple> generateTriples_ResourceVenue_upcoming(URI uri, List<Triple> url_triples) throws URISyntaxException {
		List<Triple> valid_triples = new ArrayList<Triple>();
		valid_triples.add(new Triple(uri.toString(), "wam:domain", "upcoming.yahoo.com"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Resource"));
		valid_triples.add(new Triple(uri.toString(), "wam:type", "wam:Venue"));
		valid_triples.add(new Triple(uri.toString(), "wam:URI", uri.toString()));
		valid_triples.add(new Triple(uri.toString(), "wam:url", uri.toString()));
		String title="", locality="";
		
		if(url_triples.isEmpty()){
			/*
			 * GET MORE INFO WITH ANY23
			 */
			String n3s = MetadataEvalFunc.exec(uri.toString());
			url_triples= new TripleNxParser().parse(n3s);
			logger.info(" GET MORE INFO for Venue : " + uri.toString());
		}
		for (Triple triple : url_triples) {
			if ( 	 triple.predicate.equals(Constants.NS_RDF+"type") ||
					 triple.predicate.equals(Constants.NS_VCARD+"latitude") ||
					 triple.predicate.equals(Constants.NS_VCARD+"longitude") ||
					 triple.predicate.equals(Constants.NS_VCARD+"fn") ||
					 triple.predicate.equals(Constants.NS_VCARD+"street-address") ||
					 triple.predicate.equals(Constants.NS_VCARD+"region") ||
					 triple.predicate.equals(Constants.NS_VCARD+"postal-code") ) {
					 valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
				}
			if((triple.subject.equals(uri.toString()) && triple.predicate.equals(Constants.NS_DC+"title"))){
				title = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			if( triple.predicate.equals(Constants.NS_VCARD+"locality")){
				locality = triple.object;
				valid_triples.add(new Triple(uri.toString(), triple.predicate, triple.object));
			}
			
			
		}			
	
		List<Triple> final_urltriples = new ArrayList<Triple>();
		URI duplicate_uri = getDuplicate(uri, "wam:Venue", title, locality);
		if (duplicate_uri != null) {
			final_urltriples.add(new Triple(uri.toString(), "wam:hasResource", duplicate_uri.toString()));
		} else {
			if(!title.isEmpty()){
				final_urltriples.addAll(valid_triples);
			}
		}
		
		return final_urltriples;
	}
	
	private static String getMainType(URI uri, List<Triple> url_triples){
		String type="";

		if ( (uri.getHost().contains("eventful.com") && containsTriple(Constants.NS_OG + "type", "activity", url_triples) ) ||
			 (uri.getHost().contains("eventbrite.com")  && containsTriple(Constants.NS_OG + "type", "eventbriteog:event", url_triples)) ||
			 (uri.getHost().contains("yahoo.com") && containsTriple(uri.toString(), Constants.NS_RDF + "type", Constants.NS_ICAL+"vcalendar", url_triples))) {

			type = "Event";
		
		} else if ((uri.getHost().contains("eventful.com") && containsTriple(Constants.NS_OG + "type", "company", url_triples) ) ||
				   (uri.getHost().contains("yahoo.com") && containsTriple(Constants.NS_RDF + "type", Constants.NS_VCARD+"Organization", url_triples)
						   								&& !containsTriple(uri.toString(), Constants.NS_RDF + "type", Constants.NS_ICAL+"vcalendar", url_triples)) ){
			
			type = "Venue";
		} else if ( (uri.toString().matches(".*eventful.com\\/performers\\/.*") && containsTriple(Constants.NS_OG + "type", "musician", url_triples) ) || 
				    (uri.toString().matches(".*eventful.com\\/performers\\/.*") && containsTriple(Constants.NS_OG + "type", "politician", url_triples) )){
			
			type = "Performer";
		} else {
			type = "ResourceGrouping";
		}
		
		return type;
	}
	
	private static boolean containsTriple(String predicate, String object, List<Triple> triples){
		boolean contains=false;
		for (Triple triple : triples) {
			if (triple.predicate.equals(predicate) && triple.object.equals(object)){
				contains= true;
			}
		}
		
		logger.info("NOT contain:" + predicate + ", " +object);
		return contains;
	}
	
	private static boolean containsTriple(String subject, String predicate, String object, List<Triple> triples){
		boolean contains=false;
		for (Triple triple : triples) {
			if (triple.subject.equals(subject) && triple.predicate.equals(predicate) && triple.object.equals(object)){
				contains= true;
			}
		}
		
		logger.info("NOT contain:" + predicate + ", " +object);
		return contains;
	}	

	private static URI getDuplicate(URI uri, String type, String title, String locality) {
		URI duplicate_uri = null;
		if(uri.getHost().contains("eventful.com")){
			duplicate_uri= getDuplicateInDomain(valid_triples_eventful, uri, type, title, locality);
		}else if(uri.getHost().contains("eventbrite.com")){
			duplicate_uri= getDuplicateInDomain(valid_triples_eventbrite, uri, type, title, locality);
		}else if(uri.getHost().contains("yahoo.com")){
			duplicate_uri= getDuplicateInDomain(valid_triples_upcoming, uri, type, title, locality);
		}
		return duplicate_uri;
	}
	
	private static URI getDuplicateInDomain(List<Triple> triples, URI uri, String type, String title, String locality ){
		URI duplicate_uri = null;
		String duplicate_URL = "";
		boolean sameType=false, sameTitle=false, sameLocality=false;

		for(Triple triple: triples){
			if( (triple.predicate.equals("wam:type") &&  triple.object.equals(type))){
				sameType=true;
				//logger.info("sameType: " +type + " = " + triple.object);
				duplicate_URL = triple.subject;
			}
			if(sameTitle(triple, title)){
				sameTitle=true;
				//logger.info("sameTitle: " +title + " = " + triple.object);
				duplicate_URL = triple.subject;
			}
			
			if(type.equals("wam:Event")){
				if	( (triple.predicate.equals(Constants.NS_VCARD+"locality") || triple.predicate.equals(Constants.NS_OG+"locality") ) 
						&&  triple.object.equals(locality)
					 ){
					sameLocality=true;
					//logger.info("\t  Event - sameLocality: '" +locality + "' = '" +  triple.object + "' , p:" + triple.predicate + ", eq=" + triple.object.equals(locality));
				}
			}else{
				//logger.info("\t locality: " +locality);
				sameLocality=true;
			}
			
		}
		if(sameType && sameTitle && sameLocality){
			try {
				duplicate_uri = new URI(duplicate_URL);
				logger.info("duplicate_URLs: " + uri.toString() + " = " + duplicate_uri.toString());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		
		return duplicate_uri;
	}
	
	private static boolean sameTitle(Triple triple, String title){
		/*
		 * Levenshtein : allowed distance 20% of title
		 */
		double diff= Math.floor(title.length()/5);
		if( (triple.predicate.equals(Constants.NS_OG+"title") || triple.predicate.equals(Constants.NS_DC+"title"))
				&& 
				new Levenshtein(triple.object, title).getSimilarity() < diff){
			//logger.info(title + " ~ " + triple.object + "(sim="+new Levenshtein(triple.object, title).getSimilarity()+", diff="+diff+")");
			return true;
		}else{
			return false;
		}
	}
}
