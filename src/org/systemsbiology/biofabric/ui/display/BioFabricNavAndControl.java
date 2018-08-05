
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

package org.systemsbiology.biofabric.ui.display;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.systemsbiology.biofabric.cmd.CommandSet;
import org.systemsbiology.biofabric.util.ResourceManager;


/****************************************************************************
**
** This is the BioFabric Control dashboard wrapper
*/

public class BioFabricNavAndControl {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private MyNavPanel myPanel_;
  private FabricMagnifyingTool fmt_;
  private BioFabricOverview bfo_;
  private MouseOverView mvo_;
  private FabricNavTool fnt_;
  private FabricNavTool.LabeledFabricNavTool lfnt_;
  private FabricLocation floc_;
  private CardLayout clay_;
  private boolean collapsed_;
  private JPanel withControls_;
  private JSplitPane spot_;
  private double savedSplitFrac_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BioFabricNavAndControl(boolean isMain, JFrame topWindow, boolean isHeadless) {

    if (isHeadless) {
      return;
    }
    myPanel_ = new MyNavPanel();
    
    floc_ = new FabricLocation();
