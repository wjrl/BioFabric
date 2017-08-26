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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.systemsbiology.biofabric.analysis.NetworkAlignment.COVERED_EDGE;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.GRAPH1;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.INDUCED_GRAPH2;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.HALF_UNALIGNED_GRAPH2;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.FULL_UNALIGNED_GRAPH2;

/****************************************************************************
 **
 ** This is the default layout algorithm
 */

public class NetworkAlignmentLayout extends NodeLayout {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private SortedMap<Integer, List<NID.WithName>> classToGroup; // temporary instance variables
  private BioFabricNetwork.NetworkAlignmentBuildData nabd;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Constructor
   */
  
  public NetworkAlignmentLayout() {
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
  
  public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, Params params, BTProgressMonitor monitor)
          throws AsynchExitRequestException {
    
    nabd = (BioFabricNetwork.NetworkAlignmentBuildData) rbd;

//    List<NID.WithName> targetIDs = nodeGroupByClass(nabd, monitor);
//    List<NID.WithName> targetIDs = nodeGroupBFS(nabd, monitor);
    List<NID.WithName> targetIDs = defaultNodeOrder(nabd.allLinks, nabd.loneNodeIDs, null, monitor);
    
    installNodeOrder(targetIDs, nabd, monitor);
    return (new ArrayList<NID.WithName>(targetIDs));
  }
  
  /***************************************************************************
   **
   **
   */
  
  private List<NID.WithName> nodeGroupBFS(BioFabricNetwork.NetworkAlignmentBuildData nabd, BTProgressMonitor monitor)
          throws AsynchExitRequestException {
    
    return new nodeGroupBFSLayout(nabd).process();
    
  }
  
  
  private class nodeGroupBFSLayout {
    
    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
    NetAlignNodeGrouper grouper;
    BioFabricNetwork.NetworkAlignmentBuildData nabd;
    
    nodeGroupBFSLayout(BioFabricNetwork.NetworkAlignmentBuildData nabd) {
      nodeToLinks_ = new HashMap<NID.WithName, Set<FabricLink>>();
      nodeToNeighbors_ = new HashMap<NID.WithName, Set<NID.WithName>>();
      
      grouper = new NetAlignNodeGrouper(nabd);
      this.nabd = nabd;
      
      for (FabricLink link : nabd.allLinks) {
        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
        
        if (nodeToLinks_.get(src) == null) {
          nodeToLinks_.put(src, new HashSet<FabricLink>());
        }
        if (nodeToLinks_.get(trg) == null) {
          nodeToLinks_.put(trg, new HashSet<FabricLink>());
        }
        if (nodeToNeighbors_.get(src) == null) {
          nodeToNeighbors_.put(src, new HashSet<NID.WithName>());
        }
        if (nodeToNeighbors_.get(trg) == null) {
          nodeToNeighbors_.put(trg, new HashSet<NID.WithName>());
        }
        
        nodeToLinks_.get(src).add(link);
        nodeToLinks_.get(trg).add(link);
        nodeToNeighbors_.get(src).add(trg);
        nodeToNeighbors_.get(trg).add(src);
      }
      
      for (NID.WithName node : nabd.loneNodeIDs) {
        nodeToLinks_.put(node, new HashSet<FabricLink>());
        nodeToNeighbors_.put(node, new HashSet<NID.WithName>());
      }
      return;
    }
    
    List<NID.WithName> process() throws AsynchExitRequestException {
      
      Map<NID.WithName, Boolean> visited = new HashMap<NID.WithName, Boolean>();
      List<NID.WithName> nodes = new ArrayList<NID.WithName>(BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, null));
      Collections.sort(nodes, new Comparator<NID.WithName>() {
        @Override
        public int compare(NID.WithName o1, NID.WithName o2) {
          return nodeToNeighbors_.get(o2).size() - nodeToNeighbors_.get(o1).size();
        }
      });
      
      List<NID.WithName> targetIDs = new ArrayList<NID.WithName>();
      
