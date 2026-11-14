/* File: Monument.java
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

public class Monument implements Serializable
{
    Sphere firstSphere = null;
    Sphere secondSphere = null;
    //Space upperLeftCorner = null;
    //String upperLeftCornerId = null;

    private Monument()
    {
	// do not use
    }

    public Monument( Sphere primary, Sphere secondary )
    {
	firstSphere = primary;
	secondSphere = secondary;
    }

//      Monument( Space ulSpace, Sphere primary, Sphere secondary )
//      {
//  	firstSphere = primary;
//  	secondSphere = secondary;
//  	upperLeftCorner = ulSpace;
//      }

    public Sphere getFirstSphere()
    {
	return firstSphere;
    }

    public Sphere getSecondSphere()
    {
	return secondSphere;
    }

    // used EXACTLY ONCE in the graphics!
//      private Space getUpperLeftCorner()
//      {
//  	return upperLeftCorner;
//      }

    public boolean isMonumentEqual( Monument mon )
    {
	// Sphere order doesn't matter.
	return ( firstSphere.isSphereEqual( mon.firstSphere ) &&
		 secondSphere.isSphereEqual( mon.secondSphere ) ||
		 firstSphere.isSphereEqual( mon.secondSphere ) &&
		 secondSphere.isSphereEqual( mon.firstSphere ));
    }

    public boolean equals( Object obj )
    {
	if ( obj instanceof Monument )
	    {
		Monument mon = (Monument) obj;
		// Sphere order doesn't matter.
		return ( firstSphere.isSphereEqual( mon.firstSphere ) &&
			 secondSphere.isSphereEqual( mon.secondSphere ) ||
			 firstSphere.isSphereEqual( mon.secondSphere ) &&
			 secondSphere.isSphereEqual( mon.firstSphere ));
	    }
	else
	    return false;
    }

    public String toString()
    {
	return "Monument: firstSphere=" + firstSphere + 
	    " secondSphere=" + secondSphere;
    }
}
