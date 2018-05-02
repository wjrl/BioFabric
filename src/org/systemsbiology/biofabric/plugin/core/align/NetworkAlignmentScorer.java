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
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;


/****************************************************************************
 **
 ** Calculates topological scores of network alignments: Edge Coverage (EC),
 ** Symmetric Substructure score (S3), Induced Conserved Substructure (ICS);
 **
 ** Node Correctness (NC) and Jaccard Similarity (JS) are calculatable
 ** only if we know the perfect alignment.
 **
 ** NGD and LGD are the cosine similarity between the normalized ratio vectors
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
  private Map<NID.WithName, Boolean> mergedToCorrect_;
  
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
  
  private Double EC, S3, ICS, NC, NGD, LGD, JaccSim;
  private NetworkAlignmentPlugIn.NetAlignStats netAlignStats_;
  
  public NetworkAlignmentScorer(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                Map<NID.WithName, Boolean> mergedToCorrect, Map<NID.WithName, Boolean> isAlignedNode,
                                Map<NID.WithName, Boolean> isAlignedNodePerfect,
                                Set<FabricLink> linksPerfect, Set<NID.WithName> loneNodeIDsPerfect,
                                ArrayList<FabricLink> linksSmall, HashSet<NID.WithName> lonersSmall,
                                ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2,
                                BTProgressMonitor monitor) {
    this.linksMain_ = new HashSet<FabricLink>(reducedLinks);
    this.loneNodeIDsMain_ = new HashSet<NID.WithName>(loneNodeIDs);
    this.mergedToCorrect_ = mergedToCorrect;
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
    this.groupMapMain_ = new NodeGroupMap(reducedLinks, loneNodeIDs, mergedToCorrect, isAlignedNode, false, NodeGroupMap.PerfectNGMode.NONE,
            NetworkAlignmentLayout.defaultNGOrderWithoutCorrect, NetworkAlignmentLayout.ngAnnotColorsWithoutCorrect);
    if (mergedToCorrect != null) {
      this.groupMapPerfect_ = new NodeGroupMap(linksPerfect, loneNodeIDsPerfect, mergedToCorrect, isAlignedNodePerfect, false, NodeGroupMap.PerfectNGMode.NONE,
              NetworkAlignmentLayout.defaultNGOrderWithoutCorrect, NetworkAlignmentLayout.ngAnnotColorsWithoutCorrect);
    }
    
    removeDuplicateAndShadow();
    generateStructs(reducedLinks, loneNodeIDs, nodeToLinksMain_, nodeToNeighborsMain_);
    if (mergedToCorrect != null) {
      generateStructs(linksPerfect, loneNodeIDsPerfect, nodeToLinksPerfect_, nodeToNeighborsPerfect_);
    }
    calcScores();
    finalizeMeasures();
    return;
  }
  
  private void removeDuplicateAndShadow() {
    Set<FabricLink> nonShdwLinks = new HashSet<FabricLink>();
    for (FabricLink link : linksMain_) {
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
    
    Map<String, FabricLink> map = new HashMap<String, FabricLink>();
    for (FabricLink link : nonShdwLinks) {
      
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
  
  private void generateStructs(Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs, Map<NID.WithName, Set<FabricLink>> nodeToLinks_, Map<NID.WithName, Set<NID.WithName>> nodeToNeighbors_) {
    
    for (FabricLink link : allLinks) {
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
  
  private void calcScores() {
    calcTopologicalScores();
  
    if (mergedToCorrect_ != null) { // must have perfect alignment for these measures
      calcNodeCorrectness();
      calcGroupDistance();
      calcJaccardSimilarity();
    }
  }
  
  private void finalizeMeasures() {
    NetworkAlignmentPlugIn.NetAlignMeasure[] possibleMeasures = {
            new NetworkAlignmentPlugIn.NetAlignMeasure("Edge Coverage", EC),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Symmetric Substructure Score", S3),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Induced Conserved Structure", ICS),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Node Correctness", NC),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Node Group Distance", NGD),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Link Group Distance", LGD),
            new NetworkAlignmentPlugIn.NetAlignMeasure("Jaccard Similarity", JaccSim),
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
  
  private void calcTopologicalScores() {
    int numCoveredEdge = 0, numGraph1 = 0, numInducedGraph2 = 0;
    
    for (FabricLink link : linksMain_) {
      if (link.getRelation().equals(NetworkAlignment.COVERED_EDGE)) {
        numCoveredEdge++;
      } else if (link.getRelation().equals(NetworkAlignment.GRAPH1)) {
        numGraph1++;
      } else if (link.getRelation().equals(NetworkAlignment.INDUCED_GRAPH2)) {
        numInducedGraph2++;
      }
    }
    
    if (numCoveredEdge == 0) {
      return;
    }
    
    try {
      EC = ((double) numCoveredEdge) / (numCoveredEdge + numGraph1);
      S3 = ((double) numCoveredEdge) / (numCoveredEdge + numGraph1 + numInducedGraph2);
      ICS = ((double) numCoveredEdge) / (numCoveredEdge + numInducedGraph2); // this is correct right?
    } catch (ArithmeticException ae) {
      EC = null;
      S3 = null;
      ICS = null; // add better error catching
      UiUtil.fixMePrintout("Needs better Net-Align score calculator");
    }
    return;
  }
  
  private void calcNodeCorrectness() {
    if (mergedToCorrect_ == null) {
      NC = null;
      return;
    }
    
    int numCorrect = 0;
    for (Map.Entry<NID.WithName, Boolean> node : mergedToCorrect_.entrySet()) {
      if (node.getValue()) {
        numCorrect++;
      }
    }
    NC = ((double)numCorrect) / (mergedToCorrect_.size());
    return;
  }
  
  private void calcGroupDistance() {
    GroupDistance gd = new GroupDistance();
    NGD = gd.calcNGD(groupMapMain_, groupMapPerfect_);
    LGD = gd.calcLGD(groupMapMain_, groupMapPerfect_);
    return;
  }
  
  private void calcJaccardSimilarity() {
    this.JaccSim = new JaccardSimilarity().calcScore(mapG1toG2_, perfectG1toG2_, linksLarge_, lonersLarge_);
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
   ** NGD and LGD
   */
  
  private static class GroupDistance {
  
    /***************************************************************************
     **
     ** Calculated the score
     */
  
    double calcNGD(NodeGroupMap groupMapMain, NodeGroupMap groupMapPerfect) {
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
  
    double calcLGD(NodeGroupMap groupMapMain, NodeGroupMap groupMapPerfect) {
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
  
  private static class JaccardSimilarity {
  
    /***************************************************************************
     **
     ** Calculated the score
     */
  
    double calcScore(Map<NID.WithName, NID.WithName> mapG1toG2, Map<NID.WithName, NID.WithName> perfectG1toG2,
                     ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge) {
  
      Map<NID.WithName, NID.WithName> entrezAlign = constructEntrezAlign(mapG1toG2, perfectG1toG2);
      Map<NID.WithName, Set<NID.WithName>> nodeToNeigh = makeNodeToNeigh(linksLarge, lonersLarge);
    
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
     ** Match up G2-aligned nodes with G2-aligned nodes in perfect alignment
     */
    
    private Map<NID.WithName, NID.WithName> constructEntrezAlign(Map<NID.WithName, NID.WithName> mapG1toG2,
                                                                 Map<NID.WithName, NID.WithName> perfectG1toG2) {
      Map<NID.WithName, NID.WithName> ret = new HashMap<NID.WithName, NID.WithName>();
      for (NID.WithName node : mapG1toG2.keySet()) {
        NID.WithName converted = perfectG1toG2.get(node);
        if (converted == null) {
          //System.err.println("no Entrez match for " + node);
          continue;
        }
        NID.WithName matchedWith = mapG1toG2.get(node);
        ret.put(converted, matchedWith);
      }
      return (ret);
    }
    
    /***************************************************************************
     **
     ** Construct node to neighbor map
     */
    
    private Map<NID.WithName, Set<NID.WithName>> makeNodeToNeigh(ArrayList<FabricLink> links, HashSet<NID.WithName> loners) {
  
      Map<NID.WithName, Set<NID.WithName>> ret = new HashMap<NID.WithName, Set<NID.WithName>>();
  
      for (FabricLink link : links) {
        NID.WithName src = link.getSrcID(), trg = link.getTrgID();
  
        if (ret.get(src) == null) {
          ret.put(src, new HashSet<NID.WithName>());
        }
        if (ret.get(trg) == null) {
          ret.put(trg, new HashSet<NID.WithName>());
        }
        ret.get(src).add(trg);
        ret.get(trg).add(src);
      }
      
      for (NID.WithName node : loners) {
        ret.put(node, new HashSet<NID.WithName>());
      }
      return (ret);
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