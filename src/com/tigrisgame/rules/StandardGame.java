/* File: StandardGame.java
 * Description:  Concrete implementation of an E&T game.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE:
 * it is assumed that dynasty = player index in the array, 
 * enforced in constructors, but the players could be modified
 *
 */

package com.tigrisgame.rules;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

public class StandardGame implements TigrisGame, Serializable, Cloneable
{
   // GAME STATE
   protected Board board;
   protected Player [] players;
   
   // information about who's turn it is (player who makes actions),
   // how many actions they have taken, 
   // and who will make the current move (possibly a responding sub-action).
   private int gameState = WAIT_ACTION; // renamed from state
   private int actionNumber = 1;
   private int actionDynasty = 0;
   private int moveDynasty = 0;
   private boolean tileBagWasExhausted = false;

   protected OrderedTileBag tileBag;
//     protected Ranecu rand = new Ranecu( 123 );
//     protected TileBag totalTiles, tileBag, discardPile = new TileBag();

    // used for what SPACE caused a current conflict
    // used to determine if it is a player's first opportunity 
    // to build a current monument
    // used to save processing in treasure check
    // A larger list for record keeping is outside of the scope of StandardGame
    //protected Move lastAction = null;
    protected Location lastSpace = null;

    protected Conflict m_conflict = null; 
    protected Rules rules;

    // listeners for gameEvents.
    protected transient Vector gameListeners = null;

    // the default game constructor
    // a two player game using the standard map and standard tiles
    private StandardGame()
    {
	//init( new GameOptions() );
    }

    // the constructor that sets only number of players,
    // all other as default constructor
    public StandardGame( int numPlayers )
    {
	init( new GameOptions( numPlayers ) );
    }

    // the constructor from an arbitrary set of options.
    public StandardGame( GameOptions options )
    {
	init( options );
    }

    // a method for common initialization from 
    // starting paramters for a new game.
    // the game will always start as the first action for player 0.
    // -- allows different starting tiles in the bag
    // (in addition to those on the board and those that the players have)
    // -- allows arbitrary seed for training/repeatability.
   public void init( GameOptions options )
   {
//       switch ( options.boardImpl ) {
//       case 1:
// 	 this.board = new Board1( options.mapfile );
// 	 break;
//       case 2:
// 	 this.board = new Board2( options.mapfile );
//       default:
// 	 System.out.println("WARNING: unsuppored board type, using 2");
// 	 this.board = new Board2( options.mapfile );
//       }

      // REMOVED: reading map.txt from a file.
      // entire string is stored in Board2

      this.board = new Board2();

      this.players = new Player[ options.playerNames.length ];
      for ( int pp=0; pp<players.length; pp++ ) {
	 players[pp] = new Player( pp, options.playerNames[pp] );
	 players[pp].setClock( options.baseTime );
      }
      
// 	rand.setSeed( options.seed );
      // same seeded randomness as the tile bag, so a fixed options
      // seed reproduces the tile order *and* the starting player.
      Random rand = new Random( options.seed );
      tileBag = new OrderedTileBag( options.tiles );
      tileBag.randomize( rand );

      // randomly choose the starting player.  both the mover and the
      // action-rotation index have to start here; everything else
      // (next turn = actionDynasty+1) follows from them.
      int startingPlayer = rand.nextInt( players.length );
      moveDynasty = startingPlayer;
      actionDynasty = startingPlayer;
// 	totalTiles = new TileBag();
// 	totalTiles.putTiles( tileBag );
// 	totalTiles.putTiles( board );

      rules = new Rules( this, !options.tileDumping, false );
      fillPlayerTiles();
   }

