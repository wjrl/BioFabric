/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

package org.systemsbiology.biofabric.cmd;

import java.awt.Color;
import java.awt.Dimension;
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import org.systemsbiology.biofabric.analysis.NetworkAlignment;
//import org.freehep.graphics2d.VectorGraphics;
//import org.freehep.graphicsio.PageConstants;
//import org.freehep.graphicsio.pdf.PDFGraphics2D;
//import org.freehep.graphicsio.ps.PSGraphics2D;
import org.systemsbiology.biofabric.app.BioFabricApplication;
import org.systemsbiology.biofabric.app.BioFabricWindow;
import org.systemsbiology.biofabric.event.EventManager;
import org.systemsbiology.biofabric.event.SelectionChangeEvent;
import org.systemsbiology.biofabric.event.SelectionChangeListener;
import org.systemsbiology.biofabric.io.AttributeLoader;
import org.systemsbiology.biofabric.io.FabricFactory;
import org.systemsbiology.biofabric.io.FabricSIFLoader;
import org.systemsbiology.biofabric.layouts.NodeClusterLayout;
import org.systemsbiology.biofabric.layouts.NodeSimilarityLayout;
import org.systemsbiology.biofabric.layouts.ControlTopLayout;
import org.systemsbiology.biofabric.layouts.DefaultLayout;
import org.systemsbiology.biofabric.layouts.HierDAGLayout;
import org.systemsbiology.biofabric.layouts.WorldBankLayout;
import org.systemsbiology.biofabric.model.BioFabricNetwork;
import org.systemsbiology.biofabric.model.FabricLink;
import org.systemsbiology.biofabric.parser.ParserClient;
import org.systemsbiology.biofabric.parser.SUParser;
import org.systemsbiology.biofabric.ui.FabricColorGenerator;
import org.systemsbiology.biofabric.ui.FabricDisplayOptionsManager;
import org.systemsbiology.biofabric.ui.ImageExporter;
import org.systemsbiology.biofabric.ui.dialogs.BreadthFirstLayoutDialog;
import org.systemsbiology.biofabric.ui.dialogs.ClusterLayoutSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.NodeSimilarityLayoutSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.CompareNodesSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.ExportSettingsDialog;
import org.systemsbiology.biofabric.ui.dialogs.ExportSettingsPublishDialog;
import org.systemsbiology.biofabric.ui.dialogs.FabricDisplayOptionsDialog;
import org.systemsbiology.biofabric.ui.dialogs.FabricSearchDialog;
import org.systemsbiology.biofabric.ui.dialogs.LinkGroupingSetupDialog;
import org.systemsbiology.biofabric.ui.dialogs.NetworkAlignmentDialog;
import org.systemsbiology.biofabric.ui.dialogs.RelationDirectionDialog;
import org.systemsbiology.biofabric.ui.dialogs.ReorderLayoutParamsDialog;
import org.systemsbiology.biofabric.ui.display.BioFabricPanel;
import org.systemsbiology.biofabric.ui.display.FabricMagnifyingTool;
import org.systemsbiology.biofabric.ui.render.BucketRenderer;
import org.systemsbiology.biofabric.ui.render.BufferBuilder;
import org.systemsbiology.biofabric.util.AsynchExitRequestException;
import org.systemsbiology.biofabric.util.BTProgressMonitor;
import org.systemsbiology.biofabric.util.BackgroundWorker;
import org.systemsbiology.biofabric.util.BackgroundWorkerClient;
import org.systemsbiology.biofabric.util.BackgroundWorkerOwner;
import org.systemsbiology.biofabric.util.ExceptionHandler;
import org.systemsbiology.biofabric.util.FileExtensionFilters;
import org.systemsbiology.biofabric.util.FixedJButton;
import org.systemsbiology.biofabric.util.Indenter;
import org.systemsbiology.biofabric.util.InvalidInputException;
import org.systemsbiology.biofabric.util.NID;
import org.systemsbiology.biofabric.util.ResourceManager;
import org.systemsbiology.biofabric.util.UiUtil;
import org.systemsbiology.biofabric.util.UniqueLabeller;
import org.systemsbiology.biotapestry.biofabric.FabricCommands;

/****************************************************************************
**
** Collection of primary commands for the application
*/

public class CommandSet implements ZoomChangeTracker, SelectionChangeListener, FabricDisplayOptionsManager.DisplayOptionTracker {
  
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
  
  // Former Gaggle Commands 17-24 dropped
  
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
  public static final int LAYOUT_VIA_NODE_CLUSTER_ASSIGN = 46;
  public static final int PRINT_PDF                    = 47;
  public static final int SHOW_TOUR                    = 48;
  public static final int SHOW_NAV_PANEL               = 49;
  public static final int LAYOUT_TOP_CONTROL           = 50;
  public static final int HIER_DAG_LAYOUT              = 51;
  public static final int WORLD_BANK_LAYOUT            = 52;
  public static final int LOAD_WITH_EDGE_WEIGHTS       = 53;
  public static final int LAYOUT_NETWORK_ALIGNMENT     = 54;
  
  
  
  public static final int GENERAL_PUSH   = 0x01;
  public static final int ALLOW_NAV_PUSH = 0x02;
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private static HashMap<String, CommandSet> perClass_;
  private BioFabricWindow topWindow_;
  private BioFabricApplication bfa_;
  private BioFabricPanel bfp_;
  private File currentFile_;
  private boolean isAMac_;
  private boolean isForMain_;
  private boolean showNav_;
  
  private HashMap<Integer, ChecksForEnabled> withIcons_;
  private HashMap<Integer, ChecksForEnabled> noIcons_;
  private FabricColorGenerator colGen_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  /***************************************************************************
  **
  ** Needed for Cytoscape app support
  */ 
  
  public BioFabricWindow getBFW() {
    return (topWindow_);
  }
   
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
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.checkIfEnabled();
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.checkIfEnabled();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Push a disabled condition
  */ 
  
