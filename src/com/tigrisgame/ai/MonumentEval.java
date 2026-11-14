/* File: MonumentEval.java
 * Description:  Evaluates value of controlling monuments.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;

public class MonumentEval implements GameEvaluator
{
    // points for having a monument
    // score isn't right, maybe, just for testing
    int monumentScore = 2000;

    public MonumentEval() {}

    public MonumentEval( int score ) 
    {
	monumentScore = score;
    }

    public void setMonumentScore ( int score )
    {
	monumentScore = score;
    }

    // evaluates for all dynasties, return an array of int scores
    public int [] evaluate( TigrisGame game)
    {
	int [] scores = new int[ game.getNumPlayers() ];
	
	for ( int pp=0; pp < scores.length; pp++) {
	    scores[pp] = evaluate( game, pp );
	}

	return scores;
    }

    // evaluates for one dynasty, returns an int.
    // may or may not consider other dynasties to get relative score.
    public int evaluate ( TigrisGame game, int dynasty )
    {
	Player player = game.getPlayer( dynasty );
	return evaluatePlayer( game.getBoard(), dynasty);
    }

    // public method exposed for testing.
    // this should always evaluate (assuming waste < 100)
    // higher for a player who is actually winning.
    //
    // assumes monuments are already built!
    // to make this good, need to evaluate building monuments.
    public int evaluatePlayer( Board board, int dynasty )
    {
	int score = 0;

	// COPIED FROM StandardGame.assignMonumentVP().
	// except new leader change (don't have access to leader.sphere)
	Sphere spheres [] = Sphere.getSpheres();
	Space leaderSpaces [] = board.
	    getLeaderSpaces( dynasty, spheres ); // this is O(spaces).

	Monument [] mons = board.getMonuments();  // this is O(spaces).
	for ( int mm=0; mm<mons.length; mm++ ) {  // max of 6.

	    // would be better to grab something like board.getMonumentSpaces 
	    // but that isn't implemented, so we do an extra 6 * O(spaces).
	    Space monSpace = board.getUpperLeftSpace( mons[mm] );

	    Sphere firstSphere = mons[mm].getFirstSphere();
	    Sphere secondSphere = mons[mm].getSecondSphere();
		
	    for ( int ss=0; ss<spheres.length; ss++ ) { // 4 times.
		// skip if the leader isn't down, or isn't in 
		// the same kingdom of the monument.
		if ( leaderSpaces[ss] == null ||
		     ! board.isKingdomEqual( leaderSpaces[ss], monSpace ))
		    continue;

		// can't match both spheres in a monument.
		// so only check for one per leader.
		if ( spheres[ss].equals( firstSphere ) ||
		     spheres[ss].equals( secondSphere ) ) {

		    score = monumentScore + score;
		}
	    }
	}





	return score;
    }
}


