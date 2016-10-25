/*
**    Copyright (C) 2003-2014 Institute for Systems Biology
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

package org.systemsbiology.biofabric.ui.dialogs;

import org.systemsbiology.biofabric.analysis.NetworkAlignment;

import javax.swing.*;

public class NetworkAlignmentDialog extends JDialog {
  
  public NetworkAlignmentDialog(JFrame parent) {
    super(parent, "Network Alignment", true);
    setSize(400, 450);
    
    JPanel jp = new JPanel();
    
    JLabel graph_1 = new JLabel("Graph 1:");
    JLabel graph_2 = new JLabel("Graph 2:");
    JLabel alignment = new JLabel("Alignment:");
    
    
    
    setVisible(true);
    
  }
  
}
