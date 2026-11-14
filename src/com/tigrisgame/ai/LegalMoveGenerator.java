/* File: LegalMoveGenerator.java
 * Description:  Generates all legal moves.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES
 * a utility class for the AI programs that generates moves from a position.
 * all of these methods are call at your own risk, and will return a zero length
 * array of moves if the game state is incompatible.
  */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ArrayUtils;

import java.util.Vector;

public class LegalMoveGenerator
{
    public static int verbosity = 0;

    // returns all legal moves in a decision
    public static Move [] getMoves( TigrisGame game )
    {
	// a big trade off in this and its called functions
	// is logic in generating the moves and
	// logic in verifying they are correct.
	// for instance, we could simply have a list of ALL
	// possible moves and check isLegal, or generate 
	// a more refined subset.
	int dynasty = game.getMoveDynasty();

	switch ( game.getState() )
	    {
	    case TigrisGame.WAIT_ACTION:
		return getActions( game );
	    case TigrisGame.WAIT_MONUMENT:
		return getMonumentMoves( game );
	    case TigrisGame.WAIT_TREASURE:
		return getTreasureMoves( game );
	    case TigrisGame.WAIT_ATTACK_SUPPORT:
	    case TigrisGame.WAIT_DEFEND_SUPPORT:
		return getSupporterMoves( game );
	    case TigrisGame.WAIT_CONFLICT_ORDER:
		return getConflictOrders( game );
	    case TigrisGame.GAME_OVER:
		// no legal moves if game is over.
		return new Move[0];
	    default:
		System.out.println("Warning: LegalMoveGenerator encountered " +
				   "an unknown game state ");
	    }
	return null;
    }

