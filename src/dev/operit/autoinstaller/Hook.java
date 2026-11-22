package dev.operit.autoinstaller;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Targets OnePlus PLZ110 installer 17.0.1. Confirm UI only; no signature bypass. */
public final class Hook implements IXposedHookLoadPackage {
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!"com.android.packageinstaller".equals(p.packageName)) return;
        try {
            // InstallStart redirects directly to AppDetail before the installer activity.
            // Suppress only resolution of its INSTALL entrance, not its uninstall path.
            Class<?> redirect = XposedHelpers.findClass("m1.c", p.classLoader);
            XposedHelpers.findAndHookMethod(redirect, "h", android.content.Context.class, boolean.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (Boolean.TRUE.equals(p.args[1])) {
                        XposedBridge.log("AutoInstaller: skip decoupled AppDetail entrance");
                        p.setResult(null);
                    }
                }
            });
            Class<?> install = XposedHelpers.findClass("com.android.packageinstaller.oplus.OPlusPackageInstallerActivity", p.classLoader);
            // Skip the vendor AppDetail detour once the APK has been parsed and staged.
            XposedHelpers.findAndHookMethod(install, "f3", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        XposedBridge.log("AutoInstaller: bypass AppDetail");
                        XposedHelpers.callMethod(p.thisObject, "z3");
                        p.setResult(null);
                    } catch (Throwable e) { XposedBridge.log(e); }
                }
            });
            // t2 normally launches the vendor scan. Advance to the confirmation setup.
            XposedHelpers.findAndHookMethod(install, "t2", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        XposedBridge.log("AutoInstaller: bypass vendor scan");
                        XposedHelpers.callMethod(p.thisObject, "O2");
                        p.setResult(null);
                    } catch (Throwable e) { XposedBridge.log(e); }
                }
            });
            XposedHelpers.findAndHookMethod(install, "y3", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    final Activity activity = (Activity) p.thisObject;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override public void run() {
                            if (activity.isFinishing()) return;
                            try { XposedBridge.log("AutoInstaller: start install"); XposedHelpers.callMethod(activity, "x3"); }
                            catch (Throwable e) { XposedBridge.log(e); }
                        }
                    });
                }
            });
            Class<?> uninstall = XposedHelpers.findClass("com.android.packageinstaller.UninstallerActivity", p.classLoader);
            XposedHelpers.findAndHookMethod(uninstall, "j1", android.content.Intent.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    final Activity activity = (Activity) p.thisObject;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override public void run() {
                            if (activity.isFinishing()) return;
                            try {
                                int id = activity.getResources().getIdentifier("ok_button", "id", "com.android.packageinstaller");
                                View button = activity.findViewById(id);
                                if (button != null && button.isEnabled()) button.performClick();
                            } catch (Throwable e) { XposedBridge.log(e); }
                        }
                    });
                }
            });
        } catch (Throwable e) { XposedBridge.log(e); }
    }
}
