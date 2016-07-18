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
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;

/****************************************************************************
**
** This is a hierarchical layout of a DAG
*/

public class HierDAGLayout {
  
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

   private Map<String, Set<String>> l2s_;
   private Map<String, Integer> inDegs_;
   private Map<String, Integer> outDegs_;
   private ArrayList<String> placeList_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HierDAGLayout() {
     l2s_ = new HashMap<String, Set<String>>();
     inDegs_ = new HashMap<String, Integer>();
     outDegs_ = new HashMap<String, Integer>();
     placeList_ = new ArrayList<String>();
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
    
    List<String> targets = orderByNodeDegree(rbd);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    (new DefaultLayout()).installNodeOrder(targets, rbd);
    return (targets);
  }
  

  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<String> orderByNodeDegree(BioFabricNetwork.RelayoutBuildData rbd) {
    
    HashSet<String> nodesToGo = new HashSet<String>(rbd.allNodes);      
    linksToSources(rbd.allNodes, rbd.allLinks);
    List<String> placeList = extractRoots();
    addToPlaceList(placeList);
    nodesToGo.removeAll(placeList);
    
    //
    // Find the guys whose cites have already been placed and place them:
    //
    
    while (!nodesToGo.isEmpty()) {     
      List<String> nextBatch = findNextCandidates();
      addToPlaceList(nextBatch);
      nodesToGo.removeAll(nextBatch);
      System.out.println("Nodes to Go = " + nodesToGo.size());
    }
    
    return (placeList_);
    
  }
  
  /***************************************************************************
  ** 
  ** Build the set of guys we are looking at and degrees
  */

  public Map<String, Set<String>> linksToSources(Set<String> nodeList, Set<FabricLink> linkList) {   
      
    Iterator<String> nit = nodeList.iterator();
    while (nit.hasNext()) {
      String node = nit.next();
      l2s_.put(node, new HashSet<String>());
      inDegs_.put(node, new Integer(0));
      outDegs_.put(node, new Integer(0));
    } 
    
    Iterator<FabricLink> llit = linkList.iterator();
    while (llit.hasNext()) {
      FabricLink link = llit.next();
      String src = link.getSrc();
      String trg = link.getTrg();
      Set<String> toTarg = l2s_.get(src);
      toTarg.add(trg);
      Integer deg = outDegs_.get(src);
      outDegs_.put(src, new Integer(deg.intValue() + 1));
      deg = inDegs_.get(trg);
      inDegs_.put(trg, new Integer(deg.intValue() + 1)); 
    } 
    return (l2s_);
  }
  
  /***************************************************************************
  ** 
  ** Add to list to place
  */

  public void addToPlaceList(List<String> nextBatch) {         
    placeList_.addAll(nextBatch);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Extract the root nodes in order from highest degree to low
  */

  public List<String> extractRoots() {
 
    Map<String, Integer> roots = new HashMap<String, Integer>();
      
    Iterator<String> lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      Set<String> fn = l2s_.get(node);
      if (fn.isEmpty()) {
        roots.put(node, new Integer(0));
      }
    } 
    
    lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      Set<String> fn = l2s_.get(node);
      Iterator<String> sit = fn.iterator();
      while (sit.hasNext()) {
        String trg = sit.next();
        Integer rs = roots.get(trg);
        if (rs != null) {
          roots.put(trg, new Integer(rs.intValue() + 1));          
        }
      }
    } 
    
    ArrayList<String> buildList = new ArrayList<String>();
    
    int count = 1;
    while (buildList.size() < roots.size()) {
      TreeSet<String> alpha = new TreeSet<String>(Collections.reverseOrder());
      alpha.addAll(roots.keySet());
      Iterator<String> rit = alpha.iterator();
      while (rit.hasNext()) {
        String node = rit.next();
        Integer val = roots.get(node);
        if (val.intValue() == count) {
          buildList.add(node);
        }
      }
      count++;
    }
  
    Collections.reverse(buildList);
    return (buildList);
  }
  
  /***************************************************************************
  ** 
  ** Find the next guys to go:
  */

  public List<String> findNextCandidates() {
 
    HashSet<String> quickie = new HashSet<String>(placeList_);
     
    TreeSet<SourcedNode> nextOut = new TreeSet<SourcedNode>(Collections.reverseOrder());
    
    Iterator<String> lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      if (quickie.contains(node)) {
        continue;
      }
      Set<String> fn = l2s_.get(node);
      boolean allThere = true;
      Iterator<String> sit = fn.iterator();
      while (sit.hasNext()) {
        String trg = sit.next();
        if (!quickie.contains(trg)) {
          allThere = false;
          break;
        }
      }
      if (allThere) {
        nextOut.add(new SourcedNode(node));
      }
    } 
  
    ArrayList<String> retval = new ArrayList<String>();
    Iterator<SourcedNode> noit = nextOut.iterator();
    while (noit.hasNext()) {
      SourcedNode sn = noit.next();
      retval.add(sn.getNode());
    }
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /****************************************************************************
  **
  ** A Class
  */
  
  public class SourcedNode implements Comparable<SourcedNode> {
    
    private String node_;

    
    public SourcedNode(String node) {
      node_ = node;
    }
    
    public String getNode() {
      return (node_);
    }
    
    @Override
    public int hashCode() {
      return (node_.hashCode());
    }
  
    @Override
    public String toString() {
      return (" node = " + node_);
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNode)) {
        return (false);
      }
      SourcedNode otherDeg = (SourcedNode)other;    
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(SourcedNode otherDeg) {
      
      //
      // Same name, same node:
      //
      
      if (this.node_.equals(otherDeg.node_)) {
        return (0);
      }
      
      Set<String> mySet = l2s_.get(this.node_);
      Set<String> hisSet = l2s_.get(otherDeg.node_);
      
      TreeSet<Integer> myOrder = new TreeSet<Integer>(); 
      TreeSet<Integer> hisOrder = new TreeSet<Integer>(); 
      int numNode = placeList_.size();
      for (int i = 0; i < numNode; i++) {
        String node = placeList_.get(i);
        if (mySet.contains(node)) {
          myOrder.add(new Integer(i));
        }
        if (hisSet.contains(node)) {
          hisOrder.add(new Integer(i));
        }
      } 
      
      ArrayList<Integer> myList = new ArrayList<Integer>(myOrder);
      ArrayList<Integer> hisList = new ArrayList<Integer>(hisOrder);
      
      
      int mySize = myOrder.size();
      int hisSize = hisOrder.size();
      int min = Math.min(mySize, hisSize);
      for (int i = 0; i < min; i++) {
        int myVal = myList.get(i).intValue();
        int hisVal = hisList.get(i).intValue();
        int diff = hisVal - myVal;
        if (diff != 0) {
          return (diff);
        }
      }
      
      int diffSize = hisSize - mySize;
      if (diffSize != 0) {
        return (diffSize);
      }
      
      int myIn = inDegs_.get(this.node_);
      int hisIn = inDegs_.get(otherDeg.node_);
      int diffIn = myIn - hisIn;
      if (diffIn != 0) {
        return (diffIn);
      }
      
      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    } 
  }
}
