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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.analysis.NIDLink;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This is the default layout algorithm for edges. Actually usable in combination 
** with a wide variety of different node layout algorithms, not just the default
*/

public class DefaultEdgeLayout {
  
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

  public DefaultEdgeLayout() {
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
  
  public void layoutEdges(BioFabricNetwork.RelayoutBuildData rbd) {
    
    Map<NID.WithName, Integer> nodeOrder = rbd.nodeOrder;
    
    //
    // Build target->row maps and the inverse:
    //
    HashMap<NID.WithName, Integer> targToRow = new HashMap<NID.WithName, Integer>();
    HashMap<Integer, NID.WithName> rowToTarg = new HashMap<Integer, NID.WithName>();
    Iterator<NID.WithName> trit = nodeOrder.keySet().iterator();
    while (trit.hasNext()) {
      NID.WithName target = trit.next();
      Integer rowObj = nodeOrder.get(target);
      targToRow.put(target, rowObj);
      rowToTarg.put(rowObj, target);
    }
    
    //
    // Now each link is given a vertical extent.
    //
    
    TreeMap<Integer, SortedSet<Integer>> rankedLinks = new TreeMap<Integer, SortedSet<Integer>>();
    Map<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>> relsForPair = 
    	generateLinkExtents(rbd.allLinks, targToRow, rankedLinks);
    
    //
    // Ordering of links:
    //
  
    rbd.setLinkOrder(defaultLinkToColumn(rankedLinks, relsForPair, rowToTarg, rbd)); 
    
    return;
  }
 
      
  /***************************************************************************
  ** 
  ** Process a link set
  */

  private SortedMap<Integer, FabricLink> defaultLinkToColumn(SortedMap<Integer, SortedSet<Integer>> rankedLinks,         
                                                             Map<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>> relsForPair,
                                                             HashMap<Integer, NID.WithName> rowToTarg,
                                                             BioFabricNetwork.RelayoutBuildData rbd) {    
  
    TreeMap<Integer, FabricLink> linkOrder = new TreeMap<Integer, FabricLink>();
    //
    // This now assigns the link to its column.  Note that we order them
    // so that the shortest vertical link is drawn first!
    //
   
    ArrayList<String> microRels;
    ArrayList<String> macroRels;
    if (rbd.layoutMode == BioFabricNetwork.LayoutMode.PER_NODE_MODE) {
    	microRels = new ArrayList<String>(rbd.linkGroups);
    	macroRels = null;
    } else if (rbd.layoutMode == BioFabricNetwork.LayoutMode.PER_NETWORK_MODE) {
    	microRels = new ArrayList<String>();
    	macroRels = new ArrayList<String>(rbd.linkGroups);
    } else {
    	microRels = new ArrayList<String>();
    	macroRels = null;
    }

    if (microRels.isEmpty()) {
      microRels.add("");
    }
    int numRel = microRels.size();
      
    int colCount = 0;
    int rowCount = rbd.nodeOrder.size();
    // For each top row...
    for (int k = 0; k < rowCount; k++) {
      Integer topRow = Integer.valueOf(k);
      for (int i = 0; i < numRel; i++) {
        String relOnly = microRels.get(i);
        if (relOnly.equals("")) {
          relOnly = null;
        }
        colCount = shadowLinkToColumn(topRow.intValue(), rankedLinks, relsForPair, relOnly, microRels, colCount, rowToTarg, rbd, linkOrder);     
        SortedSet<Integer> perSrc = rankedLinks.get(topRow);
        if (perSrc == null) {
          continue;
        }
        Iterator<Integer> psit = perSrc.iterator();

        // Drain the bottom rows, in increasing order...
        while (psit.hasNext()) {
          Integer botRow = psit.next();
          NID.WithName topNode = rowToTarg.get(topRow);
          NID.WithName botNode = rowToTarg.get(botRow);
          // Dumping links in order of the relation sort (alphabetical)...
          SortedMap<FabricLink.AugRelation, FabricLink> forPair1 = relsForPair.get(new NIDLink(topNode, botNode));
          if (forPair1 != null) {
            Iterator<FabricLink> fp1it = forPair1.values().iterator();
            while (fp1it.hasNext()) {
              FabricLink nextLink = fp1it.next();
              if (!nextLink.isShadow()) {
                String augRel = nextLink.getAugRelation().relation;
                if (bestSuffixMatch(augRel, relOnly, microRels)) {
                  Integer shadowKey = Integer.valueOf(colCount++);
                  linkOrder.put(shadowKey, nextLink);
                }
              }
            }
          }
          // With directed links from above coming before directed links from below...       
          if (!topNode.equals(botNode)) { // DO NOT DUPLICATE FEEDBACK LINKS!
            SortedMap<FabricLink.AugRelation, FabricLink> forPair2 = relsForPair.get(new NIDLink(botNode, topNode));
            if (forPair2 != null) {        
              Iterator<FabricLink> fp2it = forPair2.values().iterator();
              while (fp2it.hasNext()) {
                FabricLink nextLink = fp2it.next();
                if (!nextLink.isShadow()) {
                  String augRel = nextLink.getAugRelation().relation;
                  if (bestSuffixMatch(augRel, relOnly, microRels)) {
                    Integer shadowKey = Integer.valueOf(colCount++);
                    linkOrder.put(shadowKey, nextLink);
                  }
                }
              }
            }
          }
        }
      }
    }

    //
    // This is what determines if we have an existing link order that needs to reorganize on a per-network basis:
    //
    	
    if (rbd.layoutMode == BioFabricNetwork.LayoutMode.PER_NETWORK_MODE) {
    	orderNetworkByGroups(linkOrder, macroRels);
    }
 
    return (linkOrder);
  }
  
  /***************************************************************************
  *  existingOrd's link order will follow groupOrder's relation order.
  */

    private void orderNetworkByGroups(SortedMap<Integer, FabricLink> existingOrd, List<String> groupOrder) {

      Map<String, List<FabricLink>> groups = new TreeMap<String, List<FabricLink>>();
      // String: link relation, List: all the links with that relation

      for (Map.Entry<Integer, FabricLink> entry : existingOrd.entrySet()) {

        FabricLink fl = entry.getValue();
        String rel = fl.getRelation();

        if (groups.get(rel) == null) {
          groups.put(rel, new ArrayList<FabricLink>());
        }

        groups.get(rel).add(fl);
      }

      int rowIdx = 0;
      for (String suffix : groupOrder) {  // note: 'groupOrder' contains the suffixes

        for (String fullRel : groups.keySet()) {  // iterate through full Relation names to find best match

          if (bestSuffixMatch(fullRel, suffix, groupOrder)) {

            List<FabricLink> group = groups.get(fullRel);
        for (FabricLink fl : group) {
          existingOrd.put(rowIdx, fl);
          rowIdx++;                    // increment the row index
        }
          }
        }

      }
      return;
    }
  
  /***************************************************************************
  ** 
  ** Get shadow links into their columns:
  */

  private int shadowLinkToColumn(int currDrainRow, SortedMap<Integer, SortedSet<Integer>> rankedLinks, 
                                  Map<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>> relsForPair, 
                                  String relOnly, List<String> allRels, int colCount, HashMap<Integer, NID.WithName> rowToTarg,
                                  BioFabricNetwork.RelayoutBuildData rbd, TreeMap<Integer, FabricLink> linkOrder) {    
    
    Iterator<Integer> rlit = rankedLinks.keySet().iterator();
    // For each top row...
    while (rlit.hasNext()) {
      Integer topRow = rlit.next();
      SortedSet<Integer> perSrc = rankedLinks.get(topRow);
      Iterator<Integer> psit = perSrc.iterator();
      // Drain ONLY the bottom row that ends on our row...
      while (psit.hasNext()) {
        Integer botRow = psit.next();
        if (botRow.intValue() != currDrainRow) {
          continue;
        }
        // We NEVER create shadow feedback links.  They are insanely redundant.
        if (topRow.equals(botRow)) {
          continue;
        }
        
        NID.WithName topNode = rowToTarg.get(topRow);
        NID.WithName botNode = rowToTarg.get(botRow);
        // Dumping links in order of the relation sort (alphabetical)...
        SortedMap<FabricLink.AugRelation, FabricLink> forPair1 = relsForPair.get(new NIDLink(topNode, botNode));
        if (forPair1 != null) {
          Iterator<FabricLink> fp1it = forPair1.values().iterator();
          while (fp1it.hasNext()) {
            // But ONLY if they are shadow links:
            FabricLink nextLink = fp1it.next();
            if (nextLink.isShadow()) {
              String augRel = nextLink.getAugRelation().relation;
              if (bestSuffixMatch(augRel, relOnly, allRels)) {
                Integer shadowKey = Integer.valueOf(colCount++);
                linkOrder.put(shadowKey, nextLink);
              }
            }
          }
        }
        // With directed links from above coming before directed links from below... 
        // This test should now always be true, given we are never doing feedback....
        if (!topNode.equals(botNode)) { // DO NOT DUPLICATE FEEDBACK LINKS!
          SortedMap<FabricLink.AugRelation, FabricLink> forPair2 = relsForPair.get(new NIDLink(botNode, topNode));
          if (forPair2 != null) {        
            Iterator<FabricLink> fp2it = forPair2.values().iterator();
            while (fp2it.hasNext()) {
              FabricLink nextLink = fp2it.next();
              if (nextLink.isShadow()) {
                String augRel = nextLink.getAugRelation().relation;
                if (bestSuffixMatch(augRel, relOnly, allRels)) {
                  Integer shadowKey = Integer.valueOf(colCount++);
                  linkOrder.put(shadowKey, nextLink);
                }
              }
            }
          }
        } else {
          throw new IllegalStateException();  // Now should never get here for shadow links...
        }
      }
    }
    return (colCount);
  }

  /***************************************************************************
  ** 
   ** Answer if the given relation has the best suffix match with the given match,
  ** given all the options. Thus, "430" should match "30" instead of "0" if both
  ** are present.
  */

  private boolean bestSuffixMatch(String augR, String relToMatch, List<String> allRels) {
  	if (relToMatch == null) {
  		return (true);
  	}
  	int topLen = 0;
  	String topRel = null;
  	for (String aRel : allRels) {
  		int matchLen = aRel.length();
  		if (matchLen < topLen) {
  			continue;
  		}
  		int ioaRel = augR.indexOf(aRel);
  		if ((ioaRel >= 0) && ((ioaRel == (augR.length() - matchLen)))) {
        if (topLen == matchLen) {
        	throw new IllegalStateException();
  		  } else if (topLen < matchLen) {
  		    topLen = matchLen;
          topRel = aRel;	
  		  }
      }	 
  	}
  	if (topRel == null) {
  		throw new IllegalStateException();
  	}
    return (topRel.equals(relToMatch));
  }
  
  /***************************************************************************
  ** 
  ** Generate vertical extents for links:
  **
  ** Returns Map relsForPair:
  **  Maps each simple source/target "Link" to sorted map
  **  Sorted map maps each link relation to the associated FabricLink, ordered by
  **  how the AugRelation comparator works
  ** 
  ** Fills in empty SortedMap rankedLinks:
  **  Maps each top link row to an ordered set of link maximums 
  */

  private Map<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>> generateLinkExtents(Set<FabricLink> allLinks,
                                                                                       HashMap<NID.WithName, Integer> targToRow,
                                                                                       TreeMap<Integer, SortedSet<Integer>> rankedLinks) {
    //
    // Now each link is given a vertical extent.
    //
    
    HashMap<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>> relsForPair 
      = new HashMap<NIDLink, SortedMap<FabricLink.AugRelation, FabricLink>>();
  //  HashMap directedMap = new HashMap();
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      NID.WithName source = nextLink.getSrcID();
      NID.WithName target = nextLink.getTrgID();
   //   boolean directed = nextLink.isDirected();
  //    directedMap.put(new Link(source, target), new Boolean(directed));
      NIDLink key = new NIDLink(source, target);
      SortedMap<FabricLink.AugRelation, FabricLink> rels = relsForPair.get(key);
      if (rels == null) {
        rels = new TreeMap<FabricLink.AugRelation, FabricLink>();
        relsForPair.put(key, rels);
      }
      rels.put(nextLink.getAugRelation(), nextLink);   
      Integer srcRow = targToRow.get(source);
      Integer trgRow = targToRow.get(target);
      Integer minRow;
      Integer maxRow;
      if (srcRow.intValue() < trgRow.intValue()) {
        minRow = srcRow;
        maxRow = trgRow;
      } else {
        minRow = trgRow;
        maxRow = srcRow;         
      }
      SortedSet<Integer> perSrc = rankedLinks.get(minRow);  // min == node row!
      if (perSrc == null) {
        perSrc = new TreeSet<Integer>();
        rankedLinks.put(minRow, perSrc);
      }   
      perSrc.add(maxRow);  // Have the ordered set of link maxes
    }
    return (relsForPair);
  }
}
