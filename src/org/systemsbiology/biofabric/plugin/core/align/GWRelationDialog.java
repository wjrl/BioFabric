/*
 **    File created by Rishi Desai
 **
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

package org.systemsbiology.biofabric.plugin.core.align;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.systemsbiology.biofabric.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biofabric.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FixedJButton;

public class GWRelationDialog extends BTStashResultsDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE FIELDS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private JFrame parent_;
  private FixedJButton buttonOK_;
  private JTextField textField_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public GWRelationDialog(JFrame parent, final String defaultRelation) {
    super(parent, "fileRead.relationDialog", new Dimension(400, 200), 2);
    this.parent_ = parent;
    
    addWidgetFullRow(new JLabel("fileRead.relationMessage"), false, true);
    
    textField_ = new JTextField(defaultRelation);
    textField_.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) {
        try {
          manageOKButton();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
  
      public void removeUpdate(DocumentEvent e) {
        try {
          manageOKButton();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
  
      public void changedUpdate(DocumentEvent e) {
        try {
          manageOKButton();
        } catch (Exception ex) {
          ExceptionHandler.getHandler().displayException(ex);
        }
      }
    });
  
    JLabel label = new JLabel("Rel");
    addLabeledWidget(label, textField_, false, false);
  
    //
    // OK button
    //
  
    DialogSupport.Buttons buttons = finishConstruction();
  
    buttonOK_ = buttons.okButton;
    buttonOK_.setEnabled(true);
  
    setLocationRelativeTo(parent);
  }
  
  public String getRelation() {
    return (textField_.getText().trim());
  }
  
  /**
   * Check whether OK button should be activated or deactivated
   */
  
  private void manageOKButton () {
    if (hasMinRequirements()) {
      buttonOK_.setEnabled(true);
    } else {
      buttonOK_.setEnabled(false);
    }
    return;
  }
  
  /**
   * Check whether dialog has textfield with text
   */
  
  private boolean hasMinRequirements() {
    String text = textField_.getText().trim();
    return (!text.isEmpty() && text.split(" ").length == 1);
  }
  
  @Override
  public void okAction() {
    try {
      super.okAction();
    } catch (Exception ex) {
      // should never happen because OK button won't activate without min requirements
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
  
  @Override
  public void closeAction() {
    try {
      super.closeAction();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
  }
  
  @Override
  protected boolean stashForOK() {
    return (hasMinRequirements());
  }
  
}
