/* File: BetterMoveGenerator.java
 * Description:  A utility class for the AI programs that generates moves from a position.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ArrayUtils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class BetterMoveGenerator
{
    public static int verbosity = 0;

    public static Move [] getMoves( TigrisGame game )
    {
	Sphere [] spheres = Sphere.getSpheres();

	switch ( game.getState() ) {
	case TigrisGame.WAIT_ACTION:
	    Move [] tileMoves = getTileAddToKingdomMoves( game, spheres );
	    Move [] catMoves  = getCatOverTileMoves( game );
	    Move [] swapMoves  = getSwapButSphereMoves( game );
	    Move [] conflictMoves  = getPairwiseConflictMoves( game, spheres );
	    return (Move []) ArrayUtils.
		mergeArrays( new Move[][] { tileMoves, catMoves, 
					    swapMoves, conflictMoves } );
	case TigrisGame.WAIT_MONUMENT:
	    return LegalMoveGenerator.getMonumentMoves( game );
	case TigrisGame.WAIT_TREASURE:
	    return new Move[] { getOneTreasureMove( game ) };
	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    return new Move[] { getMaxSupportMove( game ) };
	case TigrisGame.WAIT_CONFLICT_ORDER:
	    return LegalMoveGenerator.getConflictOrders( game );
	case TigrisGame.GAME_OVER:
	    return new Move[0];
	default:
	    System.out.println("unknown game state " +
			       game.getState());
	    return null;
	}
    }

    // returns a subset of all legal PLACE_TILE moves that
    // add the tile to a kingdom, 
    // NOT including any mergers with other kingdoms.
    // this removes the random tiles in the middle of the board,
    // although it does lose the "place a temple to attack an 
    // unassailable kingdom" strategy, (and others).

    public static Move [] getTileAddToKingdomMoves( TigrisGame game, Sphere sphere )
    {
	return getTileAddToKingdomMoves( game, new Sphere[]{sphere} );
    }

    public static Move [] getTileAddToKingdomMoves( TigrisGame game, 
						    Sphere [] spheres )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Move move = null;

	// only search over passed in spheres.
	// Sphere [] spheres = Sphere.getSpheres();

	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );
		if ( ! sp.isEmpty() )
		    continue;

		int numKings = game.getBoard().getNumAdjKingdoms( sp );
		if ( numKings == 1 ) {
		    for ( int ss=0; ss < spheres.length; ss ++ ) {
			// check, water and blue, land and nonblue agree.
			// check, tiles okay if >0 for that type or hidden
			boolean waterMatch =  
			    ( sp.isWater() && spheres[ss] == Sphere.BLUE ||
			      sp.isLand() && spheres[ss] != Sphere.BLUE );
			boolean tilesOkay = 
			    ( game.getPlayer( dynasty ).
			      getNumTiles( spheres[ss] ) > 0 ||
			      game.getPlayer( dynasty ).
			      getNumTiles( Sphere.HIDDEN ) > 0 );
			if ( waterMatch && tilesOkay ) {
			    move = Move.newPlaceTile( dynasty, sp,
						      spheres[ss] );
			    actions.addElement( move );
			}
		    }
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	for ( int ii=0; ii<moveArray.length; ii++)
	    {
		moveArray[ii] = (Move) actions.elementAt( ii );
	    }
	return moveArray;	
    }

    public static Move [] getPairwiseConflictMoves( TigrisGame game, 
						    Sphere sphere )
    {
	return getPairwiseConflictMoves( game, new Sphere[]{sphere} );
    }

    // returns the subset of all legal TILE_MOVES that
    // cause a conflict, but only on ONE MOVE per kingdom pair
    // the space and tile chosen of that move are at random.
    public static Move [] getPairwiseConflictMoves( TigrisGame game, 
						    Sphere [] spheres )
    {
	Hashtable pairs = new Hashtable();
	int dynasty = game.getMoveDynasty();
	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Move move = null;

	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );
		Kingdom [] kings = game.getBoard().
		    getAdjacentKingdoms( sp );

		if ( kings.length == 2 ) {

		    // conflict!!
		    KingdomPair kp = new KingdomPair( kings[0], kings[1] );
		    Vector moves = (Vector) pairs.get( kp );

		    if ( moves == null ) {
			// a new pair.
			moves = new Vector();
		    }

		    for ( int ss=0; ss<spheres.length; ss++ ) {
			// check, water and blue, land and nonblue agree.
			// check, tiles okay if >0 for that type or hidden
			boolean waterMatch =  
			    ( sp.isWater() && spheres[ss] == Sphere.BLUE ||
			      sp.isLand() && spheres[ss] != Sphere.BLUE );
			boolean tilesOkay = 
			    ( game.getPlayer( dynasty ).
			      getNumTiles( spheres[ss] ) > 0 ||
			      game.getPlayer( dynasty ).
			      getNumTiles( Tile.HIDDEN ) > 0 );
			if ( waterMatch && tilesOkay ) {
			    move = Move.newPlaceTile( dynasty, sp,
						      spheres[ss] );
			    moves.addElement( move );
			}
		    }

		    // ensure that only pairs with *some* 
		    // legal move are placed in the array.
		    if ( moves.size() > 0 ) {
			pairs.put( kp, moves );
		    }
		}
	    }
	}

	Move [] moveArray = new Move[ pairs.size() ];
	// okay, now we have a bunch of spaces sorted by kingdom pair.
	// we return all the relevant moves of with "spheres" tiles
	// from a random one of those spaces.
	Enumeration ee=pairs.elements();
	for ( int ii=0; ii<moveArray.length; ii++) {
	    Vector spaces = (Vector) ee.nextElement();
	    // TODO: pick a random space, hopefully with 
	    // a settable random number seed.
	    moveArray[ii] = (Move) spaces.elementAt( 0 );
	}
	return moveArray;
    }

    static class KingdomPair
    {
	Kingdom king1, king2;

	public KingdomPair( Kingdom king1, Kingdom king2 )
	{
	    this.king1 = king1; this.king2= king2;
	}

	public int hashCode()
	{
	    // not sure how we can map this to different
	    // numbers while assuring the kings are equal AND
	    // not take too long in processing w/o 
	    // looking at internal representations. 
	    // TODO. fix hashcode and equals on Kingdom1 and Kingdom2

	    return 0;
	}

	public boolean equals( Object obj )
	{
	    boolean returnBool;
	    if ( obj instanceof KingdomPair ) {
		KingdomPair thatPair = (KingdomPair) obj;
		// for the kingdom pair to be equal
		// either a.king1 == b.king1 and a.king2 == b.king2
		// OR     a.king1 == b.king2 and a.king2 == b.king1

		returnBool =  ( this.king1.isKingdomEqual( thatPair.king1 ) &&
				this.king2.isKingdomEqual( thatPair.king2 ) ||
				this.king1.isKingdomEqual( thatPair.king2 ) &&
				this.king2.isKingdomEqual( thatPair.king1 ) );
	    } else
		returnBool = false;

	    return returnBool;
	}

	public String toString()
	{
	    return "KingdomPair king1=" + king1 + " spaces=" +
		ArrayUtils.prettyPrint(king1.getSpaces()) + 
		" king2=" +king2 + " spaces=" +
		ArrayUtils.prettyPrint(king2.getSpaces());
	}
    }

    // returns a subset of all legal PLACE_CATASTROPHE moves that
    // have the catastrophe placed on a tile.
    // although there is some amount of use that a catastrophe can do
    //  "blocking" (i.e. prevent a monument from being built,or build a wall),
    // in most cases cat moves are needed to break kingdoms apart
    public static Move [] getCatOverTileMoves( TigrisGame game )
    {
	Vector actions = new Vector();
	int dynasty = game.getMoveDynasty();

	if ( game.getPlayer( dynasty ).getNumCatastrophes() < 1 )
	    return new Move[0];

	int rows = game.getBoard().getNumRows();
	int cols = game.getBoard().getNumCols();
	Move move = null;

	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = game.getBoard().getSpace( rr, cc );

		if ( sp.hasTile() && ! sp.hasTreasure() ) {
		    // should be exactly the legality conditions, 
		    // but check anyway.
		    move = Move.newPlayCatastrophe( dynasty, sp );
		    actions.addElement( move );
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	for ( int ii=0; ii<moveArray.length; ii++)
	    {
		moveArray[ii] = (Move) actions.elementAt( ii );
	    }
	return moveArray;	
    }

    // note: when swapping, we are either trying to get a certain type of 
    // tile, or we are trying to empty the bag.
    // if we don't have one sphere, it will also generate the "swap all"
    // options, but if we do have one of each it 
    // will always generate a swap of at least 5 tiles, so that is okay.
    public static Move [] getSwapButSphereMoves( TigrisGame game )
    {
	int dynasty = game.getMoveDynasty();
	Player player = game.getPlayer( dynasty );
	Vector moves = new Vector();
	Sphere [] spheres = Sphere.getSpheres();
	Move move = null;

	for ( int ss=0; ss<spheres.length; ss++ ) {
	    TileBag tiles = new TileBag();
	    Sphere notSphere = spheres[ss];
	    tiles.putTiles( player );
	    tiles.drawTiles( notSphere, player.getNumTiles( notSphere ) );
	    move = Move.newSwapTiles( dynasty, tiles );

	    if ( ! moves.contains( move ) )
		moves.addElement( move );
	}

	Move moveArray [] = new Move[ moves.size() ];
	moves.copyInto( moveArray );	
	return moveArray;
    }

    public static Move getOneTreasureMove( TigrisGame game )
    {
	// not the most efficient way.
	Move [] moves = LegalMoveGenerator.getTreasureMoves( game );

	if ( moves != null && 
	     moves.length > 0 )
	    return moves[0];
	else
	    return null;
    }

    public static Move getMaxSupportMove( TigrisGame game )
    {
       // the color of the supporters is either red for internal, 
       // or the conflict sphere color.
       Conflict conflict = game.getConflict();
       Sphere conflictSphere = conflict.getSupportSphere();
       
       int dynasty = game.getMoveDynasty();
       Player mePlayer = game.getPlayer( dynasty );
       TileBag support = new TileBag();

       int numSupport = mePlayer.getNumTiles( conflictSphere );
       int tieAmount = conflict.getNumTilesToTie();

       // check ENGLISH variant and a win is impossible.
       // in that case maxsupport is zero .
       if ( game.getRules().isEnglish() && 
	    game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT && 
	    tieAmount + numSupport < 1 )
	  {
	     numSupport = 0;
	  }
	  
       // also as defender, can't play more than necessary.
       if ( game.getRules().isEnglish() && 
	    game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT ) 
	  {
	     if ( tieAmount < 0 ||
		  tieAmount > numSupport )
		numSupport = 0;
	     else
		numSupport = tieAmount;
	  }

       support.putTiles( conflictSphere, numSupport );
       Move move = Move.newChooseSupporters( dynasty, support );
       return move;
    }
}
