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
import org.systemsbiology.biofabric.util.UiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    
    BioFabricNetwork.NetworkAlignmentBuildData nabd = (BioFabricNetwork.NetworkAlignmentBuildData) rbd;
    
    UiUtil.fixMePrintout("Clique Misalignment needs Default layout not BFSNodeGroup");
//    List<NID.WithName> targetIDs = BFSNodeGroupByClass(nabd, monitor);
    List<NID.WithName> targetIDs = (new DefaultLayout()).defaultNodeOrder(rbd.allLinks, rbd.loneNodeIDs,null,monitor);
    installNodeOrder(targetIDs, nabd, monitor);
    return (new ArrayList<NID.WithName>(targetIDs));
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

//      if (classToGroup[i].size() == 0) {
//        System.out.println("Empty Class:" + i + '\n');
//      }
      
      Set<NID.WithName> setForm = new TreeSet<NID.WithName>(classToGroup[i]);
      if (setForm.size() != classToGroup[i].size()) {
        throw new IllegalStateException("duplicate nodes in node group");
      }
      
      grouper.sortByDegree(classToGroup[i]);
      
      for (NID.WithName node : classToGroup[i]) {
        if (targetIDs.contains(node)) {
          throw new IllegalStateException("node already exists in node group");
        }
        targetIDs.add(node);
      }
    }
    
    return targetIDs;
  }
  
  /***************************************************************************
   **
   ** Breadth first search based on node groups
   */
  
  public List<NID.WithName> BFSNodeGroupByClass(BioFabricNetwork.NetworkAlignmentBuildData nabd,
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
    
    int numLink = nabd.allLinks.size();
    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
    
    Iterator<FabricLink> alit = nabd.allLinks.iterator();
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
    // Initialize data stuctures for layout
    //
    
    NetAlignNodeGrouper grouper = new NetworkAlignmentLayout.NetAlignNodeGrouper(nabd.allLinks, nabd.loneNodeIDs);
    
    SortedMap<Integer, List<NID.WithName>> classToGroup = new TreeMap<Integer, List<NID.WithName>>(); // master list of nodes in each group
    
    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) {
      classToGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
    for (NID.WithName node : allNodes) {
      int nodeClass = grouper.getNodeGroup(node);
      classToGroup.get(nodeClass).add(node);
    }
    
    for (List<NID.WithName> group : classToGroup.values()) { // sort by decreasing degree
      grouper.sortByDegree(group);
    }
    
    SortedMap<Integer, List<NID.WithName>> targsLeftToGoGroup = new TreeMap<Integer, List<NID.WithName>>();
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
      targsLeftToGoGroup.put(i, new ArrayList<NID.WithName>());
      for (NID.WithName node : classToGroup.get(i)) {
        targsLeftToGoGroup.get(i).add(node);
      }
    }
    
    SortedMap<Integer, List<NID.WithName>> targetsGroup = new TreeMap<Integer, List<NID.WithName>>();
    SortedMap<Integer, List<NID.WithName>> queueGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) { // each group gets a queue and targets list
      targetsGroup.put(i, new ArrayList<NID.WithName>());
      queueGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    //
    // Start breadth-first-search on first node group
    //
    
    int currGroup = 1;
    while (currGroup < NUMBER_NODE_GROUPS) {
      
      if (targsLeftToGoGroup.get(currGroup).isEmpty()) {
        currGroup++;
        continue; // continue only after each node in group has been visited
      }
      
      if (queueGroup.get(currGroup).isEmpty()) {
        // if queue is emtpy, pull head node from list
        NID.WithName head = targsLeftToGoGroup.get(currGroup).remove(0);
        queueGroup.get(currGroup).add(head);
      }
      
      flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup,
              classToGroup, monitor, .25, .50, nabd.allLinks, nabd.loneNodeIDs, currGroup, grouper);
    }
    
    //
    // Add lone nodes and "flatten" out the targets into one list
    //
    
    List<NID.WithName> targets = new ArrayList<NID.WithName>();
    
    targets.addAll(classToGroup.get(PURPLE_SINGLETON));
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
      List<NID.WithName> get = targetsGroup.get(i);