    // completely initialized this object's data with the fromGame parameter, 
    // the fromGame is not affected and any mutable objects are copied.
    // new fields initialized if inputs null,
    // so also sufficient to use as a copy constructor.
    public void init( TigrisGame fromGame )
    {
	// Board type, init object recycle available on Board2,
	// how to handle copy of unknown Board type?
	// but create a new Board if necessary.
	if ( this.board != null ) this.board.init( fromGame.getBoard() );
	else this.board = fromGame.getBoard().deepCopy();

	// Player [] type, init objects recycle if non-null
	// otherwise create with copy.
	if ( this.players != null && 
	     this.players.length == fromGame.getNumPlayers()) 
	    for ( int pp=0; pp<players.length; pp++ ) 
		players[pp].init( fromGame.getPlayer(pp) );
	else {
	    players = new Player [ fromGame.getNumPlayers() ]; 
	    for ( int pp=0; pp<players.length; pp++ )  
		players[pp] = (Player) (fromGame.getPlayer(pp));
	}

// 	this.rand.setSeed( fromGame.getSeed() );
	
	// OrderedTileBag replaces regular TileBag with
	// tileBag guaranteed to be non-null.

	// TODO -- NOT GOING TO WORK, we are returning new TileBag()!!!
	this.tileBag.init( (OrderedTileBag) fromGame.getTileBag() );
	
	// immutable ints.
	this.gameState = fromGame.getState();
	this.moveDynasty = fromGame.getMoveDynasty();
	this.actionDynasty = fromGame.getActionDynasty();
	
	// space row/col is immutable.
	this.lastSpace = (fromGame.getLastSpace() == null) ? null : 
	    (Space) fromGame.getLastSpace();
	this.m_conflict = (fromGame.getConflict() == null) ? null :
	    (Conflict) fromGame.getConflict().clone();

	this.rules.setGame( this );

	// NOT COPIED.
	//this.gameListeners 
    }
    
    // system provided shallow copy.
    // then deepCopy the fields we want.
    public Object clone()
    {	
	try
	    {
		StandardGame copy = (StandardGame) super.clone();

		copy.board = board.deepCopy();

		copy.tileBag = (OrderedTileBag) tileBag.clone();

		// removing these fields.
// 		copy.discardPile = new TileBag( discardPile );
// 		copy.rand = ( rand == null ) ? null : 
// 		    new Ranecu( rand.getSeed() );

		if ( m_conflict != null ) {
		    // update the new "inner class" ref to the copy.
		    copy.m_conflict = (Conflict) m_conflict.clone();
		    //copy.m_conflict.game = copy;
		};

		Player [] clonePlayers = new Player[ players.length ];
		for ( int ii=0; ii<clonePlayers.length; ii++ ) {
		    clonePlayers[ii] = (Player) players[ii].clone();
		}

		copy.players = clonePlayers;

		copy.gameListeners = null;

		copy.rules = (Rules) rules.clone();
		copy.rules.setGame( copy );

		return copy;
	    }
	catch (CloneNotSupportedException e)
	    {
		// this shouldn't happen, since we are Cloneable
		return null;
	    }
    }

    // ************************************************************************
    // GET / SET METHODS
    // ************************************************************************

    public Rules getRules()
    {
	return rules;
    }

    public Board getBoard()
    {
	return board;
    }

    public Conflict getConflict()
    {
	return m_conflict;
    }

    public void setConflict( Conflict conflict )
    {
	m_conflict = conflict;
    }

    public TileHolder getTotalTiles()
    {
	return (TileHolder) tileBag.getTotalTiles();
    }

    // return TileHolder interface type to hopefully prevent mutations.
    public TileHolder getTileBag()
    {
	return new TileBag( tileBag );
    }

   public boolean getTileBagWasExhausted()
   {
      return tileBagWasExhausted;
   }

   public void setTileBagWasExhausted( boolean val )
   {
      tileBagWasExhausted = val;
   }

    public void hideTiles( int view )
    {
	tileBag.hideTiles();

	for ( int hh=0; hh < players.length; hh ++) {
	    if ( view != hh )
		players[hh].hideTiles();
	}
    }

    public int getActionNumber()
    {
	return actionNumber;
    }

   public void setActionNumber( int num )
   {
      this.actionNumber = num;
   }

    public int getMoveDynasty()
    {
	return moveDynasty;
    }

   public void setMoveDynasty( int dyn )
   {
      this.moveDynasty = dyn;
   }

    public int getActionDynasty()
    {
	return actionDynasty;
    }

   public void setActionDynasty( int dyn )
   {
      this.actionDynasty = dyn;
   }

//     public long getSeed()
//     {
// 	return rand.getSeed();
//     }
   
   // get the current Space object from the Board 
   // where the last action took place.
   public Space getLastSpace()
   {
      return board.getSpace( lastSpace );
   }
   
   protected void setLastSpace( Location space )
   {
      lastSpace = null;
      if ( space != null )
	 lastSpace = new LocationImpl( space );
   }

   public int getState()
   {
      return gameState;
   }
    
   public void setState( int state )
   {
      this.gameState = state;
   }

