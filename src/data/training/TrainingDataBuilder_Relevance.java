package data.training;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;
import metrics.similarity.TFIDF;

import org.apache.log4j.Logger;

import svm.features.FeatureVectorBuilder;
import triple.parser.Quad;
import util.Constants;
import util.FileUtils;

public class TrainingDataBuilder_Relevance {//extends TrainingDataBuilder
	final static Logger logger = Logger.getLogger(TrainingDataBuilder_Relevance.class);

	static Map<String, List<Log>> sessions;// = new HashMap<String, List<Log>>();
	static KnowledgeBase KNOWLEDGE_BASE;
	static float threshold_relevance=0;
	List<String> unique_urls;
	
	public TrainingDataBuilder_Relevance(){
		sessions = new HashMap<String, List<Log>>();
		
		KNOWLEDGE_BASE = new KnowledgeBase();
		KNOWLEDGE_BASE.readKNOWLEDGE_BASE();
	}

	public void run(String filename) {
		sessions.putAll(LogParser.readEventSessionsFromFile(filename));
		logger.info("*** Total Nr. Sessions " + sessions.size());
	}
	
	protected static void saveDatasetToFile(String filename, List<String> lines)
			throws IOException {
		
		System.out.println("**SAVE DATA to: " + filename + "********************************");
		Writer out = new OutputStreamWriter(new FileOutputStream(filename));
		for (String line : lines) {
			out.write(line);
		}
		out.close();
	}
	
