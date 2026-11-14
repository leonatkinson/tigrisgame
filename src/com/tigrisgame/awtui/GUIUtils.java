/* File: GUIUtils.java
 * Description: small helpers for window placement.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.awtui;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.Window;

public final class GUIUtils
{
    private GUIUtils()
    {
    }

    /** place a window in the center of the screen (best effort). */
    public static void centerWindowOnScreen( Window window )
    {
	try {
	    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	    window.setLocation( Math.max( 0, ( screen.width -
						window.getWidth() ) / 2 ),
				Math.max( 0, ( screen.height -
						window.getHeight() ) / 2 ) );
	}
	// running headless: just leave the window alone.
	catch ( HeadlessException he ) {
	}
    }
}
