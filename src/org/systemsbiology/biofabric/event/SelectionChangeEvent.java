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


package org.systemsbiology.biofabric.event;

/****************************************************************************
**
** Used to signal a selection change
*/

public class SelectionChangeEvent implements ChangeEvent {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static int NO_CHANGE          = 0;
  public final static int UNSPECIFIED_CHANGE = 1;
  public final static int SELECTED_MODEL     = 2;
  public final static int SELECTED_ELEMENT   = 3;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private String genomeKey_;
  private String layoutKey_;  
  private int changeType_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public SelectionChangeEvent(String genomeKey, String layoutKey, int changeType) {
    genomeKey_ = genomeKey;
    layoutKey_ = layoutKey;
    changeType_ = changeType;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the genome key
  */ 
  
  public String getGenomeKey() {
    return (genomeKey_);
  }

  /***************************************************************************
  **
  ** Get the layout key
  */ 
  
  public String getLayoutKey() {
    return (layoutKey_);
  }  
  
  /***************************************************************************
  **
  ** Get the change type
  */ 
  
  public int getChangeType() {
    return (changeType_);
  }
  
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
    if (!(other instanceof SelectionChangeEvent)) {
      return (false);
    }
    SelectionChangeEvent otherSCE = (SelectionChangeEvent)other;
    if (!this.genomeKey_.equals(otherSCE.genomeKey_)) {
      return (false);
    }
    if (!this.genomeKey_.equals(otherSCE.genomeKey_)) {
      return (false);
    }    
    return (this.changeType_ == otherSCE.changeType_);
  }   
}
