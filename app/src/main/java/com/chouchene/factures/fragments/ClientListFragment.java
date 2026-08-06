package com.chouchene.factures.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.CustomAdapter;
import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.chouchene.factures.entity.Client;
import com.chouchene.factures.utils.LottieUtils;
import com.chouchene.factures.utils.SwipeToDeleteCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private com.facebook.shimmer.ShimmerFrameLayout shimmerContainer;
    private FloatingActionButton fab;
    private View speedDialLayout;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchContactPicker();
                } else {
                    Toast.makeText(getContext(), R.string.msg_contact_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleContactResult(result.getData().getData());
                }
            });

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
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        fab = view.findViewById(R.id.fab);

        if (mode == Mode.RECENT) {
            fab.setVisibility(View.GONE);
        }

        loadData();

        listAdapter = new CustomAdapter(this.getActivity(), myClients, -1);
        listAdapter.setOnDataChangedListener(this::checkEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(listAdapter);

        // Start transition once data is loaded and laid out
        recyclerView.post(() -> {
            startPostponedEnterTransition();
        });

        // Speed Dial logic
        speedDialLayout = view.findViewById(R.id.speedDialLayout);
        fab.setOnClickListener(v -> {
            toggleSpeedDial();
        });

        view.findViewById(R.id.optionManual).setOnClickListener(v -> {
            toggleSpeedDial();
            showAddClientDialog(false);
        });

        view.findViewById(R.id.optionImport).setOnClickListener(v -> {
            toggleSpeedDial();
            showAddClientDialog(true);
        });

        new ItemTouchHelper(new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                onDeleteClick(listAdapter.getClientAt(position));
            }
        }).attachToRecyclerView(recyclerView);

        if (viewModel != null && mode == Mode.ALL) {
            viewModel.getHighlightClientId().observe(getViewLifecycleOwner(), id -> {
                if (id != -1) {
                    performHighlight(id);
                    viewModel.consumeHighlight();
                }
            });
        }
    }

    private void showAddClientDialog(boolean autoStartImport) {
        if (!isAdded()) return;
        
        if (autoStartImport) {
            checkPermissionAndLaunchPicker();
        } else {
            openAddClientBottomSheet(null);
        }
    }

    private void openAddClientBottomSheet(Client prefilledClient) {
        AddClientBottomSheet bottomSheet = AddClientBottomSheet.newInstance(prefilledClient);
        bottomSheet.setOnClientSavedListener(() -> {
            if (isAdded()) {
                loadData();
                listAdapter.setData(myClients);
                com.google.android.material.snackbar.Snackbar.make(requireView(), "Client ajouté avec succès", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
        bottomSheet.show(getChildFragmentManager(), "ADD_CLIENT");
    }

    private void checkPermissionAndLaunchPicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            launchContactPicker();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void handleContactResult(Uri contactUri) {
        if (contactUri == null) return;

        ContentResolver cr = requireContext().getContentResolver();
        try (Cursor cursor = cr.query(contactUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Name
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String name = nameIndex != -1 ? cursor.getString(nameIndex) : "";

                // Phone
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phone = phoneIndex != -1 ? cursor.getString(phoneIndex) : "";

                // Contact ID to fetch email
                int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                String email = "";
                if (contactIdIndex != -1) {
                    String contactId = cursor.getString(contactIdIndex);
                    email = fetchEmail(contactId);
                }

                // Pre-fill a client object
                Client prefilled = new Client(name, "", "", "", "", "", "", email, phone);
                openAddClientBottomSheet(prefilled);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.msg_import_error, Toast.LENGTH_SHORT).show();
        }
    }

    private String fetchEmail(String contactId) {
        ContentResolver cr = requireContext().getContentResolver();
        try (Cursor emailCursor = cr.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{contactId}, null)) {

            if (emailCursor != null && emailCursor.moveToFirst()) {
                int emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                if (emailIndex != -1) return emailCursor.getString(emailIndex);
            }
        }
        return "";
    }

    private void toggleSpeedDial() {
        if (speedDialLayout.getVisibility() == View.GONE) {
            speedDialLayout.setVisibility(View.VISIBLE);
            fab.setImageResource(R.drawable.baseline_cancel_24);
        } else {
            speedDialLayout.setVisibility(View.GONE);
            fab.setImageResource(R.drawable.rounded_add_24);
        }
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
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Simulate delay to show off shimmer
                Thread.sleep(1200);
            } catch (InterruptedException ignored) {}

            ArrayList<Client> data;
            if (mode == Mode.RECENT) {
                data = (ArrayList<Client>) clientDao.getRecentClients();
            } else {
                data = (ArrayList<Client>) clientDao.getAllClients();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    myClients = data;
                    if (shimmerContainer != null) {
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                    // Update data and show list OR empty state
                    listAdapter.setData(myClients);
                    // checkEmptyState is called inside setData via the listener, 
                    // but we ensure list/empty state visibility here for safety
                    boolean isEmpty = myClients.isEmpty();
                    emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
            }
        });
    }

    private void checkEmptyState() {
        if (listAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            com.airbnb.lottie.LottieAnimationView lottie = emptyState.findViewById(R.id.lottie_empty_state);
            android.widget.ImageView staticImg = emptyState.findViewById(R.id.img_empty_state);
            LottieUtils.loadLottieWithFallback(lottie, staticImg, "anim_empty_invoices.json");
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
