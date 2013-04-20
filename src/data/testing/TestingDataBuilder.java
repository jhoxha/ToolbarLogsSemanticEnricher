package data.testing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.Pair;

import org.apache.log4j.Logger;

import data.training.TrainingDataBuilder;

import util.Constants;


public class TestingDataBuilder extends TrainingDataBuilder{
	final static Logger logger = Logger.getLogger(TestingDataBuilder.class);
	static Map<String, List<Log>> sessions = new HashMap<String, List<Log>>();
	static Map<String, List<Log>> test_sessions = new HashMap<String, List<Log>>();
	static List<Pair> testing_pairs = new ArrayList<Pair>();

	public void run(String filename) {
		sessions = LogParser.readEventSessionsFromFile(filename);
		List<String> lines = new ArrayList<String>();
		List<String> binary_lines = new ArrayList<String>();
		
		String line = "", binary_line="";
		int index=0;
		for (String sessid : sessions.keySet()) {
			line = sessid+"\t";
			if(sessions.get(sessid).size()>3){
				test_sessions.put(sessid,  sessions.get(sessid));
				for (Log log : sessions.get(sessid)) {
//					if(index==1){
//						
//						binary_lines.add(e);
//					}
					line += "<URL>"+log.getUrl();
				}			
				line += "\n";
				lines.add(line);
				logger.info("add " + line);
				index++;
			}
		}

		logger.info("Total lines " + lines.size());
//		Collections.shuffle(lines);
//		lines = lines.subList(0, lines.size());
		
		for (String sessid : test_sessions.keySet()) {
		
		}
		
		try {
			filename=filename.substring(filename.length()-8) + "-test";
			saveDatasetToFile(Constants.TESTINGDATA_LOGS_FOLDER
					+ "eventlogs-"+filename, lines);
			
			filename=filename.substring(filename.length()-8) + "svm-test";
			saveDatasetToFile(Constants.TESTINGDATA_LOGS_FOLDER
					+ "eventlogs-"+filename, binary_lines);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public void generateEvaluationsFile(String predictions_file, String method){
		List<String> lines = new ArrayList<String>();
		String url1="http://upcoming.yahoo.com/event/9035830/PA/Hershey/Mamma-Mia-National-Tour/Hershey-Theater/";
		String url2="http://upcoming.yahoo.com/venue/402185/PA/Hershey/Hershey-Theater/";
		String listid="";
		String line ="";
		String DELIMITER =",";
		for(int i=1; i<=10;i++){
			listid = ""+i;
			for(int r=1; r<=10; r++){
				line = method+ DELIMITER+listid +DELIMITER+ url1 +DELIMITER+ url2 +DELIMITER+ r + "\n";
				lines.add(line);
			}
		}
		
		
		try {
			saveDatasetToFile(predictions_file+"-evaluation", lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String filename = Constants.EVENTLOGS_FOLDER+"list-toolbar-events.pig-20120702_v02";
		//TestingDataBuilder.run(filename);
		
		String predictions_file = Constants.TESTINGDATA_FOLDER + "predictions-svm";
		new TestingDataBuilder().generateEvaluationsFile(predictions_file, "SVM");
	}

}
