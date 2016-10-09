/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;

/****************************************************************************
**
** This is the default layout algorithm
*/

public class DefaultLayout {
  
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

  public DefaultLayout() {

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
  
  public void doLayout(BioFabricNetwork.RelayoutBuildData rbd, NodeSimilarityLayout.CRParams params) {   
    doNodeLayout(rbd, ((Params)params).startNodes);
    (new DefaultEdgeLayout()).layoutEdges(rbd);
    return;
  }
  
  /***************************************************************************
  **
  ** Relayout the network!
  */
  
  public List<String> doNodeLayout(BioFabricNetwork.RelayoutBuildData rbd, List<String> startNodes) {
    
    List<String> targets = defaultNodeOrder(rbd.allLinks, rbd.loneNodes, startNodes);       

    //
    // Now have the ordered list of targets we are going to display.
    // Build target->row maps and the inverse:
    //
    
    installNodeOrder(targets, rbd);
    return (targets);
  }
  
  /***************************************************************************
  **
  ** Install node orders
  */
  
  public void installNodeOrder(List<String> targets, BioFabricNetwork.RelayoutBuildData rbd) {
  
    int currRow = 0;
    HashMap<String, String> nodeOrder = new HashMap<String, String>();
    Iterator<String> trit = targets.iterator();
    while (trit.hasNext()) {
      String target = trit.next();
      String rowTag = Integer.toString(currRow++);
      nodeOrder.put(target.toUpperCase(), rowTag);
      //System.out.println("ino " + target + " " + rowTag);
    }  
    rbd.setNodeOrder(nodeOrder);
    return;
  }

  /***************************************************************************
  ** 
  ** Calculate default node order
  */

  public List<String> defaultNodeOrder(Set<FabricLink> allLinks, Set<String> loneNodes, List<String> startNodes) {    
    //
    // Note the allLinks Set has pruned out duplicates and synonymous non-directional links
    //
    //
    // Build a target list, top to bottom, that adds the node with the most
    // links first, and adds those link targets ASAP. If caller supplies a start node,
    // we go there first:
    // 
    
    HashMap<String, Integer> linkCounts = new HashMap<String, Integer>();
    HashMap<String, Set<String>> targsPerSource = new HashMap<String, Set<String>>();
    ArrayList<String> targets = new ArrayList<String>();
         
    HashSet<String> targsToGo = new HashSet<String>();
    Iterator<FabricLink> alit = allLinks.iterator();
    while (alit.hasNext()) {
      FabricLink nextLink = alit.next();
      String source = nextLink.getSrc();
      String target = nextLink.getTrg();
      Set<String> targs = targsPerSource.get(source);
      if (targs == null) {
        targs = new HashSet<String>();
        targsPerSource.put(source, targs);
      }
      targs.add(target);
      targs = targsPerSource.get(target);
      if (targs == null) {
        targs = new HashSet<String>();
        targsPerSource.put(target, targs);
      }
      targs.add(source);
      targsToGo.add(source);
      targsToGo.add(target);        
      Integer srcCount = linkCounts.get(source);
      linkCounts.put(source, (srcCount == null) ? Integer.valueOf(1) : Integer.valueOf(srcCount.intValue() + 1));
      Integer trgCount = linkCounts.get(target);
      linkCounts.put(target, (trgCount == null) ? Integer.valueOf(1) : Integer.valueOf(trgCount.intValue() + 1));
    }
    
    //
    // Rank the nodes by link count:
    //
    
    TreeMap<Integer, SortedSet<String>> countRank = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
    Iterator<String> lcit = linkCounts.keySet().iterator();
    while (lcit.hasNext()) {
      String src = lcit.next();
      Integer count = linkCounts.get(src);
      SortedSet<String> perCount = countRank.get(count);
      if (perCount == null) {
        perCount = new TreeSet<String>();
        countRank.put(count, perCount);
      }
      perCount.add(src);
    }
    
    //
    // Handle the specified starting nodes case:
    //
    
    if ((startNodes != null) && !startNodes.isEmpty()) {
      ArrayList<String> queue = new ArrayList<String>();
      targsToGo.removeAll(startNodes);
      targets.addAll(startNodes);
      queue.addAll(startNodes);
//      System.out.println("added all " + startNodes);
      flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue);
    }   
    
    //
    // Get all kids added in.  Now doing this without recursion; seeing blown
    // stacks for huge networks!
    //
     
    while (!targsToGo.isEmpty()) {
      Iterator<Integer> crit = countRank.keySet().iterator();
      while (crit.hasNext()) {
        Integer key = crit.next();
        SortedSet<String> perCount = countRank.get(key);
        Iterator<String> pcit = perCount.iterator();
        while (pcit.hasNext()) {
          String node = pcit.next();    
          if (targsToGo.contains(node)) {
            ArrayList<String> queue = new ArrayList<String>();
            targsToGo.remove(node);
            targets.add(node);
            addMyKidsNR(targets, targsPerSource, linkCounts, targsToGo, node, queue);
          }
        }
      }
    }
    
    //
    //
    // Tag on lone nodes.  If a node is by itself, but also shows up in the links,
    // we drop it:
    //
    
    HashSet<String> remains = new HashSet<String>(loneNodes);
    remains.removeAll(targets);
    targets.addAll(new TreeSet<String>(remains));
    return (targets);
  }
        
