/* File: MiniMaxAI.java
 * Description:  AI with Mini-max search.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */
/* NOTES:
 * adapted from 
 * http://www.cs.caltech.edu/~petrovic/games/archex/othellodir/
 * 
 * NOTABLE DIFFERENCES:
 * 1) instead of just depth, we have actionDepth and trueDepth.
 * either one of these can cause and "end of search" condition.
 * 2) end to recursion in miniMax method instead of evaluate AND generate.
 * 3) assumptions in opp's tiles take the form of a special MM_Move that
 * includes a TileHolder that is the assumed tiles.
 */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;
import com.tigrisgame.textui.ColorTextGame;
import com.tigrisgame.util.ArrayUtils;
import com.tigrisgame.util.ResourceLoader;

import java.util.Vector;

public class MiniMaxAI
    extends DefaultGameObserver 
    implements GameObserver, Runnable
{
   public static int verbosity = 1;
   public static boolean printGen = false;
   int debugTopExamined = 0;
   int debugTopSize = 0;
   boolean abortSearch;
   
   GameServer gameServer;
   
   int maxActionDepth = 2;  // the limiting factor.
   int maxTrueDepth = 20;  // pretty big.
   int maxPruneSize = 80;  // (was 1000 for unlimited.)
   public boolean useAlphaBeta = true;
   double keepProb = 0.01;
   VPEval       evalA = new VPEval();
   LeaderEval   evalB = new LeaderEval();
   MonumentEval evalC = new MonumentEval();
   HandEval     evalD = new HandEval();
   WinnerEval   evalW = new WinnerEval();
   
   MM_Move currentPlan = null;

   // DEBUG
   public static boolean useSearchViz = false;
   SearchViz viz = null;
   
   public MiniMaxAI()
   {
      // note: (aDepth, trueDepth, prune).  the no-arg default used
      // to pass (20, 2, 80) which swapped the two depths and made
      // the search examine only 2 true moves.
      this( null, 2, 20, 80 );
   }

   public MiniMaxAI( GameServer gameServer )
   { 
      this( gameServer, 2, 20, 80 ); 
   }
   
   public MiniMaxAI( GameServer gameServer, 
		     int aDepth, int mDepth, int prune )
   { 
      setName( "MiniMaxAI");
      setEvalWeights();
      this.gameServer = gameServer; 
      this.maxActionDepth = aDepth;
      this.maxTrueDepth = mDepth;
      this.maxPruneSize = prune;
      
      if ( verbosity > 0 ) {
	 System.out.println("MiniMaxAI created with " + 
			    aDepth + " " + 
			       mDepth + " " +
			    prune );
      }
   }


   public void setEvalWeights()
   {
      // set the Evaluation defaults from G44s0
      // [2568, 798, 2, 8, 2811, 77, 1672, 65, 3, 2580]
      evalA.efficientScore = 2568;
      evalA.smallWasteScore = 798;
      evalA.largeWasteScore = 2;
      evalA.largeWasteThreshold = 8;
      evalA.treasureBonus = 2811;
      
      evalB.leaderScore = 77;
      evalB.supportScore = 1672;
      evalB.treasureScore = 65;
      evalB.potentialScore = 3;
      
      evalC.monumentScore = 2580;
   }
   
   public void setGameServer( GameServer gameServer ) 
   { 
      this.gameServer = gameServer; 
   }

   /** set the probability threshold below which assumed opponent
       support tiles are pruned from chance-node consideration
       (MultiMaxAI style moves only). */
   public void setKeepProb( double keepProb )
   {
      this.keepProb = keepProb;
   }
   
   public synchronized void gameStart( TigrisGame g )
   {
      // if we get a gameStart, 
      // discard any old game and thinking and wait
      // for another yourMove().
      super.gameStart( g );
      abortSearch = true;
   }
   
   public void moveMade( Move move )
   {
      super.moveMade( move );
      
      // update the plan

      Move nextMove = nextPlannedMove();
      
      if ( nextMove != null &&
	   nextMove.equals( move ) ){
// 	    if ( verbosity > 0 )
// 		System.out.println("the plan was RIGHT!" +
// 				   " predicting " + move + 
// 				   " advancing plan" );
	 currentPlan = currentPlan.next;
      }
      else {
// 	    if ( verbosity > 0 )
// 		System.out.println("the plan was WRONG!" +
// 				   " expected " + nextMove + 
// 				   " not " + move + 
// 				   " discarding plan" );
	 currentPlan = null;
      }
   }
   
    // get the next planned move, 
    // skip any non-move assumptions.
    // then, DO NOT advance the plan to the "next" after that. 
   public Move nextPlannedMove()
   {
      Move nextMove = null;
      while ( currentPlan != null ) {
	 if ( currentPlan.move != null ) {
	    nextMove = currentPlan.move;
	    break;
	 }
	 currentPlan = currentPlan.next;
      }
      return nextMove;
   }

   public boolean requestUndoMoves( int number )
   {
      return true;
   }
   
   Thread workerThread;
   public synchronized void yourMove()
   {
      super.yourMove();
      
      abortSearch = false;
      // start new thread on low priority to allow responsiveness to GUI.
      workerThread = new Thread( this );
      workerThread.setPriority( Thread.MIN_PRIORITY + 2 );
      workerThread.start();
   }
  
   public void run()
   {
      if ( verbosity > 0 )
	 System.out.println( "MiniMaxAI thinking...");

      TigrisGame game = getGame();
      
      // -*- viz -*-
      if ( useSearchViz ) {
	 viz = new SearchViz( (TigrisGame) game.clone() );
      }
      
      try {
	 Move move = miniMax( game );
	 synchronized( this ) {
	    if ( ! abortSearch )
	       gameServer.makeMove( this, move );
	    else {
	       if ( verbosity > 0 )
		  System.out.println("killing thinking thread");
	       throw new ThreadDeath();
	    }
	 }
      } 
      catch (IllegalMoveException ime)
	 { ime.printStackTrace(); }
      catch (ServerException se)
	 { se.printStackTrace(); }
      catch (RuntimeException re) 
	 { re.printStackTrace(); }
      catch (OutOfMemoryError oome) 
	 { 
	    System.out.println("MiniMaxAI run thread ran out of memory");
	    throw oome; 
	 }
   }
   
    public boolean maxDepthReached( MM_Info info )
    {
	int gameState = info.mmGame.game.getState();
	return ( gameState == TigrisGame.GAME_OVER ||
		 info.trueDepth >= maxTrueDepth ||
		 info.actionDepth >= maxActionDepth && 
		 gameState == TigrisGame.WAIT_ACTION );
    }

    // the most "public" way to access this algorithm, without
    // knowing anything about the internal classes
    // MM_Move and MM_Info
    public Move miniMax( TigrisGame game )
    {
	MM_Info info = new MM_Info( game );
	MM_Move mmMove = null;
	Move bestMove = null;
	long startTime = System.currentTimeMillis();
	int gameState = game.getState();

	// apply the "non-action" penatly.
	// this prevents it from exploring "maxActionDepth"
	// moves TIMES whatever artificial branch factor is here.
	if ( gameState != TigrisGame.WAIT_ACTION ) {
	    info.actionDepth ++;
	}

	// consult the plan for these subactions.
	if ( gameState == TigrisGame.WAIT_MONUMENT ||
	     gameState == TigrisGame.WAIT_TREASURE ||
	     gameState == TigrisGame.WAIT_CONFLICT_ORDER &&
	     nextPlannedMove() != null ) {
	    Move plannedMove = nextPlannedMove();
	    try {
		game.isLegal( plannedMove );
		if ( verbosity > 0 ) {
		    ColorTextGame ctg = new ColorTextGame( game );
		    System.out.print( "USING PLANNED MOVE: ");
		    System.out.println( ctg.formatMove( plannedMove ));
		}
		return plannedMove;
	    } catch (IllegalMoveException ime) {
		if ( verbosity > 0 )
		    System.out.print( "PLAN FAILED!!");
	    }
		
	}

	debugTopExamined = 0;
	mmMove = decendTree( info );
	bestMove = mmMove.move;

	// save the last plan to respond to sub actions,
	// but only if there is more than one move in the "plan".
	if ( mmMove.next != null )
	    currentPlan = mmMove;


	// ensure no divide by zero.
	long runTime = System.currentTimeMillis() - startTime + 1;
	if ( verbosity > 0 ) {
	    ColorTextGame ctg = new ColorTextGame( game );
	    System.out.print( "MOVE: ");
	    System.out.println( ctg.formatMove( mmMove.move ));
	    System.out.println( " SCORE: " + mmMove.score + 
				" NODES: " + info.nodeCount + 
				" TIME: " + runTime + " ms" +
				" RATE: " + (1000*info.nodeCount/runTime) );
	    
	    if ( verbosity > 1 ) {
		System.out.println(" --- PLAN SUMMARY --- ");
		while ( mmMove != null ) {
		    System.out.println( ""+ ctg.formatMove( mmMove.move ));
		    mmMove = mmMove.next;
		}
	    }
	}
	
	if ( bestMove == null ) {
	    System.out.println( "WARNING: MiniMaxAI returning null move" );
// 	    ResourceLoader.fileSaveObject( game, "MINIMAX_ERROR_LOCAL" );
// 	    gameServer.saveGame( "MINIMAX_ERROR_SERVER" );
// 	    throw new RuntimeException();
	}

	if ( viz != null ) {
	    viz.orderTree();
	    viz.wrapWithFrame();
	}

	return bestMove;
    }

    // decend tree decides what type of miniMax call to make
    // based on whether the branch needs assumptions for the other player, 
    // and what type are needed.
   public MM_Move decendTree( MM_Info info )
    {
	Player movePlayer = info.mmGame.game.
	    getPlayer( info.mmGame.game.getMoveDynasty() );
	
	if ( abortSearch ) {
	    if ( verbosity > 0 )
		System.out.println("killing thinking thread");
	    throw new ThreadDeath();
	}

// 	for ( int ii=0; ii<info.trueDepth; ii++)
// 	    System.out.print(" . " );
// 	System.out.println("decend");

	if ( verbosity > 0 &&
	     info.nodeCount > 0 &&
	     info.nodeCount % 100000 == 0 ) 
	    System.out.println(" ... still thinking " + 
			       " PCT DONE: " + 
			       (int)(debugTopExamined*100/debugTopSize) +
			       "% (" + debugTopExamined + " of " + 
			       debugTopSize + ")" +
			       " NODES: " + info.nodeCount + " " );

	MM_Move rMove = null;
	// only if there is a conflict and
	// we don't know about the player's hand.
	if ( ! info.assumptionMade &&
	     ( info.mmGame.game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT ||
	       info.mmGame.game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT )&&
	     movePlayer.getNumTiles( Sphere.HIDDEN ) != 0 )
	    {
		rMove = chanceMiniMax( info );
	    }
	else 
	    {
		rMove = miniMax( info );
	    }

// 	for ( int ii=0; ii<info.trueDepth; ii++)
// 	    System.out.print(" . " );
// 	System.out.println("ascend");

	return rMove;
    }

   public MM_Move miniMax( MM_Info info )
    {
	MM_Move bestResult;

	// debug
	int node = info.nodeCount ++;

	// use the static evaluation if at a leaf.
	// bestResult will have 
	// move == "null"
	// next == "null"
	if ( maxDepthReached( info ) ) {
	    bestResult = new MM_Move( evaluateGame( info.mmGame ));
	}

	// otherwise, use miniMax recusively
	else {
	    bestResult = new MM_Move();

	    // parity is defined by who's move it is, 
	    // player 0 will try to have high positive, 1 high negative.
	    if ( info.mmGame.game.getMoveDynasty() == 0 ) {
		bestResult.score = Integer.MIN_VALUE + 1;
		info.validateAlpha();
	    }
	    else {
		bestResult.score = Integer.MAX_VALUE;
		info.validateBeta();
	    }

	    if ( viz != null ) {
// 		if ( info.mmGame.game.getMoveDynasty() == 0 ) 
		    viz.addNote( "alpha= " + info.alpha );
// 		else
		    viz.addNote( "beta= " + info.beta );
	    }

	    // generateMoves returns an array of MM_Moves possibly "sorted"
	    MM_Move [] moveArray = generateMoves( info );
	    MM_Game origGame = info.mmGame;

	    // check for error condition if generateMoves broken, 
	    // otherwise set default best move to the first one;
	    // this is usefull is all moves immediatly lose 
	    // (i.e. none better than worst possible).
	    // also may be usefull if we want to abort early (?)
	    if ( moveArray.length == 0 ) 
		System.out.println( "WARNING: MiniMaxAI " +
				    "0 moves generate move" );
	    else 
		bestResult.move = moveArray[0].move;

	    // check if top level move and only one choice.
	    // if so, return that move immediatly.
	    // we do need to explore otherwise, because
	    // we must compare nodes at equal depths.
	    if ( moveArray.length == 1 && 
		 info.trueDepth == 0 )
		return moveArray[ 0 ];

	    if ( info.trueDepth == 0 )
		debugTopSize = moveArray.length;

	    for ( int ii=0; ii < moveArray.length; ii++ ) {

		if ( verbosity > info.trueDepth + 1) {
		    ColorTextGame ctg = new ColorTextGame( info.mmGame.game );
		    printDebug( info, "DECENDING " + 
				ctg.formatMove( moveArray[ii].move ) );
		}		

		MM_Move result = null;
		try {

		    // Gt_move(&current_mimx_data, &temp_move, &temp_unmove);
		    applyMove( info, moveArray[ii] );
		    
		    // dec_tree( &current_mimx_data, &temp_score);
		    result = decendTree( info );
		    
		    // Gt_unmove(&current_mimx_data,&temp_unmove);
		    undoMove( info, moveArray[ii] );

// 		    if ( info.trueDepth == 0 )
// 			System.out.println("INFO " + info );


		} catch (IllegalMoveException ime) {
		    System.out.println("WARNING: miniMax " +
				       "tried to make illegal move");
		    printGameAndMove( info, moveArray[ii] );
		    continue;
		} catch (Exception ex) {
		    ResourceLoader.fileSaveObject( info.mmGame.game, 
						   "MINIMAX_EXCEPTION" );
		    printGameAndMove( info, moveArray[ii] );
		    ex.printStackTrace();
		    throw new RuntimeException(ex);
		}

		if ( verbosity > info.trueDepth + 1) {
		    printDebug( info, 
				(ii+1) + "/" + moveArray.length + 
				" score=" + result.score +
				" best=" + bestResult.score );
		}

		if ( info.trueDepth == 0 )
		    debugTopExamined ++;

		if ( info.mmGame.game.getMoveDynasty() == 0 ) {
		    // Positive is GOOD for MAX branch.
		    if ( result.score > bestResult.score ) {
			bestResult.score = result.score;
			bestResult.next = result;
			bestResult.move = moveArray[ii].move;
		    }
		    if ( result.score > info.alpha.value ) {
			info.alpha.value = result.score;
		    }
		    if ( info.beta != null &&
			 result.score >= info.beta.value ) {
			// CUT !!
			if ( viz != null ) {
			    viz.addNote("BETA CUT!!!",
					viz.currentNode.
					getLastChild() );
			}
			if ( useAlphaBeta )
			    return bestResult;
		    }
		} else {
		    // Negative is GOOD for MIN branch.
		    if ( result.score < bestResult.score ) {
			bestResult.score = result.score;
			bestResult.next = result;
			bestResult.move = moveArray[ii].move;
		    }
		    if ( result.score < info.beta.value ) {
			info.beta.value = result.score;
		    }
		    if( info.alpha != null && 
			result.score <= info.alpha.value ) {
			// CUT !!
			if ( viz != null )
			    viz.addNote("ALPHA CUT!!!",
					viz.currentNode.
					getLastChild());
			if ( useAlphaBeta )
			    return bestResult;
		    }
		}
	    }
	} // end if (maxDepth) ... else ...

// 	if ( info.trueDepth == 0 )
// 	    System.out.println("INFO " + info );

	// -*- viz -*-
	if ( viz != null ) {
	    viz.setScore( bestResult.score );
	}

	return bestResult;
    }

    // chanceMiniMax operates on a node that has successors
    // with mutually exclusive random outcomes that all sum to one.
    // the returned score is thus the average of the outcomes.
    // the returned move, while only needed for "plan" visualization,
    // is the move with the highest probability.
    // PRUNING can be done on this node that involve the bounds of evaluations,
    // i.e. you lose (score = -INF) 51% of the time and you have a draw on 
    // another branch you can cease evaluating.  
    // To help speed up the search, you could also order the
    // branches highest to lowest priority.
   public MM_Move chanceMiniMax( MM_Info info )
    {
	MM_Move avgResult;
	double scoreSum = 0.0;

	// a chance node invalidates the alpha-beta from above,
	// so we invalidate belowcreate a new instance in the current mm_game
	info.alpha = null;
	info.beta = null;

	// debug
	int node = info.nodeCount ++;

	// use the static evaluation if at a leaf.
	// bestResult will have 
	// move == "null"
	// next == "null"
	if ( maxDepthReached( info ) ) {
	    avgResult = new MM_Move( evaluateGame( info.mmGame ));
	}

	// otherwise, use miniMax recusively
	else {
	    avgResult = new MM_Move();

	    // generateMoves returns an array of MM_Moves possibly "sorted"
	    MM_Move [] moveArray = generateAssumeMoves( info );
	    MM_Game origGame = info.mmGame;

	    if ( info.trueDepth == 0 )
		debugTopSize = moveArray.length;

	    for ( int ii=0; ii < moveArray.length; ii++ ) {
		
 		if ( verbosity > info.trueDepth + 1) {
		    ColorTextGame ctg = new ColorTextGame( info.mmGame.game );
		    printDebug( info, "DECENDING " + 
				moveArray[ii].assumedTiles );
		}		

		MM_Move result = null;
		try {

		    // Gt_move(&current_mimx_data, &temp_move, &temp_unmove);
		    applyMove( info, moveArray[ii] );
		
		    // dec_tree( &current_mimx_data, &temp_score);
		    result = decendTree( info );
		    
		    // Gt_unmove(&current_mimx_data,&temp_unmove);
		    undoMove( info, moveArray[ii] );

		} catch (IllegalMoveException ime) {
		    System.out.println("WARNING: chanceMiniMax " +
				       "tried to make illegal move");
		    printGameAndMove( info, moveArray[ii] );
		    throw new RuntimeException(ime);
		}

 		if ( verbosity > info.trueDepth + 1 ) {
		    printDebug( info, 
				(ii+1) + "/" + moveArray.length + 
				" score=" + result.score +
				" avg score=" + 
				(int)(scoreSum/moveArray.length) );
		}		


 		if ( verbosity > info.trueDepth + 1) {
		    ColorTextGame ctg = new ColorTextGame( info.mmGame.game );
		    System.out.println("at chance node = " + node + " move " +
				       (ii+1) + "/" + moveArray.length + " " +
				       ctg.formatMove( moveArray[ii].move ) );
		    System.out.println(" worth " + moveArray[ii].score +
				       " trueDepth = " + info.trueDepth + 
				       " actionDepth = " + info.actionDepth + 
				       " score returned = " + result.score +
				       " probability = " + 
				       moveArray[ii].probability +
				       " avg result so far = " + 
				       (int)(scoreSum/moveArray.length) +
				       " nodecount= " + info.nodeCount );
		}		

		if ( info.trueDepth == 0 )
		    debugTopExamined ++;

		if ( moveArray[ii].probability != null )
		    scoreSum += result.score * 
			moveArray[ii].probability.doubleValue();
		else 
		    scoreSum += result.score;

		// this stores the "most probable" successor 
		// for the plan, which is the first choice.
		if ( avgResult.move == null ) {
		    avgResult.move = moveArray[ii].move;
		    avgResult.next = result;
		}

	    } // end for ( 0 < ii < moveArray.length )

	    avgResult.score = (int) (scoreSum);

	} // end if (maxDepth) ... else ...

	// -*- viz -*-
	if ( viz != null ) {
	    viz.setScore( avgResult.score );
	}

	return avgResult;
    }

    // returns a list of moves pre-evaluated with the static eval.
    MM_Move [] generateMoves( MM_Info info )
    {
	Move [] moves = null;
	MM_Move [] scoredMoves = null;
	Sphere [] spheres = Sphere.getSpheres();

	TigrisGame game = info.mmGame.game;
	int gameState = game.getState();
	
	switch ( gameState ) {
	case TigrisGame.WAIT_ACTION:
	    // leader moves: all legal
	    // tile moves: add to kingdom, assume player has tile.
	    // swap moves: swap all but one sphere.
	    // cat moves: cat over tile.

	    moves = LegalMoveGenerator.getLeaderMoves( game );
	    MM_Move [] leaderMoves = wrapMoves( moves, 0 );
	    moves = BetterMoveGenerator.getCatOverTileMoves( game );
	    MM_Move [] catMoves = wrapMoves( moves, 0 );
	    moves = BetterMoveGenerator.getSwapButSphereMoves( game );
	    MM_Move [] swapMoves = wrapMoves( moves, 0 );

	    MM_Move [][] tileMoves = new MM_Move[spheres.length][0]; 
	    for ( int ss=0; ss<spheres.length; ss++ ) {
		TileBag assumed = new TileBag();
		Move [] addToKingMoves = BetterMoveGenerator.
		    getTileAddToKingdomMoves( game, spheres[ss] );
		Move [] conflictMoves = BetterMoveGenerator.
		    getPairwiseConflictMoves( game, spheres[ss] );

		moves = (Move []) ArrayUtils.
		    mergeArrays( addToKingMoves, conflictMoves );

		// assume with 100% probability that
		// they have at least one tile (if necessary)
		if ( game.getPlayer( game.getMoveDynasty()).
		     getNumTiles( spheres[ss] ) < 1 )
		    assumed.putTile( spheres[ss] );

		tileMoves[ss] = wrapMoves( moves, 0, assumed, null );
	    }

	    scoredMoves = (MM_Move []) ArrayUtils.
		mergeArrays( new MM_Move[][] 
		    { leaderMoves, catMoves, swapMoves, 
		      tileMoves[0], tileMoves[1], 
		      tileMoves[2], tileMoves[3] });


	    break;
	case TigrisGame.WAIT_ATTACK_SUPPORT:
	case TigrisGame.WAIT_DEFEND_SUPPORT:
	    // only when we are immediatly presented with support moves
	    // do we think about how many to play.
// 	    if ( info.trueDepth == 0 ) {
// 		scoredMoves = wrapMoves( LegalMoveGenerator.
// 					 getMoves( game ), 0 );
// 		break;
// 	    }
	case TigrisGame.WAIT_TREASURE:
	case TigrisGame.WAIT_MONUMENT:
	case TigrisGame.WAIT_CONFLICT_ORDER:
	case TigrisGame.GAME_OVER:
	    scoredMoves = wrapMoves( BetterMoveGenerator.
				     getMoves( game ), 0 );
	    break;
        default:
	    System.out.println("unknown game state " +
			       game.getState());

	}


	// print all the moves
	if ( verbosity > info.trueDepth && printGen ) {
	    System.out.println( "GENERATED MOVES: ");
	    printMoves( info, scoredMoves );
	}

	// Pre-score the root candidates with a static evaluation of the
	// successor position and order them best-first.  This is done only
	// at the root: a single pass over the candidate moves gives alpha-beta
	// a strong opening bound, which prunes weaker branches throughout the
	// rest of the tree.  Pre-scanning at every node would dominate the
	// search (it is cheaper to explore a subtree than to statically eval
	// every sibling at every level), so deeper levels keep their
	// generation order and rely on the bound propagated from the root.
	if ( info.trueDepth == 0 && scoredMoves != null && scoredMoves.length > 0 ) {
	    boolean maxNode = ( game.getMoveDynasty() == 0 );
	    for ( int ii = 0; ii < scoredMoves.length; ii++ ) {
	        Move candidate = scoredMoves[ii].move;
	        if ( candidate == null ) {
	            scoredMoves[ii].score = maxNode
	                ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE;
	            continue;
	        }
	        TigrisGame successor;
	        try {
	            successor = (TigrisGame) game.clone();
	            successor.makeMove( candidate );
	        } catch (IllegalMoveException ime) {
	            // LegalMoveGenerator should only emit legal moves.
	            scoredMoves[ii].score = maxNode
	                ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE;
	            continue;
	        }
	        scoredMoves[ii].score = evaluateGame( new MM_Game( successor ));
	    }

	    // best moves first: MAX (player 0) wants high scores first,
	    // MIN (player 1) wants low scores first (bad for player 0).
	    if ( maxNode ) {
	        ArrayUtils.sortDescending( scoredMoves, MM_Move.SORTER );
	    } else {
	        ArrayUtils.sortAscending( scoredMoves, MM_Move.SORTER );
	    }
	}

	// to limit the branch factor we return at most "maxPruneSize" moves.
	scoredMoves = (MM_Move []) ArrayUtils.trimToSize( scoredMoves,
	                          maxPruneSize );

	if ( verbosity > info.trueDepth && printGen ) {
	    System.out.println( "PRUNED MOVES: ");
	    printMoves( info, scoredMoves );
	}

	return scoredMoves;
    }

    // called in a WAIT_SUPPORTERS state from chanceMiniMax.
    // DEFENCE will return:
    // assume = 0 with prob tiles = [ 0, tie - 1 ]
    // assume = tie with prob tiles = [ tie, max ]
    // ATTACK will return:
    // if defender's tiles fully know (the case without swap or depth>3)
    // we can figure out amount that will always win.
    // assume = 0 with prob tiles = [ 0, minwin - 1 ]
    // assume = minwin to maxwin -1 with each with prob tiles = amount
    // assume = maxwin with prob tiles = [ maxwin, max ]
    MM_Move [] generateAssumeMoves( MM_Info info )
    {
	Vector assumptions = new Vector();
	assumptions.addElement( new TileBag() );

	TigrisGame game = info.mmGame.game;
	Player movePlayer = game.getPlayer( game.getMoveDynasty() );
	Conflict conflict = game.getConflict();
	Sphere supportSphere = conflict.getSupportSphere();
	int alreadyHave = movePlayer.getNumTiles( supportSphere );

	if ( conflict.getAttackScreenSupport() == -1 ) {
	    // ******************************
	    // the ATTACKER's turn for support.
	    // ******************************
	    Player defendPlayer = game.getPlayer( conflict.getDefender() );

	    // the min and max amount to GUARANTEE a win.
	    int minWinAmount = conflict.getDefendBoardSupport() +
		defendPlayer.getNumTiles( supportSphere ) - 
		conflict.getAttackBoardSupport() + 1;
	    int maxWinAmount = minWinAmount + 
		defendPlayer.getNumTiles( Sphere.HIDDEN );

// 	    if ( verbosity > info.trueDepth ) {
// 		System.out.println("generateAssumeMoves() for ATTACKER" +
// 				   " minWinAmount " + minWinAmount +
// 				   " maxWinAmount " + maxWinAmount );
// 	    }

	    for ( int ii=Math.max( minWinAmount - alreadyHave, 1 ); 
		  ii<= ( maxWinAmount - alreadyHave ); ii++ ) {
		TileBag tiles = new TileBag();
		tiles.putTiles( supportSphere, ii );
		assumptions.addElement( tiles );
	    }
	} 
	else {
	    // ******************************
	    // the DEFENDER's turn for support.
	    // ******************************
	    int tieAmount = conflict.getNumTilesToTie();

// 	    if ( verbosity > info.trueDepth ) {
// 		System.out.println("generateAssumeMoves() for DEFENDER" +
// 				   " tieAmount " + tieAmount );
// 	    }


	    // tie amount requires MORE tiles to be assumed AND
	    // tie amount is possible with the number of tiles
	    if ( tieAmount > alreadyHave &&
		 tieAmount <= ( movePlayer.getNumTiles( Sphere.HIDDEN ) +
				alreadyHave ))
		{
		    TileBag nonZeroAssumption = new TileBag();
		    nonZeroAssumption.putTiles( supportSphere, 
						tieAmount - alreadyHave );
		    assumptions.addElement( nonZeroAssumption );
		}
	}

// 	if ( verbosity > info.trueDepth ) {
// 	    System.out.println("before scording/pruning " +
// 			       assumptions );
// 	}

	MM_Move [] scoredMoves = new MM_Move[ assumptions.size() ];	
	for ( int ii=0; ii<scoredMoves.length; ii++ ) {
	    TileHolder assumedTiles = (TileHolder) assumptions.elementAt(ii);
	    scoredMoves[ii] = new MM_Move( assumedTiles );
	}

	assignProbability( info, scoredMoves );

	// by sorting, we put the most probable moves towards the front.
	ArrayUtils.sortDescending( scoredMoves, MM_Move.SORTER );

	// pruning all moves below "keepProb" probability.
	// find out how many that is, but iterating through the list
	// high prob to low prob, if below break.
	// if above, increment pruneSize to include that move.
	// this also has the effect of removing "impossible" moves p=0.0
	int pruneSize = 0;
	for ( int ii=0; ii<maxPruneSize && ii<scoredMoves.length; ii++ ) {
	    if ( scoredMoves[ii].probability.
		 doubleValue() < keepProb )
		break;
	    else 
		pruneSize ++;
	}
	scoredMoves = (MM_Move []) ArrayUtils.trimToSize( scoredMoves,
							  pruneSize );

	// print all the moves
// 	if ( verbosity > info.trueDepth ) {
// 	    System.out.println( "ASSUMED PRUNED MOVES: ");
// 	    printMoves( info, scoredMoves );
// 	}

	return scoredMoves;
    }

    void applyMove( MM_Info info, MM_Move mmMove )
	throws IllegalMoveException
    {
	// -*- viz -*-
	if ( viz != null ) {
	    viz.addChild( mmMove.move, mmMove.assumedTiles );
	    if ( mmMove.probability != null )
		viz.addNote( "prob=" + mmMove.probability );
	    viz.addNote( "depth=" + info.actionDepth + ":" + 
			 info.trueDepth );
	}

// 	System.out.println("applying move at depth (" + info.trueDepth +
// 			   ") " + mmMove.move );

	// decend one true depth (includes assumptions and subactions)
	info.trueDepth ++;

	// apply the assumption of tiles drawn.
	if ( mmMove.assumedTiles != null ) {
	    Player player = info.mmGame.game.
		getPlayer( info.mmGame.game.getMoveDynasty() );
	    player.revealHidden( mmMove.assumedTiles );
	} 
	
	if ( mmMove.move != null ) {
	    // also bookkeep "action depth".
	    // action depth only includes action moves made (not assumed)
	    if ( info.mmGame.game.getState() == TigrisGame.WAIT_ACTION )
		info.actionDepth ++;
	    
	    // create undo information.
	    mmMove.unmove = new MM_Game( info.mmGame );
	    
	    // modify the first game info.
	    info.mmGame.game.makeMove( mmMove.move );

	    // no "assumption only" move.
	    info.assumptionMade = false;
	} else {
	    info.assumptionMade = true;
	}

// 	System.out.println("apply move CURRENT " + 
// 			   info.alpha + "," + info.beta );

	// setup alpha beta undo.
	mmMove.unAlpha = info.alpha;
	mmMove.unBeta = info.beta;
    }

    void undoMove( MM_Info info, MM_Move mmMove )
    {
	// -*- viz -*-
	if ( viz != null ) {
	    viz.ascend();
	}
	
// 	System.out.println("undo move CURRENT " + 
// 			   info.alpha + "," + info.beta + " OLD " +
// 			   mmMove.unAlpha + "," + mmMove.unBeta );

	// alpha beta undo.
	info.alpha = mmMove.unAlpha;
	info.beta = mmMove.unBeta;

	if ( mmMove.unmove != null ) {
	    // recover the game before the move was applied.
	    info.mmGame = mmMove.unmove;
	    
	    // ascend one action depth, 
	    // if the original game state was an action.
	    if ( info.mmGame.game.getState() == TigrisGame.WAIT_ACTION )
		info.actionDepth --;

	    info.assumptionMade = false;
	} else {
	    // this was an "assuption only" move
	    info.assumptionMade = true;
	}
	
	if ( mmMove.assumedTiles != null ) {
	    Player player = info.mmGame.game.
		getPlayer( info.mmGame.game.getMoveDynasty() );
	    player.hideTiles( mmMove.assumedTiles );
	}

	// ascend one true depth.
	info.trueDepth --;
    }

    int evaluateGame( MM_Game mmGame )
    {
	if ( mmGame.game.getState() == TigrisGame.GAME_OVER ) {
	    // GAME_OVER eval.
	    return evalW.evaluate( mmGame.game, 0 );
	}
	else {
	    // IN PROGESS eval
 	    int scoreZero = 
		evalA.evaluate( mmGame.game, 0 ) +
		evalB.evaluate( mmGame.game, 0 ) +
		evalC.evaluate( mmGame.game, 0 ) +
		evalD.evaluate( mmGame.game, 0 );
	    int scoreOne  = 
		evalA.evaluate( mmGame.game, 1 ) + 
		evalB.evaluate( mmGame.game, 1 ) +
		evalC.evaluate( mmGame.game, 1 ) +
		evalD.evaluate( mmGame.game, 1 );
	    
	    // zero tries to get higher, one lower.
	    // we also set the "score" field in the game just in case we are
	    // using this for pre-eval at generation to sort by "promisingness"
	    mmGame.score = scoreZero - scoreOne;
	    return mmGame.score;
	}
    }

    // assigns the mutually exclusive probabilities 
    // such that the sum it one.
    // calculates the probability of each of the
    // assumptions from 0 to <max-hidden>
    // then drops each of these probabilities into the right move "bin".
    // (see generate assume moves).
    // ASSUMES: moves only have ONE sphere assumed, 
    // AND that sphere is the support sphere for the current conflict 
    // AND moves ordered by number assumed (low to high)
   public void assignProbability( MM_Info info, MM_Move [] moves )
    {
	TigrisGame game = info.mmGame.game;

	// Stuff for BETTER PROBABILITY METRICS
// 	Sphere supportSphere = game.getConflict().getSupportSphere();
// 	TileHolder totalTiles = game.getTotalTiles();
// 	double tileProb = totalTiles.getNumTiles( supportSphere ) /
// 	    totalTiles.getNumTiles();

	if ( moves.length == 1 )
	    moves[0].probability = new Double(1.0); // 1.0 certain.
	else {

	    // DEBUG.
// 	    System.out.println( "ASSIGN PROBABILITY INPUT ");
// 	    System.out.println( "HIDDEN = " + 
// 				game.getPlayer( game.getMoveDynasty() ).
// 				getNumTiles( Sphere.HIDDEN ));
// 	    printMoves( info, moves );
	    // END DEBUG

	    int max = game.getPlayer( game.getMoveDynasty() ).
		getNumTiles( Sphere.HIDDEN );
	    int moveIndex = 0;

	    // init all moves to zero probability for summing.
	    for ( int ii=0; ii<moves.length; ii++ ) {
		moves[ ii ].probability = new Double( 0.0 );
	    }

	    for ( int ii=0; ii<=max; ii++ ) {
		// uniform distribution.
		double prob = 1.0 / (max+1);

		// TODO: BINOMIAL DISTRIBUTION. 
		// (maybe whip out SAMPLING PROBS)

		// DEBUG.
// 		System.out.println( "ii = " + ii +
// 				    " moveIndex = " + moveIndex +
// 				    " assumed for this move " + 
// 				    moves[ moveIndex ].assumedTiles.
// 				    getNumTiles() );
		// END DEBUG.

		// increment the probability of the range we are in.
		// assign everything into the lowest bin that assumes
		// each at least that many tiles.
		// (therefore) all probability over past the last move 
		// will be assigned to the highest assumption.
		// also be sure to not increment anything past the 
		// max possible assumption.

		if ( moveIndex < moves.length - 1 && 
		     moves[ moveIndex + 1 ].assumedTiles.getNumTiles() == ii )
		    moveIndex ++;

		// DEBUG.
// 		System.out.println( "incrementing " + moveIndex + 
// 				    " by " + prob );
		// END DEBUG.

		// increment the "Double" (assuming non-null).
		moves[ moveIndex ].probability = new 
		    Double( moves[ moveIndex ].probability.
			    doubleValue() + prob );
	    }

	    // DEBUG.
//   	    System.out.println( "ASSIGN PROBABILITY OUTPUT ");
//   	    printMoves( info, moves );
	    // END DEBUG
	}
    }

   public static MM_Move [] wrapMoves( Move [] moves, int score )
    {
	return wrapMoves( moves, score, null, null );
    }

   public static MM_Move [] wrapMoves( Move [] moves, int score,
					TileHolder assumedTiles, Double prob )
    {
	MM_Move [] wrappedMoves = new MM_Move[ moves.length ];
	for ( int ii=0; ii<moves.length; ii++ ) {
	    wrappedMoves[ii] = new MM_Move();
	    wrappedMoves[ii].move = moves[ii];
	    wrappedMoves[ii].score = score;
	    wrappedMoves[ii].assumedTiles = assumedTiles;
	    wrappedMoves[ii].probability = prob;
	}
	return wrappedMoves;
    }

    public static void printDebug( MM_Info info, String msg  )
    {
	String depthPad = getDepthPad( info.trueDepth );

	System.out.println( depthPad + msg );
    }

    public static void printGameAndMove( MM_Info info, MM_Move move )
    {
	ColorTextGame ctg = new ColorTextGame( info.mmGame.game );
	String depthPad = getDepthPad( info.trueDepth );

	System.err.println( "GAME: ");
	System.err.println( ctg.formatGame());
	System.err.println( "INFO: ");
	System.err.println( "" + info );
	System.err.println( "MOVE: " + ctg.formatMove( move.move ) + 
			    " ASSUME: " + move.assumedTiles );
    }

    public static void printMoves( MM_Info info, MM_Move [] moves  )
    {
	ColorTextGame ctg = new ColorTextGame( info.mmGame.game );
	String depthPad = getDepthPad( info.trueDepth );

	for ( int ii=0; ii<moves.length; ii++)
	    System.out.println( depthPad + 
				(ii+1) + "/" + moves.length + " " +
				ctg.formatMove( moves[ii].move ) +
				" score=" + moves[ii].score + 
				" assumed=" + moves[ii].assumedTiles +
				" prob=" + moves[ii].probability );
	if ( moves.length == 0 )
	    System.out.println( depthPad + "NONE");
    }

    public static String getDepthPad( int depth )
    {
	String depthPad = "";
	for ( int ii=0; ii<depth; ii++)
	    depthPad += " . ";
	return depthPad;
    }

    public static void main( String [] args)
	throws IllegalMoveException
    {
	int depth = 1;
	TigrisGame game = null;
	MiniMaxAI mimx = new MiniMaxAI();
	ArrayUtils.rand.setSeed( 2 );
	long start = System.currentTimeMillis();

	if ( args.length > 0 )
	    depth = Integer.parseInt( args[0] );
	if ( args.length > 1 ) {
	    Object obj = ResourceLoader.fileLoadObject( args[1] );
	    if ( obj == null )
		System.out.println("couldn't load from " + args[1]);
	    else if ( obj instanceof TigrisGame )
		game = (TigrisGame) obj;
	    else if ( obj instanceof MoveHistory ) 
		game = ((MoveHistory)obj).getGame();
	}
 	if ( args.length > 2 )
 	    ArrayUtils.rand.setSeed( Integer.parseInt( args[2] ));

	if ( game == null ) {
	    GameOptions options = new GameOptions();
	    options.seed = 20;
	    game = new StandardGame( options );
	}

	mimx.maxActionDepth = depth;
	//mimx.maxTrueDepth = depth;
	mimx.verbosity = 0;
	Move move = mimx.miniMax( game );
	long end = System.currentTimeMillis();
	System.out.println("result is " + move + 
			   " in " + (end-start) + " ms");

	// HOW TO USE A SMALL MAP.
// 	GameOptions options = new GameOptions();
// 	options.tiles = new TileBag( StandardGame.TOTAL_BLACK/2, 
// 				     StandardGame.TOTAL_RED/2-5,
// 				     StandardGame.TOTAL_BLUE/2,
// 				     StandardGame.TOTAL_GREEN/2);
// 	options.mapfile = "small_map.txt";
    }
}

