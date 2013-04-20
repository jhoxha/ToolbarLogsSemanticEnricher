package triple.parser;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class TripleNxParser {
	public List<Triple> parse(String n3s){
		List<Triple> triples = new ArrayList<Triple>();
		
		Triple triple = null;
		// convert String into InputStream
		NxParser nxp;
		try {
			if(n3s !=null ){
				InputStream is = new ByteArrayInputStream(n3s.getBytes());
				nxp = new NxParser(is,false);
				  while (nxp.hasNext()) {
					    Node[] nodes = nxp.next();
						triple = new Triple(nodes[0].toString(),nodes[1].toString().replace("\n", "").replace("\t", "") ,nodes[2].toString());
						triples.add(triple);
					  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return triples;
	}
	
	private void parseFile() throws FileNotFoundException, IOException{
		NxParser nxp = new NxParser(new FileInputStream("data//"+"crawls/part-m-04973"),false);
		  while (nxp.hasNext()) {
		    Node[] ns = nxp.next();
					
		    for (Node n: ns) {
		      System.out.print(n.toN3());
		      
		      System.out.print(" ");
		    }
		    System.out.println(".");
		  }
		
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String n3s = MetadataEvalFunc.exec("http://tailgate13-eorg.eventbrite.com/");
		List<Triple> triples = new TripleNxParser().parse(n3s);
		for(Triple t: triples){
		    //System.out.println((nodes[0].toString()+ "\t" + nodes[1].toString().replace("\n", "").replace("\t", "") +"\t" + nodes[2].toString()) );
			System.out.println(t.print());
		}
	}

}
