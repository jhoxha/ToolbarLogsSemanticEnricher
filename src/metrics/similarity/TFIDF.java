package metrics.similarity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import metadata.annotator.KnowledgeBase;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import triple.parser.Quad;
import util.Constants;

public class TFIDF {
	final static Logger logger = Logger.getLogger(TFIDF.class);
	String FIELD_URI = "URI";
	String FIELD_PATH = "path";
	String FIELD_CONTENT_METADATA = "content_metadata";
	String FIELD_CONTENT_HTML = "content_html";
	Directory index_directory;
	IndexReader reader;
	
	Vector<Map<String,Double>> all_vectors;
	Map<String, Integer> uri_docnum_map = new HashMap<String, Integer>();
	String DIRECTORY_HTML_FILES_TO_INDEX=Constants.PATH+"html-pages";
	
	public TFIDF(KnowledgeBase KNOWLEDGE_BASE){
		try {
			index_directory = FSDirectory.open(new File(Constants.INDEX_DIRECTORY));
			createIndex(KNOWLEDGE_BASE);
			readIndexDirectory();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public TFIDF(KnowledgeBase KNOWLEDGE_BASE, List<String> urls){
		try {
			index_directory = FSDirectory.open(new File(Constants.INDEX_DIRECTORY));
			createIndex(KNOWLEDGE_BASE, urls);

			readIndexDirectory();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		logger.info("Ended creating the index directory");
	}
	
	public TFIDF(){
		try {
			index_directory = FSDirectory.open(new File(Constants.INDEX_DIRECTORY));
			//readIndexDirectory();
			
			//computeTFIDFs(FIELD_CONTENT_HTML);
			readTFIDFVectors();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public void createIndex(KnowledgeBase KNOWLEDGE_BASE) throws CorruptIndexException, LockObtainFailedException, IOException {
		logger.info("Creating Index ...");
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_34, analyzer);
		IndexWriter indexWriter = new IndexWriter(index_directory, config);
		
		File dir = new File(DIRECTORY_HTML_FILES_TO_INDEX);
		File[] files = dir.listFiles();
		
		for (int i=0; i< 10; i++) {//
			
			File file=files[i];
			Document document = new Document();

			String path = file.getCanonicalPath();
			//logger.info(FIELD_PATH + " = " + path);
			document.add(new Field(FIELD_PATH, path, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

			DataInputStream input1 = new DataInputStream(new FileInputStream(file));
			BufferedReader br = new BufferedReader(new InputStreamReader(input1));
			String strLine="", uri="", content_html ="";
			uri = br.readLine().replace("<url>", "").replace("</url>", "");
			while ((strLine=br.readLine()) != null)   {
				content_html += strLine; 
			}
			
			String content_metadata="";
			for(Quad quad: KNOWLEDGE_BASE.getKNOWLEDGE_BASE()){
				if( quad.subject.equals(uri) || quad.context.equals(uri)) {
					content_metadata += " "+quad.object;
				}
			}

			//logger.info("\t "+FIELD_URI + " = " + uri);
			document.add(new Field(FIELD_URI, uri, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			document.add(new Field(FIELD_CONTENT_HTML, content_html, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			document.add(new Field(FIELD_CONTENT_METADATA, content_metadata, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

			indexWriter.addDocument(document);
		}
		
		indexWriter.optimize();
		indexWriter.close();
	}
	
	public void createIndex(KnowledgeBase KNOWLEDGE_BASE, List<String> urls) throws CorruptIndexException, LockObtainFailedException, IOException {
		logger.info("Creating Index ...");
		 logger.info("urls size : " + urls.size());
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_34, analyzer);
		IndexWriter indexWriter = new IndexWriter(index_directory, config);
		
		List<Quad> quads;
		int nr_url=1;
		for(String url: urls){
			logger.info("URL " +nr_url+ "out of " + urls.size());
			nr_url++;
			
			String content_metadata="";
			for(Quad quad: KNOWLEDGE_BASE.getKNOWLEDGE_BASE()){
				if( quad.subject.equals(url) || quad.context.equals(url)) {
					content_metadata += " "+quad.object;
				}
			}
			
			Document document = new Document();
			document.add(new Field(FIELD_PATH, "", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			logger.info("\t "+FIELD_URI + " = " + url);
			document.add(new Field(FIELD_URI, url, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
			document.add(new Field(FIELD_CONTENT_METADATA, content_metadata, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

			indexWriter.addDocument(document);
		}
		
		indexWriter.optimize();
		indexWriter.close();
	}
	
	private void readIndexDirectory(){
		logger.info("Reading index directory ");
		try {
			reader = IndexReader.open(index_directory);
		    logger.info("reader numDocs: " + reader.numDocs());
		    
		    Document adoc= reader.document(0);
		    logger.info("adoc path: " + adoc.get(FIELD_PATH));
		    logger.info("adoc uri: " + adoc.get(FIELD_URI));
		    logger.info("adoc content_metadata: " + adoc.get(FIELD_CONTENT_METADATA));
		    logger.info("adoc content_html: " + adoc.get(FIELD_CONTENT_HTML));
		    
		    adoc= reader.document(1);
		    logger.info("adoc path: " + adoc.get(FIELD_PATH));
		    logger.info("adoc uri: " + adoc.get(FIELD_URI));
		    logger.info("adoc content_metadata: " + adoc.get(FIELD_CONTENT_METADATA));
		    logger.info("adoc content_html: " + adoc.get(FIELD_CONTENT_HTML));

		    adoc= reader.document(2);
		    logger.info("adoc path: " + adoc.get(FIELD_PATH));
		    logger.info("adoc uri: " + adoc.get(FIELD_URI));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("uri_docnum_map size" + uri_docnum_map.size());
		
	}
		
	private void computeTFIDFs(String field) throws IOException{
    	String[] fieldNames = new String[1];
    	fieldNames[0] = field;
    	
    	Map<String,Integer> totalTfv = getTermFrequencyMap(reader, fieldNames, new HashSet<String>());
    	int max_freq=0;
    	for(String term: totalTfv.keySet()){
    		//logger.info("term-freq: " + term +", "+totalTfv.get(term));
    		if(totalTfv.get(term)>max_freq){
    			max_freq=totalTfv.get(term);
    		}
    	}
    	
    	logger.info("\n max_freq: " + max_freq);
    	
     	all_vectors = new Vector<Map<String,Double>>();
     	uri_docnum_map = new HashMap<String, Integer>();
     	String uri ="";
     	
    	Map<String,Double> doc_vec= new HashMap<String,Double>();
    	Term term = new Term("");
    	double tf=0, idf=0, tf_idf =0;
    	double c_w_d=0, n_w=0;
    	int N = reader.maxDoc();
    
    	FileOutputStream fout;
    	ObjectOutputStream oos;
    	for (int docNum=0; docNum<reader.maxDoc(); docNum++) {//docNum<reader.maxDoc()
    		 doc_vec = new HashMap<String,Double>();
    		 
    		 logger.info(" docNum "+ docNum + " out of " + reader.maxDoc());
    		 //logger.info(" FIELD_CONTENT "+ field);
    		 
    		 TermFreqVector vec_doc = reader.getTermFreqVector(docNum, field);
    		 String[] doc_terms = vec_doc.getTerms(); //get the terms
    		 int[] doc_terms_freqs = vec_doc.getTermFrequencies();
             //logger.info("doc_terms_freqs: " + doc_terms_freqs.length);
    		 
    		 for(String word: totalTfv.keySet()){
    			 c_w_d=0;
    			 //logger.info("\n ");
    			 //logger.info("\t this is the  word: " + word);
    			for(int i=0;i<doc_terms.length;i++){
    				String doc_term=doc_terms[i];
    				
    				if(doc_term.equals(word)){
    					//logger.info("\t doc_term: " + doc_term);
    					c_w_d = doc_terms_freqs[i];
    				}
    			} 
    			
    			//logger.info("\t c_w_d: " + c_w_d);
    			 tf = c_w_d / max_freq;
    			 
    			 //logger.info("\t max_freq: " + max_freq );
    			 //logger.info("\t tf: " + tf );
    			 
    			 term = new Term(field, word);
    			 //logger.info("\t this is the term: " + term);

    			 n_w = reader.docFreq(term);
    			 idf = (float) Math.log10(N/n_w);
    			 tf_idf = tf * idf;
    			 //logger.info("\t N: " + N +", n_w: " + n_w );
    			 //logger.info("\t idf= " + idf+ ", tf_idf=" + tf_idf );
    			 doc_vec.put(word, tf_idf);
    			 if(tf_idf>0){
            		 //tfidf_vector_asStr += "(" + word + ", " + tf_idf + ") ,  ";
    			 }
    			 //logger.info("\t  put: (" + word+ ", " + tf_idf+")" );
    		 }

    		all_vectors.add(doc_vec);
    		 
 		    uri = reader.document(docNum).get(FIELD_URI);
		    uri_docnum_map.put(uri, docNum);
		    
	    	try {
	    	      fout = new FileOutputStream(Constants.PATH+"tf-idf-vectors//"+"vector-"+docNum);
	    	      oos = new ObjectOutputStream(fout);
	    	      oos.writeObject(doc_vec);
	    	      oos.close();
	    	 }  catch (Exception e) { e.printStackTrace(); }
   	     
    	 }
    	
    	logger.info("Serializing TF-IDF vectors");
    	try {
    	      fout = new FileOutputStream(Constants.PATH+"tf-idf-vectors//"+"uri_docnum_map");
    	      oos = new ObjectOutputStream(fout);
    	      oos.writeObject(uri_docnum_map);
    	      oos.close();
    	 }  catch (Exception e) { e.printStackTrace(); }
	}
	
	
	public void readTFIDFVectors(){
	   	 /*
	   	  * First read the uri_docnum_map and TF-IDF Vectors
	   	  * which we have serialized beforehand in directory /tf-idf-vectors
	   	  */
		File vector_folder = new File(Constants.PATH+"tf-idf-vectors//");
		Map<String, Double> vector;
		try {
	   	    FileInputStream fin = new FileInputStream(Constants.PATH+"tf-idf-vectors//"+"uri_docnum_map");
	   	    ObjectInputStream ois = new ObjectInputStream(fin);
	   	    uri_docnum_map = (Map<String, Integer>) ois.readObject();
	   	    ois.close();
	   	}  catch (Exception e) { e.printStackTrace(); }
	 	//logger.info("uri_docnum_map size " + uri_docnum_map.size());

	}

	public double[] getTFIDFVectorStored(String URI){
		double[] vec = null;
		if(!uri_docnum_map.containsKey(URI)){
			logger.info("NO MAP for URI; "+URI);
			return new double[0]; 
		}
	  	 int doc_index=uri_docnum_map.get(URI);
		 //logger.info("\t doc_index: " + doc_index +" for url " + URI); 

		FileInputStream fin;
		try {
			fin = new FileInputStream(Constants.PATH+"tf-idf-vectors//"+"vector-"+doc_index);
	  		 ObjectInputStream ois = new ObjectInputStream(fin);
	  		 Map<String, Double> vector = (Map<String, Double>) ois.readObject();

	  		 int t=0;
	  		 vec = new double[vector.size()] ;
	  		 String vect_str="";
	  		 for(String aterm:vector.keySet()){
	  			 vec[t] = vector.get(aterm);
	  			 if(vec[t]>0){
	  				vect_str += "(" + aterm + ", " + vec[t] + ") ,  ";
	  			 }
	  			 t++;
	  		 }
	  		 //logger.info("\t v: " + vect_str); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
  		return vec;
	}

	private void addDoc(IndexWriter w, String value) throws IOException {
	    Document doc = new Document();
	    doc.add(new Field("title", value, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
	    w.addDocument(doc);
	  }
	
	/**
	 * Sums the term frequency vector of each document into a single term frequency map
	 * @param indexReader the index reader, the document numbers are specific to this reader
	 * @param docNumbers document numbers to retrieve frequency vectors from
	 * @param fieldNames field names to retrieve frequency vectors from
	 * @param stopWords terms to ignore
	 * @return a map of each term to its frequency
	 * @throws IOException
	 */
	private Map<String,Integer> getTermFrequencyMap(IndexReader indexReader, String[] fieldNames, Set<String> stopWords)
	throws IOException {
	    Map<String,Integer> totalTfv = new HashMap<String,Integer>();//1024

	    for (int docNum=0; docNum<indexReader.maxDoc(); docNum++) {//Integer docNum : indexReader.maxDoc()
	    	//logger.info("docNum: " +docNum);
	        for (String fieldName : fieldNames) {
	            TermFreqVector tfv = indexReader.getTermFreqVector(docNum, fieldName);
	            if (tfv == null) {
	            	logger.info("\t null: ");
	                // ignore empty fields
	                continue;
	            }
	            

	            String terms[] = tfv.getTerms();
	            int termCount = terms.length;
	            int freqs[] = tfv.getTermFrequencies();

	            for (int t=0; t < termCount; t++) {
	                String term = terms[t];
	                int freq = freqs[t];

	                // filter out single-letter words and stop words
	                if (StringUtils.length(term) < 2 ||
	                    stopWords.contains(term)) {
	                    continue; // stop
	                }

	                Integer totalFreq = totalTfv.get(term);
	                totalFreq = (totalFreq == null) ? freq : freq + totalFreq;
	                totalTfv.put(term, totalFreq);
	            }
	        }
	    }

	    return totalTfv;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		KnowledgeBase KNOWLEDGE_BASE = new KnowledgeBase();
//		KNOWLEDGE_BASE.readKNOWLEDGE_BASE();
//		TFIDF tf_idf_scheme = new TFIDF(KNOWLEDGE_BASE);
		
		TFIDF tf_idf_scheme = new TFIDF();
		String url1="http://www.youtube.com/watch?v=C_0PnbdDhTQ&feature=youtube_gdata_player";
		String url2="http://eventful.com/performers/wale-/P0-001-000111777-7";
		String url3="http://eventful.com/performers/black-sabbath-/P0-001-000004500-2";
		
		double[] vec1 = tf_idf_scheme.getTFIDFVectorStored(url1);
		double[] vec2 = tf_idf_scheme.getTFIDFVectorStored(url2);
		double[] vec3 = tf_idf_scheme.getTFIDFVectorStored(url3);
		logger.info("cosine v1-v2:" + new Cosine().cosine(vec1, vec2));

		logger.info("cosine v2-v3:" + new Cosine().cosine(vec2, vec3));
	}

}
