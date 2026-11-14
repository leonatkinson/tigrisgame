/* File: GameServerImpl.java
 * Description:  Implementation of GameServer and GameSetupServer
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * class that handles the simplest interaction between Games and GameObservers.
 * handles hiding information that should be forgotten and revealing 
 * necessary information to make moves legal from authoritative source.
 * if an illegal move is made, the object will be corrupted.
 * uses files to load and save games.  (alternatives are databases, client/server)
 * ASSUMES: only one game at a time that is set with gameStart()
 * and all moves arrive in order from starting point
 * NOT multithread safe.  mainly with the players array.
 *
 * what about move history? if so, include it before the info is hidden for swap
 * what about hiding the random-number-seed in the game?
 */

package com.tigrisgame.rules;

import com.tigrisgame.util.ArrayUtils;
import com.tigrisgame.util.PlayerScoreSorter;
import com.tigrisgame.util.ResourceLoader;

import java.util.Enumeration;
import java.util.Vector;

public class GameServerImpl implements GameSetupServer
{
   // might want to subclass someday.
   protected TigrisGame _game = null;
   protected GameOptions _options = null;
   protected GameObserver [] _players = new GameObserver[0];
   protected Vector _observers = new Vector();
   protected boolean _started = false;
   protected boolean _autoStart = false;
   protected MoveHistory _moveHistory;
   protected long _lastMoveTime = 0;
   protected boolean _countUp = true;
   public int verbosity = 0;

   public GameServerImpl() {}

    public void newGame()
	throws ServerException
    {
	if ( _started ) {
	    throw new ServerException("newGame called while game in progress" );
	}

	message( "newGame" );

	_options = new GameOptions();
	_game = new StandardGame( _options );
	_players = new GameObserver[ _game.getNumPlayers() ];
	_moveHistory = new MoveHistory();
	_moveHistory.gameStart( (TigrisGame) _game.clone() );
    }

    public void newGame( int numPlayers )
 	throws ServerException
    {
 	if ( _started ) {
 	    throw new ServerException("Warning: newGame called while " +
				      "game in progress" );
 	}

	message( "newGame " + numPlayers );

	_options = new GameOptions( numPlayers );
 	_game = new StandardGame( _options );
 	_players = new GameObserver[ _game.getNumPlayers() ];
	_moveHistory = new MoveHistory();
	_moveHistory.gameStart( (TigrisGame) _game.clone() );
     }

//     public void newGame( String mapfile, int numplayers, 
// 			 long seed, int variant )
// 	throws ServerException
//     {
// 	if ( _started ) {
// 	    throw new ServerException("Warning: newGame called " + 
// 				      "while game in progress" );
// 	}

// 	_players = new GameObserver[ numplayers ];

// 	GameOptions options = new GameOptions( numplayers );
// 	options.variant = variant;
// 	options.mapfile = mapfile;
// 	options.seed = seed;
	
// 	_game = new StandardGame( options );
//     }

    public void newGame( GameOptions options )
	throws ServerException
    {
	if ( _started ) {
	    throw new ServerException("Warning: newGame called " + 
				      "while game in progress" );
	}

	message( "newGame " + options );

	_players = new GameObserver[ options.playerNames.length ];

	_options = options;
	_game = new StandardGame( _options );
	_moveHistory = new MoveHistory();
	_moveHistory.gameStart( (TigrisGame) _game.clone() );
    }

    // REMOVED SO CAN ALTERNATLY LOAD MOVE HISTORY.
    // awful name choice for the static version versus regular.
//     public static Game fileLoadGame( String filename )
// 	throws IOException, ClassNotFoundException
//     {
// 	Game game = null;

// 	ObjectInputStream in =
// 	    new ObjectInputStream( new FileInputStream( filename ));
// 	game = (Game) in.readObject();
// 	in.close();

// 	return game;
//     }
    
    public void loadGame( String filename )
	throws ServerException
    {
	if ( _started ) {
	    throw new ServerException("Warning: newGame called while " +
				      " game in progress" );
	}

	message( "loadGame " + filename );

	Object obj = ResourceLoader.fileLoadObject( filename );

	if ( obj == null ) {
	    throw new ServerException("Error: Unable to load game from " +
				      "file " + filename );
	}
	else if ( obj instanceof TigrisGame ) {
	    _game = (TigrisGame) obj;
	    _moveHistory = new MoveHistory();
	    _moveHistory.gameStart( (TigrisGame) _game.clone() );
	} 
	else if ( obj instanceof MoveHistory ) {
	    _moveHistory = (MoveHistory) obj;
	    _game = _moveHistory.getGame();
	} 
	else {
	    throw new ServerException("Error: Unable to load game from " +
				      "file " + filename + ": " + 
				      "Unrecognized class " + 
				      obj.getClass() );
	}

	_players = new GameObserver[ _game.getNumPlayers() ];
    }

