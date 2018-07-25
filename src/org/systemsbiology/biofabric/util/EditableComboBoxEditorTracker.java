/*
**    Copyright (C) 2003-2004 Institute for Systems Biology 
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


package org.systemsbiology.biofabric.util;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biofabric.api.util.ExceptionHandler;

/****************************************************************************
**
** Helper to track combo box editor
*/

public class EditableComboBoxEditorTracker implements ItemListener, CaretListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private EditableComboBoxEditor comboEdit_;
  private AbstractTableModel table_;
  private int ignoreCount_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

   /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public EditableComboBoxEditorTracker(EditableComboBoxEditor comboEdit,
                                       AbstractTableModel table) { 
    comboEdit_ = comboEdit;
    table_ = table;

    comboEdit.addItemListener(this);
    JTextComponent jtc = (JTextComponent)comboEdit.getEditor().getEditorComponent();
    jtc.addCaretListener(this);
       
  }
  
  public void itemStateChanged(ItemEvent e) {
    try {
      if (comboEdit_.entryIsValid()) {
        int cc = comboEdit_.getCurrColumn();
        int cr = comboEdit_.getCurrRow();
        String si = (String)comboEdit_.getSelectedItem();
        table_.setValueAt(comboEdit_.displayStringToValue(si), cr, cc);
      }
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }

  public void caretUpdate(CaretEvent e) {
    try {
      if (comboEdit_.entryIsValid()) {
        int cc = comboEdit_.getCurrColumn();
        int cr = comboEdit_.getCurrRow();
        String si = (String)comboEdit_.getCurrValue();
        table_.setValueAt(comboEdit_.displayStringToValue(si), cr, cc);
      }
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
  
}
