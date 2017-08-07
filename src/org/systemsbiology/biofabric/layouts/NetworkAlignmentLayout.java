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

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/****************************************************************************
 **
 ** This is the default layout algorithm
 */

public class NetworkAlignmentLayout {
  
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
  
  public NetworkAlignmentLayout() {}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public void doLayout(BioFabricNetwork.NetworkAlignmentBuildData rbd, NodeSimilarityLayout.CRParams params,
                       BTProgressMonitor monitor) throws AsynchExitRequestException {
    doNodeLayout(rbd, ((DefaultLayout.Params)params).startNodes, monitor);
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor);
    return;
  }
  
  /***************************************************************************
   **
   ** Relayout the network!
   */
  
  public List<NID.WithName> doNodeLayout(BioFabricNetwork.NetworkAlignmentBuildData rbd,
                                         List<NID.WithName> startNodeIDs,
                                         BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    List<NID.WithName> targetIDs = defaultNodeOrder(rbd.allLinks, rbd.loneNodeIDs, startNodeIDs, monitor);
    
//    Set<FabricLink> alignedLinks = new HashSet<FabricLink>();
//    Set<FabricLink> unalignedLinks = new HashSet<FabricLink>();
//    Set<NID.WithName> alignedLoners = new HashSet<NID.WithName>();
//    Set<NID.WithName> unalignedLoners = new HashSet<NID.WithName>();
//
//    for (FabricLink link : rbd.allLinks) {
//      if (link.getRelation().equals("CCS") || link.getRelation().equals("G1A") || link.getRelation().equals("G2A")) {
//        alignedLinks.add(link);
//      } else{
//        unalignedLinks.add(link);
//      }
//    }
//
//    for (NID.WithName loner : rbd.loneNodeIDs) {
//      if (loner.getName().contains("-")) {
//        alignedLoners.add(loner);
//      } else {
//        unalignedLoners.add(loner);
//      }
//    }
//
//    List<NID.WithName> aligned = defaultNodeOrder(alignedLinks, alignedLoners, null, monitor);
//    List<NID.WithName> unaligned = defaultNodeOrder(unalignedLinks,unalignedLoners,null,monitor);
//    List<NID.WithName> testTargetIDs = new ArrayList<NID.WithName>();
//
//    for (NID.WithName node : aligned) testTargetIDs.add(node); // SET DOES NOT PRESERVE ORDER
//
//    for (NID.WithName node : unaligned){
//      if (!node.getName().contains("-"))
//      testTargetIDs.add(node);
//    }
//
//
//    System.out.println(targetIDs.size() + "   " + testTargetIDs.size() +"  \n\n\n\n\n\n\n\n\n\n");
//    System.out.println(alignedLinks.size() + "  " + unalignedLinks.size()+ " "+rbd.allLinks.size() +"  "+"\n\n\n");
    
    
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
//    installNodeOrder(new ArrayList<NID.WithName>(testTargetIDs), rbd, monitor);
//    return (new ArrayList<NID.WithName>(testTargetIDs));
    installNodeOrder(targetIDs, rbd, monitor);
    return (new ArrayList<NID.WithName>(targetIDs));
  }
  
  /***************************************************************************
   **
   ** Install node orders
   */
  
  public void installNodeOrder(List<NID.WithName> targetIDs, BioFabricNetwork.RelayoutBuildData rbd,
                               BTProgressMonitor monitor) throws AsynchExitRequestException {
    int currRow = 0;
    LoopReporter lr = new LoopReporter(targetIDs.size(), 20, monitor, 0.0, 1.0, "progress.installOrdering");
    
    HashMap<NID.WithName, Integer> nodeOrder = new HashMap<NID.WithName, Integer>();
    Iterator<NID.WithName> trit = targetIDs.iterator();
    while (trit.hasNext()) {
      NID.WithName target = trit.next();
      lr.report();
      Integer rowTag = Integer.valueOf(currRow++);
      nodeOrder.put(target, rowTag);
    }
    rbd.setNodeOrder(nodeOrder);
    lr.finish();
    return;
  }
  
  /***************************************************************************
   **
   ** Calculate default node order
   */
  
  public List<NID.WithName> defaultNodeOrder(Set<FabricLink> allLinks,
                                             Set<NID.WithName> loneNodes,
                                             List<NID.WithName> startNodes,
                                             BTProgressMonitor monitor) throws AsynchExitRequestException {
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
    //
    // Build a target list, top to bottom, that adds the node with the most
    // links first, and adds those link targets ASAP. If caller supplies a start node,
    // we go there first:
    //
    
    HashMap<NID.WithName, Integer> linkCounts = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, Set<NID.WithName>> targsPerSource = new HashMap<NID.WithName, Set<NID.WithName>>();
    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
    
    HashSet<NID.WithName> targsToGo = new HashSet<NID.WithName>();
    
    int numLink = allLinks.size();
    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
    
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      lr.report();
      NID.WithName sidwn = nextLink.getSrcID();
      NID.WithName tidwn = nextLink.getTrgID();
      Set<NID.WithName> targs = targsPerSource.get(sidwn);
      if (targs == null) {
        targs = new HashSet<NID.WithName>();
        targsPerSource.put(sidwn, targs);
      }
      targs.add(tidwn);
      targs = targsPerSource.get(tidwn);
      if (targs == null) {
        targs = new HashSet<NID.WithName>();
        targsPerSource.put(tidwn, targs);
      }
      targs.add(sidwn);
      targsToGo.add(sidwn);
      targsToGo.add(tidwn);
      Integer srcCount = linkCounts.get(sidwn);
      linkCounts.put(sidwn, (srcCount == null) ? Integer.valueOf(1) : Integer.valueOf(srcCount.intValue() + 1));
      Integer trgCount = linkCounts.get(tidwn);
      linkCounts.put(tidwn, (trgCount == null) ? Integer.valueOf(1) : Integer.valueOf(trgCount.intValue() + 1));
    }
    lr.finish();
    
    //
    // Rank the nodes by link count:
    //
    
    lr = new LoopReporter(linkCounts.size(), 20, monitor, 0.25, 0.50, "progress.rankByDegree");
    
    TreeMap<Integer, SortedSet<NID.WithName>> countRank = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    Iterator<NID.WithName> lcit = linkCounts.keySet().iterator();
    while (lcit.hasNext()) {
      NID.WithName src = lcit.next();
      lr.report();
      Integer count = linkCounts.get(src);
      SortedSet<NID.WithName> perCount = countRank.get(count);
      if (perCount == null) {
        perCount = new TreeSet<NID.WithName>();
        countRank.put(count, perCount);
      }
      perCount.add(src);
    }
    lr.finish();
    
    //
    // Handle the specified starting nodes case:
    //
    
    if ((startNodes != null) && !startNodes.isEmpty()) {
      ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
      targsToGo.removeAll(startNodes);
      targets.addAll(startNodes);
      queue.addAll(startNodes);
      flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, 0.50, 0.75);
    }
    
    //
    // Get all kids added in.  Now doing this without recursion; seeing blown
    // stacks for huge networks!
    //
    
    while (!targsToGo.isEmpty()) {
      Iterator<Integer> crit = countRank.keySet().iterator();
      while (crit.hasNext()) {
        Integer key = crit.next();
        SortedSet<NID.WithName> perCount = countRank.get(key);
        Iterator<NID.WithName> pcit = perCount.iterator();
        while (pcit.hasNext()) {
          NID.WithName node = pcit.next();
          if (targsToGo.contains(node)) {
            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
            targsToGo.remove(node);
            targets.add(node);
            addMyKidsNR(targets, targsPerSource, linkCounts, targsToGo, node, queue, monitor, 0.75, 1.0);
          }
        }
      }
    }
    
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
    // GOES AWAY IF remains 190804 targets 281832
    System.err.println("remains " + remains.size() + " targets " + targets.size());
    remains.removeAll(targets);
    System.err.println("remains now " + remains.size());
    targets.addAll(new TreeSet<NID.WithName>(remains));
    return (targets);
  }
  
  /***************************************************************************
   **
   ** Node ordering
   */
  
  private List<NID.WithName> orderMyKids(Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                                         Map<NID.WithName, Integer> linkCounts,
                                         Set<NID.WithName> targsToGo, NID.WithName node) {
    Set<NID.WithName> targs = targsPerSource.get(node);
    if (targs == null) {
      return (new ArrayList<NID.WithName>());
    }
    TreeMap<Integer, SortedSet<NID.WithName>> kidMap = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    Iterator<NID.WithName> tait = targs.iterator();
    while (tait.hasNext()) {
      NID.WithName nextTarg = tait.next();
      Integer count = linkCounts.get(nextTarg);
      SortedSet<NID.WithName> perCount = kidMap.get(count);
      if (perCount == null) {
        perCount = new TreeSet<NID.WithName>();
        kidMap.put(count, perCount);
      }
      perCount.add(nextTarg);
    }
    
    ArrayList<NID.WithName> myKidsToProc = new ArrayList<NID.WithName>();
    Iterator<SortedSet<NID.WithName>> kmit = kidMap.values().iterator();
    while (kmit.hasNext()) {
      SortedSet<NID.WithName> perCount = kmit.next();
      Iterator<NID.WithName> pcit = perCount.iterator();
      while (pcit.hasNext()) {
        NID.WithName kid = pcit.next();
        if (targsToGo.contains(kid)) {
          myKidsToProc.add(kid);
        }
      }
    }
    return (myKidsToProc);
  }
  
  /***************************************************************************
   **
   ** Node ordering, non-recursive:
   */
  
  private void addMyKidsNR(List<NID.WithName> targets, Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                           Map<NID.WithName, Integer> linkCounts,
                           Set<NID.WithName> targsToGo, NID.WithName node, List<NID.WithName> queue,
                           BTProgressMonitor monitor, double startFrac, double endFrac)
          throws AsynchExitRequestException {
    queue.add(node);
    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, startFrac, endFrac);
    return;
  }
  
  /***************************************************************************
   **
   ** Node ordering, non-recursive:
   */
  
  private void flushQueue(List<NID.WithName> targets,
                          Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                          Map<NID.WithName, Integer> linkCounts,
                          Set<NID.WithName> targsToGo, List<NID.WithName> queue,
                          BTProgressMonitor monitor, double startFrac, double endFrac)
          throws AsynchExitRequestException {
    
    LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
    int lastSize = targsToGo.size();
    while (!queue.isEmpty()) {
      NID.WithName node = queue.remove(0);
      int ttgSize = targsToGo.size();
      lr.report(lastSize - ttgSize);
      lastSize = ttgSize;
      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      Iterator<NID.WithName> ktpit = myKids.iterator();
      while (ktpit.hasNext()) {
        NID.WithName kid = ktpit.next();
        if (targsToGo.contains(kid)) {
          targsToGo.remove(kid);
          targets.add(kid);
          queue.add(kid);
        }
      }
    }
    lr.finish();
    return;
  }
  
  /***************************************************************************
   **
   ** For passing around layout params
   */
  
  public static class Params implements NodeSimilarityLayout.CRParams {
    
    public List<NID.WithName> startNodes;
    
    public Params(List<NID.WithName> startNodes) {
      this.startNodes = startNodes;
    }
  }
  
}
