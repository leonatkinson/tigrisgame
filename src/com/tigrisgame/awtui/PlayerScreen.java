/* File: PlayerScreen.java
 * Description:  Resizable panel that shows tiles, leaders, and VP
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE:
 * TODO (minor) sometimes the non-graphical vp region has a pixel that is left 
 * at the BG color b/c of rounding errors dividing by 4.
 * getSize().width used b/c getWidth not a method until 1.2
 */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

public class PlayerScreen extends Panel
{
    // geometries for drawing
    private final int
	tilePad,  // for drawing tiles and leaders
	bigSquare, // for drawing VP
	bigPad,
	smallSquare;
    
    /*              pixel dimesions                  
     *                                               
     *     |---250---|---200---|---163---|         
     *     |  tiles  | leaders |   VP    |                        
     *     50        |         |         |                          
     *     |         |         |         |                 
     *     |         |         |         |                       
     * regions give x,y, width, height of rectangles
     *
     * VP width = 8 * bigsquare + (8+1) * bigPad
     */

//      private static final int 
//  	defaultTileRegion [] = { 0, 0, 250, 50 },
//  	defaultLeaderRegion [] = { 250, 0, 200, 50 },
//  	defaultVPRegion [] = { 450, 0, 163, 50 };

    private int 
	tileRegion []   = new int[4],
	leaderRegion [] = new int[4],
	vpRegion []     = new int[4];

    private int numVPCols = 2;
    private int minVPCols = 2;
    private int maxVPCols = 6;
    private int tileWidth, tileHeight;
    private int screenHeight;
    private Dimension minSize;
    private Font tileFont;
    private Font vpFont;
    private boolean graphicalVPRegion = true;
    // the "ideal" size of this component

    Vector actionListeners = new Vector();
    Player player;

    public boolean debug = false;

    {
	// scale all the geometry constants for HiDPI displays.
	tilePad = UIScale.scale( 4 );
	bigSquare = UIScale.scale( 17 );
	bigPad = UIScale.scale( 3 );
	smallSquare = UIScale.scale( 7 );
    }

    public PlayerScreen()
    {
	this ( null );
    }

    public PlayerScreen( Player p )
    {
	setTileSize( UIScale.scale( 42 ), UIScale.scale( 42 ) );
	player = p;
	enableEvents( AWTEvent.MOUSE_EVENT_MASK );
    }

    // reset the player and repaint
    public void setPlayer( Player p )
    {
	player = p;
	repaint();
    }

    /* sets the actual painting size of the region.
     * sets the min/max/preferred and actual sizes.
     * sets the components font's.
     * calls updateRegions
     */
    public void setTileSize( int width, int height )
    {
	tileWidth = width;
	tileHeight = height;
	screenHeight = tileHeight + tilePad * 2;
	// resize the height, no change to the width
	updateVPCols();
	updateRegions();
	updateFonts();
	invalidate();
    }

    /* gives more of an AWT "this is how much area you have to fill" statement
     * assumed to be called from setBounds(int X 4) and setSize( Dim ) so 
     * this method is effective on all of the size changing calls
     */
    public void setSize( int width, int height )
    {
	if ( debug )
	    System.out.println("setSize " + width + " " + height );
	super.setSize( width, height );
	updateVPCols();
	updateRegions();
    }

    public void setBounds( int x, int y, int width, int height )
    {
	if ( debug )
	    System.out.println("setBounds " + x + " " + y + " " + width + " " + height );
	super.setBounds( x, y, width, height );
	updateVPCols();
	updateRegions();
    }

    private void updateVPCols()
    {
	// also include the +1 of the bigPad in the used up width so the 
	// division is even in the next step.
	int vpWidth = getSize().width - (leaderRegion[0] + leaderRegion[2] - bigPad);
	numVPCols = vpWidth / ( bigSquare + bigPad );
	// also divide by the number of spheres
	numVPCols /= 4; 
	// but bound it by the max and min.
	numVPCols = Math.max( minVPCols, numVPCols );
	numVPCols = Math.min( maxVPCols, numVPCols );
    }

