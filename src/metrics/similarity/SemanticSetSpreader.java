package metrics.similarity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import metadata.annotator.KnowledgeBase;
import metadata.annotator.Resource;
import metrics.similarity.SpreadedVector.TermWeightPair;

import org.apache.log4j.Logger;

import triple.parser.Quad;
import util.Constants;

public class SemanticSetSpreader {
	final static Logger logger = Logger.getLogger(SemanticSetSpreader.class);
	Resource  resource;
	
	SpreadedVector spreaded_vector;
	double initial_weight=1.0;
	
	Map<String, Double> relation_weights_Table; //for each relation there is a weight
	static KnowledgeBase KNOWLEDGE_BASE;
	
	public SemanticSetSpreader(KnowledgeBase myKNOWLEDGE_BASE){
		KNOWLEDGE_BASE = myKNOWLEDGE_BASE;
		
		initializeRelation_weights_Table();
		initial_weight=1.0;
	}
	
	public SpreadedVector getSpreadedVector(Resource resource){
		//logger.info("getSpreadedVector:" + resource.getURI());
		spreaded_vector = new SpreadedVector();

		List<TermWeightPair> original_set = new ArrayList<TermWeightPair>();
		original_set.add(new TermWeightPair(resource.getURI(), resource.getTitle(), initial_weight, "", true));
		//logger.info("original_set: <" + resource.getURI() + ", " + resource.getTitle() + ", " + initial_weight+">");
		
		spread(1, original_set);
		
		//logger.info(spreaded_vector.print());
		SpreadedVector normalized_spreaded_vector = getNormalizedVector(spreaded_vector);
		
		return normalized_spreaded_vector;
	}
	
	public void spread(int iteration, List<TermWeightPair>  original_set){
		List<TermWeightPair> iteration_spreaded_set = new ArrayList<TermWeightPair>();
		Vector<List<TermWeightPair>> vec_set = new Vector<List<TermWeightPair>>();
		List<TermWeightPair> resource_spreaded_set = new ArrayList<TermWeightPair>();
		//logger.info("iter:" + iteration);
		
		/*
		 * add next related term_weight pair 
		 * based on the relation_weights_Table
		 * and on the KNOWLEDGE_BASE and ONTOLOGY (hierarchy)
		 */
		
		for(TermWeightPair pair: original_set){
			resource_spreaded_set = getTermSpreadedSet(pair);
			if(!resource_spreaded_set.isEmpty()){
				vec_set.add(resource_spreaded_set);
				iteration_spreaded_set.addAll(resource_spreaded_set);
				//logger.info("vec_set ADD");
			}
		}

		
		if(!iteration_spreaded_set.isEmpty()){
			for(List<TermWeightPair> set: vec_set){
				//update spreaded_vector
				spreaded_vector.appendSpreadedSet(set,iteration);
				//logger.info("append spreaded_vector - iter " + iteration);
			}

			iteration++;
			//recursive call
			spread(iteration, iteration_spreaded_set);	
		}
	
	}
	
	private List<TermWeightPair> getTermSpreadedSet(TermWeightPair term_weight_pair){
		List<TermWeightPair> term_spreaded_set = new ArrayList<TermWeightPair>();
		Vector<List<TermWeightPair>> vector_lists = new Vector<List<TermWeightPair>>();
		
		String relatedresource_URI="";
		Map<String, String> related_resource = new HashMap<String,String>();
		double weight=0;

		//logger.info("getTermSpreadedSet for : (" + term_weight_pair.resourceURI+" , "+term_weight_pair.term+" , "+term_weight_pair.relation+")");
		
		//term_weight_pair.term, term_weight_pair.weight
		for(String relation: relation_weights_Table.keySet()){
			weight = relation_weights_Table.get(relation);
			related_resource = getRelatedResource(term_weight_pair.resourceURI, relation); //subject, predicate
			
			for(String resourceTitle: related_resource.keySet()){
				if(!related_resource.get(resourceTitle).isEmpty()){ //if it is a URI
					relatedresource_URI = related_resource.get(resourceTitle);
					term_spreaded_set.add(new TermWeightPair(relatedresource_URI, resourceTitle, weight, relation, true));
					//logger.info("\t add relatedRes:" + relatedresource_URI+" , "+resourceTitle + " ("+relation+")");
				} else {
					relatedresource_URI ="";
					term_spreaded_set.add(new TermWeightPair("", resourceTitle, weight, relation, false));
					//logger.info("\t add relatedTerm:" + relatedresource_URI+" , "+resourceTitle+ " ("+relation+")");
				}
			}
			
		}

		return term_spreaded_set;
	}

	/*
	 * Some utility functions
	 */
	public Map<String, String>  getRelatedResource(String resourceURI, String predicate) {
		Map<String, String> resource_uri_title = new HashMap<String, String>();
		String related_resource="";
		Resource resource = null;
		
		for(Quad quad: KNOWLEDGE_BASE.getKNOWLEDGE_BASE()){
			if(quad.subject.equals(resourceURI) && quad.predicate.equals(predicate)){ 
				related_resource = quad.object; //this is URI or a literal
			}
		}
		
		if(!related_resource.isEmpty()){
			resource = new Resource(related_resource, KNOWLEDGE_BASE.getResourceTriples(related_resource));

			if(resource.isResource()){
				resource_uri_title.put(resource.getTitle(),resource.getURI());
			} else {
				resource_uri_title.put(related_resource, "");
			}
		}
		

		return resource_uri_title;
	}
	
