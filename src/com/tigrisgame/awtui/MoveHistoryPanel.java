/* File: MoveHistoryPanel.java
 * Description:  Shows move list and creates GameEvents on clicks.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

public class MoveHistoryPanel extends Panel implements GameObserver
{
    MoveHistory moveHistory = new MoveHistory();
    Vector actionListeners = new Vector();

    List moveList = new List( 50 );
    Button moveHistoryButtons [] = new Button [] 
	{ new Button("<<"), new Button("<"), 
	  new Button(">"), new Button(">>") };

    MoveHistoryPanel()
    {
	Panel buttonPanel = new Panel();
	buttonPanel.setLayout( new GridLayout( 1, 0 ));
	for ( int ii=0; ii<moveHistoryButtons.length; ii++ ) {
	    buttonPanel.add( moveHistoryButtons[ii] );
	}

	setLayout( new BorderLayout() );
	add(  BorderLayout.NORTH, moveList );
	add(  BorderLayout.SOUTH, buttonPanel );
    }


    public void gameStart( TigrisGame g )
    {
	moveHistory.gameStart( g );
	moveList.removeAll();
    }

    public void moveMade( Move move )
    {
	moveHistory.moveMade( move );
	moveList.add( "" + move );
    }

    public void showScreen( int dynasty, TileHolder tiles )
    {
	moveHistory.showScreen( dynasty, tiles );
    }

   public void showClock( int dynasty, long clock ) {}

    public void gameEnd( int [] dyns)
    {
	moveHistory.gameEnd( dyns );
    }

    public boolean requestUndoMoves( int number ) { return false; }
    public void yourMove() {}
    public String getName() { return "MoveHistoryPanel"; }


    public synchronized void addActionListener( ActionListener al )
    {
	if ( ! actionListeners.contains( al ) )
	    actionListeners.addElement( al );
    }
    
    public synchronized void removeActionListener( ActionListener al )
    {
	actionListeners.removeElement( al );
    }

    /// handle events.
    void foo( MouseEvent me ) 
    {
	Enumeration ee = actionListeners.elements();
	ActionEvent ae = new ActionEvent( new Object(),
					  ActionEvent.ACTION_PERFORMED,
					  "Tigris Command",
					  me.getModifiers() );
	while ( ee.hasMoreElements() )
	    {
		ActionListener al = (ActionListener) ee.nextElement();
		al.actionPerformed( ae );
	    }
    }
	
    public static void main( String [] args )
    {
	MoveHistoryPanel panel = new MoveHistoryPanel();

	panel.gameStart( new StandardGame( new GameOptions() ) );
	for ( int ii=0; ii<50; ii++ ) {
	    panel.moveMade( Move.newSwapTiles( 0, new TileBag()));
	}

	Frame frame = new Frame( "Move History" );
	frame.add( panel );
	frame.pack();
	frame.addWindowListener( new WindowAdapter() 
	    { public void windowClosing( WindowEvent e ) 
		{ System.exit(0); }});
	frame.setVisible( true );
    }
}
