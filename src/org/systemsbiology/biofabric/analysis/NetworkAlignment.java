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

package org.systemsbiology.biofabric.analysis;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UniqueLabeller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/****************************************************************************
 **
 ** This merges two individual graphs and an alignment to form the
 ** network alignment
 */

public class NetworkAlignment {
  
  private final String TAG_G1 = "G1", TAG_CC = "CC", TAG_G2 = "G2";
  private final String TEMP_TAG = "TEMP";
  
  private Map<NID.WithName, NID.WithName> mapG1toG2_;
  private ArrayList<FabricLink> linksG1_;
  private HashSet<NID.WithName> lonersG1_;
  private ArrayList<FabricLink> linksG2_;
  private HashSet<NID.WithName> lonersG2_;
  private UniqueLabeller idGen_;
  
  private Map<NID.WithName, NID.WithName> smallToNA_;
  private Map<NID.WithName, NID.WithName> largeToNA_;
  
  private Set<FabricLink> reducedLinks_;
  private Set<NID.WithName> reducedLoners_;
  
  private enum Graph {SMALL, LARGE}
  
  public NetworkAlignment(Set<FabricLink> reducedLinks, Set<NID.WithName> reducedLoners,
                          Map<NID.WithName, NID.WithName> mapG1toG2,
                          ArrayList<FabricLink> linksG1, HashSet<NID.WithName> lonersG1,
                          ArrayList<FabricLink> linksG2, HashSet<NID.WithName> lonersG2,
                          UniqueLabeller idGen) {
    
    this.mapG1toG2_ = mapG1toG2;
    this.linksG1_ = linksG1;
    this.lonersG1_ = lonersG1;
    this.linksG2_ = linksG2;
    this.lonersG2_ = lonersG2;
    this.idGen_ = idGen;
  
    this.reducedLinks_ = reducedLinks;
    this.reducedLoners_ = reducedLoners;
  }
  
  /****************************************************************************
   **
   ** Merge the Network!
   */
  
  public void mergeNetworks() {
    
    //
    // Create merged nodes
    //
    
    createMergedNodes();
    
    //
    // Create individual link sets
    //
    
    List<FabricLink> newLinksG1 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG1 = new HashSet<NID.WithName>();
    
    createNewLinkSet(newLinksG1, newLonersG1, Graph.SMALL);
  
    List<FabricLink> newLinksG2 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG2 = new HashSet<NID.WithName>();
    
    createNewLinkSet(newLinksG2, newLonersG2, Graph.LARGE);
    
    //
    // Split individual sets into G1 only, CC only, G2 only
    //
    
    splitLinkSets(newLinksG1, newLinksG2);
    
    finalizeLoners(newLonersG1, newLonersG2);
    
    return;
  }
  
  private void createMergedNodes() {
  
    smallToNA_ = new TreeMap<NID.WithName, NID.WithName>();
    largeToNA_ = new TreeMap<NID.WithName, NID.WithName>();
  
    for (Map.Entry<NID.WithName, NID.WithName> entry : mapG1toG2_.entrySet()) {
    
      NID.WithName smallNode = entry.getKey(), largeNode = entry.getValue();
      String smallName = smallNode.getName(), largeName = largeNode.getName();
    
      String merged_name = String.format("%s-%s", smallName, largeName);
    
      NID nid = idGen_.getNextOID();
      NID.WithName merged_node = new NID.WithName(nid, merged_name);
    
      smallToNA_.put(smallNode, merged_node);
      largeToNA_.put(largeNode, merged_node);
    }
    return;
  }
  
