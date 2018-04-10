/*
**    File created by Rishi Desai
**
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

package org.systemsbiology.biofabric.plugin.core.align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.BuildData;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;

public class NetworkAlignmentEdgeLayout extends DefaultEdgeLayout {
  
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
  
  public NetworkAlignmentEdgeLayout() {
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Do necessary pre-processing steps (e.g. automatic assignment to link groups)
   */
  
  @Override
  public void preProcessEdges(BuildData.RelayoutBuildData rbd,
                              BTProgressMonitor monitor) throws AsynchExitRequestException {
    installLinkGroups(rbd, monitor);
    return;
  }
  
  @Override
  public void layoutEdges(BuildData.RelayoutBuildData rbd,
                          BTProgressMonitor monitor) throws AsynchExitRequestException {
    super.layoutEdges(rbd, monitor);
    this.installLinkAnnotations(rbd, monitor);
  }
  
  /***************************************************************************
  **
  ** Install Link groups for network alignments in this order:
  ** 1) All Covered Edges  2) uncovered G1  3) induced G2  4) half aligned half unaligned G2  5) full unaligned G2
  ** Note: some link groups may not be present.
  */

  private void installLinkGroups(BuildData.RelayoutBuildData rbd, BTProgressMonitor monitor)
          throws AsynchExitRequestException {
  
    LoopReporter lr = new LoopReporter(rbd.allLinks.size(), 20, monitor, 0.0, 1.0, "progress.orderingLinkGroups");
    Set<String> relations = new HashSet<String>();
    for (FabricLink link : rbd.allLinks) {
      relations.add(link.getRelation());
      lr.report();
    }
    lr.finish();
  
    List<String> groupOrder = new ArrayList<String>(relations);
  
    // trivial operation (group order is at most length 5)
    Collections.sort(groupOrder, new NetAlignLinkGroupLocator());
  
    rbd.setGroupOrderAndMode(groupOrder, BioFabricNetwork.LayoutMode.PER_NETWORK_MODE);
  
    return;
  }
  
  /***************************************************************************
   **
   ** Install Link Annotations (link order must be calculated prior to this)
   */
  
  private void installLinkAnnotations(BuildData.RelayoutBuildData rbd, BTProgressMonitor monitor)
    throws AsynchExitRequestException {
  
    LoopReporter lr = new LoopReporter(rbd.linkOrder.size(), 20, monitor, 0, .25, "progress.linkAnnotationSifting");
  
    List<FabricLink> nonShdw = new ArrayList<FabricLink>(), withShdw = new ArrayList<FabricLink>();
  
    for (Map.Entry<Integer, FabricLink> entry : rbd.linkOrder.entrySet()) {
      FabricLink link = entry.getValue();
      if (!link.isShadow()) {
        nonShdw.add(link);
      }
      withShdw.add(link);
      lr.report();
    }
    
    AnnotationSet nonShdwAnnots = findLinkGroupIntervals(nonShdw, monitor);
    AnnotationSet withShdwAnnots = findLinkGroupIntervals(withShdw, monitor);
    
    Map<Boolean, AnnotationSet> linkAnnots = new HashMap<Boolean, AnnotationSet>();
    linkAnnots.put(true, withShdwAnnots);
    linkAnnots.put(false, nonShdwAnnots);
    
    rbd.setLinkAnnotations(linkAnnots);
    return;
  }
  
  /***************************************************************************
   **
   ** Find link group intervals (same algorithm as Drain Zone calculator
   */
  
  private AnnotationSet findLinkGroupIntervals(List<FabricLink> linkSet, BTProgressMonitor monitor)
    throws AsynchExitRequestException {
  
    LoopReporter lr = new LoopReporter(linkSet.size(), 20, monitor, 0, .25, "progress.linkAnnotations");
  
    AnnotationSet annots = new AnnotationSet(); // same as multiple drain zone algorithm
  
    int startIdx = 0;
    String currentRel = linkSet.get(startIdx).getRelation(); // these keep track of start of interval and interval's relation
  
    for (int index = 0; index <= linkSet.size(); index++) {
      lr.report();
      if (index == linkSet.size()) {
      
        int endIdx = linkSet.size() - 1;
      
        AnnotationSet.Annot annot = new AnnotationSet.Annot(currentRel, startIdx, endIdx, 0, null);
        annots.addAnnot(annot);
        
      } else if (! linkSet.get(index).getRelation().equals(currentRel)) {
      
        int endIdx = index - 1;  // backtrack one position
  
        AnnotationSet.Annot annot = new AnnotationSet.Annot(currentRel, startIdx, endIdx, 0, null);
        annots.addAnnot(annot);
      
        startIdx = index;                              // update the start index
        currentRel = linkSet.get(index).getRelation(); // update the current relation whose interval we're calculating
      }
    }
    return (annots);
  }
  
  /***************************************************************************
   **
   ** Link groups for network alignments in this order:
   ** 1) All Covered Edges  2) uncovered G1  3) induced G2  4) half aligned half unaligned G2  5) full unaligned G2
   */
  
  private static class NetAlignLinkGroupLocator implements Comparator<String> {
    
    /***************************************************************************
     **
     ** Use fabricated indexes to enforce default link group order
     */
    
    public int compare(String rel1, String rel2) {
      return (getIndex(rel1) - getIndex(rel2));
    }
    
    /***************************************************************************
     **
     ** Create an integer index to enforce the default link group order
     */
    
    private int getIndex(String rel) {
      
      if(rel.equals(NetworkAlignment.COVERED_EDGE)) {
        return 1;
        
      } else if (rel.equals(NetworkAlignment.GRAPH1)) {
        return 2;
        
      } else if (rel.equals(NetworkAlignment.INDUCED_GRAPH2)) {
        return 3;
        
      } else if (rel.equals(NetworkAlignment.HALF_UNALIGNED_GRAPH2)) {
        return 4;
        
      } else if (rel.equals(NetworkAlignment.FULL_UNALIGNED_GRAPH2)) {
        return 5;
        
      } else {
        throw new IllegalArgumentException();
      }
    }
    
  }
  
}
