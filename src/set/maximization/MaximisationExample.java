/*
*	    Class MaximisationExample
*
*       An example of the use of the class Maximisation
*       and the interface MaximisationFunction
*
*       Finds the maximum of the function
*           z = a + x^2 + 3y^4
*       where a is constant
*       (an easily solved function has been chosen
*       for clarity and easy checking)
*
* 	    WRITTEN BY: Michael Thomas Flanagan
*
*	    DATE:   29 December 2005
*
*       PERMISSION TO COPY:
* 	    Permission to use, copy and modify this software and its documentation
*	    for NON-COMMERCIAL purposes and without fee is hereby granted provided
*	    that an acknowledgement to the author, Michael Thomas Flanagan, and the
*	    disclaimer below, appears in all copies.
*
* 	    The author makes no representations or warranties about the suitability
*       or fitness of the software for any or for a particular purpose.
*       The author shall not be liable for any damages suffered as a result of
*       using, modifying or distributing this software or its derivatives.
*
**********************************************************/

package set.maximization;

import flanagan.math.*;

// Class to evaluate the function z = a + -(x-1)^2 - 3(y+1)^4
// where a is fixed and the values of x and y
// (x[0] and x[1] in this method) are the
// current values in the maximisation method.
class MaximFunct implements MaximisationFunction{

    private double a = 0.0D;

    // evaluation function
    public double function(double[] x){
        //double z = a - (x[0]-1.0D)*(x[0]-1.0D) - 3.0D*Math.pow((x[1]+1.0D), 4);
    	double z = a*x[0] + (a-1)*x[1];
        return z;
    }

    // Method to set a
    public void setA(double a){
        this.a = a;
    }
}

// Class to demonstrate maximisation method, Maximisation nelderMead
public class MaximisationExample{

    public static void main(String[] args){

        //Create instance of Maximisation
        Maximisation max = new Maximisation();

        // Create instace of class holding function to be maximised
        MaximFunct funct = new MaximFunct();

        // Set value of the constant a to 5
        funct.setA(0.5D);

        // initial estimates
        double[] start = {1.0D, 0.8D};

        // initial step sizes
        double[] step = {0.1D, 0.2D};

        // convergence tolerance
        double ftol = 1e-15;

        // Nelder and Mead maximisation procedure
        max.nelderMead(funct, start, step, ftol);

        // get the maximum value
        double maximum = max.getMaximum();

        // get values of y and z at maximum
         double[] param = max.getParamValues();

        // Print results to a text file
        max.print("MaximExampleOutput.txt");

        // Output the results to screen
        System.out.println("Maximum = " + max.getMaximum());
        System.out.println("Value of x at the maximum = " + param[0]);
        //System.out.println("Value of y at the maximum = " + param[1]);


    }
}
