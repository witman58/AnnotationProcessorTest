package com.hys.mockbutterknife_source;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.UiThread;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by 胡延森（QQ：1015950695） on 2018/2/15.
 */

public class MockButterKnife {

    private static final String TAG = "MockButterKnife";

    @UiThread
    public static void bind(Activity target) {
        View sourceView = target.getWindow().getDecorView();
        createBinding(target, sourceView);
    }

    @UiThread
    public static void bind(View target) {
         createBinding(target, target);
    }

   @UiThread
    public static void bind(Dialog target) {
        View sourceView = target.getWindow().getDecorView();
        createBinding(target, sourceView);
    }

    @UiThread
    public static void bind(Object target, View source) {
        createBinding(target, source);
    }

    private static void createBinding(Object target, View source) {
        Class<?> targetClass = target.getClass();
        Constructor constructor = findBindConstructorForClass(targetClass);

        if (constructor == null) {
            return ;
        }

        try {
            constructor.newInstance(target, source);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    @UiThread
    private static Constructor findBindConstructorForClass(Class<?> cls) {

        Constructor bindConstructor = null;

        String clsName = cls.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
            Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            Class<?> bindClass = cls.getClassLoader().loadClass(clsName + "_ViewBinding");
            bindConstructor = bindClass.getConstructor(cls, View.class);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            bindConstructor = findBindConstructorForClass(cls.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
        }

        return bindConstructor;
    }
}
