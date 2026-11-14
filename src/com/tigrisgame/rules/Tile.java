/* File: Tile.java
 * Description:  READ-ONLY enumeration that includes 'hidden'
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

public class Tile extends Sphere implements Serializable
{
//     public static final int HIDDEN_NUM = Sphere.GREY_NUM + 1;
//     public static final char HIDDEN_CHAR = 'H';

   public static final Tile
      BLACK  = new Tile( BLACK_NUM ),
      RED    = new Tile( RED_NUM ),
      BLUE   = new Tile( BLUE_NUM ),
      GREEN  = new Tile( GREEN_NUM ),
      HIDDEN = new Tile( HIDDEN_NUM );
   
   private Tile()
   {
   }

   protected Tile( int color )
   {
      super( color );
   }
    
   public Tile( Sphere sp )
   {
      if ( sp == null )
	 color = INVALID;
      else
	 color = sp.color;
   }

    public boolean isHidden()
    {
	return color == HIDDEN_NUM;
    }

    public static Tile [] getTiles()
    {
	return new Tile [] { BLACK,
			     RED,
			     BLUE,
			     GREEN,
			     HIDDEN };
    }

    public char toChar()
    {
	if ( color == HIDDEN_NUM )
	    return HIDDEN_CHAR;
	else
	    return super.toChar();
    }

    public String toString()
    {
	if ( color == HIDDEN_NUM )
	    return "Hidden";
	else
	    return super.toString();
    }
}
