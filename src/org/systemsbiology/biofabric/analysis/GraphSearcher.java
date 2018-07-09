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

package org.systemsbiology.biofabric.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.modelAPI.NetLink;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.worker.AsynchExitRequestException;
import org.systemsbiology.biofabric.worker.BTProgressMonitor;

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
  private HashSet<NetLink> allEdges_;
  private ArrayList<NID.WithName> nodeOrder_;
  private ArrayList<NetLink> edgeOrder_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GraphSearcher(Set<NID.WithName> nodes, Set<NetLink> links) {
    allNodes_ = new HashSet<NID.WithName>(nodes);
    allEdges_ = new HashSet<NetLink>(links);
    edgeOrder_ = null;
    nodeOrder_ = null;
  }  
  
  /***************************************************************************
  **
  ** Constructor.  Used to create a depth-first order that 
  ** retains original sibling order.  If link appears multiple
  ** times, the order is based on first appearance.
  */

  public GraphSearcher(List<NID.WithName> nodes, List<NetLink> links) {

    allNodes_ = new HashSet<NID.WithName>(nodes);
    allEdges_ = new HashSet<NetLink>();
    edgeOrder_ = new ArrayList<NetLink>();
    nodeOrder_ = new ArrayList<NID.WithName>(nodes);

    for (NetLink link : links) {
      if (!allEdges_.contains(link)) {
        edgeOrder_.add(link);
      }
      allEdges_.add(link);
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

  public Map<NID.WithName, Integer> nodeDegree(boolean inOnly, BTProgressMonitor monitor) throws AsynchExitRequestException {
    return (nodeDegree(inOnly, allEdges_, monitor));
  }
  
  /***************************************************************************
  ** 
  ** Map of node degree. If inOnly == true, we only calculate in-degree. If false,
  ** the degree number for a node is a combined in/out degree. Both sources and
  ** targets are included.
  */

  public static Map<NID.WithName, Integer> nodeDegree(boolean inOnly, Set<NetLink> edges, 
                                                      BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    HashMap<NID.WithName, Integer> retval = new HashMap<NID.WithName, Integer>();

    for (NetLink link : edges) {
      NID.WithName src = link.getSrcID();
      NID.WithName trg = link.getTrgID();
      if (!inOnly) {
        Integer deg = retval.get(src);
        if (deg == null) {
          retval.put(src, Integer.valueOf(1));
        } else {
          retval.put(src, Integer.valueOf(deg.intValue() + 1));
        }
      }
      Integer deg = retval.get(trg);
      if (deg == null) {
        retval.put(trg, Integer.valueOf(1));
      } else {
        retval.put(trg, Integer.valueOf(deg.intValue() + 1));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of node degree
  */

  private SortedSet<NodeDegree> nodeDegreeSet(BTProgressMonitor monitor) throws AsynchExitRequestException {
    return (nodeDegreeSet(allEdges_, monitor));
  }
  
  /***************************************************************************
  ** 
  ** Nodes annotated by full degree as NodeDegree objects are ordered by degree.
  ** When equal degree, sorted by name:
  */

  private static SortedSet<NodeDegree> nodeDegreeSet(Set<NetLink> edges, 
                                                     BTProgressMonitor monitor) throws AsynchExitRequestException {
    TreeSet<NodeDegree> retval = new TreeSet<NodeDegree>();   
    // Map of node degree. Since the first argument is false,
    // the provided degree number is the combined in/out degree.
    Map<NID.WithName, Integer> nds = nodeDegree(false, edges, monitor);
    // Adding to sorted set orders by degree:
    for (NID.WithName node : nds.keySet()) {
      NodeDegree ndeg = new NodeDegree(node, nds.get(node).intValue());
      retval.add(ndeg);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorts the provided set of nodes by node degree, then name.
  */

  public static List<NID.WithName> nodesByDegree(Set<NID.WithName> nodes, SortedSet<NodeDegree> nds) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    
    Iterator<NodeDegree> li = nds.iterator();
    while (li.hasNext()) {
      NodeDegree nd = li.next();
      NID.WithName node = nd.getNodeID();
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

    for (NetLink nextLink : allEdges_) {
      NID.WithName trg = nextLink.getTrgID();
      Set<NID.WithName> trgSources = allSrcs.get(trg);
      if (trgSources == null) {
        trgSources = new HashSet<NID.WithName>();
        allSrcs.put(trg, trgSources);
      }
      trgSources.add(nextLink.getSrcID());
    } 
    
    TreeSet<SourcedNodeDegree> retval = new TreeSet<SourcedNodeDegree>();

    for (NID.WithName node : allSrcs.keySet()) {
      Set<NID.WithName> trgSources = allSrcs.get(node);
      SourcedNodeDegree ndeg = new SourcedNodeDegree(node, sourceOrder, trgSources);
      retval.add(ndeg);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of nodes via gray code
  */

  public SortedSet<SourcedNodeGray> nodeGraySetWithSource(List<NID.WithName> sourceOrder) {
    HashMap<NID.WithName, Set<NID.WithName>> allSrcs = new HashMap<NID.WithName, Set<NID.WithName>>();
    
    for (NetLink nextLink : allEdges_) {
      NID.WithName trg = nextLink.getTrgID();
      Set<NID.WithName> trgSources = allSrcs.get(trg);
      if (trgSources == null) {
        trgSources = new HashSet<NID.WithName>();
        allSrcs.put(trg, trgSources);
      }
      trgSources.add(nextLink.getSrcID());
    } 
    
    TreeSet<SourcedNodeGray> retval = new TreeSet<SourcedNodeGray>();

    for (NID.WithName node : allSrcs.keySet()) {
      Set<NID.WithName> trgSources = allSrcs.get(node);
      SourcedNodeGray ndeg = new SourcedNodeGray(node, sourceOrder, trgSources);
      retval.add(ndeg);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Sorted set of nodes via gray code
  */

  public static SortedSet<SourcedNodeGray> nodeGraySetWithSourceFromMap(List<NID.WithName> sourceOrder, 
                                                                        Map<NID.WithName, Set<NID.WithName>> srcsPerTarg) {
     
    TreeSet<SourcedNodeGray> retval = new TreeSet<SourcedNodeGray>();
    for (NID.WithName node : srcsPerTarg.keySet()) {
      SourcedNodeGray ndeg = new SourcedNodeGray(node, sourceOrder, srcsPerTarg.get(node));
      retval.add(ndeg);
    }
    return (retval);
  }
    
  /***************************************************************************
  ** 
  ** Sorted list of nodes, ordered by degree. 
  */

  public List<NID.WithName> nodeDegreeOrder(BTProgressMonitor monitor) throws AsynchExitRequestException {
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    SortedSet<NodeDegree> nds = nodeDegreeSet(monitor);
    for (NodeDegree ndeg : nds) {
      retval.add(ndeg.getNodeID());
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
    Set<NetLink> currentEdges = new HashSet<NetLink>();
    Iterator<NetLink> li = allEdges_.iterator();
    while (li.hasNext()) {
      NetLink link = li.next();
      currentEdges.add(((FabricLink)link).clone());
    }
      
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(currentEdges);
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
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(allEdges_); 

    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    if (edgeOrder_ != null) {
      HashSet<NID.WithName> seenRoots = new HashSet<NID.WithName>();
      for (NID.WithName currNode : nodeOrder_) {
        if (!rootNodes.contains(currNode)) {
          continue;
        }
        boolean gottaLink = false;
        for (NetLink link : edgeOrder_) {
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
      for (NID.WithName currNode : rootNodes) {
        searchGutsDepth(currNode, visited, outEdges, 0, null, retval);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  ** 
  ** Breadth-First Search, ordered by degree
  */

  public List<QueueEntry> breadthSearch(List<NID.WithName> startNodes, BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    List<NID.WithName> byDeg = nodeDegreeOrder(monitor);
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
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(allEdges_);    
       
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

  public List<QueueEntry> breadthSearch(boolean byDegree, List<NID.WithName> useRoots, 
                                        BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    if (edgeOrder_ != null) {
      throw new IllegalStateException();
    }
    
    List<NID.WithName> byDeg = null;
    if (byDegree) {
      byDeg = nodeDegreeOrder(monitor);
      Collections.reverse(byDeg);
    }   
    
    //
    // Do until roots are exhausted
    //
    HashSet<NID.WithName> visited = new HashSet<NID.WithName>();
    ArrayList<QueueEntry> queue = new ArrayList<QueueEntry>();
    List<QueueEntry> retval = new ArrayList<QueueEntry>();
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(allEdges_);    
   
    
    List<NID.WithName> rootNodes;
    if (useRoots == null) {
      rootNodes = new ArrayList<NID.WithName>(buildRootList(allNodes_, allEdges_));
    } else {
      rootNodes = useRoots;
    }
    
    
    Iterator<NID.WithName> rit = rootNodes.iterator();
    while (rit.hasNext()) {
      queue.add(new QueueEntry(0, rit.next()));
    }
  
    searchGutsBreadth(visited, queue, outEdges, retval, null, byDeg);
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
    
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(allEdges_);    
    
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
  
  public List<NID.WithName> topoSortToPartialOrdering(Map<NID.WithName, Integer> topoSort, Set<NetLink> allLinks,
                                                      BTProgressMonitor monitor) throws AsynchExitRequestException {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    TreeMap<Integer, List<NID.WithName>> invert = new TreeMap<Integer, List<NID.WithName>>();
    
    SortedSet<NodeDegree> nds = nodeDegreeSet(allLinks, monitor);
    
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
  
  /***************************************************************************
  ** 
  ** Prune edge list to only those from given source nodes
  */

  public List<NetLink> onlyLinksFromSources(List<NetLink> linkList, Set<NID.WithName> nodes) {    
    
    ArrayList<NetLink> retval = new ArrayList<NetLink>();
    int numLinks = linkList.size();
    for (int j = 0; j < numLinks; j++) {
      NetLink link = linkList.get(j);
      if (nodes.contains(link.getSrcID())) {
        retval.add(link);
      }
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

  public static Map<NID.WithName, Integer> topoSortReposition(Map<NID.WithName, Integer> origSort, 
                                                              NID.WithName moveID, boolean moveMin) {
    
    Integer origCol = origSort.get(moveID);
    if (origCol == null) {
      return (new HashMap<NID.WithName, Integer>(origSort));
    }
    
    int colVal = origCol.intValue();
    boolean colDup = false;
    int minVal = Integer.MAX_VALUE;
    int maxVal = Integer.MIN_VALUE;
    Iterator<NID.WithName> oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      NID.WithName key = oskit.next();
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
      
    HashMap<NID.WithName, Integer> retval = new HashMap<NID.WithName, Integer>();
  
    oskit = origSort.keySet().iterator();
    while (oskit.hasNext()) {
      NID.WithName key = oskit.next();
      if (key.equals(moveID)) {
        retval.put(moveID, Integer.valueOf(moveCol));
      } else {   
        Integer checkCol = origSort.get(key);
        int chekVal = checkCol.intValue();
        if ((chekVal >= minMove) && (chekVal <= maxMove)) {
          retval.put(key, Integer.valueOf(chekVal + inc));
        } else {
          retval.put(key, checkCol);
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<NID.WithName> topoSortToPartialOrderingByDegree(Map<NID.WithName, Integer> topoSort, Map<NID.WithName, Integer> degree) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    TreeMap<Integer, List<NID.WithName>> invert = new TreeMap<Integer, List<NID.WithName>>();
    
    invertTopoSort(topoSort, invert);    
    
    boolean first = true;
    Iterator<List<NID.WithName>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<NID.WithName> listForLevel = kit.next();
      if (first) {
        first = false;
        SortedSet<NodeDegree> ssnd = new TreeSet<NodeDegree>(Collections.reverseOrder());
        for (int i = 0; i < listForLevel.size(); i++) {
          NID.WithName node = listForLevel.get(i);
          ssnd.add(new NodeDegree(node, degree.get(node).intValue()));
        } 
        for (NodeDegree nd : ssnd) {
          retval.add(nd.getNodeID());
        }
      } else {
        HashSet<NID.WithName> snSortSet = new HashSet<NID.WithName>(retval);
        List<NetLink> justFromSrc = onlyLinksFromSources(new ArrayList<NetLink>(allEdges_), new HashSet<NID.WithName>(retval));
        ArrayList<NID.WithName> working = new ArrayList<NID.WithName>(retval);
        working.addAll(listForLevel);
        GraphSearcher gs = new GraphSearcher(working, justFromSrc); 
        SortedSet<GraphSearcher.SourcedNodeDegree> snds = gs.nodeDegreeSetWithSource(retval);
        Iterator<GraphSearcher.SourcedNodeDegree> dfit = snds.iterator();
        while (dfit.hasNext()) {
          GraphSearcher.SourcedNodeDegree node = dfit.next();
          if (!snSortSet.contains(node.getNode())) {
            retval.add(node.getNode());
          }
        } 
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Take a sort to a simple listing
  */
  
  public List<NID.WithName> topoSortToPartialOrderingByGray(Map<NID.WithName, Integer> topoSort, Map<NID.WithName, Integer> degree) {
    
    ArrayList<NID.WithName> retval = new ArrayList<NID.WithName>();
    TreeMap<Integer, List<NID.WithName>> invert = new TreeMap<Integer, List<NID.WithName>>();
    
    invertTopoSort(topoSort, invert);    
    
    boolean first = true;
    Iterator<List<NID.WithName>> kit = invert.values().iterator();
    while (kit.hasNext()) {
      List<NID.WithName> listForLevel = kit.next();
      if (first) {
        first = false;
        SortedSet<NodeDegree> ssnd = new TreeSet<NodeDegree>(Collections.reverseOrder());
        for (int i = 0; i < listForLevel.size(); i++) {
          NID.WithName node = listForLevel.get(i);
          ssnd.add(new NodeDegree(node, degree.get(node).intValue()));
        } 
        for (NodeDegree nd : ssnd) {
          retval.add(nd.getNodeID());
        }
      } else {
        HashSet<NID.WithName> snSortSet = new HashSet<NID.WithName>(retval);
        List<NetLink> justFromSrc = onlyLinksFromSources(new ArrayList<NetLink>(allEdges_), new HashSet<NID.WithName>(retval));
        ArrayList<NID.WithName> working = new ArrayList<NID.WithName>(retval);
        working.addAll(listForLevel);
        GraphSearcher gs = new GraphSearcher(working, justFromSrc); 
        SortedSet<GraphSearcher.SourcedNodeGray> snds = gs.nodeGraySetWithSource(retval);
        Iterator<GraphSearcher.SourcedNodeGray> dfit = snds.iterator();
        while (dfit.hasNext()) {
          GraphSearcher.SourcedNodeGray node = dfit.next();
          if (!snSortSet.contains(node.getNodeID())) {
            retval.add(node.getNodeID());
          }
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
    
    @Override
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
  ** Node annotated by degree. The comparator orders by degree. For equal
  ** degree, uses lexicographic node name order. 
  */
  
  public static class NodeDegree implements Comparable<NodeDegree> {
    
    private NID.WithName node_;
    private int degree_;
  
    public NodeDegree(NID.WithName node, int degree) {
      node_ = node;
      degree_ = degree;
    }
    
    public NID.WithName getNodeID() {
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
  ** Node annotated by degree and by sources. Ordering such that higher degree
  ** nodes some first, and equal degree nodes are ordered by order of source
  ** nodes.
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
      
      if (this.node_.equals(otherDeg.node_)) {
        return (0);
      }    
      
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
  
  /****************************************************************************
  **
  ** A Class
  */
  
  public static class SourcedNodeGray implements Comparable<SourcedNodeGray> {
    
    private NID.WithName node_;
    private int degree_;
    private char[] srcs_;
    private String binNum_;
    private BigInteger bi_;
    private BigInteger gray_;
    private List<NID.WithName> srcOrder_;
    
    public SourcedNodeGray(NID.WithName node, List<NID.WithName> srcOrder, Set<NID.WithName> mySrcs) {
      node_ = node;
      degree_ = mySrcs.size();
      int numSrc = srcOrder.size();
      srcOrder_ = new ArrayList<NID.WithName>(srcOrder);
      srcs_ = new char[numSrc];
      for (int i = 0; i < numSrc; i++) {
        srcs_[i] = (mySrcs.contains(srcOrder.get(i))) ? '1' : '0';
      }
      binNum_ = String.copyValueOf(srcs_);
   
      gray_ = new BigInteger(binNum_, 2);
      bi_ = gray_;
      BigInteger working = gray_;
      while (true) {
        working = working.shiftRight(1); // sign extension, but our numbers are always positive.
        if (working.equals(BigInteger.ZERO)) {
          break;
        }
        bi_ = bi_.xor(working);
      }
    }
      
    public NID.WithName getNodeID() {
      return (node_);
    }

    public String getBinNum() {
      return (binNum_);
    }    

    @Override
    public int hashCode() {
      return (node_.hashCode() + degree_);
    }
  
    @Override
    public String toString() {
      return (" node = " + node_ + " degree = " + degree_ + " gray_ = " + gray_.toString(2) + " bi = " + bi_.toString(2));
    }
    
    public String toStringAsSets() {
      StringBuffer ret = new StringBuffer();
      ret.append(node_.getName());
      ret.append(": ");
          
      boolean first = true;
      for (int i = 0; i < srcs_.length; i++) {
        if (srcs_[i] == '1') {
          if (!first) {
            ret.append(" + ");
          } else {
            first = false;
          }
          ret.append(srcOrder_.get(i));
        }
      }   
      return (ret.toString());
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNodeGray)) {
        return (false);
      }
      SourcedNodeGray otherDeg = (SourcedNodeGray)other;    
      if (this.degree_ != otherDeg.degree_) {
        return (false);
      }
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(SourcedNodeGray otherDeg) {
      int nameDiff = (this.node_.compareTo(otherDeg.node_));
      if (nameDiff == 0) {
        return (0);
      }
    
      int grayComp = -this.bi_.compareTo(otherDeg.bi_);
      if (grayComp != 0) {
        return (grayComp);
      }
  
      return (nameDiff);
    } 
  }
  
  /****************************************************************************
  **
  ** A Class
  */
  
  public static class SourcedNode implements Comparable<SourcedNode> {
    
    private NID.WithName node_;
    private int myIn_;
    private ArrayList<Integer> mySourceOrderList_;
   
    public SourcedNode(NID.WithName node, Map<NID.WithName, Integer> inDegs, 
                       Map<NID.WithName, Integer> nameToRow, Map<NID.WithName, Set<NID.WithName>> l2s) {
      node_ = node;
      TreeSet<Integer> myOrder = new TreeSet<Integer>(); 
      Set<NID.WithName> mySet = l2s.get(node_);
      for (NID.WithName nextNode : mySet) {
        Integer nextSourceRow = nameToRow.get(nextNode);
        myOrder.add(nextSourceRow);       
      }
      mySourceOrderList_ = new ArrayList<Integer>(myOrder);
      myIn_ = inDegs.get(node_).intValue();
    }
    
    public NID.WithName getNode() {
      return (node_);
    }
    
    @Override
    public int hashCode() {
      return (node_.hashCode());
    }
  
    @Override
    public String toString() {
      return (" node = " + node_);
    }
    
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNode)) {
        return (false);
      }
      SourcedNode otherDeg = (SourcedNode)other;    
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }
    
    public int compareTo(SourcedNode otherDeg) {
      
      //
      // Same name, same node:
      //
      
      if (this.node_.equals(otherDeg.node_)) {
        return (0);
      }
          
      int mySize = this.mySourceOrderList_.size();
      int hisSize = otherDeg.mySourceOrderList_.size();
      int min = Math.min(mySize, hisSize);
      for (int i = 0; i < min; i++) {
        int myVal = this.mySourceOrderList_.get(i).intValue();
        int hisVal = otherDeg.mySourceOrderList_.get(i).intValue();
        int diff = hisVal - myVal;
        if (diff != 0) {
          return (diff);
        }
      }
      
      int diffSize = hisSize - mySize;
      if (diffSize != 0) {
        return (diffSize);
      }
      
      int diffIn = this.myIn_ - otherDeg.myIn_;
      if (diffIn != 0) {
        return (diffIn);
      }
      
      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    } 
  }
  
  /****************************************************************************
  **
  ** A class that allows us to sort nodes based on input order
  */

  public static class MMDSourcedNode implements Comparable<MMDSourcedNode> {

    private NID.WithName node_;
    private int myIn_;
    private ArrayList<Integer> mySourceOrderList_;
   
    public MMDSourcedNode(NID.WithName node, Map<NID.WithName, Integer> inDegs, 
                          List<NID.WithName> placeList, Map<NID.WithName, Set<NID.WithName>> l2s) {
      node_ = node;
      Set<NID.WithName> mySet = l2s.get(node_);
      TreeSet<Integer> mySourceOrder = new TreeSet<Integer>();
      int numNode = placeList.size();
      for (int i = 0; i < numNode; i++) {
        NID.WithName nodex = placeList.get(i);
        if (mySet.contains(nodex)) {
          mySourceOrder.add(Integer.valueOf(i));
        }
      }
      mySourceOrderList_ = new ArrayList<Integer>();
      myIn_ = inDegs.get(this.node_).intValue();
    }

    public NID.WithName getNode() {
      return (node_);
    }

    @Override
    public int hashCode() {
      return (node_.hashCode());
    }

    @Override
    public String toString() {
      return (" node = " + node_);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNode)) {
        return (false);
      }
      MMDSourcedNode otherDeg = (MMDSourcedNode)other;
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }

    public int compareTo(MMDSourcedNode otherDeg) {

      //
      // Same name, same node:
      //

      if (this.node_.equals(otherDeg.node_)) {
        return (0);
      }

      int mySize = this.mySourceOrderList_.size();
      int hisSize = otherDeg.mySourceOrderList_.size();
      int min = Math.min(mySize, hisSize);
      for (int i = 0; i < min; i++) {
        int myVal = this.mySourceOrderList_.get(i).intValue();
        int hisVal = otherDeg.mySourceOrderList_.get(i).intValue();
        int diff = hisVal - myVal;
        if (diff != 0) {
          return (diff);
        }
      }

      int diffSize = hisSize - mySize;
      if (diffSize != 0) {
        return (diffSize);
      }

      int myIn = this.myIn_;
      int hisIn = otherDeg.myIn_;
      int diffIn = myIn - hisIn;
      if (diffIn != 0) {
        return (diffIn);
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

  private Map<NID.WithName, Set<NetLink>> calcOutboundEdges(Set<NetLink> edges) {
    
    HashMap<NID.WithName, Set<NetLink>> retval = new HashMap<NID.WithName, Set<NetLink>>();
    Iterator<NetLink> li = edges.iterator();

    while (li.hasNext()) {
      NetLink link = li.next();
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

  private void addaLink(NetLink link, NID.WithName bin, Map<NID.WithName, Set<NetLink>> collect) {
    Set<NetLink> forBin = collect.get(bin);
    if (forBin == null) {
      forBin = new HashSet<NetLink>();
      collect.put(bin, forBin);
    }
    forBin.add(link);
    return;
  }
  
  /***************************************************************************
  **
  ** Build a root list
  */

  private Set<NID.WithName> buildRootList(Set<NID.WithName> nodes, Set<NetLink> edges) {
  
    HashSet<NID.WithName> retval = new HashSet<NID.WithName>();
    retval.addAll(nodes);
    
    Iterator<NetLink> ei = edges.iterator();
    while (ei.hasNext()) {
      NetLink link = ei.next();
      NID.WithName trg = link.getTrgID();
      retval.remove(trg);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Invert
  */

  private Set<NetLink> invertOutboundEdges(Map<NID.WithName, Set<NetLink>> outEdges) {
    
    HashSet<NetLink> retval = new HashSet<NetLink>();
    Iterator<NID.WithName> ki = outEdges.keySet().iterator();

    while (ki.hasNext()) {
      NID.WithName src = ki.next();
      Set<NetLink> links = outEdges.get(src);
      Iterator<NetLink> sit = links.iterator();
      while (sit.hasNext()) {
        NetLink lnk = sit.next();
        if (lnk.isFeedback()) {
          retval.add(((FabricLink)lnk).clone());
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
  		                         Map<NID.WithName, Set<NetLink>> edgesFromSrc,
                               int depth, List<NetLink> edgeOrder, List<QueueEntry> results) {

    if (visited.contains(vertexID)) {
      return;
    }
    visited.add(vertexID);
    results.add(new QueueEntry(depth, vertexID));
    Set<NetLink> outEdges = edgesFromSrc.get(vertexID);
    if (outEdges == null) {
      return;
    }
    
    if (edgeOrder != null) {
      Iterator<NetLink> eit = edgeOrder.iterator();
      while (eit.hasNext()) {
        NetLink link = eit.next();
        if (!vertexID.equals(link.getSrcID())) {
          continue;
        }
        NID.WithName targ = link.getTrgID();
        if (!visited.contains(targ)) {
          searchGutsDepth(targ, visited, edgesFromSrc, depth + 1, edgeOrder, results);
        }
      }
    } else {
      Iterator<NetLink> eit = outEdges.iterator();
      while (eit.hasNext()) {
        NetLink link = eit.next();
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

  private void searchGutsBreadth(HashSet<NID.WithName> visited, ArrayList<QueueEntry> queue, Map<NID.WithName, Set<NetLink>> edgesFromSrc, 
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
      
      Set<NetLink> outEdges = edgesFromSrc.get(curr.name);
      if (outEdges == null) {
        continue;
      }
      
      if (byDegree != null) {
      	HashSet<NID.WithName> fltrg = new HashSet<NID.WithName>();
      	for (NetLink fl : outEdges) {
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
        Iterator<NetLink> oit = outEdges.iterator();
        while (oit.hasNext()) { 
          NetLink flink = oit.next();
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
    
    Map<NID.WithName, Set<NetLink>> outEdges = calcOutboundEdges(allEdges_);    
    
    while (true) {
      boolean changed = false;
      for (int i = maxLevel; i >= 0; i--) {
        List<NID.WithName> nodeList = nodesAtLevel.get(Integer.valueOf(i));
        List<NID.WithName> listCopy = new ArrayList<NID.WithName>(nodeList);
        int numNodes = nodeList.size();
        for (int j = 0; j < numNodes; j++) {
          NID.WithName currNode = listCopy.get(j);
          Set<NetLink> linksForNode = outEdges.get(currNode);
          HashSet<NID.WithName> targsForNode = new HashSet<NID.WithName>();
          Iterator<NetLink> lfnit = linksForNode.iterator();
          while (lfnit.hasNext()) {
            NetLink link = lfnit.next();
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
