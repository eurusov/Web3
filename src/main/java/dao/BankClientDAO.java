package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import model.BankClient;

public class BankClientDAO implements AutoCloseable {

    private Connection connection;

    public BankClientDAO(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Возвращает список всех клиентов в таблице, или пустой List - если таблица пуста.
     *
     * @return List of BankClient objects.
     */
    public List<BankClient> getAllBankClient() throws SQLException { /* was blank */

        try (Statement stmt = connection.createStatement();
             ResultSet result = stmt.executeQuery("SELECT * FROM bank_client")
        ) {
            List<BankClient> clientsList = new ArrayList<>();

            while (result.next()) {
                BankClient client = new BankClient(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getString("password"),
                        result.getLong("money")
                );
                clientsList.add(client);
            }
            return clientsList;
        }
    }

    /**
     * Проверяет, есть ли клиент с таким именем и паролем в базе.
     *
     * @param name     имя клиента.
     * @param password пароль клиента.
     * @return true - если клиент с таким именем и паролем существует в таблице,
     * false - в случае если такой клиент не найден.
     */
    public boolean validateClient(final String name, final String password)
            throws SQLException {
        BankClient client = getClientBySqlQuery(
                "SELECT * FROM bank_client WHERE name=? AND password=?",
                name,
                password
        );
        return client != null;
    }

//    public boolean validateClient(final String name, final String password)
//            throws SQLException {
//        try (PreparedStatement stmt = connection.prepareStatement(
//                "SELECT * FROM bank_client WHERE name=? AND password=?"
//        )
//        ) {
//            stmt.setString(1, name);
//            stmt.setString(2, password);
//
//            try (ResultSet resultSet = stmt.executeQuery()) {
//                return resultSet.next();
//            }
//        }
//    }
//

    /**
     * Вспомогательный метод.
     * Выполняет SQL запрос, и если результат содержит хотя бы одного клиента,
     * то возвращает первого, иначе - null.
     * В логике данной программы предполагается, что корректный запрос для данного метода
     * должен вернуть результат состоящий только из одной записи или пустой результат.
     * В противном случае возвращается BankClient соответствующий первой записи из ResultSet.
     *
     * @param sql строка содержащая SQL запрос.
     * @return объект BankClient, найденный в таблице по данному SQL запросу,
     * или null если результат запроса пуст.
     */
    private BankClient getClientBySqlQuery(final String sql, final String... args)
            throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                stmt.setString(i + 1, args[i]);
            }
            try (ResultSet result = stmt.executeQuery()) {
                BankClient client = null;
                if (result.next()) {
                    client = new BankClient(
                            result.getLong("id"),
                            result.getString("name"),
                            result.getString("password"),
                            result.getLong("money")
                    );
                }
                return client;
            }
        }
    }

    /**
     * Изменяет сумму на счете клиента.
     *
     * @param name          имя клиента
     * @param password      пароль
     * @param transactValue сумма транзакции (положительная - деньги зачисляются,
     *                      отрицательная - снимаются)
     * @throws IllegalStateException в случае, если количество измененных
     *                               строк в таблице в результате данной операции не равно 1
     */
    public void updateClientsMoney(
            final String name,
            final String password,
            final long transactValue
    )
            throws SQLException {

        int updatedRows = 0;

        if (validateClient(name, password)) {

            BankClient client = getClientByName(name);
            long money = client.getMoney() + transactValue;

            if (money >= 0) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE bank_client SET money=? WHERE id=?")
                ) {
                    stmt.setLong(1, money);
                    stmt.setLong(2, client.getId());
                    updatedRows = stmt.executeUpdate();
                }
            }
        }
        // подумать, что с этим делать
        if (updatedRows != 1) {
            throw new IllegalStateException("Error while updating clients money!");
        }
    }

    /**
     * Осуществляет перевод денег от одного клиента другому.
     *
     * <p>Выполняет перевод за один <tt>executeBatch</tt>
     *
     * @throws IllegalStateException в случае, если количество измененных
     *                               строк в таблице в результате данной операции не равно 2
     */
    public void doMoneyTransfer(final BankClient from, final BankClient to, final long sum) throws SQLException {
        if (validateClient(from.getName(), from.getPassword())
                && validateClient(to.getName(), to.getPassword())
                && isClientHasSum(from.getName(), sum)) {

            boolean autocmt = connection.getAutoCommit();
            connection.setAutoCommit(false);

            Statement stmt = connection.createStatement();
            stmt.addBatch("UPDATE bank_client"
                    + " SET money=" + (from.getMoney() - sum)
                    + " WHERE id=" + from.getId()
            );
            stmt.addBatch("UPDATE bank_client"
                    + " SET money=" + (to.getMoney() + sum)
                    + " WHERE id=" + to.getId()
            );

            int[] updatedRows = stmt.executeBatch();
            connection.commit();
            stmt.close();
            connection.setAutoCommit(autocmt);

            if (updatedRows.length < 2 || updatedRows[0] != 1 || updatedRows[1] != 1) {
                throw new IllegalStateException("Error while updating clients money!");
            }
        }
    }

    /**
     * Возвращает клиента по его id или null если такого клиента нет.
     *
     * @param id id клиента
     * @return объект BankClient или null если такого клиента нет
     */
    public BankClient getClientById(final Long id) throws SQLException {  /* was blank */
        return getClientBySqlQuery("SELECT * FROM bank_client WHERE id=?", id.toString());
    }

    /**
     * Проверяет, есть ли на счете клиента с данным именем сумма большая или равная ожидаемой.
     *
     * @param name        имя клиента
     * @param expectedSum ожидаемая сумма
     * @return false, если сумма на счете клиента меньше указанной или такого клиента нет,
     * иначе - true
     */
    public boolean isClientHasSum(final String name, final long expectedSum) throws SQLException {
        BankClient client = getClientByName(name);
        return (client != null) && (client.getMoney() >= expectedSum);
    }

    /**
     * Возвращает id клиента по его имени или null если такого клиента нет.
     *
     * @param name имя клиента
     * @return id клиента или null если такого клиента нет
     */
    public Long getClientIdByName(final String name) throws SQLException {
        BankClient client = getClientByName(name);
        return (client != null) ? client.getId() : null;
    }

    /**
     * Возвращает клиента по его имени или null если такого клиента нет.
     *
     * @param name имя клиента
     * @return объект BankClient или null если такого клиента нет
     */
    public BankClient getClientByName(final String name) throws SQLException {
        return getClientBySqlQuery("SELECT * FROM bank_client WHERE name=?", name);
    }

    /**
     * Добавляет клиента в таблицу.
     *
     * @param client объект BankClient
     * @throws IllegalStateException в случае, если количество измененных
     *                               строк в таблице в результате данной операции не равно 1
     */
    public void addClient(final BankClient client) throws SQLException {
        int updatesCount = 0;
        // возможно здесь надо убрать излишнюю проверку на присутствие клиента в таблице
        if (getClientByName(client.getName()) == null) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO bank_client (name, password, money) values (?, ?, ?)")
            ) {
                stmt.setString(1, client.getName());
                stmt.setString(2, client.getPassword());
                stmt.setLong(3, client.getMoney());
                updatesCount = stmt.executeUpdate();
            }
        }
        // подумать, что с этим делать
        if (updatesCount != 1) {  // Если изменена не 1 строка в таблице, то что-то пошло не так.
            throw new IllegalStateException("Error while adding client!");
        }
    }

    public void deleteClient(final String name) throws SQLException {
        int updatedRows = 0;
        // возможно здесь надо убрать излишнюю проверку на отсутствие клиента в таблице
        if (getClientByName(name) != null) {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM bank_client WERE name='?'")) {
                stmt.setString(1, name);
                updatedRows = stmt.executeUpdate();
            }
        }
        // подумать, что с этим делать
        if (updatedRows != 1) {  // Если изменена не 1 строка в таблице, то что-то пошло не так.
            throw new IllegalStateException("Error while deleting client!");
        }
    }

    public void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS bank_client ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`name` VARCHAR(255) NOT NULL,"
                    + "`password` VARCHAR(60) NOT NULL,"
                    + "`money` BIGINT NOT NULL)"
            );
        }
    }

    public void dropTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS bank_client");
        }
    }

    @Override
    public void close() throws SQLException {
        if (!connection.isClosed()) {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            connection.close();
        }
    }
}
