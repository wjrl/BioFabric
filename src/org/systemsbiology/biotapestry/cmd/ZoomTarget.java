/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.cmd;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Used to abstract away zoom operations of SUPanel
*/

public interface ZoomTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

  //
  // Handling options for bounding with modules:
  //
  
  public static final int NO_MODULES      = 0;
  public static final int VISIBLE_MODULES = 1;
  public static final int ALL_MODULES     = 2;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Methods
  */ 

  public Point pointToViewport(Point world); 
  public Point2D getRawCenterPoint();  
  public Rectangle getSelectedBounds();
  public boolean haveCurrentSelectionForBounds();
  public boolean haveMultipleSelectionsForBounds();
  public Rectangle getCurrentSelectedBounds();
  public void incrementToNextSelection();
  public void decrementToPreviousSelection();
  public Rectangle getWorkspaceBounds();
  public Dimension getPreferredSize();
  public void fixCenterPoint(boolean doComplete, UndoSupport support, boolean closeIt);
  public void repaint();
  public Dimension getBasicSize(boolean doComplete, boolean doBuffer, int moduleHandling);
  public Point2D viewToWorld(Point vPt);    
  public void setWideZoomFactor(double newZoomVal, Dimension vDim);
  public double getWorkspaceZoom(Dimension viewportDim, double percent);
  public Rectangle getCurrentBasicBounds(boolean doComplete, boolean doBuffer, int moduleHandling);
  public Rectangle getAllModelBounds();
  public void adjustWideZoomForSize(Dimension dims);
  public Point getCenterPoint();
  public void setZoomFactor(double zoom);
  public void setCurrClipRect(Rectangle2D clipRect);
  public double getZoomFactor();
}
