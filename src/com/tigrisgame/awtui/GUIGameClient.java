/* File: GUIGameClient.java
 * Description:  GameObserver wrapper for GamePanel, subscribes to GameServer
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * TODO. don't forget about moveEnabled on the GamePanel
 */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ResourceLoader;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class GUIGameClient extends DefaultGameObserver 
    implements GameObserver, ActionListener
{
    Frame _frame;
    MenuBar _menuBar = new MenuBar();

    /** logs victory-point awards into the game log as they happen,
     * so points earned by any seat (tiles, conflicts, monuments) show
     * up right away, next to the move that caused them.
     */
    private class VPLogger extends GameListenerAdapter
    {
	public void tileVPAwarded( int dynasty, Sphere sphere, Sphere sphere2 )
	{
	    String way = ( sphere2 != null && sphere2.isBlack() )
		? " (tile, black leader)" : " (tile)";
	    award( dynasty, 1, sphere, way );
	}

	public void conflictVPAwarded( int dynasty,
				       Sphere sphere, int num )
	{
	    award( dynasty, num, sphere, " (conflict)" );
	}

	public void monumentVPAwarded( int dynasty, Sphere sphere )
	{
	    award( dynasty, 1, sphere, " (monument)" );
	}

	private void award( int dynasty, int num,
			    Sphere sphere, String way )
	{
	    StringBuffer strbuf = new StringBuffer();
	    if ( dynasty >= 0 && _vpGame != null &&
		 dynasty < _vpGame.getNumPlayers() )
		strbuf.append( _vpGame.getPlayer( dynasty ).getName() );
	    else
		strbuf.append( "Player " + dynasty );
	    strbuf.append( ": +" + num + " " +
			   sphere.toString().toLowerCase() + " VP" + way );

	    String msg = strbuf.toString();
	    if ( _collectingVP )
		_pendingVPs.addElement( msg );
	    else
		_gamePanel.logEvent( msg );
	}
    }

    private VPLogger _vpLogger = new VPLogger();
    private Vector _pendingVPs = new Vector();
    private boolean _collectingVP = false;
    // the game this session's listener is registered on; the private
    // _game in DefaultGameObserver can't be read from the inner class.
    private TigrisGame _vpGame = null;

    GamePanel _gamePanel = new GamePanel();
    //MoveHistoryPanel _moveHistoryPanel = new MoveHistoryPanel();
    //MoveHistory _moveHistory;
//     TextArea _chatArea = new TextArea("chat", 15, 100, TextArea.
// 				      SCROLLBARS_VERTICAL_ONLY);
    GameServer _gameServer;

    boolean _undoPending;
    boolean _undoResponse;

    // pacing for human observation: this client is seated as the
    // human player (seat 0).
    static final int HUMAN_DYNASTY = 0;
    /** seconds to pause (in the moveMade callback) after each
     * opponent move so the board change can be seen.  Set with
     * -Dtigris.delay=<seconds> (fractional values allowed, 0 turns
     * the pacing off); default 3.
     */
    double moveDelaySeconds = doubleProperty( "tigris.delay", 3.0 );

    public GUIGameClient( GameServer server )
    {
	// seat 0 is the human player; the game server copies this
	// name into its Player record on assignPlayer().
	setName( "You" );

	_gameServer = server;
	//setLayout( new BorderLayout() );
	//add( BorderLayout.CENTER, _gamePanel );

	// hmm...  need to make the chat panel much smaller....
//  	Panel southPanel = new Panel();
//  	southPanel.setLayout( new BorderLayout() );
	//add( BorderLayout.SOUTH, _chatArea );

	// TODO
	// menu stuff for controlling game panel
	//setMenuBar();
	
	_gamePanel.addActionListener( this );


	UndoListener undoListener = new UndoListener();
	_gamePanel.okayButton.addActionListener( undoListener );
	_gamePanel.cancelButton.addActionListener( undoListener );

	// create a new frame.
	_frame = new Frame("Tigris & Euphrates");

	_frame.add( _gamePanel );
	fillMenuBar();
	_frame.setMenuBar( _menuBar );
	_frame.setSize( UIScale.scale( 625 ), UIScale.scale( 550 ) );
	GUIUtils.centerWindowOnScreen( _frame );
    }

    public GamePanel getGamePanel()
    {
	return _gamePanel;
    }

    public void setVisible( boolean value )
    {
	_frame.setVisible( value );
    }

    public void setTitle( String title )
    {
	_frame.setTitle( title );
    }

    public void fillMenuBar()
    {
	// scale the menu bar height and text with the rest of the UI.
	Font menuFont = UIScale.createFont( "Dialog", Font.PLAIN, 12 );

	Menu gameMenu = new Menu( "Game" );

	MenuItem saveItem = new MenuItem("Save (Server View)");
	MenuItem saveLocalItem = new MenuItem("Save (Local View)");	
	MenuItem exitItem = new MenuItem("Exit");	
	MenuItem undoItem = new MenuItem("Undo Move");	
	MenuItem undoNItem = new MenuItem("Undo 3 Moves");	

	saveItem.setEnabled( false );
	saveItem.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent ae ) {
	       //_gameServer.saveGame( "SAVE_GAME" ); 
	    }});
	saveLocalItem.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent ae ) {
		ResourceLoader.fileSaveObject( _gamePanel._game, 
					       "LOCAL_SAVE_GAME" ); 
	    }});
	exitItem.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent ae ) {
		System.exit(0);
	    }});
	undoItem.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent ae ) {
		new Thread(){ public void run() {
		    undoMoves( 1 );}}.start();
	    }});
	undoNItem.addActionListener( new ActionListener() {
	    public void actionPerformed( ActionEvent ae ) {
		new Thread(){ public void run() {
		    undoMoves( 3 );}}.start();
	    }});

	gameMenu.add( saveItem );
	gameMenu.add( saveLocalItem );
	gameMenu.add( undoItem );
	gameMenu.add( undoNItem );
	gameMenu.add( new MenuItem("-" ));
	gameMenu.add( exitItem );

	_menuBar.add( gameMenu );

	Menu optionsMenu = new Menu( "Options" );

	CheckboxMenuItem gridItem =  
	    new CheckboxMenuItem("Show Grid",
				 _gamePanel.boardPanel.drawGrid );
	CheckboxMenuItem kingdomItem = 
	    new CheckboxMenuItem("Show Kingdoms",
				 _gamePanel.boardPanel.highlightKingdoms );
	CheckboxMenuItem lastActionItem = 
	    new CheckboxMenuItem("Show Last Action", 
				 _gamePanel.highlightLastAction );

	gridItem.addItemListener( new ItemListener() {
	    public void itemStateChanged(ItemEvent ae) {
		CheckboxMenuItem source = (CheckboxMenuItem) ae.getSource();
		_gamePanel.boardPanel.
		    setDrawGrid( source.getState() );
	    }});
	kingdomItem.addItemListener( new ItemListener() {
	    public void itemStateChanged(ItemEvent ae) {
		CheckboxMenuItem source = (CheckboxMenuItem) ae.getSource();
 		_gamePanel.boardPanel.setOutlineKingdoms( source.getState() );
	    }});
	lastActionItem.addItemListener( new ItemListener() {
	    public void itemStateChanged(ItemEvent ae) {
		CheckboxMenuItem source = (CheckboxMenuItem) ae.getSource();
 		_gamePanel.setHighlightLastAction( source.getState());
	    }});

	optionsMenu.add( gridItem );
	optionsMenu.add( kingdomItem );
	optionsMenu.add( lastActionItem );

	_menuBar.add( optionsMenu );

	// apply the scaled font to the menu bar and every item.
	// (some AWT platforms look at the Menu's font, others at
	//  the MenuItem's font, so set both to be safe.)
	setMenuFont( _menuBar, menuFont );
    }

    /** apply a font recursively to a menu bar or menu. */
    private static void setMenuFont( Object menuObject, Font font )
    {
	if ( menuObject instanceof MenuComponent )
	    ((MenuComponent) menuObject).setFont( font );

	if ( menuObject instanceof MenuBar ) {
	    MenuBar menuBar = (MenuBar) menuObject;
	    for ( int ii=0; ii<menuBar.countMenus(); ii++ )
		setMenuFont( menuBar.getMenu( ii ), font );
	}
	else if ( menuObject instanceof Menu ) {
	    Menu menu = (Menu) menuObject;
	    for ( int ii=0; ii<menu.countItems(); ii++ )
		setMenuFont( menu.getItem( ii ), font );
	}
    }

    public void undoMoves( int number )
    {
 	boolean oldMovesEnabled = _gamePanel.getMovesEnabled();
	_gamePanel.setMovesEnabled( false );
	_gamePanel.setStatusField( "Waiting for permission " +
				   "from other players to undo " +
				   number + " move(s).");
 	try {
 	    _gameServer.undoMoves( this, number );
 	} catch (ServerException se) {
 	    _gamePanel.setStatusField( se.getMessage() );
 	    _gamePanel.setMovesEnabled( oldMovesEnabled );
 	}
    }

    // **********************
    // Implement GameObserver
    // **********************
    
    public void gameStart( TigrisGame g )
    {
	super.gameStart( g );

	// move history stuff ??
	//_moveHistory = new MoveHistory();
	//_moveHistoryPanel.setMoveHistory( _moveHistory );
	//_gameServer.observeGame( _moveHistory );

	// ASSUMES: that the move history will get all future calls
	// and only needs to be sent the game start,
	// if this is on a different thread as the server, that may not be the case.
	//_moveHistory.gameStart( g );
	_gamePanel.clearMoveLog();
	// listen on the view's game so VP awards log as they happen.
	_pendingVPs.removeAllElements();
	_vpGame = g;
	addGameListener( _vpLogger );
	_gamePanel.setGameView( g );
	_gamePanel.setMovesEnabled( false );
	_gamePanel.setPlayerView( g.getPlayer( 0 ) );
    }

    public void moveMade( Move move )
    {
	// VP awards fire from the view's replay of the move; collect
	// them so they log *after* the move line instead of before.
	_collectingVP = true;
	_pendingVPs.removeAllElements();
	try {
	    super.moveMade( move );
	}
	finally {
	    _collectingVP = false;
	}

	_gamePanel.logMove( move );
	for ( int ii=0; ii<_pendingVPs.size(); ii++ )
	    _gamePanel.logEvent( (String) _pendingVPs.elementAt(ii) );
	_pendingVPs.removeAllElements();

	_gamePanel.setGameView( getGame() );

	// pace the display for a human watching computer players:
	// pause after every move so the change to the board can be
	// seen (the human's own moves are shown with no pause).
	if ( move.getDynasty() != HUMAN_DYNASTY && moveDelaySeconds > 0 ) {
	    try {
		Thread.sleep( (long)( moveDelaySeconds * 1000.0 ) );
	    }
	    catch ( InterruptedException ie ) {
		// fall through; just resume.
	    }
	}

	_gamePanel.setMovesEnabled( false );
    }

    public void showScreen( int dynasty, TileHolder tiles )
    {
	super.showScreen( dynasty, tiles );
	_gamePanel.setGameView( getGame() );
    }

    public void gameEnd( int [] order )
    {
	super.gameEnd( order );

	_gamePanel.setMovesEnabled( false );
// 	chatMsg( "The game has ended, finish order " + ArrayUtils.prettyPrint( order ));
    }

    public synchronized boolean requestUndoMoves( int number )
    {
	_undoResponse = false;
	_undoPending = true;

	_gamePanel.setMovesEnabled( false );
	_gamePanel.setStatusField( "Your opponent would like to undo " +
				   "the last " + number + " move(s)." );

	try {
	    wait();
	} catch (InterruptedException ie) {
	    if ( verbosity > 0 )
		System.out.println("GUIGameClient interrupted " +
				   "in requestUndoMove!!");
	}

	_undoPending = false;

	return _undoResponse;
    }

    public void yourMove()
    {
	TigrisGame game = getGame();

	_gamePanel.setMovesEnabled( true );

	// update the context to the current game and player.
	//_moveHistoryPanel.setGameView( _game );

	_gamePanel.setGameView( game );
	// bind the panel to the real view-game player rather than a
	// clone: getGame() hands out a throwaway copy, so victory
	// points earned later (which mutate the real game) would
	// never show up until the screen got re-bound.
	if ( _vpGame != null )
	    _gamePanel.setPlayerView
		( _vpGame.getPlayer( _vpGame.getMoveDynasty()));
	else
	    _gamePanel.setPlayerView
		( game.getPlayer( game.getMoveDynasty()));
    }

