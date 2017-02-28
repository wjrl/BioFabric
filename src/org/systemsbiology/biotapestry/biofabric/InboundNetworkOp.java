/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.biofabric;

import java.util.ArrayList;
import java.util.List;


/****************************************************************************
**
** Class for operation to build a network
*/

public class InboundNetworkOp extends InboundGaggleOp {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList links_;
  private ArrayList singles_;
  private String species_;
   
  /***************************************************************************
  **
  ** Create the op - called on RMI thread
  */

  public InboundNetworkOp(String species, List links, List singles) {
    super(null);
    links_ = new ArrayList();
    int iLen = links.size();
    for (int i = 0; i < iLen; i++) {
      links_.add(((FabricLink)links.get(i)).clone());
    }
    singles_ = new ArrayList(singles);
    species_ = species;
    return;
  }

////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Execute the op - called on AWT thread
  */

  public void executeOp() {
    FabricCommands fc = FabricCommands.getCmds("mainWindow");
    fc.loadFromGaggle(links_, singles_);
    return;
  } 
}
