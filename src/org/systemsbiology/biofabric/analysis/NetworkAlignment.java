/*
**
**    File created by Rishi Desai
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

package org.systemsbiology.biofabric.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;

import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
 **
 ** This merges two individual graphs and an alignment to form the
 ** network alignment
 */

public class NetworkAlignment {
  
  public static final String                // Ordered as in the default link group order
          COVERED_EDGE = "G12",             // Covered Edges
          GRAPH1 = "G1A",                   // G1 Edges w/ two aligned nodes (all non-covered G1 Edges)
          INDUCED_GRAPH2 = "G2A",           // G2 Edges w/ two aligned nodes (induced)
          HALF_UNALIGNED_GRAPH2 = "G2B",    // G2 Edges w/ one aligned node and one unaligned node
          FULL_UNALIGNED_GRAPH2 = "G2C";    // G2 Edges w/ two unaligned nodes
  
  private final String TEMPORARY = "TEMP";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  //
  // G1 is the small (#nodes) network, G2 is the large network
  //
  
  private Map<NID.WithName, NID.WithName> mapG1toG2_, perfectG1toG2_;
  private ArrayList<FabricLink> linksG1_;
  private HashSet<NID.WithName> lonersG1_;
  private ArrayList<FabricLink> linksG2_;
  private HashSet<NID.WithName> lonersG2_;
  private boolean forOrphanEdges;
  private UniqueLabeller idGen_;
  private BTProgressMonitor monitor_;
  
  //
  // largeToMergedID only contains aligned nodes
  //
  
  private Map<NID.WithName, NID.WithName> smallToMergedID_;
  private Map<NID.WithName, NID.WithName> largeToMergedID_;
  private Map<NID.WithName, NID.WithName> mergedIDToSmall_;
  
  //
  // mergedToCorrect only has aligned nodes
  //
  
  private ArrayList<FabricLink> mergedLinks_;
  private Set<NID.WithName> mergedLoners_;
  private Map<NID.WithName, Boolean> mergedToCorrect_, isAlignedNode_;
  
