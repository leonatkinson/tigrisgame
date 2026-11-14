/* File: DefaultGameObserver.java
 * Description:  GameObserver that maintains an up-to-date local Game object.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE:
 * Will assume unknown tiles necessary in order to make moves legal.
 */

package com.tigrisgame.rules;

import java.io.Serializable;

public class DefaultGameObserver implements GameObserver, 
					    Serializable, Cloneable
{
   static private int INST_NUM = 0;

   private TigrisGame _game;
   private boolean _inProgress = false;
   private int _instance = INST_NUM ++;
   private String _name = "DefaultGameObserver";

   private long _lastClock = 0;
   
   // which player to victory points for.
   // other players will be cleared at the start of each turn.
   // -1 will keep all the victory points.
   private int _playerVPView = -1;
   
   // 0 is silent, 1 is error only, 2 is error and messages.
   public static int verbosity = 1;

   public DefaultGameObserver() {}

   public DefaultGameObserver( String name ) 
   {
      setName( name );
   }
   
   public Object clone() throws CloneNotSupportedException
   {
//   	try
//   	    {

      DefaultGameObserver copy = 
	 (DefaultGameObserver) super.clone();
      // DEEP copy the game, otherwise okay.
      copy._game = (TigrisGame) _game.clone();
      return copy;

//   	    }
//   	catch (CloneNotSupportedException e)
//   	    {
//   		// this shouldn't happen, since we are Cloneable
//   		return null;
//   	    }
   }
   
   public void gameStart( TigrisGame g )
   {
      message( "gameStart");
      
      if ( _inProgress ) {
	 message( "gameStart called while game still in progress, " +
		  "overwriting with new game");
      }
      
      _game = g;
      _lastClock = System.currentTimeMillis();
      // set the inProgress state if the game is still being played.
      // a null or finished game sets inprogress to false.
      _inProgress = (g != null && g.getState() != TigrisGame.GAME_OVER );
   }
   
   public void moveMade( Move move )
   {
      message( "moveMade");
      
      if ( _game == null || _inProgress == false ) {
	 error( "moveMade called with null game" );
	 return; 
      }

      // we want to hide the victory points on the uptick of
      // this player's first action move each round.

      boolean willHide = ( _game.getState() == TigrisGame.WAIT_ACTION &&
			   _game.getActionNumber() == 1 && 
			   _game.getActionDynasty() == _playerVPView);

      try {
	 revealNeededTiles( move );
	 _game.isLegal( move );
	 if ( willHide ) hideVictoryPoints();
	 _game.makeMove( move );
	 
      } catch ( IllegalMoveException ime ) {
	 
	  //ColorTextGame ctg = new ColorTextGame( _game );
	  error( move  + " failed!!! " + ime.getMessage() );
	  throw new RuntimeException( ime );
	 ////System.exit( 1 );
	 
	 //error( "moveMade error " + ime.getMessage() );
      }
   }
   
   public void showScreen( int dynasty, TileHolder tiles )
   {
      message( "showScreen : " + tiles );
      
      if ( _game == null || _inProgress == false )
	 error( "moveMade called with null game" );
      
      Player player = _game.getPlayer( dynasty );
      
      if ( tiles == player ) 
	 error( "showScreen called with exact reference to player" +
		" (should be a copy)" );
      
      
      // this client doesn't care about logging or tile bag cohesion,
      // so we can clean out the old screen.
      player.drawTiles( player );
      player.putTiles( tiles );
   }

   
   public void showClock( int dynasty, long clock )
   {
      message( "showClock : " + clock );
      
      if ( _game == null || _inProgress == false )
	 error( "moveMade called with null game" );
      
      _lastClock = System.currentTimeMillis();

      Player player = _game.getPlayer( dynasty );
      player.setClock( clock );
   }
   
   public boolean requestUndoMoves( int number )
   {
      message( "requestUndoMoves" );
      return false;
   }
   
   public void yourMove()
   { 
      message( "yourMove" + ", state=" + _game.getState() );
   }
   
   public void gameEnd( int [] dyns )
   {
      message( "gameEnd" );

      _inProgress = false;
   }
   
   public boolean isPlaying()
   {
      return _inProgress;
   }
   
   public void setName( String name ) { this._name = name; }
   public String getName() { return _name; }
   
   // get a safely mutable copy of the game.
   public TigrisGame getGame()
   {
      TigrisGame gg = null;
      String ss = null;
      if ( _game != null ) {
	 gg = (TigrisGame) _game.clone();
	 // update the current player's clock.
	 Player pp = gg.getPlayer( gg.getMoveDynasty());
	 long timeLeft = pp.getClock() -  // time left.
	    (System.currentTimeMillis() - _lastClock ); //diff since last clock

	 pp.setClock( timeLeft );
	 ss = ""+_game.getState();
      }

      message( "getGame" + ", state=" + ss );
      return gg;
   }
   
   private void revealNeededTiles( Move move )
      throws IllegalMoveException
   {
      revealNeededTiles( _game, move );
   }
   
   // reveal those tiles necessary to make the move legal.
   // if there are not enough tiles are already known or hidden
   // then throw exception
   public static void revealNeededTiles( TigrisGame game, Move move )
      throws IllegalMoveException
   {
      Player player = game.getPlayer( game.getMoveDynasty() );
      TileBag neededTiles = new TileBag();
      
      switch ( move.getType()) 
	 {
	 case Move.PLACE_TILE:
	    neededTiles.putTile( move.getSphere() );
	    break;
	 case Move.SWAP_TILES:
	 case Move.CHOOSE_SUPPORTERS:
	    neededTiles.putTiles( move.getTiles() );
	    break;
	 case Move.POSITION_LEADER:
	 case Move.PLAY_CATASTROPHE:
	 case Move.CHOOSE_SPHERE:
	 case Move.BUILD_MONUMENT:
	 case Move.CHOOSE_TREASURE:
	 default:
	    // No work needed for these cases
	    return;
	 }
      
      // don't reveal anything we already know they have, 
      // but as needed reveal the rest.
      Sphere [] spheres = Sphere.getSpheres();
      for ( int ss=0, ssNeed, ssHas; ss<spheres.length; ss++) {
	 ssNeed = neededTiles.getNumTiles( spheres[ss] );
	 ssHas  = player.getNumTiles( spheres[ss] );
	 if ( ssNeed > 0 &&
	      ssHas > 0 )
	    neededTiles.drawTiles( spheres[ss], 
				   Math.min( ssNeed, ssHas ) );
      }
      
      if ( player.getNumTiles( Sphere.HIDDEN ) <
	   neededTiles.getNumTiles() )
	 throw new IllegalMoveException( "More tiles needed for " + move + 
					 " than known or available to be " +
					 "revealed for player " + player );
      player.revealHidden( neededTiles );
   } 
   
   public void setPlayerVPView( int dynasty )
   {
      _playerVPView = dynasty;
   }
   
   public int getPlayerVPView()
   {
      return _playerVPView;
   }
   

   public void addGameListener( GameListener listener )
   {
      _game.addGameListener( listener );
   }
   
   public void hideVictoryPoints()
   {
      for ( int ii=0; ii< _game.getNumPlayers(); ii++ ) 
	 if ( ii != _playerVPView )
	    _game.getPlayer(ii).clearVictoryPoints();
   }
   
   public void hideTiles( int view )
   {
      _game.hideTiles( view );
   }
   
   public void message( String msg )
   {
      if ( verbosity > 1 )
	 System.out.println(this.getClass().getName()
			    + " " + _instance + " " + msg );
   }
   
   public void error( String msg )
   {
      if ( verbosity > 0 )
	 System.out.println(this.getClass().getName()
			    + " "  + _instance + " error: " + msg );
   }
}
