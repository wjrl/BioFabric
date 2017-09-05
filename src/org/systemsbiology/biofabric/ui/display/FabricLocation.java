/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.ui.display;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;

import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** This is location announcement
*/

public class FabricLocation extends JPanel {
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
	
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private InfoPanel nodePanel_;
  private InfoPanel linkPanel_;
  private InfoPanel linkZonePanel_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public FabricLocation() {
    setBackground(Color.WHITE);
  	setLayout(new GridLayout(1, 3));
    nodePanel_ = new InfoPanel(false, 15, 150, false);  
    linkPanel_ = new InfoPanel(false, 15, 150, false);
    UiUtil.fixMePrintout("Create some separation! This doesn't do it!");
   // linkPanel_.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));
    linkPanel_.setBackground(Color.BLUE);
    linkZonePanel_ = new InfoPanel(false, 15, 150, false);
    
    nodePanel_.installName("Mouse Over Node Row: <none>");
    linkPanel_.installName("Mouse Over Link: <none>");
    linkZonePanel_.installName("Mouse Over Node Link Zone: <none>");
   
    add(nodePanel_);
    add(linkPanel_);
    add(linkZonePanel_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Drawing routine
  */

  public void setNodeAndLink(BioFabricPanel.MouseLocInfo mlo) {
     nodePanel_.installName("Mouse Over Node Row: " + mlo.nodeDesc);
	   linkPanel_.installName("Mouse Over Link: " + mlo.linkDesc);
	   linkZonePanel_.installName("Mouse Over Node Link Zone: " + mlo.zoneDesc);
     repaint();   
    return;
  }   
}
