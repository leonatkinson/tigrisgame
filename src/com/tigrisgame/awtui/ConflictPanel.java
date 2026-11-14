/* File: ConflictPanel.java
 * Description:  Resizable Panel that holds conflict status.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 TODO... needs text on top and bottom,
 some sane limits on the size of things,
 and DOUBLE BUFFER!!!

 LAYOUT
 ------
 * ConflictPanel, layout = borderLayout
  ** NORTH = playerLabel
  ** SOUTH = playerLabel
  ** CENTER = tilePanel (inner class)
  
  */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.Player;
import com.tigrisgame.rules.Sphere;
import com.tigrisgame.rules.Tile;

import java.awt.*;

class ConflictPanel extends Panel
{
    TileTable tiles = new TileTable();
    boolean verbose = false;
    Label firstLabel = new Label( "firstLabel" );
    Label secondLabel = new Label( "secondLabel" );
    TextArea bottomText = new TextArea( "textArea", 1, 1, TextArea.SCROLLBARS_NONE );
    Font labelFont;
    
    ConflictPanel()
    {
	Panel labelPanel = new Panel( new GridLayout( 2, 0 ));
	labelPanel.add( firstLabel );
	labelPanel.add( secondLabel );

	bottomText.setEditable( false );
	Panel panel = new Panel( new BorderLayout() );
	panel.add( BorderLayout.NORTH, tiles );
	panel.add( BorderLayout.CENTER, bottomText );

	setBackground( TigrisGraphics.landColor );
	setLayout( new BorderLayout() );
	add( BorderLayout.NORTH, labelPanel );
	add( BorderLayout.CENTER, panel );
    }

    public void setAttacker( Player player, int board, int screen )
    {
	if ( verbose )
	    System.out.println("setting attacker to " + board + " and " + screen );

	//attackerLabel.setPlayer( player );
	tiles.numTiles[0] = board; 
	tiles.numTiles[1] = screen;
	repaint();
    }

    public void setTopText( String msg1, String msg2 )
    {
	firstLabel.setText( msg1 );
	secondLabel.setText( msg2 );
    }

    public void setBottomText( String msg )
    {
	bottomText.setText( msg );
    }

    public void setDefender( Player player, int board, int screen )
    {
	if ( verbose )
	    System.out.println("setting defender to " + board + " and " + screen );

	//defenderLabel.setPlayer( player );
	tiles.numTiles[2] = board; 
	tiles.numTiles[3] = screen;
	repaint();
    }

   public void setConflictSphere( Sphere sp )
   {
      tiles.tileColor = new Tile( sp );
      repaint();
   }

    public void setTileSize( int width, int height )
    {
	labelFont = new Font( "Monospaced", Font.PLAIN, Math.min( width/2, height/2) );
	firstLabel.setFont( labelFont );
	secondLabel.setFont( labelFont );
	bottomText.setFont( labelFont );
	tiles.setTileSize( width, height );
    }

    // getXXXsize should respect the size of the containted components.
//      public Dimension getMinimumSize()
//      {
//    	// for now
//    	return tiles.getMinimumSize();
//      }
    
//      public Dimension getPreferredSize() 
//      {
//    	// for now
//    	return tiles.getMinimumSize();
//      }
    
    public void repaint()
    {
	super.repaint();
	tiles.repaint();
    }

    public static void main( String args[] )
    {
	final Frame f = new Frame("Conflict Panel");
	ConflictPanel conf = new ConflictPanel();
	if ( args.length > 1 )
	    conf.setAttacker( new Player(1,"Bob"), 
			      Integer.parseInt( args[0] ),
			      Integer.parseInt( args[1] ));
	if ( args.length > 3 )
	    conf.setDefender( new Player(2,"Craig"), 
			      Integer.parseInt( args[2] ),
			      Integer.parseInt( args[3] ));
	if ( args.length > 4 ) 
	    conf.setConflictSphere( new Sphere( Integer.parseInt( args[4] )));
	else
	    conf.setConflictSphere( Sphere.RED );
	//conf.setTileSize( 21, 21 );
	f.setLayout( new BorderLayout() );
	f.add( BorderLayout.CENTER, conf );
	f.setSize( 300,300 );
	/*f.addWindowListener( new WindowAdapter()
	    { public void windowClosing( WindowEvent e ) 
		{ f.dispose(); }});*/
	f.setVisible( true );
    }