  private enum Graph {SMALL, LARGE}
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public NetworkAlignment(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
                          Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2_,
                          ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                          ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                          Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                          boolean forOrphanEdges, UniqueLabeller idGen, BTProgressMonitor monitor) {
    
    this.mapG1toG2_ = mapG1toG2;
    this.perfectG1toG2_ = perfectG1toG2_;
    this.linksG1_ = linksG1;
    this.lonersG1_ = lonersG1;
    this.linksG2_ = linksG2;
    this.lonersG2_ = lonersG2;
    this.forOrphanEdges = forOrphanEdges;
    this.idGen_ = idGen;
    this.monitor_ = monitor;
    
    this.mergedLinks_ = mergedLinks;
    this.mergedLoners_ = mergedLoneNodeIDs;
    this.mergedToCorrect_ = mergedToCorrect;
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
    
    List<FabricLink> newLinksG1 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG1 = new HashSet<NID.WithName>();
    
    createNewLinkList(newLinksG1, newLonersG1, Graph.SMALL);
    
    List<FabricLink> newLinksG2 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG2 = new HashSet<NID.WithName>();
    
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
    
    if (forOrphanEdges) {
      (new OrphanEdgeLayout()).process(mergedLinks_, mergedLoners_, mergedIDToSmall_, monitor_);
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
    
    smallToMergedID_ = new TreeMap<NID.WithName, NID.WithName>();
    largeToMergedID_ = new TreeMap<NID.WithName, NID.WithName>();
    mergedIDToSmall_ = new TreeMap<NID.WithName, NID.WithName>();
    
    for (Map.Entry<NID.WithName, NID.WithName> entry : mapG1toG2_.entrySet()) {
      
      NID.WithName smallNode = entry.getKey(), largeNode = entry.getValue();
      String smallName = smallNode.getName(), largeName = largeNode.getName();
      
      //
      // Aligned nodes merge name in the form large-small
      //
      
      String mergedName = String.format("%s-%s", largeName, smallName);
      
      NID nid = idGen_.getNextOID();
      NID.WithName merged_node = new NID.WithName(nid, mergedName);
      
      smallToMergedID_.put(smallNode, merged_node);
      largeToMergedID_.put(largeNode, merged_node);
      mergedIDToSmall_.put(merged_node, smallNode);
      
      //
      // Nodes are correctly aligned map
      //
      
      if (perfectG1toG2_ != null) { // perfect alignment must be provided
        NID.WithName perfectLarge = perfectG1toG2_.get(smallNode);
        boolean alignedCorrect = perfectLarge.equals(largeNode);
        mergedToCorrect_.put(merged_node, alignedCorrect);
      }
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Create new link lists based on merged nodes for both networks
   */
  
  private void createNewLinkList(List<FabricLink> newLinks, Set<NID.WithName> newLoners, Graph graph)
          throws AsynchExitRequestException {
    
    List<FabricLink> oldLinks;
    Set<NID.WithName> oldLoners;
    Map<NID.WithName, NID.WithName> oldToNewID;
    
    switch (graph) {
      case SMALL:
        oldLinks = linksG1_;
        oldLoners = lonersG1_;
        oldToNewID = smallToMergedID_;
        break;
      case LARGE:
        oldLinks = linksG2_;
        oldLoners = lonersG2_;
        oldToNewID = largeToMergedID_;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    LoopReporter lr = new LoopReporter(oldLinks.size(), 20, monitor_, 0.0, 1.0, "progress.mergingLinks");
    
    for (FabricLink oldLink : oldLinks) {
      
      NID.WithName oldA = oldLink.getSrcID();
      NID.WithName oldB = oldLink.getTrgID();
      
      //
      // Not all nodes are mapped in the large graph
      //
      
      NID.WithName newA = (oldToNewID.containsKey(oldA)) ? oldToNewID.get(oldA) : oldA;
      NID.WithName newB = (oldToNewID.containsKey(oldB)) ? oldToNewID.get(oldB) : oldB;
      
      FabricLink newLink = new FabricLink(newA, newB, TEMPORARY, false, false);
      // 'directed' must be false
      
      newLinks.add(newLink);
      lr.report();
    }
    
    lr.finish();
    
    for (NID.WithName oldLoner : oldLoners) {
      
      NID.WithName newLoner = (oldToNewID.containsKey(oldLoner)) ? oldToNewID.get(oldLoner) : oldLoner;
      
      newLoners.add(newLoner);
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Combine the two link lists into one, with G2,CC,G1 tags accordingly
   */
  
  private void createMergedLinkList(List<FabricLink> newLinksG1, List<FabricLink> newLinksG2)
          throws AsynchExitRequestException {
    
    long totalSize = newLinksG2.size() + newLinksG2.size();
    LoopReporter lr = new LoopReporter(totalSize, 20, monitor_, 0.0, 1.0, "progress.separatingLinks");

    NetAlignFabricLinkLocator comp = new NetAlignFabricLinkLocator();
    Collections.sort(newLinksG1, comp);
    
    for (FabricLink linkG2 : newLinksG2) {
      
      int index = Collections.binarySearch(newLinksG1, linkG2, comp);
      
      NID.WithName src = linkG2.getSrcID(), trg = linkG2.getTrgID();
      
      if (index >= 0) {
        addMergedLink(src, trg, COVERED_EDGE);
      } else {
        // contains all alinged nodes; contains() works in O(log(n))
        SortedSet<NID.WithName> alignedNodesG2 = new TreeSet<NID.WithName>(largeToMergedID_.values());
        
        if (alignedNodesG2.contains(src) && alignedNodesG2.contains(trg)) {
          addMergedLink(src, trg, INDUCED_GRAPH2);
        } else if (alignedNodesG2.contains(src) || alignedNodesG2.contains(trg)) {
          addMergedLink(src, trg, HALF_UNALIGNED_GRAPH2);
        } else {
          addMergedLink(src, trg, FULL_UNALIGNED_GRAPH2);
        }
      }
      lr.report();
    }
    
    Collections.sort(newLinksG2, comp);
    
    for (FabricLink linkG1 : newLinksG1) {
      
      int index = Collections.binarySearch(newLinksG2, linkG1, comp);
      
      if (index < 0) {
        addMergedLink(linkG1.getSrcID(), linkG1.getTrgID(), GRAPH1);
      }
      lr.report();
    }
    lr.finish();
    return; // This method is not ideal. . .
  }
  
  /****************************************************************************
   **
   ** Add both non-shadow and shadow links to merged link-list
   */
  
  private void addMergedLink(NID.WithName src, NID.WithName trg, String tag) {
    FabricLink newMergedLink = new FabricLink(src, trg, tag, false, null);
    FabricLink newMergedLinkShadow = new FabricLink(src, trg, tag, true, null);
    
    mergedLinks_.add(newMergedLink);
    mergedLinks_.add(newMergedLinkShadow);
  }
  
  /****************************************************************************
   **
   ** Combine loneNodeIDs lists into one
   */
  
  private void finalizeLoneNodeIDs(Set<NID.WithName> newLonersG1, Set<NID.WithName> newLonersG2) {
    mergedLoners_.addAll(newLonersG1);
    mergedLoners_.addAll(newLonersG2);
    return;
  }
  
  /****************************************************************************
   **
   ** POST processing: Create isAlignedNode map
   */
  
  private void createIsAlignedMap() throws AsynchExitRequestException {
  
    Set<NID.WithName> allNodes = BioFabricNetwork.extractNodes(mergedLinks_, mergedLoners_, monitor_);
    for (NID.WithName node : allNodes) {
      // here mergedIDToSmall_ is a tool: if node is in it, we know it is an aligned node
      if (mergedIDToSmall_.get(node) != null) {
        isAlignedNode_.put(node, true);
      } else {
        isAlignedNode_.put(node, false);
      }
    }
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
    
    private void process(List<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
                         Map<NID.WithName, NID.WithName> mergedIDToSmall, BTProgressMonitor monitor)
            throws AsynchExitRequestException {
      
      Set<NID.WithName> unalignedNodesG1 = new TreeSet<NID.WithName>();
      for (FabricLink link : mergedLinks) { // find the nodes of interest
        if (link.getRelation().equals(GRAPH1)) {
          unalignedNodesG1.add(link.getSrcID()); // it's a set- so with shadows no duplicates
          unalignedNodesG1.add(link.getTrgID());
        }
      }
      
      List<FabricLink> blueEdgesPlusContext = new ArrayList<FabricLink>();
      for (FabricLink link : mergedLinks) { // add the edges connecting to the nodes of interest (one hop away)
        
        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
        
        if (unalignedNodesG1.contains(src) || unalignedNodesG1.contains(trg)) {
          blueEdgesPlusContext.add(link);
        }
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
  
  private static class NetAlignFabricLinkLocator implements Comparator<FabricLink> {
    
    /***************************************************************************
     **
     ** For any different links in the two separate network link sets, this
     ** says which comes first
     */
    
    public int compare(FabricLink link1, FabricLink link2) {
      
      if (link1.synonymous(link2)) {
        return (0);
      }
      
      //
      // Must sort the node names because A-B must be equivalent to B-A
      //
      
      String[] arr1 = {link1.getSrcID().getName(), link1.getTrgID().getName()};
      Arrays.sort(arr1);
      
      String[] arr2 = {link2.getSrcID().getName(), link2.getTrgID().getName()};
      Arrays.sort(arr2);
      
      String concat1 = String.format("%s___%s", arr1[0], arr1[1]);
      String concat2 = String.format("%s___%s", arr2[0], arr2[1]);
      
      //
      // Meant to be temporary (7/16/17) but will stay (1/27/18)
      // It cuts the merge lists algorithm to O(nlogn) because I can use
      // binary search
      //
      
      return concat1.compareTo(concat2);
    }
  }
  
}

//  private void process(List<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
//                       Map<NID.WithName, NID.WithName> mergedIDToSmall, BTProgressMonitor monitor)
//          throws AsynchExitRequestException {
//
//    List<FabricLink> nonShdwMergedLinks = new ArrayList<FabricLink>();
//    for (FabricLink link : mergedLinks) {
//      if (! link.isShadow()) {
//        nonShdwMergedLinks.add(link);
//      }
//    }
//
//    Set<NID.WithName> unalignedNodesG1 = new TreeSet<NID.WithName>();
//    for (FabricLink link : nonShdwMergedLinks) { // find the nodes of interest
//      if (link.getRelation().equals(GRAPH1)) {
//        unalignedNodesG1.add(link.getSrcID());
//        unalignedNodesG1.add(link.getTrgID());
//      }
//    }
//
//    List<FabricLink> unalignedEdgesG1 = new ArrayList<FabricLink>();
//    for (FabricLink link : nonShdwMergedLinks) { // add the edges connecting to the nodes of interest (one hop away)
//
//      NID.WithName src = link.getSrcID(), trg = link.getTrgID();
//
//      if (unalignedNodesG1.contains(src) || unalignedNodesG1.contains(trg)) {
//        unalignedEdgesG1.add(link);
//      }
//    }
//
//    //
//    // Go back to old G1 names
//    //
//
//    List<FabricLink> oldUnalignedEdgesG1 = new ArrayList<FabricLink>();
//    for (FabricLink link : unalignedEdgesG1) {
//
//      NID.WithName srcNew = link.getSrcID(), trgNew = link.getTrgID();
//      NID.WithName srcOld = mergedIDToSmall.get(srcNew), trgOld = mergedIDToSmall.get(trgNew);
//
//      if (srcOld == null) { // this is an unaligned node so it stays the same
//        srcOld = srcNew;
//      }
//      if (trgOld == null) { // same here
//        trgOld = trgNew;
//      }
//
//      FabricLink linkOldName = new FabricLink(srcOld, trgOld, GRAPH1, false, null);
//      FabricLink linkOldNameShdw = new FabricLink(srcOld, trgOld, GRAPH1, true, null);
//
//      oldUnalignedEdgesG1.add(linkOldName);
//      oldUnalignedEdgesG1.add(linkOldNameShdw);
//    }
//
//    //
//    // Change the final link-lists
//    //
//
//    mergedLinks.clear();
//    mergedLoneNodeIDs.clear();
//    mergedLinks.addAll(oldUnalignedEdgesG1);
//
//    return;
//  }

