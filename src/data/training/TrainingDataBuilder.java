package data.training;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import log.parser.Log;
import log.parser.LogParser;
import metadata.annotator.Pair;

import org.apache.log4j.Logger;

import util.Constants;

public class TrainingDataBuilder {
	final static Logger logger = Logger.getLogger(TrainingDataBuilder.class);
	public static final String DELIMITER_ITEM = "<item>";
	public static final String DELIMITER_RELEVANT = "<relevant>";
	
	static Map<String, List<Log>> sessions = new HashMap<String, List<Log>>();
	static List<String> uniqueURLs = new ArrayList<String>();
	static List<Pair> unique_pairs = new ArrayList<Pair>();

	public TrainingDataBuilder() {
		sessions = new HashMap<String, List<Log>>();
	}

	public void run(String filename) {
		logger.info("exec run");
		sessions = LogParser.readEventSessionsFromFile(filename);

		for (String sessid : sessions.keySet()) {
			for (Log log : sessions.get(sessid)) {
				if (!uniqueURLs.contains(log.getUrl()) ) {
					uniqueURLs.add(log.getUrl());
				}
			}
		}

		logger.info("Total uniqueURLs " + uniqueURLs.size());
		/*
		 * This is the main Part Creating each LINE of the Training Dataset
		 * file
		 */
		List<String> lines = new ArrayList<String>();
		List<String> labels = new ArrayList<String>();
		String line = "";
		int id = 1;

		for (String url : uniqueURLs) {
			logger.info("uniqueURL: "+ url);
			line ="";

			line = id + "\t"+DELIMITER_ITEM + url + "\t<wam:type>Type";
			line = line +DELIMITER_RELEVANT;
			labels=getLabelsForURL(url);
			for(String label: labels){
				//logger.info("\t label: "+ label);
				line = line+ label + "\t" + "\t<wam:type>Type , ";
			}
			
			line = line.substring(0, line.length()-2);
			line = line + "\n";
			id++;
			
			logger.info("\t line : "+ line);
			lines.add(line);
		}
		
		logger.info("\t line nr: "+ id);

		try {
			saveDatasetToFile(Constants.TRAININGDATA_FOLDER
					+ "eventlogs-"+filename.substring(filename.length()-8), lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> getLabelsForURL(String url){
		List<Log> labelsLogs = new ArrayList<Log>();
		for (String sessid : sessions.keySet()) {
			for (Log log : sessions.get(sessid)) {
				if (log.getUrl().equals(url) ) {
					labelsLogs.addAll(sessions.get(sessid));
					//labelsLogs.remove(log); //CHECK THIS
				}
			}			
		}
		
		List<String> labels = new ArrayList<String>();
		for(Log labelLog: labelsLogs){
			if(!labels.contains(labelLog.getUrl())){
				labels.add(labelLog.getUrl());
			}
		}
		
		labels.remove(url);
		return labels;
	}

	protected static void saveDatasetToFile(String filename, List<String> lines)
			throws IOException {
		System.out.println("**SAVE DATA to: " + filename
				+ "********************************");
		Writer out = new OutputStreamWriter(new FileOutputStream(filename));
		for (String line : lines) {
			out.write(line);
		}

		out.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = Constants.EVENTLOGS_FOLDER+"list-toolbar-events.pig-20120702";
		TrainingDataBuilder builder = new TrainingDataBuilder();
		builder.run(filename);
		
//		filename = Constants.EVENTLOGS_FOLDER+"list-toolbar-events.pig-20120806";
//		TrainingDataBuilder.run(filename);

	}

}
