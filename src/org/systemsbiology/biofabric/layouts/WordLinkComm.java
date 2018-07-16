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

package org.systemsbiology.biofabric.layouts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.biofabric.analysis.Link;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.util.MinMax;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** Do layout that creates edge communities
*/

public class WordLinkComm {
  
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

  public WordLinkComm() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Do reading to records
  */

  public Set readEdgeTab(File infile) throws IOException {
    //
    // Read in the lines.
    //    
    
    BufferedReader in = null;
    HashSet interact = new HashSet();
    
    try {
      in = new BufferedReader(new FileReader(infile));

      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\\t");
        if (tokens.length != 2) {
          System.err.println("WARNING: Short Line: " + line);
          continue;
        } else {
          Link link = new Link(tokens[0].toUpperCase(), tokens[1].toUpperCase());
          interact.add(link);
        }
      }
  
    } finally {
      if (in != null) in.close();
    }
    return (interact);
  }
  
  /***************************************************************************
  ** 
  ** Do reading 
  */

  public SortedMap readCSV(File infile, List allLinks, Map nodeOrder, SortedSet groupOrder, Set trueLinks) throws IOException {

    TreeMap retval = new TreeMap();
    ArrayList groups = new ArrayList();
    BufferedReader in = null;
    int lineNum = 0;
    try {
      in = new BufferedReader(new FileReader(infile));
      String line = null;
      while ((line = in.readLine()) != null) {
        if (lineNum++ == 0) {
          continue;
        }
        if (line.trim().equals("")) {
          continue;
        }
        List tokens = processCSVLine(line);
        if (tokens.isEmpty()) {
          continue;
        }
        Integer comName = Integer.valueOf(((String)tokens.get(0)).trim());
        MinMax forGrp = (new MinMax()).init();
        groups.add(new GroupRange(comName, forGrp));
        HashSet links = (HashSet)retval.get(comName);
        if (links == null) {
          links = new HashSet();
          retval.put(comName, links);
        }
        String geneStr = ((String)tokens.get(1)).trim();        
        String[] genes = geneStr.split(",");
        for (int i = 0; i < genes.length - 1; i++) {
          String gi = genes[i].replaceAll(" ", "_");
          for (int j = i + 1; j < genes.length; j++) {
            String gj = genes[j].replaceAll(" ", "_");
            Link testLink = new Link(gi, gj);
            Link testLinkR = new Link(gj, gi);
            if ((trueLinks == null) || (trueLinks.contains(testLink) || trueLinks.contains(testLinkR))) {
            	UiUtil.fixMePrintout("Restore this");
            	/*
              FabricLink nfl = new FabricLink(gi, gj, comName.toString(), false);
              if (!nodeOrder.isEmpty()) {
                forGrp.update((Integer.parseInt((String)nodeOrder.get(gi))));
                forGrp.update((Integer.parseInt((String)nodeOrder.get(gj))));
              }
              allLinks.add(nfl);     
              links.add(nfl);
              FabricLink snfl = new FabricLink(gi, gj, comName.toString(), true);
              links.add(snfl);
              */
            }
          }         
        }
      }
    } finally {
      if (in != null) in.close();
    }
    
    groupOrder.addAll(groups);
 
    return (retval);
  }

 /***************************************************************************
  ** 
  ** Process a csv line into tokens
  */

  private List processCSVLine(String line) {
    // Pattern for CSV from "Mastering Regular Expressions 2nd Ed." by Friedl (O'Reilly)
    Pattern pat = Pattern.compile(
      "\\G(?:^|,) (?: \" ( (?> [^\"]*+ ) (?> \"\" [^\"]*+ )*+ ) \" | ( [^\",]*+ ) )",
      Pattern.COMMENTS);
    Pattern doubq = Pattern.compile("\"\"");
    Matcher mainMatch = pat.matcher("");
    Matcher doubMatch = doubq.matcher("");
    
    ArrayList argList = new ArrayList();
    argList.clear();
    mainMatch.reset(line);
    while (mainMatch.find()) {
      String group = mainMatch.group(2);
      if (group != null) {
        argList.add(group.trim());
      } else {        
        doubMatch.reset(mainMatch.group(1));
        argList.add(doubMatch.replaceAll("\"").trim());
      }
    }
    //
    // Chop off trailing empty tokens:
    //
    ArrayList retval = new ArrayList();
    int alnum = argList.size();
    boolean chopping = true;
    for (int i = alnum - 1; i >= 0; i--) {
      String tok = (String)argList.get(i);
      if (chopping) {
        if (tok.trim().equals("")) {
          continue;
        } else {
          chopping = false;
        }
      }
      if (!chopping) {
        retval.add(0, tok);
      }
    }
    
    return (retval);
  } 
    
 
  /***************************************************************************
  ** 
  ** Save nodes
  */

  public void dumpEDA(String outfile, SortedMap ordering) throws IOException { 
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));            
    out.println("Link Column");    
    Iterator onit = ordering.keySet().iterator();
    while (onit.hasNext()) {
      Integer col = (Integer)onit.next();
      String edgeID = (String)ordering.get(col);
      out.print(edgeID);
      out.print(" = ");
      out.println(col);
    }
    out.close();   
    return;
  } 
   
  /***************************************************************************
  ** 
  ** Do reading
  */

  public Map readEdgeOrder(File file) throws IOException {
    HashMap attributes = new HashMap();   
    AttributeLoader.ReadStats stats = new AttributeLoader.ReadStats();
    AttributeLoader alod = new AttributeLoader();
    UiUtil.fixMePrintout("Restore this");
    //alod.readAttributes(file, false, attributes, stats);
    return (attributes);
  }
  
  /***************************************************************************
  ** 
  ** Do reading
  */

  public Map readNodeOrder(File file) throws IOException {
    HashMap attributes = new HashMap();   
    AttributeLoader.ReadStats stats = new AttributeLoader.ReadStats();
    AttributeLoader alod = new AttributeLoader();
    UiUtil.fixMePrintout("Restore this");
   // alod.readAttributes(file, true, attributes, stats);
    return (attributes);
  }
  
  /***************************************************************************
  ** 
  ** Save nodes
  */

  public void dumpNOA(String outfile, SortedMap ordering) throws IOException { 
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));            
    out.println("Node Row");
    int count = 0;
    Iterator onit = ordering.keySet().iterator();
    while (onit.hasNext()) {
      Integer row = (Integer)onit.next();
      String nodeID = (String)ordering.get(row);
      out.print(nodeID);
      out.print(" = ");
      out.println(count++);
    }
    out.close();   
    return;
  }   
  
  /***************************************************************************
  ** 
  ** Dump sif
  */

  public void dumpSIF(String outfile, List allLinks) throws IOException { 
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));
    Iterator onit = allLinks.iterator();
    while (onit.hasNext()) {
      FabricLink nfl = (FabricLink)onit.next();
//      out.print(nfl.getSrcID());
      out.print("\t");
      out.print(nfl.getAugRelation().relation);
      out.print("\t");
//      out.println(nfl.getTrgID());
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
  ** HOWTO!
  **
  ** 1) Build sif file from CSV (set forSIF = true!)
  ** 2) Load into BioFabric
  ** 3) Order nodes using similarity layout
  ** 4) Export node order
  ** 5) Export link order
  ** 6) Run this again (set forSIF = false!)
  ** 7) Run BioFabric edge-attribute layout 
  */  
  
  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    try {
      boolean forSIF = false; // set to true for first-time sif creation!
      WordLinkComm cp = new WordLinkComm();
      ArrayList allLinks = new ArrayList();
      
      Set trueLinks = cp.readEdgeTab(new File("/users/wlongaba/XXX/word.edgelist"));

      Map nodeOrder = (!forSIF) ? cp.readNodeOrder(new File("/users/wlongaba/XXX.noa"))
                                : new HashMap();

      TreeSet forGroups = new TreeSet(new GroupRangeComparator());
      SortedMap linkAssign = cp.readCSV(new File("/users/wlongaba/XXX.csv"), allLinks, nodeOrder, forGroups, trueLinks);
      cp.dumpSIF("/users/wlongaba/XXX.sif", allLinks);    
      

      if (!forSIF) {     
        Map origOrder = cp.readEdgeOrder(new File("/users/wlongaba/XXX.eda"));

        TreeMap reorder = new TreeMap(new GroupRangeComparator());      
        Iterator grit = forGroups.iterator();
        while (grit.hasNext()) {
          GroupRange gr = (GroupRange)grit.next();
          TreeMap perGrp = new TreeMap();
          reorder.put(gr, perGrp);
          Set laPerGrp = (Set)linkAssign.get(gr.name);
          Iterator pgit = laPerGrp.iterator();
          while (pgit.hasNext()) {
            FabricLink nextLink = (FabricLink)pgit.next();
            String col = (String)origOrder.get(nextLink);
            String rel = nextLink.getAugRelation().relation;
            String pref = (nextLink.getAugRelation().isShadow) ? "shdw(" : "(";
//            String asText = nextLink.getSrcID() + " " + pref + rel + ") " + nextLink.getTrgID();
            Integer colI = Integer.valueOf(col);
//            perGrp.put(colI, asText);
          }
        }      

        int count = 0;
        TreeMap ordering = new TreeMap();
        Iterator rit = reorder.keySet().iterator();
        while (rit.hasNext()) {
          GroupRange comm = (GroupRange)rit.next();
          TreeMap forComm = (TreeMap)reorder.get(comm);
          Iterator fcit = forComm.values().iterator();
          while (fcit.hasNext()) {
            String linkTxt = (String)fcit.next();
            ordering.put(new Integer(count++), linkTxt);          
          }
        }

        cp.dumpEDA("/users/wlongaba/XXX-G.eda", ordering);
      }     
    } catch (IOException ioex) {
      System.err.println("IO Error " + ioex);
    } 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  
  /***************************************************************************
  **
  ** Just compares the basics.  Child class may be equal even though they
  ** are different.
  */
  
  
  public static class GroupRange  {
    public Integer name;
    public MinMax range; 
    
    public GroupRange(Integer name, MinMax range) {
      this.name = name;
      this.range = range;
    }
    
    public int hashCode() {
      return (name.hashCode() + range.hashCode());      
    }
       
    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof GroupRange)) {
        return (false);
      }
      GroupRange otherGR = (GroupRange)other;
      
      if (!this.name.equals(otherGR.name)) {
        return (false);
      }      
      return (this.range.equals(otherGR));
    }
  }
  
  
  /***************************************************************************
  **
  */
  
  
  public static class GroupRangeComparator implements Comparator {

    public int compare(Object first, Object second) {
      GroupRange firstGR = (GroupRange)first;
      GroupRange secondGR = (GroupRange)second;
      
      MinMax firstMM = firstGR.range;
      MinMax secondMM = secondGR.range;
      int fComp = firstMM.min - secondMM.min;
      if (fComp != 0) {
        return (fComp);
      }
      int xComp = firstMM.max - secondMM.max;
      if (xComp != 0) {
        return (xComp);
      }     
      return (firstGR.name.compareTo(secondGR.name));      
    }
  }   
}
