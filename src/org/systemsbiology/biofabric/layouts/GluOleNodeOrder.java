/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/****************************************************************************
**
** Generate node order attribute files for the example combined Glucose/Oleate
** network discussed in the blog post at:
**
** http://biofabric.blogspot.com/2013/11/i-view-yeast-to-breadth-and-height.html
*/

public class GluOleNodeOrder {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GluOleNodeOrder() {
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Read in a SIF line and return a map of target node names to Bintags
  */

  public Map<String, Bintag> readSIF(String infile1) throws IOException {    
 
    Map<String, Bintag> tags = new HashMap<String, Bintag>();
    String line;   
    BufferedReader in = new BufferedReader(new FileReader(infile1));
    while ((line = in.readLine()) != null) {
      String[] tokens = line.split("\\t");
      if (tokens.length == 0) {
        continue;
      } else if ((tokens.length == 1) || (tokens.length == 2)) {
        throw new IOException();
      } else {
        String src = tokens[0].trim();
        String tag = tokens[1].trim();
        String trg = tokens[2].trim();
        Bintag btag = tags.get(trg);
        if (btag == null) {
          btag = new Bintag(src, tag);
          tags.put(trg, btag);
        } else {
          btag.addRel(src, tag);
        }
      }
    }  
    in.close();
    return (tags);
  }
 
  /***************************************************************************
  ** 
  ** Invert the tag map to order the nodes
  */

  public SortedMap<Bintag, SortedSet<String>> invertTags(Map<String, Bintag> tagging) { 
    SortedMap<Bintag, SortedSet<String>> retval = new TreeMap<Bintag, SortedSet<String>>(Collections.reverseOrder());
    Iterator<String> tkit = tagging.keySet().iterator();
    while (tkit.hasNext()) {
      String trg = tkit.next();
      Bintag btag = tagging.get(trg);
      SortedSet<String> ss = retval.get(btag);
      if (ss == null) {
        ss = new TreeSet<String>();
        retval.put(btag, ss);
      }
      ss.add(trg);
    }  
    return (retval);
  }
  
  /***************************************************************************
  ** 
  ** Flatten the sorted inverted map to a single list
  */

  public List<String> flattenTags(SortedMap<Bintag, SortedSet<String>> mapped) { 
    List<String> retval = new ArrayList<String>();
    Iterator<SortedSet<String>> tkit = mapped.values().iterator();
    while (tkit.hasNext()) {
      SortedSet<String> ss = tkit.next();
      retval.addAll(ss);
    }  
    return (retval);
  }
   
  /***************************************************************************
  ** 
  ** Save node order file
  */

  public void dumpNOA(String outfile, List<String> ordering) throws IOException { 
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));               
    out.println("Node Row");
    out.println("O = 0");
    out.println("Y = 1");
    out.println("P = 2");
    out.println("A = 3");
    
    int count = 4;
    Iterator<String> onit = ordering.iterator();
    while (onit.hasNext()) {
      String nodeID = onit.next();
      out.print(nodeID);
      out.print(" = ");
      out.println(count++);
    }
    out.close();   
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Run a cycle
  */
  
  public void runCycle(String sifName, String noaBase, Bintag.Mode daMode) throws IOException {
    Bintag.setMode(daMode);
    Map<String, Bintag> btm = readSIF(sifName);
    SortedMap<Bintag, SortedSet<String>> im = invertTags(btm);
    List<String> flat = flattenTags(im);
    dumpNOA(noaBase + daMode + ".noa", flat);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Run it
  */

  public static void main(String[] argv) {
    try {
      GluOleNodeOrder cp = new GluOleNodeOrder();
      String sifName = argv[0];
      String noaBase = argv[1];
      cp.runCycle(sifName, noaBase, Bintag.Mode.ODOMETER);
      cp.runCycle(sifName, noaBase, Bintag.Mode.GLUCOSE_BASE);
      cp.runCycle(sifName, noaBase, Bintag.Mode.OLEATE_BASE);
    } catch (IOException ioex) {
      System.err.println("IO Error " + ioex);
    } 
  }
  
////////////////////////////////////////////////////////////////////////////
//
// INNER CLASSES
//
////////////////////////////////////////////////////////////////////////////
  
 /***************************************************************************
 **
 ** Sorting tags
 */  
  
  
  
  public static class Bintag implements Comparable<Bintag> {
    
    public enum Mode {ODOMETER, GLUCOSE_BASE, OLEATE_BASE}
    
    public boolean inAo;
    public boolean inOo;
    public boolean inYo;
    public boolean inPo;
    public boolean inAg;
    public boolean inOg;
    public boolean inYg;
    public boolean inPg;
    private static Mode ourMode_;
      
    public static void setMode(Mode daMode) {
      ourMode_ = daMode;
      return;
    }  
    
    Bintag(String src, String rel) {
      addRel(src, rel);
    }
      
    public final void addRel(String src, String rel) {
      if (src.equals("A")) {
        if (rel.equals("pd-o")) {
          inAo = true;
        } else {
          inAg = true;
        }        
      } else if (src.equals("O")) {
        if (rel.equals("pd-o")) {          
          inOo = true;
        } else {
          inOg = true;
        } 
      } else if (src.equals("P")) {
        if (rel.equals("pd-o")) {
          inPo = true;
        } else {
          inPg = true;
        }                
      } else if (src.equals("Y")) {
        if (rel.equals("pd-o")) {
          inYo = true;
        } else {
          inYg = true;
        }                                
      }
      return;
    }
      
    @Override
    public boolean equals(Object other) {    
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof Bintag)) {
        return (false);
      }
      Bintag otherBT = (Bintag)other;

      if (this.inAo != otherBT.inAo) return false;
      if (this.inOo != otherBT.inOo) return false;
      if (this.inYo != otherBT.inYo) return false;
      if (this.inPo != otherBT.inPo) return false;
      if (this.inAg != otherBT.inAg) return false;
      if (this.inOg != otherBT.inOg) return false;
      if (this.inYg != otherBT.inYg) return false;
      if (this.inPg != otherBT.inPg) return false;
    
      return (true);
    }
    
    @Override
    public int hashCode() {
      int sum = 0;
      switch (ourMode_) {
        case ODOMETER:
          // All O, then all Y, etc.
          if (inAo) sum += 1;
          if (inAg) sum += 2;
          if (inPo) sum += 4;
          if (inPg) sum += 8;
          if (inYo) sum += 16;
          if (inYg) sum += 32;
          if (inOo) sum += 64;
          if (inOg) sum += 128;
          break;
        case GLUCOSE_BASE:
          // All Glu, Then Ola
          if (inAo) sum += 1;
          if (inAg) sum += 16;
          if (inPo) sum += 2;
          if (inPg) sum += 32;
          if (inYo) sum += 4;
          if (inYg) sum += 64;
          if (inOo) sum += 8;
          if (inOg) sum += 128;
          break;
        case OLEATE_BASE:
          // All Ola, then GLu        
          if (inAo) sum += 16;
          if (inAg) sum += 1;
          if (inPo) sum += 32;
          if (inPg) sum += 2;
          if (inYo) sum += 64;
          if (inYg) sum += 4;
          if (inOo) sum += 128;
          if (inOg) sum += 8;
          break;
        default:
          throw new IllegalStateException();
      }
      return (sum);
    }
    
    public int compareTo(Bintag otherBT) {
      return (this.hashCode() - otherBT.hashCode());
    }    
    
    @Override
    public String toString() {
      return (Integer.toString(hashCode()));
    }       
  }  
}