//     public void chatMsg( String msg )
//     {
// 	_chatArea.append( msg );
//     }

    // **********************
    // Implement ActionListener
    // **********************

    public void actionPerformed( ActionEvent ae )
    {
	if ( ae.getSource() instanceof Move ) {
	    Move move = (Move) ae.getSource();
	    try 
		{
		    _gameServer.makeMove( this, move );
		}
	    catch( IllegalMoveException ime )
		{
		    System.out.println("Illegal Move: " + ime.getMessage() );
		    _gamePanel.illegalMove( ime );
// 		    chatMsg( "Illegal Move: " + ime.getMessage() );
		    
		    // red colored sphere means illegal move.
		    // TODO ... what to do with this...
		    //playerLabel.setSphere( Sphere.RED );
		}
	    catch( Exception ex) {
		ex.printStackTrace();
	    }
	}
    }

    public synchronized void handleUndoResponse( boolean response )
    {
	if ( _undoPending ) {
	    _undoResponse = response;
	    _gamePanel.setStatusField("");
	    notify();
	}
    }

    class UndoListener implements ActionListener
    { 
	public void actionPerformed( ActionEvent ae )
	{
	    if ( ae.getSource() == _gamePanel.okayButton )
		handleUndoResponse( true );
	    else if ( ae.getSource() == _gamePanel.cancelButton ) 
		handleUndoResponse( false );
	}
    }

    public static void main( String args[] ) throws Exception 
    {
	GameServerImpl server = new GameServerImpl();
	if ( args.length > 0 ) {
	    server.loadGame( args[0] );
	} else {
	    // how many seats?  the default is a 4 player game:
	    // you plus three computer opponents.  supported range [2..4].
	    int numPlayers = intProperty( "tigris.players", 4 );
	    if ( numPlayers < 2 || numPlayers > 4 ) {
		System.out.println("Warning: tigris.players=" + numPlayers +
				   " out of range [2..4], using 2.");
		numPlayers = 2;
	    }
	    server.newGame( numPlayers );
	}

	int numPlayers =
	    server.getMoveHistory().getGame().getNumPlayers();

	// default AI by seat count: MiniMaxAI is only fair head-to-head.
	// (-Dtigris.ai always wins if set explicitly.)
	String aiName = System.getProperty( "tigris.ai" );
	if ( aiName == null )
	    aiName = ( numPlayers == 2 ) ? "MiniMaxAI" : "MultiMaxAI";
	else
	    aiName = aiName.trim();

	if ( numPlayers > 2 && aiName.equalsIgnoreCase( "MiniMaxAI" ))
	    System.out.println("Warning: MiniMaxAI evaluates only the " +
			       "scores of players 0 and 1, which is not " +
			       "fair in a " + numPlayers + " player game.  " +
			       "Consider -Dtigris.ai=MultiMaxAI, which " +
			       "handles any number of players.");

	Frame frame = new Frame();
	GUIGameClient client = new GUIGameClient( server );
	frame.add( client._gamePanel );
	// attach the menu bar to the frame that is actually shown.
	// (the client constructor builds the menus on its own
	//  internal frame, which is never displayed when run
	//  from main.)
	frame.setMenuBar( client._menuBar );
	server.assignPlayer( client, 0 );

	// seat 0 is the human player ("You"); every other seat is an AI.
	for ( int dynasty = 1; dynasty < numPlayers; dynasty ++ ) {
	    com.tigrisgame.rules.GameObserver ai = createAI( server, aiName );
	    // DefaultGameObserver.setName - the server copies this name
	    // into the Player record on assignPlayer().
	    ((com.tigrisgame.rules.DefaultGameObserver) ai).
		setName( "Computer " + dynasty );
	    server.assignPlayer( ai, dynasty );
	}
	
	
	frame.validate();
	frame.setSize( UIScale.scale( 800 ), UIScale.scale( 600 ) );
	GUIUtils.centerWindowOnScreen( frame );
	frame.addWindowListener( new WindowAdapter()
	    { public void windowClosing( WindowEvent e ) 
		{ System.exit(1);}
	    });
	frame.setVisible( true );

	server.startGame();
    }

    // **********************
    // AI configuration
    // **********************

    /** build the AI opponent for one seat.
     * <ul>
     * <li>tigris.ai = MiniMaxAI | MultiMaxAI | CraigAI -- passed in by
     *     main, which picks the default by player count</li>
     * <li>tigris.ai.actionDepth = N</li>
     * <li>tigris.ai.trueDepth = N</li>
     * <li>tigris.ai.prune = N</li>
     * <li>tigris.ai.keepProb = X</li>
     * <li>tigris.ai.thinkMillis = N (CraigAI only: wall-clock cut)</li>
     * </ul>
     */
    private static com.tigrisgame.rules.GameObserver
	createAI( GameServer gameServer, String aiName )
    {
	int actionDepth = intProperty( "tigris.ai.actionDepth", -1 );
	int trueDepth   = intProperty( "tigris.ai.trueDepth", -1 );
	int pruneSize   = intProperty( "tigris.ai.prune", -1 );
	double keepProb = doubleProperty( "tigris.ai.keepProb", -1 );

	if ( aiName.equalsIgnoreCase( "CraigAI" ) ) {
	    // CraigAI takes its game server in the constructor
	    // (it has no setGameServer).
	    com.tigrisgame.ai.CraigAI ai =
		new com.tigrisgame.ai.CraigAI( gameServer );
	    // CraigAI is time-limited, not node-limited: interpret
	    // actionDepth as its search depth and add a think-time switch.
	    if ( actionDepth >= 0 )
		ai.setThinkActionDepth( actionDepth );
	    int thinkMillis = intProperty( "tigris.ai.thinkMillis", -1 );
	    if ( thinkMillis >= 0 )
		ai.setThinkTime( thinkMillis );
	    return ai;
	}

	if ( aiName.equalsIgnoreCase( "MultiMaxAI" ) ) {
	    // MultiMaxAI() defaults are (1, 10, 80).
	    com.tigrisgame.ai.MultiMaxAI ai =
		( actionDepth < 0 && trueDepth < 0 && pruneSize < 0 ) ?
		new com.tigrisgame.ai.MultiMaxAI() :
		new com.tigrisgame.ai.MultiMaxAI
		    ( (actionDepth < 0) ? 1 : actionDepth,
		      (trueDepth < 0) ? 10 : trueDepth,
		      (pruneSize < 0) ? 80 : pruneSize );
	    ai.setGameServer( gameServer );
	    if ( keepProb >= 0 )
		ai.setKeepProb( keepProb );
	    return ai;
	}

	// default: MiniMaxAI.  class defaults are (2, 20, 80).
	com.tigrisgame.ai.MiniMaxAI ai =
	    ( actionDepth < 0 && trueDepth < 0 && pruneSize < 0 ) ?
	    new com.tigrisgame.ai.MiniMaxAI() :
	    new com.tigrisgame.ai.MiniMaxAI
		( null,
		  (actionDepth < 0) ? 2 : actionDepth,
		  (trueDepth < 0) ? 20 : trueDepth,
		  (pruneSize < 0) ? 80 : pruneSize );
	ai.setGameServer( gameServer );
	if ( keepProb >= 0 )
	    ai.setKeepProb( keepProb );
	return ai;
    }

    private static int intProperty( String name, int dflt )
    {
	String value = System.getProperty( name );
	if ( value == null )
	    return dflt;
	try {
	    return Integer.parseInt( value.trim() );
	}
	catch ( NumberFormatException nfe ) {
	    System.out.println("Warning: bad value for " + name +
			       "=\"" + value + "\", using " + dflt );
	    return dflt;
	}
    }

    private static double doubleProperty( String name, double dflt )
    {
	String value = System.getProperty( name );
	if ( value == null )
	    return dflt;
	try {
	    return Double.parseDouble( value.trim() );
	}
	catch ( NumberFormatException nfe ) {
	    System.out.println("Warning: bad value for " + name +
			       "=\"" + value + "\", using " + dflt );
	    return dflt;
	}
    }
}
