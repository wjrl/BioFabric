/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.event;

/****************************************************************************
**
** Used to signal changes in the current model
*/

public class ModelChangeEvent implements ChangeEvent {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static int NO_CHANGE           = 0;
  public final static int UNSPECIFIED_CHANGE  = 1;
  public final static int MODEL_DROPPED       = 2;
  public final static int PROPERTY_CHANGE     = 3;
  public final static int MODEL_ADDED         = 4;  
  public final static int DYNAMIC_MODEL_ADDED = 5;    
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private String key_;
  private int changeType_;
  private boolean isProxy_;
  private String oldKey_;
  private boolean oldKeyIsProxy_;  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public ModelChangeEvent(String genomeKey, int changeType) {
    key_ = genomeKey;
    changeType_ = changeType;
    isProxy_ = false;
  }

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public ModelChangeEvent(String key, int changeType, boolean isProxy) {
    key_ = key;
    changeType_ = changeType;
    isProxy_ = isProxy;
  }  

  /***************************************************************************
  **
  ** Build the event
  */ 
  
  public ModelChangeEvent(String key, int changeType, boolean isProxy, 
                          String oldKey, boolean oldKeyIsProxy) {
    key_ = key;
    changeType_ = changeType;
    isProxy_ = isProxy;
    oldKey_ = oldKey;
    oldKeyIsProxy_ = oldKeyIsProxy;    
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
    if (isProxy_) {
      throw new IllegalStateException();
    }
    return (key_);
  }

  /***************************************************************************
  **
  ** Answer if we have a proxy key
  */ 
  
  public boolean isProxyKey() {
    return (isProxy_);
  }
  
  /***************************************************************************
  **
  ** Get the proxy key
  */ 
  
  public String getProxyKey() {
    if (!isProxy_) {
      throw new IllegalStateException();
    }
    return (key_);
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
  ** Get the old key (may be null)
  */ 
  
  public String getOldKey() {
    return (oldKey_);
  }  
  
  /***************************************************************************
  **
  ** Answer if the old key is a proxy key (only valid if old key != null)
  */ 
  
  public boolean oldKeyIsProxy() {
    return (oldKeyIsProxy_);
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
    if (!(other instanceof ModelChangeEvent)) {
      return (false);
    }
    ModelChangeEvent otherMCE = (ModelChangeEvent)other;

    if (this.changeType_ != otherMCE.changeType_) {
      return (false);
    }   

    if (this.isProxy_ != otherMCE.isProxy_) {
      return (false);
    }
    
    if (!this.key_.equals(otherMCE.key_)) {
      return (false);
    }
    
    if (this.oldKey_ == null) {
      return (otherMCE.oldKey_ == null);
    } else if (otherMCE.oldKey_ == null) {
      return (this.oldKey_ == null);
    }
    
    if (!this.oldKey_.equals(otherMCE.oldKey_)) {
      return (false);
    }    
 
    return (this.oldKeyIsProxy_ == otherMCE.oldKeyIsProxy_);
  }     
}
