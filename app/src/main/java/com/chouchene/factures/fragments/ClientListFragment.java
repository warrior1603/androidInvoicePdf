package com.chouchene.factures.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.CustomAdapter;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.utils.SwipeToDeleteCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.Hold;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class ClientListFragment extends Fragment {
    
    public enum Mode { ALL, RECENT }
    private Mode mode = Mode.ALL;

    private ClientsViewModel viewModel;
    private CustomAdapter listAdapter;
    private ArrayList<Client> myClients = new ArrayList<>();
    private ClientDao clientDao;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    public static ClientListFragment newInstance(Mode mode, int highlightId) {
        ClientListFragment fragment = new ClientListFragment();
        Bundle args = new Bundle();
        args.putSerializable("mode", mode);
        if (highlightId != -1) {
            args.putInt("highlight_client_id", highlightId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = (Mode) getArguments().getSerializable("mode");
        }
        if (getActivity() != null) {
            viewModel = new ViewModelProvider(getActivity()).get(ClientsViewModel.class);
        }
        
        setExitTransition(new Hold());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_list_clients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Required for Shared Element Transition in RecyclerView
        postponeEnterTransition();

        clientDao = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase().clientDao();

        recyclerView = view.findViewById(R.id.recyclerViewClients);
        emptyState = view.findViewById(R.id.empty_state);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);

        if (mode == Mode.RECENT) {
            fab.setVisibility(View.GONE);
        }

        loadData();

        listAdapter = new CustomAdapter(this.getActivity(), myClients, -1);
        listAdapter.setOnDataChangedListener(this::checkEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listAdapter);

        // Start transition once data is loaded and laid out
        recyclerView.getViewTreeObserver().addOnPreDrawListener(() -> {
            startPostponedEnterTransition();
            return true;
        });

        new ItemTouchHelper(new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                onDeleteClick(listAdapter.getClientAt(position));
            }
        }).attachToRecyclerView(recyclerView);

        checkEmptyState();

        if (viewModel != null && mode == Mode.ALL) {
            viewModel.getHighlightClientId().observe(getViewLifecycleOwner(), id -> {
                if (id != -1) {
                    performHighlight(id);
                    viewModel.consumeHighlight();
                }
            });
        }

        fab.setOnClickListener(v -> {
            AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(null);
            bottomSheet.setOnClientSavedListener(() -> {
                loadData();
                listAdapter.setData(myClients);
                com.google.android.material.snackbar.Snackbar.make(view, "Client ajouté avec succès", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            });
            bottomSheet.show(getChildFragmentManager(), "ADD_CLIENT");
        });
    }

    private void onDeleteClick(Client client) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmez-vous la suppression ?")
                .setMessage("Si vous confirmez, votre client sera definitivement effacé de la liste des clients?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        clientDao.deleteClient(client);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadData();
                                listAdapter.setData(myClients);
                            });
                        }
                    });
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> listAdapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> listAdapter.notifyDataSetChanged())
                .show();
    }

    private void performHighlight(int id) {
        for (int i = 0; i < myClients.size(); i++) {
            if (myClients.get(i).getId() == id) {
                final int pos = i;
                recyclerView.post(() -> {
                    recyclerView.scrollToPosition(pos);
                    listAdapter.setHighlightId(id);
                });
                break;
            }
        }
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
