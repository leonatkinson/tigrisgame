/* File: TigrisGraphics.java
 * Description:  Graphics algorithms for drawing icons and colors in awt.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.Leader;
import com.tigrisgame.rules.Monument;
import com.tigrisgame.rules.Sphere;

import java.awt.*;

public class TigrisGraphics
{
    Graphics m_g;

    // COLORS
    // XXXTileColor is the background color of the tile (background)
    // XXXOffColor is the color of the writing of the tile (foreground)
    
    public static final Color
	waterColor       = new Color( 0x6666CC ),
	landColor        = new Color( 0xFFCC99 ),
	gridColor        = new Color( 0x333333 ), 
	monBgColor       = new Color( 0xCC9966 ),
	catColor         = new Color( 0xFF3333 ),
	catBgColor       = new Color( 0xCCCCCC ),
	blackTileColor   = new Color( 0x333333 ), 
	blackOffColor    = new Color( 0xFFCC33 ),
	redTileColor     = new Color( 0xFF3333 ),
	redOffColor      = new Color( 0x333333 ),
	greenTileColor   = new Color( 0x339933 ),
	greenOffColor    = new Color( 0x333333 ),
	blueTileColor    = new Color( 0x0000CC ),
	blueOffColor     = new Color( 0x99CCCC ),//new Color( 0x99CCFF ),
	treasureColor    = new Color( 0xFFCC33 ),
	treasureOffColor = new Color( 0x333333 ),
	// roster icon colors.  Chosen to (a) stay clear of every tile,
	// terrain, and sphere color, and (b) remain distinguishable from
	// each other under common color-vision deficiencies: a neutral
	// gray plus two chromatic hues on opposite sides of gray that
	// differ in both hue and lightness (no red/green pairing).
	rosterIdleColor     = new Color( 0x6F6F78 ),
	rosterActiveColor   = new Color( 0x00A0B0 ),
	rosterDecisionColor = new Color( 0xCC3399 );

    public TigrisGraphics()
    {
    }

    public TigrisGraphics( Graphics g )
    {
	m_g = g;
    }

    // COLOR METHODS
    public static Color getForegroundColor( Sphere sp )
    {
	Color color;
	if ( sp.isBlack() )
	    {
		color = blackOffColor;
	    }
	else if ( sp.isRed() )
	    {
		color = redOffColor;
	    }
	else if ( sp.isBlue() )
	    {
		color = blueOffColor;
	    }
	else if ( sp.isGreen() )
	    {
		color = greenOffColor;
	    }
	else 
	    {
		// no known color for sphere !!
		color = null;
	    }
	return color;
    }
    public static Color getBackgroundColor( Sphere sp )
    {
	Color color;

	if ( sp.isBlack() )
	    {
		color = blackTileColor;
	    }
	else if ( sp.isRed() )
	    {
		color = redTileColor;
	    }
	else if ( sp.isBlue() )
	    {
		color = blueTileColor;
	    }
	else if ( sp.isGreen() )
	    {
		color = greenTileColor;
	    }
	else 
	    {
		// no known color for sphere !!
		color = null;
	    }
	return color;
    }

    // DRAW METHODS
    public void drawTile( int x, int y, int width, int height, Sphere tile )
    {
	drawTile( x,y,width,height, tile, m_g );
    }

    public static void drawTile( int x, int y, int width, int height,
				 Sphere tile, Graphics g  )
    {
	// how much away from the square we draw the tile
	final int tilePad = 0; //cm used to be 1.
	
	if ( tile == null )
	    return;

	g.setColor( getBackgroundColor( tile ) );
	//g.fillRect( x+tilePad,y+tilePad,
	//	    width-2*tilePad,height-2*tilePad );

	g.fill3DRect( x+tilePad,y+tilePad,
		      width-2*tilePad,height-2*tilePad, true );

	// no outline
	//g.setColor( getForegroundColor( tile ) );
	//g.drawRect( x+tilePad,y+tilePad,
	//	    width-2*tilePad -1,height-2*tilePad -1);

	// 2nd rect one pixel inside first
	//g.drawRect( x+tilePad+1,y+tilePad+1,
	//width-2*tilePad -3,height-2*tilePad -3);
    }

    public void drawLeader(int x,
			   int y,
			   int width,
			   int height,
			   Leader leader )
    {
	drawLeader( x,y,width,height,leader,m_g);
    }

    public static void drawLeader(int x,
				  int y,
				  int width,
				  int height,
				  Leader leader, 
				  Graphics g)
    {
	if ( leader == null )
	    return;

	Color fgColor = getForegroundColor( leader.getSphere() );
	Color bgColor = getBackgroundColor( leader.getSphere() );

	g.setColor( bgColor );
	g.fillOval( x,y,width,height );

	// black outline of circle
	//g.setColor( Color.black );
	//g.drawOval( x, y, width, height );

	// slightly smaller circle.
	//g.drawOval( x + width/8,
	//	    y + width/8,
	//	    width-width/4,
	//	    height-height/4 );	

	g.setColor( fgColor );

	drawLeaderGlyph( x, y, width, height, leader.getDynasty(), g );
    }

    /** draw the dynasty glyph (circle, triangle, square, or X) inside
     * an already-painted leader oval.  Shared by drawLeader() and
     * drawLeaderIcon().
     */
    private static void drawLeaderGlyph(int x,
					int y,
					int width,
					int height,
					int dynasty,
					Graphics g)
    {
	// hmm is there a better way to do this?
	switch ( dynasty )
	    {
	    case 0:
		/*
		g.drawOval( x+ width/4,
			    y,
			    width/2,
			    height/2);
		g.drawOval( x+width/4,
			    y+height/2,
			    width/2,
			    height/2);
		*/
		g.drawOval( x+ width/4,
			    y,
			    width/2,
			    (height*5)/8);
		g.drawOval( x+width/4,
			    y+(3*height)/8,
			    width/2,
			    (height*5)/8);
		break;
	    case 1:
		Polygon triangle = new Polygon( new int[]{ x+width/2, x+(width*7)/8, x+width/8},
						new int[]{y, y+height/2, y+height/2},3);
		g.drawPolygon( triangle );
		g.drawLine( x+width/2, y, x+width/2, y+height );
		break;
	    case 2:
		// this rectangle is an attempt to inscribe the circle of the leader.
		// is assumes 3/10 ~= 1-1/sqrt(2) = .707
		g.drawRect( x+(3*width)/20, y+(3*height)/20, (width*7)/10, (height*7)/10);
		g.drawRect( x+(10*width)/20, y+(10*height)/20, (width*7)/20, (height*7)/20);
		break;
	    case 3:
		// draw an X that touches the circle.
		// also assumes 3/10 ~= 1-1/sqrt(3)
		g.drawLine( x+(3*width)/20,y+(3*height)/20,x+(17*width)/20, y+(17*height)/20 );
		g.drawLine( x+(3*width)/20, y+(17*height)/20,x+(17*width)/20,y+(3*height)/20);
		break;
	    default:
		break;
	    }
    }

    /** draw a leader icon with explicit colors, for places like the
     * player roster where the sphere's own colors would conflict with
     * its state meaning there (green = your move, blue = your action
     * decision, black = waiting, red = illegal).  Used by PlayerLabel
     * with one of the roster colors as the background.
     */
    public static void drawLeaderIcon(int x,
				      int y,
				      int width,
				      int height,
				      Leader leader,
				      Color bgColor,
				      Graphics g)
    {
	if ( leader == null )
	    return;

	g.setColor( bgColor );
	g.fillOval( x, y, width, height );

	// pick a glyph color with real contrast against the stated
	// background: dark glyph on bright backgrounds, white on the
	// darker ones (WCAG 1.4.11 wants 3:1 for graphics; the old
	// gold glyph falls under 2.5:1 on the teal).
	int greyLevel = bgColor.getRed() * 299
	    + bgColor.getGreen() * 587 + bgColor.getBlue() * 114;
	if ( greyLevel > 140000 )
	    g.setColor( Color.black );
	else
	    g.setColor( Color.white );

	drawLeaderGlyph( x, y, width, height, leader.getDynasty(), g );
    }

    public void drawMonument(int x,
			     int y,
			     int width,
			     int height,
			     Monument mon )
    {
	drawMonument( x,y,width,height, mon, m_g);
    }

    public static void drawMonument(int x,
				    int y,
				    int width,
				    int height,
				    Monument mon,
				    Graphics g)
    {
	//g.setColor( Color.black );
	//g.drawRect( x+1,y+1, width-2,height-2 );

	g.setColor( TigrisGraphics.monBgColor );
	g.fill3DRect( x, y, width, height, true );

	g.setColor( getBackgroundColor( mon.getFirstSphere() ));
	g.fill3DRect( x+width/4,
		      y+height/4,
		      width / 2,
		      height/ 2, true );
	
	g.setColor( getBackgroundColor( mon.getSecondSphere() ));
	g.fillRect( x+width/3,
		      y+height/2,
		      width/3,
		      height/4);
    }

    public void drawCatastrophe(int x,
				int y,
				int width,
				int height)
	
    {
	drawCatastrophe( x,y,width,height, m_g);
    }
    public static void drawCatastrophe(int x,
				       int y,
				       int width,
				       int height,
				       Graphics g)
    {
	g.setColor( catBgColor );
	//g.fill3DRect( x, y, width,height,false);
	g.fillRect( x, y, width,height );
	//g.fillRect( x+width/4, y+height/4, width/2,height/2 );
	g.setColor( catColor );
	// X that takes up half the square
	//g.drawLine( x+width/4, y+height/4, x+width*3/4-1, y+height*3/4-1 );
	//g.drawLine( x+width*3/4-1, y+height/4, x+width/4, y+height*3/4-1);
	// X that takes up all the square
	g.drawLine( x, y, x+width-1, y+height-1 );
	g.drawLine( x+width-1, y, x, y+height-1);
    }

    public void drawJoinMarker(int x,
			       int y,
			       int width,
			       int height)
			       
    {
	drawJoinMarker( x,y,width,height, m_g);
    }

    // IS WIDTH TOO BIG??  MAYBE WIDTH - 1 ??
    public static void drawJoinMarker(int x,
				      int y,
				      int width,
				      int height,
				      Graphics g)
    {
	g.setColor( Color.white );
	g.fillRect( x, y, width,height );

	// Left hand of "handshake" is on the bottom
	int [] leftX = new int[] { x, x,
				   x+width*3/10,
				   x+width*6/10,
				   x+width*8/10,
				   x+width*6/10 };
	int [] leftY = new int[] { y + height*4/10,
				   y + height*7/10,
				   y + height*7/10,
				   y + height*8/10,
				   y + height*6/10,
				   y + height*4/10 };
	// Right hand of "handshake" is on top
	int [] rightX = new int[] { x + width,
				    x + width*6/10,
				    x + width*5/10,
				    x + width*3/10,
				    x + width*4/10,
				    x + width*6/10,
				    x + width*15/20,
				    x + width };
	int [] rightY = new int [] { y,
				     y + height*3/10,
				     y + height*3/10,
				     y + height*5/10,
				     y + height*6/10,
				     y + height*4/10,
				     y + height*11/20,
				     y + height*3/10 };

	g.setColor( redTileColor );	
	g.fillRect( x,y,width,height );

	g.setColor( treasureColor );
	g.fillRect( x,y,width,height );
	//g.fillOval( x + width/8, y + height/8,
	//width*3/4, height*3/4 );
	
	g.setColor( landColor );
	g.fillPolygon( leftX, leftY, 6 );
	g.setColor( Color.black );
	g.drawPolygon( leftX, leftY, 6 );

	g.setColor( monBgColor );
	g.fillPolygon( rightX, rightY, 8 );
	g.setColor( Color.black );
	g.drawPolygon( rightX, rightY, 8 );

	// draw the "finger" grooves.
	g.drawLine( x + width*13/20, y + height*15/20,
		    x + width*11/20, y + height*13/20 );
	g.drawLine( x + width*14/20, y + height*14/20,
		    x + width*12/20, y + height*12/20 );
	g.drawLine( x + width*15/20, y + height*13/20,
		    x + width*13/20, y + height*11/20 );
    }

    public void drawLand(int x,
			  int y,
			  int width,
			  int height)
    {
	drawLand( x,y,width,height, m_g);
    }

    public static void drawLand(int x,
				int y,
				int width,
				int height,
				Graphics g)
    {
	g.setColor( landColor );
	g.fill3DRect( x,y,width, height, true );
    }

    public void drawWater(int x,
			  int y,
			  int width,
			  int height)
    {
	drawWater( x,y,width,height, m_g);
    }

    public static void drawWater(int x,
				 int y,
				 int width,
				 int height,
				 Graphics g)
    {
	g.setColor( waterColor );
	g.fill3DRect( x,y,width, height, true );
	g.setColor( Color.black );

	// Swooshes of Water!
	int flat = height/16; // flatten the wave so they aren't as square
	g.drawArc( x+width/4,
		   y+height/4+flat,
		   width/4,
		   height/4-flat,
		   0,
		   180 );
	g.drawArc( x+width/2,
		   y+height/4+flat,
		   width/4,
		   height/4-flat,
		   180,
		   180 );
	g.drawArc( x+width/4,
		   y+height/2,
		   width/4,
		   height/4-flat,
		   0,
		   180 );
	g.drawArc( x+width/2,
		   y+height/2,
		   width/4,
		   height/4-flat,
		   180,
		   180 );
    }

    public void drawTreasure(int x,
			     int y,
			     int width,
			     int height)
	
    {
	drawTreasure(x,y,width,height, m_g);
    }
    
    // x,y,width,height define the size of the square that the treasure is on.
    public static void drawTreasure(int x,
				    int y,
				    int width,
				    int height,
				    Graphics g)
    {
	g.setColor( Color.orange );
	g.fill3DRect( x+width/4, y+height/4, width/2, height/2, true );
	g.setColor( Color.black );
    }

    public void drawCenteredString(int x,
				   int y,
				   int width,
				   int height,
				   String str)
    {
	drawCenteredString( x, y, width,height, str, m_g );
    }

    // this method takes a rectangular region defined by {x,y,width,height}
    // and attempts to center the text given by string str
    // font and color are set before hand.
    public static void drawCenteredString(int x,
					  int y,
					  int width,
					  int height,
					  String str,
					  Graphics g)
    {
	FontMetrics font = g.getFontMetrics();

	int xPad;
	int yPad;

	xPad = ( width - font.stringWidth( str ) );
	if ( xPad != 0 )
	    xPad /= 2;
	    
	yPad = ( height - font.getAscent() ); // font.getHeight() );
	if ( yPad != 0 )
	    yPad /= 2;

	g.drawString( str, x + xPad, y + height - yPad );
    }

    public static void main( String args [] )
    {
	Frame f = new Frame();
	f.add( new Panel() {
	    public void paint( Graphics g ) {
		drawJoinMarker(0,0,getSize().width, getSize().height, g);
	    }});
	f.setSize( 200, 200 );
	f.setVisible( true );
    }
}