	private void initializeRelation_weights_Table(){
		relation_weights_Table = new HashMap<String, Double>();
		//relation_weights_Table.put("wam:domain", 1.0);
		//relation_weights_Table.put(Constants.NS_RDF+"subClassOf", 0.8);
		relation_weights_Table.put("wam:type", 1.0);
		relation_weights_Table.put(Constants.NS_ICAL+"categories", 0.8);
		
		relation_weights_Table.put(Constants.NS_OG+"title", 1.0);
		relation_weights_Table.put(Constants.NS_DC+"title", 1.0);
		relation_weights_Table.put(Constants.NS_OG+"type", 1.0);

		relation_weights_Table.put(Constants.NS_SCHEMA_ORG+"addressLocality", 0.5);
		relation_weights_Table.put(Constants.NS_SCHEMA_ORG+"addressRegion", 0.5);

		relation_weights_Table.put(Constants.NS_OG+"locality", 0.5);
		relation_weights_Table.put(Constants.NS_OG+"region", 0.5);

		relation_weights_Table.put(Constants.NS_VCARD+"locality", 0.5);
		relation_weights_Table.put(Constants.NS_VCARD+"region", 0.5);

		relation_weights_Table.put("wam:hasVenue", 0.5);
		relation_weights_Table.put("wam:hasPerformer", 0.5);
		relation_weights_Table.put("wam:hasVideo", 0.5);
		relation_weights_Table.put("wam:performerName", 0.5);
	}
	
	private SpreadedVector getNormalizedVector(SpreadedVector vector){
		SpreadedVector normalized_vec = new SpreadedVector();
		List<TermWeightPair> normalized_iter_set = new ArrayList<TermWeightPair>();
		boolean occurs=false;
		int iter=0;
		
		for(List<TermWeightPair> iter_spreaded_set: vector.getSpreaded_vector().keySet()){
			iter = vector.getSpreaded_vector().get(iter_spreaded_set);
			normalized_iter_set = new ArrayList<TermWeightPair>();
			for(String relation: relation_weights_Table.keySet()){
				occurs=false;
				for(TermWeightPair apair: iter_spreaded_set){
					if(apair.relation.equals(relation)){
						normalized_iter_set.add(apair);
						occurs=true;
					}
				}
				if(!occurs){
					TermWeightPair null_pair_for_relation= new TermWeightPair("","",0,relation, false);
					normalized_iter_set.add(null_pair_for_relation);
				}
			}
			
			Collections.sort(normalized_iter_set, new RelationComparator());
			normalized_vec.appendSpreadedSet(normalized_iter_set, iter);
		}

		
		return normalized_vec;
	}
	
	public class RelationComparator implements Comparator<TermWeightPair> {
		String r1="", r2="";
		    @Override
		    public int compare(TermWeightPair p1, TermWeightPair p2) {
		    	r1 = p1.relation;
		    	r2 = p2.relation;
		
		        return  r1.compareTo(r2);
		    }
		}
	
	public static void main(String[] args) {
		KNOWLEDGE_BASE = new KnowledgeBase();
		KNOWLEDGE_BASE.readKNOWLEDGE_BASE();
		
		SemanticSetSpreader set_spreader= new SemanticSetSpreader(KNOWLEDGE_BASE);
		String resourceURI="http://eventful.com/topeka/events/candye-kane-/E0-001-046627183-2";
		SpreadedVector vec1 = set_spreader.getSpreadedVector(new Resource(resourceURI, KNOWLEDGE_BASE.getResourceTriples(resourceURI)));
		logger.info("*** VECTOR1 ****");
		logger.info(vec1.print());
		
		logger.info("\n\n");
		resourceURI="http://upcoming.yahoo.com/event/8820909/MD/Baltimore/Fresh-Music-Festival-Keith-Sweat-Doug-E-Fresh-K-Ci-and-JoJo/1st-Mariner-Arena/";
		SpreadedVector vec2 = set_spreader.getSpreadedVector(new Resource(resourceURI, KNOWLEDGE_BASE.getResourceTriples(resourceURI)));
			logger.info("*** VECTOR2 ****");
		logger.info(vec2.print());
		
		
		logger.info("\n\n");
		resourceURI="http://urlsm2.eventbrite.com/";
		SpreadedVector vec3 = set_spreader.getSpreadedVector(new Resource(resourceURI, KNOWLEDGE_BASE.getResourceTriples(resourceURI)));
		logger.info("*** VECTOR3 ****");
		logger.info(vec3.print());
		
		logger.info("relations size " + set_spreader.relation_weights_Table.size());
		
		logger.info("sim (vec1, vec2): " + new SetSpreadingSimilarity().similarity(vec1, vec2));
		logger.info("sim (vec1, vec3): " + new SetSpreadingSimilarity().similarity(vec1, vec3));

	}

}
