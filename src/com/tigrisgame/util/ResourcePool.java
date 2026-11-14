/* File: ResourcePool.java
 * Description:  Memory utility pool for some AI implementations.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;

import java.util.Hashtable;
import java.util.Stack;

public abstract class ResourcePool
{
    protected int maxSize;
    protected int minSize;
    protected Stack freePool = new Stack();
    protected Hashtable usedPool = new Hashtable();

    public ResourcePool()
    {
	this( Integer.MAX_VALUE, 0 );
    }
       
    public ResourcePool( int max )
    {
	this( max, 0 );
    }

    public ResourcePool( int max, int min )
    {
	this.maxSize = max;
	this.minSize = min;

	for ( int ii=0; ii<minSize; ii++) {
	    if ( ii%50 == 0 )
		System.out.println("creatingResouce " + ii );
	    freePool.addElement( createResource() );
	}
    }

    public Object getResource()
    {
	Object resource = null;

	// see if we have a free one lying around.
	if ( freePool.size() > 0 ) {
	    int index = freePool.size() - 1;
	    resource = freePool.pop();
	    usedPool.put( resource, resource );
	}  
	// if not, see if we are allowed to make aother one.
	else if ( freePool.size() + usedPool.size() < maxSize ) {
	    resource = createResource();
	    usedPool.put( resource, resource );
	}
	// otherwise the caller is out of luck.
	else {
	    throw new RuntimeException("no more resources left");
	}

	return resource;
    }

    public void releaseResource( Object resource )
    {
	// remove it from the used pool, and if
	// it was in fact one of the objects being used
	// put it back in the freePool, this ensures
	// that no garbage goes in the freePool
	if ( usedPool.remove( resource ) != null )
	    freePool.push( resource );
	else
	    System.out.println("Warning: ResourcePool releasedResource " +
			       "that was not in used pool " + resource);
    }

    // to be defined by the subclass
    protected abstract Object createResource();
}
