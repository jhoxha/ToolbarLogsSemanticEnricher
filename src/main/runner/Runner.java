package main.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import triple.parser.Quad;
import util.Constants;

import log.parser.LogParser;
import metadata.annotator.KnowledgeBase;

public class Runner {
	final static Logger logger = Logger.getLogger(Runner.class);

	public static void parseLogs(String logsDirectory){
		logger.info("logsDirectory: "+ logsDirectory);
		File dir = new File(logsDirectory);
		int i=1;
		
		if(dir.isDirectory()){
			File[] files = dir.listFiles();
			for(File file: files){
				if(file.isFile() ){//&& i==1
					logger.info("File " + file+ " (" + i +" out of "+ files.length+")");
					LogParser parser = new LogParser();
					parser.readFileAndStoreEventLogs(file.getPath()); //+"list-toolbar-events.pig-"+date+"//"
				}
				i++;	
			}
		}
	}
	
	public void constructKnowledgebaseFromLogs(){
		new KnowledgeBase().constructKB(Constants.EVENTLOGS_FOLDER);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		new Runner().parseLogs(Constants.LOGS_FOLDER);
//		new Runner().parseLogs("C://SemanticRecommender//data//toolbar-events//");

		new Runner().constructKnowledgebaseFromLogs();
	}

}