//      if (!get.isEmpty()) {
//        System.out.println("NG " + i);
//      }
      for (int k = 0; k < get.size(); k++) {
        NID.WithName node = get.get(k);
        targets.add(node);
//        if (k == 0 || k == get.size() - 1) {
//          System.out.println(node.getName());
//        }
      }
    }
    targets.addAll(classToGroup.get(RED_SINGLETON));
    
    if (targets.size() != allNodes.size()) {
      throw new IllegalStateException("target size not equal to all-nodes size");
    }
    
    UiUtil.fixMePrintout("Loop Reporter all messed up in NetworkAlignmentLayout.FlushQueue");
    return (targets);
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
                          SortedMap<Integer, List<NID.WithName>> classToGroup,
                          BTProgressMonitor monitor, double startFrac, double endFrac, Set<FabricLink> allLinks,
                          Set<NID.WithName> loneNodes, final int currGroup,
                          NetAlignNodeGrouper grouper)
          throws AsynchExitRequestException {
    
    LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
    int lastSize = targsToGo.size();
    List<NID.WithName> queue = queuesGroup.get(currGroup);
    List<NID.WithName> leftToGo = targsLeftToGoGroup.get(currGroup);
    
    while (! queue.isEmpty()) {
      
      NID.WithName node = queue.remove(0);
      int ttgSize = leftToGo.size();
      lr.report(lastSize - ttgSize);
      lastSize = ttgSize;
      
      if (targetsGroup.get(currGroup).contains(node)) {
        continue; // visited each node only once
      }
      targetsGroup.get(currGroup).add(node);
      
      if (grouper.getNodeGroup(node) != currGroup) {
        throw new IllegalStateException("Node of incorrect group in queue");
      }
      
      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node, allLinks, loneNodes);
      for (NID.WithName kid : myKids) {
        
        if (! targsToGo.contains(kid)) {
          throw new IllegalStateException("kid not in targsToGo");
        }
        
        int kidGroup = grouper.getNodeGroup(kid);

//        if (kidGroup < currGroup) {
//          throw new IllegalStateException("kid group less than current (parent) group");
//        }
        
        if (kidGroup == currGroup) {
          if (leftToGo.contains(kid)) {
            queue.add(kid);
            leftToGo.remove(kid);
            targsToGo.remove(kid);
          }
        } else {
          if (! queuesGroup.get(kidGroup).contains(kid)) {
            queuesGroup.get(kidGroup).add(kid); // if node from another group, put it in its queue
          }
        }
//        if (queue.size() > classToGroup.get(currGroup).size()) {
//          throw new IllegalStateException("queue bigger than node group");
//        }
      }
    }
    lr.finish();
    return;
  }
  
  /***************************************************************************
   **
   ** Node ordering
   */
  
  private static List<NID.WithName> orderMyKids(final Map<NID.WithName, Set<NID.WithName>> targsPerSource,
                                                Map<NID.WithName, Integer> linkCounts,
                                                Set<NID.WithName> targsToGo, final NID.WithName node, Set<FabricLink> allLinks,
                                                Set<NID.WithName> loneNodeIDs) {
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
  
  private static final int NUMBER_NODE_GROUPS = 19;
  
  private static final int

//          PURPLE_SINGLETON = 0,
//          PURPLE_WITH_ONLY_PURPLE = 1,            // PURPLE NODES IN LINK GROUP 1, 2, 3
//          PURPLE_WITH_PURPLE_BLUE = 2,
//          PURPLE_WITH_PURPLE_BLUE_RED = 3,
//          PURPLE_WITH_PURPLE_RED = 4,
//          PURPLE_WITH_ONLY_RED = 5,
//          PURPLE_WITH_BLUE_RED = 6,
//          PURPLE_WITH_ONLY_BLUE = 7,
//
//          PURPLE_WITH_BLUE_ORANGE = 8,            // PURPLE NODES IN LINK GROUP 4
//          PURPLE_WITH_ONLY_ORANGE = 9,
//          PURPLE_WITH_PURPLE_ORANGE = 10,
//          PURPLE_WITH_PURPLE_BLUE_ORANGE = 11,
//          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 12,
//          PURPLE_WITH_PURPLE_RED_ORANGE = 13,
//          PURPLE_WITH_RED_ORANGE = 14,
//          PURPLE_WITH_BLUE_RED_ORANGE = 15,
//
//          RED_WITH_ORANGE = 16,                   // RED NODES IN LINK GROUP 4
//          RED_WITH_ORANGE_YELLOW = 17,
//          RED_WITH_ONLY_YELLOW = 18,              // RED NODES IN LINK GROUP 5
//          RED_SINGLETON = 19;
          
          
          //original node ordering taken from previous push
          PURPLE_SINGLETON = 0,
          PURPLE_WITH_ONLY_PURPLE = 1,             // FIRST THREE LINK GROUPS
          PURPLE_WITH_ONLY_BLUE = 2,
          PURPLE_WITH_ONLY_RED = 3,
          PURPLE_WITH_PURPLE_BLUE = 4,
          PURPLE_WITH_PURPLE_RED = 5,
          PURPLE_WITH_BLUE_RED = 6,
          PURPLE_WITH_PURPLE_BLUE_RED = 7,
  
          PURPLE_WITH_ONLY_ORANGE = 8,              // PURPLE NODES IN LINK GROUP 3
          PURPLE_WITH_PURPLE_ORANGE = 9,
          PURPLE_WITH_BLUE_ORANGE = 10,
          PURPLE_WITH_RED_ORANGE = 11,
          PURPLE_WITH_PURPLE_BLUE_ORANGE = 12,
          PURPLE_WITH_PURPLE_RED_ORANGE = 13,
          PURPLE_WITH_BLUE_RED_ORANGE = 14,
          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 15,
  
          RED_WITH_ORANGE = 16,                    // RED NODES IN LINK GROUP 5
          RED_WITH_ONLY_YELLOW = 17,
          RED_WITH_ORANGE_YELLOW = 18,
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
      
//      if (node.getName().equals("PHM8") ||
//              node.getName().equals("ATM1")||
//      node.getName().equals("PUT1")||
//      node.getName().equals("CTM1")||
//      node.getName().equals("SBE2")){
//
//      }
//
//      if (!nodeToLinks_.containsKey(node)) {
//        System.out.println("Node error:" + node);
//      }
      
      if (purpleSingleton(node)) {
        return PURPLE_SINGLETON;
        
      } else if (purpleWithOnlyPurple(node)) {
        return PURPLE_WITH_ONLY_PURPLE;
        
      } else if (purpleWithPurpleBlue(node)) {
        return PURPLE_WITH_PURPLE_BLUE;
        
      } else if (purpleWithPurpleBlueRed(node)) {
        return PURPLE_WITH_PURPLE_BLUE_RED;
        
      } else if (purpleWithPurpleRed(node)) {
        return PURPLE_WITH_PURPLE_RED;
        
      } else if (purpleWithOnlyRed(node)) {
        return PURPLE_WITH_ONLY_RED;
        
      } else if (purpleWithBlueRed(node)) {
        return PURPLE_WITH_BLUE_RED;
        
      } else if (purpleWithOnlyBlue(node)) {
        return PURPLE_WITH_ONLY_BLUE;
        
      } else if (purpleWithBlueOrange(node)) {
        return PURPLE_WITH_BLUE_ORANGE;
        
      } else if (purpleWithOnlyOrange(node)) {
        return PURPLE_WITH_ONLY_ORANGE;
        
      } else if (purpleWithPurpleOrange(node)) {
        return PURPLE_WITH_PURPLE_ORANGE;
        
      } else if (purpleWithPurpleBlueOrange(node)) {
        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
        
      } else if (purpleWithPurpleBlueRedOrange(node)) {
        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
        
      } else if (purpleWithPurpleRedOrange(node)) {
        return PURPLE_WITH_PURPLE_RED_ORANGE;
        
      } else if (purpleWithRedOrange(node)) {
        return PURPLE_WITH_RED_ORANGE;
        
      } else if (purpleWithBlueRedOrange(node)) {
        return PURPLE_WITH_BLUE_RED_ORANGE;
        
      } else if (redWithOnlyOrange(node)) {
        return RED_WITH_ORANGE;
        
      } else if (redWithOrangeYellow(node)) {
        return RED_WITH_ORANGE_YELLOW;
        
      } else if (redWithOnlyYellow(node)) {
        return RED_WITH_ONLY_YELLOW;
        
      } else if (redSingleton(node)) {
        return RED_SINGLETON;
        
      } else {
//        System.out.println(node.getName() + "  " + nodeToLinks_.get(node).size());
//        return RED_SINGLETON;
//        System.out.println(node + "  " + nodeToLinks_.get(node).size());
        throw new IllegalArgumentException("Node group not found");
//        return RED_SINGLETON;
      }
    }
    
    void sortByDegree(List<NID.WithName> group) {
      
      Collections.sort(group, new Comparator<NID.WithName>() {
        public int compare(NID.WithName node1, NID.WithName node2) {
          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
        }
      });
      return;
    }
    
    // LINK GROUPS 1, 2, 3
    boolean purpleSingleton(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      boolean isOK = nodeToLinks_.get(node).isEmpty();
      return (isOK);
    }
    
    boolean purpleWithOnlyPurple(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithOnlyBlue(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {GRAPH1};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithOnlyRed(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {INDUCED_GRAPH2};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleBlue(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, GRAPH1};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleRed(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithBlueRed(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {GRAPH1, INDUCED_GRAPH2};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleBlueRed(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2};
      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    // LINK GROUP 4
    boolean purpleWithOnlyOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithBlueOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {GRAPH1, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithRedOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleBlueOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, GRAPH1, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleRedOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithBlueRedOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean purpleWithPurpleBlueRedOrange(NID.WithName node) {
      if (! isPurple(node)) {
        return (false);
      }
      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean redWithOnlyOrange(NID.WithName node) {
      if (! isRed(node)) {
        return (false);
      } // is it ONLY orange?
      String[] rels = {HALF_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    // LINK GROUP 5
    boolean redWithOnlyYellow(NID.WithName node) {
      if (! isRed(node)) {
        return (false);
      }
      String[] rels = {FULL_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean redWithOrangeYellow(NID.WithName node) {
      if (! isRed(node)) {
        return (false);
      }
      String[] rels = {HALF_UNALIGNED_GRAPH2, FULL_UNALIGNED_GRAPH2};
      boolean isOK = hasOnlyEdgesOfRels(node, rels);
      return (isOK);
    }
    
    boolean redSingleton(NID.WithName node) {
      if (! isRed(node)) {
        return (false);
      }
      boolean isOK = nodeToLinks_.get(node).isEmpty();
      return (isOK);
    }
    
    // Helper functions
    boolean hasOnlyRedNeighbors(NID.WithName node) {
      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
        if (! isRed(neigh)) {
          return (false);
        }
      }
      return (true);
    }
    
    boolean hasOnlyPurpleNeighbors(NID.WithName node) {
      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
        if (! isPurple(neigh)) {
          return (false);
        }
      }
      return (true);
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
          return (false);
        }
      }
      // check if each relation has been visited at least once
      for (boolean visit : visitedEachRel) {
        if (! visit) {
          return (false);
        }
      }
      return (true);
    }
    
    /*******************************************************************
     **
     ** Identifies Aligned Nodes if they have a dash ('-') in name:
     ** This must be changed.
     */
    
    boolean isPurple(NID.WithName node) {
      UiUtil.fixMePrintout("FIX ME:find way to identify aligned nodes besides having dash in name");
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
      if (purpleWithOnlyPurple(node)) {
        err.add(1);
        
      }
      
      if (purpleWithOnlyBlue(node)) {
        err.add(2);
        
        
      }
      if (purpleWithOnlyRed(node)) {
        err.add(3);
        
        
      }
      if (purpleWithPurpleBlue(node)) {
        err.add(4);
        
      }
      if (purpleWithPurpleRed(node)) {
        err.add(5);
        
        
      }
      if (purpleWithBlueRed(node)) {
        err.add(6);
        
        
      }
      if (purpleWithPurpleBlueRed(node)) {
        err.add(7);
      }
      // LINK GROUP 4
      if (purpleWithOnlyOrange(node)) {
        err.add(8);
        
      }
      if (purpleWithPurpleOrange(node)) {
        err.add(9);
        
      }
      if (purpleWithBlueOrange(node)) {
        err.add(10);
        
      }
      if (purpleWithRedOrange(node)) {
        err.add(11);
        
      }
      if (purpleWithPurpleBlueOrange(node)) {
        err.add(12);
        
        
      }
      if (purpleWithPurpleRedOrange(node)) {
        err.add(13);
        
      }
      if (purpleWithBlueRedOrange(node)) {
        err.add(14);
        
      }
      if (purpleWithPurpleBlueRedOrange(node)) {
        err.add(15);
        
      }
      if (redWithOnlyOrange(node)) {
        err.add(16);
        
      }
      // LINK GROUP 5
      
      if (redWithOnlyYellow(node)) {
        err.add(17);
        
      }
      if (redWithOrangeYellow(node)) {
        err.add(18);
        
        
      }
      if (purpleSingleton(node)) {
        err.add(19);
        
      }
      if (redSingleton(node)) {
        err.add(20);
      }
      
      if (err.size() != 1) {
        System.out.println(node + "\nerror" + err + "\n\n\n\n\n\n");
      }
    }
    
  }
  
}

//if (purpleSingleton(node)) {
//        return PURPLE_SINGLETON;
//
//        } else if (purpleWithOnlyPurple(node)) {
//        return PURPLE_WITH_ONLY_PURPLE;
//
//        } else if (purpleWithPurpleBlue(node)) {
//        return PURPLE_WITH_PURPLE_BLUE;
//
//        } else if (purpleWithPurpleBlueRed(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED;
//
//        } else if (purpleWithPurpleRed(node)) {
//        return PURPLE_WITH_PURPLE_RED;
//
//        } else if (purpleWithOnlyRed(node)) {
//        return PURPLE_WITH_ONLY_RED;
//
//        } else if (purpleWithBlueRed(node)) {
//        return PURPLE_WITH_BLUE_RED;
//
//        } else if (purpleWithOnlyBlue(node)) {
//        return PURPLE_WITH_ONLY_BLUE;
//
//        } else if (purpleWithBlueOrange(node)) {
//        return PURPLE_WITH_BLUE_ORANGE;
//
//        } else if (purpleWithOnlyOrange(node)) {
//        return PURPLE_WITH_ONLY_ORANGE;
//
//        } else if (purpleWithPurpleOrange(node)) {
//        return PURPLE_WITH_PURPLE_ORANGE;
//
//        } else if (purpleWithPurpleBlueOrange(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
//
//        } else if (purpleWithPurpleBlueRedOrange(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
//
//        } else if (purpleWithPurpleRedOrange(node)) {
//        return PURPLE_WITH_PURPLE_RED_ORANGE;
//
//        } else if (purpleWithRedOrange(node)) {
//        return PURPLE_WITH_RED_ORANGE;
//
//        } else if (purpleWithBlueRedOrange(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//        } else if (redWithOnlyOrange(node)) {
//        return RED_WITH_ORANGE;
//
//        } else if (redWithOrangeYellow(node)) {
//        return RED_WITH_ORANGE_YELLOW;
//
//        } else if (redWithOnlyYellow(node)) {
//        return RED_WITH_ONLY_YELLOW;
//
//        } else if (redSingleton(node)) {
//        return RED_SINGLETON;
//
//        } else {
//        throw new IllegalArgumentException("Node group not found");
//        }
