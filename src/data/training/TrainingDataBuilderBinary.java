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

public class TrainingDataBuilderBinary {//extends TrainingDataBuilder
	final static Logger logger = Logger.getLogger(TrainingDataBuilderBinary.class);

	static Map<String, List<Log>> sessions;// = new HashMap<String, List<Log>>();
	static KnowledgeBase KNOWLEDGE_BASE;
	static float threshold_relevance=0;
	List<String> unique_urls;
	
	public TrainingDataBuilderBinary(){
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
	
	private Map<Pair, String> readLabeledPairs() {
		String filename = Constants.LABELDATA_FOLDER+"labeled_set_1230.csv";
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
					
					logger.info(" pair: " +url1+";"+url2);
					//label =  fields[3];
					String expected_label= fields[3];
					label = getLabelFromJudges(fields);
					//logger.info("\t label: " +label);
					labeled_pairs.put(pair, label);
					logger.info("\t expected_label=" +expected_label+", judge_label="+label);
					
					if(url1.contains(url2) ||  url2.contains(url1)){
						//logger.info(i + " dup: " +url1+", "+url2);
						i++;
					}
					if(!unique_urls.contains(url1)){
						unique_urls.add(url1);
						logger.info("unique_url: \n" + url1);
					}
					if(!unique_urls.contains(url2)){
						unique_urls.add(url2);
						logger.info("unique_url: \n" + url2);
					}
					

				}else{
					logger.info("Warning: readLabeledDataset incorrect file");
				}
			}
			input1.close();
			
			//loadHTLMPages(unique_urls);
			//getDuplicates(pairs);
			
