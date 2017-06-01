package com.apkscanner.gui.install;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.apkscanner.core.scanner.ApkScanner;
import com.apkscanner.core.scanner.ApktoolScanner;
import com.apkscanner.gui.dialog.ApkInstallWizard;
import com.apkscanner.gui.dialog.PackageInfoPanel;
import com.apkscanner.gui.install.DeviceTablePanel.DeviceDO;
import com.apkscanner.resource.Resource;
import com.apkscanner.tool.adb.AdbServerMonitor;
import com.apkscanner.tool.adb.PackageInfo;
import com.apkscanner.tool.adb.PackageManager;
import com.apkscanner.util.Log;
import com.apkscanner.util.XmlPath;
import com.sun.corba.se.impl.protocol.BootstrapServerRequestDispatcher;

public class DeviceCustomList extends JList implements ListSelectionListener{

	DefaultListModel listmodel;
	private XmlPath sdkXmlPath;
	ButtonsRenderer listrenderer;
	ActionListener FindPackagelistener;
    public DeviceCustomList(ActionListener listener) {
		// TODO Auto-generated constructor stub
    	setLayout(new BorderLayout());    	
        listmodel = new DefaultListModel ();
		//AndroidDebugBridge.init(true);
        //IDevice[] devices = AdbServerMonitor.getAndroidDebugBridge().getDevices();        
        //Log.d(devices.length + "         " + ApkInstallWizard.pakcageFilePath);        
        //AndroidDebugBridge.addDeviceChangeListener(this);
        
        setPreferredSize(new Dimension(200, 0));
        
        listrenderer = new ButtonsRenderer<DeviceListData>(listmodel);
        FindPackagelistener = listener;
        
        CellButtonsMouseListener cbml = new CellButtonsMouseListener(this);
        
        this.addMouseListener(cbml);
        this.addMouseMotionListener(cbml);
        
        this.setModel(listmodel);
        this.setCellRenderer ( listrenderer);
        
        
        this.setBorder ( BorderFactory.createEmptyBorder ( 5, 5, 5, 5 ) );
        this.addListSelectionListener(this);
        
	}

