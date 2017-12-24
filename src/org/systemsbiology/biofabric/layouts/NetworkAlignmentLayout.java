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

import org.systemsbiology.biofabric.model.AnnotationSet;
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
  
    List<NID.WithName> targetIDs;
    
    if (nabd.forOrphans) {
      targetIDs = (new DefaultLayout()).defaultNodeOrder(nabd.allLinks, nabd.loneNodeIDs,null, monitor);
    } else {
      targetIDs = BFSNodeGroup(nabd, monitor);
    }
    
    installNodeOrder(targetIDs, nabd, monitor);
    return (new ArrayList<NID.WithName>(targetIDs));
  }
  
  /***************************************************************************
   **
   ** Breadth first search based on node groups
   */
  
  public List<NID.WithName> BFSNodeGroup(BioFabricNetwork.NetworkAlignmentBuildData nabd,
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
    // Initialize data structures for layout
    //
    
    NodeGroupMap grouper = new NodeGroupMap(nabd, DefaultNodeGroupOrder);
  
    // master list of nodes in each group
    SortedMap<Integer, List<NID.WithName>> classToGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) {
      classToGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
    for (NID.WithName node : allNodes) {
      int nodeClass = grouper.getIndex(node);
      classToGroup.get(nodeClass).add(node);
    }
    
    for (List<NID.WithName> group : classToGroup.values()) { // sort by decreasing degree
      grouper.sortByDegree(group);
    }
    
    SortedMap<Integer, List<NID.WithName>> targetsGroup = new TreeMap<Integer, List<NID.WithName>>(),
                                           queueGroup = new TreeMap<Integer, List<NID.WithName>>(),
                                           targsLeftToGoGroup = new TreeMap<Integer, List<NID.WithName>>();
  
    // each node group (not singletons) gets queue and targets list
    for (int i = 1; i < NUMBER_NODE_GROUPS; i++) {
      targetsGroup.put(i, new ArrayList<NID.WithName>());
      queueGroup.put(i, new ArrayList<NID.WithName>());
      targsLeftToGoGroup.put(i, new ArrayList<NID.WithName>());
      for (NID.WithName node : classToGroup.get(i)) {
        targsLeftToGoGroup.get(i).add(node);
      }
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
      // if queue is empty, pull head node from list
      if (queueGroup.get(currGroup).isEmpty()) {
        NID.WithName head = targsLeftToGoGroup.get(currGroup).remove(0);
        queueGroup.get(currGroup).add(head);
      }
      
      flushQueue(targetsGroup, targsPerSource, linkCounts, targsToGo, targsLeftToGoGroup, queueGroup,
              monitor, .25, .50, currGroup, grouper);
    }
    
    //
    // Add lone nodes and "flatten" out the targets into one list
    //
    
    targetsGroup.put(PURPLE_SINGLETON, new ArrayList<NID.WithName>());
    targetsGroup.get(PURPLE_SINGLETON).addAll(classToGroup.get(PURPLE_SINGLETON));
    targetsGroup.put(RED_SINGLETON, new ArrayList<NID.WithName>());
    targetsGroup.get(RED_SINGLETON).addAll(classToGroup.get(RED_SINGLETON));
    
    List<NID.WithName> targets = new ArrayList<NID.WithName>();
    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) {
      List<NID.WithName> group = targetsGroup.get(i);
      for (NID.WithName node : group) {
        targets.add(node);
      }
    }
    
    if (targets.size() != allNodes.size()) {
      throw new IllegalStateException("target size not equal to all-nodes size");
    }
    
    installAnnotations(nabd, targetsGroup, targets);

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
                          BTProgressMonitor monitor, double startFrac, double endFrac, final int currGroup,
                          NodeGroupMap grouper)
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
      
      if (grouper.getIndex(node) != currGroup) {
        throw new IllegalStateException("Node of incorrect group in queue");
      }
      
      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      for (NID.WithName kid : myKids) {
        
        if (! targsToGo.contains(kid)) {
          throw new IllegalStateException("kid not in targsToGo");
        }
        
        int kidGroup = grouper.getIndex(kid);

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
                                                Set<NID.WithName> targsToGo, final NID.WithName node) {
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
   ** Install Layer Zero Node Annotations
   */
  
  private void installAnnotations(BioFabricNetwork.NetworkAlignmentBuildData nabd,
                                         SortedMap<Integer, List<NID.WithName>> targetsGroup,
                                         List<NID.WithName> targets) {
  
    Map<Integer, List<NID.WithName>> layerZeroAnnot = new TreeMap<Integer, List<NID.WithName>>();
  
    for (int i = 0; i <= NUMBER_NODE_GROUPS; i++) { // include singletons
      List<NID.WithName> group = targetsGroup.get(i);
      if (group.isEmpty()) {
        continue;
      }
      layerZeroAnnot.put(i, new ArrayList<NID.WithName>()); // add first and last node in each group
      layerZeroAnnot.get(i).add(group.get(0));
      layerZeroAnnot.get(i).add(group.get(group.size() - 1));
    }

    AnnotationSet annots = new AnnotationSet();
    for (Map.Entry<Integer, List<NID.WithName>> entry : layerZeroAnnot.entrySet()) {

      int nodeGroup = entry.getKey();
      String start = entry.getValue().get(0).toString(), end = entry.getValue().get(1).toString();
      int min = -1, max = -1;

      // make more efficient
      for (int i = 0; i < targets.size(); i++) {
        if (start.equals(targets.get(i).toString())) {
          min = i;
        }
        if (end.equals(targets.get(i).toString())) {
          max = i;
        }
      }
      if (min > max || min < 0) {
//        System.out.println(min + "  " + max +"  NG:" + nodeGroup);
        throw new IllegalStateException("Annotation min max error in NetAlign Layout");
      }

      AnnotationSet.Annot annot = new AnnotationSet.Annot(DefaultNodeGroupOrder[nodeGroup], min, max,0);
      annots.addAnnot(annot);
    }
    nabd.setNodeAnnots(annots);
    return;
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
   **
   **
   ** WE HAVE 18 DISTINCT CLASSES (NODE GROUPS) FOR EACH ALIGNED AND UNALIGNED NODE
   ** TECHNICALLY 20 INCLUDING THE SINGLETON ALIGNED AND SINGLETON UNALIGNED NODES
   **
   */
  
  private static final int NUMBER_LINK_GROUPS = 5;   // 0..4
  
  private static final int
          PURPLE_EDGES = 0,
          BLUE_EDGES = 1,
          RED_EDGES = 2,
          ORANGE_EDGES = 3,
          YELLOW_EDGES = 4;
  
  private static final int NUMBER_NODE_GROUPS = 19; // 0..19
  
  private static final int
          PURPLE_SINGLETON = 0,
          RED_SINGLETON = 19;
  
  
  private static final String[] DefaultNodeGroupOrder = {
          "(P:0)",
          "(P:P)",            // FIRST THREE LINK GROUPS
          "(P:B)",
          "(P:pRp)",
          "(P:P/B)",
          "(P:P/pRp)",
          "(P:B/pRp)",
          "(P:P/B/pRp)",
          "(P:pRr)",          // PURPLE NODES IN LINK GROUP 3
          "(P:P/pRr)",
          "(P:B/pRr)",
          "(P:pRp/pRr)",
          "(P:P/B/pRr)",
          "(P:P/pRp/pRr)",
          "(P:B/pRp/pRr)",
          "(P:P/B/pRp/pRr)",
          "(R:pRr)",          // RED NODES IN LINK GROUP 5
          "(R:rRr)",
          "(R:pRr/rRr)",
          "(R:0)"
  };
  
  /***************************************************************************
   **
   ** HashMap based Data structure
   */
  
  private static class NodeGroupMap {
  
    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
    private Map<NID.WithName, Boolean> mergedToCorrect_;
    private Map<GroupID, Integer> groupIDtoIndex;
    
    public NodeGroupMap(BioFabricNetwork.NetworkAlignmentBuildData nabd, String[] nodeGroupOrder) {
      this(nabd.allLinks, nabd.loneNodeIDs, nabd.mergedToCorrect_, nodeGroupOrder);
    }
  
    public NodeGroupMap(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs,
                        Map<NID.WithName, Boolean> mergedToCorrect, String[] nodeGroupOrder) {
      this.mergedToCorrect_ = mergedToCorrect;
      generateStructs(allLinks, loneNodeIDs);
      generateMap(nodeGroupOrder);
      return;
    }
    
    private void generateStructs(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs) {
      
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
    
    private void generateMap(String[] nodeGroupOrder) {
      groupIDtoIndex = new HashMap<GroupID, Integer>();
      for (int index = 0; index < nodeGroupOrder.length; index++) {
        GroupID gID = new GroupID(nodeGroupOrder[index]);
        groupIDtoIndex.put(gID, index);
      }
      return;
    }
  
    /***************************************************************************
     **
     ** Return the index from the given node group ordering
     */
    
    public int getIndex(NID.WithName node) {
      GroupID groupID = generateID(node);
      if (groupIDtoIndex.get(groupID) == null) {
        System.out.println(groupID + " null");
        throw new IllegalStateException("GroupID not found in given order");
      }
      return (groupIDtoIndex.get(groupID));
    }
  
    /***************************************************************************
     **
     ** Hash function
     */
    
    private GroupID generateID(NID.WithName node) {
      StringBuilder sb = new StringBuilder();
      
      //
      // See which types of link groups the node's links are in
      //
    
      String[] possibleRels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2, FULL_UNALIGNED_GRAPH2};
      boolean[] inLG = new boolean[NUMBER_LINK_GROUPS];
      
      for (FabricLink link : nodeToLinks_.get(node)) {
        for (int rel = 0; rel < inLG.length; rel++) {
          if (link.getRelation().equals(possibleRels[rel])) {
            inLG[rel] = true;
          }
        }
      }
      
      List<String> tags = new ArrayList<String>();
      if (inLG[PURPLE_EDGES]) {
        tags.add("P");
      }
      if (inLG[BLUE_EDGES]) {
        tags.add("B");
      }
      if (inLG[RED_EDGES]) {
        tags.add("pRp");
      }
      if (inLG[ORANGE_EDGES]) {
        tags.add("pRr");
      }
      if (inLG[YELLOW_EDGES]) {
        tags.add("rRr");
      }
  
      sb.append("(");
      sb.append((isPurple(node) ? "P" : "R") + ":");  // aligned/unaligned node
      
      for (int i = 0; i < tags.size(); i++) {         // link group tags
        if (i != tags.size() - 1) {
          sb.append(tags.get(i) + "/");
        } else {
          sb.append(tags.get(i));
        }
      }
      
      if (tags.size() == 0) { // for singletons
        sb.append("0");
      }
  
      sb.append(")");
      
      // aligned correctly
//      if (mergedToCorrect_ == null) {
//        sb.append(0);
//      } else {
//        sb.append((mergedToCorrect_.get(node)) ? 1 : 0);
//      }
      UiUtil.fixMePrintout("GroupID correctly aligned");
      return (new GroupID(sb.toString()));
    }
    
    /*******************************************************************
     **
     ** Identifies Aligned Nodes if they have a dash ('-') in name:
     */
  
    private boolean isPurple(NID.WithName node) {
      UiUtil.fixMePrintout("FIX ME:find way to identify aligned nodes besides having dash in name");
      return (node.getName().contains("-"));
    }
  
    /*******************************************************************
     **
     ** Sorts Node group on decreasing degree
     */
    
    private void sortByDegree(List<NID.WithName> group) {
      Collections.sort(group, new Comparator<NID.WithName>() {
        public int compare(NID.WithName node1, NID.WithName node2) {
          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
        }
      });
      return;
    }
  
  }
  
  /***************************************************************************
   **
   ** Hash for Hash-map data structure for node groups
   */
  
  private static class GroupID {
    
    private final String key;
    
    public GroupID(String key) {
      this.key = key;
    }
  
    @Override
    public String toString() {
      return (key);
    }
  
    @Override
    public boolean equals(Object o) {
      if (this == o) return (true);
      if (! (o instanceof GroupID)) return (false);
      
      GroupID groupID = (GroupID) o;
      
      if (key != null ? (! key.equals(groupID.key)) : (groupID.key != null)) return (false);
      return (true);
    }
  
    @Override
    public int hashCode() {
      return (key != null ? key.hashCode() : 0);
    }

  }
  
}

//      sb.append((inLG[PURPLE_EDGES] ? "P"   : 0) + "/");
//      sb.append((inLG[BLUE_EDGES]   ? "B"   : 0) + "/");
//      sb.append((inLG[RED_EDGES]    ? "pRp" : 0) + "/");
//      sb.append((inLG[ORANGE_EDGES] ? "pRr" : 0) + "/");
//      sb.append((inLG[YELLOW_EDGES] ? "rRr" : 0) + "/");

//      sb.append((inLG[PURPLE_EDGES] ? "P/"   : "") + "");
//      sb.append((inLG[BLUE_EDGES]   ? "B/"   : "") + "");
//      sb.append((inLG[RED_EDGES]    ? "pRp/" : "") + "");
//      sb.append((inLG[ORANGE_EDGES] ? "pRr/" : "") + "");
//      sb.append((inLG[YELLOW_EDGES] ? "rRr"  : "") + "");
//
//      if (sb.substring(sb.length()-1, sb.length()).equals("/")) {
//        sb.delete(sb.length()-1, sb.length());
//      }

//PURPLE_SINGLETON = 0,
//          PURPLE_WITH_ONLY_PURPLE = 1,             // FIRST THREE LINK GROUPS
//          PURPLE_WITH_ONLY_BLUE = 2,
//          PURPLE_WITH_ONLY_RED = 3,
//          PURPLE_WITH_PURPLE_BLUE = 4,
//          PURPLE_WITH_PURPLE_RED = 5,
//          PURPLE_WITH_BLUE_RED = 6,
//          PURPLE_WITH_PURPLE_BLUE_RED = 7,
//
//          PURPLE_WITH_ONLY_ORANGE = 8,              // PURPLE NODES IN LINK GROUP 3
//          PURPLE_WITH_PURPLE_ORANGE = 9,
//          PURPLE_WITH_BLUE_ORANGE = 10,
//          PURPLE_WITH_RED_ORANGE = 11,
//          PURPLE_WITH_PURPLE_BLUE_ORANGE = 12,
//          PURPLE_WITH_PURPLE_RED_ORANGE = 13,
//          PURPLE_WITH_BLUE_RED_ORANGE = 14,
//          PURPLE_WITH_PURPLE_BLUE_RED_ORANGE = 15,
//
//          RED_WITH_ORANGE = 16,                    // RED NODES IN LINK GROUP 5
//          RED_WITH_ONLY_YELLOW = 17,
//          RED_WITH_ORANGE_YELLOW = 18,
//        RED_SINGLETON = 19;

//  private static final String[] DefaultNodeGroupOrder = {
//          "(P:0/0/0/0/0/0)",
//          "(P:P/0/0/0/0/0)",            // 1
//          "(P:0/B/0/0/0/0)",
//          "(P:0/0/pRp/0/0/0)",
//          "(P:P/B/0/0/0/0)",
//          "(P:P/0/pRp/0/0/0)",
//          "(P:0/B/pRp/0/0/0)",        // 6
//          "(P:P/B/pRp/0/0/0)",
//          "(P:0/0/0/pRr/0/0)",
//          "(P:P/0/0/pRr/0/0)",
//          "(P:0/B/0/pRr/0/0)",
//          "(P:0/0/pRp/pRr/0/0)",      // 11
//          "(P:P/B/0/pRr/0/0)",
//          "(P:P/0/pRp/pRr/0/0)",
//          "(P:0/B/pRp/pRr/0/0)",
//          "(P:P/B/pRp/pRr/0/0)",
//          "(R:0/0/0/pRr/0/0)",          // 16
//          "(R:0/0/0/0/rRr/0)",
//          "(R:0/0/0/pRr/rRr/0)",
//          "(R:0/0/0/0/0/0)"
//  };


//  private static final String[] NodeGroupOrder = {
//          "(P:0)",
//          "(P:P)",            // 1
//          "(P:B)",
//          "(P:pRp)",
//          "(P:P/B)",
//          "(P:P/pRp",
//          "(P:B/pRp)",        // 6
//          "(P:P/B/pRp)",
//          "(P:pRr)",
//          "(P:P/pRr)",
//          "(P:B/pRr)",
//          "(P:pRp/pRr)",      // 11
//          "(P:P/B/pRr)",
//          "(P:P/pRp/pRr)",
//          "(P:B/pRp/pRr)",
//          "(P:P/B/pRp/pRr)",
//          "(R:pRr)",          // 16
//          "(R:rRr)",
//          "(R:pRr/rRr)",
//          "(R:0)"
//  };

// from the second file NodeGroups-Order.txt
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
////      if (node.getName().equals("PHM8") ||
////              node.getName().equals("ATM1")||
////      node.getName().equals("PUT1")||
////      node.getName().equals("CTM1")||
////      node.getName().equals("SBE2")){
////
////      }
////
////      if (!nodeToLinks_.containsKey(node)) {
////        System.out.println("Node error:" + node);
////      }
//
//      if (purpleSingleton(node)) {
//        return PURPLE_SINGLETON;
//
//      } else if (purpleWithOnlyPurple(node)) {
//        return PURPLE_WITH_ONLY_PURPLE;
//
//      } else if (purpleWithPurpleBlue(node)) {
//        return PURPLE_WITH_PURPLE_BLUE;
//
//      } else if (purpleWithPurpleBlueRed(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED;
//
//      } else if (purpleWithPurpleRed(node)) {
//        return PURPLE_WITH_PURPLE_RED;
//
//      } else if (purpleWithOnlyRed(node)) {
//        return PURPLE_WITH_ONLY_RED;
//
//      } else if (purpleWithBlueRed(node)) {
//        return PURPLE_WITH_BLUE_RED;
//
//      } else if (purpleWithOnlyBlue(node)) {
//        return PURPLE_WITH_ONLY_BLUE;
//
//      } else if (purpleWithBlueOrange(node)) {
//        return PURPLE_WITH_BLUE_ORANGE;
//
//      } else if (purpleWithOnlyOrange(node)) {
//        return PURPLE_WITH_ONLY_ORANGE;
//
//      } else if (purpleWithPurpleOrange(node)) {
//        return PURPLE_WITH_PURPLE_ORANGE;
//
//      } else if (purpleWithPurpleBlueOrange(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_ORANGE;
//
//      } else if (purpleWithPurpleBlueRedOrange(node)) {
//        return PURPLE_WITH_PURPLE_BLUE_RED_ORANGE;
//
//      } else if (purpleWithPurpleRedOrange(node)) {
//        return PURPLE_WITH_PURPLE_RED_ORANGE;
//
//      } else if (purpleWithRedOrange(node)) {
//        return PURPLE_WITH_RED_ORANGE;
//
//      } else if (purpleWithBlueRedOrange(node)) {
//        return PURPLE_WITH_BLUE_RED_ORANGE;
//
//      } else if (redWithOnlyOrange(node)) {
//        return RED_WITH_ORANGE;
//
//      } else if (redWithOrangeYellow(node)) {
//        return RED_WITH_ORANGE_YELLOW;
//
//      } else if (redWithOnlyYellow(node)) {
//        return RED_WITH_ONLY_YELLOW;
//
//      } else if (redSingleton(node)) {
//        return RED_SINGLETON;
//
//      } else {
////        System.out.println(node.getName() + "  " + nodeToLinks_.get(node).size());
////        return RED_SINGLETON;
////        System.out.println(node + "  " + nodeToLinks_.get(node).size());
//        throw new IllegalArgumentException("Node group not found");
////        return RED_SINGLETON;
//      }
//    }
//
//    void sortByDegree(List<NID.WithName> group) {
//
//      Collections.sort(group, new Comparator<NID.WithName>() {
//        public int compare(NID.WithName node1, NID.WithName node2) {
//          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
//          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
//        }
//      });
//      return;
//    }
//
//    // LINK GROUPS 1, 2, 3
//    boolean purpleSingleton(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      boolean isOK = nodeToLinks_.get(node).isEmpty();
//      return (isOK);
//    }
//
//    boolean purpleWithOnlyPurple(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithOnlyBlue(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {GRAPH1};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithOnlyRed(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {INDUCED_GRAPH2};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleBlue(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, GRAPH1};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleRed(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithBlueRed(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {GRAPH1, INDUCED_GRAPH2};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleBlueRed(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2};
//      boolean isOK = hasOnlyPurpleNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    // LINK GROUP 4
//    boolean purpleWithOnlyOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithBlueOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {GRAPH1, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithRedOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleBlueOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, GRAPH1, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleRedOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithBlueRedOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean purpleWithPurpleBlueRedOrange(NID.WithName node) {
//      if (! isPurple(node)) {
//        return (false);
//      }
//      String[] rels = {COVERED_EDGE, GRAPH1, INDUCED_GRAPH2, HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean redWithOnlyOrange(NID.WithName node) {
//      if (! isRed(node)) {
//        return (false);
//      } // is it ONLY orange?
//      String[] rels = {HALF_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    // LINK GROUP 5
//    boolean redWithOnlyYellow(NID.WithName node) {
//      if (! isRed(node)) {
//        return (false);
//      }
//      String[] rels = {FULL_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyRedNeighbors(node) && hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean redWithOrangeYellow(NID.WithName node) {
//      if (! isRed(node)) {
//        return (false);
//      }
//      String[] rels = {HALF_UNALIGNED_GRAPH2, FULL_UNALIGNED_GRAPH2};
//      boolean isOK = hasOnlyEdgesOfRels(node, rels);
//      return (isOK);
//    }
//
//    boolean redSingleton(NID.WithName node) {
//      if (! isRed(node)) {
//        return (false);
//      }
//      boolean isOK = nodeToLinks_.get(node).isEmpty();
//      return (isOK);
//    }
//
//    // Helper functions
//    boolean hasOnlyRedNeighbors(NID.WithName node) {
//      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
//        if (! isRed(neigh)) {
//          return (false);
//        }
//      }
//      return (true);
//    }
//
//    boolean hasOnlyPurpleNeighbors(NID.WithName node) {
//      for (NID.WithName neigh : nodeToNeighbors_.get(node)) {
//        if (! isPurple(neigh)) {
//          return (false);
//        }
//      }
//      return (true);
//    }
//
//    boolean hasOnlyEdgesOfRels(NID.WithName node, String[] relsAllowed) {
//
//      boolean[] visitedEachRel = new boolean[relsAllowed.length];
//
//      for (FabricLink link : nodeToLinks_.get(node)) {
//
//        boolean linkAllowed = false;
//        for (int i = 0; i < relsAllowed.length; i++) {
//          if (link.getRelation().equals(relsAllowed[i])) {
//            linkAllowed = true;
//            visitedEachRel[i] = true;
//          }
//        }
//        if (! linkAllowed) {
//          return (false);
//        }
//      }
//      // check if each relation has been visited at least once
//      for (boolean visit : visitedEachRel) {
//        if (! visit) {
//          return (false);
//        }
//      }
//      return (true);
//    }
//
//    /*******************************************************************
//     **
//     ** Identifies Aligned Nodes if they have a dash ('-') in name:
//     ** This must be changed.
//     */
//
//    boolean isPurple(NID.WithName node) {
//      UiUtil.fixMePrintout("FIX ME:find way to identify aligned nodes besides having dash in name");
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
//      if (purpleWithOnlyPurple(node)) {
//        err.add(1);
//
//      }
//
//      if (purpleWithOnlyBlue(node)) {
//        err.add(2);
//
//
//      }
//      if (purpleWithOnlyRed(node)) {
//        err.add(3);
//
//
//      }
//      if (purpleWithPurpleBlue(node)) {
//        err.add(4);
//
//      }
//      if (purpleWithPurpleRed(node)) {
//        err.add(5);
//
//
//      }
//      if (purpleWithBlueRed(node)) {
//        err.add(6);
//
//
//      }
//      if (purpleWithPurpleBlueRed(node)) {
//        err.add(7);
//      }
//      // LINK GROUP 4
//      if (purpleWithOnlyOrange(node)) {
//        err.add(8);
//
//      }
//      if (purpleWithPurpleOrange(node)) {
//        err.add(9);
//
//      }
//      if (purpleWithBlueOrange(node)) {
//        err.add(10);
//
//      }
//      if (purpleWithRedOrange(node)) {
//        err.add(11);
//
//      }
//      if (purpleWithPurpleBlueOrange(node)) {
//        err.add(12);
//
//
//      }
//      if (purpleWithPurpleRedOrange(node)) {
//        err.add(13);
//
//      }
//      if (purpleWithBlueRedOrange(node)) {
//        err.add(14);
//
//      }
//      if (purpleWithPurpleBlueRedOrange(node)) {
//        err.add(15);
//
//      }
//      if (redWithOnlyOrange(node)) {
//        err.add(16);
//
//      }
//      // LINK GROUP 5
//
//      if (redWithOnlyYellow(node)) {
//        err.add(17);
//
//      }
//      if (redWithOrangeYellow(node)) {
//        err.add(18);
//
//
//      }
//      if (purpleSingleton(node)) {
//        err.add(19);
//
//      }
//      if (redSingleton(node)) {
//        err.add(20);
//      }
//
//      if (err.size() != 1) {
//        System.out.println(node + "\nerror" + err + "\n\n\n\n\n\n");
//      }
//    }
//
//  }


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
