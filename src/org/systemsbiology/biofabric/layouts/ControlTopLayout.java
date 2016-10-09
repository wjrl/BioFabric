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
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd, List<String> forcedTop) {   
    doNodeLayout(rbd, forcedTop);
    (new DefaultEdgeLayout()).layoutEdges(rbd);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<String> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, List<String> forcedTop) {
    
    List<String> targets = orderByNodeDegree(rbd, forcedTop);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    Iterator<String> tit = targets.iterator();
    while (tit.hasNext()) {
//      System.out.println(tit.next());
    }
    
    
    (new DefaultLayout()).installNodeOrder(targets, rbd);
    return (targets);
  }
  
  /***************************************************************************
  **
  ** Target nodes are ordered by degree. Within degree, targets are arranged
  ** to "odometer" thru the inputs
  */
  
   private List<String> orderByNodeDegree(BioFabricNetwork.RelayoutBuildData rbd, List<String> forcedTop) {
   
    List<String> snSorted = (forcedTop == null) ? controlSort(rbd.allNodes, rbd.allLinks) : forcedTop;
    HashSet<String> snSortSet = new HashSet<String>(snSorted);
    ArrayList<String> outList = new ArrayList<String>();
    outList.addAll(snSorted);
    
    UiUtil.fixMePrintout("UGH HACK");
    
    HashSet<FabricLink> repLin = new HashSet<FabricLink>();
    Iterator<FabricLink> alit = rbd.allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nLink = alit.next();
      if ((forcedTop != null) && !forcedTop.contains(nLink.getSrc()) && !nLink.isFeedback()) {
        repLin.add(nLink.flipped());
      } else {
        repLin.add(nLink);
      }
    }
    
    GraphSearcher gs = new GraphSearcher(rbd.allNodes, repLin); //rbd.allLinks); 
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

  private List<String> controlSort(Set<String> nodes, Set<FabricLink> links) { 
    
    //
    // Create a set of all the source nodes:
    //
    
    HashSet<String> srcs = new HashSet<String>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrc());
    }     
    
    //
    // Create a set of links that are internal to the control
    // subnetwork:
    //
    
    HashSet<FabricLink> ctrlLinks = new HashSet<FabricLink>();
    lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      if (srcs.contains(nextLink.getTrg())) {
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
//        System.out.println("***remove " + nextLink);
        testLinks.remove(nextLink);
      }
    }
    
    //
    // Topo sort the nodes:
    //
 
    GraphSearcher gs = new GraphSearcher(srcs, dagLinks);
    Map<String, Integer> ts = gs.topoSort(false);
    List<String> retval = gs.topoSortToPartialOrdering(ts, links);
    int count = 0;
    for (String ct : retval) {
//      System.out.println("***ctrl " + count++ + " " + ct);
    }
    
    //
    // Nodes that were dropped due to cycles still need to be added as 
    // source nodes! FIXME: Should add in degree order, not alpha order!
    //
    //
    
    TreeSet<String> remaining = new TreeSet<String>(srcs);
    remaining.removeAll(retval);
    retval.addAll(remaining);
 
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Calculate an ordering of nodes that puts the highest degree nodes first:
  */

  private List<String> allNodeOrder(Set<String> nodes, Set<FabricLink> links) {    
    GraphSearcher gs = new GraphSearcher(nodes, links); 
    List<String> retval = gs.nodeDegreeOrder();
    Collections.reverse(retval);
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Order source nodes by median target degree
  */

  private SortedSet<GraphSearcher.NodeDegree> medianTargetDegree(Set<String> nodes, Set<FabricLink> links) { 
    
    GraphSearcher gs = new GraphSearcher(nodes, links); 
    Map<String, Integer> nDeg = gs.nodeDegree(true);
    
    HashMap<String, List<Integer>> deg = new HashMap<String, List<Integer>>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      String src = nextLink.getSrc();
      String trg = nextLink.getTrg();
      Integer trgDeg = nDeg.get(trg);
      List<Integer> forSrc = deg.get(src);
      if (forSrc == null) {
        forSrc = new ArrayList<Integer>();
        deg.put(src, forSrc);
      }
      forSrc.add(trgDeg);
    }  
        
    TreeSet<GraphSearcher.NodeDegree> retval = new TreeSet<GraphSearcher.NodeDegree>();
    Iterator<String> sit = deg.keySet().iterator();
    while (sit.hasNext()) {
      String src = sit.next();
      List<Integer> forSrc = deg.get(src);
      Collections.sort(forSrc);
      int size = forSrc.size();    
      int medI = size / 2;
      Integer med = forSrc.get(medI);
      retval.add(new GraphSearcher.NodeDegree(src, med.intValue()));
//      System.out.println(src + ": " + forSrc.get(0) + " - " + forSrc.get(forSrc.size() - 1) + " median = " + med);
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate control node order
  */

  private List<String> controlSortDegreeOnly(Set<String> nodes, Set<FabricLink> links) { 
    
    HashSet<String> srcs = new HashSet<String>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrc());
    }  
       
    HashSet<FabricLink> ctrlLinks = new HashSet<FabricLink>();
    lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      if (srcs.contains(nextLink.getTrg())) {
        ctrlLinks.add(nextLink);
      }
    }   
 
    GraphSearcher gs = new GraphSearcher(srcs, ctrlLinks); 
    List<String> retval = gs.nodeDegreeOrder();
    Collections.reverse(retval);
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Return source nodes
  */

  private Set<String> controlNodes(Set<String> nodes, Set<FabricLink> links) { 
    
    HashSet<String> srcs = new HashSet<String>();
    Iterator<FabricLink> lit = links.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      srcs.add(nextLink.getSrc());
    }  
    return (srcs);
  }
  
  /***************************************************************************
  **
  ** Generate breadth-first-order
  */

  private List<String> orderBreadth(Set<String> nodes, Set<FabricLink> links) {
   
   //   List<String> controlNodes  = cp.controlSortDegreeOnly(nodes, links); //cp.controlSort(nodes, links);
      
    ArrayList<String> ctrlList = new ArrayList<String>();
    Set<String> cnSet = controlNodes(nodes, links);
    
    
    List<String> dfo = allNodeOrder(nodes, links);
    Iterator<String> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      String node = dfit.next();
      if (cnSet.contains(node)) {
        ctrlList.add(node);
      }
    }
    
    GraphSearcher gs = new GraphSearcher(nodes, links);
    List<GraphSearcher.QueueEntry> queue = gs.breadthSearch(ctrlList);
    
    ArrayList<String> outList = new ArrayList<String>();
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

  private List<String> orderPureDegree(Set<String> nodes, Set<FabricLink> links) {
       
    Set<String> cnSet = controlNodes(nodes, links);           
    ArrayList<String> outList = new ArrayList<String>();

    List<String> dfo = allNodeOrder(nodes, links);
    Iterator<String> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      String node = dfit.next();
      if (cnSet.contains(node)) {
        outList.add(node);
      }
    }
    dfit = dfo.iterator();
    while (dfit.hasNext()) {
      String node = dfit.next();
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

  private List<String> orderCtrlMedianTargetDegree(Set<String> nodes, Set<FabricLink> links) {
 
    Set<String> cnSet = controlNodes(nodes, links);           
    ArrayList<String> outList = new ArrayList<String>();
    
    SortedSet<GraphSearcher.NodeDegree> ctrlMed = medianTargetDegree(nodes, links);
    Iterator<GraphSearcher.NodeDegree> ndit = ctrlMed.iterator();
    while (ndit.hasNext()) {
      GraphSearcher.NodeDegree nodeDeg = ndit.next();
      outList.add(nodeDeg.getNode());
    }
    Collections.reverse(outList);
      
    List<String> dfo = allNodeOrder(nodes, links);
    Iterator<String> dfit = dfo.iterator();
    while (dfit.hasNext()) {
      String node = dfit.next();
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

  private List<String> mainSourceSortedTargs(Set<String> nodes, Set<FabricLink> links) {
  
    List<String> snSorted = controlSort(nodes, links);
    HashSet<String> snSortSet = new HashSet<String>(snSorted);
    ArrayList<String> outList = new ArrayList<String>();
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

