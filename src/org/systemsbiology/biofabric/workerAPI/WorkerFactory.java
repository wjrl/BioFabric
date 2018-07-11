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

package org.systemsbiology.biofabric.workerAPI;

import javax.swing.JFrame;

import org.systemsbiology.biofabric.worker.BackgroundWorker;
import org.systemsbiology.biofabric.worker.BackgroundWorkerClient;
import org.systemsbiology.biofabric.worker.WorkerClientBundle;

/****************************************************************************
**
** Factory for returning API Implementations
*/

public class WorkerFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get a BFWorker
  */
  
  public static BFWorker getBFWorker(BackgroundWorkerOwner owner,
                                     JFrame topWindow, BackgroundWorkerControlManager suw, String waitTitle, 
                                     String waitMsg, boolean allowCancels) {
  	
  	BackgroundWorker bw = new BackgroundWorker();
    BackgroundWorkerClient bwc = new BackgroundWorkerClient(owner, bw, topWindow, suw, waitTitle, waitMsg, allowCancels);
  	return (new WorkerClientBundle(bw, bwc));
  }
  
}
