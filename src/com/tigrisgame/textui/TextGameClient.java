/* File: TextGameClient.java
 * Description:  command-line text-only game client.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.textui;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.PlayerScoreSorter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class TextGameClient extends DefaultGameObserver
{
    GameServer _gameServer;
    ColorTextGame _textGame = new ColorTextGame();
    PrintStream _out;
    BufferedReader _in;
    Thread _inputThread = new UserInputThread();
    public static final String commandEscape = "/";

    boolean yourMove = false;
    boolean killInput = false;
    boolean swapPending = false;
    boolean undoPending = false;
    boolean undoResponse = false;

    int firstPromptMode = GAME_MODE;
    boolean firstPrompt = true;
    boolean prompted = false;

    // the current readLine being processed 
    // and the next char to read.
    String _readLine = null;
    int _readIndex = -1;
    boolean _readSpaces = false;

    // 2 is debug verbosity
    // 1 is normal, which shows GameObserver messages.
    // 0 is silent except as requested by commands.
    public static int verbosity = 1;

    public static final int OFF_MODE     = 0;
    public static final int GAME_MODE    = 1;
    public static final int BOARD_MODE   = 2;
    public static final int MISC_MODE    = 3;
    public static final int PLAYERS_MODE = 4;
    public static final int SCREEN_MODE  = 5;

    public TextGameClient( GameServer server )
    {
	this( server, "TextGameClient", System.out, 
	      new BufferedReader( new InputStreamReader( System.in )) );
    }

    public TextGameClient( GameServer server, String name,
			   PrintStream out, BufferedReader in )
    {
	setName( name );
	this._out = out;
	this._in = in;
	_gameServer = server;
    }

    // **********************
    // Implement GameObserver
    // **********************
    
    public void gameStart( TigrisGame g )
    {
	super.gameStart( g );
	_textGame.setGameView( g );

	// initialize varables that may need reset after undo move.
	yourMove = false; 
	swapPending = false;

	if ( _inputThread != null &&
	     ! _inputThread.isAlive() ) {
	    _inputThread.start();
	    prompt();
	}

	synchronized ( _out ) {
	    if ( verbosity > 0 )
		_out.println(" === GAME START === " );
	}
    }

    public void moveMade( Move move )
    {
	super.moveMade( move );
	_textGame.setGameView( getGame() );
	yourMove = false; // and then wait for another yourMove

	synchronized ( _out ) {
	    if ( verbosity > 0 ) {
		_out.println(" === MOVE MADE === " );
		_out.println( _textGame.formatMove( move ) );
	    }
	}
    }

    public void showScreen( int dynasty, TileHolder tiles )
    {
	TigrisGame oldGame = getGame();
	TileBag oldTiles = new TileBag ( oldGame.getPlayer( dynasty ));

	super.showScreen( dynasty, tiles );
	_textGame.setGameView( getGame() );

	synchronized ( _out ) {
	    if ( verbosity > 0 && 
		 // show if the tiles are different or we just swapped.
		 ( ! oldTiles.equals( tiles ) || swapPending ) ) {
		swapPending = false;
		_out.println(" === NEW TILES === " );
	    }
	}
    }

    public void gameEnd( int [] order )
    {
	super.gameEnd( order );

	yourMove = false;
	MoveHistory history = _gameServer.getMoveHistory();
	TigrisGame finalGame = history.getGame();
	_textGame.setGameView( finalGame );

	int winners = 1;
	PlayerScoreSorter sorter = new PlayerScoreSorter();
	for ( int ii=1; ii<order.length; ii++) {
	    if ( sorter.eq( finalGame.getPlayer( order[ii-1] ),
			    finalGame.getPlayer( order[ii] )))
		winners ++;
	}


	if ( verbosity > 0 ) {
	    synchronized ( _out ) {
		_out.println(" === GAME END === " );
		if ( winners == 1 ) {
		    _out.println( finalGame.getPlayer(order[0]).
				  getName() + " wins!");
		} else {
		    for ( int ii=0; ii<winners; ii++ ) {
			_out.print( finalGame.getPlayer(order[ii]).getName());
			if ( (ii+1) == winners )
			    _out.println(" tie!");
			else
			    _out.print(", ");
		    }
		}
		for ( int ii=0; ii<order.length; ii++ ) {
		    Player player = finalGame.getPlayer(order[ii]);
		    _out.println( player.getName() + " "  +
				  _textGame.formatVictoryPoints(player," "));
		}
	    }
	    prompt();
	}
    }

    // only one thread can ask to undo at a time.
    public synchronized boolean requestUndoMoves( int number )
    {
	undoResponse = false;
	undoPending = true;

	synchronized ( _out ) {
	    _out.println("Your opponent has requested to undo the last " + 
			 number + " move(s)");
	    _out.println("type undo to accept or any other command to reject.");
	    prompt();
	} 

	try {
	    wait();
	} catch (InterruptedException ie) {
	    if ( verbosity > 0 )
		System.out.println("TextGameClient interrupted " +
				   "in requestUndoMove!!");
	}

	System.out.println("TextGameClient requestUndoMove() = " 
			   + undoResponse );

	return undoResponse;
    }

    public void yourMove()
    {
	yourMove = true;
	firstPrompt = true;
	prompted = false;
	if ( verbosity > 0 ) {
	    synchronized ( _out ) {
		_out.println(" === YOUR MOVE === " );
		prompt();
	    }
	}
    }

    // **********************
    // Prompting and Help
    // **********************

    public void prompt()
    {
	synchronized ( _out ) {
	    if ( !prompted ) {
		if ( yourMove )
		    promptMove();
		_out.print(">");
	    }
	}
    }

    public void promptMove()
    {
	TigrisGame game = getGame();
	_textGame.setGameView( game );
	_textGame.setPlayerView( game.getPlayer( game.getMoveDynasty()));

	if ( firstPrompt )
	    show( firstPromptMode );	    
	firstPrompt = false;
	
	int gameState = game.getState();
	switch ( gameState )
	    {
	    case TigrisGame.WAIT_ACTION:
		_out.println("Your turn to make action " + 
			     game.getActionNumber() +
			     " of 2." );
		break;
	    case TigrisGame.WAIT_TREASURE:
		_out.println("Your turn to take a treasure.");
		break;
	    case TigrisGame.WAIT_ATTACK_SUPPORT:
	    case TigrisGame.WAIT_DEFEND_SUPPORT:
		_out.println("Your turn to add " +
			     game.getConflict().getSupportSphere() + 
			     " tiles in support." );
		break;
	    case TigrisGame.WAIT_CONFLICT_ORDER:
		_out.println("Your turn to choose the next " +
			     "conflict color to resolve");
		break;
	    case TigrisGame.WAIT_MONUMENT:
		_out.println("Your turn to build a monument" );
		break;
	    case TigrisGame.GAME_OVER:
		_out.println("Game Over, yourMove should not be possible");
		break;
	    default:
		_out.println("Unknown game state " + gameState 
			     + ", no help available");
	    }
	prompted = true;
    }

    public void printHelp( String topic )
    {
	if ( verbosity > 1 )
	    _out.println("printHelp got " + topic );

	_out.println(" === HELP === ");

	if ( topic == null || 
	     topic.trim().equals( "" ) ) {
	    _out.println("Commands are:");
	    _out.println("help           rules          ");
	    _out.println("list           save           ");
	    _out.println("move           show           ");
	    _out.println("prompt         undo           ");
	    _out.println("quit                          ");
	    _out.println("type help <command> for more help");
	}
	if ( topic.equals( "help" ) ) {
	    _out.println("help <command> -- gives additional " +
			 "help about a topic" );
	}
	else if ( topic.equals( "list" ) ) {
	    _out.println("list -- lists the moves of the game" );
	}
	else if ( topic.equals( "move" ) ) {
	    printMoveHelp();
	}
	else if ( topic.equals( "prompt" ) ) {
	    _out.println("prompt <option> -- chooses what game " +
			 "information to automatically " );
	    _out.println("show when you are first prompted to move.");
	    _out.println("legal <option> choices are:");
	    _out.println("game           misc           ");
	    _out.println("board          players        ");
	    _out.println("screen         off            ");
	    _out.println("see \"help show\" for more info");
	}
	else if ( topic.equals( "quit" ) ) {
	    _out.println("quit -- leaves the game");
	}
	else if ( topic.equals( "save" ) ) {
	    _out.println("save <filename> -- has the game server " +
			 "save the game to disk" );
	}
	else if ( topic.equals( "rules" ) ) {
	    _out.println("complete game rules are available online at ");
	    _out.println("http://http://freespace.virgin.net/chris.lawson/rk/e-t/rules.htm");
	}
	else if ( topic.equals( "show" ) ) {
	    _out.println("show (default) -- print some game info ");
	    _out.println("legal choices for <option> are :");
	    _out.println("board   -- shows the board");
	    _out.println("players -- shows the screen of each player");
	    _out.println("screen  -- shows your screen only");
	    _out.println("misc    -- shows the general game info including " +
			 "who's turn, conflict info, and tiles");
	    _out.println("game    -- shows everything: misc, board, and players");
	    _out.println("/ is shorthand for show");
	}
	else if ( topic.equals( "undo" ) ) {
	    _out.println("undo <number> -- requests to undo the last N moves"+
			 "OR okays the undo request of another player" );
 	}
    } 

    public void printMoveHelp()
    {
	if ( verbosity <= 0 )
	    return;

	TigrisGame game = getGame();
	int gameState = game.getState();

	_out.println("current game state is " +
		     StandardGame.stateToString( gameState ) + 
		     ", moves:");

	switch( gameState ) {

	case TigrisGame.WAIT_ACTION:
	    _out.println("leader <color> <space>   -- (re)position a leader");
 	    _out.println("tile <color> <space>     -- place a tile");
	    _out.println("catastrophe <space>      -- place a catastrophe");
	    _out.println("swap (<color> <num>) ... -- swap tiles");
	    _out.println();
	    printColorHelp();
	    printSpaceHelp();
	    _out.println();
	    _out.println("moves can be abbreviated as: " );
	    _out.println("L<color_letter><space>");
	    _out.println("T<color_letter><space>");
	    _out.println("C<space>");
	    _out.println("S<num_black><num_red><num_blue><num_green>");
	    _out.println("all with no spaces and case insensitive");
	    break;
	case TigrisGame.WAIT_TREASURE:
	    _out.println("<space> -- the space of the treasure to take");
	    printSpaceHelp();
	    break;

	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    _out.println("<number> -- the number of " +
			 game.getConflict().getSupportSphere() + 
			 " tiles to play in support");
	    break;
	case TigrisGame.WAIT_CONFLICT_ORDER:
	    _out.println("<color> -- the color of the next conflict to resolve");
	    printColorHelp();
	    break;

	case TigrisGame.WAIT_MONUMENT:
	    _out.println("none            -- build no monument");
	    _out.println("<color>         -- build a " + 
			 game.getLastSpace().getTile() + 
			 " and <color> monument");
	    _out.println("<color> <space> -- build a monument at " +
			 "<space> (upper left corner) if multiple " +
			 " locations are possible");
	    printColorHelp();
	    printSpaceHelp();
	    break;

	case TigrisGame.GAME_OVER:
	default:
	    _out.println("No legal moves for state " + gameState );
	    break;
	}
    }

    public void printColorHelp()
    {
	_out.println("possible <color> choices are: " );
	_out.println("(B)lack, (R)ed, bl(U)e, and (G)reen");
	_out.println("choices may be typed out or the one " +
		     "letter abbreviation used");
    }

    public void printSpaceHelp()
    {
	_out.println("<space> is specified by the <letter> <number> " +
		     "algebraic grid coordinates on the board." );
    }

    // **********************
    // Move Processing
    // **********************

    public Move processLine()
    {
	Move move = null; 	

	if ( verbosity > 1 )
	    _out.println("processLine got " + _readLine );

	try {
	    // first check for response to undo.
	    readUndo();

	    // check for a special command.
	    readCommand();

	    // try to read a move.
	    move = readMove();

	    String line = getRemainingString();
	    if ( ! line.equals("")) {
		if ( verbosity > 1 )
		    _out.println("processLine left with " + line );
		
		throw new FormatException( "Unknown move or command, " +
					   "try typing help");
	    }
	} catch (FormatException fe) {
	    StringBuffer pad = new StringBuffer();
	    for ( int ii=0; ii<_readIndex; ii++)
		pad.append(" ");
	    _out.println( "ERROR: " + fe.getMessage() );
	    _out.println( "--> " +_readLine );
	    _out.println( "--> " + pad.toString() + "^" );
	}



	// where to say "unknown move or command"?, here?

	return move;
    }

    // gets the remaining string, consuming any leading spaces.
    String getRemainingString()
    {
	consumeWhiteSpace();
	String str = _readLine.substring(_readIndex, _readLine.length());
	return str.toLowerCase();
    }

    // checks for undo command, synched on "this" for coherence with
    // request undo method.
    synchronized void readUndo() throws FormatException
    {
	String line = getRemainingString();

	if ( line.startsWith( "undo" )) {

	    consumeWord( "undo" );
	    int number = 1;
	    line = getRemainingString();
	    if ( line.length() != 0 ) {
		try {
		    number = readInt();
		} catch (NumberFormatException nfe) {
		    throw new FormatException( "expected number of " + 
					       "moves to undo");
		}
	    }
	    
	    // if WE are asking.
	    if ( undoPending == false ) {
		_out.println("Requesting other players for permission" +
			     " to undo last move ..." );
		try {
		    _gameServer.undoMoves( this, number );
		    _out.println("Undo permission granted!");
		} catch ( ServerException se ) {
		    _out.println("" + se.getMessage() );
		}
	    }
		
	    // if THEY are asking, this command grants permission.
	    else {
		undoResponse = true;
		notify();
	    }

	    // consume the whole string.
	    _readIndex = _readLine.length();
	} 

	else {
	    // if they didn't say "undo", the request was denied.
	    if ( undoPending ) {
		undoResponse = false;
		undoPending = false;
		notify();
	    }
	}
    }

    // checks for special commands and returns true if
    // a command was found and processed.
    void readCommand()
    {
	String line = getRemainingString();
	if ( verbosity > 1 )
	    _out.println("readCommand got " + line );

	if ( line.startsWith( "show" ) || line.startsWith( commandEscape )) {
	    if ( line.startsWith( "show" ))
		 consumeWord( "show" );
	    else
		consumeWord( commandEscape );

	    String strMode = getRemainingString();
	    int intMode = getShowMode( strMode );
	    if ( intMode != -1 )
		show( intMode );
	    else 
		_out.println("unknown show option " + strMode );
	}
	else if ( line.startsWith( "list" ) || line.startsWith( "history" )) {
	    MoveHistory history = _gameServer.getMoveHistory();
	    _out.println( history.getColorHistoryString() );
	}
	else if ( line.startsWith( "help" ) || line.startsWith( "?" )) {
	    if ( line.startsWith( "help" ))
		consumeWord( "help" );
	    else
		consumeWord( "?" );

	    String topic = getRemainingString();
	    printHelp( topic );
	}
	else if ( line.startsWith( "move" )) {
	    // consume the word and return without
	    // consuming the rest of the line.
	    consumeWord("move");
	    consumeWhiteSpace();
	    return;
	}
	else if ( line.startsWith( "prompt" )) {
	    String mode = line.substring( "prompt".length(),
					  line.length() ).trim();
	    if ( verbosity > 1 )
		_out.println("prompt got " + mode );	  

	    int newMode;
	    if ( mode.startsWith( "off" ) || 
		 mode.startsWith( "none" ) )
		newMode = OFF_MODE;
	    else  
		newMode = getShowMode( mode );

	    if ( newMode != -1 ) {
		_out.println("prompt mode set to " + mode.trim() );
		firstPromptMode = newMode;
	    }
	    else 
		_out.println("unknown prompt mode " + mode );

	    if ( verbosity > 1 )
		_out.println("new prompt mode is " + firstPromptMode );
	}
	else if ( line.startsWith( "quit" )) {
	    // quit all the threads,
	    // this ends the game when run from main().
	    // extra work may need to be done to quit if 
	    // a GameServerProxy thread is alive.
	    _inputThread = null;
	    killInput = true;
	    prompted = true;  
	    _out.println("goodbye");
	}
	else if ( line.startsWith( "save" )) {
	    String name = line.substring( "save".length(),
					  line.length() );
	    _out.println("deprecated command");
	    //_gameServer.saveGame( name.trim() );
	}
	else {
	    // no command found, leave String untouched.
	    return;
	}

	// consume the whole string.
	_readIndex = _readLine.length();
    }

    public int getShowMode( String mode ) 
    {
	int showMode = -1;
	mode = mode.trim();

	if ( mode.startsWith( "g" ) || mode.equals("") ) // "game"
	    showMode = GAME_MODE;
	else if ( mode.startsWith( "b" )) // "board"
	    showMode = BOARD_MODE;
	else if ( mode.startsWith( "m" ) ||  // "misc" 
		  mode.startsWith( "i" ) )   // "info"
	    showMode = MISC_MODE;
	else if ( mode.startsWith( "p" )) // "players"
	    showMode = PLAYERS_MODE;
	else if ( mode.startsWith( "s" )) // "screen"
	    showMode = SCREEN_MODE;
	else {
	    if ( verbosity > 1 )
		_out.println("getShowMode got INVALID " + mode );
	} 

	return showMode;
    }

   public void show( int mode )
   {
      switch ( mode ) {
      case OFF_MODE:
	 break;
      case GAME_MODE:
	 _out.println(_textGame.formatGame());
	 break;
      case BOARD_MODE:
	 _out.println( _textGame.formatBoard());
	 break;
      case MISC_MODE:
	 _out.println( _textGame.formatGameInfo());
	 break;
      case PLAYERS_MODE:
	 _out.println(_textGame.formatPlayers());
	 break;
      case SCREEN_MODE:
	 TigrisGame game = getGame(); 
	 int move = game.getMoveDynasty();
	 String str = _textGame.
	    formatPlayer( game.getPlayer( move ), true, 
			  move == game.getActionDynasty(), " " );
	 _out.println( str );
	 break;
      default:
	 System.out.println( "Unknown show mode " + 
			     mode );
      }
   }

    Move readMove() throws FormatException
    {
	String line = getRemainingString();
	if ( verbosity > 1 )
	    _out.println("readMove got " + line );

	if ( line.equals("") )
	    return null;

	if ( line.indexOf(" ") != -1 )
	    _readSpaces = true;
	else
	    _readSpaces = false;

	TigrisGame game = getGame();
	Move move = null;
	int dynasty = game.getMoveDynasty();
	int gameState = game.getState();

	Sphere color = null;
	Space space = null;
	
	switch ( gameState ) {
	case TigrisGame.WAIT_ACTION:
	    move = readActionMove();
	    break;
	case TigrisGame.WAIT_TREASURE:
	    space = readSpace( game.getBoard() );
	    move = Move.newChooseTreasure( dynasty, space );
	    break;
	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    int supporters;
	    try {
		supporters = readInt();
	    } catch (NumberFormatException nfe) {
		throw new FormatException( "expected number of supporters" );
	    }
	    TileBag bag = new TileBag();
	    bag.putTiles( game.getConflict().getSupportSphere(), supporters );
	    move = Move.newChooseSupporters( dynasty, bag );
	    break;
	case TigrisGame.WAIT_CONFLICT_ORDER:
	    color = readSphere();
	    move = Move.newChooseSphere( dynasty, color );
	    break;
	case TigrisGame.WAIT_MONUMENT:
	    if ( line.startsWith("none" )) {
		move = Move.newBuildMonument( dynasty, null, null, null );
		consumeWord("none");
	    }
	    else {
		color = readSphere();
		space = readSpace( game.getBoard() );
		Sphere firstSphere = game.getLastSpace().getTile();
		move = Move.newBuildMonument( dynasty, space, 
					      color, firstSphere );
	    }
	    break;
	case TigrisGame.GAME_OVER:
	default:
	    break;
	}

	return move;
    }

    // reads an move which is known to be an action from a trimmed,
    // lower case string.
    Move readActionMove() throws FormatException
    {
 	String line = getRemainingString();
	if ( verbosity > 1 )
	    _out.println("readActionMove got " + line );

	if ( line.length() < 0 )
	    throw new FormatException( "expected action" );

	TigrisGame game = getGame();
	Move move = null;
	char firstChar = line.charAt( 0 );
	int dynasty = game.getMoveDynasty();
	Sphere color = null;
	Space space = null;

	switch (firstChar) {
	case 'l':
	    if ( verbosity > 1 )
		System.out.println("got leader move");

	    // consume the command letter or word.
	    if ( _readSpaces )
		consumeWord( "leader" ); 
	    else
		_readIndex ++;

	    // consume whitespace.
	    consumeWhiteSpace();

	    // get the color and space.
	    color = readSphere();
	    consumeWhiteSpace();
	    space = readSpace( game.getBoard() );

	    move = Move.newPositionLeader( dynasty, space, color );
	    break;
	case 't':
	    if ( verbosity > 1 )
		System.out.println("got tile move");

	    // consume the command and any whitespace.
	    if ( _readSpaces )
		consumeWord( "tile" ); 
	    else
		_readIndex ++;

	    // consume whitespace.
	    consumeWhiteSpace();

	    color = readSphere();
	    consumeWhiteSpace();
	    space = readSpace( game.getBoard());

	    // place tile move.
	    move = Move.newPlaceTile( dynasty, space, color );
	    break;
	case 'c':
	    if ( verbosity > 1 )
		System.out.println("got cat move");

	    if ( _readSpaces )
		consumeWord( "catastrophe" ); 
	    else
		_readIndex ++;

	    // consume whitespace.
	    consumeWhiteSpace();

	    // place catastrophe move.
	    space = readSpace( game.getBoard());
	    move = Move.newPlayCatastrophe( dynasty, space );
	    break;
	case 's':
	    if ( verbosity > 1 )
		System.out.println("got swap move");

	    move = readSwapMove();
	    break;
	default:
	    break;
	}

	return move;
    }

    public Move readSwapMove() throws FormatException
    {
    	// consume the command and any whitespace.
	if ( _readSpaces )
	    consumeWord( "swap" ); 
	else
	   _readIndex ++;

	// consume whitespace.
	consumeWhiteSpace();

	TigrisGame game = getGame();
	int dynasty = game.getMoveDynasty();

	// swap move.
	Move move = null;
	TileBag tiles = null;
	String line = getRemainingString();

	// check to see if BRUG mode or B2R2 mode
	if ( line.indexOf("b") == -1 && 
	     line.indexOf("r") == -1 &&
	     line.indexOf("u") == -1 &&
	     line.indexOf("g") == -1 ) 
	    {
		try {
		    char tmpChar;
		    // short mode, one char per sphere.
		    tmpChar= _readLine.charAt( _readIndex );
		    int sb = Integer.parseInt( "" + new Character( tmpChar ));
		    _readIndex ++; consumeWhiteSpace();
		    tmpChar= _readLine.charAt( _readIndex );
		    int sr = Integer.parseInt( "" + new Character( tmpChar ));
		    _readIndex ++; consumeWhiteSpace();
		    tmpChar= _readLine.charAt( _readIndex );
		    int su = Integer.parseInt( "" + new Character( tmpChar ));
		    _readIndex ++; consumeWhiteSpace();
		    tmpChar= _readLine.charAt( _readIndex );
		    int sg = Integer.parseInt( "" + new Character( tmpChar ));
		    _readIndex ++; consumeWhiteSpace();
		    tiles = new TileBag( sb, sr, su, sg );
		} catch (NumberFormatException nfe ) {
		    throw new FormatException( "expected an single digit number " +
					       "in <num_black><num_red>" +
					       "<num_blue><num_green> format" );
		}
	    }
	else 
	    {
		tiles = new TileBag();
		// consume the rest of the line with <color> <num>
		// or <num> <color> pairs.
		while ( _readIndex != _readLine.length() ) {
		    char nextChar = _readLine.charAt( _readIndex );
		    int num;
		    Sphere color;

		    try {
			if ( Character.isDigit( nextChar ) ) {
			    num = readInt();
			    consumeWhiteSpace();
			    color = readSphere();
			    consumeWhiteSpace();
			} else {
			    color = readSphere();
			    consumeWhiteSpace();
			    num = readInt();
			    consumeWhiteSpace();
			}
			tiles.putTiles( color, num );
		    } catch (NumberFormatException nfe) {
			throw new FormatException( "expected a number of tiles");
		    }
		}
	    }

	swapPending = true;  // set a flag to print NEW TILES even if same.
	return move = Move.newSwapTiles( dynasty, tiles ); 
    }


    public Sphere readSphere() throws FormatException
    {
	String line = getRemainingString();
	Sphere sphere = null;

	if ( verbosity > 1 )
	    _out.println("readSphere got " + line + " spaces? " + _readSpaces );

	if ( line.length() == 0 )
	    throw new FormatException( "expected color" );

	// no spaces, we read exactly one character.
	if ( ! _readSpaces ) {

	    char sphereChar = line.charAt( 0 );
	    sphere = getSphereFromChar( sphereChar );

	    if ( sphere == null ) {
		throw new FormatException( "expected color letter" );
	    } 
		
	    // consume the one char that we read.
	    _readIndex ++;
	}

	// with spaces, we consume as much as possible of the word.
	else {
	    if ( line.startsWith( "bla" )) {
		consumeWord("black");
		sphere = Sphere.BLACK;
	    }
	    else if ( line.startsWith( "r" )) {
		consumeWord("red");
		sphere = Sphere.RED;
	    }
	    else if ( line.startsWith( "blu" )) {
		consumeWord("blue");
		sphere = Sphere.BLUE;
	    }
	    else if ( line.startsWith( "u" )) {
		consumeWord("u");
		sphere = Sphere.BLUE;
	    }
	    else if ( line.startsWith( "g" )) {
		consumeWord("green");
		sphere = Sphere.GREEN;
	    }
	    else if ( line.startsWith( "b" )) {
		throw new FormatException( "b is ambigous between blue " +
					   "and black" );
	    }
	    else 
		throw new FormatException( "expected color" );
	}

	return sphere;
    }

    // consume as much of the word is present from the readLine.
    void consumeWord( String word )
    {
	for ( int ii=0; ii<word.length(); ii++) {
	    char wordchar = word.charAt( ii );
	    char readchar = _readLine.charAt( _readIndex );

	    // if the char is the same (ignore case) consume,
	    // otherwise return.
	    if ( wordchar == Character.toLowerCase( readchar ) ) {
		_readIndex ++;
	    } 
	    else 
		return;
	}
    }

    void consumeWhiteSpace()
    {
	for ( int ii=_readIndex; ii<_readLine.length(); ii++) {
	    char charii = _readLine.charAt( ii );
	    
	    // if end of whitespace, break.
	    if ( ! Character.isWhitespace( charii ) ) 
		break;
	    
	    // otherwise consume the character.
	    _readIndex ++;
	}
    }

    public Sphere getSphereFromChar( char sphereChar ) 
    {
	Sphere sphere = null;
	switch (sphereChar) {
	case 'b':
	    sphere = Sphere.BLACK;
	    break;
	case 'r':
	    sphere =  Sphere.RED;
	    break;
	case 'u':
	    sphere =  Sphere.BLUE;
	    break;
	case 'g':
	    sphere =  Sphere.GREEN;
	    break;
	default: 
	    _out.println("unknown sphere char " + new Character( sphereChar ));
	}

	return sphere;
    }
	
    // Reads the input line starting at _readIndex for <space>
    // parses out a <letter><number> and converts it into the row,col
    // and returns an appropriately constructed space object, or null if invalid.
    // space is always the last portion of the move.
    Space readSpace( Board board ) throws FormatException
    {
	String line = getRemainingString();

	if ( verbosity > 1 )
	    System.out.println("readSpace got " + line );

	if ( line.length() < 2 )
	    throw new FormatException("expected space");

	char firstChar = line.charAt( 0 ), colChar;
	int col = -1, row = -1;

	try {
	    // the user gave <letter><number> 
	    if ( ! Character.isDigit( firstChar )) {
		colChar = firstChar;
		_readIndex ++;
		consumeWhiteSpace();
		row = readInt(); 
	    }
	    // the user gave <number><letter>
	    else {
		row = readInt();
		consumeWhiteSpace();

		if ( _readIndex >= _readLine.length() ) 
		    throw new FormatException( "expected letter for column");

		colChar = _readLine.charAt( _readIndex );
		_readIndex ++;

	    }
	} catch (NumberFormatException nfe) {
	    throw new FormatException( "expected row number for space" );
	}
	
	col = (int) (colChar - 'a');
	row = board.getNumRows() - row;

	if ( verbosity > 1 )
	    System.out.println("col? " + col + " row? " + row );

	if ( row < 0 || row >= board.getNumRows() ||
	     col < 0 || col >= board.getNumCols() )
	    {
		throw new FormatException("row=" + row + " col=" + 
					  new Character( colChar ) + 
					  " is off the board");
	    }

	return board.getSpace( row, col );
    }

    int readInt() throws NumberFormatException
    {
	StringBuffer buffer = new StringBuffer(4);
	for ( int ii=_readIndex; ii<_readLine.length(); ii++) {
	    char numchar = _readLine.charAt( _readIndex );

	    // if the char is a number, consume it and add to the buffer.
	    if ( Character.isDigit( numchar ) ) {
		_readIndex ++;
		buffer.append( numchar );
	    } 
	    else 
		break;
	}

	// read the int from the buffer.
	return Integer.parseInt( buffer.toString());
    }

    void readNextLine() throws IOException
    {
	_readLine = _in.readLine();
	_readIndex = 0;
	prompted = false;
    }
	

    // a thread that reads from _in and prints to _out to 
    // capture user moves, print requests, and help requests.
    public class UserInputThread extends Thread
    {
	public void run()
	{
	    if ( verbosity > 1 )
		System.out.println("TextGameClient starting UserInputThread");

	    String line = null;

	    while ( ! killInput ) {

		try {
		    readNextLine();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    break;
		}

		Move move = processLine();
		
		try {
		    if ( move != null )
			_gameServer.makeMove( TextGameClient.this, move );
		}
		catch( IllegalMoveException ime ) {
		    _out.println("Illegal Move: " + ime.getMessage() );
		    //ime.printStackTrace();
		    move = null;
		}
		catch ( ServerException se ) {
		    _out.println("Server Exception: " + se.getMessage() );
		    //se.printStackTrace();
		    move = null;
		}

		prompt();
	    }
	}
    }

    public static void main( String [] args ) throws ServerException
    {
	GameServerImpl server = new GameServerImpl();
	TextGameClient client = new TextGameClient( server );
	server.newGame( 1 );
	server.assignPlayer( client, 0 );
	server.startGame();

	// verbosity = 2;

	while ( true ) {
	    try {
		Thread.currentThread().sleep(500);
	    } catch (InterruptedException ie) {}
	}
    }
}

