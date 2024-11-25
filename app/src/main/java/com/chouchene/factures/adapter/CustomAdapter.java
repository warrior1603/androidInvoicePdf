package com.chouchene.factures.adapter;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.repository.ClientRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CustomAdapter extends ArrayAdapter<Client> {

    //private List<Client> values;

    private final FragmentActivity fragmentActivity;

    private ClientRepository clientRepository;
    ClientDao clientDao;

    private ArrayList<Client> originalList;
    private ArrayList<Client> filteredList;

    public CustomAdapter(FragmentActivity context, ArrayList<Client> values) {
        super(context, R.layout.list_item_with_button, values);
        //this.values = values;
        this.fragmentActivity = context;
        this.originalList = new ArrayList<>(values);
        this.filteredList = new ArrayList<>(values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) fragmentActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate the custom layout for each item
        View rowView = inflater.inflate(R.layout.list_item_with_button, parent, false);

        clientDao = Room.databaseBuilder(getContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build().clientDao();

        // Get references to the TextView and Button
        TextView textView = rowView.findViewById(R.id.textViewItem);
        Button editbutton = rowView.findViewById(R.id.buttonEdit);
        Button deleteButton = rowView.findViewById(R.id.imageDelete);

        LinearLayout datailsText = rowView.findViewById(R.id.details);
        CardView cardview = rowView.findViewById(R.id.cardView);
        LinearLayout layout = rowView.findViewById(R.id.layout1);

        cardview.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Client selectedClient = getItem(position);
                        TextView txtRue = v.findViewById(R.id.show_rue);
                        TextView txtVille = v.findViewById(R.id.show_ville);
                        TextView txtCodePostale = v.findViewById(R.id.show_codePostale);
                        TextView txtPays = v.findViewById(R.id.show_pays);
                        TextView txtSiren = v.findViewById(R.id.show_siren);
                        TextView txtEmail = v.findViewById(R.id.show_email);
                        TextView txtTva = v.findViewById(R.id.show_tva);

                            txtRue.setText(selectedClient.getStreet());
                            txtVille.setText(selectedClient.getVille());
                            txtCodePostale.setText(selectedClient.getCodePostale());
                            txtPays.setText(selectedClient.getPays());
                            txtSiren.setText(selectedClient.getNumeroSiren());
                            txtEmail.setText(selectedClient.getEmail());
                            txtTva.setText(selectedClient.getNumeroTVA());

                        layout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                        int visibility = (datailsText.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE;
                        TransitionManager.beginDelayedTransition(layout, new AutoTransition());
                        datailsText.setVisibility(visibility);

                    }
                }
        );

        // Set the text for the TextView
        textView.setText(getItem(position).getClientName());

        editbutton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Client selectedClient = getItem(position);
                        View view1 = inflater.inflate(R.layout.popup_add_client, null);
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

                        txtName.setText(selectedClient.getClientName());
                        txtRue.setText(selectedClient.getStreet());
                        txtVille.setText(selectedClient.getVille());
                        txtCodePostale.setText(selectedClient.getCodePostale());
                        txtPays.setText(selectedClient.getPays());
                        txtSiren.setText(selectedClient.getNumeroSiren());
                        txtEmail.setText(selectedClient.getEmail());
                        txtTva.setText(selectedClient.getNumeroTVA());

                        // Show confirmation dialog
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view1.getContext(), R.style.AlertDialogTheme)
                                .setTitle("Modifier client")
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
                                            client.setId(selectedClient.getId());
                                            clientRepository.updateClient(client);

                                            // Update the UI on the main thread
                                            fragmentActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    setData((ArrayList<Client>) clientDao.getAllClients());
                                                    notifyDataSetChanged();
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
                        dialog.show();
                    }
                }
        );


        // Set the delete button functionality
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show confirmation dialog
                MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(v.getContext(), R.style.AlertDialogTheme)
                        .setTitle("Confirmez-vous la suppression ?")
                        .setMessage("Si vous confirmez, votre client sera definitivement effacé de la liste des clients?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Client clientToDelete = getItem(position);
                                // delete item from database
                                ClientDao clientDao = Room.databaseBuilder(v.getContext(), AppDatabase.class, "MyClients").build().clientDao();
                                clientRepository = new ClientRepository(clientDao);
                                clientRepository.deleteClient(clientToDelete);
                                // Remove the item from the list and notify the adapter
                                filteredList.remove(position);
                                notifyDataSetChanged();

                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert);//ic_dialog_alert
                AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background_square);
                }
                dialog.show();
            }
        });

        return rowView;
    }


    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Client getItem(int position) {
        return filteredList.get(position);
    }

    public void setData(ArrayList<Client> clients){
        originalList.clear();
        originalList.addAll(clients);
        filteredList.clear();
        filteredList.addAll(clients);
        notifyDataSetChanged();
    }

    // Filter logic based on object name
    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            query = query.toLowerCase();
            for (Client obj : originalList) {
                if (obj.getClientName().toLowerCase().contains(query)) {
                    filteredList.add(obj);
                }
            }
        }
        notifyDataSetChanged();
    }

}
