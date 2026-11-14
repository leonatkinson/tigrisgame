/* File: ExtConflictModule.java
 * Description:   parses rules objects from strings.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;
import com.tigrisgame.util.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ExtConflictModule
{
   static SearchViz viz = new SearchViz();

   int myDyn;
   Board board;
   static Sphere [] spheres = Sphere.getSpheres();
   DrawProbTable probTable = DrawProbTable.getReference();

   public ExtConflictModule( TigrisGame game )
   {
      this.board = game.getBoard();
      this.myDyn = game.getActionDynasty();
   }

   static class KingInfo
   {
      int num;
      Leader [] leaders = new Leader[ spheres.length ];      
      int [] support = new int[ spheres.length ];
      ArrayList breakLocs = new ArrayList();

      KingInfo( int num ) { this.num = num; }

      Sphere [] getConflictSpheres( KingInfo other )
      {
	 Sphere []confs = new Sphere[ spheres.length ];
	 for ( int ii=0; ii<leaders.length; ii++) {
	    if ( this.leaders[ii] != null && 
		 other.leaders[ii] != null )
	       confs[ii] = spheres[ii];
	 }	 
	 return confs;
      }

      int getNumConflicts( KingInfo other )
      {
	 int num = 0;
	 for ( int ii=0; ii<leaders.length; ii++) {
	    if ( this.leaders[ii] != null && 
		 other.leaders[ii] != null )
	       num ++;
	 }	 
	 return num;
      }
   }

   static class MergeGroup
   {
      ResolvePick struct;
      ArrayList landLocs  = new ArrayList();
      ArrayList waterLocs = new ArrayList();
      MovePlan bestPlainPlan = null;
      MovePlan bestSwapPlan = null;

      public String toString() {
	 return "LAND:" + landLocs + " WATER:" + waterLocs + " " + struct;
      }
   }

   static class PeekTile
   {
      Sphere sp;
      double prob;

      public PeekTile( Sphere sp )
      {
	  this.sp = sp;
	  this.prob = DrawProbTable.getReference().getBasicProb(sp);
      }

      public String toString() {
	  return new Character(sp.toChar()) + " " + 
	      ArrayUtils.doubleFormat.format(prob);
      }
   }

   static class ResolvePick
   {
      ResolveProb [] probs = new ResolveProb[ spheres.length ];

      public int numConfs() {
	 int cnt =0;
	 for ( int ii=0; ii<probs.length; ii++)
	    if ( probs[ii] != null )
	       cnt ++;
	 return cnt;
      }

      public String toString() {
	 StringBuffer strbuf = new StringBuffer();
	 for ( int ii=0; ii<spheres.length; ii++) {
	    if ( probs[ii] != null ) {
	       strbuf.append( spheres[ii].toChar());
	       strbuf.append( probs[ii].attacker.getDynasty());
	       strbuf.append( probs[ii].defender.getDynasty());
	    } else {
	       strbuf.append("x");
	    }
	 }
	 strbuf.append("=");
	 strbuf.append( this.getTreeStr());
	 return strbuf.toString();
      }

      public String getTreeStr()
      {
	 StringBuffer strbuf = new StringBuffer();
	 strbuf.append("[");
	 for ( int ii=0; ii<spheres.length; ii++) {
	    if ( probs[ii] != null ) {
	       strbuf.append( probs[ii].getTreeStr());
	    }
	    strbuf.append(";");
	 }
	 strbuf.append("]");
	 return strbuf.toString();
      }

   }

   static class ResolveProb
   {
      Leader attacker;
      Leader defender;
      int attackVP;
      int defendVP;
      int toTieAmount;

      ResolvePick attackWinPick;
      ResolvePick defendWinPick;

      public String getTreeStr() {
	 StringBuffer strbuf = new StringBuffer();
	 strbuf.append( attackVP + "," );
	 strbuf.append( defendVP + "," );
	 strbuf.append( toTieAmount + "," );
	 if ( attackWinPick != null ) 
	    { strbuf.append( attackWinPick.getTreeStr()); }
	 strbuf.append( ",");
	 if ( defendWinPick != null ) 
	    { strbuf.append( defendWinPick.getTreeStr()); }
	 strbuf.append( ",");
	 return strbuf.toString();
      }
   }

   public double getNet( double [] scores )
   {
      double net = 0.0;
      for ( int jj=0; jj<scores.length; jj++) {
	 if ( jj == myDyn )
	    net += scores[jj];
	 else
	    net -= scores[jj];
      }
      return net;
   }

   public double[] evalConflictPick( ResolvePick pick, TileBag[] hands,
				     PeekTile peek, Sphere joinSp )
   {
      double bestNet = Integer.MIN_VALUE+1;
      double [] bestScores = null;

      if ( pick == null ) {
	 return new double[hands.length];
      }

      for ( int ii=0; ii<pick.probs.length; ii++ ) {
	 if ( pick.probs[ii] == null )
	    continue;

	 if ( viz != null && pick.numConfs() > 1 ) {
	    viz.addChild( Move.newChooseSphere( myDyn, spheres[ii] ), null );
	 }

	 double [] scores = evalConflictProb( pick.probs[ii], hands, 
					      peek, joinSp );
	 double net = getNet( scores );

	 if ( net > bestNet ) {
	    bestNet = net;
	    bestScores = scores;
	 }

	 if ( viz != null && pick.numConfs() > 1 ) {
	    viz.ascend();
	 }
      }

      if ( viz != null && pick.numConfs() > 1 ) {
	 viz.addNote( "result= " + ArrayUtils.prettyPrint(bestScores));  

      }
      viz.addNote("net=" + ArrayUtils.doubleFormat.format(bestNet) );

      return bestScores;
   }

   public double[] evalConflictProb( ResolveProb prob, TileBag[] hands,
				     PeekTile peek, Sphere joinSp )
   {
      // amount considering current tiles (but not unknown).
      int attDyn = prob.attacker.getDynasty();
      int defDyn = prob.defender.getDynasty();
      Sphere sp = prob.attacker.getSphere();

      int adjToTieAmount = prob.toTieAmount + 
	 hands[attDyn].getNumTiles( sp ) - 
	 hands[defDyn].getNumTiles( sp ); 

      if ( sp.isSphereEqual( joinSp ) && attDyn == myDyn ) {
	 // join tile will not be available to play for support.
	 adjToTieAmount --;
      }

      int attHidden = hands[attDyn].getNumTiles( Tile.HIDDEN );
      int defHidden = hands[defDyn].getNumTiles( Tile.HIDDEN );

      double defWinProb;
      if ( peek != null && sp.isSphereEqual( peek.sp )) {
	 defWinProb = probTable.getProbGtEq( sp, attHidden, peek.prob, 
					     defHidden, adjToTieAmount );
      } else {
	 defWinProb = probTable.getProbGtEq( sp, attHidden, 
					     defHidden, adjToTieAmount );
      }

      viz.addChild( attDyn + " wins " + prob.attackVP + " " + sp + 
		    " " + defHidden + "/" + attHidden + "/" + adjToTieAmount + 
		    " peek " + peek + 
		    " prob " + ArrayUtils.doubleFormat.format(1.0-defWinProb));

      double [] attWinBase;
      if ( -adjToTieAmount >= attHidden || adjToTieAmount > 0) {
	 // impossible for attacker to win -- OR --  
	 // attacker is already winning, odds will be handled as opposed draws-
	 // but no tiles will be taken from the hidden stacks.
	 attWinBase = evalConflictPick( prob.attackWinPick, hands, 
					peek, joinSp );
      } else {
	 hands[attDyn].drawTiles( Tile.HIDDEN, -adjToTieAmount + 1 );
	 attWinBase = evalConflictPick( prob.attackWinPick, hands, 
					peek, joinSp );
	 hands[attDyn].putTiles( Tile.HIDDEN, -adjToTieAmount + 1 );
      }
      viz.ascend();

      viz.addChild( defDyn + " wins " + prob.defendVP + " " +  sp + " prob " +
		    ArrayUtils.doubleFormat.format(defWinProb));
      double [] defWinBase;
      if ( adjToTieAmount > defHidden || adjToTieAmount <= 0 ) { 
	 defWinBase = evalConflictPick( prob.defendWinPick, hands, 
					peek, joinSp );
      } else {
	 hands[defDyn].drawTiles( Tile.HIDDEN, adjToTieAmount );
	 defWinBase = evalConflictPick( prob.defendWinPick, hands, 
					peek, joinSp );
	 hands[defDyn].putTiles( Tile.HIDDEN, adjToTieAmount );
      }
      viz.ascend();

      // add the VP for winning the current fight.
      attWinBase[prob.attacker.getDynasty()] += prob.attackVP;
      defWinBase[prob.defender.getDynasty()] += prob.defendVP;

//                 "toTie " + adjToTieAmount + 
// 		  " attHid " + attHidden + 
// 		  " defHid " + defHidden +
// 		  " ATT " + ArrayUtils.prettyPrint(attWinBase) + " " +
// 		  " DEF" + ArrayUtils.prettyPrint(defWinBase) + " " );

      for ( int ii=0; ii<attWinBase.length; ii++) {
	 // WISHLIST: it is possible we may want to lose an early battle --
	 // in that case, we can choose to lose in.. 
	 // this isn't accounted for in the calculation.
	 attWinBase[ii] = attWinBase[ii]* (1.0 - defWinProb) + 
	    defWinBase[ii] * defWinProb;
      }

      viz.addNote( "result= " + ArrayUtils.prettyPrint(attWinBase));

      return attWinBase;
   }

   public TileBag[] wrapPlayers( TigrisGame g )
   {
      TileBag[] hands = new TileBag[ g.getNumPlayers() ];
      for ( int ii=0; ii<hands.length; ii++) {
	 hands[ii] = new TileBag( g.getPlayer( ii ));

	 if ( ii != myDyn )
	    hands[ii].hideTiles();
      }
      return hands;
   }

   public ResolvePick buildConflictTree( Board board,
					 HashMap kingHash, 
					 Location bridge, 
					 Location join)
   {
      if ( kingHash == null )
	 kingHash = extractKingInfo( board, null );

      int kingNum1 = 0, kingNum2 = 0;
      Kingdom [] kings = board.getAdjacentKingdoms( join );
      if ( kings.length == 2 ) {
	 kingNum1 = kings[0].getKingdomNum();	 
	 kingNum2 = kings[1].getKingdomNum();
      }
      else if ( kings.length == 1 ) {
	 kingNum1 = kings[0].getKingdomNum();	 
	 kings = board.getAdjacentKingdoms( bridge );
	 if ( kings.length == 1 ) {
	    kingNum2 = board.getAdjacentKingdoms( bridge )[0].getKingdomNum();
	 }
      }

      // are there still two attached kingdoms?
      if ( kingNum1 == 0 || kingNum2 == 0 )
	 return null;

      KingInfo king1 = (KingInfo)kingHash.get( new Integer( kingNum1 ));
      KingInfo king2 = (KingInfo)kingHash.get( new Integer( kingNum2 ));

      // are there any leaders in conflict?
      if ( king1.getNumConflicts( king2 ) == 0 )
	 return null;
      
      ResolvePick rPick = new ResolvePick();
      Sphere [] confs = king1.getConflictSpheres( king2 );
      for ( int ss=0; ss<confs.length; ss++) {
	 if ( confs[ss] == null ) continue;
	 ResolveProb rProb = new ResolveProb();
	 rPick.probs[ss] = rProb;
	 Leader lead1 = king1.leaders[ss];
	 Leader lead2 = king2.leaders[ss];
	 KingInfo attKing;
	 KingInfo defKing;

	 if ( lead1.getDynasty() == 
	      Conflict.getExternalAttacker( lead1.getDynasty(), 
					    lead2.getDynasty(),
					    myDyn )) {
	    attKing = king1;
	    defKing = king2;
	 } else {
	    attKing = king2;
	    defKing = king1;

	 }

	 rProb.attacker = attKing.leaders[ss];
	 rProb.defender = defKing.leaders[ss];
	 rProb.toTieAmount = attKing.support[ss] - defKing.support[ss];
	 Board attWinBoard = null;
	 Board defWinBoard = null;
	 if ( !spheres[ss].isSphereEqual( Sphere.RED ) && 
	      king1.getNumConflicts( king2 ) == 1 ) {
	    // no expansion shortcut.
	    rProb.attackVP = defKing.support[ss] + 1;
	    rProb.defendVP = attKing.support[ss] + 1;
	 } else {
	    attWinBoard = (Board) board.deepCopy();
	    defWinBoard = (Board) board.deepCopy();
	    rProb.attackVP = removeLosing( attWinBoard, spheres[ss], 
					   defKing.num );
	    rProb.defendVP = removeLosing( defWinBoard, spheres[ss], 
					   attKing.num );
	    attWinBoard.updateKingdoms();
	    defWinBoard.updateKingdoms();
	    rProb.attackWinPick = buildConflictTree(attWinBoard, null, 
						    bridge, join);
	    rProb.defendWinPick = buildConflictTree(defWinBoard, null, 
						    bridge, join);
	 }
      }

      return rPick;
   }

   public static int removeLosingLeader( Board board, Sphere color, 
					 int kingNum, Space sp )
   {
      if ( sp != null &&  sp.hasLeader() && 
	   board.getKingdomNum(sp) == kingNum && 
	   color.isSphereEqual( sp.getLeader().getSphere() )) {
	 board.emptyLocation( sp );
	 return 1;
      }      
      else 
	 return 0;
   }

   public static int removeLosing( Board board, Sphere color, int kingNum ) 
   {
      int rows = board.getNumRows();
      int cols = board.getNumCols();
      int total = 0;

      // peek ahead for leader so we can remove it early 
      // and use board.hasAdjacent leader.
      for ( int cc = 0; cc < cols; cc ++) {
	 total += removeLosingLeader( board, color, 
				      kingNum, board.getSpace( 0, cc ));
      }
      
      for ( int rr = 0; rr < rows; rr ++ ) {
	 // peek...
	 total += removeLosingLeader( board, color, 
				      kingNum, board.getSpace( rr, 0 ));
	 for ( int cc = 0; cc < cols; cc ++) {
	    // peek ...
	    total += removeLosingLeader( board, color,
					 kingNum, board.getSpace(rr+1,cc+1));

	    Space sp = board.getSpace( rr, cc );
	    if ( board.getKingdomNum(sp) == kingNum && 
		 sp.hasTile() && 
		 color.isSphereEqual( sp.getTile() )) {
	       // check special case with RED ...
	       if ( color.isSphereEqual( Sphere.RED ) 
		    && ( sp.hasTreasure() || 
			 board.hasAdjacentLeader( sp ))) 
		  { continue; }
	       
	       board.emptyLocation( sp );
	       total ++;
	    }
	 }
      }

      return total;
   }

   // @param mergeLocs ref to be filled with merge locs.
   // @param breakLocs ref to be filled with break pts (tiles)
   // returns HashMap of KingInfo, keyed by kingNum Integer
   public static HashMap extractKingInfo( Board board, 
					  ArrayList mergeLocs )
   {
      HashMap kingHash = new HashMap();

      // Iterate through the board, fill KingInfo and note merger spaces.
      int rows = board.getNumRows();
      int cols = board.getNumCols();
      for ( int rr = 0; rr < rows; rr ++ ) {
	 for ( int cc = 0; cc < cols; cc ++) {
	    Space sp = board.getSpace( rr, cc );

	    int kingNum = board.getKingdomNum(sp);
	    if ( kingNum > 0 ) {
	       KingInfo info = (KingInfo) kingHash.get(new Integer(kingNum));
	       if ( info == null ) {
		  info = new KingInfo(kingNum);
		  kingHash.put( new Integer(kingNum), info );
	       }

	       if ( sp.hasLeader() ) {
		  Leader lead = sp.getLeader();
		  info.leaders[ lead.getSphere().getColorCode() ] = lead;
	       }
	       else if ( sp.hasTile() ) {
		  Tile tile = sp.getTile();
		  info.support[ tile.getColorCode() ] ++;

		  if ( !sp.hasTreasure() && false ) {
		     info.breakLocs.add( sp );
		  }
	       }
	    }
	    else if ( mergeLocs != null && sp.isEmpty()) {
	       Kingdom [] kings = board.getAdjacentKingdoms( sp );
	       if ( kings.length == 2 ) { 
		  mergeLocs.add( sp );
	       }
	    }
	 }
      }
      return kingHash;
   }

   // Finds all locations that join kingdoms with D=1
   // that cause conflict involving one of dyn's leaders.
   // complexity is one iteration over the spaces.
   public HashMap getMyConflictMergeLocs( ArrayList conflictLocs )
   {
      ArrayList mergeLocs = new ArrayList();
      HashMap kingHash = extractKingInfo( board, mergeLocs );

      // Take out any mergers that don't have dyn in conflict.
      for ( Iterator ii=mergeLocs.iterator(); ii.hasNext(); ) {
	 Location loc = (Space) ii.next();
	 Kingdom [] kings = board.getAdjacentKingdoms( loc );
	 Integer key1 = new Integer( kings[0].getKingdomNum());
	 Integer key2 = new Integer( kings[1].getKingdomNum());
	 KingInfo king1 = (KingInfo) kingHash.get(key1);
	 KingInfo king2 = (KingInfo) kingHash.get(key2 );

	 for ( int ss=0; ss<spheres.length; ss++ ) {
	    if ( king1.leaders[ss] == null ||
		 king2.leaders[ss] == null ) {
	       continue;
	    }

	    if ( king1.leaders[ss].getDynasty() == myDyn || 
		 king2.leaders[ss].getDynasty() == myDyn ) {
	       conflictLocs.add( new LocationImpl(loc));
	       break;
	    }
	 }
      }

      return kingHash;
   }

   public MovePlan [] getMovePlans( TigrisGame game )
   {
      Player ply = game.getPlayer( myDyn );

      if ( game.getState() == TigrisGame.WAIT_ACTION ) {
	 ArrayList confLocs = new ArrayList();
	 HashMap kingHash = getMyConflictMergeLocs( confLocs );
	 viz.addNote( "" + confLocs );
	 
	 HashMap mergeGroups = new HashMap();
	 TileBag[] hands = wrapPlayers(game);
	 for ( Iterator jj=confLocs.iterator(); jj.hasNext(); ) {
	    Location loc = (Location) jj.next();
	    ResolvePick pk=buildConflictTree( board, kingHash,
					      null, loc );
	    String key = pk.toString();
	    MergeGroup group = (MergeGroup) mergeGroups.get( key );
	    if ( group == null ) {
	       group = new MergeGroup();
	       group.struct = pk;
	       mergeGroups.put( key, group );
	    } 
	    
	    boolean water = board.getSpace( loc ).isWater();
	    if ( water )
	       group.waterLocs.add( loc );
	    else
	       group.landLocs.add( loc );
	 }
	 
	 
	 if ( mergeGroups.size() > 0 ) {
	    viz.addNote( mergeGroups.size() + "G");
	 }
	 
	 // PLAIN MERGERS.
	 for (Iterator jj=mergeGroups.values().iterator();jj.hasNext();){
	    MergeGroup group = (MergeGroup) jj.next();
	    
	    //System.out.println(group);
	    
	    Sphere [] spheres = Sphere.getSpheres();
	    for ( int ss=0; ss<spheres.length; ss++) {
	       if ( ply.getNumTiles(spheres[ss]) == 0 &&
		    ply.getNumTiles(Tile.HIDDEN) == 0 )
		  continue;
	       
	       Location loc = null;
	       if ( ss == Sphere.BLUE_NUM && 
		    group.waterLocs.size() > 0 )
		  loc = (Location) group.waterLocs.get(0);
	       
	       else if (ss != Sphere.BLUE_NUM && 
			group.landLocs.size() > 0 ) 
		  loc = (Location) group.landLocs.get(0);
	       
	       if ( loc == null )
		  continue;
	       
	       viz.addChild
		  ( Move.newPlaceTile( myDyn, loc, spheres[ss] ), null);
	       
	       double [] rslt = evalConflictPick(group.struct, hands,
						 null, spheres[ss]);
	       
	       double net = getNet( rslt );
	       if ( group.bestPlainPlan == null || 
		    net > group.bestPlainPlan.value ) {
		  group.bestPlainPlan = new MovePlan
		     ( Move.newPlaceTile( myDyn, loc, spheres[ss] ), net );
	       }
	       
	       viz.ascend();
	    }
	 }	 
	 
	 if ( mergeGroups.size() == 0 || 
	      game.getActionNumber() != 1 ) {
	    return null;
	 }

	 viz.addChild("TACTICS");
	 viz.ascend();

	 // Swapping effects the odds in several ways.
	 // 1. attDraw will be non-zero.
	 // 2. attack may not be possible b/c no join tile.
	 // 3. attack may have to use his support as a join tile.
	 
	 for (Iterator jj=mergeGroups.values().iterator();jj.hasNext();){
	    MergeGroup group = (MergeGroup) jj.next();
	    boolean swapSix = false;
	    for ( int ss=0; ss<spheres.length; ss++) {
	       ResolveProb prob = group.struct.probs[ss];
	       // only swap for dyns we are interested in.
	       if ( prob == null || 
		    prob.attacker.getDynasty() != myDyn || 
		    ply.getNumTiles( spheres[ss])  == 6 )
		  continue;
	       
	       // placeholder for hand after swap.
	       TileBag tiles = new TileBag();
	       int keepNum = ply.getNumTiles(spheres[ss]);
	       int swapNum = 6 - keepNum;
	       tiles.putTiles( spheres[ss], keepNum );
	       hands[myDyn] = tiles;
	       
	       // only do one 'swap 6' move 
	       // (we may have 0 in more than one sphere)
	       if ( swapNum == 6 ) {
		  if ( swapSix ) continue;
		  else swapSix = true;
	       }

	       TileBag swapTiles = new TileBag( ply );
	       swapTiles.drawTiles( spheres[ss], keepNum );
	       Move move = Move.newSwapTiles( myDyn, swapTiles );

	       viz.addChild( move, null );
	       viz.addNote( "keep " + keepNum + 
			    new Character(spheres[ss].toChar()));
	       
	       double [] rslt;
	       double probMerge;
	       double probWaste;
	       // ==== MERGE WATER ONLY ====
	       if ( group.landLocs.size() == 0 ) {
		  move = Move.newPlaceTile(myDyn, 
					   (Location)group.waterLocs.get(0),
					   Sphere.BLUE );
		  viz.addChild( move, null );
		  if ( ss == Sphere.BLUE_NUM && keepNum > 0 ) {
		     // draw full amount normally.
		     // use Blue join sphere to subtract 1 from hand.
		     tiles.putTiles( Tile.HIDDEN, swapNum );
		     rslt = evalConflictPick(group.struct, hands,
						 null, spheres[ss]);
		  }
		  else {
		     // for P[no merge] eval to 0.0
		     // odds are as if draw 1 less.
		     probMerge = 1.0 -  Math.pow
			( 1.0 - probTable.getBasicProb(Sphere.BLUE), swapNum );
		     tiles.putTiles( Tile.HIDDEN, swapNum - 1);
		     rslt = evalConflictPick(group.struct, hands,
					     null, null );
		     mult(rslt, probMerge);
		     viz.addNote("probMerge " + ArrayUtils.doubleFormat.format(probMerge ));
		  }
	       }
	       // ==== MERGE LAND ONLY ====
	       else if ( group.waterLocs.size() == 0 ) {
		  move = Move.newPlaceTile(myDyn, 
					   (Location)group.landLocs.get(0),
					   Sphere.RED );
		  viz.addChild( move, null );
		  if ( spheres[ss] == Sphere.BLUE ) {
		     // fight Blue.
		     // for P[no merge] eval to 0.0
		     // odds are as if draw 1 less.
		     probMerge = 1.0 - Math.pow
			( probTable.getBasicProb(Sphere.BLUE), swapNum );
		     tiles.putTiles( Tile.HIDDEN, swapNum - 1);
		     rslt = evalConflictPick(group.struct, hands,
						 null, null );
		     mult(rslt, probMerge);
		     viz.addNote("probMerge " + ArrayUtils.doubleFormat.format(probMerge ));
		  }
		  else {
		     // fight Land.
		     // add the result of two mutually exclusive outcomes.
		     // CASE 1, assume draw another land to join.
		     // odds for rest as if draw 1 less.
		     // merge always possible in CASE 1.
		     probWaste = Math.  // prob of CASE 2.
			pow( probTable.getBasicProb(spheres[ss] ) + 
			     probTable.getBasicProb(Sphere.BLUE ), swapNum );

		     viz.addChild("CASE 1");
		     viz.addNote("(" + ArrayUtils.doubleFormat.format(1.0 - probWaste )+")");

		     tiles.putTiles( Tile.HIDDEN, swapNum - 1);
		     rslt = evalConflictPick(group.struct, hands,
						 null, null );
		     
		     // CASE 2, assume no other land possible, 
		     // altered probs for remaining full draw
		     // use this sphere to join.
		     // case discounted by p[no merge]
		     viz.ascend();
		     viz.addChild("CASE 2");
		     viz.addNote("(" + ArrayUtils.doubleFormat.format(probWaste )+")");		     

		     double [] rslt2;
		     tiles.putTiles( Tile.HIDDEN, 1 );
		     // TODO add other tables
		     // and adjust probability upwards!!
		     rslt2 = evalConflictPick(group.struct, hands,
						  null, spheres[ss] );
		     rslt2 = new double[ hands.length ];
		     probMerge = 1.0 - Math. // prob of Merge | CASE 2
			pow( probTable.getBasicProb(spheres[ss] ) / 
			     ( probTable.getBasicProb(spheres[ss] ) + 
			       probTable.getBasicProb(Sphere.BLUE )), swapNum );
		     mult( rslt2, probMerge );
		     viz.addNote("probMerge " + ArrayUtils.doubleFormat.format(probMerge ));
		     
		     mult(rslt, 1.0 - probWaste );
		     mult(rslt2, probWaste );
		     add( rslt, rslt2 );
		     viz.ascend();
		  }
	       }
	       // ==== MERGE LAND OR WATER ====
	       else {
		  move = Move.newPlaceTile(myDyn, 
					   (Location)group.waterLocs.get(0),
					   Sphere.RED );
		  viz.addChild( move, null );
		  PeekTile peek = new PeekTile( spheres[ss] );
		  // odds are draw 1 less, put peek at 1.
		  tiles.putTiles( Tile.HIDDEN, swapNum - 1 );
		  rslt = evalConflictPick(group.struct, hands,
					  peek, spheres[ss] );
	       }
	       viz.ascend();

	       double net = getNet( rslt );
	       if ( (group.bestPlainPlan == null || 
		     net - group.bestPlainPlan.value > 0.0 ) && 
		    (group.bestSwapPlan == null || 
		     net > group.bestSwapPlan.value )) {
		  group.bestSwapPlan = new MovePlan
		     ( Move.newSwapTiles( myDyn, swapTiles ), net );
	       }

	       viz.addNote("result=" + ArrayUtils.prettyPrint( rslt ));
	       viz.addNote("net=" + ArrayUtils.doubleFormat.format(net));
	       viz.ascend();
	    }
	 }
      }
      return null;
   }

   public static void main( String [] args ) 
   {
      String filename = "game.log";
      if ( args.length > 0 )
	 filename = args[0];

      try {
	 BufferedReader in = new BufferedReader
	    ( new FileReader( new File( filename)));
	 TigrisReader tr = new TigrisReader( in );

	 MoveHistory hist = tr.readMoveHistory();

	 int MAX = 200;
	 for ( int ii=0; ii< Math.min(MAX,hist.size()); ii++) {
	    TigrisGame game = hist.getGame(ii);
	    ExtConflictModule ext = new ExtConflictModule(game);
	    viz.initSearch( game );
	    viz.addNote( "" + ii + " " + game.getState());
	    System.out.println("-------------"+ ii +"------------");
	    ext.getMovePlans( game );
	 }

	 viz.wrapWithFrame();

	 synchronized( viz ) {
	    viz.wait();
	 }

      } catch (Exception ex) {
	 //System.out.println("" + ex );
	 ex.printStackTrace();
      }
   }

    public static void mult( double [] array , double val)
    {
	for ( int ii=0; ii<array.length; ii++)
	    array[ii] *= val;
    }

    public static void add( double [] array , double [] array2 )
    {
	for ( int ii=0; ii<array.length; ii++)
	    array[ii] = array[ii] + array2[ii];
    }
}

