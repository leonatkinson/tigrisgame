/* File: Rules.java
 * Description:  Encapsulation of the rules mechanics.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * A class that encapsulates the rules of the tigris and euphrates game.
 * <li>Determine whether a move is legal.
 * <li>Determine whether the game is over.
 * <li>Determine who's turn it is to move. (set action, move dynasty)
 * <li>Determine scoring for any actions.
 */

package com.tigrisgame.rules;

import java.io.Serializable;

public class Rules implements Serializable, Cloneable
{
    // game parameters.
    public static final int ACTIONS_PER_TURN = 2;
    public static final int TILES_PER_PLAYER = 6;
    // starting tiles in the bag,
    // does NOT include the 10 temples on the board.
    public static final int 	
	TOTAL_BLACK = 30,
	TOTAL_RED   = 47,
	TOTAL_BLUE  = 36,
	TOTAL_GREEN = 30;

    boolean fuegiVariant = false;
    boolean englishVariant = false;

    // variable used for formatting long error messgaes.
    String errMsg = null;

    // temporary variales for processing moves.
    // however, they are serialized the Game, so we 
    // can't make them transient.
    private TigrisGame game;
    private Board board;

    private Rules()
    {
    }

    public Rules( TigrisGame game, boolean english, boolean fuegi )
    {
	setGame( game );
	this.englishVariant = english;
	this.fuegiVariant = fuegi;
    }