    class TileTable extends Panel
    {
	// used in TigrisGraphics.drawTile
	Tile tileColor;
	int tileWidth, tileHeight;

	// 0 = attackBoard,
	// 1 = attackScreen,
	// 2 = defendBoard,
	// 3 = defendScreen;
	int [] numTiles = new int [4];
	int padding = 5;
	int minWidth;

	TileTable()
	{
	    this( null, 0, 0, 0, 0 );
	}

	TileTable( Sphere s, int ab, int as, int db, int ds )
	{
	    tileColor = new Tile( s );
	    numTiles[0] = ab;
	    numTiles[1] = as;
	    numTiles[2] = db;
	    numTiles[3] = ds;
	    setTileSize( UIScale.scale( 42 ), UIScale.scale( 42 ) );
	}

	public void setTileSize( int width, int height )
	{
	    tileWidth = width;
	    tileHeight = height;
	    padding = UIScale.scale( 5 );
	    minWidth = 4*padding + 7*tileWidth; // 2 and a half tiles per side.
	    invalidate();
	    //FontMetrics fm = new FontMetrics( font );
	    //minWidth = 4*padding + fm.stringWidth( "Board" ) + fm.stringWidth( "Screen" );
	}

	public void setBounds( int x, int y, int width, int height )
	{
	    super.setBounds( x, y,
			     Math.max( minWidth, width ), 
			     tileHeight * 3 + padding * 4 );
	}

	public Dimension getMinimumSize()
	{
//  	    if ( true )
//  		throw new RuntimeException("called from where???");

	    Dimension minSize = new Dimension( minWidth, tileHeight * 3 + padding * 4 );

	    return minSize;
	}
	
	public Dimension getPreferredSize() 
	{
	    return getMinimumSize();
	}
	

	public void update( Graphics g )
	{
	    paint( g );
	}
	