  /***************************************************************************
  **
  ** Node ordering
  */
  
  private ArrayList<String> orderMyKids(Map<String, Set<String>> targsPerSource, Map<String, Integer> linkCounts, 
                                        HashSet<String> targsToGo, String node) {
    Set<String> targs = targsPerSource.get(node);
    if (targs == null) {
//    	System.out.println("no kids for " + node);
    	return (new ArrayList<String>());
    }
    TreeMap<Integer, SortedSet<String>> kidMap = new TreeMap<Integer, SortedSet<String>>(Collections.reverseOrder());
    Iterator<String> tait = targs.iterator();
    while (tait.hasNext()) {  
      String nextTarg = tait.next(); 
      Integer count = linkCounts.get(nextTarg);
      SortedSet<String> perCount = kidMap.get(count);
      if (perCount == null) {
        perCount = new TreeSet<String>();
        kidMap.put(count, perCount);
      }
      perCount.add(nextTarg);
    }
    
    ArrayList<String> myKidsToProc = new ArrayList<String>();
    Iterator<SortedSet<String>> kmit = kidMap.values().iterator();
    while (kmit.hasNext()) {  
      SortedSet<String> perCount = kmit.next(); 
      Iterator<String> pcit = perCount.iterator();
      while (pcit.hasNext()) {  
        String kid = pcit.next();
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
  
  private void addMyKidsNR(ArrayList<String> targets, Map<String, Set<String>> targsPerSource, 
                           Map<String, Integer> linkCounts, 
                           HashSet<String> targsToGo, String node, ArrayList<String> queue) {
    queue.add(node);
    flushQueue(targets, targsPerSource, linkCounts, targsToGo, queue);
    return;
  }
  
  /***************************************************************************
  **
  ** Node ordering, non-recursive:
  */
  
  private void flushQueue(ArrayList<String> targets, Map<String, Set<String>> targsPerSource, 
                           Map<String, Integer> linkCounts, 
                           HashSet<String> targsToGo, ArrayList<String> queue) {
    while (!queue.isEmpty()) {
      String node = queue.remove(0);
      ArrayList<String> myKids = orderMyKids(targsPerSource, linkCounts, targsToGo, node);
      Iterator<String> ktpit = myKids.iterator(); 
      while (ktpit.hasNext()) {  
        String kid = ktpit.next();
        if (targsToGo.contains(kid)) {
          targsToGo.remove(kid);
          targets.add(kid);
          queue.add(kid);
        }
      }
    }
    return;
  }

  
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  public static class Params implements NodeSimilarityLayout.CRParams {
        
    public List<String> startNodes;

    public Params(List<String> startNodes) {
      this.startNodes = startNodes;
    } 
  }
}
