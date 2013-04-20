package log.parser;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;
import util.GetPLD;

public class Log {
	final static Logger logger = Logger.getLogger(Log.class);
	
	String sessId,guid, date, time, url, location;
	
	public Log(String sessId, String guid, String date, String time, String url, String location){
		this.sessId = sessId;
		this.guid = guid;
		this.date = date;
		this.time = time;
		this.url = url.trim();
		this.location = location;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getUrl() {
		return url.trim();
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSessId() {
		return sessId;
	}

	public void setSessId(String sessId) {
		this.sessId = sessId;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getLog(){
		String log = sessId + ",\t" +guid + ",\t"+date+",\t"+time+",\t"+url+",\t"+location+"";
		
		return log;
	}
	
	public String getHost() {
		String host="";
		try {
			host=GetPLD.computePLD(url);
		} catch (URISyntaxException e) {
			logger.info("HOST error parsing " + url);
			//e.printStackTrace();
		}
		return host;
	}

	public boolean containedIn(List<Log> logs_list) {
		boolean contained=false;
		for(Log log: logs_list){
			if(url.equals(log.url)){
				 contained=true;
			}
		}
		return contained;
	}
}
