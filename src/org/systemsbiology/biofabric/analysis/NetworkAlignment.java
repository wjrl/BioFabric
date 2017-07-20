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
  
  private ArrayList<FabricLink> mergedLinks_;
  private Set<NID.WithName> mergedLoners_;
  
  private enum Graph {SMALL, LARGE}
  
  public NetworkAlignment(ArrayList<FabricLink> mergedLinks, Set<NID.WithName> mergedLoneNodeIDs,
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
  
    this.mergedLinks_ = mergedLinks;
    this.mergedLoners_ = mergedLoneNodeIDs;
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
    // Create individual link sets; "old" refers to pre-merged networks, "new" is merged network
    //
    
    List<FabricLink> newLinksG1 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG1 = new HashSet<NID.WithName>();
    
    createNewLinkList(newLinksG1, newLonersG1, Graph.SMALL);
  
    List<FabricLink> newLinksG2 = new ArrayList<FabricLink>();
    Set<NID.WithName> newLonersG2 = new HashSet<NID.WithName>();
    
    createNewLinkList(newLinksG2, newLonersG2, Graph.LARGE);
    
    //
    // Split individual link-lists into G1 only, CC only, G2 only -> combine into one list
    //
    
    createMergedLinkList(newLinksG1, newLinksG2);
    
    finalizeLoneNodeIDs(newLonersG1, newLonersG2);
    
    createShadowLinks();
    
    return;
  }
  
  /****************************************************************************
   **
   ** Create merged nodes, install into maps
   */
  
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
  
  /****************************************************************************
   **
   ** Create new link lists based on merged nodes for both networks
   */
  
  private void createNewLinkList(List<FabricLink> newLinks, Set<NID.WithName> newLoners, Graph graph) {
    
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
      // 'directed' must be false
      
      newLinks.add(newLink);
    }
    
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
  
  private void createMergedLinkList(List<FabricLink> newLinksG1, List<FabricLink> newLinksG2) {
  
    Collections.sort(newLinksG1, new NetAlignFabricLinkLocator());
  
    for (FabricLink linkG2 : newLinksG2) {
    
      int index = Collections.binarySearch(newLinksG1, linkG2, new NetAlignFabricLinkLocator());
      
      FabricLink newFinalLink;
      if (index >= 0) {
        newFinalLink = new FabricLink(linkG2.getSrcID(), linkG2.getTrgID(), TAG_CC, false, null);
        // Ideally, I would remove this link from newLinksG1, but that would make this O(e*n), right now it's O(eloge)
      } else {
        newFinalLink = new FabricLink(linkG2.getSrcID(), linkG2.getTrgID(), TAG_G2, false, null);
      }
      mergedLinks_.add(newFinalLink);
    }
  
    Collections.sort(newLinksG2, new NetAlignFabricLinkLocator());
  
    for (FabricLink linkG1 : newLinksG1) {

      int index = Collections.binarySearch(newLinksG2, linkG1, new NetAlignFabricLinkLocator());
    
      if (index < 0) {
        FabricLink newFinalLink = new FabricLink(linkG1.getSrcID(), linkG1.getTrgID(), TAG_G1, false, null);
        mergedLinks_.add(newFinalLink);
      }
    }
    return; // This method is not ideal. . .
  }
  
  /****************************************************************************
   **
   ** Create an equivalent shadow link for each link in the merged link list
   */
  
  private void createShadowLinks() {
    
    ArrayList<FabricLink> shadows = new ArrayList<FabricLink>();
    for (FabricLink link : mergedLinks_) {
      
      FabricLink shadow = new FabricLink(link.getSrcID(), link.getTrgID(), link.getRelation(), true, null);
      shadows.add(shadow);
    }
    mergedLinks_.addAll(shadows);
    return;
  }
  
  /****************************************************************************
   **
   ** Combine loneNodeIDs lists into one
   */
  
  private void finalizeLoneNodeIDs(Set<NID.WithName> newLonersG1, Set<NID.WithName> newLonersG2) {
    
    mergedLoners_.addAll(newLonersG1);
    mergedLoners_.addAll(newLonersG2);
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
      
      //
      // Must sort the node names because A-B must be equivalent to B-A
      //
      
      String[] arr1 = {link1.getSrcID().getName(), link1.getTrgID().getName()};
      Arrays.sort(arr1);
      
      String[] arr2 = {link2.getSrcID().getName(), link2.getTrgID().getName()};
      Arrays.sort(arr2);
  
//      String concat1 = arr1[0] + "xxx" + arr1[1];
//      String concat2 = arr2[0] + "xxx" + arr2[1];
      String concat1 = String.format("%s___%s",arr1[0], arr1[1]);
      String concat2 = String.format("%s___%s",arr2[0], arr2[1]);

      // THIS IS COMPLETELY TEMPORARY - RISHI DESAI 7/16/17
      
      return concat1.compareTo(concat2);
    }
  }
  
}
