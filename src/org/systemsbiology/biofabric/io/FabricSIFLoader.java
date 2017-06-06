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

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This loads SIF files
*/

public class FabricSIFLoader { // MAKE THIS GENERALIZED
  
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

  public FabricSIFLoader() {
 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

    
  /***************************************************************************
  ** 
  ** Process a SIF input
  */

  public SIFStats readSIF(File infile, UniqueLabeller idGen, List<FabricLink> links, 
  		                    Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap, Integer magBins, 
  		                    BTProgressMonitor monitor) throws AsynchExitRequestException, IOException { 

  	SIFStats retval = new SIFStats();
    long fileLen = infile.length();
    HashMap<String, NID.WithName> nameToID = new HashMap<String, NID.WithName>();
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
	      String[] tokens = line.split("\\t");
	      if ((tokens.length == 1) && (line.indexOf("\\t") == -1)) {
	        tokens = line.split(" ");
	      }
	      if (tokens.length == 0) {
	        continue;
	      } else if ((tokens.length == 2) || (tokens.length > 3)) {
	        retval.badLines.add(line);
	        continue;
	      } else {        
	        tokSets.add(tokens);
	      }
	    }
    } finally {
    	if (in != null) {
        in.close();
    	}
    }
    lr.finish();
    
    int numLines = tokSets.size();
    lr = new LoopReporter(numLines, 20, monitor, 0.0, 1.0, "progress.buildingEdgesAndNodes");
    
    for (int i = 0; i < numLines; i++) {
      String[] tokens = tokSets.get(i);
      lr.report();
      if (tokens.length == 3) {
        String source = tokens[0].trim();
        if ((source.indexOf("\"") == 0) && (source.lastIndexOf("\"") == (source.length() - 1))) {
          source = source.replaceAll("\"", "");
        }
        String target = tokens[2].trim();
        if ((target.indexOf("\"") == 0) && (target.lastIndexOf("\"") == (target.length() - 1))) {
          target = target.replaceAll("\"", "");
        }
        //
        // This name map is for the load SIF with node attributes feature:
        //
        
        if (nameMap != null) {
          String mappedSource = nameMap.get(source);
          if (mappedSource != null) {
            source = mappedSource;
          }
          String mappedTarget = nameMap.get(target);
          if (mappedTarget != null) {
            target = mappedTarget;
          }
        }
        
        //
        // Map the name to an ID, if none yet, get a new ID and assign it
        //
        
        String normSrc = DataUtil.normKey(source);
        String normTrg = DataUtil.normKey(target);
        
        NID.WithName srcID = nameToID.get(normSrc);
        if (srcID == null) {
        	NID srcNID = idGen.getNextOID();
        	srcID = new NID.WithName(srcNID, source);
        	nameToID.put(normSrc, srcID);
        }
        
        NID.WithName trgID = nameToID.get(normTrg);
        if (trgID == null) {
        	NID trgNID = idGen.getNextOID();
        	trgID = new NID.WithName(trgNID, target);
        	nameToID.put(normTrg, trgID);
        }

        String rel = tokens[1].trim();
        if ((rel.indexOf("\"") == 0) && (rel.lastIndexOf("\"") == (rel.length() - 1))) {
          rel = rel.replaceAll("\"", "");
        }

	      FabricLink nextLink = new FabricLink(srcID, trgID, rel, false);
	      links.add(nextLink);
	      
	      // We never create shadow feedback links!
	      if (!srcID.equals(trgID)) {
	        FabricLink nextShadowLink = new FabricLink(srcID, trgID, rel, true);
	        links.add(nextShadowLink);
	      }
 
      } else {
        String loner = tokens[0].trim();
        if ((loner.indexOf("\"") == 0) && (loner.lastIndexOf("\"") == (loner.length() - 1))) {
          loner = loner.replaceAll("\"", "");
        }
        if (nameMap != null) {
          String mappedLoner = nameMap.get(loner);
          if (mappedLoner != null) {
            loner = mappedLoner;
          }
        }
        
        String normLoner = DataUtil.normKey(loner);
        
        NID.WithName loneID = nameToID.get(normLoner);
        if (loneID == null) {
        	NID loneNID = idGen.getNextOID();
        	loneID = new NID.WithName(loneNID, loner);
        	nameToID.put(normLoner, loneID);
        }
        loneNodeIDs.add(loneID); 
        System.err.println("new lone " + loneID);
      }
    }
    lr.finish();
    return (retval);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASES
  //
  ////////////////////////////////////////////////////////////////////////////

  public static class SIFStats {
    public ArrayList<String> badLines;
    
    public SIFStats() {
      badLines = new ArrayList<String>();
    }
    
    public void copyInto(SIFStats other) {
      this.badLines = new ArrayList<String>(other.badLines);
      return;
    }
  }   
}
