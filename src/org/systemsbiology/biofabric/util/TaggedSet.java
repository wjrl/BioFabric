/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;

/****************************************************************************
**
** Set of STRINGS with an attribute tag
*/

public class TaggedSet implements Cloneable {
  
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
   
  public static final int UNTAGGED = -1;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  public int tag;
  public HashSet set;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedSet() {
    this.tag = UNTAGGED;
    this.set = new HashSet();
  }       
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedSet(int tag, Set set) {
    this.tag = tag;
    this.set = new HashSet(set);
  }
  
  /***************************************************************************
  **
  ** Constructor for IO
  */

  public TaggedSet(String tag) throws IOException {
    try {
      this.tag = Integer.parseInt(tag);
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    this.set = new HashSet();
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TaggedSet(TaggedSet other) {
    this.tag = other.tag;
    this.set = new HashSet(other.set);
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Standard equals
  */

  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TaggedSet)) {
      return (false);
    }
    TaggedSet otherTS = (TaggedSet)other;

    if (this.tag != otherTS.tag) {
      return (false);
    }
    
    return (this.set.equals(otherTS.set));
  }
  
  /***************************************************************************
  **
  ** Standard hashcode
  */

  public int hashCode() {
    return (set.hashCode() + tag);
  }  
 
  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      TaggedSet retval = (TaggedSet)super.clone();
      retval.set = (HashSet)this.set.clone();
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();
    }
  }      

  /***************************************************************************
  **
  ** Standard toString
  */

  public String toString() {
    return ("TaggedSet: tag = " + tag + " set = " + set);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();     
    out.print("<taggedSet");
    if (tag != UNTAGGED) {
      out.print(" tag=\""); 
      out.print(tag);
      out.print("\" ");
    }
    if (set.isEmpty()) {
      out.println("/>");          
      return;
    }    
    out.println(">");    
    ind.up().indent();
    out.println("<setMembers>");
    Iterator si = set.iterator();
    ind.up();
    while (si.hasNext()) {
      String mod = (String)si.next();
      ind.indent();
      out.print("<setMember id=\"");
      out.print(mod);
      out.println("\" />");
    }
    ind.down().indent();
    out.println("</setMembers>");    
    ind.down().indent();
    out.println("</taggedSet>");
    return;
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TaggedSetWorker extends AbstractFactoryClient {
    
    public TaggedSetWorker(FactoryUtilWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("taggedSet");
      installWorker(new TaggedSetMemberWorker(whiteboard), new MyGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("taggedSet")) {
        FactoryUtilWhiteboard board = (FactoryUtilWhiteboard)this.sharedWhiteboard_;
        board.currentTaggedSet = buildFromXML(elemName, attrs);
        retval = board.currentTaggedSet;
      }
      return (retval);     
    }
    
    private TaggedSet buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String tag = AttributeExtractor.extractAttribute(elemName, attrs, "taggedSet", "tag", false);
      return ((tag == null) ? new TaggedSet() : new TaggedSet(tag));
    }
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryUtilWhiteboard board = (FactoryUtilWhiteboard)optionalArgs;
      TaggedSet tagSet = board.currentTaggedSet;
      String tagSetMember = board.currentTaggedSetMember;
      tagSet.set.add(tagSetMember);
      return (null);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class TaggedSetMemberWorker extends AbstractFactoryClient {
    
    public TaggedSetMemberWorker(FactoryUtilWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("setMember");
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("setMember")) {
        FactoryUtilWhiteboard board = (FactoryUtilWhiteboard)this.sharedWhiteboard_;
        board.currentTaggedSetMember = buildFromXML(elemName, attrs);
        retval = board.currentTaggedSetMember;
      }
      return (retval);
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "setMember", "id", true);
      return (id);
    } 
  }  
}
