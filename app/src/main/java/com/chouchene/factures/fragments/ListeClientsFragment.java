package com.chouchene.factures.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.SearchView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.CustomAdapter;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class ListeClientsFragment extends Fragment {
    public ListeClientsFragment(){
    }

    private ClientRepository clientRepository;

    private CustomAdapter listAdapter;

    ArrayList<Client> myClients = new ArrayList<>();

    ClientDao clientDao;

    RecyclerView recyclerView;
    LinearLayout emptyState;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.activity_list_clients, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.baseline_contacts_24);

         clientDao = Room.databaseBuilder(getActivity().getApplicationContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().clientDao();

        myClients= (ArrayList<Client>) clientDao.getAllClients();

        recyclerView = myView.findViewById(R.id.recyclerViewClients);
        emptyState = myView.findViewById(R.id.empty_state);

        listAdapter = new CustomAdapter(this.getActivity(), myClients);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listAdapter);
        checkEmptyState();


        SearchView searchView=myView.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                listAdapter.filter(query);
                checkEmptyState();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listAdapter.filter(newText);
                checkEmptyState();
                return true;
            }
        });

        FloatingActionButton fab = myView.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listAdapter.showClientDialog(null);
            }
        });

        return myView;
    }

    private void checkEmptyState() {
        if (listAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        // Check if the activity has a default ActionBar
//        if (getActivity() != null) {
//            getActivity().setTitle("   Liste des clients");  // Set the ActionBar title
//        }
//        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        // Enable the display of the home icon
//        actionBar.setDisplayShowHomeEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(true);
//        // Change the ActionBar icon
//        actionBar.setLogo(R.drawable.baseline_contacts_24);
    }
}
