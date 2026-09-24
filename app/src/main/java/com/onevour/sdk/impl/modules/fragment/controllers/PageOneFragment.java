package com.onevour.sdk.impl.modules.fragment.controllers;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.onevour.core.utilities.commons.ValueOf;
import com.onevour.core.components.fragment.FragmentNavigation;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.FragmentPageOneBinding;


/**
 * A simple {@link Fragment} subclass.
 */
public class PageOneFragment extends Fragment {

    static PageOneFragment fragment;

    public PageOneFragment() {
        // Required empty public constructor
    }

    public static PageOneFragment newInstance() {
        if (ValueOf.isNull(fragment)) {
            fragment = new PageOneFragment();
        }
        return fragment;
    }

    FragmentPageOneBinding binding;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.fragment_page_one, container, false);
//        ButterKnife.bind(this, view);
//        return view;
        binding = FragmentPageOneBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnNext.setOnClickListener(this::nextPage);
    }

//    @OnClick(R.id.btn_next)
    public void nextPage(View view) {
        FragmentNavigation.addBackStack(getActivity(), R.id.container, PageTwoFragment.newInstance(), "B");
    }

}
