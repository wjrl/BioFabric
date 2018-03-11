/*
**    File created by Rishi Desai
**
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.NID;

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
  
  public String readAlignment(File infile, Map<NID.WithName, NID.WithName> mapG1ToG2, NetAlignFileStats stats,
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
     
      boolean existsInG1 = G1nameToNID.containsKey(strNameG1),
              existsInG2 = G2nameToNID.containsKey(strNameG2);
      
      String msg = "";
      if (!existsInG1) {
        msg += "Alignment file's node \"" + strNameG1 + "\" not found in smaller graph";
      }
      if (!existsInG2) {
        if (!msg.isEmpty()) {
          msg += "\n";
        }
        msg += "Alignment file's node \"" + strNameG2 + "\" not found in larger graph";
      }
      if (!msg.isEmpty()) {
        throw new IOException("Load Error: " + msg);
      }
      
      NID.WithName nodeG1 = G1nameToNID.get(strNameG1), nodeG2 = G2nameToNID.get(strNameG2);
      
      if (mapG1ToG2.containsKey(nodeG1)) {
        
        if (! mapG1ToG2.get(nodeG1).equals(nodeG2)) {
          throw new IOException("Node mapping must be one-to-one: \"" + strNameG1 + "\" is not");
        } else {
          stats.dupLines.add(line);
        }
      } else {
        mapG1ToG2.put(nodeG1, nodeG2);
      }
    }
  
    if (mapG1ToG2.size() != G1nameToNID.size()) {
      String msg = "size of alignment map: " + mapG1ToG2.size() +
              "; size of smaller graph: " + G1nameToNID.size() + "; sizes not equal";
      throw (new IOException("Incomplete node mapping: " + msg));
    }
  
    in.close();
    return (null);
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
  
  public static class NetAlignFileStats {
    public ArrayList<String> dupLines;
    public ArrayList<String> badLines;
    
    public NetAlignFileStats() {
      badLines = new ArrayList<String>();
      dupLines = new ArrayList<String>();
    }
  }
  
}
