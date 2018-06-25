/*
**
**    File created by Rishi Desai
**
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;


/****************************************************************************
 **
 ** Calculates topological scores of network alignments: Edge Coverage (EC),
 ** Symmetric Substructure score (S3), Induced Conserved Substructure (ICS);
 **
 ** Node Correctness (NC) and Jaccard Similarity (JS) are calculatable
 ** only if we know the perfect alignment.
 **
 ** NGS and LGS are the angular similarity between the normalized ratio vectors
 ** of the respective node groups and link groups of the main
 ** alignment and the perfect alignment.
 */

public class NetworkAlignmentScorer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  //
  // Keep track of both the main alignment and perfect alignment's info
  //
  
  private Set<FabricLink> linksMain_, linksPerfect_;
  private Set<NID.WithName> loneNodeIDsMain_, loneNodeIDsPerfect_;
  private Map<NID.WithName, Boolean> isAlignedNodeMain_, isAlignedNodePerfect_;
  private Map<NID.WithName, Boolean> mergedToCorrectNC_;
  
  //
  // This are from original untouched graphs and alignments
  //
  
  private ArrayList<FabricLink> linksSmall_, linksLarge_;
  private HashSet<NID.WithName> lonersSmall_, lonersLarge_;
  private Map<NID.WithName, NID.WithName> mapG1toG2_, perfectG1toG2_;
  
  private BTProgressMonitor monitor_;
  
  private Map<NID.WithName, Set<FabricLink>> nodeToLinksMain_, nodeToLinksPerfect_;
  private Map<NID.WithName, Set<NID.WithName>> nodeToNeighborsMain_, nodeToNeighborsPerfect_;
  private NodeGroupMap groupMapMain_, groupMapPerfect_;
  
  //
  // The scores
  //
  
  private Double EC, S3, ICS, NC, NGS, LGS, JaccSim;
  private NetworkAlignmentPlugIn.NetAlignStats netAlignStats_;
  
  public NetworkAlignmentScorer(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                Map<NID.WithName, Boolean> mergedToCorrectNC, Map<NID.WithName, Boolean> isAlignedNode,
                                Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                Set<FabricLink> linksPerfect, Set<NID.WithName> loneNodeIDsPerfect,
                                ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2,
                                BTProgressMonitor monitor) throws AsynchExitRequestException {
    this.linksMain_ = new HashSet<FabricLink>(reducedLinks);
    this.loneNodeIDsMain_ = new HashSet<NID.WithName>(loneNodeIDs);
    this.mergedToCorrectNC_ = mergedToCorrectNC;
    this.linksPerfect_ = linksPerfect;
    this.loneNodeIDsPerfect_ = loneNodeIDsPerfect;
    this.isAlignedNodeMain_ = isAlignedNode;
    this.isAlignedNodePerfect_ = isAlignedNodePerfect;
    this.monitor_ = monitor;
    this.nodeToLinksMain_ = new HashMap<NID.WithName, Set<FabricLink>>();
    this.nodeToNeighborsMain_ = new HashMap<NID.WithName, Set<NID.WithName>>();
    this.nodeToLinksPerfect_ = new HashMap<NID.WithName, Set<FabricLink>>();
    this.nodeToNeighborsPerfect_ = new HashMap<NID.WithName, Set<NID.WithName>>();
    this.linksSmall_ = linksSmall;
    this.lonersSmall_ = lonersSmall;
    this.linksLarge_ = linksLarge;
    this.lonersLarge_ = lonersLarge;
    this.mapG1toG2_ = mapG1toG2;
    this.perfectG1toG2_ = perfectG1toG2;
    this.groupMapMain_ = new NodeGroupMap(reducedLinks, loneNodeIDs, mapG1toG2, perfectG1toG2, linksLarge, lonersLarge,
            mergedToCorrectNC, isAlignedNode, NodeGroupMap.PerfectNGMode.NONE,
            NetworkAlignmentLayout.defaultNGOrderWithoutCorrect, NetworkAlignmentLayout.ngAnnotColorsWithoutCorrect, monitor);
    if (mergedToCorrectNC != null) {
      this.groupMapPerfect_ = new NodeGroupMap(linksPerfect, loneNodeIDsPerfect, mapG1toG2, perfectG1toG2, linksLarge,
              lonersLarge, mergedToCorrectNC, isAlignedNodePerfect, NodeGroupMap.PerfectNGMode.NONE,
              NetworkAlignmentLayout.defaultNGOrderWithoutCorrect, NetworkAlignmentLayout.ngAnnotColorsWithoutCorrect, monitor);
    }
    removeDuplicateAndShadow();
    generateStructs(reducedLinks, loneNodeIDs, nodeToLinksMain_, nodeToNeighborsMain_);
    if (mergedToCorrectNC != null) {
      generateStructs(linksPerfect, loneNodeIDsPerfect, nodeToLinksPerfect_, nodeToNeighborsPerfect_);
    }
    calcScores();
    finalizeMeasures();
    return;
  }
  
  private void removeDuplicateAndShadow() throws AsynchExitRequestException {
    LoopReporter lr = new LoopReporter(linksMain_.size(), 20, monitor_, 0.0, 1.0, "progress.filteringLinksA");
    Set<FabricLink> nonShdwLinks = new HashSet<FabricLink>();
    for (FabricLink link : linksMain_) {
      lr.report();
      if (! link.isShadow()) { // remove shadow links
        nonShdwLinks.add(link);
      }
    }
    
    //
    // We have to remove synonymous links (a->b) same as (b->a), and keep one;
    // Sort the names and concat into string (the key), so they are the same key in the map.
    // This means (a->b) and (b->a) should make the same string key.
    // If the key already has a value, we got a duplicate link.
    //
  
    lr = new LoopReporter(nonShdwLinks.size(), 20, monitor_, 0.0, 1.0, "progress.filteringLinksB");
    Map<String, FabricLink> map = new HashMap<String, FabricLink>();
    for (FabricLink link : nonShdwLinks) {
      lr.report();
      String[] arr1 = {link.getSrcID().getName(), link.getTrgID().getName()};
      Arrays.sort(arr1);
      String concat = String.format("%s___%s", arr1[0], arr1[1]);
      
      if (map.get(concat) == null) {
        map.put(concat, link);
      } // skip duplicates
    }
    
    linksMain_.clear();
    for (Map.Entry<String, FabricLink> entry : map.entrySet()) {
      linksMain_.add(entry.getValue());
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Create structures (node-to-neighbors and node-to-inks
   */
  
  private void generateStructs(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName,
          Set<FabricLink>> nodeToLinks_, Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_) throws AsynchExitRequestException {
    
    LoopReporter lr = new LoopReporter(allLinks.size(), 20, monitor_, 0.0, 1.0, "progress.generatingStructures");
    for (FabricLink link : allLinks) {
      lr.report();
      NID.WithName src = link.getSrcID(), trg = link.getTrgID();
      
      if (nodeToLinks_.get(src) == null) {
        nodeToLinks_.put(src, new HashSet<FabricLink>());
      }
      if (nodeToLinks_.get(trg) == null) {
        nodeToLinks_.put(trg, new HashSet<FabricLink>());
      }
      if (nodeToNeighbors_.get(src) == null) {
        nodeToNeighbors_.put(src, new HashSet<NID.WithName>());
      }
      if (nodeToNeighbors_.get(trg) == null) {
        nodeToNeighbors_.put(trg, new HashSet<NID.WithName>());
      }
      
      nodeToLinks_.get(src).add(link);
      nodeToLinks_.get(trg).add(link);
      nodeToNeighbors_.get(src).add(trg);
      nodeToNeighbors_.get(trg).add(src);
    }
    
    for (NID.WithName node : loneNodeIDs) {
      nodeToLinks_.put(node, new HashSet<FabricLink>());
      nodeToNeighbors_.put(node, new HashSet<NID.WithName>());
    }
    return;
  }
  
  /****************************************************************************
   **
   ** Calculate the scores!
   */
  
  private void calcScores() throws AsynchExitRequestException {
    calcTopologicalScores();
  
    if (mergedToCorrectNC_ != null) { // must have perfect alignment for these measures
      calcNodeCorrectness();
      calcGroupSimilarity();
      calcJaccardSimilarity();
    }
  }
  
  /****************************************************************************
   **
   ** Create the Measure list and filter out 'null' measures
   */
  
  private void finalizeMeasures() {
    ResourceManager rMan = ResourceManager.getManager();
    String
            ECn = rMan.getString("networkAlignment.edgeCoverage"),
            S3n = rMan.getString("networkAlignment.symmetricSubstructureScore"),
            ICSn = rMan.getString("networkAlignment.inducedConservedStructure"),
            NCn = rMan.getString("networkAlignment.nodeCorrectness"),
            NGSn = rMan.getString("networkAlignment.nodeGroupSimilarity"),
            LGSn = rMan.getString("networkAlignment.linkGroupSimilarity"),
            JSn = rMan.getString("networkAlignment.jaccardSimilarity");
    
    NetworkAlignmentPlugIn.NetAlignMeasure[] possibleMeasures = {
            new NetworkAlignmentPlugIn.NetAlignMeasure(ECn, EC),
            new NetworkAlignmentPlugIn.NetAlignMeasure(S3n, S3),
            new NetworkAlignmentPlugIn.NetAlignMeasure(ICSn, ICS),
            new NetworkAlignmentPlugIn.NetAlignMeasure(NCn, NC),
            new NetworkAlignmentPlugIn.NetAlignMeasure(NGSn, NGS),
            new NetworkAlignmentPlugIn.NetAlignMeasure(LGSn, LGS),
            new NetworkAlignmentPlugIn.NetAlignMeasure(JSn, JaccSim),
    };
  
    List<NetworkAlignmentPlugIn.NetAlignMeasure> measures = new ArrayList<NetworkAlignmentPlugIn.NetAlignMeasure>();
    for (NetworkAlignmentPlugIn.NetAlignMeasure msr : possibleMeasures) {
      if (msr.val != null) { // no point having null measures
        measures.add(msr);
      }
    }
    this.netAlignStats_ = new NetworkAlignmentPlugIn.NetAlignStats(measures);
    return;
  }
  
  private void calcTopologicalScores() throws AsynchExitRequestException{
    LoopReporter lr = new LoopReporter(linksMain_.size(), 20, monitor_, 0.0, 1.0, "progress.topologicalMeasures");
    int numCoveredEdge = 0, numGraph1 = 0, numInducedGraph2 = 0;
    
    for (FabricLink link : linksMain_) {
      lr.report();
      if (link.getRelation().equals(NetworkAlignment.COVERED_EDGE)) {
        numCoveredEdge++;
      } else if (link.getRelation().equals(NetworkAlignment.GRAPH1)) {
        numGraph1++;
      } else if (link.getRelation().equals(NetworkAlignment.INDUCED_GRAPH2)) {
        numInducedGraph2++;
      }
    }
    
    try {
      EC = ((double) numCoveredEdge) / (numCoveredEdge + numGraph1);
      S3 = ((double) numCoveredEdge) / (numCoveredEdge + numGraph1 + numInducedGraph2);
      ICS = ((double) numCoveredEdge) / (numCoveredEdge + numInducedGraph2);
    } catch (ArithmeticException ae) {
      EC = null;
      S3 = null;
      ICS = null;
    }
    return;
  }
  
  private void calcNodeCorrectness() {
    if (mergedToCorrectNC_ == null) {
      NC = null;
      return;
    }
    
    int numCorrect = 0;
    for (Map.Entry<NID.WithName, Boolean> node : mergedToCorrectNC_.entrySet()) {
      if (node.getValue()) {
        numCorrect++;
      }
    }
    NC = ((double)numCorrect) / (mergedToCorrectNC_.size());
    return;
  }
  
  private void calcGroupSimilarity() {
    GroupSimilarity gd = new GroupSimilarity();
    NGS = gd.calcNGS(groupMapMain_, groupMapPerfect_);
    LGS = gd.calcLGS(groupMapMain_, groupMapPerfect_);
    return;
  }
  
  private void calcJaccardSimilarity() throws AsynchExitRequestException {
    this.JaccSim = new JaccardSimilarityScore().calcScore(mapG1toG2_, perfectG1toG2_, linksLarge_, lonersLarge_, monitor_);
    return;
  }
  
  public NetworkAlignmentPlugIn.NetAlignStats getNetAlignStats() {
    return (netAlignStats_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /****************************************************************************
   **
   ** N-dimensional vector used for scores
   */

  private static class VectorND {
  
    private double[] values_;
  
    public VectorND(int size) {
      this.values_ = new double[size];
    }
  
    public double get(int index) {
      return (values_[index]);
    }
  
    public void set(int index, double val) {
      values_[index] = val;
      return;
    }
  
    /****************************************************************************
     **
     ** Euclidean distance between two vectors
     */
  
    public double distance(VectorND vector) {
      if (this.values_.length!= vector.values_.length) {
        throw new IllegalArgumentException("score vector length not equal");
      }
      double ret = 0;
      for (int i = 0; i < values_.length; i++) {
        ret += (this.values_[i] - vector.values_[i]) * (this.values_[i] - vector.values_[i]);
      }
      ret = Math.pow(ret, .5);
      return (ret);
    }
  
    /****************************************************************************
     **
     ** Magnitude
     */
  
    public double magnitude() {
      double ret = this.dot(this);
      ret = Math.sqrt(ret);
      return (ret);
    }
  
    /****************************************************************************
     **
     ** Normalize
     */
  
    public void normalize() {
      double mag = magnitude();
      if (mag == 0) {
        return;
      }
      for (int i = 0; i < values_.length; i++) {
        values_[i] /= mag;
      }
      return;
    }
  
    /****************************************************************************
     **
     ** Dot product
     */
  
    public double dot(VectorND vector) {
      if (this.values_.length != vector.values_.length) {
        throw new IllegalArgumentException("score vector length not equal");
      }
      double ret = 0;
      for (int i = 0; i < this.values_.length; i++) {
        ret += this.values_[i] * vector.values_[i];
      }
      return (ret);
    }
  
    /****************************************************************************
     **
     ** Cosine similarity = returns cos(angle)
     */
  
    public double cosSim(VectorND vector) {
      double cosTheta = dot(vector) / (this.magnitude() * vector.magnitude());
      return (cosTheta);
    }
  
    /****************************************************************************
     **
     ** Angular Similarity = 1 - (2 * arccos(similarity) / pi)
     **
     ** similarity = cos(angle)
     */
    
    public double angSim(VectorND vector) {
      double sim = 1 - (2 * Math.acos(cosSim(vector)) / Math.PI);
      return (sim);
    }
    
    @Override
    public String toString() {
      return "VectorND{" +
              "values_=" + Arrays.toString(values_) +
              '}';
    }
  
    @Override
    public boolean equals(Object o) {
      if (this == o) return (true);
      if (! (o instanceof VectorND)) {
        return (false);
      }
      VectorND vectorND = (VectorND) o;
      if (! Arrays.equals(values_, vectorND.values_)) {
        return (false);
      }
      return (true);
    }
  
    @Override
    public int hashCode() {
      return (Arrays.hashCode(values_));
    }
    
  }
  
  /****************************************************************************
   **
   ** NGS and LGS - with Angular similarity
   */
  
  private static class GroupSimilarity {
  
    /***************************************************************************
     **
     ** Calculated the score
     */
  
    double calcNGS(NodeGroupMap groupMapMain, NodeGroupMap groupMapPerfect) {
      VectorND main = getNGVector(groupMapMain), perfect = getNGVector(groupMapPerfect);
      double score = main.angSim(perfect);
      return (score);
    }
  
    /***************************************************************************
     **
     ** Convert ratio to vector
     */
    
    private VectorND getNGVector(NodeGroupMap groupMap) {
      VectorND vector = new VectorND(groupMap.numGroups());
      Map<String, Double> ngRatios = groupMap.getNodeGroupRatios();
  
      for (Map.Entry<String, Double> entry : ngRatios.entrySet()) {
        int index = groupMap.getIndex(entry.getKey());
        vector.set(index, entry.getValue());
      }
      vector.normalize();
      return (vector);
    }
  
    /***************************************************************************
     **
     ** Calculated the score
     */
  
    double calcLGS(NodeGroupMap groupMapMain, NodeGroupMap groupMapPerfect) {
      VectorND main = getLGVector(groupMapMain), perfect = getLGVector(groupMapPerfect);
      double score = main.angSim(perfect);
      return (score);
    }
  
    /***************************************************************************
     **
     ** Convert ratio to vector
     */
    
    private VectorND getLGVector(NodeGroupMap groupMap) {

      Map<String, Integer> relToIndex = new HashMap<String, Integer>();
      relToIndex.put(NetworkAlignment.COVERED_EDGE, NodeGroupMap.PURPLE_EDGES);
      relToIndex.put(NetworkAlignment.GRAPH1, NodeGroupMap.BLUE_EDGES);
      relToIndex.put(NetworkAlignment.INDUCED_GRAPH2, NodeGroupMap.RED_EDGES);
      relToIndex.put(NetworkAlignment.HALF_UNALIGNED_GRAPH2, NodeGroupMap.ORANGE_EDGES);
      relToIndex.put(NetworkAlignment.FULL_UNALIGNED_GRAPH2, NodeGroupMap.YELLOW_EDGES);
      
      VectorND vector = new VectorND(NodeGroupMap.NUMBER_LINK_GROUPS);
      Map<String, Double> lgRatios = groupMap.getLinkGroupRatios();
      
      for (Map.Entry<String, Double> entry : lgRatios.entrySet()) {
        int index = relToIndex.get(entry.getKey());
        vector.set(index, entry.getValue());
      }
      vector.normalize();
      return (vector);
    }
    
  }
  
  /****************************************************************************
   **
   ** Jaccard Similarity Measure - Adapted from NodeEQC.java
   */
  
  private static class JaccardSimilarityScore {
  
    /***************************************************************************
     **
     ** Calculated the score
     */
  
    double calcScore(Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2,
                     ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                     BTProgressMonitor monitor) throws AsynchExitRequestException {
  
      NodeGroupMap.JaccardSimilarityFunc funcJS =
              new NodeGroupMap.JaccardSimilarityFunc(mapG1toG2, perfectG1toG2, linksLarge, lonersLarge, monitor);
      Map<NID.WithName, NID.WithName> entrezAlign = funcJS.entrezAlign;
      Map<NID.WithName, Set<NID.WithName>> nodeToNeigh = funcJS.nodeToNeighL;
  
      HashSet<NID.WithName> union = new HashSet<NID.WithName>();
      HashSet<NID.WithName> intersect = new HashSet<NID.WithName>();
      HashSet<NID.WithName> scratchNode = new HashSet<NID.WithName>();
      HashSet<NID.WithName> scratchMatch = new HashSet<NID.WithName>();
      double totJ = 0.0;
      int numEnt = 0;
    
      for (NID.WithName node : entrezAlign.keySet()) {
        int lenAdjust = 0;
        NID.WithName match = entrezAlign.get(node);
        Set<NID.WithName> neighOfNode = nodeToNeigh.get(node);
        Set<NID.WithName> neighOfMatch = nodeToNeigh.get(match);
        scratchNode.clear();
        scratchNode.addAll(neighOfNode);
        scratchMatch.clear();
        scratchMatch.addAll(neighOfMatch);
        if (scratchNode.contains(match)) {
          scratchNode.remove(match);
          scratchMatch.remove(node);
          lenAdjust = 1;
        }
        union.clear();
        union(scratchNode, scratchMatch, union);
        intersect.clear();
        intersection(scratchNode, scratchMatch, intersect);
        int uSize = union.size() + lenAdjust;
        int iSize = intersect.size() + lenAdjust;
        double jaccard = (double)(iSize) / (double)uSize;
        totJ += jaccard;
        numEnt++;
      }
      double score = totJ / numEnt;
    
      return (score);
    }
  
    /***************************************************************************
     **
     ** Set intersection helper
     */
  
    private  <T> Set<T> intersection(Set<T> one, Set<T> two, Set<T> result) {
      result.clear();
      result.addAll(one);
      result.retainAll(two);
      return (result);
    }
  
    /***************************************************************************
     **
     ** Set union helper
     */
  
    private  <T> Set<T> union(Set<T> one, Set<T> two, Set<T> result) {
      result.clear();
      result.addAll(one);
      result.addAll(two);
      return (result);
    }
    
  }

}
