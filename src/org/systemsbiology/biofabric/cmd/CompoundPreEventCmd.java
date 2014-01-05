/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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


package org.systemsbiology.biofabric.cmd;

import java.util.ArrayList;

import org.systemsbiology.biofabric.event.ChangeEvent;
import org.systemsbiology.biofabric.event.EventManager;
import org.systemsbiology.biofabric.event.LayoutChangeEvent;
import org.systemsbiology.biofabric.event.SelectionChangeEvent;

/****************************************************************************
**
** Does eventing following Compound Editing undo
*/

public class CompoundPreEventCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList ev_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public CompoundPreEventCmd() {
    ev_ = new ArrayList();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Add the events
  */ 
  
  public void addChangeEvents(ArrayList events) {
    ev_.addAll(events);
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Name to show
  */ 
  
  public String getPresentationName() {
    return ("PreEvent");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  public void undo() {
    super.undo();
    EventManager mgr = EventManager.getManager();
    int num = ev_.size();
    
    //
    // For better performance, some listeners who pay no attention to
    // the contents of a model change only care about last event:
    //
 
    for (int i = num - 1; i >= 0; i--) {
      ChangeEvent ce = (ChangeEvent)ev_.get(i);
      if (ce instanceof LayoutChangeEvent) {
        mgr.sendLayoutChangeEvent((LayoutChangeEvent)ce);
      } else if (ce instanceof SelectionChangeEvent) {
        mgr.sendSelectionChangeEvent((SelectionChangeEvent)ce);
      } else {
        throw new IllegalArgumentException();
      }
    } 
    return;
  }
  
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  public void redo() {
    super.redo();
    return;
  }
}
