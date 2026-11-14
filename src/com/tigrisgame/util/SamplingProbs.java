/* File: SamplingProbs.java
 * Description:  Probability utility for AI.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE
 * 152 is the starting number of tiles in tigris.
 * the numbers used in the calculation scale with (totalNum)^(numDrawn)
 * with X, 56, 152 (which calculates the probability for reds) the rounding errors
 * become to great at X = 32 or 33 depending on w/ or w/o replacement
 */

package com.tigrisgame.util;

public class SamplingProbs
{
    public static final double MAX_ERROR = 0.01;

    static boolean checkArgs( int numDrawn, int numMarked, int totalNum )
    {
	// check arguments validity
	if ( numDrawn < 0 || numMarked < 0 || totalNum < 0 ) {
	    System.out.println("ERROR: SamplingProbs: Invalid arguments, cannot be negative");
	    return false;
	}
	if ( numDrawn > totalNum ) {
	    System.out.println("ERROR: SamplingProbs: Cannot draw more than totalNum");
	    return false;
	}
	if ( numMarked > totalNum ) {
	    System.out.println("ERROR: SamplingProbs: Cannot mark more than totalNum");
	    return false;
	}
	return true;
    }

    static boolean checkArgs( int numDrawn, int numMarkA,
			      int drawnA, int numMarkB, int totalNum )
    {
	if ( numDrawn < 0 || 
	     numMarkA < 0 || drawnA  < 0 ||
	     numMarkB < 0 || totalNum < 0 ) {
	    System.out.println("ERROR: SamplingProbs: Invalid arguments, cannot be negative");
	    return false;
	}
	if ( numDrawn > totalNum ) {
	    System.out.println("ERROR: SamplingProbs: Cannot draw more than totalNum");
	    return false;
	}
	if ( drawnA > numMarkA ) {
	    System.out.println("ERROR: SamplingProbs: Cannot reveal more A's than marked");
	    return false;
	}
	if ( numMarkA + numMarkB > totalNum ) {
	    System.out.println("ERROR: SamplingProbs: Cannot mark more than totalNum");
	    return false;
	}
	if ( drawnA > numDrawn ) {
	    System.out.println("ERROR: SamplingProbs: Cannot know more than numDrawn");
	    return false;
	}

	return true;
    }

    // the sum of the result probabilities must equal 1, so check
    // that it is with a certain epsilon
    static boolean checkResults( double [] results )
    {
	double sum = 0;
	for ( int ii=0; ii< results.length; ii++) {
	    sum += results[ii];
	}
	
	double error = Math.abs( sum - 1.0 );

	if (  error >= MAX_ERROR ) {
	    System.out.println("ERROR: SamplingProbs: Floating point rounding errors too great: " + error);
	    return false;
	}
	else {
	    //System.out.println("SamplingProbs: Check error is: " + error);
	    return true;
	}
    }

    public static double [] sampleWithReplacement( int numDrawn, int numMarked, int totalNum) 
    {	
	if ( ! checkArgs( numDrawn, numMarked, totalNum ))
	    return null;
	
	BinomialCo binCo = getCoefficients( numDrawn );
	double [] resultProbs  = new double[ numDrawn + 1 ];

	double markedProb = (double) numMarked / totalNum;
	double otherProb  = (double) (totalNum - numMarked) / totalNum;

	for ( int xx=0; xx <= numDrawn; xx ++ ) {
	    
	    resultProbs[xx] = 
		Math.pow( markedProb, xx ) *
		Math.pow( otherProb, numDrawn - xx ) *
		binCo.getCoeff( xx, numDrawn );
	}

	if ( checkResults( resultProbs) )
	    return resultProbs;
	else 
	    return null;
    }

    // the array returned contains the probability for drawing the index number of marked things
    // the array will be size numDrawn+1.
    public static double [] sampleNoReplacement( int numDrawn, int numMarked, int totalNum )
    {
	if ( ! checkArgs( numDrawn, numMarked, totalNum ))
	    return null;

	BinomialCo binCo = getCoefficients( numDrawn );
	double [] resultProbs  = new double[ numDrawn + 1 ];

	double denom = 1;
	double [] prodMarked = new double[ numDrawn + 1];
	double [] prodOther  = new double[ numDrawn + 1];

	// calculate the denominator and
	// products of T(T-1)(T-2)... and M(M-1)(M-2)...
	// okay to go negative since passing zero will zero out the product.
	for ( int ii=0; ii <= numDrawn; ii++ ) {
	    if ( ii != numDrawn )
		denom *= totalNum - ii;
	    if ( ii == 0 ) {
		prodMarked[ii] = 1;
		prodOther[ii]  = 1;
	    } else {
		prodMarked[ii] = prodMarked[ii-1] * (numMarked-ii+1);
		prodOther[ii]  = prodOther[ii-1]  * (totalNum-numMarked-ii+1);
	    }
	}

	// calculate the products for numerator for each number of marked draws.
	for ( int xx=0; xx <= numDrawn; xx++ ) {

	    resultProbs[xx] = 
		prodMarked[ xx ] * 
		prodOther[ numDrawn - xx ] *
		binCo.getCoeff( xx, numDrawn ) / 
		denom;
	}
	
	if ( checkResults( resultProbs ) )
	    return resultProbs;
	else 
	    return null;
    }

