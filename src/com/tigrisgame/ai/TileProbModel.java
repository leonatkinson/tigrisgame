/* File: TileProbModel.java
 * Description:  Utility class for calculating probability of tiles.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.GameOptions;
import com.tigrisgame.rules.Sphere;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class TileProbModel
{
   public static final int HAND_SIZE = 6;
   double [] probs = new double[HAND_SIZE+1]; // prob tile is "marked" color
   double sum = 1.0;
   int numTiles = 0;
   double markProb = 0.0;
   static DecimalFormat probFormat = new DecimalFormat( "#.###");

   public TileProbModel( double markProb )
   {
      probs[0] = 1.0;  // start with empty hand, certain that we have no tiles.
      numTiles = 0;
      probFormat.setMinimumIntegerDigits( 1 );
      probFormat.setMinimumFractionDigits( 3 );
      this.markProb = markProb;
   }

   // assert that less than "num" tiles are marked.
   // this would happen if a player fought and lost a conflict
   // but didn't play enough tiles to win.
   // num is the number of additional tiles the player needed 
   // (after playing any that he *did play)
   public void assertMarksLessThan( int num )
   {
      for ( int ii=num; ii<probs.length; ii++ ) {
	 probs[ii] = 0.0;
      }
      normalizeProbs();
   }

   public void playTile( boolean marked )
   {
      if ( numTiles <= 0 ) {
	 System.out.println("Warning: no tiles to draw" );
	 return;
      }
	 

      if ( marked ) {
	 leftShift( 1 );
	 normalizeProbs();

      } else {
	 probs[numTiles] = 0.0;
	 normalizeProbs();
      }
      numTiles --;
   }

   public void fillTiles()
   {
      while( numTiles < HAND_SIZE ) {
	 drawTile();
      }
   }

   public void drawTile()
   {
      if ( numTiles >= HAND_SIZE ) {
	 System.out.println("Warning: hand full, cannot draw more" );
	 return;
      }

      double noMarkProb = 1.0 - markProb;
      double [] newprobs = new double[probs.length];
      // probs[HAND_SIZE] should be ZERO now (or else inconsistant state).
      for ( int ii=0; ii<newprobs.length; ii++) {
	 newprobs[ii] = noMarkProb * probs[ii];
	 if ( ii>0 ) 
	    newprobs[ii] += markProb * probs[ii-1];
      }
      probs = newprobs;
      numTiles ++;
   }

   // used by the computer player to compute the winning percentage
   // of beating the defender in an attack.
   public double probMarksLessThan( int num )
   {
      double sum = 0.0;
      for ( int ii=0; ii<num; ii++) {
	 sum += probs[ii];
      }
      return sum;
   }

   public double getExpectedMarks()
   {
      double sum = 0.0;
      for ( int ii=1; ii<probs.length; ii++ ) {
	 sum += ii * probs[ii];
      }
      return sum;
   }


   protected void leftShift( int shift )
   {
      for ( int ii=0; ii<probs.length; ii++ ) {
	 if ( ii+shift < probs.length )
	    probs[ii] = probs[ii+shift];
	 else
	    probs[ii] = 0;
      }
   }

   protected void normalizeProbs()
   {
      double sum = 0.0;
      for ( int ii=0; ii<probs.length; ii++)
	 sum += probs[ii];

      if ( sum == 0.0 ) {
	 sum = 1.0;
	 probs[0] = 1.0;
      }

      for ( int ii=0; ii<probs.length; ii++) {
	 probs[ii] = probs[ii] / sum;
      }
   }

   public String toString()
   {
      StringBuffer strbuf = new StringBuffer();
//       for ( int ii=0; ii<probs.length; ii++) 
// 	 strbuf.append( "  " + ii + "  |" );
//       strbuf.append( System.getProperty( "line.separator" ));
      for ( int ii=0; ii<probs.length; ii++) 
	 strbuf.append(probFormat.format( probs[ii] ) + " ");
      strbuf.append( probFormat.format( getExpectedMarks()));
      strbuf.append(" / ");
      strbuf.append( numTiles );
      return strbuf.toString();
   }

   public static void main(String [] args) 
      throws Exception
   {
      Sphere sphere = Sphere.RED;
      GameOptions options = new GameOptions(); 
      if ( args.length > 0) 
	 sphere = Sphere.getSphereFromChar( args[0].charAt(0));

      double colorProb = (double) options.tiles.getNumTiles( sphere ) / 
	 (double) options.tiles.getNumTiles();
      TileProbModel model = new TileProbModel( colorProb );

      for ( int ii=0; ii<6; ii++) {
	 model.drawTile();
      }

      BufferedReader reader = new BufferedReader
	 ( new InputStreamReader( System.in ));
      while ( true ) {
	 String line = reader.readLine();
	 for ( int ii=0; ii<line.length(); ii++) {
	    char cc = line.charAt( ii );
	    switch ( cc ) {
	    case 'd':
	       model.drawTile();
	       break;
	    case 'm':
	       model.playTile( true );
	       break;
	    case 'u':
	       model.playTile( false );
	       break;
	    case 'p':
	       System.out.println("" + model );
	       break;
	    case '1':
	    case '2':
	    case '3':
	    case '4':
	    case '5':
	    case '6':
	       model.assertMarksLessThan
		  ( Integer.parseInt( ""+new Character(cc)));
	       break;
	    }
	 }
	 model.fillTiles();
	 System.out.println("" + model );
      }
   }
}
