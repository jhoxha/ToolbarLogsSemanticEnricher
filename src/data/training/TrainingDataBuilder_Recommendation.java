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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;
import metadata.annotator.Pair;
import metadata.annotator.Resource;

import org.apache.log4j.Logger;

import svm.features.FeatureVectorBuilder;
import triple.parser.Quad;
import util.Constants;

public class TrainingDataBuilder_Recommendation {//extends TrainingDataBuilder
	final static Logger logger = Logger.getLogger(TrainingDataBuilder_Recommendation.class);

	static Map<String, List<Log>> sessions;// = new HashMap<String, List<Log>>();
	static KnowledgeBase KNOWLEDGE_BASE;
	static float threshold_relevance=0;
	List<String> unique_urls;
	
	public TrainingDataBuilder_Recommendation(){
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
	
	private List<String> readTestFoldURLS(String filename){
		DataInputStream input1;
		String strLine="";
		List<String> url_list= new ArrayList<String>();

		logger.info("Reading TestFoldURLS : "+filename);
		try {
			input1 = new DataInputStream(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(new InputStreamReader(input1));
			while ((strLine = br.readLine()) != null)   {
				if(!url_list.contains(strLine)){
					url_list.add(strLine);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return url_list;
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("unique_urls:" + unique_urls.size());
		return labeled_pairs;
	}
	
	private static String getLabelFromJudges(String[] fields){
		int freq_zero=0, freq_one=0;
		String label ="", judge="";
		for(int i=4; i<fields.length; i++){
			judge = fields[i];
			if(judge.equals("0")){
				freq_zero++;	
			}
			if(judge.equals("1")){
				freq_one++;	
			}
		}
		if(freq_zero>freq_one){
			label="-1";
		}else {
			label="+1";
		}
		return label;
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
//		logger.info("UNIQUE URLS:");
//		for(String unique_url: unique_urls){
//			logger.info(unique_url);
//		}
//		logger.info("END UNIQUE URLS:");
		
		/*
		 * Now read the urls of the one TEST FOLD dataset,
		 * Do NOT construct feature vectors for pairs that contain any of these urls
		 */
		Map<Pair, String> training_labeled_pairs = new HashMap<Pair, String>();
		String TestFold_filename=Constants.PATH+"eval-recommendation/"+"R_s5.txt";
		List<String> urls_test=readTestFoldURLS(TestFold_filename);
		for(Pair pair: labeled_pairs.keySet()){
			if(!urls_test.contains(pair.getURL1()) && !urls_test.contains(pair.getURL2())){
				training_labeled_pairs.put(pair, labeled_pairs.get(pair));
				logger.info("training pair: " + pair.getURL1() + ", " + pair.getURL2() + ", l=" + labeled_pairs.get(pair));
			}
		}
		logger.info("TOTAL training pairs: " + training_labeled_pairs.size());
		
		/*
		 * Now, construct feature vectors only for these training pairs
		 * Features and respective values are created here
		 * Two files are produced:
		 * 1- file with the full string pairs of item (needed for evaluation later)
		 * 2- file containing same pairs as the first one, but in the SVM format
		 */
		List<String> text_lines = generateLines(training_labeled_pairs);
		boolean COLLAB_SAME_SESSION = true;
		boolean COLLAB_SIM_CONDITIONAL = true;
		boolean CONTENT_SAME_TYPE=true;
		boolean CONTENT_SHARE_RELATION=true;
		boolean CONTENT_SEMANTIC_SIMILARITY=true;

		boolean COLLAB_SIMILAR_USERS=false;
		boolean CONTENT_SYNTACTIC_SIMILARITY=false;
		
		FeatureVectorBuilder feature_vector_builder = 
				new FeatureVectorBuilder(sessions, training_labeled_pairs, KNOWLEDGE_BASE, 
				COLLAB_SAME_SESSION, COLLAB_SIMILAR_USERS, COLLAB_SIM_CONDITIONAL,
				CONTENT_SAME_TYPE, CONTENT_SHARE_RELATION,
				CONTENT_SYNTACTIC_SIMILARITY, CONTENT_SEMANTIC_SIMILARITY);
		List<String> feature_vectors = feature_vector_builder.generateFeatureVectors();
		
		String filename_with_feature="Dt5-suadeo";
		
		Date now = new Date();
	    String saveToFile = Constants.PATH+"eval-recommendation/"+"trainingset-"+filename_with_feature+now.hashCode();
		String saveToSVMFullFile = saveToFile+"_libsvm";
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
		TrainingDataBuilder_Recommendation builder = new TrainingDataBuilder_Recommendation();
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