    public void saveGame( String filename )
    {
	message( "saveGame " + filename );

	ResourceLoader.fileSaveObject( _moveHistory, filename );
    }

//     public static void fileSaveGame( Game g, String filename )
//     {
// 	System.out.println("NOT IMPLEMENTED");
//     }

    public void assignPlayer( GameObserver player, int dynasty )
	throws ServerException
    {
	if ( _started ) {
	    throw new ServerException("assignPlayers called while game in progress" );
	}
	if ( _game == null || _players == null ) {
	    throw new ServerException("assignPlayers called " +
				      "without game or players object" );
	}
	if ( player == null ) {
	    throw new ServerException("assignPlayers called with null observer");
	}
	
	if ( dynasty < 0 || dynasty >= _players.length ) {
	    throw new ServerException("assignPlayers called with " +
				      "out of bounds dynasty, " + dynasty + 
				      " in a " + _players.length 
				      + " player game");
	}
	if ( _players[ dynasty ] != null ) {
	    throw new ServerException("joinGame called with dynasty already assigned");
	}

	for ( int ii=0; ii<_players.length; ii++ ) {
	    if ( _players[ ii ] == player )
		throw new ServerException("joinGame called with a player already assigned to a dynasty");
	}

	message( "assignPlayer " + player + " to " + dynasty );

	// make sure the player isn't also an observer.
	unobserveGame( player );
	_players[ dynasty ] = player;
	_game.getPlayer( dynasty ).setName( player.getName() );

	// move history _startingGame set at the same
	// time _game is set, so they should both be in sync.
	_moveHistory._startingGame.getPlayer( dynasty ).
	    setName( player.getName());

	// since this implementation doesn't allow reassignment,
	// we will immediatly try to start he game if everyone assigned.
	if ( _autoStart ) {
	    for ( int pp=0; pp < _players.length; pp++ ) {
		if ( _players[pp] == null )
		    return;
	    }
	    startGame();
	}
    }

    public void startGame()
	throws ServerException
    {
	if ( _started ) {
	    throw new ServerException("startGame called while game in progress" );
	}
	if ( _game == null || _players == null ) {
	    throw new ServerException("startGame called " +
			       "without game or players object" );
	}
	for ( int pp=0; pp < _players.length; pp++ ) {
	    if ( _players[pp] == null ) {
		throw new ServerException("startGame called " +
				   "without all players assigned" );
	    }
	}

	message( "startGame" );

	reallyStartGame();
    }

    private void reallyStartGame()
    {
	// DEBUG
	//_game.addGameListener( new GameListenerAdapter() );

	// hide all tiles for all players and the tile bag.
	// make sure each player get's their own unique copy
	// so they don't reveal each other's screen.
	TigrisGame hiddenGame = (TigrisGame) _game.clone();
	hiddenGame.hideTiles( -1 );

	for ( int pp=0; pp < _players.length; pp++ ) {
	    _players[pp].gameStart( (TigrisGame) hiddenGame.clone() );
	}

	// don't need to notify move history,
	// already notified in either startGame or undoMoves

	// also let the observers know.
	for ( Enumeration ee = _observers.elements(); ee.hasMoreElements(); ) {
	    ((GameObserver) ee.nextElement()).
		gameStart( (TigrisGame) hiddenGame.clone() );
	}
	
	// show the players their starting hands.
	showAllScreens();

	_started = true;
	
	_lastMoveTime = System.currentTimeMillis();

	// give special notification to the person who's move is next.
	_players[ _game.getMoveDynasty() ].yourMove();		
    }

   public void abortGame()
   {
      message( "abortGame" );
      // propogate the move to all the players.
      for ( int pp=0; pp < _players.length; pp++ ) {
	 _players[pp].gameEnd( null );
      }
      
      // also let the observers know.
      Enumeration ee;
      for ( ee = _observers.elements(); ee.hasMoreElements(); ) {
	 ((GameObserver) ee.nextElement()).gameEnd( null );
      }
      
      
      // don't remove this "lock" on playing with the
      // state of the server until all notified.
      _players = new GameObserver[0];
      _started = false;
   }

