package com.chouchene.factures.fragments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DocumentsViewModel extends ViewModel {
    public static class Filter {
        public String type; // "DAY", "MONTH", "YEAR"
        public String value; // e.g., "01-01-2024" or "01-2024" or "2024"
        public String label; // e.g., "Janvier 2024"
        public String status; // "Payée", "En attente", "Annulée", null for all

        public Filter(String type, String value, String label) {
            this.type = type;
            this.value = value;
            this.label = label;
        }

        public Filter(String type, String value, String label, String status) {
            this.type = type;
            this.value = value;
            this.label = label;
            this.status = status;
        }
    }

    private final MutableLiveData<Filter> currentFilter = new MutableLiveData<>(null);

    public LiveData<Filter> getCurrentFilter() {
        return currentFilter;
    }

    public void setFilter(Filter filter) {
        currentFilter.setValue(filter);
    }

    public void setStatusFilter(String status) {
        Filter current = currentFilter.getValue();
        if (current == null) {
            currentFilter.setValue(new Filter(null, null, null, status));
        } else {
            currentFilter.setValue(new Filter(current.type, current.value, current.label, status));
        }
    }

    public void clearFilter() {
        currentFilter.setValue(null);
    }

    public void clearTimeFilter() {
        Filter current = currentFilter.getValue();
        if (current != null) {
            currentFilter.setValue(new Filter(null, null, null, current.status));
        }
    }
}
