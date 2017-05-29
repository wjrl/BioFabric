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

import org.systemsbiology.biofabric.analysis.CycleFinder;
import org.systemsbiology.biofabric.analysis.GraphSearcher;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This is a hierarchical layout for a directed acyclic graph
*/

public class HierDAGLayout {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

   private Map<NID.WithName, Set<NID.WithName>> l2s_;
   private Map<NID.WithName, Integer> inDegs_;
   private Map<NID.WithName, Integer> outDegs_;
   private ArrayList<NID.WithName> placeList_;
   private HashMap<NID.WithName, Integer> nameToRow_;
   private boolean pointUp_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public HierDAGLayout(boolean pointUp) {
     l2s_ = new HashMap<NID.WithName, Set<NID.WithName>>();
     inDegs_ = new HashMap<NID.WithName, Integer>();
     outDegs_ = new HashMap<NID.WithName, Integer>();
     placeList_ = new ArrayList<NID.WithName>();
     nameToRow_ = new HashMap<NID.WithName, Integer>();
     pointUp_ = pointUp;
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
  
  public boolean criteriaMet(BioFabricNetwork.RelayoutBuildData rbd,
  		                     BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                             LayoutCriterionFailureException {
  	//
  	// 1) All the relations in the network are directed
  	// 2) There are no cycles in the network
  	//
	  
	  LoopReporter lr = new LoopReporter(rbd.allLinks.size(), 20, monitor, 0.0, 1.0, "progress.hDagLayoutCriteriaCheck");
	  
    for (FabricLink aLink : rbd.allLinks) {
      lr.report();
      if (!aLink.isDirected()) {
    	  throw new LayoutCriterionFailureException();
      }
    }
    lr.finish();
	  
  	CycleFinder cf = new CycleFinder(rbd.allNodeIDs, rbd.allLinks, monitor);
    if (cf.hasACycle(monitor)) {
      throw new LayoutCriterionFailureException();
    }
    return (true); 	
  }

  /***************************************************************************
  **
  ** Layout the network
  */
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd,
  		                 BTProgressMonitor monitor) throws AsynchExitRequestException {
    doNodeLayout(rbd, monitor);
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor);
    return;
  }
  
  /***************************************************************************
  **
  ** Generate the Node ordering
  */
  
