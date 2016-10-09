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

package org.systemsbiology.biofabric.layouts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/****************************************************************************
**
** Do Element Ration Import
*/

public class ElementRatios {
  
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

  public ElementRatios() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Do reading 
  */

  public List<String[]> readERT(File infile) throws IOException {
    //
    // Read in the lines.
    //
    ArrayList<String[]> retval = new ArrayList<String[]>();
    ArrayList<String[]> rows = new ArrayList<String[]>();
    ArrayList<String> colNames = new ArrayList<String>();
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "ISO-8859-1"));
      String line = null;
      int count = 0;
      while ((line = in.readLine()) != null) {
        if (line.trim().equals("")) {
          continue;
        }
        if (count == 0) {
          String[] toks = line.split(" ");
          colNames.addAll(Arrays.asList(toks));
          count++;
        } else {
          String[] toks = line.split(" ");
          rows.add(toks);
        }
      }
    } finally {
      if (in != null) in.close();
    }
    
    Iterator<String[]> roit = rows.iterator();
    double conv = 1.0 / Math.log10(2.0);
    while (roit.hasNext()) {
      String[] row = roit.next();
      String src = row[0];
      if (src.equals("R") || src.equals("E")) {
        continue;
      }
      for (int i = 1; i < row.length; i++) {
        if (colNames.get(i).equals("R") || colNames.get(i).equals("E")) {
          continue;
        }
        Double doub = Double.parseDouble(row[i]);
        if (doub == 0) {
          continue;
        }
        double log2 = Math.log10(doub.doubleValue()) * conv;
        int rndLog2 = (int)Math.round(log2);
        int numLink = (rndLog2 <= 0) ? 1 : rndLog2;
        for (int j = 0; j < numLink; j++) {
          String[] link = new String[3];
          link[0] = src;
          link[1] = (rndLog2 <= 0) ? Integer.toString(rndLog2) : Integer.toString(j + 1);
          link[2] = colNames.get(i);
          retval.add(link);
        }
      }     
    }
    return (retval);
  }
 
  
  /***************************************************************************
  ** 
  ** Save nodes
  */

  public void dumpSIF(String outfile, List<String[]> edges) throws IOException { 
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));            
    Iterator<String[]> onit = edges.iterator();
    while (onit.hasNext()) {
      String[] edge = onit.next();
      out.print(edge[0]);
      out.print("\t");
      out.print(edge[1]);
      out.print("\t");
      out.println(edge[2]);
    }
    out.close();   
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    try {
      ElementRatios cp = new ElementRatios();
      List<String[]> edges = cp.readERT(new File("/Users/bill/XXX.txt"));
      cp.dumpSIF("/Users/bill/XXX.sif", edges);
    } catch (IOException ioex) {
//      System.err.println("IO Error " + ioex);
    } 
  }
}
