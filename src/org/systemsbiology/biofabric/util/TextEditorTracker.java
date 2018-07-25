/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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

import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.table.AbstractTableModel;

import org.systemsbiology.biofabric.utilAPI.ExceptionHandler;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

/****************************************************************************
**
** Helper to track text editor
*/

public class TextEditorTracker implements CaretListener, CellEditorListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private TextEditor textEdit_;
  private AbstractTableModel table_;
  private TextFinishedTracker finished_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TextEditorTracker(TextEditor textEdit, AbstractTableModel table) { 
    this(textEdit, table, null);
  }
  
  public TextEditorTracker(TextEditor textEdit, AbstractTableModel table, TextFinishedTracker track) { 
    textEdit_ = textEdit;
    table_ = table;
    finished_ = track;

    textEdit_.addCaretListener(this);
    textEdit_.addCellEditorListener(this);    
  }  

  public void caretUpdate(CaretEvent e) {
    try {
      if (textEdit_.entryIsValid()) {
        int cc = textEdit_.getCurrColumn();          
        int cr = textEdit_.getCurrRow();
        table_.setValueAt(textEdit_.getText(), cr, cc);
      }
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }      
    
  public void editingCanceled(ChangeEvent e) {
    try {
      if (textEdit_.entryIsValid()) {
        int cc = textEdit_.getCurrColumn();          
        int cr = textEdit_.getCurrRow();
        table_.setValueAt(textEdit_.getText(), cr, cc);        
      }
      return;
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
  }
    
  public void editingStopped(ChangeEvent e) {
    try {
      if (finished_ != null) {
        if (textEdit_.entryIsValid()) {
          int cc = textEdit_.getCurrColumn();          
          int cr = textEdit_.getCurrRow();
          if (!finished_.textEditingDone(cc, cr, textEdit_.getText())) {
            textEdit_.resetValue();
          }
        }
      }
      return;
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
  }

}