      for (int i = 0; i < nodes.size(); i++) {
        
        if (visited.get(nodes.get(i)) == null) {
          continue;
        }
        
        Queue<NID.WithName> queue = new LinkedList<NID.WithName>();
        queue.add(nodes.remove(0));
        
        while (! queue.isEmpty()) {
          NID.WithName node = queue.poll();
          
          if (visited.get(node)) {
            continue;
          }
          visited.put(node, true);
          
          List<NID.WithName> neighbors = new ArrayList<NID.WithName>(nodeToNeighbors_.get(node));
          Collections.sort(neighbors, new Comparator<NID.WithName>() {
            @Override
            public int compare(NID.WithName neigh1, NID.WithName neigh2) {
              int neigh1Group = grouper.getNodeGroup(neigh1), neigh2Group = grouper.getNodeGroup(neigh2);
              
              if (neigh1Group != neigh2Group) {
                return neigh1Group - neigh2Group; // increasing group assignment
              } else {
                return nodeToNeighbors_.get(neigh1Group).size() - nodeToNeighbors_.get(neigh2Group).size();
              }
            }
          });
          
          targetIDs.add(node);
          for (NID.WithName neighs : neighbors) {
            targetIDs.add(neighs);
          }
          
        }
      }
      
      
      return targetIDs;
    }
    
  }
  
  /***************************************************************************
   **
   **
   */
  
  private List<NID.WithName> nodeGroupByClass(BioFabricNetwork.NetworkAlignmentBuildData nabd, BTProgressMonitor monitor)
          throws AsynchExitRequestException {
    
    NetAlignNodeGrouper grouper = new NetAlignNodeGrouper(nabd);
    
    ArrayList<NID.WithName>[] classToGroup; // list should be set, but error checking for now. . .
    
    classToGroup = new ArrayList[NUMBER_NODE_GROUPS + 1];
    for (int i = 0; i < classToGroup.length; i++) {
      classToGroup[i] = new ArrayList<NID.WithName>();
    }
    
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
    
    for (NID.WithName node : allNodes) {
      
      int nodeClass = grouper.getNodeGroup(node);
      
      classToGroup[nodeClass].add(node);
    }
    
    List<NID.WithName> targetIDs = new ArrayList<NID.WithName>();
    
    for (int i = 1; i < classToGroup.length; i++) {
      
      if (classToGroup[i].size() == 0) {
        System.out.println("Empty Class:" + i + '\n');
      }
      
      Set<NID.WithName> setForm = new TreeSet<NID.WithName>(classToGroup[i]);
      if (setForm.size() != classToGroup[i].size()) {
        throw new IllegalStateException("different sizes classes");
      }
      
      grouper.sortByDegree(classToGroup[i]);
      
      for (NID.WithName node : classToGroup[i]) {
        if (targetIDs.contains(node)) {
          throw new IllegalStateException("seeing contains");
        }
        targetIDs.add(node);
      }
    }
    
    return targetIDs;
  }
  
  
  /***************************************************************************
   **
   ** Calculate default node order. Used by several other layout classes
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
    
    grouper = new NetworkAlignmentLayout.NetAlignNodeGrouper(allLinks, loneNodes);
    
    
    classToGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) {
      classToGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
    for (NID.WithName node : allNodes) {
      
      int nodeClass = grouper.getNodeGroup(node);
      classToGroup.get(nodeClass).add(node);
    }
    
    for (List<NID.WithName> group : classToGroup.values()) {
      grouper.sortByDegree(group);
    }
    
    //
    // Handle the specified starting nodes case:
    //
    
    SortedMap<Integer, List<NID.WithName>> targetsGroup = new TreeMap<Integer, List<NID.WithName>>();
    SortedMap<Integer, List<NID.WithName>> queueGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
      targetsGroup.put(i, new ArrayList<NID.WithName>());
      queueGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    SortedMap<Integer, List<NID.WithName>> targsLeftToGoGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) { // skip loners
      targsLeftToGoGroup.put(i, new ArrayList<NID.WithName>());
      for (NID.WithName node : classToGroup.get(i)) {
        targsLeftToGoGroup.get(i).add(node);
      }
    }

//    startNodes.add(classToGroup.get(1).get(0)); // highest degree {P:P} DON'T FORGET TO CHECK IF IT EXISTS
//
//    if ((startNodes != null) && !startNodes.isEmpty()) {
////      ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//      targsToGo.removeAll(startNodes);
//      targsLeftToGoGroup.get(1).remove(startNodes.get(0));
//
//      targetsGroup.get(1).addAll(startNodes);
//
//
//      queueGroup.get(1).addAll(startNodes);
//      flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup, monitor, 0.50, 0.75,
//              allLinks, loneNodes, 1);
//    }


////    while (! targsToGo.isEmpty()) {
//    Iterator<Integer> crit = countRank.keySet().iterator();
//    while (crit.hasNext()) {
//      Integer key = crit.next();
//      SortedSet<NID.WithName> perCount = countRank.get(key);
//      Iterator<NID.WithName> pcit = perCount.iterator();
//      while (pcit.hasNext()) {
//        NID.WithName node = pcit.next();
//        if (targsToGo.contains(node)) {
////            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//
//          targsToGo.remove(node);
//
//          int group = grouper.getNodeGroup(node);
//          targetsGroup.get(group).add(node);
//
//
//          addMyKidsNR(targetsGroup, targsPerSource, linkCounts, targsToGo, node, targsLeftToGoGroup, queueGroup, monitor,
//                  0.75, 1.0, allLinks, loneNodes);
//        }
//      }
//    }
////    }

//    int group = grouper.getNodeGroup(node);
//    queueGroup.get(group).add(node);
//    // do loop here
//    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {  // skip loners for now
//
//      while (!targsToGoGroup.get(group).isEmpty()) {
//
//        // add from targsToGoGroup
//
//        flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsToGoGroup, queueGroup, monitor, startFrac, endFrac,
//                allLinks, loneNodes, i);
//      }
//    }
    
    
//    NID.WithName head = classToGroup.get(1).get(0); // highest degree {P:P} DON'T FORGET TO CHECK IF IT EXISTS

//    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
    {
      int currGroup = 1;
      while (currGroup < NUMBER_NODE_GROUPS) {
        
//        flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup, monitor, 0, 1.0,
//                allLinks, loneNodes, i);
    
//        if (! targsLeftToGoGroup.get(currGroup).isEmpty()) {
          
          if (targetsGroup.get(currGroup).size() < classToGroup.get(currGroup).size()) {
          
          if (queueGroup.get(currGroup).isEmpty()) {
  
            System.out.println("Empty group " + currGroup);
  
            NID.WithName next = targsLeftToGoGroup.get(currGroup).remove(0);
            targsToGo.remove(next);
  
            targetsGroup.get(currGroup).add(next);
            queueGroup.get(currGroup).add(next);
          }
  
          flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup, monitor, 0, 1.0,
                  allLinks, loneNodes, currGroup);
  
//          System.out.println("iteration:  " + i);
      
          continue;
        }
        currGroup++;
      }
    }
    
    
    List<NID.WithName> targets = new ArrayList<NID.WithName>();
    targets.addAll(classToGroup.get(0));
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
      for (NID.WithName node : targetsGroup.get(i)) {
        targets.add(node);
      }
    }
    targets.addAll(classToGroup.get(19));
    
    if (targets.size() != allNodes.size()) {
      System.out.println(targets.size() + "    " + allNodes.size() + "  size error\n\n\n\n");
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
   ** Node ordering, non-recursive:
   */
  
  private void addMyKidsNR(SortedMap<Integer, List<NID.WithName>> targetsGroup, Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                           Map<NID.WithName, Integer> linkCounts,
                           Set<NID.WithName> targsToGo, NID.WithName node, SortedMap<Integer, Set<NID.WithName>> targsToGoGroup,
                           SortedMap<Integer, List<NID.WithName>> queueGroup,
                           BTProgressMonitor monitor, double startFrac, double endFrac, Set<FabricLink> allLinks,
                           Set<NID.WithName> loneNodes)
          throws AsynchExitRequestException {
    
    int group = grouper.getNodeGroup(node);
    queueGroup.get(group).add(node);
    // do loop here
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {  // skip loners for now
      
      while (! targsToGoGroup.get(group).isEmpty()) {
        
        
        // add from targsToGoGroup
        
//        flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsToGoGroup, queueGroup, monitor, startFrac, endFrac,
//                allLinks, loneNodes, i);
      }
    }

//    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, startFrac, endFrac, allLinks,
//            loneNodes);
    return;
  }
  
  /***************************************************************************
   **
   ** Node ordering, non-recursive:
   */
  
  private void flushQueue(SortedMap<Integer, List<NID.WithName>> targetsGroup,
                          Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                          Map<NID.WithName, Integer> linkCounts,
                          Set<NID.WithName> targsToGo, SortedMap<Integer, List<NID.WithName>> targsLeftToGoGroup,
                          SortedMap<Integer, List<NID.WithName>> queuesGroup,
                          BTProgressMonitor monitor, double startFrac, double endFrac, Set<FabricLink> allLinks,
                          Set<NID.WithName> loneNodes, int currGroup)
          throws AsynchExitRequestException {

//    NetAlignNodeGrouper grouper = new NetAlignNodeGrouper(allLinks, loneNodes);
    
    LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
    int lastSize = targsToGo.size();
    List<NID.WithName> queue = queuesGroup.get(currGroup);
    
    while (! queue.isEmpty()) {
      NID.WithName node = queue.remove(0);
      int ttgSize = targsLeftToGoGroup.get(currGroup).size();
      lr.report(lastSize - ttgSize);
      lastSize = ttgSize;
      int parentGroup = grouper.getNodeGroup(node);
      
      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node, allLinks, loneNodes);
      Iterator<NID.WithName> ktpit = myKids.iterator();
      while (ktpit.hasNext()) {
        NID.WithName kid = ktpit.next();
        
        if (targsToGo.contains(kid)) {
          
          int kidGroup = grouper.getNodeGroup(kid);
          if (parentGroup == kidGroup) {
  
            targsToGo.remove(kid);
            targsLeftToGoGroup.get(currGroup).remove(kid);
            
            queue.add(kid);
            
            targetsGroup.get(parentGroup).add(kid);
//            System.out.println("added kid");
            
          } else {
            if (kidGroup < parentGroup) {
              System.out.println("GROUP ERROR \n\n\n\n");
            }
            queuesGroup.get(kidGroup).add(kid);
  
            targsToGo.remove(kid);
//            targsLeftToGoGroup.get(kidGroup).remove(kid);
          }
          
//          int group = grouper.getNodeGroup(kid);
//          targetsGroup.get(group).add(kid);
        }
        if (queue.size() > classToGroup.get(parentGroup).size()) {
          System.out.println("QUEUE ERR \n\n\n\n");
        }
      }
    }
    lr.finish();
    return;
  }
  
  /***************************************************************************
   **
   ** Node ordering
   */
  
  private List<NID.WithName> orderMyKids(final Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                                         Map<NID.WithName, Integer> linkCounts,
                                         Set<NID.WithName> targsToGo, final NID.WithName node, Set<FabricLink> allLinks,
                                         Set<NID.WithName> loneNodeIDs) {
    Set<NID.WithName> targs = targsPerSource.get(node);
    if (targs == null) {
      return (new ArrayList<NID.WithName>());
    }
//    TreeMap<Integer, SortedSet<NID.WithName>> kidMap = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
//    Iterator<NID.WithName> tait = targs.iterator();
//    while (tait.hasNext()) {
//      NID.WithName nextTarg = tait.next();
//      Integer count = linkCounts.get(nextTarg);
//      SortedSet<NID.WithName> perCount = kidMap.get(count);
//      if (perCount == null) {
//        perCount = new TreeSet<NID.WithName>();
//        kidMap.put(count, perCount);
//      }
//      perCount.add(nextTarg);
//    }
//
//    ArrayList<NID.WithName> myKidsToProc = new ArrayList<NID.WithName>();
//    Iterator<SortedSet<NID.WithName>> kmit = kidMap.values().iterator();
//    while (kmit.hasNext()) {
//      SortedSet<NID.WithName> perCount = kmit.next();
//      Iterator<NID.WithName> pcit = perCount.iterator();
//      while (pcit.hasNext()) {
//        NID.WithName kid = pcit.next();
//        if (targsToGo.contains(kid)) {
//          myKidsToProc.add(kid);
//        }
//      }
//    }
//    return (myKidsToProc);

//    final NetworkAlignmentLayout.NetAlignNodeGrouper grouper = new NetworkAlignmentLayout.NetAlignNodeGrouper(allLinks, loneNodeIDs);
    
    List<NID.WithName> neighbors = new ArrayList<NID.WithName>(targsPerSource.get(node));
//    Collections.sort(neighbors, new Comparator<NID.WithName>() {
//      @Override
//      public int compare(NID.WithName neigh1, NID.WithName neigh2) {
//        int g1 = grouper.getNodeGroup(neigh1), g2 = grouper.getNodeGroup(neigh2);
//        if (g1 != g2) {
//          return g1 - g2;
//        } else {
//          return targsPerSource.get(neigh2).size() - targsPerSource.get(neigh1).size();
//        }
//      }
//    });
  
    Collections.sort(neighbors, new Comparator<NID.WithName>() {
      @Override
      public int compare(NID.WithName neigh1, NID.WithName neigh2) {
        return targsPerSource.get(neigh2).size() - targsPerSource.get(neigh1).size();
      }
    });
    
  
    return neighbors;
  }
  
  
  private NetAlignNodeGrouper grouper;
  
  /***************************************************************************
   **
   ** LG = LINK GROUP
   **
   ** FIRST LG  = PURPLE EDGES           // COVERERED EDGE
   ** SECOND LG = BLUE EDGES             // GRAPH1
   ** THIRD LG  = RED EDGES              // INDUCED_GRAPH2
   ** FOURTH LG = ORANGE EDGES           // HALF_UNALIGNED_GRAPH2    (TECHNICALLY RED EDGES)
   ** FIFTH LG  = YELLOW EDGES           // FULL_UNALIGNED_GRAPH2    (TECHNICALLY RED EDGES)
   **
   ** PURPLE NODE =  ALIGNED NODE
   ** RED NODE    =  UNALINGED NODE
   */
  
  private final int NUMBER_NODE_GROUPS = 19;
  
  private static final int
          
          PURPLE_SINGLETON = 0,
          PURPLE_WITH_ONLY_PURPLE = 1,            // PURPLE NODES IN LINK GROUP 1, 2, 3
          PURPLE_WITH_PURPLE_BLUE = 2,
          PURPLE_WITH_PURPLE_BLUE_RED = 3,
          PURPLE_WITH_PURPLE_RED = 4,
          PURPLE_WITH_ONLY_RED = 5,
          PURPLE_WITH_BLUE_RED = 6,
          PURPLE_WITH_ONLY_BLUE = 7,
  
          PURPLE_WITH_BLUE_ORANGE = 8,            // PURPLE NODES IN LINK GROUP 4
          PURPLE_WITH_ONLY_ORANGE = 9,
          PURPLE_WITH_PURPLE_ORANGE = 10,
          PURPLE_WITH_PURPLE_BLUE_ORANGE = 11,
          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 12,
          PURPLE_WITH_PURPLE_RED_ORANGE = 13,
          PURPLE_WITH_RED_ORANGE = 14,
          PURPLE_WITH_BLUE_RED_ORANGE = 15,
  
          RED_WITH_ORANGE = 16,                   // RED NODES IN LINK GROUP 4
          RED_WITH_ORANGE_YELLOW = 17,
          RED_WITH_ONLY_YELLOW = 18,              // RED NODES IN LINK GROUP 5
          RED_SINGLETON = 19;
  
  
  private static class NetAlignNodeGrouper {
    
    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
    
    public NetAlignNodeGrouper(BioFabricNetwork.NetworkAlignmentBuildData nabd) {
      this(nabd.allLinks, nabd.loneNodeIDs);
    }
    
    public NetAlignNodeGrouper(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs) {
      
      nodeToLinks_ = new HashMap<NID.WithName, Set<FabricLink>>();
      nodeToNeighbors_ = new HashMap<NID.WithName, Set<NID.WithName>>();
      
      for (FabricLink link : allLinks) {
        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
        
        if (nodeToLinks_.get(src) == null) {
          nodeToLinks_.put(src, new HashSet<FabricLink>());
        }
        if (nodeToLinks_.get(trg) == null) {
          nodeToLinks_.put(trg, new HashSet<FabricLink>());
        }
        if (nodeToNeighbors_.get(src) == null) {
          nodeToNeighbors_.put(src, new HashSet<NID.WithName>());
        }
        if (nodeToNeighbors_.get(trg) == null) {
          nodeToNeighbors_.put(trg, new HashSet<NID.WithName>());
        }
        
        nodeToLinks_.get(src).add(link);
        nodeToLinks_.get(trg).add(link);
        nodeToNeighbors_.get(src).add(trg);
        nodeToNeighbors_.get(trg).add(src);
      }
      
      for (NID.WithName node : loneNodeIDs) {
        nodeToLinks_.put(node, new HashSet<FabricLink>());
        nodeToNeighbors_.put(node, new HashSet<NID.WithName>());
      }
      return;
    }
    
    int getNodeGroup(NID.WithName node) {
      
      if (PURPLE_SINGLETON(node)) {
        return PURPLE_SINGLETON;
        
      } else if (PURPLE_WITH_ONLY_PURPLE(node)) {
        return PURPLE_WITH_ONLY_PURPLE;
        
      } else if (PURPLE_WITH_PURPLE_BLUE(node)) {
        return PURPLE_WITH_PURPLE_BLUE;
        
      } else if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
        return PURPLE_WITH_PURPLE_BLUE_RED;
        
      } else if (PURPLE_WITH_PURPLE_RED(node)) {
        return PURPLE_WITH_PURPLE_RED;
        
      } else if (PURPLE_WITH_ONLY_RED(node)) {
        return PURPLE_WITH_ONLY_RED;
        
      } else if (PURPLE_WITH_BLUE_RED(node)) {
        return PURPLE_WITH_BLUE_RED;
        
      } else if (PURPLE_WITH_ONLY_BLUE(node)) {
        return PURPLE_WITH_ONLY_BLUE;
        
      } else if (PURPLE_WITH_BLUE_ORANGE(node)) {
        return PURPLE_WITH_BLUE_ORANGE;
        
      } else if (PURPLE_WITH_ONLY_ORANGE(node)) {
        return PURPLE_WITH_ONLY_ORANGE;
        
      } else if (PURPLE_WITH_PURPLE_ORANGE(node)) {
        return PURPLE_WITH_PURPLE_ORANGE;
        
      } else if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
        
      } else if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
        
      } else if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
        return PURPLE_WITH_PURPLE_RED_ORANGE;
        
      } else if (PURPLE_WITH_RED_ORANGE(node)) {
        return PURPLE_WITH_RED_ORANGE;
        
      } else if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
        return PURPLE_WITH_BLUE_RED_ORANGE;
        
      } else if (RED_WITH_ORANGE(node)) {
        return RED_WITH_ORANGE;
        
      } else if (RED_WITH_ORANGE_YELLOW(node)) {
        return RED_WITH_ORANGE_YELLOW;
        
      } else if (RED_WITH_ONLY_YELLOW(node)) {
        return RED_WITH_ONLY_YELLOW;
        
      } else if (RED_SINGLETON(node)) {
        return RED_SINGLETON;
        
      } else {
        throw new IllegalArgumentException("Node group not found");
      }
    }
    
    void sortByDegree(List<NID.WithName> group) {
      
      Collections.sort(group, new Comparator<NID.WithName>() {
        public int compare(NID.WithName node1, NID.WithName node2) {
//          return nodeToNeighbors_.get(o1).size() - nodeToNeighbors_.get(o2).size();
          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
//          if (diffSize != 0) {
//            return diffSize;
//          } else {
//            return node1.getName().compareTo(node2.getName());
//          }
//
          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
//          return nodeToNeighbors_.get(o2).size() - nodeToNeighbors_.get(o1).size();
        }
      });
      return;
    }
    
    // LINK GROUPS 1, 2, 3
    boolean PURPLE_WITH_ONLY_PURPLE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_ONLY_BLUE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {GRAPH1};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    boolean PURPLE_WITH_ONLY_RED(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {INDUCED_GRAPH2};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_BLUE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, GRAPH1};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_RED(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    boolean PURPLE_WITH_BLUE_RED(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {GRAPH1, INDUCED_GRAPH2};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_BLUE_RED(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2};
      
      
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    // LINK GROUP 4
    boolean PURPLE_WITH_ONLY_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      String[] rels = {HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      if (isOK) {
        System.out.println("purple w/ only orange");
      }
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_BLUE_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {GRAPH1, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    boolean PURPLE_WITH_RED_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_BLUE_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, GRAPH1, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_WITH_PURPLE_RED_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    boolean PURPLE_WITH_BLUE_RED_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
      
    }
    
    boolean PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(NID.WithName node) {
      if (! isPurple(node)) {
        return false;
      }
      
      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean PURPLE_SINGLETON(NID.WithName node) {
      
      if (! isPurple(node)) {
        return false;
      }
      return nodeToLinks_.get(node).isEmpty() && nodeToNeighbors_.get(node).isEmpty();
    }
    
    boolean RED_WITH_ORANGE(NID.WithName node) {
      if (! isRed(node)) {
        return false;
      }
      
      String[] rels = {HALF_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    // LINK GROUP 5
    boolean RED_WITH_ONLY_YELLOW(NID.WithName node) {
      if (! isRed(node)) {
        return false;
      }
      
      String[] rels = {FULL_UNALIGNED_GRAPH2};
      
      
      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean RED_WITH_ORANGE_YELLOW(NID.WithName node) {
      if (! isRed(node)) {
        return false;
      }
      
      String[] rels = {HALF_UNALIGNED_GRAPH2, FULL_UNALIGNED_GRAPH2};
      
      
      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
      
      return isOK;
    }
    
    boolean RED_SINGLETON(NID.WithName node) {
      
      if (! isRed(node)) {
        return false;
      }
      return nodeToLinks_.get(node).isEmpty() && nodeToNeighbors_.get(node).isEmpty();
    }
    
    
    // Helper functions
    boolean hasOnlyRedNeighbors(NID.WithName node) {
      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
        if (! isRed(neigh)) {
          return false;
        }
      }
      return true;
    }
    
    boolean hasOnlyPurpleNeighbors(NID.WithName node) {
      
      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
        if (! isPurple(neigh)) {
          return false;
        }
      }
      return true;
    }
    
    boolean hasOnlyEdgesOfRels(NID.WithName node, String[] relsAllowed) {
      
      boolean[] visitedEachRel = new boolean[relsAllowed.length];
      
      for (FabricLink link : nodeToLinks_.get(node)) {
        
        boolean linkAllowed = false;
        
        for (int i = 0; i < relsAllowed.length; i++) {
          if (link.getRelation().equals(relsAllowed[i])) {
            linkAllowed = true;
            visitedEachRel[i] = true;
          }
        }
        
        if (! linkAllowed) {
          return false;
        }
      }
      
      for (boolean visit : visitedEachRel) {
        if (! visit) {
          return false;
        }
      }
      
      return true;
    }
    
    boolean isPurple(NID.WithName node) {
      return node.getName().contains("-"); // THERE IS DEFINETLY A BETTER WAY TO DO THIS
    }
    
    boolean isRed(NID.WithName node) {
      return ! isPurple(node);
    }
    
    @SuppressWarnings("unused")
    void checkOnlyOneGroup(NID.WithName node) {
      
      if (true) {  // only for debuggin purposes - will remove later
        throw new IllegalStateException();
      }
      
      // the numbers for errors are WRONG
      List<Integer> err = new ArrayList<Integer>();
      
      // LINK GROUP 1, 2 , 3
      if (PURPLE_WITH_ONLY_PURPLE(node)) {
        err.add(1);
        
      }
      
      if (PURPLE_WITH_ONLY_BLUE(node)) {
        err.add(2);
        
        
      }
      if (PURPLE_WITH_ONLY_RED(node)) {
        err.add(3);
        
        
      }
      if (PURPLE_WITH_PURPLE_BLUE(node)) {
        err.add(4);
        
      }
      if (PURPLE_WITH_PURPLE_RED(node)) {
        err.add(5);
        
        
      }
      if (PURPLE_WITH_BLUE_RED(node)) {
        err.add(6);
        
        
      }
      if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
        err.add(7);
      }
      // LINK GROUP 4
      if (PURPLE_WITH_ONLY_ORANGE(node)) {
        err.add(8);
        
      }
      if (PURPLE_WITH_PURPLE_ORANGE(node)) {
        err.add(9);
        
      }
      if (PURPLE_WITH_BLUE_ORANGE(node)) {
        err.add(10);
        
      }
      if (PURPLE_WITH_RED_ORANGE(node)) {
        err.add(11);
        
      }
      if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
        err.add(12);
        
        
      }
      if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
        err.add(13);
        
      }
      if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
        err.add(14);
        
      }
      if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
        err.add(15);
        
      }
      if (RED_WITH_ORANGE(node)) {
        err.add(16);
        
      }
      // LINK GROUP 5
      
      if (RED_WITH_ONLY_YELLOW(node)) {
        err.add(17);
        
      }
      if (RED_WITH_ORANGE_YELLOW(node)) {
        err.add(18);
        
        
      }
      if (PURPLE_SINGLETON(node)) {
        err.add(19);
        
      }
      if (RED_SINGLETON(node)) {
        err.add(20);
      }
      
      if (err.size() != 1) {
        System.out.println(node + "\nerror" + err + "\n\n\n\n\n\n");
      }
    }
    
  }
  
}

