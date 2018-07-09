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

import org.systemsbiology.biofabric.io.BuildData;
import org.systemsbiology.biofabric.modelInterface.NetLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This does the "world bank" layout
*/

public class WorldBankLayout extends NodeLayout {
  
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
  ** Find out if the necessary conditions for this layout are met. 
  */
  
  @Override
  public boolean criteriaMet(BuildData.RelayoutBuildData rbd,
                             BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                               LayoutCriterionFailureException {
    //
    // 1) This is going to have problems with two connected nodes of degree 1.
    // 2) Other topology issues???
    //
    // NOTE: If we want to reproduce the true world bank network, we need to assign in-country,
    // between two-country, and global links to different link groups. 
    // 
    // NOTE 2: Note the "popular" hubs in the world bank network ARE NOT THEMSELVES INTERACTING. 
    
    
    LoopReporter lr = new LoopReporter(rbd.allLinks.size(), 20, monitor, 0.0, 1.0, "progress.hDagLayoutCriteriaCheck");
    
    for (NetLink aLink : rbd.allLinks) {
      lr.report();
      //if (!aLink.isDirected()) {
       // throw new LayoutCriterionFailureException();
     // }
    }
    lr.finish();
    
   // CycleFinder cf = new CycleFinder(rbd.allNodeIDs, rbd.allLinks, monitor);
  //  if (cf.hasACycle(monitor)) {
   //   throw new LayoutCriterionFailureException();
   // }
    return (true);  
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> doNodeLayout(BuildData.RelayoutBuildData rbd,
  																			 Params params,
  		                                   BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    List<NID.WithName> targets = calcNodeOrder(rbd.allLinks, rbd.loneNodeIDs, monitor);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    installNodeOrder(targets, rbd, monitor);
    return (targets);
  }
  
  /***************************************************************************
  ** 
  ** Bump count
  */

  private void bumpDaCount(Map<NID.WithName, Integer> countMap, NID.WithName dNode) {    
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

  private void addANeighbor(Map<NID.WithName, SortedSet<NID.WithName>> neighMap, NID.WithName daNode, NID.WithName daNeigh) {    
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
  ** Take the node->degree map and invert to create a decreasing-order 
  ** degree->sortedSet(node) map:
  */

  private SortedMap<Integer, SortedSet<NID.WithName>> invertCountMap(Map<NID.WithName, Integer> countMap, 
  		                                                               BTProgressMonitor monitor) throws AsynchExitRequestException { 

    LoopReporter lr = new LoopReporter(countMap.size(), 20, monitor, 0.0, 1.0, "progress.sortingByDegree");
  	    
  	TreeMap<Integer, SortedSet<NID.WithName>> retval = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    for (NID.WithName daKey : countMap.keySet()) {
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

  private List<NID.WithName> flattenDaCount(SortedMap<Integer, SortedSet<NID.WithName>> invCountMap) { 
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

  private List<NID.WithName> calcNodeOrder(Set<NetLink> allLinks, Set<NID.WithName> loneNodes,
  		                                     BTProgressMonitor monitor) throws AsynchExitRequestException {
 
    HashMap<NID.WithName, Integer> node2Degree = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, SortedSet<NID.WithName>> node2Neighbor = new HashMap<NID.WithName, SortedSet<NID.WithName>>(); 
    HashSet<NID.WithName> allNodes = new HashSet<NID.WithName>();
    
    LoopReporter lr = new LoopReporter(allLinks.size(), 20, monitor, 0.0, 1.0, "progress.wblCalcNodeOrder");

    for (NetLink nextLink : allLinks) {
      lr.report();
      NID.WithName source = nextLink.getSrcID();
      NID.WithName target = nextLink.getTrgID();
 
      //
      // Track all link-related nodes:
      //
      
      allNodes.add(source);
      allNodes.add(target);
      
      //
      // Build the node2Degree map:
      //
      
      bumpDaCount(node2Degree, source);
      bumpDaCount(node2Degree, target);
      
      //
      // Record the neighbors for all the node:
      //
      addANeighbor(node2Neighbor, source, target);
      addANeighbor(node2Neighbor, target, source);
    }
    lr.finish();
    
    SortedMap<Integer, SortedSet<NID.WithName>> degree2Nodes = invertCountMap(node2Degree, monitor);
    
    //
    // For nodes that have *one* neighbor, collect those popular neighbors (i.e. hubs):
    //
    
    LoopReporter lr2 = new LoopReporter(node2Neighbor.size(), 20, monitor, 0.0, 1.0, "progress.wblPopNeighbors");
    
    HashMap<NID.WithName, Set<NID.WithName>> oneNeighbor = new HashMap<NID.WithName, Set<NID.WithName>>();
    for (NID.WithName node : node2Neighbor.keySet()) {
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
    
    
    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
    HashSet<NID.WithName> tSet = new HashSet<NID.WithName>(); // For rapid lookup
    
    //
    // Do this in decreasing degree order for the popular neighbors (hubs):
    //
    
    Set<NID.WithName> populars = oneNeighbor.keySet();
    
    for (Integer deg : degree2Nodes.keySet()) {
      lr3.report();
      //
      if (deg.intValue() == 1) {
         
        
        
        UiUtil.fixMePrintout("DO SOMETHING: Don't want 1-deg node added twice!");
      }
      //
      // Crank thru the populars at a particular degree:
      //
      SortedSet<NID.WithName> forDeg = degree2Nodes.get(deg);
      for (NID.WithName degNode : forDeg) {
        if (populars.contains(degNode)) {
          targets.add(degNode); // Add the popular node
          tSet.add(degNode);
          HashMap<NID.WithName, Integer> forDaPop = new HashMap<NID.WithName, Integer>();
          // Get the unpopular friends:
          Set<NID.WithName> unpopFriends = oneNeighbor.get(degNode);
          for (NID.WithName unPop : unpopFriends) {
            Integer upd = node2Degree.get(unPop);
            forDaPop.put(unPop, upd);            
          }
          //
          // Unpopular friends are ordered by decreasing degree. Note that this is not separating
          // out the friends into two separate sets (e.g. in-country versus between country) as
          // is done in the classic world-bank layout. That requires link groups to be defined:
          //
          SortedMap<Integer, SortedSet<NID.WithName>> invFor = invertCountMap(forDaPop, null);
          List<NID.WithName> fdc = flattenDaCount(invFor);
          targets.addAll(fdc); 
          tSet.addAll(fdc);
        }
      }
    }
    lr3.finish();
 
    //
    // Handle all remaining unplaced (linked) nodes! This is purely in order of decreasing degree.
    //
    
    HashSet<NID.WithName> stillToPlace = new HashSet<NID.WithName>(allNodes);
    stillToPlace.removeAll(targets);

    LoopReporter lr4 = new LoopReporter(degree2Nodes.size(), 0, monitor, 0.0, 1.0, "progress.addingGlobalNodes");
    
    for (SortedSet<NID.WithName> fdeg : degree2Nodes.values()) {
      lr4.report();
      for (NID.WithName chkNode : fdeg) {
        if (stillToPlace.contains(chkNode)) {
          targets.add(chkNode);
        }
      }
    }
    lr4.finish();

    //
    //
    // Tag on lone nodes. If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
    remains.removeAll(targets);
    targets.addAll(new TreeSet<NID.WithName>(remains));
    return (targets);
  } 
}