    // todo.. possibly update the regions to have "insets" to center if the width is too big.
    private void updateRegions()
    {
	tileRegion = new int[] { 0, 0, tileWidth * 5 + tilePad * 12, screenHeight };
	leaderRegion = new int [] { tileRegion[2], 0, tileWidth * 4 + tilePad * 8, 
				    screenHeight };
	// set the vp region, it's width is at first "the rest"
	vpRegion = new int [] { leaderRegion[2] + leaderRegion[0], 0,
				getSize().width - tileRegion[2] - leaderRegion[2], 
				screenHeight };
	// bound it's width by the min and max values
	vpRegion[2] = Math.max( vpRegion[2],
				bigSquare * minVPCols * 4 + 
				bigPad * (minVPCols * 4 + 1));
	vpRegion[2] = Math.min( vpRegion[2],
				bigSquare * maxVPCols * 4 + 
				bigPad * (maxVPCols * 4 + 1));
    }

    private void updateFonts()
    {
	tileFont = new Font( "Monospaced", Font.PLAIN, 
			     Math.min( tileHeight /2, tileWidth /2 ) );
	vpFont = new Font( "Monospaced", Font.PLAIN, 
			   UIScale.scale( 10 ) );
    }

    public Dimension getMinimumSize()
    {
	Dimension minSize = new Dimension( tileRegion[2] + leaderRegion[2] +
					   bigSquare * minVPCols *4 + bigPad * (minVPCols * 4 + 1),  
					   screenHeight );
	if ( debug )
	    System.out.println("getMinimumSize " + minSize );
	return minSize;
    }

    public Dimension getPreferredSize() 
    {
	return getMinimumSize();
    }

    // this method rarely gets called because the component is a fixed
    // size and is simply moved around as the bigger window is resized.
    public void paint( Graphics g )
    {
	update( g );
    }

    public void update( Graphics g )
    {
	TigrisGraphics tg = new TigrisGraphics( g );
	Player drawPlayer = player != null ? player: new Player( Player.INVALID );

	// fill the component to the background region
	g.setColor( TigrisGraphics.landColor );
	g.fillRect( 0,0,getSize().width, screenHeight);

	// draw lines between the regions.
//  	g.setColor( getForeground() );
//  	g.drawLine( leaderRegion[0],leaderRegion[1],
//  		    leaderRegion[0],
//  		    leaderRegion[1] +leaderRegion[3]);
//  	g.drawLine( vpRegion[0],vpRegion[1],
//  		    vpRegion[0],
//  		    vpRegion[1] + vpRegion[3]);

	//g.setColor( Color.black );
	//g.drawRect( vpRegion[0],vpRegion[1],vpRegion[2]-1,vpRegion[3]-1);

	g.fill3DRect( tileRegion[0],tileRegion[1],tileRegion[2],tileRegion[3], true);
	g.fill3DRect( leaderRegion[0],leaderRegion[1],leaderRegion[2],leaderRegion[3], true);
	g.setColor( TigrisGraphics.monBgColor );
	g.fill3DRect( vpRegion[0], vpRegion[1], vpRegion[2], vpRegion[3],true);
	// add a small easter egg at the end.
	g.drawString( "wawa rules", 
		      tileRegion[2] + leaderRegion[2] + vpRegion[2] + tilePad,
		      getSize().height - tilePad );
	//g.fill3DRect( vpRegion[0],vpRegion[1],vpRegion[2],vpRegion[3],true);

	// DRAW TILES
	Sphere [] spheres = Sphere.getSpheres();
	g.setFont( tileFont );
	for ( int ss=0; ss< spheres.length; ss++) {
	   tg.drawTile( tileRegion[0] + (tileWidth+tilePad*2)*ss + tilePad,
			tileRegion[1] + tilePad,
			tileRegion[2]/5 - tilePad*2,
			tileRegion[3] - tilePad*2,
			spheres[ss] ); 
	   
	   g.setColor( tg.getForegroundColor( spheres[ss] ) );
	   tg.drawCenteredString( tileRegion[0] + (tileWidth+tilePad*2)*ss,
				  tileRegion[1],
				  tileRegion[2]/5,
				  tileRegion[3],
				  "" + drawPlayer.getNumTiles( spheres[ss] ));
	}

	// CATASTROPHE	
	tg.drawCatastrophe( tileRegion[0] + (tileWidth+tilePad*2)*4 + tilePad,
			    tileRegion[1] + tilePad,
			    tileRegion[2]/5 - tilePad*2,
			    tileRegion[3] - tilePad*2);

 	g.setColor( TigrisGraphics.blackTileColor );
	tg.drawCenteredString( tileRegion[0] + (tileWidth+tilePad*2)*4 + tilePad,
			       tileRegion[1],
			       tileRegion[2]/5 - tilePad*2,
			       tileRegion[3],
	    "" + drawPlayer.getNumCatastrophes());

	/*
	g.setColor( tg.redTileColor );
	g.fillRect( tileRegion[0]+50 + tilePad,
		    tileRegion[1] + tilePad,
		    tileRegion[2]/4 - tilePad*2,
		    tileRegion[3] - tilePad*2);
	g.setColor( tg.blueTileColor );
	g.fillRect( tileRegion[0]+100+ tilePad,
		    tileRegion[1]+ tilePad,
		    tileRegion[2]/4 - tilePad*2,
		    tileRegion[3] - tilePad*2);
	g.setColor( tg.greenTileColor );
	g.fillRect( tileRegion[0]+150+ tilePad,
		    tileRegion[1]+ tilePad,
		    tileRegion[2]/4- tilePad*2,
		    tileRegion[3]- tilePad*2);
	*/

	// DRAW LEADERS
	// TigrisGraphics.drawLeader ignores null, so leaders are only drawn
	// if player.getLeader != null
	for ( int ss=0; ss< spheres.length; ss++)
	    {
		tg.drawLeader( leaderRegion[0] + (tileWidth + tilePad * 2) * ss + tilePad,
			       leaderRegion[1] + tilePad,
			       leaderRegion[2]/4 - tilePad*2,
			       leaderRegion[3] - tilePad*2, 
			       drawPlayer.getLeader( spheres[ss] ));
	    }

	// DRAW VP
	drawVPRegion(g);
    }

