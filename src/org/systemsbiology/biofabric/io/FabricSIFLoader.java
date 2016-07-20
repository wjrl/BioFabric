/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biofabric.model.FabricLink;

/****************************************************************************
**
** This loads SIF files
*/

public class FabricSIFLoader {
  
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

  public SIFStats readSIF(File infile, List<FabricLink> links, Set<String> loneNodes, Map<String, String> nameMap, Integer magBins) throws IOException { 
    SIFStats retval = new SIFStats();
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
    ArrayList<String[]> tokSets = new ArrayList<String[]>();
    String line = null;
    while ((line = in.readLine()) != null) {
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
    in.close();
    

    
    int numLines = tokSets.size();  
    for (int i = 0; i < numLines; i++) {
      String[] tokens = tokSets.get(i);
      if (tokens.length == 3) {
        String source = tokens[0].trim();
        if ((source.indexOf("\"") == 0) && (source.lastIndexOf("\"") == (source.length() - 1))) {
          source = source.replaceAll("\"", "");
        }
        String target = tokens[2].trim();
        if ((target.indexOf("\"") == 0) && (target.lastIndexOf("\"") == (target.length() - 1))) {
          target = target.replaceAll("\"", "");
        }
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
        String rel = tokens[1].trim();
        if ((rel.indexOf("\"") == 0) && (rel.lastIndexOf("\"") == (rel.length() - 1))) {
          rel = rel.replaceAll("\"", "");
        }

	      FabricLink nextLink = new FabricLink(source, target, rel, false);
	      links.add(nextLink);
	      
	      // We never create shadow feedback links!
	      if (!source.equals(target)) {
	        FabricLink nextShadowLink = new FabricLink(source, target, rel, true);
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
        loneNodes.add(loner);       
      }
    }
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
