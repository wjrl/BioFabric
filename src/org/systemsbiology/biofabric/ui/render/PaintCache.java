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

package org.systemsbiology.biofabric.ui.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This is the cache of simple paint objects
*/

public interface PaintCache {
  
	////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public final static int STROKE_SIZE = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  **  Dump used memory
  */
  
  public void clear();
  
  /***************************************************************************
  **
  **  paint it
  */
  
  public boolean paintIt(Graphics2D g2, Rectangle clip, Reduction reduce);
  
  /***************************************************************************
  **
  ** Draw the selection rubber band
  */
  
  public void drawFloater(Graphics2D g2, FloaterSet floaters);

 
  /***************************************************************************
  **
  ** Build objcache
  */
  
  public void buildObjCache(List<BioFabricNetwork.NodeInfo> targets, List<BioFabricNetwork.LinkInfo> links, 
                            boolean shadeNodes, boolean showShadows, Map<NID.WithName, Rectangle2D> nameMap, 
                            Map<NID.WithName, List<Rectangle2D>> drainMap, Rectangle2D worldRect,
                            AnnotationSet nodeAnnot, AnnotationSet linkAnnot,
                            BTProgressMonitor monitor) throws AsynchExitRequestException;
 
  /***************************************************************************
  ** 
  ** Get a link color
  */

  public Color getColorForLink(BioFabricNetwork.LinkInfo link, FabricColorGenerator colGen);
 
  /***************************************************************************
  ** 
  ** Get a node color
  */

  public Color getColorForNode(BioFabricNetwork.NodeInfo node, FabricColorGenerator colGen);
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Floaters
  */  
  
  public static class FloaterSet {
    public Rectangle floater;
    public Rectangle tourRect;
    public Rectangle currSelRect;
    
    public FloaterSet(Rectangle floater, Rectangle tourRect, Rectangle currSelRect) {
      this.floater = floater;
      this.tourRect = tourRect;
      this.currSelRect = currSelRect;    
    }
    
    public void clear() {
      floater = null;
      tourRect = null;
      currSelRect = null;
      return;
    }
    
    public boolean isEmpty() {
      return ((floater == null) &&
              (tourRect == null) &&
              (currSelRect == null));
    }   
  } 
  
  /***************************************************************************
  **
  ** Used to reduce painting to a limited selection
  */  
  
  public static class Reduction {
    Set<Integer> paintRows;
    Set<Integer> paintCols;
    Set<NID> paintNames;
    
    public Reduction(Set<Integer> rows, Set<Integer> cols, Set<NID> names) {
      this.paintRows = rows;
      this.paintCols = cols;
      this.paintNames = names;    
    }
    
    public boolean somethingToPaint() {
    	return (!paintRows.isEmpty() || !paintCols.isEmpty() || !paintNames.isEmpty());	
    }
    
  } 
}
