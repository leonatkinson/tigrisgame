/* File: GamePanel.java
 * Description:  Resizable panel that holds the board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * Always displays as much information as it has.
 * Always moves for the current player.
 * Canvas for the Tigris game board and all of the other buttons and displays
 *
 * TODO:
 * . moveHistoryHandler buttons, and figure out which m_game is chaning for
 * partial info, and display, and for getting state.
 *
 LAYOUT
 ------
 * GamePanel, layout = borderLayout
  ** SOUTH = bottomPanel, layout = boardLayout
     *** CENTER = playerScreen
     *** SOUTH = Panel
      **** statusField
      **** selectionField
  ** EAST = rightPanel, borderLayout
     *** NORTH = playerLabel
     *** CENTER = rightMiddlePanel, layout = gridLayout (3,1)
	 **** playerList
	 **** tempPanel, layout = gridLayout(3,2)
	      ***** tileBagButton
	      ***** blank
	      ***** gridCheckbox
	      ***** kingdomCheckbox
	      ***** okayButton
	      ***** cancelButton
  ** CENTER = boardPanel
  ------
 */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

public class GamePanel extends Panel implements ActionListener, ItemListener
{
    // Panels
    public BoardPanel boardPanel = new BoardPanel();
    private PlayerScreen playerScreen = new PlayerScreen();
    private Panel rightPanel = new Panel();
    private Panel bottomPanel = new Panel();
    private ConflictPanel conflictPanel = new ConflictPanel();
    private TextArea selectionTextArea = 
	new TextArea( "text", 5, 20, TextArea.SCROLLBARS_NONE);
    private SelectionPanel actionSelectionPanel = new SelectionPanel();
    private Panel selectionPanel = new Panel();
    private CardLayout selectionCards = new CardLayout();

    // ?? maybe.
    private Panel playerLabelPanel = new Panel();
    private PlayerLabel [] playerLabels = new PlayerLabel[4];
    private java.awt.List playerList = new java.awt.List( 7 );
//     private TextArea statusField = 
// 	new TextArea("-=- Status Field -=-", 2, 50, TextArea.SCROLLBARS_NONE);
    private Font fixedWidthFont =
	UIScale.createFont( "Monospaced", Font.PLAIN, 12 );
    
    // UI State Variables
    protected TigrisGame _game;
    private Object _selectedObject;
    private Vector _actionListeners = new Vector();
    private boolean _moveEnabled = false;

    // ***** TO MOVE TO CONTROL PANEL
    private TextArea selectionField = 
	new TextArea("-=- Selection Field -=-", 2, 50, TextArea.SCROLLBARS_NONE);
    private Button tileBagButton = new Button("Tile Bag: ??");
    protected Button okayButton = new Button("OK");
    protected Button cancelButton = new Button("Cancel");

    protected boolean highlightLastAction = true;

    // ***** TO BE GUTTED --> moved to GUIGameClient.java menus
//     private Button evalButton = new Button("Eval");
    //private Button saveButton = new Button("Save");
//     private Checkbox lastActionCheckbox = new Checkbox("Last Action", true);
//     private Checkbox gridCheckbox = new Checkbox("Grid", true);
//     private Checkbox kingdomCheckbox = new Checkbox("Kingdoms", true);

    public static int verbosity = 0;

    public GamePanel()
    { 
	setLayout( new IterativeLayout() );
	//setLayout( new BorderLayout() );
	
	setBackground( TigrisGraphics.landColor );
	setForeground( TigrisGraphics.blackTileColor );

	// set properties for components
	// note: 20 columns
	playerList.setFont( fixedWidthFont );
	tileBagButton.setFont( fixedWidthFont );
	okayButton.setFont( fixedWidthFont );
	cancelButton.setFont( fixedWidthFont );
	selectionTextArea.setFont( fixedWidthFont );
	selectionField.setFont( fixedWidthFont );

	selectionField.setEditable( false );
// 	statusField.setEditable( false );

	// Layout and add Components and Panels.
	Panel buttonPanel = new Panel();
	buttonPanel.setLayout( new GridLayout( 0,2 ) );
//  	Panel leftButtons = new Panel();
//  	Panel rightButtons = new Panel();
//  	leftButtons.setLayout( new GridLayout( 1,2 ));
//  	rightButtons.setLayout( new GridLayout( 1,2 ));
	//buttonPanel.add( leftButtons );
	//buttonPanel.add( rightButtons );
// 	buttonPanel.add( gridCheckbox );
// 	buttonPanel.add( kingdomCheckbox );
// 	buttonPanel.add( lastActionCheckbox );
 	buttonPanel.add( new Label() );
 	//buttonPanel.add( saveButton );
	buttonPanel.add( tileBagButton );
	buttonPanel.add( okayButton );
	buttonPanel.add( cancelButton );

	selectionTextArea.setEditable( false );
	selectionTextArea.setBackground( TigrisGraphics.landColor );
	selectionTextArea.setForeground( TigrisGraphics.blackTileColor );
	selectionPanel.setLayout( selectionCards );
	selectionPanel.add( conflictPanel, "conflict" );
// 	selectionPanel.add( selectionTextArea, "text" );
	selectionPanel.add( actionSelectionPanel, "action" );

  	Panel rightMiddlePanel = new Panel();
  	rightMiddlePanel.setLayout( new BorderLayout());
	//rightMiddlePanel.add( playerList, "North" );
	rightMiddlePanel.add( selectionPanel, "Center" );

	rightPanel.setLayout( new BorderLayout() );
	//rightPanel.setLayout( new IterativeLayout() );

	playerLabelPanel.setLayout( new GridLayout( 0, 1 ));
	ActionListener playerListener = new PlayerListener();
	for ( int ii=0; ii<4; ii++) {
	    playerLabels[ii] = new PlayerLabel();
	    playerLabels[ii].setTileSize( UIScale.scale( 25 ),
					  UIScale.scale( 25 ) );
	    playerLabelPanel.add( playerLabels[ii] );
	    playerLabels[ii].addActionListener( playerListener );
	}

	rightPanel.add( playerLabelPanel, "North" );
	rightPanel.add( selectionPanel, "Center" );
	rightPanel.add( buttonPanel, "South" );

//  	Panel p1 = new Panel();
//  	p1.setLayout( new GridLayout(0,2) );
//  	p1.add( statusField );
//  	p1.add( selectionField );

  	bottomPanel.setLayout( new BorderLayout() );
  	bottomPanel.add( playerScreen, "Center");
//  	bottomPanel.add( p1, "South" );

  	Panel centerPanel = new Panel( new BorderLayout() );
//  	Panel centerPanel = new Panel( new IterativeLayout());
  	centerPanel.add( boardPanel, "Center" );
  	//centerPanel.add( playerScreen, "South" );
  	//centerPanel.add( selectionPanel, "East" );

	add( centerPanel, "Center");
	add( rightPanel, "East");
	add( bottomPanel, "South");

	// Event Listeners
	addListeners();
    }