    // return a copy of the player objects.
    // a copy ensures that gui components can't alter the player list
    private Player [] getPlayers()
    {
	Player [] returnPlayers = new Player[ players.length ];
	for ( int ii=0; ii<players.length; ii++)
	    {
		returnPlayers[ii] = players[ii];
	    }
	return returnPlayers;
    }

   public int getNumPlayers()
   {
      return players.length;
   }
   
   // dynasty info implied in the argument.
   protected void setPlayer( Player pl )
   {
      players[ pl.getDynasty() ] = pl;
   }

   public Player getPlayer( int dynasty )
   {
      // we know that dynasty == array index.
      if ( dynasty >= 0 && dynasty < players.length )
	 return players[ dynasty ];
      else 
	 return null;
   }
   
   public Player getPlayer( Leader l  )
   {
      return ( l == null ) ? null : getPlayer( l.getDynasty() );
   }

    // ************************************************************************
    // MAKE MOVE
    // ************************************************************************

    public void makeMove( Move move )
	throws IllegalMoveException
    {
	// throws illegal move exception if invalid.
	isLegal( move );
	makeLegalMove( move );
    }	

    public void isLegal( Move move )
	throws IllegalMoveException
    {
	rules.isLegal( move );
    }

    // update the state of the game based on a legal move.
    public void makeLegalMove( Move move )
    {
	// from here forward we ASSUME THE MOVE IS LEGAL.
	switch ( move.getType() ) {
	case Move.POSITION_LEADER:
	    makeLeaderMove( move );
	    break;
	case Move.PLACE_TILE:
	    makeTileMove( move );
	    break;
	case Move.PLAY_CATASTROPHE:
	    makeCatastropheMove( move );
	    break;
	case Move.SWAP_TILES:
	    makeSwapMove( move );
	    break;
	case Move.CHOOSE_SPHERE:
	    makeChooseSphereMove( move );
	    break;
	case Move.CHOOSE_SUPPORTERS:
	    makeSupportMove( move );
	    break;
	case Move.BUILD_MONUMENT:
	    makeMonumentMove( move );
	    break;
	case Move.CHOOSE_TREASURE:
	    makeTreasureMove( move );
	    break;
	default:
	    System.out.println("Warning: Game.makeMove " +
			       " invalid move type " + move );
	}
    }

