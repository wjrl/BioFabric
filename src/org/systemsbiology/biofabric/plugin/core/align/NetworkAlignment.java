/*
**
**    Copyright (C) 2018 Rishi Desai
**
**    Copyright (C) 2003-2018 Institute for Systems Biology
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

package org.systemsbiology.biofabric.plugin.core.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.api.model.NetLink;
import org.systemsbiology.biofabric.api.model.NetNode;
import org.systemsbiology.biofabric.api.util.NID;
import org.systemsbiology.biofabric.api.util.UniqueLabeller;
import org.systemsbiology.biofabric.api.worker.AsynchExitRequestException;
import org.systemsbiology.biofabric.api.worker.BTProgressMonitor;
import org.systemsbiology.biofabric.api.worker.LoopReporter;
import org.systemsbiology.biofabric.plugin.PluginSupportFactory;

/****************************************************************************
 **
 ** This merges two individual graphs and an alignment to form the
 ** network alignment
 */

public class NetworkAlignment {
   
  public static final String                // Ordered as in the default link group order
          COVERED_EDGE = "P",               // Covered Edges
          GRAPH1 = "B",                     // G1 Edges w/ two aligned nodes (all non-covered G1 Edges)
          INDUCED_GRAPH2 = "pRp",           // G2 Edges w/ two aligned nodes (induced)
          HALF_UNALIGNED_GRAPH2 = "pRr",    // G2 Edges w/ one aligned node and one unaligned node
          FULL_UNALIGNED_GRAPH2 = "rRr";    // G2 Edges w/ two unaligned nodes
  
  private final String TEMPORARY = "TEMP";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  //
  // G1 is the small (#nodes) network, G2 is the large network
  //
  
  private Map<NetNode, NetNode> mapG1toG2_, perfectG1toG2_;
  private ArrayList<NetLink> linksG1_;
  private HashSet<NetNode> lonersG1_;
  private ArrayList<NetLink> linksG2_;
  private HashSet<NetNode> lonersG2_;
  private NetworkAlignmentBuildData.ViewType outType_;
  private UniqueLabeller idGen_;
  private BTProgressMonitor monitor_;
  
  //
  // largeToMergedID only contains aligned nodes
  //
  
  private Map<NetNode, NetNode> smallToMergedID_;
  private Map<NetNode, NetNode> largeToMergedID_;
  private Map<NetNode, NetNode> mergedIDToSmall_;
  
  //
  // mergedToCorrect only has aligned nodes
  //
  
  private ArrayList<NetLink> mergedLinks_;
  private Set<NetNode> mergedLoners_;
  private Map<NetNode, Boolean> mergedToCorrectNC_, isAlignedNode_;
  
  private enum Graph {SMALL, LARGE}
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public NetworkAlignment(ArrayList<NetLink> mergedLinks, Set<NetNode> mergedLoneNodeIDs,
                          Map<NetNode, NetNode> mapG1toG2, Map<NetNode, NetNode> perfectG1toG2_,
                          ArrayList<NetLink> linksG1, HashSet<NetNode> lonersG1,
                          ArrayList<NetLink> linksG2, HashSet<NetNode> lonersG2,
                          Map<NetNode, Boolean> mergedToCorrectNC, Map<NetNode, Boolean> isAlignedNode,
                          NetworkAlignmentBuildData.ViewType outType, UniqueLabeller idGen, BTProgressMonitor monitor) {
    
    this.mapG1toG2_ = mapG1toG2;
    this.perfectG1toG2_ = perfectG1toG2_;
    this.linksG1_ = linksG1;
    this.lonersG1_ = lonersG1;
    this.linksG2_ = linksG2;
    this.lonersG2_ = lonersG2;
    this.outType_ = outType;
    this.idGen_ = idGen;
    this.monitor_ = monitor;
    
    this.mergedLinks_ = mergedLinks;
    this.mergedLoners_ = mergedLoneNodeIDs;
    this.mergedToCorrectNC_ = mergedToCorrectNC;
    this.isAlignedNode_ = isAlignedNode;
  }
  
