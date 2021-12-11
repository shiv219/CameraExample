package com.homehub_cam.ui.fragments;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


public abstract class BaseFragment extends Fragment {

    private static final String TAG = BaseFragment.class.getSimpleName();

    protected AppCompatActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity) context;
        } else {
            activity = (AppCompatActivity) getActivity();
        }

    }
}
