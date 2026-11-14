/* File: WinnerEval.java
 * Description:  binary eval if someone is winning.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE:
 * a simple eval that returns 
 * MAX_INT for a win, MIN_INT for a loss, and ZERO for a tie.
 */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.TigrisGame;
import com.tigrisgame.util.ArrayUtils;
import com.tigrisgame.util.PlayerScoreSorter;

public class WinnerEval implements GameEvaluator
{
    PlayerScoreSorter scoreSorter = new PlayerScoreSorter();

    // evalutes for all dynasties, returns an array of int scores.
    public int [] evaluate( TigrisGame game )
    {
	// let's be extra careful not to muck with the real array.
	Player [] sortPlayers = new Player[ game.getNumPlayers() ];
	for ( int ii=0; ii<sortPlayers.length; ii++ )
	    sortPlayers[ii] = game.getPlayer( ii );

	ArrayUtils.sortDescending( sortPlayers, scoreSorter );

	// check if the first two are equal, a tie.
	boolean isTieScore = scoreSorter.eq( sortPlayers[0], sortPlayers[1] );

	int [] scores = new int[ sortPlayers.length ];
	for ( int pp=0; pp<scores.length; pp++ ) {

	    // either the player IS the first player in the
	    // sorted array, or the score is equal.
	    if ( game.getPlayer(pp) == sortPlayers[0] ||
		 scoreSorter.eq( game.getPlayer(pp), sortPlayers[0] ))
		{
		    if ( ! isTieScore )
			scores[pp] = Integer.MAX_VALUE;
		    else
			scores[pp] = 0;
		}
	    else 
		scores[pp] = Integer.MIN_VALUE;
	}
	return scores;
    }

    public int evaluate( TigrisGame game, int dynasty )
    {
	return evaluate( game )[ dynasty ];
    }
}