    // IMPLEMENTATION 1.
    // a more advanced probability calculation that the first sampleNoReplacement.
    // numMarkA and revealedA note minimum number of "A" things drawn.
    // the conditional probability of drawing from things marked "B" is then returned.
    // if A and B are the same pool, the results from first version of sampleNoReplacment can be
    // easily modified to give the conditional probability.
    public static double [] sampleNoReplacement( int numDrawn, int numMarkA, 
						 int revealedA, int numMarkB, int totalNum )
    {
	if ( ! checkArgs( numDrawn, numMarkA, revealedA, numMarkB, totalNum ))
	    return null;
	
	BinomialCo binCo = getCoefficients( numDrawn );
	double [] resultProbs  = new double[ numDrawn + 1 ];

	double denom = 1;
	double [] prodMarkA = new double[numDrawn + 1];
	double [] prodMarkB = new double[numDrawn + 1];
	double [] prodOther = new double[numDrawn + 1];
	double probAGTRevealed = 0; 
	int numOther = totalNum - numMarkA - numMarkB;
	
	// calculate the probability that >= revealedA of numMarkedA were drawn.
	// depending on numRevealed, this call may be 1.0 or the simpler
	// version of this method may be used. (but not checked for now)
	double [] probAOnly = sampleNoReplacement( numDrawn, numMarkA, totalNum );
	for ( int ii = revealedA; ii <= numDrawn; ii ++ )
	    probAGTRevealed += probAOnly[ii];

	// calculate the denominator and
	// products of T(T-1)(T-2)... and M(M-1)(M-2)...
	// okay to go negative since passing zero will zero out the product.
	for ( int ii=0; ii <= numDrawn; ii++ ) {
	    if ( ii != numDrawn )
		denom *= totalNum - ii;
	    if ( ii == 0 ) {
		prodMarkA[ii] = 1;
		prodMarkB[ii] = 1;
		prodOther[ii] = 1;
	    } else {
		prodMarkA[ii] = prodMarkA[ii-1] * (numMarkA-ii+1);
		prodMarkB[ii] = prodMarkB[ii-1] * (numMarkB-ii+1);
		prodOther[ii]  = prodOther[ii-1]  * (numOther-ii+1);
	    }
	}

	// calculate the products for numerator for each number of marked draws.
	// xx is from markA and yy is from markB
	for ( int xx=revealedA; xx <= numDrawn; xx++ )
	    // calculate the products for numerator for each number of marked draws from mark.
	    {
		for ( int yy=0; yy <= (numDrawn - xx); yy++ ) {
		    
		    resultProbs[ yy ] += 
			prodMarkA[ xx ] *
			prodMarkB[ yy ] *
			prodOther[ numDrawn - xx - yy ] *
			binCo.getCoeff( xx, numDrawn ) *
			binCo.getCoeff( yy, (numDrawn - xx) ) / 
			denom / 
			probAGTRevealed;
		}
		
		
	}

	if ( checkResults( resultProbs ) )
	    return resultProbs;
	else 
	    return null;
    }

    // get a handle to an approriately sized array of coefficients.
    static BinomialCo getCoefficients( int size )
    {
	BinomialCo binCo;

	if ( size <= 8 )
	    binCo = BinomialCo.BIN_8;
	else if ( BinomialCo.BIN_X != null &&
		  BinomialCo.BIN_X.getMaxDegree() >= size )
	    binCo = BinomialCo.BIN_X;
	else
	    binCo = BinomialCo.BIN_X = new BinomialCo( size );

	return binCo;
    }
}

class BinomialCo
{
    // convenience member for small binomial coefficients
    public static final BinomialCo BIN_8 = new BinomialCo( 8 );
    // member for storing globally the largest size coefficients
    // that is worth keeping in memory.
    // the size of the data is O(n^2).
    public static BinomialCo BIN_X = null;

    // the coefficients indexed by [row][column]
    private int [][] coeffs;

    // initialize a binomial coefficients
    public BinomialCo( int degree )
    {
	this.coeffs = new int[degree+1][degree+1];

	for ( int rr=0; rr<degree+1; rr++ )
	    for ( int cc=0; cc<=rr; cc++) {
		if ( cc == 0 )
		    coeffs[rr][cc] = 1;
		else if ( cc == rr )
		    coeffs[rr][cc] = 1;
		else
		    coeffs[rr][cc] = coeffs[rr-1][cc-1] + coeffs[rr-1][cc];
	    }
    }

    // returns the coefficient of x^index of polynomial (x+y)^degree
    public int getCoeff( int index, int degree )
    {
	return coeffs[ degree ][ index ];
    }

    // THIS IS ALSO CURRENTLY IN CRAIGAI.

//     // n choose k
//     // the binomail coefficient of x^k y^(n-k) from (x+y)^n
//     public static int binomialCoefficient( int n, int k )
//     {
// 	int coeff = factoral( n ) / factoral( k ) / factoral( n - k );
// 	//System.out.println( n + " choose " + k + " is " + coeff);
// 	return coeff;
//     }
    
//     // simple factoral function.
//     public static int factoral( int x )
//     {
// 	int fact = 1;
// 	if ( x == 0 )
// 	    return 1;
// 	while ( x > 1 )
// 	    {
// 		fact *= x;
// 		x --;
// 	    }
// 	return fact;
//     }

    public int getMaxDegree()
    {
	return coeffs.length - 1;
    }

    // prints out pascals triangle.  :)
    // int the future we could be fancy and center the top
    // by finding out the maximum number of characters in the largest number
    // and justifying, but not now.
    public String toString()
    {
	StringBuffer strbuf = new StringBuffer();
	for ( int rr=0; rr<coeffs.length; rr++) { 
	    for ( int cc=0; cc<=rr; cc++ ) {
		strbuf.append( coeffs[rr][cc] );
		if ( rr != cc )
		    strbuf.append(" ");
	    }
	    if ( rr != coeffs.length )
		strbuf.append("\n");
	}
	return strbuf.toString();
    }
}
