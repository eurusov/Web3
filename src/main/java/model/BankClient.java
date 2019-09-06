package model;

public class BankClient {
    private long id;
    private String name;
    private String password;
    private long money;

    public BankClient() {

    }

    public BankClient(String name, String password, long money) {
        this.name = name;
        this.password = password;
        this.money = money;
    }

    public BankClient(long id, String name, String password, long money) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.money = money;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankClient that = (BankClient) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return "BankClient{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", password='" + password + '\''
                + ", money=" + money
                + '}';
    }
}