	public void paint( Graphics g )
	{
	    Dimension size = this.getSize();
	    TigrisGraphics tg = new TigrisGraphics( g );

	    // now these variables are set with setTileSize
	    //int tileWidth = size.width / 10;
	    //int tileHeight = size.height / 4;

	    // total width allowed to draw the tiles in.
	    //int regionWidth = size.width / 2 - tileWidth - 2*padding;
	    int regionWidth = size.width / 2 - 2*padding;

	    // the whole region.
	    //g.fill3DRect( 0,0,size.width, size.height, true);

	    g.setColor( TigrisGraphics.monBgColor );
	    // individual parts.
  	    g.fill3DRect( 0, 0,
  			  size.width/2,
  			  size.height-1 , true);
  	    g.fill3DRect( size.width/2, 0,
  			  size.width/2,
  			  size.height-1, true);

//  	    g.fill3DRect( 0, 0,
//  			  size.width/2 - tileWidth,
//  			  size.height-1 , true);
//  	    g.fill3DRect( size.width/2 - tileWidth, 0,
//  			  tileWidth, size.height-1, true);
//  	    g.fill3DRect( size.width/2, 0,
//  			  size.width/2 - tileWidth,
//  			  size.height-1, true);
//  	    g.fill3DRect( size.width - tileWidth, 0,
//  			  tileWidth, size.height-1, true);


//  	    try{
//  	    Thread.sleep(2000);
//  	    } catch (Exception e) {
//  	    }

	    // actually draw the tiles.	
	    // or ??'s if they didn't set them yet.
	    // font set for the ?'s
	    //g.setFont( labelFont );

	    for ( int tt=0; tt < numTiles.length; tt ++ ) {
		if ( numTiles[tt] == -1 ) {
		    // color set for ?'s
		    g.setColor( TigrisGraphics.blackTileColor );
		    tg.drawCenteredString( padding +
					   tt%2*size.width/2,
					   padding + tt/2*(tileHeight * 2 + padding * 2 ),
					   //(1 + tt/2*4) * size.height / 8,
					   regionWidth,
					   tileHeight,
					   "??" );
		}
		else if ( numTiles[tt] == 0 )
		    continue;
		
		else if ( numTiles[tt] == 1 ) {
		    tg.drawTile( (int)(regionWidth - tileWidth)/2  + 
				 tt%2*size.width/2,
				 padding + tt/2*(tileHeight * 2 + padding * 2 ),
				 //(1 + tt/2*4) * size.height / 8,
				 tileWidth,
				 tileHeight,
				 tileColor);
		    g.setColor( tg.getForegroundColor( tileColor ) );
		    tg.drawCenteredString( (int)(regionWidth - tileWidth)/2  + 
					   tt%2*size.width/2,
					   padding + tt/2*(tileHeight * 2 + padding * 2 ),
					   tileWidth,
					   tileHeight,
					   "" + numTiles[tt] );		

		}
		else {
		    // spacing between the left edges of the tiles.
		    // possibly do a float division here?
		    float spacing = (float) (regionWidth - tileWidth )/(float) (numTiles[tt] - 1);
		    
		    //int spacing = (regionWidth - tileWidth )/(numTiles[tt] - 1);
		    
		    // depending on the darkness of the color we either 
		    // draw the tiles from right to left or left to right
		    if ( true ) //tileColor.isBlue() || tileColor.isRed() )
			{
			    for ( int ii=numTiles[tt]-1; ii>=0; ii--)
				{
				    tg.drawTile( (int)(spacing * ii) + padding +
						 tt%2*size.width/2,
						 padding + tt/2*(tileHeight * 2 + padding * 2 ),
						 //(1 + tt/2*4) * size.height / 8,
						 tileWidth,
						 tileHeight,
						 tileColor);
				    if ( ii == 0 ) {
					g.setColor( tg.getForegroundColor( tileColor ) );
					tg.drawCenteredString( (int)(spacing * ii) + padding +
							       tt%2*size.width/2,
							       padding + tt/2*(tileHeight * 2 + padding * 2 ),
							       //(1 + tt/2*4) * size.height / 8,
							       tileWidth,
							       tileHeight,
							       "" + numTiles[tt] );
				    }
				}
			}
		    else
			{
			    for ( int ii=0; ii<numTiles[tt]; ii++)
				tg.drawTile( (int)(spacing * ii) + padding +
					     tt%2*size.width/2,
					     padding + tt/2*(tileHeight * 2 + padding * 2 ),
					     //(1 + tt/2*4) * size.height / 8,
					     tileWidth,
					     tileHeight,
					     tileColor);
			}
		}
	    }
	    g.setColor( TigrisGraphics.blackTileColor );
	    
	    tg.drawCenteredString( 0,
				   padding + tileHeight + padding,
				   //size.height*3/ 8,
				   size.width/2,
				   tileHeight,
				   "Board");
	    tg.drawCenteredString( size.width/2,
				   padding + tileHeight + padding,
				   //size.height*3/8,
				   size.width/2,
				   tileHeight,
				   "Screen" );

//  	    tg.drawCenteredString( size.width*9/10,
//  				   size.height / 8,
//  				   tileWidth,
//  				   tileHeight,
//  				   ""+(Math.max( numTiles[0],0) + Math.max(numTiles[1],0)));
//  	    tg.drawCenteredString( size.width*9/10,
//  				   size.height*5 / 8,
//  				   tileWidth,
//  				   tileHeight,
//  				   ""+(Math.max(numTiles[2],0) + Math.max(numTiles[3],0)) );
//  	    tg.drawCenteredString( size.width*4/10,
//  				   size.height / 8,
//  				   tileWidth,
//  				   tileHeight,
//  				   "+");
//  	    tg.drawCenteredString( size.width*4/10,
//  				   size.height*5 / 8,
//  				   tileWidth,
//  				   tileHeight,
//  				   "+");
	}
    }
}
