/*
**    Copyright (C) 2003-2008 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.util;

import java.util.ArrayList;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.systemsbiology.biotapestry.cmd.CompoundPreEventCmd;
import org.systemsbiology.biotapestry.cmd.CompoundPostEventCmd2;
import org.systemsbiology.biotapestry.cmd.BTUndoCmd;
import org.systemsbiology.biotapestry.event.ChangeEvent;

/****************************************************************************
**
** A Class
*/

public class UndoSupport {
  
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
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static int changeCount_ = 0;
  private ArrayList shadowEdits_;
  private CompoundEdit edit_;   
  private CompoundPreEventCmd pre_;
  private CompoundPostEventCmd2 post_;
  private ArrayList preList_;
  private ArrayList postList_;
  private UndoManager undo_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public UndoSupport(UndoManager undo, String presentation) {
    edit_ = new CompoundEdit();    
    pre_ = new CompoundPreEventCmd();
    post_ = new CompoundPostEventCmd2(ResourceManager.getManager().getString(presentation));
    preList_ = new ArrayList();
    postList_ = new ArrayList();
    shadowEdits_ = new ArrayList();
    edit_.addEdit(pre_);
    undo_ = undo;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Rollback the unfinished support session
  */
  
  public void rollback() {
    // FIX ME: Change count is not fixed?
    edit_.end();
    edit_.undo();
    return;
  }  
 
  /***************************************************************************
  **
  ** Add an edit
  */
  
  public void addEdit(BTUndoCmd edit) {
    if (edit.changesModel()) {
      changeCount_++;
    }
    edit_.addEdit(edit);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an edit
  */
  
  public void addEdits(BTUndoCmd[] edits) {
    for (int i = 0; i < edits.length; i++) {
      addEdit(edits[i]);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add an event to both lists
  */
  
  public void addEvent(ChangeEvent event) {
    if (!preList_.contains(event)) {
      preList_.add(event);
    }
    if (!postList_.contains(event)) {
      postList_.add(event);
    }
    return;
  }

  /***************************************************************************
  **
  ** Add an event to the pre list
  */
  
  public void addPreEvent(ChangeEvent event) {
    if (!preList_.contains(event)) {
      preList_.add(event);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add an event to the post list
  */
  
  public void addPostEvent(ChangeEvent event) {
    if (!postList_.contains(event)) {
      postList_.add(event);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Complete the operation
  */
  
  public void finish() {
    edit_.addEdit(post_);
    pre_.addChangeEvents(preList_);
    post_.addChangeEvents(postList_);
    post_.execute();        
    edit_.end();
    undo_.addEdit(edit_);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Clear the change tracking
  */
  
  public static void clearTracking() {
    changeCount_ = 0;  
    return;
  }  
 
  /***************************************************************************
  **
  ** Answer if a change has occurred sincce last clear.  FIX ME: We don't
  ** account for undone/redone changes.
  */
  
  public static boolean hasAChange() {
    return (changeCount_ > 0);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
