/* File: DrawProbTable.java
 * Description:   static computation of draw odds given N tiles
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Rules;
import com.tigrisgame.rules.Sphere;
import com.tigrisgame.util.ArrayUtils;

public class DrawProbTable
{
   int [][] PASCAL;
   double [][] RED;
   double [][] BLUE;
   double [][] GREEN;

   private DrawProbTable()
   {
      final int SZ = 7;
      PASCAL = new int[SZ][SZ];
      PASCAL[0][0] = 1;
      for ( int ii=1; ii<SZ; ii++ ) {
	 PASCAL[ii][0] = 1;
	 for ( int jj=1; jj<=ii; jj++) {
	    PASCAL[ii][jj] = PASCAL[ii-1][jj-1] + PASCAL[ii-1][jj];
	 }
	 //System.out.println(ArrayUtils.prettyPrint( PASCAL[ii] ));
      }

      RED   = new double[SZ][SZ];
      BLUE  = new double[SZ][SZ];
      GREEN = new double[SZ][SZ];
      int totalTiles = (Rules.TOTAL_BLACK + Rules.TOTAL_RED + 
			Rules.TOTAL_BLUE + Rules.TOTAL_GREEN );
      double probRed   = (double)Rules.TOTAL_RED / (double) totalTiles;
      double probBlue  = (double)Rules.TOTAL_BLUE / (double) totalTiles;
      double probGreen = (double)Rules.TOTAL_GREEN / (double) totalTiles;
      //System.out.println( "probRed = " + probRed );
      for ( int ii=0; ii<SZ; ii++) {
	 for ( int jj=0; jj<=ii; jj++) {
 	    RED[ii][jj] = PASCAL[ii][jj] * 
	       Math.pow( probRed, jj ) * 
	       Math.pow( 1.0 - probRed, ii - jj );
 	    BLUE[ii][jj] = PASCAL[ii][jj] * 
	       Math.pow( probBlue, jj ) * 
	       Math.pow( 1.0 - probBlue, ii - jj );
 	    GREEN[ii][jj] = PASCAL[ii][jj] * 
	       Math.pow( probGreen, jj ) * 
	       Math.pow( 1.0 - probGreen, ii - jj );

	 }
      }

      double val = getBasicProb( Sphere.RED );
      System.out.print( ArrayUtils.doubleFormat.format( val ) + " " );

//        for ( int ii=-1; ii<7; ii++) {
//  	 double val = 1.0 - getProbGtEq( Sphere.RED, 6, ii );
//  	 System.out.print( nf.format( val ) + " " );
//        }
//      
//       for ( int dd=0; dd<7; dd++) {
//  	 System.out.print(dd + " " );
//  	 for ( int tt=-1; tt<6; tt ++ ) {
//  	    double val = 1.0 - getProbGtEq( Sphere.RED, dd, 6, tt );
//  	    System.out.print( ArrayUtils.doubleFormat.format( val ) + " " );
//  	 }
// 	 System.out.println();
//       }
      
//       System.out.println("\n--------------");
//       for ( int dd=0; dd<7; dd++) {
//  	 System.out.print(dd + " " );
//  	 for ( int tt=-1; tt<6; tt ++ ) {
//  	    double val = 1.0 - getProbGtEq( Sphere.RED, dd, 1.0/3.0, 6, tt );
//  	    System.out.print( ArrayUtils.doubleFormat.format( val ) + " " );
//  	 }
// 	 System.out.println();
//       }
   }

   public double getBasicProb( Sphere sp )
   {
      return getProbRow( sp, 1 )[1];
   }

   public double [] getProbRow( Sphere sp, int draw )
   {
      switch (sp.getColorCode()) {
      case Sphere.RED_NUM:
	 return RED[draw];
      case Sphere.BLUE_NUM:
	 return BLUE[draw];
      case Sphere.GREEN_NUM:
      case Sphere.BLACK_NUM:
	 return GREEN[draw];
      default:
	 return null;
      }
   }

   // get the probability the a player with NUM tiles 
   // has at least TGT tiles of the sphere SP.
   // typical use is seeing what the odds your opponent can 
   // match you needing "N to tie".
   public double getProbGtEq( Sphere sp, int defDraw, int tgt )
   {
      if ( tgt > defDraw )
	 return 0.0;
      else if ( tgt <= 0 )
	 return 1.0;

      double [] probRow = getProbRow( sp, defDraw );
      double pp = 0.0;
      for ( int ii=tgt; ii<=defDraw; ii++) {
	 pp += probRow[ii];
      }
      return pp;
   }

   public double getProbGtEq( Sphere sp, int attDraw, int defDraw, int tgt )
   {
      double pp;
      if ( tgt > defDraw )
	 pp = 0.0;
      else if ( tgt + attDraw <= 0 )
	 pp = 1.0;
      else {
	 pp = 0.0;
	 for ( int ii=0; ii<=attDraw; ii++) {
	    // for each # we might draw, what is the probability of winning?
	    pp += getProbRow( sp, attDraw )[ii] * 
	       getProbGtEq( sp, defDraw, tgt + ii );
	 }
      }
      return pp;
   }

   // @param attBonus the attacker gets to see an extra tile and choose it
   // if it is the desired color, however, the attacker still can only choose
   // a max of attDraw.  
   // For example: Fight Red, Swap 6, Join with any Tile.
   // effectively you get to see 6 tiles, but both 5 and 6 reds count as 5.
   public double getProbGtEq( Sphere sp, int attDraw, double attBonus, 
			      int defDraw, int tgt )
   {
      double pp;
      double notAttBonus = 1.0 - attBonus;
      if ( tgt > defDraw )
	 pp = 0.0;
      else if ( tgt + attDraw <= 0 )
	 pp = 1.0;
      else {
	 double[] probRow = getProbRow( sp, attDraw );

	 pp = notAttBonus * probRow[0] * getProbGtEq( sp, defDraw, tgt );
	 // convolve with P[0,1] -> [notAttDraw, attDraw]
	 for ( int ii=1; ii<=attDraw; ii++) {
	    pp += (notAttBonus * probRow[ii] + attBonus * probRow[ii-1]) * 
	       getProbGtEq( sp, defDraw, tgt + ii );
	 }
	 pp += attBonus * probRow[attDraw] * 
	    getProbGtEq( sp, defDraw, tgt + attDraw );
      }
      return pp;
   }

   static DrawProbTable s_ref;
   public static DrawProbTable getReference()
   {
      if ( s_ref == null ) {
	 s_ref = new DrawProbTable();
      }
      return s_ref;
   }
}