    public static Move [] getMonumentMoves( TigrisGame game )
    {
	// not called very often...
	// we can afford to generate a bunch
	// and check with isLegal
	// but INEFFICIENT!
	// and now up to 5% of total ai processing, will rewrite.

	int dynasty = game.getMoveDynasty();
	Vector monumentDecisions = new Vector();
	Move move;

	// "manually" get all the spaces.
	// man this is inefficient.
	Board board = game.getBoard();
	int rows = board.getNumRows();
	int cols = board.getNumCols();

	Space lastSpace = game.getLastSpace();
	Sphere firstSphere = lastSpace.getTile();

	// upper left of new monument must be current space or
	// something to the NW of the current space.
	Space [] spaces = new Space[] {
	    lastSpace, 
	    board.getNorthSpace( lastSpace ),
	    board.getWestSpace( lastSpace ),
	    board.getNorthSpace( board.getWestSpace( lastSpace ))
	};

	// trim out any spaces that aren't upper left corners.
	// at least one should be okay.
	for ( int ii=0; ii<spaces.length; ii ++ ) {
	    if ( ! board.isTileSquare( spaces[ii] ) )
		spaces[ii] = null;
	}

	Sphere [] spheres = Sphere.getSpheres();
	Monument [] mons = game.getBoard().getMonuments();
	Monument tmpMon = null;

	// trim out any second spheres that already exist.
	// also trim out same as first sphere.
	for ( int ss=0; ss<spheres.length; ss++ ) {
	    if ( spheres[ss].equals( firstSphere ))
		spheres[ss] = null;
	    else {
		tmpMon = new Monument( firstSphere, spheres[ss] );
		if ( ArrayUtils.contains( tmpMon, mons ) )
		    spheres[ss] = null;
	    }
	}

	for ( int ii=0; ii<spaces.length; ii++ ) {
	    if ( spaces[ii] == null )
		continue;

	    for ( int ss=0; ss<spheres.length; ss++ ) {
		if ( spheres[ss] != null ) {
		    monumentDecisions.
			addElement( Move.newBuildMonument( dynasty, 
							   spaces[ii],
							   spheres[ss], 
							   firstSphere ));
		}
	    }
	}

	// add the "build no monument" decision.
	monumentDecisions.addElement( Move.newBuildMonument( dynasty, null, 
							     null, null ));

	Move moveArray [] = new Move[ monumentDecisions.size() ];
	monumentDecisions.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getTreasureMoves( TigrisGame game )
    {
	Vector treasureDecisions = new Vector();
	int dynasty = game.getMoveDynasty();

	// since you MUST take a treasure, we can
	// assume that if a kingdom is the only one
	// with two and we have the green leader.
	Space [] tSpaces = game.getBoard().getTreasureSpaces();
	Move pd;
	for ( int ii=0; ii<tSpaces.length; ii++)
	    {
		pd = Move.newChooseTreasure( dynasty,
					     tSpaces[ii] );

		try { 
		    game.isLegal( pd ); 
		    treasureDecisions.addElement( pd );
		}
		catch (IllegalMoveException ime) {
		    // isLegal used to generate moves
		    // (quick implementation but slow)
		    // guess it wasn't mean to be.
		}
	    }

	Move moveArray [] = new Move[ treasureDecisions.size() ];
	treasureDecisions.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getSupporterMoves( TigrisGame game )
    {
	// the color of the supporters is either red for internal, 
	// or the conflict sphere color.
	Sphere conflictSphere = game.getConflict().getSupportSphere();

	int dynasty = game.getMoveDynasty();

	Player mePlayer = game.getPlayer( dynasty );
	int numPossibleTiles = mePlayer.getNumTiles( conflictSphere );
	Vector moves = new Vector();

	// adding 0 is always legal.
	moves.addElement( Move.newChooseSupporters( dynasty, new TileBag()));

	// amount the defender needs to tie.
	int tieAmount = game.getConflict().getNumTilesToTie();

	for ( int ii=1; ii<numPossibleTiles; ii++) {
	   if ( game.getRules().isEnglish() &&
		game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT &&
		ii + tieAmount < 1 )
	      break;

	   if ( game.getRules().isEnglish() &&
		game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT &&
		ii != tieAmount )
	      break;

	   TileBag supporters = new TileBag();
	   supporters.putTiles( conflictSphere, ii );
	   moves.addElement( Move.newChooseSupporters( dynasty, supporters ));
	}

	Move moveArray [] = new Move[ moves.size() ];
	moves.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getConflictOrders( TigrisGame game )
    {
	Sphere [] spheres = Sphere.getSpheres();
	Vector decisions = new Vector();
	int dynasty = game.getMoveDynasty();

	for ( int ii=0; ii<spheres.length; ii++)
	    {
		Move pd = Move.newChooseSphere( dynasty, spheres[ii] );

		try
		    {
			game.isLegal( pd );
			decisions.addElement( pd );
		    }
		catch ( IllegalMoveException ime )
		    {
			// isLegal used to generate moves
			// (quick implementation but slow)
			// try the next move.
		    }
	    }

	Move moveArray [] = new Move[ decisions.size() ];
	decisions.copyInto( moveArray );	
	return moveArray;
    }

    // even more inefficient than before, but this is called 
    // 500 times less then Game.clone() so speed isn't a bottleneck
    // unless there is something REALLY bad i haven't noticed.
    public static Move [] getActions( TigrisGame game )
    {
	Move [] leaderMoves = getLeaderMoves( game );
	Move [] tileMoves   = getTileMoves( game );
	Move [] catMoves    = getCatMoves( game );
	Move [] swapMoves   = getSwapMoves( game );
	
	Move [] moveArray = (Move []) ArrayUtils.
	    mergeArrays( new Move[][] { leaderMoves, tileMoves,
					catMoves, swapMoves });

	/* keeping until we make sure that new generalize mergeArrays method works.
	 * ***********************************************************************

	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Sphere [] spheres = Sphere.getSpheres();
	Move pd = null;
	Player player = game.getPlayer( dynasty );

	...

	int lLen = leaderActions.length;
	int tLen = tileActions.length;
	int cLen = catActions.length;
	int sLen = swapActions.length;

	Move moveArray [] = new Move[ lLen + tLen + cLen + sLen ];

	System.arraycopy( leaderActions, 0, moveArray, 0, lLen );
	System.arraycopy( tileActions, 0, moveArray, lLen, tLen );
	System.arraycopy( catActions, 0, moveArray, lLen+tLen, cLen );
	System.arraycopy( swapActions, 0, moveArray, lLen+tLen+cLen, sLen );
	*/

	return moveArray;
    }

    // 4 times faster, but doesn't take into account 
    // that moving a leader undoes the kingdom or splits the kingdom that he is in.
    // 
    // one way to improve (fix) this is to put the spheres loop on the outside, and
    // update the kingdoms 4 times, and create 4*row*col space objects.
    // even faster, but harder to read, is to unwrap the loops so there 
    // are 4 board objects that are used once each inside the loop,
    // this way SOME checks can be repeated 
    // on the same space object (land?, #adjacent reds)
    // 
    // needs more thorough testing... FastAI filling out the search tree
    // will sure do that.  :)
    public static Move [] getLeaderMoves( TigrisGame game )
    {
	return getLeaderMoves( game, true );
    }

    public static Move [] getLeaderMoves( TigrisGame game, boolean conflictMoves )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	Board board = game.getBoard();
	int rows = board.getNumRows();
	int cols = board.getNumCols();
	Sphere [] spheres = Sphere.getSpheres();
	Space [] leaderSpaces = board.getLeaderSpaces( dynasty, spheres );
	Leader leader = null;
	Move move = null;
	
	for ( int ss=0; ss<spheres.length; ss++) {
	    Space leaderSpace = leaderSpaces[ss];
	    if ( leaderSpace != null ) {
		board.emptyLocation( leaderSpace );
		board.updateKingdoms( leaderSpace );
	    }
	    for ( int rr = 0; rr < rows; rr ++ ) {
		for ( int cc = 0; cc < cols; cc ++) {
		    Space sp = board.getSpace( rr, cc );
		    int numKings = board.getNumAdjKingdoms( sp );

		    if ( sp.isLand() && sp.isEmpty() && 
			 game.getBoard().
			 getNumAdjacentTiles( sp, Sphere.RED ) != 0 && 
			 !sp.isLocationEqual( leaderSpace ) && 
			 numKings < 2 ) {
			// check to see if we should ignore conflict moves.
			// otherwise just proceed as normal.
			if ( ! conflictMoves &&
			     numKings == 1 &&
			     board.getAdjacentKingdoms( sp )[0].
			     getLeader( spheres[ss] ) != null )
			    continue;
			
			move = Move.newPositionLeader( dynasty, sp,
						       spheres[ss] );
			actions.addElement( move );
		    }
		}
	    }
	    // maybe it's cheaper to copy the board and not
	    // update the kingdoms,
	    // but we choose to undo the changes instead
	    // CM 9/6/2002 -- seemed to be validated by profiling.
	    // update kingdoms only took 1% of this method call.
	    if ( leaderSpace != null ) {
		board.putLeader( leaderSpace, spheres[ss], dynasty );
		board.updateKingdoms( leaderSpace );
	    }	    
	}
	
	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }

    /* old way that WORKS, but is SLOW
    public static Move [] getLegalLeaderActions( Game game )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Sphere [] spheres = Sphere.getSpheres();
	Move move = null;
	
	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );
		for ( int ss=0; ss<spheres.length; ss++) {
		    move = Move.newPositionLeader( dynasty, sp, spheres[ss] );
		    try {
			game.isLegal( move );
			actions.addElement( move );
		    } catch ( IllegalMoveException ime ) {
			// try the next move.
		    }
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }
    */
    
    public static Move [] getTileMoves( TigrisGame game )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Sphere [] spheres = Sphere.getSpheres();
	Move move = null;
	
	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );
		for ( int ss=0; ss<spheres.length; ss++) {
		    try {
			move = Move.newPlaceTile( dynasty, sp, spheres[ss] );
			game.isLegal( move );
			actions.addElement( move );
		    } catch ( IllegalMoveException ime ) {
			// isLegal used to generate moves
			// (quick implementation but slow)
			// try the next move.
		    }
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getCatMoves( TigrisGame game )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Move move = null;
	
	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );
		move = Move.newPlayCatastrophe( dynasty, sp );
		try {
		    game.isLegal( move );
		    actions.addElement( move );
		} catch ( IllegalMoveException ime ) {
		    // isLegal used to generate moves
		    // (quick implementation but slow)
		    // try the next move.
		}
	    }
	}
	
	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getSwapMoves( TigrisGame game )
    {
	int dynasty = game.getMoveDynasty();
	Player player = game.getPlayer( dynasty );
	Vector actions = new Vector();
	Sphere [] spheres = Sphere.getSpheres();
	Move move = null;

	// SWAP TILES, must have the tiles.
	int [] numTiles = new int[ spheres.length ];
	for ( int ss=0; ss<spheres.length; ss++)
	    {
		numTiles[ss] = game.getPlayer( dynasty ).
		    getNumTiles( spheres[ss] );
	    }

	// hmm.. it seems the only way not to know apriori that there
	// are 4 spheres is to use recursion.
	for ( int ii=0; ii<=numTiles[0]; ii++)
	    for ( int jj=0; jj<=numTiles[1]; jj++)
		for ( int kk=0; kk<=numTiles[2]; kk++)
		    for ( int ll=0; ll<=numTiles[3]; ll++)
			{
			    move = Move.
				newSwapTiles( dynasty, 
					      new TileBag( ii,jj,kk,ll ) );
			    actions.addElement( move );
			}
	
	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }
}