    private int hashCode(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            //hash = str.charAt(i)^10 + ((hash << 4) - hash);
        	hash = str.charAt(i) + ((hash << 10) * hash);
        }
        return hash;
    }
    
    private String intToARGB(int i) {
        String hex = ""+ Integer.toHexString((i>>24)&0xFF) + Integer.toHexString((i>>16)&0xFF) +
        		Integer.toHexString((i>>8)&0xFF) + Integer.toHexString(i&0xFF);
        // Sometimes the string returned will be too short so we 
        // add zeros to pad it out, which later get removed if
        // the length is greater than six.
        //hex += "000000";
        return hex.substring(0, 6);
    }
    
    private void setModeldata(DefaultListModel listmodel, final IDevice device) {
    	for(int i=0; i < listmodel.size(); i++) {
    		DeviceListData temp = (DeviceListData) listmodel.getElementAt(i);
    		if(temp.serialnumber.equals(device.getSerialNumber())) {
    			setDeviceProperty(device, temp, IDevice.PROP_DEVICE_MODEL);
    			setDeviceProperty(device,temp,IDevice.PROP_BUILD_VERSION);    			
    			temp.status = temp.SDKVersion == null ? "OFFLINE" : device.getState().toString();    			
    			temp.AppDetailpanel = getPackageInfopanel(device);
    			
    			Log.d(temp.name + "#"+intToARGB(hashCode(temp.name)));
    			temp.circleColor = Color.decode("#"+intToARGB(hashCode(temp.name)));
    			
    			
    			this.repaint();
    			return;
    		}
    	}
    	
		final DeviceListData data = new DeviceListData();
		//data.circleColor = new Color( 209, 52, 23 );
		data.serialnumber = device.getSerialNumber();
		//data.SDKVersion = device.getProperty(IDevice.PROP_BUILD_VERSION_NUMBER);
		//data.name = device.getName();
		setDeviceProperty(device,data,IDevice.PROP_DEVICE_MODEL);
		setDeviceProperty(device,data,IDevice.PROP_BUILD_VERSION);
		
		//Log.d(data.name + "#"+intToARGB(hashCode(data.name)));
		data.circleColor = Color.decode("#"+intToARGB(hashCode(data.name)));
		
		
		//data.circleColor = ;
		
		data.status = data.SDKVersion == null ? "OFFLINE" : device.getState().toString();
		data.installoptionpanel = new InstallOptionPanel();
		data.showstate = DeviceListData.SHOW_INSTALL_DETAL;
		
		data.pacakgeLoadingstatus =DeviceListData.WAITING; 
		data.AppDetailpanel = new JLabel(Resource.IMG_LOADING.getImageIcon());
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				data.AppDetailpanel = getPackageInfopanel(device);
				//data.AppDetailpanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
				
				if(data.AppDetailpanel instanceof PackageInfoPanel) {
					data.isinstalled = DeviceListData.INSTALLED;
				} else {
					data.isinstalled = DeviceListData.NOT_INSTALLED;
				}
				
				data.pacakgeLoadingstatus =DeviceListData.DONE;
				
		        Log.d(""+ data.isinstalled);
				fireSelectionValueChanged(0, 0, true);
			}			
		}).start();
		
		listmodel.addElement (data);
		
		if(listmodel.size() ==1) {
			setSelectedIndex(0);
			fireSelectionValueChanged(0, 0, true);
		}
		this.repaint();
    }
    
    private JComponent getPackageInfopanel(IDevice dev)
	{
    	
        String packageName = ApkScanner.getPackageName(ApkInstallWizard.pakcageFilePath);
        PackageInfo info = PackageManager.getPackageInfo(dev, packageName);
            
        if(info != null) {
        	PackageInfoPanel packageInfoPanel = new PackageInfoPanel();
        	packageInfoPanel.setPackageInfo(info);
        	//packageInfoDlg.setVisible(true);
			return packageInfoPanel;
        }
                
		return new JLabel("not installed");
        //return null;
	}
    
	public void setDeviceProperty(IDevice device, final DeviceListData DO, final String propertyname) {
		try {
		final String DeviceName = null;
		if("ONLINE".equals(device.getState().toString())) {
				device.executeShellCommand("getprop "+propertyname, new MultiLineReceiver() {
					String temp;
					    @Override
					    public void processNewLines(String[] lines) {					        
					        	if(lines[0].length() >0) {
					        		if(propertyname.indexOf(IDevice.PROP_DEVICE_MODEL) > -1) {
					        			DO.name = lines[0];					        		
					        		} else if(propertyname.indexOf(IDevice.PROP_BUILD_VERSION) > -1) {
					        			DO.SDKVersion = lines[0];
					        			
					        		}
					        		return ;
					        	}
					    }
					    @Override
					    public boolean isCancelled() {
					        return false;
					    }
					});
				
			} else {				
				DO.name = device.getName();
			}
			return;
		} catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException
				| IOException e) {
			e.printStackTrace();
		}
		return;
	}
    
	class CellButtonsMouseListener extends MouseAdapter {
	    private int prevIndex = -1;
	    private JButton prevButton;
	    private final JList<String> list;
	    protected CellButtonsMouseListener(JList<String> list) {
	        super();
	        this.list = list;
	    }
	    @Override public void mouseMoved(MouseEvent e) {
	        //JList list = (JList) e.getComponent();
	        Point pt = e.getPoint();
	        int index = list.locationToIndex(pt);
	        if (!list.getCellBounds(index, index).contains(pt)) {
	            if (prevIndex >= 0) {
	                listRepaint(list, list.getCellBounds(prevIndex, prevIndex));
	            }
	            index = -1;
	            prevButton = null;
	            return;
	        }
	        if (index >= 0) {
	            JButton button = getButton(list, pt, index);
	            ButtonsRenderer renderer = (ButtonsRenderer) list.getCellRenderer();
	            renderer.button = button;
	            if (Objects.nonNull(button) && button.isEnabled()) {
	                button.getModel().setRollover(true);
	                renderer.rolloverIndex = index;
	                if (!button.equals(prevButton)) {
	                    listRepaint(list, list.getCellBounds(prevIndex, index));
	                }
	            } else {
	                renderer.rolloverIndex = -1;
	                Rectangle r = null;
	                if (prevIndex == index) {
	                    if (prevIndex >= 0 && Objects.nonNull(prevButton)) {
	                        r = list.getCellBounds(prevIndex, prevIndex);
	                    }
	                } else {
	                    r = list.getCellBounds(index, index);
	                }
	                listRepaint(list, r);
	                prevIndex = -1;
	            }
	            prevButton = button;
	        }
	        prevIndex = index;
	    }
	    @Override public void mousePressed(MouseEvent e) {
	        //JList list = (JList) e.getComponent();
	        Point pt = e.getPoint();
	        int index = list.locationToIndex(pt);
	        if (index >= 0) {
	            JButton button = getButton(list, pt, index);
	            if (Objects.nonNull(button) && button.isEnabled()) {
	            	ButtonsRenderer renderer = (ButtonsRenderer) list.getCellRenderer();
	                renderer.pressedIndex = index;
	                renderer.button = button;
	                listRepaint(list, list.getCellBounds(index, index));
	            }
	        }
	    }
	    @Override public void mouseReleased(MouseEvent e) {
	        //JList list = (JList) e.getComponent();
	        Point pt = e.getPoint();
	        int index = list.locationToIndex(pt);
	        if (index >= 0) {
	            JButton button = getButton(list, pt, index);
	            if (Objects.nonNull(button) && button.isEnabled()) {
	            	ButtonsRenderer renderer = (ButtonsRenderer) list.getCellRenderer();
	                renderer.pressedIndex = -1;
	                renderer.button = null;
	                button.doClick();
	                listRepaint(list, list.getCellBounds(index, index));
	                
                	DeviceListData temp = (DeviceListData) listmodel.get(list.getSelectedIndex());
                	
                	if(button.getActionCommand().equals(ToggleButtonBar.BUTTON_TYPE_INSTALL_INFO)) {
                		temp.showstate = DeviceListData.SHOW_INSTALL_OPTION;
                	} else if(button.getActionCommand().equals(ToggleButtonBar.BUTTON_TYPE_PACAKGE_INFO)){                		
                		temp.showstate = DeviceListData.SHOW_INSTALL_DETAL;
                	}
                	list.repaint();
                	
	                FindPackagelistener.actionPerformed(new ActionEvent(this, 0, FindPackagePanel.REQ_REFRESH_DETAIL_PANEL));
	            }
	        }
	    }
	    private void listRepaint(JList list, Rectangle rect) {
	        if (Objects.nonNull(rect)) {
	            list.repaint(rect);
	        }
	    }
	    private JButton getButton(JList<String> list, Point pt, int index) {
	        Component c = list.getCellRenderer().getListCellRendererComponent(list, "", index, false, false);
	        Rectangle r = list.getCellBounds(index, index);
	        c.setBounds(r);
	        //c.doLayout(); //may be needed for mone LayoutManager
	        pt.translate(-r.x, -r.y);
	        Component b = SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y);
	        if (b instanceof JButton) {
	            return (JButton) b;
	        } else {
	            return null;
	        }
	    }
	}
    
	class ButtonsRenderer<E> extends JPanel implements ListCellRenderer<E> {
	    private final Color EVEN_COLOR = new Color(230, 255, 230);
	    private final DefaultListModel<E> model;
	    private int index;
	    public int pressedIndex  = -1;
	    public int rolloverIndex = -1;
	    public JButton button;
	    ToggleButtonBar Tagpanel;
	    CustomLabel customlabel = new CustomLabel();

	    protected ButtonsRenderer(DefaultListModel<E> model) {
	        super(new BorderLayout());
	        this.model = model;
	        
	        setBorder ( BorderFactory.createEmptyBorder ( 5, 5 , 5, 5 ) );
	        
	        setOpaque(true);
	        Tagpanel = new ToggleButtonBar(DeviceListData.WAITING);
	        
	        JPanel Iconpanel = new JPanel(new BorderLayout());
	        
	        Iconpanel.setBackground(Color.WHITE);
	        
	        Iconpanel.add(Tagpanel, BorderLayout.CENTER);
	        Iconpanel.add(new JLabel(Resource.IMG_INSTALL_CHECK.getImageIcon()), BorderLayout.WEST);
	        
	        //add(textArea);
	        add(customlabel, BorderLayout.CENTER);
	        add(Iconpanel, BorderLayout.SOUTH);	        
	    }
	    @Override public Dimension getPreferredSize() {
	        Dimension d = super.getPreferredSize();
	        d.width = 0; // VerticalScrollBar as needed
	        return d;
	    }
	    
        @Override
        protected void paintComponent ( Graphics g ) {
        	Graphics2D g2d = ( Graphics2D ) g;
        	g2d.setPaint ( Color.LIGHT_GRAY );
            g.drawLine(2, getHeight()-2, getWidth()-2, getHeight()-2 );                    
        }
        
	    @Override public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
	        //textArea.setText(Objects.toString(value, ""));
	        
	    	if(value instanceof DeviceListData) {
	    		customlabel.setData((DeviceListData)value);
	    		Tagpanel.setData((DeviceListData)value);
	    		resetButtonStatus((DeviceListData) value);
	    	}
	    	
	        this.index = index;
	        if (isSelected) {
	            setBackground(list.getSelectionBackground());
	            customlabel.setSelected(isSelected);
	            
	        } else {
	            setBackground(list.getBackground());
	            customlabel.setSelected(isSelected);
	        }
	        
	        if (Objects.nonNull(button)) {
	            if (index == pressedIndex) {
	                button.getModel().setSelected(true);
	                button.getModel().setArmed(true);
	                button.getModel().setPressed(true);
	            } else if (index == rolloverIndex) {
	                button.getModel().setRollover(true);
	            }
	        }
	        return this;
	    }
	    private void resetButtonStatus(DeviceListData value) {	    	
	    	for( Component b: Tagpanel.getComponents() ) {
	    		if(b instanceof JButton) {
	    			
	    			if((value).status.equals("OFFLINE") || (value).pacakgeLoadingstatus ==DeviceListData.WAITING) {
	    				b.setEnabled(false);
	    				continue;
	    			} else if((value).status.equals("ONLINE")) {
	    				b.setEnabled(true);	    				
	    			}
	    			
	    			ButtonModel m = ((JButton)b).getModel();
	    			m.setRollover(false);
		            m.setArmed(false);
		            m.setPressed(false);
		            m.setSelected(false);
	    		}
	    	}
	    	
	    }
	}
    
    public class DeviceListData
    {
        public Color circleColor;
        public String status;
        public int pacakgeLoadingstatus;
        
        public String name;
        public String serialnumber;
        public String SDKVersion;
                
        public JComponent AppDetailpanel;
        public JComponent installoptionpanel;
        
        public int isinstalled = WAITING;
        public int possibleOption = WAITING;
        
        public int showstate;
        
        public static final int INSTALLED = 0;
        public static final int NOT_INSTALLED = 1;
        
        public static final int POSSIBLE_INSTALL = 0;
        public static final int POSSIBLE_PUSH = 1;
        public static final int IMPOSSIBLE_INSTALL = 2;
        
                
        public static final int WAITING = 3;
        public static final int DONE = 1;
        
        public static final int SHOW_INSTALL_DETAL = 0;
        public static final int SHOW_INSTALL_OPTION = 1;
        
        
        public DeviceListData ( Color circleColor, String status, String name, String sdkVersion, String serialnumber )
        {
            super ();
            this.circleColor = circleColor;
            this.status = status;
            this.name = name;
            this.SDKVersion = sdkVersion;
        }
        
        public DeviceListData ()
        {

        }

        private Color getCircleColor () {
            return circleColor;
        }

        private String getStatus() {
            return status;
        }

        private String getName () {
            return name;
        }
        
        private String getSDKVersion() {
        	return SDKVersion;
        }
        
    }
    
    private class CustomLabel extends JLabel
    {
        private final Color selectionColor = new Color ( 82, 158, 202 );

        private boolean selected;
        private DeviceListData data;

        public CustomLabel ()
        {
            super ();
            setOpaque ( false );
            setBorder ( BorderFactory.createEmptyBorder ( 0, 60, 0, 40 ) );
            setFont(new Font(getFont().getName(), Font.BOLD, 9));
        }

        private void setSelected ( boolean selected )
        {
            this.selected = selected;
            setForeground ( selected ? Color.WHITE : Color.BLACK );
        }

        private void setData ( DeviceListData data )
        {
            this.data = data;
            setText ( data.getName () );
        }

        private void centerString(Graphics g, Rectangle r, String s, 
                Font font) {
            FontRenderContext frc = 
                    new FontRenderContext(null, true, true);
            
            Rectangle2D r2D = font.getStringBounds(s, frc);
            int rWidth = (int) Math.round(r2D.getWidth());
            int rHeight = (int) Math.round(r2D.getHeight());
            int rX = (int) Math.round(r2D.getX());
            int rY = (int) Math.round(r2D.getY());

            int a = (r.width / 2) - (rWidth / 2) - rX;
            int b = (r.height / 2) - (rHeight / 2) - rY;

            g.setFont(font);
            g.drawString(s, r.x + a, r.y + b);
        }
        
        @Override
        protected void paintComponent ( Graphics g )
        {
            Graphics2D g2d = ( Graphics2D ) g;
            g2d.setRenderingHint ( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

            if ( selected )
            {
                Area area = new Area ( new Ellipse2D.Double ( 0, 0, 52, 52 ) );
                area.add ( new Area ( new RoundRectangle2D.Double ( 36, 6, getWidth () - 36, 40, 12, 12 ) ) );
                g2d.setPaint ( selectionColor );
                g2d.fill ( area );

                g2d.setPaint ( Color.WHITE );
                g2d.fill ( new Ellipse2D.Double ( 4, 4, 44, 44 ) );
            }
            
            g2d.setPaint ( data.status.indexOf("ONLINE") > -1? data.getCircleColor():Color.GRAY );
            g2d.fill ( new Ellipse2D.Double ( 6, 6, 40, 40 ) );
            
            g2d.setPaint ( Color.WHITE );
            //g2d.drawString("N", 22, 22);
            
            if(data.getSDKVersion()!=null) {
                if(data.getSDKVersion().length() < 4) {
	            	centerString(g2d,new Rectangle(4, 4, 44, 44), data.getSDKVersion(), new Font(getFont().getName(), Font.BOLD, 20));
	            } else {
	            	centerString(g2d,new Rectangle(4, 4, 44, 44), data.getSDKVersion(), new Font(getFont().getName(), Font.BOLD, 15));
	            }
            }
            if(data.status.indexOf("ONLINE") > -1) {
            	g2d.setPaint ( new Color(116, 211, 109) ); // online color            
            } else if(data.status.indexOf("OFFLINE") > -1) {            	
            	g2d.setPaint(Color.GRAY);
            } else {
            	g2d.setPaint(Color.ORANGE);            	
            }
            g2d.fill ( new Ellipse2D.Double ( getWidth () - 18 - 10, getHeight () / 2 - 9, 18, 18 ) );
            
            g.setFont(new Font(getFont().getName(), Font.BOLD, 15));
            super.paintComponent ( g );
        }

        @Override
        public Dimension getPreferredSize ()
        {
            final Dimension ps = super.getPreferredSize ();
            ps.height = 54;
            ps.width = 200;
            return ps;
        }
    }
	public void deviceChanged(IDevice arg0, int arg1) {
		// TODO Auto-generated method stub
		Log.d("change device state : " + arg0.getSerialNumber() + " : " + arg0.getState());
		setModeldata(listmodel, arg0);
	}

	public void deviceConnected(IDevice arg0) {
		// TODO Auto-generated method stub
		Log.d("deviceConnected device state : " + arg0.getSerialNumber() + " : " + arg0.getState());
		setModeldata(listmodel, arg0);
	}

	public void deviceDisconnected(IDevice arg0) {
		// TODO Auto-generated method stub
		Log.d("deviceDisconnected device state : " + arg0.getSerialNumber() + " : " + arg0.getState());
				
    	for(int i=0; i < listmodel.size(); i++) {
    		DeviceListData temp = (DeviceListData) listmodel.getElementAt(i);
    		if(temp.serialnumber.equals(arg0.getSerialNumber())) {
    			listmodel.remove(i);
    			if(listmodel.size() >=1) {
    				this.setSelectedIndex(0);
    			}
    			return;
    			//setDeviceProperty(device, temp, IDevice.PROP_DEVICE_MODEL);
    		}
    	}		
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub
        boolean adjust = e.getValueIsAdjusting();
        //if (!adjust) {
          JList list = (JList) e.getSource();
          int selections[] = list.getSelectedIndices();
          Object selectionValues[] = list.getSelectedValues();
          for (int i = 0, n = selections.length; i < n; i++) {
            
            //System.out.print(selections[i] + "/" + selectionValues[i] + " ");
        	  ((DeviceListData)selectionValues[i]).showstate  = DeviceListData.SHOW_INSTALL_OPTION;
        	  FindPackagelistener.actionPerformed(new ActionEvent(this, 0, FindPackagePanel.REQ_REFRESH_DETAIL_PANEL));
          }
        //}
		 
	}
}
