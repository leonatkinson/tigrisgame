/* File: AIDisplayPanel.java
 * Description:  Display for AI search
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.awtui.PlayerLabel;
import com.tigrisgame.rules.Move;
import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.Sphere;

import java.awt.*;

public class AIDisplayPanel extends Panel
{
    PlayerLabel playerLabel = new PlayerLabel();
    TextField nameField = new TextField( 20 );
    TextField thinkingStatsField = new TextField( 20 );
    TextArea bestDecisionField = new TextArea( "best decision", 2, 20, 
					       TextArea.SCROLLBARS_NONE );
    AIPlayer ai;
    
    public AIDisplayPanel()
    {
	this( null, false );
    }

    public AIDisplayPanel( AIPlayer ai )
    {
	this( ai, false );
    }

    public AIDisplayPanel( AIPlayer ai, boolean startFrame )
    {
	setAIPlayer( ai );

	setLayout( new BorderLayout( 0, 1 ) );
	
	playerLabel.setSphere( new Sphere( Sphere.RED ) );
	nameField.setEditable( false );
	thinkingStatsField.setEditable( false );
	bestDecisionField.setEditable( false );	

	add( "North", playerLabel );
	Panel p1 = new Panel();
	p1.setLayout( new GridLayout( 0,1 ));
	p1.add( "North", nameField );
	p1.add( "North", thinkingStatsField );
	p1.add( "North", bestDecisionField );

	Panel p2 = new Panel();
	p2.setLayout( new BorderLayout() );
	p2.add( "North", p1 );
	add( p2, "Center" );

	Thread updateThread = new UpdateThread();
	updateThread.start();

	if ( startFrame ) {
	    Frame frame = new Frame("AIDisplayPanel");
	    frame.add( this );
	    frame.validate();
	    frame.setSize( 200,200 );
	    frame.setVisible( true );
	}
    }

    public void setAIPlayer( AIPlayer ai )
    {
	this.ai = ai;
	if( ai != null )
	    nameField.setText( ai.getName() );
    }

    public void setPlayer( Player p )
    {
	playerLabel.setPlayer( p );
    }
    
    // total decisions made int the evaulation tree
    // (equivalent to number of Game.copy calls)
//      public void setDecisionTreeSize( int size )
//      {
//  	decisionTreeSize = size;
//      }

    public void setBestMove( Move move )
    {
	bestDecisionField.setText( "" + move );
    }
    
    // top level decisions.
//      public void setLegalMoves( int moves )
//      {
//  	legalMoves = moves;
//  	decisionStatsField.setText( movesEvaluated + " / " + legalMoves
//  				    + " : " + decisionTreeSize );
//      }


//      // top level decisions considered fully.
//      public void setMovesEvaluated( int moves )
//      {
//  	movesEvaluated = moves;
//  	decisionStatsField.setText( movesEvaluated + " / " + legalMoves + " : " + decisionTreeSize );
//      }

    class UpdateThread extends Thread
    {
	public void run()
	{
	    while ( true ) {
		if ( ai.isThinking() )
		    thinkingStatsField.setText( "thinking : " 
						+ ai.getPercentDone() + "% done, " 
						//+"size = " + ai.getSearchTreeSize()
						);
		else 
		    thinkingStatsField.setText( "not thinking" );
		
		try {
		    Thread.currentThread().sleep( 1000 );
		} catch (InterruptedException ex) {}
	    }
	}
    }
}



/*
    // temp until we get a better API for AI/GUI callbacks
    public class GUI extends Frame
    {
	TextField bestMoveField = new TextField();
	TextField pctDoneField = new TextField();

	public GUI()
	{
	    super("FastAI thinking frame");
	    setLayout( new BorderLayout() );
	    add( "North", new Label("hi"));
	    add( "Center", pctDoneField );
	    validate();
	    setSize( 200,200 );
	    setVisible( true );

	    Thread thread = new Thread( new UpdateThread() );
	    thread.start();
	}


    }
*/