    private void endGame()
    {
	message( "endGame" );

	Player [] players = new Player[ _game.getNumPlayers() ];
	for ( int ii=0; ii<players.length; ii++ )
	    players[ii] = _game.getPlayer( ii );
	PlayerScoreSorter psort = new PlayerScoreSorter();
	psort.penalty = _options.penalty;
	ArrayUtils.sortDescending( players, psort );
	int [] finishOrder = new int[ players.length ];
	for ( int pp=0; pp < players.length; pp++) {
	    finishOrder[pp] = players[pp].getDynasty();
	}

	// also let the move history know.
	_moveHistory.gameEnd( finishOrder );

	// propogate the move to all the players.
	for ( int pp=0; pp < _players.length; pp++ ) {
	    _players[pp].gameEnd( finishOrder );
	}

	// also let the observers know.
	Enumeration ee;
	for ( ee = _observers.elements(); ee.hasMoreElements(); ) {
	    ((GameObserver) ee.nextElement()).gameEnd( finishOrder );
	}

	// don't remove this "lock" on playing with the
	// state of the server until all notified.
	_players = new GameObserver[0];
	_started = false;
    }
    
    public void makeMove( GameObserver obs, Move move )
	throws ServerException, IllegalMoveException
    {
	if ( ! _started ) {
	    throw new ServerException("No game in progress, " +
				      "cannot make a move");
	}

	if ( obs != _players[ _game.getMoveDynasty() ] ) {
	    throw new IllegalMoveException("Not your turn to move" );
	}

	message( "makeMove " + move );
	int dynasty = _game.getMoveDynasty();
	boolean wasActionMove = (_game.getState() == TigrisGame.WAIT_ACTION);

	_game.makeMove( move );

	// update the player's clock.
	// subtract any time used, and add the increment.
	// must do it after the move in case it was illegal.
	long now = System.currentTimeMillis();
	Player movePlayer = _game.getPlayer( dynasty );
	long timeConsumed = ( now - _lastMoveTime );
	if ( !_countUp )
	   timeConsumed = -(timeConsumed);
	// give the play bonus time based equal to the
	// increment if it was an action move.
	if ( wasActionMove )
	   timeConsumed -= _options.increment;
	movePlayer.setClock( movePlayer.getClock() - timeConsumed );
	_lastMoveTime = now;

	// hide the tiles in the move.
	// this is the only move type that can contain
	// hidden information meant only for the game server
	// but first send it to the person who made the move
	Move hiddenMove = move;
	if ( move.isSwapTiles() ) {
	    // replace the move with one with hidden tiles
	    TileBag hiddenTiles = new TileBag( hiddenMove.getTiles() );
	    hiddenTiles.hideTiles();
	    hiddenMove = Move.newSwapTiles( dynasty, hiddenTiles );
	}

	// let the move history know the *real* move.
	// notify first, so history is up to date for callbacks.
	_moveHistory.moveMade( move );

	// propogate the move to all the players.
	// but don't repeat if we already sent him the priviliged info.
	for ( int pp=0; pp < _players.length; pp++ ) {
	    if ( pp == dynasty )
		_players[pp].moveMade( move );
	    else 
		_players[pp].moveMade( hiddenMove );

	    // also show the time remaining for that player.
	    _players[pp].showClock( dynasty, movePlayer.getClock() );
	}

	// also let the observers know.
	Enumeration ee;
	for ( ee = _observers.elements(); ee.hasMoreElements(); ) {
	    ((GameObserver) ee.nextElement()).moveMade( move );
	}

	// check for gameover.
	if ( _game.getState() == TigrisGame.GAME_OVER ) {
	    showAllScreens();
	    endGame();
	    return;
	} 

	if ( _game.getActionNumber() == 1 &&     
	     _game.getState() == TigrisGame.WAIT_ACTION )    
	    showAllScreens();

	// if it is the start of a new turn, show everyone their hand
	// otherwise, if it was a swap move, 
	// let that player only know about his tiles,
	else if ( move.isSwapTiles() ) {
	    int dyn = move.getDynasty();
	    _players[dyn].showScreen( dyn, _game.getPlayer(dyn) );
	    showScreenToObservers( dyn, _game.getPlayer(dyn) );
	}

	// give special notification to the person who's move is next.
	_players[ _game.getMoveDynasty() ].yourMove();	
    }

