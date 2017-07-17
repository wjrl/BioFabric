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

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/****************************************************************************
 **
 ** This loads alignment (.align) files
 */

public class AlignmentLoader {
  
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
  
  public AlignmentLoader() {
  
  }
  
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
    
    Map<String, NID.WithName> G1nameToNID = makeStringMap(extractNodes(linksGraph1, loneNodesGraph1));
    Map<String, NID.WithName> G2nameToNID = makeStringMap(extractNodes(linksGraph2, loneNodesGraph2));
    
    
//    for (Map.Entry<String, NID.WithName> entry : G1nameToNID.entrySet()) {
//      System.out.println(entry.getKey());
//    }
  
//    System.out.println("\n\n\n\n");
//
//    for (Map.Entry<String, NID.WithName> entry : G2nameToNID.entrySet()) {
//      System.out.println(entry.getKey());
//    }
    
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
    
    String line = null;
    while ((line = in.readLine()) != null) {
  
      StringTokenizer st = new StringTokenizer(line);
      if (st.countTokens() != 2) {
        
//        System.out.println("WE HAVE GOT A BAD LINE \n\n\n\n\n");
        stats.badLines.add(line);
        continue;
      }
  
      //
      // Checking if nodes are in graphs
      //
  
//      System.out.println(G2nameToNID.size() + "  " + G2nameToNID.get("836329"));
      
      String strNameG1 = st.nextToken(), strNameG2 = st.nextToken();
//      System.out.println(G1nameToNID.containsKey(strNameG1) +" " + G2nameToNID.containsKey(strNameG2));
     
      if (!(G1nameToNID.containsKey(strNameG1) && G2nameToNID.containsKey(strNameG2))) {
//        System.out.println(strNameG1);
//        System.out.println(strNameG2);
//
//        System.out.println("CONTAINS KEY ERROR ALIGN LOADER \n\n\n\n\n");
//
        throw new IOException();
      }
//      System.out.println("MADE IT");
      NID.WithName nodeG1 = G1nameToNID.get(strNameG1), nodeG2 = G2nameToNID.get(strNameG2);
      
      
      // EVERY NODE IN G1 MUST BE MAPPED
      
      if (mapG1ToG2.containsKey(nodeG1)) {
        
        if (! mapG1ToG2.get(nodeG1).equals(nodeG2)) { // mapping has to be one-to-one

//          System.out.println("CONTAINS NOT EQUAL NODE NAME ERROR ALIGN LOADER \n\n\n\n\n");
//
          throw new IOException();
        } else {
          stats.dupLines.add(line);
        }
      } else {
        mapG1ToG2.put(nodeG1, nodeG2);
      }
    }
    
    stats.mappedAllNodes = (mapG1ToG2.size() == G1nameToNID.size());    // JUST FOR NOW
  
    in.close();
    return null;
  }
  
  /***************************************************************************
   **
   **
   */
  
  private static Map<String,NID.WithName> makeStringMap(Set<NID.WithName> nodes) {
  
    Map<String,NID.WithName> retval = new HashMap<String, NID.WithName>();
    
    for (NID.WithName node : nodes) {
      retval.put(node.getName(), node);
    }
    return retval;
  }
  
  /***************************************************************************
   **
   **
   */
  
  private static Set<NID.WithName> extractNodes(ArrayList<FabricLink> links, HashSet<NID.WithName> loneNodeIDs) {
    
    Set<NID.WithName> retval;
    
    if (loneNodeIDs != null) {
      retval = new HashSet<NID.WithName>(loneNodeIDs);
    } else {
      retval = new HashSet<NID.WithName>();
    }
  
    for (FabricLink link : links) {
      NID.WithName A = link.getSrcID(), B = link.getTrgID(); // WILL A = B? - so do i need to check for this here??
      retval.add(A);
      retval.add(B);
    }
    return retval;
  }
  
  
  public static class NetAlignStats {
    public ArrayList<String> dupLines;
    public ArrayList<String> badLines;
    public boolean mappedAllNodes;
    
    public NetAlignStats() {
      badLines = new ArrayList<String>();
      dupLines = new ArrayList<String>();
    }
  }
  
}

