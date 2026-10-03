package com.myapp.quicknotes.ui.common;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.myapp.quicknotes.data.Folder;

import java.util.List;

// A spinner for choosing a folder. The folder list arrives after the screen is built, so the
// picker remembers which folder should be selected and applies it whenever the list changes.
public class FolderPicker {
    public interface OnFolderPicked {
        void onFolderPicked(long folderId);
    }

    private final Spinner spinner;
    private final ArrayAdapter<Folder> adapter;
    private long selectedFolderId;

    public FolderPicker(Spinner spinner, long selectedFolderId, OnFolderPicked onFolderPicked) {
        this.spinner = spinner;
        this.selectedFolderId = selectedFolderId;
        adapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Folder folder = adapter.getItem(position);
                // The spinner also reports selections made in code; only a change is news.
                if (folder != null && folder.getId() != FolderPicker.this.selectedFolderId) {
                    FolderPicker.this.selectedFolderId = folder.getId();
                    onFolderPicked.onFolderPicked(folder.getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void setFolders(List<Folder> folders) {
        adapter.clear();
        adapter.addAll(folders);
        select(selectedFolderId);
    }

    public void select(long folderId) {
        selectedFolderId = folderId;
        for (int position = 0; position < adapter.getCount(); position++) {
            Folder folder = adapter.getItem(position);
            if (folder != null && folder.getId() == folderId) {
                spinner.setSelection(position);
                return;
            }
        }
    }

    public long getSelectedFolderId() {
        return selectedFolderId;
    }
}
