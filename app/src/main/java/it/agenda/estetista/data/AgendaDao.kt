package it.agenda.estetista.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {
    // Operatrici
    @Query("SELECT * FROM operators ORDER BY id")
    fun operators(): Flow<List<Operator>>

    @Query("SELECT * FROM operators ORDER BY id")
    suspend fun allOperators(): List<Operator>

    @Query("SELECT COUNT(*) FROM operators")
    suspend fun operatorCount(): Int

    @Upsert
    suspend fun upsertOperators(list: List<Operator>)

    // Servizi
    @Query("SELECT * FROM services ORDER BY name COLLATE NOCASE")
    fun services(): Flow<List<Service>>

    @Query("SELECT * FROM services")
    suspend fun allServices(): List<Service>

    @Query("SELECT COUNT(*) FROM services")
    suspend fun serviceCount(): Int

    @Upsert
    suspend fun upsertService(s: Service)

    @Insert
    suspend fun insertServices(list: List<Service>)

    @Delete
    suspend fun deleteService(s: Service)

    @Query("DELETE FROM services")
    suspend fun clearServices()

    // Appuntamenti
    @Query("SELECT * FROM appointments WHERE date = :day ORDER BY startMin")
    fun appointmentsOn(day: Long): Flow<List<Appointment>>

    @Query(
        "SELECT * FROM appointments WHERE date = :day AND operatorId = :op " +
            "AND id != :excludeId AND status != 'DISDETTO'"
    )
    suspend fun sameColumn(day: Long, op: Int, excludeId: Long): List<Appointment>

    @Query("SELECT * FROM appointments ORDER BY date, startMin")
    suspend fun allAppointments(): List<Appointment>

    @Upsert
    suspend fun upsertAppointment(a: Appointment)

    @Insert
    suspend fun insertAppointments(list: List<Appointment>)

    @Delete
    suspend fun deleteAppointment(a: Appointment)

    @Query("DELETE FROM appointments")
    suspend fun clearAppointments()

    // Anagrafica clienti
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    fun clients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
    suspend fun allClients(): List<Client>

    @Query(
        "SELECT * FROM clients WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' " +
            "ORDER BY name COLLATE NOCASE LIMIT 8"
    )
    suspend fun searchClients(q: String): List<Client>

    @Query("SELECT * FROM clients WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun clientByName(name: String): Client?

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun clientById(id: Long): Client?

    @Insert
    suspend fun insertClient(c: Client): Long

    @Insert
    suspend fun insertClients(list: List<Client>)

    @Upsert
    suspend fun upsertClient(c: Client)

    @Delete
    suspend fun deleteClient(c: Client)

    @Query("UPDATE appointments SET clientId = NULL WHERE clientId = :id")
    suspend fun unlinkClient(id: Long)

    @Query("UPDATE appointments SET clientName = :name, phone = :phone WHERE clientId = :id AND date >= :fromDay")
    suspend fun syncClientInFuture(id: Long, name: String, phone: String, fromDay: Long)

    @Query("DELETE FROM clients")
    suspend fun clearClients()

    @Query("SELECT * FROM appointments WHERE clientId = :id ORDER BY date DESC, startMin DESC")
    fun appointmentsOfClient(id: Long): Flow<List<Appointment>>

    @Query("SELECT clientId AS id, COUNT(*) AS n FROM appointments WHERE clientId IS NOT NULL AND status != 'DISDETTO' GROUP BY clientId")
    fun visitCounts(): Flow<List<VisitCount>>

    @Query("SELECT * FROM appointments WHERE date BETWEEN :from AND :to ORDER BY date, startMin")
    fun appointmentsBetween(from: Long, to: Long): Flow<List<Appointment>>

    @Query("UPDATE clients SET birthdayMsgYear = :year WHERE id = :id")
    suspend fun setBirthdaySent(id: Long, year: Int)

    // Turni e assenze
    @Query("SELECT * FROM shifts ORDER BY operatorId, dayOfWeek, startMin")
    fun shifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts")
    suspend fun allShifts(): List<Shift>

    @Query("DELETE FROM shifts WHERE operatorId = :op AND dayOfWeek = :dow")
    suspend fun deleteShifts(op: Int, dow: Int)

    @Query("DELETE FROM shifts WHERE operatorId = :op")
    suspend fun deleteAllShifts(op: Int)

    @Query("DELETE FROM shifts")
    suspend fun clearShifts()

    @Insert
    suspend fun insertShifts(list: List<Shift>)

    @Query("SELECT * FROM absences ORDER BY fromDate, operatorId")
    fun absences(): Flow<List<Absence>>

    @Query("SELECT * FROM absences")
    suspend fun allAbsences(): List<Absence>

    @Upsert
    suspend fun upsertAbsence(a: Absence)

    @Insert
    suspend fun insertAbsences(list: List<Absence>)

    @Delete
    suspend fun deleteAbsence(a: Absence)

    @Query("DELETE FROM absences")
    suspend fun clearAbsences()

    // Post-it
    @Query("SELECT * FROM postit WHERE date = :day OR date IS NULL ORDER BY done, date IS NOT NULL, createdAt DESC")
    fun postItsFor(day: Long): Flow<List<PostIt>>

    @Query("SELECT * FROM postit WHERE done = 0 ORDER BY date IS NULL, date, createdAt")
    fun openPostIts(): Flow<List<PostIt>>

    @Query("SELECT * FROM postit")
    suspend fun allPostIts(): List<PostIt>

    @Upsert
    suspend fun upsertPostIt(p: PostIt)

    @Insert
    suspend fun insertPostIts(list: List<PostIt>)

    @Delete
    suspend fun deletePostIt(p: PostIt)

    @Query("DELETE FROM postit")
    suspend fun clearPostIts()
}

data class VisitCount(val id: Long, val n: Int)
