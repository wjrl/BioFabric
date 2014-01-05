/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.gaggle;

import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.model.FabricLink;


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

  private ArrayList<FabricLink> links_;
  private ArrayList<String> singles_;
  //private String species_;
   
  /***************************************************************************
  **
  ** Create the op - called on RMI thread
  */

  public InboundNetworkOp(String species, List<FabricLink> links, List<String> singles) {
    super(null);
    links_ = new ArrayList<FabricLink>();
    int iLen = links.size();
    for (int i = 0; i < iLen; i++) {
      links_.add(links.get(i).clone());
    }
    singles_ = new ArrayList<String>(singles);
    //species_ = species;
    return;
  }

////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Execute the op - called on AWT thread
  */

  @Override
  public void executeOp() {
    CommandSet fc = CommandSet.getCmds("mainWindow");
    fc.loadFromGaggle(links_, singles_);
    return;
  } 
}