    public void addListeners()
    {
	tileBagButton.addActionListener( this );
	playerScreen.addActionListener( this );
	boardPanel.addActionListener( this );
 	//evalButton.addActionListener( ebh ); 
//  	saveButton.addActionListener( new SaveListener() );
	okayButton.addActionListener( this );
	cancelButton.addActionListener( this );

	playerList.addItemListener( this );
// 	gridCheckbox.addItemListener( this );
// 	lastActionCheckbox.addItemListener( this );
// 	kingdomCheckbox.addItemListener( this );

    }

    public synchronized void addActionListener( ActionListener al )
    {
	if ( ! _actionListeners.contains( al ) )
	    _actionListeners.addElement( al );
    }
    
    public synchronized void removeActionListener( ActionListener al )
    {
	_actionListeners.removeElement( al );
    }

    public void setMovesEnabled( boolean enabled )
    {
	_moveEnabled = enabled;

	if ( enabled == true ) {
	    // clear selection to default when it's your move.
	    setDefaultSelection();
	}
    }

    public boolean getMovesEnabled()
    {
	return _moveEnabled;
    }

    public void setStatusField( String msg )
    {
	actionSelectionPanel.setBottomText( msg );
	selectionCards.show( selectionPanel, "action" );
    }

    public void setHighlightLastAction( boolean value )
    {
	highlightLastAction = value;
	highlightRelevant();
    }

    // **********************
    // GUI Layout and Updates
    // **********************

    // public method for setting the game.
    public void setGameView( TigrisGame g )
    {
	if ( g == null )
	    return;

	TigrisGame oldGame = _game;
	_game = g;

	boardPanel.setBoard( _game.getBoard() );	
	// gui updates, we need everything.
	updateAll();

	// if the game board size has changed or was previously null,
	// we also need to revalidate the layout.
	if ( oldGame == null ||
	     oldGame.getBoard().getNumRows() != 
	     _game.getBoard().getNumRows() ||
	     oldGame.getBoard().getNumCols() != 
	     _game.getBoard().getNumCols() )
	    validate();

	// automatically set view to the current player to move.
	//setPlayerView( g.getPlayer( g.getMoveDynasty()));
    }

    // sets the dynasty to display.
    // depending on whether viewAll is set,
    // we may see a blank player.
    public void setPlayerView( Player pl )
    {
	if ( pl == null )
	    return;

	// we now only know what info we are allowed to, so show everything we know!
	playerScreen.setPlayer( pl );
	//playerLabel.setPlayer( pl );

	// gui updates, do just what we need.
	updatePlayerLabels();
    }    

    // **********************
    // Game log
    // **********************

    /** clear the running log (start of a new game). */
    public void clearMoveLog()
    {
	actionSelectionPanel.clearBottomText();
    }

    /** log a game event (victory point awards etc.) to the log panel
     * verbatim.
     */
    public void logEvent( String msg )
    {
	actionSelectionPanel.logMessage( msg );
    }