  /****************************************************************************
   **
   ** Merge the Network!
   */
  
  public void mergeNetworks() throws AsynchExitRequestException {
    
    //
    // Create merged nodes and Correctness
    //
    
    createMergedNodes();
    
    //
    // Create individual link sets; "old" refers to pre-merged networks, "new" is merged network
    //
    
    List<NetLink> newLinksG1 = new ArrayList<NetLink>();
    Set<NetNode> newLonersG1 = new HashSet<NetNode>();
    
    createNewLinkList(newLinksG1, newLonersG1, Graph.SMALL);
    
    List<NetLink> newLinksG2 = new ArrayList<NetLink>();
    Set<NetNode> newLonersG2 = new HashSet<NetNode>();
    
    createNewLinkList(newLinksG2, newLonersG2, Graph.LARGE);
    
    //
    // Give each link its respective link relation
    //
    
    createMergedLinkList(newLinksG1, newLinksG2);
    
    finalizeLoneNodeIDs(newLonersG1, newLonersG2);
    
    //
    // POST processing
    //
    
    createIsAlignedMap();
    
    //
    // Orphan Edges: All unaligned edges; plus all of their endpoint nodes' edges
    //
    
    if (outType_ == NetworkAlignmentBuildData.ViewType.ORPHAN) {
      (new OrphanEdgeLayout()).process(mergedLinks_, mergedLoners_, monitor_);
    }
    
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /****************************************************************************
   **
   ** Create merged nodes, install into maps
   */
  
  private void createMergedNodes() {
    
    smallToMergedID_ = new TreeMap<NetNode, NetNode>();
    largeToMergedID_ = new TreeMap<NetNode, NetNode>();
    mergedIDToSmall_ = new TreeMap<NetNode, NetNode>();
    
    boolean doingPerfectGroup = (outType_ == NetworkAlignmentBuildData.ViewType.GROUP) &&
                                (perfectG1toG2_ != null);
     
    for (Map.Entry<NetNode, NetNode> entry : mapG1toG2_.entrySet()) {
      
      NetNode smallNode = entry.getKey(), largeNode = entry.getValue();
      String smallName = smallNode.getName(), largeName = largeNode.getName();
      
      //
      // Aligned nodes merge name in the form small::large
      //
      
      String mergedName = String.format("%s::%s", smallName, largeName);
      
      NID nid = idGen_.getNextOID();
      NetNode merged_node = PluginSupportFactory.buildNode(nid, mergedName);
      
      smallToMergedID_.put(smallNode, merged_node);
      largeToMergedID_.put(largeNode, merged_node);
      mergedIDToSmall_.put(merged_node, smallNode);
      
      //
      // Nodes are correctly aligned map
      //
      
      if (doingPerfectGroup) { // perfect alignment must be provided
        NetNode perfectLarge = perfectG1toG2_.get(smallNode);
        boolean alignedCorrect = perfectLarge.equals(largeNode);
        mergedToCorrectNC_.put(merged_node, alignedCorrect);
      }
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Create new link lists based on merged nodes for both networks
   */
  
  private void createNewLinkList(List<NetLink> newLinks, Set<NetNode> newLoners, Graph graph)
          throws AsynchExitRequestException {
    
    List<NetLink> oldLinks;
    Set<NetNode> oldLoners;
    Map<NetNode, NetNode> oldToNewID;
    String msg;
    
    switch (graph) {
      case SMALL:
        oldLinks = linksG1_;
        oldLoners = lonersG1_;
        oldToNewID = smallToMergedID_;
        msg = "progress.mergingSmallLinks";
        break;
      case LARGE:
        oldLinks = linksG2_;
        oldLoners = lonersG2_;
        oldToNewID = largeToMergedID_;
        msg = "progress.mergingLargeLinks";
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    LoopReporter lr = new LoopReporter(oldLinks.size(), 20, monitor_, 0.0, 1.0, msg);
    Set<NetLink> newLinkSet = new HashSet<NetLink>();
    
    for (NetLink oldLink : oldLinks) {
      
      NetNode oldA = oldLink.getSrcNode();
      NetNode oldB = oldLink.getTrgNode();
      
      //
      // Not all nodes are mapped in the large graph
      //
      
      NetNode newA = (oldToNewID.containsKey(oldA)) ? oldToNewID.get(oldA) : oldA;
      NetNode newB = (oldToNewID.containsKey(oldB)) ? oldToNewID.get(oldB) : oldB;
      
      NetLink newLink = PluginSupportFactory.buildLink(newA, newB, TEMPORARY, false, Boolean.valueOf(false));
      // 'directed' must be false
      newLinkSet.add(newLink);
      lr.report();
    }
    newLinks.addAll(newLinkSet);
    lr.finish();
    
    for (NetNode oldLoner : oldLoners) {
      
      NetNode newLoner = (oldToNewID.containsKey(oldLoner)) ? oldToNewID.get(oldLoner) : oldLoner;
      
      newLoners.add(newLoner);
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Combine the two link lists into one, with G2,CC,G1 tags accordingly
   */
  
  private void createMergedLinkList(List<NetLink> newLinksG1, List<NetLink> newLinksG2)
          throws AsynchExitRequestException {
    
    LoopReporter lr = new LoopReporter(newLinksG2.size(), 20, monitor_, 0.0, 1.0, "progress.separatingLinksA");

    NetAlignFabricLinkLocator comp = new NetAlignFabricLinkLocator();
    sortLinks(newLinksG1);
    
    SortedSet<NetNode> alignedNodesG2 = new TreeSet<NetNode>(largeToMergedID_.values());
    // contains all aligned nodes; contains() works in O(log(n))
  
    for (NetLink linkG2 : newLinksG2) {
      
      int index = Collections.binarySearch(newLinksG1, linkG2, comp);
      
      NetNode src = linkG2.getSrcNode(), trg = linkG2.getTrgNode();
      
      if (index >= 0) {
        addMergedLink(src, trg, COVERED_EDGE);
      } else {
        boolean containsSRC = alignedNodesG2.contains(src), containsTRG = alignedNodesG2.contains(trg);
        if (containsSRC && containsTRG) {
          addMergedLink(src, trg, INDUCED_GRAPH2);
        } else if (containsSRC || containsTRG) {
          addMergedLink(src, trg, HALF_UNALIGNED_GRAPH2);
        } else {
          addMergedLink(src, trg, FULL_UNALIGNED_GRAPH2);
        }
      }
      lr.report();
    }
    lr = new LoopReporter(newLinksG1.size(), 20, monitor_, 0.0, 1.0, "progress.separatingLinksB");
    sortLinks(newLinksG2);
  
    for (NetLink linkG1 : newLinksG1) {
      
      int index = Collections.binarySearch(newLinksG2, linkG1, comp);
      
      if (index < 0) {
        addMergedLink(linkG1.getSrcNode(), linkG1.getTrgNode(), GRAPH1);
      }
      lr.report();
    }
    return; // This method is not ideal. . . but shall stay (6/19/18)
  }
  
  /****************************************************************************
   **
   ** Add both non-shadow and shadow links to merged link-list
   */
  
  private void addMergedLink(NetNode src, NetNode trg, String tag) {
    NetLink newMergedLink = PluginSupportFactory.buildLink(src, trg, tag, false);
    mergedLinks_.add(newMergedLink);
    
    // We never create shadow feedback links!
    if (!src.equals(trg)) {
      NetLink newMergedLinkShadow = PluginSupportFactory.buildLink(src, trg, tag, true);
      mergedLinks_.add(newMergedLinkShadow);
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Combine loneNodeIDs lists into one
   */
  
  private void finalizeLoneNodeIDs(Set<NetNode> newLonersG1, Set<NetNode> newLonersG2) {
    mergedLoners_.addAll(newLonersG1);
    mergedLoners_.addAll(newLonersG2);
    return;
  }
  
  /****************************************************************************
   **
   ** POST processing: Create isAlignedNode map
   */
  
  private void createIsAlignedMap() throws AsynchExitRequestException {
  
    Set<NetNode> allNodes = PluginSupportFactory.getBuildExtractor().extractNodes(mergedLinks_, mergedLoners_, monitor_);
    for (NetNode node : allNodes) {
      // here mergedIDToSmall_ is a tool: if node is in it, we know it is an aligned node
      if (mergedIDToSmall_.get(node) != null) {
        isAlignedNode_.put(node, true);
      } else {
        isAlignedNode_.put(node, false);
      }
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Sort list of FabricLinks
   */
  
  private void sortLinks(List<NetLink> newLinks) throws AsynchExitRequestException {
    NetAlignFabricLinkLocator comp = new NetAlignFabricLinkLocator();
    Set<NetLink> sorted = new TreeSet<NetLink>(comp);
    LoopReporter lr = new LoopReporter(newLinks.size(), 20, monitor_, 0.0, 1.0, "progress.sortingLinks");
  
    for (NetLink link : newLinks) {
      sorted.add(link);
      lr.report();
    }
    newLinks.clear();
    newLinks.addAll(sorted);  // assume to have no loop reporter here
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /****************************************************************************
   **
   ** All unaligned edges plus all of their endpoint nodes' edges
   */
  
  private static class OrphanEdgeLayout {
    
    public OrphanEdgeLayout() {
    }
    
    private void process(List<NetLink> mergedLinks, Set<NetNode> mergedLoneNodeIDs,
                         BTProgressMonitor monitor)
            throws AsynchExitRequestException {
      
      LoopReporter reporter = new LoopReporter(mergedLinks.size(), 20, monitor, 0.0, 1.0,
              "progress.findingOrphanEdges");
      
      Set<NetNode> blueNodesG1 = new TreeSet<NetNode>();
      for (NetLink link : mergedLinks) { // find the nodes of interest
        if (link.getRelation().equals(GRAPH1)) {
          blueNodesG1.add(link.getSrcNode()); // it's a set - so with shadows no duplicates
          blueNodesG1.add(link.getTrgNode());
        }
        reporter.report();
      }
      
      reporter = new LoopReporter(mergedLinks.size(), 20, monitor, 0.0, 1.0,
              "progress.orphanEdgesContext");
      
      List<NetLink> blueEdgesPlusContext = new ArrayList<NetLink>();
      for (NetLink link : mergedLinks) { // add the edges connecting to the nodes of interest (one hop away)
        
        NetNode src = link.getSrcNode(), trg = link.getTrgNode();
        
        if (blueNodesG1.contains(src) || blueNodesG1.contains(trg)) {
          blueEdgesPlusContext.add(link);
        }
        reporter.report();
      }
  
      mergedLinks.clear();
      mergedLoneNodeIDs.clear();
      mergedLinks.addAll(blueEdgesPlusContext);
      return;
    }
    
  }
  
  /***************************************************************************
   **
   ** Used ONLY to order links for creating the merged link set in Network Alignments
   */
  
  private static class NetAlignFabricLinkLocator implements Comparator<NetLink> {
    
    /***************************************************************************
     **
     ** For any different links in the two separate network link sets, this
     ** says which comes first
     */
    
    public int compare(NetLink link1, NetLink link2) {
      
      if (link1.synonymous(link2)) {
        return (0);
      }
      
      //
      // Must sort the node names because A-B must be equivalent to B-A
      //
      
      String[] arr1 = {link1.getSrcNode().getName(), link1.getTrgNode().getName()};
      Arrays.sort(arr1);
      
      String[] arr2 = {link2.getSrcNode().getName(), link2.getTrgNode().getName()};
      Arrays.sort(arr2);
      
      String concat1 = String.format("%s___%s", arr1[0], arr1[1]);
      String concat2 = String.format("%s___%s", arr2[0], arr2[1]);
      
      //
      // This cuts the merge-lists algorithm to O(eloge) because binary search
      //
      
      return concat1.compareTo(concat2);
    }
  }
  
}
