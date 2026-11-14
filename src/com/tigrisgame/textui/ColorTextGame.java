/* File: ColorTextGame.java
 * Description:  text-only ui game with ANSI color escapes.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.textui;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ResourceLoader;

import java.text.DecimalFormat;

public class ColorTextGame extends TigrisAnsiColor
{
   private TigrisGame _game;
   private Player _player;
   private ColorTextBoard _textBoard = new ColorTextBoard();

   public static String DELIM = " ";
   public String NEWLINE = System.getProperty("line.separator");
   static DecimalFormat clockFormat = new DecimalFormat( "##" );

   public ColorTextGame()
   {
      this( null );
   }

   public ColorTextGame( TigrisGame g )
   {
      setGameView( g );
      clockFormat.setMinimumIntegerDigits( 2 );
   }
   

   public void setGameView( TigrisGame g )
   {
      if ( g == null )
	 return;
      
      _game = g;
      _textBoard.setBoard( g.getBoard() );
   }
   
   public void setPlayerView( Player pl )
   {
      if ( pl == null )
	 return;
   }

   public String formatGame()
   {
      StringBuffer strbuf = new StringBuffer();
      strbuf.append( formatGameInfo());
      strbuf.append( NEWLINE );
      strbuf.append( formatBoard());
      strbuf.append( NEWLINE );
      strbuf.append( formatPlayers());
      return strbuf.toString();
   }
   
   public String formatGameInfo()
   {
      if ( _game == null ) return "null game";

      StringBuffer strbuf = new StringBuffer();
      int action = _game.getActionDynasty();
      int move = _game.getMoveDynasty();
      
      strbuf.append("State: " + StandardGame.stateToString( _game.getState() )+
		    ", Action: " + _game.getPlayer(action).getName() + 
		    " " + _game.getActionNumber() + "/" +
		    _game.getRules().ACTIONS_PER_TURN +
		    ", Move: " + _game.getPlayer(move).getName());
      strbuf.append( NEWLINE );
      strbuf.append("Tiles: " + formatTileHolder( _game.getTileBag(), DELIM ) 
		    + " left of " +
		    formatTileHolder( _game.getTotalTiles(), DELIM ));
      
      Conflict conflict = _game.getConflict();
      if ( conflict != null ) {
	 strbuf.append( NEWLINE );
	 strbuf.append( formatConflict( conflict, DELIM ) );
      }
      return strbuf.toString();
   }
   
   public String formatBoard()
   {
      Conflict conflict = _game.getConflict();
      return _textBoard.formatBoard();
   }

   public String formatPlayers()
   {
      StringBuffer strbuf = new StringBuffer();
      int action = _game.getActionDynasty(), move = _game.getMoveDynasty();
      
      for ( int pp=0; pp<_game.getNumPlayers(); pp++) {
	 if ( pp != 0 ) strbuf.append( NEWLINE );
	 strbuf.append( formatPlayer( _game.getPlayer(pp), pp == move, 
				      pp == action, DELIM  ));
      }
      return strbuf.toString();
   }
   
   // returns a String of an ansi color formatted file bag.
   // player is 2 lines long, so to fit in 80 cols.
   public String formatPlayer( Player player, 
			       boolean yourMove, 
			       boolean yourAction)
   {
      return formatPlayer( player, yourMove, yourAction, DELIM );
   }

   public static String formatClock( long clock )
   {
      clock = Math.max( 0, clock );
      int millis  = (int)(clock % 1000);
      clock = clock / 1000;
      int seconds = (int)(clock%60);
      clock = clock / 60;
      int minutes = (int)(clock%60);
      clock = clock / 60;
      int hours = (int)clock;

      StringBuffer clockStr = new StringBuffer();
      if ( hours > 0 ) {
	 clockStr.append( hours );
	 clockStr.append( ":" );
      }
      clockStr.append( clockFormat.format( minutes ));
      clockStr.append( ":" );
      clockStr.append( clockFormat.format( seconds ));
      return clockStr.toString();
   }
   
   public String formatPlayer( Player player, 
			       boolean yourMove, 
			       boolean yourAction,
			       String delim ) 
   {
      if ( player == null )
	 return "null";
      
      StringBuffer str = new StringBuffer();
      Sphere [] spheres = Sphere.getSpheres();
      
      String header = PLAIN + "P" + player.getDynasty()+ ":";
      
      str.append( header );
      
      if ( yourMove ) // Bright.
	 str.append( "[1m" );
      // 	if ( yourAction ) // Reverse.
      // 	    str.append( "[7m" );
      str.append( pad( player.getName(), 15 , true));
      
      // leaders.
      str.append( PLAIN + " Leaders: " );
      for ( int ss=0; ss<spheres.length; ss++) {
	 if ( ss != 0 )
	    str.append( PLAIN + delim );
	 Leader ll = player.getLeader( spheres[ss] );
	 if ( ll != null ) {
	    str.append( getColor( ll.getSphere() ) + 
			getLeader( ll.getDynasty() ) );
	 } else {
	    str.append("---");
	 }
      }
      // vp.
      str.append( PLAIN + " VP: " );
      str.append( formatVictoryPoints( player, delim ));
      
      // name.  15 chars.
      str.append( PLAIN + NEWLINE + header );
      str.append( pad( formatClock(player.getClock()), 15, true));
      
      // tiles 
      str.append( PLAIN + " Tiles:   " );
      str.append( pad( formatTileHolder( player, delim ), 15, true ) );
      
      // cat.
      str.append( PLAIN + " Cat: " );
      for ( int ii=0; ii < player.getNumCatastrophes(); ii++ ) {
	 if ( ii != 0 )
	    str.append( PLAIN + delim );
	 str.append( CAT );
      }
      
      str.append( PLAIN );
      return str.toString();
   }

   public static String formatVictoryPoints( Player player, 
					     String delim ) 
   {
      StringBuffer str = new StringBuffer();
      Sphere [] spheres = Sphere.getSpheres();
      
      // first sort ??
      for ( int ss=0; ss<spheres.length; ss++) {
	 if ( ss != 0 )
	    str.append( PLAIN + delim);
	 str.append( getColor( spheres[ss] ) );
	 str.append(" ");
	 str.append( player.getVictoryPoints( spheres[ss]));
	 str.append(" ");
      }
      int numTreasure = player.getNumTreasures();
      if ( numTreasure > 0 ) {
	 str.append(PLAIN + " ");
	 str.append( TREASURE );
	 str.append(" ");
	 str.append( player.getNumTreasures() );
	 str.append(" ");
      }
      
      str.append( PLAIN );
      
      return str.toString();
   }
   
   public String formatMove( Move move )
   {
      if ( move == null ) 
	 return "null move";
      if ( _game == null )
	 return "null game";
      
      String locationDelim = "";
      StringBuffer str = new StringBuffer();
      str.append( _game.getPlayer( move.getDynasty() ).getName() );
      str.append( " " );
      if ( move.isPositionLeader() ) {
	 str.append("positions " );
	 str.append( getColor( move.getSphere() ));
	 str.append( getLeader( move.getDynasty()) );
	 str.append(PLAIN + " leader at " );
	 str.append( formatLocation( move.getLocation(),locationDelim ));
      } else if ( move.isPlaceTile() ) {
	 str.append( "places " );
	 str.append( getColor( move.getSphere()) );
	 str.append( TILE );
	 str.append( PLAIN +  " tile at " );
	 str.append( formatLocation( move.getLocation(), locationDelim));
      } else if ( move.isPlayCatastrophe() ) {
	 str.append("plays " );
	 str.append( CAT );
	 str.append( PLAIN + " catastrophe at " );
	 str.append( formatLocation( move.getLocation(),locationDelim ));
      } else if ( move.isSwapTiles() ) {
	 str.append("swaps " );
	 str.append( formatTileHolder( move.getTiles() ));
      } else if ( move.isChooseSphere() ) {
	 str.append("chooses to resolve " );
	 str.append( getColor( move.getSphere() ));
	 str.append( TILE );
	 str.append( PLAIN );
      } else if ( move.isBuildMonument() ) {
	 if ( move.getLocation() != null ) {
	    str.append( "builds a ");
	    str.append( getColor( move.getSphere() ));
	    str.append( TILE );
	    str.append( PLAIN + " monument at " );
	    str.append( formatLocation( move.getLocation(), locationDelim ));
	 } else {
	    str.append( "declines to build a monument" );
	 }
      } else if ( move.isChooseSupporters() ) {
	 str.append("adds " );
	 TileHolder tiles = move.getTiles();
	 Sphere [] spheres = Sphere.getSpheres();
	 if ( tiles.getNumTiles() == 0 ) { str.append( "no" ); }
	 for ( int ss =0; ss<spheres.length; ss++) {
	    if ( tiles.getNumTiles( spheres[ss] ) > 0 ) {
	       str.append( getColor( spheres[ss]));
	       str.append( " " );
	       str.append( tiles.getNumTiles( spheres[ss] ));
	       str.append( " " );
	    }
	 }
	 str.append( PLAIN + " support" );
      } else if ( move.isChooseTreasure() ) {
	 str.append("takes " );
	 str.append( TREASURE_TILE );
	 str.append( PLAIN + " treasure at " );
	 str.append( formatLocation( move.getLocation(), locationDelim ));
      } else {
	 str.append("unknown move type");
      }
      
      return str.toString();
   }
   

    // MAYBE FOR A BLACK & WHITE GAME ???
    // -- copied from Move

//     public String getNotation( Board board )
//     {
// 	StringBuffer note = new StringBuffer();

// 	switch ( type )
// 	    {
// 		// ACTIONS
// 	    case POSITION_LEADER:
// 		note.append( "L" );
// 		note.append( sphere.toChar() );
// 		note.append( space.getRow() + "," + space.getCol() );
// 		break;
// 	    case PLACE_TILE:
// 		note.append( "T" );
// 		note.append( sphere.toChar() );
// 		note.append( space.getRow() + "," + space.getCol() );
// 		break;
// 	    case PLAY_CATASTROPHE:
// 		note.append( "C" );
// 		note.append( space.getRow() + "," + space.getCol() );
// 		break;
// 	    case SWAP_TILES:
// 		note.append( "S" );
// 		if ( tiles.getNumTiles( Tile.HIDDEN ) > 0 )
// 		    note.append( "?" + tiles.getNumTiles() );
// 		else {
// 		    Sphere [] spheres = Sphere.getSpheres();
// 		    for ( int ii=0; ii< spheres.length; ii++ )
// 			note.append( tiles.getNumTiles( spheres[ii] ) );
// 		}
// 		break;
// 		// SUBACTIONS
// 	    case CHOOSE_SPHERE:
// 		note.append( "c" );
// 		note.append( sphere.toChar() );
// 		break;
// 	    case CHOOSE_SUPPORTERS:
// 		note.append( "s" );
// 		note.append( tiles.getNumTiles() );
// 		break;
// 	    case BUILD_MONUMENT:
// 		note.append( "m" );

// 		if ( space == null )
// 		    note.append( "X" );
// 		else {
// 		    note.append( sphere.toChar() );
// 		    note.append( space.getRow() + "," + space.getCol() );
// 		}
// 		break;
// 	    case CHOOSE_TREASURE:
// 		//str += "Choose Treasure at " + spaceRow + "," + spaceCol;
// 		note.append( "t" );
// 		note.append( space.getRow() + "," + space.getCol() );
// 		break;
// 	    default:
// 		System.out.println("Warning: Move.getNotation() not defined for " 
// 				   + typeToString( type ) );
// 		note.append( "Invalid" );
// 		break;
// 	    }

// 	return note.toString();
//     }

   // kept in place as a shortcut.
   private String formatLocation( Location sp, String delim )
   {
      return LocationImpl.formatLocation( sp, delim );
   }
   
   // returns a String of an ansi color formatted file bag.
   // 
   public static String formatTileHolder( TileHolder bag )
   {
      return formatTileHolder( bag, DELIM );
   }
   
   public static String formatTileHolder( TileHolder bag, String delim ) 
   {
      if ( bag == null )
	 return "null";
      
      StringBuffer str = new StringBuffer();
      
      if ( bag.getNumTiles( Sphere.HIDDEN ) == 0 ) {
	 // no hidden tiles, draw normal.
	 
	 Sphere [] spheres = Sphere.getSpheres();
	 for ( int ss=0; ss<spheres.length; ss++) {
	    if ( ss != 0 )
	       str.append( PLAIN + delim );
	    str.append( getColor( spheres[ss] ) );
	    str.append(" ");
	    str.append( bag.getNumTiles( spheres[ss]));
	    str.append(" ");
	 }
	 str.append( PLAIN );
      } else {
	 str.append( bag.getNumTiles( Sphere.HIDDEN ) );
	 str.append(" hidden");
      }
      
      return str.toString();
   }
   
   public static String formatTileHolderBW( TileHolder bag, String delim ) 
   {
      if ( bag == null )
	 return "null";
      
      StringBuffer str = new StringBuffer();
      
      if ( bag.getNumTiles() == 0 ) {
	 str.append("none");
      } else if ( bag.getNumTiles( Sphere.HIDDEN ) == 0 ) {
	 // no hidden tiles, draw normal.
	 
	 Sphere [] spheres = Sphere.getSpheres();
	 boolean needDelim = false;
	 for ( int ss=0; ss<spheres.length; ss++) {
	    
	    if ( bag.getNumTiles( spheres[ss] ) > 0 ) {
	       if ( needDelim )
		  str.append( delim );
	       else
		  needDelim = true;
	       str.append(" ");
	       str.append( bag.getNumTiles( spheres[ss]));
	       str.append(" ");
	       str.append( spheres[ss].toString() );
	    }
	 }
      } else {
	 str.append( bag.getNumTiles( Sphere.HIDDEN ) );
	 str.append(" hidden");
      }
      
      return str.toString();
   }
   
   public static String formatTileHolder2( TileHolder bag, String delim ) 
   {
      if ( bag == null )
	 return "null";
      
      StringBuffer str = new StringBuffer();
      boolean firstTile = true;
      
      Sphere [] spheres = Sphere.getSpheres();
      for ( int ss=0; ss<spheres.length; ss++) {
	 for ( int ii=0; ii< bag.getNumTiles( spheres[ss] ); ii++) {
	    if ( firstTile ) 
	       firstTile = false;
	    else 
	       str.append( PLAIN + delim );
	    
	    str.append( getColor( spheres[ss] ) + TILE );
	 }
      }
      
      for ( int ii=0; ii< bag.getNumTiles( Sphere.HIDDEN ); ii++) {
	 str.append( HIDDEN_TILE );
      }
      
      str.append( PLAIN );
      
      return str.toString();
   }
   
   public static String formatConflict( Conflict conflict, String delim  )
   {
      if ( conflict == null )
	 return "null";
      
      StringBuffer str = new StringBuffer();
      String colorStr = getColor( conflict.getSupportSphere() );

      if ( conflict.isInternal() ) {
	 str.append( "Internal " );
      } else {
	 str.append( "External " );
      }
      
      str.append( "Conflict: " );
      
      // leaders.
      Leader attacker = conflict.getAttacker();
      str.append( getColor( attacker.getSphere() ) ); 
      str.append( getLeader( attacker.getDynasty() ) );
      
      str.append( PLAIN + " with "  );
      
      // attacker support.
      str.append( colorStr + " " + 
		  conflict.getAttackBoardSupport() + " " 
		  + PLAIN + " + " );
      int attackScreenSupport = conflict.getAttackScreenSupport();
      if ( attackScreenSupport != -1 ) {
	 str.append( colorStr ); 
	 str.append( " " + attackScreenSupport + " " );
      } else {
	 str.append( "???" );
      }

      str.append( PLAIN + " attacks " );
      
      Leader defender = conflict.getDefender();
      str.append( getColor( defender.getSphere() ) ); 
      str.append( getLeader( defender.getDynasty() ) );
      
      str.append( PLAIN + " with "  );
      
      // defender support.
      str.append( colorStr + " " + 
		  conflict.getDefendBoardSupport() + " " 
		  + PLAIN + " + " );
      int defendScreenSupport = conflict.getDefendScreenSupport();
      if ( defendScreenSupport != -1 ) {
	 str.append( colorStr ); 
	 str.append( " " + defendScreenSupport + " " );
      } else {
	 str.append( "???" );
      }
      
      str.append( PLAIN );
      
      return str.toString();
   }
   
   // WARNING!! only works correctly on plaintext.
   public static String pad( String str, int size, boolean center )
   {
      if ( str.length() > size )
	 return str;
      
      StringBuffer strbuf = new StringBuffer(size);
      for ( int ii=0; ii<size; ii++) strbuf.insert(ii, " ");
      
      int leftPos = 0;
      if ( center ) 
	 leftPos = (size - str.length())/2;
      
      // JDK1.2 method.
      //strbuf.replace( leftPos, leftPos + str.length(), str );
      
      for ( int ii=0; ii<str.length(); ii++ ) 
	 strbuf.setCharAt( leftPos + ii, str.charAt( ii ) );
      
      return strbuf.toString();
   }
   
   // main displays a game given by the filename.
   public static void main(String args[]) throws Exception
   {
      String gameName;
      if ( args.length > 0 ) 
	 gameName = args[0];
      else
	 gameName = "ERROR_GAME";
      
      TigrisGame game = null;
      Object obj = ResourceLoader.fileLoadObject( gameName );
      if ( obj == null )
	 System.out.println("couldn't load from " + args[1]);
      else if ( obj instanceof TigrisGame )
	 game = (TigrisGame) obj;
      else if ( obj instanceof MoveHistory ) 
	 game = ((MoveHistory)obj).getGame();
      
      ColorTextGame ctg = new ColorTextGame();
      ctg.setGameView( game );
      System.out.println( ctg.formatGame());
   }
}
