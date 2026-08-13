package com.chouchene.factures.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.chouchene.factures.entity.Booking;

import java.util.Date;
import java.util.List;

@Dao
public interface BookingDao {

    @Insert
    long insertBooking(Booking booking);

    @Update
    void updateBooking(Booking booking);

    @Delete
    void deleteBooking(Booking booking);

    @Query("SELECT * FROM bookings ORDER BY date_time ASC")
    LiveData<List<Booking>> getAllBookings();

    @Query("SELECT * FROM bookings WHERE id = :id")
    Booking getBookingById(int id);

    @Query("SELECT * FROM bookings WHERE date_time >= :start AND date_time <= :end ORDER BY date_time ASC")
    List<Booking> getBookingsBetweenDates(Date start, Date end);

    @Query("SELECT * FROM bookings WHERE date_time >= :startOfDay ORDER BY date_time ASC")
    LiveData<List<Booking>> getUpcomingBookings(Date startOfDay);
    
    @Query("SELECT COUNT(*) FROM bookings WHERE date_time >= :start AND date_time <= :end AND status = 'Scheduled'")
    int getUpcomingCount(Date start, Date end);

    @Query("SELECT COUNT(*) FROM bookings WHERE strftime('%m-%Y', date_time / 1000, 'unixepoch', 'localtime') = strftime('%m-%Y', :date / 1000, 'unixepoch', 'localtime')")
    int getMonthlyBookingsCount(Date date);

    @Query("SELECT * FROM bookings ORDER BY date_time DESC LIMIT 10")
    List<Booking> getLatestBookings();

    @Query("SELECT * FROM bookings WHERE date_time >= :now AND status = 'Scheduled' ORDER BY date_time ASC LIMIT 1")
    Booking getNextUpcomingBooking(Date now);

    @Query("SELECT * FROM bookings WHERE status = 'Scheduled' AND date_time >= :now ORDER BY date_time ASC")
    List<Booking> getActiveUpcomingBookings(Date now);

    @Query("SELECT * FROM bookings WHERE client_name LIKE '%' || :query || '%' OR pickup_location LIKE '%' || :query || '%' OR destination_location LIKE '%' || :query || '%' ORDER BY date_time DESC")
    List<Booking> searchBookings(String query);
}
