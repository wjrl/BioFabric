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

package org.systemsbiology.biofabric.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/****************************************************************************
 **
 ** This loads alignment (.align) files
 */

public class AlignmentLoader {
  
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
  
  public AlignmentLoader() {}
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Process an alignment (.align) file
   */
  
  public String readAlignment(File infile, Map<NID.WithName, NID.WithName> mapG1ToG2, AlignmentLoader.NetAlignStats stats,
                              ArrayList<FabricLink> linksGraph1, HashSet<NID.WithName> loneNodesGraph1,
                              ArrayList<FabricLink> linksGraph2, HashSet<NID.WithName> loneNodesGraph2)
          throws IOException {
  
  
    Map<String, NID.WithName> G1nameToNID, G2nameToNID;
    try {
      G1nameToNID = makeStringMap(BioFabricNetwork.extractNodes(linksGraph1, loneNodesGraph1, null));
      G2nameToNID = makeStringMap(BioFabricNetwork.extractNodes(linksGraph2, loneNodesGraph2, null));
    } catch (AsynchExitRequestException aere) {
      throw new IllegalStateException();
    }
  
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
    
    String line = null;
    while ((line = in.readLine()) != null) {
  
      StringTokenizer st = new StringTokenizer(line);
      if (st.countTokens() != 2) {
        stats.badLines.add(line);
        continue;
      }
  
      //
      // Checking if nodes are in graphs
      //
  
      String strNameG1 = st.nextToken(), strNameG2 = st.nextToken();
  
      strNameG1 = strNameG1.replaceAll("-","_");
      strNameG2 = strNameG2.replaceAll("-","_");
      UiUtil.fixMePrintout("Auto-replacing dashes with underscores in .align files");
  
//      Set<String> notFiltered = new HashSet<String>();
//      String[] nodesWithoutEntrezIDs = {
//              "ATM1",
//              "CTM1",
//              "IMD1",
//              "PHM8",
//              "PUT1",
//              "SBE2",
//              "YAR075W",
//              "YDL026W",
//              "YDR133C",
//              "YGR272C",
//              "YNL276C",};
//      Collections.addAll(notFiltered, nodesWithoutEntrezIDs);
//      if (notFiltered.contains(strNameG1) || notFiltered.contains(strNameG2)) {
//        continue;
//      }
     
      boolean existsInG1 = G1nameToNID.containsKey(strNameG1),
              existsInG2 = G2nameToNID.containsKey(strNameG2);
      
      if (!(existsInG1 && existsInG2)) {
        System.out.println(G1nameToNID.size()+ "Load error: " + strNameG1 + " " + existsInG1 + "  " + strNameG2 + " "+ existsInG2);
        throw new IOException("Incorrect node names or nodes do not exist in graph files");
      }
      
      NID.WithName nodeG1 = G1nameToNID.get(strNameG1), nodeG2 = G2nameToNID.get(strNameG2);
      
      if (mapG1ToG2.containsKey(nodeG1)) {
        
        if (! mapG1ToG2.get(nodeG1).equals(nodeG2)) {
          throw new IOException("Node mapping must be one-to-one");
        } else {
          stats.dupLines.add(line);
        }
      } else {
        mapG1ToG2.put(nodeG1, nodeG2);
      }
    }
  
    if (mapG1ToG2.size() != G1nameToNID.size()) {
      System.out.println(mapG1ToG2.size() + "  " + G1nameToNID.size());
      throw new IOException("Incomplete node mapping");
    }
  
    in.close();
    return null;
  }
  
  /***************************************************************************
   **
   ** Map node name to NID.WithName using node set
   */
  
  private static Map<String,NID.WithName> makeStringMap(Set<NID.WithName> nodes) {
  
    Map<String,NID.WithName> retval = new HashMap<String, NID.WithName>();
    
    for (NID.WithName node : nodes) {
      retval.put(node.getName(), node);
    }
    return retval;
  }
  
  public static class NetAlignStats {
    public ArrayList<String> dupLines;
    public ArrayList<String> badLines;
    
    public NetAlignStats() {
      badLines = new ArrayList<String>();
      dupLines = new ArrayList<String>();
    }
  }
  
}