    public void makeLeaderMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );

	// needed to reconstruct conflicts, monuments and treasures.
	this.setLastSpace( move.getLocation() );

	// new leader to place
	Leader placeLeader = new Leader( move.getSphere(),
					 player.getDynasty() );
	Space oldSpace = board.getSpace( placeLeader );
	
	// remove from board or player object
	player.removeLeader( move.getSphere() );
	if ( oldSpace != null ) {
	    board.emptyLocation( oldSpace );
	    
	    // NOW update kingdoms.
	    // this is important for cases where "picking up" 
	    // the leader changes or elimantes kingdoms and 
	    // is necessary to determine conflicts later.
	    // See positionleader in rules.isLegal.
	    board.updateKingdoms( oldSpace );
	}
	
	// this is the special case for
	// the "withdraw leader to my hand" move.
	// we have already updated the kingdoms because oldSpace
	// must not be null to remove it from the board,
	// therefore, we can return and not check for conflicts.
	if ( move.getLocation() == null ) {
	    player.addLeader( move.getSphere() );
	    // sent listener event.
	    this.fireGameEvent( GameEvent.LEADER_WITHDRAWN, 
				placeLeader, null, null ); 
	    rules.nextAction();
	} 
	// otherwise, place on the board.
	else {
	    Space newSpace = board.getSpace( move.getLocation() );
	    board.putLeader( newSpace, placeLeader.getSphere(), 
			     placeLeader.getDynasty() ) ;
	    
	    // send listener event.
	    if ( oldSpace != null ) {
		this.fireGameEvent( GameEvent.LEADER_MOVED, 
				    placeLeader, newSpace, oldSpace ); 
	    } else {
		this.fireGameEvent( GameEvent.LEADER_PLACED, 
				    placeLeader, newSpace, null ); 
	    }
	    
	    // we can find out if there is an internal conflict by 
	    // checking for a rival leader from the old kingdom.
	    Kingdom[] adjKings = board.getAdjacentKingdoms( newSpace );
	    Leader otherLeader = null;
	    
	    // note: because only one kingdom can be adjacent for a
	    // legal move we can simply look at the first one.
	    // note: because we have already removed the original leader 
	    // from the Space contained in kingdom, the repositioning
	    //  leader within a large kingdom is NOT a conflict.
	    if ( adjKings.length > 0 )
		otherLeader = adjKings[0].getLeader(move.getSphere());
	    
	    if ( otherLeader  != null ) {
		
		// internal conflict!!
		// blood, war, guts, rape and murder!! whoohoo!
		
		// resolution process
		// 1st query for support from the agressor.
		// query for support from the defender
		// calculate support for each side
		// remove the losing side
		// assign 1 red vp
		
		// clear out any previous conflicts and replace it
		// with them new one
		// (at this point player == currentTurn == moveDynasty )
		this.setConflict( Conflict.newInternalConflict
				  ( this, placeLeader, adjKings[0].getLeader
				    ( placeLeader.getSphere() )));
		
		// send listener event.
		this.fireGameEvent(GameEvent.INTERNAL_CONFLICT,
				   placeLeader, otherLeader, null );
		
		
		this.setState( TigrisGame.WAIT_ATTACK_SUPPORT );
		// this SHOULD be redundant.
		this.setMoveDynasty( this.getActionDynasty() );
	    }
	    else {
		// NO CONFLICT for place leader.
		board.updateKingdoms( newSpace );
		rules.nextAction();
	    }
	}
    }
    
    public void makeTileMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	
	this.setLastSpace( move.getLocation() );
	Sphere tileSphere =  move.getSphere();
	
	// note: tile is placed on the board after
	// any possible external conflicts.
	
	// remove tile from player
	player.drawTile( move.getSphere() );
	
	Space tileSpace =  board.getSpace( move.getLocation() );
	
	// send listener event.
	this.fireGameEvent( GameEvent.TILE_PLACED, 
			    new Integer( this.getMoveDynasty() ), 
			    tileSphere, tileSpace );
	
	Kingdom [] adjKings = board.getAdjacentKingdoms( tileSpace );
	
	switch ( adjKings.length ) {
	    
	case 2:
	    board.putJoinTile( tileSpace, move.getSphere() );
	    
	    // send listener event.
	    this.fireGameEvent( GameEvent.KINGDOM_JOIN_CAUSED,
				tileSpace, adjKings[0], adjKings[1] );
	    rules.nextExternalConflict();
	    break;
	case 1:
	    // delegate score of points to rules.
	    rules.scoreTilePoint( adjKings[0], tileSphere );
	case 0:
	    // placed randomly, no VP assigned.
	    // break ommited from case 1 intentionally
	    
	    // no conflicts, it's now okay to (immediatly) place the tile.
	    board.putTile( tileSpace, tileSphere );			
	    
	    // update kingdoms appropriatly (the tile only 
	    // may add to one kingdom)
	    board.updateKingdoms( tileSpace );
	    
	    // process the next player action.
	    rules.nextAction();
	    
	    break;
	default:
	    System.out.println("WARNING: Game.makeMove merger of " +
			       adjKings.length + " kingdoms");
	}
    }

    public void makeCatastropheMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );

	this.setLastSpace( move.getLocation() );

	Space catSpace = board.getSpace( move.getLocation() );

	Tile oldTile = catSpace.getTile();
	
	// put any displace tiles in the discard.
	// 		discardPile.putTile( catSpace.getTile() );
	
	// place catastrohpe on board
	board.putCatastrophe( catSpace );
	
	// remove from player screen
	player.removeCatastrophe();
	
	// send listener event.
	this.fireGameEvent( GameEvent.CATASTROPHE_PLACED, 
			    new Integer( this.getMoveDynasty() ),
			    catSpace, oldTile );
	
	// remove orphaned leaders
	Space [] effectedSpaces = board.getAdjacentSpaces( catSpace );
	for ( int ii=0; ii< effectedSpaces.length; ii ++) {
	    removeOrphanedLeader( effectedSpaces[ii] );
	}
	// this only effects one kingdom,
	// and it is safe to call even if it didn't hit a kingdom
	// because of validity check in updateKingdoms( k )
	// board.updateKingdoms( board.getKingdom( catSpace ));
	board.updateKingdoms();
	
	rules.nextAction();
    }

    public void makeSwapMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	
	this.setLastSpace( null ); // no space in a swap move.
	
	TileBag swapped = new TileBag( move.getTiles() );
	
	// send listener event.
	this.fireGameEvent( GameEvent.TILES_SWAPPED,
			    new Integer( this.getMoveDynasty() ),
			    new TileBag( swapped ), null );
	
	TileBag drawn = new TileBag();
	while ( swapped.hasTiles() ) {
	   // remove from the temporary move bag
	   // "randomly" until it is empty
	   Sphere discardTile = swapped.drawTile( 0.0 );
	    
	   // remove from the player's screen.
	   player.drawTile( discardTile );
	   
	   // draw a new tile at random from the tile bag.
	   this.drawTileFromBag( player, drawn );
	}

	player.putTiles( drawn );
	if ( drawn.getNumTiles() > 0 ) {
	    fireGameEvent( GameEvent.TILES_DRAWN, 
			   new Integer(player.getDynasty()),
			   new TileBag( drawn ), null );
	}

	
	// NOTE: no update kingdoms needed here. we just swapped tiles.
	rules.nextAction();
    }

    public void makeChooseSphereMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	Conflict conflict = Conflict.
	    newExternalConflict( this, move.getSphere() );
	this.setConflict( conflict );
	
	// send listener event.
	this.fireGameEvent( GameEvent.EXTERNAL_CONFLICT,
			    conflict.getAttacker(), 
			    conflict.getDefender(), null );
	
	this.setState( TigrisGame.WAIT_ATTACK_SUPPORT );
	this.setMoveDynasty( conflict.getAttacker().getDynasty() );
	
	// okay, we have set the state and currentMove so the player
	// can choose supporters to fight the choosen conflict.
    }

    public void makeSupportMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	Conflict conflict = this.getConflict();
	// a conflict is being resolved.  either external or internal.
	// the player just choose how many supporters to add from his
	// screen. we also know that this is a legal amount.
	
	if ( conflict.getAttackScreenSupport() == -1 ) {
	    
	    // attacker chose tiles,
	    // now let the defender.
	    conflict.setAttackScreenSupport( move.getTiles() );
	    
	    // remove from the player.
	    player.drawTiles( move.getTiles() );
	    
	    // send listener event.
	    this.fireGameEvent
		(GameEvent.ATTACK_SUPPORT_SET,
		 new Integer( this.getMoveDynasty() ), 
		 new Integer( conflict.getAttackBoardSupport() ),
		 new Integer( conflict.getAttackScreenSupport() ));
	    
	    this.setState( TigrisGame.WAIT_DEFEND_SUPPORT );
	    this.setMoveDynasty( conflict.getDefender().getDynasty() );
	}
	else {
	    // both have chosen, let's fight!
	    
	    conflict.setDefendScreenSupport( move.getTiles() );
	    
	    // remove from the player.
	    player.drawTiles( move.getTiles() );
	    
	    // send listener event.
	    this.fireGameEvent
		(GameEvent.DEFEND_SUPPORT_SET,
		 new Integer( this.getMoveDynasty() ),
		 new Integer( conflict.getDefendBoardSupport() ),
		 new Integer( conflict.getDefendScreenSupport() ));
	    
	    // use rules helper class to resolve conflict. 
	    rules.resolveConflict(); 
	    
	    // update kingdoms, they are sure to have changed.
	    // in fact, it's just a bloody mess, the old kingdom is
	    // blow apart and parts or it may be annexed, not kingdoms,
	    // or seperate kingdoms, let's just update everything.
	    board.updateKingdoms();
	    
	    if ( this.getConflict().isInternal() ) {
		// clean up the conflict object
		this.setConflict( null );			
		
		// Internal conflicts end the action.
		
		// NOTE: no need to update kingdoms here because 
		// we just did 3 seconds ago.
		rules.nextAction();
	    }
	    else {
		// External conflicts might have multiples.
		// game.conflict nulled in nextExternalConflict
		rules.nextExternalConflict();
	    }
	}
    }

    public void makeMonumentMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	Space monSpace = board.getSpace( move.getLocation() );

	if ( monSpace == null ) {
	    
	    // player doesn't want to build the damn thing.
	    // no need to update kingdoms b/c no orphans
	    
	    // send listener event.
	    this.fireGameEvent( GameEvent.NO_MONUMENT_BUILT, 
				new Integer( this.getMoveDynasty() ),
				null, null );
	}		
	else {
	    
	    /*       %-0   %-2
	     * %-1  UL-A  UR-B  %-3
	     * %-4  LL-C  LR-D  %-6
	     *       %-5   %-7
	     *
	     *  Place the monument in the following order, checking for
	     *  orphaned leaders along the way on all % squares.
	     */
	    
	    Space ul = board.getSpace( move.getLocation());
	    Space ur = board.getEastSpace( ul );
	    Space ll = board.getSouthSpace( ul );
	    Space lr = board.getEastSpace( ll );
			
		// discard the tiles turned over.
//                 discardPile.putTile ( ul.getTile() );
// 		   discardPile.putTile ( ur.getTile() );
// 		   discardPile.putTile ( ll.getTile() );
// 		   discardPile.putTile ( lr.getTile() );
			
	    // place the monument on the board.
	    // must go through board.
	    Monument mon = new Monument( move.getSphere(),
					 move.getSphere2());
	    board.putMonument( ul, mon );
	    
	    // send listener event.
	    this.fireGameEvent( GameEvent.MONUMENT_BUILT,
				new Integer(this.getMoveDynasty()), mon, ul );
	    
	    // don't forget to check for orphaned leaders.
	    Space [] possibleOrphanSpaces = new Space [8];
	    possibleOrphanSpaces[0] = board.getNorthSpace( ul );
	    possibleOrphanSpaces[1] = board.getWestSpace( ul );
	    possibleOrphanSpaces[2] = board.getNorthSpace( ur );
	    possibleOrphanSpaces[3] = board.getEastSpace( ur );
	    possibleOrphanSpaces[4] = board.getWestSpace( ll );
	    possibleOrphanSpaces[5] = board.getSouthSpace( ll );
	    possibleOrphanSpaces[6] = board.getEastSpace( lr );
	    possibleOrphanSpaces[7] = board.getSouthSpace( lr );
	    for ( int ii=0; ii< possibleOrphanSpaces.length; ii ++)
		{
		    removeOrphanedLeader( possibleOrphanSpaces[ii] );
		}
	    
	    // monument happens during nextAction end of action checks,
	    // attemp to end the action again.
	    
	    // update kingdoms, the update may only include spaces with
	    // leaders being removed from one kingdom so we use updateSplit
	    //board.updateKingdoms( board.getKingdom( ul ) );
	    board.updateKingdoms();
	}
	
	// build it or not, time for the next action
	rules.nextAction();
    }

    public void makeTreasureMove( Move move )
    {
	Player player = this.getPlayer( getMoveDynasty() );
	board.takeTreasure( move.getLocation() );
	player.addTreasure();

	// send listener event.
	this.fireGameEvent( GameEvent.TREASURE_TAKEN,
			    new Integer(this.getMoveDynasty()), 
			    move.getLocation(), null );

	// treasure happens during nextAction end of action checks,
	// attemp to end the action again.
	// NOTE: no update of kingdoms necessary, 
	// we just took a treasure
	rules.nextAction();
    }

    // ************************************************************************
    // MISC METHODS
    // ************************************************************************

    public static String stateToString( int ss )
    {
	switch (ss)
	    {
	    case WAIT_ACTION:
		return "Wait Action";
	    case WAIT_TREASURE:
		return "Wait Treasure";
	    case WAIT_ATTACK_SUPPORT:
		return "Wait Attack Support";
	    case WAIT_DEFEND_SUPPORT:
		return "Wait Defend Support";
	    case WAIT_CONFLICT_ORDER:
		return "Wait Conflict Order";
	    case WAIT_MONUMENT:
		return "Wait Monument";
	    case GAME_OVER:
		return "Game Over";
	    default:
		return "Unknown state " + ss + " in stateToString";
	    }
    }

    public void addGameListener( GameListener listener )
    {
	if ( gameListeners == null )
	    gameListeners = new Vector();

	gameListeners.addElement( listener );
    }

    public void removeGameListener( GameListener listener )
    {
	if ( gameListeners != null )
	    gameListeners.removeElement( listener );
    }

    public void fireGameEvent( int type, Object obj1, 
			       Object obj2, Object obj3 )
    {
	if ( gameListeners == null || gameListeners.size() == 0 ) 
	    return;

	GameEvent event = new GameEvent( this, type, obj1, obj2, obj3 );
	fireGameEvent( event );
    }

    public void fireGameEvent( GameEvent event )
    {
	if ( gameListeners == null || gameListeners.size() == 0 ) 
	    return;

	for ( Enumeration ee = gameListeners.elements(); 
	      ee.hasMoreElements();) {
	    GameEvent.processGameEvent( (GameListener) ee.nextElement(),
					event );
	}
    }

    public void removeOrphanedLeader( Space space )
    {
	if ( space == null )
	    return;

	Leader ll = space.getLeader();
	if ( ll != null &&
	     board.getNumAdjacentTiles( space, Sphere.RED ) == 0 )
	    {
		// remove from board or player object
		board.emptyLocation( space );
		
		// add back to the player object
		getPlayer( ll.getDynasty()).addLeader( ll.getSphere() );

		// send listener event.
		fireGameEvent( GameEvent.LEADER_ORPHANED, ll, space, null );
	    }
    }

    // return true if there were enough tiles to fill everyone up.
    public boolean fillPlayerTiles()
    {
	// fill each players tile bins
	boolean hadEnough = true;
	for ( int ii = 0; ii < players.length; ii++  ) {
	    Player pp = players[ (ii + actionDynasty) % players.length ];
	    TileBag dd = new TileBag();  // tmp drawn tiles, passed by REF.
	    while ( pp.getNumTiles() + dd.getNumTiles() 
		    < rules.TILES_PER_PLAYER ) 
		{
		    if ( ! drawTileFromBag( pp, dd ) ) {
			hadEnough = false;
			break;
		    }
		}

	    pp.putTiles( dd );
	    
	    // send listener event.
	    if ( dd.getNumTiles() > 0  ) {
		fireGameEvent( GameEvent.TILES_DRAWN, 
			       new Integer(pp.getDynasty()),
			       new TileBag( dd ), null );
	    }
	}

	return hadEnough;
    }
    
    // protected so that HiddenGame can fill the players up with unknown tiles 
    protected boolean drawTileFromBag( Player pp, TileBag dd )
    {
	//System.out.println("Game drawTileFromBag " + tileBag.getNumTiles() );

	// draw from bag... add to player
	if ( tileBag.getNumTiles() == 0 ) {
	    tileBagWasExhausted = true;
	    return false;
	} 
	
	// tt is assumed not null b/c we checked for bag not empty
	Sphere tt = tileBag.drawTile();
	dd.putTile(tt);

	return true;
    }

   public String toString()
   {
      StringBuffer strbuf = new StringBuffer();
      // --- PLAYERS --- 
      strbuf.append( players.length );
      strbuf.append("/");
      for ( int ii=0; ii<players.length; ii++) {
	 // toString of player has full reconstruction info with 
	 // delimiters, including trailing "/"
	 strbuf.append( players[ii] );
      }
      
      // --- SINGLE CHAR STATE INFO ---
      strbuf.append( gameState );
      strbuf.append( actionNumber );
      strbuf.append( actionDynasty );
      strbuf.append( moveDynasty );
      // 2 chars for conflict, 
      // 1st for sphere color, 2nd for played attack support
      if ( m_conflict != null ) {
	 strbuf.append( m_conflict.getLeaderSphere().toChar() );
	 int attSupport = m_conflict.getAttackScreenSupport();
	 if ( attSupport != -1 )
	    strbuf.append( attSupport );
	 else
	    strbuf.append( "-" );
      } else 
	 strbuf.append("--");
      strbuf.append("/");

      // -- LAST SPACE ---
      if ( lastSpace != null )
	 strbuf.append( LocationImpl.formatLocation(lastSpace,""));
      else
	 strbuf.append("--");
      strbuf.append("/");
      
      // --- TILE BAG INFO ---
      // could be long, but has order information contained.
      strbuf.append( tileBagWasExhausted?"1":"0" );
      strbuf.append( tileBag.getLetterString());
      strbuf.append("/");

      // --- BOARD ---
      strbuf.append(board);

      return strbuf.toString();
   }

    public void printGameDump()
    {
	System.out.println("---GAME DUMP---");
	System.out.println( this );
	//System.out.println("Move Number=" + moveNumber);
	System.out.println("Current Move=" + moveDynasty );
	//System.out.println("Action Number=" + actionNumber);
	System.out.println("State=" + gameState);
	System.out.println("Tile Bag=" + tileBag );
// 	System.out.println("Discard=" + discardPile );
	for ( int ii=0; ii<players.length; ii++)
	    {
		System.out.println("" + players[ii] );
	    }
	System.out.println("Last Space=" + lastSpace );
	System.out.println("Conflict=" + m_conflict);
	board.printAsciiMap();
	board.printKingdoms();
    }
}