			TFIDF tf_idf_scheme = new TFIDF(KNOWLEDGE_BASE, unique_urls);
			
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
		String label ="", judge="";
		for(int i=4; i<fields.length; i++){
			judge = fields[i];
			logger.info("\t judge: " + judge);
			if(judge.equals("0")){
				freq_zero++;	
			}
			if(judge.equals("1")){
				freq_one++;	
			}
		}
		
//		logger.info("\t freq_zero: " + freq_zero);
//		logger.info("\t freq_one: " + freq_one);
		if(freq_zero>freq_one){
			label="0";
		}else {
			label="1";
		}
		logger.info("\t final judge: " + label);
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
		for(int i=0; i<urls.size(); i++){
			String url = urls.get(i);
			String filename = Constants.PATH+"html-pages//page-id-"+i;//+url.replace("http://", "").replace("/", "_").replace(":", "");
			try {
				logger.info("get file nr " + i);
				saveDatasetToFile(filename, getURLContent(new URL(url)));
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
	public void runDir (String training_dir_str, String testing_dir_str){
		logger.info("training_dir logs: "+ training_dir_str);
		File testing_dir = new File(testing_dir_str);
		List<String> testing_filenames = new ArrayList<String>();
		if(testing_dir.isDirectory()){
			File[] testing_files = testing_dir.listFiles();
			for(File file: testing_files){
				if(file.isFile()){
					testing_filenames.add(file.getName());
				}
			}
		}
		
		File training_dir = new File(training_dir_str);
		List<String> training_filenames = new ArrayList<String>();
		if(training_dir.isDirectory()){
			File[] training_files = training_dir.listFiles();
			for(File file: training_files){
				/*
				 * Exclude Testing files from the Training Dataset
				 */
				
				if(file.isFile() && !testing_filenames.contains(file.getName())){
					training_filenames.add(file.getPath());
				}
			}
		}
		
		logger.info("Total: " + training_filenames.size() + " training files");
		String training_file="";
		for(int i=1; i<training_filenames.size() ; i++){//training_filenames.size()
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
		Map<Pair, String> labeled_pairs = readLabeledPairs();
		
		/*
		 * Features and respective values are created here
		 * Two files are produced:
		 * 1- file with the full string pairs of item (needed for evaluation later)
		 * 2- file containing same pairs as the first one, but in the SVM format
		 */
		List<String> text_lines = generateLines(labeled_pairs);
		boolean COLLAB_SAME_SESSION = true;
		boolean COLLAB_SIM_CONDITIONAL = false;
		boolean COLLAB_SIMILAR_USERS=false;
		
		boolean CONTENT_SYNTACTIC_SIMILARITY=false;

		boolean CONTENT_SAME_TYPE=false;
		boolean CONTENT_SHARE_RELATION=false;
		boolean CONTENT_SEMANTIC_SIMILARITY=false;

		FeatureVectorBuilder feature_vector_builder = 
				new FeatureVectorBuilder(sessions, labeled_pairs, KNOWLEDGE_BASE, 
				COLLAB_SAME_SESSION, COLLAB_SIMILAR_USERS, COLLAB_SIM_CONDITIONAL,
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
		if(CONTENT_SYNTACTIC_SIMILARITY){
			filename_with_feature += "syntactic_";
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
		
//		Date now = new Date();
//	    String saveToFile = Constants.TRAININGDATA_FOLDER+"trainingset-labeled-"+filename_with_feature+now.hashCode();
//		String saveToSVMFullFile = saveToFile+"_svm";
//		try {
//			saveDatasetToFile(saveToFile, text_lines);
//			saveDatasetToFile(saveToSVMFullFile, feature_vectors);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	    
//	    /*
//	     * For SVM training split dataset, in 3 different subsets
//	     */
//		int nrLists = 3;
//	    List<List<String>> lists = split(feature_vectors, nrLists);
//	    logger.info("list1: "+lists.get(0).size());
//	    logger.info("list2: "+lists.get(1).size());
//	    logger.info("list3: "+lists.get(2).size());
//	    
//	    String path = Constants.TRAININGDATA_FOLDER;
//	    String saveToSVMFile = "trainingset-labeled-"+filename_with_feature+now.hashCode()+"_svm";
//	    generateSVMFiles (path, saveToSVMFile, lists, now);
	}
	
	private void generateSVMFiles(String path, String saveToSVMFile, List<List<String>> lists, Date now){
		List<String> lines = new ArrayList<String>();
		String line = "";
		
		/*
		 * First fold
		 */
		String train_filename = saveToSVMFile+"-train1";
		String test_filename = saveToSVMFile+"-test1";
    	List<String> f1_f2= new ArrayList<String>();
    	f1_f2.addAll(lists.get(0)); f1_f2.addAll(lists.get(1));
    	List<String> f3 = new ArrayList<String>();
    	f3.addAll(lists.get(2));
		logger.info("train_filename: "+ train_filename);
		logger.info("test_filename: "+ test_filename);
    	
		try {
			saveDatasetToFile(path+train_filename, f1_f2);
			saveDatasetToFile(path+test_filename, f3);
			line = "./svm_perf_learn -c 20.0 dataset-logs/"+train_filename+" dataset-logs/model1\n";
			lines.add(line);
			line = "./svm_perf_classify dataset-logs/"+test_filename+" dataset-logs/model1 dataset-logs/predictions1\n";
			lines.add(line);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * Second fold
		 */		
		train_filename = saveToSVMFile+"-train2";
		test_filename = saveToSVMFile+"-test2";
    	List<String> f1_f3= new ArrayList<String>();
    	f1_f3.addAll(lists.get(0)); f1_f3.addAll(lists.get(2));
    	List<String> f2 = new ArrayList<String>();
    	f2.addAll(lists.get(1));
		logger.info("train_filename: "+ train_filename);
		logger.info("test_filename: "+ test_filename);
		try {
			saveDatasetToFile(path+train_filename, f1_f3);
			saveDatasetToFile(path+test_filename, f2);
			
			line = "./svm_perf_learn -c 20.0 dataset-logs/"+train_filename+" dataset-logs/model2\n";
			lines.add(line);
			line = "./svm_perf_classify dataset-logs/"+test_filename+" dataset-logs/model2 dataset-logs/predictions2\n";
			lines.add(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		/*
		 * Third fold
		 */	
		train_filename = saveToSVMFile+"-train3";
		test_filename = saveToSVMFile+"-test3";
    	List<String> f2_f3  = new ArrayList<String>();
    	f2_f3.addAll(lists.get(1)); f2_f3.addAll(lists.get(2));
    	List<String> f1  = new ArrayList<String>();
    	f1.addAll(lists.get(0));
		logger.info("train_filename: "+ train_filename);
		logger.info("test_filename: "+ test_filename);
		try {
			saveDatasetToFile(path+train_filename, f2_f3);
			saveDatasetToFile(path+test_filename, f1);
			
			line = "./svm_perf_learn -c 20.0 dataset-logs/"+train_filename+" dataset-logs/model3\n";
			lines.add(line);
			line = "./svm_perf_classify dataset-logs/"+test_filename+" dataset-logs/model3 dataset-logs/predictions3\n";
			lines.add(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("script lines: " + lines);
		
	    String svm_script_name = Constants.TRAININGDATA_FOLDER+ "svm-script" +now.hashCode();
		try {
			saveDatasetToFile(svm_script_name, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static <T extends Object> List<List<T>> split(List<T> list, int nrLists) {
		logger.info("total list size = " + list.size());
		int sublist_size= (int) Math.floor(list.size() / nrLists);
		//int last_list_size = list.size() - (sublist_size * nrLists);
		int start=0;
	    List<List<T>> lists = new ArrayList<List<T>>();
	    for (int i = 1; i <= nrLists-1; i++) {
	    	logger.info("sublist " + i);
	    	List<T> sublist = list.subList(start, start+sublist_size);
	        lists.add(sublist);
	        start += sublist_size;
	        logger.info("\t sublist size = " + sublist.size());
	    }
	    
	    logger.info("list " + nrLists);
	    List<T> last_list = list.subList(start, list.size());
        lists.add(last_list);
        logger.info("\t last_list size = " + last_list.size());
        
	    return lists;
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
		TrainingDataBuilderBinary builder = new TrainingDataBuilderBinary();
		Date t1 = new Date();
		logger.info("Start building training dataset " +  t1.toLocaleString() );
		builder.runDir(Constants.EVENTLOGS_FOLDER, Constants.TESTINGDATA_LOGS_FOLDER);

		Date t2 = new Date();
        long diff = t2.getTime() - t1.getTime();
        long diffSeconds = diff / 1000 % 60;  
        long diffMinutes = diff / (60 * 1000) % 60; 
		logger.info("Ended building training dataset " + t2.toLocaleString() + ", duration:" +diffMinutes+"min, "+ diffSeconds+ " secs");
	
	}

}
