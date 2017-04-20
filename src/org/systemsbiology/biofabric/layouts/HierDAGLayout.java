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
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.NID;

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

   private Map<NID.WithName, Set<NID.WithName>> l2s_;
   private Map<NID.WithName, Integer> inDegs_;
   private Map<NID.WithName, Integer> outDegs_;
   private ArrayList<NID.WithName> placeList_;
  
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
     l2s_ = new HashMap<NID.WithName, Set<NID.WithName>>();
     inDegs_ = new HashMap<NID.WithName, Integer>();
     outDegs_ = new HashMap<NID.WithName, Integer>();
     placeList_ = new ArrayList<NID.WithName>();
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
    doNodeLayout(rbd, monitor, startFrac, endFrac);
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor, startFrac, endFrac);
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
    
    List<NID.WithName> targets = orderByNodeDegree(rbd);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    (new DefaultLayout()).installNodeOrder(targets, rbd, monitor, startFrac, endFrac);
    return (targets);
  }
  

  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> orderByNodeDegree(BioFabricNetwork.RelayoutBuildData rbd) {
    
    HashSet<NID.WithName> nodesToGo = new HashSet<NID.WithName>(rbd.allNodeIDs);      
    linksToSources(rbd.allNodeIDs, rbd.allLinks);
    List<NID.WithName> placeList = extractRoots();
    addToPlaceList(placeList);
    nodesToGo.removeAll(placeList);
    
    //
    // Find the guys whose cites have already been placed and place them:
    //
    
    while (!nodesToGo.isEmpty()) {     
      List<NID.WithName> nextBatch = findNextCandidates();
      addToPlaceList(nextBatch);
      nodesToGo.removeAll(nextBatch);
    }
    
    return (placeList_);
    
  }
  
  /***************************************************************************
  ** 
  ** Build the set of guys we are looking at and degrees
  */

  public Map<NID.WithName, Set<NID.WithName>> linksToSources(Set<NID.WithName> nodeList, Set<FabricLink> linkList) {   
      
    Iterator<NID.WithName> nit = nodeList.iterator();
    while (nit.hasNext()) {
      NID.WithName node = nit.next();
      l2s_.put(node, new HashSet<NID.WithName>());
      inDegs_.put(node, Integer.valueOf(0));
      outDegs_.put(node, Integer.valueOf(0));
    } 
    
    Iterator<FabricLink> llit = linkList.iterator();
    while (llit.hasNext()) {
      FabricLink link = llit.next();
      NID.WithName src = link.getSrcID();
      NID.WithName trg = link.getTrgID();
      Set<NID.WithName> toTarg = l2s_.get(src);
      toTarg.add(trg);
      Integer deg = outDegs_.get(src);
      outDegs_.put(src, Integer.valueOf(deg.intValue() + 1));
      deg = inDegs_.get(trg);
      inDegs_.put(trg, Integer.valueOf(deg.intValue() + 1)); 
    } 
    return (l2s_);
  }
  
  /***************************************************************************
  ** 
  ** Add to list to place
  */

  public void addToPlaceList(List<NID.WithName> nextBatch) {         
    placeList_.addAll(nextBatch);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Extract the root nodes in order from highest degree to low
  */

  public List<NID.WithName> extractRoots() {
 
    Map<NID.WithName, Integer> roots = new HashMap<NID.WithName, Integer>();
      
    Iterator<NID.WithName> lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      NID.WithName node = lit.next();
      Set<NID.WithName> fn = l2s_.get(node);
      if (fn.isEmpty()) {
        roots.put(node, Integer.valueOf(0));
      }
    } 
    
    lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      NID.WithName node = lit.next();
      Set<NID.WithName> fn = l2s_.get(node);
      Iterator<NID.WithName> sit = fn.iterator();
      while (sit.hasNext()) {
        NID.WithName trg = sit.next();
        Integer rs = roots.get(trg);
        if (rs != null) {
          roots.put(trg, Integer.valueOf(rs.intValue() + 1));          
        }
      }
    } 
    
    ArrayList<NID.WithName> buildList = new ArrayList<NID.WithName>();
    
    int count = 1;
    while (buildList.size() < roots.size()) {
      TreeSet<NID.WithName> alpha = new TreeSet<NID.WithName>(Collections.reverseOrder());
      alpha.addAll(roots.keySet());
      Iterator<NID.WithName> rit = alpha.iterator();
      while (rit.hasNext()) {
        NID.WithName node = rit.next();
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

  public List<NID.WithName> findNextCandidates() {
 
    HashSet<NID.WithName> quickie = new HashSet<NID.WithName>(placeList_);
     
    TreeSet<SourcedNode> nextOut = new TreeSet<SourcedNode>(Collections.reverseOrder());
    
    Iterator<NID.WithName> lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      NID.WithName node = lit.next();
      if (quickie.contains(node)) {
        continue;
      }
      Set<NID.WithName> fn = l2s_.get(node);
      boolean allThere = true;
      Iterator<NID.WithName> sit = fn.iterator();
      while (sit.hasNext()) {
        NID.WithName trg = sit.next();
        if (!quickie.contains(trg)) {
          allThere = false;
          break;
        }
      }
      if (allThere) {
        nextOut.add(new SourcedNode(node));
      }
    } 
  
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
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
    
    private NID.WithName node_;

    
    public SourcedNode(NID.WithName node) {
      node_ = node;
    }
    
    public NID.WithName getNode() {
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
      
      Set<NID.WithName> mySet = l2s_.get(this.node_);
      Set<NID.WithName> hisSet = l2s_.get(otherDeg.node_);
      
      TreeSet<Integer> myOrder = new TreeSet<Integer>(); 
      TreeSet<Integer> hisOrder = new TreeSet<Integer>(); 
      int numNode = placeList_.size();
      for (int i = 0; i < numNode; i++) {
        NID.WithName node = placeList_.get(i);
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
