package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class StatementController {
    final static private Logger LOG = LoggerFactory.getLogger(StatementController.class);

    private final
    StatementRepository statementRepository;

    @Autowired
    public StatementController(StatementRepository statementRepository) {
        this.statementRepository = statementRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private Iterable<Statement> statements() {
        LOG.info("Get statements.");

        List<Statement> statementList = statementRepository.findAllByOrderByAccountAsc();

        // Sort the list by the backup time.
        Collections.sort(statementList);

        return statementList;
    }

    @RequestMapping(path="/ext/money/statement", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Statement>  statementsExt() {
        return statements();
    }

    @RequestMapping(path="/int/money/statement", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Statement>  statementsInt() {
        return statements();
    }
}
