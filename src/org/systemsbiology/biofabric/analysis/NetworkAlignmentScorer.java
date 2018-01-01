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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.systemsbiology.biofabric.layouts.NetworkAlignmentLayout;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;

import static org.systemsbiology.biofabric.layouts.NetworkAlignmentLayout.NodeGroupMap;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.COVERED_EDGE;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.GRAPH1;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.INDUCED_GRAPH2;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.HALF_UNALIGNED_GRAPH2;
import static org.systemsbiology.biofabric.analysis.NetworkAlignment.FULL_UNALIGNED_GRAPH2;

/****************************************************************************
 **
 ** Calculates topological scores of network alignments: Edge Coverage (EC),
 ** Symmetric Substructure score (S3), Induced Conserved Substructure (ICS);
 **
 ** Node Correctness (NC) is only if we know the perfect alignment.
 *
 ** We created Node Group (NG)/Link Group (LG) ratios distance score:
 ** Basically we find the euclidean distance (N-space) between the given
 ** alignment's vector and perfect alignment's vector, where vector has the
 ** percents of each node/link group size to the whole
 */

public class NetworkAlignmentScorer {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set<FabricLink> linksMain_, linksPerfect_;
  private Set<NID.WithName> loneNodeIDsMain_, loneNodeIDsPerfect_;
  private Map<NID.WithName, Boolean> mergedToCorrect_;
  
  private BTProgressMonitor monitor;
  
  private double EC, S3, ICS, NC, NGDist, LGDist, NGLGDist;
  private ScoreReport report;
  
  public NetworkAlignmentScorer(Set<FabricLink> reducedLinks, Set<NID.WithName> loneNodeIDs,
                                Map<NID.WithName, Boolean> mergedToCorrect, Set<FabricLink> linksPerfect,
                                Set<NID.WithName> loneNodeIDsPerfect, BTProgressMonitor monitor) {
    this.linksMain_ = new HashSet<FabricLink>(reducedLinks);
    this.loneNodeIDsMain_ = new HashSet<NID.WithName>(loneNodeIDs);
    this.mergedToCorrect_ = mergedToCorrect;
    this.linksPerfect_ = linksPerfect;
    this.loneNodeIDsPerfect_ = loneNodeIDsPerfect;
    this.monitor = monitor;
    removeDuplicateAndShadow();
    calcScores();
    this.report = new ScoreReport(EC, S3, ICS, NC, NGDist, LGDist, NGLGDist);
    return;
  }
  
  private void calcScores() {
    calcTopologicalScores();
    calcNodeCorrectness();
    calcNodeGroupValues();
    calcLinkGroupValues();
    calcBothGroupValues();
  }
  
