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

public class MoneyTransactionServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {

        resp.getWriter().println(
                PageGenerator
                        .getInstance()
                        .getPage("moneyTransactionPage.html", Collections.emptyMap())
        );
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {

        /* Получаем параметры*/
        String senderName = req.getParameter("senderName").trim();
        String senderPass = req.getParameter("senderPass");
        long count = Long.parseLong(req.getParameter("count"));
        String nameTo = req.getParameter("nameTo").trim();

        BankClientService bankClientService = new BankClientService();
        BankClient sender = bankClientService.getClientByName(senderName);

        boolean result = false;
        if (sender != null && sender.getPassword().equals(senderPass)) {
            result = bankClientService.sendMoneyToClient(sender, nameTo, count);
        }

        String resultString = result
                ? "The transaction was successful"
                : "transaction rejected";

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
