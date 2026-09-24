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
import com.onevour.sdk.impl.databinding.FragmentPageTwoBinding;

//import butterknife.ButterKnife;
//import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class PageTwoFragment extends Fragment {

    private static PageTwoFragment fragment;

    public PageTwoFragment() {
        // Required empty public constructor
    }

    public static PageTwoFragment newInstance() {
        if (ValueOf.isNull(fragment)) {
            fragment = new PageTwoFragment();
        }
        return fragment;
    }

    FragmentPageTwoBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPageTwoBinding.inflate(inflater, container, false);
//        View view = inflater.inflate(R.layout.fragment_page_two, container, false);
//        ButterKnife.bind(this, view);
//        return view;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnNext.setOnClickListener(this::nextPage);
    }

    //    @OnClick(R.id.btn_next)
    public void nextPage(View view) {
        FragmentNavigation.addBackStack(getActivity(), R.id.container, PageThreeFragment.newInstance(), "C");
    }

}
