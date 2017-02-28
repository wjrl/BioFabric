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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JSplitPane;
import javax.swing.border.LineBorder;
import org.systemsbiology.biotapestry.util.ResourceManager;


/****************************************************************************
**
** This is the BioFabric Control dashboard
*/

public class BioFabricNavAndControl extends JPanel {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
  private BioFabricPanel cp_;
  private FabricMagnifyingTool fmt_;
  private BioFabricOverview bfo_;
  private FabricNavTool fnt_;
  private FabricLocation floc_;
  
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
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public BioFabricNavAndControl(boolean isMain, JFrame topWindow) {

    floc_ = new FabricLocation();

    FabricCommands fc = FabricCommands.getCmds((isMain) ? "mainWindow" : "selectionWindow");
    fmt_ = new FabricMagnifyingTool(fc.getColorGenerator());
    fmt_.keyInstall((JPanel)topWindow.getContentPane());
    JPanel fmpan = new JPanel();
    fmpan.setLayout(new BorderLayout());
    fmpan.setBorder(new LineBorder(Color.black, 2));
    Font labelFont = new Font("SansSerif", Font.BOLD, 20);
    JLabel magLab = new JLabel(ResourceManager.getManager().getString("biofabric.magnifier"));
    magLab.setOpaque(true);
    magLab.setBackground(Color.white);
    magLab.setFont(labelFont);
    fmpan.add(magLab, BorderLayout.NORTH);
    fmpan.add(fmt_, BorderLayout.CENTER);

    bfo_ = new BioFabricOverview();
    fmt_.setFabricOverview(bfo_);
    
    JPanel fopan = new JPanel();
    fopan.setLayout(new BorderLayout());
    fopan.setBorder(new LineBorder(Color.black, 2));
    JLabel overLab = new JLabel(ResourceManager.getManager().getString("biofabric.overview"));
    overLab.setOpaque(true);
    overLab.setBackground(Color.white);
    overLab.setFont(labelFont);
    fopan.add(overLab, BorderLayout.NORTH);
    fopan.add(bfo_, BorderLayout.CENTER);
    
    fnt_ = new FabricNavTool(topWindow);
    JPanel fnpan = new JPanel();
    fnpan.setLayout(new BorderLayout());
    fnpan.setBorder(new LineBorder(Color.black, 2));
    JLabel navLab = new JLabel(ResourceManager.getManager().getString("biofabric.tour"));
    navLab.setOpaque(true);
    navLab.setBackground(Color.white);
    navLab.setFont(labelFont);
    fnpan.add(navLab, BorderLayout.NORTH);
    fnpan.add(fnt_, BorderLayout.CENTER);
      
    JPanel buttonsAndStuff = new JPanel();
    buttonsAndStuff.setLayout(new BorderLayout());
    buttonsAndStuff.add(fopan, BorderLayout.CENTER);
    buttonsAndStuff.add(fnpan, BorderLayout.EAST);
    
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fmpan, buttonsAndStuff);
    setLayout(new BorderLayout());
    add(floc_, BorderLayout.NORTH);
    add(sp, BorderLayout.CENTER);   
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the FabricNavTool
  */

  public FabricNavTool getNavTool() {
    return (fnt_);
  }  
    
  /***************************************************************************
  **
  ** Get the FabricLocation
  */

  public FabricLocation getFabricLocation() {
    return (floc_);
  }  
  
  /***************************************************************************
  **
  ** Get the FMT
  */

  public FabricMagnifyingTool getFMT() {
    return (fmt_);
  }
  
  /***************************************************************************
  **
  ** Get the Overview
  */

  public BioFabricOverview getOverview() {
    return (bfo_);
  }
   
  /***************************************************************************
  **
  ** Set the fabric panel
  */

  public void setFabricPanel(BioFabricPanel cp) {
    cp_ = cp;
    fnt_.setFabricPanel(cp);
    return;
  }
}