    public void drawVPRegion( Graphics g )
    {
	// GET SPHERES
	Sphere [] spheres = Sphere.getSpheres();

	Player drawPlayer = player != null ? player: new Player( Player.INVALID );

	// INITIALIZERS
	int [] victoryPoints = new int [] {
	    drawPlayer.getVictoryPoints( spheres[0] ),
	    drawPlayer.getVictoryPoints( spheres[1] ),
	    drawPlayer.getVictoryPoints( spheres[2] ),
	    drawPlayer.getVictoryPoints( spheres[3] ) };

	int treasures = drawPlayer.getNumTreasures();
	int [] treasureDist = Player.assignTreasures( victoryPoints, treasures );

	// this is a mangling of vp and treasures assigned so that it is sorted
	// first by total, and then by number w/o treasures.
	// the assumption here is there will never be more than 100 treasures.
	// ( ( total Victory Points + Treasures ) * 100 ) + Victory Points
	int [] totalVP = 
	    new int [] { ( (victoryPoints[0] + treasureDist[0]) * 100 ) 
			 + (victoryPoints[0]), 
			 ( (victoryPoints[1] + treasureDist[1]) * 100 ) 
			 + (victoryPoints[1]),
			 ( (victoryPoints[2] + treasureDist[2]) * 100 ) 
			 + (victoryPoints[2]),
			 ( (victoryPoints[3] + treasureDist[3]) * 100 )
			 + (victoryPoints[3]) };

	// GET SORTED SPHERES
	spheres = Sphere.getSpheres(); 
	//(Sphere []) Player.sortSpheresDescending( spheres, totalVP );

	victoryPoints = new int [] {
	    drawPlayer.getVictoryPoints( spheres[0] ),
	    drawPlayer.getVictoryPoints( spheres[1] ),
	    drawPlayer.getVictoryPoints( spheres[2] ),
	    drawPlayer.getVictoryPoints( spheres[3] ) };

	treasureDist = Player.assignTreasures( victoryPoints, treasures );
	
	if ( graphicalVPRegion )
	    {
		// btw, this is WAY cleaner than the first attempt. ;)
		for ( int spNum=0; spNum< spheres.length; spNum++)
		    {
			Color col = TigrisGraphics.getBackgroundColor( spheres[spNum] );
			Color tc = TigrisGraphics.treasureColor;
			
			// Draw the Treasures.
			// draw first so that the big treasure square is drawn under the 
			// indivual colored smallSquares when treasures fill a set of five.
			for ( int treasure = 1; treasure <= treasureDist[spNum]; treasure++ )
			    {
				if (( treasure + victoryPoints[spNum] ) % 5 == 0 )
				    {
					drawBigSquare( g, tc, treasure + 
						       victoryPoints[spNum], spNum ); 
				    }
				else
				    {
					drawSmallSquare( g, tc, treasure + 
							 victoryPoints[spNum], spNum);
				    }
			    }
			// draw the regular VP 
			for ( int regularVP = victoryPoints[spNum]; regularVP > 0;  )
			    {
				switch ( regularVP % 5 )
				    {
				    case 0:
					// draw the big square.
					// note: not drawn for regularVP=0 
					// because loop would be skipped
					drawBigSquare( g, col, regularVP, spNum );
					regularVP -= 5; // took care of drawing 5 points.
					break;
				    case 4:
					drawSmallSquare( g, col, regularVP, spNum );
					regularVP -= 1; // took care of one vp.
				    case 3:
					drawSmallSquare( g, col, regularVP, spNum );
					regularVP -= 1;
				    case 2:
					drawSmallSquare( g, col, regularVP, spNum );
					regularVP -= 1;
				    case 1:					
					drawSmallSquare( g, col, regularVP, spNum );
					regularVP -= 1;
					break;
				    default:
					System.out.println("ERROR in PlayerScreen.drawVPRegion,"+
							   "default in regularVP switch");
				    }

			    }
		    }
	    }
	else
	    // PRINT the total number (w/ treasure) in the BG color of the tile in the middle
	    // or the region. (centered.. possibly with a FG box around it.
	    // NOTE.. check out the comments at the end of gamecanvas(?) about toolbox 
	    // and graphics metrics stuff.
	    {
		final int xPad=0;
		final int yPad=0;

		g.setFont( vpFont );
		for ( int spNum=0; spNum< spheres.length; spNum++)
		    {
			Color bgCol = TigrisGraphics.getBackgroundColor( spheres[spNum] );
			Color fgCol = TigrisGraphics.getForegroundColor( spheres[spNum] );
			Color tc = TigrisGraphics.treasureColor;
			
			g.setColor( bgCol );
			g.fillRect( vpRegion[0] + ( vpRegion[2]*spNum )/ 4 + xPad,
				    vpRegion[1] + yPad,
				    vpRegion[2]/4 - 2*xPad,
				    vpRegion[3] - 2*yPad);
			
			g.setColor( fgCol );
			// note: centered in the big rectangle is the 
			// same as centered in the padded rect.
			// WISHLIST/UI ISSUE: is there a better string to display?
			// the choices are: total w/ treasures, 
			// total w/o treasure, treasures added
			// or just total treasures at the end.
			String vpString = "" + victoryPoints[spNum];
			if ( treasureDist[spNum] > 0 )
			    vpString = vpString + "+" + treasureDist[spNum];
			// used to be = victoryPoints[spNum] ( treasureDist[spNum] )
			TigrisGraphics.drawCenteredString( vpRegion[0] + ( vpRegion[2]*spNum )/ 4,
							   vpRegion[1], vpRegion[2]/4,vpRegion[3],
							   vpString, g );
		    }
	    }    
    }

