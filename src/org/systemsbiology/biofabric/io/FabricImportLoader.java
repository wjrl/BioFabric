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
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This loads SIF files
*/

<<<<<<< HEAD:src/org/systemsbiology/biofabric/io/FabricSIFLoader.java
public class FabricSIFLoader { // MAKE THIS GENERALIZED
=======
public abstract class FabricImportLoader {
>>>>>>> wjrl/Sprint5:src/org/systemsbiology/biofabric/io/FabricImportLoader.java
  
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

  public FabricImportLoader() {
 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  ** 
  ** Parse a line to tokens
  */

  protected abstract String[] lineToToks(String line, SIFStats stats) throws IOException; 
  
  /***************************************************************************
  ** 
  ** Consume tokens, make links
  */

  protected abstract void consumeTokens(String[] tokens, UniqueLabeller idGen, List<FabricLink> links, 
  		                                  Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap, Integer magBins, 
  		                                  HashMap<String, NID.WithName> nameToID, SIFStats stats) throws IOException; 
    
  /***************************************************************************
  ** 
  ** Process a SIF input
  */

  public SIFStats importFabric(File infile, UniqueLabeller idGen, List<FabricLink> links, 
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
	    	String[] tokens = lineToToks(line, retval);
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
    
    int numLines = tokSets.size();
    lr = new LoopReporter(numLines, 20, monitor, 0.0, 1.0, "progress.buildingEdgesAndNodes");
    
    for (int i = 0; i < numLines; i++) {
      String[] tokens = tokSets.get(i);
      lr.report();
      consumeTokens(tokens, idGen, links, loneNodeIDs, nameMap, magBins, nameToID, retval);
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
