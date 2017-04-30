/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biofabric.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.SwingUtilities;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.QuadTree;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This builds and manages image chunks
*/

public class GarbageRequester {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
	
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GarbageRequester() {
  
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Ask for GC run
  */
  
  public void askForGC(BTProgressMonitor monitor) throws AsynchExitRequestException {

  	long freeMem;
    System.out.println("Tot " + Runtime.getRuntime().totalMemory());
    freeMem = Runtime.getRuntime().freeMemory();
    System.out.println("Free " + freeMem);
    System.out.println("Max " + Runtime.getRuntime().maxMemory());
    System.out.flush();
    
    LoopReporter lr2 = new LoopReporter(1, 20, monitor, 0.0, 1.0, "progress.garbageRequest");
    lr2.report();
    UiUtil.fixMePrintout("ditch the INDET hack");
    LoopReporter lr3 = new LoopReporter(1, 20, monitor, 0.0, 1.0, "INDET");
    lr3.report(); 	

    try {
      Thread.sleep(2000);
    } catch (InterruptedException iex) {
    }
    
    // Experiments show doing this twice has a significant effect
  	Runtime.getRuntime().gc();
  	Runtime.getRuntime().gc();
  	for (int i = 0; i < 10; i++) {
    	try {
        Thread.sleep(1000);
      } catch (InterruptedException iex) {
      }
    	long freeNow = Runtime.getRuntime().freeMemory();
    	if (freeNow > freeMem) {
    		break;
    	}
  		System.out.println("Tot " + Runtime.getRuntime().totalMemory());
  	  System.out.println("Free " + Runtime.getRuntime().freeMemory());
  	  System.out.println("Max " + Runtime.getRuntime().maxMemory());
  	  System.out.flush(); 	
  	}
  }
}