    void drawBigSquare( Graphics g, Color color, int location, int positionNum )
    {
	g.setColor( color );

	int bigSquareNum = location / 5 - 1; // start at zero not 1.

	int [] bigSquareCorner = getBigSquareCorner( bigSquareNum, positionNum );	

	// draw big sqaure
	if ( location == 0 )
	    {
		System.out.println("Warning: PlayerScreen.drawBigSquare called for location 0");
	    }
	else if ( location % 5 != 0 )
	    {
		System.out.println("Warning: PlayerScreen.drawBigSquare called " +
				   "for location not divisible 5: " + location);
	    }
	else
	    {
		g.fill3DRect( bigSquareCorner[0], bigSquareCorner[1], bigSquare, bigSquare, true );
	    }
    }
    
    void drawSmallSquare( Graphics g, Color color, int location, int positionNum )
    {
	g.setColor( color );

	int bigSquareNum = location / 5;

	int [] bigSquareCorner = getBigSquareCorner( bigSquareNum, positionNum );	

	// draw small square
	// small squares are drawn within what would have been the big square
	// they are padded smallPad away from each other.
	// for this reason, smallSquare*2 < bigSquare (should be)
	switch ( location % 5 )
	    {
	    case 4:
		g.fill3DRect( bigSquareCorner[0] + bigSquare - smallSquare,
			      bigSquareCorner[1],
			      smallSquare, smallSquare, true );

		break;
	    case 3:
		g.fill3DRect( bigSquareCorner[0],
			      bigSquareCorner[1],
			      smallSquare, smallSquare, true );
		break;
	    case 2:
		g.fill3DRect( bigSquareCorner[0] + bigSquare - smallSquare,
			      bigSquareCorner[1] + bigSquare - smallSquare,
			      smallSquare, smallSquare, true );
		break;
	    case 1:
		g.fill3DRect( bigSquareCorner[0],
			      bigSquareCorner[1] + bigSquare - smallSquare,
			      smallSquare, smallSquare, true );
		break;
	    default:
		System.out.println("Warning: PlayerScreen.drawSmallSquare called " +
				   "for location divisible by 5: " + location);
	    }
    }