  private void calcTopologicalScores() {
    int numCoveredEdge = 0, numGraph1 = 0, numInducedGraph2 = 0;
    
    for (FabricLink link : linksMain_) {
      if (link.getRelation().equals(COVERED_EDGE)) {
        numCoveredEdge++;
      } else if (link.getRelation().equals(GRAPH1)) {
        numGraph1++;
      } else if (link.getRelation().equals(INDUCED_GRAPH2)) {
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
      EC = -1;
      S3 = -1;
      ICS = -1; // add better error catching
      UiUtil.fixMePrintout("Needs better Net-Align score calculator");
    }
    return;
  }
  
  private void calcNodeCorrectness() {
    if (mergedToCorrect_ == null) {
      NC = -1;
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
  
  private void calcNodeGroupValues() {
    ScoreVector mainAlign = getNodeGroupRatios(linksMain_, loneNodeIDsMain_, mergedToCorrect_, monitor);
    ScoreVector perfectAlign = getNodeGroupRatios(linksPerfect_, loneNodeIDsPerfect_, null, monitor);
    
    NGDist = mainAlign.distance(perfectAlign);
    return;
  }
  
  private void calcLinkGroupValues() {
    ScoreVector mainAlign = getLinkGroupRatios(linksMain_, monitor);
    ScoreVector perfectAlign = getLinkGroupRatios(linksPerfect_, monitor);
    
    LGDist = mainAlign.distance(perfectAlign);
    return;
  }
  
  private void calcBothGroupValues() {
    ScoreVector mainNG = getNodeGroupRatios(linksMain_, loneNodeIDsMain_, mergedToCorrect_, monitor);
    ScoreVector mainLG = getLinkGroupRatios(linksMain_, monitor);
    mainNG.concat(mainLG);
  
    ScoreVector perfectNG = getNodeGroupRatios(linksPerfect_, loneNodeIDsPerfect_, null, monitor);
    ScoreVector perfectLG = getLinkGroupRatios(linksPerfect_, monitor);
    perfectNG.concat(perfectLG);
    
    NGLGDist = mainNG.distance(perfectNG);
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
      
      if (map.get(concat) != null) {
        continue; // skip the duplicate
      } else {
        map.put(concat, link);
      }
    }
    
    linksMain_.clear();
    for (Map.Entry<String, FabricLink> entry : map.entrySet()) {
      linksMain_.add(entry.getValue());
    }
    
    return;
  }
  
  public ScoreReport getReport() {
    return (report);
  }
  
  private static ScoreVector getNodeGroupRatios(Set<FabricLink> links, Set<NID.WithName> loneNodeIDs,
                                                Map<NID.WithName, Boolean> mergedToCorrect,
                                                BTProgressMonitor monitor) {
    
    NodeGroupMap map = new NodeGroupMap(links, loneNodeIDs, mergedToCorrect, NetworkAlignmentLayout.DefaultNodeGroupOrder);
    
    Set<NID.WithName> allNodes;
    try {
      allNodes = BioFabricNetwork.extractNodes(links, loneNodeIDs, monitor);
    } catch (AsynchExitRequestException aere) {
      throw new IllegalStateException();
    }
    
//    UiUtil.fixMePrintout("hard coded values for node group ratios");
    int[] groupCounter = new int[map.numGroups()];
    
    for (NID.WithName node : allNodes) {
      int index = map.getIndex(node);
      groupCounter[index]++;
    }
    
    ScoreVector scoreNG = new ScoreVector(map.numGroups());
    for (int i = 0; i < groupCounter.length; i++) {
      scoreNG.values_[i] = ((double)groupCounter[i]) / ((double)allNodes.size());
//      System.out.println(scoreNG.values_[i] + "  ng " + groupCounter[i]);
    }
    return (scoreNG);
  }
  
  private static ScoreVector getLinkGroupRatios(Set<FabricLink> links, BTProgressMonitor monitor) {
    int[] counts = new int[NetworkAlignmentLayout.NUMBER_LINK_GROUPS];
    
    for (FabricLink link : links) {
      if (link.getRelation().equals(COVERED_EDGE)) {
        counts[0]++;
      } else if (link.getRelation().equals(GRAPH1)) {
        counts[1]++;
      } else if (link.getRelation().equals(INDUCED_GRAPH2)) {
        counts[2]++;
      } else if (link.getRelation().equals(HALF_UNALIGNED_GRAPH2)) {
        counts[3]++;
      } else if (link.getRelation().equals(FULL_UNALIGNED_GRAPH2)) {
        counts[4]++;
      }
    }
    
    ScoreVector scoreLG = new ScoreVector(NetworkAlignmentLayout.NUMBER_LINK_GROUPS);
    
    for (int i = 0; i < counts.length; i++) {
      scoreLG.values_[i] = ((double)counts[i]) / ((double)links.size());
//      System.out.println(scoreLG.values_[i] + "  lg  " + counts[i]);
    }
    return (scoreLG);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /****************************************************************************
   **
   ** Contains common network alignment scores
   */
  
  public static class ScoreReport {
    
    public final double EC, S3, ICS, NC, NGDist, LGDist, NGLGDist;
    
    public ScoreReport(double EC, double S3, double ICS, double NC, double NGDist, double LGDist, double NGLGDist) {
      this.EC = EC;
      this.S3 = S3;
      this.ICS = ICS;
      this.NC = NC;
      this.NGDist = NGDist;
      this.LGDist = LGDist;
      this.NGLGDist = NGLGDist;
    }
  
    @Override
    public String toString() {
      String scores = String.format("SCORES\nEC:%4.4f\nS3:%4.4f\nICS:%4.4f\nNC:%4.4f\nNGD:%4.4f\nLGD:%4.4f\nNGLGD:%4.4f",
              EC, S3, ICS, NC, NGDist, LGDist, NGLGDist);
      JOptionPane.showMessageDialog(null, scores);
      System.out.println(scores);
      return (scores);
    }
  }
  
  /****************************************************************************
   **
   ** N-dimensional vector used for scores
   */
  
  private static class ScoreVector {
    
    public double[] values_;
    
    public ScoreVector(int size) {
      this.values_ = new double[size];
    }
  
    /****************************************************************************
     **
     ** Euclidean distance between two vectors
     */
    
    public double distance(ScoreVector vector) {
      if (this.values_.length!= vector.values_.length) {
        throw new IllegalArgumentException("score vector length not equal");
      }
      
      double ret = 0;
  
      for (int i = 0; i < values_.length; i++) {
//        System.out.println(ret + " ret is here");
        ret += (this.values_[i] - vector.values_[i]) * (this.values_[i] - vector.values_[i]);
      }
      ret = Math.pow(ret, .5);
      return (ret);
    }
  
    /****************************************************************************
     **
     ** Concatenating two vectors: [A,B,C] + [D,E] -> [A,B,C,D,E]
     */
    
    public void concat(ScoreVector vector) {
      
      double[] concated = new double[this.values_.length + vector.values_.length];
  
      int count = 0;
      for (int i = 0; i < this.values_.length; i++) {
        concated[i] = this.values_[i];
        count++;
      }
      for (int i = 0; i < vector.values_.length; i++) {
        concated[count] = vector.values_[i];
        count++;
      }
      this.values_ = concated;
      return;
    }
    
  }

}



