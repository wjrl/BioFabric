/*
**    Copyright (C) 2003-2018 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** Methods for extracting info while building networks
*/

public class BuildExtractor {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Extract nodes
  */
  
  public static Set<NID.WithName> extractNodes(Collection<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs,
                                               BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    Set<NID.WithName> retval = new HashSet<NID.WithName>(loneNodeIDs);
    LoopReporter lr = new LoopReporter(allLinks.size(), 20, monitor, 0.0, 1.0, "progress.analyzingNodes");
    
    for (FabricLink link : allLinks) {
      retval.add(link.getSrcID());
      retval.add(link.getTrgID());
      lr.report();
    }
    lr.finish();  // DO I NEED THIS lr.finish() HERE? THE OTHER STATIC METHODS DON'T HAVE IT
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Extract relations
  */

  public static void extractRelations(List<FabricLink> allLinks, 
  		                                SortedMap<FabricLink.AugRelation, Boolean> relMap, 
  		                                BTProgressMonitor monitor) 
  		                                  throws AsynchExitRequestException {
    HashSet<FabricLink> flipSet = new HashSet<FabricLink>();
    HashSet<FabricLink.AugRelation> flipRels = new HashSet<FabricLink.AugRelation>();
    HashSet<FabricLink.AugRelation> rels = new HashSet<FabricLink.AugRelation>();
    int size = allLinks.size();
    LoopReporter lr = new LoopReporter(size, 20, monitor, 0.0, 1.0, "progress.analyzingRelations");
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      lr.report();
      FabricLink.AugRelation relation = nextLink.getAugRelation();
      if (!nextLink.isFeedback()) {  // Autofeedback not flippable
        FabricLink flipLink = nextLink.flipped();
        if (flipSet.contains(flipLink)) {
          flipRels.add(relation);
        } else {
          flipSet.add(nextLink);
        }
      }
      rels.add(relation);
    } 
    
    //
    // We have a hint that something is signed if there are two
    // separate links running in opposite directions!
    //
        
    Boolean noDir = new Boolean(false);
    Boolean haveDir = new Boolean(true);
    Iterator<FabricLink.AugRelation> rit = rels.iterator();
    while (rit.hasNext()) {
      FabricLink.AugRelation rel = rit.next();
      relMap.put(rel, (flipRels.contains(rel)) ? haveDir : noDir);
    }    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Helper to drop to map to single name: useful
  */

  public static Map<String, NID.WithName> reduceNameSetToOne(Map<String, Set<NID.WithName>> mapsToSets) { 
  	HashMap<String, NID.WithName> retval = new HashMap<String, NID.WithName>();
    Iterator<String> alit = mapsToSets.keySet().iterator();
    while (alit.hasNext()) {
      String nextName = alit.next();
      Set<NID.WithName> forName = mapsToSets.get(nextName);
      if (forName.size() != 1) {
      	throw new IllegalStateException();
      }
      retval.put(nextName, forName.iterator().next());
    }
    return (retval);
  }
  
  
  /***************************************************************************
  ** 
  ** Process a link set that has not had directionality established
  */

  public static void assignDirections(List<FabricLink> allLinks, 
  		                                Map<FabricLink.AugRelation, Boolean> relMap,
  		                                BTProgressMonitor monitor) throws AsynchExitRequestException { 
     
	  int numLink = allLinks.size();
	  LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 1.0, "progress.installDirections");
	 
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      lr.report();
      FabricLink.AugRelation rel = nextLink.getAugRelation();
      Boolean isDir = relMap.get(rel);
      nextLink.installDirection(isDir);
    }
    return;
  }

  /***************************************************************************
  ** 
  ** This culls a set of links to remove non-directional synonymous and
  ** duplicate links.  Note that shadow links have already been created
  ** and added to the allLinks list. 
  */

  public static void preprocessLinks(List<FabricLink> allLinks, Set<FabricLink> retval, Set<FabricLink> culled,
  		                               BTProgressMonitor monitor) throws AsynchExitRequestException {
  	FabricLink.FabLinkComparator flc = new FabricLink.FabLinkComparator();
  	int numLink = allLinks.size();
	  LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 1.0, "progress.cullingAndFlipping");
  	
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      lr.report();
      if (retval.contains(nextLink)) {
        culled.add(nextLink);
      } else if (!nextLink.isDirected()) {
        if (!nextLink.isFeedback()) {
          FabricLink flipLink = nextLink.flipped();
          if (retval.contains(flipLink)) {
            // Make the order consistent for a given src & pair!
            if (flc.compare(nextLink, flipLink) < 0) {
              retval.remove(flipLink);
              culled.add(flipLink);
              retval.add(nextLink);
            } else {
              culled.add(nextLink);              
            }  
          } else {
            retval.add(nextLink);
          }
        } else {
          retval.add(nextLink);
        }
      } else {
        retval.add(nextLink);
      }
    }
    return;
  }
}
