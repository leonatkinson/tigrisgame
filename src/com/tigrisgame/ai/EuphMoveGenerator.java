/* File: EuphMoveGenerator.java
 * Description:  Generates the types of moves made when out of time.
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

import java.util.Vector;

public class EuphMoveGenerator
{
    public static Move [] getMoves( TigrisGame game )
    {
	Sphere [] spheres = Sphere.getSpheres();

	switch ( game.getState() ) {
	case TigrisGame.WAIT_ACTION:
	    Move [] moves;
	    moves = getTileBuildMonumentMoves( game );
	    if ( moves.length == 0 ) {
		moves = getTileScoreNeededMoves( game );
	    }
	    if ( moves.length == 0 ) {
		moves = getLeaderMaxSupportMoves( game );
	    }
	    if ( moves.length == 0 ) {
		moves = getSwapTwoMostMoves( game );
	    }
	    return moves;
	case TigrisGame.WAIT_MONUMENT:
	    return getBuildMostMatchingMoves( game );
	case TigrisGame.WAIT_TREASURE:
	    return LegalMoveGenerator.getMoves( game );
	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    return new Move [] { getMinimumWinSupportMove( game ) };
	case TigrisGame.WAIT_CONFLICT_ORDER:
	    return LegalMoveGenerator.getMoves( game );
	case TigrisGame.GAME_OVER:
	    return new Move[0];
	default:
	    System.out.println("unknown game state " +
			       game.getState());
	    return null;
	}
    }

    // utitily method.
    public final static boolean tilesOkay( Player player, Sphere sphere )
    {
	return ( player.getNumTiles( sphere ) > 0 ||
		 player.getNumTiles( Sphere.HIDDEN ) > 0 );
    }
    // utitily method.
    public final static boolean waterOkay( Space sp, Sphere sphere )
    {
	return ( sp.isWater() && sphere == Sphere.BLUE ||
		 sp.isLand() && sphere != Sphere.BLUE );
    }

    public static Move [] getTileBuildMonumentMoves( TigrisGame game )
    {
	Board board = game.getBoard();
	Vector actions = new Vector();
	Sphere [] spheres = Sphere.getSpheres();
	boolean validColor[] = new boolean[ spheres.length ];
	int dynasty = game.getActionDynasty();

	// --------------------------------------
	// filter based on monument availability.
	// --------------------------------------
	Monument [] mons = board.getMonuments();
	int [] builtSpheres = new int[ spheres.length ];
	for ( int mm=0; mm<mons.length; mm++ ) {
	    Monument monx = mons[mm];
	    builtSpheres[monx.getFirstSphere().getColorCode()] ++;
	    builtSpheres[monx.getSecondSphere().getColorCode()] ++;
	}
	for ( int ss=0; ss<spheres.length;ss++ ) {
	    if ( !tilesOkay( game.getPlayer( dynasty ), spheres[ss] ) ||
		 builtSpheres[ss] == 3 ) {
		validColor[ss] = false;
	    } else {
		validColor[ss] = true;
	    }
	}

	int rows = board.getNumRows();
	int cols = board.getNumCols();
	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = board.getSpace( rr, cc );
		if ( ! sp.isEmpty() )
		    continue;

		// -----------------------------------------------
		// iterating over all empty spaces on the board...
		// -----------------------------------------------
		boolean [] monSpheres = new boolean[4];  // tiles will build

		Space north = board.getNorthSpace( sp );
		Space east = board.getEastSpace( sp );
		Space south = board.getSouthSpace( sp );
		Space west = board.getWestSpace( sp );
		
		Space northEast = board.getEastSpace( north );
		Space northWest = board.getWestSpace( north );
		Space southEast = board.getEastSpace( south );
		Space southWest = board.getWestSpace( south );

		Space [] spaceRing = new Space[] 
		    { north, northEast, east, southEast, 
		      south, southWest, west, northWest };

		// iterate over the 4 main compas points.
		for ( int ii=0; ii<spaceRing.length; ii+=2 ) {

		    if ( spaceRing[ii] == null || 
			 !spaceRing[ii].hasTile())
			continue;

		    Tile curr = spaceRing[ii].getTile();
		    // abort if all of this type built.
		    if ( !validColor[curr.getColorCode()] )
			continue;

		    // must equal next 2 tiles.
		    Space next1 = spaceRing[(ii+1)%spaceRing.length];
		    Space next2 = spaceRing[(ii+2)%spaceRing.length];

		    if ( next1 != null &&
			 next2 != null &&
			 curr.isSphereEqual( next1.getTile() ) &&
			 curr.isSphereEqual( next2.getTile() )) 
			{
			    monSpheres[curr.getColorCode()] = true;
			}

		    // or equal previous 2 tiles.
		    Space prev1 = spaceRing[(ii-1+8)%spaceRing.length];
		    Space prev2 = spaceRing[(ii-2+8)%spaceRing.length];

		    if ( prev1 != null &&
			 prev2 != null &&
			 curr.isSphereEqual( prev1.getTile() ) &&
			 curr.isSphereEqual( prev2.getTile() )) 
			{
			    monSpheres[curr.getColorCode()] = true;
			}
		}

		// construct moves for the valid tiles
		for ( int jj=0; jj<monSpheres.length; jj++) {
		    boolean waterMatch = waterOkay( sp, spheres[jj] );

		    Move move;
		    if ( monSpheres[jj] && waterMatch ) {
			move = Move.newPlaceTile( dynasty, sp, spheres[jj] );
			actions.addElement( move );
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
    // returns a subset of all legal PLACE_TILE moves that score
    // the current player a vp that does not exceed any (xxx) other 
    // by 3 or more point.
    public static int UNNEEDED_AMOUNT = 3;
    public static Move [] getTileScoreNeededMoves( TigrisGame game )
    {
        Board board = game.getBoard();
	int dynasty = game.getMoveDynasty();
	Vector actions = new Vector();
	Sphere [] spheres = Sphere.getSpheres(); 	
	Space [] leaderSpaces = board.getLeaderSpaces( dynasty, spheres );
	boolean validColor[] = new boolean[ spheres.length ];

	// --------------------------------------
	// filter based on 'needed'.
	// --------------------------------------
	Player player = game.getPlayer( dynasty );
	int minVp = 9999;
	for ( int ss=0; ss<spheres.length; ss++ ) {
	    int vp = player.getVP( spheres[ss] );
	    if ( vp < minVp )
		minVp = vp;
	}
	boolean gotValid = false;
	for ( int ss=0; ss<spheres.length; ss++ ) {
	    boolean tilesOkay = tilesOkay( player, spheres[ss] );

	    boolean neededOkay = ( player.getVP( spheres[ss] ) < 
				   minVp + UNNEEDED_AMOUNT );

	    validColor[ss] = (tilesOkay && neededOkay && 
			      (leaderSpaces[ss] != null ||
			       leaderSpaces[Sphere.BLACK_NUM] != null) );

	    if ( validColor[ss] ) {
		gotValid = true;
	    }
	}

	// make sure we have at least one valid sphere
	// that has 1) leader, 2) need, 3) tile.
	if ( !gotValid ) {
	   return new Move[0];
	}

	int rows = board.getNumRows();
	int cols = board.getNumCols();

	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = board.getSpace( rr, cc );
		if ( ! sp.isEmpty() )
		    continue;

		int numKings = board.getNumAdjKingdoms( sp );
		if ( numKings != 1 )
		   continue;

		Kingdom king = board.getAdjacentKingdoms( sp )[0];
		Move move;
		for ( int ss=0; ss < spheres.length; ss ++ ) {
		    // check legality of water/tiles.
		    if ( ! validColor[ss] || 
			 ! waterOkay( sp, spheres[ss]) || 
			 ! tilesOkay( player, spheres[ss] )) 
			continue;

		   // dyn scores if 1) exact match OR
		   // 2) dyn has black leader and no exact match present.
		   if ( king.hasSpace( leaderSpaces[ss] ) || // exact match
			king.hasSpace( leaderSpaces[Sphere.BLACK_NUM] ) &&
			king.getLeader( spheres[ss] ) == null ) { // free black

		      move = Move.newPlaceTile( dynasty, sp,
						spheres[ss] );
		      actions.addElement( move );
		   }
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }

    // returns a subset of legal PLACE_LEADER moves that
    // place leaders from the screen to any of the places on the board
    // that have maximum support.
    public static Move [] getLeaderMaxSupportMoves( TigrisGame game )
    {
	Vector actions = new Vector();
	int maxSupport = 1;
	int dynasty = game.getMoveDynasty();
	Player player = game.getPlayer( dynasty );

	if ( player.getNumLeaders() == 0 ) {
	    // all leaders on the board!
	    return new Move[0];
	}
	Sphere [] spheres = Sphere.getSpheres();
	Board board = game.getBoard();
	Space [] leaderSpaces = board.getLeaderSpaces( dynasty, spheres );

	int rows = board.getNumRows();
	int cols = board.getNumCols();


	for ( int rr = 0; rr < rows; rr ++ ) {
	    for ( int cc = 0; cc < cols; cc ++) {
		Space sp = board.getSpace( rr, cc );
		int numKings = board.getNumAdjKingdoms( sp );
		int numRed = board.getNumAdjacentTiles( sp, Sphere.RED );

		if ( sp.isWater() || ! sp.isEmpty() || 
		     numKings >= 2 || numRed < maxSupport ) 
		    continue;

		if ( numRed > maxSupport ) {
		    maxSupport = numRed;
		    actions = new Vector();
		}

		Move move;
		for ( int ss=0; ss<spheres.length; ss++ ) {
		    if ( leaderSpaces[ss] == null ) {
			move = Move.newPositionLeader( dynasty, sp,
						       spheres[ss] );
			actions.addElement( move );			
		    }
		}
	    }
	}

	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }
	
    // return moves that swap 2 tiles of whatever sphere
    // the player has the most (or tied for most) of.
    public static Move [] getSwapTwoMostMoves( TigrisGame game )
    {
	int dynasty = game.getMoveDynasty();
	Player player = game.getPlayer( dynasty );
	Sphere [] spheres = Sphere.getSpheres();
	int mostTiles = 2;
	Vector actions = new Vector();

	for ( int ss=0; ss<spheres.length; ss++) {
	    int sNum = player.getNumTiles( spheres[ss] );
	    if ( sNum < mostTiles ) 
		continue;

	    if ( sNum > mostTiles ) {
		mostTiles = sNum;
		actions = new Vector();
	    }

	    TileBag tiles = new TileBag();
	    tiles.putTiles( spheres[ss], Math.min( 2, mostTiles));

	    Move move = Move.newSwapTiles( dynasty, tiles );
	    actions.addElement( move );
	}

	// we must not have had any tiles, swap zero.
	if ( actions.size() == 0 ) {
	    Move move = Move.newSwapTiles( dynasty, new TileBag());
	    actions.addElement( move );
	}
	
	Move moveArray [] = new Move[ actions.size() ];
	actions.copyInto( moveArray );	
	return moveArray;
    }

    public static Move [] getBuildMostMatchingMoves( TigrisGame game )
    {
	Space lastSpace = game.getLastSpace();
	Sphere firstSphere = lastSpace.getTile();

	Sphere [] spheres = Sphere.getSpheres();
	int dynasty = game.getMoveDynasty();
	Board board = game.getBoard();
	Monument [] mons = board.getMonuments();
	Space [] leaderSpaces = board.getLeaderSpaces( dynasty, spheres );
	Monument tmpMon = null;
	Space theSpace = null;
	int maxHelp = 0;
	Vector validSpheres = new Vector();

	// trim out any second spheres that already exist.
	// also trim out same as first sphere.
	for ( int ss=0; ss<spheres.length; ss++ ) {
	    tmpMon = new Monument( firstSphere, spheres[ss] );
	    if ( spheres[ss].isSphereEqual( firstSphere ) || 
		 ArrayUtils.contains( tmpMon, mons )) 
		continue;

	    Kingdom king = board.getKingdom( lastSpace );
	    int help = 0;
	    for ( int hh=0; hh<leaderSpaces.length; hh++) {
		if ( king != null && king.hasSpace( leaderSpaces[hh] ) &&
		     (ss == hh || spheres[ss].isSphereEqual( firstSphere ))) {
		    help ++;
		}
	    }

	    if ( help < maxHelp ) 
		continue;

	    if ( help > maxHelp ) {
		validSpheres = new Vector();
		maxHelp = help;
	    }

	    validSpheres.add( spheres[ss] );
	}


	// upper left of new monument must be current space or
	// something to the NW of the current space.
	Space [] spaces = new Space[] {
	    lastSpace, 
	    board.getNorthSpace( lastSpace ),
	    board.getWestSpace( lastSpace ),
	    board.getNorthSpace( board.getWestSpace( lastSpace ))
	};


	Vector moves = new Vector();
	for ( int ii=0; ii<spaces.length; ii++ ) {

	    if ( ! board.isTileSquare( spaces[ii] ) )
		continue;

	    for ( int ss=0; ss<validSpheres.size(); ss++ ) {
		if ( spheres[ss] != null ) {
		    moves.addElement
			( Move.newBuildMonument( dynasty, 
						 spaces[ii],
						 (Sphere)validSpheres.get(ss), 
						 firstSphere ));
		}
	    }
	}

	if ( moves.size() == 0 ) {
	    moves.addElement( Move.newBuildMonument( dynasty, null, 
						     null, null ));
	}

	Move moveArray [] = new Move[ moves.size() ];
	moves.copyInto( moveArray );	
	return moveArray;
    }

    public static Move getMinimumWinSupportMove( TigrisGame game )
    {
	Conflict conflict = game.getConflict();
	Sphere sphere = conflict.getSupportSphere();
	int tieAmount = conflict.getNumTilesToTie();
	int dyn = game.getMoveDynasty();
	Player player  = game.getPlayer( dyn );
	int maxAmount = player.getNumTiles( sphere );
	TileBag supporters = new TileBag();

	boolean attackersTurn = (game.getState() == 
				 TigrisGame.WAIT_ATTACK_SUPPORT);
	if ( attackersTurn ) {
	    if ( tieAmount >= 1 ) {
		// we already have a 'one tile' lead - add none.
	    }
	    else if ( -tieAmount + 1 > maxAmount ) {
		// time amount more negative than max amount - add none.
	    } 
	    else {
		// add enough to go up by one.
		supporters.putTiles( sphere, -tieAmount + 1 );
	    }
	}
	else {
	    if ( tieAmount <= 0 ) {
		// we already won - add none.
	    }
	    else if ( tieAmount > maxAmount ) {
		// will lose anyway - add none.
	    }
	    else {
		// add enough to tie.
		supporters.putTiles( sphere, tieAmount );
	    }
	}

	return Move.newChooseSupporters( dyn, supporters );
    } 
}

