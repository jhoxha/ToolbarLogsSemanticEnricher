package metrics.similarity;

import org.apache.log4j.Logger;

public class Cosine {
	final static Logger logger = Logger.getLogger(Cosine.class);

    /**
     * Calculate the Dot Product
     * (inner product) of two vectors
     * @param vectorOne Vector
     * @param vectorTwo Vector
     * @return Dot Product
     * @throws VectorMathException Thrown if vectors are not of equal length
     */
    public static double dotProduct(double[] vectorOne, double[] vectorTwo) throws Exception {
     
       if(vectorOne.length != vectorTwo.length){ throw new Exception(
             "Input Vectors do not have the" + "same number of dimensions.");
       }
      
       double dotProduct = 0;
       for(int i = 0; i < vectorOne.length; i++){
          dotProduct += (vectorOne[i] * vectorTwo[i]);
//   	   	logger.info("vectorOne[i]: "+ vectorOne[i]);
//   	   	logger.info("vectorTwo[i]: "+ vectorTwo[i]);

       }

//  	   	logger.info("dotProduct: "+ dotProduct);
       return dotProduct;
    }
    
    /**
     * Calculate the Magnitude of a vector
     * @param vector Vector
     * @return Magnitude of the Vector
     */
    public static double magnitude(double[] vector){
       double magnitude = 0;
       for(int i = 0; i < vector.length; i++){
          magnitude += Math.pow(vector[i], 2);
       }
       return Math.sqrt(magnitude);
    }
    
    /**
     * Calculate the similarity of two strings using Cosine Similarity
     * @param stringOne First input string
     * @param stringTwo Second input string
     * @return cosine of the two angles (percentage of similarity)
     */
    public double cosine(double[] vec1, double[] vec2) {
       double dotProduct = 0;
       double cosine = 0;
     
       try {
          dotProduct = dotProduct( vec1, vec2);
       } catch (Exception e){
          e.printStackTrace();
          return -2;
       }
       //logger.info("dotProduct: "+ dotProduct);
       
       double vectorOneMagnitude = magnitude(vec1);
       //logger.info("vectorOneMagnitude: "+ vectorOneMagnitude);
       double vectorTwoMagnitude = magnitude(vec2);
       //logger.info("vectorTwoMagnitude: "+ vectorTwoMagnitude);

       cosine = dotProduct / (vectorOneMagnitude * vectorTwoMagnitude);
       //logger.info("cosine: "+ cosine);
       return cosine;
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
