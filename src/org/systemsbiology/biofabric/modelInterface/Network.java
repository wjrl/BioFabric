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

package org.systemsbiology.biofabric.modelInterface;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biofabric.layouts.DefaultEdgeLayout;
import org.systemsbiology.biofabric.layouts.EdgeLayout;
import org.systemsbiology.biofabric.layouts.NodeLayout;
import org.systemsbiology.biofabric.modelInterface.NetLink;
import org.systemsbiology.biofabric.analysis.Link;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.io.BuildData;
import org.systemsbiology.biofabric.io.FabricFactory;

import org.systemsbiology.biofabric.parser.AbstractFactoryClient;
import org.systemsbiology.biofabric.parser.GlueStick;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugIn;
import org.systemsbiology.biofabric.plugin.BioFabricToolPlugInData;
import org.systemsbiology.biofabric.plugin.PlugInManager;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.FabricDisplayOptions;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.AttributeExtractor;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.CharacterEntityMapper;
import org.systemsbiology.biofabric.util.DataUtil;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.LoopReporter;
import org.systemsbiology.biofabric.util.MinMax;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;

/****************************************************************************
**
** This is the Network model.
*/

public interface Network {
  
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
  
  public enum LayoutMode {
    UNINITIALIZED_MODE("notSet"),
    PER_NODE_MODE("perNode"),
    PER_NETWORK_MODE("perNetwork");

    private String text;

    LayoutMode(String text) {
      this.text = text;
    }

    public String getText() {
      return (text);
    }

	  public static LayoutMode fromString(String text)  throws IOException {
	    if (text != null) {
	      for (LayoutMode lm : LayoutMode.values()) {
	        if (text.equalsIgnoreCase(lm.text)) {
	          return (lm);
	        }
	      }
	    }
	    throw new IOException();
	  }
  }
                    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

}