    public void undoMoves( GameObserver obs, int number )
	throws ServerException
    {
	if ( ! _started ) {
	    throw new ServerException("No game in progress, " +
				      "cannot make a move");
	}

	boolean isPlayer = false;
	for ( int pp=0; pp < _players.length; pp++ ) {
	    if ( _players[pp] == obs ) 
		isPlayer = true;
	}
	if ( ! isPlayer ) {
	    throw new ServerException("Only a player may ask to undoMove");
	}

	// check illegal of irrelavent move numbers.
	if ( number < 0 ) {
	    throw new ServerException("Cannot undo a negative" +
				      " number of moves");
	} 
	else if ( number == 0 ) {
	    return;
	}
	else if ( number > _moveHistory.size()-1 ) {
	    throw new ServerException("Cannot undo " + number + " moves," +
				      " only " + (_moveHistory.size()-1) + 
				      " can be undone" );
	} 

	message( "undoMoves " + number);
	
	Vector requestThreads = new Vector();
	RequestUndoThread thread;
	for ( int pp=0; pp < _players.length; pp++ ) {
	    if ( _players[pp] != obs ) {
		thread = new RequestUndoThread( _players[pp], number );
		thread.start();
		requestThreads.addElement( thread );
	    }
	}

	for ( Enumeration ee = requestThreads.elements(); 
	      ee.hasMoreElements(); ) { 
	    thread = (RequestUndoThread) ee.nextElement();
	    try {
		thread.join();
	    } catch ( InterruptedException ie ) {
		System.out.println( thread + " interrupted, assuming FALSE" ); 
	    }

	    if ( ! thread.answer )
		throw new ServerException( "Undo request denied by " +
					   thread.obs.getName() );
	}

// 	System.out.println("undoing " + number + " moves ");

	for ( int ii=0; ii< number; ii++ ) 
	    _moveHistory.removeLastMove();

	// restore our copy of the game with the new latest from history.
	_game = _moveHistory.getGame();

	// restart the game.
	reallyStartGame();
    }

    class RequestUndoThread extends Thread
    {
	GameObserver obs;
	boolean answer = false;
	int number = 1;

	RequestUndoThread( GameObserver obs, int number )
	{ 
	    this.obs = obs; 
	    this.number = number;
	}

	public void run() 
	{
	    answer = obs.requestUndoMoves( number );
	}
    }

    public MoveHistory getMoveHistory()
    {
	return getMoveHistory( null );
    }

    public MoveHistory getMoveHistory( GameObserver obs )
    {
// 	System.out.println("Server Impl getting move history " + 
// 			   _moveHistory.size() );
	message( "getMoveHistory" );

	if ( _moveHistory == null ) return null;

	int view = -1;
	for ( int ii=0; ii<_players.length; ii++ ) {
	    if ( obs == _players[ii] ) {
		view = ii;
		break;
	    }
	}

	MoveHistory historyCopy = (MoveHistory) _moveHistory.clone();
	if ( _game != null && 
	     _game.getState() != TigrisGame.GAME_OVER )
	    historyCopy.hideTiles( view );
	
	return historyCopy;
    }

    public void observeGame( GameObserver obs )
    {
	message( "observerGame " + obs );

	boolean alreadyObserver = false;
	if ( _observers.contains( obs ) )
	    alreadyObserver = true;

	for ( int ii=0; ii< _players.length; ii++) {
	    if ( obs.equals( _players[ii] ) )
		 alreadyObserver = true;
	}

	if ( ! alreadyObserver ) {
	    _observers.addElement( obs );

	    // catch the observer up if he started late.
	    // he'll have to get a move list though for full history.
	    obs.gameStart( _game );
	}
    }
    
    public void unobserveGame( GameObserver obs )
    {
	message( "unobserverGame " + obs );
	_observers.removeElement( obs );
    }

    /*
    public void chat( GameObserver obs, String msg )
    {
	String name = null;
	if ( obs != null )
	    msg = obs.getName() + ": " + msg;
	else
	    msg = "Nobody: " + msg;

	for ( int pp=0; pp < _players.length; pp++ ) {
	    _players[pp].chatMsg( msg );
	}

	// also let the observers know.
	for ( Enumeration ee = _observers.elements(); ee.hasMoreElements(); ) {
	    ((GameObserver) ee.nextElement()).chatMsg( msg );
	}
    }
    */

    public void setAutoStart( boolean val )
    {
	_autoStart = val;
    }

    private void showScreenToObservers( int dynasty, TileHolder tiles )
    {
	// move history is a priviledges observer with full info.
	_moveHistory.showScreen( dynasty, tiles );

	for ( Enumeration ee = _observers.elements(); ee.hasMoreElements(); ) {
	    ((GameObserver) ee.nextElement()).showScreen( dynasty, tiles );
	}
    }

    private void showAllScreens()
    {
	for ( int pp=0; pp < _players.length; pp++ ) {
	    _players[pp].showScreen( pp, _game.getPlayer(pp) );
	    showScreenToObservers( pp, _game.getPlayer(pp) );
	}
    }

    public void message( String msg )
    {
	if ( verbosity > 1 )
	    System.out.println(this.getClass().getName()
			       + " " + msg );
    }

    public void printGameDump()
    {
	System.out.println("::: GameServerImpl game dump :::");
	System.out.println("::: TODO :::");
	//_game.printGameDump();
    }
}
