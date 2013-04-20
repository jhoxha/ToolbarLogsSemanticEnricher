package util;

public class Constants {
	final static public String PATH = "data//results//";
	final static public String TRIPLES_FOLDER = PATH + "triples//";
	final static public String LOGS_FOLDER=PATH+"logs//"; //list-toolbar-events.pig-20120806
	final static public String EVENTLOGS_FOLDER=PATH+"pruned-logs//";
	//final static public String KNOWLEDGE_BASE=TRIPLES_FOLDER+"kbase//";
	final static public String KNOWLEDGE_BASE="J://myworkspace//ToolbarLogsSemanticEnricher//data//results//triples//kbase//";
	

	//final static public String TRAININGDATA_LOGS_FOLDER="C://workspace//Recommender//resources//toolbar_logs//";
	//final static public String TRAININGDATA_LOGS_FOLDER_BINARY="C://workspace//Recommender//resources//toolbar_logs//binary//";
	//final static public String TESTINGDATA_LOGS_FOLDER="C://workspace//Recommender//resources//toolbar_logs//testing//";

	final static public String TRAININGDATA_RELEVANCE_FOLDER=PATH + "training-relevance//";
	final static public String TRAININGDATA_FOLDER=PATH + "training//";
	final static public String TESTINGDATA_FOLDER= PATH + "testing//";
	final static public String TESTINGDATA_LOGS_FOLDER= PATH + "testing//testing-logs//";
	final static public String LABELDATA_FOLDER= PATH + "labeling//";
	final static public String INDEX_DIRECTORY = Constants.PATH+"index-directory";

	public static String EVAL_RECOMMENDATIONS = "J://myworkspace//ToolbarLogsSemanticEnricher//data//results//"+ "eval-recommendation//";
	
	final static public String EVENTFUL=".*eventful\\.com.*";
	final static public String EVENTBRITE=".*eventbrite\\.com.*";
	final static public String UPCOMING=".*upcoming\\.yahoo.*";
	final static public String YOUTUBE=".*youtube\\.com.*";
	
	final static public String RDFS_TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	final static public String OG_TYPE ="http://opengraphprotocol.org/schema/type";
	
	final static public String NS_SCHEMA_ORG="http://schema.org/";
	final static public String NS_OG="http://opengraphprotocol.org/schema/";
	final static public String NS_DC="http://purl.org/dc/terms/";
	final static public String NS_XHTML="http://www.w3.org/1999/xhtml/vocab#";
	final static public String NS_RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final static public String NS_ICAL="http://www.w3.org/2002/12/cal/icaltzd#";
	final static public String NS_VCARD="http://www.w3.org/2006/vcard/ns#";
	final static public String NS_GEO="http://www.w3.org/2003/01/geo/wgs84_pos#";
	
		
	final static public int SESSION_LENGTH_UPPER_LIMIT= 15;
	final static public int SESSION_LENGTH_LOWER_LIMIT= 2;

//	enum Strings {
//		DOMAIN_EVENTFUL(Constants.EVENTFUL), DOMAIN_EVENTBRITE(Constants.EVENTBRITE), DOMAIN_UPCOMING(Constants.UPCOMING);
//		   private final String stringValue;
//		   private Strings(final String s) { stringValue = s; }
//		   public String toString() {
//			   System.out.println("stringValue= " + stringValue);
//			   return stringValue; }
//		}

}
