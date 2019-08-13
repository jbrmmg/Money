package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.dataaccess.RegularRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/jbr")
public class RegularController {
    final static private Logger LOG = LoggerFactory.getLogger(RegularController.class);

    private final
    RegularRepository regularRepository;

    @Autowired
    public RegularController(RegularRepository regularRepository) {
        this.regularRepository = regularRepository;
    }

    @RequestMapping(path="/ext/money/transaction/regulars",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsExt() {
        LOG.info("Get the regular payments. (ext)");
        return regularRepository.findAll();
    }

    @RequestMapping(path="/int/money/transaction/regulars",method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Regular> getRegularPaymentsInt() {
        LOG.info("Get the regular payments.(int)");
        return regularRepository.findAll();
    }
}