  private List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, 
  		   																  BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    List<NID.WithName> targets = orderByNodeDegree(rbd, monitor);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    (new DefaultLayout()).installNodeOrder(targets, rbd, monitor);
    return (targets);
  }
  

  /***************************************************************************
  **
  ** Get the ordering of nodes by node degree:
  */
  
  private List<NID.WithName> orderByNodeDegree(BioFabricNetwork.RelayoutBuildData rbd, 
  		                                         BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    HashSet<NID.WithName> nodesToGo = new HashSet<NID.WithName>(rbd.allNodeIDs);
    
    // Build map of sources to targets, also record in and out degrees of each node:
    linksToSources(rbd.allNodeIDs, rbd.allLinks, monitor);
    
    List<NID.WithName> placeList = extractRoots(monitor);
    addToPlaceList(placeList);
    nodesToGo.removeAll(placeList);
    
    //
    // Find the guys whose cites have already been placed and place them:
    //
  
    LoopReporter lr = new LoopReporter(nodesToGo.size(), 20, monitor, 0.0, 1.0, "progress.findingCandidates");
    
    while (!nodesToGo.isEmpty()) {
      List<NID.WithName> nextBatch = findNextCandidates();
      lr.report(nextBatch.size());
      addToPlaceList(nextBatch);
      nodesToGo.removeAll(nextBatch);
    }
    lr.finish();
    
    return (placeList_);
    
  }
  
  /***************************************************************************
  ** 
  ** Construct a map of the targets of each node. Note that instance members
  */

  private void linksToSources(Set<NID.WithName> nodeList, Set<FabricLink> linkList,
  		                        BTProgressMonitor monitor) throws AsynchExitRequestException {
    
  	//
  	// For each node, we initialize a map of nodes it is pointing at, and initialize the inDegree and outDegree
  	// entries for it as well:
  	//
  	
  	LoopReporter lr = new LoopReporter(nodeList.size(), 20, monitor, 0.0, 1.0, "progress.hDagLayoutInit");
  	
    Iterator<NID.WithName> nit = nodeList.iterator();
    while (nit.hasNext()) {
      NID.WithName node = nit.next();
      lr.report();
      l2s_.put(node, new HashSet<NID.WithName>());
      inDegs_.put(node, Integer.valueOf(0));
      outDegs_.put(node, Integer.valueOf(0));
    } 
    
    //
    // Crank thru the links, accumulate degrees and targets
    //
    
    LoopReporter lr2 = new LoopReporter(linkList.size(), 20, monitor, 0.0, 1.0, "progress.hDagDegAndTargs");
    
    Iterator<FabricLink> llit = linkList.iterator();
    while (llit.hasNext()) {
      FabricLink link = llit.next();
      lr2.report();
      //
      // By default, layout designed to have links point up. Quick way to switch this
      // is to reverse semantics of source and target:
      //
      NID.WithName src = (pointUp_) ? link.getSrcID() : link.getTrgID();
      NID.WithName trg = (pointUp_) ? link.getTrgID() : link.getSrcID();
      Set<NID.WithName> toTarg = l2s_.get(src);
      toTarg.add(trg);
      Integer deg = outDegs_.get(src);
      outDegs_.put(src, Integer.valueOf(deg.intValue() + 1));
      deg = inDegs_.get(trg);
      inDegs_.put(trg, Integer.valueOf(deg.intValue() + 1)); 
    } 
    return;
  }
  
  /***************************************************************************
  ** 
  ** Add to list to place
  */

  public void addToPlaceList(List<NID.WithName> nextBatch) {
    int nextRow = placeList_.size();
    for (NID.WithName nextNode : nextBatch) {
      placeList_.add(nextNode);
      nameToRow_.put(nextNode, Integer.valueOf(nextRow++));
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Extract the root nodes in order from highest degree to low
  */

  public List<NID.WithName> extractRoots(BTProgressMonitor monitor) throws AsynchExitRequestException {
 
    LoopReporter lr = new LoopReporter(l2s_.size(), 20, monitor, 0.0, 1.0, "progress.rootExtractPass1");
    
    Map<NID.WithName, Integer> roots = new HashMap<NID.WithName, Integer>();
      
    Iterator<NID.WithName> lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      NID.WithName node = lit.next();
      lr.report();
      Set<NID.WithName> fn = l2s_.get(node);
      if (fn.isEmpty()) {
        roots.put(node, Integer.valueOf(0));
      }
    }
    lr.finish();
    
    
    LoopReporter lr2 = new LoopReporter(l2s_.size(), 20, monitor, 0.0, 1.0, "progress.rootExtractPass2");
    lit = l2s_.keySet().iterator();
    while (lit.hasNext()) {
      NID.WithName node = lit.next();
      lr2.report();
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
    lr2.finish();
    
    ArrayList<NID.WithName> buildList = new ArrayList<NID.WithName>();
    
    LoopReporter lr3 = new LoopReporter(roots.size(), 20, monitor, 0.0, 1.0, "progress.rootExtractPass3");
    int count = 1;
    TreeSet<NID.WithName> alpha = new TreeSet<NID.WithName>(Collections.reverseOrder());
    alpha.addAll(roots.keySet());
    while (buildList.size() < roots.size()) {
      for (NID.WithName node : alpha) {
        Integer val = roots.get(node);
        if (val.intValue() == count) {
          buildList.add(node);
          lr3.report();
        }
      }
      count++;
    }
    lr3.finish();
    Collections.reverse(buildList);
    return (buildList);
  }
  
  /***************************************************************************
  ** 
  ** Find the next guys to go:
  */

  public List<NID.WithName> findNextCandidates() {
 
    HashSet<NID.WithName> quickie = new HashSet<NID.WithName>(placeList_);
     
    ArrayList<GraphSearcher.SourcedNode> nextOutList = new ArrayList<GraphSearcher.SourcedNode>();
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
        nextOutList.add(new GraphSearcher.SourcedNode(node, inDegs_, nameToRow_, l2s_));
      }
    }
    
    //
    // Order the nodes:
    //
    
    TreeSet<GraphSearcher.SourcedNode> nextOut = new TreeSet<GraphSearcher.SourcedNode>(Collections.reverseOrder());
    nextOut.addAll(nextOutList);
    
    //
    // Make them a list:
    //
   
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    for (GraphSearcher.SourcedNode sn : nextOut) {
      retval.add(sn.getNode());
    }
    return (retval);
  }
}
