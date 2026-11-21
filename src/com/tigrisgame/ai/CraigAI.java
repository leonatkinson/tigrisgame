/* File: CraigAI.java
 * Description:  First AI implementaiton.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;
import com.tigrisgame.textui.ColorTextGame;
import com.tigrisgame.util.ArrayUtils;
import com.tigrisgame.util.ResourceLoader;

import java.util.Date;
import java.util.Vector;

public class CraigAI extends DefaultGameObserver implements Runnable
{
    // 0 = no comments
    // 1 = consise move statements
    // 2 = top level branch info.
    // 3 = every level branch and eval info.
    static int verbosity = 1; 
    static String debugEvalString;
    
    // What dynasty we are playing.
    //int _dynasty = -1;

    GameServer gameServer;

    Thread thinkingThread = null;
    java.util.concurrent.atomic.AtomicInteger decisionTreeSize =
	new java.util.concurrent.atomic.AtomicInteger( 0 );

    // signal variable for the thinking thread to make a move.
//     boolean myMove = false;
//     boolean gameOver = false;

    // signal to the thinking thread that you can't expand 
    // your thinking tree anymore,
    // and to return a move asap.
    volatile boolean forceMove = false;
    volatile boolean abortSearch = false;

    /** number of root-level worker threads. 0 = not yet read from
     * property (sentinel), 1 = sequential. */
    int numThreads = 0;

    /** lazy fixed thread pool for parallel root search; daemon threads. */
    private java.util.concurrent.ExecutorService rootPool = null;

    // how long to think before aborting
    int maxThinkMillis = 30 * 1000;  // N times seconds.
    int maxThinkActionDepth = 2; // how many actions deep to consider

    public CraigAI()
    {
	// default constructor
	// need to set the game to use.
	setName( "CraigAI" );
    }
    