    /** append a plain-language description of a move to the log
     * panel, named for the seat that made it.
     */
    public void logMove( Move move )
    {
	if ( move == null || _game == null )
	    return;

	StringBuffer strbuf = new StringBuffer();
	int dynasty = move.getDynasty();
	Player player =
	    ( dynasty >= 0 && dynasty < _game.getNumPlayers() )
	    ? _game.getPlayer( dynasty ) : null;
	if ( player != null )
	    strbuf.append( player.getName() );
	else
	    strbuf.append( "Player " + dynasty );

	strbuf.append( ": " );
	switch ( move.getType() )
	    {
	    case Move.POSITION_LEADER:
		strbuf.append( move.getSphere().toString().toLowerCase() +
			       " leader to " +
			       LocationImpl.formatLocation( move.getLocation() ));
		break;
	    case Move.PLACE_TILE:
		strbuf.append( move.getSphere().toString().toLowerCase() +
			       " tile at " +
			       LocationImpl.formatLocation( move.getLocation() ));
		break;
	    case Move.PLAY_CATASTROPHE:
		strbuf.append( "catastrophe at " +
			       LocationImpl.formatLocation( move.getLocation() ));
		break;
	    case Move.SWAP_TILES:
		strbuf.append( "swaps " + move.getTiles().getNumTiles() +
			       " tiles" );
		break;
	    case Move.CHOOSE_SPHERE:
		strbuf.append( "resolves conflict first for " +
			       move.getSphere().toString().toLowerCase() );
		break;
	    case Move.CHOOSE_SUPPORTERS:
		strbuf.append( "plays " + move.getTiles().getNumTiles() +
			       " supporter tile(s)" );
		break;
	    case Move.BUILD_MONUMENT:
		if ( move.getLocation() == null )
		    strbuf.append( "does not build a monument" );
		else
		    strbuf.append( "builds a " +
				   move.getSphere().toString().toLowerCase() +
				   "/" +
				   move.getSphere2().toString().toLowerCase() +
				   " monument at " +
				   LocationImpl.formatLocation(
				       move.getLocation() ));
		break;
	    case Move.CHOOSE_TREASURE:
		strbuf.append( "takes treasure from " +
			       LocationImpl.formatLocation( move.getLocation() ));
		break;
	    default:
		strbuf.append( Move.typeToString( move.getType() ));
		break;
	    }

	actionSelectionPanel.logMessage( strbuf.toString() );
    }

    // calls all the private methods to read status from the displayGame
    // object and repaints the screen.
    private void updateAll()
    {
	// update and repaint modified fields.
	highlightRelevant();
	updatePlayerLabels();	
	updatePlayerList();
	tileBagButton.setLabel("Tile Bag: " + 
			       _game.getTileBag().getNumTiles() ); 
	updateConflict();
	setDefaultSelection();
	updateSelection();
// 	updateStatusField();
	playerScreen.repaint();	
	boardPanel.repaint();
    }

    private void updateSelection()
    {
	if ( _selectedObject instanceof IllegalMoveException ) {
	    selectionCards.show( selectionPanel, "action" );
	    actionSelectionPanel.setBottomText( "Illegal Move!\n" +
               ((IllegalMoveException)_selectedObject).getMessage());
	    return;
	}

	int state = _game.getState();
	switch ( state )
	    {
	    case TigrisGame.WAIT_ACTION:
		int action = _game.getActionNumber();
		String actNumStr = "" + action;
		if ( action == 1 )
		    actNumStr = "First";
		else if ( action == 2 )
		    actNumStr = "Second";		
		selectionCards.show( selectionPanel, "action" );
		actionSelectionPanel.setTopText( actNumStr + " Action:" );
		actionSelectionPanel.setSelectedObject( _selectedObject );
		break;
	    case TigrisGame.WAIT_TREASURE:
		selectionCards.show( selectionPanel, "action" );
		actionSelectionPanel.setBottomText( "Choose Treasure:\n" +
		       "choose a treasure to take by cliking on the board");
		actionSelectionPanel.setSelectedObject( null );
		break;
	    case TigrisGame.WAIT_ATTACK_SUPPORT:
	    case TigrisGame.WAIT_DEFEND_SUPPORT:
		selectionCards.show( selectionPanel, "conflict" );
		actionSelectionPanel.setSelectedObject( null );
		updateConflict();
		break;
	    case TigrisGame.WAIT_CONFLICT_ORDER:
		selectionCards.show( selectionPanel, "action" );
		actionSelectionPanel.setBottomText( "Choose Conflict Order:\n" +
				"choose the sphere to resolve by clicking on "+
				"a tile or leader" );
		actionSelectionPanel.setSelectedObject( null );
		break;
	    case TigrisGame.WAIT_MONUMENT:
		selectionCards.show( selectionPanel, "action" );
		if ( _selectedObject == null ) {
		    actionSelectionPanel.setBottomText( "Build Monument:\n" +
				    "click where you want the upper-left corner " +
				    "to choose the location of the monument" +
				    "or click cancel to not build a monument");
		actionSelectionPanel.setSelectedObject( null );
		}
		else if ( _selectedObject instanceof Space ) {
		    Space loc = (Space) _selectedObject;
		    actionSelectionPanel.setBottomText( "Build Monument:\n" +
				    "click on a tile " +
				    "to choose the second sphere of the monument " +
				    "at " + loc.getRow() + "," + loc.getCol() + " " +
				    "or click cancel to not build a monument");
		    actionSelectionPanel.setSelectedObject( null );
		}
		else
		    System.out.println("invalid _selectedObject");
		break;
	    case TigrisGame.GAME_OVER:
		selectionCards.show( selectionPanel, "action" );
		actionSelectionPanel.setSelectedObject( null );
		actionSelectionPanel.setBottomText( getGameOverText( _game ));
		break;
	    default:
		System.out.println("Unknown Game state in " +
				   "GamePanel.updateSelection");
	    }

	// update the text field too, until we have the selectionPanel fully up to snuf.
	updateCurrentSelection();

	selectionPanel.repaint();
    }

