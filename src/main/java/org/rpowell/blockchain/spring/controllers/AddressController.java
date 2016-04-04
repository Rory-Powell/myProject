package org.rpowell.blockchain.spring.controllers;

import org.rpowell.blockchain.spring.repositories.GraphRepository;
import org.rpowell.blockchain.spring.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    @Autowired
    private GraphService graphService;

    @RequestMapping(value = "/addresses", method = RequestMethod.GET)
    public String list(Model model){
        model.addAttribute("addresses", graphService.getAllAddresses());
        log.info("Returning all addresses");
        return "addresses";
    }


}
