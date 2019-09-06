package servlet;

import model.BankClient;
import service.BankClientService;
import util.PageGenerator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegistrationServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.getWriter().println(
                PageGenerator
                        .getInstance()
                        .getPage("registrationPage.html", Collections.emptyMap())
        );
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        /* Получаем параметры*/
        String name = req.getParameter("name");
        String password = req.getParameter("password");
        long money = Long.parseLong(req.getParameter("money"));

        /* Создаем непроверенного клиента */
        BankClient newClient = new BankClient(name, password, money);

        BankClientService bankClientService = new BankClientService();

        boolean result = bankClientService.addClient(newClient);

        String resultString = result ? "Add client successful" : "Client not add";

        /* формируем response */
        Map<String, Object> pageVariables = new HashMap<>();
        pageVariables.put("message", resultString);
        resp.getWriter().println(
                PageGenerator
                        .getInstance()
                        .getPage("resultPage.html", pageVariables)
        );
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
