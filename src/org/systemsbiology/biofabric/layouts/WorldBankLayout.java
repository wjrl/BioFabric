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

package org.systemsbiology.biofabric.layouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This does the "world bank" layout
*/

public class WorldBankLayout {
  
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

  public WorldBankLayout() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd,                            
  		                 BTProgressMonitor monitor) throws AsynchExitRequestException { 

  	System.out.println("doLays");   
    doNodeLayout(rbd, monitor);
    System.out.println("doLays2");   
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor);
    System.out.println("doLays3");   
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd,
  		                                   BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    List<NID.WithName> targets = calcNodeOrder(rbd.allLinks, rbd.loneNodeIDs, monitor);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    (new DefaultLayout()).installNodeOrder(targets, rbd, monitor);
    return (targets);
  }
  
  /***************************************************************************
  ** 
  ** Bump count
  */

  public void bumpDaCount(Map<NID.WithName, Integer> countMap, NID.WithName dNode) {    
    Integer inc = countMap.get(dNode);
    if (inc == null) {
      countMap.put(dNode, Integer.valueOf(1));
    } else {
      countMap.put(dNode, Integer.valueOf(inc.intValue() + 1));
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Track neighbors
  */

  public void addANeighbor(Map<NID.WithName, SortedSet<NID.WithName>> neighMap, NID.WithName daNode, NID.WithName daNeigh) {    
    SortedSet<NID.WithName> forNode = neighMap.get(daNode);
    if (forNode == null) {
      forNode = new TreeSet<NID.WithName>();
      neighMap.put(daNode, forNode);
    }
    forNode.add(daNeigh);
    return;
  }

  /***************************************************************************
  ** 
  ** Invert a map
  */

  public SortedMap<Integer, SortedSet<NID.WithName>> invertDaCount(Map<NID.WithName, Integer> countMap, 
  		                                                             BTProgressMonitor monitor) throws AsynchExitRequestException { 

    LoopReporter lr = new LoopReporter(countMap.size(), 20, monitor, 0.0, 1.0, "progress.sortingByDegree");
  	    
  	TreeMap<Integer, SortedSet<NID.WithName>> retval = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    Iterator<NID.WithName> cmit = countMap.keySet().iterator();
    while (cmit.hasNext()) {
      NID.WithName daKey = cmit.next();
      lr.report();
      Integer daCount = countMap.get(daKey);
      SortedSet<NID.WithName> forCount = retval.get(daCount);
      if (forCount == null) {
        forCount = new TreeSet<NID.WithName>();
        retval.put(daCount, forCount);
      }
      forCount.add(daKey);
    }
    lr.finish();
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Flatten a map
  */

  public List<NID.WithName> flattenDaCount(SortedMap<Integer, SortedSet<NID.WithName>> invCountMap) { 
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    Iterator<SortedSet<NID.WithName>> icmit = invCountMap.values().iterator();
    while (icmit.hasNext()) {
      retval.addAll(icmit.next());
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate node order
  */

  public List<NID.WithName> calcNodeOrder(Set<FabricLink> allLinks, Set<NID.WithName> loneNodes,
  		                                    BTProgressMonitor monitor) throws AsynchExitRequestException {
 
    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
    HashMap<NID.WithName, Integer> node2Degree = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, SortedSet<NID.WithName>> node2Neighbor = new HashMap<NID.WithName, SortedSet<NID.WithName>>(); 
    HashSet<NID.WithName> allNodes = new HashSet<NID.WithName>();
    
    LoopReporter lr = new LoopReporter(allLinks.size(), 20, monitor, 0.0, 1.0, "progress.wblCalcNodeOrder");

    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      lr.report();
      NID.WithName source = nextLink.getSrcID();
      NID.WithName target = nextLink.getTrgID();
 
      allNodes.add(source);
      allNodes.add(target);
      
      bumpDaCount(node2Degree, source);
      bumpDaCount(node2Degree, target);
      
      addANeighbor(node2Neighbor, source, target);
      addANeighbor(node2Neighbor, target, source);
    }
    lr.finish();
    
    SortedMap<Integer, SortedSet<NID.WithName>> degree2Nodes = invertDaCount(node2Degree, monitor);
    
    //
    // For nodes that have one neighbor, collect those popular neighbors:
    //
    
    LoopReporter lr2 = new LoopReporter(node2Neighbor.size(), 20, monitor, 0.0, 1.0, "progress.wblPopNeighbors");
    
    HashMap<NID.WithName, Set<NID.WithName>> oneNeighbor = new HashMap<NID.WithName, Set<NID.WithName>>();
    Iterator<NID.WithName> nit = node2Neighbor.keySet().iterator();
    while (nit.hasNext()) {
      NID.WithName node = nit.next();
      lr2.report();
      SortedSet<NID.WithName> nextDoor = node2Neighbor.get(node);
      if (nextDoor.size() == 1) {        
        NID.WithName popular = nextDoor.first();
        Set<NID.WithName> popFriends = oneNeighbor.get(popular);
        if (popFriends == null) {
          popFriends = new HashSet<NID.WithName>();
          oneNeighbor.put(popular, popFriends);
        }
        popFriends.add(node);
      }
    }
    lr2.finish();
    
    LoopReporter lr3 = new LoopReporter(degree2Nodes.size(), 0, monitor, 0.0, 1.0, "progress.addingSatellites");
    
    Iterator<Integer> degit = degree2Nodes.keySet().iterator();
    while (degit.hasNext()) {
      Integer deg = degit.next();
      lr3.report();
      SortedSet<NID.WithName> forDeg = degree2Nodes.get(deg);
      Iterator<NID.WithName> fdit = forDeg.iterator();
      while (fdit.hasNext()) {
        NID.WithName degNode = fdit.next();
        if (oneNeighbor.keySet().contains(degNode)) {
          targets.add(degNode);
          HashMap<NID.WithName, Integer> forDaPop = new HashMap<NID.WithName, Integer>();
          Set<NID.WithName> unpopFriends = oneNeighbor.get(degNode);
          Iterator<NID.WithName> upfit = unpopFriends.iterator();
          while (upfit.hasNext()) {
            NID.WithName unPop = upfit.next();
            Integer upd = node2Degree.get(unPop);
            forDaPop.put(unPop, upd);            
          }
          SortedMap<Integer, SortedSet<NID.WithName>> invFor = invertDaCount(forDaPop, null);
          targets.addAll(flattenDaCount(invFor));         
        }
      }
    }
    lr3.finish();
 
    HashSet<NID.WithName> stillToPlace = new HashSet<NID.WithName>(allNodes);
    stillToPlace.removeAll(targets);

    LoopReporter lr4 = new LoopReporter(degree2Nodes.size(), 0, monitor, 0.0, 1.0, "progress.addingGlobalNodes");
    
    Iterator<SortedSet<NID.WithName>> icmit = degree2Nodes.values().iterator();
    while (icmit.hasNext()) {
      SortedSet<NID.WithName> fdeg = icmit.next();
      lr4.report();
      Iterator<NID.WithName> fdit = fdeg.iterator();
      while (fdit.hasNext()) {
        NID.WithName chkNode = fdit.next();
        if (stillToPlace.contains(chkNode)) {
          targets.add(chkNode);
        }
      }
    }
    lr4.finish();

    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
    remains.removeAll(targets);
    targets.addAll(new TreeSet<NID.WithName>(remains));
    return (targets);
  } 
}
