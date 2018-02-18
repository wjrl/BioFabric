/*
**    File created by Rishi Desai
**
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

package org.systemsbiology.biofabric.ui.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.systemsbiology.biofabric.analysis.NetworkAlignmentScorer;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;

public class NetAlignScoreDialog extends JDialog {
  
  private JFrame parent_;
  private NetworkAlignmentScorer.NetAlignStats netAlignStats_;
  
  public NetAlignScoreDialog(JFrame parent, NetworkAlignmentScorer.NetAlignStats stats) {
    super(parent, ResourceManager.getManager().getString("networkAlignment.scores"), true);
    this.parent_ = parent;
    this.netAlignStats_ = stats;
  
    final ResourceManager rMan = ResourceManager.getManager();
    setSize(300, 300);
    JPanel cp = (JPanel) getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
  
    JLabel[] labels = {
            new JLabel("EC: "         + stats.EC),
            new JLabel("S3: "         + stats.S3),
            new JLabel("ICS: "        + stats.ICS),
            new JLabel("NC: "         + stats.NC),
            new JLabel("NGDist: "     + stats.NGDist),
            new JLabel("LGDist: "     + stats.LGDist),
            new JLabel("NGLGDist: "   + stats.NGLGDist),
            new JLabel("JaccardSim: " + stats.JaccardSim)
    };
    
    Box scoreBox = Box.createVerticalBox();
    for (JLabel label : labels) {
      scoreBox.add(label);
    }
    
    cp.add(scoreBox, gbc);
    
    FixedJButton buttonO = new FixedJButton(rMan.getString("networkAlignment.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        NetAlignScoreDialog.this.setVisible(false);
        NetAlignScoreDialog.this.dispose();
      }
    });
  
    JPanel panOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
  
    panOptions.add(buttonO);
    panOptions.add(Box.createHorizontalStrut(10));
  
    Box optButtonBox = Box.createHorizontalBox();
    optButtonBox.add(panOptions);
  
    UiUtil.gbcSet(gbc, 0, 1, 5, 1, UiUtil.HOR, 0, 0,
            5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(optButtonBox, gbc);
  
    setLocationRelativeTo(parent);
  }
  
}
