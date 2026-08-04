package com.chouchene.factures.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.chouchene.factures.entity.Client;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface ClientDao {
    @Insert
    void insertClient(Client client);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateClient(Client client);

    @Delete
    void deleteClient(Client client);

    @Query("SELECT * FROM Client")
    List<Client> getAllClients();

    @Query("SELECT * FROM Client WHERE clientName = :name LIMIT 1")
    Client getClientByName(String name);

    @Query("SELECT * FROM Client WHERE id = :id LIMIT 1")
    Client getClientById(int id);

    @Query("SELECT * FROM Client WHERE clientName = :clientName LIMIT 1")
    Client getClientByClientName(String clientName);

    @Query("SELECT * FROM Client WHERE clientName LIKE '%' || :query || '%'")
    List<Client> searchClients(String query);

    @Query("SELECT DISTINCT Client.* FROM Client JOIN invoices ON Client.clientName = invoices.client_name ORDER BY invoices.date DESC LIMIT 10")
    List<Client> getRecentClients();
}