  public void pushDisabled(int pushCondition) {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.pushDisabled(pushCondition);
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
      cfe.pushDisabled(pushCondition);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Pop the disabled condition
  */ 
  
   public void popDisabled() {
    Iterator<ChecksForEnabled> wiit = withIcons_.values().iterator();
    while (wiit.hasNext()) {
      ChecksForEnabled cfe = wiit.next();
      cfe.popDisabled();
    }
    Iterator<ChecksForEnabled> niit = noIcons_.values().iterator();
    while (niit.hasNext()) {
      ChecksForEnabled cfe = niit.next();
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
          new BioFabricNetwork.PreBuiltBuildData(bfn, BioFabricNetwork.BuildMode.SHADOW_LINK_CHANGE);
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
    HashMap<Integer, ChecksForEnabled> useMap = (withIcon) ? withIcons_ : noIcons_;
    Integer actionKeyObject = Integer.valueOf(actionKey);
    ChecksForEnabled retval = useMap.get(actionKeyObject);
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
          retval = new ImportSIFAction(withIcon, false); 
          break;
        case LOAD_WITH_EDGE_WEIGHTS:
          retval = new ImportSIFAction(withIcon, true); 
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
        case PRINT_PDF:
          retval = new PrintPDFAction(withIcon); 
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
        case HIER_DAG_LAYOUT:
          retval = new HierDAGLayoutAction(withIcon); 
          break;
        case RELAYOUT_USING_SHAPE_MATCH:
          retval = new LayoutViaShapeMatchAction(withIcon); 
          break;  
        case SET_LINK_GROUPS:
          retval = new SetLinkGroupsAction(withIcon);
          break;  
        case LAYOUT_NETWORK_ALIGNMENT:
          retval = new LayoutNetworkAlignment(withIcon);
          break;
        case COMPARE_NODES:
          retval = new CompareNodesAction(withIcon);           
          break;
        case LAYOUT_VIA_NODE_CLUSTER_ASSIGN:
          retval = new LayoutViaNodeClusterAction(withIcon); 
          break;
        case LAYOUT_TOP_CONTROL:
          retval = new LayoutTopControlAction(withIcon); 
          break;          
        case SHOW_TOUR:
          retval = new ToggleShowTourAction(withIcon); 
          break;
        case SHOW_NAV_PANEL:
          retval = new ToggleShowNavPanelAction(withIcon); 
          break;
        case WORLD_BANK_LAYOUT:
          retval = new WorldBankLayoutAction(withIcon); 
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
    
  private boolean loadFromSifSource(File file, Map<AttributeLoader.AttributeKey, String> nameMap, Integer magBins, 
  		                              UniqueLabeller idGen) {  
    ArrayList<FabricLink> links = new ArrayList<FabricLink>();
    HashSet<NID.WithName> loneNodes = new HashSet<NID.WithName>();
    HashMap<String, String> nodeNames = null;
    if (nameMap != null) {
      nodeNames = new HashMap<String, String>();
      for (AttributeLoader.AttributeKey key : nameMap.keySet()) {
        nodeNames.put(((AttributeLoader.StringKey)key).key, nameMap.get(key));
      }
    }
    
    TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
    FabricSIFLoader.SIFStats sss;
    if (file.length() > 500000) {
      sss = new FabricSIFLoader.SIFStats();
      BackgroundFileReader br = new BackgroundFileReader();
      br.doBackgroundSIFRead(file, idGen, links, loneNodes, nodeNames, sss, magBins, relMap);
      return (true);
    } else {
      try { 
        sss = (new FabricSIFLoader()).readSIF(file, idGen, links, loneNodes, nodeNames, magBins, null, 0.0, 0.0);
        BioFabricNetwork.extractRelations(links, relMap, null, 0.0, 0.0);
        return (finishLoadFromSIFSource(file, idGen, sss, links, loneNodes, (magBins != null), relMap));
      } catch (AsynchExitRequestException axex) {
      	// Should never happen
        return (false);              
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
    
  private boolean finishLoadFromSIFSource(File file, UniqueLabeller idGen, FabricSIFLoader.SIFStats sss, 
  		                                    List<FabricLink> links, Set<NID.WithName> loneNodeIDs, 
  		                                    boolean binMag, SortedMap<FabricLink.AugRelation, Boolean> relaMap) {
    ResourceManager rMan = ResourceManager.getManager();
    try {
      if (!sss.badLines.isEmpty()) {        
        String badLineFormat = rMan.getString("fabricRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(sss.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("fabricRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      
      RelationDirectionDialog rdd = new RelationDirectionDialog(topWindow_, relaMap);
      rdd.setVisible(true);
      if (!rdd.haveResult()) {
        return (false);
      }
      if (rdd.getFromFile()) {
        File fileEda = getTheFile(".rda", ".txt", "AttribDirectory", "filterName.rda");
        if (fileEda == null) {
          return (true);
        }
        Map<AttributeLoader.AttributeKey, String> relAttributes = loadTheFile(fileEda, null, true);  // Use the simple a = b format of node attributes
        if (relAttributes == null) {
          return (true);
        }
        
        HashSet<FabricLink.AugRelation> needed = new HashSet<FabricLink.AugRelation>(relaMap.keySet());
      
        boolean tooMany = false;
        Iterator<AttributeLoader.AttributeKey> rit = relAttributes.keySet().iterator();
        while (rit.hasNext()) {
          AttributeLoader.StringKey sKey = (AttributeLoader.StringKey)rit.next();
          String key = sKey.key;
          String val = relAttributes.get(sKey);
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
      
      HashSet<FabricLink> reducedLinks = new HashSet<FabricLink>();
      HashSet<FabricLink> culledLinks = new HashSet<FabricLink>();
      PreprocessNetwork pn = new PreprocessNetwork();
      pn.doNetworkPreprocess(links, relaMap, reducedLinks, culledLinks);
        
      if (!culledLinks.isEmpty()) {
        String dupLinkFormat = rMan.getString("fabricRead.dupLinkFormat");
        // Ignore shadow link culls: / 2
        String dupLinkMsg = MessageFormat.format(dupLinkFormat, new Object[] {Integer.valueOf(culledLinks.size() / 2)});
        JOptionPane.showMessageDialog(topWindow_, dupLinkMsg,
                                      rMan.getString("fabricRead.dupLinkTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      
      //
      // Handle magnitude bins:
      //
      
      if (binMag) {
      	HashSet<FabricLink> binnedLinks = new HashSet<FabricLink>();
        Pattern p = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
        
        Iterator<FabricLink> alit = reducedLinks.iterator();
        while (alit.hasNext()) {
          FabricLink nextLink = alit.next();
          FabricLink.AugRelation rel = nextLink.getAugRelation();
          Matcher m = p.matcher(rel.relation);
          int magCount = 0;
          if (m.find()) {      
            double d = Double.parseDouble(m.group(0));
            magCount = (int)Math.floor((Math.abs(d) * 10.0) - 5.0);
          }
          /*
          for (int k = 0; k < magCount; k++) {
        	  String suf = ":" + Integer.toString(k);
	          FabricLink nextLink = new FabricLink(source, target, rel + suf, false);
	        links.add(nextLink);
	        // We never create shadow feedback links!
	        if (!source.equals(target)) {
	          FabricLink nextShadowLink = new FabricLink(source, target, rel + suf, true);
	          links.add(nextShadowLink);
	        }*/       
        }
      }
            System.out.println("realyBuilData " + System.currentTimeMillis());
      BioFabricNetwork.RelayoutBuildData bfn = new BioFabricNetwork.RelayoutBuildData(idGen, reducedLinks, loneNodeIDs,
                                                                                      new HashMap<NID.WithName, String>(), colGen_,                                                               
                                                                                      BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
      NetworkBuilder nb = new NetworkBuilder(); 
            System.out.println("do network build " + System.currentTimeMillis());
      nb.doNetworkBuild(bfn, true);            
    } catch (OutOfMemoryError oom) {
      ExceptionHandler.getHandler().displayOutOfMemory(oom);
      return (false);  
    }
    currentFile_ = null;
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
    manageWindowTitle(file.getName());
    return (true);
  }  

  /***************************************************************************
  **
  ** Preprocess ops that are either run in forground or background:
  */ 
    
  private void preprocess(List<FabricLink> links, 
  		                    SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                      Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks,  
  	                      BTProgressMonitor monitor, double startFrac, double endFrac) 
  		                      throws AsynchExitRequestException {
    double midFrac = startFrac + ((endFrac - startFrac) / 3.0); // First part maybe 33% of total...
    BioFabricNetwork.assignDirections(links, relaMap, monitor, startFrac, midFrac);
    BioFabricNetwork.preprocessLinks(links, reducedLinks, culledLinks, monitor, midFrac, endFrac);
    return;
  }  
    
  /***************************************************************************
  **
  ** Common load operations.
  */ 
    
  private boolean loadXMLFromSource(File file) {  
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
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
    BioFabricNetwork.PreBuiltBuildData pbd = new BioFabricNetwork.PreBuiltBuildData(bfn, BioFabricNetwork.BuildMode.BUILD_FROM_XML);
    NetworkBuilder nb = new NetworkBuilder(); 
    nb.doNetworkBuild(pbd, true);
    manageWindowTitle(fileName);
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
    String filename = FabricCommands.getPreference(prefTag);
    while (file == null) {
      JFileChooser chooser = new JFileChooser();       
      FileFilter filter;
      if (ext2 == null) {
        filter = new FileExtensionFilters.SimpleFilter(ext1, desc);
      } else {
        filter = new FileExtensionFilters.DoubleExtensionFilter(ext1, ext2, desc);
      }     
      chooser.addChoosableFileFilter(filter);
      chooser.setAcceptAllFileFilterUsed(true);
      chooser.setFileFilter(filter);
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
  ** Load the file. Map keys are strings or Links
  */
     
  public Map<AttributeLoader.AttributeKey, String> loadTheFile(File file, Map<String, Set<NID.WithName>> nameToIDs, boolean forNodes) {
    HashMap<AttributeLoader.AttributeKey, String> attributes = new HashMap<AttributeLoader.AttributeKey, String>();   
    try {    
      AttributeLoader.ReadStats stats = new AttributeLoader.ReadStats();
      AttributeLoader alod = new AttributeLoader(); 
      Map<String, NID.WithName> nameToID = (!forNodes) ? BioFabricNetwork.reduceNameSetToOne(nameToIDs) : null;
      alod.readAttributes(file, forNodes, attributes, nameToID, stats);
      if (!stats.badLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String badLineFormat = rMan.getString("attribRead.badLineFormat");
        String badLineMsg = MessageFormat.format(badLineFormat, new Object[] {Integer.valueOf(stats.badLines.size())});
        JOptionPane.showMessageDialog(topWindow_, badLineMsg,
                                      rMan.getString("attribRead.badLineTitle"),
                                      JOptionPane.WARNING_MESSAGE);
      }
      if (!stats.dupLines.isEmpty()) {
        ResourceManager rMan = ResourceManager.getManager();
        String dupLineFormat = rMan.getString("attribRead.dupLineFormat");
        String dupLineMsg = MessageFormat.format(dupLineFormat, new Object[] {Integer.valueOf(stats.dupLines.size())});
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
    FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());
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

  public BufferedImage expensiveModelOperations(BioFabricNetwork.BuildData bfnbd, 
  		                                          boolean forMain, 
  		                                          BTProgressMonitor monitor, 
                                                double startFrac, 
                                                double endFrac) throws IOException, AsynchExitRequestException {
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(600, 800);
    System.out.println("Expensive in " + System.currentTimeMillis());
     screenSize.setSize((int)(screenSize.getWidth() * 1.0), (int)(screenSize.getHeight() * 1.0));
  //  screenSize.setSize((int)(screenSize.getWidth() * 0.8), (int)(screenSize.getHeight() * 0.4));
    double midFrac = (startFrac + endFrac) / 2.0;
    // Possibly expensive network analysis preparation:
    BioFabricNetwork bfn = new BioFabricNetwork(bfnbd, monitor, startFrac, midFrac);
    System.out.println("BFN Expensive done " + System.currentTimeMillis());
    // Possibly expensive display object creation:
    bfp_.installModel(bfn, monitor, 0.0, midFrac); 
     System.out.println("Model installed Expensive done " + System.currentTimeMillis());
    // Very expensive display buffer creation:
    int[] preZooms = BufferBuilder.calcImageZooms(bfn);
    bfp_.zoomForBuf(preZooms, screenSize);
    BufferedImage topImage = null;
     System.out.println("Buffer Builder started " + System.currentTimeMillis());
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 100, bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildBufs(preZooms, bfp_, 25, monitor, 0.0, endFrac);
       System.out.println("Buffs Built started " + System.currentTimeMillis());
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_, bfp_.getBucketRend());
      topImage = bb.buildOneBuf(preZooms);      
      bfp_.setBufBuilder(null);
    }
    System.out.println("Expensive out " + System.currentTimeMillis());
    Runtime.getRuntime().gc();
    return (topImage);
  }

  /***************************************************************************
  **
  ** Do new model operations
  */ 

  public BufferedImage expensiveRecolorOperations(boolean forMain,
  		                                            BTProgressMonitor monitor, 
                                                  double startFrac, 
                                                  double endFrac) throws IOException, AsynchExitRequestException {
    Dimension screenSize = (forMain) ? Toolkit.getDefaultToolkit().getScreenSize() : new Dimension(800, 400);
    screenSize.setSize((int)(screenSize.getWidth() * 0.8), (int)(screenSize.getHeight() * 0.4));
    colGen_.newColorModel();
    bfp_.changePaint();
    BioFabricNetwork bfn = bfp_.getNetwork();
    int[] preZooms = BufferBuilder.calcImageZooms(bfn);
    bfp_.zoomForBuf(preZooms, screenSize);
    BufferedImage topImage = null;
    if (forMain) {
      BufferBuilder bb = new BufferBuilder(null, 100, bfp_, bfp_.getBucketRend(), bfp_.getBufImgStack());
      topImage = bb.buildBufs(preZooms, bfp_, 24, monitor, startFrac, endFrac);
      bfp_.setBufBuilder(bb);      
    } else {
      BufferBuilder bb = new BufferBuilder(bfp_, bfp_.getBucketRend());
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
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldScreen());
    return;
  }
   
  /***************************************************************************
  **
  ** Handles post-loading operations
  */ 
       
  public void postLoadOperations(BufferedImage topImage) {
    topWindow_.getOverview().installImage(topImage, bfp_.getWorldScreen());
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
    try {
      BufferedImage topImage = expensiveModelOperations(bfnbd, forMain, null, 0.0, 0.0);
      postLoadOperations(topImage);
    } catch (AsynchExitRequestException aex) {
    	// Not being used in background; will not happen
    }
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
      String dirName = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
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
      outMsg = MessageFormat.format(format, new Object[] {Integer.valueOf(lineno + 1), keyedErr});
    } else if (haveKey && !haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageWithDesc");
      String keyedErr = rMan.getString("invalidInput." + errKey);
      outMsg = MessageFormat.format(format, new Object[] {keyedErr});
    } else if (!haveKey && haveLine) {
      String format = rMan.getString("fileRead.inputErrorMessageForLine");
      outMsg = MessageFormat.format(format, new Object[] {Integer.valueOf(lineno + 1)});      
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

    InOutZoomAction zaOutWI = (InOutZoomAction)withIcons_.get(Integer.valueOf(ZOOM_OUT));
    InOutZoomAction zaOutNI = (InOutZoomAction)noIcons_.get(Integer.valueOf(ZOOM_OUT));
    InOutZoomAction zaInWI = (InOutZoomAction)withIcons_.get(Integer.valueOf(ZOOM_IN));
    InOutZoomAction zaInNI = (InOutZoomAction)noIcons_.get(Integer.valueOf(ZOOM_IN));
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
  ** Set the current file
  */ 
    
  public void setCurrentXMLFile(File file) {
    currentFile_ = file;
    if (currentFile_ == null) {
      return;
    }
    FabricCommands.setPreference("LoadDirectory", file.getAbsoluteFile().getParent());
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

  public static synchronized CommandSet getCmds(String className) {
    if (perClass_ == null) {
      throw new IllegalStateException();
    }
    CommandSet fc = perClass_.get(className);
    if (fc == null) {
      throw new IllegalStateException();
    }
    return (fc);
  }  
 
  /***************************************************************************
  ** 
  ** Init the commands for the given tag
  */

  public static synchronized CommandSet initCmds(String className, BioFabricApplication bfa, 
                                                     BioFabricWindow topWindow, boolean isForMain) {
    if (perClass_ == null) {
      perClass_ = new HashMap<String, CommandSet>();
    }
    CommandSet fc = perClass_.get(className);
    if (fc != null) {
      throw new IllegalStateException();
    }
    fc = new CommandSet(bfa, topWindow, isForMain);
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
  
  private CommandSet(BioFabricApplication bfa, BioFabricWindow topWindow, boolean isMain) {
    bfa_ = bfa;
    topWindow_ = topWindow;
    withIcons_ = new HashMap<Integer, ChecksForEnabled>();
    noIcons_ = new HashMap<Integer, ChecksForEnabled>();
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
    
    private static final long serialVersionUID = 1L;
    
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
    
    private static final long serialVersionUID = 1L;
    
    PropagateDownAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.PropagateDown"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.PropagateDown"));      
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/PropagateSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PropagateDownMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    ZoomToSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToAllFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    ZoomToModelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToModel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToModel"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToAllFabric24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToModelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentMouse(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMouse"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMouse"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMouseMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    ZoomToCurrentMagnify(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToCurrentMagnify"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToCurrentMagnify"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToCurrentMagnifyMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    CenterOnNextSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnNextSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnNextSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Forward24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnNextSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    CenterOnPreviousSelected(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CenterOnPreviousSelected"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CenterOnPreviousSelected"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Back24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CenterOnPreviousSelectedMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    AddFirstNeighborsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.AddFirstNeighbors"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.AddFirstNeighbors"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/PlusOneDeg24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AddFirstNeighborsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
    
    BuildSelectAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.BuildSelect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.BuildSelect"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.BuildSelectMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
   
    private static final long serialVersionUID = 1L;
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
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/" + iconName);  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemName); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
      char accel = rMan.getChar(accelName);
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
      	System.out.println("ZOOM in: " + Runtime.getRuntime().freeMemory());
        bfp_.getZoomController().bumpZoomWrapper(sign_);
        System.out.println("ZOOM out: " + Runtime.getRuntime().freeMemory());
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
      return ((pushCondition & CommandSet.ALLOW_NAV_PUSH) == 0x00);
    }     
    
  }
    
  /***************************************************************************
  **
  ** Command
  */ 
  
  private class ClearSelectionsAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
   
    ClearSelectionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ClearSel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ClearSel"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ClearFabricSelected24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ClearSelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    private static final long serialVersionUID = 1L;
   
    ZoomToRect(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ZoomToRect"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ZoomToRect"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/ZoomToFabricRect24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ZoomToRectMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
    
    private static final long serialVersionUID = 1L;
   
    CancelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CancelAddMode"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CancelAddMode"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CancelAddModeMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
      return ((pushCondition & CommandSet.ALLOW_NAV_PUSH) != 0x00);
    }    
      
  }

  /***************************************************************************
  **
  ** Command
  */ 
  
  private class SearchAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    SearchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Search"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Search"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Find24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SearchMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
          Set<NID.WithName> matches = fsd.getMatches();
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
    
    private static final long serialVersionUID = 1L;
    
    CompareNodesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.CompareNodes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.CompareNodes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CompareNodesMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        Set<NID.WithName> allNodeIDs = bfp_.getNetwork().getNodeSetIDs();
        Map<String, Set<NID.WithName>> normNameToIDs = bfp_.getNetwork().getNormNameToIDs();
        CompareNodesSetupDialog fsd = new CompareNodesSetupDialog(topWindow_, allNodeIDs, normNameToIDs);      
        fsd.setVisible(true);
        if (fsd.haveResult()) {
          Set<NID.WithName> result = fsd.getResults();
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
    
    private static final long serialVersionUID = 1L;
    
    CloseAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Close"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Close"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Stop24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.CloseMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaAttributesAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));       
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    private static final long serialVersionUID = 1L;
    
    LayoutNodesViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutNodesViaAttributes", "command.LayoutNodesViaAttributesMnem");
    }
    
    protected boolean performOperation() {
      File file = getTheFile(".noa", ".na", "AttribDirectory", "filterName.noa");
      if (file == null) {
        return (true);
      }
      Map<AttributeLoader.AttributeKey, String> nodeAttributes = loadTheFile(file, null, true);
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
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.NODE_ATTRIB_LAYOUT);
      bfn.setNodeOrderFromAttrib(nodeAttributes);
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
       
    private static final long serialVersionUID = 1L;
    
    LayoutLinksViaAttributesAction(boolean doIcon) {
      super(doIcon, "command.LayoutLinksViaAttributes", "command.LayoutLinksViaAttributesMnem");
    }
    
    protected boolean performOperation() {   
      File file = getTheFile(".eda", ".ed", "AttribDirectory", "filterName.eda");
      if (file == null) {
        return (true);
      }
      Map<String, Set<NID.WithName>> nameToIDs = bfp_.getNetwork().getNormNameToIDs();
      Map<AttributeLoader.AttributeKey, String> edgeAttributes = loadTheFile(file, nameToIDs, false);
      if (edgeAttributes == null) {
        return (true);
      }
      SortedMap<Integer, FabricLink> modifiedAndChecked = bfp_.getNetwork().checkNewLinkOrder(edgeAttributes);      
      if (modifiedAndChecked == null) { 
        ResourceManager rMan = ResourceManager.getManager();
        JOptionPane.showMessageDialog(topWindow_, rMan.getString("attribRead.badColMessage"),
                                      rMan.getString("attribRead.badColSemanticsTitle"),
                                      JOptionPane.WARNING_MESSAGE);
        return (true);
      }
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.LINK_ATTRIB_LAYOUT);
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
   
  private class ToggleShowTourAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    private boolean showTour_;
    
    ToggleShowTourAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ShowTourAction"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ShowTourAction"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ShowTourActionMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      showTour_ = true;
    }
    
    public void actionPerformed(ActionEvent ev) {
      showTour_ = !showTour_;
      topWindow_.showTour(showTour_);
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (showNav_);
    }   
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ToggleShowNavPanelAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    ToggleShowNavPanelAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.ShowNavPanel"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.ShowNavPanel"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.ShowNavPanelMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      showNav_ = true;
    }
    
    public void actionPerformed(ActionEvent ev) {
      showNav_ = !showNav_;
      topWindow_.showNavAndControl(showNav_);
      checkForChanges(); // To disable/enable tour hiding
      return;
    }
  }  

  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaNodeClusterAction extends LayoutViaAttributesAction {
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaNodeClusterAction(boolean doIcon) {
      super(doIcon, "command.LayoutViaNodeClusterAction", "command.LayoutViaNodeClusterAction");
    }
    
    protected boolean performOperation() {
    	Set<NID.WithName> sels = bfp_.getNodeSelections();
      NID.WithName selNode = (sels.size() == 1) ? sels.iterator().next() : null;
    	
    	ClusterLayoutSetupDialog clsd = new ClusterLayoutSetupDialog(topWindow_, bfp_.getNetwork(), selNode);
      clsd.setVisible(true);      
      if (clsd.haveResult()) {
        NodeClusterLayout.ClusterParams params = clsd.getParams();
        if (params.needsFile()) {
        	if (!ClusterLayoutSetupDialog.askForFileInfo(params, CommandSet.this, bfp_.getNetwork())) {
        		return (true);
        	}
        }        
        BioFabricNetwork.RelayoutBuildData bfn = 
          new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.NODE_CLUSTER_LAYOUT);
        NetworkRelayout nb = new NetworkRelayout(); 
        nb.doNetworkRelayout(bfn, params);
      }
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
   
  private class LayoutTopControlAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    LayoutTopControlAction(boolean doIcon) {
      super(doIcon, "command.TopControlLayout", "command.TopControlLayoutMnem", BioFabricNetwork.BuildMode.CONTROL_TOP_LAYOUT);
    }
  }  
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class HierDAGLayoutAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    HierDAGLayoutAction(boolean doIcon) {
      super(doIcon, "command.HierDAGLayout", "command.HierDAGLayoutMnem", BioFabricNetwork.BuildMode.HIER_DAG_LAYOUT);
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class WorldBankLayoutAction extends BasicLayoutAction {
     
    private static final long serialVersionUID = 1L;
    
    WorldBankLayoutAction(boolean doIcon) {
      super(doIcon, "command.WorldBankLayout", "command.WorldBankLayoutMnem", BioFabricNetwork.BuildMode.WORLD_BANK_LAYOUT);
    }
  }

  /***************************************************************************
  **
  ** Command
  */ 
   
  private class DefaultLayoutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    
    DefaultLayoutAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.DefaultLayout"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.DefaultLayout"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.DefaultLayoutMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
        
    public void actionPerformed(ActionEvent e) {
      try {
        Set<NID.WithName> sels = bfp_.getNodeSelections();
        NID.WithName selNode = (sels.size() == 1) ? sels.iterator().next() : null;
        BreadthFirstLayoutDialog bfl = new BreadthFirstLayoutDialog(topWindow_, selNode, topWindow_.getFabricPanel().getNetwork());
        bfl.setVisible(true);
           
        if (bfl.haveResult()) {
          DefaultLayout.Params params = bfl.getParams();
          BioFabricNetwork.RelayoutBuildData bfn = 
            new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.DEFAULT_LAYOUT);
          NetworkRelayout nb = new NetworkRelayout(); 
          nb.doNetworkRelayout(bfn, params); 
        }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
    }
    
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private abstract class BasicLayoutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    private BioFabricNetwork.BuildMode bMode_;
    
    BasicLayoutAction(boolean doIcon, String nameTag, String mnemTag, BioFabricNetwork.BuildMode bMode) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(nameTag));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(nameTag));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar(mnemTag); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      bMode_ = bMode;
    }
        
    public void actionPerformed(ActionEvent e) {
      try {
        BioFabricNetwork.RelayoutBuildData bfn = new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), bMode_);
        (new NetworkRelayout()).doNetworkRelayout(bfn, null); 
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }      
      return;
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
     
    private static final long serialVersionUID = 1L;
    
    LayoutViaConnectivityAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaConnectivity"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaConnectivity"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaConnectivityMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    private boolean performOperation(Object[] args) {
          
      NodeSimilarityLayoutSetupDialog clpd = 
        new NodeSimilarityLayoutSetupDialog(topWindow_, new NodeSimilarityLayout.ClusterParams());  
  
      clpd.setVisible(true);
      if (!clpd.haveResult()) {
        return (false);
      }
  
      NodeSimilarityLayout.ClusterParams result = clpd.getParams();     
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.CLUSTERED_LAYOUT);
      NetworkRelayout nb = new NetworkRelayout(); 
      nb.doNetworkRelayout(bfn, result);        
      return (true);   
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel() && (bfp_.getNetwork().getLinkCount(true) != 0));
    }   
  }
  
   /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutViaShapeMatchAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;

    LayoutViaShapeMatchAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LayoutViaShapeMatch"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutViaShapeMatch"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutViaShapeMatchMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    private boolean performOperation(Object[] args) {
      
      ReorderLayoutParamsDialog clpd = 
        new ReorderLayoutParamsDialog(topWindow_, new NodeSimilarityLayout.ResortParams());  
  
      clpd.setVisible(true);
      if (!clpd.haveResult()) {
        return (false);
      }
  
      NodeSimilarityLayout.ResortParams result = clpd.getParams();        
      BioFabricNetwork.RelayoutBuildData bfn = 
        new BioFabricNetwork.RelayoutBuildData(bfp_.getNetwork(), BioFabricNetwork.BuildMode.REORDER_LAYOUT);
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
      
    private static final long serialVersionUID = 1L;
    
    SetLinkGroupsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetLinkGroups"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetLinkGroups"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetLinkGroupsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    private boolean performOperation(Object[] args) {
          
      BioFabricNetwork bfn = bfp_.getNetwork();
      List<String> currentTags = bfn.getLinkGroups();
      ArrayList<FabricLink> links = new ArrayList<FabricLink>(bfn.getAllLinks(true));
      TreeMap<FabricLink.AugRelation, Boolean> relMap = new TreeMap<FabricLink.AugRelation, Boolean>();
      try {
        BioFabricNetwork.extractRelations(links, relMap, null, 0.0, 0.0);
      } catch (AsynchExitRequestException aerx) {
      	// Should not happen...
      }
      Set<FabricLink.AugRelation> allRelations = relMap.keySet(); 
      LinkGroupingSetupDialog lgsd = new LinkGroupingSetupDialog(topWindow_, currentTags, allRelations, bfn);
      lgsd.setVisible(true);
      if (!lgsd.haveResult()) {
        return (false);
      }

      BioFabricNetwork.LayoutMode mode = lgsd.getChosenMode();

      BioFabricNetwork.BuildMode bmode; 
      if (mode == BioFabricNetwork.LayoutMode.PER_NODE_MODE) {
        bmode = BioFabricNetwork.BuildMode.GROUP_PER_NODE_CHANGE;
      } else if (mode == BioFabricNetwork.LayoutMode.PER_NETWORK_MODE) {
      	bmode = BioFabricNetwork.BuildMode.GROUP_PER_NETWORK_CHANGE;
      } else {
        throw new IllegalStateException();
      }
      
      BioFabricNetwork.RelayoutBuildData bfnd = new BioFabricNetwork.RelayoutBuildData(bfn, bmode);
      bfnd.setGroupOrderAndMode(lgsd.getGroups(), mode);

      NetworkRelayout nb = new NetworkRelayout();
      nb.doNetworkRelayout(bfnd, null);

      return (true);
    }

    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LayoutNetworkAlignment extends ChecksForEnabled {
  
    private static final long serialVersionUID = 1L;
    
    LayoutNetworkAlignment(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager();
      putValue(Action.NAME, rMan.getString("command.LayoutNetworkAlignment"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LayoutNetworkAlignment"));
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LayoutNetworkAlignmentMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
  
    private boolean performOperation(Object[] args) {
  
      NetworkAlignmentDialog nad = new NetworkAlignmentDialog(topWindow_);
      nad.setVisible(true);
      
      NetworkAlignment.NetworkAlignInfo nai = nad.getNAInfo();
      
      NetworkAlignment na = new NetworkAlignment(nai);
  
//      System.out.println(na.getSmall().getSize());
//      System.out.println(na.getLarge().getSize());
      
      return (true);
    }
  
//    @Override
//    protected boolean checkGuts() {
//      return (bfp_.hasAModel());
//    }
  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class ImportSIFAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
    private boolean doWeights_;
    
    ImportSIFAction(boolean doIcon, boolean doWeights) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString((doWeights) ? "command.LoadSIFWithWeights" :"command.LoadSIF"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString((doWeights) ? "command.LoadSIFWithWeights" :"command.LoadSIF"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar((doWeights) ? "command.LoadSIFWithWeightsMnem" :"command.LoadSIFMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      doWeights_ = doWeights;
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
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);

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
      Integer magBins = (doWeights_) ? Integer.valueOf(4) : null;
      return (loadFromSifSource(file, null, magBins, new UniqueLabeller()));
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class LoadXMLAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
     
    LoadXMLAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadXML"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadXML"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadXMLMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.LoadXMLAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));
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
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".bif", "filterName.bif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);

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
     
    private static final long serialVersionUID = 1L;
     
    LoadWithNodeAttributesAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.LoadSIFWithNodeAttributes"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.LoadSIFWithNodeAttributes"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.LoadSIFWithNodeAttributesMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
 
    private boolean performOperation(Object[] args) {
      File file = null;      
      String filename = FabricCommands.getPreference("LoadDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser(); 
        FileExtensionFilters.SimpleFilter sf = new FileExtensionFilters.SimpleFilter(".sif", "filterName.sif");
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
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
  
      Map<AttributeLoader.AttributeKey, String> attribs = loadTheFile(attribFile, null, true);
      if (attribs == null) {
        return (true);
      }
 
      return (loadFromSifSource(file, attribs, null, new UniqueLabeller()));
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAsAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    SaveAsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SaveAs"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SaveAs"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/SaveAs24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveAsMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }      
  }  
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class SaveAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    SaveAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Save"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Save"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Save24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar("command.SaveMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }   
   
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportOrderAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    ExportOrderAction(boolean doIcon, String name, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(name));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(name));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    protected abstract List<String> getSuffixList();
    protected abstract String getPrefSuffix();
    protected abstract void writeItOut(File file) throws IOException;
 
    public boolean performOperation(Object[] args) {      
      File file = null;
      String dirName = FabricCommands.getPreference("AttribDirectory");
      while (file == null) {
        JFileChooser chooser = new JFileChooser();
        FileFilter sf = getFilter();
        chooser.addChoosableFileFilter(sf);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(sf);
        
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
            List<String> cand = getSuffixList();
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
      FabricCommands.setPreference("AttribDirectory", file.getAbsoluteFile().getParent());     
      return (true);
    }
   
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }
     
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportNodeOrderAction extends ExportOrderAction {

    private static final long serialVersionUID = 1L;
      
    ExportNodeOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportNodeOrder", "command.ExportNodeOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".noa", ".na", "filterName.noa"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
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
    
    private static final long serialVersionUID = 1L;
    
    ExportLinkOrderAction(boolean doIcon) {
      super(doIcon, "command.ExportLinkOrder", "command.ExportLinkOrderMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.DoubleExtensionFilter(".eda", ".ea", "filterName.eda"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
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
    
    private static final long serialVersionUID = 1L;
    
    ExportSelectedNodesAction(boolean doIcon) {
      super(doIcon, "command.ExportSelectedNodes", "command.ExportSelectedNodesMnem");
    }
    
    protected FileFilter getFilter() {
      return (new FileExtensionFilters.SimpleFilter(".txt", "filterName.txt"));
    }
    
    protected List<String> getSuffixList() {
      ArrayList<String> cand = new ArrayList<String>();
      cand.add(".txt");
      return (cand);
    }
    
    protected String getPrefSuffix() {
      return (".txt");    
    }
    
    protected void writeItOut(File file) throws IOException {
      PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));        
      Set<NID.WithName> sels = bfp_.getNodeSelections();
      Iterator<NID.WithName> sit = sels.iterator();
      while (sit.hasNext()) {
        NID.WithName node = sit.next();
        out.println(node.getName());
      }
      out.close();
      return;
    } 
    
    @Override
    protected boolean checkGuts() {
      return (super.checkGuts() && !bfp_.getNodeSelections().isEmpty());
    }
    
  }
 
  /***************************************************************************
  **
  ** Command
  */ 
      
  private class PrintAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    PrintAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.Print"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.Print"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Print24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PrintMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  private class PrintPDFAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    PrintPDFAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.PrintPDF"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.PrintPDF"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/Print24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.PrintPDFMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
      char accel = rMan.getChar("command.PrintPDFAccel");
      putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accel, Event.CTRL_MASK, false));     
    }

    public void actionPerformed(ActionEvent e) {
      try {
        //PrinterJob pj = PrinterJob.getPrinterJob();
       //// PageFormat pf = pj.defaultPage();
      //  pf.setOrientation(PageFormat.LANDSCAPE);
        Properties p = new Properties();
        p.setProperty("PageSize","A5");
       // p.setProperty(PSGraphics2D.ORIENTATION, PageConstants.LANDSCAPE);
   //     ExportDialog export = new ExportDialog();
     //   export.showExportDialog(topWindow_, "Export view as ...", bfp_, "export");
  
        
        Rectangle2D viewInWorld = bfp_.getViewInWorld();
        Dimension viw = new Dimension((int)(viewInWorld.getWidth() / 10.0), (int)(viewInWorld.getHeight() / 10.0));
      //  VectorGraphics g = new PDFGraphics2D(new File("/Users/bill/OutputAOa.pdf"), viw); 
     //   g.setProperties(p); 
     //   g.startExport(); 
     //   bfp_.print(g); 
     //   g.endExport();
       
             
        // FIX ME: Needed for Win32?  Linux won't default to landscape without this?
        //PageFormat pf2 = pj.pageDialog(pf);
      //  pj.setPrintable(bfp_, pf);
      //  if (pj.printDialog()) {
      //    try {
        //    pj.print();
       //   } catch (PrinterException pex) {
        //    System.err.println(pex);
        //  }
      //  }
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  
 
  /***************************************************************************
  **
  ** Command
  */ 
    
  private abstract class ExportImageAction extends ChecksForEnabled {

    private static final long serialVersionUID = 1L;
    
    ExportImageAction(boolean doIcon, String res, String mnemStr) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString(res));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString(res));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {     
        char mnem = rMan.getChar(mnemStr); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
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
        List<String> supported = ImageExporter.getSupportedFileSuffixes();
        String filename = FabricCommands.getPreference("ExportDirectory");
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
            List<String> suffs = ImageExporter.getFileSuffixesForType(set.formatType);
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
          FabricCommands.setPreference("ExportDirectory", file.getAbsoluteFile().getParent());
        }  
      } catch (IOException ioe) {
        displayFileOutputError();
        return (false);
      }
 
      return (true);
    }
    
    @Override
    protected boolean checkGuts() {
      return (bfp_.hasAModel());
    }  
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportSimpleAction extends ExportImageAction {
 
    private static final long serialVersionUID = 1L;
    
    ExportSimpleAction(boolean doIcon) {
      super(doIcon, "command.Export", "command.ExportMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getWorldRect();
      ExportSettingsDialog esd = new ExportSettingsDialog(topWindow_, wr.width, wr.height);
      esd.setVisible(true);
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
    
  
  /***************************************************************************
  **
  ** Command
  */ 
    
  private class ExportPublishAction extends ExportImageAction {
 
    private static final long serialVersionUID = 1L;
    
    ExportPublishAction(boolean doIcon) {
      super(doIcon, "command.ExportPublish", "command.ExportPublishMnem");
    }
   
    protected ExportSettingsDialog.ExportSettings getExportSettings() {
      Rectangle wr = bfp_.getWorldRect();
      ExportSettingsPublishDialog esd = new ExportSettingsPublishDialog(topWindow_, wr.width, wr.height);
      esd.setVisible(true);
      ExportSettingsDialog.ExportSettings set = esd.getResults();
      return (set); 
    }
  }
  
  /***************************************************************************
  **
  ** Command
  */ 
   
  private class EmptyNetworkAction extends ChecksForEnabled {
     
    private static final long serialVersionUID = 1L;
    
    EmptyNetworkAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.EmptyNet"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.EmptyNet"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.EmptyNetMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem)); 
      }
    }
    
    public void actionPerformed(ActionEvent e) {
      try {
        manageWindowTitle(null);
          
        BioFabricNetwork.RelayoutBuildData obd = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
        		                                                                            new HashSet<FabricLink>(), 
                                                                                        new HashSet<NID.WithName>(), 
                                                                                        new HashMap<NID.WithName, String>(),
                                                                                        colGen_, 
                                                                                        BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
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
    
    private static final long serialVersionUID = 1L;
    
    SetDisplayOptionsAction(boolean doIcon) {
      ResourceManager rMan = ResourceManager.getManager(); 
      putValue(Action.NAME, rMan.getString("command.SetDisplayOpts"));
      if (doIcon) {
        putValue(Action.SHORT_DESCRIPTION, rMan.getString("command.SetDisplayOpts"));        
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/FIXME.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.SetDisplayOptsMnem");
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
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
  
  public class AboutAction extends ChecksForEnabled {
    
    private static final long serialVersionUID = 1L;
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
        URL ugif = getClass().getResource("/org/systemsbiology/biofabric/images/About24.gif");  
        putValue(Action.SMALL_ICON, new ImageIcon(ugif));
      } else {
        char mnem = rMan.getChar("command.AboutMnem"); 
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnem));
      }
      aboutURL_ = getClass().getResource("/org/systemsbiology/biofabric/license/about.html");
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
        gnuUrl_ = getClass().getResource("/org/systemsbiology/biofabric/license/LICENSE");
        sunUrl_ = getClass().getResource("/org/systemsbiology/biofabric/license/LICENSE-SUN");
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
          @Override
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
          restore_ = new BioFabricNetwork.PreBuiltBuildData(net, BioFabricNetwork.BuildMode.BUILD_FROM_XML);
        } else {
          restore_ = null;
        }
        System.out.println("PLO " + System.currentTimeMillis());
        preLoadOperations();
        System.out.println("End PLO " + System.currentTimeMillis());
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
        ubd = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
    		                                             new HashSet<FabricLink>(), new HashSet<NID.WithName>(),
    		                                             new HashMap<NID.WithName, String>(), 
    		                                             colGen_, BioFabricNetwork.BuildMode.BUILD_FROM_SIF);
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
       
    public void doNetworkRelayout(BioFabricNetwork.RelayoutBuildData rbd, NodeSimilarityLayout.CRParams params) {
      if (rbd.canRestore()) {
        BioFabricNetwork net = bfp_.getNetwork();
        restore_ = new BioFabricNetwork.PreBuiltBuildData(net, BioFabricNetwork.BuildMode.BUILD_FROM_XML);
      } else {
        restore_ = null;
      }

      try {
        preLoadOperations();
        NetworkRelayoutRunner runner = new NetworkRelayoutRunner(rbd, params);                                                                  
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netRelayout.waitTitle", "netRelayout.wait", null, true);
        if (rbd.getMode() == BioFabricNetwork.BuildMode.REORDER_LAYOUT) {
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
        ubd = new BioFabricNetwork.RelayoutBuildData(new UniqueLabeller(),
                                                     new HashSet<FabricLink>(), 
                                                     new HashSet<NID.WithName>(), 
                                                     new HashMap<NID.WithName, String>(),
                                                     colGen_, 
                                                     BioFabricNetwork.BuildMode.BUILD_FROM_SIF);

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
    private List<FabricLink> links_; 
    private Set<NID.WithName> loneNodeIDs_;
    private UniqueLabeller idGen_;
    private FabricSIFLoader.SIFStats sss_;
    private Integer magBins_;
    private SortedMap<FabricLink.AugRelation, Boolean> relMap_;
     
    public void doBackgroundSIFRead(File file, UniqueLabeller idGen,
    		                            List<FabricLink> links, Set<NID.WithName> loneNodeIDs, 
    		                            Map<String, String> nameMap, FabricSIFLoader.SIFStats sss, 
    		                            Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relMap) {
      file_ = file;
      links_ = links;
      loneNodeIDs_ = loneNodeIDs;
      idGen_ = idGen;
      sss_ = sss;
      magBins_ = magBins;
      relMap_ = relMap;
      try {       
        SIFReaderRunner runner = new SIFReaderRunner(file, idGen, links, loneNodeIDs, nameMap, sss, magBins, relMap);                                                        
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "fileLoad.waitTitle", "fileLoad.wait", null, true);
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
    	UiUtil.fixMePrintout("Do anything on cancel??");
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
        finishLoadFromSIFSource(file_, idGen_, sss_, links_, loneNodeIDs_, (magBins_ != null), relMap_);
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
  ** Class for recoloring networks
  */ 
    
  public class PreprocessNetwork implements BackgroundWorkerOwner {
    
    public void doNetworkPreprocess(List<FabricLink> links, 
  		                              SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                                Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks) {
      try {
        bfp_.shutdown();
        PreprocessRunner runner = new PreprocessRunner(links, relaMap, reducedLinks, culledLinks);                                                            
        BackgroundWorkerClient bwc = new BackgroundWorkerClient(this, runner, topWindow_, topWindow_, 
                                                                 "netPreprocess.waitTitle", "netPreprocess.wait", null, false);
        runner.setClient(bwc);
        bwc.launchWorker();         
      } catch (Exception ex) {
        ExceptionHandler.getHandler().displayException(ex);
      }
      return;
    }

    public boolean handleRemoteException(Exception remoteEx) {
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
        BufferedImage bi = expensiveModelOperations(bfn_, forMain_, this, 1.0, 1.0);
        return (bi);
      } catch (IOException ex) {
        stashException(ex);
        return (null);
      }
    }
    
    public Object postRunCore() {
    	System.out.println("Tot " + Runtime.getRuntime().totalMemory());
    	System.out.println("Free " + Runtime.getRuntime().freeMemory());
    	System.out.println("Max " + Runtime.getRuntime().maxMemory());
      try {
        Thread.sleep(2000);
      } catch (InterruptedException iex) {
      }
    	Runtime.getRuntime().gc();
    	try {
        Thread.sleep(10000);
      } catch (InterruptedException iex) {
      }
      return (null);
    } 
  }  
  
  /***************************************************************************
  **
  ** Background network layout
  */ 
    
  private class NetworkRelayoutRunner extends BackgroundWorker {
 
    private BioFabricNetwork.RelayoutBuildData rbd_;
    private BioFabricNetwork.BuildMode mode_;
    private NodeSimilarityLayout.CRParams params_;
    
    public NetworkRelayoutRunner(BioFabricNetwork.RelayoutBuildData rbd, NodeSimilarityLayout.CRParams params) {
      super("Early Result");      
      rbd_ = rbd;
      mode_ = rbd.getMode();
      params_ = params;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {            
        switch (mode_) {
          case DEFAULT_LAYOUT:
            (new DefaultLayout()).doLayout(rbd_, params_, this, 0.0, 0.5);
            break;
          case CONTROL_TOP_LAYOUT:
            List<NID.WithName> forcedTop = new ArrayList<NID.WithName>();
            // forcedTop.add("VIM");
            //  forcedTop.add("PACS1");    
            // this has to be FORCED; the layers after the first are laid out in a crap fashion!
            UiUtil.fixMePrintout("Gotta handle the forced top!");
            (new ControlTopLayout()).doLayout(rbd_, forcedTop, this, 0.0, 0.5);
            break;
          case HIER_DAG_LAYOUT:
            (new HierDAGLayout()).doLayout(rbd_, this, 0.0, 0.5);
            break;
          case WORLD_BANK_LAYOUT:
            (new WorldBankLayout()).doLayout(rbd_, this, 0.0, 0.5);
            break;
          case NODE_ATTRIB_LAYOUT:
          case LINK_ATTRIB_LAYOUT:
          case GROUP_PER_NODE_CHANGE:
          case GROUP_PER_NETWORK_CHANGE:
            // previously installed....
            break;
          case REORDER_LAYOUT:
            (new NodeSimilarityLayout()).doReorderLayout(rbd_, params_, this, 0.0, 0.5);
            break;            
          case CLUSTERED_LAYOUT:
            (new NodeSimilarityLayout()).doClusteredLayout(rbd_, params_, this, 0.0, 0.5);
            break;
          case NODE_CLUSTER_LAYOUT:
            (new NodeClusterLayout()).orderByClusterAssignment(rbd_, params_, this, 0.0, 0.5);
            break;                        
          case SHADOW_LINK_CHANGE:
          case BUILD_FOR_SUBMODEL:
          case BUILD_FROM_XML:
          case BUILD_FROM_SIF:
          case BUILD_FROM_GAGGLE:
          default:
            throw new IllegalArgumentException();
        }
        BufferedImage bi = expensiveModelOperations(rbd_, true, this, 0.5, 1.0);
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
        BufferedImage bi = expensiveRecolorOperations(forMain_, this, 0.0, 1.0);
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
    
  private class PreprocessRunner extends BackgroundWorker {
   
    List<FabricLink> links_; 
    SortedMap<FabricLink.AugRelation, Boolean> relaMap_;
  	Set<FabricLink> reducedLinks_; 
  	Set<FabricLink> culledLinks_;
  	
  	public PreprocessRunner(List<FabricLink> links, 
  		                      SortedMap<FabricLink.AugRelation, Boolean> relaMap,
  	                        Set<FabricLink> reducedLinks, Set<FabricLink> culledLinks) {
      super("Early Result");
      links_ = links;
      relaMap_ = relaMap;
      reducedLinks_ = reducedLinks;
      culledLinks_ = culledLinks;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      preprocess(links_, relaMap_, reducedLinks_, culledLinks_, this, 0.0, 1.0);
      return (new Boolean(true));  
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
    private List<FabricLink> links_;
    private Set<NID.WithName> loneNodeIDs_;
    private UniqueLabeller idGen_; 
    private Map<String, String> nameMap_;
    private FabricSIFLoader.SIFStats sss_;
    private Integer magBins_;
    private SortedMap<FabricLink.AugRelation, Boolean> relaMap_;
    
    public SIFReaderRunner(File file, UniqueLabeller idGen, List<FabricLink> links, 
    		                   Set<NID.WithName> loneNodeIDs, Map<String, String> nameMap, 
    		                   FabricSIFLoader.SIFStats sss, 
    		                   Integer magBins, SortedMap<FabricLink.AugRelation, Boolean> relaMap) {
      super("Early Result");
      myFile_ = file;
      links_ = links;
      loneNodeIDs_ = loneNodeIDs;
      idGen_ = idGen;
      nameMap_ = nameMap;
      sss_ = sss;
      magBins_ = magBins;
      relaMap_ = relaMap;
    }
    
    public Object runCore() throws AsynchExitRequestException {
      try {
        FabricSIFLoader.SIFStats sss = (new FabricSIFLoader()).readSIF(myFile_, idGen_, links_, loneNodeIDs_, nameMap_, magBins_, this, 0.0, 0.75);
        sss_.copyInto(sss);
        // Extracting relations can be expensive as well, so this should occur while the file loading
        // dialog is up....
        BioFabricNetwork.extractRelations(links_, relaMap_, this, 0.75, 1.0);     
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
