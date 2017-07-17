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

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UniqueLabeller;

import java.io.IOException;
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
  
  private final String DEFAULT_RELATION = "pp";
  private final int PARAM_LINES = 4; // # of parameter/version info lines
  
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
  
  private int lnToTokIndex_, consTokIndex_;
  private Integer numNodes_, numEdges_;
//  private boolean forNetAlign_;
//  private String netAlignRel_;
  private Map<Integer, String> indexToName_;
  private Map<Integer, Boolean> indexToBeenUsed_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
   **
   ** Constructor
   */
  
  public GWImportLoader(/* boolean forNetAlign, String netAlignRelation */) {
//    if (forNetAlign) {
//      this.netAlignRel_ = netAlignRelation; // for network alignments, all edges have same relation
//    } else {
//      this.netAlignRel_ = null; // won't be used
//    }
//    this.forNetAlign_ = forNetAlign;
    this.lnToTokIndex_ = 0;
    this.consTokIndex_ = 0;
    this.indexToName_ = new HashMap<Integer, String>();
    this.indexToBeenUsed_ = new HashMap<Integer, Boolean>();
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
    
    String[] tokens = line.split(" ");
    if (tokens.length == 0 || tokens.length == 2 || tokens.length == 3 || tokens.length > 4) {
      stats.badLines.add(line);
      return (null);
    } else {
      
      if (lnToTokIndex_ == PARAM_LINES) {
        try {
          numNodes_ = Integer.parseInt(tokens[0].trim());
        } catch (Exception ex) {
          throw new IOException();
        }
      }
      if (numNodes_ != null && lnToTokIndex_ == PARAM_LINES + numNodes_ + 1) { // AM I DOING THIS RIGHT
        try {
          numEdges_ = Integer.parseInt(tokens[0].trim());
        } catch (Exception ex) {
          throw new IOException();
        }
      }
      
      lnToTokIndex_++;
      
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
  
      if (consTokIndex_ > PARAM_LINES && consTokIndex_ != PARAM_LINES + numNodes_ + 1) {
        
        int index = consTokIndex_ - PARAM_LINES; // nodes index starts w/ one
        
        String nodeName = tokens[0].trim();
        nodeName = stripBrackets(nodeName);
        
        indexToName_.put(index, nodeName);
      }
      
    } else if (tokens.length == 4) {
      
      String sourceIndexStr = tokens[0].trim();
      String targetIndexStr = tokens[1].trim();
      String edgeRelation = tokens[3].trim();
      
      int sourceIndex, targetIndex;
      try {
        sourceIndex = Integer.parseInt(sourceIndexStr);
        targetIndex = Integer.parseInt(targetIndexStr);
      } catch (Exception ex) {
        throw new IOException();
      }
      
      indexToBeenUsed_.put(sourceIndex, true);
      indexToBeenUsed_.put(targetIndex, true);
      
      String sourceName = indexToName_.get(sourceIndex);
      String targetName = indexToName_.get(targetIndex);
      
      sourceName = stripBrackets(sourceName);
      targetName = stripBrackets(targetName);
  
      NID.WithName srcID = nameToNode(sourceName, idGen, nameToID);
      NID.WithName trgID = nameToNode(targetName, idGen, nameToID);
  
      edgeRelation = stripBrackets(edgeRelation);
      
      if (edgeRelation.equals("")) {
        edgeRelation = DEFAULT_RELATION;
      }
  
      //
      // Build the link, plus shadow if not auto feedback:
      //
  
//      if (forNetAlign_) {
//        buildLinkAndShadow(srcID, trgID, netAlignRel_, links);   // WHAT ABOUT LONE NODES
//      } else {
      buildLinkAndShadow(srcID, trgID, edgeRelation, links);
//      }
  
    } else {
      throw new IllegalArgumentException();
    }
    
    consTokIndex_++;
    
    if (consTokIndex_ == PARAM_LINES + numNodes_ + 1 + numEdges_ + 1) {
//      System.out.println(numNodes_ + " " + numEdges_);
//      for (int i = 0; i<10;i++) {
//        System.out.println("CODE HAS REACHED END OF GW FILE");
//      }
      addLoneNodes(idGen, loneNodeIDs, nameToID);
    }
    
    return;
  }
  
  private void addLoneNodes(UniqueLabeller idGen, Set<NID.WithName> loneNodeIDs,
                            HashMap<String, NID.WithName> nameToID) {
    
    for (Map.Entry<Integer, Boolean> entry : indexToBeenUsed_.entrySet()) {
      
      if (! entry.getValue()) {
        String loner = indexToName_.get(entry.getKey());
        NID.WithName lonerID = nameToNode(loner, idGen, nameToID);
        loneNodeIDs.add(lonerID);
      }
    }
    return;
  }
  