    private void updateConflict()
    {
	Conflict conflict = _game.getConflict();

	if ( conflict == null ) {
	    conflictPanel.setAttacker( null, -1, -1 );
	    conflictPanel.setDefender( null, -1, -1 );
	}
	else {
	    int attackScreen = conflict.getAttackScreenSupport();
	    int defendScreen = conflict.getDefendScreenSupport();
	    
	    Player attackPlayer = _game.getPlayer( _game.getConflict().getAttacker());
	    Player defendPlayer = _game.getPlayer( _game.getConflict().getDefender());

	    conflictPanel.setConflictSphere( conflict.getSupportSphere() );
	    conflictPanel.setAttacker( attackPlayer, 
				       conflict.getAttackBoardSupport(), 
				       attackScreen );
	    
	    conflictPanel.setDefender( defendPlayer, 
				       conflict.getDefendBoardSupport(), 
				       defendScreen );    
	    
	    StringBuffer text1 = new StringBuffer();
	    text1.append( conflict.getLeaderSphere().toString() );
	    if ( conflict.isExternal() )
		text1.append(" external");
	    else
		text1.append(" internal");
	    text1.append( " conflict");

	    StringBuffer text2 = new StringBuffer( attackPlayer.getName() );
	    text2.append(" attacking ");
	    text2.append( defendPlayer.getName());
	    //text2.append(".");

	    StringBuffer text3 = new StringBuffer();
	    boolean attackersTurn = ( conflict.getAttackScreenSupport() == -1 );
	    int tieAmount = conflict.getNumTilesToTie();
	    if ( _selectedObject != null && 
		 _selectedObject instanceof TileBag ) {
		TileBag selectedTiles = (TileBag) _selectedObject;
		if ( attackersTurn ) {
		    tieAmount += selectedTiles.getNumTiles();
		    conflictPanel.setAttacker( attackPlayer, 
					       conflict.getAttackBoardSupport(),
					       selectedTiles.getNumTiles() );
		}
		else {
		    tieAmount -= ((TileBag) _selectedObject).getNumTiles();
		    conflictPanel.setDefender( defendPlayer, 
					       conflict.getDefendBoardSupport(),
					       selectedTiles.getNumTiles() );
		}
	    }		    
		
	    if ( tieAmount > 0 ) {
		if ( attackersTurn ) {
		    text3.append( "Defender will need ");
		    text3.append( tieAmount );
		    text3.append( " to tie" );
		} else {
		    text3.append( "Defender will lose by " );
		    text3.append( tieAmount );
		}
	    } else if ( tieAmount == 0 ) {
		text3.append( "Defender will win the tie" );
	    } else {
		text3.append( "Defender will win by " );
		text3.append( Math.abs( tieAmount ) );
	    }

	    text3.append(",\nclick additional tiles, ok, or cancel");

	    conflictPanel.setTopText( text1.toString(), text2.toString() );
	    conflictPanel.setBottomText( text3.toString() );
	}

	if ( _game.getState() == TigrisGame.WAIT_CONFLICT_ORDER ||
	     conflict != null && conflict.isExternal() ) {
	    Space joinSpace = _game.getLastSpace();
	    boardPanel.setJoinMarker( new Point( joinSpace.getRow(), 
						 joinSpace.getCol() ));
	    
	} else {
	    boardPanel.setJoinMarker( null );
	}
    }


//     private void updateStatusField()
//     {
// 	if ( _game.getState() != Game.GAME_OVER )
// 	    setStatusField( "Game State: " + 
// 			    /* Game.stateToString( _game.getState() ) + */ "\n" + 
// 			    "Player: " + _game.getMoveDynasty() + "   " +
// 			    "Move: N/A" /* + _game.getMoveNumber() */ );
// 	else
// 	    {
// 		setStatusField( "Game Over" );
// 	    }

// 	// added info for conflicts. 
// 	// -- seems to be updated after a player context switch
// 	if ( _game.getState() == Game.WAIT_SUPPORTERS &&
// 	     _game.getConflict().getAttackScreenSupport() != -1 )
// 	    {
// 		setStatusField( statusField.getText() + "   " + "Conflict: " +
// 				"Attacker added " +  
// 				_game.getConflict().getAttackScreenSupport() +
// 				" extra supporter(s)" );
// 	    }
//     }

    private void updateCurrentSelection()
    {
	String selectionStr = "Selection: ";
	if ( _selectedObject instanceof Tile )
	    selectionStr += _selectedObject + " Tile";
	else if ( _selectedObject instanceof Space )
	    selectionStr += "Space";
	else 
	    selectionStr += _selectedObject;

	selectionField.setText( selectionStr );
    }

    private void updatePlayerList()
    {
	String spacePad = "                         "; // 25 spaces

	playerList.removeAll();
	// note: 20 spaces wide.
	playerList.add( "Player List:            " );
	if ( _game.getState() != TigrisGame.GAME_OVER )
	    playerList.add( "Name              Turn  " );
	else
	    playerList.add( "Name              Score " );
	playerList.add( "------------------------" );

	for ( int ii=0; ii< _game.getNumPlayers(); ii++) {

	    Player player = _game.getPlayer( ii );

	    String playerText = (player.getName()+spacePad).substring(0,15);

	    if ( _game.getState() == TigrisGame.GAME_OVER )
		{
		    // if game over, list the players victory 
		    // points lowest to highest.
		    int [] finalVp = player.getFinalVictoryPoints();
		    for ( int jj=0; jj<finalVp.length; jj++)
			{
			    if ( jj != 0 )
				playerText += ",";
			    playerText += finalVp[jj];
			}
		}
	    else 
		{
		    // otherwise, write who's turn it is by showing what turn number.
		    // also draw a "*D*" for the player who has to make the next decision.
		    if ( ii == _game.getActionDynasty() )
			{
			    switch ( _game.getActionNumber() )
				{
				case 1:
				    playerText += " 1st";
				    break;
				case 2:
				    playerText += " 2nd";
				    break;
				default:
				    // default should only happen if the number
				    // of turns per action is modified
				    playerText += _game.getActionNumber();
				}
			}
		    if ( ii == _game.getMoveDynasty() )
			{
			    playerText += " *D*";
			}
		}
	    // list.
	    playerList.add( playerText );
	}
    }

