package com.apkscanner.gui.action;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;

import com.android.ddmlib.IDevice;
import com.apkscanner.data.apkinfo.ApkInfo;
import com.apkscanner.gui.dialog.PackageInfoPanel;
import com.apkscanner.gui.easymode.contents.EasyGuiDeviceToolPanel;
import com.apkscanner.gui.messagebox.MessageBoxPool;
import com.apkscanner.tool.adb.PackageInfo;
import com.apkscanner.util.Log;

@SuppressWarnings("serial")
public class DeviceShowInstalledPackageAction extends AbstractDeviceAction
{
	public static final String ACTION_COMMAND = "ACT_CMD_SHOW_INSTALLED_PACKAGE_INFO";

	public DeviceShowInstalledPackageAction(ActionEventHandler h) { super(h); }

	@Override
	public void actionPerformed(ActionEvent e) {
		IDevice device = null;
		if(e.getSource() instanceof EasyGuiDeviceToolPanel) {
			device = ((EasyGuiDeviceToolPanel) e.getSource()).getSelecteddevice();
		}

		evtShowInstalledPackageInfo(getWindow(e), device);
	}

	private void evtShowInstalledPackageInfo(final Window owner, final IDevice target) {
		final ApkInfo apkInfo = getApkInfo();
		if(apkInfo == null) return;

		final String packageName = apkInfo.manifest.packageName;

		Thread thread = new Thread(new Runnable() {
			public void run() {
				IDevice[] devices = null;
				if(target == null) {
					devices = getInstalledDevice(packageName);
				} else {
					if(getPackageInfo(target, packageName) != null) {
						devices =  new IDevice[] { target };
					}
				}

				if(devices == null || devices.length == 0) {
					Log.i("No such device of a package installed.");
					MessageBoxPool.show(owner, MessageBoxPool.MSG_NO_SUCH_PACKAGE_DEVICE);
					return;
				}

				for(IDevice device: devices) {
					final PackageInfo info = getPackageInfo(device, packageName);
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							PackageInfoPanel packageInfoPanel = new PackageInfoPanel();
							packageInfoPanel.setPackageInfo(info);
							packageInfoPanel.showDialog(owner);
						}
					});
				}
			}
		});
		thread.setPriority(Thread.NORM_PRIORITY);
		thread.start();
	}
}
