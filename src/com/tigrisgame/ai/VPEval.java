/* File: VPEval.java
 * Description:  Evaluates score based on VP
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * returns a score based on victory points alone based on efficiency.
 * efficient points are given a value, then small waste, then large waste.
 * all of the values can be tuned as desired.
 * this is especially usefull since "wasted" vp are 
 * worth less later in the game.
 *
 * EFFICIENCY:
 * How well distributed your victory points are so
 * that as little as possible are "wasted".
 * Raw Sum (RS) -- add up the points in each sphere and the treasure.
 * Efficient Sum (ES) -- the Raw Sum minus the Waste.
 * If a player has all treasures, his Efficient sum is always equal his Raw Sum.
 * Waste (W) -- after distributing treasures, 
 * find the minimum sphere score and add one to it.
 * points greater than this number are waste.
 * Percent Efficieny (%E) -- the ratio of Efficient Sum and Raw Sum
 * For instance 3,4,4,4 AND 4,4,5,5 is perfect efficiency but 3,4,4,5 is NOT.
 * Calculated by finding your minimum sphere and adding one.
 * Any more tiles past this point in other spheres are W (wasted).
 * %E = ( RS - W ) / RS
 * Theorem: A player with a higher Efficient Sum beats 
 * a player with a lower Efficient Sum.
 *
 * ANOTEHR METHOD:
 * composite VP is a present evaluator of victory points.
 * it combines all vp's into a single number by weighting each
 * weakest sphere by a multiplier, in this case 100.
 * the evaluation will break down if vp's in a sphere ever
 * exceeds the multiplier.
 * int compositeVP = 
 * finalVP[3] + 
 * finalVP[2] * 100 + 
 * finalVP[1] * 100 * 100 + 
 * finalVP[0] * 100 * 100 * 100;
 */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.Sphere;
import com.tigrisgame.rules.TigrisGame;

public class VPEval implements GameEvaluator
{
    public static int verbosity = 0;

    // points for each efficient victory point.
    int efficientScore = 1000;
    // points for each "wasted" victory point up to threshold.
    int smallWasteScore = 500;
    // points for each "wasted" victory point past to the threshold.
    int largeWasteScore = 50;
    // the large/small waste threshold.
    int largeWasteThreshold = 10;
    // the bonus for having a treasure.
    int treasureBonus = 1000;

    public VPEval() {}

    public VPEval( int eff, int small, int large, int thresh, int treas )
    {
	efficientScore = eff;
	smallWasteScore = small;
	largeWasteScore = large;
	largeWasteThreshold = thresh;
	treasureBonus = treas;
    } 

    public void setEfficientScore( int value )
    {
	efficientScore = value;
    }

    public void setSmallWasteScore( int value )
    {
	smallWasteScore = value;
    }

    public void setLargeWasteScore( int value )
    {
	largeWasteScore = value;
    }

    public void setLargeWasteThreshold( int value )
    {
	largeWasteThreshold = value;
    }

    public void setTreasureBonus( int value )
    {
	treasureBonus = value;
    }

    // evalutes for all dynasties, returns an array of int scores.
    public int [] evaluate( TigrisGame game )
    {
	int [] scores = new int[ game.getNumPlayers() ];
	for ( int pp=0; pp<scores.length; pp++ ) {
	    scores[pp] = evaluate( game, pp );
	}
	return scores;
    }

    // evalutates for one dynasty, returns an int.
    // may or may not consider other dynasties to get relative score.
    public int evaluate( TigrisGame game, int dynasty )
    {
	Player player = game.getPlayer( dynasty );
	return evaluatePlayer( player );
    }

    // public method exposed for testing.
    // this should always evaluate (assuming waste < 100)
    // higher for a player who is actually winning.
    public int evaluatePlayer( Player player )
    {
	Sphere [] spheres = Sphere.getSpheres();
	int [] finalVps = player.getFinalVictoryPoints();

	// compute raw sum and min score.
	int rawSum = 0;
	int minScore = Integer.MAX_VALUE;
	for (int ii=0; ii<spheres.length; ii++) {
	    rawSum += finalVps[ii];
	    if ( finalVps[ii] < minScore )
		minScore = finalVps[ii];
	}

	if ( verbosity > 1 )
	    System.out.println( "rawSum? " + rawSum + 
				" minScore " + minScore );

	// compute waste.
	int waste = 0;
	for (int ii=0; ii<spheres.length; ii++) {
	    if ( finalVps[ii] > minScore + 1 ) 
		waste += finalVps[ii] - (minScore + 1);
	}

	// compute efficient sum.
	int effSum = rawSum - waste;

	if ( verbosity > 1 )
	    System.out.println( "waste? " + rawSum + 
				" effSum? " + effSum ); 
	

	if ( verbosity > 1 ) {
	    // floating point division! 
	    // only if they want to use it.
	    double effPct = ((double) effSum) / rawSum;
	    System.out.println( "effPct? " + effPct );
	}

	// calculate score.
	int score = 0;

	score += efficientScore * effSum;

	if ( verbosity > 1 )
	    System.out.println( "score (efficient only)? " + score );

	// assign "smallWasteScore" points for each up to the threshold
	score += smallWasteScore *
	    Math.min( largeWasteThreshold, waste );

	if ( verbosity > 1 )
	    System.out.println( "score (plus small waste)? " + score );

	// assign "largeWasteScore" points for each past the threshold
	score += largeWasteScore *
	    Math.max( 0, waste - largeWasteThreshold );

	if ( verbosity > 1 )
	    System.out.println( "score (plus large waste)?  " + score );

	score += treasureBonus * 
	    player.getNumTreasures();

	if ( verbosity > 1 )
	    System.out.println( "score (plus large treasure bonus)? " + score);

	return score;
    }
}
