/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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
import java.text.MessageFormat;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.table.TableCellEditor;

import org.systemsbiology.biofabric.api.util.ExceptionHandler;

import javax.swing.event.CellEditorListener;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import javax.swing.event.ChangeEvent;

/***************************************************************************
**
** Class for editing combo box table cells with double values
*/
  
public class DoubleEditor extends JTextField implements TableCellEditor, NumberEditor {
  
  private JFrame topWindow_;
  private ArrayList listeners_;
  private ProtoDouble origValue_;
  private int currRow_;
  private int currColumn_;
  boolean neverEdited_;
  private boolean blanksOK_;
  
  public DoubleEditor(JFrame topWindow) {
    this(topWindow, false);
  }
  
  public DoubleEditor(JFrame topWindow, boolean blanksOK) {
    listeners_ = new ArrayList();
    currRow_ = -1;
    currColumn_ = -1;       
    topWindow_ = topWindow;
    neverEdited_ = true;
    blanksOK_ = blanksOK;
  }  
  
  public static void triggerWarning(JFrame top) {
    ResourceManager rMan = ResourceManager.getManager();
    JOptionPane.showMessageDialog(top, 
                                  rMan.getString("doubleEditor.notADouble"), 
                                  rMan.getString("doubleEditor.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);    
  }
  
  public static void triggerWarningWithTag(JFrame top, String tag, String val) {
    ResourceManager rMan = ResourceManager.getManager();
    String format = rMan.getString("doubleEditor.taggedNotADouble");
    String desc = MessageFormat.format(format, new Object[] {tag, val}); 
    JOptionPane.showMessageDialog(top,
                                  desc,
                                  rMan.getString("doubleEditor.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);    
  }
  
  public Component getTableCellEditorComponent(JTable table, Object value, 
                                               boolean isSelected,
                                               int row, int column) {
    try {
      currRow_ = row;
      currColumn_ = column;

      if (value == null) {
        setText(null);
      } else { 
        this.setText(((ProtoDouble)value).textValue);
      }
      table.setRowSelectionInterval(row, row);      
      table.setColumnSelectionInterval(column, column);
      origValue_ = (ProtoDouble)value;
      neverEdited_ = false;
    } catch (Exception ex) {
      ExceptionHandler.getHandler().displayException(ex);
    }
    return (this);             
  }
  
  public int getCurrRow() {
    return (currRow_);
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
      retval = new ProtoDouble(getText());
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
    setText((origValue_ == null) ? null : origValue_.textValue);
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
    if (!neverEdited_) {  // Added because we can stop editing on a field never edited
      String text = getText();
      if (!(blanksOK_ && (text != null) && (text.trim().equals("")))) {
        ProtoDouble test = new ProtoDouble(getText());
        if (!test.valid) {
          setText((origValue_ == null) ? null : origValue_.textValue);
          triggerWarning(topWindow_);
        }
      }
    }
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
  