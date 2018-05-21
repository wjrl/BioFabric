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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BuildData;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This is the default layout algorithm for edges. Actually usable in combination 
** with a wide variety of different node layout algorithms, not just the default
*/

public class AlignCycleEdgeLayout extends DefaultEdgeLayout {
  
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

  public AlignCycleEdgeLayout() {
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
    
    List<String> groupOrder = new ArrayList<String>();
    groupOrder.add("G1A");
    groupOrder.add("G12");
    groupOrder.add("G2A");
    rbd.setGroupOrderAndMode(groupOrder, BioFabricNetwork.LayoutMode.PER_NODE_MODE);  
    return;
  } 
  
  /***************************************
  **
  ** Write out link annotation file
  
    
  private AnnotationSet writeLinkAnnotBlockCycle(List<String> links, List<NID.WithName> nodes, PrintWriter out, boolean shadow, List<String[]> bounds) throws IOException {    
         
    HashMap<NID.WithName, Integer> nodeOrder = new HashMap<NID.WithName, Integer>();
    for (int i = 0; i < nodes.size(); i++) {
      nodeOrder.put(nodes.get(i), Integer.valueOf(i));      
    } 
    String shadTag = (shadow) ? "isShadow" : "noShadow";
    // Purple "853996-TRM11 (G12) 855782-TRM112"  "855114-YTA12 (G12) 853033-PHB1"  0 noShadow
    String currZoner = null;
    String[] currLooper = new String[2];
    String startLink = null;
    String endLink = null;
    HashSet<String> seen = new HashSet<String>();
    int cycle = 0;
    for (String link : links) {
      if ((link.indexOf("shdw") != -1) && !shadow) {
        continue;
      }
      String[] linodes = getNodes(link);
      boolean linkIsShad = isShadow(link);
      String zoner = getZoneNode(linodes, nodeOrder, linkIsShad);
      if ((currZoner == null) || !currZoner.equals(zoner)) { // New Zone
        if (currZoner != null) { // End the zone
          System.out.println("CZ " + currZoner +  " zo " + zoner);
          if (currZoner.equals(currLooper[1])) {
            String color = (cycle % 2 == 0) ? "PowderBlue" : "Pink";
            out.println("cycle " + cycle++  + "\t\"" + startLink +  "\"\t\"" + endLink + "\"\t0\t" + shadTag + "\t" + color);
            System.out.println("slel " + startLink +  " ::: " + endLink);
          }
        }
        currZoner = zoner;
        for (String[] bound : bounds) {
          if (!seen.contains(bound[0]) && bound[0].equals(currZoner)) {
            startLink = link;
            seen.add(bound[0]);
            currLooper = bound;
            System.out.println("CL " + currLooper[0] +  " 1: " + currLooper[1]);
          }
        }
      }
      endLink = link;
    }
    String color = (cycle % 2 == 0) ? "PowderBlue" : "Pink";
    out.println("cycle " + cycle++  + "\t\"" + startLink +  "\"\t\"" + endLink + "\"\t0\t" + shadTag + "\t" + color);
    return;
  }
  
  /***************************************
  **
  ** A zone node is the bottom node of a shadow link or the top node of
  ** a regular link
  */
    
  private NID.WithName getZoneNode(NID.WithName[] linodes, Map<NID.WithName, Integer> nodes, boolean isShadow) {
    int zeroIndex = nodes.get(linodes[0]).intValue();
    int oneIndex = nodes.get(linodes[1]).intValue();
    if (isShadow) {
      NID.WithName botnode = (zeroIndex < oneIndex) ? linodes[1] : linodes[0];
      return (botnode);
    } else {
      NID.WithName topnode = (zeroIndex < oneIndex) ? linodes[0] : linodes[1];
      return (topnode); 
    }
  }

  /***************************************
  **
  ** Get the color
  */
     
  public String getColor(String type) {
    String trimmed = type.trim();
    if (trimmed.equals("G12")) {
      return ("Purple");
    } else if (trimmed.equals("G1A")) {
      return ("PowderBlue");
    } else if (trimmed.equals("G2A")) {
      return ("Pink");
    }
    throw new IllegalArgumentException();
  } 
}