// ****************************
// DATA STRUCTURES
// ****************************

// class MM_Exception extends Exception
// {
//     public MM_Exception( String msg ) { super( msg ); }
// }

// a "mutable" integer 
class AlphaBetaValue { 

    // alpha is the best score seen for player zero (MAX)
    // beta  is the best score seen for player one (MIN)
    // a-b pruning is only relevant in the tree 
    // *below* where the opponent has already moved.
    // also, a chance node doesn't allow the evaluated score
    // to be compared to an to alpha not from the same chance branch.
    // thus, we bookkeep whether or not alpha and beta are valid for pruning.

    int value; 
    AlphaBetaValue( int value ) { this.value = value; }
    public String toString() { return "" + SearchViz.
				   formatInt( new Integer(value) ); }
}

class MM_Move
{
    Move move;
    int score;

    // assumptions can be made with a move 
    // with an associated probability.
    // probability is a Double to allow "null"
    // to mean there is no chance involved.
    TileHolder assumedTiles;
    Double probability;

    // the next move in the "plan".  (debug)
    MM_Move next;

    // in archgt.c one gt_unmove is stored on the
    // stack for each level of recursion.
    MM_Game unmove;

    AlphaBetaValue unAlpha;
    AlphaBetaValue unBeta;

    MM_Move() {}

