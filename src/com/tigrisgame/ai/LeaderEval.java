/* File: LeaderEval.java
 * Description:  Heuristically scores values of leaders on the board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;

public class LeaderEval implements GameEvaluator
{
    public static int verbosity = 0;

    // points for having a leader down.
    int leaderScore = 500;

    // points for each internal support.
    int supportScore = 100;

    // the bonus for having your internal support on a treasure space.
    int treasureScore = 50;

    // the score for having an open space to lend support.
    int potentialScore = 10;

    public LeaderEval() {}

    public LeaderEval( int lead, int support, int treasure, int potential )
    {
	leaderScore = lead;
	supportScore = support;
	treasureScore = treasure;
	potentialScore = potential;
    } 

    public void setLeaderScore( int lead )
    {
	leaderScore = lead;
    }

    public void setSupportScore( int support )
    {
	supportScore = support;
    }

    public void setTreasureScore( int treasure )
    {
	treasureScore = treasure;
    }

    public void setPotentialScore( int potential )
    {
	potentialScore = potential;
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
	return evaluatePlayer( game.getBoard(), player );
    }

    // public method exposed for testing.
    // this should always evaluate (assuming waste < 100)
    // higher for a player who is actually winning.
    public int evaluatePlayer( Board board, Player player )
    {
	int score = 0;

	Sphere [] spheres = Sphere.getSpheres();
	int dynasty = player.getDynasty();
	Space [] leaderSpaces = board.getLeaderSpaces( dynasty, spheres );

	for ( int ss=0; ss<spheres.length; ss++) {
	    if ( player.getLeader( spheres[ss] ) == null ) {
		// the leader is not in the players screen,
		// therefore it is on the board.
		Space leaderSpace = leaderSpaces[ ss ];

		int support = 0;
		int treasureSupport = 0;
		int potentialSupport = 0;

		// the quick way to *just* get current support
		support = board.getNumAdjacentTiles( leaderSpace,
						     Sphere.RED );

		// get the spaces for more detailed info.
		Space [] adjSpaces = board.getAdjacentSpaces( leaderSpace );
		for ( int ii=0; ii<adjSpaces.length; ii++) {
		    if ( adjSpaces[ii].isLand() )
			{
			    if ( adjSpaces[ii].isEmpty() )
				potentialSupport ++;
			    else if ( adjSpaces[ii].hasTreasure() && 
				      adjSpaces[ii].hasTile() )
				treasureSupport ++;
			}
		}
		
		if ( verbosity > 1 )
		    System.out.println(spheres[ss] + 
				       " support? " + support +
				       " treasure? " + treasureSupport +
				       " potential? " + potentialSupport );

		score += leaderScore;
		score += supportScore * support;
		score += treasureScore * treasureSupport;
		score += potentialScore * potentialSupport;
	    }
	    if ( verbosity > 1 )
		System.out.println(spheres[ss] + " no leader");


	}

	return score;
    }
}
