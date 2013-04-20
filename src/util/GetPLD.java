package util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.WeakHashMap;

import com.google.common.net.InternetDomainName;

/** Get the pay-level domain or effective top-level domain,
 * 
 * @link http://en.wikipedia.org/wiki/Public_Suffix_List
 * 
 * @author pmika
 *
 */
public class GetPLD {

	private static WeakHashMap<String, String> cache = new WeakHashMap<String, String>();
	
	public static String computePLD(String str) throws URISyntaxException {
		URI uri;
		
		if (str.startsWith("<") && str.endsWith(">")) {
			uri = new URI(str.substring(1, str.length() - 1)); 
		} else {
			uri = new URI(str);
		}
		
		String host = uri.getHost();
		if (host != null){
			String name = cache.get(host);
			if (name == null) {
				try {
					InternetDomainName idn = InternetDomainName.from(host);
					name = idn.topPrivateDomain().name();
				} catch (java.lang.IllegalStateException e) {
					name = ("NOT_UNDER_A_PUBLIC_SUFFIX");
				} catch (java.lang.IllegalArgumentException e) {
					name = ("NOT_A_DOMAIN_NAME");
				}
				cache.put(host, name);
			}
			return name;
		} else {
			return("NULL_PLD");
		}
	}


	/** Test only
	 * @param args
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws URISyntaxException {
		String test = "http://www.news.corp.yahoo.co.uk/madonna#1";
		System.out.println(computePLD(test));
		

	}

}
