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
import android.widget.ListView;
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

    ListView listView;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Mes Clients");
        View myView = inflater.inflate(R.layout.activity_list_clients, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.customer_svgrepo_com);

         clientDao = Room.databaseBuilder(getActivity().getApplicationContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().clientDao();

        myClients= (ArrayList<Client>) clientDao.getAllClients();

        listView = myView.findViewById(R.id.listview);

        listAdapter = new CustomAdapter(this.getActivity(), myClients);
        listView.setAdapter(listAdapter);


        SearchView searchView=myView.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                listAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listAdapter.filter(newText);
                return true;
            }
        });

        FloatingActionButton fab = myView.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.popup_add_client, null);
                TextInputEditText txtName = view1.findViewById(R.id.edit_user_name_client);
                TextInputEditText txtRue = view1.findViewById(R.id.edit_street);
                TextInputEditText txtVille = view1.findViewById(R.id.edit_ville);
                TextInputEditText txtCodePostale = view1.findViewById(R.id.edit_code_postale);
                TextInputEditText txtPays = view1.findViewById(R.id.edit_pays);
                TextInputEditText txtSiren = view1.findViewById(R.id.edit_siren);
                TextInputEditText txtEmail = view1.findViewById(R.id.edit_email_client);
                TextInputEditText txtTva = view1.findViewById(R.id.tva_client);

                txtCodePostale.addTextChangedListener(new TextWatcher() {
                                                          @Override
                                                          public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                                          }

                                                          @Override
                                                          public void onTextChanged(CharSequence s, int start, int before, int count) {

                                                          }

                                                          @Override
                                                          public void afterTextChanged(Editable s) {
                                                              FetchVilleFromCodePostale.fetchDataFromApiWithParams(txtCodePostale.getText().toString(), txtVille, txtPays);
                                                          }
                                                      }

                );

                // Show confirmation dialog
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(myView.getContext(), R.style.AlertDialogTheme)
                        .setTitle("Nouveau client")
                        .setView(view1)
                        .setPositiveButton("Enregistrer", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Supplied
                                String customerName = txtName.getText().toString();
                                String rueClient = txtRue.getText().toString();
                                String villeClient = txtVille.getText().toString();
                                String codePostaleClient = txtCodePostale.getText().toString();
                                String pays = txtPays.getText().toString();
                                String siren = txtSiren.getText().toString();
                                String emailClient = txtEmail.getText().toString();
                                String tva = txtTva.getText().toString();

                                // Insert item in the background thread
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    // Create the client instance
                                    Client client = new Client(customerName,rueClient,villeClient,codePostaleClient,pays,siren,tva,emailClient);
                                    clientRepository = new ClientRepository(clientDao);
                                    clientRepository.addClientIfNotExists(client);

                                    // Update the UI on the main thread
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateListView();
                                        }
                                    });
                                });


                            }
                        })
                        .setNegativeButton("Annuler", null);

                        //.setIcon(android.R.drawable.ic_input_add) //ic_dialog_info
                AlertDialog dialog = builder.create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background_square);
                }
//                dialog.setOnShowListener(
//                                dlg -> {
//                                    if (dialog.getWindow() != null) {
//                                        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                                    }
//                                }
//                        );
                dialog.show();
            }
        });

        return myView;
    }

    private void updateListView() {
        // Refresh or update your list here
        listAdapter.setData((ArrayList<Client>) clientDao.getAllClients());
        listAdapter.notifyDataSetChanged();
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
//        actionBar.setLogo(R.drawable.customer_svgrepo_com);
    }
}
