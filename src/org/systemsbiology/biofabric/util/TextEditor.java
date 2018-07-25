/*
**    Copyright (C) 2003-2007 Institute for Systems Biology 
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

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import org.systemsbiology.biofabric.utilAPI.ExceptionHandler;

import javax.swing.event.CellEditorListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import javax.swing.event.ChangeEvent;

/***************************************************************************
**
** Class for editing combo box table cells
*/
  
public class TextEditor extends JTextField implements TableCellEditor {
   
  private ArrayList listeners_;
  private Object origValue_;
  private int currRow_;
  private int currColumn_;
   
  public TextEditor() {
    super();
    listeners_ = new ArrayList();
    currRow_ = -1;
    currColumn_ = -1;      
  }

  public Component getTableCellEditorComponent(JTable table, Object value, 
                                               boolean isSelected,
                                               int row, int column) {
    try {
      currRow_ = row;
      currColumn_ = column;

      this.setText((String)value);
      table.setRowSelectionInterval(row, row);      
      table.setColumnSelectionInterval(column, column);
      origValue_ = value;
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }

    return (this);             
  }
   
  public int getCurrRow() {
    return (currRow_);
  }
  
  public Object getOrigValue() {
    return (origValue_);
  } 
  
  public void resetValue() {
    setText((String)origValue_);
    return;
  }   
  
  public int getCurrColumn() {
    return (currColumn_);
  }
  
  public boolean entryIsValid() {  
    return ((currRow_ != -1) && (currColumn_ != -1));
  }
  
  public void addCellEditorListener(CellEditorListener l) {
    try {
      this.listeners_.add(l);
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
    
  public void cancelCellEditing() {
    try {
      editingCanceled();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
    
  public Object getCellEditorValue() {
    Object retval = null;
    try {
      retval = getText();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return (retval);
  }

  public boolean isCellEditable(EventObject anEvent) {
    return (true);
  }
    
  public void removeCellEditorListener(CellEditorListener l) {
    try {
      listeners_.remove(l);
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return;
  }
    
  public boolean shouldSelectCell(EventObject anEvent) {
    return (true);
  }

  public boolean stopCellEditing() {
    try {
      editingStopped();
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return (true);
  }

  private void editingCanceled() {
    // Want row, col to be up to date when text is set
    setText((String)origValue_);
    ChangeEvent cev = new ChangeEvent(this);
    ArrayList lCopy = new ArrayList(listeners_);
    Iterator lit = lCopy.iterator();
    while (lit.hasNext()) {
      CellEditorListener cel = (CellEditorListener)lit.next();
      cel.editingCanceled(cev);
    }
    currRow_ = -1;
    currColumn_ = -1;    
    return;
  }
    
  private void editingStopped() {
    ChangeEvent cev = new ChangeEvent(this);
    ArrayList lCopy = new ArrayList(listeners_);      
    Iterator lit = lCopy.iterator();
    while (lit.hasNext()) {
      CellEditorListener cel = (CellEditorListener)lit.next();
      cel.editingStopped(cev);
    }
    currRow_ = -1;
    currColumn_ = -1;    
    return;
  }    
}
  
