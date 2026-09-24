package com.onevour.core.components.fragment;
//
// Created by Zuliadin on 2019-12-30.
//

import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.onevour.core.utilities.commons.ValueOf;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class FragmentBottomNavigationManager {

    private Fragment activeFragment;

    private SparseArray<Fragment.SavedState> stateSparse = new SparseArray<>();

    private String SAVED_STATE_CONTAINER_KEY = "STATE_CONTAINER";

    private String SAVED_STATE_CURRENT_TAB_KEY = "STATE_CONTAINER_CURRENT";

    // fragment attribute

    private Context context;

    private int containerLayout;

    private int selectedIndexFragment;

    private FragmentManager fm;

    // fragment mapping

    private Map<Integer, FragmentWrapper> map = new HashMap<>();

    public void setup(Context context, int containerLayout, FragmentManager fragmentManager, BottomNavigationView navigation, BottomNavigationView.OnNavigationItemSelectedListener listener) {
        this.context = context;
        this.containerLayout = containerLayout;
        this.fm = fragmentManager;
        navigation.setOnNavigationItemSelectedListener(listener);
    }

    public void register(int menuId, Fragment fragment, String tag) {
        if (ValueOf.isZero(menuId)) {
            throw new InvalidParameterException("menu id must > 0");
        }
        if (ValueOf.isNull(fragment)) {
            throw new NullPointerException("fragment cannot set null");
        }
        if (ValueOf.isEmpty(tag)) {
            throw new NullPointerException("tag cannot set null");
        }
        this.map.put(menuId, new FragmentWrapper(fragment, tag));
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        if (ValueOf.nonNull(activeFragment)) {
            stateSparse.put(selectedIndexFragment, fm.saveFragmentInstanceState(activeFragment));
        }
        return active(item.getItemId());
    }

    public boolean active(int fragmentId) {
        selectedIndexFragment = fragmentId;
        FragmentWrapper fragmentWrapper = map.get(selectedIndexFragment);
        if (ValueOf.isNull(fragmentWrapper)) {
            return false;
        }
        if (ValueOf.nonNull(activeFragment) && activeFragment.equals(fragmentWrapper.getFragment())) {
            return true;
        }
        return initFragment(selectedIndexFragment, fragmentWrapper);
    }

    private boolean initFragment(int fragmentId, FragmentWrapper fragmentWrapper) {
        if (ValueOf.isNull(context, fragmentWrapper, fm) && containerLayout == 0) {
            return false;
        }
        Fragment fragment = fragmentWrapper.getFragment();
        String tag = fragmentWrapper.getKey();
        if (ValueOf.nonNull(stateSparse)) {
            Fragment.SavedState state = stateSparse.get(fragmentId);
            if (ValueOf.nonNull(state)) {
                if (ValueOf.isNull(fragment.getFragmentManager())) {
                    fragment.setInitialSavedState(state);
                }
            }
        }
        FragmentNavigation.add(context, containerLayout, fragment, tag);
        activeFragment = fragment;
        return true;
    }

    // state handler
    public void onCreateStateHandler(Bundle savedInstanceState) {
        if (ValueOf.nonNull(savedInstanceState)) {
            stateSparse = savedInstanceState.getSparseParcelableArray(SAVED_STATE_CONTAINER_KEY);
            selectedIndexFragment = savedInstanceState.getInt(SAVED_STATE_CURRENT_TAB_KEY);
        }
    }

    // state handler
    public void onSaveInstanceState(Bundle outState) {
        if (ValueOf.nonNull(outState)) {
            outState.putSparseParcelableArray(SAVED_STATE_CONTAINER_KEY, stateSparse);
            outState.putInt(SAVED_STATE_CONTAINER_KEY, selectedIndexFragment);
        }
    }

    class FragmentWrapper {

        private Fragment fragment;

        private String key;

        public FragmentWrapper(Fragment fragment, String key) {
            this.fragment = fragment;
            this.key = key;
        }

        public Fragment getFragment() {
            return fragment;
        }

        public String getKey() {
            return key;
        }
    }

}
