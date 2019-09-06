package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import model.BankClient;

public class BankClientDAO implements AutoCloseable {

    private Connection connection;

    public BankClientDAO(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Возвращает список всех клиентов из таблицы, или пустой список - если таблица пуста.
     *
     * @return <tt>List of BankClient</tt>
     */
    public @NotNull
    List<BankClient> getAllBankClient() throws SQLException {

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
            return (clientsList.isEmpty()) ? Collections.emptyList() : clientsList;
        }
    }

    /**
     * Проверяет, есть ли клиент с таким именем и паролем в таблице.
     *
     * @param name     имя клиента.
     * @param password пароль клиента.
     * @return <code>true</code> - если клиент с таким именем и паролем существует в таблице
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

    /**
     * Вспомогательный метод.<p>Выполняет SQL запрос с параметрами,
     * и если результат содержит хотя бы одного клиента,
     * то возвращает первого, иначе - <code>null</code>.
     * <p>В логике данной программы предполагается, что корректный запрос для данного метода
     * должен возвращать результат состоящий только из одной записи или пустой результат.
     * <p>В противном случае возвращается <code>BankClient</code> соответствующий первой записи из <code>ResultSet</code>.
     *
     * @param sql  строка содержащая SQL запрос
     * @param args подстановочные параметры для запроса
     * @return объект <code>BankClient</code>, найденный в таблице по данному SQL запросу,
     * или <code>null</code> если результат запроса пуст
     */
    private @Nullable
    BankClient getClientBySqlQuery(final String sql, final String... args)
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
            long finalAmount = client.getMoney() + transactValue;

            if (finalAmount >= 0) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE bank_client SET money=? WHERE id=?")
                ) {
                    stmt.setLong(1, finalAmount);
                    stmt.setLong(2, client.getId());
                    updatedRows = stmt.executeUpdate();
                }
            }
        }
        if (updatedRows != 1) { // TODO: подумать, что с этим делать
            throw new IllegalStateException("Error while updating clients money!");
        }
    }

    /**
     * Возвращает клиента по его <i>id</i>, или <code>null</code> если такого клиента нет.
     *
     * @param id <i>id</i> клиента
     * @return объект <code>BankClient</code> или <code>null</code> если такого клиента нет
     */
    public @Nullable
    BankClient getClientById(final Long id) throws SQLException {
        return getClientBySqlQuery("SELECT * FROM bank_client WHERE id=?", id.toString());
    }

    /**
     * Проверяет, есть ли на счете клиента с данным именем сумма большая или равная ожидаемой.
     *
     * @param name        имя клиента
     * @param expectedSum ожидаемая сумма
     * @return <code>true</code>, если такой клиент есть, и сумма на его счете не меньше <code>expectedSum</code>
     */
    public boolean isClientHasSum(final String name, final long expectedSum) throws SQLException {
        BankClient client = getClientByName(name);
        return (client != null) && (client.getMoney() >= expectedSum);
    }

    /**
     * Возвращает <i>id</i> клиента по его имени, или <code>null</code> если такого клиента нет.
     *
     * @param name имя клиента
     * @return <i>id</i> клиента или <tt>null</tt> если такого клиента нет
     */
    public @Nullable
    Long getClientIdByName(final String name) throws SQLException {
        BankClient client = getClientByName(name);
        return (client != null) ? client.getId() : null;
    }

    /**
     * Возвращает клиента по его имени или <code>null</code>  если такого клиента нет.
     *
     * @param name имя клиента
     * @return объект <code>BankClient</code> или <code>null</code> если такого клиента нет
     */
    public @Nullable
    BankClient getClientByName(final String name) throws SQLException {
        return getClientBySqlQuery("SELECT * FROM bank_client WHERE name=?", name);
    }

    /**
     * Добавляет клиента в таблицу.
     *
     * @param client объект <code>BankClient</code>
     * @throws IllegalStateException в случае, если количество измененных
     *                               строк в таблице в результате данной операции не равно 1
     */
    public void addClient(final BankClient client) throws SQLException {
        int updatesCount = 0;

        if (getClientByName(client.getName()) == null) { //TODO: возможно здесь надо убрать излишнюю проверку на присутствие клиента в таблице
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO bank_client (name, password, money) values (?, ?, ?)")
            ) {
                stmt.setString(1, client.getName());
                stmt.setString(2, client.getPassword());
                stmt.setLong(3, client.getMoney());
                updatesCount = stmt.executeUpdate();
            }
        }

        if (updatesCount != 1) { //TODO: подумать, что с этим делать
            // Если изменена не 1 строка в таблице, то что-то пошло не так
            throw new IllegalStateException("Error while adding client!");
        }
    }

    public void deleteClient(final String name) throws SQLException {
        int updatedRows = 0;

        if (getClientByName(name) != null) { //TODO: возможно здесь надо убрать излишнюю проверку на отсутствие клиента в таблице
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM bank_client WHERE name='?'")) {
                stmt.setString(1, name);
                updatedRows = stmt.executeUpdate();
            }
        }
        if (updatedRows != 1) { //TODO: подумать, что с этим делать
            // Если изменена не 1 строка в таблице, то что-то пошло не так.
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
