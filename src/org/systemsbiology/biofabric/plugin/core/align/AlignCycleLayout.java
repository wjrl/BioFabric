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

package org.systemsbiology.biofabric.plugin.core.align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.layouts.NodeLayout;
import org.systemsbiology.biofabric.model.BuildData;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This is the default layout algorithm
*/

public class AlignCycleLayout extends NodeLayout {
  
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

  public AlignCycleLayout() {

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
  
  public List<NID.WithName> doNodeLayout(BuildData.RelayoutBuildData rbd, 
  		                                   Params params,
  		                                   BTProgressMonitor monitor) throws AsynchExitRequestException {
      
    List<NID.WithName> targetIDs = doNodeOrder(rbd, params, monitor);

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    installNodeOrder(targetIDs, rbd, monitor);
    return (targetIDs);
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NID.WithName> doNodeOrder(BuildData.RelayoutBuildData rbd, 
                                        Params params,
                                        BTProgressMonitor monitor) throws AsynchExitRequestException {
      
    List<NID.WithName> startNodeIDs = (params == null) ? null : ((DefaultParams)params).startNodes;
    
    NetworkAlignmentBuildData narbd = (NetworkAlignmentBuildData)rbd;
    
    Map<String, String> normal = normalizeAlignMap(narbd.mapG1toG2);
    
    Set<NID.WithName> allNodes = genAllNodes(narbd);
    Map<NID.WithName, String> nodesToPathElem = genNodeToPathElem(allNodes);
    Map<String, NID.WithName> pathElemToNode = genPathElemToNode(allNodes); 
    Map<String, AlignPath> alignPaths = calcAlignPaths(normal);
    List<NID.WithName[]> cycleBounds = new ArrayList<NID.WithName[]>();
        
    List<NID.WithName> targetIDs = alignPathNodeOrder(rbd.allLinks, rbd.loneNodeIDs, 
                                                      startNodeIDs, alignPaths, 
                                                      nodesToPathElem,
                                                      pathElemToNode,
                                                      cycleBounds,
                                                      monitor);
    narbd.cycleBounds = cycleBounds;
    return (targetIDs);
  }
  
  /***************************************************************************
  ** 
  ** Calculate alignPath node order.
  */

  private List<NID.WithName> alignPathNodeOrder(Set<FabricLink> allLinks,
  		                                          Set<NID.WithName> loneNodes, 
  		                                          List<NID.WithName> startNodes,
  		                                          Map<String, AlignPath> alignPaths,
  		                                          Map<NID.WithName, String> nodesToPathElem,
  		                                          Map<String, NID.WithName> pathElemToNode,
  		                                          List<NID.WithName[]> cycleBounds,
  		                                          BTProgressMonitor monitor) throws AsynchExitRequestException { 
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
    // NOTE! If we are handed a perfect alignment, we can build cycles even if the network is not aligned
    // to itself!
    //
    // We are handed a data structure that points from each aligned node to the alignment cycle it belongs to.
    // Working with a start node (highest degree or from list), order neighbors by decreasing degree, but instead
    // adding unseen nodes in that order to the queue, we add ALL the nodes in the cycle to the list, in cycle order.
    // Choice of doing straight cycle order, or alternating order. If it is a true cycle (i.e. does not terminate in
    // an unaligned node) we can start the cycle at the neighbor. If it is not a cycle but a path, we need to start
    // at the beginning.
    // 
    
    HashMap<NID.WithName, Integer> linkCounts = new HashMap<NID.WithName, Integer>();
    HashMap<NID.WithName, Set<NID.WithName>> targsPerSource = new HashMap<NID.WithName, Set<NID.WithName>>();
    ArrayList<NID.WithName> targets = new ArrayList<NID.WithName>();
         
    HashSet<NID.WithName> targsToGo = new HashSet<NID.WithName>();
    
    int numLink = allLinks.size();
    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
    
    Iterator<FabricLink> alit = allLinks.iterator();
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
    // Rank the nodes by link count:
    //
    
    lr = new LoopReporter(linkCounts.size(), 20, monitor, 0.25, 0.50, "progress.rankByDegree");
    
    TreeMap<Integer, SortedSet<NID.WithName>> countRank = new TreeMap<Integer, SortedSet<NID.WithName>>(Collections.reverseOrder());
    Iterator<NID.WithName> lcit = linkCounts.keySet().iterator();
    while (lcit.hasNext()) {
      NID.WithName src = lcit.next();
      lr.report();
      Integer count = linkCounts.get(src);
      SortedSet<NID.WithName> perCount = countRank.get(count);
      if (perCount == null) {
        perCount = new TreeSet<NID.WithName>();
        countRank.put(count, perCount);
      }
      perCount.add(src);
    }
    lr.finish();
    
    //
    // Handle the specified starting nodes case:
    //
    
    if ((startNodes != null) && !startNodes.isEmpty()) {
      ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
      targsToGo.removeAll(startNodes);
      targets.addAll(startNodes);
      queue.addAll(startNodes);
      flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, 
                 alignPaths, nodesToPathElem, pathElemToNode, cycleBounds, monitor, 0.50, 0.75);
    }   
    
    //
    // Get all kids added in.  Now doing this without recursion; seeing blown
    // stacks for huge networks!
    //
     
    while (!targsToGo.isEmpty()) {
      Iterator<Integer> crit = countRank.keySet().iterator();
      while (crit.hasNext()) {
        Integer key = crit.next();
        SortedSet<NID.WithName> perCount = countRank.get(key);
        Iterator<NID.WithName> pcit = perCount.iterator();
        while (pcit.hasNext()) {
          NID.WithName node = pcit.next();
          if (targsToGo.contains(node)) {
            ArrayList<NID.WithName> queue = new ArrayList<NID.WithName>();
            targsToGo.remove(node);
            targets.add(node);
            addMyKidsNR(targets, targsPerSource, linkCounts, targsToGo, 
                        node, queue, alignPaths, nodesToPathElem,
                        pathElemToNode, cycleBounds, monitor, 0.75, 1.0);
          }
        }
      }
    }
    
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<NID.WithName> remains = new HashSet<NID.WithName>(loneNodes);
    // If we have a huge number of lone nodes, the removeAll() set operation is
    // taking FOREVER, e.g. remains 190804 targets 281832. Use different approach?
    System.err.println("remains " + remains.size() + " targets " + targets.size());
    remains.removeAll(targets);
    System.err.println("remains now " + remains.size());
    targets.addAll(new TreeSet<NID.WithName>(remains));
    return (targets);
  }
        
  /***************************************************************************
  **
  ** Node ordering
  */
  
  private List<NID.WithName> orderMyKids(Map<NID.WithName, Set<NID.WithName>> targsPerSource, 
  		                                   Map<NID.WithName, Integer> linkCounts, 
                                         Set<NID.WithName> targsToGo, NID.WithName node) {
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
  ** Node ordering, non-recursive:
  */
  
  private void addMyKidsNR(List<NID.WithName> targets, Map<NID.WithName, Set<NID.WithName>> targsPerSource, 
                           Map<NID.WithName, Integer> linkCounts, 
                           Set<NID.WithName> targsToGo, NID.WithName node, List<NID.WithName> queue,
                           Map<String, AlignPath> alignPaths,
                           Map<NID.WithName, String> nodesToPathElem,
                           Map<String, NID.WithName> pathElemToNode,
                           List<NID.WithName[]> cycleBounds,
                           BTProgressMonitor monitor, double startFrac, double endFrac) 
                          	 throws AsynchExitRequestException {
    queue.add(node);
    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, alignPaths, nodesToPathElem,
               pathElemToNode, cycleBounds, monitor, startFrac, endFrac);
    return;
  }
  
  /***************************************************************************
  **
  ** Node ordering, non-recursive:
  */
  
  private void flushQueue(List<NID.WithName> targets, 
  		                    Map<NID.WithName, Set<NID.WithName>> targsPerSource, 
                          Map<NID.WithName, Integer> linkCounts, 
                          Set<NID.WithName> targsToGo, List<NID.WithName> queue,
                          Map<String, AlignPath> alignPaths,
                          Map<NID.WithName, String> nodesToPathElem,
                          Map<String, NID.WithName> pathElemToNode,
                          List<NID.WithName[]> cycleBounds,
                          BTProgressMonitor monitor, double startFrac, double endFrac) 
                            throws AsynchExitRequestException {
  	
  	LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
  	int lastSize = targsToGo.size();	
    while (!queue.isEmpty()) {
      NID.WithName node = queue.remove(0);
      int ttgSize = targsToGo.size();
      lr.report(lastSize - ttgSize);
      lastSize = ttgSize;
      List<NID.WithName> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      Iterator<NID.WithName> ktpit = myKids.iterator(); 
      while (ktpit.hasNext()) {  
        NID.WithName kid = ktpit.next();
        if (targsToGo.contains(kid)) {
          // here we add the entire cycle containing the kid. If kid is not in a cycle (unaligned), we just add
          // kid. If not a cycle but a path, we add the first kid in the path, then all following kids. If a cycle,
          // we start with the kid, and loop back around to the kid in front of us.
          String kidKey = nodesToPathElem.get(kid);
          AlignPath ac = alignPaths.get(kidKey);
          if (ac == null) {
            targsToGo.remove(kid);
            targets.add(kid);
            queue.add(kid);    
          } else {
            targsToGo.removeAll(ac.pathNodes);
            List<String> unlooped = ac.getReorderedKidsStartingAtKidOrStart(kidKey);
            for (String ulnode : unlooped) { 
              NID.WithName daNode = pathElemToNode.get(ulnode);
              targsToGo.remove(daNode);
              targets.add(daNode);
              
              queue.add(daNode);
            }
            NID.WithName[] bounds = new NID.WithName[2];
            cycleBounds.add(bounds);
            bounds[0] = pathElemToNode.get(unlooped.get(0));
            bounds[1] = pathElemToNode.get(unlooped.get(unlooped.size() - 1));
          }
        }
      }
    }
    lr.finish();
    return;
  }

  /***************************************************************************
  **
  ** In the alignment map we are provided, nodes in G1 and G2 that have the same
  ** name have different OIDs. Eliminate this difference.
  */
  
  private Map<String, String> normalizeAlignMap(Map<NID.WithName, NID.WithName> align) { 

     Map<String, String> nameToName = new HashMap<String, String>();
     
     for (NID.WithName key : align.keySet()) {
       NID.WithName matchNode = align.get(key);
       // Gotta be unique:
       if (nameToName.containsKey(key)) {
         throw new IllegalStateException();
       }
       nameToName.put(key.getName(), matchNode.getName());
     }
     return (nameToName);
   } 
  
  /***************************************************************************
  **
  ** Get all nodes
  */
  
  private Set<NID.WithName> genAllNodes(NetworkAlignmentBuildData narbd) { 

    Set<NID.WithName> allNodes = new HashSet<NID.WithName>();
     
     for (FabricLink link : narbd.allLinks) {
       allNodes.add(link.getSrcID());
       allNodes.add(link.getTrgID());
     }
     allNodes.addAll(narbd.loneNodeIDs);
     return (allNodes);
   } 
   
  /***************************************************************************
  **
  ** Get map from network NID.WithName (of form G1::G2) to path elem (G1)
  */
  
  private Map<NID.WithName, String> genNodeToPathElem(Set<NID.WithName> allNodes) { 

     Map<NID.WithName, String> n2pe = new HashMap<NID.WithName, String>();
     
     for (NID.WithName key : allNodes) {
       String[] toks = key.getName().split("::");
       if (toks.length == 2) {
         n2pe.put(key, toks[0]);
       }
     }
     return (n2pe);
   } 
  
  
  /***************************************************************************
  **
  ** Inverse of above
  */
  
  private Map<String, NID.WithName> genPathElemToNode(Set<NID.WithName> allNodes) { 

    Map<String, NID.WithName> pe2n = new HashMap<String, NID.WithName>();
    
    for (NID.WithName key : allNodes) {
      String[] toks = key.getName().split("::");
      if (toks.length == 2) {
        pe2n.put(toks[0], key);
      }
    }
    return (pe2n);
  } 
  
  
  /***************************************************************************
  **
  ** Extract the paths in the alignment network:
  */
  
  private Map<String, AlignPath> calcAlignPaths(Map<String, String> align) { 

     Map<String, AlignPath> pathsPerStart = new HashMap<String, AlignPath>();
     
     HashSet<String> working = new HashSet<String>(align.keySet());
     while (!working.isEmpty()) {
       String startKey = working.iterator().next();
       working.remove(startKey);
       AlignPath path = new AlignPath();
       pathsPerStart.put(startKey, path);
       path.pathNodes.add(startKey);
       String nextKey = align.get(startKey);
       // Not every cycle closes itself, so nextKey can == null:
       while (nextKey != null) {
         if (nextKey.equals(startKey)) {
           path.isCycle = true;
           break;
         }
         AlignPath existing = pathsPerStart.get(nextKey);
         if (existing != null) {
           path.pathNodes.addAll(existing.pathNodes);
           pathsPerStart.remove(nextKey);
           if (working.contains(nextKey)) {
             throw new IllegalStateException();
           }
           if (existing.isCycle) {
             throw new IllegalStateException();
           }
           break;
         } else {
           path.pathNodes.add(nextKey);
           working.remove(nextKey);
           nextKey = align.get(nextKey);
         }
       }
     }
     Map<String, AlignPath> pathsPerEveryNode = new HashMap<String, AlignPath>();
     for (String keyName : pathsPerStart.keySet()) {
       AlignPath path = pathsPerStart.get(keyName);
       for (String nextName : path.pathNodes) {
         pathsPerEveryNode.put(nextName, path); 
       }
     }
     return (pathsPerEveryNode);
   }


  
  /***************************************************************************
  **
  ** For passing around layout path
  */  
  
  private static class AlignPath  {
        
    List<String> pathNodes;
    boolean isCycle;

    AlignPath() {
      pathNodes = new ArrayList<String>();
      isCycle = false;
    } 
    
    List<String> getReorderedKidsStartingAtKidOrStart(String start) {
      // If we have a loop, unroll it starting at the provided start:
      if (isCycle) {
        int startIndex = pathNodes.indexOf(start);
        int len = pathNodes.size();
        List<String> retval = new ArrayList<String>();
        for (int i = 0; i < len; i++) {
          int index = (startIndex + i) % len;
          retval.add(pathNodes.get(index));
        }
        return (retval);
      } else {
        return (pathNodes);
      }
    }
  }
 
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  public static class DefaultParams implements Params {
        
    public List<NID.WithName> startNodes;

    public DefaultParams(List<NID.WithName> startNodes) {
      this.startNodes = startNodes;
    } 
  }

}
