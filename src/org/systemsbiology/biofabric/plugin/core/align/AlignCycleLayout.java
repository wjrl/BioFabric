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

import org.systemsbiology.biofabric.api.io.BuildData;
import org.systemsbiology.biofabric.api.layout.LayoutCriterionFailureException;
import org.systemsbiology.biofabric.api.layout.NodeLayout;
import org.systemsbiology.biofabric.api.model.NetLink;
import org.systemsbiology.biofabric.api.model.NetNode;
import org.systemsbiology.biofabric.api.worker.AsynchExitRequestException;
import org.systemsbiology.biofabric.api.worker.BTProgressMonitor;
import org.systemsbiology.biofabric.api.worker.LoopReporter;

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
  
  private NodeMaps maps_;
    
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
    maps_ = null;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Find out if the necessary conditions for this layout are met. For this layout, we either
  ** need to have the source and target nodes of the alignment in the same namespace, or a
  ** perfect alignment file to map the two namespaces.
  ** If you reuse the same object, this layout will cache the calculation it uses to answer
  ** the question for the actual layout!
  */
  
  @Override
  public boolean criteriaMet(BuildData rbd,
                             BTProgressMonitor monitor) throws AsynchExitRequestException, 
                                                               LayoutCriterionFailureException {
    
    NetworkAlignmentBuildData narbd = (NetworkAlignmentBuildData)rbd.getPluginBuildData();
    maps_ = normalizeAlignMap(narbd.mapG1toG2, narbd.perfectG1toG2, narbd.allLargerNodes, 
                              narbd.allSmallerNodes, monitor);
    if (maps_ == null) {
      throw new LayoutCriterionFailureException();
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** If you reuse the same object, this layout will cache the calculation it uses to answer
  ** the question for the actual layout. If you need to clear the cache, use this:
  */
  
  public void clearCache() {
    maps_ = null;
    return;
  }
  
   
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<NetNode> doNodeLayout(BuildData rbd, 
  		                              Params params,
  		                              BTProgressMonitor monitor) throws AsynchExitRequestException {
      
    List<NetNode> targetIDs = doNodeOrder(rbd, params, monitor);

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
  
  public List<NetNode> doNodeOrder(BuildData rbd, 
                                   Params params,
                                   BTProgressMonitor monitor) throws AsynchExitRequestException {
      
    
    //
    // The actual start node might be different since we unroll paths to find the first node.
    // Thus, skip allowing the user to mess with this for now.
    //
    
    List<NetNode> startNodeIDs = null;
    
    NetworkAlignmentBuildData narbd = (NetworkAlignmentBuildData)rbd.getPluginBuildData();
    
    if (maps_ == null) {
      maps_ = normalizeAlignMap(narbd.mapG1toG2, narbd.perfectG1toG2, narbd.allLargerNodes, 
                                narbd.allSmallerNodes, monitor);
    }
    
    Set<NetNode> allNodes = genAllNodes(rbd);
    Map<NetNode, String> nodesToPathElem = genNodeToPathElem(allNodes);
    Map<String, NetNode> pathElemToNode = genPathElemToNode(allNodes); 
    Map<String, AlignPath> alignPaths = calcAlignPaths(maps_);
    List<CycleBounds> cycleBounds = new ArrayList<CycleBounds>();
        
    List<NetNode> targetIDs = alignPathNodeOrder(rbd.getLinks(), rbd.getSingletonNodes(), 
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

  private List<NetNode> alignPathNodeOrder(Set<NetLink> allLinks,
  		                                          Set<NetNode> loneNodes, 
  		                                          List<NetNode> startNodes,
  		                                          Map<String, AlignPath> alignPaths,
  		                                          Map<NetNode, String> nodesToPathElem,
  		                                          Map<String, NetNode> pathElemToNode,
  		                                          List<CycleBounds> cycleBounds,
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
    // If it is a true cycle (i.e. does not terminate in an unaligned node) we can start the cycle at the neighbor. 
    // If it is not a cycle but a path, we need to start at the beginning.
    //
    // PLUS, since the start node of a path is unaligned, the unaligned node gets stuck at the front,
    
    // 
    
    HashMap<NetNode, Integer> linkCounts = new HashMap<NetNode, Integer>();
    HashMap<NetNode, Set<NetNode>> targsPerSource = new HashMap<NetNode, Set<NetNode>>();
    ArrayList<NetNode> targets = new ArrayList<NetNode>();
         
    HashSet<NetNode> targsToGo = new HashSet<NetNode>();
    
    int numLink = allLinks.size();
    LoopReporter lr = new LoopReporter(numLink, 20, monitor, 0.0, 0.25, "progress.calculateNodeDegree");
    
    Iterator<NetLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      NetLink nextLink = alit.next();
      lr.report();
      NetNode sidwn = nextLink.getSrcNode();
      NetNode tidwn = nextLink.getTrgNode();
      Set<NetNode> targs = targsPerSource.get(sidwn);
      if (targs == null) {
        targs = new HashSet<NetNode>();
        targsPerSource.put(sidwn, targs);
      }
      targs.add(tidwn);
      targs = targsPerSource.get(tidwn);
      if (targs == null) {
        targs = new HashSet<NetNode>();
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
    
    TreeMap<Integer, SortedSet<NetNode>> countRank = new TreeMap<Integer, SortedSet<NetNode>>(Collections.reverseOrder());
    Iterator<NetNode> lcit = linkCounts.keySet().iterator();
    while (lcit.hasNext()) {
      NetNode src = lcit.next();
      lr.report();
      Integer count = linkCounts.get(src);
      SortedSet<NetNode> perCount = countRank.get(count);
      if (perCount == null) {
        perCount = new TreeSet<NetNode>();
        countRank.put(count, perCount);
      }
      perCount.add(src);
    }
    lr.finish();
    
    //
    // Get all kids added in.  Now doing this without recursion; seeing blown
    // stacks for huge networks!
    //
     
    while (!targsToGo.isEmpty()) {
      Iterator<Integer> crit = countRank.keySet().iterator();
      while (crit.hasNext()) {
        Integer key = crit.next();
        SortedSet<NetNode> perCount = countRank.get(key);
        Iterator<NetNode> pcit = perCount.iterator();
        while (pcit.hasNext()) {
          NetNode node = pcit.next();
          if (targsToGo.contains(node)) {
            String nodeKey = nodesToPathElem.get(node);
            AlignPath ac = alignPaths.get(nodeKey);
            ArrayList<NetNode> queue = new ArrayList<NetNode>();
            if (ac == null) {
              targsToGo.remove(node);
              targets.add(node);
              queue.add(node); 
            } else {
              List<String> unlooped = ac.getReorderedKidsStartingAtKidOrStart(nodeKey);
              for (String ulnode : unlooped) { 
                NetNode daNode = pathElemToNode.get(ulnode);
                targsToGo.remove(daNode);
                targets.add(daNode);
                queue.add(daNode);
              }
              NetNode boundsStart = pathElemToNode.get(unlooped.get(0));
              NetNode boundsEnd = pathElemToNode.get(unlooped.get(unlooped.size() - 1));
              cycleBounds.add(new CycleBounds(boundsStart, boundsEnd, ac.correct, ac.isCycle));
            }
            flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue, alignPaths, nodesToPathElem,
                       pathElemToNode, cycleBounds, monitor,  0.75, 1.0);
          }
        }
      }
    }
    
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it.
    //
    // Used to do a set removeAll() operation, but discovered the operation was
    // taking FOREVER, e.g. remains 190804 targets 281832. So do this in a loop
    // that can be monitored for progress:
    //
    
    LoopReporter lr2 = new LoopReporter(loneNodes.size(), 20, monitor, 0.0, 0.25, "progress.addSingletonsToTargets");
    HashSet<NetNode> targSet = new HashSet<NetNode>(targets);
    TreeSet<NetNode> remains = new TreeSet<NetNode>();
    
    for (NetNode lnod : loneNodes) {
    	if (!targSet.contains(lnod)) {
    		lr2.report();
    		remains.add(lnod); 		
    	}    	
    }
    lr2.finish();
    targets.addAll(remains);
       
    return (targets);
  }
        
  /***************************************************************************
  **
  ** Node ordering
  */
  
  private List<NetNode> orderMyKids(Map<NetNode, Set<NetNode>> targsPerSource, 
  		                                   Map<NetNode, Integer> linkCounts, 
                                         Set<NetNode> targsToGo, NetNode node) {
    Set<NetNode> targs = targsPerSource.get(node);
    if (targs == null) {
    	return (new ArrayList<NetNode>());
    }
    TreeMap<Integer, SortedSet<NetNode>> kidMap = new TreeMap<Integer, SortedSet<NetNode>>(Collections.reverseOrder());
    Iterator<NetNode> tait = targs.iterator();
    while (tait.hasNext()) {  
      NetNode nextTarg = tait.next(); 
      Integer count = linkCounts.get(nextTarg);
      SortedSet<NetNode> perCount = kidMap.get(count);
      if (perCount == null) {
        perCount = new TreeSet<NetNode>();
        kidMap.put(count, perCount);
      }
      perCount.add(nextTarg);
    }
    
    ArrayList<NetNode> myKidsToProc = new ArrayList<NetNode>();
    Iterator<SortedSet<NetNode>> kmit = kidMap.values().iterator();
    while (kmit.hasNext()) {  
      SortedSet<NetNode> perCount = kmit.next(); 
      Iterator<NetNode> pcit = perCount.iterator();
      while (pcit.hasNext()) {  
        NetNode kid = pcit.next();
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
  
  private void flushQueue(List<NetNode> targets, 
  		                    Map<NetNode, Set<NetNode>> targsPerSource, 
                          Map<NetNode, Integer> linkCounts, 
                          Set<NetNode> targsToGo, List<NetNode> queue,
                          Map<String, AlignPath> alignPaths,
                          Map<NetNode, String> nodesToPathElem,
                          Map<String, NetNode> pathElemToNode,
                          List<CycleBounds> cycleBounds,
                          BTProgressMonitor monitor, double startFrac, double endFrac) 
                            throws AsynchExitRequestException {
  	
  	LoopReporter lr = new LoopReporter(targsToGo.size(), 20, monitor, startFrac, endFrac, "progress.nodeOrdering");
  	int lastSize = targsToGo.size();	
    while (!queue.isEmpty()) {
      NetNode node = queue.remove(0);
      int ttgSize = targsToGo.size();
      lr.report(lastSize - ttgSize);
      lastSize = ttgSize;
      List<NetNode> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      Iterator<NetNode> ktpit = myKids.iterator(); 
      while (ktpit.hasNext()) {  
        NetNode kid = ktpit.next();
        if (targsToGo.contains(kid)) {
          // here we add the entire cycle containing the kid. If kid is not in a cycle (unaligned), we just add
          // kid. If not a cycle but a path, we add the first kid in the path, then all following kids. If a cycle,
          // we start with the kid, and loop back around to the kid in front of us.
          // nodesToPathElem is of form NetNode(G1:G2) -> "G1"
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
              NetNode daNode = pathElemToNode.get(ulnode);
              targsToGo.remove(daNode);
              targets.add(daNode);
              queue.add(daNode);
            }
            
            NetNode boundsStart = pathElemToNode.get(unlooped.get(0));
            NetNode boundsEnd = pathElemToNode.get(unlooped.get(unlooped.size() - 1));
            cycleBounds.add(new CycleBounds(boundsStart, boundsEnd, ac.correct, ac.isCycle));
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
  ** name have different OIDs. Eliminate this difference. Also, if the source and
  ** target are not in the same namespace, we need to use the perfect alignment 
  ** (if available) to create the cycle/path map. 
  */
  
  private NodeMaps normalizeAlignMap(Map<NetNode, NetNode> align, 
                                     Map<NetNode, NetNode> perfectAlign,
                                     Set<NetNode> allLargerNodes,
                                     Set<NetNode> allSmallerNodes,                                     
                                     BTProgressMonitor monitor)  throws AsynchExitRequestException {
    
    //
    // Build sets of names. If the names are not unique, this layout cannot proceed.
    //
    
    LoopReporter lr = new LoopReporter(align.size(), 20, monitor, 0.0, 1.00, "progress.normalizeAlignMapA");
    
    HashSet<String> keyNames = new HashSet<String>();
    for (NetNode key : align.keySet()) {
      if (keyNames.contains(key.getName())) {
      	lr.finish();
      	System.err.println("Duplicated key " + key.getName());
        return (null); 
      }
      keyNames.add(key.getName());
      lr.report();
    }
    lr.finish();
    
    LoopReporter lr2 = new LoopReporter(align.size(), 20, monitor, 0.0, 1.00, "progress.normalizeAlignMapB");
    
    HashSet<String> valNames = new HashSet<String>();
    for (NetNode value : align.values()) {
      if (valNames.contains(value.getName())) {
      	lr2.finish();
      	System.err.println("Duplicated value " + value.getName());
        return (null); 
      }
      valNames.add(value.getName());
      lr2.report();
    }
    lr2.finish();
    
    //
    // Reminder: a map takes elements in range and spits out values in the domain.
    // Alignment map A takes all the nodes in smallNodes set S into some subset of the nodes 
    // in largeNodes set L. To do the cycle layout, we need to have some inverse map F from L to S such 
    // the domain of F is a subset of L, and the range of F completely covers S. If the nodes are in the
    // same namespace, the identity map on the elements of L is sufficient. If not, the inverse of the
    // perfect alignment map does the trick. Check first if the identity map can work.
    //
    
    LoopReporter lr3 = new LoopReporter(allLargerNodes.size(), 20, monitor, 0.0, 1.00, "progress.identityMapCheckA");
    
    HashSet<String> largeNames = new HashSet<String>();
    for (NetNode large : allLargerNodes) {
      if (largeNames.contains(large.getName())) {
      	 lr3.finish();
        return (null); 
      }
      largeNames.add(large.getName());
      lr3.report();
    }
    lr3.finish();
    
    LoopReporter lr4 = new LoopReporter(allSmallerNodes.size(), 20, monitor, 0.0, 1.00, "progress.identityMapCheckB");
    
    HashSet<String> smallNames = new HashSet<String>();
    for (NetNode small : allSmallerNodes) {
      if (smallNames.contains(small.getName())) {
      	lr4.finish();
        return (null); 
      }
      smallNames.add(small.getName());
      lr4.report();
    }
    lr4.finish();
    
    boolean identityOK = largeNames.containsAll(smallNames);
 
    // 
    // If identity map does not work, we need to build maps from the perfect alignment:
    //
    
    Map<String, String> backMap = null;
    Map<String, String> correctMap = null;

    if (!identityOK) {
      if ((perfectAlign == null) || perfectAlign.isEmpty()) {
        return (null);
      } 
      backMap = new HashMap<String, String>();
      correctMap = new HashMap<String, String>();
      
      LoopReporter lr5 = new LoopReporter(perfectAlign.size(), 20, monitor, 0.0, 1.00, "progress.namespaceMapBuilding");
         
      for (NetNode key : perfectAlign.keySet()) {
        NetNode val = perfectAlign.get(key);
        backMap.put(val.getName(), key.getName());
        correctMap.put(key.getName(), val.getName());
        lr5.report();
      }
      lr5.finish();
    }
    
    LoopReporter lr6 = new LoopReporter(align.size(), 20, monitor, 0.0, 1.00, "progress.buildTheMap");
    
    Map<String, String> nameToName = new HashMap<String, String>();
     
    for (NetNode key : align.keySet()) {
       NetNode matchNode = align.get(key);
       // Gotta be unique:
       if (nameToName.containsKey(key)) {
         throw new IllegalStateException();
       }
       nameToName.put(key.getName(), matchNode.getName());
       lr6.report();
     }   
     lr6.finish();
     
     return (new NodeMaps(nameToName, backMap, correctMap));
  } 
  
  /***************************************************************************
  **
  ** Get all nodes
  */
  
  private Set<NetNode> genAllNodes(BuildData narbd) { 

    Set<NetNode> allNodes = new HashSet<NetNode>();
     
     for (NetLink link : narbd.getLinks()) {
       allNodes.add(link.getSrcNode());
       allNodes.add(link.getTrgNode());
     }
     allNodes.addAll(narbd.getSingletonNodes());
     return (allNodes);
   } 
   
  /***************************************************************************
  **
  ** Get map from network NetNode (of form G1::G2) to path elem (G1)
  */
  
  private Map<NetNode, String> genNodeToPathElem(Set<NetNode> allNodes) { 

     Map<NetNode, String> n2pe = new HashMap<NetNode, String>();
     
     for (NetNode key : allNodes) {
       String[] toks = key.getName().split("::");
       if (toks.length == 2) {
         n2pe.put(key, toks[0]);
       } else {
         n2pe.put(key, key.getName());
       }
     }
     return (n2pe);
   } 
  
  
  /***************************************************************************
  **
  ** Inverse of above
  */
  
  private Map<String, NetNode> genPathElemToNode(Set<NetNode> allNodes) { 

    Map<String, NetNode> pe2n = new HashMap<String, NetNode>();
    
    for (NetNode key : allNodes) {
      String[] toks = key.getName().split("::");
      if (toks.length == 2) {
        pe2n.put(toks[0], key);
      } else {
        pe2n.put(key.getName(), key);
      }
    }
    return (pe2n);
  } 
  
  
  /***************************************************************************
  **
  ** Extract the paths in the alignment network.
  ** We have two orthogonal issues: 1) are nodes in same namespace, and 2) are there more
  ** nodes in the larger network? With different namespaces, we need a reverse map. If the
  ** larger network has more nodes, there might not be a reverse mapping, so not every path
  ** will be a cycle. Note that while A->A is an obvious correct match, A->1234 is not so
  ** clear. A->B B->A is a swap that needs to be annotated as a cycle, but A->B should obviously
  ** also be marked as a path if B is in the larger net and not aligned. Similarly, A->1234 should
  ** be unmarked if it is correct, but marked if is incorrect.
  */
  
  private Map<String, AlignPath> calcAlignPaths(NodeMaps align) { 

     Map<String, AlignPath> pathsPerStart = new HashMap<String, AlignPath>();
     
     HashSet<String> working = new HashSet<String>(align.normalMap.keySet());
     while (!working.isEmpty()) {
       String startKey = working.iterator().next();
       working.remove(startKey);
       AlignPath path = new AlignPath();
       pathsPerStart.put(startKey, path);
       path.pathNodes.add(startKey);
       String nextKey = align.normalMap.get(startKey);
       // Not every cycle closes itself, so nextKey can == null:
       while (nextKey != null) {
         // Back map is used when the two networks have different node namespaces...
         if (align.backMap != null) {
           nextKey = align.backMap.get(nextKey);
           if (nextKey == null) { // Node in larger net has no partner in smaller
             break;
           }
         }
         if (nextKey.equals(startKey)) {
           path.isCycle = true;
           path.correct = (path.pathNodes.size() == 1);
           break;
         }
         AlignPath existing = pathsPerStart.get(nextKey);
         // If there is an existing path for the next key, we just glue that
         // existing path onto the end of this new path head. Note we do not
         // bother with then getting the "nextKey", as we have already 
         // traversed that path tail.
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
           // Go set next key, which may well be null, i.e. we deadend.
           nextKey = align.normalMap.get(nextKey);
         }
       }
     }
     
     //
     // For each AlignPath, if it is not a cycle, we look for the unaligned node corresponding to the first
     // element of the path, and glue it on the front.
     //
     
     Map<String, AlignPath> completePathsPerStart = new HashMap<String, AlignPath>();
     for (String start : pathsPerStart.keySet()) {
       AlignPath apfs = pathsPerStart.get(start);
       if (apfs.isCycle) {
         completePathsPerStart.put(start, apfs);
         continue;
       }
       String unaligned = (align.correctMap == null) ? start : align.correctMap.get(start);
       List<String> replace = new ArrayList<String>();
       replace.add(unaligned);
       replace.addAll(apfs.pathNodes);
       apfs.pathNodes.clear();
       apfs.pathNodes.addAll(replace);
       completePathsPerStart.put(unaligned, apfs);       
     }

     Map<String, AlignPath> pathsPerEveryNode = new HashMap<String, AlignPath>();
     for (String keyName : completePathsPerStart.keySet()) {
       AlignPath path = completePathsPerStart.get(keyName);
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
    boolean correct;

    AlignPath() {
      pathNodes = new ArrayList<String>();
      isCycle = false;
      correct = false;
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
        
    public List<NetNode> startNodes;

    public DefaultParams(List<NetNode> startNodes) {
      this.startNodes = startNodes;
    } 
  }
  
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  private static class NodeMaps  {
        
    Map<String, String> normalMap;
    Map<String, String> backMap;
    Map<String, String> correctMap;

    NodeMaps(Map<String, String> normalMap, Map<String, String> backMap, Map<String, String> correctMap) {
      this.normalMap = normalMap;
      this.backMap = backMap;
      this.correctMap = correctMap;
    } 
  }
  
  /***************************************************************************
  **
  ** For passing around cycles. A cycle that starts and ends on the same node
  ** might be incorrect if a smaller network is overlaid on a larger one.
  */  
  
  public static class CycleBounds  {
        
    public NetNode boundStart;
    public NetNode boundEnd;
    public boolean isCorrect;
    public boolean isCycle;

    CycleBounds(NetNode boundStart, NetNode boundEnd, boolean isCorrect, boolean isCycle) {
      this.boundStart = boundStart;
      this.boundEnd = boundEnd;
      this.isCorrect = isCorrect;
      this.isCycle = isCycle;
    } 
  }
 
}
