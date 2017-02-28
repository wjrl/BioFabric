/*
**    Copyright (C) 2003-2012 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.biofabric;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.HashMap;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;
import javax.swing.ImageIcon;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biotapestry.cmd.ZoomChangeTracker;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeListener;
import org.systemsbiology.biotapestry.parser.SUParser;
import org.systemsbiology.biotapestry.ui.ImageExporter;
import org.systemsbiology.biotapestry.ui.dialogs.ExportSettingsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.ExportSettingsPublishDialog;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;
import org.systemsbiology.biotapestry.util.BackgroundWorker;
import org.systemsbiology.biotapestry.util.BackgroundWorkerClient;
import org.systemsbiology.biotapestry.util.BackgroundWorkerOwner;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.ExceptionHandler;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.InvalidInputException;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FileExtensionFilters;

/****************************************************************************
**
** Collection of primary commands for the application
*/

public class FabricCommands implements ZoomChangeTracker, SelectionChangeListener, FabricDisplayOptionsManager.DisplayOptionTracker {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** For standard file checks
  */

  public static final boolean FILE_MUST_EXIST_DONT_CARE = false;
  public static final boolean FILE_MUST_EXIST           = true;
  
  public static final boolean FILE_CAN_CREATE_DONT_CARE = false;
  public static final boolean FILE_CAN_CREATE           = true;
  
  public static final boolean FILE_DONT_CHECK_OVERWRITE = false;
  public static final boolean FILE_CHECK_OVERWRITE      = true;
  
  public static final boolean FILE_MUST_BE_FILE         = false;
  public static final boolean FILE_MUST_BE_DIRECTORY    = true;  
          
  public static final boolean FILE_CAN_WRITE_DONT_CARE  = false;
  public static final boolean FILE_CAN_WRITE            = true;
  
  public static final boolean FILE_CAN_READ_DONT_CARE   = false;
  public static final boolean FILE_CAN_READ             = true;  

  public static final int EMPTY_NETWORK     = 0; 
  public static final int CLOSE             = 1; 
  public static final int LOAD              = 2; 
  public static final int SEARCH            = 3; 
  public static final int ZOOM_OUT          = 4; 
  public static final int ZOOM_IN           = 5; 
  public static final int CLEAR_SELECTIONS  = 6; 
  public static final int SAVE_AS           = 7; 

  public static final int ZOOM_TO_MODEL     = 8; 
  public static final int ZOOM_TO_SELECTIONS = 9;
  public static final int PROPAGATE_DOWN    = 10; 
  public static final int ZOOM_TO_RECT      = 11;
  public static final int CANCEL            = 12;
  public static final int ZOOM_TO_CURRENT_SELECTION = 13;  
  public static final int ADD_FIRST_NEIGHBORS = 14;
  public static final int BUILD_SELECT        = 15;
  public static final int SET_DISPLAY_OPTIONS = 16;
  
  public static final int GAGGLE_GOOSE_UPDATE    = 17;
  public static final int GAGGLE_RAISE_GOOSE     = 18;
  public static final int GAGGLE_LOWER_GOOSE     = 19;
  public static final int GAGGLE_SEND_NETWORK    = 20;
  public static final int GAGGLE_SEND_NAMELIST   = 21;
  public static final int GAGGLE_PROCESS_INBOUND = 22; 
  public static final int GAGGLE_CONNECT         = 23; 
  public static final int GAGGLE_DISCONNECT      = 24;
  public static final int ABOUT                  = 25;
  public static final int CENTER_ON_NEXT_SELECTION = 26;  
  public static final int CENTER_ON_PREVIOUS_SELECTION = 27;  
  public static final int LAYOUT_NODES_VIA_ATTRIBUTES  = 28;
  public static final int LAYOUT_LINKS_VIA_ATTRIBUTES  = 29;
  public static final int LOAD_WITH_NODE_ATTRIBUTES = 30;
  public static final int LOAD_XML                  = 31; 
  public static final int RELAYOUT_USING_CONNECTIVITY  = 32;
  public static final int RELAYOUT_USING_SHAPE_MATCH   = 33;
  public static final int SET_LINK_GROUPS              = 34;
  public static final int COMPARE_NODES                = 35;
  public static final int ZOOM_TO_CURRENT_MOUSE        = 36;  
  public static final int ZOOM_TO_CURRENT_MAGNIFY      = 37; 
  public static final int EXPORT_NODE_ORDER            = 38; 
  public static final int EXPORT_LINK_ORDER            = 39; 
  public static final int EXPORT_IMAGE                 = 40; 
  public static final int EXPORT_IMAGE_PUBLISH         = 41; 
  public static final int PRINT                        = 42;
  public static final int DEFAULT_LAYOUT               = 43;
  public static final int EXPORT_SELECTED_NODES        = 44;
  public static final int SAVE                         = 45; 
  
  public static final int GENERAL_PUSH   = 0x01;
  public static final int ALLOW_NAV_PUSH = 0x02;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static HashMap perClass_;
  private BioFabricWindow topWindow_;
  private BioFabricApplication bfa_;
  private BioFabricPanel bfp_;
  private File currentFile_;
  private JButton gaggleButton_;
  private JButton gaggleUpdateGooseButton_;
  private Color gaggleButtonOffColor_;
  private boolean isAMac_;
  private JMenu gaggleGooseChooseMenu_;
  private JComboBox gaggleGooseCombo_;
  private boolean managingGaggleControls_;
  private boolean isForMain_;
  

  private HashMap withIcons_;
  private HashMap noIcons_;
  private FabricColorGenerator colGen_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
   
  /***************************************************************************
  **
  ** Let us know we are on a mac
  */ 
  
  public boolean isAMac() {
    return (isAMac_);
  }
  
  /***************************************************************************
  **
  ** Notify listener of selection change
  */ 
  
  public void selectionHasChanged(SelectionChangeEvent scev) {
    checkForChanges();
    return;
  }
   
  /***************************************************************************
  **
  ** Trigger the enabled checks
  */ 
  