    MM_Move( int score ) { this.score = score; }

    MM_Move( TileHolder tiles ) { this.assumedTiles = tiles; }

    public String toString() { return move + " score= " + score +
				   " assumed= " + assumedTiles + 
				   " prob= " + probability; }

    // mechanisms to sort.
    // always sort by probability first and then by score.
    static final ArrayUtils.Sorter SORTER = new MM_MoveSorter();
    public static class MM_MoveSorter implements ArrayUtils.Sorter {
	public boolean lteq( Object obj1, Object obj2 )
	{
	    MM_Move move1 = (MM_Move) obj1;
	    MM_Move move2 = (MM_Move) obj2;

	    // a null probability means the move is certain, probablity 1.0 
	    double prob1 = (move1.probability == null) ? 
		1.0 : move1.probability.doubleValue();
	    double prob2 = (move2.probability == null) ? 
		1.0 : move2.probability.doubleValue();

	    // check if prob lt or gt, 
	    // otherwise check scores.
	    if ( prob1 < prob2 )
		return true;
	    else if ( prob1 > prob2 )
		return false;
	    else
		return ( move1.score <= move2.score );
	}
    }
}

class MM_Game
{
    TigrisGame game;

    // a score could be used if you wanted to move and unmove
    // to create a static evaluation at generate move time.
    int score;

