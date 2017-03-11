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
  		                 BTProgressMonitor monitor, 
                       double startFrac, 
                       double endFrac) throws AsynchExitRequestException { 

  	double perStep = (endFrac - startFrac) / 2.0;
  	System.out.println("doLays");   
    doNodeLayout(rbd, monitor, startFrac, startFrac + perStep);
    System.out.println("doLays2");   
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor, startFrac + perStep, endFrac);
    System.out.println("doLays3");   
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd,
  		                                   BTProgressMonitor monitor, 
									                       double startFrac, 
									                       double endFrac) throws AsynchExitRequestException {
    
    List<NID.WithName> targets = calcNodeOrder(rbd.allLinks, rbd.loneNodeIDs, monitor, startFrac, endFrac);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    (new DefaultLayout()).installNodeOrder(targets, rbd);
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

  public SortedMap<Integer, SortedSet<NID.WithName>> invertDaCount(Map<NID.WithName, Integer> countMap) { 
    TreeMap<Integer, SortedSet<NID.WithName>> retval = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    Iterator<NID.WithName> cmit = countMap.keySet().iterator();
    while (cmit.hasNext()) {
      NID.WithName daKey = cmit.next();
      Integer daCount = countMap.get(daKey);
      SortedSet<NID.WithName> forCount = retval.get(daCount);
      if (forCount == null) {
        forCount = new TreeSet<NID.WithName>();
        retval.put(daCount, forCount);
      }
      forCount.add(daKey);
    }
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
  		                                    BTProgressMonitor monitor, 
									                        double startFrac, 
									                        double endFrac) throws AsynchExitRequestException {
 
    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
    HashMap<NID.WithName, Integer> node2Degree = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, SortedSet<NID.WithName>> node2Neighbor = new HashMap<NID.WithName, SortedSet<NID.WithName>>(); 
    HashSet<NID.WithName> allNodes = new HashSet<NID.WithName>();
    
    double perLoop = (endFrac - startFrac) / 4.0;
    
    System.out.println("fracs " + startFrac + " " + endFrac + "  " + perLoop);
    double currStart1 = startFrac;
    double currEnd1 = currStart1 + perLoop;
    double inc1 = (currEnd1 - currStart1) / ((allLinks.size() == 0) ? 1 : allLinks.size());
    double currProg1 = currStart1;

    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      NID.WithName source = nextLink.getSrcID();
      NID.WithName target = nextLink.getTrgID();
 
      allNodes.add(source);
      allNodes.add(target);
      
      bumpDaCount(node2Degree, source);
      bumpDaCount(node2Degree, target);
      
      addANeighbor(node2Neighbor, source, target);
      addANeighbor(node2Neighbor, target, source);
      
      if (monitor != null) {
        currProg1 += inc1;
        if (!monitor.updateProgress((int)(currProg1 * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    System.out.println("currEnd1 " + currEnd1);
    SortedMap<Integer, SortedSet<NID.WithName>> degree2Nodes = invertDaCount(node2Degree);
     
    double currStart2 = currEnd1;
    double currEnd2 = currStart2 + perLoop;
    double inc2 = (currEnd2 - currStart2) / ((node2Neighbor.size() == 0) ? 1 : node2Neighbor.size());
    double currProg2 = currStart2;
    
    //
    // For nodes that have one neighbor, collect those popular neighbors:
    //
    
    HashMap<NID.WithName, Set<NID.WithName>> oneNeighbor = new HashMap<NID.WithName, Set<NID.WithName>>();
    Iterator<NID.WithName> nit = node2Neighbor.keySet().iterator();
    while (nit.hasNext()) {
      NID.WithName node = nit.next();
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
      
      if (monitor != null) {
        currProg2 += inc2;
        if (!monitor.updateProgress((int)(currProg2 * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
        System.out.println("currEnd2 " + currEnd2);
    double currStart3 = currEnd2;
    double currEnd3 = currStart3 + perLoop;
    double inc3 = (currEnd3 - currStart3) / ((degree2Nodes.size() == 0) ? 1 : degree2Nodes.size());
    double currProg3 = currStart3;
    
    Iterator<Integer> degit = degree2Nodes.keySet().iterator();
    while (degit.hasNext()) {
      Integer deg = degit.next();
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
          SortedMap<Integer, SortedSet<NID.WithName>> invFor = invertDaCount(forDaPop);
          targets.addAll(flattenDaCount(invFor));         
        }
        
        if (monitor != null) {
	        currProg3 += inc3;
	        if (!monitor.updateProgress((int)(currProg3 * 100.0))) {
	          throw new AsynchExitRequestException();
	        }
	      }
      }
    }
     
        System.out.println("currEnd3 " + currEnd3);
    double currStart4 = currEnd3;
    double currEnd4 = currStart4 + perLoop;
    double inc4 = (currEnd4 - currStart4) / ((degree2Nodes.size() == 0) ? 1 : degree2Nodes.size());
    double currProg4 = currStart4;
    
    HashSet<NID.WithName> stillToPlace = new HashSet<NID.WithName>(allNodes);
    stillToPlace.removeAll(targets);

    Iterator<SortedSet<NID.WithName>> icmit = degree2Nodes.values().iterator();
    while (icmit.hasNext()) {
      SortedSet<NID.WithName> fdeg = icmit.next();
      Iterator<NID.WithName> fdit = fdeg.iterator();
      while (fdit.hasNext()) {
        NID.WithName chkNode = fdit.next();
        if (stillToPlace.contains(chkNode)) {
          targets.add(chkNode);
        }
      }
      
      if (monitor != null) {
        currProg4 += inc4;
        if (!monitor.updateProgress((int)(currProg4 * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    System.out.println("currEnd4 " + currEnd4);   
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
