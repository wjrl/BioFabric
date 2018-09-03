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

package org.systemsbiology.biofabric.ui.dialogs;

import java.awt.Dimension;
import java.text.MessageFormat;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import org.systemsbiology.biofabric.api.dialog.BTStashResultsDialog;
import org.systemsbiology.biofabric.layouts.SetLayout;
import org.systemsbiology.biofabric.util.ResourceManager;

/****************************************************************************
**
** Dialog box for specifying if the set relation means "contains" or "is element of"
*/

public class SetRelationSemanticsDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private static final long serialVersionUID = 1L;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JRadioButton isElementOf_;
  private JRadioButton contains_;
  private boolean userWantsUp_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SetRelationSemanticsDialog(JFrame parent, String relation) {     
    super(parent, ResourceManager.getManager().getString("setRelDialog.title"), new Dimension(400, 200), 2);

    boolean pointUp = false;
    boolean pointDown = false;
    
    String compRel = relation.toLowerCase();
    if (compRel.indexOf("contain") != -1) {
    	pointDown = true;
    }
    if (compRel.indexOf("element") != -1) {
    	pointUp = true;
    } 
    if (compRel.indexOf("belong") != -1) {
      pointUp = true;
    }
    //
    // If both are false or both are true, we cannot make a choice:
    //
    if ((pointUp && pointDown) || (!pointUp && !pointDown)) {
    	userWantsUp_ = true;
    } else if (pointUp) {
    	userWantsUp_ = true;
    } else if (pointDown) {
    	userWantsUp_ = false;  	
    } else {
    	throw new IllegalStateException();
    }
    
    String questionMsg = MessageFormat.format(rMan_.getString("setRelDialog.question"), new Object[] {relation});
    
    addWidgetFullRow(new JLabel(questionMsg), true);
    isElementOf_ = new JRadioButton(rMan_.getString("setRelDialog.pointUp"), userWantsUp_);
    addWidgetFullRow(isElementOf_, false);
    contains_ = new JRadioButton(rMan_.getString("setRelDialog.meansContains"), !userWantsUp_);
    addWidgetFullRow(contains_, false);
          
    ButtonGroup group = new ButtonGroup();
    group.add(isElementOf_);
    group.add(contains_);
    
    finishConstruction();
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Get results
  */
  
  public SetLayout.LinkMeans getLinkMeaning() {
    return ((userWantsUp_) ? SetLayout.LinkMeans.BELONGS_TO : SetLayout.LinkMeans.CONTAINS);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Stash our results for later interrogation.
  ** 
  */
  
  protected boolean stashForOK() { 
    userWantsUp_ = isElementOf_.isSelected();
    return (true);
  }
}