    // update the player label.
    // green means it's your turn, blue means some other decision of yours,
    // black is not your turn, red is illegal move.
    private void updatePlayerLabels()
    {
	for ( int ii=0; ii<playerLabels.length; ii++) {
	    if ( ii < _game.getNumPlayers() ) {
		playerLabels[ii].setPlayer( _game.getPlayer(ii) );

		if ( ii == _game.getMoveDynasty() )
		    playerLabels[ii].setSphere( Sphere.GREEN );
		else if ( ii == _game.getActionDynasty() )
		    playerLabels[ii].setSphere( Sphere.BLUE );
		else 
		    playerLabels[ii].setSphere( Sphere.BLACK );
	    }
	    else {
		playerLabels[ii].setPlayer( null );
	    }
	}
    }

    /** build the end-of-game score summary.
     * In Tigris & Euphrates a player's score is the smallest of their
     * four sphere totals once treasures are assigned, which is what
     * Player.getFinalVictoryPoints()()[0] reports.  List the players
     * best score first, showing each sphere total with any treasure
     * bonuses so the final numbers can be checked.
     */
    private static String getGameOverText( TigrisGame game )
    {
	StringBuffer strbuf = new StringBuffer( "Game Over\n\n" );

	int numPlayers = game.getNumPlayers();
	int scores [] = new int [ numPlayers ];
	int order [] = new int [ numPlayers ];
	int topScore = Integer.MIN_VALUE;
	for ( int pp=0; pp<numPlayers; pp++ ) {
	    order[pp] = pp;
	    // final victory points come back sorted ascending,
	    // index 0 is the player's actual (lowest) score.
	    scores[pp] = game.getPlayer(pp).getFinalVictoryPoints()[0];
	    if ( scores[pp] > topScore )
		topScore = scores[pp];
	}

	// sort the finish order best score first (insertion sort;
	// the player count is tiny).
	for ( int ii=1; ii<numPlayers; ii++ ) {
	    for ( int jj=ii;
		  jj>0 && scores[order[jj]] > scores[order[jj-1]];
		  jj-- ) {
		int tempInt = order[jj];
		order[jj] = order[jj-1];
		order[jj-1] = tempInt;
	    }
	}

	Sphere spheres [] = Sphere.getSpheres();
	for ( int ii=0; ii<numPlayers; ii++ ) {
	    Player player = game.getPlayer( order[ii] );
	    StringBuffer line = new StringBuffer
		( player.getName() + ": " + scores[order[ii]] );
	    if ( scores[order[ii]] == topScore )
		line.append( "  (winner)" );

	    StringBuffer detail = new StringBuffer();
	    int assignment [] = player.getTreasureAssignment();
	    for ( int ss=0; ss<spheres.length; ss++ ) {
		if ( ss > 0 )
		    detail.append( ", " );
		detail.append( spheres[ss].toString().toLowerCase() +
			       " " + player.getVictoryPoints( spheres[ss] ));
		if ( assignment[ss] > 0 )
		    detail.append( "+" + assignment[ss] );
	    }

	    strbuf.append( line + "\n      (" + detail + ")\n" );
	}

	return strbuf.toString();
    }

    private void highlightRelevant()
    {
	// colors of the attacker and defender in a conflict.
	Color colorA = Color.red;
	Color colorD = Color.green;
	
	// they probably changed.
	boardPanel.removeHighlights();
	
	// lastly, highlight the last action space if appropriate
	if ( highlightLastAction )
	    {
		Space last = _game.getLastSpace();
		if ( last != null )
		    {
			Space sp = _game.getBoard().getSpace( last );
			boardPanel.highlightSpaces( new Space [] { sp }, 
						    Color.yellow );
		    }
		else
		    {
			// how can we show swap tiles?
		    }
	    }	

	// highlight regions based upon game state
	if ( _game.getState() == TigrisGame.WAIT_TREASURE )
	    {
		// TODO.. highlight the treasures.
	    }
	// if supporters, highlight the regions in conflict
	else if ( _game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT || 
		  _game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT )
	    {
		// WAIT_SUPPORTERS ensures that there is a conflict

		// declare and get some variables needed for painting below.
		Conflict con = _game.getConflict();
		Sphere sp = con.getLeaderSphere();
		Space leadA = _game.getBoard().
		    getSpace(  con.getAttacker() );
		Space leadD = _game.getBoard().
		    getSpace( con.getDefender() );
		Kingdom kingA = _game.getBoard().getKingdom( leadA );

		if ( con.isInternal() )
		    {
//  			boardPanel.highlightSpaces( displayGame.getBoard().
//  						     getNumAdjacentTiles( leadA, Sphere.RED ),
//  						     colorA );
//  			boardPanel.highlightSpaces( displayGame.getBoard().
//  						     getNumAdjacentTiles( leadD, Sphere.RED ),
//  						     colorD );
			// WISHLIST... highlight spaces that are in
			// BOTH lists with another color, that signifies
			// that both are highlighted
		    }
		else // external
		    {
			Kingdom kingD = _game.getBoard().getKingdom( leadD );
			// gets painted over right now.
			boardPanel.highlightSpaces( kingA.getTileSpaces( sp ),
						     colorA );
			boardPanel.highlightSpaces( kingD.getTileSpaces( sp ),
						     colorD );
		    }
		boardPanel.highlightSpaces( new Space[] { leadA },
					     colorA );
		boardPanel.highlightSpaces( new Space[] { leadD },
					     colorD );
	    }
	else if ( _game.getState() == TigrisGame.WAIT_CONFLICT_ORDER )
	    {
		// TODO
		// lightlight the leaders.
	    }
	else if ( _game.getState() == TigrisGame.WAIT_MONUMENT )
	    {
		// highlight the respective upper lefts?
		// seems like a lot of work.
	    }
	else if ( _game.getState() == TigrisGame.GAME_OVER )
	    {
		// what to do here?
		// highlight the winner's leaders?
	    }
	else
	    {
		// aww.. nevermind. don't highlight anything.
	    }
	boardPanel.repaint();
    }

