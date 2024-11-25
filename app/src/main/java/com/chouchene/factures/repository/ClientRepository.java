package com.chouchene.factures.repository;

import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.entity.Client;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientRepository {

    private ClientDao clientDao;
    private ExecutorService executorService;

    public ClientRepository (ClientDao clientDao) {
        this.clientDao = clientDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void addClientIfNotExists(final Client clientToInsert) {
        // Run on a background thread using Executor
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Client existingClient = clientDao.getClientByClientName(clientToInsert.clientName);
                if(existingClient == null) {
                    clientDao.insertClient(clientToInsert);
                }
            }
        });
    }

    public void deleteClient(final Client clientToDelete) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                clientDao.deleteClient(clientToDelete);
            }
        });
    }

    public List<Client> getAllClients() {
        return clientDao.getAllClients();
    }

    public void updateClient(final Client clientToUpdate) {
        clientDao.updateClient(clientToUpdate);
    }

}
