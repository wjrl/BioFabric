/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

/****************************************************************************
**
** This does the world bank layout
*/

public class ProcessWorldBankCSV {
  
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

  public ProcessWorldBankCSV() {
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
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd) {   
    doNodeLayout(rbd);
    (new DefaultEdgeLayout()).layoutEdges(rbd);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<String> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd) {
    
    List<String> targets = calcNodeOrder(rbd.allLinks, rbd.loneNodes);       

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

  public void bumpDaCount(Map<String, Integer> countMap, String dNode) {    
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

  public void addANeighbor(Map<String, SortedSet<String>> neighMap, String daNode, String daNeigh) {    
    SortedSet<String> forNode = neighMap.get(daNode);
    if (forNode == null) {
      forNode = new TreeSet<String>();
      neighMap.put(daNode, forNode);
    }
    forNode.add(daNeigh);
    return;
  }

  /***************************************************************************
  ** 
  ** Invert a map
  */

  public SortedMap<Integer, SortedSet<String>> invertDaCount(Map<String, Integer> countMap) { 
    TreeMap<Integer, SortedSet<String>> retval = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
    Iterator<String> cmit = countMap.keySet().iterator();
    while (cmit.hasNext()) {
      String daKey = cmit.next();
      Integer daCount = countMap.get(daKey);
      SortedSet<String> forCount = retval.get(daCount);
      if (forCount == null) {
        forCount = new TreeSet<String>();
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

  public List<String> flattenDaCount(SortedMap<Integer, SortedSet<String>> invCountMap) { 
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<SortedSet<String>> icmit = invCountMap.values().iterator();
    while (icmit.hasNext()) {
      retval.addAll(icmit.next());
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate node order
  */

  public List<String> calcNodeOrder(Set<FabricLink> allLinks, Set<String> loneNodes) {    
 
    ArrayList<String> targets = new ArrayList<String>();
    HashMap<String, Integer> node2Degree = new HashMap<String, Integer>();
    HashMap<String, SortedSet<String>> node2Neighbor = new HashMap<String, SortedSet<String>>(); 
    HashSet<String> allNodes = new HashSet<String>();
    
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      String source = nextLink.getSrc();
      String target = nextLink.getTrg();
 
      allNodes.add(source);
      allNodes.add(target);
      
      bumpDaCount(node2Degree, source);
      bumpDaCount(node2Degree, target);
      
      addANeighbor(node2Neighbor, source, target);
      addANeighbor(node2Neighbor, target, source);
    }
    
    SortedMap<Integer, SortedSet<String>> degree2Nodes = invertDaCount(node2Degree);
     
    //
    // For nodes that have one neighbor, collect those popular neighbors:
    //
    
    HashMap<String, Set<String>> oneNeighbor = new HashMap<String, Set<String>>();
    Iterator<String> nit = node2Neighbor.keySet().iterator();
    while (nit.hasNext()) {
      String node = nit.next();
      SortedSet<String> nextDoor = node2Neighbor.get(node);
      if (nextDoor.size() == 1) {        
        String popular = nextDoor.first();
        Set<String> popFriends = oneNeighbor.get(popular);
        if (popFriends == null) {
          popFriends = new HashSet<String>();
          oneNeighbor.put(popular, popFriends);
        }
        popFriends.add(node);
      }
    }
    
    Iterator<Integer> degit = degree2Nodes.keySet().iterator();
    while (degit.hasNext()) {
      Integer deg = degit.next();
      SortedSet<String> forDeg = degree2Nodes.get(deg);
      Iterator<String> fdit = forDeg.iterator();
      while (fdit.hasNext()) {
        String degNode = fdit.next();
        if (oneNeighbor.keySet().contains(degNode)) {
          targets.add(degNode);
          HashMap<String, Integer> forDaPop = new HashMap<String, Integer>();
          Set<String> unpopFriends = oneNeighbor.get(degNode);
          Iterator<String> upfit = unpopFriends.iterator();
          while (upfit.hasNext()) {
            String unPop = upfit.next();
            Integer upd = node2Degree.get(unPop);
            forDaPop.put(unPop, upd);            
          }
          SortedMap<Integer, SortedSet<String>> invFor = invertDaCount(forDaPop);
          targets.addAll(flattenDaCount(invFor));         
        }
      }
    }
        
    HashSet<String> stillToPlace = new HashSet<String>(allNodes);
    stillToPlace.removeAll(targets);

    Iterator<SortedSet<String>> icmit = degree2Nodes.values().iterator();
    while (icmit.hasNext()) {
      SortedSet<String> fdeg = icmit.next();
      Iterator<String> fdit = fdeg.iterator();
      while (fdit.hasNext()) {
        String chkNode = fdit.next();
        if (stillToPlace.contains(chkNode)) {
          targets.add(chkNode);
        }
      }  
    }
       
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<String> remains = new HashSet<String>(loneNodes);
    remains.removeAll(targets);
    targets.addAll(new TreeSet<String>(remains));
    return (targets);
  } 
}