    // override the layout function so the 
    // boardPanel can repaint correctly with double-buffering
    // after things get resized.
    // validate is not called each time.
    // validateTree will call doLayout for each component
//      public void doLayout()
//      {
//  	super.doLayout();	


//  	if ( rCount == 2 ) {
//  	    rCount = 0;
//  	    return;
//  	}
//  	else {
//  	    rCount ++;
//  	}


//  	int rCount = 0;

//  	// layout and then set the tile sizes.
//  	playerScreen.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
//  	conflictPanel.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
//  	playerLabel.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );

//  	// again!
//  	getParent().validate();

//        	boardPanel.repaint();
//  	playerScreen.repaint();
//  	conflictPanel.repaint();
//      }

//      public void validate()
//      {
//    	super.validate();
//      }

//      public void setBounds( int x, int y, int w, int h )
//      {
//  	super.setBounds( x, y, w, h );
//      }

    // a class that lays-out the container multiple times
    // adjusting the tileSize so that everything fits nicely.
    // this layout manager MUST be for the container that contains the boardPanel
    class IterativeLayout extends BorderLayout
    {
	int ITERATIONS = 3;
	//LayoutManager layout;

	int tileSize = UIScale.scale( 25 );

	public IterativeLayout()
	{
	    //this.layout = layout;
	}

	public void layoutContainer( Container parent )
	{
	    for ( int ii=0; ii< ITERATIONS; ii ++ ) {
		// layout and then set the tile sizes.
		super.layoutContainer( parent );
		playerScreen.setTileSize( tileSize, tileSize );
		conflictPanel.setTileSize( tileSize, tileSize );
		//playerLabel.setTileSize( 25, 25 ); 
		actionSelectionPanel.setTileSize( tileSize, tileSize );

// 		playerScreen.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
// 		conflictPanel.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
// 		playerLabel.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
// 		actionSelectionPanel.setTileSize( boardPanel.colWidth, boardPanel.rowHeight );
	    }
	    
	}

//  	public void addLayoutComponent( String name, Component comp )
//  	{
//  	    layout.addLayoutComponent( name, comp );
//  	}

//  	public Dimension minimumLayoutSize( Container parent )
//  	{
//  	    return layout.minimumLayoutSize( parent );
//  	} 

//  	public Dimension preferredLayoutSize( Container parent )
//  	{
//  	    return layout.preferredLayoutSize( parent );
//  	}

//  	public void removeLayoutComponent( Component comp )
//  	{
//  	    layout.removeLayoutComponent( comp );
//  	}
    }

    // ************************
    // GUI LISTENERS
    // ************************

    private class PlayerListener implements ActionListener
    {
	// history buttons updated in setDisplayGame
	public void actionPerformed( ActionEvent ae )
	{
	    if ( ae.getSource() instanceof Player ) {
		Player player = (Player) ae.getSource();
		setPlayerView( player );
	    }
	}
    }

//     private class SaveListener implements  ActionListener
//     {
// 	public void actionPerformed( ActionEvent ae ) {
// 	    if ( _game != null )
// 		ResourceLoader.fileSaveObject( _game, "SAVE_GAME" );
// 	}
//     }

    // handles checkboxes for toggeling kingdoms, gridlines, 
    // clicking on players,
    // and other gui things.
    public void itemStateChanged( ItemEvent event )
    {
	if ( event.getSource() == playerList )
	    {
		//it appears to return an integer
	
		int index =0;
		// assume it is the selection index of playerList if it is 
		// of type Integer
		if ( event.getItem() instanceof Integer &&
		     (index = ((Integer)event.getItem()).intValue()) >= 3 )
		    {
			setPlayerView( _game.getPlayer(index - 3) );
		    }
	    }
// 	else if ( event.getSource() == gridCheckbox )
// 	    {
// 		boardPanel.setDrawGrid( gridCheckbox.getState() );
// 	    }
// 	else if ( event.getSource() == kingdomCheckbox )
// 	    {
// 		boardPanel.setOutlineKingdoms( kingdomCheckbox.getState() );
// 	    }
// 	else if ( event.getSource() == lastActionCheckbox )
// 	    {
// 		// cm?? when highlighted?
// 		highlightRelevant();
// 	    }


    	/*
	Toolkit tk = Toolkit.getDefaultToolkit();
	String [] fonts = tk.getFontList();
	for (int ii=0; ii<fonts.length; ii++)
	    {
		System.out.println( fonts[ii] );
	    }

	    there is also tk.getImage( String filename!!!! )
	*/
    }
    
    // ***************************
    // LOGIC FOR MAKING MOVES 
    // ***************************

