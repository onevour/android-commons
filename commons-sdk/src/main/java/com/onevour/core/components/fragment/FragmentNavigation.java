package com.onevour.core.components.fragment;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.onevour.core.utilities.commons.ValueOf;

public class FragmentNavigation {
    
    /**
     * replace fragment, no back stack
     */
    public static void add(Context context, int containerLayout, Fragment fragment, String tag) {
        add(context, containerLayout, fragment, tag, false);
    }

    /**
     * replace fragment, no back stack
     */
    public static void addBackStack(Context context, int containerLayout, Fragment fragment, String tag) {
        add(context, containerLayout, fragment, tag, true);
    }

    /**
     * replace fragment, back stack optional
     */
    public static void add(Context context, int containerLayout, Fragment fragment, String tag, boolean isback) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            Fragment existFragment = fragmentManager.findFragmentByTag(tag);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (ValueOf.nonNull(existFragment)) transaction.remove(existFragment);
            transaction.replace(containerLayout, fragment, tag);
            if (isback) {
                transaction.addToBackStack(null);
            } else {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            transaction.commit();
        }
    }

    public static boolean onBackPressed(Context context, String tag) {
        if (!(context instanceof AppCompatActivity)) {
            return true;
        }
        AppCompatActivity activity = (AppCompatActivity) context;
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment existFragment = fragmentManager.findFragmentByTag(tag);
        if (ValueOf.isNull(existFragment)) {
            return true;
        }
        return !existFragment.isVisible();

    }

}