    public int [] getBigSquareCorner( int bigSquareNum, int positionNum )
    {
	// big Square Corner {x,y}
	int bigSquareCorner [] = new int [2];
	
	bigSquareCorner[0] = 
	    vpRegion[0] + 
	    (bigSquareNum%numVPCols)* 
	    (bigSquare + bigPad) +
	    // the new way, centering it in a quarter of the vp region
	    (vpRegion[2]*positionNum)/4
	    // the old way, moving across col by col
	    //(positionNum*numVPCols)*
	    //(bigSquare + bigPad) 
	    + bigPad;
	bigSquareCorner[1] = 
	    vpRegion[1] + vpRegion[3] - 
	    (bigSquareNum/numVPCols + 1)*
	    (bigSquare + bigPad);
	
	return bigSquareCorner;
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
	//or MouseEvent.MOUSE_CLICKED if you prefer
	if ( me.getID() == MouseEvent.MOUSE_PRESSED )
	    {
		Object objectClicked = null;

		int xPos = me.getX();
		if ( xPos < tileRegion[0] + tileRegion[2] )
		    {
			// TILE CLICKED
			
			if ( xPos < tileRegion[0] + tileRegion[2] / 5 )
			    {
				objectClicked = new Tile( Sphere.BLACK );
			    }
			else if ( xPos < tileRegion[0] + tileRegion[2] * 2 / 5 )
			    {
				objectClicked = new Tile( Sphere.RED );
			    }
			else if ( xPos < tileRegion[0] + (tileRegion[2] * 3)/ 5 )
			    {
				objectClicked = new Tile( Sphere.BLUE );
			    }
			else if ( xPos < tileRegion[0] + (tileRegion[2] * 4)/ 5 )
			    {
				objectClicked = new Tile( Sphere.GREEN );
			    }
			else 
			    {
				objectClicked = new Catastrophe();
			    }
		    }
		else if ( xPos < leaderRegion[0] + leaderRegion[2])
		    {
			// Leader CLICKED
			// Fire the event with a Leader object if the player has
			// the leader in their hand, if the space is empty, fire it
			// with a Player object, that way the GamePanel can detect
			// a "put it back in my hand" move.
			Sphere leaderSphere;

			if ( xPos < leaderRegion[0] + leaderRegion[2] / 4 )
			    { 
				leaderSphere = new Sphere( Sphere.BLACK );
				if ( player.getLeader( leaderSphere ) != null )
				    objectClicked = new Leader( leaderSphere, player.getDynasty());
				else
				    objectClicked = player;
			    }
			else if ( xPos < leaderRegion[0] + leaderRegion[2] / 2 )
			    {
				leaderSphere = new Sphere( Sphere.RED);
				if ( player.getLeader( leaderSphere ) != null )
				    objectClicked = new Leader( leaderSphere, player.getDynasty());
				else
				    objectClicked = player;
			    }
			else if ( xPos < leaderRegion[0] + (leaderRegion[2] * 3)/ 4 )
			    { 
				leaderSphere = new Sphere( Sphere.BLUE );
				if ( player.getLeader( leaderSphere ) != null )
				    objectClicked = new Leader( leaderSphere, player.getDynasty());
				else
				    objectClicked = player;
			    }
			else
			    {
				leaderSphere = new Sphere( Sphere.GREEN );
				if ( player.getLeader( leaderSphere ) != null )
				    objectClicked = new Leader( leaderSphere, player.getDynasty());
				else
				    objectClicked = player;
			    }
		    }
		else if ( xPos < vpRegion[0] + vpRegion[2] )
		    {
			// VP REGION CLICKED
			// switch from graphics to text
			graphicalVPRegion = !graphicalVPRegion;
			repaint();
		    }
		
		// fire action listeners
		if ( objectClicked != null )
		    {
			Enumeration ee = actionListeners.elements();
			ActionEvent ae = new ActionEvent( objectClicked,
							  ActionEvent.ACTION_PERFORMED,
							  "Tigris Command",
							  me.getModifiers() );
			while ( ee.hasMoreElements() )
			    {
				ActionListener al = (ActionListener) ee.nextElement();
				al.actionPerformed( ae );
			    }
			
			if ( debug ) 
			    System.out.println( "clicked on " + objectClicked );
		    }
	    }
    }