    private void setDefaultSelection()
    {
	// ... general comments about user interface, may not be relevant now ...
	// logic for resetting what you have selected after each move.
	// dad said he prefers not having this reset when you or 
	// someone else moves...SO we leave it alone IF you have a tile selected
	// leaders will unselect (don't want to place twice by mistake)
	// and tilebags are just annoying.
	
//  	if ( _game.getState() == Game.WAIT_SUPPORTERS )
//  	    _selectedObject = new TileBag();
//  	else if ( ! (_selectedObject instanceof Tile) )
//  	    _selectedObject = null;

	switch ( _game.getState() ) 
	    {
	    case TigrisGame.WAIT_ACTION:
	    case TigrisGame.WAIT_TREASURE:
	    case TigrisGame.WAIT_CONFLICT_ORDER:
	    case TigrisGame.GAME_OVER:
		_selectedObject = null;
		break;
	    case TigrisGame.WAIT_ATTACK_SUPPORT:
	    case TigrisGame.WAIT_DEFEND_SUPPORT:
		_selectedObject = new TileBag();
		break;
	    case TigrisGame.WAIT_MONUMENT:
		// slightly tricky here.
		// first we need to detect if choosing the UL corner
		// space is redunant, if so make it the selected object,
		// otherwise null.
		Board board = _game.getBoard();
		Space lastTile = _game.getLastSpace();
		Space [] possSpots = 
		    new Space[] { board.getNorthSpace( lastTile ),
				  board.getWestSpace( lastTile ),
				  board.getNorthSpace( board.getWestSpace( lastTile )),
				  lastTile };
		_selectedObject = null;
		for ( int ii=0; ii<possSpots.length; ii++ ) {
		    if ( board.isTileSquare( possSpots[ii] ) )
			if ( _selectedObject == null ) {
			    // this is the first valid space we have found, mark it,
			    _selectedObject = possSpots[ii];
			} else {
			    // we already found one, we are going to have to pick a space.
			    _selectedObject = null;
			    break;
			}
			    
		}
		break;
	    default:
		_selectedObject = null;
		System.out.println("Unknown Game state in " +
				   "GamePanel.setDefaultSelection");
	    }
    }

    public void illegalMove( IllegalMoveException ime )
    {
	_selectedObject = ime;
	updateSelection();
    }

