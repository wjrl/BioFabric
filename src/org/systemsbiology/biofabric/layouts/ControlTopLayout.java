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
import java.util.SortedSet;
import java.util.TreeSet;

import org.systemsbiology.biofabric.analysis.CycleFinder;
import org.systemsbiology.biofabric.analysis.GraphSearcher;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** Control nodes at top
*/

public class ControlTopLayout {
  
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

  public ControlTopLayout() {
 
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
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd, List<NID.WithName> forcedTop,
  		                 BTProgressMonitor monitor, 
                       double startFrac, 
                       double endFrac) throws AsynchExitRequestException {   
    doNodeLayout(rbd, forcedTop, monitor, startFrac, endFrac);
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor, startFrac, endFrac);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, List<NID.WithName> forcedTop, 
  		                                   BTProgressMonitor monitor, 
									                       double startFrac, 
									                       double endFrac) throws AsynchExitRequestException {
									    
    List<NID.WithName> targets = orderByNodeDegree(rbd, forcedTop);       
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    (new DefaultLayout()).installNodeOrder(targets, rbd, monitor, startFrac, endFrac);
    return (targets);
  }
  
  /***************************************************************************
  **
  ** Target nodes are ordered by degree. Within degree, targets are arranged
  ** to "odometer" thru the inputs
  */
  
   private List<NID.WithName> orderByNodeDegree(BioFabricNetwork.RelayoutBuildData rbd, List<NID.WithName> forcedTop) {
   
    List<NID.WithName> snSorted = (forcedTop == null) ? controlSort(rbd.allNodeIDs, rbd.allLinks) : forcedTop;
    HashSet<NID.WithName> snSortSet = new HashSet<NID.WithName>(snSorted);
    ArrayList<NID.WithName> outList = new ArrayList<NID.WithName>();
    outList.addAll(snSorted);
    
    UiUtil.fixMePrintout("UGLY HACK");
    
    HashSet<FabricLink> repLin = new HashSet<FabricLink>();
    Iterator<FabricLink> alit = rbd.allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nLink = alit.next();
      if ((forcedTop != null) && !forcedTop.contains(nLink.getSrcID()) && !nLink.isFeedback()) {
        repLin.add(nLink.flipped());
      } else {
        repLin.add(nLink);
      }
    }
    
    GraphSearcher gs = new GraphSearcher(rbd.allNodeIDs, repLin); //rbd.allLinks); 
    SortedSet<GraphSearcher.SourcedNodeDegree> snds = gs.nodeDegreeSetWithSource(snSorted);
      
    Iterator<GraphSearcher.SourcedNodeDegree> dfit = snds.iterator();
    while (dfit.hasNext()) {
      GraphSearcher.SourcedNodeDegree node = dfit.next();
      if (!snSortSet.contains(node.getNode())) {
        outList.add(node.getNode());
      }
    }
    return (outList);
  }
 
  /***************************************************************************
  ** 
  ** Calculate control node order; this will create a partial ordering. Cycles
  ** are currently broken arbitrarily:
  */

  private List<NID.WithName> controlSort(Set<NID.WithName> nodes, Set<FabricLink> links) { 
    
    //
    // Create a set of all the source nodes:
    //
    
    HashSet<NID.WithName> srcs = new HashSet<NID.WithName>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrcID());
    }     
    
    //
    // Create a set of links that are internal to the control
    // subnetwork:
    //
    
    HashSet<FabricLink> ctrlLinks = new HashSet<FabricLink>();
    lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      if (srcs.contains(nextLink.getTrgID())) {
        ctrlLinks.add(nextLink);
      }
    }   
 
    //
    // Create a subset of the control links that form a DAG:
    // Note the order is arbitrary:
    //
    
    HashSet<FabricLink> dagLinks = new HashSet<FabricLink>();
    HashSet<FabricLink> testLinks = new HashSet<FabricLink>();
    lit = ctrlLinks.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      if (nextLink.isShadow()) {
      	continue;
      }
      testLinks.add(nextLink);              
      CycleFinder cf = new CycleFinder(nodes, testLinks);
      if (!cf.hasACycle()) {
        dagLinks.add(nextLink);
      } else {
        testLinks.remove(nextLink);
      }
    }
    
    //
    // Topo sort the nodes:
    //
 
    GraphSearcher gs = new GraphSearcher(srcs, dagLinks);
    Map<NID.WithName, Integer> ts = gs.topoSort(false);
    List<NID.WithName> retval = gs.topoSortToPartialOrdering(ts, links);
    
    //
    // Nodes that were dropped due to cycles still need to be added as 
    // source nodes! FIXME: Should add in degree order, not alpha order!
    //
    //
    
    TreeSet<NID.WithName> remaining = new TreeSet<NID.WithName>(srcs);
    remaining.removeAll(retval);
    retval.addAll(remaining);
 
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Calculate an ordering of nodes that puts the highest degree nodes first:
  */

  private List<NID.WithName> allNodeOrder(Set<NID.WithName> nodes, Set<FabricLink> links) {    
    GraphSearcher gs = new GraphSearcher(nodes, links); 
    List<NID.WithName> retval = gs.nodeDegreeOrder();
    Collections.reverse(retval);
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Order source nodes by median target degree
  */

  private SortedSet<GraphSearcher.NodeDegree> medianTargetDegree(Set<NID.WithName> nodes, Set<FabricLink> links) { 
    
    GraphSearcher gs = new GraphSearcher(nodes, links); 
    Map<NID.WithName, Integer> nDeg = gs.nodeDegree(true);
    
    HashMap<NID.WithName, List<Integer>> deg = new HashMap<NID.WithName, List<Integer>>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      NID.WithName src = nextLink.getSrcID();
      NID.WithName trg = nextLink.getTrgID();
      Integer trgDeg = nDeg.get(trg);
      List<Integer> forSrc = deg.get(src);
      if (forSrc == null) {
        forSrc = new ArrayList<Integer>();
        deg.put(src, forSrc);
      }
      forSrc.add(trgDeg);
    }  
        
    TreeSet<GraphSearcher.NodeDegree> retval = new TreeSet<GraphSearcher.NodeDegree>();
    Iterator<NID.WithName> sit = deg.keySet().iterator();
    while (sit.hasNext()) {
      NID.WithName src = sit.next();
      List<Integer> forSrc = deg.get(src);
      Collections.sort(forSrc);
      int size = forSrc.size();    
      int medI = size / 2;
      Integer med = forSrc.get(medI);
      retval.add(new GraphSearcher.NodeDegree(src, med.intValue()));
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate control node order
  */

  private List<NID.WithName> controlSortDegreeOnly(Set<NID.WithName> nodes, Set<FabricLink> links) { 
    
    HashSet<NID.WithName> srcs = new HashSet<NID.WithName>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrcID());
    }  
       
    HashSet<FabricLink> ctrlLinks = new HashSet<FabricLink>();
    lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      if (srcs.contains(nextLink.getTrgID())) {
        ctrlLinks.add(nextLink);
      }
    }   
 
    GraphSearcher gs = new GraphSearcher(srcs, ctrlLinks); 
    List<NID.WithName> retval = gs.nodeDegreeOrder();
    Collections.reverse(retval);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Return source nodes
  */

  private Set<NID.WithName> controlNodes(Set<NID.WithName> nodes, Set<FabricLink> links) { 
    
    HashSet<NID.WithName> srcs = new HashSet<NID.WithName>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrcID());
    }  
    return (srcs);
  }
  
  /***************************************************************************
  **
  ** Generate breadth-first-order
  */

  private List<NID.WithName> orderBreadth(Set<NID.WithName> nodes, Set<FabricLink> links) {
   
   //   List<String> controlNodes  = cp.controlSortDegreeOnly(nodes, links); //cp.controlSort(nodes, links);
      
    ArrayList<NID.WithName> ctrlList = new ArrayList<NID.WithName>();
    Set<NID.WithName> cnSet = controlNodes(nodes, links);
    
    
    List<NID.WithName> dfo = allNodeOrder(nodes, links);
    Iterator<NID.WithName> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      NID.WithName node = dfit.next();
      if (cnSet.contains(node)) {
        ctrlList.add(node);
      }
    }
    
    GraphSearcher gs = new GraphSearcher(nodes, links);
    List<GraphSearcher.QueueEntry> queue = gs.breadthSearch(ctrlList);
    
    ArrayList<NID.WithName> outList = new ArrayList<NID.WithName>();
    Iterator<GraphSearcher.QueueEntry> qit = queue.iterator();
    while (qit.hasNext()) {
      GraphSearcher.QueueEntry qe = qit.next();
      outList.add(qe.name);
    }
    return (outList);
  }
  
  /***************************************************************************
  **
  ** Test frame
  */

  private List<NID.WithName> orderPureDegree(Set<NID.WithName> nodes, Set<FabricLink> links) {
       
    Set<NID.WithName> cnSet = controlNodes(nodes, links);           
    ArrayList<NID.WithName> outList = new ArrayList<NID.WithName>();

    List<NID.WithName> dfo = allNodeOrder(nodes, links);
    Iterator<NID.WithName> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      NID.WithName node = dfit.next();
      if (cnSet.contains(node)) {
        outList.add(node);
      }
    }
    dfit = dfo.iterator();
    while (dfit.hasNext()) {
      NID.WithName node = dfit.next();
      if (!cnSet.contains(node)) {
        outList.add(node);
      }
    }
    return (outList);
  }
  
  /***************************************************************************
  **
  ** Test frame
  */

  private List<NID.WithName> orderCtrlMedianTargetDegree(Set<NID.WithName> nodes, Set<FabricLink> links) {
 
    Set<NID.WithName> cnSet = controlNodes(nodes, links);           
    ArrayList<NID.WithName> outList = new ArrayList<NID.WithName>();
    
    SortedSet<GraphSearcher.NodeDegree> ctrlMed = medianTargetDegree(nodes, links);
    Iterator<GraphSearcher.NodeDegree> ndit = ctrlMed.iterator();
    while (ndit.hasNext()) {
      GraphSearcher.NodeDegree nodeDeg = ndit.next();
      outList.add(nodeDeg.getNode());
    }
    Collections.reverse(outList);
      
    List<NID.WithName> dfo = allNodeOrder(nodes, links);
    Iterator<NID.WithName> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      NID.WithName node = dfit.next();
      if (!cnSet.contains(node)) {
        outList.add(node);
      }
    }
    return (outList);
  }
  
  /***************************************************************************
  **
  ** Test frame
  */

  private List<NID.WithName> mainSourceSortedTargs(Set<NID.WithName> nodes, Set<FabricLink> links) {
  
    List<NID.WithName> snSorted = controlSort(nodes, links);
    HashSet<NID.WithName> snSortSet = new HashSet<NID.WithName>(snSorted);
    ArrayList<NID.WithName> outList = new ArrayList<NID.WithName>();
    outList.addAll(snSorted);
    
    GraphSearcher gs = new GraphSearcher(nodes, links); 
    SortedSet<GraphSearcher.SourcedNodeDegree> snds = gs.nodeDegreeSetWithSource(snSorted);
      
    Iterator<GraphSearcher.SourcedNodeDegree> dfit = snds.iterator();
    while (dfit.hasNext()) {
      GraphSearcher.SourcedNodeDegree node = dfit.next();
      if (!snSortSet.contains(node.getNode())) {
        outList.add(node.getNode());
      }
    }
    return (outList);
  }
}

