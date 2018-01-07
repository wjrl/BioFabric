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

package org.systemsbiology.biofabric.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/****************************************************************************
 **
 ** This loads GW (LEDA file format) files
 */

public class GWImportLoader extends FabricImportLoader {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private final String DEFAULT_RELATION = "default";
  private final int HEADER_LINES = 4; // # of header/parameter/version-info lines at the beginning of file
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private int lineToTokIndex_, consTokIndex_;
  private Integer numNodes_, numEdges_;
  private Map<Integer, String> indexToName_;
  private Map<Integer, Boolean> indexUsed_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Constructor
   */
  
  public GWImportLoader() {
    this.lineToTokIndex_ = 0; // counter while reading tokens
    this.consTokIndex_ = 0;   // counter while consuming tokens
    this.indexToName_ = new HashMap<Integer, String>();
    this.indexUsed_ = new HashMap<Integer, Boolean>();
  }
  
  /***************************************************************************
   **
   ** Parse a line to tokens
   */
  
  protected String[] lineToToks(String line, FileImportStats stats) throws IOException {
    if (line.trim().equals("")) {
      return (null);
    }
    
    //
    // length == 1: Node Name or parameters(lines 0-3); Length == 4: Edge
    //
  
    String[] tokens = line.split("\\t");
    if ((tokens.length == 1) && (line.indexOf("\\t") == -1)) {
      tokens = line.split(" ");
    }
    
    if (tokens.length == 0 || tokens.length == 2 || tokens.length == 3 || tokens.length > 4) {
      stats.badLines.add(line);
      return (null);
    } else {
      
      if (lineToTokIndex_ == HEADER_LINES) {
        try {
          numNodes_ = Integer.parseInt(tokens[0].trim());
        } catch (NumberFormatException ex) {
          throw new IOException("We assume 4 header lines");
        }
      }
      if (numNodes_ != null && lineToTokIndex_ == HEADER_LINES + numNodes_ + 1) {
        try {
          numEdges_ = Integer.parseInt(tokens[0].trim());
        } catch (NumberFormatException ex) {
          throw new IOException("We assume 4 header lines");
        }
      }
      
      lineToTokIndex_++;
      
      return (tokens);
    }
  }
  
  /***************************************************************************
   **
   ** Consume tokens, process nodes, make links
   */
  
  protected void consumeTokens(String[] tokens, UniqueLabeller idGen, List<FabricLink> links,
                               Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap,
                               Integer magBins, HashMap<String, NID.WithName> nameToID,
                               FileImportStats stats) throws IOException {
    
    if (tokens.length == 1) {
  
      if (consTokIndex_ > HEADER_LINES && consTokIndex_ != HEADER_LINES + numNodes_ + 1) {
        
        int index = consTokIndex_ - HEADER_LINES; // nodes index starts with one
        
        String nodeName = tokens[0].trim();
        nodeName = stripBrackets(nodeName);
        
        indexToName_.put(index, nodeName);
        indexUsed_.put(index, false);
      }
      
    } else if (tokens.length == 4) {
      
      String sourceIndexStr = tokens[0].trim();
      String targetIndexStr = tokens[1].trim();
      String edgeRelation = tokens[3].trim();
      
      int sourceIndex, targetIndex;
      try {
        sourceIndex = Integer.parseInt(sourceIndexStr);
        targetIndex = Integer.parseInt(targetIndexStr);
      } catch (NumberFormatException ex) {
        throw new IOException("Could not parse integer");
      }
      
      indexUsed_.put(sourceIndex, true);
      indexUsed_.put(targetIndex, true);
      
      String sourceName = indexToName_.get(sourceIndex);
      String targetName = indexToName_.get(targetIndex);
      
      sourceName = stripBrackets(sourceName);
      targetName = stripBrackets(targetName);
      
      sourceName = sourceName.replaceAll("-","_");
      targetName = targetName.replaceAll("-","_");
      sourceName = sourceName.replaceAll(",","");
      targetName = targetName.replaceAll(",","");
      UiUtil.fixMePrintout("Auto-replacing dashes with underscores in .gw files");
  
      NID.WithName srcID = nameToNode(sourceName, idGen, nameToID);
      NID.WithName trgID = nameToNode(targetName, idGen, nameToID);
  
      edgeRelation = stripBrackets(edgeRelation);
      
      if (edgeRelation.equals("")) {
        edgeRelation = DEFAULT_RELATION;
      }
  
      //
      // Build the link, plus shadow if not auto feedback:
      //
  
      buildLinkAndShadow(srcID, trgID, edgeRelation, links);
  
    } else {
      throw new IllegalArgumentException();
    }
    
    consTokIndex_++;
    
    if (consTokIndex_ == HEADER_LINES + numNodes_ + 1 + numEdges_ + 1) { // reached end of file
      addLoneNodes(idGen, loneNodeIDs, nameToID);
    }
    // We don't check if there may be more lines or edges after the specified number of edges
    return;
  }
  
  private void addLoneNodes(UniqueLabeller idGen, Set<NID.WithName> loneNodeIDs,
                            HashMap<String, NID.WithName> nameToID) {
    
    for (Map.Entry<Integer, Boolean> entry : indexUsed_.entrySet()) {
      
      if (! entry.getValue()) {
        String loner = indexToName_.get(entry.getKey());
        NID.WithName lonerID = nameToNode(loner, idGen, nameToID);
        loneNodeIDs.add(lonerID);
      }
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Test if file is .gw file
   */
  
  public static boolean isGWFile(File file) {
    UiUtil.fixMePrintout("Sketchy way to check if file is .gw");
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
  
      String line = in.readLine();
      if (line.equals("LEDA.GRAPH")) {
        return (true);
      }
      for (int i = 1;i <= 3; i++) { // skip next three lines; We assume four header lines
        in.readLine();
      }
      String[] tok = in.readLine().split(" ");
      if (tok.length != 1) {  // should be #nodes here
        return (false);
      }
      
      int numNodes = Integer.parseInt(tok[0]); // to test if #nodes is in fact integer
      if (numNodes > 0) {
        String aNode = in.readLine();
        int len = aNode.length();
        if (aNode.substring(0,2).equals("|{") && aNode.substring(len - 2, len).equals("}|")) {
          return (true);
        }
      } else {
        return (false);
      }
  
    } catch (IOException ioe) {
      ExceptionHandler.getHandler().displayException(ioe);
    } catch (NumberFormatException nfe) {
      return (false);
    }
    return (false);
  }
  
}
