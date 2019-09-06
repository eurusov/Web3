import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import service.BankClientService;
import servlet.ApiServlet;
import servlet.MoneyTransactionServlet;
import servlet.RegistrationServlet;
import servlet.ResultServlet;

public class Main {
    public static void main(String[] args) throws Exception {
        ApiServlet apiServlet = new ApiServlet();
        RegistrationServlet registrationServlet = new RegistrationServlet();
        MoneyTransactionServlet moneyTransactionServlet = new MoneyTransactionServlet();
        ResultServlet resultServlet = new ResultServlet();

        BankClientService clientService = new BankClientService();
        clientService.createTable();

        /**/
//        List<BankClient> clientList = clientService.getAllClient();
//        clientList.forEach(System.out::println);
        /**/

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.addServlet(new ServletHolder(apiServlet), "/api/*");
        context.addServlet(new ServletHolder(registrationServlet), "/");
        context.addServlet(new ServletHolder(registrationServlet), "/registration");
        context.addServlet(new ServletHolder(moneyTransactionServlet), "/transaction");
        context.addServlet(new ServletHolder(resultServlet), "/result");

        Server server = new Server(8080);
        server.setHandler(context);

        server.start();
        server.join();
    }
}
