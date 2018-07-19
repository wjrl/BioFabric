/*
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
import org.systemsbiology.biofabric.ioAPI.BuildData;
import org.systemsbiology.biofabric.layoutAPI.LayoutCriterionFailureException;
import org.systemsbiology.biofabric.layoutAPI.NodeLayout;
import org.systemsbiology.biofabric.modelAPI.NetLink;
import org.systemsbiology.biofabric.modelAPI.NetNode;
import org.systemsbiology.biofabric.workerAPI.AsynchExitRequestException;
import org.systemsbiology.biofabric.workerAPI.BTProgressMonitor;
import org.systemsbiology.biofabric.workerAPI.LoopReporter;

/****************************************************************************
**
** This handles creating a layout for the network presentation of set membership
*/

public class SetLayout extends NodeLayout {
  
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
  
  private Map<NetNode, Set<NetNode>> elemsPerSet_;
  private Map<NetNode, Set<NetNode>> setsPerElem_;
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
  
  @Override
  public boolean criteriaMet(BuildData rbd,
                             BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                               LayoutCriterionFailureException {
    //
    // 1) All the relations in the network are directed
    // 2) The network is bipartite
    // 3) There are no singleton nodes
    // 4) Not a multigraph.
    //
    
    LoopReporter lr = new LoopReporter(rbd.getLinks().size(), 20, monitor, 0.0, 1.0, "progress.setLayoutCriteriaCheck"); 
    
    
    if (!((rbd.getSingletonNodes() == null) || rbd.getSingletonNodes().isEmpty())) {
      throw new LayoutCriterionFailureException();
    }
     
    HashSet<String> rels = new HashSet<String>();
    for (NetLink aLink : rbd.getLinks()) {
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
    
    elemsPerSet_ = new HashMap<NetNode, Set<NetNode>>();
    setsPerElem_ = new HashMap<NetNode, Set<NetNode>>();    
    extractSets(rbd.getLinks(), monitor);
 
    return (true);  
  }
  
  /***************************************************************************
  **
  ** Order the nodes
  */
  
  public List<NetNode> doNodeLayout(BuildData rbd,
  		                              Params params,
                                    BTProgressMonitor monitor) throws AsynchExitRequestException {
        
    //
    // We order the sets by cardinality, largest first. Ties broken by lexicographic order:
    //
    
    Map<Integer, SortedSet<NetNode>> byDegree = new TreeMap<Integer, SortedSet<NetNode>>(Collections.reverseOrder());
    for (NetNode set : elemsPerSet_.keySet()) {
      Set<NetNode> elems = elemsPerSet_.get(set);
      Integer card = Integer.valueOf(elems.size());
      SortedSet<NetNode> setsWithCard = byDegree.get(card);
      if (setsWithCard == null) {
        setsWithCard = new TreeSet<NetNode>();
        byDegree.put(card, setsWithCard);
      }
      setsWithCard.add(set);
    }

    //
    // Now we create an ordered list of the sets:
    //
    
    ArrayList<NetNode> setList = new ArrayList<NetNode>();
    for (SortedSet<NetNode> forDeg : byDegree.values()) {
      setList.addAll(forDeg);
    }
    ArrayList<NetNode> nodeOrder = new ArrayList<NetNode>(setList);

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

  private void extractSets(Set<NetLink> links,
                           BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                             LayoutCriterionFailureException {
  
    //
    // This graph has to be bipartite, with all directional links going from node set A to node set B
    // ("BELONGS_TO") or the opposite ("CONTAINS"):
    //
    
    HashSet<NetNode> setNodes = new HashSet<NetNode>();
    HashSet<NetNode> elementNodes = new HashSet<NetNode>();     
    
    LoopReporter lr = new LoopReporter(links.size(), 20, monitor, 0.0, 1.0, "progress.setLayoutSetExtraction"); 
    
    for (NetLink link : links) {
      lr.report();
      NetNode set = (direction_ == LinkMeans.CONTAINS) ? link.getSrcNode() : link.getTrgNode();
      NetNode elem = (direction_ == LinkMeans.CONTAINS) ? link.getTrgNode() : link.getSrcNode();
      setNodes.add(set);
      elementNodes.add(elem);
      
      Set<NetNode> forSet = elemsPerSet_.get(set);
      if (forSet == null) {
        forSet = new HashSet<NetNode>();
        elemsPerSet_.put(set, forSet);
      }
      forSet.add(elem);
      
      Set<NetNode> forElem = setsPerElem_.get(elem);
      if (forElem == null) {
        forElem = new HashSet<NetNode>();
        setsPerElem_.put(elem, forElem);
      }
      forElem.add(set);  
    }
    lr.finish();
    
    HashSet<NetNode> intersect = new HashSet<NetNode>(setNodes);
    intersect.retainAll(elementNodes);
    if (!intersect.isEmpty()) {
      throw new LayoutCriterionFailureException();
    }
  
    return;
  }
}
