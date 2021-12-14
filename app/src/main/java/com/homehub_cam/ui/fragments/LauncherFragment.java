package com.homehub_cam.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.homehub_cam.MainActivity;
import com.homehub_cam.R;
import com.homehub_cam.listener.NavigationListener;

public class LauncherFragment extends BaseFragment{

    NavigationListener navigationListener;
    public LauncherFragment(NavigationListener navigationListener){
        this.navigationListener = navigationListener;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment, container, false);
        view.findViewById(R.id.tvClick).setOnClickListener(view1 -> {
            navigationListener.navigate(MainActivity.Image_Fragment);
        });

        view.findViewById(R.id.tvVideo).setOnClickListener(view1 -> {
            navigationListener.navigate(MainActivity.Video_Fragment);
        });

        return view;
    }

}
