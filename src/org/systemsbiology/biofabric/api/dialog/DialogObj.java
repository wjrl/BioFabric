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

package org.systemsbiology.biofabric.api.dialog;

import javax.swing.Box;

import org.systemsbiology.biofabric.api.util.FixedJButton;

/****************************************************************************
**
** Helpers for common dialog tasks
*/

public interface DialogObj {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLIENT INTERFACE
  //
  ////////////////////////////////////////////////////////////////////////////    

  public interface DialogSupportClient {
    public void applyAction();
    public void okAction();
    public void closeAction();
  }
 
  /***************************************************************************
  **
  ** Use to hand back buttons
  */ 

  public static class Buttons {
    public FixedJButton applyButton;
    public FixedJButton okButton;
    public FixedJButton cancelButton;  
  }
  
  public static class ButtonsAndBox {
    public Buttons buttons;
    public Box buttonBox;
  }
}