//  /***************************************************************************
//   **
//   ** Process a GW Files
//   */
//
//  public FileImportStats processFile(File infile, UniqueLabeller idGen, List<FabricLink> links,
//                             Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap, Integer magBins,
//                             BTProgressMonitor monitor) throws AsynchExitRequestException, IOException {
//
//    FabricImportLoader.FileImportStats retval = new FabricImportLoader.FileImportStats();
//
//    long fileLen = infile.length();
//    ArrayList<String[]> tokSets = new ArrayList<String[]>();
//    LoopReporter lr = new LoopReporter(fileLen, 20, monitor, 0.0, 1.0, "progress.readingFile");
//
//    BufferedReader in = null;
//    try {
//      in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
//      // First 4 lines
//      String str1 = in.readLine(), str2 = in.readLine(), str3 = in.readLine(), str4 = in.readLine();
//
//      final int numNodes = Integer.parseInt(in.readLine());
//
//      for (int i = 1; i <= numNodes; i++) {
//        String line = in.readLine();
//        if (line == null) { // WHAT DO I DO HERE
//          break;
//        }
//        lr.report(line.length() + 1);
//        if (line.trim().equals("")) {
//          continue;
//        }
//
//        String[] toks  = lineToToks(line, retval);
//        if (toks != null) {
//          tokSets.add(toks);
//        }
//
////        ret.addNode(i, new NodeNA(line));
//      }
//
//      final int numEdges_ = Integer.parseInt(in.readLine());
//
//      for (int i = 1; i <= numEdges_; i++) {
//        String line = in.readLine();
//        if (line == null) { // WHAT DO I DO HERE
//          break;
//        }
//        lr.report(line.length() + 1);
//        if (line.trim().equals("")) {
//          continue;
//        }
//
//        String[] toks  = lineToToks(line, retval);
//        if (toks != null) {
//          tokSets.add(toks);
//        }
//      }
//    } finally {
//      if (in != null) {
//        in.close();
//      }
//    }
//    lr.finish();
//
//    int numLines = tokSets.size();
//    lr = new LoopReporter(numLines, 20, monitor, 0.0, 1.0, "progress.buildingEdgesAndNodes");
//
//    HashMap<String, NID.WithName> nameToID = new HashMap<String, NID.WithName>();
//
//    for (int i = 0; i < numLines; i++) {
//      String[] tokens = tokSets.get(i);
//      lr.report();
//      consumeTokens(tokens, idGen, links, loneNodeIDs, nameMap, magBins, nameToID, retval);
//    }
//    lr.finish();
//
//    return (retval);
//  }
  
  
//  public static class GWStats extends FileImportStats {
//    public ArrayList<String> badLines;
    
//    public GWStats() {
//      badLines = new ArrayList<String>();
//    }
    
//    public void copyInto(GWStats other) {
//      this.badLines = new ArrayList<String>(other.badLines);
//      return;
//    }
//  }
  
}

//class ALIGNImportLoader extends FabricImportLoader {
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
//  public ALIGNImportLoader() {}
//
//  @Override
//  protected String[] lineToToks(String line, SIFStats stats) throws IOException {
//    return new String[0];
//  }
//
//  @Override
//  protected void consumeTokens(String[] tokens, UniqueLabeller idGen, List<FabricLink> links,
//                               Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap,
//                               Integer magBins, HashMap<String, NID.WithName> nameToID,
//                               SIFStats stats) throws IOException {
//
//  }
//
//}
