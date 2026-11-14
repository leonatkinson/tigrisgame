/* File: HandEval.java
 * Description:  Heuristically evaluates how good a hand is.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.TigrisGame;

public class HandEval implements GameEvaluator
{
    public static int verbosity = 0;

    // points for each efficient victory point.
    int catastropheScore = 1000;

    public HandEval() {}

    public HandEval( int cat )
    {
	catastropheScore = cat;
    } 

    public void setCatastropheScore( int value )
    {
	catastropheScore = value;
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

	// calculate score.
	int score = 0;

	score += player.getNumCatastrophes() * catastropheScore;

	return score;
    }
}
