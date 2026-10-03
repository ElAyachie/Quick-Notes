package com.myapp.quicknotes.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.myapp.quicknotes.R;
import com.myapp.quicknotes.databinding.FragmentHomeBinding;
import com.myapp.quicknotes.ui.common.Screens;

// The start screen: one tab lists the folders, the other writes a new note straight away.
public class HomeFragment extends Fragment {
    private static final int[] TAB_TITLES = {R.string.tab_collection, R.string.tab_make_note};

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new FoldersFragment() : new QuickNoteFragment();
            }

            @Override
            public int getItemCount() {
                return TAB_TITLES.length;
            }
        });
        new TabLayoutMediator(binding.tabs, binding.pager,
                (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Screens.hideKeyboard(binding.pager);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
