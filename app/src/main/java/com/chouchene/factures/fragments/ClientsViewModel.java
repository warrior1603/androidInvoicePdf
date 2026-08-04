package com.chouchene.factures.fragments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ClientsViewModel extends ViewModel {
    private final MutableLiveData<Integer> highlightClientId = new MutableLiveData<>(-1);

    public LiveData<Integer> getHighlightClientId() {
        return highlightClientId;
    }

    public void setHighlightClientId(int id) {
        highlightClientId.setValue(id);
    }
    
    public void consumeHighlight() {
        highlightClientId.setValue(-1);
    }
}
