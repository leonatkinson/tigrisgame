/* File: SelectionPanel.java
 * Description:  Panel showing an icon of the current selection.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;

public class SelectionPanel extends Panel
{
    private Label _topText = new Label();
    // running game log; everything (moves and instructions) is
    // appended to the bottom, and the view auto-scrolls to follow.
    private TextArea _bottomText = new TextArea
	( "", 1, 1, TextArea.SCROLLBARS_VERTICAL_ONLY );
    // instructions are re-set on every screen update; only log them
    // when the prompt text actually changes, so repeated resets
    // (even interleaved with move entries) do not spam the log.
    private String _lastInstruction = "";
    private TigrisIcon _selectedIcon = new TigrisIcon();
    private Object _selectedObject;
    private int _tileWidth, _tileHeight;

    public SelectionPanel()
    {
	super( new BorderLayout() );
	Panel northPanel = new Panel( new BorderLayout() );
	Font uiFont = UIScale.createFont( "Dialog", Font.PLAIN, 12 );
	_topText.setFont( uiFont );
	_bottomText.setFont( uiFont );
	_bottomText.setEditable( false );
	northPanel.add( BorderLayout.NORTH, _topText );
	northPanel.add( BorderLayout.CENTER, _selectedIcon );
	add( BorderLayout.NORTH, northPanel );
	add( BorderLayout.CENTER, _bottomText );
    }

    public void setSelectedObject( Object obj )
    {
	_selectedIcon.setSelectedObject( obj );
	_selectedObject = obj;
	updateBottomText();
    }

    public void setTopText( String topText )
    {
	_topText.setText( topText );
    }

    /** show an instruction prompt: appended to the log only when the
     * prompt changes (these are re-set constantly by screen updates).
     */
    public void setBottomText( String bottomText )
    {
	if ( bottomText == null || bottomText.equals( _lastInstruction ) )
	    return;
	_lastInstruction = bottomText;
	logToBottom( bottomText );
    }

    /** always log a message (game moves etc.) to the bottom of the
     * log.
     */
    public void logMessage( String msg )
    {
	if ( msg == null )
	    return;
	logToBottom( msg );
    }

    private void logToBottom( String msg )
    {
	_bottomText.append( msg );
	_bottomText.append( "\n\n" );
	// keep the newest (bottom) entries visible.
	_bottomText.setCaretPosition( _bottomText.getText().length() );
    }

    /** start a fresh log (called at the start of each game). */
    public void clearBottomText()
    {
	_lastInstruction = "";
	_bottomText.setText( "" );
	_bottomText.setCaretPosition( 0 );
    }

    public void setTileSize( int width, int height )
    {
	_tileWidth = width;
	_tileHeight = height;
	_selectedIcon.setTileSize( width, height );
    }

    public void updateBottomText()
    {
	if ( _selectedObject == null ) {
	    setBottomText("click on a leader, tile, catastrophe, " +
			  "or click the tile bag to swap" );	    
	} else if ( _selectedObject instanceof Catastrophe ) {
	    setBottomText("catastrophe selected, click on the board to place it");
	} else if ( _selectedObject instanceof Tile ) {
	    Tile tile = (Tile) _selectedObject;
	    setBottomText( tile.toString().toLowerCase() + 
			   " tile selected, click on the board to place it");
	} else if ( _selectedObject instanceof Leader ) {
	    Leader lead = (Leader) _selectedObject;
	    setBottomText( (new Sphere( lead.getSphere() )
			    .toString().toLowerCase()) + 
			   " tile selected, click on the board to place it");
	} else if ( _selectedObject instanceof TileBag ) {
	    TileBag tiles = (TileBag) _selectedObject;
	    StringBuffer strbuf = new StringBuffer("swapping tiles \n{");
	    Sphere [] spheres = Sphere.getSpheres();
	    boolean first = true;
	    for ( int ss=0; ss<spheres.length; ss++) {
		int tnum = tiles.getNumTiles( spheres[ss] );
		if ( tnum > 0 ) {
		    if ( ! first ) {
			strbuf.append(",");
		    }
		    strbuf.append( " " + tnum + " " + 
				   spheres[ss].toString().toLowerCase() );
		    first = false;
		}
	    }
	    if ( tiles.getNumTiles() == 0 )
		strbuf.append(" None ");

	    strbuf.append("}");
	    strbuf.append(" \nclick more tiles, ok, tilebag, or cancel");
	    setBottomText( strbuf.toString() );
	} 
    }
//      public Dimension getMinimumSize()
//      {
//  	Dimension minSize = new Dimension(3,3);
//  	return minSize;
//      }
    
//      public Dimension getPreferredSize()
//      {
//  	return getMinimumSize();
//      }
}

