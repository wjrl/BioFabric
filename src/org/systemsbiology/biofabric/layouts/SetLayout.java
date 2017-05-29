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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.analysis.GraphSearcher;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This handles creating a layout for the network presentation of set membership
*/

public class SetLayout {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  public enum LinkMeans {BELONGS_TO, CONTAINS}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Map<NID.WithName, Set<NID.WithName>> elemsPerSet_;
  private Map<NID.WithName, Set<NID.WithName>> setsPerElem_;
  private LinkMeans direction_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public SetLayout(LinkMeans direction) {
    direction_ = direction;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Find out if the necessary conditions for this layout are met. 
  */
  
  public boolean criteriaMet(BioFabricNetwork.RelayoutBuildData rbd,
                             BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                               LayoutCriterionFailureException {
    //
    // 1) All the relations in the network are directed
    // 2) The network is bipartite
    // 3) There are no singleton nodes
    // 4) Not a multigraph.
    //
    
    LoopReporter lr = new LoopReporter(rbd.allLinks.size(), 20, monitor, 0.0, 1.0, "progress.setLayoutCriteriaCheck"); 
    
    if (!((rbd.loneNodeIDs == null) || rbd.loneNodeIDs.isEmpty())) {
      throw new LayoutCriterionFailureException();
    }
     
    HashSet<String> rels = new HashSet<String>();
    for (FabricLink aLink : rbd.allLinks) {
      lr.report();
      if (!aLink.isDirected()) {
        throw new LayoutCriterionFailureException();
      }
      rels.add(aLink.getRelation());
      if (rels.size() > 1) {
        throw new LayoutCriterionFailureException();
      }    
    }
    lr.finish();
    
    elemsPerSet_ = new HashMap<NID.WithName, Set<NID.WithName>>();
    setsPerElem_ = new HashMap<NID.WithName, Set<NID.WithName>>();    
    extractSets(rbd.allLinks, monitor);
 
    return (true);  
  }

  /***************************************************************************
  **
  ** Relayout the network.
  */
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd,
                       BTProgressMonitor monitor) throws AsynchExitRequestException {   
    doNodeLayout(rbd, monitor);
    (new DefaultEdgeLayout()).layoutEdges(rbd, monitor);
    return;
  }
  
  /***************************************************************************
  **
  ** Order the nodes
  */
  
  private List<NID.WithName> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd,
                                          BTProgressMonitor monitor) throws AsynchExitRequestException {
        
    //
    // We order the sets by cardinality, largest first. Ties broken by lexicographic order:
    //
    
    Map<Integer, SortedSet<NID.WithName>> byDegree = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    for (NID.WithName set : elemsPerSet_.keySet()) {
      Set<NID.WithName> elems = elemsPerSet_.get(set);
      Integer card = Integer.valueOf(elems.size());
      SortedSet<NID.WithName> setsWithCard = byDegree.get(card);
      if (setsWithCard == null) {
        setsWithCard = new TreeSet<NID.WithName>();
        byDegree.put(card, setsWithCard);
      }
      setsWithCard.add(set);
    }

    //
    // Now we create an ordered list of the sets:
    //
    
    ArrayList<NID.WithName> setList = new ArrayList<NID.WithName>();
    for (SortedSet<NID.WithName> forDeg : byDegree.values()) {
      setList.addAll(forDeg);
    }
    ArrayList<NID.WithName> nodeOrder = new ArrayList<NID.WithName>(setList);

    SortedSet<GraphSearcher.SourcedNodeGray> snds = GraphSearcher.nodeGraySetWithSourceFromMap(setList, setsPerElem_);
    for (GraphSearcher.SourcedNodeGray node : snds) {   
      nodeOrder.add(node.getNodeID());
    }
    
    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    (new DefaultLayout()).installNodeOrder(nodeOrder, rbd, monitor);
    return (nodeOrder);
  }

  
  /***************************************************************************
  ** 
  ** Extract the set information from the network
  */

  private void extractSets(Set<FabricLink> links,
                           BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                             LayoutCriterionFailureException {
  
    //
    // This graph has to be bipartite, with all directional links going from node set A to node set B
    // ("BELONGS_TO") or the opposite ("CONTAINS"):
    //
    
    HashSet<NID.WithName> setNodes = new HashSet<NID.WithName>();
    HashSet<NID.WithName> elementNodes = new HashSet<NID.WithName>();     
    
    LoopReporter lr = new LoopReporter(links.size(), 20, monitor, 0.0, 1.0, "progress.setLayoutSetExtracton"); 
    
    for (FabricLink link : links) {
      lr.report();
      NID.WithName set = (direction_ == LinkMeans.CONTAINS) ? link.getSrcID() : link.getTrgID();
      NID.WithName elem = (direction_ == LinkMeans.CONTAINS) ? link.getTrgID() : link.getSrcID();
      setNodes.add(set);
      elementNodes.add(elem);
      
      Set<NID.WithName> forSet = elemsPerSet_.get(set);
      if (forSet == null) {
        forSet = new HashSet<NID.WithName>();
        elemsPerSet_.put(set, forSet);
      }
      forSet.add(elem);
      
      Set<NID.WithName> forElem = setsPerElem_.get(elem);
      if (forElem == null) {
        forElem = new HashSet<NID.WithName>();
        setsPerElem_.put(elem, forElem);
      }
      forElem.add(set);  
    }
    lr.finish();
    
    HashSet<NID.WithName> intersect = new HashSet<NID.WithName>(setNodes);
    intersect.retainAll(elementNodes);
    if (!intersect.isEmpty()) {
      throw new LayoutCriterionFailureException();
    }
  
    return;
  }
}
