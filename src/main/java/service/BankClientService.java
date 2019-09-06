package service;

import dao.BankClientDAO;
import exception.DBException;
import model.BankClient;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class BankClientService {

    public BankClientService() {
    }

    public BankClient getClientById(long id) throws DBException {
        try (BankClientDAO dao = getBankClientDAO()) {
            return dao.getClientById(id);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * Возвращает клиента по его имени, или null если такого клиента нет.
     *
     * @param name имя клиента
     * @return объект BankClient, или null если такого клиента нет
     * @throws DBException если во время выполнения запроса было выброшено SQLException
     */
    public BankClient getClientByName(String name) throws DBException {
        try (BankClientDAO dao = getBankClientDAO()) {
            return dao.getClientByName(name);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * Возвращает список всех клиентов в таблице или пустой список, если клиентов нет.
     *
     * @return List of BankClient objects.
     * @throws DBException если во время выполнения запроса было выброшено SQLException
     */
    public List<BankClient> getAllClient() {
        try (BankClientDAO dao = getBankClientDAO()) {
            return dao.getAllBankClient();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * Удаляет клиента с данным именем из таблицы.
     *
     * @param name имя клиента
     * @return true в случае успеха, false в случае если такой клиент отсутствует в таблице
     * @throws DBException если во время выполнения запроса было выброшено SQLException или IllegalStateException
     */
    public boolean deleteClient(String name) {
        if (getClientByName(name) == null) {
            return false;
        }
        try (BankClientDAO dao = getBankClientDAO()) {
            dao.deleteClient(name);
            return true;
        } catch (IllegalStateException | SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * Добавляет клиента в таблицу.
     *
     * @param client объект BankClient
     * @return true в случае успеха, false в случае если такой клиент уже был
     * @throws DBException если во время выполнения запроса было выброшено SQLException или IllegalStateException
     */
    public boolean addClient(BankClient client) throws DBException {
        if (getClientByName(client.getName()) != null) {
            return false;
        }
        try (BankClientDAO dao = getBankClientDAO()) {
            dao.addClient(client);
            return true;
        } catch (SQLException | IllegalStateException e) {
            throw new DBException(e);
        }
    }

    /**
     * Переводит деньги от одного клиента другому.
     *
     * @param sender клиент от которого переводятся деньги
     * @param name   имя клиента которому переводятся деньги
     * @param value  сумма перевода
     * @return <tt>true</tt> в случае успеха
     * @throws DBException если во время выполнения запроса было выброшено
     *                     <code>SQLException</code> или <code>IllegalStateException</code>
     */
    public boolean sendMoneyToClient(BankClient sender, String name, long value) {
        BankClient recipient = getClientByName(name);
        if (sender == null || recipient == null || value <= 0) {
            return false;
        }
        try (BankClientDAO dao = getBankClientDAO()) {
//            getBankClientDAO().doMoneyTransfer(sender, recipient, value);
            if (dao.validateClient(sender.getName(), sender.getPassword())
                    && dao.isClientHasSum(sender.getName(), value)
            ) {
//            connection.setAutoCommit(false);
                dao.updateClientsMoney(sender.getName(), sender.getPassword(), -value);
                dao.updateClientsMoney(name, recipient.getPassword(), value);
                return true;
//            connection.commit();
            } else {
                return false;
            }
        } catch (SQLException | IllegalStateException e) {
            throw new DBException(e);
        }
    }

    public void cleanUp() throws DBException {
        try (BankClientDAO dao = getBankClientDAO()) {
            dao.dropTable();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    public void createTable() throws DBException {
        try (BankClientDAO dao = getBankClientDAO()) {
            dao.createTable();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    private static Connection getMysqlConnection() {
        try {
            DriverManager.registerDriver((Driver) Class.forName("com.mysql.cj.jdbc.Driver").newInstance());

            StringBuilder url = new StringBuilder();

            url.
                    append("jdbc:mysql://").        //db type
                    append("localhost:").           //host name
                    append("3306/").                //port
                    append("bankdb?").              //db name
                    append("user=root&").           //login
                    append("password=msql74_&").    //password
                    append("serverTimezone=UTC");   //timezone

            System.out.println("URL: " + url + "\n");
            return DriverManager.getConnection(url.toString());

        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Return existing BankClientDAO instance
     */
    private BankClientDAO getBankClientDAO() {
        return new BankClientDAO(getMysqlConnection());
    }
}