//     public CraigAI( int dynasty )
//     {
// 	// the AI player.
// 	// plays the dynasty specified
// 	setName( "CraigAI" );
// 	this._dynasty = dynasty;
//     }

    public CraigAI( GameServer g ) //, int dynasty )
    {
	// plays the dynasty specified in the specific game
	//this._dynasty = dynasty;
	this.gameServer = g;
	setName( "CraigAI" );
    }
    
    public void setThinkTime( int time )
    {
 	maxThinkMillis = time;
    }
    
    public void setThinkActionDepth( int depth )
    {
 	maxThinkActionDepth = depth;
    }
    
    /** number of root-level worker threads. reads -Dtigris.ai.threads on
     * first call; defaults to the available processor count, floored at 1. */
    private int getNumThreads()
    {
	if ( numThreads == 0 ) { // sentinel: not yet read from property
	    String v = System.getProperty( "tigris.ai.threads" );
	    if ( v != null ) {
		try {
		    numThreads = Integer.parseInt( v.trim() );
		}
		catch ( NumberFormatException nfe ) {
		    numThreads = 1;
		}
	    }
	    if ( numThreads <= 0 ) {
		numThreads = Math.max( 1,
		    Runtime.getRuntime().availableProcessors() );
	    }
	}
	return numThreads;
    }

    /** lazy fixed thread pool for parallel root search; daemon threads. */
    private java.util.concurrent.ExecutorService getRootPool()
    {
	if ( rootPool == null ) {
	    int n = getNumThreads();
	    rootPool = java.util.concurrent.Executors
		.newFixedThreadPool( n,
		    r -> { Thread t = new Thread( r, "CraigAI-root-" + n );
		           t.setDaemon( true ); return t; } );
	}
	return rootPool;
    }

    /** result from one parallel root worker: the best root move found
     * in the worker's chunk (best by score, first on ties). */
    private static class CraigRootResult
    {
	int score = Integer.MIN_VALUE;
	Move decision = null;
	int index = -1;
    }

    /**
     * Evaluate a set of root-level decisions in parallel across worker
     * threads.  Each worker gets its own deep clone of the game and
     * evaluates its chunk of decisions, sharing the same wall-clock
     * deadline (stopThink) and abort flags.  Returns the reduced best
     * result, or null to fall back to the sequential loop.
     */
    private CraigRootResult searchRootMovesParallelCraig(
	    TigrisGame actualGame, Move [] decisions, Date stopThink,
	    int actionDepth )
    {
	int numThreads = getNumThreads();
	int numWorkers = Math.min( decisions.length, numThreads );
	if ( numWorkers < 2 )
	    return null;

	java.util.concurrent.ExecutorService pool = getRootPool();
	java.util.ArrayList<java.util.concurrent.Callable<CraigRootResult>> tasks =
	    new java.util.ArrayList<>( numWorkers );

	int chunkSize = ( decisions.length + numWorkers - 1 ) / numWorkers;
	for ( int w = 0; w < numWorkers; w++ ) {
	    int start = w * chunkSize;
	    int end = Math.min( start + chunkSize, decisions.length );
	    if ( start >= end )
		break;
	    final int s = start, e = end;
	    tasks.add( () -> {
		CraigRootResult local = new CraigRootResult();
		for ( int ii = s; ii < e; ii++ ) {
		    if ( abortSearch )
			break;
		    try {
			TigrisGame gamePrime = (TigrisGame) actualGame.clone();
			gamePrime.makeMove( decisions[ ii ] );
			int [] allScores = evaluateGame( gamePrime, stopThink,
							actionDepth, 1,
							false );
			int score = allScores[ gamePrime.getMoveDynasty() ];
			if ( score > local.score ) {
			    local.score = score;
			    local.decision = decisions[ ii ];
			    local.index = ii;
			}
		    }
		    catch ( ForceMoveException fme ) {
			break; // time up or abort: return what we have
		    }
		    catch ( Exception ex ) {
			continue; // try the next decision
		    }
		}
		return local;
	    } );
	}

	java.util.List<java.util.concurrent.Future<CraigRootResult>> futures;
	try {
	    futures = pool.invokeAll( tasks );
	}
	catch ( InterruptedException ie ) {
	    return null; // fall back to sequential
	}

	CraigRootResult best = null;
	int bestScore = Integer.MIN_VALUE;
	for ( java.util.concurrent.Future<CraigRootResult> f : futures ) {
	    try {
		CraigRootResult r = f.get();
		if ( r == null )
		    continue;
		if ( r.score > bestScore ) {
		    bestScore = r.score;
		    best = r;
		}
	    }
	    catch ( Exception ex ) { }
	}
	return best;
    }

    // **********************
    // Implement GameObserver
    // **********************
    
    public void gameStart( TigrisGame g )
    {
	super.gameStart( g );
	abortSearch = true;
    }

    // use DefaultGameObserver implementation for ...
