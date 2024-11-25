package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.room.Room;
import com.chouchene.factures.R;
import com.chouchene.factures.api.ApiService;
import com.chouchene.factures.api.FetchVilleFromCodePostale;
import com.chouchene.factures.api.VilleDataModel;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.dao.InvoiceDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.entity.Invoice;
import com.chouchene.factures.repository.ClientRepository;
import com.chouchene.factures.entity.Client;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InvoiceGenrationFragment extends Fragment{
    private static final String DIRECTORY_KEY = "directory";
    private static final String CURRENCY_KEY = "default_currency";

    public InvoiceGenrationFragment() {

    }

    String customerName;
    String rueClient;
    String villeClient;
    String codePostaleClient;
    String pays;
    String siren;
    String tva;
    String email;

    TextInputEditText txtName;
    TextInputEditText txtRue;
    TextInputEditText txtVille;
    TextInputEditText txtCodePostale;
    TextInputEditText txtPays;
    TextInputEditText txtSiren;
    TextInputEditText txtEmail;
    TextInputEditText txtTvaClient;

    TextInputEditText txtDesciption;
    TextInputEditText txtQuantite;
    TextInputEditText txtPrix;
    TextInputEditText txtTva;

    TextInputEditText editDateFactureForm;

    TextInputLayout txtModePayement;

    TextInputLayout inputClient;
    LinearLayout inputClientProvisoire;

    Integer mumeroFacture = 00;
    SharedPreferences sharedPreferences;
    SharedPreferences settingsSharedPreferences;
    private ClientRepository clientRepository;

    private AppDatabase db;
    private ClientDao itemDao;

    Boolean isClientProvisoire = false;

    Client selectedClient = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getActivity().setTitle("Factures");
        sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mumeroFacture = sharedPreferences.getInt("last-invoice-number", 00);

        View myView = inflater.inflate(R.layout.activity_invoice, container, false);

        MaterialToolbar toolbar = requireActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.invoice);

        String[] Payements = new String[] {"Virement", "Carte","Espèce", "Cheque"};

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        getContext(),
                        R.layout.dropdown_menu_popup_item,
                        Payements);

        AutoCompleteTextView editTextFilledExposedDropdown =
                myView.findViewById(R.id.filled_exposed_dropdown);
        editTextFilledExposedDropdown.setAdapter(adapter);

        AutoCompleteTextView autoCompleteTextView =
                myView.findViewById(R.id.autoCompleteTextView);


        // Initialize Room database
        db = Room.databaseBuilder(getContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        itemDao = db.clientDao();

        List<Client> items = itemDao.getAllClients();
        // Load items from the database in a background thread
        new Thread(() -> {
            List<String> itemNames = new ArrayList<>();
            for (Client item : items) {
                itemNames.add(item.getClientName());
            }
            // Update the UI on the main thread
            getActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, itemNames);
                autoCompleteTextView.setAdapter(adapter1);
            });
        }).start();

        // Handle selection
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            for (Client item : items) {
                if (item.getClientName().equals(selectedItem)) {
                    selectedClient = item;
                    break;
                }
            }
            if (selectedClient != null) {
                Log.i("Selected: ", selectedClient.getClientName());

            }
        });

        RadioGroup radioGroup =  myView.findViewById(R.id.radio_group);

        inputClient = myView.findViewById(R.id.client_input);
        inputClientProvisoire = myView.findViewById(R.id.client_input_provisoire);

        radioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        int radioButtonID = radioGroup.getCheckedRadioButtonId();
                        RadioButton radioButton =  radioGroup.findViewById(radioButtonID);
                        String selectedText = (String) radioButton.getText();
                        Log.i("Selected: ", selectedText);
                        if (selectedText.equals("Permanant")) {
                            inputClient.setVisibility(View.VISIBLE);
                            inputClientProvisoire.setVisibility(View.GONE);
                            isClientProvisoire = false;
                        } else {
                            inputClient.setVisibility(View.GONE);
                            inputClientProvisoire.setVisibility(View.VISIBLE);
                            isClientProvisoire = true;
                        }
                    }
                }
        );

        txtDesciption = myView.findViewById(R.id.edit_description);
        txtQuantite = myView.findViewById(R.id.edit_quantite);
        txtPrix = myView.findViewById(R.id.edit_prix);
        txtTva = myView.findViewById(R.id.edit_tva);

        txtModePayement = myView.findViewById(R.id.dropdown_input);

        txtName = myView.findViewById(R.id.edit_user_name_client1);
        txtRue = myView.findViewById(R.id.edit_street1);
        txtVille = myView.findViewById(R.id.edit_ville1);
        txtCodePostale = myView.findViewById(R.id.edit_code_postale1);
        txtPays = myView.findViewById(R.id.edit_pays1);
        txtSiren = myView.findViewById(R.id.edit_siren1);
        txtEmail = myView.findViewById(R.id.edit_email_client1);
        txtTvaClient = myView.findViewById(R.id.edit_tva_client);

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

        MaterialButton btnCreatePDF = myView.findViewById(R.id.btnCreatePdf);

        btnCreatePDF.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                try {
                                                    if (isClientProvisoire) {
                                                        customerName = txtName.getText().toString();
                                                        rueClient = txtRue.getText().toString();
                                                        villeClient = txtVille.getText().toString();
                                                        codePostaleClient = txtCodePostale.getText().toString();
                                                        pays = txtPays.getText().toString();
                                                        siren = txtSiren.getText().toString();
                                                        email = txtEmail.getText().toString();
                                                        tva = txtTvaClient.getText().toString();
                                                    }
                                                    else {
                                                        customerName = selectedClient.getClientName();
                                                        rueClient = selectedClient.getStreet();
                                                        villeClient = selectedClient.getVille();
                                                        codePostaleClient = selectedClient.getCodePostale();
                                                        pays = selectedClient.getPays();
                                                        siren = selectedClient.getNumeroSiren();
                                                        email = selectedClient.getEmail();
                                                        tva = selectedClient.getNumeroTVA();
                                                    }

                                                    fillFormFields();
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }

                                            }
                                        }

        );

        return myView;
    }

    private void showDatePickerDialog(TextInputEditText editDate){
        MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();

        builder.setTitleText("Selectionner la date");
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());

        final MaterialDatePicker materialDatePicker = builder.build();

        materialDatePicker.show(requireActivity().getSupportFragmentManager(), "DATE_PICKER");

        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                editDate.setText(materialDatePicker.getHeaderText());
            }
        });
    }


    private void fillFormFields() throws IOException {
        PDFBoxResourceLoader.init(getActivity().getApplicationContext());
        DateFormat date = new SimpleDateFormat("dd.MM.YYYY");
        String todayCode = date.format(new Date());
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        String dateCode = df.format(new Date());
        // Récupérer la date actuelle
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        // Récupérer la dernière date enregistrée
        String lastDate = sharedPreferences.getString("last_reset_date", "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        mumeroFacture = new Integer(mumeroFacture.intValue() + 1);
        editor.putInt("last-invoice-number", mumeroFacture);
        editor.apply();
        if (!currentDate.equals(lastDate)) {
            // Réinitialiser la valeur à zéro
            mumeroFacture = 00;
            editor.putInt("last-invoice-number", mumeroFacture);
            // Mettre à jour la dernière date
            editor.putString("last_reset_date", currentDate);
            editor.apply();
        }

        String currency = settingsSharedPreferences.getString(CURRENCY_KEY, "EUR");

        String userNameEmetteur = sharedPreferences.getString("User", "");
        String streetEmetteur = sharedPreferences.getString("Street", "");
        String cityEmetteur = sharedPreferences.getString("City", "");
        String codePostaleEmetteur = sharedPreferences.getString("codePostale", "");
        String countryEmetteur = sharedPreferences.getString("Country", "");
        String sirenEmetteur = sharedPreferences.getString("siren", "");
        String tvaEmetteur = sharedPreferences.getString("tva", "");
        String telEmetteur = sharedPreferences.getString("tel", "");
        String emailEmetteur = sharedPreferences.getString("email", "");

        File templateFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "template-devis-orgapro-output.pdf");

        InputStream inputStream = getContext().getAssets().open("template-devis.pdf");
        FileUtils.copyToFile(inputStream, templateFile);

        try {
            // Load the template with form fields
            PDDocument document = PDDocument.load(templateFile);
            // Get the AcroForm from the document
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            //Information emetteur
            PDField userNameEmetteurField= acroForm.getField("nomEmetteur"); // Assuming field name is 'CustomerName'
            userNameEmetteurField.setValue(userNameEmetteur);
            PDField rueEmetteurField = acroForm.getField("rueEmetteur");
            rueEmetteurField.setValue(streetEmetteur);
            PDField coodePostVilleEmetteurField = acroForm.getField("coodePostVilleEmetteur");
            coodePostVilleEmetteurField.setValue(codePostaleEmetteur+" "+cityEmetteur);
            PDField paysEmetteurField = acroForm.getField("paysEmetteur");
            paysEmetteurField.setValue(countryEmetteur);
            PDField sirenEmetteurField = acroForm.getField("sirenEmetteur");
            sirenEmetteurField.setValue(sirenEmetteur);
            PDField tvaEmetteurField = acroForm.getField("tvaEmetteur");
            tvaEmetteurField.setValue(tvaEmetteur);
            PDField telephoneEmetteurField = acroForm.getField("telephoneEmetteur");
            telephoneEmetteurField.setValue(telEmetteur);
            PDField emailEmetteurField = acroForm.getField("emailEmetteur");
            emailEmetteurField.setValue(emailEmetteur);

            //Information Client
            PDField customerNameField = acroForm.getField("nomClient");
            customerNameField.setValue(customerName);
            PDField rueClientField = acroForm.getField("rueClient");
            rueClientField.setValue(rueClient);
            PDField codePostVilleClientField = acroForm.getField("codePostVilleClient");
            codePostVilleClientField.setValue(codePostaleClient+" "+villeClient);
            PDField paysClientField = acroForm.getField("paysClient");
            paysClientField.setValue(pays);
            PDField sirenClientField = acroForm.getField("sirenClient");
            sirenClientField.setValue(siren);
            PDField tvaClientField = acroForm.getField("tvaClient");
            tvaClientField.setValue(tva);


            PDField numeroFactureField = acroForm.getField("numeroFacture");
            numeroFactureField.setValue(dateCode+String.format("%02d", sharedPreferences.getInt("last-invoice-number", mumeroFacture)));
            PDField dateFactureField = acroForm.getField("dateFacture");
            dateFactureField.setValue(todayCode);
            PDField modePayementField = acroForm.getField("modePayement");
            modePayementField.setValue(txtModePayement.getEditText().getText().toString());

            float finalCost = Float.parseFloat(txtPrix.getText().toString());
            float prixht = finalCost / (1 + new Float(txtTva.getText().toString()) / 100);
            finalCost = prixht * (1 + new Float(txtTva.getText().toString()) / 100);

            float qauntity = Float.parseFloat(txtQuantite.getText().toString());

            PDField descriptionField = acroForm.getField("description");
            descriptionField.setValue(txtDesciption.getText().toString());
            PDField quantiteField = acroForm.getField("quantite");
            quantiteField.setValue(txtQuantite.getText().toString());
            PDField prixField = acroForm.getField("prix");
            prixField.setValue(String.format("%.2f",prixht)+" "+currency);

            PDField tvaPourcentField = acroForm.getField("tvaPourcent");
            tvaPourcentField.setValue(txtTva.getText().toString()+ "%");

            PDField tvaEuroField = acroForm.getField("tvaEuro");
            tvaEuroField.setValue(String.format("%.2f",(finalCost-prixht))+" "+currency);

            PDField totalHtField = acroForm.getField("totalHt");
            totalHtField.setValue(String.format("%.2f", prixht * qauntity)+" "+currency);

            PDField TOTALHTField = acroForm.getField("TOTALHT");
            TOTALHTField.setValue(String.format("%.2f", prixht * qauntity)+" "+currency);

            PDField totalTvaField = acroForm.getField("totalTva");
            totalTvaField.setValue(String.format("%.2f",(finalCost-prixht) * qauntity)+" "+currency);

            PDField totalTtcField = acroForm.getField("totalTtc");
            totalTtcField.setValue(String.format("%.2f", finalCost * qauntity)+" "+currency);

            String newDirectory = settingsSharedPreferences.getString(DIRECTORY_KEY, "");
            File downloadDirectoryFromPreference = new File(newDirectory);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "Facture_"+customerName.trim().replace(" ", "_")+"_"+dateCode+mumeroFacture+".pdf";
            File invoiceFile = new File(!downloadDirectoryFromPreference.toString().isEmpty()?  downloadDirectoryFromPreference : downloadsDir, fileName);

            // Save the filled form
            document.save(invoiceFile);
            // Close the document
            document.close();

            final String recipientEmail = email;
            navigateToFragmentPreviewPdf(invoiceFile.getAbsolutePath().toString(), recipientEmail);
            saveInvoiceAmount(finalCost);

        } catch (FileNotFoundException e) {
            Log.d("mylog", "Error while writing " + e.toString());
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveInvoiceAmount(float finalCost) {
        // Initialize Room database
        db = Room.databaseBuilder(getContext(), AppDatabase.class, "MyClients").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        InvoiceDao invoiceDao = db.invoiceDao();
        invoiceDao.insertInvoice(new Invoice(finalCost, new Date()));
    }

    private void navigateToFragmentPreviewPdf(String filePath, String recipientEmail) {
        // Create an instance of FragmentB
        Fragment fragmentWebView = new WebViewPdfFragment();

        Bundle bundle = new Bundle();
        bundle.putString("file_path", filePath.toString());
        bundle.putString("mail_client", recipientEmail);// Pass file path as String

        // Set the arguments to the FragmentB
        fragmentWebView.setArguments(bundle);
        // Get FragmentManager and start a transaction
       requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.flFragment, fragmentWebView)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
//        // Check if the activity has a default ActionBar
//        if (getActivity() != null) {
//            getActivity().setTitle("   Factures et Clients");
//            // Set the ActionBar title
//            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//            // Enable the display of the home icon
//            actionBar.setDisplayShowHomeEnabled(true);
//            actionBar.setDisplayUseLogoEnabled(true);
//            // Change the ActionBar icon
//            actionBar.setLogo(R.drawable.invoice);
//            //actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1017e8")));
//        }
    }


}