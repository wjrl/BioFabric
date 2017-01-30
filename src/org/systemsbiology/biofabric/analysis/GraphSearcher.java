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

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** A Class
*/

public class GraphSearcher {
  
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
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private HashSet<NID.WithName> allNodes_;
  private HashSet<FabricLink> allEdges_;
  private ArrayList<NID.WithName> nodeOrder_;
  private ArrayList<FabricLink> edgeOrder_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(Set<NID.WithName> nodes, Set<FabricLink> links) {

    allNodes_ = new HashSet<NID.WithName>();
    allEdges_ = new HashSet<FabricLink>();
    edgeOrder_ = null;
    nodeOrder_ = null;    

    Iterator<NID.WithName> ni = nodes.iterator();
    Iterator<FabricLink> li = links.iterator();
  
    while (ni.hasNext()) {
      allNodes_.add(ni.next());
    }
    
    while (li.hasNext()) {
      FabricLink link = li.next();
      allEdges_.add(link.clone());
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor.  Used to create a depth-first order that 
  ** retains original sibling order.  If link appears multiple
  ** times, the order is based on first appearance.
  */

  public GraphSearcher(List<NID.WithName> nodes, List<FabricLink> links) {

    allNodes_ = new HashSet<NID.WithName>();
    allEdges_ = new HashSet<FabricLink>();
    edgeOrder_ = new ArrayList<FabricLink>();
    nodeOrder_ = new ArrayList<NID.WithName>();

    Iterator<FabricLink> li = links.iterator();
  
    nodeOrder_.addAll(nodes);
    allNodes_.addAll(nodes);    

    while (li.hasNext()) {
      FabricLink link = li.next();
      if (!allEdges_.contains(link)) {
        edgeOrder_.add(link.clone());
      }
      allEdges_.add(link.clone());
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Map of node degree
  */

  public Map<NID.WithName, Integer> nodeDegree(boolean inOnly) {
    return (nodeDegree(inOnly, allEdges_));
  }
  
  /***************************************************************************
  ** 
  ** Map of node degree
  */

  public static Map<NID.WithName, Integer> nodeDegree(boolean inOnly, Set<FabricLink> edges) {
    
    HashMap<NID.WithName, Integer> retval = new HashMap<NID.WithName, Integer>();

    Iterator<FabricLink> li = edges.iterator();
    while (li.hasNext()) {
      FabricLink link = li.next();
      NID.WithName src = link.getSrcID();
      NID.WithName trg = link.getTrgID();
      if (!inOnly) {
        Integer deg = retval.get(src);
        if (deg == null) {
          retval.put(src, new Integer(1));
        } else {
          retval.put(src, new Integer(deg.intValue() + 1));
        }
      }
      Integer deg = retval.get(trg);
      if (deg == null) {
        retval.put(trg, new Integer(1));
      } else {
        retval.put(trg, new Integer(deg.intValue() + 1));
      }
    }
    return (retval);
  }

  
  /***************************************************************************
  ** 
  ** Sorted set of node degree
  */

  public SortedSet<NodeDegree> nodeDegreeSet() {
    
    TreeSet<NodeDegree> retval = new TreeSet<NodeDegree>();

    Map<NID.WithName, Integer> nds = nodeDegree(false);
    
    Iterator<NID.WithName> li = nds.keySet().iterator();
    while (li.hasNext()) {
      NID.WithName node = li.next();
      NodeDegree ndeg = new NodeDegree(node, nds.get(node).intValue());
      retval.add(ndeg);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of node degree
  */

  public static SortedSet<NodeDegree> nodeDegreeSet(Set<FabricLink> edges) {
    
    TreeSet<NodeDegree> retval = new TreeSet<NodeDegree>();

    Map<NID.WithName, Integer> nds = nodeDegree(false, edges);
    
    Iterator<NID.WithName> li = nds.keySet().iterator();
    while (li.hasNext()) {
      NID.WithName node = li.next();
      NodeDegree ndeg = new NodeDegree(node, nds.get(node).intValue());
      retval.add(ndeg);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of node degree
  */

  public static List<NID.WithName> nodesByDegree(Set<NID.WithName> nodes, SortedSet<NodeDegree> nds) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    
    Iterator<NodeDegree> li = nds.iterator();
    while (li.hasNext()) {
      NodeDegree nd = li.next();
      NID.WithName node = nd.getNode();
      if (nodes.contains(node)) {
        retval.add(node);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of node degree
  */

  public SortedSet<SourcedNodeDegree> nodeDegreeSetWithSource(List<NID.WithName> sourceOrder) {
 
    HashMap<NID.WithName, Set<NID.WithName>> allSrcs = new HashMap<NID.WithName, Set<NID.WithName>>();
    Iterator<FabricLink> lit = allEdges_.iterator();
    while (lit.hasNext()) {
      FabricLink nextLink = lit.next();
      NID.WithName trg = nextLink.getTrgID();
      Set<NID.WithName> trgSources = allSrcs.get(trg);
      if (trgSources == null) {
        trgSources = new HashSet<NID.WithName>();
        allSrcs.put(trg, trgSources);
      }
      trgSources.add(nextLink.getSrcID());
    } 
    
    TreeSet<SourcedNodeDegree> retval = new TreeSet<SourcedNodeDegree>();

    Iterator<NID.WithName> li = allSrcs.keySet().iterator();
    while (li.hasNext()) {
      NID.WithName node = li.next();
      Set<NID.WithName> trgSources = allSrcs.get(node);
      SourcedNodeDegree ndeg = new SourcedNodeDegree(node, sourceOrder, trgSources);
      retval.add(ndeg);
    }
    return (retval);
  }
  
   
  /***************************************************************************
  ** 
  ** Sorted list of nodes by degree
  */

  public List<NID.WithName> nodeDegreeOrder() {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    SortedSet<NodeDegree> nds = nodeDegreeSet();
    Iterator<NodeDegree> li = nds.iterator();
    while (li.hasNext()) {
      NodeDegree ndeg = li.next();
      retval.add(ndeg.getNode());
    }
    return (retval);
  }
   
  /***************************************************************************
  ** 
  ** Topo sort
  */

  public Map<NID.WithName, Integer> topoSort(boolean compress) {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Assign all roots to level 0, add them to return list at that level.
    // Delete them and edges from them.  Recalulate root list and continue.
    //
    
    HashMap<NID.WithName, Integer> retval = new HashMap<NID.WithName, Integer>();
    Set<NID.WithName> currentNodes = new HashSet<NID.WithName>(allNodes_);
    //
    // Deep copy:
    //
    Set<FabricLink> currentEdges = new HashSet<FabricLink>();
    Iterator<FabricLink> li = allEdges_.iterator();
    while (li.hasNext()) {
      FabricLink link = li.next();
      currentEdges.add(link.clone());
    }
      
    Map<NID.WithName, Set<FabricLink>> outEdges = calcOutboundEdges(currentEdges);
    Set<NID.WithName> rootNodes = buildRootList(currentNodes, currentEdges);
  
    int level = 0;
    while (!rootNodes.isEmpty()) {
      Integer ilevel = new Integer(level++);
      Iterator<NID.WithName> rit = rootNodes.iterator();
      while (rit.hasNext()) {
        NID.WithName nodeID = rit.next();
        retval.put(nodeID, ilevel);
        outEdges.remove(nodeID);
        currentNodes.remove(nodeID);
      }
      currentEdges = invertOutboundEdges(outEdges);
      rootNodes = buildRootList(currentNodes, currentEdges);
    }
    
    if (compress) {
      contractTopoSort(retval);
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Depth-First Search
  */

  public List<QueueEntry> depthSearch() {
    //
    // Do until roots are exhausted
    //
    HashSet<NID.WithName> visited = new HashSet<NID.WithName>();
    
    Set<NID.WithName> rootNodes = buildRootList(allNodes_, allEdges_);
    Map<NID.WithName, Set<FabricLink>> outEdges = calcOutboundEdges(allEdges_); 

    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    if (edgeOrder_ != null) {
      HashSet<NID.WithName> seenRoots = new HashSet<NID.WithName>();
      Iterator<NID.WithName> nit = nodeOrder_.iterator();
      while (nit.hasNext()) {
        NID.WithName currNode = nit.next();
        if (!rootNodes.contains(currNode)) {
          continue;
        }
        boolean gottaLink = false;
        Iterator<FabricLink> eit = edgeOrder_.iterator();
        while (eit.hasNext()) {
          FabricLink link = eit.next();
          NID.WithName src = link.getSrcID();
          if (!currNode.equals(src)) {
            continue;
          }
          if (seenRoots.contains(src)) {
            continue;
          }
          seenRoots.add(src);
          gottaLink = true;
          searchGutsDepth(src, visited, outEdges, 0, edgeOrder_, retval);
        }
        if (!gottaLink) {
          visited.add(currNode);
          retval.add(new QueueEntry(0, currNode));
        }
      }
    } else {
      Iterator<NID.WithName> rit = rootNodes.iterator();
      while (rit.hasNext()) {
        searchGutsDepth(rit.next(), visited, outEdges, 0, null, retval);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Breadth-First Search, ordered by degree
  */

  public List<QueueEntry> breadthSearch(List<NID.WithName> startNodes) {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    List<NID.WithName> byDeg = nodeDegreeOrder();
    Collections.reverse(byDeg);
    ArrayList<NID.WithName> toProcess = new ArrayList<NID.WithName>(byDeg);
    
    if ((startNodes != null) && !startNodes.isEmpty()) {
    	toProcess.removeAll(startNodes);
    	ArrayList<NID.WithName> useDeg = new ArrayList<NID.WithName>(startNodes);	
    	useDeg.addAll(toProcess);
    	toProcess = useDeg;
    }
 
    //
    // Do until everybody is visited
    //
    HashSet<NID.WithName> visited = new HashSet<NID.WithName>();
    ArrayList<QueueEntry> queue = new ArrayList<QueueEntry>();
    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    Map<NID.WithName, Set<FabricLink>> outEdges = calcOutboundEdges(allEdges_);    
   
    
    while (!toProcess.isEmpty()) {  
	    queue.add(new QueueEntry(0, toProcess.get(0)));
	    searchGutsBreadth(visited, queue, outEdges, retval, null, byDeg);
	    toProcess.removeAll(visited);
    }
    return (retval);
  }
  
  
  /***************************************************************************
  ** 
  ** Breadth-First Search
  */

  public List<QueueEntry> breadthSearchUntilStopped(Set<NID.WithName> startNodes, CriteriaJudge judge) {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    //
    // Do until roots are exhausted
    //
    
    HashSet<NID.WithName> visited = new HashSet<NID.WithName>();
    ArrayList<QueueEntry> queue = new ArrayList<QueueEntry>();
    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    
    Map<NID.WithName, Set<FabricLink>> outEdges = calcOutboundEdges(allEdges_);    
    
    Iterator<NID.WithName> rit = startNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, rit.next()));
    }
    searchGutsBreadth(visited, queue, outEdges, retval, judge, null);
    return (retval);
  }
   
  /***************************************************************************
  ** 
  ** A topo sort map gives column for each node.  Invert that to give a list
  ** of nodes for each column.
  */
  
  public int invertTopoSort(Map<NID.WithName, Integer> topoSort, Map<Integer, List<NID.WithName>> invert) {
        
    Iterator<NID.WithName> kit = topoSort.keySet().iterator();
    int maxLevel = -1;
    while (kit.hasNext()) {
      NID.WithName key = kit.next();
      Integer level = topoSort.get(key);
      int currLev = level.intValue();
      if (currLev > maxLevel) {
        maxLevel = currLev;
      }
      List<NID.WithName> nodeList = invert.get(level);
      if (nodeList == null) {
        nodeList = new ArrayList<NID.WithName>();
        invert.put(level, nodeList);
      }
      nodeList.add(key);
    }
    return (maxLevel);
  }
  
  /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<NID.WithName> topoSortToPartialOrdering(Map<NID.WithName, Integer> topoSort) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    TreeMap<Integer, List<NID.WithName>> invert = new TreeMap<Integer, List<NID.WithName>>();
      
    invertTopoSort(topoSort, invert);    
    Iterator<List<NID.WithName>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<NID.WithName> listForLevel = kit.next();
      // Want reproducibility between VfA and Vfg layouts.  This is required.
      Collections.sort(listForLevel);
      retval.addAll(listForLevel);
    }
    return (retval);
  }  
  
   /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<NID.WithName> topoSortToPartialOrdering(Map<NID.WithName, Integer> topoSort, Set<FabricLink> allLinks) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    TreeMap<Integer, List<NID.WithName>> invert = new TreeMap<Integer, List<NID.WithName>>();
    
    SortedSet<NodeDegree> nds = nodeDegreeSet(allLinks);
    
    invertTopoSort(topoSort, invert);    
    Iterator<List<NID.WithName>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<NID.WithName> listForLevel = kit.next();
      List<NID.WithName> l4lbyDeg = nodesByDegree(new HashSet<NID.WithName>(listForLevel), nds);
      Collections.reverse(l4lbyDeg);
      retval.addAll(l4lbyDeg);
    }
    return (retval);
  }  
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  ** 
  ** With a given topo sort, force the given moveID to one end
  */

  public static Map<String, Integer> topoSortReposition(Map<String, Integer> origSort, String moveID, boolean moveMin) {
    
    Integer origCol = origSort.get(moveID);
    if (origCol == null) {
      return (new HashMap<String, Integer>(origSort));
    }
    
    int colVal = origCol.intValue();
    boolean colDup = false;
    int minVal = Integer.MAX_VALUE;
    int maxVal = Integer.MIN_VALUE;
    Iterator<String> oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      String key = oskit.next();
      if (key.equals(moveID)) {
        continue;
      }
      Integer checkCol = origSort.get(key);
      int chekVal = checkCol.intValue();
      if (chekVal < minVal) {
        minVal = chekVal;
      }
      if (chekVal > maxVal) {
        maxVal = chekVal;
      }
      if (chekVal == colVal) {
        colDup = true;
      }
    }
    
    int minMove;
    int maxMove;
    int moveCol;
    int inc;
    if (moveMin) {      
      if (colDup) { // to front, everybody up
        moveCol = minVal; 
        minMove = minVal;
        maxMove = maxVal;
        inc = 1;
      } else { // to front, guys below move up
        moveCol = (colVal < minVal) ? colVal : minVal; 
        minMove = minVal;
        maxMove = colVal - 1;
        inc = 1;
      }
    } else {
      if (colDup) { // to end, nobody moves
        moveCol = maxVal + 1;
        minMove = minVal - 1;
        maxMove = minVal - 1;
        inc = 0;       
      } else { // to end, guys above move down
        moveCol = (colVal > maxVal) ? colVal : maxVal;
        minMove = minVal - 1;
        maxMove = minVal - 1;
        inc = 0;      
      } 
    }
      
    HashMap<String, Integer> retval = new HashMap<String, Integer>();
  
    oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      String key = oskit.next();
      if (key.equals(moveID)) {
        retval.put(moveID, new Integer(moveCol));
      } else {   
        Integer checkCol = origSort.get(key);
        int chekVal = checkCol.intValue();
        if ((chekVal >= minMove) && (chekVal <= maxMove)) {
          retval.put(key, new Integer(chekVal + inc));
        } else {
          retval.put(key, checkCol);
        }
      }
    }
    return (retval);
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public class QueueEntry {
    public int depth;
    public NID.WithName name;
 
    QueueEntry(int depth, NID.WithName name) {
      this.depth = depth;
      this.name = name;
    }
    
    public String toString() {
      return (name + " depth = " + depth);
    }
    
  }
  
  /***************************************************************************
  **
  ** Tell us when to stop looking
  */  
      
  public static interface CriteriaJudge {
    public boolean stopHere(NID.WithName nodeID);  
  } 
 
  /****************************************************************************
  **
  ** A Class
  */
  
  public static class NodeDegree implements Comparable<NodeDegree> {
    
    private NID.WithName node_;
    private int degree_;
  
    public NodeDegree(NID.WithName node, int degree) {
      node_ = node;
      degree_ = degree;
    }
    
    public NID.WithName getNode() {
      return (node_);
    }
    
    @Override
    public int hashCode() {
      return (node_.hashCode() + degree_);
    }
  
    @Override
    public String toString() {
      return (" node = " + node_ + " degree = " + degree_);
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof NodeDegree)) {
        return (false);
      }
      NodeDegree otherDeg = (NodeDegree)other;    
      if (this.degree_ != otherDeg.degree_) {
        return (false);
      }
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(NodeDegree otherDeg) {
      if (this.degree_ != otherDeg.degree_) {
        return (this.degree_ - otherDeg.degree_);
      }
      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    } 
  }
  
  /****************************************************************************
  **
  ** A Class
  */
  
  public static class SourcedNodeDegree implements Comparable<SourcedNodeDegree> {
    
    private NID.WithName node_;
    private int degree_;
    private boolean[] srcs_;
    
    public SourcedNodeDegree(NID.WithName node, List<NID.WithName> srcOrder, Set<NID.WithName> mySrcs) {
      node_ = node;
      degree_ = mySrcs.size();
      int numSrc = srcOrder.size();
      srcs_ = new boolean[numSrc];
      for (int i = 0; i < numSrc; i++) {
        if (mySrcs.contains(srcOrder.get(i))) {
          srcs_[i] = true;
        }
      }
    }
    
    public NID.WithName getNode() {
      return (node_);
    }
    
    @Override
    public int hashCode() {
      return (node_.hashCode() + degree_);
    }
  
    @Override
    public String toString() {
      return (" node = " + node_ + " degree = " + degree_);
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNodeDegree)) {
        return (false);
      }
      SourcedNodeDegree otherDeg = (SourcedNodeDegree)other;    
      if (this.degree_ != otherDeg.degree_) {
        return (false);
      }
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(SourcedNodeDegree otherDeg) {
      if (this.degree_ != otherDeg.degree_) {
        // Higher degree is first:
        return (otherDeg.degree_ - this.degree_);
      }
      
      boolean iAmBigger = false;
      boolean heIsBigger = false;
      for (int i = 0; i < this.srcs_.length; i++) {

        if (this.srcs_[i]) {
          if (otherDeg.srcs_[i] == false) {
            iAmBigger = true;
            heIsBigger = false;
            break;
          }
        }
        if (otherDeg.srcs_[i]) {
          if (this.srcs_[i] == false) {
            iAmBigger = false;
            heIsBigger = true;
            break;
          } 
        }
      }
      if (iAmBigger) {
        return (-1);
      } else if (heIsBigger) {
        return (1);
      }
      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    } 
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Build map from node to outbound edges
  */

  private Map<NID.WithName, Set<FabricLink>> calcOutboundEdges(Set<FabricLink> edges) {
    
    HashMap<NID.WithName, Set<FabricLink>> retval = new HashMap<NID.WithName, Set<FabricLink>>();
    Iterator<FabricLink> li = edges.iterator();

    while (li.hasNext()) {
      FabricLink link = li.next();
      addaLink(link, link.getSrcID(), retval);
      if (!link.isDirected()) {
      	addaLink(link, link.getTrgID(), retval);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Add a link to a bin
  */

  private void addaLink(FabricLink link, NID.WithName bin, Map<NID.WithName, Set<FabricLink>> collect) {
    Set<FabricLink> forBin = collect.get(bin);
    if (forBin == null) {
      forBin = new HashSet<FabricLink>();
      collect.put(bin, forBin);
    }
    forBin.add(link);
    return;
  }
  
  /***************************************************************************
  **
  ** Build a root list
  */

  private Set<NID.WithName> buildRootList(Set<NID.WithName> nodes, Set<FabricLink> edges) {
  
    HashSet<NID.WithName> retval = new HashSet<NID.WithName>();
    retval.addAll(nodes);
    
    Iterator<FabricLink> ei = edges.iterator();
    while (ei.hasNext()) {
      FabricLink link = ei.next();
      NID.WithName trg = link.getTrgID();
      retval.remove(trg);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Invert
  */

  private Set<FabricLink> invertOutboundEdges(Map<NID.WithName, Set<FabricLink>> outEdges) {
    
    HashSet<FabricLink> retval = new HashSet<FabricLink>();
    Iterator<NID.WithName> ki = outEdges.keySet().iterator();

    while (ki.hasNext()) {
      NID.WithName src = ki.next();
      Set<FabricLink> links = outEdges.get(src);
      Iterator<FabricLink> sit = links.iterator();
      while (sit.hasNext()) {
        FabricLink lnk = sit.next();
        if (lnk.isFeedback()) {
          retval.add(lnk.clone());
        } else {
          retval.add(lnk.flipped());
        }
      }
    }
    return (retval);
  }  

  /***************************************************************************
  ** 
  ** Depth-First Search guts
  */

  private void searchGutsDepth(NID.WithName vertexID, HashSet<NID.WithName> visited, 
  		                         Map<NID.WithName, Set<FabricLink>> edgesFromSrc,
                               int depth, List<FabricLink> edgeOrder, List<QueueEntry> results) {

    if (visited.contains(vertexID)) {
      return;
    }
    visited.add(vertexID);
    results.add(new QueueEntry(depth, vertexID));
    Set<FabricLink> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return;
    }
    
    if (edgeOrder != null) {
      Iterator<FabricLink> eit = edgeOrder.iterator();
      while (eit.hasNext()) {
        FabricLink link = eit.next();
        if (!vertexID.equals(link.getSrcID())) {
          continue;
        }
        NID.WithName targ = link.getTrgID();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    } else {
      Iterator<FabricLink> eit = outEdges.iterator();
      while (eit.hasNext()) {
        FabricLink link = eit.next();
        NID.WithName targ = link.getTrgID();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Breadth-First Search guts
  */

  private void searchGutsBreadth(HashSet<NID.WithName> visited, ArrayList<QueueEntry> queue, Map<NID.WithName, Set<FabricLink>> edgesFromSrc, 
                                 List<QueueEntry> results, CriteriaJudge judge, List<NID.WithName> byDegree) {

    while (queue.size() > 0) {
      QueueEntry curr = queue.remove(0);
      if (visited.contains(curr.name)) {
        continue;
      }
      visited.add(curr.name);
      results.add(curr);
      
      if (judge != null) {
        if (judge.stopHere(curr.name)) {
          continue;
        }     
      }
      
      Set<FabricLink> outEdges = edgesFromSrc.get(curr.name);
      if (outEdges == null) {
        continue;
      }
      
      if (byDegree != null) {
      	HashSet<NID.WithName> fltrg = new HashSet<NID.WithName>();
      	for (FabricLink fl : outEdges) {
      		if (fl.isDirected()) {
      		  fltrg.add(fl.getTrgID());
      		} else {
      		  NID.WithName flSrc = fl.getSrcID();
      		  fltrg.add((curr.name.equals(flSrc)) ? fl.getTrgID() : flSrc);
      		}
      	}
        Iterator<NID.WithName> bdit = byDegree.iterator();
        while (bdit.hasNext()) { 
          NID.WithName trg = bdit.next();
          if (fltrg.contains(trg)) {
            queue.add(new QueueEntry(curr.depth + 1, trg));
          }
        }
        
      } else {
        Iterator<FabricLink> oit = outEdges.iterator();
        while (oit.hasNext()) { 
          FabricLink flink = oit.next();
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Contract the topo sort by moving nodes as far downstream as possible without
  ** breaking the partial ordering.
  */

  private void contractTopoSort(Map<NID.WithName, Integer> topoSort) {
    
    //
    // Make a list of nodes for each level.  Starting at the highest level,
    // get a node, and go through all the outbound links from that node.
    // Get the minimum level of all targets, and then move the node to
    // level min - 1.
    //
    // Iterate this process until no more changes can occur.
    //
    
    HashMap<Integer, List<NID.WithName>> nodesAtLevel = new HashMap<Integer, List<NID.WithName>>();
    int maxLevel = invertTopoSort(topoSort, nodesAtLevel);    
    
    if (maxLevel == -1) {  // nothing to do
      return;
    }
    
    Map<NID.WithName, Set<FabricLink>> outEdges = calcOutboundEdges(allEdges_);    
    
    while (true) {
      boolean changed = false;
      for (int i = maxLevel; i >= 0; i--) {
        List<NID.WithName> nodeList = nodesAtLevel.get(Integer.valueOf(i));
        List<NID.WithName> listCopy = new ArrayList<NID.WithName>(nodeList);
        int numNodes = nodeList.size();
        for (int j = 0; j < numNodes; j++) {
          NID.WithName currNode = listCopy.get(j);
          Set<FabricLink> linksForNode = outEdges.get(currNode);
          HashSet<NID.WithName> targsForNode = new HashSet<NID.WithName>();
          Iterator<FabricLink> lfnit = linksForNode.iterator();
          while (lfnit.hasNext()) {
            FabricLink link = lfnit.next();
            NID.WithName targ = link.getTrgID();
            targsForNode.add(targ);
          }
          int min = getMinLevel(targsForNode, topoSort, i, maxLevel);
          if (min > i + 1) {
            List<NID.WithName> higherNodeList = nodesAtLevel.get(Integer.valueOf(min - 1));
            higherNodeList.add(currNode);
            nodeList.remove(currNode);
            topoSort.put(currNode, Integer.valueOf(min - 1));
            changed = true;
          }
        }
      }
      if (!changed) {
        return;
      }
    }
  }  
  
  /***************************************************************************
  ** 
  ** Get the minimum level of all the target nodes
  */

  private int getMinLevel(Set<NID.WithName> targs, Map<NID.WithName, Integer> topoSort, int currLevel, int maxLevel) {
    if (targs == null) {
      return (currLevel);
    }
    int min = maxLevel;    
    Iterator<NID.WithName> trgit = targs.iterator();
    while (trgit.hasNext()) {
      NID.WithName trg = trgit.next();
      Integer level = topoSort.get(trg);
      int currLev = level.intValue();
      if (min > currLev) {
        min = currLev;
      }
    }
    return (min);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
}
