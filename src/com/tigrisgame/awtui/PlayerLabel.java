/* File: PlayerLabel.java
 * Description:  awt Label that also draws the a leader
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/*
 * TODO:. be able to draw ?'s and other things for incomplete players.
 */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.Leader;
import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.Sphere;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

public class PlayerLabel extends Label
{
    private Font labelFont =
	UIScale.createFont( "Monospaced", Font.PLAIN, 16 );

    int tileWidth, tileHeight;

    // here's an idea. the type of leader draw 
    // represents what state the game is in!
    Player player = null;
    Sphere sphere = new Sphere();
    Vector actionListeners = new Vector();

    public PlayerLabel()
    {
	//setFont( labelFont );
	//setForeground( Color.white );
	//setBackground( Color.black );

	enableEvents( AWTEvent.MOUSE_EVENT_MASK );
    }

    public void setPlayer( Player player )
    {
	this.player = player;

	if ( player == null )
	    {
		setText( "" );
	    }
	else
	    {
		String spacePad = "                    "; // 20 spaces
		String playerName = (player.getName()+spacePad).substring(0,15);
		
		// i'm pretty sure this repaints
		setText( playerName );
	    }
	repaint();
    }

    public synchronized void addActionListener( ActionListener al )
    {
	if ( ! actionListeners.contains( al ) )
	    actionListeners.addElement( al );
    }
    
    public synchronized void removeActionListener( ActionListener al )
    {
	actionListeners.removeElement( al );
    }

    public void processMouseEvent(MouseEvent me)
    {
	if ( me.getID() == MouseEvent.MOUSE_PRESSED ) {

	    Object source = (player != null) ? player : new Object();

	    ActionEvent ae = new ActionEvent( source,
					      ActionEvent.ACTION_PERFORMED,
					      "Tigris Command",
					      me.getModifiers() );
	    
	    Enumeration ee = actionListeners.elements();
	    while ( ee.hasMoreElements() )
		{
		    ActionListener al = (ActionListener) ee.nextElement();
		    al.actionPerformed( ae );
		}
	}
    }

    public void setTileSize( int width, int height )
    {
	tileWidth = width;
	tileHeight = height;
	// keep the font proportional to the tile/label size.
	labelFont = UIScale.createFont( "Monospaced", Font.PLAIN,
					Math.max( 10, Math.min( 20,
								tileHeight / 2 )));
	setFont( labelFont );
	invalidate();
	repaint();
    }

    public Player getPlayer()
    {
	return player;
    }

    public void setSphere( Sphere sphere )
    {
	if ( sphere != null )
	    {
		this.sphere = sphere;
		repaint();
	    }
    }

    public Dimension getMinimumSize()
    {
	Dimension labelMin = super.getMinimumSize();
	return new Dimension( labelMin.width + tileWidth,
			      Math.max( labelMin.height, tileHeight ));
    }
    
    public Dimension getPreferredSize() 
    {
	return getMinimumSize();
    }
    
    public void paint( Graphics g )
    {
	super.paint( g );

	// draw the leader as a square centered on the upper right.
	// use drawLeaderIcon with a roster color rather than the sphere's
	// own colors: the shade marks state -- neutral gray waiting,
	// teal for the seat whose move it is, magenta while a decision
	// (e.g. conflict support) is pending for that seat.
	Color bgColor = TigrisGraphics.rosterIdleColor;
	if ( sphere.equals( Sphere.BLUE ) )
	    bgColor = TigrisGraphics.rosterDecisionColor;
	else if ( sphere.equals( Sphere.GREEN ) )
	    bgColor = TigrisGraphics.rosterActiveColor;
	int sideSize = Math.min( getSize().width, getSize().height );
	if ( player != null )
	    TigrisGraphics.drawLeaderIcon( getSize().width-tileWidth, 
					   (getSize().height - tileHeight)/2,
					   tileWidth, tileHeight,
					   new Leader( sphere, player.getDynasty() ),
					   bgColor,
					   g );
	else
	    TigrisGraphics.drawLeaderIcon( getSize().width-tileWidth, 0,
					   tileWidth, tileHeight,
					   new Leader( sphere, Player.INVALID ),
					   bgColor,
					   g );
    }
    
    public void update( Graphics g )
    {	
	// this should call paint.
	super.update( g );
	//paint( g );
    }
}