//public class NetworkAlignmentLayout extends NodeLayout {
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PRIVATE INSTANCE MEMBERS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PUBLIC CONSTRUCTORS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  /***************************************************************************
//   **
//   ** Constructor
//   */
//
//  public NetworkAlignmentLayout() {
//  }
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PUBLIC METHODS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  /***************************************************************************
//   **
//   ** Relayout the network!
//   */
//
//  BioFabricNetwork.NetworkAlignmentBuildData nabd;
//
//  public List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, Params params, BTProgressMonitor monitor)
//          throws AsynchExitRequestException {
//
//    nabd = (BioFabricNetwork.NetworkAlignmentBuildData) rbd;
//
////    List<NID.WithName> targetIDs = nodeGroupByClass(nabd, monitor);
////    List<NID.WithName> targetIDs = nodeGroupBFS(nabd, monitor);
//    List<NID.WithName> targetIDs = defaultNodeOrder(nabd.allLinks, nabd.loneNodeIDs, null, monitor);
//
//    installNodeOrder(targetIDs, nabd, monitor);
//    return (new ArrayList<NID.WithName>(targetIDs));
//  }
//
//  /***************************************************************************
//   **
//   **
//   */
//
//  private List<NID.WithName> nodeGroupBFS(BioFabricNetwork.NetworkAlignmentBuildData nabd, BTProgressMonitor monitor)
//          throws AsynchExitRequestException {
//
//    return new nodeGroupBFSLayout(nabd).process();
//
//  }
//
//
//  private class nodeGroupBFSLayout {
//
//    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
//    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
//    NetAlignNodeGrouper grouper;
//    BioFabricNetwork.NetworkAlignmentBuildData nabd;
//
//    nodeGroupBFSLayout(BioFabricNetwork.NetworkAlignmentBuildData nabd) {
//      nodeToLinks_ = new HashMap<NID.WithName, Set<FabricLink>>();
//      nodeToNeighbors_ = new HashMap<NID.WithName, Set<NID.WithName>>();
//
//      grouper = new NetAlignNodeGrouper(nabd);
//      this.nabd = nabd;
//
//      for (FabricLink link : nabd.allLinks) {
//        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
//
//        if (nodeToLinks_.get(src) == null) {
//          nodeToLinks_.put(src, new HashSet<FabricLink>());
//        }
//        if (nodeToLinks_.get(trg) == null) {
//          nodeToLinks_.put(trg, new HashSet<FabricLink>());
//        }
//        if (nodeToNeighbors_.get(src) == null) {
//          nodeToNeighbors_.put(src, new HashSet<NID.WithName>());
//        }
//        if (nodeToNeighbors_.get(trg) == null) {
//          nodeToNeighbors_.put(trg, new HashSet<NID.WithName>());
//        }
//
//        nodeToLinks_.get(src).add(link);
//        nodeToLinks_.get(trg).add(link);
//        nodeToNeighbors_.get(src).add(trg);
//        nodeToNeighbors_.get(trg).add(src);
//      }
//
//      for (NID.WithName node : nabd.loneNodeIDs) {
//        nodeToLinks_.put(node, new HashSet<FabricLink>());
//        nodeToNeighbors_.put(node, new HashSet<NID.WithName>());
//      }
//      return;
//    }
//
//    List<NID.WithName> process() throws AsynchExitRequestException {
//
//      Map<NID.WithName, Boolean> visited = new HashMap<NID.WithName, Boolean>();
//      List<NID.WithName> nodes = new ArrayList<NID.WithName>(BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, null));
//      Collections.sort(nodes, new Comparator<NID.WithName>() {
//        @Override
//        public int compare(NID.WithName o1, NID.WithName o2) {
//          return nodeToNeighbors_.get(o2).size() - nodeToNeighbors_.get(o1).size();
//        }
//      });
//
//      List<NID.WithName> targetIDs = new ArrayList<NID.WithName>();
//
//      for (int i = 0; i < nodes.size(); i++) {
//
//        if (visited.get(nodes.get(i)) == null) {
//          continue;
//        }
//
//        Queue<NID.WithName> queue = new LinkedList<NID.WithName>();
//        queue.add(nodes.remove(0));
//
//        while (! queue.isEmpty()) {
//          NID.WithName node = queue.poll();
//
//          if (visited.get(node)) {
//            continue;
//          }
//          visited.put(node, true);
//
//          List<NID.WithName> neighbors = new ArrayList<NID.WithName>(nodeToNeighbors_.get(node));
//          Collections.sort(neighbors, new Comparator<NID.WithName>() {
//            @Override
//            public int compare(NID.WithName neigh1, NID.WithName neigh2) {
//              int neigh1Group = grouper.getNodeGroup(neigh1), neigh2Group = grouper.getNodeGroup(neigh2);
//
//              if (neigh1Group != neigh2Group) {
//                return neigh1Group - neigh2Group; // increasing group assignment
//              } else {
//                return nodeToNeighbors_.get(neigh1Group).size() - nodeToNeighbors_.get(neigh2Group).size();
//              }
//            }
//          });
//
//          targetIDs.add(node);
//          for (NID.WithName neighs : neighbors) {
//            targetIDs.add(neighs);
//          }
//
//        }
//      }
//
//
//      return targetIDs;
//    }
//
//  }
//
//  /***************************************************************************
//   **
//   **
//   */
//
//  private List<NID.WithName> nodeGroupByClass(BioFabricNetwork.NetworkAlignmentBuildData nabd, BTProgressMonitor monitor)
//          throws AsynchExitRequestException {
//
//    NetAlignNodeGrouper grouper = new NetAlignNodeGrouper(nabd);
//
//    ArrayList<NID.WithName>[] classToGroup; // list should be set, but error checking for now. . .
//
//    classToGroup = new ArrayList[NUMBER_NODE_GROUPS + 1];
//    for (int i = 0; i < classToGroup.length; i++) {
//      classToGroup[i] = new ArrayList<NID.WithName>();
//    }
//
//    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
//
//    for (NID.WithName node : allNodes) {
//
//      int nodeClass = grouper.getNodeGroup(node);
//
//      classToGroup[nodeClass].add(node);
//    }
//
//    List<NID.WithName> targetIDs = new ArrayList<NID.WithName>();
//
//    for (int i = 1; i < classToGroup.length; i++) {
//
//      if (classToGroup[i].size() == 0) {
//        System.out.println("Empty Class:" + i + '\n');
//      }
//
//      Set<NID.WithName> setForm = new TreeSet<NID.WithName>(classToGroup[i]);
//      if (setForm.size() != classToGroup[i].size()) {
//        throw new IllegalStateException("different sizes classes");
//      }
//
//      grouper.sortByDegree(classToGroup[i]);
//
//      for (NID.WithName node : classToGroup[i]) {
//        if (targetIDs.contains(node)) {
//          throw new IllegalStateException("seeing contains");
//        }
//        targetIDs.add(node);
//      }
//    }
//
//    return targetIDs;
//  }
//
//
//  /***************************************************************************
//   **
//   ** Calculate default node order. Used by several other layout classes
//   */
//
//  public List<NID.WithName> defaultNodeOrder(Set<FabricLink> allLinks,
//                                             Set<NID.WithName> loneNodes,
//                                             List<NID.WithName> startNodes,
//                                             BTProgressMonitor monitor) throws AsynchExitRequestException {
//    //
//    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
//    //
//    //
//    // Build a target list, top to bottom, that adds the node with the most
//    // links first, and adds those link targets ASAP. If caller supplies a start node,
//    // we go there first:
//    //
//
//    HashMap<NID.WithName, Integer> linkCounts = new HashMap<NID.WithName, Integer>();
//    HashMap<NID.WithName, Set<NID.WithName>> targsPerSource = new HashMap<NID.WithName, Set<NID.WithName>>();
//
//    HashSet<NID.WithName> targsToGo = new HashSet<NID.WithName>();
//
//    int numLink = allLinks.size();
//    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
//
//    Iterator<FabricLink> alit = allLinks.iterator();
//    while (alit.hasNext()) {
//      FabricLink nextLink = alit.next();
//      lr.report();
//      NID.WithName sidwn = nextLink.getSrcID();
//      NID.WithName tidwn = nextLink.getTrgID();
//      Set<NID.WithName> targs = targsPerSource.get(sidwn);
//      if (targs == null) {
//        targs = new HashSet<NID.WithName>();
//        targsPerSource.put(sidwn, targs);
//      }
//      targs.add(tidwn);
//      targs = targsPerSource.get(tidwn);
//      if (targs == null) {
//        targs = new HashSet<NID.WithName>();
//        targsPerSource.put(tidwn, targs);
//      }
//      targs.add(sidwn);
//      targsToGo.add(sidwn);
//      targsToGo.add(tidwn);
//      Integer srcCount = linkCounts.get(sidwn);
//      linkCounts.put(sidwn, (srcCount == null) ? Integer.valueOf(1) : Integer.valueOf(srcCount.intValue() + 1));
//      Integer trgCount = linkCounts.get(tidwn);
//      linkCounts.put(tidwn, (trgCount == null) ? Integer.valueOf(1) : Integer.valueOf(trgCount.intValue() + 1));
//    }
//    lr.finish();
//
//    //
//    // Rank the nodes by link count:
//    //
//
//    lr = new LoopReporter(linkCounts.size(), 20, monitor, 0.25, 0.50, "progress.rankByDegree");
//
//    TreeMap<Integer, SortedSet<NID.WithName>> countRank = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
//    Iterator<NID.WithName> lcit = linkCounts.keySet().iterator();
//    while (lcit.hasNext()) {
//      NID.WithName src = lcit.next();
//      lr.report();
//      Integer count = linkCounts.get(src);
//      SortedSet<NID.WithName> perCount = countRank.get(count);
//      if (perCount == null) {
//        perCount = new TreeSet<NID.WithName>();
//        countRank.put(count, perCount);
//      }
//      perCount.add(src);
//    }
//    lr.finish();
//
//    grouper = new NetworkAlignmentLayout.NetAlignNodeGrouper(allLinks, loneNodes);
//
//    SortedMap<Integer, List<NID.WithName>> classToGroup = new TreeMap<Integer, List<NID.WithName>>();
//
//    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) {
//      classToGroup.put(i, new ArrayList<NID.WithName>());
//    }
//    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
//    for (NID.WithName node : allNodes) {
//
//      int nodeClass = grouper.getNodeGroup(node);
//      classToGroup.get(nodeClass).add(node);
//    }
//
//    //
//    // Handle the specified starting nodes case:
//    //
//
//    SortedMap<Integer, List<NID.WithName>> targetsGroup = new TreeMap<Integer, List<NID.WithName>>();
//    SortedMap<Integer,List<NID.WithName>> queueGroup = new TreeMap<Integer, List<NID.WithName>>();
//
//    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
//      targetsGroup.put(i, new ArrayList<NID.WithName>());
//      queueGroup.put(i, new ArrayList<NID.WithName>());
//    }
//
//    SortedMap<Integer, Set<NID.WithName>> targsLeftToGoGroup = new TreeMap<Integer, Set<NID.WithName>>();
//
//    targsLeftToGoGroup.put(0, null);
//    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) { // skip loners
//      targsLeftToGoGroup.put(i, new TreeSet<NID.WithName>());
//      for (NID.WithName node : classToGroup.get(i)) {
//        targsLeftToGoGroup.get(i).add(node);
//      }
//    }
//
//    startNodes.add(classToGroup.get(1).get(0)); // highest degree {P:P} DON'T FORGET TO CHECK IF IT EXISTS
//
//    if ((startNodes != null) && !startNodes.isEmpty()) {
////      ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//      targsToGo.removeAll(startNodes);
//      targsLeftToGoGroup.get(1).remove(startNodes.get(0));
//
//      targetsGroup.get(1).addAll(startNodes);
//
//
//      queueGroup.get(1).addAll(startNodes);
//      flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup, monitor, 0.50, 0.75,
//              allLinks, loneNodes, 1);
//    }
//
//
//
//
//    //
//    // Get all kids added in.  Now doing this without recursion; seeing blown
//    // stacks for huge networks!
//    //
//
//
//
//
////    while (! targsToGo.isEmpty()) {
//    Iterator<Integer> crit = countRank.keySet().iterator();
//    while (crit.hasNext()) {
//      Integer key = crit.next();
//      SortedSet<NID.WithName> perCount = countRank.get(key);
//      Iterator<NID.WithName> pcit = perCount.iterator();
//      while (pcit.hasNext()) {
//        NID.WithName node = pcit.next();
//        if (targsToGo.contains(node)) {
////            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//
//          targsToGo.remove(node);
//
//          int group = grouper.getNodeGroup(node);
//          targetsGroup.get(group).add(node);
//
//
//          addMyKidsNR(targetsGroup, targsPerSource, linkCounts, targsToGo, node, targsLeftToGoGroup, queueGroup, monitor,
//                  0.75, 1.0, allLinks, loneNodes);
//        }
//      }
//    }
////    }
//
//
//    List<NID.WithName> targets = new ArrayList<NID.WithName>();
////    for (List<NID.WithName> group : targsToGoGroup) {
////      for (NID.WithName node : targets) {
////        targets.add(node);
////      }
////    }
//
//    //
//    //
//    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
//    // we drop it:
//    //
//
//    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
//    // GOES AWAY IF remains 190804 targets 281832
//    System.err.println("remains " + remains.size() + " targets " + targets.size());
//    remains.removeAll(targets);
//    System.err.println("remains now " + remains.size());
//    targets.addAll(new TreeSet<NID.WithName>(remains));
//    return (targets);
//  }
//
//  /***************************************************************************
//   **
//   ** Node ordering
//   */
//
//  private List<NID.WithName> orderMyKids(final Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                                         Map<NID.WithName, Integer> linkCounts,
//                                         Set<NID.WithName> targsToGo, final NID.WithName node, Set<FabricLink> allLinks,
//                                         Set<NID.WithName> loneNodeIDs) {
//    Set<NID.WithName> targs = targsPerSource.get(node);
//    if (targs == null) {
//      return (new ArrayList<NID.WithName>());
//    }
////    TreeMap<Integer, SortedSet<NID.WithName>> kidMap = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
////    Iterator<NID.WithName> tait = targs.iterator();
////    while (tait.hasNext()) {
////      NID.WithName nextTarg = tait.next();
////      Integer count = linkCounts.get(nextTarg);
////      SortedSet<NID.WithName> perCount = kidMap.get(count);
////      if (perCount == null) {
////        perCount = new TreeSet<NID.WithName>();
////        kidMap.put(count, perCount);
////      }
////      perCount.add(nextTarg);
////    }
////
////    ArrayList<NID.WithName> myKidsToProc = new ArrayList<NID.WithName>();
////    Iterator<SortedSet<NID.WithName>> kmit = kidMap.values().iterator();
////    while (kmit.hasNext()) {
////      SortedSet<NID.WithName> perCount = kmit.next();
////      Iterator<NID.WithName> pcit = perCount.iterator();
////      while (pcit.hasNext()) {
////        NID.WithName kid = pcit.next();
////        if (targsToGo.contains(kid)) {
////          myKidsToProc.add(kid);
////        }
////      }
////    }
////    return (myKidsToProc);
//
////    final NetworkAlignmentLayout.NetAlignNodeGrouper grouper = new NetworkAlignmentLayout.NetAlignNodeGrouper(allLinks, loneNodeIDs);
//
//    List<NID.WithName> neighbors = new ArrayList<NID.WithName>(targsPerSource.get(node));
//    Collections.sort(neighbors, new Comparator<NID.WithName>() {
//      @Override
//      public int compare(NID.WithName neigh1, NID.WithName neigh2) {
//        int g1 = grouper.getNodeGroup(neigh1), g2 = grouper.getNodeGroup(neigh2);
//        if (g1 != g2) {
//          return g1 - g2;
//        } else {
//          return targsPerSource.get(neigh2).size() - targsPerSource.get(neigh1).size();
//        }
//      }
//    });
//
//    return neighbors;
//  }
//
//  /***************************************************************************
//   **
//   ** Node ordering, non-recursive:
//   */
//
//  private void addMyKidsNR(SortedMap<Integer, List<NID.WithName>> targetsGroup, Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                           Map<NID.WithName, Integer> linkCounts,
//                           Set<NID.WithName> targsToGo, NID.WithName node, SortedMap<Integer, Set<NID.WithName>> targsToGoGroup,
//                           SortedMap<Integer,List<NID.WithName>> queueGroup,
//                           BTProgressMonitor monitor, double startFrac, double endFrac, Set<FabricLink> allLinks,
//                           Set<NID.WithName> loneNodes)
//          throws AsynchExitRequestException {
//
//    int group = grouper.getNodeGroup(node);
//    queueGroup.get(group).add(node);
//    // do loop here
//    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {  // skip loners for now
//
//      while (!targsToGoGroup.get(group).isEmpty()) {
//
//        // add from targsToGoGroup
//
//        flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsToGoGroup, queueGroup, monitor, startFrac, endFrac,
//                allLinks, loneNodes, i);
//      }
//    }
//
////    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, startFrac, endFrac, allLinks,
////            loneNodes);
//    return;
//  }
//
//  /***************************************************************************
//   **
//   ** Node ordering, non-recursive:
//   */
//
//  private void flushQueue(SortedMap<Integer, List<NID.WithName>> targetsGroup,
//                          Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                          Map<NID.WithName, Integer> linkCounts,
//                          Set<NID.WithName> targsToGo, SortedMap<Integer, Set<NID.WithName>> targsLeftToGoGroup,
//                          SortedMap<Integer,List<NID.WithName>> queuesGroup,
//                          BTProgressMonitor monitor, double startFrac, double endFrac, Set<FabricLink> allLinks,
//                          Set<NID.WithName> loneNodes, int currGroup)
//          throws AsynchExitRequestException {
//
////    NetAlignNodeGrouper grouper = new NetAlignNodeGrouper(allLinks, loneNodes);
//
//    LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
//    int lastSize = targsToGo.size();
//    List<NID.WithName> queue = queuesGroup.get(currGroup);
//
//    while (! queue.isEmpty()) {
//      NID.WithName node = queue.remove(0);
//      int ttgSize = targsLeftToGoGroup.get(currGroup).size();
//      lr.report(lastSize - ttgSize);
//      lastSize = ttgSize;
//      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node, allLinks, loneNodes);
//      Iterator<NID.WithName> ktpit = myKids.iterator();
//      while (ktpit.hasNext()) {
//        NID.WithName kid = ktpit.next();
//        if (targsToGo.contains(kid)) {
//          targsToGo.remove(kid);
//          queue.add(kid);
//
//          int group = grouper.getNodeGroup(kid);
//          targetsGroup.get(group).add(kid);
//        }
//      }
//    }
//    lr.finish();
//    return;
//  }
//
//
//  private NetAlignNodeGrouper grouper;
//
//  /***************************************************************************
//   **
//   ** LG = LINK GROUP
//   **
//   ** FIRST LG  = PURPLE EDGES           // COVERERED EDGE
//   ** SECOND LG = BLUE EDGES             // GRAPH1
//   ** THIRD LG  = RED EDGES              // INDUCED_GRAPH2
//   ** FOURTH LG = ORANGE EDGES           // HALF_UNALIGNED_GRAPH2    (TECHNICALLY RED EDGES)
//   ** FIFTH LG  = YELLOW EDGES           // FULL_UNALIGNED_GRAPH2    (TECHNICALLY RED EDGES)
//   **
//   ** PURPLE NODE =  ALIGNED NODE
//   ** RED NODE    =  UNALINGED NODE
//   */
//
//  private final int NUMBER_NODE_GROUPS = 19;
//
//  private static final int
//
//          PURPLE_SINGLETON = 0,
//          PURPLE_WITH_ONLY_PURPLE = 1,            // PURPLE NODES IN LINK GROUP 1, 2, 3
//          PURPLE_WITH_PURPLE_BLUE = 2,
//          PURPLE_WITH_PURPLE_BLUE_RED = 3,
//          PURPLE_WITH_PURPLE_RED = 4,
//          PURPLE_WITH_ONLY_RED = 5,
//          PURPLE_WITH_BLUE_RED = 6,
//          PURPLE_WITH_ONLY_BLUE = 7,
//
//  PURPLE_WITH_BLUE_ORANGE = 8,            // PURPLE NODES IN LINK GROUP 4
//          PURPLE_WITH_ONLY_ORANGE = 9,
//          PURPLE_WITH_PURPLE_ORANGE = 10,
//          PURPLE_WITH_PURPLE_BLUE_ORANGE = 11,
//          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 12,
//          PURPLE_WITH_PURPLE_RED_ORANGE = 13,
//          PURPLE_WITH_RED_ORANGE = 14,
//          PURPLE_WITH_BLUE_RED_ORANGE = 15,
//
//  RED_WITH_ORANGE = 16,                   // RED NODES IN LINK GROUP 4
//          RED_WITH_ORANGE_YELLOW = 17,
//          RED_WITH_ONLY_YELLOW = 18,              // RED NODES IN LINK GROUP 5
//          RED_SINGLETON = 19;
//
//
//  private static class NetAlignNodeGrouper {
//
//    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
//    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
//
//    public NetAlignNodeGrouper(BioFabricNetwork.NetworkAlignmentBuildData nabd) {
//      this(nabd.allLinks, nabd.loneNodeIDs);
//    }
//
//    public NetAlignNodeGrouper(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs) {
//
//      nodeToLinks_ = new HashMap<NID.WithName, Set<FabricLink>>();
//      nodeToNeighbors_ = new HashMap<NID.WithName, Set<NID.WithName>>();
//
//      for (FabricLink link : allLinks) {
//        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
//
//        if (nodeToLinks_.get(src) == null) {
//          nodeToLinks_.put(src, new HashSet<FabricLink>());
//        }
//        if (nodeToLinks_.get(trg) == null) {
//          nodeToLinks_.put(trg, new HashSet<FabricLink>());
//        }
//        if (nodeToNeighbors_.get(src) == null) {
//          nodeToNeighbors_.put(src, new HashSet<NID.WithName>());
//        }
//        if (nodeToNeighbors_.get(trg) == null) {
//          nodeToNeighbors_.put(trg, new HashSet<NID.WithName>());
//        }
//
//        nodeToLinks_.get(src).add(link);
//        nodeToLinks_.get(trg).add(link);
//        nodeToNeighbors_.get(src).add(trg);
//        nodeToNeighbors_.get(trg).add(src);
//      }
//
//      for (NID.WithName node : loneNodeIDs) {
//        nodeToLinks_.put(node, new HashSet<FabricLink>());
//        nodeToNeighbors_.put(node, new HashSet<NID.WithName>());
//      }
//      return;
//    }
//
//    int getNodeGroup(NID.WithName node) {
//
//      if (PURPLE_SINGLETON(node)) {
//        return PURPLE_SINGLETON;
//
//      } else if (PURPLE_WITH_ONLY_PURPLE(node)) {
//        return PURPLE_WITH_ONLY_PURPLE;
//
//      } else if (PURPLE_WITH_PURPLE_BLUE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE;
//
//      } else if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED;
//
//      } else if (PURPLE_WITH_PURPLE_RED(node)) {
//        return PURPLE_WITH_PURPLE_RED;
//
//      } else if (PURPLE_WITH_ONLY_RED(node)) {
//        return PURPLE_WITH_ONLY_RED;
//
//      } else if (PURPLE_WITH_BLUE_RED(node)) {
//        return PURPLE_WITH_BLUE_RED;
//
//      } else if (PURPLE_WITH_ONLY_BLUE(node)) {
//        return PURPLE_WITH_ONLY_BLUE;
//
//      } else if (PURPLE_WITH_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_ORANGE;
//
//      } else if (PURPLE_WITH_ONLY_ORANGE(node)) {
//        return PURPLE_WITH_ONLY_ORANGE;
//
//      } else if (PURPLE_WITH_PURPLE_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_ORANGE;
//
//      } else if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
//
//      } else if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
//
//      } else if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_RED_ORANGE;
//
//      } else if (PURPLE_WITH_RED_ORANGE(node)) {
//        return PURPLE_WITH_RED_ORANGE;
//
//      } else if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//      } else if (RED_WITH_ORANGE(node)) {
//        return RED_WITH_ORANGE;
//
//      } else if (RED_WITH_ORANGE_YELLOW(node)) {
//        return RED_WITH_ORANGE_YELLOW;
//
//      } else if (RED_WITH_ONLY_YELLOW(node)) {
//        return RED_WITH_ONLY_YELLOW;
//
//      } else if (RED_SINGLETON(node)) {
//        return RED_SINGLETON;
//
//      } else {
//        throw new IllegalArgumentException("Node group not found");
//      }
//    }
//
//    void sortByDegree(List<NID.WithName> group) {
//
//      Collections.sort(group, new Comparator<NID.WithName>() {
//        public int compare(NID.WithName node1, NID.WithName node2) {
////          return nodeToNeighbors_.get(o1).size() - nodeToNeighbors_.get(o2).size();
//          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
////          if (diffSize != 0) {
////            return diffSize;
////          } else {
////            return node1.getName().compareTo(node2.getName());
////          }
////
//          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
////          return nodeToNeighbors_.get(o2).size() - nodeToNeighbors_.get(o1).size();
//        }
//      });
//      return;
//    }
//
//    // LINK GROUPS 1, 2, 3
//    boolean PURPLE_WITH_ONLY_PURPLE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_ONLY_BLUE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {GRAPH1};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    boolean PURPLE_WITH_ONLY_RED(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {INDUCED_GRAPH2};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_BLUE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, GRAPH1};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_RED(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    boolean PURPLE_WITH_BLUE_RED(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {GRAPH1, INDUCED_GRAPH2};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_BLUE_RED(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2};
//
//
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    // LINK GROUP 4
//    boolean PURPLE_WITH_ONLY_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//      String[] rels = {HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      if (isOK) {
//        System.out.println("purple w/ only orange");
//      }
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_BLUE_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {GRAPH1, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    boolean PURPLE_WITH_RED_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_BLUE_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, GRAPH1, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_WITH_PURPLE_RED_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    boolean PURPLE_WITH_BLUE_RED_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//
//    }
//
//    boolean PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(NID.WithName node) {
//      if (! isPurple(node)) {
//        return false;
//      }
//
//      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean PURPLE_SINGLETON(NID.WithName node) {
//
//      if (! isPurple(node)) {
//        return false;
//      }
//      return nodeToLinks_.get(node).isEmpty() && nodeToNeighbors_.get(node).isEmpty();
//    }
//
//    boolean RED_WITH_ORANGE(NID.WithName node) {
//      if (! isRed(node)) {
//        return false;
//      }
//
//      String[] rels = {HALF_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    // LINK GROUP 5
//    boolean RED_WITH_ONLY_YELLOW(NID.WithName node) {
//      if (! isRed(node)) {
//        return false;
//      }
//
//      String[] rels = {FULL_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean RED_WITH_ORANGE_YELLOW(NID.WithName node) {
//      if (! isRed(node)) {
//        return false;
//      }
//
//      String[] rels = {HALF_UNALIGNED_GRAPH2, FULL_UNALIGNED_GRAPH2};
//
//
//      boolean isOK = true && hasOnlyEdgesOfRels(node, rels);
//
//      return isOK;
//    }
//
//    boolean RED_SINGLETON(NID.WithName node) {
//
//      if (! isRed(node)) {
//        return false;
//      }
//      return nodeToLinks_.get(node).isEmpty() && nodeToNeighbors_.get(node).isEmpty();
//    }
//
//
//    // Helper functions
//    boolean hasOnlyRedNeighbors(NID.WithName node) {
//      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
//        if (! isRed(neigh)) {
//          return false;
//        }
//      }
//      return true;
//    }
//
//    boolean hasOnlyPurpleNeighbors(NID.WithName node) {
//
//      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
//        if (! isPurple(neigh)) {
//          return false;
//        }
//      }
//      return true;
//    }
//
//    boolean hasOnlyEdgesOfRels(NID.WithName node, String[] relsAllowed) {
//
//      boolean[] visited = new boolean[relsAllowed.length];
//
//      for (FabricLink link : nodeToLinks_.get(node)) {
//
//        boolean linkAllowed = false;
//
//        for (int i = 0; i < relsAllowed.length; i++) {
//          if (link.getRelation().equals(relsAllowed[i])) {
//            linkAllowed = true;
//            visited[i] = true;
//          }
//        }
//
//        if (! linkAllowed) {
//          return false;
//        }
//      }
//
//      for (boolean visit : visited) {
//        if (! visit) {
//          return false;
//        }
//      }
//
//      return true;
//    }
//
//    boolean isPurple(NID.WithName node) {
//      return node.getName().contains("-"); // THERE IS DEFINETLY A BETTER WAY TO DO THIS
//    }
//
//    boolean isRed(NID.WithName node) {
//      return ! isPurple(node);
//    }
//
//    @SuppressWarnings("unused")
//    void checkOnlyOneGroup(NID.WithName node) {
//
//      if (true) {  // only for debuggin purposes - will remove later
//        throw new IllegalStateException();
//      }
//
//      // the numbers for errors are WRONG
//      List<Integer> err = new ArrayList<Integer>();
//
//      // LINK GROUP 1, 2 , 3
//      if (PURPLE_WITH_ONLY_PURPLE(node)) {
//        err.add(1);
//
//      }
//
//      if (PURPLE_WITH_ONLY_BLUE(node)) {
//        err.add(2);
//
//
//      }
//      if (PURPLE_WITH_ONLY_RED(node)) {
//        err.add(3);
//
//
//      }
//      if (PURPLE_WITH_PURPLE_BLUE(node)) {
//        err.add(4);
//
//      }
//      if (PURPLE_WITH_PURPLE_RED(node)) {
//        err.add(5);
//
//
//      }
//      if (PURPLE_WITH_BLUE_RED(node)) {
//        err.add(6);
//
//
//      }
//      if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
//        err.add(7);
//      }
//      // LINK GROUP 4
//      if (PURPLE_WITH_ONLY_ORANGE(node)) {
//        err.add(8);
//
//      }
//      if (PURPLE_WITH_PURPLE_ORANGE(node)) {
//        err.add(9);
//
//      }
//      if (PURPLE_WITH_BLUE_ORANGE(node)) {
//        err.add(10);
//
//      }
//      if (PURPLE_WITH_RED_ORANGE(node)) {
//        err.add(11);
//
//      }
//      if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
//        err.add(12);
//
//
//      }
//      if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
//        err.add(13);
//
//      }
//      if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
//        err.add(14);
//
//      }
//      if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
//        err.add(15);
//
//      }
//      if (RED_WITH_ORANGE(node)) {
//        err.add(16);
//
//      }
//      // LINK GROUP 5
//
//      if (RED_WITH_ONLY_YELLOW(node)) {
//        err.add(17);
//
//      }
//      if (RED_WITH_ORANGE_YELLOW(node)) {
//        err.add(18);
//
//
//      }
//      if (PURPLE_SINGLETON(node)) {
//        err.add(19);
//
//      }
//      if (RED_SINGLETON(node)) {
//        err.add(20);
//      }
//
//      if (err.size() != 1) {
//        System.out.println(node + "\nerror" + err + "\n\n\n\n\n\n");
//      }
//    }
//
//  }
//
//}


//while (! targsToGo.isEmpty()) {
//        Iterator<Integer> crit = countRank.keySet().iterator();
//        while (crit.hasNext()) {
//        Integer key = crit.next();
//        SortedSet<NID.WithName> perCount = countRank.get(key);
//        Iterator<NID.WithName> pcit = perCount.iterator();
//        while (pcit.hasNext()) {
//        NID.WithName node = pcit.next();
//        if (targsToGo.contains(node)) {
////            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//
//        targsToGo.remove(node);
//
//        int group = grouper.getNodeGroup(node);
//        targetsGroup.get(group).add(node);
//
//        addMyKidsNR(targetsGroup, targsPerSource, linkCounts, targsToGo, node, targsLeftToGoGroup, queueGroup, monitor, 0.75, 1.0, allLinks,
//        loneNodes);
//        }
//        }
//        }
//        }

//if (PURPLE_WITH_ONLY_PURPLE(node)) {   // LINK GROUP 1, 2, 3
//        return PURPLE_WITH_ONLY_PURPLE;
//
//        } else if (PURPLE_WITH_ONLY_BLUE(node)) {
//        return PURPLE_WITH_ONLY_BLUE;
//
//        } else if (PURPLE_WITH_ONLY_RED(node)) {
//        return PURPLE_WITH_ONLY_RED;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE;
//
//        } else if (PURPLE_WITH_PURPLE_RED(node)) {
//        return PURPLE_WITH_PURPLE_RED;
//
//        } else if (PURPLE_WITH_BLUE_RED(node)) {
//        return PURPLE_WITH_BLUE_RED;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED;
//
//        } else if (PURPLE_WITH_ONLY_ORANGE(node)) { // LINK GROUP 4
//        return PURPLE_WITH_ONLY_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_ORANGE;
//
//        } else if (PURPLE_WITH_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_ORANGE;
//
//        } else if (PURPLE_WITH_RED_ORANGE(node)) {
//        return PURPLE_WITH_RED_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_RED_ORANGE;
//
//        } else if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
//
//        } else if (PURPLE_SINGLETON(node)) {
//        return PURPLE_SINGLETON;
//
//        } else if (RED_WITH_ORANGE(node)) {
//        return RED_WITH_ORANGE;
//
//        } else if (RED_WITH_ONLY_YELLOW(node)) {  // LINK GROUP 5
//        return RED_WITH_ONLY_YELLOW;
//
//        } else if (RED_WITH_ORANGE_YELLOW(node)) {
//        return RED_WITH_ORANGE_YELLOW;
//
//        } else if (RED_SINGLETON(node)) {
//        return RED_SINGLETON;
//
//        } else {
//        throw new IllegalArgumentException();
//        }

//  private static final int
//
//          PURPLE_WITH_ONLY_PURPLE     = 1,             // PURPLE NODES IN LINK GROUP 1 ,2, 3
//          PURPLE_WITH_ONLY_BLUE       = 2,
//          PURPLE_WITH_ONLY_RED        = 3,
//          PURPLE_WITH_PURPLE_BLUE     = 4,
//          PURPLE_WITH_PURPLE_RED      = 5,
//          PURPLE_WITH_BLUE_RED        = 6,
//          PURPLE_WITH_PURPLE_BLUE_RED = 7,
//
//  PURPLE_WITH_ONLY_ORANGE         = 8,              // PURPLE NODES IN LINK GROUP 4
//          PURPLE_WITH_PURPLE_ORANGE       = 9,
//          PURPLE_WITH_BLUE_ORANGE         = 10,
//          PURPLE_WITH_RED_ORANGE          = 11,
//          PURPLE_WITH_PURPLE_BLUE_ORANGE  = 12,
//          PURPLE_WITH_PURPLE_RED_ORANGE   = 13,
//          PURPLE_WITH_BLUE_RED_ORANGE     = 14,
//          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 15,
//          PURPLE_SINGLETON                = 16,
//
//  RED_WITH_ORANGE         = 17,                    // RED NODES IN LINK GROUP 4
//          RED_WITH_ONLY_YELLOW    = 18,                    // RED NODES IN LINK GROUP 5
//          RED_WITH_ORANGE_YELLOW  = 19,
//          RED_SINGLETON           = 20;


/*
// LINK GROUP 1, 2 , 3
      if (PURPLE_WITH_ONLY_PURPLE(node)) {
              err.add(1);
          
              }
        
              if (PURPLE_WITH_ONLY_BLUE(node)){
              err.add(2);
          
          
              } if (PURPLE_WITH_ONLY_RED(node)) {
              err.add(3);
          
          
              }  if (PURPLE_WITH_PURPLE_BLUE(node)) {
              err.add(4);
          
              }  if (PURPLE_WITH_PURPLE_RED(node)) {
              err.add(5);
          
          
              }  if (PURPLE_WITH_BLUE_RED(node)) {
              err.add(6);
          
          
              }  if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
              err.add(7);
        
        
              // LINK GROUP 4
              }  if (PURPLE_WITH_ONLY_ORANGE(node)) {
              err.add(8);
          
          
              } if (PURPLE_WITH_PURPLE_ORANGE(node)) {
              err.add(9);
          
              }  if (PURPLE_WITH_BLUE_ORANGE(node)) {
              err.add(10);
          
          
              }  if (PURPLE_WITH_RED_ORANGE(node)) {
              err.add(11);
          
          
              }  if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
              err.add(12);
          
          
              }  if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
              err.add(13);
          
          
              }  if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
              err.add(14);
          
          
              }  if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
              err.add(15);
          
          
              }  if (RED_WITH_ORANGE(node)) {
              err.add(16);
        
        
              // LINK GROUP 5
              }  if (RED_WITH_ONLY_YELLOW(node)) {
              err.add(17);
          
          
              }  if (RED_WITH_ORANGE_YELLOW(node)) {
              err.add(18);
          
          
              }  if (PURPLE_SINGLETON(node)) {
              err.add(19);
          
          
              }  if (RED_SINGLETON(node)) {
              err.add(20);


//      } else {
//        System.out.println(node.getName());
//        throw new IllegalArgumentException();
              }
              */

//if (PURPLE_WITH_ONLY_PURPLE(node)) {
//        return PURPLE_WITH_ONLY_PURPLE;
//
//        } else if (PURPLE_WITH_ONLY_BLUE(node)){
//        return PURPLE_WITH_ONLY_BLUE;
//
//        } else if (PURPLE_WITH_ONLY_RED(node)) {
//        return PURPLE_WITH_ONLY_RED;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE;
//
//        } else if (PURPLE_WITH_PURPLE_RED(node)) {
//        return PURPLE_WITH_PURPLE_RED;
//
//        } else if (PURPLE_WITH_BLUE_RED(node)) {
//        return PURPLE_WITH_BLUE_RED;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_RED(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED;
//
//        // LINK GROUP 4
//        } else if (PURPLE_WITH_ONLY_ORANGE(node)) {
//        return PURPLE_WITH_ONLY_ORANGE;
//
//        } else if (PURPLE_WITH_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_ORANGE;
//
//        } else if (PURPLE_WITH_RED_ORANGE(node)) {
//        return PURPLE_WITH_RED_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_RED_ORANGE;
//
//        } else if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//        } else if (PURPLE_WITH_PURPLE_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
//
//        } else if (PURPLE_WITH_BLUE_RED_ORANGE(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//        } else if (RED_WITH_ORANGE(node)) {
//        return RED_WITH_ORANGE;
//
//        // LINK GROUP 5
//        } else if (RED_WITH_ONLY_YELLOW(node)) {
//        return RED_WITH_ONLY_YELLOW;
//
//        } else if (RED_WITH_ORANGE_YELLOW(node)) {
//        return RED_WITH_ORANGE_YELLOW;
//
//        } else if (PURPLE_SINGLETON(node)) {
//        return PURPLE_SINGLETON;
//
//        } else if (RED_SINGLETON(node)) {
//        return RED_SINGLETON;
//
//        } else {
//        System.out.println(node.getName());
//        throw new IllegalArgumentException();
//        }

//  /***************************************************************************
//   **
//   ** Install node orders
//   */
//
//  public void installNodeOrder(List<NID.WithName> targetIDs, BioFabricNetwork.RelayoutBuildData rbd,
//                               BTProgressMonitor monitor) throws AsynchExitRequestException {
//    int currRow = 0;
//    LoopReporter lr = new LoopReporter(targetIDs.size(), 20, monitor, 0.0, 1.0, "progress.installOrdering");
//
//    HashMap<NID.WithName, Integer> nodeOrder = new HashMap<NID.WithName, Integer>();
//    Iterator<NID.WithName> trit = targetIDs.iterator();
//    while (trit.hasNext()) {
//      NID.WithName target = trit.next();
//      lr.report();
//      Integer rowTag = Integer.valueOf(currRow++);
//      nodeOrder.put(target, rowTag);
//    }
//    rbd.setNodeOrder(nodeOrder);
//    lr.finish();
//    return;
//  }

//  /***************************************************************************
//   **
//   ** Calculate default node order
//   */
//
//  public List<NID.WithName> defaultNodeOrder(Set<FabricLink> allLinks,
//                                             Set<NID.WithName> loneNodes,
//                                             List<NID.WithName> startNodes,
//                                             BTProgressMonitor monitor) throws AsynchExitRequestException {
//    //
//    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
//    //
//    //
//    // Build a target list, top to bottom, that adds the node with the most
//    // links first, and adds those link targets ASAP. If caller supplies a start node,
//    // we go there first:
//    //
//
//    HashMap<NID.WithName, Integer> linkCounts = new HashMap<NID.WithName, Integer>();
//    HashMap<NID.WithName, Set<NID.WithName>> targsPerSource = new HashMap<NID.WithName, Set<NID.WithName>>();
//    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
//
//    HashSet<NID.WithName> targsToGo = new HashSet<NID.WithName>();
//
//    int numLink = allLinks.size();
//    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
//
//    Iterator<FabricLink> alit = allLinks.iterator();
//    while (alit.hasNext()) {
//      FabricLink nextLink = alit.next();
//      lr.report();
//      NID.WithName sidwn = nextLink.getSrcID();
//      NID.WithName tidwn = nextLink.getTrgID();
//      Set<NID.WithName> targs = targsPerSource.get(sidwn);
//      if (targs == null) {
//        targs = new HashSet<NID.WithName>();
//        targsPerSource.put(sidwn, targs);
//      }
//      targs.add(tidwn);
//      targs = targsPerSource.get(tidwn);
//      if (targs == null) {
//        targs = new HashSet<NID.WithName>();
//        targsPerSource.put(tidwn, targs);
//      }
//      targs.add(sidwn);
//      targsToGo.add(sidwn);
//      targsToGo.add(tidwn);
//      Integer srcCount = linkCounts.get(sidwn);
//      linkCounts.put(sidwn, (srcCount == null) ? Integer.valueOf(1) : Integer.valueOf(srcCount.intValue() + 1));
//      Integer trgCount = linkCounts.get(tidwn);
//      linkCounts.put(tidwn, (trgCount == null) ? Integer.valueOf(1) : Integer.valueOf(trgCount.intValue() + 1));
//    }
//    lr.finish();
//
//    //
//    // Rank the nodes by link count:
//    //
//
//    lr = new LoopReporter(linkCounts.size(), 20, monitor, 0.25, 0.50, "progress.rankByDegree");
//
//    TreeMap<Integer, SortedSet<NID.WithName>> countRank = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
//    Iterator<NID.WithName> lcit = linkCounts.keySet().iterator();
//    while (lcit.hasNext()) {
//      NID.WithName src = lcit.next();
//      lr.report();
//      Integer count = linkCounts.get(src);
//      SortedSet<NID.WithName> perCount = countRank.get(count);
//      if (perCount == null) {
//        perCount = new TreeSet<NID.WithName>();
//        countRank.put(count, perCount);
//      }
//      perCount.add(src);
//    }
//    lr.finish();
//
//    //
//    // Handle the specified starting nodes case:
//    //
//
//    if ((startNodes != null) && !startNodes.isEmpty()) {
//      ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//      targsToGo.removeAll(startNodes);
//      targets.addAll(startNodes);
//      queue.addAll(startNodes);
//      flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, 0.50, 0.75);
//    }
//
//    //
//    // Get all kids added in.  Now doing this without recursion; seeing blown
//    // stacks for huge networks!
//    //
//
//    while (!targsToGo.isEmpty()) {
//      Iterator<Integer> crit = countRank.keySet().iterator();
//      while (crit.hasNext()) {
//        Integer key = crit.next();
//        SortedSet<NID.WithName> perCount = countRank.get(key);
//        Iterator<NID.WithName> pcit = perCount.iterator();
//        while (pcit.hasNext()) {
//          NID.WithName node = pcit.next();
//          if (targsToGo.contains(node)) {
//            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
//            targsToGo.remove(node);
//            targets.add(node);
//            addMyKidsNR(targets, targsPerSource, linkCounts, targsToGo, node, queue, monitor, 0.75, 1.0);
//          }
//        }
//      }
//    }
//
//    //
//    //
//    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
//    // we drop it:
//    //
//
//    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
//    // GOES AWAY IF remains 190804 targets 281832
//    System.err.println("remains " + remains.size() + " targets " + targets.size());
//    remains.removeAll(targets);
//    System.err.println("remains now " + remains.size());
//    targets.addAll(new TreeSet<NID.WithName>(remains));
//    return (targets);
//  }

//  /***************************************************************************
//   **
//   ** Node ordering
//   */
//
//  private List<NID.WithName> orderMyKids(Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                                         Map<NID.WithName, Integer> linkCounts,
//                                         Set<NID.WithName> targsToGo, NID.WithName node) {
//    Set<NID.WithName> targs = targsPerSource.get(node);
//    if (targs == null) {
//      return (new ArrayList<NID.WithName>());
//    }
//    TreeMap<Integer, SortedSet<NID.WithName>> kidMap = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
//    Iterator<NID.WithName> tait = targs.iterator();
//    while (tait.hasNext()) {
//      NID.WithName nextTarg = tait.next();
//      Integer count = linkCounts.get(nextTarg);
//      SortedSet<NID.WithName> perCount = kidMap.get(count);
//      if (perCount == null) {
//        perCount = new TreeSet<NID.WithName>();
//        kidMap.put(count, perCount);
//      }
//      perCount.add(nextTarg);
//    }
//
//    ArrayList<NID.WithName> myKidsToProc = new ArrayList<NID.WithName>();
//    Iterator<SortedSet<NID.WithName>> kmit = kidMap.values().iterator();
//    while (kmit.hasNext()) {
//      SortedSet<NID.WithName> perCount = kmit.next();
//      Iterator<NID.WithName> pcit = perCount.iterator();
//      while (pcit.hasNext()) {
//        NID.WithName kid = pcit.next();
//        if (targsToGo.contains(kid)) {
//          myKidsToProc.add(kid);
//        }
//      }
//    }
//    return (myKidsToProc);
//  }

//  /***************************************************************************
//   **
//   ** Node ordering, non-recursive:
//   */
//
//  private void addMyKidsNR(List<NID.WithName> targets, Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                           Map<NID.WithName, Integer> linkCounts,
//                           Set<NID.WithName> targsToGo, NID.WithName node, List<NID.WithName> queue,
//                           BTProgressMonitor monitor, double startFrac, double endFrac)
//          throws AsynchExitRequestException {
//    queue.add(node);
//    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, monitor, startFrac, endFrac);
//    return;
//  }

//  /***************************************************************************
//   **
//   ** Node ordering, non-recursive:
//   */
//
//  private void flushQueue(List<NID.WithName> targets,
//                          Map<NID.WithName, Set<NID.WithName>> targsPerSource,
//                          Map<NID.WithName, Integer> linkCounts,
//                          Set<NID.WithName> targsToGo, List<NID.WithName> queue,
//                          BTProgressMonitor monitor, double startFrac, double endFrac)
//          throws AsynchExitRequestException {
//
//    LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
//    int lastSize = targsToGo.size();
//    while (!queue.isEmpty()) {
//      NID.WithName node = queue.remove(0);
//      int ttgSize = targsToGo.size();
//      lr.report(lastSize - ttgSize);
//      lastSize = ttgSize;
//      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
//      Iterator<NID.WithName> ktpit = myKids.iterator();
//      while (ktpit.hasNext()) {
//        NID.WithName kid = ktpit.next();
//        if (targsToGo.contains(kid)) {
//          targsToGo.remove(kid);
//          targets.add(kid);
//          queue.add(kid);
//        }
//      }
//    }
//    lr.finish();
//    return;
//  }


//      List<NID.WithName> groupOrder = defaultNodeOrder(setForm, new HashSet<NID.WithName>(), null, null);

//      Set<FabricLink> groupLinks = new HashSet<FabricLink>();
//      for (NID.WithName node : setForm) {
//
//        groupLinks.addAll(nodeToLinks.get(node));
//      }
//
//      List<NID.WithName> groupOrder;
//      if (i != PURPLE_SINGLETON && i != RED_SINGLETON) {
//        groupOrder = defaultNodeOrder(groupLinks, new HashSet<NID.WithName>(),null,null);
//      } else {
//        groupOrder = defaultNodeOrder(new HashSet<FabricLink>(), setForm, null,null);
//      }
//
//      if (groupOrder.size() != setForm.size()) {
//        throw new IllegalStateException("different sizes group-set");
//      }
//
//      for (NID.WithName node : groupOrder) {
//        if (targetIDs.contains(node)) {
//          throw new IllegalStateException("seeing contains");
//        }
//        targetIDs.add(node);
//      }


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
//    return (new ArrayList<NID.WithName>(testTargetIDs));r
