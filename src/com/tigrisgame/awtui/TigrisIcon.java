/* File: TigrisIcon.java
 * Description:  Implementation of Icon interface for selection panel.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import com.tigrisgame.rules.Catastrophe;
import com.tigrisgame.rules.Leader;
import com.tigrisgame.rules.Tile;
import com.tigrisgame.rules.TileBag;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class TigrisIcon extends Component
{
    private Object object;
    private int tileWidth = UIScale.scale( 20 ),
	tileHeight = UIScale.scale( 20 );
    private int padding = UIScale.scale( 5 );
    
    public TigrisIcon()
    {
    }

    public void setSelectedObject( Object obj )
    {
	object = obj;
	repaint();
    }

    public void setTileSize( int width, int height )
    {
	tileWidth = width;
	tileHeight = height;
	invalidate();
    }

    public Dimension getMinimumSize()
    {
	Dimension minSize = new Dimension( tileWidth + padding*2, tileHeight + padding*2 );
	return minSize;
    }
    
    public Dimension getPreferredSize()
    {
	return getMinimumSize();
    }

    public void update( Graphics g )
    {
	paint( g );
    }

    public void paint( Graphics g )
    {
	TigrisGraphics tg = new TigrisGraphics( g );

	int x = (getSize().width - tileWidth - padding * 2)/2;
	int y = (getSize().height - tileHeight - padding * 2)/2;

	if ( object instanceof Tile ) {
	    tg.drawTile( x, y, tileWidth, tileHeight, (Tile) object );
	} else if ( object instanceof Leader ) {
	    tg.drawLeader( x, y, tileWidth, tileHeight, (Leader) object );
	} else if ( object instanceof Catastrophe ) {
	    tg.drawCatastrophe( x, y, tileWidth, tileHeight );
	} else if ( object instanceof TileBag ) {
	    tg.drawCenteredString( x, y, tileWidth, tileHeight, object.toString());
	}
    }
}

