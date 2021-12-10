package com.example;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


/**
 * Copyright © 2019 Hell Corporation. All rights reserved.
 *
 * @author Mr. Lucifer
 * @since February 16, 2012
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG = BaseFragment.class.getSimpleName();

    protected AppCompatActivity activity;
    /**
     * Getting instance for Current runnin Fragment
     */
    protected BackHandlerInterface backHandlerInterface;

    /**
     * For Handling back Buton & Most imp in every Fragment when u r overriding it is better to put whole implementation of
     * this method should be try catch for better use.
     *
     * @return return true if your Fragment is Handling back button, false if u r letting Activity handle back buttton
     * if u r handling in fragment(means if u r returning true) den in this condition dont let ur activity handle is this back
     * only nad only activity will handle back when ur fragment is not handling(means if u r rretuning false)
     * <p>
     * e.g. see onBackPressed of Activity and Fragments
     */
    public abstract boolean onBackPressed();

    /**
     * Lifecycle Method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (!(getActivity() instanceof BackHandlerInterface)) {
//            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
//        } else {
//            backHandlerInterface = (BackHandlerInterface) getActivity();
//        }
    }

    /**
     * Lifecycle Method
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            activity = (AppCompatActivity) context;
        } else {
            activity = (AppCompatActivity) getActivity();
        }

    }


    /**
     * Lifecycle Method
     */
    @Override
    public void onStart() {
        super.onStart();
        // Mark this fragment as the selected Fragment.
//        backHandlerInterface.setSelectedFragment(this);
    }

    /**
     * Lifecycle method
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Interface: provide current Fragment instance
     */
    public interface BackHandlerInterface {
        void setSelectedFragment(BaseFragment pBackHandledFragment);
    }
}
