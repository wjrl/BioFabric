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

package org.systemsbiology.biofabric.layoutAPI;

import org.systemsbiology.biofabric.ioAPI.BuildData;
import org.systemsbiology.biofabric.workerAPI.AsynchExitRequestException;
import org.systemsbiology.biofabric.workerAPI.BTProgressMonitor;

/****************************************************************************
**
** This is the interface for edge layout algorithms
*/

public interface EdgeLayout {

	////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
   
  /***************************************************************************
  **
  ** Relayout the whole network!
  */
  
  public void layoutEdges(BuildData rbd, BTProgressMonitor monitor) throws AsynchExitRequestException;
  
  
  /***************************************************************************
  **
  ** Do necessary pre-processing steps (e.g. automatic assignment to link groups)
  */
  
  public void preProcessEdges(BuildData rbd, BTProgressMonitor monitor) throws AsynchExitRequestException;
   

}
