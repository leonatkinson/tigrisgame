/* File: BoardPanel.java
 * Description:  Resizable panel that holds the board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ArrayUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class BoardPanel extends Panel
{
    // game dimenions -- constant for a particular board.
    // should be final if one BoardPanel can only draw one game,
    // but it isn't so the board can change. (or zoom!)
    int ROWS;
    int COLS;

    //Space [][] spaces = new Object[ROWS][COLS];
    Board board;
    
    Image bufferImage;
    Graphics bufferGraphics; // graphics associated with bufferImage
    Dimension bufferDim;     // dimension associated with bufferImage
    int colWidth;
    int rowHeight;
    Dimension boardOrigin = null;//new int[]{20,20};
    
    boolean drawGrid = true;
   public boolean highlightKingdoms = false;
    Point joinCoords;

    Vector actionListeners = new Vector();

    private Font gridFont =
	UIScale.createFont( "Monospaced", Font.PLAIN, 12 );

    // graphical memory
    Hashtable highlightRegions = new Hashtable();
    Hashtable highlightSpaces = new Hashtable();
    
    public BoardPanel()
    {
	enableEvents( AWTEvent.MOUSE_EVENT_MASK );

	setFont( gridFont );

	// note: getSize() returns null until constructor done
	// this could effect the first rendering.

	/// DEBUG!!
	// setJoinMarker( new Point( 10,5 ));
    }

    void setBoard( Board board )
    {
	this.board = board;
	ROWS = board.getNumRows();
	COLS = board.getNumCols();

	// gotta refresh the screen!
	repaint();
    }

    void setJoinMarker( Point joinCoords )
    {
	this.joinCoords = joinCoords;
    }
    
    void setDrawGrid( boolean value )
    {
	drawGrid = value;

	// it's conceivable depending on the implemetation of AWT threads
	// that the event thread and repaint thread could mess stuff up with
	// this nulling.
	bufferImage = null;

	repaint();
    }

    /* 
      // The internal representation doesn't seem to call
      // any of the following routines.
    public void setSize( Dimension d )
    public void setSize( int width, int height )
    public void setBounds( Rectangle r )
    */

    public void highlightSpaces( Space [] spaces, Color c)
    {
	highlightSpaces.put( spaces, c );
	repaint();
    }

    public void highlightRegion( Space [] spaces,  Color c)
    {
	highlightRegions.put( spaces, c );
	repaint();
    }

    public void removeHighlights()
    {
	highlightSpaces = new Hashtable();
	highlightRegions = new Hashtable();
	repaint();
    }

    public void setOutlineKingdoms( boolean value )
    {
	highlightKingdoms = value;
	repaint();
    }


    // highlight the given list of spaces.
    public void drawHighlightSpaces( Space [] spaces, Color c ,Graphics g)
    {
	g.setColor( c );
	for ( int ii=0; ii<spaces.length;ii++)
	    {
		if ( spaces[ii] == null ) {
		    System.out.println("Warning: drawHighlightSpaces passed null space " + ii );
		    continue;
		}
		//int pos [] = board.getRowCol( spaces[ii] );

		int row = spaces[ii].getRow();
		int col = spaces[ii].getCol();
		
		// draw a two pixel thick ring around the squares.
		g.drawRect( boardOrigin.width + colWidth * col,
			    boardOrigin.height + rowHeight * row,
			    colWidth-1, rowHeight-1 );
		g.drawRect( boardOrigin.width + colWidth * col +1,
			    boardOrigin.height + rowHeight * row+1,
			    colWidth-3, rowHeight-3 );
		g.drawRect( boardOrigin.width + colWidth * col +2,
			    boardOrigin.height + rowHeight * row+2,
			    colWidth-5, rowHeight-5 );
	    }
    }

    public void drawHighlightRegion( Space [] spaces, Color c, Graphics g )
    {
	//Graphics g = bufferGraphics;
	g.setColor( c );
	for ( int ii=0; ii<spaces.length;ii++)
	    {
		//int pos [] = board.getRowCol( spaces[ii] );
		
		int row = spaces[ii].getRow();
		int col = spaces[ii].getCol();
		
		// draw the line above the space if northern
		// neighbor is not amoung the spaces.
		if ( ! ArrayUtils.contains( board.getNorthSpace(spaces[ii]), spaces ) )
		    {  
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * row );
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * row+1,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * row +1);
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * row+2,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * row +2);
		    }
		if ( ! ArrayUtils.contains( board.getEastSpace(spaces[ii]), spaces ) )
		    {
			g.drawLine( boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			g.drawLine( boardOrigin.width + colWidth * (col+1) -2,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * (col+1) -2,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			g.drawLine( boardOrigin.width + colWidth * (col+1) -3,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * (col+1) -3,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			
			/// other 2 lines for thickness.
		    }
		if ( ! ArrayUtils.contains( board.getSouthSpace(spaces[ii]), spaces ) )
		    {
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * (row+1)-1,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * (row+1)-2,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * (row+1)-2 );
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * (row+1)-3,
				    boardOrigin.width + colWidth * (col+1) -1,
				    boardOrigin.height + rowHeight * (row+1)-3 );
			/// other 2 lines for thickness.
		    }
		if ( ! ArrayUtils.contains( board.getWestSpace(spaces[ii]), spaces ) )
		    {
			g.drawLine( boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * col,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			g.drawLine( boardOrigin.width + colWidth * col+1,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * col+1,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			g.drawLine( boardOrigin.width + colWidth * col+2,
				    boardOrigin.height + rowHeight * row,
				    boardOrigin.width + colWidth * col+2,
				    boardOrigin.height + rowHeight * (row+1)-1 );
			/// other 2 lines for thickness.
		    }
	    }
    }
    
    // simply draws the buffered image,
    // if no image draws a gray square
    // implementations for "paint" seem inconsistant.
    // win95 appletviewer called paint as a window was resizing and growing,
    // and update when it was done.
    // linux appletviewer calls only paint and only when it is done!
    public void paint(Graphics g) 
    {
	// not buffered image to draw, find out if we can get one.
	if ( bufferImage != null )
	    {
		g.drawImage(bufferImage,0,0,this);
	    }
	else if ( board != null )
	    {
		// we have a board to paint!
		// let's go about making the bufferedImage not null.
		updateBufferSize( getSize().width, getSize().height );
	    }
	else
	    {
		// no hope. no buffer and no game.
		g.setColor( Color.gray );
		g.fillRect( 0,0,getSize().width, getSize().height );
	    }
    }
    
    // creates and resizes the buffered image if necessary. 
    // then calls drawBoard (the board needs redrawing after
    // a game event or a resize, these are the only times update
    // or repaint are called so it's okay to do this
    // every time ). then calls paint (as simmilar to AWT). 
    public void update(Graphics g) 
    { 
	// DEBUG
	// getHeight and getWidth aren't invented in jdk1.1
	updateBufferSize(getSize().width, getSize().height );
	
	// always draw the board to the buffer
	drawBoard();
	
	// then paint it to the screen.
	paint(g);
    }

    public void setBounds( int x, int y, int w, int h )
    {
//    	if ( true )
//    	    throw new RuntimeException("called from where???");

	super.setBounds( x, y, w, h );
	updateBufferSize( w, h );
	repaint();
    }

    public void updateBufferSize( int width, int height )
    {
	// handle the uninitialized board case
	if ( board == null || 
	     width <= 0 ||
	     height <= 0 )
	    {
		bufferImage = null;
		bufferGraphics = null;
		bufferDim = null;
		
		// division upcoming.. ensure that the integer is at least 1.
		colWidth  = 0;
		rowHeight = 0;
    
		boardOrigin = new Dimension(0,0);

		return;
	    }

	// get a new bufferImage and bufferGraphics if necessary
	// bufferDim is temporarily what the buffer SHOULD BE.
	bufferDim = new Dimension( width, height );
	if ( bufferImage == null || 
	     bufferDim.width != bufferImage.getWidth(null) ||
	     bufferDim.height != bufferImage.getHeight(null) )
	    {
		bufferImage = createImage( bufferDim.width, bufferDim.height );
		bufferGraphics = bufferImage.getGraphics();
		
		// used as total width available for the board.
		int padWidth;
		int padHeight;
		
		if ( drawGrid )
		    {
			FontMetrics font = getGraphics().getFontMetrics();

			padWidth = font.stringWidth("00");
			padHeight = font.getHeight();
		    }
		else
		    {
			padWidth = 0;
			padHeight = 0;
		    }
		
		// division upcoming.. ensure that the integer is at least 1.
		// 2 times pad for columns on both sides.
		colWidth  = Math.max( (bufferDim.width - 2*padWidth) / COLS, 1 );
		rowHeight = Math.max( (bufferDim.height - 2*padHeight) / ROWS, 1 );
		
		// center it in the remaining space
		boardOrigin = new Dimension( ( bufferDim.width - colWidth * COLS) / 2,
					     ( bufferDim.height - rowHeight * ROWS) / 2 );
	    }
    }
    
    private void drawBoard()
    {
	if ( bufferGraphics == null )
	    return;

    	bufferGraphics.setColor( Color.black );
	TigrisGraphics tg = new TigrisGraphics( bufferGraphics );

	bufferGraphics.setColor( TigrisGraphics.landColor );

	// fill up everything with dirt color
	bufferGraphics.fillRect( boardOrigin.width,
				 boardOrigin.height,
				 colWidth*COLS,
				 rowHeight*ROWS );

	// Draw Spaces...
	for ( int ii=0; ii< ROWS; ii++)
	    for ( int jj=0; jj< COLS; jj++)
		{
		    Space sp = board.getSpace( ii, jj );
		    Tile tt = sp.getTile();
		    Leader ll = sp.getLeader();
		    Monument mm = sp.getMonument();

		    // draw tile ?
		    if ( tt != null )
			{
			    tg.drawTile( boardOrigin.width + colWidth * jj,
					 boardOrigin.height + rowHeight * ii,
					 colWidth, rowHeight, tt );
			}
		    // tiles and leaders are mut-ex
		    // draw leader ?
		    else if ( ll != null )
			{
			    tg.drawLeader( boardOrigin.width + colWidth * jj,
					   boardOrigin.height + rowHeight * ii,
					   colWidth, rowHeight, ll );
			}
		    // draw catastrophe
		    else if ( sp.hasCatastrophe() )
			{
			    tg.drawCatastrophe( boardOrigin.width + colWidth * jj,
						boardOrigin.height + rowHeight * ii,
						colWidth, rowHeight );
			}
		    // draw monument ?
		    else if ( mm != null )
			{
			    if ( board.getUpperLeftSpace( mm ).isLocationEqual( sp ) )
				tg.drawMonument( boardOrigin.width + colWidth * jj,
						 boardOrigin.height + rowHeight * ii,
						 colWidth *2,
						 rowHeight *2, mm);
			}
		    else
			{
			    // just an ordinary space.
			    if ( sp.isWater() )
				tg.drawWater( boardOrigin.width + colWidth * jj,
					      boardOrigin.height + rowHeight * ii,
					      colWidth, rowHeight );
			    else
				tg.drawLand(  boardOrigin.width + colWidth * jj,
					      boardOrigin.height + rowHeight * ii,
					      colWidth, rowHeight );
			}

		    
		    // tiles or monuments can have treasures..
		    // just draw it anyway on top of anything else
		    if ( sp.hasTreasure() )
			{
			    tg.drawTreasure( boardOrigin.width + colWidth * jj,
					     boardOrigin.height + rowHeight * ii,
					     colWidth, rowHeight );
			}
		}

	// draw handshake if applicable.
	if ( joinCoords != null) {
	    tg.drawJoinMarker( boardOrigin.width + colWidth * joinCoords.y,
			       boardOrigin.height + rowHeight * joinCoords.x,
			       colWidth, rowHeight );
	}

	// draw outlines for entire component and the extend of the grid
	bufferGraphics.setColor( Color.black );
	//bufferGraphics.drawRect( 0,0, bufferDim.width-1, bufferDim.height-1 );

	// outline the whole region
	bufferGraphics.drawRect( 0,0, bufferDim.width-1, bufferDim.height-1);

	if ( drawGrid )
	    {
		FontMetrics font = bufferGraphics.getFontMetrics();

		bufferGraphics.setColor( TigrisGraphics.blackTileColor );
		for ( int cc=0; cc< COLS; cc++)
		    {
			String colStr = " " + (1 + cc);

			tg.drawCenteredString( boardOrigin.width + cc*colWidth,
					       0,
					       colWidth,
					       font.getHeight(),
					       "" + colStr );
			tg.drawCenteredString( boardOrigin.width + cc*colWidth,
					       boardOrigin.height + rowHeight*ROWS,
					       colWidth,
					       font.getHeight(),
					       "" + colStr );

		    }

		for ( int rr=0; rr< ROWS; rr++)
		    {
			String rowStr = "" + new Character((char)('a' + rr));

			tg.drawCenteredString( 0,
					       boardOrigin.height + rr*rowHeight,
					       font.stringWidth("00"),
					       rowHeight,
					       "" + rowStr );
			tg.drawCenteredString( boardOrigin.width + colWidth*COLS,
					       boardOrigin.height + rr*rowHeight,
					       font.stringWidth("00"),
					       rowHeight,
					       "" + rowStr );
		    }
	    }

	if ( highlightKingdoms )
	    // test highlighting of ALL kingdoms.
	    {
		Kingdom kings [] = board.getKingdoms();
		for (int ii=0; ii<kings.length; ii++ )
		    {
			Space [] kingSpaces = kings[ii].getSpaces();


			// number the kingdoms
			tg.m_g.setColor( Color.white );
			for ( int jj=0; jj<kingSpaces.length; jj++ )
			    tg.drawCenteredString( boardOrigin.width + colWidth * kingSpaces[jj].getCol(),
						   boardOrigin.height + rowHeight * kingSpaces[jj].getRow(),
						   colWidth, rowHeight, ""+ii );
			
			// highlight the region
			drawHighlightRegion( kingSpaces,
					     Color.magenta,
					     bufferGraphics );
		    }
	    }


	Enumeration eeSpaces = highlightRegions.keys();
	Enumeration eeColors = highlightRegions.elements();
	while ( eeSpaces.hasMoreElements() )
	    {
		drawHighlightRegion( (Space[]) eeSpaces.nextElement(),
				     (Color) eeColors.nextElement(),
				     bufferGraphics );
	    }

	eeSpaces = highlightSpaces.keys();
	eeColors = highlightSpaces.elements();
	while ( eeSpaces.hasMoreElements() )
	    {
		drawHighlightSpaces( (Space[]) eeSpaces.nextElement(),
				     (Color) eeColors.nextElement(),
				     bufferGraphics );
	    }       
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
	// or MouseEvent.MOUSE_CLICKED if you prefer.
	if ( me.getID() == MouseEvent.MOUSE_PRESSED &&
	     bufferDim != null )
	    {
		int col = (me.getX() - boardOrigin.width) / colWidth;
		int row = (me.getY() - boardOrigin.height) / rowHeight;
		
		// don't let the round of pixels cause
		// an outof bounds exception
		if ( me.getX() >= boardOrigin.width &&
		     me.getY() >= boardOrigin.height &&
		     row < ROWS && col < COLS )
		    { 
			Enumeration ee = actionListeners.elements();
			Space space = board.getSpace( row, col );
			ActionEvent ae = new ActionEvent( space,
							  ActionEvent.ACTION_PERFORMED,
							  "Tigris Command",
							  me.getModifiers() );
			
			while ( ee.hasMoreElements() )
			    {
				ActionListener al = (ActionListener) ee.nextElement();
				al.actionPerformed( ae );
			    }

		    }
	    }
    }
}


