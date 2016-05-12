package com.example.frank.mysticklayout;

import android.app.Application;
import android.os.Environment;

import cn.jiajixin.nuwa.Nuwa;

/**
 * 创建者     Frank
 * 创建时间   2016/5/11 15:26
 * 描述	      ${TODO}
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Nuwa.init(this);
        Nuwa.loadPatch(this, Environment.getExternalStorageDirectory().getAbsolutePath().concat("/patch.jar"));
    }
}