//     public void showScreen( int dynasty, TileHolder tiles )
//     public void gameEnd( int [] finish )
//     public void moveMade( Move pd )

    public void yourMove()
    {
	// reset the abort signals so we have time to think again.
	abortSearch = false;
	forceMove = false;
	// reset the move counter.
	decisionTreeSize.set( 0 );

	thinkingThread = new Thread( this );
	// set min priority so the gui is responsive.
	thinkingThread.setPriority( Thread.MIN_PRIORITY );
	thinkingThread.start();
    }

    public boolean requestUndoMoves( int number )
    {
	return true;
    }

    public void run()
    {
	if ( verbosity > 0 )
	    System.out.println("CraigAI thinking..." );

	Move responsePd = chooseMove();
		
	try {
	    if ( responsePd != null ) {
		gameServer.makeMove( this, responsePd );
		if ( verbosity > 0 ) {
		    ColorTextGame ctg = new ColorTextGame( getGame() );
		    System.out.println("CraigAI chose " 
				       + ctg.formatMove( responsePd ));
		}
	    }
	}
	catch ( Exception ime ) {
	    ime.printStackTrace();
	    System.out.println("Warning: CraigAI made " +
			       " an illegal move: " + ime);
	    throw new RuntimeException(ime);
	    //System.exit(1);
	}
    }

    // this is the main method for choosing a response as the computer.
    // it will return the best Move for the current game
    // that it can in the availible time and depth.
    // NOTE: because the current player is always fully known at this point
    // there is no need for probabistic branching.
    public Move chooseMove()
    {
	Date now = new Date();
	TigrisGame actualGame = super.getGame();
	Move [] decisions = LegalMoveGenerator.getMoves( actualGame );

	// skip all the processing if there are no legal moves.
	if ( decisions.length == 0 )
	    {
		System.out.println("CraigAI - no legal moves");
		throw new RuntimeException();
		//return null;
	    }

	// randomize the moves so we have different results
	decisions = (Move []) ArrayUtils.randomize( decisions );

	// see how we are doing right now.
	if ( verbosity > 1 ) {
	    System.out.println("=============================================");
	    System.out.println("current relative game score is " 
			       + ArrayUtils.
			       prettyPrint( evaluateGame( actualGame )));
	    
	    System.out.println( "chooseMove: " + decisions.length 
				+ " legal moves" );	
	    System.out.println( "starting eval " + now );
	}
		
	// for all possible moves... evaluate them and find the best score
	// in future, do true A-B search.
	int bestScore = Integer.MIN_VALUE;
	int bestIndex = -1;
	//start at the first one in case we can't think of ANYTHING
	Move bestDecision = decisions[0];

	// think for at most maxThinkMillis.
	Date stopThink = new Date( now.getTime() + maxThinkMillis );
	// think of this depth of action responses
	// 1 considers only your first action and no opponent or 2nd actions
	// action depth MUST be positive, otherwise it will 
	// do a depth first search to solve the game.
	int actionDepth = maxThinkActionDepth;
	// parallel root search: evaluate root decisions on separate
	// worker threads with their own game clones.
	boolean parallelHandled = false;
	if ( getNumThreads() > 1 && decisions.length > 1 ) {
	    CraigRootResult best = searchRootMovesParallelCraig(
		actualGame, decisions, stopThink, actionDepth );
	    if ( best != null && best.decision != null ) {
		bestScore = best.score;
		bestDecision = best.decision;
		bestIndex = best.index;
		parallelHandled = true;
	    }
	}


	for ( int ii=0; !parallelHandled && ii<decisions.length; ii++) {

	    // clone game and make the move.
	    // then eval the position for each of the
	    // possible moves and choose one.
	    TigrisGame gamePrime = (TigrisGame) actualGame.clone();
	    int [] allScores = null;

	    if ( verbosity > 1 )
		System.out.println("TOP " + ii + 
				   " state: " + gamePrime.getState() + 
				   " " + decisions[ii] ); 
	    try {
		gamePrime.makeMove( decisions[ii] );
			
		allScores = evaluateGame( gamePrime, stopThink, 
					  actionDepth, 1, false);

		int score = allScores [ gamePrime.getMoveDynasty() ];

		if ( score > bestScore ) {
		    bestScore = score;
		    bestDecision = decisions[ii];
		    bestIndex = ii;
		}
	    }		
	    catch (IllegalMoveException e) {
		System.out.println("illegal move got through " +
				   "the cracks." + e);
		System.out.println("--" + decisions[ii] );
		
		//gamePrime.printGameDump();
		
		throw new RuntimeException(e);
	    }
	    catch ( ForceMoveException e ) {
		// they probably left printing junk (in debug),
		// but oh well, the *'s should
		// be enough so we can tell what's going on.
		if ( verbosity > 0 )
		    System.out.println("*** Aborting decision loop due to " 
				       + e.getMessage() + " ***");

		if ( abortSearch ) 
		    return null;
		break;
	    }
	    catch (Exception e) {
		ResourceLoader.fileSaveObject( gamePrime, "ERROR_GAME" );
		e.printStackTrace();
		throw new RuntimeException(e);
	    }
	}
	
	if ( verbosity > 1 ) {
	    System.out.println("finished eval " + (new Date()));
	    System.out.println("total decisions viewed: " + 
			       decisionTreeSize );
	    System.out.println("best decision: " + bestIndex + " "
			       + bestDecision + " = " + bestScore );
	}

	return bestDecision;	
    }
    
    // returns an array of scores from the perspective of each of the players.
    // the int array corresponds to game.getPlayers()
    // evaluate the game until an exit condition has been reached.
    // those conditions are :
    // 1) the current system time is past endTime
    // (null endtime ignores this condition)
    // 2) the depth (# of plys) for actions is equal to actionDepth 
    // (MUST be positive or will try to solve game DFS)
    // 3) a flag forceMove has been set to true 
    // 4) the game is over.
    // decision depth just keeps track of the levels of recursion
    public int [] evaluateGame( TigrisGame game, Date endTime,
				int actionDepth, int branchDepth,
				boolean lastBranchAssumed )
	throws ForceMoveException
    {
	if ( abortSearch )
	    throw new ForceMoveException("abortSearch flag set");

	// increment the counter for statistics
	decisionTreeSize.incrementAndGet();

	// decrement actionDepth if the current move is an action.
	// that way an initial actionDepth of 1 will analyze all side effects
	// of the action ( monument, conflicts, ect ) but won't consider
	// further actions.
	if ( game.getState() == TigrisGame.WAIT_ACTION )
	    actionDepth --;

	Date now = new Date();
	int gameState = game.getState();

	// find the currently moving player.
	int currentPlayerIndex = game.getMoveDynasty();
	
	// decide whether or not to evaluate the game here
	// or recursively continue.
	if ( gameState == TigrisGame.GAME_OVER ||
	     (actionDepth <= 0 && gameState == TigrisGame.WAIT_ACTION) ||
	     (endTime != null && now.after( endTime ) ) )	     
	    {
		int eval [] = evaluateGame( game );
		
		// we know the above returns without recursion, so 
		// we wait and print the score.
		if ( verbosity > 2 )
		    {
			for ( int ss=0;ss<branchDepth;ss++) 
			    System.out.print(" ");
			System.out.print( "E D" + branchDepth 
					  + " A" + actionDepth 
					  + " Rel Scores = [");
			for ( int ee=0;ee<eval.length;ee++) 
			    { 
				System.out.print(eval[ee]);
				if (ee+1 != eval.length)
				    System.out.print(",");
				else
				    System.out.println("]");
			    }
		    }

		return eval;
	    }
	// decide whether or not to evaulte game deterministically or
	// to do probablistic branching.
	else if ( ! lastBranchAssumed &&
		  (game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT || 
		   game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT )&&
		  game.getPlayer( currentPlayerIndex ).
		  getNumTiles( Sphere.HIDDEN ) > 0 )
	    {
		// first decide what sphere to branch the assumptions on.
		Sphere conflictSphere = game.getConflict().getSupportSphere();

		// calculate the range of assumptions we will make.
		Player currentPlayer = game.getPlayer( currentPlayerIndex );
		int lowAssume = currentPlayer.getNumTiles( conflictSphere );
		int highAssume = currentPlayer.getNumTiles( conflictSphere) +
		    currentPlayer.getNumTiles( Sphere.HIDDEN );
		
 		float [] evalSum = new float[ game.getNumPlayers() ];
		float [] evalSumSq = new float[ game.getNumPlayers() ];
		for ( int ii=0; ii<evalSum.length; ii++)
		    {
			evalSum[ii] = 0;
			evalSumSq[ii] = 0;
		    }
		
		// TODO.. replace this with the more sophisticated prob measure.
		TileHolder unknownTiles = game.getTotalTiles();
		double prob;
		double probTile = (double) unknownTiles.
		    getNumTiles( conflictSphere ) /
		    (double) unknownTiles.getNumTiles();

		if ( verbosity > 2 )
		    {
			for ( int ss=0;ss<branchDepth;ss++) 
			    System.out.print(" ");
			System.out.println( "P D" + branchDepth 
					    + " A"+ actionDepth 
					    + " assuming: from " 
					    + lowAssume + " to " + highAssume
					    + " " + 
					    new Sphere( conflictSphere ) 
					    + " Tiles" 
					    + " probTile=" + probTile);

		    }

		for ( int aa = lowAssume; aa <= highAssume; aa ++ )
		    {
			TigrisGame gamePrime = (TigrisGame) game.clone();

			// calculate probability
			// NOTE: unknown tiles is definatly > 0 because
			// of the condition into this state.
			// TODO: how 'bout a slightly more accurate
			// method of calculating this.
			
			//uniform distribution
			prob = (float) 1.0 / ((float) currentPlayer.
					      getNumTiles( Sphere.HIDDEN )+1.0f);
			
			// sampling WITH replacement			
			prob = binomialCoefficient( highAssume - 
						    lowAssume, aa ) 
			    * Math.pow( probTile, aa ) 
			    * Math.pow( 1.0 - probTile, highAssume - 
					lowAssume - aa );

			if ( verbosity > 2 )
			    {
				for ( int ss=0;ss<branchDepth;ss++) 
				    System.out.print(" ");
				System.out.println(aa + " probability: " 
						   + prob );
			    }

			// modify the player object to make the assumption.
			Player playerPrime = gamePrime.
			    getPlayer( currentPlayerIndex );
			TileBag revealTiles = new TileBag();
			revealTiles.putTiles( conflictSphere, aa 
					      - currentPlayer.
					      getNumTiles( conflictSphere ));

			try {
			    playerPrime.revealHidden( revealTiles );
			}
			catch (RuntimeException rte) {
			    //gamePrime.printGameDump();
			    System.out.println( "HiddenPlayer " + playerPrime 
						+ " aa " + aa + " set amt " +
						(aa - currentPlayer.
						 getNumTiles( conflictSphere ))
						+ " sphere "+ conflictSphere );
			    throw rte;
			}

			int score [] = evaluateGame( gamePrime, endTime,
						     actionDepth, branchDepth
						     + 1, true );
			
			for ( int jj=0; jj<evalSum.length; jj++)
			    {
				evalSum[jj] += score[jj]*prob;
				evalSumSq[jj] += score[jj]*score[jj]*prob;
			    }

			// ABORT loop if after endTime
			// this may or may not be worth the processing time,
			// perhaps if it is a problem,
			// only do it on (ii % 100 == 0)
			now = new Date();
			if ( now.after( endTime ) ) {
			    throw new ForceMoveException( "Out of time at "
							      + now );
			}
			if ( forceMove ) {
			    throw new ForceMoveException( "forceMove variable "
							  + " set to true" );
			}
		    }
		// calculate the standard deviation
		float [] evalStdDev = new float [ game.getNumPlayers() ];
		for ( int jj=0; jj<evalSumSq.length; jj++)
		    {
			evalStdDev[jj] = (float) Math.sqrt
			    ( evalSumSq[jj] - evalSum[jj]*evalSum[jj] );
		    }

		if ( verbosity > 2 )
		    {
			for ( int ss=0;ss<branchDepth;ss++)
			    System.out.print(" ");
			System.out.print( "Average Evaluation = [");
			for ( int ee=0;ee<evalSum.length;ee++) 
			    { 
				System.out.print(evalSum[ee]);
				if (ee+1 != evalSum.length)
				    System.out.print(",");
				else
				    System.out.print("]");
			    }
			System.out.print( " Std Dev = [");
			for ( int ee=0;ee<evalStdDev.length;ee++) 
			    { 
				System.out.print(evalStdDev[ee]);
				if (ee+1 != evalStdDev.length)
				    System.out.print(",");
				else
				    System.out.println("]");
			    }
		    }
		
		int evalInt [] = new int [ evalSum.length ];
		for ( int ll=0; ll<evalInt.length; ll++)
		    {			
			evalInt[ll] = (int) evalSum[ll];
		    }

		return evalInt;
	    }
	else
	    {
		Move [] decisions = LegalMoveGenerator.getMoves( game );

		if ( game.getState() == TigrisGame.WAIT_ATTACK_SUPPORT ||  
		     game.getState() == TigrisGame.WAIT_DEFEND_SUPPORT )
		    {
			//System.out.print("PRUNED FROM " + decisions.length );
			decisions = getPrunedSupportDecisions(decisions,game );
			//System.out.println(" TO " + decisions.length );
		    }

		if ( verbosity > 2 )
		    {
			for ( int ss=0;ss<branchDepth;ss++) 
			    System.out.print(" ");
			System.out.println( "R D" + branchDepth + 
					    " A"+ actionDepth 
					    + " decisions: " + 
					    decisions.length);
		    }

		// the best eval is the one with the highest score for
		// the currently moving player.
		int [] bestEval = new int [ game.getNumPlayers() ];
		for ( int ii=0; ii<bestEval.length; ii ++)
		    { // how bout we give it a GOOD init value.
  		    bestEval[ii] = Integer.MIN_VALUE;
  		}

		int bestEvalIndex = -1;
		for ( int ii=0; ii<decisions.length; ii++)
		    {
			// clone game and make the move.
			// then eval the position for each
			// of the possible moves and choose one.
			TigrisGame gamePrime = (TigrisGame) game.clone();

			if ( verbosity > 2 ) {
			    for ( int ss=0;ss<branchDepth;ss++)
				System.out.print(" ");
			    System.out.println(ii + " state: " 
					       + gamePrime.getState() 
					       + " " + decisions[ii] );
			}
			
			try {
			    gamePrime.makeMove( decisions[ii] );
			}
			catch (IllegalMoveException e) {
			    System.out.println("\nillegal move got through"
					       + " the cracks." + e);
			    System.out.println("--" + decisions[ii] );
			    throw new RuntimeException();
			}
			catch (RuntimeException e) {
			    error( gamePrime, e );
			}

			int score [] = evaluateGame( gamePrime, endTime,
						     actionDepth, 
						     branchDepth + 1, false );
			if ( bestEval == null ||
			     score[ currentPlayerIndex ] > 
			     bestEval[ currentPlayerIndex ] )
			    {
				bestEval = score;
				bestEvalIndex = ii;
			    }

			// ABORT loop if after endTime
			// this may or may not be worth the processing time,
			// perhaps if it is a problem, 
			// only do it on (ii % 100 == 0)
			now = new Date();
			if ( now.after( endTime ) ) {
			    throw new ForceMoveException( "Out of time at "
							  + now );
			}
			if ( forceMove ) {
			    throw new ForceMoveException( "forceMove variable"
							  + "set to true" );
			}
		    }
		
		if ( verbosity > 2 ) {
		    for ( int ss=0;ss<branchDepth;ss++) 
			System.out.print(" ");
		    System.out.print( "Chose decision " + bestEvalIndex 
				      + ", Rel Scores = [");
		    for ( int ee=0;ee<bestEval.length;ee++) 
			{ 
			    System.out.print(bestEval[ee]);
			    if (ee+1 != bestEval.length)
				System.out.print(",");
			    else
				System.out.println("] for player " 
						   + currentPlayerIndex);
			}
		}
		
		return bestEval;
	    }
    }
    
    // the is called to reduce the combinitorial explosion for 
    // support decisions by triming the list to at most 2 decisions.
    // called only when game state is WAIT_SUPPORTERS
    // if attacking, you either go hard or go home. 0 or max.
    // if defending, you either tie or don't defend.
    public Move [] getPrunedSupportDecisions( Move [] decisions, TigrisGame game )
    {
	Conflict conflict = game.getConflict();
	int nonZeroSupportAmount = -1;
	
	// if the attack has yet to declare support.
	// we assume that since state is WAIT_SUPPORTERS and it's our
	// turn that we are the attacker.
	if ( conflict.getAttackScreenSupport() == -1 )
	    {
		Player playerMe = game.getPlayer( conflict.getAttacker() );
		nonZeroSupportAmount = playerMe.
		    getNumTiles( conflict.getSupportSphere() );
	    }
	// as above, but here we are the defender.
	else
	    {
		nonZeroSupportAmount = Math.
		    max( conflict.getNumTilesToTie(), 0 );
	    }

	Vector prunedVector = new Vector(2);
	for ( int pp=0; pp<decisions.length; pp ++)
	    {
		// get all the tiles, we know that only one is 
		// non-zero if the move is legal, that way 
		// we don't have to know what sphere type 
		// the conflict is.
		// also the "else if" should prevent any decision
		// from being used twice even if nonZeroSupport is zero.. hehe.
		if ( decisions[pp].getTiles().getNumTiles() == 0 )
		    prunedVector.addElement( decisions[ pp ] );
		else if ( decisions[pp].getTiles().getNumTiles() == 
			  nonZeroSupportAmount )
		    prunedVector.addElement( decisions[ pp ] );
	    }
	Move [] prunedDecisions= new Move[ prunedVector.size() ];
	for ( int ii=0; ii<prunedDecisions.length; ii++)
	    {
		prunedDecisions[ii] = (Move) prunedVector.elementAt( ii );
	    }
	return prunedDecisions;
    }


    // evalute a game. possibly the game after some future moves.
    public static int [] evaluateGame( TigrisGame game )
    {
	if ( verbosity > 2 )
	    System.out.println("Complete Pct = " +
			       (100*getGameCompletedFraction ( game )));

	// evaluate vp.
	int maxScore = Integer.MIN_VALUE;
	int scoreSum = 0;
	int playerScores [] = new int[ game.getNumPlayers() ];

	// calculate using a raw evaluateDyansty
	// for victory points, etc.
	for ( int ii=0; ii<playerScores.length; ii++)
	    {
		int dynasty = game.getPlayer(ii).getDynasty();
		playerScores[ii] = evaluateDynasty( dynasty, game );

		scoreSum += playerScores[ii];
		if ( playerScores[ii] > maxScore )
		    { maxScore = playerScores[ii]; }
	    }
	// find out the RELATIVE score.
	// relative score is your score compared to the average score.	
	int avgScore = scoreSum / playerScores.length;
	int relativeScores [] = new int [ playerScores.length ];
	for ( int ii=0; ii<playerScores.length; ii++)
	    {
		relativeScores[ ii ] = playerScores[ ii ] - avgScore;
	    }

	return relativeScores;
    }

    public static int evaluateDynasty( int dynasty, TigrisGame game )
    {
	VPEval vpEval = new VPEval();
	return vpEval.evaluate( game, dynasty );
    }

    // returns a number between 0.0 and 1.0
    public static float getGameCompletedFraction( TigrisGame game )
    {
	int totalTiles = 0;
	// BROKEN... todo fixme.
// 	for ( int ii=0; ii<StanGame.TOTAL_TILES.length; ii ++)
// 	    totalTiles += Game.TOTAL_TILES[ii];

	// 10 is a magic number here for the starting number of treasures
	float tileComplete = 1.0f 
	    - (float) ( game.getTileBag().getNumTiles() )
	    / (float) ( totalTiles - 10 - game.getNumPlayers()*6 );

	// todo.. remove 10 magic number and replace with 
	// for that matter, 2 is a magic number.
	// say board.getNumStartingTreasures ??
	float treasureComplete = 
	    (float) ( 10 - game.getBoard().getNumTreasures() ) /
	    (float) ( 10 - 2 );
	
	//System.out.println("Tile Complete = " + tileComplete);
	//System.out.println("Treasure Complete = " + treasureComplete);

	// the game ends when the first leg falls,
	// but never return more than 100% done.
	return Math.min( 1.0f, Math.max( tileComplete, treasureComplete ));
    }

    // TODO... remove from here and refactor to use from util package.

    // n choose k
    // the binomail coefficient of x^k y^(n-k) from (x+y)^n
    public static int binomialCoefficient( int n, int k )
    {
	int coeff = factoral( n ) / factoral( k ) / factoral( n - k );
	//System.out.println( n + " choose " + k + " is " + coeff);
	return coeff;
    }
    
    // simple factoral function.
    public static int factoral( int x )
    {
	int fact = 1;
	if ( x == 0 )
	    return 1;
	while ( x > 1 )
	    {
		fact *= x;
		x --;
	    }
	return fact;
    }

    public static void main( String args[] ) throws Exception 
    {

	GameServerImpl server = new GameServerImpl();

	GameObserver players [] = new GameObserver [] {
	    new CraigAI( server )
	    ,new CraigAI( server )
	    //,new StupidAI( server )
	    //,new StupidAI( server ) 
		};
	
	GameOptions options = new GameOptions( players.length );
	options.seed = 123;

	server.newGame( options );

	for ( int ii=0; ii<players.length; ii ++ ) {
	    server.assignPlayer( players[ii], ii  );
	}

	server.startGame();
    }

    // move to test package.
    public static void foo( String args[] ) throws Exception
    {
	if ( args.length < 2 )
	    {
		System.out.println("Usage CraigAI <tests> <runs>");
		return;
	    }
	
	int runs = Integer.parseInt( args[1] );
	//int tests = Integer.parseInt( args[0] );


	// Benchmark the implementation of the 2 games.
	Player [] players = null;
	TigrisGame g1 = null; 
	TigrisGame gtest = null;
	Move [] decisions = null;
	Date day; 
	long time;

	boolean copy =  (args[0].indexOf("c") != -1 );
	boolean gen =  (args[0].indexOf("g") != -1 );
	boolean eval =  (args[0].indexOf("e") != -1 );
	boolean make =  (args[0].indexOf("m") != -1 );

	for ( int gg = 1; gg < 3; gg ++)
	    {
		players = new Player [] { new Player( 0, "D0" ), new Player( 1, "D1" ) };
		g1 = null;//new Game( players, "map.dat", 123, gg );
		decisions = LegalMoveGenerator.getMoves( g1 );

		// just test copy and generate.
		day = new Date();
		time = day.getTime();
		System.out.println("Starting board" + gg + ", " + day + ", " + decisions.length + " legal moves");

//  		for ( int dd=0; dd< decisions.length; dd++)
//  		    System.out.println(""+ decisions[dd] );

		for ( int ii=0; ii < runs; ii ++)
		    {			
			if ( gen )
			    decisions = LegalMoveGenerator.getMoves( g1 );

			for ( int dd = 0; dd < decisions.length; dd ++ )
			    {
				if ( copy )
				    gtest = null; //new HiddenGame( g1, 0 ); 
				else 
				    gtest = g1;

				if ( make )
				    gtest.makeMove( decisions[dd%decisions.length] );

				if ( eval )
				    evaluateGame( gtest );
			    }
		    }
		day = new Date();
		System.out.println("Done in        , " + ((day.getTime() - time ) ) + " msec ");
	    }
    }

    public void error( TigrisGame g, Throwable t )
    {
	ResourceLoader.fileSaveObject( g, "ERROR_GAME" );
	ColorTextGame gui = new ColorTextGame();
	//gui.setGameView( g );
	//gui.printGame( System.out );
	t.printStackTrace();
	throw new RuntimeException(t);
    }
}





