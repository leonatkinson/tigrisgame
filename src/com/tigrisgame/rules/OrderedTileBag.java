/* File: OrderedTileBag.java
 * Description:  Tile bag that has a predetermined draw order.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;
import java.util.Random;

public class OrderedTileBag implements Cloneable, TileHolder, Serializable
{
   protected int [] tileArray; 
   protected int nextDraw = 0;
   
   public OrderedTileBag()
   {
      tileArray = new int[0];
   }
   
   public OrderedTileBag( TileHolder tiles )
   {
      tileArray = new int[ tiles.getNumTiles() ];
      
      Tile [] tileTypes = Tile.getTiles();
      int filledIndex = 0;
      for ( int ss=0; ss<tileTypes.length; ss++ )
	 for ( int ii=0; ii<tiles.getNumTiles( tileTypes[ss] ); ii ++ )
	    tileArray[filledIndex++] = tileTypes[ss].getColorCode();
   }
   
   public OrderedTileBag( int [] tiles )
   {
      this.tileArray = tiles;
   }

    public void init( OrderedTileBag tiles )
    {
	tileArray = new int[ tiles.tileArray.length ];
	for ( int ii=0; ii<tiles.tileArray.length; ii++ ) 
	    tileArray[ii] = tiles.tileArray[ii];
    }

    public Object clone() //throws CloneNotSupportedException
    {
  	try
  	    {
		OrderedTileBag copy = (OrderedTileBag) super.clone();
		copy.tileArray = (int[]) tileArray.clone();
		return copy;
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    // mostly copied from the ArrayUtils randomize for an object array.
    public void randomize( Random rand )
    {
	for ( int ii = nextDraw + 1; ii<tileArray.length; ii++ ) {
	    // want to swap element ii with a
	    //  random index from 0 to ii.
	    int swapIndex = (int) (ii * rand.nextDouble());

	    if ( swapIndex != ii ) {
		int tmp = tileArray[ ii ];
		tileArray[ ii ] = tileArray[ swapIndex ];
		tileArray[ swapIndex ] = tmp;
	    }
	}
    }

    // thread unsafe.
    public TileBag getTotalTiles()
    {
	// thread UNSAFE trick.
	int tmp = nextDraw;
	nextDraw = 0;
	TileBag totalTiles = new TileBag( this );
	nextDraw = tmp;
	
	return totalTiles;
    }

   public void setNextDraw( int index )
   {
      nextDraw = index;
   }

    // ***************************
    // TileHolder interface
    // ***************************

    public boolean hasTiles()
    {
	return ( nextDraw != tileArray.length );
    }
    
    public int getNumTiles()
    {
	return tileArray.length - nextDraw;
    }

    public int getNumTiles( Sphere sp )
    {
	return getNumTiles( new Tile( sp ));
    }

    public int getNumTiles( Tile tl ) 
    {
	int count = 0;
	int tileCode = tl.getColorCode();
	for ( int ii = nextDraw; ii < tileArray.length; ii++ ) {
	    if (tileArray[ii] == tileCode)
		count ++;
	} 
	return count;
    }

    // ***************************
    // putTile mutators
    // ***************************

    // also handles the "unknown" tile type
    public void putTiles( Sphere tl, int num )
    {
	if ( num <= 0 ) {
	    // ignore this.
	    System.out.println("Warning: OrderedTileBag.putTile called " +
			       "with non-positive tiles " + num );
	    throw new RuntimeException();
	}

	int [] moreTiles = new int [ tileArray.length + num ];

	// copy old tiles.
	System.arraycopy( tileArray, 0, moreTiles, 0, tileArray.length );
	// fill in new tiles.
	for ( int ii=0; ii<num; ii++ ) {
	    moreTiles[ ii + tileArray.length ] = tl.getColorCode();
	}

	this.tileArray = moreTiles;
    }

    // ***************************
    // drawTile mutators
    // ***************************    

    // draw the next ordered tile from bag at random.
    // the tile is removed from the bag.
    public Tile drawTile()
    {
	if ( ! hasTiles() )
	    return null;

	// return a tile object initialized to the code of the 
	// nextDraw index, then post-incremement the nextDraw.
	return new Tile( tileArray[nextDraw ++ ] );
    }

    // ***************************
    // hidden mutators
    // ***************************

    
    public OrderedTileBag hideTiles()
    {
	for ( int ii=0; ii<tileArray.length; ii++) {
	    tileArray[ii] = Sphere.HIDDEN_NUM;
	}
	return this;
    }

    public String getLetterString()
    {
	StringBuffer strbuf = new StringBuffer( getNumTiles() );

	// draw a letter for all characters, including
	// those already drawn, however mark the "next to draw"
	// with a special delimiter.
	for ( int ii=0; ii<tileArray.length; ii++ ) {

	    if ( ii == nextDraw )
		strbuf.append("-");

	    switch (tileArray[ii]) {
	    case Sphere.BLACK_NUM:
		strbuf.append( Sphere.BLACK_CHAR );
		break;
	    case Sphere.RED_NUM:
		strbuf.append( Sphere.RED_CHAR );
		break;
	    case Sphere.BLUE_NUM:
		strbuf.append( Sphere.BLUE_CHAR );
		break;
	    case Sphere.GREEN_NUM:
		strbuf.append( Sphere.GREEN_CHAR );
		break;
	    case Sphere.HIDDEN_NUM:
		strbuf.append( Sphere.HIDDEN_CHAR );
		break;
	    } 
	}

	return strbuf.toString();
    }

    public String toString()
    {
	StringBuffer strbuf = new StringBuffer( "OrderedTileBag { ");

	if ( getNumTiles() == 0 ) {
	    strbuf.append( "empty" );
	} else {
	    strbuf.append( getLetterString() );
	}
	    
	strbuf.append(" }");

	return strbuf.toString();
    }

    public static void main( String args[] )
    {
	OrderedTileBag th = new OrderedTileBag();
	while ( th.hasTiles() )
	    {
		System.out.println("Drew: " + th.drawTile() );
	    }
    }

    public boolean equals( Object obj )
    {
	if ( obj != null && obj instanceof OrderedTileBag ) {
	    OrderedTileBag ordered = (OrderedTileBag) obj;

	    if ( this.tileArray.length != ordered.tileArray.length ||
		 this.nextDraw != ordered.nextDraw )
		return false;

	    for ( int ii=0; ii<this.tileArray.length; ii++ ) {
		if ( this.tileArray[ii] != ordered.tileArray[ii] )
		    return false;
	    }
	    
	    return true;
	}
	return false;
    } 
}
