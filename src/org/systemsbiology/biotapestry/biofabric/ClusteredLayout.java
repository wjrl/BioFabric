/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.biofabric;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import java.util.Vector;
import org.systemsbiology.biotapestry.analysis.Link;
import org.systemsbiology.biotapestry.util.AffineCombination;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.DoubMinMax;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This is the Network model
*/

public class ClusteredLayout {
  
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

  public ClusteredLayout() {
 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get connectivity vectors
  */

  public SortedMap getConnectivityVectors(BioFabricNetwork bfn) { 
    
    HashMap targToRow = new HashMap();
    Iterator rtit = bfn.getRows();
    while (rtit.hasNext()) {
      Integer row = (Integer)rtit.next();
      String node = bfn.getNodeForRow(row);
      targToRow.put(node,row);
    }
    
    TreeMap retval = new TreeMap();
    Iterator ldit = bfn.getOrderedLinkInfo(false);
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      BioFabricNetwork.LinkInfo linf = bfn.getLinkDefinition(col, false);
      String srcName = linf.getSource();
      Integer srcRow = (Integer)targToRow.get(srcName);
      String trgName = linf.getTarget();
      Integer trgRow = (Integer)targToRow.get(trgName);

      TreeSet forRetval = (TreeSet)retval.get(srcRow);
      if (forRetval == null) {
        forRetval = new TreeSet();
        retval.put(srcRow, forRetval);
      }
      forRetval.add(trgRow);

      forRetval = (TreeSet)retval.get(trgRow);
      if (forRetval == null) {
        forRetval = new TreeSet();
        retval.put(trgRow, forRetval);
      }
      forRetval.add(srcRow);
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Get cosines.
  */

  public Integer getConnectivityCosines(SortedMap connVec, SortedMap retval, Map connMag, BioFabricNetwork bfn) {
    int rowCount = bfn.getRowCount();
    Integer[] icache = new Integer[rowCount];
    String[] scache = new String[rowCount];
    for (int i = 0; i < rowCount; i++) {
      icache[i] = new Integer(i);
      scache[i] = icache[i].toString();
    }
    HashMap targToRow = new HashMap();
    Iterator rtit = bfn.getRows();
    while (rtit.hasNext()) {
      Integer row = (Integer)rtit.next();
      String node = bfn.getNodeForRow(row);
      targToRow.put(node,row);
    }
  
    int count = 0;
    int fullCount = (rowCount * rowCount) / 2;
 
    Integer mostConnected = null;
    int biggestMag = Integer.MIN_VALUE;
    HashSet intersect = new HashSet();
    
    Iterator ldit = bfn.getOrderedLinkInfo(false);
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      BioFabricNetwork.LinkInfo linf = bfn.getLinkDefinition(col, false);
      String srcName = linf.getSource();
      Integer srcRow = (Integer)targToRow.get(srcName);
      String trgName = linf.getTarget();
      Integer trgRow = (Integer)targToRow.get(trgName);
      TreeSet srcVec = (TreeSet)connVec.get(srcRow);     
      int srcSize = srcVec.size();
      if (srcSize > biggestMag) {
        biggestMag = srcSize;
        mostConnected = srcRow;
      }
      TreeSet trgVec = (TreeSet)connVec.get(trgRow);     
      int trgSize = trgVec.size();
      if (trgSize > biggestMag) {
        biggestMag = trgSize;
        mostConnected = trgRow;
      }
      connMag.put(scache[srcRow.intValue()], icache[srcSize]);
      connMag.put(scache[trgRow.intValue()], icache[trgSize]);
     
      double sqrs = Math.sqrt((double)(srcSize));
      double sqrt = Math.sqrt((double)(trgSize));
      intersect.clear();
      intersect.addAll(srcVec);
      intersect.retainAll(trgVec);
      
      // Since normalized and equal, this is the sum of products of (1 / sqrs) * (1 / sqrt),
      // where we sum over intersect identical terms.  Note that the "vectors" do not include
      // an entry for the node itself, so two nodes only connected to each other end up with
      // zero intersection.
      
      double val = (double)(intersect.size()) / (sqrs * sqrt);
      Double dot = new Double(val);
      ArrayList forDot = (ArrayList)retval.get(dot);
      if (forDot == null) {
        forDot = new ArrayList();
        retval.put(dot, forDot);
      }
      forDot.add(new Link(scache[srcRow.intValue()], scache[trgRow.intValue()]));
      count++;
    }  
    return (mostConnected);
  }
 
  /***************************************************************************
  ** 
  ** Per Wikipedia:
   * 
   * Jaccard coefficient measures similarity between sample sets, and is 
   * defined as the size of the intersection divided by the size of the 
   * union of the sample sets:
   * 
   * |A I B| / |A U B|
  **
  ** The Jaccard distance, which measures dissimilarity between sample sets, is 
  ** complementary to the Jaccard coefficient and is obtained by subtracting 
  ** the Jaccard coefficient from 1, or, equivalently, by dividing the difference 
  ** of the sizes of the union and the intersection of two sets by the size of the union:
  ** 
  ** (|A U B| - |A I B|) / |A U B|
   * 
   * This distance is a proper metric
  */
  
  public Integer getConnectivityJaccard(SortedMap connVec, SortedMap retval, Map connMag, BioFabricNetwork bfn) {

    int rowCount = bfn.getRowCount();
    Integer[] icache = new Integer[rowCount];
    String[] scache = new String[rowCount];
    for (int i = 0; i < rowCount; i++) {
      icache[i] = new Integer(i);
      scache[i] = icache[i].toString();
    }
    HashMap targToRow = new HashMap();
    Iterator rtit = bfn.getRows();
    while (rtit.hasNext()) {
      Integer row = (Integer)rtit.next();
      String node = bfn.getNodeForRow(row);
      targToRow.put(node,row);
    }
  
    int count = 0;

    Integer mostConnected = null;
    int biggestMag = Integer.MIN_VALUE;
    HashSet union = new HashSet();
    HashSet intersect = new HashSet();
    
    Iterator ldit = bfn.getOrderedLinkInfo(false);
    while (ldit.hasNext()) {
      Integer col = (Integer)ldit.next();
      BioFabricNetwork.LinkInfo linf = bfn.getLinkDefinition(col, false);
      String srcName = linf.getSource();
      Integer srcRow = (Integer)targToRow.get(srcName);
      String trgName = linf.getTarget();
      Integer trgRow = (Integer)targToRow.get(trgName);
      TreeSet srcVec = (TreeSet)connVec.get(srcRow);     
      int srcSize = srcVec.size();
      if (srcSize > biggestMag) {
        biggestMag = srcSize;
        mostConnected = srcRow;
      }
      TreeSet trgVec = (TreeSet)connVec.get(trgRow);     
      int trgSize = trgVec.size();
      if (trgSize > biggestMag) {
        biggestMag = trgSize;
        mostConnected = trgRow;
      }
      connMag.put(scache[srcRow.intValue()], icache[srcSize]);
      connMag.put(scache[trgRow.intValue()], icache[trgSize]);
     
      union.clear();
      DataUtil.union(srcVec, trgVec, union);
      intersect.clear();
      DataUtil.intersection(srcVec, trgVec, intersect);
      int uSize = union.size();
      int iSize = intersect.size();
      
      double jaccard = (double)(iSize) / (double)uSize;    
      Double jaccardObj = new Double(jaccard);
   
      ArrayList forDot = (ArrayList)retval.get(jaccardObj);
      if (forDot == null) {
        forDot = new ArrayList();
        retval.put(jaccardObj, forDot);
      }
      forDot.add(new Link(scache[srcRow.intValue()], scache[trgRow.intValue()]));
      count++;
    }  
    return (mostConnected);
  }
   
  /***************************************************************************
  ** 
  ** Like the above, but we prefer to keep growing out off of the last used
  ** set of nodes.
  */

  public List orderByDistanceChained(BioFabricNetwork bfn, Integer start, SortedMap cosines, 
                                     Map connMag, List linkTrace, 
                                     int limit, double tol, List jumpLog,
                                     BTProgressMonitor monitor, double startFrac, double endFrac) 
                                       throws AsynchExitRequestException { 
    int rowCount = bfn.getRowCount();
    ArrayList retval = new ArrayList();
    HashSet seen = new HashSet();  
    String startNode = Integer.toString(start.intValue());
    retval.add(startNode);
    seen.add(startNode);
    
    //
    // Tried running multiple chains, with the idea of being able to
    // pick up on a previously successful chain instead of starting
    // from scratch.  But all the chains got down to the tolerance
    // below the best-match and sat there until they were reused.
    // Or worse, a single match from an old chain fired off before
    // we went to reclaim another chain to start over.  Seems to
    // be little benefit, and big speed hit.
  
    ArrayList currentChain = new ArrayList();
    currentChain.add(startNode);
    int switchCount = 0;
    int stayCount = 0;

    //
    // Keep adding until all nodes are accounted for:
    //
    while (retval.size() < rowCount) {
      // Find best uncontrained hop:
      DoubleRanked bestHop = findBestUnseenHop(bfn, cosines, connMag, seen, null);  
      // Find best hop off the current search net:   
      DoubleRanked currentHop = findBestUnseenHop(bfn, cosines, connMag, seen, currentChain);
 
      if (bestHop == null) { // Not found, need to find the highest non-seen guy.
        if (currentHop != null) {
          throw new IllegalStateException();
        }
        handleFallbacks(bfn, connMag, linkTrace, seen, retval);
        continue;
      } 
      //
      // If the CURRENT chained hop stuff sucks, we toss the chain and start over:
      //      
      if ((currentHop == null) || (currentHop.rank <= (bestHop.rank * tol))) {
        seen.add(bestHop.id);
        linkTrace.add(bestHop.byLink);
        jumpLog.add(new Integer(retval.size()));
        retval.add(bestHop.id);       
        currentChain.clear();
        maintainChain(currentChain, bestHop, limit);
        switchCount++;      
      } else {     
        // As long as the CURRENT chained hop distance is not too bad compared to the unchained,
        // we prefer to use it:
        seen.add(currentHop.id);
        linkTrace.add(currentHop.byLink);
        retval.add(currentHop.id);
        maintainChain(currentChain, currentHop, limit);
        stayCount++;
      }
      if (monitor != null) {
        double currProg = startFrac + ((endFrac - startFrac) * ((double)retval.size() / (double)rowCount));
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }
    return (retval);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Maintain  the recent used chain
  */

  private void maintainChain(List chain, DoubleRanked bestChainedHop, int limit) {
    //
    // The guy who established the link goes first.  The newly added guy goes second.
    // If we hit the limit, the last guys are tossed
    //
    
    String bySrc = bestChainedHop.byLink.getSrc();
    String byTrg = bestChainedHop.byLink.getTrg();
        
    String bridge = (bySrc.equals(bestChainedHop.id)) ? byTrg : bySrc;    
    chain.remove(bridge);
    chain.add(0, bestChainedHop.id);
    chain.add(0, bridge);
            
    while (chain.size() > limit) {
      chain.remove(chain.size() - 1);
    }
    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Start with most connected node.  Find the highest cosine for it and
  ** another node.  That node then becomes the next to search on.
  */

  private DoubleRanked findBestUnseenHop(BioFabricNetwork bfn, SortedMap cosines, Map connMag, HashSet seen, List launchNodes) { 
    if ((launchNodes != null) && launchNodes.isEmpty()) {
      return (null);
    }
   
    Iterator dotIt = cosines.keySet().iterator();
    //
    // Look in order of cosine magnitude, high to low:
    //
    
    String maxConnNode = null;
    Integer maxConnConn = null;
    
    while (dotIt.hasNext()) {       
      Double dot = (Double)dotIt.next(); 
      ArrayList forDot = (ArrayList)cosines.get(dot);
      int numForDot = forDot.size();
      maxConnNode = null;
      maxConnConn = null;
      Link viaLink = null;
      // Each cosine magnitude has a list of links.  Find ones that span from the
      // set of placed nodes to the set of unplaced nodes:
      for (int i = 0; i < numForDot; i++) {
        Link nextLink = (Link)forDot.get(i);
        String src = nextLink.getSrc();
        String trg = nextLink.getTrg();
        String cand = null;
        if (seen.contains(src) && !seen.contains(trg)) {
          if ((launchNodes == null) || launchNodes.contains(src)) {
            cand = trg;
          }
        } else if (seen.contains(trg) && !seen.contains(src)) {
          if ((launchNodes == null) || launchNodes.contains(trg)) {
            cand = src;
          }
        }
        //
        // Having found one, record who has the highest connectivity:
        //
        if (cand != null) {
          Integer connVal = (Integer)connMag.get(cand);
          if ((maxConnConn == null) || (maxConnConn.intValue() < connVal.intValue())) {
            maxConnNode = cand;
            maxConnConn = connVal;
            viaLink = nextLink;
          }
        }          
      }
      //
      // Having found the one with the highest cosine, tie-breaking with highest connectivity,
      // add it to the list:
      //
      if (maxConnNode != null) { 
        return (new DoubleRanked(dot.doubleValue(), maxConnNode, viaLink));
      }
    }
        
    return (null);
  }  
  
  /***************************************************************************
  ** 
  ** Handle the fallback case.
  */

  private void handleFallbacks(BioFabricNetwork bfn, Map connMag, List linkTrace, HashSet seen, List retval) { 
    String nextBest = getMostConnectedRemaining(seen, connMag);
    if (nextBest != null) {        
      retval.add(nextBest);
      seen.add(nextBest);
      linkTrace.add(new Link(nextBest, nextBest));
    } else {
      // Nodes not connected need to be flushed
      TreeSet remainingTargs = new TreeSet();
      Iterator rtkit = bfn.getRows();
      while (rtkit.hasNext()) {       
        Integer row = (Integer)rtkit.next();
        remainingTargs.add(row.toString());
      }       
      remainingTargs.removeAll(retval);
      Iterator rtit = remainingTargs.iterator();
      while (rtit.hasNext()) {
        String rTrg = (String)rtit.next();
        retval.add(rTrg);
        linkTrace.add(new Link(rTrg, rTrg));
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** When we run out of connected nodes, go get the best one remaining
  */

  private String getMostConnectedRemaining(Set seen, Map connMag) { 
    String maxConnNode = null;
    Integer maxConnConn = null;
    Iterator connIt = connMag.keySet().iterator();
    while (connIt.hasNext()) {       
      String cand = (String)connIt.next();
      if (seen.contains(cand)) {
        continue;
      }
      Integer connVal = (Integer)connMag.get(cand);
      if ((maxConnConn == null) || (maxConnConn.intValue() < connVal.intValue())) {
        maxConnNode = cand;
        maxConnConn = connVal;
      }
    }              
    return (maxConnNode);
  }
 
  /***************************************************************************
  ** 
  ** Utility conversion
  */

  public List convertOrder(BioFabricNetwork bfn, List orderedStringRows) { 
    ArrayList retval = new ArrayList();
    int numOsr = orderedStringRows.size();
    for (int i = 0; i < numOsr; i++) {
      String sval = (String)orderedStringRows.get(i);
      try {
        Integer intval = Integer.valueOf(sval);
        retval.add(bfn.getNodeForRow(intval));
      } catch (NumberFormatException nfex) {
        throw new IllegalStateException();
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Utility conversion
  */

  public Map convertOrderToMap(BioFabricNetwork bfn, List orderedStringRows) { 
    HashMap retval = new HashMap();
    int numOsr = orderedStringRows.size();
    for (int i = 0; i < numOsr; i++) {
      String sval = (String)orderedStringRows.get(i);
      try {
        Integer intval = Integer.valueOf(sval);
        retval.put(bfn.getNodeForRow(intval).toUpperCase(), Integer.toString(i));
      } catch (NumberFormatException nfex) {
        throw new IllegalStateException();
      }
    }
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Utility conversion
  */

  public void orderToMaps(List orderedStringRows, Map forward, Map backward) { 
    int numOsr = orderedStringRows.size();
    for (int i = 0; i < numOsr; i++) {
      String sval = (String)orderedStringRows.get(i);
      try {
        Integer newPos = Integer.valueOf(sval);
        Integer oldPos = new Integer(i);
        forward.put(oldPos, newPos);
        backward.put(newPos, oldPos);
      } catch (NumberFormatException nfex) {
        throw new IllegalStateException();
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Debug output:
  */

  private void curveDebug(SortedMap curve) {
    Iterator cit = curve.keySet().iterator();
    int count = 0;
    while (cit.hasNext()) {
      Integer key = (Integer)cit.next();
      Double val = (Double)curve.get(key);
      System.out.print("(" + key + "," + val + ")");
      if (count++ == 5) {
        break;
      }
    }
    System.out.println();    
  }
 
  /***************************************************************************
  ** 
  ** Build curves and caches
  */

  private void buildCurvesAndCaches(TreeMap oldToNew, TreeMap newToOld, SortedMap connVec, Integer[] icache, Double[] dcache, HashMap curves, HashMap connectMap, int numRows) {
    for (int i = 0; i < numRows; i++) {
      Integer newRow = new Integer(i);
      icache[i] = newRow;
      dcache[i] = new Double((double)i);
      Integer oldRow = (Integer)newToOld.get(newRow);  
      TreeSet baseNodeVec = (TreeSet)connVec.get(oldRow);        
      SortedMap curve = calcShapeCurve(baseNodeVec, oldToNew);
      curves.put(newRow, curve);
      double connLog = Math.log(baseNodeVec.size()) / Math.log(2.0);
      Double connLogKey = new Double(connLog);
      int logBin = (connLog < 4.0) ? 0 : (int)Math.floor(connLog);
      Integer logBinKey = new Integer(logBin);
      HashMap fineGrain = (HashMap)connectMap.get(logBinKey);
      if (fineGrain == null) {
        fineGrain = new HashMap();
        connectMap.put(logBinKey, fineGrain);
      }
      HashSet perFine = (HashSet)fineGrain.get(connLogKey);
      if (perFine == null) {
        perFine = new HashSet();
        fineGrain.put(connLogKey, perFine);
      }
      perFine.add(newRow);
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Build the set to check:
  */

  private void buildCheckSet(SortedMap baseCurve, TreeSet connBuf, SortedSet logKeys, HashMap connectMap) {
    //
    // Chose who to check against based on having about the same number of neighbors:
    //
    double baseConnLog = Math.log(baseCurve.size()) / Math.log(2.0);
    double baseConnLogLo = 0.98 * baseConnLog;
    double baseConnLogHi = 1.02 * baseConnLog;
    logKeys.clear();
    int logBinLo = (baseConnLog < 4.0) ? 0 : (int)Math.floor(baseConnLogLo);
    Integer logBinLoKey = new Integer(logBinLo);
    logKeys.add(logBinLoKey);
    int logBinHi = (baseConnLog < 4.0) ? 0 : (int)Math.floor(baseConnLogHi);
    Integer logBinHiKey = new Integer(logBinHi);
    logKeys.add(logBinHiKey);
    logKeys = DataUtil.fillOutHourly(logKeys);
    connBuf.clear();
    Iterator lit = logKeys.iterator();
    while (lit.hasNext()) {
      Integer logBinKey = (Integer)lit.next();
      int logBinVal = logBinKey.intValue();
      HashMap fineGrain = (HashMap)connectMap.get(logBinKey);
      if (fineGrain == null) {
        continue;
      }
      Iterator pfg = fineGrain.keySet().iterator();
      while (pfg.hasNext()) {
        Double fineKey = (Double)pfg.next();
        double fineKeyVal = fineKey.doubleValue();
        if ((logBinVal == 0) || ((baseConnLogLo <= fineKeyVal) && (baseConnLogHi >= fineKeyVal))) {
          HashSet fineGrains = (HashSet)fineGrain.get(fineKey);
          connBuf.addAll(fineGrains);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  ** 
  ** Just report current goodness:
  */

  public ClusterPrep setupForResort(BioFabricNetwork bfn, SortedMap connVec, List orderedStringRows, SortedMap rankings) {

    ClusterPrep retval = new ClusterPrep(orderedStringRows.size()); 
 
    orderToMaps(orderedStringRows, retval.oldToNew, retval.newToOld);
      
    //
    // Create a curve cache.
    //
    
    buildCurvesAndCaches(retval.oldToNew, retval.newToOld, connVec, retval.icache, retval.dcache, retval.curves, retval.connectMap, retval.numRows);
    
    //
    // Expensive method did a row rearrangement after every swap and shift.  Note that meant
    // we were running O(n^3), maybe O(n^2logn) at best.  Even then, the first decisions were
    // stale (based on orginal order) by the time the last decisions were made.  Instead, we
    // keep the orginal curves, and instead do multiple passes to iterate to (hopefully) a better
    // fit.
    // Moving left to right, compare each node fit to righthand neighbor (following shift) to all 
    // the remaining righthand neighbors.
    //
    
    TreeSet unionBuf = new TreeSet();
    TreeSet keyBuf = new TreeSet();
    double deltaSum = 0.0;
    Iterator n2oit = (new TreeSet(retval.newToOld.keySet())).iterator();
    while (n2oit.hasNext()) {
      Integer newRow = (Integer)n2oit.next();
      int currRow = newRow.intValue();
      if (currRow == (retval.numRows - 1)) {
        break;
      }
      SortedMap baseCurve = (SortedMap)retval.curves.get(newRow);
      SortedMap nextCurve = (SortedMap)retval.curves.get(retval.icache[currRow + 1]);
      double delt = calcShapeDeltaUsingCurveMaps(baseCurve, nextCurve, unionBuf, keyBuf);
      deltaSum += delt;  
    }
    Integer useKey = (rankings.isEmpty()) ? new Integer(0) : new Integer(((Integer)rankings.lastKey()).intValue() + 1);
    rankings.put(useKey, new Double(deltaSum));  
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Resort to group shapes:
  */

  public List resort(ClusterPrep prep, BTProgressMonitor monitor, double startFrac, double endFrac) 
                        throws AsynchExitRequestException { 

    /*
    int numRows = orderedStringRows.size();
    TreeMap oldToNew = new TreeMap();
    TreeMap newToOld = new TreeMap();    
    orderToMaps(orderedStringRows, oldToNew, newToOld);
      
    //
    // Create a curve cache.
    //
    
    Integer[] icache = new Integer[numRows];
    Double[] dcache = new Double[numRows];
    HashMap curves = new HashMap();
    HashMap connectMap = new HashMap();
    buildCurvesAndCaches(oldToNew, newToOld, connVec, icache, dcache, curves, connectMap, numRows);
    
    //
    // Expensive method did a row rearrangement after every swap and shift.  Note that meant
    // we were running O(n^3), maybe O(n^2logn) at best.  Even then, the first decisions were
    // stale (based on orginal order) by the time the last decisions were made.  Instead, we
    // keep the orginal curves, and instead do multiple passes to iterate to (hopefully) a better
    // fit.
    // Moving left to right, compare each node fit to righthand neighbor (following shift) to all 
    // the remaining righthand neighbors.
    //
    

    double deltaSum = 0.0;
    Iterator n2oit = (new TreeSet(newToOld.keySet())).iterator();
    while (n2oit.hasNext()) {
      Integer newRow = (Integer)n2oit.next();
      int currRow = newRow.intValue();
      if (currRow == (numRows - 1)) {
        break;
      }
      SortedMap baseCurve = (SortedMap)curves.get(newRow);
      SortedMap nextCurve = (SortedMap)curves.get(icache[currRow + 1]);
      double delt = calcShapeDeltaUsingCurveMaps(baseCurve, nextCurve, unionBuf, keyBuf);
      deltaSum += delt;
    }
    Integer useKey = (rankings.isEmpty()) ? new Integer(0) : new Integer(((Integer)rankings.lastKey()).intValue() + 1);
    rankings.put(useKey, new Double(deltaSum));
    
    */
    
    
    
    
    TreeSet unionBuf = new TreeSet();
    TreeSet keyBuf = new TreeSet();
    

    TreeSet stillAvail = new TreeSet(prep.newToOld.keySet());   
    TreeMap results = new TreeMap();
 
    TreeSet connBuf = new TreeSet();
    SortedSet logKeys = new TreeSet();
    SortedMap baseCurve = null;
    int currRow = 0;
    int startCheck = 1;
    int fillSlot = 0;
    results.put(prep.icache[0], prep.icache[fillSlot++]);
    stillAvail.remove(prep.icache[0]);
    baseCurve = (SortedMap)prep.curves.get(prep.icache[0]);

    while (!stillAvail.isEmpty()) {  
 
      Integer newRow = (Integer)stillAvail.first();
      startCheck = newRow.intValue();

      buildCheckSet(baseCurve, connBuf, logKeys, prep.connectMap);
      connBuf.retainAll(stillAvail);
      // Always include the start in the search!
      connBuf.add(prep.icache[startCheck]);
          
      DoubMinMax dmm = new DoubMinMax();
      dmm.inverseInit();
      double baseDelt = 0.0;
      double minMatch = Double.POSITIVE_INFINITY;
      int minI = currRow;
      int numCheck = 0;
      Iterator cit = connBuf.iterator();
      while (cit.hasNext()) {
        Integer swapCheck = (Integer)cit.next();
        int swapCheckVal = swapCheck.intValue();
        numCheck++;
        SortedMap shiftCandCurve = (SortedMap)prep.curves.get(swapCheck);
        double delt = calcShapeDeltaUsingCurveMaps(baseCurve, shiftCandCurve, unionBuf, keyBuf);
        if (swapCheckVal == startCheck) {
          baseDelt = delt;
        }
        dmm.update(delt);
        if (delt < minMatch) {
         // Need to see if there is a global improvement!
        //  Integer preRemIndx = (Integer)newToOld.get(new Integer(i - 1));
         // TreeSet preRemoveVec = (TreeSet)connVec.get(preRemIndx);
         // Integer postRemIndx = (Integer)newToOld.get(new Integer(i + 1));
        //  TreeSet postRemoveVec = (TreeSet)connVec.get(oldForSwap);
        //  double deltRight = calcShapeDeltaViaCurve(preRemoveVec, postRemoveVec, oldToNew);
          minI = swapCheckVal;
          minMatch = delt;
        }
      }
      
      if (minI > startCheck) {
        baseCurve = (SortedMap)prep.curves.get(prep.icache[minI]);
        stillAvail.remove(prep.icache[minI]);
        results.put(prep.icache[minI], prep.icache[fillSlot++]);      
      } else {
        baseCurve = (SortedMap)prep.curves.get(prep.icache[startCheck]);
        stillAvail.remove(prep.icache[startCheck]);
        results.put(prep.icache[startCheck], prep.icache[fillSlot++]);              
      }
            
      if (monitor != null) {
        double currProg = startFrac + ((endFrac - startFrac) * (1.0 - ((double)stillAvail.size() / (double)prep.numRows)));
        if (!monitor.updateProgress((int)(currProg * 100.0))) {
          throw new AsynchExitRequestException();
        }
      }
    }

    //
    // Convert and leave:
    //
    
    ArrayList retval = new ArrayList();
    Iterator o2nit = prep.oldToNew.values().iterator();
    while (o2nit.hasNext()) {
      Integer newRow = (Integer)o2nit.next();
      Integer mappedRow = (Integer)results.get(newRow);
      retval.add(mappedRow.toString());
    }
    return (retval);
  } 
  
  /***************************************************************************
  ** 
  ** Calculate the shape per node:
  */

  private SortedMap calcShapeCurve(SortedSet vec1, Map newOrder) {
                 
    TreeSet reordered1 = new TreeSet();
    Iterator ub1it = vec1.iterator();
    while (ub1it.hasNext()) {
      Integer linkEnd = (Integer)ub1it.next();
      Integer mappedEnd = (Integer)newOrder.get(linkEnd);
      reordered1.add(mappedEnd);
    }    
    ArrayList vec1Order = new ArrayList(reordered1);
    int numPts = vec1Order.size();
    TreeMap vec1Points = new TreeMap();
    for (int i = 0; i < numPts; i++) {
      Integer linkEnd = (Integer)vec1Order.get(i);
      calcCurveMap(reordered1, vec1Order, linkEnd, vec1Points);
    }
  
    return (vec1Points);
  }

  /***************************************************************************
  ** 
  ** Debug output:
  */

  private double curveAverage(SortedMap curve) {
    double retval = 0.0;
    Double firstVal = null;
    double lastX = 0.0;
    Iterator cit = curve.keySet().iterator();
    while (cit.hasNext()) {
      Integer key = (Integer)cit.next();
      Double val = (Double)curve.get(key);
      double thisKey = key.doubleValue();
      double thisVal = val.doubleValue();
      if (firstVal == null) {
        firstVal = val;
      } else {
        retval += ((thisKey - lastX) * thisVal);        
      }
      lastX = thisKey;
    }
    if (firstVal == null) {
      return (0.0); 
    } else if (lastX == 0.0) {
      return (firstVal.doubleValue()); 
    }
    retval /= lastX;
    return (retval);
  }

  /***************************************************************************
  ** 
  ** Calculate the shape distance:
  */

  private double calcShapeDeltaUsingCurveMaps(SortedMap vec1Points, SortedMap vec2Points, SortedSet unionBuf, SortedSet keyBuf) {
          
    //
    // We want to find out how close the "link shapes" of two nodes are, given the current
    // node ordering given in newOrder.  We compare the left-hand profiles:
    //
   
    double deltaSqSum = 0.0;
    if (vec1Points.isEmpty() || vec2Points.isEmpty()) {
      return (Double.POSITIVE_INFINITY);
    }
    
    double ca1 = curveAverage(vec1Points);
    double ca2 = curveAverage(vec2Points);
    
    
    unionBuf.clear();
    DataUtil.union(vec1Points.keySet(), vec2Points.keySet(), unionBuf);
    Iterator ubit = unionBuf.iterator();
    while (ubit.hasNext()) {
      Integer point = (Integer)ubit.next();
      double v1Val = interpCurve(vec1Points, point, keyBuf) - ca1;
      double v2Val = interpCurve(vec2Points, point, keyBuf) - ca2;
      double yDelt = v1Val - v2Val;
      deltaSqSum += (yDelt * yDelt);
    }
    double retval = Math.sqrt(deltaSqSum);
    if (Double.isNaN(retval)) {
      System.err.println(vec1Points);
      System.err.println(vec2Points);
      System.err.println(unionBuf);
      throw new IllegalStateException();
    }
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Calculate a curve.  Now only used for exact hits; interpolation code is obsolete!
  */

  private void calcCurveMap(SortedSet vec, List vecOrder, Integer linkEnd, Map curvePoints) {
    int numVec = vecOrder.size();      
    int vec1Pos = vecOrder.indexOf(linkEnd);
    if (vec1Pos != -1) {
      curvePoints.put(linkEnd, new Double((double)(numVec - vec1Pos)));
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Calculate a curve point:
  */

  private double interpCurve(SortedMap curve, Integer xVal, SortedSet xBuf) {

    Double exact = (Double)curve.get(xVal);    
    if (exact != null) {
      return (exact.doubleValue());
    } else {
      double[] weights = new double[2];
      xBuf.clear();
      xBuf.addAll(curve.keySet());
      MinMax bounds = DataUtil.boundingInts(xBuf, xVal.intValue());
      boolean areDiff = AffineCombination.getWeights((double)bounds.min, (double)bounds.max, xVal.doubleValue(), weights);
      if (areDiff) {        
        Integer mappedEndLo = new Integer(bounds.min);
        Integer mappedEndHi = new Integer(bounds.max);
       // double vec1XVal = Math.round(weights[0] * (double)bounds.min + weights[1] * (double)bounds.max);     
        double vec1YVal = weights[0] * ((Double)curve.get(mappedEndLo)).doubleValue() + weights[1] * ((Double)curve.get(mappedEndHi)).doubleValue();        
        return (vec1YVal);
      } else if (bounds.min > ((Integer)xBuf.first()).intValue())  {
        return (((Double)curve.get(curve.firstKey())).doubleValue());
      } else {
        return (0.0);
      }        
    }
  }

  /***************************************************************************
  **
  ** For passing around ranked nodes
  */  
  
  static class DoubleRanked  {
     double rank;
     String id;
     Link byLink;

    DoubleRanked(double rank, String id, Link byLink) {
      this.rank = rank;
      this.id = id;
      this.byLink = byLink;
    } 
  }
  
  
  /***************************************************************************
  **
  ** For passing around prep data
  */  
  
  public static class ClusterPrep  { 
    int numRows;
    TreeMap oldToNew;
    TreeMap newToOld;    
    Integer[] icache;
    Double[] dcache;
    HashMap curves;
    HashMap connectMap;  

    ClusterPrep(int numRows) { 
      this.numRows = numRows;
      oldToNew = new TreeMap();
      newToOld = new TreeMap();    
      icache = new Integer[numRows];
      dcache = new Double[numRows];
      curves = new HashMap();
      connectMap = new HashMap();  
    }
  }

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Marker interface
  */  
  
  public interface CRParams  {

  }
  
  /***************************************************************************
  **
  ** For passing around layout params
  */  
  
  public static class ClusterParams implements CRParams {
    
    // NOTE:
    //
    // JACCARD gives intersection size over "how many connections do WE have"
    // COSINES gives intersection size over:
    //   sqrt("how many connections do I have)" * sqrt("how many connections do you have)"
    //
     
    public static final int JACCARD = 0;
    public static final int COSINES = 1;
        
    public double tolerance;
    public int chainLength; 
    public int distanceMethod;

    ClusterParams(double tolerance, int chainLength, int distanceMethod) {
      this.tolerance = tolerance;
      this.chainLength = chainLength;
      this.distanceMethod = distanceMethod;
    }
    
    ClusterParams() {
      tolerance = 0.80;
      chainLength = 15;
      distanceMethod = JACCARD;    
    }
    
    public static Vector getDistanceChoices() {
      ResourceManager rMan = ResourceManager.getManager();
      Vector retval = new Vector();
      retval.add(new ChoiceContent(rMan.getString("clusterParams.jaccard"), JACCARD));
      retval.add(new ChoiceContent(rMan.getString("clusterParams.cosines"), COSINES));   
      return (retval);
    }
  } 
  
  /***************************************************************************
  **
  ** For passing around resort params
  */  
  
  public static class ResortParams implements CRParams {        
    public int passCount; 
    public boolean terminateAtIncrease; 

    ResortParams(double tolerance, int passCount, boolean terminateAtIncrease) {
      this.passCount = passCount;
      this.terminateAtIncrease = terminateAtIncrease;
    }    
    ResortParams() {
      passCount = 10;
      terminateAtIncrease = false;    
    }
  }   
}
