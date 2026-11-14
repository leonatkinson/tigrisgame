/* File: MoveHistory.java
 * Description:  Keeps a history of moves to recover previous games states.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */
package com.tigrisgame.rules;

import com.tigrisgame.textui.ColorTextGame;
import com.tigrisgame.util.ArrayUtils;

import java.io.Serializable;
import java.util.Vector;

public class MoveHistory extends DefaultGameObserver 
    implements Serializable, Cloneable
{
    protected TigrisGame _startingGame;
 
    // matched arrays of Move and MoveNumber objects.
    private Vector _history = new Vector();

    public MoveHistory()
    {
	setName("MoveHistory");
    }

    public Object clone() //throws CloneNotSupportedException
    {
  	try
  	    {
		MoveHistory copy = (MoveHistory) super.clone();

		// DEEP copy the history.
  		copy._history = new Vector();
		copy._startingGame = (TigrisGame) _startingGame.clone();
		for ( int ii=0; ii<_history.size(); ii++) {
		    Object elementCopy = 
			((HistoryElement) _history.elementAt( ii )).clone();
		    copy._history.addElement( elementCopy );
		}

		return copy;
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    public void gameStart( TigrisGame g )
    {
	_startingGame = (TigrisGame) g.clone();

	// add the default "empty" first move for pregame notes.
	_history = new Vector();
	_history.addElement( new HistoryElement( new Move() ) );

	// here we *want* to hide the tiles, so
	// that the when moves are made and screens are shown
	// it has to figure what tiles are actually drawn.

	// CM -- okay, so i changed my mind, we no longer need to 
	// calculate diffs to preserve tile /draws/ so we keep as much
	// info as possible. 
	// this actually caused a bug with saving move history with the server.
	
// 	hideTiles();

	super.gameStart( g );
    }

    public void moveMade( Move move )
    {
	HistoryElement element = new HistoryElement( move ); 
 	super.moveMade( move );

	// only add after the move is okay.
	_history.addElement( element );
    }

    public void showScreen( int dynasty, TileHolder tiles )
    {
	super.showScreen( dynasty, tiles );

	HistoryElement element = (HistoryElement) 
	    _history.elementAt( size() -1 );

	element.showScreen( dynasty, tiles );

// 	TileBag beforeTiles, afterTiles;
// 	beforeTiles = new TileBag( getGame().getPlayers()[ dynasty ]);
// 	afterTiles = new TileBag( getGame().getPlayers()[ dynasty ]);
// 	if ( ! afterTiles.equals( beforeTiles ) ) {
// 	    // find out how many were actually revealed.
// 	    // that is the after minus before (not counting hidden)
// 	    beforeTiles.drawTiles( Tile.HIDDEN, 
// 				   beforeTiles.getNumTiles( Tile.HIDDEN ));
// 	    afterTiles.drawTiles( beforeTiles );

// 	    RevealedTiles note = new RevealedTiles( dynasty, afterTiles );
// 	    addNote( note ); 
// 	}

	
    }
    
    // use DefaultGameObserver implementation for the following methods.
    // maybe write some closing statements to the log on gameEnd ??
//     public void gameEnd( int [] finish );
//     public void yourMove();
//     public String getName();


    public int size()
    {
	return _history.size();
    }

    public void hideTiles()
    {
	hideTiles( -1 );
    }

    // hide the tiles, however preserve the tile of "view" player.
    public void hideTiles( int view )
    {
	super.hideTiles( view ); // i.e. hide current tiles in _game.
	_startingGame.hideTiles( view );

	for ( int ii=0; ii< size(); ii++ ) {
	    HistoryElement element = (HistoryElement) _history.elementAt(ii);

	    Move move = element.move;
	    int dynasty = -1;
	        // XXX -- process through game and get move at each step??

	    if ( move != null && move.isSwapTiles() && 
		 (dynasty=move.getDynasty()) != view ) {
		TileBag moveTiles = new TileBag( move.getTiles() );
		moveTiles.hideTiles();
		element.move = Move.newSwapTiles( dynasty, moveTiles );
	    }

// 		for ( Enumeration ee = element.notes.elements();
// 		      ee.hasMoreElements(); ) {
// 		Object obj = ee.nextElement();
// 		if ( obj instanceof RevealedTiles ) {
// 		    RevealedTiles revealed = (RevealedTiles) obj;
// 		    revealed.tiles.hideTiles();
//	        }

	    for ( int ss=0; ss < element.screens.length; ss++  ) {
		if ( ss != view )
		    element.screens[ss] = null;
	    }
	}
    }

    // int moveNum is the number of moves that should be applied to the game,
    // up to and including moveNum.
    // moveNum = 0 means the starting position, while
    // moveNum = _history.size() means the current position.
    public TigrisGame getGame( int moveNum )
    {
	// check if they want the current game,
	if ( moveNum >= _history.size() ) {
	    return getGame();
	} 

	TigrisGame game = (TigrisGame) _startingGame.clone();
	for ( int ii=0; ii<= moveNum; ii++ ) {
	    HistoryElement element = (HistoryElement) _history.elementAt( ii );

	    Move move = element.move;

	    if ( ii != 0 ) 
		try {
		    revealNeededTiles( game, move );
		    game.makeMove( move );
		} catch (IllegalMoveException ex) {
		    ex.printStackTrace();
		    return null;
		}

	    showScreens( game, ii );
	}
			 

// 	    for ( Enumeration ee = element.notes.elements();
// 		  ee.hasMoreElements(); ) {
// 		Object obj = ee.nextElement();
// 		if ( obj instanceof RevealedTiles ) {
// 		    RevealedTiles revealed = (RevealedTiles) obj;

// 		    Player player = game.getPlayer( revealed.dynasty );
// 		    if ( player.getNumTiles( Tile.HIDDEN ) >=
// 			 revealed.tiles.getNumTiles() )
// 			player.revealHidden( revealed.tiles );
// 		}
// 	    }


	return game;
    }

    public void showScreens( TigrisGame game, int index )
    {
	HistoryElement element = (HistoryElement) _history.elementAt(index);
	for ( int ss=0; ss < element.screens.length; ss++ ) {
	    TileHolder tiles = element.screens[ss];
	    if ( tiles != null ) {
		Player player = game.getPlayer( ss );
		player.drawTiles( player );
		player.putTiles( tiles );
	    }
	}
    }

    public Move getMove( int index )
    {
	HistoryElement element = (HistoryElement) _history.elementAt( index );
	return element.move;
    }

    // package private.
    HistoryElement getHistoryElement( int index )
    {
	return (HistoryElement) _history.elementAt( index );
    }

    public Move [] getMoves()
    {
	Move [] moves = new Move[ size() ];

	for ( int ii=0; ii < moves.length; ii++ ) {
	    moves[ii] = ((HistoryElement) _history.elementAt(ii)).move;
	}

	return moves;
    }

    public Move getLastMove()
    {
	return ((HistoryElement) _history.lastElement()).move;
    }

    public void removeLastMove()
    {
	int lastIndex = _history.size() - 1;

	if ( lastIndex == 0 ) {
	    System.out.println("WARNING: MoveHistory cannot undo move 0");
	    return;
	}

	// remove the last element from our history list.
	_history.removeElementAt( lastIndex );

	// reset the _game variable in our superclass DefaultGameObserver.
	super.gameStart( getGame( lastIndex - 1 ) );
    }

    public void addNote( Object note )
    {
	addNote( (HistoryElement) _history.elementAt( size() -1 ), note );
    }


    public void addNote( int index, Object note )
    {
	addNote( (HistoryElement) _history.elementAt( index ), note );
    }

    private void addNote( HistoryElement element, Object note )
    {
	element.notes.addElement( note );
    }

    public String getColorHistoryString()
    {
	if ( size() == 0 )
	    return "No Moves";

	ColorTextGame ctg = new ColorTextGame( _startingGame );
	StringBuffer strbuf = new StringBuffer("Move History:\n" );
	strbuf.append(                         "-------------\n");
	Sphere [] spheres = Sphere.getSpheres();
	String notePad = "   ";

	for ( int ii=0; ii<size(); ii++ ) {
	    HistoryElement element = (HistoryElement) _history.elementAt( ii );
	    Move move = (Move) element.move;

	    if ( ii != 0 ) {
		strbuf.append( ii + ". " );
		strbuf.append( ctg.formatMove( move ) );
	    } else {
		strbuf.append( "0. (Game Start)" );
	    }
	    strbuf.append( "\n" );

	    Vector notes = (Vector) element.notes;
	    String [] noteString = getNotes( ii, true );
	    for ( int jj=0; jj<noteString.length; jj++) {
		strbuf.append( notePad + noteString[jj]);
		strbuf.append( "\n" );
	    }
	}
	
	strbuf.append( "end" );

	return strbuf.toString();
    }


    public String getServerLogString()
    {
       StringBuffer strbuf = new StringBuffer();
       strbuf.append("MoveHistory START" + "\n");
       strbuf.append("" + _startingGame + "\n");
       for ( int ii=1; ii<_history.size(); ii++ ) {
	  HistoryElement elem = (HistoryElement) _history.elementAt(ii);
	  strbuf.append( "" + elem.move + "\n");
       }
       strbuf.append("MoveHistory END\n\n");
       return strbuf.toString();
    }

    public String [] getNotes( int index, boolean color )
    {
	if ( index < 0 || index >= size() )
	    return null;

	Vector notes = ((HistoryElement) _history.elementAt( index )).notes;

	if ( notes == null )
 	    return new String[0];
	    
	//RevealedTiles revealed;
 	String [] noteStr;

	noteStr = new String[ notes.size() ];
	for ( int ii=0; ii<notes.size(); ii++ ) {
	    noteStr[ii] = new Character((char)(ii+'a')) + ". ";
	    Object obj = notes.elementAt( ii );
	    noteStr[ii] += obj;
	}
 	return noteStr;
     }


    public String toString()
    {
	StringBuffer strbuf = new StringBuffer();
	strbuf.append("MoveHistory START" + "\n");
	strbuf.append("startingGame = " + _startingGame + "\n");
	for ( int ii=0; ii<_history.size(); ii++ ) {
	    strbuf.append( "" + _history.elementAt(ii) + "\n");
	}
	strbuf.append("currentGame = " + getGame() + "\n");
	strbuf.append("MoveHistory END");
	return strbuf.toString();
    }

    class HistoryElement implements Serializable, Cloneable
    {
	Move move;

	// what screens were shown AFTER the move.
	// ensure size is sufficient for all players in the game.
	TileHolder [] screens = new 
	    TileHolder[ _startingGame.getNumPlayers() ];

	// notes can be any object type, but usually String.
	Vector notes;
	
	public HistoryElement( Move move )
	{
	    this.move = move;
	    notes = new Vector();
	}

	public Object clone() //throws CloneNotSupportedException
	{
	    try
		{
		    HistoryElement copy = (HistoryElement) super.clone();

		    // DEEP copy the screens, by initting a new tile bag.
		    // from each of the old ones.
		    copy.screens = new TileHolder[ screens.length ];
		    for ( int ii=0; ii<screens.length; ii++) {
			if ( screens[ii] != null )
			    copy.screens[ii] = new TileBag( screens[ii] );
		    }
		    // shallow copy of notes.
		    // if notes are CONSTANT and we don't change 
		    // this in different views it will be okay.
		    copy.notes = (Vector)notes.clone();

		    return copy;
		}
	    catch (CloneNotSupportedException e)
		{
		    // this shouldn't happen, since we are Cloneable
		    return null;
		}
	}

	public void showScreen( int dynasty, TileHolder tiles )
	{
	    screens[dynasty] = new TileBag( tiles );
	}

	public String toString()
	{
	    return "HistoryElement: " + move + " " + 
		ArrayUtils.prettyPrint( screens );
	}
    }
}
