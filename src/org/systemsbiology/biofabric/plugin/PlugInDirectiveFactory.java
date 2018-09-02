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

package org.systemsbiology.biofabric.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.systemsbiology.biofabric.api.parser.ParserClient;
import org.xml.sax.Attributes;

/****************************************************************************
**
** This handles Plugin Directive Creation from XML files
*/

public class PlugInDirectiveFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashSet<String> allKeys_; 
  private ParserClient currClient_;
  private HashMap<String, ParserClient> clients_;
  private FactoryWhiteboard whiteBoard_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PlugInDirectiveFactory(PlugInManager mgr) {
      
    allKeys_ = new HashSet<String>();
    whiteBoard_ = new FactoryWhiteboard(mgr);
   
    PlugInManager.DirectiveWorker ndw = new PlugInManager.DirectiveWorker(whiteBoard_);    
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(ndw);
    
    allKeys_.addAll(ndw.keywordsOfInterest()); 

    Iterator<ParserClient> cit = alist.iterator();
    clients_ = new HashMap<String, ParserClient>();
    while (cit.hasNext()) {
      ParserClient pc = cit.next();
      Set<String> keys = pc.keywordsOfInterest();
      Iterator<String> ki = keys.iterator();
      while (ki.hasNext()) {
        String key = ki.next();
        Object prev = clients_.put(key, pc);
        if (prev != null) {
          throw new IllegalArgumentException();
        }
      }
    }
    currClient_ = null;
    
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  ** 
  ** Set the container
  */

  public void setContainer(Object container) {
    if (currClient_ != null) {
      currClient_.setContainer(container);
    }    
    return;    
  }
    
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) throws IOException {
    if (currClient_ == null) {
      return (false);
    }
    if (currClient_.finishElement(elemName)) {
      currClient_ = null;
    }
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
    if (currClient_ != null) {
      currClient_.processCharacters(chars, start, length);
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public Set<String> keywordsOfInterest() {
    return (allKeys_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {

    if (currClient_ != null) {
      return (currClient_.processElement(elemName, attrs));
    }
    
    ParserClient pc = clients_.get(elemName);
    if (pc != null) {
      currClient_ = pc; 
      return (currClient_.processElement(elemName, attrs));
    }
    return (null);
  }

  public static class FactoryWhiteboard {
  	public PlugInManager mgr;
  	public AbstractPlugInDirective dir;
    
    public FactoryWhiteboard(PlugInManager mgr) {
      this.mgr = mgr;
    } 
  }
}

