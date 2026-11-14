/* File: ColorTextBoard.java
 * Description:  text-only ui board with ANSI color escapes.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.textui;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.TextUtils;

public class ColorTextBoard extends TigrisAnsiColor
{
   private boolean _drawGrid = true;
   private Board _board = null;
   public static String NEWLINE = System.getProperty("line.separator");

//     private Space _joinSpace = null;

   public ColorTextBoard()
   {
   }
   
   public void setDrawGrid( boolean val )
   {
      _drawGrid = val;
   }
   
   public void setBoard( Board b )
   {
      if ( b == null )
	 return;
      
      _board = b;
   }

   public String formatBoard()
   {
      StringBuffer strbuf = new StringBuffer();
      int rows = _board.getNumRows();
      int cols = _board.getNumCols();
      
      if ( _drawGrid ) { 
	 strbuf.append( getColGridString());
	 strbuf.append( NEWLINE );
      }
      
      for ( int rr=0; rr<rows; rr++) {
	 
	 // draw the grid number at the beginning of each row
	 if ( _drawGrid ) {
	    strbuf.append( new Character((char)('a' + (char)rr))+" " );
	 }
	 
	 for ( int cc=0; cc<cols; cc++ ) {
	     
	    Space sp = _board.getSpace( rr, cc );
	    Tile tt = sp.getTile();
	    Leader ll = sp.getLeader();
	    Monument mm = sp.getMonument();
	    
	    // draw tile ?
	    if ( sp.hasJoinTile() ) {
	       strbuf.append( JOIN_TILE );
	    }
	    
	    else if ( tt != null ) {
	       if ( sp.hasTreasure() )
		  strbuf.append( TREASURE_TILE );
	       else 
		  strbuf.append( getColor( tt ) + TILE );
	    }
	    // tiles and leaders are mut-ex
	    // draw leader ?
	    else if ( ll != null )
	       strbuf.append( getColor( ll.getSphere() ) 
			      + getLeader( ll.getDynasty() ) );
	    
	    // draw catastrophe
	    else if ( sp.hasCatastrophe() )
	       strbuf.append( CAT );
	    
	    // draw monument ?
	    else if ( mm != null ) {
	       
	       // draw monument
	       String s1 = getColor( mm.getFirstSphere() );
	       String s2 = getColor( mm.getSecondSphere() );
		
	       Location ulSpace = _board.getUpperLeftSpace( mm );
	       if ( ulSpace == null )
		  throw new RuntimeException
		     ("mismatch between board and space");

	       // find out where we are.
	       if ( ulSpace.isLocationEqual( sp ) ) {
		  
		  // upper left corner.
		  strbuf.append( s2 + " ");
		  if ( sp.hasTreasure() ) 
		     strbuf.append( TREASURE + "*" );
		  else
		     strbuf.append( s1 + "/");
		  strbuf.append( s1 + "-");
	       }
	       else if ( _board.getEastSpace
			 (_board.getUpperLeftSpace( mm )).
			 isLocationEqual( sp ) ) {
		  
		  // upper right corner.
		  strbuf.append( s1 + "-");
		  if ( sp.hasTreasure() ) 
		     strbuf.append( TREASURE + "*" );
		  else
		     strbuf.append( s1 + "\\");
		  strbuf.append( s2 + " ");
	       }
	       else {
		  strbuf.append( s2  + " ");
		  if ( sp.hasTreasure() ) 
		     strbuf.append( TREASURE + "*" );
		  else
		     strbuf.append( s1  + "|");
		  strbuf.append( s2  + " ");
	       }
	       
	       // also might have treasure.
	       
	    }
	    else {
	       
	       // just an ordinary space.
	       if ( sp.isWater() )
		  strbuf.append( WATER );
	       else
		  strbuf.append( LAND );
	    }
	 }
	 
	 // at back to plain for return or grid.
	 strbuf.append( PLAIN );
	 
	 // draw the grid number at the beginning of each row
	 if ( _drawGrid ) {
	    strbuf.append( " " );
	    strbuf.append( new Character((char)('a' + (char)rr))+" " );
	 }
	 
	 // at end of each row enter.
	 if ( rr != rows-1 )
	    strbuf.append( NEWLINE );
      }
      
      if ( _drawGrid ) {
	 strbuf.append( NEWLINE );
	 strbuf.append( getColGridString() );
      }
      
      return strbuf.toString();
   }
   
   public String getColGridString()
   {
//       DecimalFormat nf = new DecimalFormat();
//       nf.setMinimumIntegerDigits( 2 );
//       nf.setPositivePrefix( "" );

      int cols = _board.getNumCols();
      StringBuffer buf = new StringBuffer( ( cols + 1 )* 3 );

      if ( _drawGrid ) {
	 buf.append("  ");
	 for ( int cc=0; cc<cols; cc++ ) {
	    buf.append(" ");
	    buf.append( TextUtils.fitLeft( (cc+1)+"", 2 ));
	 }
      }
      
      return buf.toString();
   }

   // same as color text game.
   public static void main( String args[] )
      throws Exception
   {
      ColorTextGame.main( args );
   }
}