    public void actionPerformed( ActionEvent ae )
    {
	// uses gameState and _selectedObject 
	int gameState = _game.getState();
	int dynasty = _game.getMoveDynasty();
	Move move = null;

	// return if we are not enabled to make moves.
	if ( ! _moveEnabled )
	    return;

	// we are clicking on some game move action
	// but we are not focused on the current player or current move.
  	if ( playerScreen.player == null ||
  	     playerScreen.player.getDynasty() != dynasty )
	    return;

	switch ( gameState ) {

	case TigrisGame.WAIT_ACTION:
	    // player may choose
	    // 1. tile, then space
	    // 2. leader, then space
	    // 3. cat, then space,
	    // 4. tiles for swapping (order of selection?)
	    
	    if ( ae.getSource() instanceof Tile ) {

		// they have already selected the tile bag to swap.
		// now add tiles to it.
		Tile tile = (Tile) ae.getSource();
		if ( _selectedObject instanceof TileBag )
		    {
			TileBag prevSelect = (TileBag) _selectedObject;
			TileHolder availSelect = _game.getPlayer( dynasty );
			
			// check to make sure they have it available.
			if ( availSelect.getNumTiles( tile ) >
			     prevSelect.getNumTiles( tile ) )
			    prevSelect.putTile( tile );
		    }
		else
		    {
			_selectedObject = tile;
		    }
	    }		
	    else if ( ae.getSource() instanceof Leader ||
		      ae.getSource() instanceof Catastrophe )
		{
		    _selectedObject = ae.getSource();
		}
	    else if ( ae.getSource() instanceof Space )
		{
		    Space aeSpace = (Space) ae.getSource();
		    
		    if ( aeSpace.getLeader() != null &&
			 aeSpace.getLeader().getDynasty() == 
			 dynasty )
			{
			    _selectedObject = aeSpace.getLeader();
			}
		    else if ( _selectedObject instanceof Tile )
			{
			    Tile tt = (Tile) _selectedObject;
			    
			    // a regular tile
			    move = Move.
				newPlaceTile( dynasty, aeSpace, tt );
			}
		    else if ( _selectedObject instanceof Catastrophe )
			{
			    // a catastrophe tile.
			    move = Move.
				newPlayCatastrophe( dynasty, aeSpace );
			}
		    else if ( _selectedObject instanceof Leader )
			{
			    move = Move.newPositionLeader
				( dynasty, aeSpace,
				  ((Leader) _selectedObject).getSphere() );
			}
		    else
			{
			    // Invalid selection for WAIT_ACTION clicking on a Space 
			}
		}
	    else if ( ae.getSource() instanceof Player ) 
		{
		    // this is the special case for the "withdraw leader to my hand" move
		    if ( _selectedObject instanceof Leader )
			{
			    move = Move.
				newPositionLeader
				( dynasty, null,
				  ((Leader) _selectedObject).getSphere() );
			}
		    else 
			{
			    // ignore clicks on the PlayerScreen leader region otherwise.
			}
		}
	    else if ( ae.getSource() == tileBagButton )
		{
		    _selectedObject = new TileBag();
		}
	    else if ( ae.getSource() == okayButton )
		{
		    // okay is only needed for tile swap
		    if ( _selectedObject instanceof TileBag )
			{
			    move = Move.
				newSwapTiles( dynasty, (TileBag) _selectedObject );
			    _selectedObject = null; // cancel the tile bag partial selection
			}
		}
	    else if ( ae.getSource() == cancelButton )
		{
		    if ( _game.getState() != TigrisGame.WAIT_ATTACK_SUPPORT ||
			 _game.getState() != TigrisGame.WAIT_DEFEND_SUPPORT )
			_selectedObject = null;
		    else
			_selectedObject = new TileBag();
		}
	    else
		{
		    // Invalid selection for WAIT_ACTION
		}
	    break;
	case TigrisGame.WAIT_TREASURE:
	    // player must choose a space with a treasure OR CANCEL (not strictly in rules)
	    if ( ae.getSource() instanceof Space )
		{
		    Space aeSpace = (Space) ae.getSource();
		    move = Move.newChooseTreasure( dynasty, aeSpace );
		}
	    else
		{
		    // Invalid selection for WAIT_TREASURER
		}
	    break;
	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    // player must choose N tiles of the appropriate color.
	    if ( ae.getSource() instanceof Tile )
		{
		    Tile tile = (Tile) ae.getSource();
		    TileBag selectedTiles = (TileBag) _selectedObject;
		    Conflict conflict = _game.getConflict();
		    // check if the additional tile will be legal.
		    if ( tile.isSphereEqual( conflict.getSupportSphere() ) &&
			 _game.getPlayer( dynasty ).getNumTiles( tile ) >=
			 selectedTiles.getNumTiles( tile ) + 1 )
			selectedTiles.putTile( tile );
		}
	    else if ( ae.getSource() == okayButton &&
		      _selectedObject instanceof TileBag )
		{
		    // make action
		    move = Move.newChooseSupporters( dynasty, 
						     (TileBag) _selectedObject );
		}
	    else if ( ae.getSource() == cancelButton || 
		      ae.getSource() == tileBagButton )
		{
		    // reset tile bag			
		    _selectedObject = new TileBag();
		}
	    else
		{
		    // Invalid selection for WAIT_SUPPORTERS
		}
	    break;
	case TigrisGame.WAIT_CONFLICT_ORDER:
	    // player must choose a sphere. (but picks by choosing a leader)
	    // leader can be choosen from the board or the player screen
	    // the object will either be Space or Leader.
	    Leader pickedLeader = null;
	    if ( ae.getSource() instanceof Space )
		{
		    Space aeSpace = (Space) ae.getSource();
		    if ( aeSpace.getLeader() != null )
			{
			    pickedLeader = aeSpace.getLeader();
			}
		}
	    else if ( ae.getSource() instanceof Leader )
		{
		    pickedLeader = (Leader) ae.getSource();
		}
	    
	    // if they picked a leader it is valid, otherwise invalid
	    if ( pickedLeader != null )
		{
		    move = Move.newChooseSphere( dynasty, 
						 pickedLeader.getSphere() );
		}
	    else
		{
		    // Invalid selection for WAIT_CONFLICT_ORDER
		}
	    break;
	case TigrisGame.WAIT_MONUMENT:
	    // to build a monument.
	    // 1. choose the UL corner space
	    // 2. choose the secondary sphere by clicking on an appropriate tile.
	    // cancel declines to build a monument
	    if ( ae.getSource() instanceof Space )
		{
		    _selectedObject = ae.getSource();
		}
	    else if ( ae.getSource() == cancelButton )
		{
		    move = Move.newBuildMonument( dynasty,
						  null, null, null );
		}
	    else if ( ae.getSource() instanceof Tile &&
		      _selectedObject instanceof Space )
		{
		    Sphere firstSphere = _game.getLastSpace().getTile();
		    move = Move.newBuildMonument( dynasty, 
						  ((Space) _selectedObject),
						  (Tile) ae.getSource(),
						  firstSphere );
		}
	    else
		{
		    // Invalid selection for WAIT_MONUMENT
		}
	    
	    break;
	case TigrisGame.GAME_OVER:
	    // player can only choose new game (might not be on this screen)
	    break;
	default:
	    System.out.println("Unknown Game state in " +
			       "GamePanel.processPlayerAction");
	}
	
	if ( move != null )
	    {
		if ( verbosity > 0 )
		    System.out.println("made move " + move );

		Enumeration ee = _actionListeners.elements();
		ActionEvent moveEvent = new ActionEvent( move,
							 ActionEvent.ACTION_PERFORMED,
							 "Tigris Command" );
		
		while ( ee.hasMoreElements() )
		    {
			ActionListener al = (ActionListener) ee.nextElement();
			al.actionPerformed( moveEvent );
		    }
	    }

	// update the selection, the have probably changed it.
	updateSelection();    
    }

    // this is actually a very good main for testing.
    // pass a filename on the command line and it loads the 
    // GamePanel in a frame with the saved position.
    // so after a fileSaveGame on a crash, you can view the trouble position
    public static void main( String args [] ) throws Exception 
    {
	TigrisGame game;

	if ( args.length > 0 ) {
	    game = null; //GameServerImpl.fileLoadGame( args[0] );
	} else {
	    game = new StandardGame( new GameOptions() );
	}

	Frame frame = new Frame("GamePanel");
	GamePanel gp = new GamePanel();
	gp.setGameView( game );
	frame.add( gp );
	frame.validate();
	frame.setSize( 625,550 );
	frame.addWindowListener( new WindowAdapter()
	    { public void windowClosing( WindowEvent e ) 
		{ System.exit(1);}
	    });
	frame.setVisible( true );
    }
}