    MM_Game( TigrisGame game ) { this.game = game; }

    MM_Game( MM_Game mmGame ) 
    {
	this.game = (TigrisGame) mmGame.game.clone();
	this.score = mmGame.score; 

	// we want to keep the same alpha-beta value references
	// originally tagged to the root (or chance node).
	// the copy constructor does a shallow copy for this purpose.
	//////this.abState = new AlphaBetaState( mmGame.abState );
    }
}

class MM_Info
{
    int trueDepth;
    int actionDepth;
    int nodeCount; // debug;
    boolean assumptionMade = false;
    MM_Game mmGame;
    AlphaBetaValue alpha = null;
    AlphaBetaValue beta = null;
    
    public MM_Info( TigrisGame game )
    {
	trueDepth = 0;
	actionDepth = 0;
	nodeCount = 0;
	this.mmGame = new MM_Game( game );
    }

//     public MM_Info( MM_Info info )
//     {
// 	this.trueDepth = info.trueDepth;
// 	this.actionDepth = info.actionDepth;
// 	this.nodeCount = info.nodeCount;
// 	this.mmGame = new MM_Game( game );
//     }

    void validateAlpha() 
    {
	if ( alpha == null ) {
	    alpha = new AlphaBetaValue( Integer.MIN_VALUE + 1 );
	}
    }

    void validateBeta() 
    {
	if ( beta == null ) {
	    beta = new AlphaBetaValue( Integer.MAX_VALUE );
	}
    }

    public String toString() { return "trueDepth " + trueDepth +
				   " actionDepth " + actionDepth + 
				   " nodeCount " + nodeCount +
				   " alpha "  + alpha +
				   " beta " + beta; }
}

