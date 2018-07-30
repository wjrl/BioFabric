/*
**
**    Copyright (C) 2018 Rishi Desai
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

import org.systemsbiology.biofabric.api.layout.DefaultEdgeLayout;
import org.systemsbiology.biofabric.api.layout.DefaultLayout;
import org.systemsbiology.biofabric.api.layout.EdgeLayout;
import org.systemsbiology.biofabric.api.layout.NodeLayout;
import org.systemsbiology.biofabric.api.model.NetLink;
import org.systemsbiology.biofabric.api.model.NetNode;
import org.systemsbiology.biofabric.plugin.PluginBuildData;

/***************************************************************************
 **
 ** For passing around Network Alignment data
 */

public class NetworkAlignmentBuildData implements PluginBuildData {
  
  public enum ViewType {GROUP, ORPHAN, CYCLE}
  
  public ArrayList<NetLink> linksLarge;
  public HashSet<NetNode> lonersLarge;
  public Set<NetNode> allLargerNodes;
  public Set<NetNode> allSmallerNodes;
  public Map<NetNode, Boolean> mergedToCorrectNC, isAlignedNode;
  public NetworkAlignmentPlugIn.NetAlignStats netAlignStats;
  public ViewType view;
  public Map<NetNode, NetNode> mapG1toG2;
  public Map<NetNode, NetNode> perfectG1toG2;
  public List<AlignCycleLayout.CycleBounds> cycleBounds;
  public NodeGroupMap.PerfectNGMode mode;

  public NetworkAlignmentBuildData(Set<NetNode> allLargerNodes,
                                   Set<NetNode> allSmallerNodes,                                 
                                   Map<NetNode, Boolean> mergedToCorrectNC,
                                   Map<NetNode, Boolean> isAlignedNode,
                                   NetworkAlignmentPlugIn.NetAlignStats netAlignStats,
                                   ViewType view,
                                   Map<NetNode, NetNode> mapG1toG2,
                                   Map<NetNode, NetNode> perfectG1toG2,
                                   ArrayList<NetLink> linksLarge, HashSet<NetNode> lonersLarge,
                                   NodeGroupMap.PerfectNGMode mode) {

    this.allLargerNodes = allLargerNodes;
    this.allSmallerNodes = allSmallerNodes;
    this.view = view;
    this.linksLarge = linksLarge;
    this.lonersLarge = lonersLarge;
    this.mergedToCorrectNC = mergedToCorrectNC;
    this.isAlignedNode = isAlignedNode;
    this.netAlignStats = netAlignStats;
    this.mapG1toG2 = mapG1toG2;
    this.perfectG1toG2 = perfectG1toG2;
    this.mode = mode;
  }

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
}