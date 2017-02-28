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

package org.systemsbiology.biofabric.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** A Class
*/

public class CycleFinder {
  
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
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set<NID.WithName> nodes_;
  private Set<FabricLink> links_;
  private HashMap<NID.WithName, Set<FabricLink>> linksForNode_;  
  private Integer white_;
  private Integer grey_;
  private Integer black_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public CycleFinder(Set<NID.WithName> nodes, Set<FabricLink> links) {
    nodes_ = nodes;
    links_ = links;
    linksForNode_ = new HashMap<NID.WithName, Set<FabricLink>>();
    Iterator<FabricLink> lit = links_.iterator();
    while (lit.hasNext()) {
      FabricLink link = lit.next();
      Set<FabricLink> linksForSrc = linksForNode_.get(link.getSrcID());
      if (linksForSrc == null) {
        linksForSrc = new HashSet<FabricLink>();
        linksForNode_.put(link.getSrcID(), linksForSrc);
      }
      linksForSrc.add(link);
    }
    white_ = new Integer(0);
    grey_ = new Integer(1);
    black_ = new Integer(2);    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Answer if there is a cycle
  */

  public boolean hasACycle() {
    
    //
    // Color vertices white:
    //
    
    HashMap<NID.WithName, Integer> colors = new HashMap<NID.WithName, Integer>();
    Iterator<NID.WithName> vit = nodes_.iterator();
    while (vit.hasNext()) {
      NID.WithName node = vit.next();
      colors.put(node, white_);
    }
    
    //
    // Visit each white vertex:
    //
    
    vit = nodes_.iterator();
    while (vit.hasNext()) {
      NID.WithName node = vit.next();
      Integer color = colors.get(node);
      if (color.equals(white_)) {
        if (visit(node, colors)) {
          return (true);
        }
      }
    }
    
    return (false);
  }
 
  /***************************************************************************
  ** 
  ** Visit a node.  Return true if a cycle
  */

  private boolean visit(NID.WithName vertex, Map<NID.WithName, Integer> colors) {
    colors.put(vertex, grey_);
    Set<FabricLink> linksForVertex = linksForNode_.get(vertex);
    if (linksForVertex != null) {
      Iterator<FabricLink> lit = linksForVertex.iterator();
      while (lit.hasNext()) {
        FabricLink link = lit.next();
        Integer targColor = colors.get(link.getTrgID());
        if (targColor.equals(grey_)) {
          return (true);
        } else if (targColor.equals(white_)) {
          if (visit(link.getTrgID(), colors)) {
            return (true);
          }
        }
      }
    }
    colors.put(vertex, black_);
    return (false);
  }
}