  public void checkForChanges() {
    Iterator wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)wiit.next();
      cfe.checkIfEnabled();
    }
    Iterator niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)niit.next();
      cfe.checkIfEnabled();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
  */ 
  
  public void pushDisabled(int pushCondition) {
    Iterator wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)wiit.next();
      cfe.pushDisabled(pushCondition);
    }
    Iterator niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)niit.next();
      cfe.pushDisabled(pushCondition);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
   public void popDisabled() {
    Iterator wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)wiit.next();
      cfe.popDisabled();
    }
    Iterator niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = (ChecksForEnabled)niit.next();
      cfe.popDisabled();
    }

    return;
  } 

  /***************************************************************************
  **
  ** Display options have changed!
  */   
  
  public void optionsHaveChanged(boolean needRebuild, boolean needRecolor) {
    if (!needRebuild && !needRecolor) {
      bfp_.repaint();
      return;
    }
    if (needRecolor && !needRebuild) {
      NetworkRecolor nb = new NetworkRecolor(); 
      nb.doNetworkRecolor(isForMain_);
    } else if (needRebuild) {
      BioFabricNetwork bfn = bfp_.getNetwork();
      if (bfn != null) {
        BioFabricNetwork.PreBuiltBuildData rbd = 
          new BioFabricNetwork.PreBuiltBuildData(bfn, BioFabricNetwork.SHADOW_LINK_CHANGE);
        NetworkBuilder nb = new NetworkBuilder(); 
        nb.doNetworkBuild(rbd, true);
      }
    }
    return;   
  }
    
  /***************************************************************************
  **
  ** Get ColorGenerator
  */ 
  
  public FabricColorGenerator getColorGenerator() {
    return (colGen_);
  }
  
  /***************************************************************************
  **
  ** Hold gaggle button data
  */ 
  
  public void setGaggleButtons(JButton button, JButton gooseButton, Color defaultColor) {
    gaggleButton_ = button;
    gaggleButtonOffColor_ = defaultColor;
    gaggleUpdateGooseButton_ = gooseButton;
    return;
  }  
  
  /***************************************************************************
  **
  ** Call to let us know gaggle buttons need work.
  */    
  
  public void triggerGaggleState(int whichAction, boolean activate) {
    if (whichAction == GAGGLE_PROCESS_INBOUND) {
      GaggleProcessInbound gpi = (GaggleProcessInbound)withIcons_.get(new Integer(GAGGLE_PROCESS_INBOUND));
      gpi.setButtonCondition(activate);
    } else if (whichAction == GAGGLE_GOOSE_UPDATE) {
      GaggleUpdateGeese gug = (GaggleUpdateGeese)withIcons_.get(new Integer(GAGGLE_GOOSE_UPDATE));
      gug.setButtonCondition(activate);
    } else {
      throw new IllegalArgumentException();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Update the controls for gaggle 
  */ 
    
  public void updateGaggleTargetActions() {
    FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
    if ((goose != null) && goose.isActivated()) {
      managingGaggleControls_ = true;
      SelectionSupport ss = goose.getSelectionSupport();
      List targets = ss.getGooseList();
      int numTarg = targets.size();
      
      if (gaggleGooseChooseMenu_ != null) {
        gaggleGooseChooseMenu_.removeAll();
        SetCurrentGaggleTargetAction scupa = new SetCurrentGaggleTargetAction(FabricGooseInterface.BOSS_NAME, 0); 
        JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
        gaggleGooseChooseMenu_.add(jcb);   
      }

      if (gaggleGooseCombo_ != null) {
        gaggleGooseCombo_.removeAllItems();
        gaggleGooseCombo_.addItem(new ObjChoiceContent(FabricGooseInterface.BOSS_NAME, FabricGooseInterface.BOSS_NAME)); 
      }

      for (int i = 0; i < numTarg; i++) {
        String gooseName = (String)targets.get(i);
        ObjChoiceContent occ = new ObjChoiceContent(gooseName, gooseName);
        if (gaggleGooseChooseMenu_ != null) {
          SetCurrentGaggleTargetAction scupa = new SetCurrentGaggleTargetAction(occ.val, i + 1); 
          JCheckBoxMenuItem jcb = new JCheckBoxMenuItem(scupa);
          gaggleGooseChooseMenu_.add(jcb);
        }
        if (gaggleGooseCombo_ != null) {
          gaggleGooseCombo_.addItem(occ);
        }      
      }
      
      if (gaggleGooseChooseMenu_ != null) {        
        JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)gaggleGooseChooseMenu_.getItem(0);
        jcbmi.setSelected(true);
      }

      if (gaggleGooseCombo_ != null) {
        gaggleGooseCombo_.setSelectedIndex(0); 
        gaggleGooseCombo_.invalidate();
        gaggleGooseCombo_.validate(); 
      }
      
      managingGaggleControls_ = false;
    }
    return;
  }  

  /***************************************************************************
  **
  ** Update the controls for user paths
  */ 
    
  public void setCurrentGaggleTarget(int index) {
    managingGaggleControls_ = true;
    
    if (gaggleGooseChooseMenu_ != null) {
      int numUpm = gaggleGooseChooseMenu_.getItemCount();
      for (int i = 0; i < numUpm; i++) {
        JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)gaggleGooseChooseMenu_.getItem(i);
        jcbmi.setSelected(i == index);
      }
    }
    if (gaggleGooseCombo_ != null) {
      gaggleGooseCombo_.setSelectedIndex(index); 
      gaggleGooseCombo_.invalidate();
      gaggleGooseCombo_.validate(); 
    }
       
    managingGaggleControls_ = false;
    return;    
  }   
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  public NetworkBuilder getANetworkBuilder() {
    return (new NetworkBuilder());
  }
  
  /***************************************************************************
  **
  ** Get an action
  */ 
  
  public Action getAction(int actionKey, boolean withIcon, Object[] optionArgs) {
    HashMap useMap = (withIcon) ? withIcons_ : noIcons_;
    Integer actionKeyObject = new Integer(actionKey);
    Action retval = (Action)useMap.get(actionKeyObject);
    if (retval != null) {
      return (retval);
    } else {
      switch (actionKey) { 
        case ABOUT:
          retval = new AboutAction(withIcon); 
          break;
        case EMPTY_NETWORK:
          retval = new EmptyNetworkAction(withIcon); 
          break;
        case CLOSE:
          retval = new CloseAction(withIcon); 
          break;
        case LOAD_XML:
          retval = new LoadXMLAction(withIcon); 
          break;
        case LOAD:
          retval = new ImportSIFAction(withIcon); 
          break;
        case LOAD_WITH_NODE_ATTRIBUTES:
          retval = new LoadWithNodeAttributesAction(withIcon); 
          break;  
        case SAVE_AS:
          retval = new SaveAsAction(withIcon); 
          break;
        case SAVE:
          retval = new SaveAction(withIcon); 
          break;
        case EXPORT_NODE_ORDER:
          retval = new ExportNodeOrderAction(withIcon); 
          break;  
        case EXPORT_LINK_ORDER:
          retval = new ExportLinkOrderAction(withIcon); 
          break;  
        case EXPORT_SELECTED_NODES:
          retval = new ExportSelectedNodesAction(withIcon); 
          break;  
        case EXPORT_IMAGE:
          retval = new ExportSimpleAction(withIcon); 
          break;  
        case EXPORT_IMAGE_PUBLISH:
          retval = new ExportPublishAction(withIcon); 
          break;    
        case PRINT:
          retval = new PrintAction(withIcon); 
          break;      
        case SEARCH:
          retval = new SearchAction(withIcon); 
          break;
        case ZOOM_IN:
          retval = new InOutZoomAction(withIcon, '+'); 
          break;
        case ZOOM_OUT:
          retval = new InOutZoomAction(withIcon, '-'); 
          break;
        case CLEAR_SELECTIONS:
          retval = new ClearSelectionsAction(withIcon); 
          break;
        case ZOOM_TO_MODEL:
          retval = new ZoomToModelAction(withIcon); 
          break;
        case ZOOM_TO_SELECTIONS:
          retval = new ZoomToSelected(withIcon); 
          break;  
        case PROPAGATE_DOWN:
          retval = new PropagateDownAction(withIcon); 
          break; 
        case ZOOM_TO_RECT:
          retval = new ZoomToRect(withIcon); 
          break;
        case CANCEL:
          retval = new CancelAction(withIcon); 
          break;
        case CENTER_ON_NEXT_SELECTION:
          retval = new CenterOnNextSelected(withIcon); 
          break; 
        case CENTER_ON_PREVIOUS_SELECTION:
          retval = new CenterOnPreviousSelected(withIcon); 
          break; 
        case ZOOM_TO_CURRENT_SELECTION:
          retval = new ZoomToCurrentSelected(withIcon); 
          break;
        case ZOOM_TO_CURRENT_MOUSE:
          retval = new ZoomToCurrentMouse(withIcon); 
          break;
        case ZOOM_TO_CURRENT_MAGNIFY:
          retval = new ZoomToCurrentMagnify(withIcon); 
          break;  
        case ADD_FIRST_NEIGHBORS:
          retval = new AddFirstNeighborsAction(withIcon); 
          break;          
        case BUILD_SELECT:
          retval = new BuildSelectAction(withIcon); 
          break;  
        case SET_DISPLAY_OPTIONS:
          retval = new SetDisplayOptionsAction(withIcon); 
          break;
        case GAGGLE_GOOSE_UPDATE:
          retval = new GaggleUpdateGeese(withIcon); 
          break;
        case GAGGLE_RAISE_GOOSE:
          retval = new GaggleRaiseGoose(withIcon); 
          break;            
        case GAGGLE_LOWER_GOOSE:
          retval = new GaggleLowerGoose(withIcon); 
          break;              
        case GAGGLE_SEND_NETWORK:
          retval = new GaggleSendNetwork(withIcon); 
          break;  
        case GAGGLE_SEND_NAMELIST:
          retval = new GaggleSendNameList(withIcon); 
          break;              
        case GAGGLE_PROCESS_INBOUND:
          retval = new GaggleProcessInbound(withIcon); 
          break;  
        case GAGGLE_CONNECT:
          retval = new GaggleConnect(withIcon, true); 
          break;      
        case GAGGLE_DISCONNECT:
          retval = new GaggleConnect(withIcon, false); 
          break; 
        case LAYOUT_NODES_VIA_ATTRIBUTES:
          retval = new LayoutNodesViaAttributesAction(withIcon); 
          break;
        case LAYOUT_LINKS_VIA_ATTRIBUTES:
          retval = new LayoutLinksViaAttributesAction(withIcon); 
          break;
        case RELAYOUT_USING_CONNECTIVITY:
          retval = new LayoutViaConnectivityAction(withIcon); 
          break;
        case DEFAULT_LAYOUT:
          retval = new DefaultLayoutAction(withIcon); 
          break;
        case RELAYOUT_USING_SHAPE_MATCH:
          retval = new LayoutViaShapeMatchAction(withIcon); 
          break;  
        case SET_LINK_GROUPS:
          retval = new SetLinkGroupsAction(withIcon);
          break;  
        case COMPARE_NODES:
          retval = new CompareNodesAction(withIcon);           
          break;          
        default:
          throw new IllegalArgumentException();
      }
      useMap.put(actionKeyObject, retval);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Common load operations.  Take your pick of input sources
  */ 
    
  private boolean loadFromSifSource(File file, Map nameMap) {  
    ArrayList links = new ArrayList();
    HashSet loneNodes = new HashSet();
    FabricSIFLoader.SIFStats sss;
    if (file.length() > 500000) {
      sss = new FabricSIFLoader.SIFStats();
      BackgroundFileReader br = new BackgroundFileReader();
      br.doBackgroundSIFRead(file, links, loneNodes, nameMap, sss);
      return (true);
    } else {
      try { 
        sss = (new FabricSIFLoader()).readSIF(file, links, loneNodes, nameMap); 
        return (finishLoadFromSIFSource(file, sss, links, loneNodes));
      } catch (IOException ioe) {
        displayFileInputError(ioe);
        return (false);              
      } catch (OutOfMemoryError oom) {
        ExceptionHandler.getHandler().displayOutOfMemory(oom);
        return (false);  
      }
    }   
  }
   
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  private boolean finishLoadFromSIFSource(File file, FabricSIFLoader.SIFStats sss, List links, Set loneNodes) {
    ResourceManager rMan = ResourceManager.getManager();
    try {
      if (!sss.badLines.isEmpty()) {        
        String badLineFormat = rMan.getString("fabricRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {new Integer(sss.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("fabricRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      } 
      SortedMap relaMap = BioFabricNetwork.extractRelations(links);  
      RelationDirectionDialog rdd = new RelationDirectionDialog(topWindow_, relaMap);
      rdd.show();
      if (!rdd.haveResult()) {
        return (false);
      }
      if (rdd.getFromFile()) {
        File fileEda = getTheFile(".rda", ".txt", "AttribDirectory", "filterName.rda");
        if (fileEda == null) {
          return (true);
        }
        Map relAttributes = loadTheFile(fileEda, true);  // Use the simple a = b format of node attributes
        if (relAttributes == null) {
          return (true);
        }
        
        HashSet needed = new HashSet(relaMap.keySet());
      
        boolean tooMany = false;
        Iterator rit = relAttributes.keySet().iterator();
        while (rit.hasNext()) {
          String key = (String)rit.next();
          String val = (String)relAttributes.get(key);
          Boolean dirVal = Boolean.valueOf(val);
          FabricLink.AugRelation forNorm = new FabricLink.AugRelation(key, false);
          FabricLink.AugRelation forShad = new FabricLink.AugRelation(key, true);
          boolean matched = false;
          if (needed.contains(forNorm)) {
            matched = true;
            relaMap.put(forNorm, dirVal);
            needed.remove(forNorm);
          }
          if (needed.contains(forShad)) {
            matched = true;
            relaMap.put(forShad, dirVal);
            needed.remove(forShad);
          }
          if (!matched) {
            tooMany = true;
            break;
          }          
        }
        if (!needed.isEmpty() || tooMany) {
          JOptionPane.showMessageDialog(topWindow_, rMan.getString("fabricRead.directionMapLoadFailure"),
                                        rMan.getString("fabricRead.directionMapLoadFailureTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      } else {
        relaMap = rdd.getRelationMap();
      }
      
      BioFabricNetwork.assignDirections(links, relaMap);
      HashSet reducedLinks = new HashSet();
      HashSet culledLinks = new HashSet();
      BioFabricNetwork.preprocessLinks(links, reducedLinks, culledLinks);
      if (!culledLinks.isEmpty()) {
        String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
        // Ignore shadow link culls: / 2
        String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[] {new Integer(culledLinks.size() / 2)});
        JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                                      rMan.getString("fabricRead.dupLinkTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      
      BioFabricNetwork.OrigBuildData bfn = new BioFabricNetwork.OrigBuildData(reducedLinks, loneNodes, colGen_, BioFabricNetwork.BUILD_FROM_SIF);
      NetworkBuilder nb = new NetworkBuilder(); 
      nb.doNetworkBuild(bfn, true);            
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);  
    }
    currentFile_ = null;
    setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    manageWindowTitle(file.getName());
    return (true);
  }  
   
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  private boolean loadXMLFromSource(File file) {  
    ArrayList alist = new ArrayList();
    FabricFactory ff = new FabricFactory();
    alist.add(ff);
    SUParser sup = new SUParser(alist);   
    if (file.length() > 1000000) {
      BackgroundFileReader br = new BackgroundFileReader(); 
      br.doBackgroundRead(ff, sup, file);
      return (true);
    } else {
      try {
        sup.parse(file);  
      } catch (IOException ioe) {
        displayFileInputError(ioe);
        return (false);              
      } catch (OutOfMemoryError oom) {
        ExceptionHandler.getHandler().displayOutOfMemory(oom);
        return (false);  
      }
    }
    setCurrentXMLFile(file);
    postXMLLoad(ff, file.getName());
    return (true);
  }
  
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  boolean postXMLLoad(FabricFactory ff, String fileName) {  
    BioFabricNetwork bfn = ff.getFabricNetwork();
    BioFabricNetwork.PreBuiltBuildData pbd = new BioFabricNetwork.PreBuiltBuildData(bfn, BioFabricNetwork.BUILD_FROM_XML);
    NetworkBuilder nb = new NetworkBuilder(); 
    nb.doNetworkBuild(pbd, true);
    manageWindowTitle(fileName);
    return (true);
  }
 
  /***************************************************************************
  **
  ** Load network from gaggle
  */ 
    
  public boolean loadFromGaggle(List links, List singles) { 
    HashSet reducedLinks = new HashSet();
    HashSet culledLinks = new HashSet();
    BioFabricNetwork.preprocessLinks(links, reducedLinks, culledLinks);
    if (!culledLinks.isEmpty()) {
      ResourceManager rMan = ResourceManager.getManager();
      String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
      // Ignore shadow link culls: / 2
      String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[] {new Integer(culledLinks.size() / 2)});
      JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                                    rMan.getString("fabricRead.dupLinkTitle"),
                                    JOptionPane.WARNING_MESSAGE);
    }
    BioFabricNetwork.OrigBuildData bfnbd = new BioFabricNetwork.OrigBuildData(reducedLinks, new HashSet(singles), colGen_, BioFabricNetwork.BUILD_FROM_GAGGLE);
    NetworkBuilder nb = new NetworkBuilder(); 
    nb.doNetworkBuild(bfnbd, true);
    manageWindowTitle("Gaggle");
    return (true);
  }  
    
  /***************************************************************************
  **
  ** Do standard file checks and warnings
  */
 
  public boolean standardFileChecks(File target, boolean mustExist, boolean canCreate,
                                    boolean checkOverwrite, boolean mustBeDirectory, 
                                    boolean canWrite, boolean canRead) {
    ResourceManager rMan = ResourceManager.getManager();
    boolean doesExist = target.exists();
  
    if (mustExist) {
      if (!doesExist) {
        String noFileFormat = rMan.getString("fileChecks.noFileFormat");
        String noFileMsg = MessageFormat.format(noFileFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noFileMsg,
                                      rMan.getString("fileChecks.noFileTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    if (mustBeDirectory) {
      if (doesExist && !target.isDirectory()) {
        String notADirectoryFormat = rMan.getString("fileChecks.notADirectoryFormat");
        String notADirectoryMsg = MessageFormat.format(notADirectoryFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, notADirectoryMsg,
                                      rMan.getString("fileChecks.notADirectoryTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    } else { // gotta be a file
      if (doesExist && !target.isFile()) {
        String notAFileFormat = rMan.getString("fileChecks.notAFileFormat");
        String notAFileMsg = MessageFormat.format(notAFileFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, notAFileMsg,
                                      rMan.getString("fileChecks.notAFileTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }

    if (!doesExist && canCreate) {
      if (mustBeDirectory) {
        throw new IllegalArgumentException();
      }
      boolean couldNotCreate = false;
      try {
        if (!target.createNewFile()) {
          couldNotCreate = true;
        }
      } catch (IOException ioex) {
        couldNotCreate = true;   
      }
      if (couldNotCreate) {
        String noCreateFormat = rMan.getString("fileChecks.noCreateFormat");
        String noCreateMsg = MessageFormat.format(noCreateFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noCreateMsg,
                                      rMan.getString("fileChecks.noCreateTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    boolean didExist = doesExist;
    doesExist = target.exists();    
    
    if (canWrite) {
      if (doesExist && !target.canWrite()) {
        String noWriteFormat = rMan.getString("fileChecks.noWriteFormat");
        String noWriteMsg = MessageFormat.format(noWriteFormat, new Object[] {target.getName()});
        JOptionPane.showMessageDialog(topWindow_, noWriteMsg,
                                      rMan.getString("fileChecks.noWriteTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    if (canRead) {
      if (doesExist && !target.canRead()) {
        String noReadFormat = rMan.getString("fileChecks.noReadFormat");
        String noReadMsg = MessageFormat.format(noReadFormat, new Object[] {target.getName()});     
        JOptionPane.showMessageDialog(topWindow_, noReadMsg,
                                      rMan.getString("fileChecks.noReadTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    
    if (didExist && checkOverwrite) {  // note we care about DID exist (before creation)
      String overFormat = rMan.getString("fileChecks.doOverwriteFormat");
      String overMsg = MessageFormat.format(overFormat, new Object[] {target.getName()});
      int overwrite =
        JOptionPane.showConfirmDialog(topWindow_, overMsg,
                                      rMan.getString("fileChecks.doOverwriteTitle"),
                                      JOptionPane.YES_NO_OPTION);        
      if (overwrite != JOptionPane.YES_OPTION) {
        return (false);
      }
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get readable attribute file
  */
  
  public File getTheFile(String ext1, String ext2, String prefTag, String desc) { 
    File file = null;      
    String filename = getPreference(prefTag);
    while (file == null) {
      JFileChooser chooser = new JFileChooser();       
      FileFilter filter;
      if (ext2 == null) {
        filter = new FileExtensionFilters.SimpleFilter(ext1, desc);
      } else {
        filter = new FileExtensionFilters.DoubleExtensionFilter(ext1, ext2, desc);
      }     
      chooser.addChoosableFileFilter(filter);
      if (filename != null) {
        File startDir = new File(filename);
        if (startDir.exists()) {
          chooser.setCurrentDirectory(startDir);  
        }
      }

      int option = chooser.showOpenDialog(topWindow_);
      if (option != JFileChooser.APPROVE_OPTION) {
        return (null);
      }
      file = chooser.getSelectedFile();
      if (file == null) {
        return (null);
      }
      if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                    FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                    FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
        file = null;
        continue; 
      }
    }
    return (file);
  }

  /***************************************************************************
  **
  ** Do standard file checks and warnings
  */
     
  public Map loadTheFile(File file, boolean forNodes) {
    HashMap attributes = new HashMap();   
    try {    
      AttributeLoader.ReadStats stats = new AttributeLoader.ReadStats();
      AttributeLoader alod = new AttributeLoader();
      alod.readAttributes(file, forNodes, attributes, stats);
      if (!stats.badLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String badLineFormat = rMan.getString("attribRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {new Integer(stats.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("attribRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (!stats.dupLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String dupLineFormat = rMan.getString("attribRead.dupLineFormat");
        String dupLineMsg = MessageFormat.format(dupLineFormat, new Object[] {new Integer(stats.dupLines.size())});
        JOptionPane.showMessageDialog(topWindow_, dupLineMsg,
                                      rMan.getString("attribRead.dupLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (!forNodes && !stats.shadowsPresent) {
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.noShadowError"),
                                      rMan.getString("attribRead.noShadowTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (null);
      }
     
    } catch (IOException ioe) {
      displayFileInputError(ioe);
      return (null);              
    }
    setPreference("AttribDirectory", file.getAbsoluteFile().getParent());
    return (attributes);
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public void preLoadOperations() { 
    bfp_.reset();
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public BufferedImage expensiveModelOperations(BioFabricNetwork.BuildData bfnbd, boolean forMain) throws IOException { 
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(600, 800);
    screenSize.setSize((int)(screenSize.getWidth() * 0.8), (int)(screenSize.getHeight() * 0.4));
    // Possibly expensive network analysis preparation:
    BioFabricNetwork bfn = new BioFabricNetwork(bfnbd);
    // Possibly expensive display object creation:
    bfp_.installModel(bfn);  
    // Very expensive display buffer creation:
    boolean showShadows = FabricDisplayOptionsManager.getMgr().getDisplayOptions().getDisplayShadows();
    int lco = bfn.getLinkCount(showShadows);
    int[] preZooms;
    if (lco != 0) {
      int linkLog = (int)Math.ceil(Math.log(lco) / Math.log(2.0));
      int firstDrawLog = (int)Math.ceil(Math.log(1.0E4) / Math.log(2.0));  
      int numPre = Math.max(linkLog - firstDrawLog, 4);
      preZooms = new int[numPre];
      preZooms[0] = 1;
      for (int i = 1; i < numPre; i++) {
        preZooms[i] = 2 * preZooms[i - 1];
      }
    } else {
      preZooms = new int[1];
      preZooms[0] = 1;   
    }
    
    bfp_.zoomForBuf(preZooms, screenSize);
    BufferedImage topImage = null;
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 1/*30*/, bfp_);
      topImage = bb.buildBufs(preZooms, bfp_, 24);
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_);
      topImage = bb.buildOneBuf(preZooms);      
      bfp_.setBufBuilder(null);
    }
    return (topImage);
  }
  
  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public BufferedImage expensiveRecolorOperations(boolean forMain) throws IOException { 
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(800, 400);
    screenSize.setSize((int)(screenSize.getWidth() * 0.8), (int)(screenSize.getHeight() * 0.4));
    colGen_.newColorModel();
    bfp_.changePaint();
    
    BioFabricNetwork bfn = bfp_.getNetwork();
    int numPre = 4;
    if (bfn != null) {
      boolean showShadows = FabricDisplayOptionsManager.getMgr().getDisplayOptions().getDisplayShadows();
      int linkLog = (int)Math.ceil(Math.log(bfn.getLinkCount(showShadows)) / Math.log(2.0));
      int firstDrawLog = (int)Math.ceil(Math.log(1.0E4) / Math.log(2.0));  
      numPre = Math.max(linkLog - firstDrawLog, 4);
    }
    int[] preZooms = new int[numPre];
    preZooms[0] = 1;
    for (int i = 1; i < numPre; i++) {
      preZooms[i] = 2 * preZooms[i - 1];
    }
    
    bfp_.zoomForBuf(preZooms, screenSize);
    BufferedImage topImage = null;
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 1, bfp_);
      topImage = bb.buildBufs(preZooms, bfp_, 24);
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_);
      topImage = bb.buildOneBuf(preZooms);      
      bfp_.setBufBuilder(null);
    }
    return (topImage);
  }
  
  /***************************************************************************
  **
  ** Handles post-recolor operations
  */ 
       
  public void postRecolorOperations(BufferedImage topImage) {
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldRect());
    return;
  }
   
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  public void postLoadOperations(BufferedImage topImage) {
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldRect());
    bfp_.installModelPost();
    bfp_.installZooms();
    bfp_.initZoom();
    checkForChanges();
    bfp_.repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Do new model operations all on AWT thread!
  */ 

  public void newModelOperations(BioFabricNetwork.BuildData bfnbd, boolean forMain) throws IOException { 
    preLoadOperations();
    BufferedImage topImage = expensiveModelOperations(bfnbd, forMain);
    postLoadOperations(topImage);
    return;
  }
    
  /***************************************************************************
  **
  ** Do window title
  */ 

  public void manageWindowTitle(String fileName) {
    ResourceManager rMan = ResourceManager.getManager();
    String title;
    if (fileName == null) {
      title = rMan.getString("window.title");  
    } else {
      String titleFormat = rMan.getString("window.titleWithName");
      title = MessageFormat.format(titleFormat, new Object[] {fileName});
    }
    topWindow_.setTitle(title);
    return;
  } 
    
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  boolean saveToFile(String fileName) {
       
    File file = null;
    if (fileName == null) {
      String dirName = getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif"));    
        if (dirName != null) {
          File startDir = new File(dirName);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }  
        int option = chooser.showSaveDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file != null) {
          if (!file.exists()) {
            if (!FileExtensionFilters.hasSuffix(file.getName(), ".bif")) {
              file = new File(file.getAbsolutePath() + ".bif");
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }       
      }     
    } else {
      // given a name, we do not check overwrite:
      file = new File(fileName);
      if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                    FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                    FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
        return (false);
      }        
    }

    
    BioFabricNetwork bfn = bfp_.getNetwork();
      
    if (bfn.getLinkCount(true) > 5000) {
      BackgroundFileWriter bw = new BackgroundFileWriter(); 
      bw.doBackgroundWrite(file);
      return (true);
    } else {
      try {
        saveToOutputStream(new FileOutputStream(file));
        setCurrentXMLFile(file);
        manageWindowTitle(file.getName());
        return (true);
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
    }  
  }
    
  /***************************************************************************
  ** 
  ** Move nodes to match shapes
  */

  public Map doReorderLayout(BioFabricNetwork bfn, 
                             ClusteredLayout.CRParams params,
                             BTProgressMonitor monitor, 
                             double startFrac, 
                             double endFrac) throws AsynchExitRequestException { 

    ClusteredLayout lo = new ClusteredLayout();
    SortedMap connVecs = lo.getConnectivityVectors(bfn);
   
    ClusteredLayout.ResortParams rp = (ClusteredLayout.ResortParams)params;
    
    List ordered = new ArrayList();
    int numRows = bfn.getRowCount();
    for (int i = 0; i < numRows; i++) {
      ordered.add(Integer.toString(i));
    }
    
    double currStart = startFrac;
    double inc = (endFrac - startFrac) / (double)rp.passCount;
    double currEnd = currStart + inc;
    
    TreeMap rankings = new TreeMap();
    ClusteredLayout.ClusterPrep cprep = lo.setupForResort(bfn, connVecs, ordered, rankings);
    Double lastRank = (Double)rankings.get(rankings.lastKey());
    
    for (int i = 0; i < rp.passCount; i++) {
      monitor.updateRankings(rankings);
      List nextOrdered = lo.resort(cprep, monitor, currStart, currEnd);
      currStart = currEnd;
      currEnd = currStart + inc;
      cprep = lo.setupForResort(bfn, connVecs, nextOrdered, rankings);
      Integer lastKey = (Integer)rankings.lastKey();
      Double nowRank = (Double)rankings.get(lastKey);
      if (rp.terminateAtIncrease) {
        if (lastRank.doubleValue() < nowRank.doubleValue()) {
          rankings.remove(lastKey);
          break;
        }
      }
      ordered = nextOrdered;
      lastRank = nowRank;
    }
    
    monitor.updateRankings(rankings);
    Map orderedNames = lo.convertOrderToMap(bfn, ordered);
    return (orderedNames);
  }
  
  /***************************************************************************
  **
  ** Clustered Layout guts
  */   

  public Map doClusteredLayout(BioFabricNetwork bfn, 
                               ClusteredLayout.CRParams params,
                               BTProgressMonitor monitor, 
                               double startFrac, double endFrac) throws AsynchExitRequestException {
   
    
 
    ClusteredLayout lo = new ClusteredLayout();
    ClusteredLayout.ClusterParams cp = (ClusteredLayout.ClusterParams)params;
    SortedMap connVecs = lo.getConnectivityVectors(bfn);

    TreeMap dists = new TreeMap(Collections.reverseOrder());
    HashMap connMag = new HashMap();
    
    Integer mostConnected = (cp.distanceMethod == ClusteredLayout.ClusterParams.COSINES) ?
                               lo.getConnectivityCosines(connVecs, dists, connMag, bfn) :
                               lo.getConnectivityJaccard(connVecs, dists, connMag, bfn);
    
    ArrayList linkTrace = new ArrayList();
    ArrayList jumpLog = new ArrayList();
    List ordered = lo.orderByDistanceChained(bfn, mostConnected, dists, 
                                             connMag, linkTrace, cp.chainLength, 
                                             cp.tolerance, jumpLog, monitor, startFrac, endFrac);
    Map orderedNames = lo.convertOrderToMap(bfn, ordered);
    return (orderedNames);
  }
  
  /***************************************************************************
  **
  ** Common save activities
  */   
  
  void saveToOutputStream(OutputStream stream) throws IOException {
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")));
    Indenter ind = new Indenter(out, Indenter.DEFAULT_INDENT);
    BioFabricNetwork bfn = bfp_.getNetwork();
    bfn.writeXML(out, ind);
    out.close();
    return;
  }
  
  /***************************************************************************
  **
  ** Displays file reading error message
  */ 
       
  public void displayFileInputError(IOException ioex) { 
    ResourceManager rMan = ResourceManager.getManager();
    
    if ((ioex == null) || (ioex.getMessage() == null) || (ioex.getMessage().trim().equals(""))) {
      JOptionPane.showMessageDialog(topWindow_, 
                                    rMan.getString("fileRead.errorMessage"), 
                                    rMan.getString("fileRead.errorTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    String errMsg = ioex.getMessage().trim();
    String format = rMan.getString("fileRead.inputErrorMessageForIOEx");
    String outMsg = MessageFormat.format(format, new Object[] {errMsg}); 
    JOptionPane.showMessageDialog(topWindow_, outMsg, 
                                  rMan.getString("fileRead.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
    return;
  }
  
  /***************************************************************************
  **
  ** Displays file writing error message
  */ 
       
  void displayFileOutputError() { 
    ResourceManager rMan = ResourceManager.getManager(); 
    JOptionPane.showMessageDialog(topWindow_, 
                                  rMan.getString("fileWrite.errorMessage"), 
                                  rMan.getString("fileWrite.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
    return;
  }
 
  /***************************************************************************
  **
  ** Displays file reading error message for invalid input
  */ 
       
  public void displayInvalidInputError(InvalidInputException iiex) { 
    ResourceManager rMan = ResourceManager.getManager(); 
    String errKey = iiex.getErrorKey();
    boolean haveKey = (errKey != null) && (!errKey.equals(InvalidInputException.UNSPECIFIED_ERROR)); 
    int lineno = iiex.getErrorLineNumber();
    boolean haveLine = (lineno != InvalidInputException.UNSPECIFIED_LINE);
    String outMsg;
    if (haveKey && haveLine) { 
      String format = rMan.getString("fileRead.inputErrorMessageForLineWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {new Integer(lineno + 1), keyedErr});
    } else if (haveKey && !haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {keyedErr});
    } else if (!haveKey && haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageForLine");
      outMsg = MessageFormat.format(format, new Object[] {new Integer(lineno + 1)});      
    } else {
      outMsg = rMan.getString("fileRead.inputErrorMessage");      
    } 
    JOptionPane.showMessageDialog(topWindow_, outMsg, 
                                  rMan.getString("fileRead.errorTitle"),
                                  JOptionPane.ERROR_MESSAGE);
  return;
  }
 
  /***************************************************************************
  **
  ** Set the fabric panel
  */ 
  
  public void setFabricPanel(BioFabricPanel bfp) {
    bfp_ = bfp;
    return;
  }  
   
  /***************************************************************************
  **
  ** Tell us the zoom state has changed
  */ 
  
  public void zoomStateChanged(boolean scrollOnly) {
    if (!scrollOnly) {
      handleZoomButtons();
    }
    topWindow_.getOverview().setViewInWorld(bfp_.getViewInWorld());
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle zoom buttons
  */ 

  private void handleZoomButtons() {  
    //
    // Enable/disable zoom actions based on zoom limits:
    //

    InOutZoomAction zaOutWI = (InOutZoomAction)withIcons_.get(new Integer(ZOOM_OUT));
    InOutZoomAction zaOutNI = (InOutZoomAction)noIcons_.get(new Integer(ZOOM_OUT));
    InOutZoomAction zaInWI = (InOutZoomAction)withIcons_.get(new Integer(ZOOM_IN));
    InOutZoomAction zaInNI = (InOutZoomAction)noIcons_.get(new Integer(ZOOM_IN));
    // In this case, we do not want to allow a "wide" zoom, since we do not have
    // a buffered image to handle it!  Restrict to first defined zoom!
    if (bfp_.getZoomController().zoomIsFirstDefined()) {
      zaOutWI.setConditionalEnabled(false);
      if (zaOutNI != null) zaOutNI.setConditionalEnabled(false);
      zaInWI.setConditionalEnabled(true);
      if (zaInNI != null) zaInNI.setConditionalEnabled(true);
    } else if (bfp_.getZoomController().zoomIsMax()) {
      zaOutWI.setConditionalEnabled(true);
      if (zaOutNI != null) zaOutNI.setConditionalEnabled(true);
      zaInWI.setConditionalEnabled(false);
      if (zaInNI != null) zaInNI.setConditionalEnabled(false);        
    } else {
      zaOutWI.setConditionalEnabled(true);
      if (zaOutNI != null) zaOutNI.setConditionalEnabled(true);
      zaInWI.setConditionalEnabled(true);
      if (zaInNI != null) zaInNI.setConditionalEnabled(true);              
    }
    return;
  }

  /***************************************************************************
  **
  ** Gaggle setup
  */ 
  
  public void setGaggleElements(JMenu gaggleGooseChooseMenu, JComboBox gaggleGooseCombo) {    
    //
    // Controls for Gaggle:
    //
    if ((gaggleGooseChooseMenu != null) && (gaggleGooseCombo != null)) {
      gaggleGooseChooseMenu_ = gaggleGooseChooseMenu;
      gaggleGooseCombo_ = gaggleGooseCombo;
      gaggleGooseCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (managingGaggleControls_) {
              return;
            }
            FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
            if ((goose != null) && goose.isActivated()) {
              ObjChoiceContent occ = (ObjChoiceContent)gaggleGooseCombo_.getSelectedItem();
              goose.setCurrentGaggleTarget((occ == null) ? null : occ.val);
              setCurrentGaggleTarget(gaggleGooseCombo_.getSelectedIndex());
            }
          } catch (Exception ex) {
            ExceptionHandler.getHandler().displayException(ex);
          }
          return;
        }
      });
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Preferences are stored by package. 
  */ 
    
  public void setPreference(String key, String val) {
    Preferences prefs = Preferences.userNodeForPackage(FabricCommands.class);
    prefs.put(key, val);
    return;
  }    
  
  /***************************************************************************
  **
  ** Preferences are stored by package.
  */ 
    
  public String getPreference(String key) {
    Preferences prefs = Preferences.userNodeForPackage(FabricCommands.class);    
    String retval = prefs.get(key, null);
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Set the current file
  */ 
    
  public void setCurrentXMLFile(File file) {
    currentFile_ = file;
    if (currentFile_ == null) {
      return;
    }
    setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    return;
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  ** 
  ** Get the commands for the given tag
  */

  public static synchronized FabricCommands getCmds(String className) {
    if (perClass_ == null) {
      throw new IllegalStateException();
    }
    FabricCommands fc = (FabricCommands)perClass_.get(className);
    if (fc == null) {
      throw new IllegalStateException();
    }
    return (fc);
  }  
 
  /***************************************************************************
  ** 
  ** Init the commands for the given tag
  */

  public static synchronized FabricCommands initCmds(String className, BioFabricApplication bfa, 
                                                     BioFabricWindow topWindow, boolean isForMain) {
    if (perClass_ == null) {
      perClass_ = new HashMap();
    }
    FabricCommands fc = (FabricCommands)perClass_.get(className);
    if (fc != null) {
      throw new IllegalStateException();
    }
    fc = new FabricCommands(bfa, topWindow, isForMain);
    perClass_.put(className, fc);
    return (fc);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private FabricCommands(BioFabricApplication bfa, BioFabricWindow topWindow, boolean isMain) {
    bfa_ = bfa;
    topWindow_ = topWindow;
    withIcons_ = new HashMap();
    noIcons_ = new HashMap();
    colGen_ = new FabricColorGenerator();
    colGen_.newColorModel();
    isForMain_ = isMain;
    isAMac_ = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    FabricDisplayOptionsManager.getMgr().addTracker(this);
    EventManager mgr = EventManager.getManager();
    mgr.addSelectionChangeListener(this);
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Checks if it is enabled or not
  */
  
  public abstract class ChecksForEnabled extends AbstractAction {
    
    protected static final int IGNORE   = -1;
    protected static final int DISABLED =  0;
    protected static final int ENABLED  =  1;
    
    protected boolean enabled_ = true;
    protected boolean pushed_ = false;
    
    public void checkIfEnabled() {
      enabled_ = checkGuts();
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
      return;
    }
    
    public void pushDisabled(int pushCondition) {
      pushed_ = canPush(pushCondition);
      boolean reversed = reversePush(pushCondition);
      if (pushed_) {
        this.setEnabled(reversed);
      }
    }
    
    public void setConditionalEnabled(boolean enabled) {
      //
      // If we are pushed, just stash the value.  If not
      // pushed, stash and apply.
      //
      enabled_ = enabled;
      if (!pushed_) {
        this.setEnabled(enabled_);
      }
    }    
    
    public boolean isPushed() {
      return (pushed_);
    }    
 
    public void popDisabled() {
      if (pushed_) {
        this.setEnabled(enabled_);
        pushed_ = false;
      }
      return;
    }
    
    // Default can always be enabled:
    protected boolean checkGuts() {
      return (true);
    }
 
    // Default can always be pushed:
    protected boolean canPush(int pushCondition) {
      return (true);
    }
    
    // Signals we are reverse pushed (enabled when others disabled)
    protected boolean reversePush(int pushCondition) {
      return (false);
    }    
 
  }

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class PropagateDownAction extends ChecksForEnabled {
    
    PropagateDownAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.PropagateDown"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.PropagateDown"));      
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/PropagateSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PropagateDownMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.selectionsToSubmodel();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }
    
  }
    
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToSelected extends ChecksForEnabled {
    
    ZoomToSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ZoomToAllFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }    
  }
   
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToModelAction extends ChecksForEnabled {
    
    ZoomToModelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToModel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToModel"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ZoomToAllFabric24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToModelMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToModel();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }    
  }  

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentSelected extends ChecksForEnabled {
    
    ZoomToCurrentSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().zoomToCurrentSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentMouse extends ChecksForEnabled {
    
    ZoomToCurrentMouse(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMouse"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMouse"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMouseMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
        char accel = rMan.getChar("command.ZoomToCurrentMouseAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricMagnifyingTool fmt = bfp_.getMagnifier();
        Point2D pt = fmt.getMouseLoc();
        Point rcPoint = bfp_.worldToRowCol(pt);
        Rectangle rect = bfp_.buildFocusBox(rcPoint);
        bfp_.getZoomController().zoomToRect(rect);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ZoomToCurrentMagnify extends ChecksForEnabled {
    
    ZoomToCurrentMagnify(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMagnify"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMagnify"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMagnifyMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
        char accel = rMan.getChar("command.ZoomToCurrentMagnifyAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {        
        FabricMagnifyingTool fmt = bfp_.getMagnifier();
        Rectangle rect = fmt.getClipRect();
        bfp_.getZoomController().zoomToRect(rect);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
  }

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CenterOnNextSelected extends ChecksForEnabled {
    
    CenterOnNextSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnNextSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnNextSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Forward24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnNextSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().centerToNextSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CenterOnPreviousSelected extends ChecksForEnabled {
    
    CenterOnPreviousSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnPreviousSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnPreviousSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Back24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnPreviousSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().centerToPreviousSelected();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class AddFirstNeighborsAction extends ChecksForEnabled {
    
    AddFirstNeighborsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.AddFirstNeighbors"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.AddFirstNeighbors"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/PlusOneDeg24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AddFirstNeighborsMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.addFirstNeighbors();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }      
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
    
  private class BuildSelectAction extends ChecksForEnabled {
    
    BuildSelectAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.BuildSelect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.BuildSelect"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.BuildSelectMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.toggleBuildSelect();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class InOutZoomAction extends ChecksForEnabled {
   
    private char sign_;
    
    InOutZoomAction(boolean doIcon, char sign) {
      sign_ = sign;
      ResourceManager rMan = ResourceManager.getManager();
      String iconName;
      String stringName;
      String mnemName;
      String accelName;      
      if (sign == '+') {
        iconName = "ZoomIn24.gif";
        stringName = "command.ZoomIn";
        mnemName = "command.ZoomInMnem";
        accelName = "command.ZoomInAccel";        
      } else {
        iconName = "ZoomOut24.gif";
        stringName = "command.ZoomOut";
        mnemName = "command.ZoomOutMnem";
        accelName = "command.ZoomOutAccel";
      }      
      putValue(Action.NAME, rMan.getString(stringName));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(stringName));      
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/" + iconName);  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemName); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
      char accel = rMan.getChar(accelName);
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.getZoomController().bumpZoomWrapper(sign_);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    public void setConditionalEnabled(boolean enabled) {
      setEnabled(enabled);
      return;
    } 
    
    //
    // Override: We handle this internally.
    //
    public void checkIfEnabled() {
    }    
        
    protected boolean canPush(int pushCondition) {
      return ((pushCondition & FabricCommands.ALLOW_NAV_PUSH) == 0x00);
    }     
    
  }
    
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class ClearSelectionsAction extends ChecksForEnabled {
   
    ClearSelectionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ClearSel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ClearSel"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ClearFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ClearSelMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfp_.clearSelections();        
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.haveASelection());
    }       
  } 
 
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class ZoomToRect extends ChecksForEnabled {
   
    ZoomToRect(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToRect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToRect"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/ZoomToFabricRect24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToRectMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
        char accel = rMan.getChar("command.ZoomToRectAccel");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
      }
      
    }
    
    public void actionPerformed(ActionEvent e) {
      try {    
        bfp_.setToCollectZoomRect();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
       
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class CancelAction extends ChecksForEnabled {
   
    CancelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CancelAddMode"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CancelAddMode"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CancelAddModeMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
         bfp_.cancelModals();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (false);      
    }
 
    protected boolean reversePush(int pushCondition) {
      return ((pushCondition & FabricCommands.ALLOW_NAV_PUSH) != 0x00);
    }    
      
  }

  /***************************************************************************
  **
  ** Command
  */ 
  
  private class SearchAction extends ChecksForEnabled {
    
    SearchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Search"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Search"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Find24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SearchMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        boolean haveSelection = bfp_.haveASelection();
        boolean buildingSels = bfp_.amBuildingSelections();
        FabricSearchDialog fsd = new FabricSearchDialog(topWindow_, topWindow_.getFabricPanel().getNetwork(), 
                                                        haveSelection, buildingSels);      
        fsd.setVisible(true);
        if (fsd.itemWasFound()) {
          Set matches = fsd.getMatches();
          boolean doDiscard = fsd.discardSelections();
          topWindow_.getFabricPanel().installSearchResult(matches, doDiscard);
        }
        return;
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class CompareNodesAction extends ChecksForEnabled {
    
    CompareNodesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CompareNodes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CompareNodes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CompareNodesMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        Set allNodes = bfp_.getNetwork().getNodeSet();
        CompareNodesSetupDialog fsd = new CompareNodesSetupDialog(topWindow_, allNodes);      
        fsd.setVisible(true);
        if (fsd.haveResult()) {
          Set result = fsd.getResults();
          bfp_.installSearchResult(result, true);          
          bfp_.addFirstNeighbors();
          bfp_.selectionsToSubmodel();
        }
        return;
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class CloseAction extends ChecksForEnabled {
    
    CloseAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Close"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Close"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CloseMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        bfa_.shutdownFabric();
      } catch (Exception ex) {
        // Going down (usually) so don't show exception in UI
        ex.printStackTrace();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private abstract class LayoutViaAttributesAction extends ChecksForEnabled {
        
    LayoutViaAttributesAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));       
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    } 
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation();
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
   
    protected abstract boolean performOperation();
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
 
  }        
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutNodesViaAttributesAction extends LayoutViaAttributesAction {
        
    LayoutNodesViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutNodesViaAttributes", "command.LayoutNodesViaAttributesMnem");
    }
    
    protected boolean performOperation() {
      File file = getTheFile(".noa", ".na", "AttribDirectory", "filterName.noa");
      if (file == null) {
        return (true);
      }
      Map nodeAttributes = loadTheFile(file, true);
      if (nodeAttributes == null) {
        return (true);
      }
      if (!bfp_.getNetwork().checkNewNodeOrder(nodeAttributes)) { 
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.badRowMessage"),
                                      rMan.getString("attribRead.badRowSemanticsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
        return (true);
      }
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.NODE_ATTRIB_LAYOUT);
      bfn.setNodeOrder(nodeAttributes);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, null);    
      return (true);
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }
  }      
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutLinksViaAttributesAction extends LayoutViaAttributesAction {
        
    LayoutLinksViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutLinksViaAttributes", "command.LayoutLinksViaAttributesMnem");
    }
    
    protected boolean performOperation() {   
      File file = getTheFile(".eda", ".ed", "AttribDirectory", "filterName.eda");
      if (file == null) {
        return (true);
      }
      Map edgeAttributes = loadTheFile(file, false);
      if (edgeAttributes == null) {
        return (true);
      }
      SortedMap modifiedAndChecked = bfp_.getNetwork().checkNewLinkOrder(edgeAttributes);      
      if (modifiedAndChecked == null) { 
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.badColMessage"),
                                      rMan.getString("attribRead.badColSemanticsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
        return (true);
      }
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.LINK_ATTRIB_LAYOUT);
      bfn.setLinkOrder(modifiedAndChecked);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, null);         
      return (true);
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    } 
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
   
  private class DefaultLayoutAction extends ChecksForEnabled {
        
    DefaultLayoutAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.DefaultLayout"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.DefaultLayout"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.DefaultLayoutMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    DefaultLayoutAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
          
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.DEFAULT_LAYOUT);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, null);        
      return (true);   
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaConnectivityAction extends ChecksForEnabled {
        
    LayoutViaConnectivityAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaConnectivity"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaConnectivity"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaConnectivityMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    LayoutViaConnectivityAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
          
      ClusteredLayoutParamsDialog clpd = 
        new ClusteredLayoutParamsDialog(topWindow_, new ClusteredLayout.ClusterParams());  
  
      clpd.show();
      if (!clpd.haveResult()) {
        return (false);
      }
  
      ClusteredLayout.ClusterParams result = clpd.getParams();
      
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.CLUSTERED_LAYOUT);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, result);        
      return (true);   
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaShapeMatchAction extends ChecksForEnabled {
        
    LayoutViaShapeMatchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaShapeMatch"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaShapeMatch"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaShapeMatchMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    LayoutViaShapeMatchAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
      
      ReorderLayoutParamsDialog clpd = 
        new ReorderLayoutParamsDialog(topWindow_, new ClusteredLayout.ResortParams());  
  
      clpd.show();
      if (!clpd.haveResult()) {
        return (false);
      }
  
      ClusteredLayout.ResortParams result = clpd.getParams();
          
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.REORDER_LAYOUT);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, result);   
      return (true);   
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class SetLinkGroupsAction extends ChecksForEnabled {
        
    SetLinkGroupsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetLinkGroups"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetLinkGroups"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetLinkGroupsMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    SetLinkGroupsAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
          
      BioFabricNetwork bfn = bfp_.getNetwork();
      List currentTags = bfn.getLinkGroups();
      ArrayList links = new ArrayList(bfn.getAllLinks(true));
      Set allRelations = BioFabricNetwork.extractRelations(links).keySet();       
      LinkGroupingSetupDialog lgsd = new LinkGroupingSetupDialog(topWindow_, currentTags, allRelations); 
      lgsd.show();
      if (!lgsd.haveResult()) {
        return (false);
      }
      
      List newGroupings = lgsd.getGroups();
      if (newGroupings.equals(currentTags)) {
        return (true);
      }
     
      BioFabricNetwork.RelayoutBuildData bfnd = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.LINK_GROUP_CHANGE);
      bfnd.setLinkGroups(newGroupings);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfnd, null);   
      return (true);   
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ImportSIFAction extends ChecksForEnabled {
        
    ImportSIFAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadSIF"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadSIF"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadSIFMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    ImportSIFAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
 
      File file = null;      
      String filename = getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        chooser.addChoosableFileFilter(new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif"));
        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      return (loadFromSifSource(file, null));
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LoadXMLAction extends ChecksForEnabled {
        
    LoadXMLAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadXML"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadXML"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadXMLMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
      char accel = rMan.getChar("command.LoadXMLAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
    }
    
    LoadXMLAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    private boolean performOperation(Object[] args) {
 
      File file = null;      
      String filename = getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        chooser.addChoosableFileFilter(new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif"));
        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      return (loadXMLFromSource(file));
    }
  }

   /***************************************************************************
  **
  ** Command
  */ 
   
  private class LoadWithNodeAttributesAction extends ChecksForEnabled {
        
    LoadWithNodeAttributesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadSIFWithNodeAttributes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadSIFWithNodeAttributes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadSIFWithNodeAttributesMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    LoadWithNodeAttributesAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }    
 
    private boolean performOperation(Object[] args) {
      File file = null;      
      String filename = getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        chooser.addChoosableFileFilter(new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif"));
        if (filename != null) {
          File startDir = new File(filename);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }

        int option = chooser.showOpenDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file == null) {
          return (true);
        }
        if (!standardFileChecks(file, FILE_MUST_EXIST, FILE_CAN_CREATE_DONT_CARE, 
                                      FILE_DONT_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                      FILE_CAN_WRITE_DONT_CARE, FILE_CAN_READ)) {
          file = null;
          continue; 
        }
      }
      
      File attribFile = getTheFile(".noa", ".na", "AttribDirectory", "filterName.noa");
      if (attribFile == null) {
        return (true);
      }
  
      Map attribs = loadTheFile(attribFile, true);
      if (attribs == null) {
        return (true);
      }
 
      return (loadFromSifSource(file, attribs));
    }
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAsAction extends ChecksForEnabled {

    SaveAsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SaveAs"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SaveAs"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/SaveAs24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveAsMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      } 
    }
    
    SaveAsAction() {
    }  
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    public boolean performOperation(Object[] args) {
      if (args == null) {
        return (saveToFile(null));
      } else {
        if (((Boolean)args[0]).booleanValue()) {
          String fileName = (String)args[1];
          return (saveToFile(fileName));
        } else {
          OutputStream stream = (OutputStream)args[1];
          try {
            saveToOutputStream(stream);
          } catch (IOException ioe) {
            displayFileOutputError(); // Which is kinda bogus...
            return (false);
          }
          return (true);
        }
      }
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }      
  }  
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAction extends ChecksForEnabled {

    SaveAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Save"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Save"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Save24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
      char accel = rMan.getChar("command.SaveAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }
  
    public void actionPerformed(ActionEvent e) { 
      try {
        if (currentFile_ != null) {
          saveToFile(currentFile_.getAbsolutePath());
        } else {
          saveToFile(null);
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportOrderAction extends ChecksForEnabled {

    ExportOrderAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    protected abstract FileFilter getFilter();
    protected abstract List getSuffixList();
    protected abstract String getPrefSuffix();
    protected abstract void writeItOut(File file) throws IOException;
 
    public boolean performOperation(Object[] args) {      
      File file = null;
      String dirName = getPreference("AttribDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(getFilter());    
        if (dirName != null) {
          File startDir = new File(dirName);
          if (startDir.exists()) {
            chooser.setCurrentDirectory(startDir);  
          }
        }  
        int option = chooser.showSaveDialog(topWindow_);
        if (option != JFileChooser.APPROVE_OPTION) {
          return (true);
        }
        file = chooser.getSelectedFile();
        if (file != null) {
          if (!file.exists()) {
            List cand = getSuffixList();
            if (!FileExtensionFilters.hasASuffix(file.getName(), "", cand)) { 
              file = new File(file.getAbsolutePath() + getPrefSuffix());
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }       
      }     
       
      try {
        writeItOut(file);
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
      setPreference("AttribDirectory", file.getAbsoluteFile().getParent());     
      return (true);
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
     
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportNodeOrderAction extends ExportOrderAction {

    ExportNodeOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportNodeOrder", "command.ExportNodeOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".noa", ".na", "filterName.noa"));
    }
    
    protected List getSuffixList() {
      ArrayList cand = new ArrayList();
      cand.add(".noa");
      cand.add(".na");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".noa");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      bfp_.getNetwork().writeNOA(out);
      out.close();
      return;
    }    
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportLinkOrderAction extends ExportOrderAction {
    
    
    ExportLinkOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportLinkOrder", "command.ExportLinkOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".eda", ".ea", "filterName.eda"));
    }
    
    protected List getSuffixList() {
      ArrayList cand = new ArrayList();
      cand.add(".eda");
      cand.add(".ea");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".eda");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      bfp_.getNetwork().writeEDA(out);
      out.close();
      return;
    }   
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportSelectedNodesAction extends ExportOrderAction {
    
    
    ExportSelectedNodesAction(boolean doIcon) {
      super(doIcon, "command.ExportSelectedNodes", "command.ExportSelectedNodesMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.SimpleFilter(".txt", "filterName.txt"));
    }
    
    protected List getSuffixList() {
      ArrayList cand = new ArrayList();
      cand.add(".txt");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".txt");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      Set sels = bfp_.getNodeSelections();
      Iterator sit = sels.iterator();
      while (sit.hasNext()) {
        String node = (String)sit.next();
        out.println(node);
      }
      out.close();
      return;
    } 
    
    protected boolean checkGuts() {
      return (super.checkGuts() && !bfp_.getNodeSelections().isEmpty());
    }
    
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
      
  private class PrintAction extends ChecksForEnabled {

    PrintAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Print"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Print"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/Print24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PrintMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
      char accel = rMan.getChar("command.PrintAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }

    public void actionPerformed(ActionEvent e) {
      try {
        PrinterJob pj = PrinterJob.getPrinterJob();
        PageFormat pf = pj.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);
        // FIX ME: Needed for Win32?  Linux won't default to landscape without this?
        //PageFormat pf2 = pj.pageDialog(pf);
        pj.setPrintable(bfp_, pf);
        if (pj.printDialog()) {
          try {
            pj.print();
          } catch (PrinterException pex) {
            System.err.println(pex);
          }
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportImageAction extends ChecksForEnabled {

    ExportImageAction(boolean doIcon, String res, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(res));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(res));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        performOperation(null);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    protected abstract ExportSettingsDialog.ExportSettings getExportSettings();
    
    
    public boolean performOperation(Object[] args) { 
   
      ExportSettingsDialog.ExportSettings set;
      if (args == null) {
        set = getExportSettings();
        if (set == null) {
          return (true);
        }
      } else {
        set = (ExportSettingsDialog.ExportSettings)args[0];
      }
      
      File file = null;
      OutputStream stream = null;
      
      if (args == null) {  // not headless...      
        List supported = ImageExporter.getSupportedFileSuffixes();
        String filename = getPreference("ExportDirectory");
        while (file == null) {
          JFileChooser chooser = new JFileChooser(); 
          chooser.addChoosableFileFilter(new FileExtensionFilters.MultiExtensionFilter(supported, "filterName.img"));
          if (filename != null) {
            File startDir = new File(filename);
            if (startDir.exists()) {
              chooser.setCurrentDirectory(startDir);  
            }
          }

          int option = chooser.showSaveDialog(topWindow_);
          if (option != JFileChooser.APPROVE_OPTION) {
            return (true);
          }
          file = chooser.getSelectedFile();
          if (file == null) {
            continue;
          }
          if (!file.exists()) {
            List suffs = ImageExporter.getFileSuffixesForType(set.formatType);
            if (!FileExtensionFilters.hasASuffix(file.getName(), "." , suffs)) {
              file = new File(file.getAbsolutePath() + "." + 
                              ImageExporter.getPreferredSuffixForType(set.formatType));
            }
          }
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                        FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                        FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            file = null;
            continue; 
          }
        }
      } else {
        if (((Boolean)args[1]).booleanValue()) {
          file = new File((String)args[2]);
          if (!standardFileChecks(file, FILE_MUST_EXIST_DONT_CARE, FILE_CAN_CREATE, 
                                       FILE_CHECK_OVERWRITE, FILE_MUST_BE_FILE, 
                                       FILE_CAN_WRITE, FILE_CAN_READ_DONT_CARE)) {
            return (false);
          }
        } else {
          stream = (OutputStream)args[2];
        }
      }  
         
      try {         
        if (file != null) {
          bfp_.exportToFile(file, set.formatType, set.res, set.zoomVal, set.size);  
        } else {
          bfp_.exportToStream(stream, set.formatType, set.res, set.zoomVal, set.size);  
        }
        if (args == null) {
          setPreference("ExportDirectory", file.getAbsoluteFile().getParent());
        }  
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
 
      return (true);
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportSimpleAction extends ExportImageAction {
 
    ExportSimpleAction(boolean doIcon) {
      super(doIcon, "command.Export", "command.ExportMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getWorldRect();
      ExportSettingsDialog esd = new ExportSettingsDialog(topWindow_, wr.width, wr.height);
      esd.show();
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
    
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportPublishAction extends ExportImageAction {
 
    ExportPublishAction(boolean doIcon) {
      super(doIcon, "command.ExportPublish", "command.ExportPublishMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getWorldRect();
      ExportSettingsPublishDialog esd = new ExportSettingsPublishDialog(topWindow_, wr.width, wr.height);
      esd.show();
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class EmptyNetworkAction extends ChecksForEnabled {
        
    EmptyNetworkAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.EmptyNet"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.EmptyNet"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.EmptyNetMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        manageWindowTitle(null);
        BioFabricNetwork.OrigBuildData obd = new BioFabricNetwork.OrigBuildData(new HashSet(), new HashSet(), colGen_, BioFabricNetwork.BUILD_FROM_SIF);
        newModelOperations(obd, true);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SetDisplayOptionsAction extends ChecksForEnabled {
    
    SetDisplayOptionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetDisplayOpts"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetDisplayOpts"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetDisplayOptsMnem");
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }   
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricDisplayOptionsDialog dod = new FabricDisplayOptionsDialog(topWindow_);
        dod.setVisible(true);
        if (dod.haveResult()) {
          FabricDisplayOptionsManager dopmgr = FabricDisplayOptionsManager.getMgr();
          dopmgr.setDisplayOptions(dod.getNewOpts(), dod.needsRebuild(), dod.needsRecolor());
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }  
   
  /***************************************************************************
  **
  ** Command
  */ 
   
  public class SetCurrentGaggleTargetAction extends ChecksForEnabled {
        
    private String gooseName_;
    private int gooseIndex_;
    
    public SetCurrentGaggleTargetAction(String gooseName, int gooseIndex) {
      gooseName_ = gooseName;
      gooseIndex_ = gooseIndex;
      putValue(Action.NAME, gooseName);
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          goose.setCurrentGaggleTarget(gooseName_);
        }
        setCurrentGaggleTarget(gooseIndex_);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }  
  }      
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleUpdateGeese extends ChecksForEnabled {
    
    private ImageIcon standard_;
    private ImageIcon inbound_;
    
    GaggleUpdateGeese(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleUpdateGeese"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleUpdateGeese"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/U24.gif");
        standard_ = new ImageIcon(ugif);
        if (isAMac_) {
          ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/U24Selected.gif");
          inbound_ = new ImageIcon(ugif);
        }
        putValue(Action.SMALL_ICON, standard_);
      } else {
        char mnem = rMan.getChar("command.GaggleUpdateGeeseMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          setButtonCondition(false);
          updateGaggleTargetActions();
        }        
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    public void setButtonCondition(boolean activate) {
      if (isAMac_) {
        putValue(Action.SMALL_ICON, (activate) ? inbound_ : standard_);
        gaggleUpdateGooseButton_.validate();
      } else {
        gaggleUpdateGooseButton_.setBackground((activate) ? Color.orange : gaggleButtonOffColor_);
      }
      return;
    }
  }  
  

  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleRaiseGoose extends ChecksForEnabled {
    
    GaggleRaiseGoose(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleRaiseGoose"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleRaiseGoose"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/S24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.GaggleRaiseGooseMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          goose.raiseCurrentTarget();
        }        
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleLowerGoose extends ChecksForEnabled {
    
    GaggleLowerGoose(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleLowerGoose"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleLowerGoose"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/H24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.GaggleLowerGooseMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          goose.hideCurrentTarget();
        }        
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleSendNetwork extends ChecksForEnabled {
    
    GaggleSendNetwork(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleSendNetwork"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleSendNetwork"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/N24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.GaggleSendNetworkMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          SelectionSupport ss = goose.getSelectionSupport();
          SelectionSupport.NetworkForSpecies net = ss.getOutboundNetwork();
          if ((net == null) || net.getLinks().isEmpty()) {
            return;
          }   
          goose.transmitNetwork(net);
        }                  
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleSendNameList extends ChecksForEnabled {
    
    GaggleSendNameList(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleSendNameList"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleSendNameList"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/L24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.GaggleSendNameListMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          goose.transmitSelections();
        }                  
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    } 
  }  
          
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleProcessInbound extends ChecksForEnabled {
    
    private ImageIcon standard_;
    private ImageIcon inbound_;
    
    GaggleProcessInbound(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.GaggleProcessInbound"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.GaggleProcessInbound"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/P24.gif");
        standard_ = new ImageIcon(ugif);
        if (isAMac_) {
          ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/P24Selected.gif");
          inbound_ = new ImageIcon(ugif);
        }       
        putValue(Action.SMALL_ICON, standard_);
      } else {
        char mnem = rMan.getChar("command.GaggleProcessInboundMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {   
          // Will be on background thread for awhile; don't lose incoming commands
          //gaggleButton_.setBackground(gaggleButtonOffColor_);
          setButtonCondition(false);
          SelectionSupport ss = goose.getSelectionSupport();
          // Kinda hackish.  First time in, set the targets when this button is pressed!
          List targets = ss.getGooseList();
          int numTarg = targets.size();
          if (numTarg != gaggleGooseCombo_.getItemCount()) {
            updateGaggleTargetActions();
          }
          List pending = ss.getPendingCommands();         
          Iterator pit = pending.iterator();
          while (pit.hasNext()) {
            InboundGaggleOp op = (InboundGaggleOp)pit.next();
            op.executeOp();
          }
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    public void setButtonCondition(boolean activate) {
      if (isAMac_) {
        putValue(Action.SMALL_ICON, (activate) ? inbound_ : standard_);
        gaggleButton_.validate();
      } else {
        gaggleButton_.setBackground((activate) ? Color.orange : gaggleButtonOffColor_);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class GaggleConnect extends ChecksForEnabled {
   
    private boolean forConnect_;
    
    GaggleConnect(boolean doIcon, boolean forConnect) {
      forConnect_ = forConnect;
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString((forConnect) ? "command.GaggleConnect" : "command.GaggleDisconnect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString((forConnect) ? "command.GaggleConnect" 
                                                                       : "command.GaggleDisconnect"));        
        URL ugif = getClass().getResource((forConnect) ? "/org/systemsbiology/biotapestry/biofabric/images/C24.gif" : "/org/systemsbiology/biotapestry/biofabric/images/D24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar((forConnect) ? "command.GaggleConnectMnem" : "command.GaggleDisconnectMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
    }

    public void actionPerformed(ActionEvent e) {
      try {
        FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
        if ((goose != null) && goose.isActivated()) {
          if (forConnect_) {
            goose.connect();
          } else {
            goose.disconnect();
          }
        }                  
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    protected boolean checkGuts() {
      FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
      if ((goose != null) && goose.isActivated()) {
        if (forConnect_) {
          return (!goose.isConnected());
        } else {
          return (goose.isConnected());
        }
      }     
      return (false);
    } 
  }
  
  /***************************************************************************
  **
  ** Command
  */
  
  public class AboutAction extends ChecksForEnabled {
    
    private URL aboutURL_;
    private JEditorPane pane_;
    private JFrame frame_;
    private FixedJButton buttonB_;
    private URL gnuUrl_;
    private URL sunUrl_;
        
    AboutAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.About"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.About"));
        URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/images/About24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AboutMnem"); 
        putValue(Action.MNEMONIC_KEY, new Integer(mnem));
      }
      aboutURL_ = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/license/about.html");
    }
    
    //
    // Having an image link in the html turns out to be problematic
    // starting Fall 2008 with URL security holes being plugged.  So
    // change the window.  Note we use a back button now too!
    
    public void actionPerformed(ActionEvent e) {
      try {
        if (frame_ != null) {      
          frame_.setExtendedState(JFrame.NORMAL);
          frame_.toFront();
          return;
        }
        try {
          pane_ = new JEditorPane(aboutURL_);
        } catch (IOException ioex) {
          ExceptionHandler.getHandler().displayException(ioex);
          return;
        }
        // 8/09: COMPLETELY BOGUS, but URLs are breaking everywhere in the latest JVMs, an I don't
        // have time to fix this in a more elegant fashion!
        gnuUrl_ = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/license/LICENSE");
        sunUrl_ = getClass().getResource("/org/systemsbiology/biotapestry/biofabric/license/LICENSE-SUN");
        ResourceManager rMan = ResourceManager.getManager();
        pane_.setEditable(false);
        frame_ = new JFrame(rMan.getString("window.aboutTitle"));
        pane_.addHyperlinkListener(new HyperlinkListener() {
          public void hyperlinkUpdate(HyperlinkEvent ev) {
            try {
              if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL toUse = (ev.getDescription().indexOf("-SUN") != -1) ? sunUrl_ : gnuUrl_;
                pane_.setPage(toUse);
                buttonB_.setEnabled(true);
              }
            } catch (IOException ex) {
            }
          }
        });
        frame_.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            frame_ = null;
            e.getWindow().dispose();
          }
        });
               
        JPanel cp = (JPanel)frame_.getContentPane();
        cp.setBackground(Color.white);   
        cp.setBorder(new EmptyBorder(20, 20, 20, 20));
        cp.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();    
        // URL sugif = getClass().getResource(
        //  "/org/systemsbiology/biotapestry/images/BioTapestrySplash.gif");
        JLabel label = new JLabel(); //new ImageIcon(sugif));        
        
        UiUtil.gbcSet(gbc, 0, 0, 1, 3, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
        cp.add(label, gbc);
        
        JScrollPane jsp = new JScrollPane(pane_);
        UiUtil.gbcSet(gbc, 0, 3, 1, 2, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
        cp.add(jsp, gbc);
        
        
        buttonB_ = new FixedJButton(rMan.getString("dialogs.back"));
        buttonB_.setEnabled(false);
        buttonB_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              pane_.setPage(aboutURL_);
              buttonB_.setEnabled(false);
            } catch (Exception ex) {
              ExceptionHandler.getHandler().displayException(ex);
            }
          }
        });     
        FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
        buttonC.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              frame_.setVisible(false);
              frame_.dispose();
              frame_ = null;
            } catch (Exception ex) {
              ExceptionHandler.getHandler().displayException(ex);
            }
          }
        });
        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(Box.createHorizontalGlue()); 
        buttonPanel.add(buttonB_);
        buttonPanel.add(Box.createHorizontalStrut(10));    
        buttonPanel.add(buttonC);
        UiUtil.gbcSet(gbc, 0, 5, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
        cp.add(buttonPanel, gbc);        
        frame_.setSize(700, 700);
        frame_.setLocationRelativeTo(topWindow_);
        frame_.setVisible(true);
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for building networks
  */ 
    
  public class NetworkBuilder implements BackgroundWorkerOwner {
    
    private BioFabricNetwork.PreBuiltBuildData restore_;
    
    public void doNetworkBuild(BioFabricNetwork.BuildData bfn, boolean isMain) {
      try {
        if (bfn.canRestore()) {
          BioFabricNetwork net = bfp_.getNetwork();
          restore_ = new BioFabricNetwork.PreBuiltBuildData(net, BioFabricNetwork.BUILD_FROM_XML);
        } else {
          restore_ = null;
        }
        preLoadOperations();
        NewNetworkRunner runner = new NewNetworkRunner(bfn, isMain);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netBuild.waitTitle", "netBuild.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedImport(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      BioFabricNetwork.BuildData ubd;
      if (restore_ != null) {
        ubd = restore_;
      } else {
        ubd = new BioFabricNetwork.OrigBuildData(new HashSet(), new HashSet(), colGen_, BioFabricNetwork.BUILD_FROM_SIF);
      }
      try {
        newModelOperations(ubd, true);
      } catch (IOException ioex) {
        //Silent fail
      }
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedImport(result, null);
      return;
    }

    private void finishedImport(Object result, IOException ioEx) {     
      if (ioEx != null) {
        displayFileInputError(ioEx);
        return;                
      }
     // FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
     // if ((goose != null) && goose.isActivated()) {
     //   SelectionSupport ss = goose.getSelectionSupport();
     //   ss.setSpecies(species_);
     // }
      postLoadOperations((BufferedImage)result);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for relayout of networks
  */ 
    
  public class NetworkRelayout implements BackgroundWorkerOwner {
    
    private BioFabricNetwork.PreBuiltBuildData restore_;
       
    public void doNetworkRelayout(BioFabricNetwork.RelayoutBuildData bfn, ClusteredLayout.CRParams result) {
      if (bfn.canRestore()) {
        BioFabricNetwork net = bfp_.getNetwork();
        restore_ = new BioFabricNetwork.PreBuiltBuildData(net, BioFabricNetwork.BUILD_FROM_XML);
      } else {
        restore_ = null;
      }

      try {
        preLoadOperations();
        NetworkRelayoutRunner runner = new NetworkRelayoutRunner(bfn, result);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netRelayout.waitTitle", "netRelayout.wait", null, true);
        if (bfn.getMode() == BioFabricNetwork.REORDER_LAYOUT) {
          bwc.makeSuperChart();
        }
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedImport(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      BioFabricNetwork.BuildData ubd;
      if (restore_ != null) {
        ubd = restore_;
      } else {
        ubd = new BioFabricNetwork.OrigBuildData(new HashSet(), new HashSet(), colGen_, BioFabricNetwork.BUILD_FROM_SIF);
      }
      try {
        newModelOperations(ubd, true);
      } catch (IOException ioex) {
        //Silent fail
      }
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedImport(result, null);
      return;
    }

    private void finishedImport(Object result, IOException ioEx) {     
      if (ioEx != null) {
        displayFileInputError(ioEx);
        return;                
      }
     // FabricGooseInterface goose = FabricGooseManager.getManager().getGoose();
     // if ((goose != null) && goose.isActivated()) {
     //   SelectionSupport ss = goose.getSelectionSupport();
     //   ss.setSpecies(species_);
     // }
      postLoadOperations((BufferedImage)result);
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Class for loading huge files in 
  */ 
    
  public class BackgroundFileReader implements BackgroundWorkerOwner {
    
    private FabricFactory ff_;
    private Exception ex_;
    
    private File file_; 
    private List links_; 
    private Set loneNodes_;
    private FabricSIFLoader.SIFStats sss_;
     
    public void doBackgroundSIFRead(File file, List links, Set loneNodes, Map nameMap, FabricSIFLoader.SIFStats sss) {
      file_ = file;
      links_ = links;
      loneNodes_ = loneNodes;
      sss_ = sss;
      try {       
        SIFReaderRunner runner = new SIFReaderRunner(file, links, loneNodes, nameMap, sss);                                                        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileLoad.waitTitle", "fileLoad.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
  
    public void doBackgroundRead(FabricFactory ff, SUParser sup, File file) {
      ff_ = ff;
      file_ = file;
      try {
        ReaderRunner runner = new ReaderRunner(sup, file);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileLoad.waitTitle", "fileLoad.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        ex_ = remoteEx;
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      throw new UnsupportedOperationException();
    }     
    
    public void cleanUpPostRepaint(Object result) { 
      finishedLoad();
      return;
    }
     
    private void finishedLoad() {     
      if (ex_ != null) {
        displayFileInputError((IOException)ex_);
        return;                
      }      
      if (ff_ != null) {
        setCurrentXMLFile(file_);
        postXMLLoad(ff_, file_.getName());
      } else {
        finishLoadFromSIFSource(file_, sss_, links_, loneNodes_);
      }
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Class for writing huge files out
  */ 
    
  public class BackgroundFileWriter implements BackgroundWorkerOwner {
    
    private Exception ex_;   
    private File file_; 

    public void doBackgroundWrite(File file) {
      file_ = file;
      try {
        WriterRunner runner = new WriterRunner(file);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileWrite.waitTitle", "fileWrite.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        ex_ = remoteEx;
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      throw new UnsupportedOperationException();
    }     
    
    public void cleanUpPostRepaint(Object result) { 
      finishedOut();
      return;
    }
     
    private void finishedOut() {     
      if (ex_ != null) {
        displayFileOutputError();
        return;                
      }     
      setCurrentXMLFile(file_);
      manageWindowTitle(file_.getName());
      return;
    }
  }
 
  /***************************************************************************
  **
  ** Class for recoloring networks
  */ 
    
  public class NetworkRecolor implements BackgroundWorkerOwner {
    
    public void doNetworkRecolor(boolean isMain) {
      try {
        bfp_.shutdown();
        RecolorNetworkRunner runner = new RecolorNetworkRunner(isMain);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netRecolor.waitTitle", "netRecolor.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
      if (remoteEx instanceof IOException) {
        finishedRecolor(null, (IOException)remoteEx);
        return (true);
      }
      return (false);
    }    
        
    public void cleanUpPreEnable(Object result) {
      return;
    }
    
    public void handleCancellation() {
      // Not allowing cancellation!
      return;
    }     
    
    public void cleanUpPostRepaint(Object result) {   
      finishedRecolor(result, null);
      return;
    }

    private void finishedRecolor(Object result, IOException ioEx) {     
      postRecolorOperations((BufferedImage)result);
      return;
    }
  }
  
  /***************************************************************************
  **
  ** Background network import
  */ 
    
  private class NewNetworkRunner extends BackgroundWorker {
 
    private BioFabricNetwork.BuildData bfn_;
    private boolean forMain_;
    
    public NewNetworkRunner(BioFabricNetwork.BuildData bfn, boolean forMain) {
      super("Early Result");      
      bfn_ = bfn;
      forMain_ = forMain;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {
        BufferedImage bi = expensiveModelOperations(bfn_, forMain_);
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Background network layout
  */ 
    
  private class NetworkRelayoutRunner extends BackgroundWorker {
 
    private BioFabricNetwork.RelayoutBuildData rbd_;
    private int mode_;
    private ClusteredLayout.CRParams params_;
    
    public NetworkRelayoutRunner(BioFabricNetwork.RelayoutBuildData rbd, ClusteredLayout.CRParams params) {
      super("Early Result");      
      rbd_ = rbd;
      mode_ = rbd.getMode();
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {            
        switch (mode_) {
          case BioFabricNetwork.DEFAULT_LAYOUT:
          case BioFabricNetwork.NODE_ATTRIB_LAYOUT:
          case BioFabricNetwork.LINK_ATTRIB_LAYOUT:
          case BioFabricNetwork.LINK_GROUP_CHANGE:
            // previously installed....
            break;
          case BioFabricNetwork.REORDER_LAYOUT:
            rbd_.setNodeOrder(doReorderLayout(rbd_.bfn, params_, this, 0.0, 1.0));
            break;            
          case BioFabricNetwork.CLUSTERED_LAYOUT:
            rbd_.setNodeOrder(doClusteredLayout(rbd_.bfn, params_, this, 0.0, 1.0));
            break;
          case BioFabricNetwork.SHADOW_LINK_CHANGE:
          case BioFabricNetwork.BUILD_FOR_SUBMODEL:
          case BioFabricNetwork.BUILD_FROM_XML:
          case BioFabricNetwork.BUILD_FROM_SIF:
          case BioFabricNetwork.BUILD_FROM_GAGGLE:
          default:
            throw new IllegalArgumentException();
        }
        BufferedImage bi = expensiveModelOperations(rbd_, true);
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  } 
  
  /***************************************************************************
  **
  ** Background network recolor
  */ 
    
  private class RecolorNetworkRunner extends BackgroundWorker {
 
    private boolean forMain_;
    
    public RecolorNetworkRunner(boolean forMain) {
      super("Early Result");      
      forMain_ = forMain;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {
        BufferedImage bi = expensiveRecolorOperations(forMain_);
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Background file load
  */ 
    
  private class ReaderRunner extends BackgroundWorker {
   
    private File myFile_;
    private SUParser myParser_;
    
    public ReaderRunner(SUParser sup, File file) {
      super("Early Result");
      myFile_ = file;
      myParser_ = sup;
    }  
    public Object runCore() throws AsynchExitRequestException {
      try {
        myParser_.parse(myFile_);
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    } 
    public Object postRunCore() {
      return (null);
    } 
  }  
  
   /***************************************************************************
  **
  ** Background file load
  */ 
    
  private class SIFReaderRunner extends BackgroundWorker {
   
    private File myFile_;
    private List links_;
    private Set loneNodes_;
    private Map nameMap_;
    private FabricSIFLoader.SIFStats sss_;
    
    public SIFReaderRunner(File file, List links, Set loneNodes, Map nameMap, FabricSIFLoader.SIFStats sss) {
      super("Early Result");
      myFile_ = file;
      links_ = links;
      loneNodes_ = loneNodes;
      nameMap_ = nameMap;
      sss_ = sss;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {
        FabricSIFLoader.SIFStats sss = (new FabricSIFLoader()).readSIF(myFile_, links_, loneNodes_, nameMap_);
        sss_.copyInto(sss);
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    } 
    public Object postRunCore() {
      return (null);
    } 
  } 
    
  /***************************************************************************
  **
  ** Background file write
  */ 
    
  private class WriterRunner extends BackgroundWorker {
   
    private File myFile_;
    
    public WriterRunner(File file) {
      super("Early Result");
      myFile_ = file;
    }  
    public Object runCore() throws AsynchExitRequestException {
      try {
        saveToOutputStream(new FileOutputStream(myFile_));
        return (new Boolean(true));
      } catch (IOException ioe) {
        stashException(ioe);
        return (null);
      }
    } 
    public Object postRunCore() {
      return (null);
    } 
  } 
}
