package triple.parser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deri.any23.Any23;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.deri.any23.writer.NTriplesWriter;
import org.deri.any23.writer.TripleHandler;

import util.Constants;

public class MetadataEvalFunc {

	public static String exec(String input)  {

		Any23 runner = new Any23();
		runner.setHTTPUserAgent("test-user-agent");

		String uri = input;
		
		if (uri == null) {
			throw new IllegalArgumentException("URI was null");
		} else if (uri.length() == 0) {
			throw new IllegalArgumentException("URI was empty");
		}

		if (input == null )
			return null;
		try {
			HTTPClient httpClient = runner.getHTTPClient();

			DocumentSource source = new HTTPDocumentSource(httpClient, uri);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TripleHandler handler = new NTriplesWriter(out);

			runner.extract(source, handler);
			String n3 = out.toString("UTF-8");

			return n3;

		} catch (Exception e) {
			return null;

		}
	}
	
	public Map<String, String> scrapPage(String urlStr, String domain){
		/*
		 * Map<String URL, String Title>
		 */
		Map<String, String> entities = new HashMap<String, String>();
		URL url;
		Pattern patt;
		String a_link="", title="";
		try {
			url = new URL(urlStr);
			CharSequence htmlseq =getURLContent(url);
			if(domain.matches(Constants.EVENTFUL)){
				patt = Pattern.compile( "<li class=\"who-performer-id\"(.*?)</li>", Pattern.DOTALL | Pattern.UNIX_LINES);
				Matcher m = patt.matcher(htmlseq);
				while (m.find()) {
				  String li_block = m.group(1);
				  System.out.println("li_block: "+li_block );
				  
				  patt = Pattern.compile("<a class=\"goto_pdp\".*href=(.*?)title=", Pattern.DOTALL | Pattern.UNIX_LINES);// href=\"\"
				  Matcher matcher2 = patt.matcher(li_block);
				  while (matcher2.find()) {
					  a_link= matcher2.group(1).replace("\"", "").trim();
				  }
				  
				  patt = Pattern.compile("<span itemprop=\"name\">(.*?)</span>", Pattern.DOTALL | Pattern.UNIX_LINES);
				  matcher2 = patt.matcher(li_block);
				  while (matcher2.find()) {
					  title= matcher2.group(1).trim();
				  }
				  System.out.println("a_link: "+a_link + "title: "+title);
				  if(!a_link.isEmpty()){
					  entities.put(a_link, title);
				  }
				}			

			
			} else if(domain.matches(Constants.UPCOMING)){
				patt = Pattern.compile( "<div id=\"performer_list\" typeof=\"vcal:performers\">(.*?)</div>", Pattern.DOTALL | Pattern.UNIX_LINES);
				Matcher m = patt.matcher(htmlseq);
				while (m.find()) {
				  String div_block = m.group(1);
				  System.out.println("div_block: "+div_block );
				  
				  patt = Pattern.compile("<a  href=(.*?)><img title", Pattern.DOTALL | Pattern.UNIX_LINES);// href=\"\"
				  Matcher matcher2 = patt.matcher(div_block);
				  while (matcher2.find()) {
					  a_link= matcher2.group(1).replace("\"", "").trim();
				  }
				  
				  patt = Pattern.compile("<span><a  href=.*>(.*?)</a> </span>", Pattern.DOTALL | Pattern.UNIX_LINES);
				  matcher2 = patt.matcher(div_block);
				  while (matcher2.find()) {
					  title= matcher2.group(1).trim();
				  }
				  //System.out.println("a_link: "+a_link + "title: "+title);
				  if(!a_link.isEmpty()){
					  entities.put(a_link, title);
				  }
				}			
				
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return entities;
	}
	

	public static CharSequence getURLContent(URL url) throws IOException {
		  URLConnection conn = url.openConnection();
		  String encoding = conn.getContentEncoding();
		  if (encoding == null) {
		    encoding = "ISO-8859-1";
		  }
		  BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
		  StringBuilder sb = new StringBuilder(16384);
		  try {
		    String line;
		    while ((line = br.readLine()) != null) {
		      sb.append(line);
		      sb.append('\n');
		    }
		  } finally {
		    br.close();
		  }
		  return sb;
	}
	
	public static void main(String[] args) throws IOException{
		String input= "http://tailgate13-eorg.eventbrite.com/";
		//System.out.println("n3: \n" + exec(input));
		
		String page_url="http://eventful.com/grandhaven/events/tommee-profitt-/E0-001-049231224-4";
		String pattern ="";
		Map<String, String> performers = new HashMap<String, String>();
		//performers = new MetadataEvalFunc().scrapPage(page_url, "eventful.com");
		
		page_url="http://upcoming.yahoo.com/event/9124258/DC/Washington/Shanice/Blues-Alley/";
		performers.putAll(new MetadataEvalFunc().scrapPage(page_url, "upcoming.yahoo.com"));
		
		for(String performer_url: performers.keySet()){
			System.out.println("performer: " + performer_url+ ", " + performers.get(performer_url));
		}
	}

}