///*
//**    Copyright (C) 2003-2014 Institute for Systems Biology
//**                            Seattle, Washington, USA.
//**
//**    This library is free software; you can redistribute it and/or
//**    modify it under the terms of the GNU Lesser General Public
//**    License as published by the Free Software Foundation; either
//**    version 2.1 of the License, or (at your option) any later version.
//**
//**    This library is distributed in the hope that it will be useful,
//**    but WITHOUT ANY WARRANTY; without even the implied warranty of
//**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//**    Lesser General Public License for more details.
//**
//**    You should have received a copy of the GNU Lesser General Public
//**    License along with this library; if not, write to the Free Software
//**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//*/
//
//package org.systemsbiology.biofabric.io;
//
//        import org.systemsbiology.biofabric.model.FabricLink;
//        import org.systemsbiology.biofabric.util.NID;
//
//        import java.io.*;
//        import java.util.*;
//
///****************************************************************************
// **
// ** This loads alignment (.align) files
// */
//
//public class AlignmentLoader {
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PRIVATE CONSTANTS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PUBLIC CONSTANTS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PRIVATE INSTANCE MEMBERS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PUBLIC CONSTRUCTORS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//  /***************************************************************************
//   **
//   ** Constructor
//   */
//
//  public AlignmentLoader() {
//
//  }
//
//  ////////////////////////////////////////////////////////////////////////////
//  //
//  // PUBLIC METHODS
//  //
//  ////////////////////////////////////////////////////////////////////////////
//
//
//  /***************************************************************************
//   **
//   ** Process an alignment (.align) file
//   */
//
//  public String readAlignment(File infile, Map<String, String> mapG1ToG2, AlignmentLoader.NetAlignStats stats,
//                              ArrayList<FabricLink> linksGraph1, HashSet<NID.WithName> loneNodesGraph1,
//                              ArrayList<FabricLink> linksGraph2, HashSet<NID.WithName> loneNodesGraph2)
//          throws IOException {
//
//    Map<String, NID.WithName> G1nameToNID = makeStringMap(extractNodes(linksGraph1, loneNodesGraph1));
//    Map<String, NID.WithName> G2nameToNID = makeStringMap(extractNodes(linksGraph2, loneNodesGraph2));
//
//    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
//
//    String line = null;
//    while ((line = in.readLine()) != null) {
//
//      StringTokenizer st = new StringTokenizer(line);
//      if (st.countTokens() != 2) {
//        stats.badLines.add(line);
//        continue;
//      }
//
//      // FOR NOW I'M JUST GOING TO ASSUME THAT ALL THE TOKENS ARE 100% CORRECT
//
//      String nameG1 = st.nextToken(), nameG2 = st.nextToken();
//
//      //
//      // Checking if nodes are in graphs
//      //
//
//      if (!(G1nameToNID.containsKey(nameG1) && G2nameToNID.containsKey(nameG2))) {
//        throw new IOException();
//      }
//
//
//
//      // EVERY NODE IN G1 MUST BE MAPPED
//
//      if (mapG1ToG2.containsKey(nameG1)) {
//
//        if (! mapG1ToG2.get(nameG1).equals(nameG2)) { // mapping has to be one-to-one
//          throw new IOException();
//        } else {
//          stats.dupLines.add(line);
//        }
//      } else {
//        mapG1ToG2.put(nameG1, nameG2);
//      }
//    }
//
////    stats.mappedAllNodes = (mapG1ToG2.size() != 10);
//    stats.mappedAllNodes = true; // JUST FOR NOW
//
//    in.close();
//    return null;
//  }
//
//  /***************************************************************************
//   **
//   **
//   */
//
//  private static Map<String,NID.WithName> makeStringMap(Set<NID.WithName> nodes) {
//
//    Map<String,NID.WithName> retval = new HashMap<String, NID.WithName>();
//
//    for (NID.WithName node : nodes) {
//      retval.put(node.getName(), node);
//    }
//    return retval;
//  }
//
//  /***************************************************************************
//   **
//   **
//   */
//
//  private static Set<NID.WithName> extractNodes(ArrayList<FabricLink> links, HashSet<NID.WithName> loneNodeIDs) {
//
//    Set<NID.WithName> retval;
//
//    if (loneNodeIDs != null) {
//      retval = new HashSet<NID.WithName>(loneNodeIDs);
//    } else {
//      retval = new HashSet<NID.WithName>();
//    }
//
//    for (FabricLink link : links) {
//      NID.WithName A = link.getSrcID(), B = link.getTrgID(); // WILL A = B? - so do i need to check for this here??
//      retval.add(A);
//      retval.add(B);
//    }
//    return retval;
//  }
//
//
//  public static class NetAlignStats {
//    public ArrayList<String> dupLines;
//    public ArrayList<String> badLines;
//    public boolean mappedAllNodes;
//
//    public NetAlignStats() {
//      badLines = new ArrayList<String>();
//      dupLines = new ArrayList<String>();
//    }
//  }
//
//}