  private void createNewLinkSet(List<FabricLink> newLinks, Set<NID.WithName> newLoners, Graph graph) {
    
    List<FabricLink> oldLinks;
    Set<NID.WithName> oldLoners;
    Map<NID.WithName, NID.WithName> oldToNewID;
    
    switch (graph) {
      case SMALL:
        oldLinks = linksG1_;
        oldLoners = lonersG1_;
        oldToNewID = smallToNA_;
        break;
      case LARGE:
        oldLinks = linksG2_;
        oldLoners = lonersG2_;
        oldToNewID = largeToNA_;
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    for (FabricLink oldLink : oldLinks) {
      
      NID.WithName oldA = oldLink.getSrcID();
      NID.WithName oldB = oldLink.getTrgID();
      
      //
      // Not all nodes are mapped in the large graph
      //
      
      NID.WithName newA = (oldToNewID.containsKey(oldA)) ? oldToNewID.get(oldA) : oldA;
      NID.WithName newB = (oldToNewID.containsKey(oldB)) ? oldToNewID.get(oldB) : oldB;

      FabricLink newLink = new FabricLink(newA, newB, TEMP_TAG, false, false);
      
      newLinks.add(newLink);
    }
    
    for (NID.WithName oldLoner : oldLoners) {
      
      NID.WithName newLoner = (oldToNewID.containsKey(oldLoner)) ? oldToNewID.get(oldLoner) : oldLoner;
      
      newLoners.add(newLoner);
    }
    return;
  }
  
  private void splitLinkSets(List<FabricLink> newLinksG1, List<FabricLink> newLinksG2) {
  
    Collections.sort(newLinksG1, new NetAlignFabricLinkLocator());
  
    for (FabricLink linkG2 : newLinksG2) {
    
      int index = Collections.binarySearch(newLinksG1, linkG2, new NetAlignFabricLinkLocator());
      
      FabricLink newFinalLink;
      if (index >= 0) {
        newFinalLink = new FabricLink(linkG2.getSrcID(), linkG2.getTrgID(), TAG_CC, false, false);
        // Ideally, I would remove this link from newLinksG1, but that would make this O(e*n), right now it's O(eloge)
      } else {
        newFinalLink = new FabricLink(linkG2.getSrcID(), linkG2.getTrgID(), TAG_G2, false, false);
      }
      reducedLinks_.add(newFinalLink);
    }
  
    Collections.sort(newLinksG2, new NetAlignFabricLinkLocator());
  
    for (FabricLink linkG1 : newLinksG1) {

      int index = Collections.binarySearch(newLinksG2, linkG1, new NetAlignFabricLinkLocator());
    
      if (index < 0) {
        FabricLink newFinalLink = new FabricLink(linkG1.getSrcID(), linkG1.getTrgID(), TAG_G1, false, false);
        reducedLinks_.add(newFinalLink);
      }
    }
    return; // This method is not ideal. . .
  }
  
  private void finalizeLoners(Set<NID.WithName> newLonersG1, Set<NID.WithName> newLonersG2) {
    
    reducedLoners_.addAll(newLonersG1);
    reducedLoners_.addAll(newLonersG2);
    // any merged nodes in both sets - only one copy will stay (it's a set)
  }
  
  /***************************************************************************
   **
   ** Used ONLY to order links for creating the merged link set in Network Alignments
   */
  
  private class NetAlignFabricLinkLocator implements Comparator<FabricLink> {
  
    /***************************************************************************
     **
     ** For any different links in the two separate network link sets, this
     ** says which comes first
     */
    
    public int compare(FabricLink link1, FabricLink link2) {
      
      if (link1.synonymous(link2)) {
        return (0);
      }
      
      String[] arr1 = {link1.getSrcID().getName(), link1.getTrgID().getName()};
      Arrays.sort(arr1);
      
      String[] arr2 = {link2.getSrcID().getName(), link2.getTrgID().getName()};
      Arrays.sort(arr2);
  
      String concat1 = arr1[0] + "xxx" + arr1[1];
      String concat2 = arr2[0] + "xxx" + arr2[1];

      // THIS IS COMPLETELY TEMPORARY - RISHI DESAI 7/16/17
      
      return concat1.compareTo(concat2);
    }
  }
  
}

// The previous way I created the final link set
//
//  private static void createEdgeGroups(GraphNA small, GraphNA large, Map<NodeNA, NID.WithName> NAtoNID,
//                                       Set<FabricLink> G1_edges, Set<FabricLink> CC_edges, Set<FabricLink> G2_edges) {
//
//    // Go through small (place edges in G1 or CC),
//    // then through G2 (place edges in G2, CC is already filled)
//
//    for (Map.Entry<NodeNA, Set<NodeNA>> entry : small.edges_.entrySet()) {
//
//      NodeNA A_G1 = entry.getKey();
//      for (NodeNA B_G1 : entry.getValue()) {
//
//        if (large.edges_.get(A_G1).contains(B_G1)) { // check if it's in CC
//          FabricLink fl = new FabricLink(NAtoNID.get(A_G1), NAtoNID.get(B_G1), TAG_CC, false, false);
//          CC_edges.add(fl);
//        } else {
//          FabricLink fl = new FabricLink(NAtoNID.get(A_G1), NAtoNID.get(B_G1), TAG_G1, false, false);
//          G1_edges.add(fl);
//        }
//      }
//    }
//
//    for (Map.Entry<NodeNA, Set<NodeNA>> entry : large.edges_.entrySet()) {
//
//      NodeNA A_G1 = entry.getKey();
//      for (NodeNA B_G1 : entry.getValue()) {
//
//        if (! small.edges_.keySet().contains(A_G1) ||
//                ! small.edges_.get(A_G1).contains(B_G1)) { // not all nodes in G2 are aligned
//
//          FabricLink fl = new FabricLink(NAtoNID.get(A_G1), NAtoNID.get(B_G1), TAG_G2, false, false);
//          G2_edges.add(fl);
//        }
//      }
//    }
//
//  }
