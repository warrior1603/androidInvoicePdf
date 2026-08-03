package com.chouchene.factures.repository;

import com.chouchene.factures.dao.ClientDao;
import com.chouchene.factures.entity.Client;

import java.util.List;

public class ClientRepository {

    private ClientDao clientDao;
    public ClientRepository (ClientDao clientDao) {
        this.clientDao = clientDao;
    }

    public void addClientIfNotExists(final Client clientToInsert) {
        Client existingClient = clientDao.getClientByClientName(clientToInsert.clientName);
        if (existingClient == null) {
            clientDao.insertClient(clientToInsert);
        }
    }

    public void deleteClient(final Client clientToDelete) {
        clientDao.deleteClient(clientToDelete);
    }

    public List<Client> getAllClients() {
        return clientDao.getAllClients();
    }

    public void updateClient(final Client clientToUpdate) {
        clientDao.updateClient(clientToUpdate);
    }

}