    public static void main(String args[])
    {
    	Player player = new Player(1, "bob");
	//package private methods.. too bad.
	player.addVictoryPoints( Sphere.RED, 6 );
	for ( int ii=0; ii<9; ii++)
	    player.addTreasure();

	Frame frame = new Frame("PlayerScreen");
	PlayerScreen ps = new PlayerScreen( player );
	player.addVictoryPoints( Sphere.BLACK, 24 );
	player.addVictoryPoints( Sphere.BLUE, 34 );
	ps.setTileSize( 50, 50 );
	frame.validate();
	frame.setLayout( new BorderLayout() );
	frame.add( BorderLayout.SOUTH, ps );
	frame.add( BorderLayout.WEST, new PlayerScreen() );
	frame.add( BorderLayout.CENTER, new PlayerScreen() );
	//frame.add( BorderLayout.SOUTH, new Button("hi") );
	frame.setSize( 625,550 );
	frame.addWindowListener( new WindowAdapter()
	    { public void windowClosing( WindowEvent e ) 
		{ System.exit(1);}
	    });
	frame.setVisible( true );

	System.out.println("size = " + ps.getSize() );
    }
}

/* OLD

clipSizeRatio()
{
	double sizeRatio;

	sizeRatio = (double)(width) / 
  	    (double)(defaultTileRegion[2] + defaultLeaderRegion[2] + defaultVPRegion[2]);
	

//  	sizeRatio = (double)(width - defaultVPRegion[2]) / 
//  	    (double)(defaultTileRegion[2] + defaultLeaderRegion[2]);

	if ( sizeRatio > 1.0 ) 
	    {
		tileRegion[0] = 0;
		tileRegion[1] = 0;
		tileRegion[2] = (int)(sizeRatio * defaultTileRegion[2]);
		tileRegion[3] = (int)(sizeRatio * defaultTileRegion[3]);

		leaderRegion[0] = tileRegion[2];
		leaderRegion[1] = 0;
		leaderRegion[2] = (int)(sizeRatio * defaultLeaderRegion[2]);
		leaderRegion[3] = (int)(sizeRatio * defaultLeaderRegion[3]);

		vpRegion[0] = tileRegion[2] + leaderRegion[2];
		vpRegion[1] = 0;
		vpRegion[2] = (int)(sizeRatio * defaultVPRegion[2]);
		vpRegion[3] = (int)(sizeRatio * defaultVPRegion[3]);
	    }
	else
	    {
		sizeRatio = 1.0;

		tileRegion[0] = defaultTileRegion[0];
		tileRegion[1] = defaultTileRegion[1];
		tileRegion[2] = defaultTileRegion[2];
		tileRegion[3] = defaultTileRegion[3];

		leaderRegion[0] = defaultLeaderRegion[0];
		leaderRegion[1] = defaultLeaderRegion[1];
		leaderRegion[2] = defaultLeaderRegion[2];
		leaderRegion[3] = defaultLeaderRegion[3];

		vpRegion[0] = defaultVPRegion[0];
		vpRegion[1] = defaultVPRegion[1];
		vpRegion[2] = defaultVPRegion[2];
		vpRegion[3] = defaultVPRegion[3];
	    }	
}
*/
