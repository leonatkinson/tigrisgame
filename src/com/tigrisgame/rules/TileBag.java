/* File: TileBag.java
 * Description:  Collection of Tiles.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

//import java.util.*;
import java.io.Serializable;

public class TileBag implements Cloneable, TileHolder, Serializable
{
   protected int red=0, blue=0, black=0, green=0, hidden=0;
   
   public TileBag()
   {
   }
   
   public TileBag( Sphere tile )
   {
      this();
      putTile( tile );
   }

    public TileBag(int black, int red, int blue, int green)
    {
	this.black=black;
	this.red=red;
	this.blue=blue;
	this.green=green;
    }

    public TileBag( TileHolder th )
    {
	if ( th != null) {
	    this.black = th.getNumTiles( Sphere.BLACK );
	    this.red = th.getNumTiles( Sphere.RED );
	    this.blue = th.getNumTiles( Sphere.BLUE );
	    this.green = th.getNumTiles( Sphere.GREEN );
	    this.hidden = th.getNumTiles( Tile.HIDDEN );
	}
    }


    public Object clone() //throws CloneNotSupportedException
    {
  	try
  	    {
  		return super.clone();
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    // ***************************
    // TileHolder interface
    // ***************************

    public boolean hasTiles()
    {
	return (! (black == 0 && red==0 && blue==0 && green==0 && hidden == 0));
    }
    
    public int getNumTiles()
    {
	return black+red+blue+green+hidden;
    }

    public int getNumTiles( Sphere sp )
    {
	int number = 0;

	if (sp.isBlack())
	    number = black;
	else if (sp.isRed())
	    number = red;
	else if (sp.isBlue())
	    number = blue;
	else if (sp.isGreen())
	    number = green;
	else if (sp.isHidden() )
	    return hidden;
	else
	    System.out.println("Warning: TileBag.getNum invalid Sphere type " + sp );

	return number;
    }

//     public int getNumTiles( Tile tl ) 
//     {
// 	if ( tl.isHidden() )
// 	    return hidden;
// 	else
// 	    return getNumTiles( new Sphere( tl ) );
//     }

    // ***************************
    // putTile mutators
    // ***************************

    public void putTiles( TileHolder th )
    {
	this.black += th.getNumTiles( Sphere.BLACK );
	this.red += th.getNumTiles( Sphere.RED );
	this.blue += th.getNumTiles( Sphere.BLUE );
	this.green += th.getNumTiles( Sphere.GREEN );
	this.hidden += th.getNumTiles( Tile.HIDDEN );
    }

    // sugar.
//     public void putTiles( Sphere sp, int num )
//     {
// 	putTiles( new Tile( sp ), num );
//     }

    // also handles the "unknown" tile type
    public void putTiles( Sphere tl, int num )
    {
	if ( num < 0 ) {
	    // ignore this.
	    System.out.println("Warning: TileBag.putTile called " +
			       "with negative tiles " + num );
	    throw new RuntimeException();
	}

	if ( tl == null || num == 0 )
	    // do nothing
	    ;
	else if (tl.isRed())
	    red += num;
	else if (tl.isGreen())
	    green += num;
	else if (tl.isBlue())
	    blue += num;
	else if (tl.isBlack())
	    black += num;
	else if (tl.isHidden())
	    hidden += num;
	else
	    {
		// ignore this.
		System.out.println("Warning: TileBag.putTile invalid Sphere type "+ tl);
	    }
    }

    // sugar 
//     public void putTile( Sphere sp )
//     {
// 	putTiles( new Tile(sp), 1 );
//     }

    // sugar
    public void putTile( Sphere tl )
    {
	putTiles( tl, 1 );
    }

    // ***************************
    // drawTile mutators
    // ***************************
    
    public TileBag drawTiles( TileHolder th )
    {	
	if ( this.black >= th.getNumTiles( Sphere.BLACK ) &&
	     this.red >= th.getNumTiles( Sphere.RED ) &&
	     this.blue >= th.getNumTiles( Sphere.BLUE ) &&
	     this.green >= th.getNumTiles( Sphere.GREEN ) &&
	     this.hidden >= th.getNumTiles( Tile.HIDDEN ) )
	    {
		this.black -= th.getNumTiles( Sphere.BLACK );
		this.red -= th.getNumTiles( Sphere.RED );
		this.blue -= th.getNumTiles( Sphere.BLUE );
		this.green -= th.getNumTiles( Sphere.GREEN );
		this.hidden -= th.getNumTiles( Tile.HIDDEN );

		return new TileBag( th );
	    }
	else
	    {
		System.out.println("Warning: TileBag drawTiles called " +
				   "for more than availible, returning null");
		System.out.println("Warning: have " + this + " wanted " + th );
		throw new RuntimeException();
		//return null;
	    }
    }


    // candy
//     public TileHolder drawTiles( Sphere sp, int num )
//     {
// 	return drawTiles( new Tile( sp ), num );
//     }

    // candy
    public TileHolder drawTiles( Sphere tl, int num )
    {
	TileBag toDraw = new TileBag();
	toDraw.putTiles( tl, num );
	return drawTiles( toDraw );
    }

    // draw from bag at random.
    // (only you provide the seed, not fair is it?)
    // the tile is removed from the bag.
    // passing in 0.0 will draw the tiles in BRUG order.
    public Sphere drawTile( double rand )
    {
	int rnum = (int) ( rand * getNumTiles());

	if ( rnum < black ) {
	   return drawTile( Tile.BLACK );
	}
	else if ( rnum < black + red ) {
	    return drawTile( Tile.RED );
	}
	else if ( rnum < black + red + blue ) {
	    return drawTile( Tile.BLUE );
	}
	else if ( rnum < black + red + blue + green ) {
	    return drawTile( Tile.GREEN );
	}
	else if ( rnum < getNumTiles() ) {
	    return drawTile( Tile.HIDDEN );
	}
	else {
	    // the bag is empty!!
	    System.out.println("Warning: TileBag drawTile called " +
			       "with empty bag, returning null");
	    return null;
	}
    }

    // look in the bag and take out a tile of a particular sphere.
    public Sphere drawTile( Sphere sp )
    {
	Sphere tile = null;
	
	if ( sp == null)
	    // invalid, 
	    { tile = null; return null; }
	else if (sp.isRed() && red > 0)
	    { red--; tile = Sphere.RED; }
	else if (sp.isGreen() && green > 0 )
	    { green--; tile = Sphere.GREEN; }
	else if (sp.isBlue() && blue > 0 )
	    {blue--; tile = Sphere.BLUE; }
	else if (sp.isBlack() && black > 0 )
	    { black--; tile = Sphere.BLACK; }
	else if (sp.isHidden() && hidden > 0 ) 
	   { hidden--; tile = Sphere.HIDDEN; }
	else 
	    {
		System.out.println("Warning: TileBag.drawTile from " + this + 
				   " invalid or empty Sphere type " + sp  );
	    }		
	return tile;
    }
    
//     public Tile drawTile( Tile tl )
//     {
// 	if ( tl.isHidden() && hidden > 0 ) {
// 	    hidden--;
// 	    return Tile.HIDDEN;
// 	}
// 	else {
// 	    return drawTile( (Sphere) tl );
// 	}
//     }

    // ***************************
    // hidden mutators
    // ***************************

    public void revealHidden( TileHolder th )
    {
	// revealing tiles of type "hidden" has no effect,
	// they will cancel themselves out in the mutation.
	int revealNum = th.getNumTiles();
	if ( hidden >= revealNum ) {
	    putTiles( th );
	    hidden -= revealNum;
 	} else {
	    System.out.println( "Warning: TileBag.revealHidden tried to reveal " 
				+ revealNum + " amount hidden = " +  hidden );
	    throw new RuntimeException();
	}
    }
    
    public TileBag hideTiles()
    {
	return hideTiles( this );
    }

    public TileBag hideTiles( TileHolder th )
    {
	// hiding tiles of type "hidden" has no effect,
	// they will cancel themselves out in the mutation.
	int hideNum = th.getNumTiles();
	if ( this.black >= th.getNumTiles( Sphere.BLACK ) &&
	     this.red >= th.getNumTiles( Sphere.RED ) &&
	     this.blue >= th.getNumTiles( Sphere.BLUE ) &&
	     this.green >= th.getNumTiles( Sphere.GREEN ))
	    {
		drawTiles( th );
		hidden += hideNum;
	    }
	else 
	    { 
		System.out.println( "Warning: TileBag.hideTiles " +
				    "tried to hide: " + th + 
				    " more than available " + this );
		throw new RuntimeException();
	    }

	return this;
    }

    public String getLetterString()
    {
	StringBuffer strbuf = new StringBuffer( getNumTiles() );

	for ( int ii=0; ii<black; ii++ ) strbuf.append("B");
	for ( int ii=0; ii<red; ii++ ) strbuf.append("R");
	for ( int ii=0; ii<blue; ii++ ) strbuf.append("U");
	for ( int ii=0; ii<green; ii++ ) strbuf.append("G");
	for ( int ii=0; ii<hidden; ii++ ) strbuf.append("H");

	return strbuf.toString();
    }

    public String toString()
    {
	StringBuffer strbuf = new StringBuffer( "TileBag { ");

	if ( getNumTiles() == 0 ) {
	    strbuf.append( "empty" );
	} else {
	    strbuf.append( getLetterString() );
	}
	    
	strbuf.append(" }");

	// the old way.
// 	String str = "TileBag {B,R,U,G,?} = {" +
// 	    black + "," + red + "," + blue + "," + green + "," + hidden + "}";

	return strbuf.toString();
    }

    public static void main( String args[] )
    {
	TileBag th = new TileBag(5,5,5,5);
	while ( th.hasTiles() )
	    {
		System.out.println("Drew: " + th.drawTile( 0.0 ) );
	    }
    }

    public boolean equals( Object obj )
    {
	if ( obj != null && obj instanceof TileBag ) {
	    TileBag tiles = (TileBag) obj;

	    return ( this.black == tiles.black &&
		     this.red == tiles.red &&
		     this.blue == tiles.blue &&
		     this.green == tiles.green &&
		     this.hidden == tiles.hidden );
	}
	return false;
    } 
}
