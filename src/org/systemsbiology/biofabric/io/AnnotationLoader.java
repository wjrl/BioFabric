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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.model.AnnotationSet;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;

/****************************************************************************
**
** This loads Annotation files
*/

public class AnnotationLoader {
  
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

  public AnnotationLoader() { 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Process an Annotation file
  */
  
  public AnnotationSet readAnnotations(File infile, ReadStats stats, BioFabricNetwork bfn, BTProgressMonitor monitor) throws AsynchExitRequestException, IOException {
    
    long fileLen = infile.length();    
    Map<String, Set<NID.WithName>> nameToIDs = bfn.getNormNameToIDs();
    BufferedReader in = null;
    ArrayList<String[]> tokSets = new ArrayList<String[]>();
    LoopReporter lr = new LoopReporter(fileLen, 20, monitor, 0.0, 1.0, "progress.readingFile");
    try {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
      String line = null;
      while ((line = in.readLine()) != null) {
        lr.report(line.length() + 1);
        if (line.trim().equals("")) {
          continue;
        }
        String[] tokens = lineToToks(line, stats);
        if (tokens != null) {
          tokSets.add(tokens);
        }
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }
    lr.finish();
    
    //
    // Need to build a map from node name to row so we can build the Annotations. Invert
    // what we have:
    //
    
    HashMap<NID.WithName, Integer> idToRow = new HashMap<NID.WithName, Integer>();
    int numNodes = bfn.getRowCount();
    for (int i = 0; i < numNodes; i++) {
      NID.WithName nid = bfn.getNodeIDForRow(Integer.valueOf(i));
      idToRow.put(nid, Integer.valueOf(i));
    }

    int numLines = tokSets.size();
    lr = new LoopReporter(numLines, 20, monitor, 0.0, 1.0, "progress.buildingAnnotations");
    
    AnnotationSet aSet = new AnnotationSet();
    for (int i = 0; i < numLines; i++) {
      String[] tokens = tokSets.get(i);
      lr.report();
      consumeTokens(tokens, idToRow, nameToIDs, aSet, stats);
    }
    lr.finish();
    return (aSet);
  }

  /***************************************************************************
  ** 
  ** Parse a line to tokens
  */

  protected String[] lineToToks(String line, ReadStats stats) throws IOException {
  	if (line.trim().equals("")) {
  		return (null);
  	}
    String[] tokens = line.split("\\t");
    if ((tokens.length == 1) && (line.indexOf("\\t") == -1)) {
      tokens = line.split(" ");
    }
    
    if (tokens.length == 0) {
      return (null);
    } else if (tokens.length != 4) {
      stats.badLine = line;
      stats.errStr = "annotLoad.tooManyTokens";
      throw new IOException();
    } else {        
      return (tokens);
    }
  }
  
  /***************************************************************************
  **
  ** Strip quoted string
  */
  
  protected String stripQuotes(String inString) {
    String procString = inString.trim();
    if ((procString.indexOf("\"") == 0) && (procString.lastIndexOf("\"") == (procString.length() - 1))) {
      procString = procString.replaceAll("\"", "");
    }
    return (procString);
  }
  
  /***************************************************************************
  ** 
  ** Consume tokens, make Annotations
  */

  protected void consumeTokens(String[] tokens, Map<NID.WithName, Integer> idToRow, 
                               Map<String, Set<NID.WithName>> nameToID, AnnotationSet aSet,
                               ReadStats stats) throws IOException {
    
    
    int layer;
    try {
      layer = Integer.valueOf(tokens[3]).intValue();
    } catch (NumberFormatException nfex) {
      stats.badTok = tokens[3];
      stats.errStr = "annotLoad.badLayer";
      throw new IOException();
    }
    
    if (layer < 0) {
      stats.badTok = tokens[3];
      stats.errStr = "annotLoad.negLayer";
      throw new IOException();
    }
    
    String name = tokens[0].trim();
    
    String startNode = tokens[1].trim();
    startNode = stripQuotes(startNode);
    
    String endNode = tokens[2].trim();
    endNode = stripQuotes(endNode);
    
    int min = nameToRow(startNode, idToRow, nameToID, stats);  
    int max = nameToRow(endNode, idToRow, nameToID, stats);
   
    AnnotationSet.Annot annot = new AnnotationSet.Annot(name, min, max, layer);
    aSet.addAnnot(annot);
    return;
  }

  /***************************************************************************
  ** 
  ** Node ID to row
  */

  private int nameToRow(String nodeID, Map<NID.WithName, Integer> idToRow, 
                        Map<String, Set<NID.WithName>> nameToID, 
                        ReadStats stats) throws IOException {  
  
    Set<NID.WithName> forID = nameToID.get(DataUtil.normKey(nodeID));
    if (forID == null) {
      stats.badTok = nodeID;
      stats.errStr = "annotLoad.nodeNotFound";
      throw new IOException();   
    } else if (forID.size() != 1) {
      stats.badTok = nodeID;
      stats.errStr = "annotLoad.multiNodesForName";
      throw new IOException();   
    }
    NID.WithName nidwn = forID.iterator().next();
    Integer rowNum = idToRow.get(nidwn);
    if (rowNum == null) {
      throw new IllegalStateException();
    }
    
    return (rowNum.intValue());
  } 
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Problems with imports
  */  
 
  public static class ReadStats {
    public String badLine;
    public String badTok;
    public String errStr;
    
    public ReadStats() {
    }
  }  
}