	private Map<Pair, String> readLabeledPairs(String labels_filename) {
		String filename = Constants.LABELDATA_FOLDER+labels_filename;
		Map<Pair, String> labeled_pairs = new HashMap<Pair, String>();
		
		DataInputStream input1;
		String strLine="";
		String[] fields;
		List<Pair> pairs = new  ArrayList<Pair>();
		List<Quad> url_quads ;
		unique_urls = new ArrayList<String>();

		String url1="", url2="",label="";
		Resource res1, res2;
		Pair pair;
		
		int i=1;
		logger.info("Reading file: "+filename);
		try {
			input1 = new DataInputStream(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(input1));
			
			while ((strLine = br.readLine()) != null)   {
				fields = strLine.split(";");
				if(fields.length==7){
					url1 = fields[1];
					url_quads = KNOWLEDGE_BASE.getResourceTriples(url1);
					res1 = new Resource(url1, url_quads);
					
					url2 = fields[2];
					url_quads = KNOWLEDGE_BASE.getResourceTriples(url2);
					res2 = new Resource(url2, url_quads);
					pair = new Pair(res1, res2);
					pairs.add(pair);
					
					//logger.info(" pair: " +url1+";"+url2);
					//label =  fields[3];
					String expected_label= fields[3];
					label = getLabelFromJudges(fields);
					//logger.info("\t label: " +label);
					labeled_pairs.put(pair, label);
					//logger.info("\t expected_label=" +expected_label+", judge_label="+label);
					
					if(url1.contains(url2) ||  url2.contains(url1)){
						//logger.info(i + " dup: " +url1+", "+url2);
						i++;
					}
					if(!unique_urls.contains(url1)){
						unique_urls.add(url1);
						//logger.info("unique_url: \n" + url1);
					}
					if(!unique_urls.contains(url2)){
						unique_urls.add(url2);
						//logger.info("unique_url: \n" + url2);
					}
					

				}else{
					logger.info("Warning: readLabeledDataset incorrect file");
				}
			}
			input1.close();
			
			//loadHTLMPages(unique_urls);
			//getDuplicates(pairs);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("unique_urls: \n" + unique_urls);
		return labeled_pairs;
	}
	
	private static String getLabelFromJudges(String[] fields){
		int freq_zero=0, freq_one=0;
		String label ="", judge="", labels_all="";
		for(int i=4; i<fields.length; i++){
			judge = fields[i];
			//logger.info("\t judge: " + judge);
			labels_all+=judge +",";
			if(judge.equals("0")){
				freq_zero++;	
			}
			if(judge.equals("1")){
				freq_one++;	
			}
		}
		
		//logger.info("\t labels_all: " + labels_all);
		
//		logger.info("\t freq_zero: " + freq_zero);
//		logger.info("\t freq_one: " + freq_one);
		if(freq_zero>freq_one){
			label="0";
		}else {
			label="1";
		}
		//logger.info("\t final judge: " + label);
		return label;
	}
	
	private void getDuplicates(List<Pair> unique_pairs){
		logger.info("*** getDuplicates ***");
		for(Pair pair: unique_pairs){
			if(pair.getResource1().getTitle().equals(pair.getResource2().getTitle()) ){//&& pair.getResource1().getType().equals(pair.getResource2().getType()
				logger.info("\t: " + pair.getURL1() +"("+pair.getResource1().getType()+")"+", "+ pair.getURL2() +"("+pair.getResource1().getType()+")");
			}
		}
	}
	
	private void loadHTLMPages(List<String> urls){
		logger.info("LOAD html-pages for total " + urls.size()+" urls");
		/*
		 * First, check which URLs are already downloaded
		 */
		File html_folder = new File(Constants.PATH+"html-pages");
		String downloaded_url="";
		List<String> downloaded_urls = new ArrayList<String>();
		for(File html_file: html_folder.listFiles()){
			Vector<String> html_content = new FileUtils().fileToVector(html_file.getAbsolutePath());
			downloaded_url=html_content.get(0).replace("<url>", "").replace("</url>", "");
			logger.info("downloaded_url: " + downloaded_url);
			downloaded_urls.add(downloaded_url);
		}
		
		logger.info("Total downloaded_urls: " + downloaded_urls.size());
		
		List<String> new_urls = new ArrayList<String>();
		for(String url: urls){
			if(!downloaded_urls.contains(url)){
				new_urls.add(url);
				logger.info("new_url: " + url);
			}
		}
		
		logger.info("Total new_urls: " + new_urls.size());
		int id=343;
		for(int i=0; i<new_urls.size(); i++){
			String new_url = new_urls.get(i);
			String filename = Constants.PATH+"html-pages2//page-id-"+id;//+url.replace("http://", "").replace("/", "_").replace(":", "");
			id++;
			try {
				logger.info("get file nr " + i + "page-id-"+id);
				saveDatasetToFile(filename, getURLContent(new URL(new_url)));
			} catch (MalformedURLException e) {
				logger.info("MalformedURLException ");
				e.printStackTrace();
			} catch (IOException e) {
				logger.info("IOException ");
				e.printStackTrace();
			}
		}
		
	}
	public static CharSequence getURLContent(URL url) throws IOException {
		logger.info("getURLContent " + url.toString());
		  URLConnection conn = url.openConnection();
		  String encoding = conn.getContentEncoding();
		  if (encoding == null) {
		    encoding = "ISO-8859-1";
		  }
		  BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
		  StringBuilder sb = new StringBuilder(16384);
		  try {
		    String line="<url>"+url+"</url>\n";
		    sb.append(line);
		    
		    while ((line = br.readLine()) != null) {
		      sb.append(line);
		      sb.append('\n');
		    }
		  } finally {
		    br.close();
		  }
		  
		//logger.info("sb " + sb.toString());

		return sb;
	}
	
	protected static void saveDatasetToFile(String filename, CharSequence content)
			throws IOException {
		
		System.out.println("**SAVE DATA to: " + filename + "********************************");
		Writer out = new OutputStreamWriter(new FileOutputStream(filename));
		out.write(content.toString());
		out.close();
	}

	
	/*
	 * Exclude files located in the testing_dir
	 */
	public void runDir (String logs_dir_str, String labels_filename){
		logger.info("training_dir logs: "+ logs_dir_str);
		
		File training_dir = new File(logs_dir_str);
		List<String> training_filenames = new ArrayList<String>();
		if(training_dir.isDirectory()){
			File[] training_files = training_dir.listFiles();
			for(File file: training_files){
				training_filenames.add(file.getPath());
			}
		}
		
		logger.info("Total: " + training_filenames.size() + " logs-training files");
		String training_file="";
		for(int i=1; i<training_filenames.size() ; i++){
			training_file = training_filenames.get(i);
			logger.info("File " + training_file+ " (" + i +" out of "+ training_filenames.size() +")");
			/*
			 * Sessions are read here
			 */
			run(training_file);
		}
		
		/*
		 * Labeled pairs of resources are generated here
		 */
		Map<Pair, String> labeled_pairs = readLabeledPairs(labels_filename);
		
		/*
		 * Features and respective values are created here
		 * Two files are produced:
		 * 1- file with the full string pairs of item (needed for evaluation later)
		 * 2- file containing same pairs as the first one, but in the SVM format
		 */
		List<String> text_lines = generateLines(labeled_pairs);
		boolean COLLAB_SAME_SESSION = true;
		boolean COLLAB_SIMILAR_USERS=false;
		boolean COLLAB_SIM_CONDITIONAL = true;
		boolean COLLAB_ORD=false;
		
		boolean CONTENT_SYNTACTIC_SIMILARITY=true;

		boolean CONTENT_SAME_TYPE=false;
		boolean CONTENT_SHARE_RELATION=false;
		boolean CONTENT_SEMANTIC_SIMILARITY=false;

		FeatureVectorBuilder feature_vector_builder = 
				new FeatureVectorBuilder(sessions, labeled_pairs, KNOWLEDGE_BASE, 
				COLLAB_SAME_SESSION, COLLAB_SIMILAR_USERS, COLLAB_SIM_CONDITIONAL, COLLAB_ORD,
				CONTENT_SAME_TYPE, CONTENT_SHARE_RELATION,
				CONTENT_SYNTACTIC_SIMILARITY, CONTENT_SEMANTIC_SIMILARITY);
		List<String> feature_vectors = feature_vector_builder.generateFeatureVectors();
		
		String filename_with_feature="";
		if(COLLAB_SAME_SESSION){
			filename_with_feature += "samesession_";
		}
		if(COLLAB_SIM_CONDITIONAL){
			filename_with_feature += "simcond_";
		}
		if(COLLAB_ORD){
			filename_with_feature += "ord_";
		}
		if(CONTENT_SYNTACTIC_SIMILARITY){
			filename_with_feature += "syntactic_";
		}
		if(CONTENT_SAME_TYPE){
			filename_with_feature += "type_";
		}
		if(CONTENT_SHARE_RELATION){
			filename_with_feature += "relation_";
		}
		if(CONTENT_SEMANTIC_SIMILARITY){
			filename_with_feature += "semantic_";
		}
		if(!CONTENT_SAME_TYPE && !CONTENT_SHARE_RELATION && !CONTENT_SYNTACTIC_SIMILARITY && !CONTENT_SEMANTIC_SIMILARITY){
			filename_with_feature += "nocontent_";
		}
		if(!CONTENT_SAME_TYPE && !CONTENT_SHARE_RELATION && !CONTENT_SEMANTIC_SIMILARITY){
			filename_with_feature += "nosemantic_";
		}
		
		Date now = new Date();
	    String saveToFile = Constants.TRAININGDATA_RELEVANCE_FOLDER+"trainingset-labeled-"+filename_with_feature+now.hashCode();
		String saveToSVMFullFile = saveToFile+"_svm";
		try {
			saveDatasetToFile(saveToFile, text_lines);
			saveDatasetToFile(saveToSVMFullFile, feature_vectors);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> generateLines(Map<Pair, String> labeled_pairs){
		/*
		 * <item1>URL1 \t <wam:type>Type \t <wam:attr1>Attr1 \t <item2>URL2 \t <wam:type>Type … <label>REL_VL(0/1) 
		 */
		List<String> lines = new ArrayList<String>();
		String line = "";
		
		for(Pair pair: labeled_pairs.keySet()){
			line = "<item1>"+pair.getURL1() + "\t<item2>"+pair.getURL2() +
					//"\t<predicted_label>"+ +"\t"+
					"\t<label>"+labeled_pairs.get(pair)+"\n";
			lines.add(line);
		}
		return lines;
	}
	
	public static void main(String[] args) {
		TrainingDataBuilder_Relevance builder = new TrainingDataBuilder_Relevance();
		Date t1 = new Date();
		logger.info("Start building training dataset " +  t1.toLocaleString() );
		String labels_filename="labeled_set_1230.csv";
		builder.runDir(Constants.EVENTLOGS_FOLDER, labels_filename);

		Date t2 = new Date();
        long diff = t2.getTime() - t1.getTime();
        long diffSeconds = diff / 1000 % 60;  
        long diffMinutes = diff / (60 * 1000) % 60; 
		logger.info("Ended building training dataset " + t2.toLocaleString() + ", duration:" +diffMinutes+"min, "+ diffSeconds+ " secs");
	
	}

}
