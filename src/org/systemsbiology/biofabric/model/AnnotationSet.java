/*
**    Copyright (C) 2003-2018 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.CharacterEntityMapper;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.MinMax;

import org.xml.sax.Attributes;

/****************************************************************************
**
** Used for annotating contiguous sets of nodes
*/

public class AnnotationSet implements Cloneable, Iterable<AnnotationSet.Annot> {
  private TreeSet<Annot> annots_;
 
  public AnnotationSet() {
    annots_ = new TreeSet<Annot>();
  }
  
  @Override
  public AnnotationSet clone() {
    try {
      AnnotationSet retval = (AnnotationSet)super.clone();
      retval.annots_ = new TreeSet<Annot>();
      for (Annot annot : this.annots_) {
        retval.annots_.add(annot.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }    
 
  public Iterator<Annot> iterator() {
    return (annots_.iterator());
  }
  
  public int size() {
    return (annots_.size());
  }
  
  public void addAnnot(Annot an) {
    annots_.add(an);
    return;
  }
  
  public void fillAnnots(AnnotsForPos fillIt, Integer whereObj) {
    for (Annot an : annots_) {
      if (an.range_.contained(whereObj)) {
        fillIt.addAnnot(an);
      }
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Class to hold all the annotations relevant for a given position (row or column)
  */  
  
  public static class AnnotsForPos {
    private TreeMap<Integer, SortedSet<Annot>> perLayers_;
 
    public AnnotsForPos() {
      perLayers_ = new TreeMap<Integer, SortedSet<Annot>>(Collections.reverseOrder());
      perLayers_.put(Integer.valueOf(0), new TreeSet<Annot>());
    }
    
    public void addAnnot(Annot toAdd) {
      int layer = toAdd.getLayer();
      SortedSet<Annot> forLayer = perLayers_.get(Integer.valueOf(layer));
      if (forLayer == null) {
        forLayer = new TreeSet<Annot>();
        perLayers_.put(Integer.valueOf(layer), forLayer);
      }
      forLayer.add(toAdd);
      return;
    }

    public void clear() {
      for (Integer layer : perLayers_.keySet()) {
        perLayers_.get(layer).clear();
      }
      return;
    }
    
    public void displayStrings(List<String> fill) {
      for (Integer layer : perLayers_.keySet()) {
        SortedSet<Annot> forLay = perLayers_.get(layer);
        for (Annot ant : forLay) {
          fill.add(ant.getName());
        }
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class to hold a single annotation
  */  
  
  public static class Annot implements Cloneable, Comparable<Annot> {
    private String tag_;
    private MinMax range_;
    private int layer_;
   
    public Annot(String tag, int startPos, int endPos, int layer) {
      if ((startPos > endPos) || (layer < 0)) {
        System.err.println(tag + " start after end "  + startPos + " " + endPos);
        throw new IllegalArgumentException();
      }
      tag_ = tag;
      range_ = new MinMax(startPos, endPos);
      layer_ = layer;
    }
    
    @Override
    public Annot clone() {
      try {
        Annot retval = (Annot)super.clone();
        retval.range_ = this.range_.clone();
        return (retval);
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }    
   
    public String getName() {
      return (tag_);
    } 
    
    public MinMax getRange() {
      return (range_);
    }
    
    public int getLayer() {
      return (layer_);
    } 
    
    @Override
    public int hashCode() {
      return (tag_.hashCode() + range_.hashCode() + layer_);
    }

    @Override
    public String toString() {
      return ("Annot name = " + tag_ + " range = " + range_ + " layer = " + layer_);
    }
     
    @Override
    public boolean equals(Object other) {    
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof Annot)) {
        return (false);
      }
      Annot otherAnnot = (Annot)other;
    
      if (!this.tag_.equalsIgnoreCase(otherAnnot.tag_)) {
        return (false);
      }
      if (!this.range_.equals(otherAnnot.range_)) {
        return (false);
      }
      
      return (this.layer_ == otherAnnot.layer_);
    }  

    public int compareTo(Annot otherAnnot) {
      if (this.equals(otherAnnot)) {
        return (0);
      }
      
      int startDiff = this.range_.min - otherAnnot.range_.min;
      if (startDiff != 0) {
        return (startDiff);
      }
      
      int endDiff = this.range_.max - otherAnnot.range_.min;
      if (endDiff != 0) {
        return (endDiff);
      }
      
      int layerDiff = this.layer_ - otherAnnot.layer_;
      if (layerDiff != 0) {
        return (layerDiff);
      }
      return (this.tag_.compareToIgnoreCase(otherAnnot.tag_));
    }
    
    /***************************************************************************
    **
    ** Dump an annotation using XML
    */
     
    public void writeXML(PrintWriter out, Indenter ind) {    
      ind.indent();
      out.print("<annot tag=\"");
      out.print(CharacterEntityMapper.mapEntities(tag_, false));
      out.print("\" start=\"");
      out.print(range_.min);
      out.print("\" end=\"");
      out.print(range_.max); 
      out.print("\" layer=\"");
      out.print(layer_); 
      out.println("\" />");
      return;
    }
  }
  
  public static class AnnotsWorker extends AbstractFactoryClient {
    private String tag_;
  
    public AnnotsWorker(FabricFactory.FactoryWhiteboard whiteboard, String tag) {
      super(whiteboard);
      tag_ = tag;
      myKeys_.add(tag_);
      installWorker(new AnnotWorker(whiteboard), new MyAnnotGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals(tag_)) {
        FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
        board.currAnnots = new AnnotationSet();
        retval = board.currAnnots;
      }
      return (retval);     
    }
  }

  public static class MyAnnotGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)optionalArgs;
      board.currAnnots.addAnnot(board.annot);
      return (null);
    }
  }  
  public static class AnnotWorker extends AbstractFactoryClient {
     
    public AnnotWorker(FabricFactory.FactoryWhiteboard board) {
      super(board); 
      myKeys_.add("annot");
    }
     
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("annot")) {
        FabricFactory.FactoryWhiteboard board = (FabricFactory.FactoryWhiteboard)this.sharedWhiteboard_;
        board.annot = buildFromXML(elemName, attrs);
        retval = board.annot;
      }
      return (retval);     
    }
    
    private Annot buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "annot", "tag", true);
      String minStr = AttributeExtractor.extractAttribute(elemName, attrs, "annot", "start", true);
      String maxStr = AttributeExtractor.extractAttribute(elemName, attrs, "annot", "end", true);
      String layerStr = AttributeExtractor.extractAttribute(elemName, attrs, "annot", "layer", true);
   
      name = CharacterEntityMapper.unmapEntities(name, false);
      try {
        int min = Integer.valueOf(minStr).intValue();
        int max = Integer.valueOf(maxStr).intValue();
        int layer = Integer.valueOf(layerStr).intValue();
        Annot retval = new Annot(name, min, max, layer);
        return (retval);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }      
    }
  }
}
