/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.util.UiUtil;

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

  private JLabel nodeName_;
  private JLabel linkName_;
  private JLabel linkZone_;

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
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    
    nodeName_ = new JLabel("Mouse Over Node Row: <none>");
    linkName_ = new JLabel("Mouse Over Link: <none>");
    linkZone_ = new JLabel("Mouse Over Node Link Zone: <none>");

    UiUtil.gbcSet(gbc, 0, 0, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 0.0);
    add(nodeName_, gbc);
       
    UiUtil.gbcSet(gbc, 11, 0, 45, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 0.0);
    add(linkName_, gbc);
    
    UiUtil.gbcSet(gbc, 56, 0, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.5, 0.0);
    add(linkZone_, gbc);   
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

  public void setNodeAndLink(String nodeName, String linkName, String drainZone) {
    nodeName_.setText("Mouse Over Node Row: " + nodeName);
    linkName_.setText("Mouse Over Link: " + linkName);
    linkZone_.setText("Mouse Over Node Link Zone: " + drainZone);
    nodeName_.invalidate();
    linkName_.invalidate();
    linkZone_.invalidate();
    revalidate();
    return;
  }   
}
