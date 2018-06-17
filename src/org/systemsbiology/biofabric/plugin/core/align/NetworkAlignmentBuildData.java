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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.layouts.EdgeLayout;
import org.systemsbiology.biofabric.layouts.NodeLayout;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.BuildData;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/***************************************************************************
 **
 ** For passing around Network Alignment data
 */

public class NetworkAlignmentBuildData extends BuildData.RelayoutBuildData {
  
  public enum ViewType {GROUP, ORPHAN, CYCLE}
  
  public ArrayList<FabricLink> linksLarge;
  public HashSet<NID.WithName> lonersLarge;
  public Map<NID.WithName, Boolean> mergedToCorrectNC, isAlignedNode;
  public NetworkAlignmentPlugIn.NetAlignStats netAlignStats;
  public ViewType view;
  public Map<NID.WithName, NID.WithName> mapG1toG2;
  public Map<NID.WithName, NID.WithName> perfectMap;
  public List<NID.WithName[]> cycleBounds;
  public boolean forPerfectNG;
  public NodeGroupMap.PerfectNGMode mode;

  public NetworkAlignmentBuildData(UniqueLabeller idGen,
                                   Set<FabricLink> allLinks, Set<NID.WithName> loneNodeIDs,
                                   Map<NID.WithName, Boolean> mergedToCorrectNC,
                                   Map<NID.WithName, Boolean> isAlignedNode,
                                   NetworkAlignmentPlugIn.NetAlignStats netAlignStats,
                                   Map<NID.WithName, String> clustAssign, ViewType view,
                                   Map<NID.WithName, NID.WithName> mapG1toG2,
                                   Map<NID.WithName, NID.WithName> perfectMap,
                                   ArrayList<FabricLink> linksLarge, HashSet<NID.WithName> lonersLarge,
                                   boolean forPerfectNG, NodeGroupMap.PerfectNGMode mode) {

    super(idGen, allLinks, loneNodeIDs, clustAssign, null, BuildData.BuildMode.BUILD_FROM_PLUGIN);
    this.layoutMode = BioFabricNetwork.LayoutMode.PER_NETWORK_MODE;
    this.view = view;
    this.linksLarge = linksLarge;
    this.lonersLarge = lonersLarge;
    this.mergedToCorrectNC = mergedToCorrectNC;
    this.isAlignedNode = isAlignedNode;
    this.netAlignStats = netAlignStats;
    this.mapG1toG2 = mapG1toG2;
    this.perfectMap = perfectMap;
    this.forPerfectNG = forPerfectNG;
    this.mode = mode;
  }

  @Override
  public NodeLayout getNodeLayout() {
    switch (view) {
      case GROUP:
        return (new NetworkAlignmentLayout());
      case ORPHAN:
        return (new DefaultLayout());
      case CYCLE:
        return (new AlignCycleLayout());
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public EdgeLayout getEdgeLayout() {
    switch (view) {
      case GROUP:
        return (new NetworkAlignmentEdgeLayout());
      case ORPHAN:
        return (new DefaultEdgeLayout());
      case CYCLE:
        return (new AlignCycleEdgeLayout());
      default:
        throw new IllegalStateException();
    } 
  }
  
  @Override
  public void processSpecialtyBuildData() {
    UiUtil.fixMePrintout("Stick data into plugin");
  }
}