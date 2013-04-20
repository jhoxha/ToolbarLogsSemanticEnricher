package util;

import org.apache.log4j.Logger;

public class Utils {
	final static Logger logger = Logger.getLogger(Utils.class);

	public static boolean isHomepage(String url){

		boolean isHomepage = false;
		if( url.matches("http.*\\:\\/\\/www\\.eventbrite\\.com\\/") || url.matches("http.*\\:\\/\\/www\\.eventbrite\\.com") || url.matches("http.*\\:\\/\\/eventbrite\\.com\\/") ||
				url.matches("http.*\\:\\/\\/upcoming\\.yahoo\\.com") || url.matches("http.*\\:\\/\\/upcoming\\.yahoo\\.com\\/") ||
				url.matches("http.*\\:\\/\\/www\\.eventful\\.com\\/") || url.matches("http.*\\:\\/\\/eventful\\.com") || url.matches("http.*\\:\\/\\/eventful\\.com\\/")
			  ){
			isHomepage=true;
			//logger.info("isHomepage: '" + url +"'");
		}
		
		return isHomepage;
	}
	
	public static boolean isTypedResource(String url){
		return false;
	}
	
	public static boolean isInDomainOfInterest(String url){
		return (url.matches(Constants.EVENTFUL) || url.matches(Constants.EVENTBRITE) || url.matches(Constants.UPCOMING) );	
		//|| url.matches(Constants.YOUTUBE)
	}
	
	public static boolean hasValidHost(String url, String host){
		return (host.contains("eventful") || host.contains("eventbrite") || (host.contains("yahoo") && !url.matches(".*search\\.yahoo.*")));
	}
	
//	public static boolean fromDomain(String urlDomain, Constants.Strings domain){
//		return (urlDomain.matches(domain.DOMAIN_EVENTFUL.toString()));
//	}
	
}
