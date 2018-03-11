/*
**    File created by Rishi Desai
**
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

import java.security.acl.Group;
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
import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.analysis.NetworkAlignment;

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
    
    List<NID.WithName> targetIDs;
    
    if (nabd.forOrphans) {
      targetIDs = (new DefaultLayout()).defaultNodeOrder(nabd.allLinks, nabd.loneNodeIDs, null, monitor);
    } else {
      targetIDs = bfsNodeGroupLayout(nabd, monitor);
    }
    
    installNodeOrder(targetIDs, nabd, monitor);
    return (new ArrayList<NID.WithName>(targetIDs));
  }
  
  /***************************************************************************
   **
   ** Breadth first search based on node groups
   */
  
  public List<NID.WithName> bfsNodeGroupLayout(BioFabricNetwork.NetworkAlignmentBuildData nabd,
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
    
    NodeGroupMap grouper = new NodeGroupMap(nabd, defaultNGOrderWithoutCorrect, ngAnnotColorsWithoutCorrect);
    
    // master list of nodes in each group
    SortedMap<Integer, List<NID.WithName>> classToGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    for (int i = 0; i < grouper.numGroups(); i++) {
      classToGroup.put(i, new ArrayList<NID.WithName>());
    }
    
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(nabd.allLinks, nabd.loneNodeIDs, monitor);
    for (NID.WithName node : allNodes) {
      int nodeClass = grouper.getIndex(node);
      classToGroup.get(nodeClass).add(node);
    }
    
    for (List<NID.WithName> group : classToGroup.values()) { // sort by decreasing degree
//      grouper.sortByDecrDegree(group);
      Collections.sort(group, grouper.sortDecrDegree());
    }
    
    SortedMap<Integer, List<NID.WithName>> targetsGroup = new TreeMap<Integer, List<NID.WithName>>(),
            queueGroup = new TreeMap<Integer, List<NID.WithName>>(),
            targsLeftToGoGroup = new TreeMap<Integer, List<NID.WithName>>();
    
    // each node group (singletons too) gets queue and targets list
    for (int i = 0; i < grouper.numGroups(); i++) {
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

    int currGroup = 0;
    while (currGroup < grouper.numGroups()) {
      
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

    List<NID.WithName> targets = new ArrayList<NID.WithName>();
    for (int i = 0; i < grouper.numGroups(); i++) {
      List<NID.WithName> group = targetsGroup.get(i);
      for (NID.WithName node : group) {
        targets.add(node);
      }
    }
    
    if (targets.size() != allNodes.size()) {
      throw new IllegalStateException("target numGroups not equal to all-nodes numGroups");
    }

    installAnnotations(nabd, targetsGroup, targets, grouper);
    
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
      int ttgSize = targsLeftToGoGroup.get(currGroup).size();
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
                                  List<NID.WithName> targets, NodeGroupMap grouper) {
    
    Map<Integer, List<NID.WithName>> layerZeroAnnot = new TreeMap<Integer, List<NID.WithName>>();

    for (int i = 0; i < grouper.numGroups(); i++) { // include singletons
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
      int min = - 1, max = - 1;
      
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
        throw new IllegalStateException("Annotation min max error in NetAlign Layout");
      }
      
      AnnotationSet.Annot annot = new AnnotationSet.Annot(grouper.getKey(nodeGroup), min, max, 0, grouper.getColor(nodeGroup));
      annots.addAnnot(annot);
    }
    nabd.setNodeAnnotations(annots);
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
  
  public static final int NUMBER_LINK_GROUPS = 5;   // 0..4
  
  private static final int
          PURPLE_EDGES = 0,
          BLUE_EDGES = 1,
          RED_EDGES = 2,
          ORANGE_EDGES = 3,
          YELLOW_EDGES = 4;

  
  public static final String[] defaultNGOrderWithoutCorrect = {
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
  
  public static final String[] defaultNGOrderWithCorrect = {
          "(P:0/1)",
          "(P:0/0)",
          "(P:P/1)",
          "(P:P/0)",
          "(P:B/1)",
          "(P:B/0)",
          "(P:pRp/1)",
          "(P:pRp/0)",
          "(P:P/B/1)",
          "(P:P/B/0)",
          "(P:P/pRp/1)",
          "(P:P/pRp/0)",
          "(P:B/pRp/1)",
          "(P:B/pRp/0)",
          "(P:P/B/pRp/1)",
          "(P:P/B/pRp/0)",
          "(P:pRr/1)",
          "(P:pRr/0)",
          "(P:P/pRr/1)",
          "(P:P/pRr/0)",
          "(P:B/pRr/1)",
          "(P:B/pRr/0)",
          "(P:pRp/pRr/1)",
          "(P:pRp/pRr/0)",
          "(P:P/B/pRr/1)",
          "(P:P/B/pRr/0)",
          "(P:P/pRp/pRr/1)",
          "(P:P/pRp/pRr/0)",
          "(P:B/pRp/pRr/1)",
          "(P:B/pRp/pRr/0)",
          "(P:P/B/pRp/pRr/1)",
          "(P:P/B/pRp/pRr/0)",
          "(R:pRr/0)",
          "(R:rRr/0)",
          "(R:pRr/rRr/0)",
          "(R:0/0)"
  };
  
  public static final String[][] ngAnnotColorsWithoutCorrect = {
          {"(P:0)",           "GrayBlue"},
          {"(P:P)",           "Orange"},        // FIRST THREE LINK GROUPS
          {"(P:B)",           "Yellow"},
          {"(P:pRp)",         "Green"},
          {"(P:P/B)",         "Purple"},
          {"(P:P/pRp)",       "Pink"},
          {"(P:B/pRp)",       "PowderBlue"},
          {"(P:P/B/pRp)",     "Peach"},
          {"(P:pRr)",         "GrayBlue"},      // PURPLE NODES IN LINK GROUP 3
          {"(P:P/pRr)",       "Orange"},
          {"(P:B/pRr)",       "Yellow"},
          {"(P:pRp/pRr)",     "Green"},
          {"(P:P/B/pRr)",     "Purple"},
          {"(P:P/pRp/pRr)",   "Pink"},
          {"(P:B/pRp/pRr)",   "PowderBlue"},
          {"(P:P/B/pRp/pRr)", "Peach"},
          {"(R:pRr)",         "GrayBlue"},      // RED NODES IN LINK GROUP 5
          {"(R:rRr)",         "Orange"},
          {"(R:pRr/rRr)",     "Yellow"},
          {"(R:0)",           "Green"}
          
  };
  
  /***************************************************************************
   **
   ** HashMap based Data structure
   */
  
  public static class NodeGroupMap {
    
    private Map<NID.WithName, Set<FabricLink>> nodeToLinks_;
    private Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_;
    private Map<NID.WithName, Boolean> mergedToCorrect_, isAlignedNode_;
    private Map<GroupID, Integer> groupIDtoIndex_;
    private Map<Integer, GroupID> indexToGroupID_;
    private Map<GroupID, String> groupIDtoColor_;
    private final int numGroups_;
    
    public NodeGroupMap(BioFabricNetwork.NetworkAlignmentBuildData nabd, String[] nodeGroupOrder, String[][] colorMap) {
      this(nabd.allLinks, nabd.loneNodeIDs, nabd.mergedToCorrect, nabd.isAlignedNode, nodeGroupOrder, colorMap);
    }
    
    public NodeGroupMap(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs,
                        Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                        String[] nodeGroupOrder, String[][] colorMap) {
      this.mergedToCorrect_ = mergedToCorrect;
      this.isAlignedNode_ = isAlignedNode;
      this.numGroups_ = nodeGroupOrder.length;
      generateStructs(allLinks, loneNodeIDs);
      generateMap(nodeGroupOrder);
      generateColorMap(colorMap);
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
      groupIDtoIndex_ = new HashMap<GroupID, Integer>();
      indexToGroupID_ = new HashMap<Integer, GroupID>();
      for (int index = 0; index < nodeGroupOrder.length; index++) {
        GroupID gID = new GroupID(nodeGroupOrder[index]);
        groupIDtoIndex_.put(gID, index);
        indexToGroupID_.put(index, gID);
      }
      return;
    }
    
    private void generateColorMap(String[][] colorMap) {
      groupIDtoColor_ = new HashMap<GroupID, String>();
      
      for (String[] ngCol : colorMap) {
        GroupID groupID = new GroupID(ngCol[0]);
        String color = ngCol[1];
        groupIDtoColor_.put(groupID, color);
      }
      return;
    }
    
    /***************************************************************************
     **
     ** Return the index from the given node group ordering
     */
    
    public int getIndex(NID.WithName node) {
      GroupID groupID = generateID(node);
      if (groupIDtoIndex_.get(groupID) == null) {
        throw new IllegalArgumentException("GroupID not found in given order list");
      }
      return (groupIDtoIndex_.get(groupID));
    }
  
    /***************************************************************************
     **
     ** Return the GroupID from the given node group ordering
     */
    
    public String getKey(Integer index) {
      if (indexToGroupID_.get(index) == null) {
        throw new IllegalArgumentException("Index not found in given order list");
      }
      return (indexToGroupID_.get(index).getKey());
    }
   
    /***************************************************************************
     **
     ** Return the Annot Color for index of NG label
     */
    
    public String getColor(Integer index) {
      GroupID groupID = indexToGroupID_.get(index);
      return (groupIDtoColor_.get(groupID));
    }
    
    public int numGroups() {
      return (numGroups_);
    }
    
    /***************************************************************************
     **
     ** Hash function
     */
    
    private GroupID generateID(NID.WithName node) {
      //
      // See which types of link groups the node's links are in
      //
      
      String[] possibleRels = {NetworkAlignment.COVERED_EDGE, NetworkAlignment.GRAPH1,
              NetworkAlignment.INDUCED_GRAPH2, NetworkAlignment.HALF_UNALIGNED_GRAPH2, NetworkAlignment.FULL_UNALIGNED_GRAPH2};
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
      if (tags.isEmpty()) { // singleton
        tags.add("0");
      }
      
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      sb.append(isAlignedNode_.get(node) ? "P" : "R");  // aligned/unaligned node
      sb.append(":");
  
      for (String tag : tags) {  // link group tags
        sb.append(tag);
        sb.append("/");
      }
      
      sb.deleteCharAt(sb.length()-1);
      if (false) {
        if (mergedToCorrect_ == null) { // aligned correctly
          sb.append(0);
        } else {
          if (mergedToCorrect_.get(node) == null) { // red node
            sb.append(0);
          } else {
            sb.append((mergedToCorrect_.get(node)) ? 1 : 0);
          }
        }
      }
      
      sb.append(")");
      
      UiUtil.fixMePrintout("Manage whether to see if correctly aligned or not");
      return (new GroupID(sb.toString()));
    }
    
    private Comparator<NID.WithName> sortDecrDegree() {
      return (new Comparator<NID.WithName>() {
        @Override
        public int compare(NID.WithName node1, NID.WithName node2) {
          int diffSize = nodeToNeighbors_.get(node2).size() - nodeToNeighbors_.get(node1).size();
          return (diffSize != 0) ? diffSize : node1.getName().compareTo(node2.getName());
        }
      });
    }
    
  }
  
  /***************************************************************************
   **
   ** Hash for Hash-map data structure for node groups
   */
  
  private static class GroupID {
    
    private final String key_;
    
    public GroupID(String key) {
      this.key_ = key;
    }
    
    public String getKey() {
      return (key_);
    }
    
    @Override
    public String toString() {
      return (key_);
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o) return (true);
      if (! (o instanceof GroupID)) return (false);
      
      GroupID groupID = (GroupID) o;
      
      if (key_ != null ? (! key_.equals(groupID.key_)) : (groupID.key_ != null)) return (false);
      return (true);
    }
    
    @Override
    public int hashCode() {
      return (key_ != null ? key_.hashCode() : 0);
    }
    
  }
  
}
