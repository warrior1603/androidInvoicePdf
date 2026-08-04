package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.CustomAdapter;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;

public class ClientListFragment extends Fragment {
    
    public enum Mode { ALL, RECENT }
    private Mode mode = Mode.ALL;

    private CustomAdapter listAdapter;
    private ArrayList<Client> myClients = new ArrayList<>();
    private ClientDao clientDao;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    public static ClientListFragment newInstance(Mode mode) {
        ClientListFragment fragment = new ClientListFragment();
        Bundle args = new Bundle();
        args.putSerializable("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = (Mode) getArguments().getSerializable("mode");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.activity_list_clients, container, false);

        clientDao = Room.databaseBuilder(requireContext().getApplicationContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().clientDao();

        recyclerView = myView.findViewById(R.id.recyclerViewClients);
        emptyState = myView.findViewById(R.id.empty_state);
        ExtendedFloatingActionButton fab = myView.findViewById(R.id.fab);

        // Hide FAB in recent mode to keep it clean
        if (mode == Mode.RECENT) {
            fab.setVisibility(View.GONE);
        }

        Bundle args = getArguments();
        int highlightId = (args != null) ? args.getInt("highlight_client_id", -1) : -1;

        loadData();

        listAdapter = new CustomAdapter(this.getActivity(), myClients, highlightId);
        listAdapter.setOnDataChangedListener(this::checkEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listAdapter);
        checkEmptyState();

        if (highlightId != -1) {
            for (int i = 0; i < myClients.size(); i++) {
                if (myClients.get(i).getId() == highlightId) {
                    final int pos = i;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        recyclerView.scrollToPosition(pos);
                    }, 100);
                    break;
                }
            }
        }

        fab.setOnClickListener(view -> {
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(null);
            bottomSheet.setOnClientSavedListener(() -> {
                loadData();
                listAdapter.setData(myClients);
            });
            bottomSheet.show(getChildFragmentManager(), "ADD_CLIENT");
        });

        return myView;
    }

    private void loadData() {
        if (mode == Mode.RECENT) {
            myClients = (ArrayList<Client>) clientDao.getRecentClients();
        } else {
            myClients = (ArrayList<Client>) clientDao.getAllClients();
        }
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
}
