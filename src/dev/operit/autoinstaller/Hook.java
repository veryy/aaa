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
            Class<?> install = XposedHelpers.findClass("com.android.packageinstaller.oplus.OPlusPackageInstallerActivity", p.classLoader);
            XposedHelpers.findAndHookMethod(install, "y3", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    final Activity activity = (Activity) p.thisObject;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override public void run() {
                            if (activity.isFinishing()) return;
                            try { XposedHelpers.callMethod(activity, "x3"); }
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
