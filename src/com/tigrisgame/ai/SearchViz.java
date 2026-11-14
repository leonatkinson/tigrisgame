/* File: SearchViz.java
 * Description:  Tree visualization for ai search 
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.awtui.GamePanel;
import com.tigrisgame.rules.*;
import com.tigrisgame.util.ArrayUtils;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class SearchViz // extends JPanel // SplitPane
{
    static int instanceNum = 0;

    JTree searchTree;
    DefaultTreeModel searchModel;
    DefaultMutableTreeNode currentNode;

    JPanel leftPanel = new JPanel();
    JPanel rightPanel = new JPanel();
    JTextField statusBar = new JTextField("", 50);

    GamePanel gamePanel = new GamePanel();
    int verbosity = 0;

    public SearchViz()
    {
	//super( JSplitPane.HORIZONTAL_SPLIT );
	//setContinuousLayout( true );
	//setLeftComponent( new JScrollPane( leftPanel ) );
	//setRightComponent( rightPanel );

	searchTree = new JTree();
	searchTree.setEditable( true );
	
	DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
	searchModel = new DefaultTreeModel( root );
	searchTree.setModel( searchModel );
	searchTree.setSelectionPath( new TreePath( searchModel.getRoot()));

	//searchTree.setRootVisible( false );
	searchTree.addTreeSelectionListener( new ShowGameListener());
	searchTree.addTreeSelectionListener( new ShowStatusListener());
	searchTree.putClientProperty("JTree.lineStyle", "Angled");

	statusBar.setFont( new Font( "SanSerif", Font.PLAIN, 20 ));

	gamePanel.boardPanel.highlightKingdoms = false;
	leftPanel.add( BorderLayout.CENTER, searchTree );
	rightPanel.add( BorderLayout.CENTER, gamePanel );
	rightPanel.setSize( 400,400 );

	//add( BorderLayout.CENTER, new JScrollPane( leftPanel ) );

    }

    public SearchViz( TigrisGame game )
    {
	this();
	initSearch( game );
    }

    public class ShowStatusListener implements TreeSelectionListener 
    {
	public void valueChanged(TreeSelectionEvent e) 
	{
	    TreePath path = e.getPath();
	    DefaultMutableTreeNode lastNode = 
		(DefaultMutableTreeNode) path.getLastPathComponent();

	    if ( lastNode.getUserObject() instanceof ScoredNode ) {
		ScoredNode moveNode = (ScoredNode) lastNode.getUserObject();
		statusBar.setText( "" + moveNode );
	    } 
	    else {
		statusBar.setText("");
	    }

// 	    Game game = getGame ( path );
// 	    if ( game != null ) {
// 		GameEvaluator evalA = new VPEval();
// 		GameEvaluator evalB = new LeaderEval();
		
// 		String evalStr = "       Eval " + 
// 		    "vp = [" + evalA.evaluate( game, 0 ) + 
// 		    ", " + evalA.evaluate( game, 1 ) + 
// 		    "] leader = [" + evalB.evaluate( game, 0 ) + 
// 		    ", " + evalB.evaluate( game, 1 ) + "]";
		
	    statusBar.setText( statusBar.getText());
	}
    }

    public class ShowGameListener implements TreeSelectionListener 
    {
	public void valueChanged(TreeSelectionEvent e) 
	{
	    TreePath path = e.getPath();
	    TigrisGame game = getGame ( path );
	    gamePanel.setGameView( game );

	    Object lastUserObject = 
		((DefaultMutableTreeNode) path.getLastPathComponent()).
		getUserObject();
	    if ( lastUserObject instanceof MoveNode ) {
		Move move = ((MoveNode) lastUserObject).move;
		if ( move != null ) {
		    Player player = game.getPlayer( move.getDynasty());
		    gamePanel.setPlayerView( player );
		}
	    }
	}
    }

    public TigrisGame getGame( TreePath path )
    {
	Object [] objArray = path.getPath();
	
	if( objArray.length < 2 )
	   return null;

	TigrisGame game = null;
	for ( int ii=1; ii<objArray.length; ii++ ) {
	   DefaultMutableTreeNode treeNode = 
	      ((DefaultMutableTreeNode) objArray[ii]);

	   Object userObj = treeNode.getUserObject();
	   if ( userObj instanceof GameNode ) {
	      GameNode gNode = (GameNode)userObj;
	      game = (TigrisGame) (gNode.game.clone());
	      continue;
	   }
	    
	   if ( !( userObj instanceof MoveNode) )
	      continue;
	   
	   MoveNode moveNode = (MoveNode) treeNode.getUserObject();
	   
	   try {
	      // make assumption if any.
	      if ( moveNode.assume != null ) {
		 Player player = game.
		    getPlayer( game.getMoveDynasty() );
		 player.revealHidden( moveNode.assume );
	      }
	      
	      // make move if any.
	      if ( moveNode.move != null )
		 game.makeMove( moveNode.move );
	   }
	   catch (IllegalMoveException ime) {
	      System.out.println("WARNING: Couldn't apply move " + ii + 
				 " " + ime.getMessage() + " " +
				 objArray[ii]  );
// 		ColorTextGame ctg = new ColorTextGame();
// 		ctg.setGameView( game );
// 		ctg.printGame( System.out );
// 		throw new Error();
	   }
	}
	
	return game;
    }

    public void initSearch( TigrisGame game )
    {
	if ( verbosity > 0 )
	    System.out.println("addgame " + game );

	currentNode = new DefaultMutableTreeNode( new GameNode(game) );
	DefaultMutableTreeNode root = 
	   (DefaultMutableTreeNode)searchModel.getRoot();
	searchModel.insertNodeInto( currentNode, root, root.getChildCount());
    }

    public void addChild( Move move, TileHolder assume )
    {
	if ( verbosity > 0 )
	    System.out.println("addchild " + move + " " + assume );

	MoveNode pair = new MoveNode( move, assume );
	DefaultMutableTreeNode node = 
	    new DefaultMutableTreeNode( pair );
	((DefaultMutableTreeNode)currentNode).add( node );

	currentNode = node;
    }

    public void addChild( String str )
    {
	DefaultMutableTreeNode node = 
	    new DefaultMutableTreeNode( new ScoredNode(str) );
	((DefaultMutableTreeNode)currentNode).add( node );

	currentNode = node;
    }

    public void ascend()
    {
	if ( verbosity > 0 )
	    System.out.println("ascend");

	currentNode = (DefaultMutableTreeNode) currentNode.getParent();
    }
    
    public void setScore(int score)
    {
	if ( verbosity > 0 )
	    System.out.println("setScore " + score);

	DefaultMutableTreeNode mutNode = 
	   (DefaultMutableTreeNode) currentNode;
	if ( mutNode.getUserObject() instanceof ScoredNode ) {
	   ScoredNode info = (ScoredNode) 
	      ((DefaultMutableTreeNode) currentNode).getUserObject();
	   info.score = score;
	}
    }

    public void setScore(int [] aScore)
    {
	if ( verbosity > 0 )
	    System.out.println("setScore " + aScore);

	DefaultMutableTreeNode mutNode = 
	   (DefaultMutableTreeNode) currentNode;
	if ( mutNode.getUserObject() instanceof ScoredNode ) {
	   ScoredNode info = (ScoredNode) 
	      ((DefaultMutableTreeNode) currentNode).getUserObject();
	   info.aScore = aScore;
	}
    }

    public void addNote(String note)
    {
	addNote( note, currentNode );
    }

    public void addNote(String note, TreeNode node )
    {
	DefaultMutableTreeNode mutableNode =
	    (DefaultMutableTreeNode) node;

 	if ( mutableNode != searchModel.getRoot() && 
	     mutableNode.getUserObject() instanceof ScoredNode ) {
	   ScoredNode info = (ScoredNode) 
	      mutableNode.getUserObject();
	   info.notes += " " + note;
	   info.notes = info.notes.trim();
 	}
    }

    public void setPruned( boolean value )
    {
 	if ( currentNode != searchModel.getRoot()) {
 	    MoveNode info = (MoveNode) 
 		((DefaultMutableTreeNode) currentNode).getUserObject();
 	    info.pruned = value;
 	}
    }

    // entry point method.
    public void orderTree()
    {
	orderTreeNode( (DefaultMutableTreeNode) searchModel.getRoot());
    }

    // recursive.
    public void orderTreeNode( DefaultMutableTreeNode node )
    {
	// first group the children, then the node.
	for ( int ii= node.getChildCount()-1; ii >= 0; ii--) {
	    DefaultMutableTreeNode childNode = 
		(DefaultMutableTreeNode) node.getChildAt( ii );
	    orderTreeNode( childNode );
	}

	sortTreeChildren( node );
	groupTreeByType( node );
    }

    public void sortTreeChildren( DefaultMutableTreeNode node )
    {
	DefaultMutableTreeNode [] childArray = 
	    new DefaultMutableTreeNode[ node.getChildCount() ];

	Enumeration ee = node.children();
	for ( int ii=0; ii<childArray.length; ii++ ) {
	    childArray[ii] = (DefaultMutableTreeNode) ee.nextElement();
	}

	NodeSorter sorter = new NodeSorter();

	TreePath path = new TreePath( node.getPath() );

	if ( getGame( path ).getMoveDynasty() == 0 )
	    ArrayUtils.sortDescending( childArray, sorter );
	else
	    ArrayUtils.sortAscending( childArray, sorter );

	node.removeAllChildren();
	for ( int ii=0; ii<childArray.length; ii++ ) {
	    node.add( childArray[ii] );
	}
    }

    // group the tree by move type,
    // LEADER, TILE, CAT, and SWAP.
    public void groupTreeByType( DefaultMutableTreeNode node )
    {
	DefaultMutableTreeNode leaderNode = new 
	    DefaultMutableTreeNode( "Leader Moves" );
	DefaultMutableTreeNode tileNode = new 
	    DefaultMutableTreeNode( "Tile Moves" );
	DefaultMutableTreeNode catNode = new 
	    DefaultMutableTreeNode( "Catastrophe Moves" );
	DefaultMutableTreeNode swapNode = new 
	    DefaultMutableTreeNode( "Swap Moves" );

	// assume that if the first move is an action, 
	// that all moves are actions, otherwise don't group.

	if ( node.getChildCount() == 0 ) 
	    return;

	Move move = ((MoveNode)((DefaultMutableTreeNode)node.getChildAt( 0 ))
		     .getUserObject()).move;
	if ( move != null &&
	     (move.isPositionLeader()  ||
	      move.isPlaceTile()       ||
	      move.isPlayCatastrophe() ||
	      move.isSwapTiles())) {
	    // it is a grouping type.

	    for ( int ii= node.getChildCount()-1; ii >= 0; ii--) {
		DefaultMutableTreeNode childNode = 
		    (DefaultMutableTreeNode) node.getChildAt( ii );
		
		move =  ((MoveNode)childNode.getUserObject()).move;
		if ( move.isPositionLeader() ) {
		    leaderNode.add( childNode );
		} else if ( move.isPlaceTile() ) {
		    tileNode.add( childNode );
		} else if ( move.isPlayCatastrophe()) {
		    catNode.add( childNode );
		} else if ( move.isSwapTiles() ) {
		    swapNode.add( childNode );
		} else {
		    System.out.println("huh? " + move );
		}
	    }
	    
	    leaderNode.setUserObject( leaderNode.getUserObject() + 
				      " (" + leaderNode.getChildCount() + 
				      ") best score= " + 
				      getLastScore( leaderNode ));
	    tileNode.setUserObject( tileNode.getUserObject() + 
				    " (" + tileNode.getChildCount() + 
				      ") best score= " + 
				    getLastScore( tileNode ));
	    catNode.setUserObject( catNode.getUserObject() + 
				   " (" + catNode.getChildCount() + 
				   ") best score= " +
				   getLastScore( catNode ) );
	    swapNode.setUserObject( swapNode.getUserObject() + 
				    " (" + swapNode.getChildCount() +
				    ") best score= " + 
				    getLastScore( swapNode ) );
	    
	    node.add( leaderNode );
	    node.add( tileNode );
	    node.add( catNode );
	    node.add( swapNode );
	    
	    groupTreeBySphere( leaderNode );
	    groupTreeBySphere( tileNode );
	}
    }

    public static String formatInt( Integer val )
    {
	if ( val == null )
	    return "null";

	if ( val.intValue() >= 0 )
	    return "" + val;
	else 
	    return "(" + (-val.intValue()) + ")";
    }

    public String getLastScore( DefaultMutableTreeNode node )
    {
	if ( node.getChildCount() > 0 ) {
	    DefaultMutableTreeNode child = 
		(DefaultMutableTreeNode) node.getLastChild();
	    if ( child.getUserObject() instanceof MoveNode ) {
		MoveNode move = (MoveNode) child.getUserObject();

		if ( move.aScore != null )
		   return ArrayUtils.prettyPrint( move.aScore );
		else
		   return formatInt( new Integer(move.score) );
	    } 
	}

	return null;
    }

    public String getFirstScore( DefaultMutableTreeNode node )
    {
	if ( node.getChildCount() > 0 ) {
	    DefaultMutableTreeNode child = 
		(DefaultMutableTreeNode) node.getFirstChild();
	    if ( child.getUserObject() instanceof MoveNode ) {
		MoveNode move = (MoveNode) child.getUserObject();

		if ( move.aScore != null )
		   return ArrayUtils.prettyPrint( move.aScore );
		else
		   return formatInt( new Integer(move.score) );
	    } 
	}

	return null;
    }

    public void groupTreeBySphere( DefaultMutableTreeNode node )
    {
	Sphere [] spheres = Sphere.getSpheres();
	DefaultMutableTreeNode [] sphereNodes = 
	    new DefaultMutableTreeNode[ spheres.length ];

	for ( int ii=0; ii< spheres.length; ii++ ) {
	    sphereNodes[ii] = new 
		DefaultMutableTreeNode( spheres[ii].toString() );
	}

	// assume that if the first move is an action, 
	// that all moves are actions, otherwise don't group.

	if ( node.getChildCount() == 0 ) 
	    return;

	Move move = ((MoveNode)((DefaultMutableTreeNode)node.getChildAt( 0 ))
		     .getUserObject()).move;
	if ( move != null &&
	     move.getSphere() != null )
	    {
		for ( int ii= node.getChildCount()-1; ii >= 0; ii--) {
		    DefaultMutableTreeNode childNode = 
			(DefaultMutableTreeNode) node.getChildAt( ii );
		    
		    move =  ((MoveNode)childNode.getUserObject()).move;
		    int sphereNum = move.getSphere().getColorCode();
		    sphereNodes[sphereNum].add( childNode );
		}
	    }

	for ( int ii=0; ii<spheres.length; ii++ ) {
	    sphereNodes[ii].
		setUserObject( sphereNodes[ii].getUserObject() + 
			       " (" + sphereNodes[ii].getChildCount() + 
			       ") best score= " + 
			       getFirstScore( sphereNodes[ii] ));

	    node.add( sphereNodes[ii] );
	}
    }

    public void wrapWithFrame()
    {
	JFrame frame = new JFrame("SearchViz" + (instanceNum++));
	frame.getContentPane().add( BorderLayout.CENTER, 
				    new JScrollPane( leftPanel ) );
	frame.getContentPane().add( BorderLayout.EAST, rightPanel );
	frame.getContentPane().add( BorderLayout.SOUTH, statusBar );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

	searchTree.expandRow( 0 );
	frame.pack();	
	frame.setSize( 1000, frame.getSize().height );	
	frame.setVisible( true );	
    }

    public static void main( String [] args )
    {
	SearchViz viz = new SearchViz();

	TigrisGame game = new StandardGame( new GameOptions() );
	Board board = game.getBoard();
	viz.addChild( Move.newPlayCatastrophe( 0, board.getSpace( 0,0 )),
			  		       null );

	viz.addChild( Move.newPositionLeader( 1, board.getSpace( 1,1 ), 
					      Sphere.GREEN) , null );
	viz.ascend();
	viz.addChild( Move.newPositionLeader( 2, board.getSpace( 1,1 ), 
					      Sphere.RED) , null );

	viz.wrapWithFrame();
    }
}

class NodeSorter implements ArrayUtils.Sorter
{
    public boolean lteq( Object obj1, Object obj2 )
    {
	DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) obj1;
	DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) obj2;

	if ( node1.getUserObject() instanceof MoveNode &&
	     node2.getUserObject() instanceof MoveNode )
	    {
		MoveNode move1 = (MoveNode) node1.getUserObject();
		MoveNode move2 = (MoveNode) node2.getUserObject();


		if ( move1.aScore != null && move2.aScore != null )
		   return ( move1.aScore[0] <= move2.aScore[0] );
		else
		   return ( move1.score <= move2.score );
// 		return ( move1.pruned || 
// 			 ( ! move2.pruned && 
// 			   move1.score <= move2.score ));
	    }
	else 
	    // they are all equal.
	    return true;
	     
    }
}

class ScoredNode
{
   String notes = "";
   int score = 0;
   int []aScore = null;

   public ScoredNode() {}
   public ScoredNode( String str ) { this.notes = str; }

   public String toString()
   {
      String str = "";
      if ( notes != null ) {
	 str += " " + notes;
      } 
      if ( aScore != null ) {
	 str += " score = " + ArrayUtils.prettyPrint( aScore );
      }
      return str;
   }
}

class GameNode extends ScoredNode
{
   TigrisGame game;
   GameNode( TigrisGame game ) 
   {
      this.game = game;
   }
}

class MoveNode extends ScoredNode
{
   Move move;
   TileHolder assume;
   boolean pruned = false;
   
   MoveNode( Move move, TileHolder assume ) 
   {
      this.move = move;
      this.assume = assume;
   }
   
   public String toString()
   {
      String str;
      if ( move == null ) {
	 str = "ASSUME " + assume;
      } else if ( assume != null &&
		  assume.getNumTiles() > 0 ) {
	 str = "" + move + " with ASSUME " + assume; 
      } else
	 str = move.toString();
      
      if ( notes != null )
	 str += " " + notes;
      
      if ( pruned )
	 str += " PRUNED";
      else if ( aScore != null ) {
	 str += " score = " + ArrayUtils.prettyPrint( aScore );
      }
//       else {
// 	 str += " score= " + 
// 	    SearchViz.formatInt( new Integer( score )  );
//       }
      return str;
   }
}