    public Object clone()
    {
	try
	    {
		Rules copy = (Rules) super.clone();
  		return copy;
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    public void setGame( TigrisGame game )
    {
	this.game = game;
	this.board = game.getBoard();
    }

   public boolean isEnglish()
   {
      return englishVariant;
   }

    // ************************************************************************
    // IS LEGAL
    // ************************************************************************

    // throws IllegalMoveException if "false", otherwise "true"
    public void isLegal( Move move ) throws IllegalMoveException
    {	
	int gameState = game.getState();

	if ( move == null ) {
	    errMsg = "Error: Game.isLegal called with null parameters";
	    throw new IllegalMoveException( errMsg );
	}

	if ( gameState == TigrisGame.GAME_OVER ) {
	    throw new IllegalMoveException("The game is over");
	}

	if ( move.getDynasty() != game.getMoveDynasty() ) {
	    throw new IllegalMoveException( "Not your turn" );
	}


	if ( move.isPositionLeader() || 
	     move.isPlaceTile() || 
	     move.isPlayCatastrophe() || 
	     move.isBuildMonument() ||
	     move.isChooseTreasure() ) 
	   {
	      Location loc = move.getLocation();
	      if ( loc != null && board.getSpace( loc ) == null )
		 // didsplay error message with 1 indexed values.
		 throw new IllegalMoveException("row=" + (loc.getRow()+1) + 
						" col=" + (loc.getCol()+1) + 
						" is off the board");
	   }

	// call the appropriate check method based on the game state
	// and the move type.
	if ( gameState == TigrisGame.WAIT_ACTION ) {
	    if ( move.isPositionLeader() ) {
		checkLeaderMove( move );
	    } else if ( move.isPlaceTile() ) {
		checkTileMove(  move );
	    } else if ( move.isPlayCatastrophe() ) {
		checkCatastropheMove( move );
	    } else if ( move.isSwapTiles() ) {
		checkSwapMove( move );
	    } else {
		// wrong action type for WAIT_ACTION
		throw new IllegalMoveException( "Player action expected");
	    }
	}
	else if ( gameState == TigrisGame.WAIT_TREASURE ) {
	    checkTreasureMove( move );
	}
	else if ( gameState == TigrisGame.WAIT_ATTACK_SUPPORT ||
		  gameState == TigrisGame.WAIT_DEFEND_SUPPORT ) { 
	    checkSupportersMove( move );
	}
	else if ( gameState == TigrisGame.WAIT_CONFLICT_ORDER ) {
	    checkConflictOrderMove( move );
	}
	
	else if ( gameState == TigrisGame.WAIT_MONUMENT ) {
	    checkMonumentMove( move );
	}
	else {
	    // unknown player move type
	    throw new IllegalMoveException("Unknown Move Type " + move );
	}
    }

    // illegal if not on land, not next to temple, or merges kingdoms
    // must also be placed on an empty space
    public void checkLeaderMove( Move move )
	throws IllegalMoveException
    {
	int dynasty = game.getMoveDynasty();
	Leader leader = new Leader( move.getSphere(), dynasty );
	Space leaderSpace = board.getSpace( move.getLocation());
	Space oldLeaderSpace = board.getSpace( leader );
	
	// this is the special case for
	// the "withdraw leader to my hand" move
	if ( leaderSpace == null ) {
	    if ( oldLeaderSpace == null ) {
		errMsg = "Leader is *already* in your hand";
		throw new IllegalMoveException( errMsg );
	    }
	    else
		return;
	}
	if ( leaderSpace.isWater() )
	    throw new IllegalMoveException
		( IllegalMoveException.LEAD_OVER_RIVER );
	
	if ( ! leaderSpace.isEmpty() )
	    throw new IllegalMoveException
		( IllegalMoveException.LEAD_OVER_FULL );
	
	// count the number of adjacent temples
	if ( board.getNumAdjacentTiles( leaderSpace,Sphere.RED ) == 0 )
	    throw new IllegalMoveException
		( IllegalMoveException.LEAD_NEXT_TEMPLE );
	
	// IMPORTANT NOTE: Game state is temporarily altered to update
	// the kingdoms so this can be checked.  It must be undone!!
	// this is necessary for tricky moves where moving the leader
	// breaks or completely elimates a kingdom.
	if ( oldLeaderSpace != null )
	    {
		board.emptyLocation( oldLeaderSpace );
		
		// update kingdoms for one space changing.
		board.updateKingdoms( oldLeaderSpace );
	    }
	
	boolean joinTwo = false;
	int numKings = board.getNumAdjKingdoms( leaderSpace );
	
	if ( numKings >= 2 )
	    {
		joinTwo = true;
	    }
	
	// IMPORTANT: Undoing the change. 			
	if ( oldLeaderSpace != null ) {
	    board.putLeader( oldLeaderSpace, 
			     move.getSphere(), dynasty );
	    
	    // this is the update kingdoms for adding 
	    // to a kingdom ONE space. so we can use the less
	    // expensive updateKingdoms
	    board.updateKingdoms( oldLeaderSpace );
	}

	// IMPORTANT: Delay throwing the exception 
	// so we can undo the change.
	if ( joinTwo )
	    {
		throw new IllegalMoveException
		    ( IllegalMoveException.LEAD_JOIN_KING );
	    }
    }

    // a placement of a tile is valid as long 
    // as it agress with water rules 
    // (farm on river, otherwise on land),
    // there isn't already a tile or catastrophe there,
    // and it doesn't merge more than three kingdoms.
    // the player must have the tiles in his screen. 
    public void checkTileMove( Move move )
	throws IllegalMoveException
    {
	int dynasty = game.getMoveDynasty();
	Space placeSpace = board.getSpace( move.getLocation());
	Sphere placeSphere = move.getSphere();
	
	if ( game.getPlayer( dynasty ).getNumTiles( placeSphere ) <= 0 ) {
	    throw new IllegalMoveException("You don't have any " +
					   move.getSphere() +
					   " tiles left");
	}
	
	// blue must be placed on a river, 
	//otherwise must be placed on land.
	if ( placeSpace.isWater()  && ! placeSphere.isBlue() )
	    throw new IllegalMoveException
		(  IllegalMoveException.TILE_OTHER_OVER_RIVER );
	
	if ( placeSpace.isLand() && placeSphere.isBlue() )
	    throw new IllegalMoveException
		( IllegalMoveException.TILE_BLUE_OVER_LAND );
	
	if ( ! placeSpace.isEmpty() )
	    throw new IllegalMoveException
		( IllegalMoveException.TILE_OVER_FULL );
	
	int numKings = board.getNumAdjKingdoms( placeSpace );
	if ( numKings >= 3 )
	    throw new IllegalMoveException
		( IllegalMoveException.TILE_JOIN_THREE );
    }

    // legal if not a treasure, monument, or leader
    // player must also have catastrophes left.
    public void checkCatastropheMove( Move move )
	throws IllegalMoveException
    {
	Space catSpace = board.getSpace( move.getLocation());
	int dynasty = game.getMoveDynasty();

	if ( game.getPlayer( dynasty ).getNumCatastrophes() <= 0 )
	    throw new IllegalMoveException( "You don't have any " +
					    "Catastrophes left");
	
	if ( catSpace.hasTreasure() )
	    throw new IllegalMoveException
		(  IllegalMoveException.CAT_OVER_TREAS );
	
	if ( catSpace.hasMonument() )
	    throw new IllegalMoveException
		( IllegalMoveException.CAT_OVER_MON );
	
	if ( catSpace.hasLeader() )
	    throw new IllegalMoveException
		( IllegalMoveException.CAT_OVER_LEAD );

	if ( catSpace.hasCatastrophe() )
	    throw new IllegalMoveException
		( IllegalMoveException.CAT_OVER_CAT );
    }
    
    public void checkSwapMove( Move move )
	throws IllegalMoveException
    {
	//check color of tiles versus tiles in players screen;
	Sphere spheres [] = Sphere.getSpheres();
	int dynasty = game.getMoveDynasty();	

	for ( int ii=0; ii< spheres.length; ii++) {
	    // check if they have enough tiles to swap in 
	    // the appropriate sphere number.
	    if ( move.getTiles().getNumTiles( spheres[ii] ) >
		 game.getPlayer( dynasty ).getNumTiles( spheres[ii] ))
		throw new IllegalMoveException("You don't have enough "
					       + spheres[ii] + 
					       " tiles to swap");
	}
    }
    
    public void checkConflictOrderMove( Move move )
	throws IllegalMoveException
    {
	if ( ! move.isChooseSphere() ) {
	    // wrong action type for WAIT_CONFLICT_ORDER
	    throw new IllegalMoveException("Player choose conflict " +
					   " order expected" );
	}

	// we know from the game state that there is a conflict 
	// caused by the last action, there for we just need to
	// check if the sphere is one of the conflictors.
	Kingdom [] adjKings = board.
	    getAdjacentKingdoms( game.getLastSpace() );
	Sphere [] conflicters = adjKings[0].
	    getConflictingSpheres( adjKings[1] );	
	
	//StringBuffer conflictStrBuf = new StringBuffer();
	
	for ( int ii=0;ii<conflicters.length; ii++) {
	    // help build an error string while we are at it.
// 	    conflictStrBuf.append( conflicters[ii] );
// 	    conflictStrBuf.append( " " );
	    
	    if ( move.getSphere().isSphereEqual( conflicters[ii] ))
		// a match, choice is okay.
		return;
	}
	
	// didn't find it, must be an error
	throw new IllegalMoveException
	   ( IllegalMoveException.SPHERE_NOT_CONFLICT );
    }
    
    public void checkSupportersMove( Move move )
	throws IllegalMoveException
    {
	if ( ! move.isChooseSupporters() ) {
	    // wrong action type for WAIT_SUPPORTERS
	    errMsg = "Player choose supporters expected";
	    throw new IllegalMoveException( errMsg );
	}		

	// if the state is WAIT_XXX_SUPPORT 
	// then we know there is a conflict.
	
	// first, find the sphere that players 
	// can legally add from their screen.
	Sphere supportSphere;
	String supportStr;
	Conflict conflict = game.getConflict();
	if ( conflict.isInternal() ) {
	    supportSphere = Sphere.RED;
	    supportStr = "an Interal Conflict";
	}
	else {
	    supportSphere = conflict.getSupportSphere();
	    supportStr = "a " + new Sphere( supportSphere )
		+ " External Conflict";
	}
	
	Sphere [] allSpheres = Sphere.getSpheres();
	for ( int ii=0; ii<allSpheres.length; ii++) {
	    if ( supportSphere.isSphereEqual(allSpheres[ii])) {
		// it is the right sphere, 
		// make sure they have enough tiles.
		if ( move.getTiles().
		     getNumTiles( supportSphere ) >
		     game.getPlayer( game.getMoveDynasty() ).
		     getNumTiles( supportSphere ) ) {
		    errMsg = "Cannot add " + move.getTiles().
			getNumTiles( supportSphere ) +
			" supporters.  You only have " +
			game.getPlayer( game.getMoveDynasty() ).
			getNumTiles( supportSphere )+ " tile(s)";
		    throw new IllegalMoveException( errMsg );
		}
	    }
	    else {
		// it is the wrong sphere, 
		// make sure they have zero support.
		if ( move.getTiles().
		     getNumTiles( allSpheres[ii] ) != 0 ) {
		    
		    throw new IllegalMoveException
			(  IllegalMoveException.SUPPORT_WRONG_COLOR );
		}
	    }
	}
	
	// english variant forbids playing attacking tiles unless
	// there is a chance of winning.
	int supportPlayed = move.getTiles().
	   getNumTiles( supportSphere );
	if ( englishVariant && 
	     game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT ) {
	   if ( conflict.getDefendBoardSupport() >= 
		conflict.getAttackBoardSupport() + 
		supportPlayed && supportPlayed > 0 ) {
	      throw new IllegalMoveException
		 ( IllegalMoveException.SUPPORT_NO_DUMPING );
	   }
	}
	
	// english variant forbids playing defending unless
	// the amount played will result in a tie.
	if ( englishVariant && 
	     game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT &&
	     supportPlayed > 0 ) {
	   if ( conflict.getDefendBoardSupport() + 
		supportPlayed != 
		conflict.getAttackBoardSupport() + 
		conflict.getAttackScreenSupport() ) {
	      throw new IllegalMoveException
		 ( IllegalMoveException.SUPPORT_NO_DUMPING );
	   }
	}
    }

    public void checkMonumentMove( Move move )
	throws IllegalMoveException
    {
	if ( ! move.isBuildMonument() ) {
	    // wrong action type for WAIT_MONUMENT
	    throw new IllegalMoveException("Player build " + 
					   "monument expected");
	}		

	Space playedSpace = board.getSpace( move.getLocation() );
	
	if ( playedSpace == null )
	    // if no space is selected they choose not to build a
	    //  monument, this IS legal. No need for further checks.
	    return;
	
	Space lastSp = board.getSpace( game.getLastSpace() );
	boolean includesLastSpace = 
	    lastSp.isLocationEqual(playedSpace) ||
	    lastSp.isLocationEqual(board.getEastSpace(playedSpace)) ||
	    lastSp.isLocationEqual(board.getSouthSpace(playedSpace)) ||
	    lastSp.isLocationEqual(board.getEastSpace
				   (board.getSouthSpace(playedSpace)));
	if ( !includesLastSpace )
	    throw new IllegalMoveException
		( IllegalMoveException.MON_INCLUDE_LAST ); 
	
	if ( ! board.isTileSquare( playedSpace ))
	    throw new IllegalMoveException
		( IllegalMoveException.MON_TILE_SQUARE );
	
	Sphere sphere1 = move.getSphere();
	Sphere sphere2 = move.getSphere2();
	if ( sphere2.isSphereEqual ( sphere1 ))
	    throw new IllegalMoveException( "The first and second " +
					    "spheres of a Monument " +
					    "must be different" );
	
	if ( ! sphere1.equals( lastSp.getTile() ) &&
	     ! sphere2.equals( lastSp.getTile() ) )
	    throw new IllegalMoveException
		( IllegalMoveException.MON_WRONG_COLOR );
	
	// check for duplicate monument type.
	Monument [] oldMons = board.getMonuments();
	for ( int ii=0; ii< oldMons.length; ii ++ ) {
	    // if the combination of spheres is the same
	    // order is irrelavent.
	    if ((sphere1.isSphereEqual(oldMons[ii].getFirstSphere()) ||
		 sphere1.isSphereEqual(oldMons[ii].getSecondSphere()))
		&&
		(sphere2.isSphereEqual(oldMons[ii].getFirstSphere())||
		 sphere2.isSphereEqual(oldMons[ii].getSecondSphere())))
		throw new IllegalMoveException( "A " + sphere1 + "/" + 
						sphere2 + " monument "+
						"already exists");
	}
    }

    public void checkTreasureMove( Move move )
	throws IllegalMoveException
    {
	if ( ! move.isChooseTreasure() ) {
	    // wrong action type for WAIT_TREASURE
	    errMsg = "Player choose treasure expected";
	    throw new IllegalMoveException( errMsg );
	}

	Space treasSpace = board.getSpace( move.getLocation());
	if ( ! treasSpace.hasTreasure() ) {
	    errMsg =  "Selected space doesn't have a treasure.";
	    throw new IllegalMoveException( errMsg );
	}
	
	Kingdom treasureKing = board.getKingdom( treasSpace );
	if ( treasureKing == null ) {
	    errMsg = "Selected space is not in a Kingdom";
	    throw new IllegalMoveException( errMsg );
	}
	
	Space [] treasureSpaces = treasureKing.getTreasureSpaces();
	
	if ( treasureSpaces.length < 2 ) {
	    errMsg = "You must choose a treasure "  +
		"from a kingdom with 2 or more";
	    throw new IllegalMoveException( errMsg );
	}	
	
	// if you are trying to take a NON must take treasure,
	// all the treasure spaces must also not be must take
	if ( ! treasSpace.hasMustTakeTreasure() ) {
	    for ( int tt=0; tt < treasureSpaces.length; tt ++ ) {
		if ( treasureSpaces[tt].hasMustTakeTreasure() )
		    throw new IllegalMoveException
			( IllegalMoveException.TREAS_CORNER );
	    }
	} else {
	    // okay because they took must-take.
	}
	
	if ( treasureKing.getLeader( Sphere.GREEN ) == null ) {
	    errMsg="No trader (green leader) in selected kingdom";
	    throw new IllegalMoveException( errMsg );
	}
    }

    // ************************************************************************
    // HELPER METHODS
    // ************************************************************************

    // does all the checks for the end of the action such as
    // monuments building, treasure grabbing, monument VP's.
    // forwards the game to the next action if no player input is needed
    // or changes the game state to await the input w/o advancing.
    // NOTE: assumes that the kingdoms are up to date as of the previous action
    protected void nextAction()
    {
	int gameState = game.getState();
	Space lastSpace = game.getLastSpace();

	// CHECK FOR MONUMENT
	// rules errata: it doesn't strictly say whether treasures 
	// are taken before monuments, and it does matter in some instances
	// where a green trader would be orphaned by the monument build.
	// so because treasures say "at the end" and also to save some kingom
	// calculating processing, monuments are done first.
	// As a consequence, you can't remove a treasure from a temple and
	// then use that temple to build a monument.
	if ( gameState != TigrisGame.WAIT_MONUMENT && 
	     gameState != TigrisGame.WAIT_TREASURE && 
	     lastSpace != null &&
	     board.isInTileSquare( board.getSpace( lastSpace ))) 
	    {
		// update state for the monument builder.
		// this player is always the player who's turn it is.
		// also return, since we aren't ready for the next turn.
		game.setState( TigrisGame.WAIT_MONUMENT );
		game.setMoveDynasty( game.getActionDynasty() );
		return;
	    }


	// since kingdoms are up to date when this is called, we can now...
	// board.updateKingdoms();
	
	// CHECK FOR TREASURE!
	// we know that any treasure getting will be in the kingdom that a tile
	// or leader was just played in
	// treasure to be taken must be a
	if ( lastSpace != null ) {

	    // previous if statement ensures getSpace is valid.
	    // Move may later throw exceptions if you try to get
	    // an invalid parameter
	    Kingdom king = board.getKingdom( board.getSpace( lastSpace ));

	    if ( king != null &&
		 king.getNumTreasures() > 1 ) {
		
		// TREASURE TO BE TAKEN!!
		// let's see if there is a green leader in the kingdom.
		Leader trader = king.getLeader( Sphere.GREEN );
		if ( trader != null ) {
		    // update state for the treasure hound.
		    // also return, since we aren't ready for the next turn.
		    game.setState( TigrisGame.WAIT_TREASURE );
		    game.setMoveDynasty( trader.getDynasty() );
		    return;
		}
	    }
	}
	
	// cache the action dynasty.  set it back layer.
	int tmpActionDynasty = game.getActionDynasty();

	game.fireGameEvent( GameEvent.ACTION_FINISHED, 
			    new Integer( tmpActionDynasty ), 
			    null, null );
	
	// if neither monument nor treasure ...
	// REGULAR END OF TURN
	game.setState( TigrisGame.WAIT_ACTION );
	
	if ( game.getActionNumber() == ACTIONS_PER_TURN )
	    {
		game.fillPlayerTiles();

		// Monument victory points are assigned at the 
		// end of a player's turn.
		// this is done BEFORE currentTurn is 
		// updated so that the the currentTurn
		// player can be used to give points to in assign...()
		assignMonumentVictoryPoints();

		game.fireGameEvent( GameEvent.TURN_FINISHED, 
				    new Integer( tmpActionDynasty ), 
				    null, null );

		tmpActionDynasty ++;
		if ( tmpActionDynasty == game.getNumPlayers() )
		    tmpActionDynasty = 0;

		// update player and turn to next player
		game.setActionDynasty( tmpActionDynasty );
		game.setActionNumber( 1 );
		game.setMoveDynasty( tmpActionDynasty );

		// check for game over at the end of every turn.
		// game ends if players cannot fill tiles, OR
		// if there are 2 or less treasures.
		int treasures = board.getNumTreasures();
		if ( game.getTileBagWasExhausted()  ||
		     treasures <= 2 ) {
		    game.setState( TigrisGame.GAME_OVER );

		    game.fireGameEvent
			( new GameEvent (game, GameEvent.GAME_OVER,
					 new Integer( treasures ),
					 null, null ));
		}
	    }
	else
	    {
		// mnmn
		game.setActionNumber( game.getActionNumber() + 1 );
		game.setMoveDynasty( tmpActionDynasty );
	    }
    }

    protected void scoreTilePoint( Kingdom king, Sphere sphere )
    {
	// add to existing kingdom, assign VP
	Leader colorLeader = king.getLeader( sphere );
	Leader blackLeader = king.getLeader
	    ( new Sphere( Sphere.BLACK ));
	
	// NOTE: might have to do some mucking around 
	// here to implement fuegi variant, two possibilities:
	// 1) delay this vp 
	//    - need to keep extra state until possible monument
	// 2) subtract the first point from the monument scoring.
	//    - need to keep track of who/when the monuemnt was built
	//    - what if you don't score that point?
	
	if ( colorLeader != null ) {
	    game.getPlayer( colorLeader.getDynasty()).
		addVictoryPoint( sphere );
	    
	    // send listener event.
	    game.fireGameEvent( GameEvent.TILE_VP_AWARDED,
				new Integer( colorLeader.getDynasty()), 
				sphere, sphere );
	}
	else if ( blackLeader != null ) {
	    game.getPlayer( blackLeader.getDynasty()).
		addVictoryPoint( sphere );
	    
	    // send listener event.
	    game.fireGameEvent( GameEvent.TILE_VP_AWARDED,
				new Integer( blackLeader.getDynasty()), 
				sphere, Sphere.BLACK );
	}
	else {
	    // boo hoo.. no one to claim the point.
	}
    }

   // knows all the rules of conflicts and will
   // resolve the given conflict on the board
   // by removing tiles and assign victory points
   // returns tiles that were discarded in the fray
   // tileBag is passed in so we know where to stuff used up tiles.
   protected void resolveConflict()
   {
      // consequences of a conflict
      // victory points assigned.
      // leaders removed
      // tiles possibly removed
      // tiles drawn from player hand and discarded.
      Conflict conflict = game.getConflict();
      
      if ( conflict.getAttackScreenSupport() == -1 ||
	   conflict.getDefendScreenSupport() == -1 ) {
	 System.out.println("Warning: tried to call Conflict.resolve " +
			    "without setting screen supporters for " +
			    "attacker and defender");
	 return;
      }
      
      // both attacker and defender should be the same.
      Sphere sphere = new Sphere( conflict.getAttacker().getSphere() );
      
      Leader loser = conflict.getLoser();
      Leader winner = conflict.getWinner();
      
      // the losing leader is always taken off the board.
      Space loserSpace = board.getSpace( loser );
      Kingdom loserKing = board.getKingdom( loserSpace );
      
      // DEBUG
      if ( loserSpace == null ) {
	 System.out.println("could not find " + loserSpace + " in " + board );
	 throw new RuntimeException("corrupt game state in Conflict");
      }
      
      board.emptyLocation( loserSpace );
      game.getPlayer( loser.getDynasty() ).addLeader( sphere );

      int victoryPoints = 0;
      // assign victory points which are specific to the type of conflict.
      if ( conflict.isInternal() ) {
	 victoryPoints = 1;
	 
	 // to the victor, the spoils.
	 game.getPlayer( winner.getDynasty() ).
	    addVictoryPoint( Sphere.RED );
	 
	 game.fireGameEvent
	    ( new GameEvent (game, GameEvent.CONFLICT_VP_AWARDED,
			     new Integer( winner.getDynasty()),
			     Sphere.RED, 
			     new Integer( 1 ) ));
      }
      else {
	 // external conflict.
	 // determine the number of points scored by 
	 // detremoving the number of tokens removed.
	 Space [] maybeRemove = loserKing.getTileSpaces( sphere );
	 int removed = 0;
	 
	 for ( int ii=0; ii<maybeRemove.length; ii++) {
	    // remove the tile if it doesn't either have 
	    // a treasure or support a leader.
	    // formerly removeNonSupportingTiles(...) method.
	    if ( maybeRemove[ii].hasTreasure() || 
		 sphere.isSphereEqual( Sphere.RED ) &&
		 board.hasAdjacentLeader( maybeRemove[ii] )) {
	       continue;
	    }
	    
	    // empty the space and fire a game event.
	    board.emptyLocation( maybeRemove[ii] );
// 	    game.fireGameEvent( new GameEvent(game,GameEvent.TILE_REMOVED,
// 					      maybeRemove[ii],null,null ));
	    removed ++;
	 }
	 
	 
	 victoryPoints = removed + 1;
	 
	 game.getPlayer( winner.getDynasty() ).
	    addVictoryPoints( sphere, victoryPoints );
	 
	 game.fireGameEvent
	    ( new GameEvent(game, GameEvent.CONFLICT_VP_AWARDED,
			    new Integer( winner.getDynasty()),
			    sphere, 
			    new Integer( victoryPoints ) ));
      }

      // conflict 'summary' events.
      game.fireGameEvent
	  ( new GameEvent (game, GameEvent.CONFLICT_WON,
			   new Integer( winner.getDynasty()),
			   sphere, null ));
      game.fireGameEvent
	  ( new GameEvent (game, GameEvent.CONFLICT_LOST,
			   new Integer( loser.getDynasty()),
			   sphere, null ));
   }
   
    // called if a tile placement merges two or more kingdoms or after
    // one sphere of an external conflict resolves.
    // updates game state and decision as appropriate to finish.
    // if it is determined that everything was peaceful, it cleans
    // up the conflict by placing the tile and moving to the next turn.
    // NOTE: assumes that the kingdoms are updated up to that point
    protected void nextExternalConflict()
    {
	// resolution process
	// 1st determine how many warring parties
	// if more than one, state is WAIT_CONFLICT_ORDER
	// query for support of the attacker
	// query for support from the defender
	// calculate support for each side
	// remove the losing side and supporters
	// assign appropriate amount of vp
	
	game.setConflict( null );
	
	// find out who the attacker is!
	Kingdom[] adjKings = board.getAdjacentKingdoms( game.getLastSpace());
	Sphere [] conflicters;	

	if ( adjKings.length < 2 )
	    // no conflict if number of adjacent kingdoms is reduced below 2.
	    conflicters = new Sphere[0];
	else
	    conflicters = adjKings[0].getConflictingSpheres( adjKings[1] );

	switch( conflicters.length ) {
	
	case 4:
	case 3:
	case 2:
	    // a multiple leader conflict, 
	    // the placer of the tile must choose 
	    // the conflict order
	    game.setState( TigrisGame.WAIT_CONFLICT_ORDER );
	    
	    // this isn't always redundant if we have already fought
	    // case 3 or case 4 above previously.
	    game.setMoveDynasty( game.getActionDynasty() );
	    break;
	case 1:
	    Conflict conflict = Conflict.
		newExternalConflict( game, conflicters[0] );
	    game.setConflict( conflict );
	    
	    // send listener event.
	    game.fireGameEvent( GameEvent.EXTERNAL_CONFLICT,
				conflict.getAttacker(), 
				conflict.getDefender(), null );
	    
	    game.setState( TigrisGame.WAIT_ATTACK_SUPPORT );
	    game.setMoveDynasty(  conflict.getAttacker().getDynasty() );

	    break;
	case 0:
	    // no more conflicts or a peacefull merger.
	    // it's now okay to place the tile.
	    JoinTile joinTile = board.getJoinTile();
	    board.putTile( game.getLastSpace(), (Sphere) joinTile );
	    
	    // assuming the kingdoms were valid after the previous
	    // external conflict, we only have to updateKingdoms(space)
	    // to reflect the one added space.
	    board.updateKingdoms( game.getLastSpace() );
	    
	    // send listener event.
	    game.fireGameEvent( GameEvent.KINGDOM_JOIN_RESOLVED,
				game.getLastSpace(), null, null );
	    
	    nextAction();
	    break;
	}
    }

   // CM 9/4/02.
   // An attempt at a much more efficient implementation.
   // JProbe puts a typical case at only 60% the runtime 
   // of the previous implementation.  not bad!
   // after several bugs, confirmed working 11:30 9/4/2002.
   protected void assignMonumentVictoryPoints()
   {
      Sphere spheres [] = Sphere.getSpheres();
      int scoreDynasty = game.getActionDynasty();
      Space leaderSpaces [] = board.
	 getLeaderSpaces( scoreDynasty, spheres ); // this is O(spaces).
      
      Monument [] mons = board.getMonuments();  // this is O(spaces/4).
      for ( int mm=0; mm<mons.length; mm++ ) {  // max of 6.
	 
	 // would be better to grab something like board.getMonumentSpaces 
	 // but that isn't implemented, so we do an extra 6 * O(spaces).
	 Space monSpace = board.getUpperLeftSpace( mons[mm] );
	
	 if ( monSpace == null ) {
	    throw new RuntimeException( "inconsistant state error " + board );
	 }
 
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
	       
	       // add vp to appropriate player.
	       game.getPlayer(scoreDynasty).addVictoryPoint(spheres[ss]);
	       
	       // send listener event.
	       game.fireGameEvent( GameEvent.MONUMENT_VP_AWARDED, 
				   new Integer(scoreDynasty),
				   spheres[ss], null );
	    }
	 }
      }
   }
}
