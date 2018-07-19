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

package org.systemsbiology.biofabric.plugin;

import org.systemsbiology.biofabric.layoutAPI.EdgeLayout;
import org.systemsbiology.biofabric.layoutAPI.NodeLayout;
import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.plugin.core.align.AlignCycleEdgeLayout;
import org.systemsbiology.biofabric.plugin.core.align.AlignCycleLayout;
import org.systemsbiology.biofabric.plugin.core.align.NetworkAlignmentEdgeLayout;
import org.systemsbiology.biofabric.plugin.core.align.NetworkAlignmentLayout;
import org.systemsbiology.biofabric.util.UiUtil;

/****************************************************************************
**
** Extra build data needed by plugins during network construction
*/

public interface PluginBuildData {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
	/****************************************************************************
  **
  ** If these methods return a value other than null, that will be used instead 
  ** the standard BuildData values:
  */
	
	public NodeLayout getNodeLayout();
  public EdgeLayout getEdgeLayout();
  
	/****************************************************************************
  **
  ** Called during network build to process any specialty build data: 
  */  
  
  public void processSpecialtyBuildData();

}
