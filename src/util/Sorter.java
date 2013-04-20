package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Sorter {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Map<String,Integer> unsorted_map = new HashMap<String,Integer>();
		ValueComparator bvc =  new ValueComparator(unsorted_map);
        TreeMap<String,Integer> sorted_map = new TreeMap(bvc);

		  System.out.println("Sorted Map......");
	      Map<String,String> sortedMap =  sortByComparator(unsorted_map);
	 
	      for (Map.Entry entry : sortedMap.entrySet()) {
	    	  System.out.println(entry.getKey() + " : " + entry.getValue());
	      }
	}

	private static Map sortByComparator(Map unsortMap) {

		List list = new LinkedList(unsortMap.entrySet());

		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		// put sorted list into map again
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	private static List sortByEntry(List unsortList, int orderField) {

		List list = new LinkedList(unsortList);

		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		// put sorted list into map again
		List sortedList = new ArrayList();
		for (Iterator it = list.iterator(); it.hasNext();) {
			sortedList.add(it.next());
		}
		
		return list;
	}	

}

class ValueComparator implements Comparator {

	Map base;

	public ValueComparator(Map base) {
		this.base = base;
	}

	@Override
	public int compare(Object arg0, Object arg1) {
		return 0;
	}
}
