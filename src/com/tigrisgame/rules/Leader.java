/* File: Leader.java
 * Description:  READ-ONLY game state object used by Board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

public class Leader implements Serializable
{
    Sphere sphere = null;
    int dynasty;
    //Player player;

    private Leader()
    {
    }
    
    // check for the non-preferred way.
    private
	 Leader ( int sphere, int dynasty )
    {
	this.sphere = new Sphere( sphere );
	this.dynasty = dynasty;
    }

    public Leader ( Sphere sp, int dynasty  )
    {
	this.sphere = sp;
	this.dynasty = dynasty;
    }

    public int getDynasty()
    {
	return dynasty;
    }

    public Sphere getSphere()
    {
	return sphere;
    }

    /* MAYBE USEFULL???
    public boolean isKing()
    {
	return value == BLACK;
    }
    public boolean isPriest()
    {
	return value == RED;
    }
    public boolean isFarmer()
    {
	return value == BLUE;
    }
    public boolean isTrader()
    {
	return value == GREEN;
    }
    */

    // converts the leader to one character
    // representing sphere and dynasty.
    // the slightly cryptic code is 
    // number dynasty sphere
    //   0       invalid
    //   1       0      1
    //   2       0      2
    //   3       0      3
    //   4       0      4
    //   5       invalid 
    //   6       1      1
    //   7       1      2
//      public String toChars()
//      {
//  	int color;
//  	if (sphere.isBlack())
//  	    color = 1;
//  	else if (sphere.isRed())
//  	    color = 2;
//  	else if (sphere.isBlue())
//  	    color = 3;
//  	else if (sphere.isGreen())
//  	    color = 4;

//  	return dynasty * 5 + color;
//      }

    public String toString()
    {
	return "Leader: Dynasty=" + dynasty + " Sphere=" + sphere;
    }

    public boolean equals( Object obj )
    {
	return ( obj instanceof Leader && 
		 isLeaderEqual( (Leader) obj ));
    }

    public boolean isLeaderEqual( Leader leader )
    {
	return ( leader != null &&
		 this.dynasty == leader.dynasty &&
		 this.sphere.isSphereEqual( leader.getSphere() ) );
    }

   public int hashCode()
   {
      return (this.dynasty*10 + this.sphere.getColorCode());
   }
}




